package com.exiledradio.trickytrialsbackport.breeze;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The breeze's fight, as 1.21.11's {@code BreezeAi} runs it: one goal standing in for the brain's FIGHT activity
 * ({@code Shoot}, {@code LongJump}, {@code Slide}, tried in that order each tick) and the IDLE activity's
 * {@code SlideToTargetSink}. The brain's memories are the breeze's own timers ({@link EntityBreeze}), counted down
 * every tick:
 *
 * <ul>
 *   <li>{@code BREEZE_SHOOT} ({@code shootWindow}): 100 ticks on landing, 60 when a slide ends. Shooting needs it;
 *       jumping and sliding need it gone. A shot ends it, and so does a target 16 blocks or more away.</li>
 *   <li>{@code BREEZE_JUMP_COOLDOWN}: 10 ticks after landing, 2 when hurt in the last 40 ticks.</li>
 *   <li>{@code BREEZE_SHOOT_COOLDOWN}: 10 ticks after a shot. {@code BREEZE_JUMP_INHALING}: 10 ticks before a jump.</li>
 * </ul>
 * So a breeze with a target 4 to 16 blocks away and room above jumps behind it, lands, shoots once, and jumps again.
 */
public final class BreezeAI {

    /** {@code Shoot}: 15 ticks charging, the shot, 4 ticks recovering (its maximum duration); 10 ticks of cooldown. */
    public static final int SHOOT_CHARGE_TICKS = 15;
    public static final int SHOOT_RECOVER_TICKS = 4;
    public static final int SHOOT_DURATION_TICKS = SHOOT_CHARGE_TICKS + 1 + SHOOT_RECOVER_TICKS;
    public static final int SHOOT_COOLDOWN_TICKS = 10;
    /** {@code LongJump}. */
    public static final int JUMP_INHALE_TICKS = 10;
    public static final int JUMP_COOLDOWN_TICKS = 10;
    public static final int JUMP_COOLDOWN_WHEN_HURT_TICKS = 2;
    public static final int JUMP_MAX_DURATION_TICKS = 200;
    public static final int REQUIRED_AIR_BLOCKS_ABOVE = 4;
    public static final float MAX_JUMP_VELOCITY_MULTIPLIER = 0.058333334F;
    public static final List<Integer> ALLOWED_ANGLES = Collections.unmodifiableList(Arrays.asList(40, 55, 60, 75, 80));
    /** {@code BREEZE_SHOOT} after landing and after a slide. */
    public static final int SHOOT_WINDOW_AFTER_JUMP = 100;
    public static final int SHOOT_WINDOW_AFTER_SLIDE = 60;
    /** {@code Shoot.isTargetWithinRange}: under 16 blocks. */
    public static final double SHOOT_RANGE_SQ = 256.0;
    /** Living entities' gravity, {@code LivingEntity.getGravity}. */
    public static final double GRAVITY = 0.08;

    private BreezeAI() {
    }

    /**
     * A timer for a memory set with this expiry: {@code Brain.forgetOutdatedMemories} erases an expired memory before it
     * counts the others down, so one set for n ticks is still there n ticks later. The breeze counts down first, so
     * one more.
     */
    static int ttl(int ticks) {
        return ticks + 1;
    }

    /** The FIGHT activity. Runs while the breeze has a target (and until a jump lands), holding movement and looking. */
    static final class Fight extends EntityAIBase {
        private final EntityBreeze breeze;
        /** Ticks the running behavior has run, for {@code Behavior}'s maximum durations; -1 when not running. */
        private int shootTicks = -1;
        private int jumpTicks = -1;
        private boolean sliding;

        Fight(EntityBreeze breeze) {
            this.breeze = breeze;
            setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            return this.breeze.hasTarget();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.breeze.hasTarget() || this.jumpTicks >= 0;
        }

        @Override
        public void resetTask() {
            if (this.shootTicks >= 0) {
                stopShoot();
            }
            if (this.jumpTicks >= 0) {
                stopJump();
            }
            if (this.sliding) {
                stopSlide();
            }
        }

