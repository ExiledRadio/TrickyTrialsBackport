package com.exiledradio.trickytrialsbackport.breeze;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

/** The breeze's three goals. Each reads timers kept on the breeze's own tick and only sets them when it starts. */
final class BreezeAI {

    private BreezeAI() {
    }

    /** Inhale, shoot one breeze wind charge, recover. Holds movement and looking while it runs. */
    static final class Shoot extends EntityAIBase {
        private final EntityBreeze breeze;
        private int timer;

        Shoot(EntityBreeze breeze) {
            this.breeze = breeze;
            setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (this.breeze.shootCooldown > 0 || !this.breeze.hasTarget() || !this.breeze.onGround && this.breeze.motionY > 0.1) {
                return false;
            }
            EntityLivingBase target = this.breeze.getAttackTarget();
            return this.breeze.getDistanceSq(target) <= EntityBreeze.SHOOT_RANGE * EntityBreeze.SHOOT_RANGE
                    && this.breeze.getEntitySenses().canSee(target);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.timer < EntityBreeze.INHALE_TICKS + EntityBreeze.SHOOT_RECOVER_TICKS && this.breeze.hasTarget();
        }

        @Override
        public void startExecuting() {
            this.timer = 0;
            this.breeze.getNavigator().clearPath();
            this.breeze.setPose(EntityBreeze.Pose.INHALING);
            this.breeze.playSound(Content.sound("entity.breeze.inhale"), 1.0F, 1.0F);
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = this.breeze.getAttackTarget();
            if (target == null) {
                return;
            }
            this.breeze.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            this.timer++;
            if (this.timer == EntityBreeze.INHALE_TICKS) {
                shoot(target);
                this.breeze.setPose(EntityBreeze.Pose.SHOOTING);
            }
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

        @Override
        public void resetTask() {
            this.breeze.setPose(EntityBreeze.Pose.IDLE);
            this.breeze.shootCooldown = 20 + this.breeze.getRNG().nextInt(21);
        }
    }

    /**
     * A long jump to a safe spot 3-8 blocks from the target, when the target is far, close or hidden.
     * The arc is aimed from vanilla's gravity (0.08) and air drag (0.98 up, 0.91 across).
     */
    static final class LongJump extends EntityAIBase {
        private final EntityBreeze breeze;
        @Nullable private BlockPos landing;
        private int ticks;

        LongJump(EntityBreeze breeze) {
            this.breeze = breeze;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (!this.breeze.onGround || this.breeze.jumpCooldown > 0 || !this.breeze.hasTarget()) {
                return false;
            }
            EntityLivingBase target = this.breeze.getAttackTarget();
            double d = this.breeze.getDistance(target);
            if (d >= 3.0 && d <= 8.0 && this.breeze.getEntitySenses().canSee(target)) {
                return false;
            }
            this.landing = findLanding(target);
            return this.landing != null;
        }

        @Nullable
        private BlockPos findLanding(EntityLivingBase target) {
            for (int i = 0; i < 20; i++) {
                double angle = this.breeze.getRNG().nextDouble() * Math.PI * 2;
                double r = 3.0 + this.breeze.getRNG().nextDouble() * 5.0;
                int x = MathHelper.floor(target.posX + Math.cos(angle) * r);
                int z = MathHelper.floor(target.posZ + Math.sin(angle) * r);
                for (int dy = 4; dy >= -4; dy--) {
                    BlockPos feet = new BlockPos(x, MathHelper.floor(target.posY) + dy, z);
                    if (this.breeze.isSafeLanding(feet) && clearPath(feet)) {
                        return feet;
                    }
                }
            }
            return null;
        }

