package com.exiledradio.trickytrialsbackport.nautilus;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 1.21.11's {@code NautilusAi} and {@code ZombieNautilusAi} as 1.12.2 goals, in the order of their activities: the
 * charge (FIGHT) above everything, then panic (core), breeding, temptation, and swimming about (IDLE). The brain's
 * memories that matter (who it is angry at and until when, the attack-target, charge and temptation cooldowns, panic)
 * are fields of the nautilus, updated each AI tick by {@link #updateMemories}.
 */
public final class NautilusAi {

    public static final float SPEED_WHEN_IDLING = 1.0F;
    public static final float SPEED_WHEN_TEMPTED = 1.3F;
    public static final float ZOMBIE_SPEED_WHEN_TEMPTED = 0.9F;
    public static final float SPEED_WHEN_MAKING_LOVE = 0.4F;
    public static final float SPEED_WHEN_PANICKING = 1.6F;
    public static final float SPEED_WHEN_ATTACKING = 0.6F;
    public static final float ZOMBIE_SPEED_WHEN_ATTACKING = 0.5F;
    public static final float ATTACK_KNOCKBACK_FORCE = 2.0F;
    public static final int TIME_BETWEEN_ATTACKS = 80;
    public static final double MAX_CHARGE_DISTANCE = 12.0D;
    public static final double MAX_TARGET_DETECTION_DISTANCE = 11.0D;
    /** A Behavior's default duration. */
    public static final int CHARGE_DURATION = 60;
    public static final int TEMPT_RANGE = 10;
    public static final int TEMPTATION_COOLDOWN = 100;
    /** NEAREST_VISIBLE_LIVING_ENTITIES' range. */
    public static final double SENSOR_RANGE = 16.0D;
    /** 1.21.11's {@code #nautilus_hostiles}: the pufferfish (Update Aquatic's mob). */
    public static final ResourceLocation PUFFERFISH = new ResourceLocation("updateaquaticbackport", "pufferfish");

    private NautilusAi() {
    }

    /** {@code TIME_BETWEEN_NON_PLAYER_ATTACKS}: 2400-3600 ticks. */
    static int nonPlayerAttackCooldown(Random rand) {
        return 2400 + rand.nextInt(1201);
    }

    /** {@code setAngerTarget}: angry for 400 ticks at anything it may attack. */
    static void setAngerTarget(EntityAbstractNautilus n, EntityLivingBase attacker) {
        if (isAttackableIgnoringSight(n, attacker)) {
            n.angryAt = attacker.getUniqueID();
            n.angryUntil = n.world.getTotalWorldTime() + EntityAbstractNautilus.ANGER_DURATION;
        }
    }

    /** {@code Sensor.isEntityAttackableIgnoringLineOfSight}: alive, not itself, not a creative or spectator player. */
    static boolean isAttackableIgnoringSight(EntityAbstractNautilus n, EntityLivingBase e) {
        if (e == n || !e.isEntityAlive() || !e.attackable()) {
            return false;
        }
        if (e instanceof EntityPlayer && (((EntityPlayer) e).isCreative() || ((EntityPlayer) e).isSpectator())) {
            return false;
        }
        if (e instanceof EntityPlayer && n.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
            return false;
        }
        return !n.isOnSameTeam(e);
    }

    /** {@code ATTACK_TARGET_CONDITIONS}: what a charge may hit. */
    static boolean chargeHits(EntityAbstractNautilus n, EntityLivingBase e) {
        if (e instanceof EntityArmorStand && !n.world.getGameRules().getBoolean("mobGriefing")) {
            return false;
        }
        return isAttackableIgnoringSight(n, e) && n.world.getWorldBorder().contains(e.getEntityBoundingBox());
    }

    static boolean isHostileTarget(EntityLivingBase e) {
        return e.isInWater() && PUFFERFISH.equals(EntityList.getKey(e));
    }

    @Nullable
    static EntityLivingBase findByUuid(EntityAbstractNautilus n, @Nullable UUID id) {
        if (id == null) {
            return null;
        }
        List<EntityLivingBase> near = n.world.getEntitiesWithinAABB(EntityLivingBase.class, n.getEntityBoundingBox().grow(48.0D),
                e -> e != null && id.equals(e.getUniqueID()));
        return near.isEmpty() ? null : near.get(0);
    }

    /**
     * The brain's memory upkeep and {@code StartAttacking}: forget anger once it expires; count the cooldowns down; with
     * no attack target, take the one it is angry at (if it is in water), else, when the hunting cooldown is over, roll a
     * new one and half the time go for the nearest visible pufferfish in water. Only an untamed adult in water that is
     * not in love hunts.
     */
    static void updateMemories(EntityAbstractNautilus n) {
        long now = n.world.getTotalWorldTime();
        if (n.angryAt != null && now >= n.angryUntil) {
            n.angryAt = null;
        }
        if (n.attackTargetCooldown > 0) {
            n.attackTargetCooldown--;
        }
        if (n.chargeCooldown > 0) {
            n.chargeCooldown--;
        }
        EntityLivingBase target = n.getAttackTarget();
        if (target != null && !target.isEntityAlive()) {
            n.setAttackTarget(null);
        }
        if (n.getAttackTarget() == null && !n.charging()) {
            EntityLivingBase found = findNearestValidAttackTarget(n);
            if (found != null) {
                n.setAttackTarget(found);
            }
        }
    }

    @Nullable
    static EntityLivingBase findNearestValidAttackTarget(EntityAbstractNautilus n) {
        if (n.isInLove() || !n.isInWater() || n.isChild() || n.isTamed()) {
            return null;
        }
        EntityLivingBase angry = findByUuid(n, n.angryAt);
        if (angry != null && angry.isInWater() && isAttackableIgnoringSight(n, angry)) {
            return angry;
        }
        if (n.attackTargetCooldown > 0) {
            return null;
        }
        n.attackTargetCooldown = nonPlayerAttackCooldown(n.getRNG());
        if (n.getRNG().nextFloat() < 0.5F) {
            return null;
        }
        EntityLivingBase best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityLivingBase e : n.world.getEntitiesWithinAABB(EntityLivingBase.class, n.getEntityBoundingBox().grow(SENSOR_RANGE),
                e -> e != null && e != n && e.isEntityAlive())) {
            double d = n.getDistanceSq(e);
            if (d <= SENSOR_RANGE * SENSOR_RANGE && d < bestDist && isHostileTarget(e) && n.canEntityBeSeen(e)) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }

    /** {@code BehaviorUtils.getRandomSwimmablePos}: up to 10 tries at a water block within the ranges (and home). */
    @Nullable
    static Vec3d randomSwimmablePos(EntityAbstractNautilus n, int xz, int y) {
        Random r = n.getRNG();
        BlockPos at = new BlockPos(n);
        for (int i = 0; i < 10; i++) {
            BlockPos p = at.add(r.nextInt(2 * xz + 1) - xz, r.nextInt(2 * y + 1) - y, r.nextInt(2 * xz + 1) - xz);
            if (n.world.getBlockState(p).getMaterial() == Material.WATER && n.isWithinHomeDistanceFromPosition(p)) {
                return new Vec3d(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------
    // Controls
    // ------------------------------------------------------------------------------------------

    /** 1.21's {@code SmoothSwimmingMoveControl}. */
    public static final class SwimMoveHelper extends EntityMoveHelper {
        private final int maxTurnX;
        private final int maxTurnY;
        private final float inWaterSpeedModifier;
        private final float outsideWaterSpeedModifier;
        private final boolean applyGravity;

        public SwimMoveHelper(EntityLiving mob, int maxTurnX, int maxTurnY, float inWater, float outside, boolean applyGravity) {
            super(mob);
            this.maxTurnX = maxTurnX;
            this.maxTurnY = maxTurnY;
            this.inWaterSpeedModifier = inWater;
            this.outsideWaterSpeedModifier = outside;
            this.applyGravity = applyGravity;
        }

        @Override
        public void onUpdateMoveHelper() {
            if (this.applyGravity && this.entity.isInWater()) {
                this.entity.motionY += 0.005D;
            }
            if (this.action == Action.MOVE_TO && !this.entity.getNavigator().noPath()) {
                double dx = this.posX - this.entity.posX;
                double dy = this.posY - this.entity.posY;
                double dz = this.posZ - this.entity.posZ;
                double d = dx * dx + dy * dy + dz * dz;
                if (d < 2.5000003E-7D) {
                    this.entity.setMoveForward(0.0F);
                } else {
                    float yaw = (float) (MathHelper.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
                    this.entity.rotationYaw = this.limitAngle(this.entity.rotationYaw, yaw, this.maxTurnY);
                    this.entity.renderYawOffset = this.entity.rotationYaw;
                    this.entity.rotationYawHead = this.entity.rotationYaw;
                    float speed = (float) (this.speed * this.entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                    if (this.entity.isInWater()) {
                        this.entity.setAIMoveSpeed(speed * this.inWaterSpeedModifier);
                        double h = Math.sqrt(dx * dx + dz * dz);
                        if (Math.abs(dy) > 1.0E-5D || Math.abs(h) > 1.0E-5D) {
                            float pitch = -((float) (MathHelper.atan2(dy, h) * 180.0D / Math.PI));
                            pitch = MathHelper.clamp(MathHelper.wrapDegrees(pitch), -this.maxTurnX, this.maxTurnX);
                            this.entity.rotationPitch = rotateTowards(this.entity.rotationPitch, pitch, 5.0F);
                        }
                        float cos = MathHelper.cos(this.entity.rotationPitch * 0.017453292F);
                        float sin = MathHelper.sin(this.entity.rotationPitch * 0.017453292F);
                        this.entity.moveForward = cos * speed;
                        this.entity.moveVertical = -sin * speed;
                    } else {
                        float off = Math.abs(MathHelper.wrapDegrees(this.entity.rotationYaw - yaw));
                        this.entity.setAIMoveSpeed(speed * this.outsideWaterSpeedModifier * turningSpeedFactor(off));
                    }
                }
            } else {
                this.entity.setAIMoveSpeed(0.0F);
                this.entity.setMoveStrafing(0.0F);
                this.entity.setMoveVertical(0.0F);
                this.entity.setMoveForward(0.0F);
            }
        }

        private static float turningSpeedFactor(float off) {
            return 1.0F - MathHelper.clamp((off - 10.0F) / 50.0F, 0.0F, 1.0F);
        }
    }

    /** 1.21's {@code Mth.approachDegrees}-style {@code rotateTowards}. */
    static float rotateTowards(float from, float to, float max) {
        float delta = MathHelper.wrapDegrees(to - from);
        return from + MathHelper.clamp(delta, -max, max);
    }

    /** 1.21's {@code SmoothSwimmingLookControl}. */
    public static final class SwimLookHelper extends EntityLookHelper {
        private final EntityLiving mob;
        private final int maxYRotFromCenter;
        private int lookAtCooldown;
        private double x;
        private double y;
        private double z;
        private float yMaxRotSpeed;
        private float xMaxRotAngle;

        public SwimLookHelper(EntityLiving mob, int maxYRotFromCenter) {
            super(mob);
            this.mob = mob;
            this.maxYRotFromCenter = maxYRotFromCenter;
        }

        @Override
        public void setLookPositionWithEntity(Entity e, float deltaYaw, float deltaPitch) {
            double eyes = e instanceof EntityLivingBase ? e.posY + e.getEyeHeight() : (e.getEntityBoundingBox().minY + e.getEntityBoundingBox().maxY) / 2.0D;
            this.setLookPosition(e.posX, eyes, e.posZ, deltaYaw, deltaPitch);
        }

        @Override
        public void setLookPosition(double x, double y, double z, float deltaYaw, float deltaPitch) {
            super.setLookPosition(x, y, z, deltaYaw, deltaPitch);
            this.x = x;
            this.y = y;
            this.z = z;
            this.yMaxRotSpeed = deltaYaw;
            this.xMaxRotAngle = deltaPitch;
            this.lookAtCooldown = 2;
        }

        @Override
        public void onUpdateLook() {
            if (this.lookAtCooldown > 0) {
                this.lookAtCooldown--;
                double dx = this.x - this.mob.posX;
                double dy = this.y - (this.mob.posY + this.mob.getEyeHeight());
                double dz = this.z - this.mob.posZ;
                double h = Math.sqrt(dx * dx + dz * dz);
                if (Math.abs(dz) > 1.0E-5D || Math.abs(dx) > 1.0E-5D) {
                    float yaw = (float) (MathHelper.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
                    this.mob.rotationYawHead = rotateTowards(this.mob.rotationYawHead, yaw + 20.0F, this.yMaxRotSpeed);
                }
                if (Math.abs(dy) > 1.0E-5D || Math.abs(h) > 1.0E-5D) {
                    float pitch = (float) -(MathHelper.atan2(dy, h) * 180.0D / Math.PI);
                    this.mob.rotationPitch = rotateTowards(this.mob.rotationPitch, pitch + 10.0F, this.xMaxRotAngle);
                }
            } else {
                if (this.mob.getNavigator().noPath()) {
                    this.mob.rotationPitch = rotateTowards(this.mob.rotationPitch, 0.0F, 5.0F);
                }
                this.mob.rotationYawHead = rotateTowards(this.mob.rotationYawHead, this.mob.renderYawOffset, this.yMaxRotSpeed);
            }
            float off = MathHelper.wrapDegrees(this.mob.rotationYawHead - this.mob.renderYawOffset);
            if (off < -this.maxYRotFromCenter) {
                this.mob.renderYawOffset -= 4.0F;
            } else if (off > this.maxYRotFromCenter) {
                this.mob.renderYawOffset += 4.0F;
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Goals
    // ------------------------------------------------------------------------------------------

    /** 1.21's {@code ChargeAttack}: a straight dash at the attack target, hitting the first thing it touches. */
    public static final class Charge extends EntityAIBase {
        private final EntityAbstractNautilus n;
        private final float speed;
        private Vec3d velocity = Vec3d.ZERO;
        private Vec3d start = Vec3d.ZERO;
        private int ticks;
        boolean running;

        public Charge(EntityAbstractNautilus n, float speed) {
            this.n = n;
            this.speed = speed;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            return this.n.getAttackTarget() != null && this.n.chargeCooldown <= 0 && !this.n.isInLove() && !this.n.tempted();
        }

        @Override
        public boolean shouldContinueExecuting() {
            EntityLivingBase target = this.n.getAttackTarget();
            if (target == null || this.ticks >= CHARGE_DURATION || this.n.isTamed()) {
                return false;
            }
            if (this.n.getPositionVector().squareDistanceTo(this.start) >= MAX_CHARGE_DISTANCE * MAX_CHARGE_DISTANCE) {
                return false;
            }
            if (target.getPositionVector().squareDistanceTo(this.n.getPositionVector()) >= MAX_TARGET_DETECTION_DISTANCE * MAX_TARGET_DETECTION_DISTANCE) {
                return false;
            }
            return this.n.canEntityBeSeen(target) && this.n.chargeCooldown <= 0;
        }

        @Override
        public void startExecuting() {
            this.running = true;
            this.ticks = 0;
            this.start = this.n.getPositionVector();
            EntityLivingBase target = this.n.getAttackTarget();
            this.velocity = target.getPositionVector().subtract(this.start).normalize().scale(this.speed);
            this.n.getNavigator().clearPath();
            if (this.shouldContinueExecuting()) {
                this.n.playSound(this.n.getDashSound(), 1.0F, 1.0F);
            }
        }

        @Override
        public void updateTask() {
            this.ticks++;
            EntityLivingBase target = this.n.getAttackTarget();
            if (target == null) {
                return;
            }
            this.n.faceEntity(target, 360.0F, 360.0F);
            this.n.motionX = this.velocity.x;
            this.n.motionY = this.velocity.y;
            this.n.motionZ = this.velocity.z;
            List<EntityLivingBase> hit = this.n.world.getEntitiesWithinAABB(EntityLivingBase.class, this.n.getEntityBoundingBox(),
                    e -> e != null && e != this.n && chargeHits(this.n, e));
            if (!hit.isEmpty()) {
                EntityLivingBase victim = hit.get(0);
                if (this.n.isPassenger(victim)) {
                    return;
                }
                float damage = (float) this.n.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
                if (victim.attackEntityFrom(DamageSource.causeMobDamage(this.n), damage)) {
                    net.minecraft.enchantment.EnchantmentHelper.applyThornEnchantments(victim, this.n);
                    net.minecraft.enchantment.EnchantmentHelper.applyArthropodEnchantments(this.n, victim);
                }
                knockBack(this.n, victim, this.speed);
                this.ticks = CHARGE_DURATION;
            }
        }

        @Override
        public void resetTask() {
            this.running = false;
            this.n.chargeCooldown = TIME_BETWEEN_ATTACKS;
            this.n.setAttackTarget(null);
        }
    }

    /** {@code ChargeAttack.dealKnockBack} and {@code causeExtraKnockback}. */
    static void knockBack(EntityAbstractNautilus n, EntityLivingBase victim, float speed) {
        int fast = n.isPotionActive(MobEffects.SPEED) ? n.getActivePotionEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
        int slow = n.isPotionActive(MobEffects.SLOWNESS) ? n.getActivePotionEffect(MobEffects.SLOWNESS).getAmplifier() + 1 : 0;
        float extra = 0.25F * (fast - slow);
        float strength = MathHelper.clamp(speed * (float) n.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue(), 0.2F, 2.0F) + extra;
        strength *= ATTACK_KNOCKBACK_FORCE;
        if (strength > 0.0F) {
            victim.knockBack(n, strength, MathHelper.sin(n.rotationYaw * 0.017453292F), -MathHelper.cos(n.rotationYaw * 0.017453292F));
            n.motionX *= 0.6D;
            n.motionZ *= 0.6D;
        }
    }

    /** 1.21's {@code AnimalPanic(1.6)}: after a hurt, 100-120 ticks of swimming to spots within 5 x 4. */
    public static final class Panic extends EntityAIBase {
        private final EntityAbstractNautilus n;

        public Panic(EntityAbstractNautilus n) {
            this.n = n;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return this.n.panicking || this.n.isBurning();
        }

        @Override
        public void startExecuting() {
            this.n.panicTicks = 100 + this.n.getRNG().nextInt(21);
            this.n.getNavigator().clearPath();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.n.panicTicks > 0;
        }

        @Override
        public void updateTask() {
            this.n.panicTicks--;
            if (this.n.getNavigator().noPath()) {
                Vec3d to = randomSwimmablePos(this.n, 5, 4);
                if (to != null) {
                    this.n.getNavigator().tryMoveToXYZ(to.x, to.y, to.z, SPEED_WHEN_PANICKING);
                }
            }
        }

        @Override
        public void resetTask() {
            this.n.panicking = false;
            this.n.panicTicks = 0;
        }
    }

    /** 1.21's {@code FollowTemptation}: a player within 10 holding nautilus food, followed to 3.5 (2.5 for a baby). */
    public static final class Tempt extends EntityAIBase {
        private final EntityAbstractNautilus n;
        private final float speed;
        @Nullable
        EntityPlayer player;
        int cooldown;

        public Tempt(EntityAbstractNautilus n, float speed) {
            this.n = n;
            this.speed = speed;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            this.player = temptingPlayer(this.n);
            return this.player != null && !this.n.isInLove() && !this.n.panicking;
        }

        @Override
        public boolean shouldContinueExecuting() {
            this.player = temptingPlayer(this.n);
            return this.player != null && !this.n.isInLove() && !this.n.panicking;
        }

        @Override
        public void updateTask() {
            if (this.player == null) {
                return;
            }
            this.n.getLookHelper().setLookPositionWithEntity(this.player, 30.0F, 30.0F);
            double close = this.n.isChild() ? 2.5D : 3.5D;
            if (this.n.getDistanceSq(this.player) < close * close) {
                this.n.getNavigator().clearPath();
            } else {
                this.n.getNavigator().tryMoveToEntityLiving(this.player, this.speed);
            }
        }

        @Override
        public void resetTask() {
            this.player = null;
            this.cooldown = TEMPTATION_COOLDOWN;
            this.n.getNavigator().clearPath();
        }
    }

    /** The nautilus temptation sensor: the nearest player within 10 holding nautilus food in either hand. */
    @Nullable
    static EntityPlayer temptingPlayer(EntityAbstractNautilus n) {
        EntityPlayer best = null;
        double bestDist = TEMPT_RANGE * TEMPT_RANGE;
        for (EntityPlayer p : n.world.playerEntities) {
            if (p.isSpectator() || !p.isEntityAlive()) {
                continue;
            }
            double d = n.getDistanceSq(p);
            if (d <= bestDist && (NautilusItems.isFood(p.getHeldItem(EnumHand.MAIN_HAND)) || NautilusItems.isFood(p.getHeldItem(EnumHand.OFF_HAND)))) {
                best = p;
                bestDist = d;
            }
        }
        return best;
    }

    /** 1.21's {@code RandomStroll.swim(1.0)}: with nowhere to go, a water spot within 10 x 7. */
    public static final class Swim extends EntityAIBase {
        private final EntityAbstractNautilus n;

        public Swim(EntityAbstractNautilus n) {
            this.n = n;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return this.n.isInWater() && this.n.getNavigator().noPath() && !this.n.isBeingRidden() && this.n.getRNG().nextInt(5) < 2;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return false;
        }

        @Override
        public void startExecuting() {
            Vec3d to = randomSwimmablePos(this.n, 10, 7);
            if (to != null) {
                this.n.getNavigator().tryMoveToXYZ(to.x, to.y, to.z, SPEED_WHEN_IDLING);
            }
        }
    }

    /**
     * A mob riding a zombie nautilus steers it in 1.21.11 (the rider's navigation is its mount's): toward the rider's
     * attack target.
     */
    public static final class FollowRiderTarget extends EntityAIBase {
        private final EntityAbstractNautilus n;

        public FollowRiderTarget(EntityAbstractNautilus n) {
            this.n = n;
            this.setMutexBits(1);
        }

        @Nullable
        private EntityLivingBase target() {
            List<Entity> passengers = this.n.getPassengers();
            if (passengers.isEmpty() || !(passengers.get(0) instanceof EntityLiving)) {
                return null;
            }
            EntityLivingBase t = ((EntityLiving) passengers.get(0)).getAttackTarget();
            return t != null && t.isEntityAlive() ? t : null;
        }

        @Override
        public boolean shouldExecute() {
            return this.target() != null;
        }

        @Override
        public void updateTask() {
            EntityLivingBase t = this.target();
            if (t != null && (this.n.getNavigator().noPath() || this.n.ticksExisted % 10 == 0)) {
                this.n.getNavigator().tryMoveToEntityLiving(t, SPEED_WHEN_IDLING);
            }
        }
    }
}