        @Override
        public void updateTask() {
            EntityBreeze b = this.breeze;
            if (this.sliding) {
                // IDLE's SlideToTargetSink: while the walk target is out, the fight waits for the slide to end.
                if (b.getNavigator().noPath()) {
                    stopSlide();
                }
                return;
            }
            if (this.shootTicks >= 0) {
                tickShoot();
                return;
            }
            if (this.jumpTicks >= 0) {
                tickJump();
                return;
            }
            EntityLivingBase target = b.getAttackTarget();
            if (target == null) {
                return;
            }
            // Shoot (priority 1).
            if (b.pose() == EntityBreeze.Pose.IDLE && b.shootWindow > 0 && b.shootCooldown == 0 && b.jumpTarget == null) {
                if (b.getDistanceSq(target.posX, target.posY, target.posZ) < SHOOT_RANGE_SQ) {
                    startShoot();
                    return;
                }
                b.shootWindow = 0;
            }
            // LongJump (priority 2).
            if (b.jumpCooldown == 0 && b.shootWindow == 0 && canJump(b)) {
                startJump();
                return;
            }
            // Slide (priority 4).
            if (b.jumpCooldown == 0 && b.shootWindow == 0 && b.onGround && !b.isInWater() && b.pose() == EntityBreeze.Pose.IDLE) {
                startSlide(target);
            }
        }

        // ---- Shoot

        private void startShoot() {
            this.shootTicks = 0;
            this.breeze.getNavigator().clearPath();
            this.breeze.setPose(EntityBreeze.Pose.SHOOTING);
            this.breeze.shootCharging = ttl(SHOOT_CHARGE_TICKS);
            this.breeze.playSound(Content.sound("entity.breeze.inhale"), 1.0F, 1.0F);
        }

        private void tickShoot() {
            EntityBreeze b = this.breeze;
            EntityLivingBase target = b.getAttackTarget();
            if (target == null || b.shootWindow == 0 || ++this.shootTicks > SHOOT_DURATION_TICKS) {
                stopShoot();
                return;
            }
            b.getLookHelper().setLookPosition(target.posX, target.posY, target.posZ, 360.0F, 360.0F);
            if (b.shootCharging == 0 && b.shootRecovering == 0) {
                b.shootRecovering = ttl(SHOOT_RECOVER_TICKS);
                shoot(target);
            }
        }

        private void stopShoot() {
            this.shootTicks = -1;
            if (this.breeze.pose() == EntityBreeze.Pose.SHOOTING) {
                this.breeze.setPose(EntityBreeze.Pose.IDLE);
            }
            this.breeze.shootCooldown = ttl(SHOOT_COOLDOWN_TICKS);
            this.breeze.shootWindow = 0;
        }

        private void shoot(EntityLivingBase target) {
            EntityBreezeWindCharge charge = new EntityBreezeWindCharge(this.breeze.world, this.breeze);
            charge.setPosition(this.breeze.posX, this.breeze.posY + this.breeze.height * 0.5 + 0.3, this.breeze.posZ);
            double dx = target.posX - charge.posX;
            double dy = target.posY + target.height * 0.5 - charge.posY;
            double dz = target.posZ - charge.posZ;
            int difficulty = this.breeze.world.getDifficulty().getId();
            charge.shoot(dx, dy, dz, 0.7F, Math.max(1, 6 - 2 * difficulty));
            this.breeze.world.spawnEntity(charge);
            this.breeze.world.playSound(null, this.breeze.posX, this.breeze.posY, this.breeze.posZ, Content.sound("entity.breeze.shoot"),
                    SoundCategory.HOSTILE, 1.5F, 1.0F);
        }

        // ---- LongJump

        private void startJump() {
            EntityBreeze b = this.breeze;
            this.jumpTicks = 0;
            b.getNavigator().clearPath();
            if (b.jumpInhaling == 0) {
                b.jumpInhaling = ttl(JUMP_INHALE_TICKS);
            }
            b.setPose(EntityBreeze.Pose.INHALING);
            b.world.playSound(null, b.posX, b.posY, b.posZ, Content.sound("entity.breeze.charge"), SoundCategory.HOSTILE, 1.0F, 1.0F);
            if (b.jumpTarget != null) {
                b.faceTowards(b.jumpTarget.getX() + 0.5, b.jumpTarget.getZ() + 0.5);
            }
        }

