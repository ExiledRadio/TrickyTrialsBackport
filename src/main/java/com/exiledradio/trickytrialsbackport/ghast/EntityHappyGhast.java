package com.exiledradio.trickytrialsbackport.ghast;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketEntityTeleport;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

/**
 * The happy ghast and its baby, the ghastling (1.21.6; 1.21.11's {@code HappyGhast}), one entity as in vanilla:
 * 20 health, flying speed 0.05, 4 x 4 (the ghastling 0.2375 of that), no fall damage, no breeding.
 *
 * <ul>
 * <li><b>Ghastling</b>: grows up in 20 minutes like any baby animal, faster with snowballs (each takes off 10% of
 * what is left); breathes underwater; flies with a flying move control and path navigation
 * ({@link GhastAi.Baby}).</li>
 * <li><b>Adult</b>: floats around like a ghast (random spots within 16 blocks, kept near home: 64 blocks, or 32 when
 * harnessed), never near blocks ({@link GhastAi.MoveHelper}); tempted by snowballs and, without a harness, by
 * harnesses; heals 1 every 30 seconds, every second in rain or snow or in the clouds.</li>
 * <li><b>Harness</b>: put on by hand or dispenser, taken off with shears while no one rides; with one, up to four
 * players ride ({@link #updatePassenger}), the first steering: forward and back by the look (back at half speed),
 * strafing, jump to climb. A player standing on top stops it (the "still timeout", 10 ticks after they leave),
 * and then it is solid to stand on.</li>
 * </ul>
 */
public class EntityHappyGhast extends EntityAnimal {

    public static final ResourceLocation LOOT = Content.id("entities/happy_ghast");

    public static final float BABY_SCALE = 0.2375F;
    public static final float WIDTH = 4.0F;
    public static final float EYES = 2.6F;
    public static final int MAX_PASSENGERS = 4;
    /** 1.21's passenger attachments, front, left, back, right, turned with the body. */
    static final double[][] SEATS = {{0.0D, 4.0D, 1.7D}, {-1.7D, 4.0D, 0.0D}, {0.0D, 4.0D, -1.7D}, {1.7D, 4.0D, 0.0D}};
    /** A player sits 0.6 below the attachment point (1.21's player vehicle attachment). */
    static final double PLAYER_SEAT_DROP = 0.6D;
    public static final int FAST_HEALING_TICKS = 20;
    public static final int SLOW_HEALING_TICKS = 600;
    public static final int SMALL_RESTRICTION_RADIUS = 32;
    public static final int LARGE_RESTRICTION_RADIUS = 64;
    private static final int STILL_TIMEOUT_ON_LOAD_GRACE_PERIOD = 60;
    private static final int MAX_STILL_TIMEOUT = 10;
    /** 1.12.2's overworld clouds are drawn at y 128; 1.21 counts the 4 blocks above that height as "in the clouds". */
    static final double CLOUD_HEIGHT = 128.0D;
    static final double TEMPT_RANGE = 16.0D;

