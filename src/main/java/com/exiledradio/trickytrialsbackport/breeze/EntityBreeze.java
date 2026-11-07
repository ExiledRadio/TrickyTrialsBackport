package com.exiledradio.trickytrialsbackport.breeze;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * The breeze (1.21): 30 health, a trial chamber's wind spirit. It keeps its distance with long jumps,
 * shoots breeze wind charges at players and iron golems it can see within 16 blocks, takes no fall damage,
 * and turns every other projectile back ({@code BreezeEvents}). Drops 1-2 breeze rods when a player kills it.
 *
 * <h2>Behavior</h2>
 * Shooting: an inhale of 15 ticks, the shot (0.7 blocks a tick), then 20-40 ticks before the next.
 * Long jump: from the ground, when the target is too far (over 8 blocks), too near (under 3) or hidden,
 * to a safe landing spot 3-8 blocks from it, 2-5 seconds apart. Otherwise it slides about near its target.
 * Timers live on the entity's own tick, never in a goal's {@code shouldExecute}.
 */
public class EntityBreeze extends EntityMob {

    /** What the model shows: idle, inhaling, shooting, jumping, sliding. */
    public enum Pose {
        IDLE, INHALING, SHOOTING, LONG_JUMPING, SLIDING
    }

    private static final DataParameter<Byte> POSE = EntityDataManager.createKey(EntityBreeze.class, DataSerializers.BYTE);

    public static final ResourceLocation LOOT = Content.id("entities/breeze");

    static final float SHOOT_RANGE = 16.0F;
    static final int INHALE_TICKS = 15;
    static final int SHOOT_RECOVER_TICKS = 10;

    int shootCooldown;
    int jumpCooldown;

    public EntityBreeze(World world) {
        super(world);
        setSize(0.6F, 1.77F);
        this.experienceValue = 10;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(POSE, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.63D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(24.0D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new BreezeAI.Shoot(this));
        this.tasks.addTask(2, new BreezeAI.LongJump(this));
        this.tasks.addTask(3, new BreezeAI.Slide(this));
        this.tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.6D));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(7, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityIronGolem>(this, EntityIronGolem.class, true));
    }

    public Pose pose() {
        int i = this.dataManager.get(POSE);
        return i >= 0 && i < Pose.values().length ? Pose.values()[i] : Pose.IDLE;
    }

    void setPose(Pose pose) {
        this.dataManager.set(POSE, (byte) pose.ordinal());
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) {
            if (this.shootCooldown > 0) {
                this.shootCooldown--;
            }
            if (this.jumpCooldown > 0 && this.onGround) {
                this.jumpCooldown--;
            }
        }
    }

    /** @return true if a living target is set and alive */
    boolean hasTarget() {
        EntityLivingBase target = getAttackTarget();
        return target != null && target.isEntityAlive();
    }

    /** @return true if a breeze can land at this spot: solid ground, two free blocks, nothing that burns */
    boolean isSafeLanding(BlockPos feet) {
        IBlockState ground = this.world.getBlockState(feet.down());
        if (!ground.isSideSolid(this.world, feet.down(), EnumFacing.UP) || ground.getBlock() == Blocks.MAGMA) {
            return false;
        }
        for (int dy = 0; dy < 2; dy++) {
            IBlockState s = this.world.getBlockState(feet.up(dy));
            if (s.getMaterial().blocksMovement() || s.getMaterial().isLiquid() || s.getBlock() == Blocks.FIRE) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------

    @Override
    public void fall(float distance, float damageMultiplier) {
        // Immune to fall damage; the landing sound plays at the end of a long jump.
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DamageSource.FALL) {
            return false;
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    protected boolean canDespawn() {
        // 1.21's breezes come from trial spawners and stay.
        return false;
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Content.sound(this.onGround ? "entity.breeze.idle_ground" : "entity.breeze.idle_air");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return Content.sound("entity.breeze.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Content.sound("entity.breeze.death");
    }

    @Override
    public float getEyeHeight() {
        return 1.3452F;
    }

    /** @return the breeze's yaw toward a point, degrees */
    float yawTo(double x, double z) {
        return (float) (MathHelper.atan2(z - this.posZ, x - this.posX) * (180.0D / Math.PI)) - 90.0F;
    }
}