        private void tickJump() {
            EntityBreeze b = this.breeze;
            // Behavior's canStillUse (not standing, no jump cooldown) and its 200-tick limit.
            if (b.pose() == EntityBreeze.Pose.IDLE || b.jumpCooldown > 0 || ++this.jumpTicks > JUMP_MAX_DURATION_TICKS) {
                stopJump();
                return;
            }
            if (b.jumpInhaling == 0 && b.pose() == EntityBreeze.Pose.INHALING) {
                Vec3d v = b.jumpTarget == null ? null
                        : optimalJumpVector(b, b.getRNG(), new Vec3d(b.jumpTarget.getX() + 0.5, b.jumpTarget.getY(), b.jumpTarget.getZ() + 0.5));
                if (v == null) {
                    b.setPose(EntityBreeze.Pose.IDLE);
                    return;
                }
                b.playSound(Content.sound("entity.breeze.jump"), 1.0F, 1.0F);
                b.setPose(EntityBreeze.Pose.LONG_JUMPING);
                b.rotationYaw = b.renderYawOffset;
                b.discardFriction = true;
                b.motionX = v.x;
                b.motionY = v.y;
                b.motionZ = v.z;
                b.isAirBorne = true;
                b.velocityChanged = true;
                b.jumps++;
            } else if (b.pose() == EntityBreeze.Pose.LONG_JUMPING && (b.onGround || b.isInWater())) {
                b.playSound(Content.sound("entity.breeze.land"), 1.0F, 1.0F);
                b.setPose(EntityBreeze.Pose.IDLE);
                b.discardFriction = false;
                b.jumpCooldown = ttl(b.getLastDamageSource() != null ? JUMP_COOLDOWN_WHEN_HURT_TICKS : JUMP_COOLDOWN_TICKS);
                b.shootWindow = ttl(SHOOT_WINDOW_AFTER_JUMP);
            }
        }

        private void stopJump() {
            this.jumpTicks = -1;
            EntityBreeze b = this.breeze;
            if (b.pose() == EntityBreeze.Pose.LONG_JUMPING || b.pose() == EntityBreeze.Pose.INHALING) {
                b.setPose(EntityBreeze.Pose.IDLE);
            }
            b.jumpTarget = null;
            b.jumpInhaling = 0;
        }

        // ---- Slide

        private void startSlide(EntityLivingBase target) {
            EntityBreeze b = this.breeze;
            Vec3d to = null;
            Vec3d targetPos = target.getPositionVector();
            if (b.withinInnerCircle(targetPos)) {
                Vec3d away = RandomPositionGenerator.findRandomTargetBlockAwayFrom(b, 5, 5, targetPos);
                if (away != null && hasLineOfSight(b, away) && target.getDistanceSq(away.x, away.y, away.z) > target.getDistanceSq(b)) {
                    to = away;
                }
            }
            if (to == null) {
                to = b.getRNG().nextBoolean() ? randomPointBehindTarget(target, b.getRNG()) : randomPointInMiddleCircle(b, target);
            }
            BlockPos walk = new BlockPos(to);
            if (b.getNavigator().tryMoveToXYZ(walk.getX() + 0.5, walk.getY(), walk.getZ() + 0.5, 0.6D)) {
                this.sliding = true;
                b.setPose(EntityBreeze.Pose.SLIDING);
                b.playSound(Content.sound("entity.breeze.slide"), 1.0F, 1.0F);
            }
        }

        private void stopSlide() {
            this.sliding = false;
            EntityBreeze b = this.breeze;
            b.getNavigator().clearPath();
            b.setPose(EntityBreeze.Pose.IDLE);
            if (b.hasTarget()) {
                b.shootWindow = ttl(SHOOT_WINDOW_AFTER_SLIDE);
            }
        }
    }

    // ---- vanilla's helpers

    /** {@code LongJump.canRun}; sets the jump target when it says yes. */
    static boolean canJump(EntityBreeze b) {
        if (!b.onGround && !b.isInWater()) {
            return false;
        }
        if (b.jumpTarget != null) {
            return true;
        }
        EntityLivingBase target = b.getAttackTarget();
        if (target == null) {
            return false;
        }
        double follow = b.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        if (target.getDistanceSq(b) >= follow * follow) {
            b.setAttackTarget(null);
            return false;
        }
        if (target.getDistance(b) - 4.0F <= 0.0F) {
            return false;
        }
        if (!canJumpFromCurrentPosition(b)) {
            return false;
        }
        BlockPos landing = snapToSurface(b, randomPointBehindTarget(target, b.getRNG()));
        if (landing == null || isDangerous(b.world.getBlockState(landing.down()))) {
            return false;
        }
        Vec3d center = new Vec3d(landing.getX() + 0.5, landing.getY() + 0.5, landing.getZ() + 0.5);
        if (!hasLineOfSight(b, center) && !hasLineOfSight(b, center.add(0, REQUIRED_AIR_BLOCKS_ABOVE, 0))) {
            return false;
        }
        b.jumpTarget = landing;
        return true;
    }

    /** Four blocks of air (or water) above the breeze's block. */
    static boolean canJumpFromCurrentPosition(EntityBreeze b) {
        BlockPos at = new BlockPos(b);
        for (int i = 1; i <= REQUIRED_AIR_BLOCKS_ABOVE; i++) {
            BlockPos p = at.up(i);
            if (!b.world.isAirBlock(p) && b.world.getBlockState(p).getMaterial() != Material.WATER) {
                return false;
            }
        }
        return true;
    }