        /** Nothing solid between the breeze's head and the landing spot's head height. */
        private boolean clearPath(BlockPos feet) {
            Vec3d from = new Vec3d(this.breeze.posX, this.breeze.posY + this.breeze.height, this.breeze.posZ);
            Vec3d to = new Vec3d(feet.getX() + 0.5, feet.getY() + this.breeze.height, feet.getZ() + 0.5);
            RayTraceResult hit = this.breeze.world.rayTraceBlocks(from, to, false, true, false);
            return hit == null || hit.typeOfHit == RayTraceResult.Type.MISS;
        }

        @Override
        public void startExecuting() {
            BlockPos to = this.landing;
            double dx = to.getX() + 0.5 - this.breeze.posX;
            double dz = to.getZ() + 0.5 - this.breeze.posZ;
            double dy = to.getY() - this.breeze.posY;
            double across = Math.sqrt(dx * dx + dz * dz);
            double up = 0.9;
            // Flight time from the vertical motion alone, then the horizontal speed that covers the distance in it.
            double ticks = flightTicks(up, dy);
            double speed = Math.min(1.5, across * 0.09 / (1.0 - Math.pow(0.91, ticks)));
            this.breeze.motionX = across == 0 ? 0 : dx / across * speed;
            this.breeze.motionZ = across == 0 ? 0 : dz / across * speed;
            this.breeze.motionY = up;
            this.breeze.isAirBorne = true;
            this.breeze.rotationYaw = this.breeze.yawTo(to.getX() + 0.5, to.getZ() + 0.5);
            this.breeze.renderYawOffset = this.breeze.rotationYaw;
            this.breeze.setPose(EntityBreeze.Pose.LONG_JUMPING);
            this.breeze.playSound(Content.sound("entity.breeze.jump"), 1.0F, 1.0F);
            this.breeze.jumpCooldown = 40 + this.breeze.getRNG().nextInt(61);
            this.ticks = 0;
        }

        /** Ticks until a jump straight up at {@code up} comes back down to {@code dy}, with vanilla's drag. */
        static double flightTicks(double up, double dy) {
            double y = 0;
            double v = up;
            for (int t = 1; t < 200; t++) {
                y += v;
                v = (v - 0.08) * 0.98;
                if (v < 0 && y <= dy) {
                    return t;
                }
            }
            return 200;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.ticks < 100 && (this.ticks < 3 || !this.breeze.onGround);
        }

        @Override
        public void updateTask() {
            this.ticks++;
        }

        @Override
        public void resetTask() {
            this.breeze.setPose(EntityBreeze.Pose.IDLE);
            this.breeze.playSound(Content.sound("entity.breeze.land"), 1.0F, 1.0F);
            this.landing = null;
        }
    }

    /** Slides to a spot 4-8 blocks from its target now and then, so it never stands still in a fight. */
    static final class Slide extends EntityAIBase {
        private final EntityBreeze breeze;
        private double x;
        private double y;
        private double z;

        Slide(EntityBreeze breeze) {
            this.breeze = breeze;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (!this.breeze.onGround || !this.breeze.hasTarget() || this.breeze.getRNG().nextInt(20) != 0) {
                return false;
            }
            EntityLivingBase target = this.breeze.getAttackTarget();
            double angle = this.breeze.getRNG().nextDouble() * Math.PI * 2;
            double r = 4.0 + this.breeze.getRNG().nextDouble() * 4.0;
            this.x = target.posX + Math.cos(angle) * r;
            this.y = target.posY;
            this.z = target.posZ + Math.sin(angle) * r;
            return true;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.breeze.getNavigator().noPath() && this.breeze.hasTarget();
        }

        @Override
        public void startExecuting() {
            if (this.breeze.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, 0.6D)) {
                this.breeze.setPose(EntityBreeze.Pose.SLIDING);
                this.breeze.playSound(Content.sound("entity.breeze.slide"), 1.0F, 1.0F);
            }
        }

        @Override
        public void resetTask() {
            if (this.breeze.pose() == EntityBreeze.Pose.SLIDING) {
                this.breeze.setPose(EntityBreeze.Pose.IDLE);
            }
        }
    }
}
