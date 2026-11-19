package com.exiledradio.trickytrialsbackport.nautilus;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Guis;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IJumpingMount;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 1.21.11's {@code AbstractNautilus}: a tamable swimmer that takes a saddle and nautilus armor, carries one rider
 * underwater with Breath of the Nautilus, and dashes when the rider jumps. Its brain is 1.12.2 goals in the order of
 * 1.21.11's activities ({@link NautilusAi}).
 *
 * <p>The saddle and armor live in a two-slot inventory (slot 0 saddle, slot 1 armor) that the inventory screen shows;
 * both are mirrored into synced data for the renderer, and the armor's numbers become attribute modifiers.
 */
public abstract class EntityAbstractNautilus extends EntityTameable implements IJumpingMount, IInventoryChangedListener {

    public static final float WIDTH = 0.875F;
    public static final float HEIGHT = 0.95F;
    /** 1.21.11's passenger attachment (1.1375 up) less a player's vehicle attachment (0.6) plus 1.12.2's player offset. */
    public static final double MOUNTED_Y_OFFSET = 1.1375D - 0.6D + 0.35D;
    public static final double PASSENGER_ATTACHMENT = 1.1375D;
    public static final int EFFECT_DURATION = 60;
    public static final int EFFECT_REFRESH_RATE = 40;
    public static final float IN_WATER_SPEED_MODIFIER = 0.011F;
    public static final float RIDDEN_SPEED_IN_WATER = 0.0325F;
    public static final float RIDDEN_SPEED_ON_LAND = 0.02F;
    public static final double WATER_RESISTANCE = 0.9D;
    public static final int DASH_COOLDOWN_TICKS = 40;
    public static final float DASH_MOMENTUM_IN_WATER = 1.2F;
    public static final float DASH_MOMENTUM_ON_LAND = 0.5F;
    public static final int SMALL_RESTRICTION_RADIUS = 16;
    public static final int LARGE_RESTRICTION_RADIUS = 32;
    public static final int RESTRICTION_RADIUS_BUFFER = 8;
    public static final int ANGER_DURATION = 400;
    public static final int SLOT_SADDLE = 0;
    public static final int SLOT_ARMOR = 1;