    /** {@code EntityType.isBlockDangerous} for a mob that fire hurts. */
    static boolean isDangerous(IBlockState state) {
        Block b = state.getBlock();
        return b == Blocks.MAGMA || b == Blocks.FIRE || b == Blocks.LAVA || b == Blocks.FLOWING_LAVA || b == Blocks.CACTUS;
    }

    /** {@code BreezeUtil.randomPointBehindTarget}: 4-8 blocks behind where the target's head faces. */
    static Vec3d randomPointBehindTarget(EntityLivingBase target, Random random) {
        float yaw = target.rotationYawHead + 180.0F + (float) random.nextGaussian() * 90.0F / 2.0F;
        float dist = 4.0F + random.nextFloat() * (8.0F - 4.0F);
        return target.getPositionVector().add(Vec3d.fromPitchYaw(0.0F, yaw).scale(dist));
    }

    /** {@code Slide.randomPointInMiddleCircle}. */
    static Vec3d randomPointInMiddleCircle(EntityBreeze b, EntityLivingBase target) {
        Vec3d to = target.getPositionVector().subtract(b.getPositionVector());
        double len = to.length() - (8.0 + b.getRNG().nextDouble() * (4.0 - 8.0));
        Vec3d n = to.normalize();
        return b.getPositionVector().add(n.x * len, n.y * len, n.z * len);
    }

    /** {@code BreezeUtil.hasLineOfSight}: from the breeze's feet, within 50 blocks. */
    static boolean hasLineOfSight(EntityBreeze b, Vec3d to) {
        Vec3d from = b.getPositionVector();
        if (to.distanceTo(from) > Math.max(50.0, b.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue())) {
            return false;
        }
        RayTraceResult hit = b.world.rayTraceBlocks(from, to, false, true, false);
        return hit == null || hit.typeOfHit == RayTraceResult.Type.MISS;
    }

    /** {@code LongJump.snapToSurface}: down 10 blocks to a surface, else up 10; the block above where it hits. */
    @Nullable
    static BlockPos snapToSurface(EntityBreeze b, Vec3d p) {
        RayTraceResult down = b.world.rayTraceBlocks(p, p.add(0, -10, 0), false, true, false);
        if (down != null && down.typeOfHit == RayTraceResult.Type.BLOCK) {
            return new BlockPos(down.hitVec).up();
        }
        RayTraceResult up = b.world.rayTraceBlocks(p, p.add(0, 10, 0), false, true, false);
        return up != null && up.typeOfHit == RayTraceResult.Type.BLOCK ? new BlockPos(up.hitVec).up() : null;
    }

    /** {@code LongJump.calculateOptimalJumpVector}: the first of the shuffled angles that reaches the spot. */
    @Nullable
    static Vec3d optimalJumpVector(EntityBreeze b, Random random, Vec3d to) {
        List<Integer> angles = new ArrayList<Integer>(ALLOWED_ANGLES);
        Collections.shuffle(angles, random);
        float max = MAX_JUMP_VELOCITY_MULTIPLIER * (float) b.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        for (int angle : angles) {
            Vec3d v = jumpVectorForAngle(b.getPositionVector(), to, max, angle);
            if (v != null) {
                if (b.isPotionActive(MobEffects.JUMP_BOOST)) {
                    double boost = v.normalize().y * 0.1F * (b.getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1);
                    return v.add(0.0, boost, 0.0);
                }
                return v;
            }
        }
        return null;
    }

    /** {@code LongJumpUtil.calculateJumpVectorForAngle} without the path check (the breeze does not ask for one). */
    @Nullable
    public static Vec3d jumpVectorForAngle(Vec3d from, Vec3d to, float maxVelocity, int angleDegrees) {
        Vec3d flat = new Vec3d(to.x - from.x, 0.0, to.z - from.z).normalize().scale(0.5);
        Vec3d d = to.subtract(flat).subtract(from);
        float a = (float) angleDegrees * (float) Math.PI / 180.0F;
        double heading = Math.atan2(d.z, d.x);
        double horizontalSq = d.x * d.x + d.z * d.z;
        double horizontal = Math.sqrt(horizontalSq);
        double vSq = horizontalSq * GRAVITY / (horizontal * Math.sin(2.0F * a) - 2.0 * d.y * Math.pow(Math.cos(a), 2.0));
        if (vSq < 0.0) {
            return null;
        }
        double v = Math.sqrt(vSq);
        if (v > maxVelocity) {
            return null;
        }
        double across = v * Math.cos(a);
        double up = v * Math.sin(a);
        return new Vec3d(across * Math.cos(heading), up, across * Math.sin(heading)).scale(0.95F);
    }
}
