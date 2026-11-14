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
 * 1.21.11's {@code BreezeAi} ({@link BreezeAI.Fight}): with a target more than 4 blocks away and four free blocks
 * above, a 10-tick inhale, then a long jump to a spot 4-8 blocks behind the target, flying without drag; on landing a
 * 100-tick window to shoot one wind charge (15 ticks charging), then the next jump 10 ticks after landing. Where it
 * cannot jump it slides about its target and shoots after each slide. The brain's memories are the timers below,
 * counted down on the entity's own tick.
 */
public class EntityBreeze extends EntityMob {

    /** What the model shows: idle, inhaling, shooting, jumping, sliding. */
    public enum Pose {
        IDLE, INHALING, SHOOTING, LONG_JUMPING, SLIDING
    }

    private static final DataParameter<Byte> POSE = EntityDataManager.createKey(EntityBreeze.class, DataSerializers.BYTE);

    public static final ResourceLocation LOOT = Content.id("entities/breeze");

    static final float SHOOT_RANGE = 16.0F;

    /** 1.21's breeze memories, in ticks left: BREEZE_SHOOT, _SHOOT_COOLDOWN, _SHOOT_CHARGING, _SHOOT_RECOVERING,
     * _JUMP_COOLDOWN, _JUMP_INHALING. */
    int shootWindow;
    int shootCooldown;
    int shootCharging;
    int shootRecovering;
    int jumpCooldown;
    int jumpInhaling;
    /** BREEZE_JUMP_TARGET. */
    @Nullable BlockPos jumpTarget;
    /** {@code LivingEntity.discardFriction}: set for the flight of a long jump. */
    boolean discardFriction;
    /** Long jumps made, for the self-test. */
    public int jumps;

    /** @return true while a long jump's flight ignores drag */
    public boolean discardsFriction() {
        return this.discardFriction;
    }

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
        this.tasks.addTask(1, new BreezeAI.Fight(this));
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
        if (!this.world.isRemote) {
            // The brain forgets expired memories before its behaviors run.
            this.shootWindow = Math.max(0, this.shootWindow - 1);
            this.shootCooldown = Math.max(0, this.shootCooldown - 1);
            this.shootCharging = Math.max(0, this.shootCharging - 1);
            this.shootRecovering = Math.max(0, this.shootRecovering - 1);
            this.jumpCooldown = Math.max(0, this.jumpCooldown - 1);
            this.jumpInhaling = Math.max(0, this.jumpInhaling - 1);
        }
        super.onLivingUpdate();
    }

    /**
     * 1.21's {@code travelInAir} with {@code discardFriction}: the movement, then gravity, and no drag at all, so a
     * long jump flies the arc {@code LongJumpUtil} aimed. Anything else is 1.12.2's own travel.
     */
    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (!this.discardFriction || !this.isServerWorld() || this.isInWater() || this.isInLava()) {
            super.travel(strafe, vertical, forward);
            return;
        }
        float slip = 0.91F;
        if (this.onGround) {
            BlockPos below = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ));
            IBlockState under = this.world.getBlockState(below);
            slip = under.getBlock().getSlipperiness(under, this.world, below, this) * 0.91F;
        }
        float speed = this.onGround ? getAIMoveSpeed() * (0.16277136F / (slip * slip * slip)) : this.jumpMovementFactor;
        moveRelative(strafe, vertical, forward, speed);
        move(net.minecraft.entity.MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        if (!hasNoGravity()) {
            this.motionY -= BreezeAI.GRAVITY;
        }
    }

    /** {@code withinInnerCircleRange}: within 4 blocks across and 10 up or down of the breeze's block's center. */
    boolean withinInnerCircle(net.minecraft.util.math.Vec3d p) {
        BlockPos at = new BlockPos(this);
        double dx = p.x - (at.getX() + 0.5);
        double dz = p.z - (at.getZ() + 0.5);
        double dy = p.y - (at.getY() + 0.5);
        return dx * dx + dz * dz < 16.0 && Math.abs(dy) < 10.0;
    }

    /** Turns body and head to face a point, as {@code lookAt(EYES, ...)} for a jump's direction. */
    void faceTowards(double x, double z) {
        this.rotationYaw = yawTo(x, z);
        this.rotationYawHead = this.rotationYaw;
        this.renderYawOffset = this.rotationYaw;
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
