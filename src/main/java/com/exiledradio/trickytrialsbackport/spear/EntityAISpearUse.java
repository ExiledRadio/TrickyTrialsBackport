package com.exiledradio.trickytrialsbackport.spear;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

/**
 * 1.21.11's {@code SpearUseGoal}: a mob holding a spear walks up to its target, starts a charge within the approach
 * distance (10 blocks), runs at the target, and once within 2 blocks (or stuck) turns away to a spot 6-7 blocks off;
 * when the charge has run out (the tier's delay and damage window) it backs off 9-11 blocks, for at most 5 seconds,
 * and starts again. Riding, the distances are 2 blocks longer and the vehicle's {@link SpearAttacks#chargeSpeed}
 * scales the pace.
 *
 * <p>1.21.11's goals run every other tick and halve their timers to match; 1.12.2 runs a running task every tick, so
 * the timers here are the real tick counts.
 *
 * <p>Public so that other code can give it to its own mobs: {@code tasks.addTask(1, new EntityAISpearUse(mob, 1, 1, 10, 2))}.
 */
public class EntityAISpearUse extends EntityAIBase {

    private static final int MAX_FLEEING_TIME = 100;

    private final EntityCreature mob;
    private final double chargeSpeed;
    private final double repositionSpeed;
    private final float approachDistanceSq;
    private final float inRangeSq;
    @Nullable
    private State state;

    public EntityAISpearUse(EntityCreature mob, double chargeSpeed, double repositionSpeed, float approachDistance, float inRange) {
        this.mob = mob;
        this.chargeSpeed = chargeSpeed;
        this.repositionSpeed = repositionSpeed;
        this.approachDistanceSq = approachDistance * approachDistance;
        this.inRangeSq = inRange * inRange;
        setMutexBits(3);
    }

    private boolean able() {
        return this.mob.getAttackTarget() != null && this.mob.getAttackTarget().isEntityAlive()
                && this.mob.getHeldItemMainhand().getItem() instanceof ItemSpear;
    }

    @Override
    public boolean shouldExecute() {
        return able() && !this.mob.isHandActive();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.state != null && !this.state.done && able();
    }

    @Override
    public void startExecuting() {
        aggressive(true);
        this.state = new State();
    }

    @Override
    public void resetTask() {
        this.mob.getNavigator().clearPath();
        aggressive(false);
        this.state = null;
        this.mob.resetActiveHand();
    }

    private void aggressive(boolean on) {
        if (this.mob instanceof EntityZombie) {
            ((EntityZombie) this.mob).setArmsRaised(on);
        }
    }

    private int useDuration() {
        return ((ItemSpear) this.mob.getHeldItemMainhand().getItem()).getTier().damageUseDuration();
    }

    @Override
    public void updateTask() {
        State s = this.state;
        EntityLivingBase target = this.mob.getAttackTarget();
        if (s == null || target == null) {
            return;
        }
        double distSq = this.mob.getDistanceSq(target.posX, target.posY, target.posZ);
        float pace = SpearAttacks.chargeSpeed(this.mob);
        int extra = this.mob.isRiding() ? 2 : 0;
        this.mob.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        this.mob.faceEntity(target, 30.0F, 30.0F);
        if (s.engageTime < 0) {
            if (distSq > this.approachDistanceSq) {
                this.mob.getNavigator().tryMoveToEntityLiving(target, pace * this.repositionSpeed);
                return;
            }
            s.engageTime = useDuration();
            this.mob.setActiveHand(EnumHand.MAIN_HAND);
            SpearAttacks.useSound(this.mob, ((ItemSpear) this.mob.getHeldItemMainhand().getItem()).getTier());
        }
        if (s.engageTime > 0 && --s.engageTime == 0) {
            this.mob.resetActiveHand();
            double dist = Math.sqrt(distSq);
            s.away = away(target, Math.max(1.0, 11 + extra - dist), 7);
            s.fleeingTime = 1;
        }
        if (s.fleeingTime > 0 && ++s.fleeingTime > MAX_FLEEING_TIME) {
            s.done = true;
            return;
        }
        if (s.away != null) {
            this.mob.getNavigator().tryMoveToXYZ(s.away.x, s.away.y, s.away.z, pace * this.repositionSpeed);
            if (this.mob.getNavigator().noPath()) {
                if (s.fleeingTime > 0) {
                    s.done = true;
                    return;
                }
                s.away = null;
            }
        } else {
            this.mob.getNavigator().tryMoveToEntityLiving(target, pace * this.chargeSpeed);
            if (distSq < this.inRangeSq || this.mob.getNavigator().noPath()) {
                double dist = Math.sqrt(distSq);
                s.away = away(target, 7 + extra - dist, 7);
            }
        }
    }

    /** 1.21.11's {@code LandRandomPos.getPosAway}; 1.12.2's generator takes whole blocks. */
    @Nullable
    private Vec3d away(EntityLivingBase target, double range, int vertical) {
        int r = Math.max(1, (int) Math.ceil(range));
        return RandomPositionGenerator.findRandomTargetBlockAwayFrom(this.mob, r, vertical, target.getPositionVector());
    }

    private static final class State {
        int engageTime = -1;
        int fleeingTime = -1;
        @Nullable
        Vec3d away;
        boolean done;
    }
}
