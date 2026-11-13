package com.exiledradio.trickytrialsbackport.trial;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.entity.projectile.EntitySpectralArrow;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.21's ominous item spawner ({@code ominous_item_spawner}, entity id 15): an ominous trial spawner puts
 * one above a player or one of its mobs; after 3 to 6 seconds it drops its item, or fires it straight down
 * if it is a projectile (arrows, splash and lingering potions, fire charges, wind charges).
 */
public class EntityOminousItemSpawner extends Entity {

    private static final int SPAWN_ITEM_DELAY_MIN = 60;
    private static final int SPAWN_ITEM_DELAY_MAX = 120;
    public static final int TICKS_BEFORE_ABOUT_TO_SPAWN_SOUND = 36;

    private static final DataParameter<ItemStack> ITEM = EntityDataManager.createKey(EntityOminousItemSpawner.class, DataSerializers.ITEM_STACK);

    private long spawnItemAfterTicks;

    public EntityOminousItemSpawner(World world) {
        super(world);
        this.noClip = true;
        setSize(0.25F, 0.25F);
    }

    public static EntityOminousItemSpawner create(World world, ItemStack stack) {
        EntityOminousItemSpawner e = new EntityOminousItemSpawner(world);
        e.spawnItemAfterTicks = SPAWN_ITEM_DELAY_MIN + world.rand.nextInt(SPAWN_ITEM_DELAY_MAX - SPAWN_ITEM_DELAY_MIN + 1);
        e.setItem(stack);
        return e;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(ITEM, ItemStack.EMPTY);
    }

    public ItemStack getItem() {
        return this.dataManager.get(ITEM);
    }

    private void setItem(ItemStack stack) {
        this.dataManager.set(ITEM, stack);
    }

    public long spawnItemAfterTicks() {
        return this.spawnItemAfterTicks;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            if (this.world.getTotalWorldTime() % 5L == 0L) {
                com.exiledradio.trickytrialsbackport.trial.client.TrialFx.ominousSpawning(this);
            }
            return;
        }
        if (this.ticksExisted == this.spawnItemAfterTicks - TICKS_BEFORE_ABOUT_TO_SPAWN_SOUND) {
            this.world.playSound(null, new BlockPos(this), TrialContent.sound("block.trial_spawner.about_to_spawn_item"),
                    SoundCategory.NEUTRAL, 1.0F, 1.0F);
        }
        if (this.ticksExisted >= this.spawnItemAfterTicks) {
            spawnItem();
            setDead();
        }
    }

    private void spawnItem() {
        ItemStack stack = getItem();
        if (stack.isEmpty()) {
            return;
        }
        Entity projectile = projectile(stack);
        if (projectile == null) {
            this.world.spawnEntity(new EntityItem(this.world, this.posX, this.posY, this.posZ, stack));
        }
        TrialMessage.send(this.world, new BlockPos(this), TrialMessage.SPAWN_ITEM, 1);
        setItem(ItemStack.EMPTY);
    }

    /**
     * Fires a projectile item straight down with its dispenser numbers (1.21's {@code ProjectileItem}):
     * power 1.1 and spread 6 for arrows, 1.375 and 3 for thrown potions, 1 and 6.67 for charges.
     *
     * @return the projectile, or null when the item is not one
     */
    @Nullable
    private Entity projectile(ItemStack stack) {
        double x = this.posX;
        double y = this.posY;
        double z = this.posZ;
        Entity shot = null;
        float power = 1.1F;
        float spread = 6.0F;
        if (stack.getItem() == Items.ARROW || stack.getItem() == Items.TIPPED_ARROW || stack.getItem() == Items.SPECTRAL_ARROW) {
            EntityArrow arrow;
            if (stack.getItem() == Items.SPECTRAL_ARROW) {
                arrow = new EntitySpectralArrow(this.world, x, y, z);
            } else {
                EntityTippedArrow tipped = new EntityTippedArrow(this.world, x, y, z);
                tipped.setPotionEffect(stack);
                arrow = tipped;
            }
            arrow.pickupStatus = EntityArrow.PickupStatus.ALLOWED;
            shot = arrow;
        } else if (stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION) {
            shot = new EntityPotion(this.world, x, y, z, stack.copy());
            power = 1.1F * 1.25F;
            spread = 3.0F;
        } else if (stack.getItem() == Items.FIRE_CHARGE) {
            // 1.21's FireChargeItem: straight down, spread by triangle(0.11485) on each axis, no shot speed.
            java.util.Random r = this.world.rand;
            double ax = 0.11485 * (r.nextDouble() - r.nextDouble());
            double ay = -1.0 + 0.11485 * (r.nextDouble() - r.nextDouble());
            double az = 0.11485 * (r.nextDouble() - r.nextDouble());
            EntitySmallFireball fireball = new EntitySmallFireball(this.world, x, y, z, ax, ay, az);
            this.world.playEvent(1018, new BlockPos(this), 0);
            this.world.spawnEntity(fireball);
            return fireball;
        } else if (stack.getItem().getRegistryName() != null && "wind_charge".equals(stack.getItem().getRegistryName().getPath())) {
            shot = EntityList.createEntityByIDFromName(new net.minecraft.util.ResourceLocation(
                    stack.getItem().getRegistryName().getNamespace(), "wind_charge"), this.world);
            if (shot != null) {
                shot.setPosition(x, y, z);
            }
            power = 1.0F;
            spread = 6.6666665F;
        }
        if (!(shot instanceof IProjectile)) {
            return null;
        }
        ((IProjectile) shot).shoot(0.0, -1.0, 0.0, power, spread);
        this.world.spawnEntity(shot);
        return shot;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setItem(tag.hasKey("item", 10) ? VaultSettings.stack(tag.getCompoundTag("item")) : ItemStack.EMPTY);
        this.spawnItemAfterTicks = tag.getLong("spawn_item_after_ticks");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (!getItem().isEmpty()) {
            tag.setTag("item", getItem().writeToNBT(new NBTTagCompound()));
        }
        tag.setLong("spawn_item_after_ticks", this.spawnItemAfterTicks);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected boolean canBeRidden(Entity entity) {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean doesEntityNotTriggerPressurePlate() {
        return true;
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }
}