    private static final DataParameter<Boolean> IS_LEASH_HOLDER = EntityDataManager.createKey(EntityHappyGhast.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> STAYS_STILL = EntityDataManager.createKey(EntityHappyGhast.class, DataSerializers.BOOLEAN);
    private static final DataParameter<ItemStack> BODY = EntityDataManager.createKey(EntityHappyGhast.class, DataSerializers.ITEM_STACK);

    private static final Field IS_JUMPING = ReflectionHelper.findField(EntityLivingBase.class, "isJumping", "field_70703_bu");

    private int leashHolderTime;
    private int serverStillTimeout;
    /** The child state the controls were last set up for, so they change only when it does. */
    private Boolean setUpAsChild;
    /** On a client, the local player who just got off, to be put on top until the server's teleport arrives. */
    private Entity dismounted;

    public EntityHappyGhast(World world) {
        super(world);
        this.setSize(WIDTH, WIDTH);
        this.experienceValue = 0;
        if (world != null && !world.isRemote) {
            this.setUpAsChild = false;
            this.adultSetup();
        }
    }

    // ------------------------------------------------------------------------------------------
    // Attributes, size and data
    // ------------------------------------------------------------------------------------------

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.FLYING_SPEED);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).setBaseValue(0.05D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.05D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(IS_LEASH_HOLDER, false);
        this.dataManager.register(STAYS_STILL, false);
        this.dataManager.register(BODY, ItemStack.EMPTY);
    }

    @Override
    public void setScaleForAge(boolean child) {
        this.setScale(child ? BABY_SCALE : 1.0F);
        if (this.world != null && !this.world.isRemote && (this.setUpAsChild == null || this.setUpAsChild != child)) {
            this.setUpAsChild = child;
            if (child) {
                this.babySetup();
            } else {
                this.adultSetup();
            }
        }
    }

    @Override
    public float getEyeHeight() {
        return this.isChild() ? EYES * BABY_SCALE : EYES;
    }

    /** 1.21's {@code adultGhastSetup}: the ghast's move and look controls and its goals. */
    private void adultSetup() {
        this.moveHelper = new GhastAi.MoveHelper(this);
        setLookHelper(new GhastAi.LookHelper(this));
        this.navigator = new PathNavigateGround(this, this.world);
        GhastAi.adultGoals(this);
    }

    /** 1.21's {@code babyGhastSetup}: a flying move control, a flying navigation and the baby's behaviors. */
    private void babySetup() {
        this.moveHelper = new GhastAi.BabyMoveHelper(this);
        setLookHelper(new net.minecraft.entity.ai.EntityLookHelper(this));
        PathNavigateFlying nav = new PathNavigateFlying(this, this.world);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        this.navigator = nav;
        this.setServerStillTimeout(0);
        GhastAi.babyGoals(this);
    }

    /** 1.12.2's look helper is private, set as the creaking's is. */
    private void setLookHelper(net.minecraft.entity.ai.EntityLookHelper helper) {
        net.minecraftforge.fml.common.ObfuscationReflectionHelper.setPrivateValue(EntityLiving.class, this, helper, "field_70749_g");
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new PathNavigateGround(this, world);
    }

    @Override
    protected void initEntityAI() {
        // Goals are set by adultSetup / babySetup when the age is known.
    }

    // ------------------------------------------------------------------------------------------
    // The harness (1.21's body slot)
    // ------------------------------------------------------------------------------------------

    public ItemStack getHarness() {
        return this.dataManager.get(BODY);
    }

    public boolean isWearingHarness() {
        return !this.getHarness().isEmpty();
    }

    /** Puts a harness on (or takes it off with an empty stack), with the harness's equip sound. */
    public void setHarness(ItemStack stack) {
        boolean had = this.isWearingHarness();
        this.dataManager.set(BODY, stack.copy());
        if (!stack.isEmpty()) {
            this.enablePersistence();
            if (!this.world.isRemote && !this.isSilent()) {
                this.world.playSound(null, this.posX, this.posY, this.posZ, GhastContent.HARNESS_EQUIP, this.getSoundCategory(), 1.0F, 1.0F);
            }
        } else if (had) {
            this.detachHome();
        }
    }

    /** 1.21's {@code canUseSlot(BODY)}: an adult that is alive. */
    public boolean canWearHarness() {
        return this.isEntityAlive() && !this.isChild();
    }

    @Override
    protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
        super.dropEquipment(wasRecentlyHit, lootingModifier);
        if (this.isWearingHarness()) {
            this.entityDropItem(this.getHarness().copy(), 0.0F);
            this.dataManager.set(BODY, ItemStack.EMPTY);
        }
    }

    /**
     * Shears take the harness off, dropped at the middle of the four seats (4 above the feet), with the harness's
     * unequip sound; not while anyone rides, nor while sneaking (1.21.6's {@code attemptToShearEquipment}).
     */
    boolean shear(EntityPlayer player, EnumHand hand, ItemStack shears) {
        if (!this.isWearingHarness() || this.isBeingRidden() || player.isSneaking()) {
            return false;
        }
        if (!this.world.isRemote) {
            ItemStack harness = this.getHarness().copy();
            shears.damageItem(1, player);
            this.setHarness(ItemStack.EMPTY);
            this.playSound(GhastContent.HARNESS_UNEQUIP, 1.0F, 1.0F);
            this.entityDropItem(harness, 4.0F);
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem() == Items.SNOWBALL;
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (this.isChild()) {
            if (this.isBreedingItem(stack)) {
                // Animal.mobInteract for a baby: one snowball takes 10% off the time left.
                if (!player.capabilities.isCreativeMode) {
                    stack.shrink(1);
                }
                this.ageUp((int) ((float) (-this.getGrowingAge() / 20) * 0.1F), true);
                return true;
            }
            return super.processInteract(player, hand);
        }
        if (stack.getItem() == Items.SHEARS && this.shear(player, hand, stack)) {
            return true;
        }
        if (!stack.isEmpty() && stack.interactWithEntity(player, this, hand)) {
            return true;
        }
        if (this.isWearingHarness() && !player.isSneaking()) {
            if (!this.world.isRemote) {
                player.startRiding(this);
            }
            return true;
        }
        if (this.isBreedingItem(stack)) {
            // An adult has no use for snowballs (it cannot fall in love).
            return false;
        }
        return super.processInteract(player, hand);
    }

    @Override
    public boolean canMateWith(EntityAnimal other) {
        return false;
    }

    @Override
    public EntityAgeable createChild(EntityAgeable other) {
        return new EntityHappyGhast(this.world);
    }

    // ------------------------------------------------------------------------------------------
    // Riding
    // ------------------------------------------------------------------------------------------

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return this.getPassengers().size() < MAX_PASSENGERS;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        if (!this.isBeingRidden() && !this.world.isRemote) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, GhastContent.GOGGLES_DOWN, this.getSoundCategory(), 1.0F, 1.0F);
        }
        super.addPassenger(passenger);
        if (!this.world.isRemote) {
            if (!this.scanPlayerAboveGhast()) {
                this.setServerStillTimeout(0);
            } else if (this.serverStillTimeout > MAX_STILL_TIMEOUT) {
                this.setServerStillTimeout(MAX_STILL_TIMEOUT);
            }
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        // 1.21's getDismountLocationForPassenger: the middle of the top. On the server 1.12.2's dismountEntity then
        // teleports the rider there (getAdjustedHorizontalFacing is null, so it looks for no ground nearby).
        if (!this.world.isRemote) {
            Vec3d top = this.dismountLocation();
            passenger.setPosition(top.x, top.y, top.z);
            passenger.motionX = passenger.motionY = passenger.motionZ = 0.0D;
            passenger.fallDistance = 0.0F;
            this.setServerStillTimeout(MAX_STILL_TIMEOUT);
        } else {
            // A client gets off before or after the teleport, and its passenger list is also rebuilt (every rider
            // removed and added again) when anyone gets on; a rider still off next tick is put on top again.
            this.dismounted = passenger;
        }
        if (!this.isBeingRidden()) {
            this.detachHome();
            if (!this.world.isRemote) {
                this.world.playSound(null, this.posX, this.posY, this.posZ, GhastContent.GOGGLES_UP, this.getSoundCategory(), 1.0F, 1.0F);
            }
        }
    }

    /** 1.21's dismount location: the middle of the ghast's top. */
    public Vec3d dismountLocation() {
        return new Vec3d(this.posX, this.getEntityBoundingBox().maxY, this.posZ);
    }

    /**
     * 1.12.2's {@code EntityLivingBase.dismountEntity} looks for ground around the rider in this direction; with none it
     * puts the rider at the vehicle's x and z on its top ({@code minY + height}), which is 1.21's dismount location.
     */
    @Nullable
    @Override
    public net.minecraft.util.EnumFacing getAdjustedHorizontalFacing() {
        return null;
    }

    /**
     * 1.12.2's {@code EntityLiving.canPassengerSteer} also asks this: with a controlling player (harness on, not on
     * its still timeout) the rider's client flies it and the server follows its vehicle-move packets.
     */
    @Override
    public boolean canBeSteered() {
        return this.controllingPlayer() != null;
    }

    /** The first passenger steers when it is a player, the ghast wears a harness and is not on its still timeout. */
    @Nullable
    @Override
    public Entity getControllingPassenger() {
        List<Entity> passengers = this.getPassengers();
        Entity first = passengers.isEmpty() ? null : passengers.get(0);
        return this.isWearingHarness() && !this.isOnStillTimeout() && first instanceof EntityPlayer ? first : null;
    }

    @Nullable
    private EntityPlayer controllingPlayer() {
        Entity e = this.getControllingPassenger();
        return e instanceof EntityPlayer ? (EntityPlayer) e : null;
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (!this.isPassenger(passenger)) {
            return;
        }
        int index = Math.max(0, this.getPassengers().indexOf(passenger)) % SEATS.length;
        Vec3d seat = this.seat(index);
        double drop = passenger instanceof EntityPlayer ? PLAYER_SEAT_DROP : -passenger.getYOffset();
        passenger.setPosition(this.posX + seat.x, this.posY + seat.y - drop, this.posZ + seat.z);
    }

    /** @return seat {@code index}'s offset from the ghast's feet, turned with its body (1.21's attachment points) */
    public Vec3d seat(int index) {
        double[] s = SEATS[index];
        float scale = this.isChild() ? BABY_SCALE : 1.0F;
        // Vec3.yRot(-yBodyRot): local +z is the facing direction.
        float yaw = this.renderYawOffset * 0.017453292F;
        double cos = MathHelper.cos(yaw);
        double sin = MathHelper.sin(yaw);
        double x = s[0] * cos - s[2] * sin;
        double z = s[2] * cos + s[0] * sin;
        return new Vec3d(x * scale, s[1] * scale, z * scale);
    }

    @Override
    public double getMountedYOffset() {
        return this.height;
    }

    /** 1.21's riding offset when the ghast itself rides something. */
    @Override
    public double getYOffset() {
        return -0.5D;
    }

    // ------------------------------------------------------------------------------------------
    // The still timeout: stopping under a player standing on it
    // ------------------------------------------------------------------------------------------

    void setServerStillTimeout(int ticks) {
        if (this.serverStillTimeout <= 0 && ticks > 0 && this.world instanceof WorldServer) {
            // 1.21 sends the exact position to watchers when it stops, so it stands where players see it.
            ((WorldServer) this.world).getEntityTracker().sendToTracking(this, new SPacketEntityTeleport(this));
        }
        this.serverStillTimeout = ticks;
        this.dataManager.set(STAYS_STILL, this.serverStillTimeout > 0);
    }

    public int serverStillTimeout() {
        return this.serverStillTimeout;
    }

    public boolean staysStill() {
        return this.dataManager.get(STAYS_STILL);
    }

    public boolean isOnStillTimeout() {
        return this.staysStill() || this.serverStillTimeout > 0;
    }

    /**
     * 1.21 reaches 1.0E-5 below the top: its server sends exact positions while the ghast is still. 1.12.2's player
     * stands on the client's copy of the ghast, which trails the server's by the tracker's interpolation and rounding,
     * so a rising ghast would never see the player on it; this reaches half a block below the top instead.
     */
    static final double STANDING_TOLERANCE = 0.5D;

    /** A player (not riding a happy ghast) standing on top or just above, within a block of its sides. */
    boolean scanPlayerAboveGhast() {
        AxisAlignedBB box = this.getEntityBoundingBox();
        AxisAlignedBB above = new AxisAlignedBB(box.minX - 1.0D, box.maxY - STANDING_TOLERANCE, box.minZ - 1.0D, box.maxX + 1.0D,
                box.maxY + (box.maxY - box.minY) / 2.0D, box.maxZ + 1.0D);
        for (EntityPlayer player : this.world.playerEntities) {
            if (player.isSpectator()) {
                continue;
            }
            Entity root = player.getLowestRidingEntity();
            if (!(root instanceof EntityHappyGhast) && above.contains(new Vec3d(root.posX, root.posY, root.posZ))) {
                return true;
            }
        }
        return false;
    }

    /**
     * A player whose feet are within {@link #STANDING_TOLERANCE} below the top of a stopped adult is put on the top. The
     * player's own side runs this (the client for a real player): 1.12.2's copy of the ghast catches up with the
     * server's stopping point over a few ticks and can rise into a player standing on it, and 1.12.2's collision ignores a
     * box the player is already inside, so the player would sink through. 1.21's exact positions do not need this.
     *
     * @return whether the player was lifted
     */
    public static boolean liftOntoTop(EntityPlayer player) {
        if (player.isRiding() || player.isSpectator() || player.capabilities.isFlying || player.motionY > 0.0D || player.noClip) {
            return false;
        }
        AxisAlignedBB feet = player.getEntityBoundingBox();
        for (EntityHappyGhast ghast : player.world.getEntitiesWithinAABB(EntityHappyGhast.class, feet.grow(0.0D, STANDING_TOLERANCE, 0.0D))) {
            if (ghast.isChild() || !ghast.isEntityAlive() || !ghast.isOnStillTimeout()) {
                continue;
            }
            AxisAlignedBB box = ghast.getEntityBoundingBox();
            boolean over = feet.maxX > box.minX && feet.minX < box.maxX && feet.maxZ > box.minZ && feet.minZ < box.maxZ;
            if (over && player.posY < box.maxY && player.posY >= box.maxY - STANDING_TOLERANCE) {
                player.setPosition(player.posX, box.maxY, player.posZ);
                player.motionY = 0.0D;
                player.onGround = true;
                player.fallDistance = 0.0F;
                return true;
            }
        }
        return false;
    }

    /** 1.21's {@code stopInPlace}. */
    void stopInPlace() {
        this.getNavigator().clearPath();
        this.moveStrafing = 0.0F;
        this.moveVertical = 0.0F;
        this.moveForward = 0.0F;
        this.setAIMoveSpeed(0.0F);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
    }

    /**
     * Solid like a boat while it stays still; on a client also to a player at or above its top (so one can land on
     * it); never as a ghastling or dead (1.21's {@code canBeCollidedWith}).
     */
    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        if (this.isChild() || !this.isEntityAlive()) {
            return null;
        }
        AxisAlignedBB box = this.getEntityBoundingBox();
        if (this.isOnStillTimeout()) {
            return box;
        }
        if (this.world.isRemote) {
            for (EntityPlayer player : this.world.playerEntities) {
                if (!player.isSpectator() && !this.isPassenger(player) && player.posY >= box.maxY) {
                    return box;
                }
            }
        }
        return null;
    }

    /** A ridden ghast bumps into other ghasts that have riders (1.21: {@code isVehicle() && other instanceof HappyGhast}). */
    @Nullable
    @Override
    public AxisAlignedBB getCollisionBox(Entity other) {
        if (other instanceof EntityHappyGhast && other != this && other.isBeingRidden() && !((EntityHappyGhast) other).isChild()
                && other.isEntityAlive() && this.isBeingRidden()) {
            return other.getEntityBoundingBox();
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote) {
            if (this.leashHolderTime > 0) {
                this.leashHolderTime--;
            }
            if (this.ticksExisted % 5 == 0 && this.holdsQuadLeash()) {
                this.leashHolderTime = 5;
            }
            this.dataManager.set(IS_LEASH_HOLDER, this.leashHolderTime > 0);
            if (this.serverStillTimeout > 0) {
                if (this.ticksExisted > STILL_TIMEOUT_ON_LOAD_GRACE_PERIOD) {
                    this.serverStillTimeout--;
                }
                this.setServerStillTimeout(this.serverStillTimeout);
            }
            if (this.scanPlayerAboveGhast()) {
                this.setServerStillTimeout(MAX_STILL_TIMEOUT);
            }
        } else {
            Entity off = this.dismounted;
            this.dismounted = null;
            if (off instanceof EntityPlayer && ((EntityPlayer) off).isUser() && !off.isRiding() && off.world == this.world
                    && off.posY < this.getEntityBoundingBox().maxY) {
                Vec3d at = this.dismountLocation();
                off.setPosition(at.x, at.y, at.z);
            }
        }
    }

    /**
     * 1.21.6's quad leash: a horse (not a llama) leashed to this ghast makes it a leash holder, which shows the ropes
     * on its harness. 1.12.2 draws one lead from the holder's center.
     */
    private boolean holdsQuadLeash() {
        List<EntityLiving> near = this.world.getEntitiesWithinAABB(EntityLiving.class, this.getEntityBoundingBox().grow(16.0D),
                e -> e != null && e.getLeashed() && e.getLeashHolder() == this && e instanceof AbstractHorse && !(e instanceof EntityLlama));
        return !near.isEmpty();
    }

    public boolean isLeashHolder() {
        return this.dataManager.get(IS_LEASH_HOLDER);
    }

    @Override
    protected void updateAITasks() {
        this.checkRestriction();
        super.updateAITasks();
    }

    /**
     * 1.21's {@code Mob.updateControlFlags} switches off goals that move, look or jump while a player steers; every goal
     * of the happy ghast is one, so its goals stand down (1.12.2's control flags only hold while another goal runs).
     */
    public boolean isPlayerControlled() {
        return this.controllingPlayer() != null;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        this.continuousHeal();
    }

    private int restrictionRadius() {
        return !this.isChild() && !this.isWearingHarness() ? LARGE_RESTRICTION_RADIUS : SMALL_RESTRICTION_RADIUS;
    }

    /** 1.21's {@code checkRestriction}: home is where it is, moved when it strays 16 past the radius. */
    void checkRestriction() {
        if (!this.getLeashed() && !this.isBeingRidden()) {
            int radius = this.restrictionRadius();
            BlockPos here = new BlockPos(this);
            if (!this.hasHome() || this.getHomePosition().distanceSq(here) >= (double) ((radius + 16) * (radius + 16))
                    || radius != (int) this.getMaximumHomeDistance()) {
                this.setHomePosAndDistance(here, radius);
            }
        }
    }

    /** 1 health every 600 ticks, every 20 in rain or snow or in the clouds. */
    void continuousHeal() {
        if (!this.world.isRemote && this.isEntityAlive() && this.deathTime == 0 && this.getMaxHealth() != this.getHealth()) {
            boolean wet = this.isInClouds() || precipitationAt(this.world, new BlockPos(this));
            if (this.ticksExisted % (wet ? FAST_HEALING_TICKS : SLOW_HEALING_TICKS) == 0) {
                this.heal(1.0F);
            }
        }
    }

    /** 1.21's {@code precipitationAt(pos) != NONE}: raining, open to the sky, and rain or snow falling there. */
    static boolean precipitationAt(World world, BlockPos pos) {
        if (!world.isRaining() || !world.canSeeSky(pos) || world.getPrecipitationHeight(pos).getY() > pos.getY()) {
            return false;
        }
        Biome biome = world.getBiome(pos);
        return biome.canRain() || biome.getEnableSnow();
    }

    /** In the overworld's cloud layer (1.21: between the cloud height and 4 above it). */
    public boolean isInClouds() {
        if (!this.world.provider.isSurfaceWorld()) {
            return false;
        }
        return this.posY + this.height >= CLOUD_HEIGHT && this.posY <= CLOUD_HEIGHT + 4.0D;
    }

    @Override
    protected void updateLeashedState() {
        Entity holder = this.getLeashHolder();
        if (this.getLeashed() && holder != null && !holder.isDead && holder.world == this.world && this.isEntityAlive()) {
            // 1.21's happy ghast: no pull within 10 blocks, an elastic pull beyond, the lead snaps past 16. It never
            // walks to its holder (shouldStayCloseToLeashHolder is false).
            float d = this.getDistance(holder);
            if (d > 16.0F) {
                this.clearLeashed(true, true);
            } else if (d > 10.0F) {
                double dx = (holder.posX - this.posX) / d;
                double dy = (holder.posY - this.posY) / d;
                double dz = (holder.posZ - this.posZ) / d;
                this.motionX += dx * Math.abs(dx) * 0.4D;
                this.motionY += dy * Math.abs(dy) * 0.4D;
                this.motionZ += dz * Math.abs(dz) * 0.4D;
                if (this.moveHelper instanceof GhastAi.MoveHelper) {
                    ((GhastAi.MoveHelper) this.moveHelper).setWait();
                }
            }
            return;
        }
        super.updateLeashedState();
    }

    // ------------------------------------------------------------------------------------------
    // Movement
    // ------------------------------------------------------------------------------------------

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
                this.flyRidden(rider);
            } else {
                this.motionX = 0.0D;
                this.motionY = 0.0D;
                this.motionZ = 0.0D;
            }
            return;
        }
        if (this.isServerWorld() || this.canPassengerSteer()) {
            this.travelFlying(strafe, vertical, forward);
        }
    }

    /** One tick of flight under a rider: 1.21's {@code getRiddenInput} and {@code travel}. */
    public void flyRidden(EntityPlayer rider) {
        Vec3d input = riddenInput(rider.moveStrafing, rider.moveForward, rider.rotationPitch, isJumping(rider), this.flyingSpeed());
        this.travelFlying((float) input.x, (float) input.y, (float) input.z);
    }

    /**
     * 1.21's {@code getRiddenInput}: strafe as pressed; forward goes where the rider looks (back at half speed,
     * mirrored), jump adds 0.5 up; all scaled by 3.9 times the flying speed.
     */
    public static Vec3d riddenInput(float strafe, float forward, float pitch, boolean jumping, double flyingSpeed) {
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
        if (jumping) {
            up += 0.5F;
        }
        double s = 3.9F * flyingSpeed;
        return new Vec3d(strafe * s, up * s, ahead * s);
    }

    /** 1.21's {@code tickRidden}: turns 8% of the way to the rider's yaw each tick and takes half their pitch. */
    void tickRidden(EntityPlayer rider) {
        float yaw = this.rotationYaw;
        yaw += MathHelper.wrapDegrees(rider.rotationYaw - yaw) * 0.08F;
        this.setRotation(yaw, rider.rotationPitch * 0.5F);
        this.prevRotationYaw = this.renderYawOffset = this.rotationYawHead = yaw;
    }

    static boolean isJumping(EntityLivingBase e) {
        try {
            return IS_JUMPING.getBoolean(e);
        } catch (IllegalAccessException ex) {
            TrickyTrialsBackport.LOGGER.error("isJumping", ex);
            return false;
        }
    }

    double flyingSpeed() {
        return this.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).getAttributeValue();
    }

    /** 1.21's {@code travelFlying} with the happy ghast's speed (flying speed x 5/3) in water, lava and air alike. */
    void travelFlying(float strafe, float vertical, float forward) {
        float speed = (float) this.flyingSpeed() * 5.0F / 3.0F;
        double friction;
        if (this.isInWater()) {
            friction = 0.8F;
        } else if (this.isInLava()) {
            friction = 0.5D;
        } else {
            friction = 0.91F;
        }
        this.moveRelative(strafe, vertical, forward, speed);
        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        this.motionX *= friction;
        this.motionY *= friction;
        this.motionZ *= friction;
    }

    @Override
    protected float updateDistance(float yawDelta, float distance) {
        if (this.isBeingRidden()) {
            // HappyGhastBodyRotationControl: body and head follow the ghast's yaw while ridden.
            this.rotationYawHead = this.rotationYaw;
            this.renderYawOffset = this.rotationYawHead;
        }
        return super.updateDistance(yawDelta, distance);
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    protected void updateFallState(double y, boolean onGround, net.minecraft.block.state.IBlockState state, BlockPos pos) {
    }

    @Override
    public boolean isOnLadder() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return this.isChild();
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        if (!this.world.isAirBlock(pos)) {
            return 0.0F;
        }
        return this.world.isAirBlock(pos.down()) && !this.world.isAirBlock(pos.down(2)) ? 10.0F : 5.0F;
    }

    @Override
    public boolean getCanSpawnHere() {
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Sounds
    // ------------------------------------------------------------------------------------------

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.block.Block block) {
    }

    @Override
    protected float getSoundPitch() {
        return 1.0F;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.NEUTRAL;
    }

    @Override
    public int getTalkInterval() {
        int interval = super.getTalkInterval();
        return this.isBeingRidden() ? interval * 6 : interval;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isChild() ? GhastContent.GHASTLING_AMBIENT : GhastContent.AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isChild() ? GhastContent.GHASTLING_HURT : GhastContent.HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isChild() ? GhastContent.GHASTLING_DEATH : GhastContent.DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return this.isChild() ? 1.0F : 4.0F;
    }

    @Override
    public int getMaxSpawnedInChunk() {
        return 1;
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    // ------------------------------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------------------------------

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("still_timeout", this.serverStillTimeout);
        if (this.isWearingHarness()) {
            tag.setTag("body_armor_item", this.getHarness().writeToNBT(new NBTTagCompound()));
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.setServerStillTimeout(tag.getInteger("still_timeout"));
        this.dataManager.set(BODY, tag.hasKey("body_armor_item", 10) ? new ItemStack(tag.getCompoundTag("body_armor_item")) : ItemStack.EMPTY);
    }
}
