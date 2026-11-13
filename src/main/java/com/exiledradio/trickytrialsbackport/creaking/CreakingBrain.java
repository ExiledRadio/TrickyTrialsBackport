package com.exiledradio.trickytrialsbackport.creaking;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 1.21.11's {@code CreakingAi}, as a small brain of its own: two sensors and the core, idle and fight
 * activities, with the memories they share as fields. Ticked by {@link EntityCreaking#updateAITasks()}, so it
 * never touches {@code tasks}.
 *
 * <ul>
 *   <li>Sensors, each every 20 ticks from a random start: {@code nearest_players} (players not in spectator mode
 *       within the follow range, 32, nearest first; also the visible and the attackable ones) and
 *       {@code nearest_living_entities} (living things within 16, and the visible ones).</li>
 *   <li>Core: swim (0.8 a tick, while it can move); look at the look target (45-90 ticks); walk to the walk
 *       target.</li>
 *   <li>Idle: an active creaking targets the nearest attackable player; look at something within 8 blocks
 *       every 30-60 ticks; one of: stroll at 0.3 (weight 2), walk to the look target at 0.3 (2), wait 30-60
 *       ticks (1).</li>
 *   <li>Fight (with a target, and only while it can move): walk at full speed to a target out of reach, attack
 *       one in reach every 40 ticks, and drop a target that is no longer an attackable player nearby.</li>
 * </ul>
 */
final class CreakingBrain {

    private static final int SCAN_RATE = 20;
    private static final double LIVING_RANGE = 16.0D;
    private static final double LOOK_RANGE_SQ = 64.0D;
    /** {@code Mob.DEFAULT_ATTACK_REACH}: sqrt(2.04) - 0.6. */
    static final double ATTACK_REACH = Math.sqrt(2.04D) - 0.6D;
    private static final long TIRED_OF_REACHING = 200L;

    private static final Random SENSOR_RANDOM = new Random();

    private final EntityCreaking mob;

    // Sensors.
    private long playerSensorIn = SENSOR_RANDOM.nextInt(SCAN_RATE);
    private long livingSensorIn = SENSOR_RANDOM.nextInt(SCAN_RATE);

    // Memories.
    List<EntityPlayer> nearestPlayers = Collections.emptyList();
    List<EntityPlayer> visibleAttackablePlayers = Collections.emptyList();
    @Nullable
    EntityPlayer nearestVisibleAttackablePlayer;
    List<EntityLivingBase> visibleLiving = Collections.emptyList();
    @Nullable
    EntityLivingBase attackTarget;
    long attackCoolingDownUntil = Long.MIN_VALUE;
    @Nullable
    Entity lookTarget;
    @Nullable
    Vec3d lookPos;
    @Nullable
    Vec3d walkPos;
    @Nullable
    Entity walkEntity;
    float walkSpeed;
    int walkCloseEnough;
    long cantReachSince = Long.MIN_VALUE;

    // Running behaviors.
    private int lookTicks = -1;
    private int lookSometimesIn = -1;
    /** RunOne's running child: 0 none, 3 waiting (the two others are one-shots). */
    private int waitTicks;
    private boolean fighting;
    @Nullable
    private Path path;
    @Nullable
    private Vec3d pathTarget;

    CreakingBrain(EntityCreaking mob) {
        this.mob = mob;
    }

    private World world() {
        return this.mob.world;
    }

    private long now() {
        return this.mob.world.getTotalWorldTime();
    }

    // ---------------------------------------------------------------------------------------------
    // Targeting conditions

    /** {@code LivingEntity.canAttack} for a player: not peaceful, not invulnerable, not a spectator, alive. */
    static boolean canAttack(EntityLivingBase mob, EntityPlayer p) {
        return mob.world.getDifficulty() != EnumDifficulty.PEACEFUL && !p.capabilities.disableDamage && !p.isSpectator()
                && p.isEntityAlive();
    }

    /** {@code TargetingConditions} with the follow range; visibility scaling unless it is the attack target. */
    private boolean targetable(EntityLivingBase target, boolean combat) {
        if (target == this.mob || !target.isEntityAlive() || target.world != this.mob.world) {
            return false;
        }
        if (target instanceof EntityPlayer && ((EntityPlayer) target).isSpectator()) {
            return false;
        }
        if (combat) {
            if (!(target instanceof EntityPlayer) ? !this.mob.canAttackClass(target.getClass())
                    : !canAttack(this.mob, (EntityPlayer) target)) {
                return false;
            }
            if (this.mob.isOnSameTeam(target)) {
                return false;
            }
        }
        double range = this.mob.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        if (target != this.attackTarget) {
            range *= visibility(target);
        }
        if (this.mob.getDistanceSq(target) > range * range) {
            return false;
        }
        return this.mob.getEntitySenses().canSee(target);
    }

    /** {@code getVisibilityPercent}: sneaking 0.8; invisible 0.7 times the armor worn (at least 0.1). */
    private static double visibility(EntityLivingBase target) {
        double v = 1.0D;
        if (target.isSneaking()) {
            v *= 0.8D;
        }
        if (target.isInvisible()) {
            float armor = target instanceof EntityPlayer ? ((EntityPlayer) target).getArmorVisibility() : 0.0F;
            v *= 0.7D * Math.max(armor, 0.1F);
        }
        return v;
    }

    // ---------------------------------------------------------------------------------------------
    // The tick

    void tick() {
        sense();
        // Core.
        if (this.mob.canMove() && (this.mob.isInWater() || this.mob.isInLava()) && this.mob.getRNG().nextFloat() < 0.8F) {
            this.mob.getJumpHelper().setJumping();
        }
        if (this.fighting) {
            fight();
        } else {
            idle();
        }
        lookAtTarget();
        moveToTarget();
    }

    /** {@code updateActivity}: fight only with a target and while it can move. */
    void updateActivity() {
        boolean fight = this.mob.canMove() && this.attackTarget != null;
        if (fight != this.fighting) {
            this.fighting = fight;
            this.waitTicks = 0;
            this.lookSometimesIn = -1;
        }
    }

    private void sense() {
        if (--this.playerSensorIn <= 0L) {
            this.playerSensorIn = SCAN_RATE;
            double range = this.mob.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
            List<EntityPlayer> near = new ArrayList<EntityPlayer>();
            for (EntityPlayer p : world().playerEntities) {
                if (!p.isSpectator() && this.mob.getDistanceSq(p) < range * range) {
                    near.add(p);
                }
            }
            near.sort(Comparator.comparingDouble(this.mob::getDistanceSq));
            this.nearestPlayers = near;
            List<EntityPlayer> attackable = new ArrayList<EntityPlayer>();
            for (EntityPlayer p : near) {
                if (targetable(p, false) && targetable(p, true)) {
                    attackable.add(p);
                }
            }
            this.visibleAttackablePlayers = attackable;
            this.nearestVisibleAttackablePlayer = attackable.isEmpty() ? null : attackable.get(0);
        }
        if (--this.livingSensorIn <= 0L) {
            this.livingSensorIn = SCAN_RATE;
            AxisAlignedBB box = this.mob.getEntityBoundingBox().grow(LIVING_RANGE, LIVING_RANGE, LIVING_RANGE);
            List<EntityLivingBase> living = world().getEntitiesWithinAABB(EntityLivingBase.class, box,
                    e -> e != this.mob && e.isEntityAlive());
            living.sort(Comparator.comparingDouble(this.mob::getDistanceSq));
            List<EntityLivingBase> visible = new ArrayList<EntityLivingBase>();
            for (EntityLivingBase e : living) {
                if (targetable(e, false)) {
                    visible.add(e);
                }
            }
            this.visibleLiving = visible;
        }
    }

    private void idle() {
        // StartAttacking: an active creaking takes the nearest attackable player.
        if (this.attackTarget == null && this.mob.isActive() && this.nearestVisibleAttackablePlayer != null) {
            this.attackTarget = this.nearestVisibleAttackablePlayer;
            this.cantReachSince = Long.MIN_VALUE;
        }
        // SetEntityLookTargetSometimes(8, 30-60).
        if (this.lookSometimesIn < 0) {
            this.lookSometimesIn = 30 + this.mob.getRNG().nextInt(31);
        }
        if (--this.lookSometimesIn <= 0) {
            this.lookSometimesIn = 30 + this.mob.getRNG().nextInt(31);
            for (EntityLivingBase e : this.visibleLiving) {
                if (e.getDistanceSq(this.mob) <= LOOK_RANGE_SQ) {
                    this.lookTarget = e;
                    this.lookPos = null;
                    break;
                }
            }
        }
        // RunOne: stroll (2), walk to the look target (2), do nothing (1).
        if (this.waitTicks > 0) {
            this.waitTicks--;
            return;
        }
        Random rand = this.mob.getRNG();
        int[] order = shuffled(rand, new int[]{2, 2, 1});
        for (int child : order) {
            if (child == 0 && this.walkPos == null && this.walkEntity == null) {
                Vec3d to = RandomPositionGenerator.getLandPos(this.mob, 10, 7);
                if (to != null) {
                    walkTo(to, null, EntityCreaking.SPEED_MULTIPLIER_WHEN_IDLING, 0);
                }
                return;
            }
            if (child == 1 && this.walkPos == null && this.walkEntity == null && this.lookTarget != null) {
                walkTo(null, this.lookTarget, EntityCreaking.SPEED_MULTIPLIER_WHEN_IDLING, 3);
                return;
            }
            if (child == 2) {
                this.waitTicks = 30 + rand.nextInt(31);
                return;
            }
        }
    }

    /** {@code ShufflingList.shuffle}: each entry ordered by -random^(1/weight). @return the indices in order */
    private static int[] shuffled(Random rand, int[] weights) {
        final double[] keys = new double[weights.length];
        Integer[] idx = new Integer[weights.length];
        for (int i = 0; i < weights.length; i++) {
            keys[i] = -Math.pow(rand.nextFloat(), 1.0F / weights[i]);
            idx[i] = i;
        }
        java.util.Arrays.sort(idx, Comparator.comparingDouble(i -> keys[i]));
        int[] out = new int[idx.length];
        for (int i = 0; i < idx.length; i++) {
            out[i] = idx[i];
        }
        return out;
    }

    private void fight() {
        EntityLivingBase target = this.attackTarget;
        if (target == null) {
            return;
        }
        // SetWalkTargetFromAttackTargetIfTargetOutOfReach(1.0).
        this.lookTarget = target;
        this.lookPos = null;
        this.lookTicks = Math.max(this.lookTicks, 1);
        boolean visible = this.visibleLiving.contains(target);
        if (visible && inReach(target)) {
            clearWalk();
        } else {
            walkTo(null, target, 1.0F, 0);
        }
        // MeleeAttack(canMove, 40).
        if (this.mob.canMove() && now() >= this.attackCoolingDownUntil && inReach(target) && visible) {
            this.mob.swingArm(EnumHand.MAIN_HAND);
            this.mob.attackEntityAsMob(target);
            this.attackCoolingDownUntil = now() + EntityCreaking.ATTACK_INTERVAL;
        }
        // StopAttackingIfTargetInvalid: dead, gone, not an attackable player nearby, or tired of trying.
        boolean reachable = target instanceof EntityPlayer && this.visibleAttackablePlayers.contains(target);
        boolean tired = this.cantReachSince != Long.MIN_VALUE && now() - this.cantReachSince > TIRED_OF_REACHING;
        if (!target.isEntityAlive() || target.world != this.mob.world || !reachable || tired) {
            this.attackTarget = null;
            this.cantReachSince = Long.MIN_VALUE;
        }
    }

    /** {@code isWithinMeleeAttackRange}: its box grown by the reach across touches the target's. */
    boolean inReach(EntityLivingBase target) {
        return this.mob.getEntityBoundingBox().grow(ATTACK_REACH, 0.0D, ATTACK_REACH).intersects(target.getEntityBoundingBox());
    }

    /** {@code LookAtTargetSink(45, 90)}. */
    private void lookAtTarget() {
        Entity target = this.lookTarget;
        if (target == null && this.lookPos == null) {
            this.lookTicks = -1;
            return;
        }
        if (target != null && (!target.isEntityAlive() || target.world != this.mob.world)) {
            this.lookTarget = null;
            this.lookTicks = -1;
            return;
        }
        if (this.lookTicks < 0) {
            this.lookTicks = 45 + this.mob.getRNG().nextInt(46);
        }
        if (target != null) {
            this.mob.getLookHelper().setLookPosition(target.posX, target.posY + target.getEyeHeight(), target.posZ,
                    this.mob.getHorizontalFaceSpeed(), this.mob.getVerticalFaceSpeed());
        } else {
            this.mob.getLookHelper().setLookPosition(this.lookPos.x, this.lookPos.y, this.lookPos.z,
                    this.mob.getHorizontalFaceSpeed(), this.mob.getVerticalFaceSpeed());
        }
        if (--this.lookTicks <= 0) {
            this.lookTarget = null;
            this.lookPos = null;
            this.lookTicks = -1;
        }
    }

    private void walkTo(@Nullable Vec3d pos, @Nullable Entity entity, float speed, int closeEnough) {
        this.walkPos = pos;
        this.walkEntity = entity;
        this.walkSpeed = speed;
        this.walkCloseEnough = closeEnough;
    }

    private void clearWalk() {
        this.walkPos = null;
        this.walkEntity = null;
        this.path = null;
        this.pathTarget = null;
        this.mob.getNavigator().clearPath();
    }

    /** {@code MoveToTargetSink}: paths to the walk target, repathing when it has moved. */
    private void moveToTarget() {
        Vec3d to = this.walkEntity != null ? this.walkEntity.getPositionVector() : this.walkPos;
        if (to == null) {
            return;
        }
        if (this.walkEntity != null && (!this.walkEntity.isEntityAlive() || this.walkEntity.world != this.mob.world)) {
            clearWalk();
            return;
        }
        double close = this.walkCloseEnough;
        if (this.mob.getDistanceSq(to.x, to.y, to.z) <= Math.max(close * close, 1.0D) && close > 0) {
            clearWalk();
            return;
        }
        if (this.path == null || this.mob.getNavigator().noPath() || this.pathTarget == null || this.pathTarget.squareDistanceTo(to) > close * close + 1.0D) {
            Path p = this.walkEntity != null ? this.mob.getNavigator().getPathToEntityLiving(this.walkEntity)
                    : this.mob.getNavigator().getPathToXYZ(to.x, to.y, to.z);
            this.pathTarget = to;
            if (p == null) {
                if (this.cantReachSince == Long.MIN_VALUE) {
                    this.cantReachSince = now();
                }
                this.walkPos = null;
                this.walkEntity = null;
                this.path = null;
                return;
            }
            this.cantReachSince = Long.MIN_VALUE;
            this.path = p;
            this.mob.getNavigator().setPath(p, this.walkSpeed);
        }
        if (this.mob.getNavigator().noPath() && this.walkEntity == null) {
            this.walkPos = null;
            this.path = null;
        }
    }
}