    private static final DataParameter<Boolean> DASH = EntityDataManager.createKey(EntityAbstractNautilus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SADDLED = EntityDataManager.createKey(EntityAbstractNautilus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<ItemStack> BODY = EntityDataManager.createKey(EntityAbstractNautilus.class, DataSerializers.ITEM_STACK);

    private static final UUID ARMOR_UUID = UUID.fromString("3c4f7b8e-1a2d-4e55-9c61-7d0b2e9a4f11");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("3c4f7b8e-1a2d-4e55-9c61-7d0b2e9a4f12");
    private static final UUID KNOCKBACK_UUID = UUID.fromString("3c4f7b8e-1a2d-4e55-9c61-7d0b2e9a4f13");

    int dashCooldown;
    /** The rider's charged jump, waiting for the next ridden tick on the side that moves the nautilus. */
    float playerJumpPendingScale;
    public InventoryBasic inventory;
    /** Equip sounds are for changes after the first tick, not for loading. */
    private boolean equipSounds;

    // The brain's memories that matter here.
    @Nullable
    UUID angryAt;
    long angryUntil;
    int attackTargetCooldown;
    int chargeCooldown;
    boolean panicking;
    int panicTicks;
    // Set by the subclasses' initEntityAI, which runs inside the constructor: no initializers here.
    NautilusAi.Charge charge;
    NautilusAi.Tempt tempt;

    boolean charging() {
        return this.charge != null && this.charge.running;
    }

    boolean tempted() {
        return this.tempt != null && this.tempt.player != null;
    }

    protected EntityAbstractNautilus(World world) {
        super(world);
        this.setSize(WIDTH, HEIGHT);
        this.moveHelper = new NautilusAi.SwimMoveHelper(this, 85, 10, IN_WATER_SPEED_MODIFIER, 0.0F, true);
        setLookHelper(this, new NautilusAi.SwimLookHelper(this, 10));
        this.experienceValue = 1;
        this.createInventory();
    }

    private static final java.lang.reflect.Field LOOK_HELPER =
            net.minecraftforge.fml.relauncher.ReflectionHelper.findField(EntityLiving.class, "lookHelper", "field_70749_g");

    /** EntityLiving's look helper is private and final; 1.21's nautilus has its own. */
    static void setLookHelper(EntityLiving mob, net.minecraft.entity.ai.EntityLookHelper helper) {
        try {
            LOOK_HELPER.set(mob, helper);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(15.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(1.0D);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.3D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(DASH, false);
        this.dataManager.register(SADDLED, false);
        this.dataManager.register(BODY, ItemStack.EMPTY);
    }

    @Override
    public float getEyeHeight() {
        return this.height * (0.2751F / HEIGHT);
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new PathNavigateSwimmer(this, world);
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        return 0.0F;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    public boolean shouldDismountInWater(Entity rider) {
        return false;
    }

    @Override
    public boolean isNotColliding() {
        // 1.21.11's checkSpawnObstruction: only other entities, as for water mobs.
        return this.world.checkNoEntityCollision(this.getEntityBoundingBox(), this);
    }

    /** 1.21.11's {@code removeWhenFarAway}: always, once nothing made it persistent. */
    @Override
    protected boolean canDespawn() {
        return true;
    }

    @Override
    public boolean isPotionApplicable(PotionEffect effect) {
        return effect.getPotion() != MobEffects.POISON && super.isPotionApplicable(effect);
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.block.Block block) {
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        // NautilusAi.initMemories: no random hunting for the first 2-3 minutes.
        this.attackTargetCooldown = NautilusAi.nonPlayerAttackCooldown(this.rand);
        return super.onInitialSpawn(difficulty, data);
    }

    // ------------------------------------------------------------------------------------------
    // Food and taming
    // ------------------------------------------------------------------------------------------

    /** 1.21.11's {@code isFood}: pufferfish to tame a wild adult, else any fish or fish bucket. */
    public boolean isFood(ItemStack stack) {
        return !this.isTamed() && !this.isChild() ? NautilusItems.isTamingItem(stack) : NautilusItems.isFood(stack);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return this.isFood(stack);
    }

    /** 1.21.11's {@code usePlayerItem}: a fish bucket leaves a water bucket. */
    @Override
    protected void consumeItemFromStack(EntityPlayer player, ItemStack stack) {
        if (NautilusItems.isBucketFood(stack)) {
            if (player.capabilities.isCreativeMode) {
                return;
            }
            stack.shrink(1);
            ItemStack water = new ItemStack(Items.WATER_BUCKET);
            if (stack.isEmpty()) {
                for (EnumHand hand : EnumHand.values()) {
                    if (player.getHeldItem(hand) == stack) {
                        player.setHeldItem(hand, water);
                        return;
                    }
                }
            }
            if (!player.inventory.addItemStackToInventory(water)) {
                player.dropItem(water, false);
            }
            return;
        }
        super.consumeItemFromStack(player, stack);
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        // AbstractNautilus.interact: any use makes it persistent.
        this.enablePersistence();
        ItemStack stack = player.getHeldItem(hand);
        if (this.isChild()) {
            return super.processInteract(player, hand);
        }
        if (this.isTamed() && player.isSneaking()) {
            this.openInventory(player);
            return true;
        }
        // Mob.interact: shears take the armor, else the saddle, while no one rides (not when sneaking).
        if (stack.getItem() == Items.SHEARS && !player.isSneaking() && this.shear(player, stack)) {
            return true;
        }
        if (!stack.isEmpty()) {
            if (!this.world.isRemote && !this.isTamed() && this.isFood(stack)) {
                this.consumeItemFromStack(player, stack);
                this.tryToTame(player);
                return true;
            }
            if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
                if (!this.world.isRemote) {
                    this.heal(stack.getItem() instanceof ItemFood ? 2.0F * ((ItemFood) stack.getItem()).getHealAmount(stack) : 1.0F);
                    this.consumeItemFromStack(player, stack);
                    this.playEatingSound();
                }
                return true;
            }
            if (this.tryEquip(player, stack)) {
                return true;
            }
            if (stack.interactWithEntity(player, this, hand)) {
                return true;
            }
        }
        if (this.isTamed() && !player.isSneaking() && !this.isFood(stack)) {
            this.doPlayerRide(player);
            return true;
        }
        return super.processInteract(player, hand);
    }

    /** 1.21.11's {@code tryToTame}: one in three; hearts or smoke, and the eating sound either way. */
    void tryToTame(EntityPlayer player) {
        if (this.rand.nextInt(3) == 0) {
            this.setTamedBy(player);
            this.navigator.clearPath();
            this.world.setEntityState(this, (byte) 7);
        } else {
            this.world.setEntityState(this, (byte) 6);
        }
        this.playEatingSound();
    }

    @Override
    public void setTamedBy(EntityPlayer player) {
        super.setTamedBy(player);
        this.enablePersistence();
    }

    @Override
    public boolean canMateWith(net.minecraft.entity.passive.EntityAnimal other) {
        return other != this && other.getClass() == this.getClass() && this.isInLove() && other.isInLove();
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return !this.isAggravated() && super.canBeLeashedTo(player);
    }

    // ------------------------------------------------------------------------------------------
    // Saddle and armor
    // ------------------------------------------------------------------------------------------

    private void createInventory() {
        InventoryBasic old = this.inventory;
        this.inventory = new InventoryBasic("Nautilus", false, 2);
        if (old != null) {
            old.removeInventoryChangeListener(this);
            for (int i = 0; i < Math.min(old.getSizeInventory(), 2); i++) {
                this.inventory.setInventorySlotContents(i, old.getStackInSlot(i).copy());
            }
        }
        this.inventory.addInventoryChangeListener(this);
        this.updateEquipment();
    }

    /** 1.21.11's {@code canUseSlot(SADDLE / BODY)}: an alive, tamed adult. */
    public boolean canUseEquipment() {
        return this.isEntityAlive() && !this.isChild() && this.isTamed();
    }

    public boolean isSaddled() {
        return this.dataManager.get(SADDLED);
    }

    public ItemStack getBodyArmor() {
        return this.dataManager.get(BODY);
    }

    public ItemStack getSaddle() {
        return this.inventory.getStackInSlot(SLOT_SADDLE);
    }

    public void setSaddle(ItemStack stack) {
        this.inventory.setInventorySlotContents(SLOT_SADDLE, stack);
    }

    public void setBodyArmor(ItemStack stack) {
        this.inventory.setInventorySlotContents(SLOT_ARMOR, stack);
    }

    public static boolean isSaddle(ItemStack stack) {
        return stack.getItem() == Items.SADDLE;
    }

    /** Equip on use (1.21.11's {@code Equippable.equipOnInteract}): into an empty slot of a tamed adult. */
    boolean tryEquip(EntityPlayer player, ItemStack stack) {
        int slot = isSaddle(stack) ? SLOT_SADDLE : stack.getItem() instanceof ItemNautilusArmor ? SLOT_ARMOR : -1;
        if (slot < 0 || !this.canUseEquipment() || !this.inventory.getStackInSlot(slot).isEmpty()) {
            return false;
        }
        if (!this.world.isRemote) {
            ItemStack one = stack.copy();
            one.setCount(1);
            this.inventory.setInventorySlotContents(slot, one);
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
        }
        return true;
    }

    /** Puts a stack into a slot from a dispenser; false if it cannot go there. */
    public boolean dispenseEquip(ItemStack stack) {
        int slot = isSaddle(stack) ? SLOT_SADDLE : stack.getItem() instanceof ItemNautilusArmor ? SLOT_ARMOR : -1;
        if (slot < 0 || !this.canUseEquipment() || !this.inventory.getStackInSlot(slot).isEmpty()) {
            return false;
        }
        ItemStack one = stack.copy();
        one.setCount(1);
        this.inventory.setInventorySlotContents(slot, one);
        return true;
    }

    /**
     * 1.21.6's {@code attemptToShearEquipment}: the body armor first, then the saddle, dropped at the seat's height,
     * with the item's unequip sound; one shears damage. Not while ridden.
     */
    boolean shear(EntityPlayer player, ItemStack shears) {
        if (this.isBeingRidden()) {
            return false;
        }
        int slot = !this.inventory.getStackInSlot(SLOT_ARMOR).isEmpty() ? SLOT_ARMOR
                : !this.inventory.getStackInSlot(SLOT_SADDLE).isEmpty() ? SLOT_SADDLE : -1;
        if (slot < 0) {
            return false;
        }
        if (!this.world.isRemote) {
            ItemStack off = this.inventory.getStackInSlot(slot).copy();
            this.equipSounds = false;
            this.inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
            this.equipSounds = true;
            this.playSound(slot == SLOT_ARMOR ? NautilusContent.ARMOR_UNEQUIP : Content.sound("item.saddle.unequip"), 1.0F, 1.0F);
            shears.damageItem(1, player);
            this.entityDropItem(off, (float) PASSENGER_ATTACHMENT);
        }
        return true;
    }

    @Override
    public void onInventoryChanged(IInventory inv) {
        boolean wasSaddled = this.isSaddled();
        ItemStack oldArmor = this.getBodyArmor();
        this.updateEquipment();
        if (this.world.isRemote || !this.equipSounds || this.isSilent()) {
            return;
        }
        if (!wasSaddled && this.isSaddled()) {
            boolean under = this.isInsideOfMaterial(net.minecraft.block.material.Material.WATER);
            this.world.playSound(null, this.posX, this.posY, this.posZ,
                    under ? NautilusContent.SADDLE_UNDERWATER_EQUIP : NautilusContent.SADDLE_EQUIP, this.getSoundCategory(), 1.0F, 1.0F);
        }
        ItemStack armor = this.getBodyArmor();
        if (!armor.isEmpty() && !ItemStack.areItemStacksEqual(oldArmor, armor)) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, NautilusContent.ARMOR_EQUIP, this.getSoundCategory(), 1.0F, 1.0F);
        }
    }

    /** Mirrors the two slots into synced data and the armor's numbers into attribute modifiers. */
    void updateEquipment() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        this.dataManager.set(SADDLED, isSaddle(this.inventory.getStackInSlot(SLOT_SADDLE)));
        ItemStack armor = this.inventory.getStackInSlot(SLOT_ARMOR);
        this.dataManager.set(BODY, armor.copy());
        NautilusArmorTier tier = armor.getItem() instanceof ItemNautilusArmor ? ((ItemNautilusArmor) armor.getItem()).tier : null;
        modifier(SharedMonsterAttributes.ARMOR, ARMOR_UUID, "Nautilus armor", tier == null ? 0 : tier.protection);
        modifier(SharedMonsterAttributes.ARMOR_TOUGHNESS, TOUGHNESS_UUID, "Nautilus armor toughness", tier == null ? 0 : tier.toughness);
        modifier(SharedMonsterAttributes.KNOCKBACK_RESISTANCE, KNOCKBACK_UUID, "Nautilus armor knockback resistance",
                tier == null ? 0 : tier.knockbackResistance);
    }

    private void modifier(net.minecraft.entity.ai.attributes.IAttribute attribute, UUID id, String name, double amount) {
        IAttributeInstance inst = this.getEntityAttribute(attribute);
        if (inst == null) {
            return;
        }
        AttributeModifier old = inst.getModifier(id);
        if (old != null && old.getAmount() == amount) {
            return;
        }
        if (old != null) {
            inst.removeModifier(old);
        }
        if (amount != 0) {
            inst.applyModifier(new AttributeModifier(id, name, amount, 0).setSaved(false));
        }
    }

    @Override
    protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
        super.dropEquipment(wasRecentlyHit, lootingModifier);
        for (int i = 0; i < this.inventory.getSizeInventory(); i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                this.entityDropItem(stack.copy(), 0.0F);
                this.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
            }
        }
    }

    /** 1.21.11's {@code openCustomInventoryScreen}: a tamed nautilus, when no one else rides it. */
    public void openInventory(EntityPlayer player) {
        if (!this.world.isRemote && (!this.isBeingRidden() || this.isPassenger(player)) && this.isTamed()) {
            Guis.open(NautilusContent.GUI_ID, player, this.world, new BlockPos(this.getEntityId(), 0, 0));
        }
    }

    // ------------------------------------------------------------------------------------------
    // Riding
    // ------------------------------------------------------------------------------------------

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    void doPlayerRide(EntityPlayer player) {
        if (!this.world.isRemote) {
            player.startRiding(this);
            if (!this.isBeingRidden()) {
                this.detachHome();
            }
        }
    }

    @Override
    public double getMountedYOffset() {
        return MOUNTED_Y_OFFSET;
    }

    /** The first passenger steers when it is a player and the nautilus wears a saddle. */
    @Nullable
    @Override
    public Entity getControllingPassenger() {
        List<Entity> passengers = this.getPassengers();
        Entity first = passengers.isEmpty() ? null : passengers.get(0);
        return this.isSaddled() && first instanceof EntityPlayer ? first : null;
    }

    /**
     * 1.12.2's {@code EntityLiving.canPassengerSteer} also asks this. With a controlling player (saddled, a player in
     * front) the rider's client swims it and the server follows its vehicle-move packets; without the override neither
     * side moved it.
     */
    @Override
    public boolean canBeSteered() {
        return this.controllingPlayer() != null;
    }

    /** 1.21's {@code Mob.updateControlFlags}: while a player steers, goals that move or look stand down. */
    public boolean isPlayerControlled() {
        return this.controllingPlayer() != null;
    }

    @Nullable
    EntityPlayer controllingPlayer() {
        Entity e = this.getControllingPassenger();
        return e instanceof EntityPlayer ? (EntityPlayer) e : null;
    }

    /** 1.21.11's {@code isMobControlled}: a mob rides it. */
    public boolean isMobControlled() {
        List<Entity> passengers = this.getPassengers();
        return !passengers.isEmpty() && passengers.get(0) instanceof EntityLiving;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        EntityPlayer rider = this.controllingPlayer();
        if (this.isEntityAlive() && rider != null) {
            this.tickRidden(rider);
            if (this.canPassengerSteer()) {
                if (this.world.isRemote) {
                    // 1.12.2 slows a client-moved entity by 0.98 before it travels; 1.21 does not.
                    this.motionX /= 0.98D;
                    this.motionY /= 0.98D;
                    this.motionZ /= 0.98D;
                }
                this.travelRidden(rider);
            } else {
                this.motionX = 0.0D;
                this.motionY = 0.0D;
                this.motionZ = 0.0D;
            }
            return;
        }
        this.travelAs(strafe, vertical, forward, this.getAIMoveSpeed());
    }

    /** One ridden tick: 1.21.11's {@code getRiddenInput}, {@code getRiddenSpeed} and travel, after any pending dash. */
    public void travelRidden(EntityPlayer rider) {
        if (this.playerJumpPendingScale > 0.0F) {
            this.executeRidersJump(this.playerJumpPendingScale, rider);
        }
        this.playerJumpPendingScale = 0.0F;
        Vec3d input = riddenInput(rider.moveStrafing, rider.moveForward, rider.rotationPitch);
        this.travelAs((float) input.x, (float) input.y, (float) input.z, this.riddenSpeed());
    }

    /**
     * 1.21.11's {@code getRiddenInput}: strafe as pressed; forward goes where the rider looks (back at half, mirrored).
     */
    public static Vec3d riddenInput(float strafe, float forward, float pitch) {
        float up = 0.0F;
        float ahead = 0.0F;
        if (forward != 0.0F) {
            float cos = MathHelper.cos(pitch * 0.017453292F);
            float sin = -MathHelper.sin(pitch * 0.017453292F);
            if (forward < 0.0F) {
                cos *= -0.5F;
                sin *= -0.5F;
            }
            up = sin;
            ahead = cos;
        }
        return new Vec3d(strafe, up, ahead);
    }

    public float riddenSpeed() {
        float speed = (float) this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
        return (this.isInWater() ? RIDDEN_SPEED_IN_WATER : RIDDEN_SPEED_ON_LAND) * speed;
    }

    /** 1.21.11's {@code tickRidden}: half way to the rider's yaw each tick, half their pitch. */
    void tickRidden(EntityPlayer rider) {
        float yaw = this.rotationYaw;
        yaw += MathHelper.wrapDegrees(rider.rotationYaw - yaw) * 0.5F;
        this.setRotation(yaw, rider.rotationPitch * 0.5F);
        this.prevRotationYaw = this.renderYawOffset = this.rotationYawHead = yaw;
    }

    /**
     * 1.21.11's {@code travelInWater}: the input at the given speed, moved, then 0.9 of the motion kept, with no
     * gravity. Out of water 1.12.2's own travel (gravity and ground friction) at that speed.
     */
    void travelAs(float strafe, float vertical, float forward, float speed) {
        if (this.isInWater()) {
            // As 1.12.2's own travel: only the side that moves it (the server, or a steering rider's client).
            if (this.isServerWorld() || this.canPassengerSteer()) {
                this.moveRelative(strafe, vertical, forward, speed);
                this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
                this.motionX *= WATER_RESISTANCE;
                this.motionY *= WATER_RESISTANCE;
                this.motionZ *= WATER_RESISTANCE;
            }
            this.updateLimbs();
        } else {
            this.setAIMoveSpeed(speed);
            super.travel(strafe, vertical, forward);
        }
    }

    /** 1.12.2's limb swing, which its water travel would have done. */
    private void updateLimbs() {
        this.prevLimbSwingAmount = this.limbSwingAmount;
        double dx = this.posX - this.prevPosX;
        double dz = this.posZ - this.prevPosZ;
        float f = MathHelper.sqrt(dx * dx + dz * dz) * 4.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        this.limbSwingAmount += (f - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

    // ------------------------------------------------------------------------------------------
    // The dash (IJumpingMount)
    // ------------------------------------------------------------------------------------------

    /** From the rider's client when they let go of jump. */
    @Override
    public void setJumpPower(int power) {
        if (this.isSaddled() && this.dashCooldown <= 0) {
            this.playerJumpPendingScale = power >= 90 ? 1.0F : 0.4F + 0.4F * power / 90.0F;
        }
    }

    @Override
    public boolean canJump() {
        return this.isSaddled();
    }

    /** The server hears of the dash: its sound, and the synced dash flag that starts the cooldown everywhere. */
    @Override
    public void handleStartJump(int power) {
        this.playSound(this.getDashSound(), this.getSoundVolume(), 1.0F);
        this.setDashing(true);
    }

    @Override
    public void handleStopJump() {
    }

    /** 1.21.11's {@code executeRidersJump}: the rider's look x momentum x charge x speed. */
    void executeRidersJump(float scale, EntityPlayer rider) {
        Vec3d look = rider.getLookVec();
        double speed = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
        double push = (this.isInWater() ? DASH_MOMENTUM_IN_WATER : DASH_MOMENTUM_ON_LAND) * scale * speed;
        this.motionX += look.x * push;
        this.motionY += look.y * push;
        this.motionZ += look.z * push;
        this.dashCooldown = DASH_COOLDOWN_TICKS;
        this.setDashing(true);
    }

    public int getDashCooldown() {
        return this.dashCooldown;
    }

    public boolean isDashing() {
        return this.dataManager.get(DASH);
    }

    public void setDashing(boolean dashing) {
        boolean was = this.isDashing();
        this.dataManager.set(DASH, dashing);
        if (dashing && !was) {
            this.onDashStarted();
        }
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (DASH.equals(key) && !this.firstUpdate && this.world.isRemote && this.isDashing()) {
            this.onDashStarted();
        }
    }

    /** 1.21.11's {@code onSyncedDataUpdated(DASH)}: a cooldown of 40 unless one is running. */
    private void onDashStarted() {
        this.dashCooldown = this.dashCooldown == 0 ? DASH_COOLDOWN_TICKS : this.dashCooldown;
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.equipSounds = true;
        if (!this.world.isRemote) {
            this.applyEffects();
        }
        if (this.isDashing() && this.dashCooldown < DASH_COOLDOWN_TICKS - 5 && !this.world.isRemote) {
            this.dataManager.set(DASH, false);
        }
        if (this.dashCooldown > 0) {
            this.dashCooldown--;
            if (this.dashCooldown == 0 && !this.world.isRemote) {
                this.playSound(this.getDashReadySound(), this.getSoundVolume(), 1.0F);
            }
        }
        if (this.world.isRemote && this.isInWater()) {
            this.spawnBubbles();
        }
    }

    /** Breath of the Nautilus for a player riding it: given when missing, refreshed every 40 ticks, 60 ticks long. */
    void applyEffects() {
        List<Entity> passengers = this.getPassengers();
        if (passengers.isEmpty() || !(passengers.get(0) instanceof EntityPlayer) || NautilusContent.breath == null) {
            return;
        }
        EntityPlayer player = (EntityPlayer) passengers.get(0);
        boolean has = player.isPotionActive(NautilusContent.breath);
        if (!has || this.world.getTotalWorldTime() % EFFECT_REFRESH_RATE == 0) {
            player.addPotionEffect(new PotionEffect(NautilusContent.breath, EFFECT_DURATION, 0, true, true));
        }
    }

    private void spawnBubbles() {
        double speed = Math.sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
        double chance = MathHelper.clamp(speed * 2.0D, 0.15D, 1.0D);
        if (this.rand.nextFloat() < chance) {
            float pitch = MathHelper.clamp(this.rotationPitch, -10.0F, 10.0F);
            Vec3d view = this.getVectorForRotation(pitch, this.rotationYaw);
            double spread = this.rand.nextDouble() * 0.8D * (1.0D + speed);
            double vx = (this.rand.nextFloat() - 0.5D) * spread;
            double vy = (this.rand.nextFloat() - 0.5D) * spread;
            double vz = (this.rand.nextFloat() - 0.5D) * spread;
            this.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX - view.x * 1.1D, this.posY - view.y + 0.25D,
                    this.posZ - view.z * 1.1D, vx, vy, vz);
        }
    }

    @Override
    protected void updateAITasks() {
        if (this.isPlayerControlled()) {
            this.navigator.clearPath();
        }
        this.checkRestriction();
        NautilusAi.updateMemories(this);
        super.updateAITasks();
    }

    private int restrictionRadius() {
        return !this.isChild() && this.getSaddle().isEmpty() ? LARGE_RESTRICTION_RADIUS : SMALL_RESTRICTION_RADIUS;
    }

    /** 1.21.11's {@code checkRestriction}: a tamed nautilus keeps near where it is, moved when it strays 8 past. */
    void checkRestriction() {
        if (!this.getLeashed() && !this.isBeingRidden() && this.isTamed()) {
            int radius = this.restrictionRadius();
            BlockPos here = new BlockPos(this);
            if (!this.hasHome() || this.getHomePosition().distanceSq(here) >= (double) ((radius + RESTRICTION_RADIUS_BUFFER) * (radius + RESTRICTION_RADIUS_BUFFER))
                    || radius != (int) this.getMaximumHomeDistance()) {
                this.setHomePosAndDistance(here, radius);
            }
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean hurt = super.attackEntityFrom(source, amount);
        if (hurt && !this.world.isRemote) {
            this.panicking = true;
            Entity attacker = source.getTrueSource();
            if (attacker instanceof EntityLivingBase) {
                NautilusAi.setAngerTarget(this, (EntityLivingBase) attacker);
            }
        }
        return hurt;
    }

    /** 1.21.11's {@code isAggravated}: angry at someone or attacking. */
    public boolean isAggravated() {
        return this.angryAt != null || this.getAttackTarget() != null;
    }

    // ------------------------------------------------------------------------------------------
    // Sounds
    // ------------------------------------------------------------------------------------------

    boolean isUnderWater() {
        return this.isInsideOfMaterial(net.minecraft.block.material.Material.WATER);
    }

    abstract SoundEvent getDashSound();

    abstract SoundEvent getDashReadySound();

    abstract void playEatingSound();

    @Override
    protected abstract SoundEvent getSwimSound();

    // ------------------------------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------------------------------

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (!this.getSaddle().isEmpty()) {
            tag.setTag("saddle", this.getSaddle().writeToNBT(new NBTTagCompound()));
        }
        ItemStack armor = this.inventory.getStackInSlot(SLOT_ARMOR);
        if (!armor.isEmpty()) {
            tag.setTag("body_armor_item", armor.writeToNBT(new NBTTagCompound()));
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.equipSounds = false;
        this.inventory.setInventorySlotContents(SLOT_SADDLE, tag.hasKey("saddle") ? new ItemStack(tag.getCompoundTag("saddle")) : ItemStack.EMPTY);
        this.inventory.setInventorySlotContents(SLOT_ARMOR,
                tag.hasKey("body_armor_item") ? new ItemStack(tag.getCompoundTag("body_armor_item")) : ItemStack.EMPTY);
        this.equipSounds = this.ticksExisted > 0;
        this.updateEquipment();
    }

    @Nullable
    @Override
    public EntityAgeable createChild(EntityAgeable other) {
        return null;
    }

    /** For the item check: any of our nautilus armor items. */
    public static boolean isNautilusArmor(Item item) {
        return item instanceof ItemNautilusArmor;
    }
}
