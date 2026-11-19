package com.exiledradio.trickytrialsbackport.ghast;

import com.google.common.collect.ImmutableSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * The happy ghast's movement and behavior, 1.21.11's {@code Ghast.GhastMoveControl}, {@code RandomFloatAroundGoal},
 * {@code HappyGhast.HappyGhastLookControl}, {@code HappyGhastFloatGoal}, {@code TemptGoal.ForNonPathfinders},
 * {@code FlyingMoveControl} and the ghastling's brain ({@code HappyGhastAi}), on 1.12.2's goals and helpers.
 */
final class GhastAi {

    private GhastAi() {
    }

    // ------------------------------------------------------------------------------------------
    // Goals
    // ------------------------------------------------------------------------------------------

    /** 1.21's adult goals: float (3), tempt (4), float around (5). */
    static void adultGoals(EntityHappyGhast ghast) {
        clear(ghast);
        ghast.tasks.addTask(3, new FloatGoal(ghast));
        ghast.tasks.addTask(4, new TemptGoal(ghast, 1.0D, 7.0D));
        ghast.tasks.addTask(5, new RandomFloatAroundGoal(ghast, 16));
    }

    /** A ghastling has no goals: its brain runs from {@link Brain} instead. */
    static void babyGoals(EntityHappyGhast ghast) {
        clear(ghast);
        ghast.tasks.addTask(0, new Brain(ghast));
    }

    private static void clear(EntityHappyGhast ghast) {
        for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry e : new java.util.ArrayList<net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry>(ghast.tasks.taskEntries)) {
            ghast.tasks.removeTask(e.action);
        }
    }

    /** What tempts it: snowballs, and harnesses too for an adult without one. */
    static boolean tempts(EntityHappyGhast ghast, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() == Items.SNOWBALL) {
            return true;
        }
        return !ghast.isWearingHarness() && !ghast.isChild() && stack.getItem() instanceof ItemHarness;
    }

    @Nullable
    static EntityPlayer temptingPlayer(EntityHappyGhast ghast) {
        EntityPlayer best = null;
        double bestD = EntityHappyGhast.TEMPT_RANGE * EntityHappyGhast.TEMPT_RANGE;
        for (EntityPlayer p : ghast.world.playerEntities) {
            if (p.isSpectator() || !p.isEntityAlive()) {
                continue;
            }
            double d = ghast.getDistanceSq(p);
            if (d < bestD && (tempts(ghast, p.getHeldItemMainhand()) || tempts(ghast, p.getHeldItemOffhand()))) {
                best = p;
                bestD = d;
            }
        }
        return best;
    }

    /** 1.21's {@code HappyGhastFloatGoal}: jumps out of water and lava, unless on its still timeout. */
    static final class FloatGoal extends EntityAIBase {
        private final EntityHappyGhast ghast;

        FloatGoal(EntityHappyGhast ghast) {
            this.ghast = ghast;
            this.setMutexBits(4);
        }

        @Override
        public boolean shouldExecute() {
            return !this.ghast.isOnStillTimeout() && !this.ghast.isPlayerControlled() && (this.ghast.isInWater() || this.ghast.isInLava());
        }

        @Override
        public void updateTask() {
            if (this.ghast.getRNG().nextFloat() < 0.8F) {
                this.ghast.getJumpHelper().setJumping();
            }
        }
    }

    /** 1.21's {@code TemptGoal.ForNonPathfinders}: flies at a random point on the line to the player's eyes. */
    static final class TemptGoal extends EntityAIBase {
        private final EntityHappyGhast ghast;
        private final double speed;
        private final double stopDistance;
        private EntityPlayer player;
        private long calmUntil;

        TemptGoal(EntityHappyGhast ghast, double speed, double stopDistance) {
            this.ghast = ghast;
            this.speed = speed;
            this.stopDistance = stopDistance;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (this.ghast.ticksExisted < this.calmUntil || this.ghast.isPlayerControlled()) {
                return false;
            }
            this.player = temptingPlayer(this.ghast);
            return this.player != null;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.shouldExecute();
        }

        @Override
        public void resetTask() {
            this.player = null;
            ((MoveHelper) this.ghast.getMoveHelper()).setWait();
            this.calmUntil = this.ghast.ticksExisted + 100;
        }

        @Override
        public void updateTask() {
            this.ghast.getLookHelper().setLookPositionWithEntity(this.player, 30.0F, (float) this.ghast.getVerticalFaceSpeed());
            if (this.ghast.getDistanceSq(this.player) < this.stopDistance * this.stopDistance) {
                ((MoveHelper) this.ghast.getMoveHelper()).setWait();
            } else {
                double f = this.ghast.getRNG().nextDouble();
                double x = this.ghast.posX + (this.player.posX - this.ghast.posX) * f;
                double y = this.ghast.posY + (this.player.posY + this.player.getEyeHeight() - this.ghast.posY) * f;
                double z = this.ghast.posZ + (this.player.posZ - this.ghast.posZ) * f;
                this.ghast.getMoveHelper().setMoveTo(x, y, z, this.speed);
            }
        }
    }

    /** 1.21's {@code Ghast.RandomFloatAroundGoal}: a new spot when there is none, it is reached, or it is 60 away. */
    static final class RandomFloatAroundGoal extends EntityAIBase {
        private final EntityHappyGhast ghast;
        private final int distanceToBlocks;

        RandomFloatAroundGoal(EntityHappyGhast ghast, int distanceToBlocks) {
            this.ghast = ghast;
            this.distanceToBlocks = distanceToBlocks;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (this.ghast.isPlayerControlled()) {
                return false;
            }
            EntityMoveHelper move = this.ghast.getMoveHelper();
            if (!move.isUpdating()) {
                return true;
            }
            double dx = move.getX() - this.ghast.posX;
            double dy = move.getY() - this.ghast.posY;
            double dz = move.getZ() - this.ghast.posZ;
            double d = dx * dx + dy * dy + dz * dz;
            return d < 1.0D || d > 3600.0D;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return false;
        }

        @Override
        public void startExecuting() {
            Vec3d to = suitableFlyToPosition(this.ghast, this.distanceToBlocks);
            this.ghast.getMoveHelper().setMoveTo(to.x, to.y, to.z, 1.0D);
        }
    }

    static Vec3d suitableFlyToPosition(EntityHappyGhast ghast, int distanceToBlocks) {
        World world = ghast.world;
        Random random = ghast.getRNG();
        Vec3d from = new Vec3d(ghast.posX, ghast.posY, ghast.posZ);
        Vec3d to = null;
        for (int i = 0; i < 64; i++) {
            to = randomWithRestriction(ghast, from, random);
            if (to != null && isGoodTarget(world, to, distanceToBlocks)) {
                return to;
            }
        }
        if (to == null) {
            to = randomPosition(from, random);
        }
        BlockPos at = new BlockPos(to);
        int height = world.getPrecipitationHeight(at).getY();
        if (height < at.getY() && height > 0) {
            to = new Vec3d(to.x, ghast.posY - Math.abs(ghast.posY - to.y), to.z);
        }
        return to;
    }

    private static boolean isGoodTarget(World world, Vec3d to, int distance) {
        if (distance <= 0) {
            return true;
        }
        BlockPos at = new BlockPos(to);
        if (!world.isAirBlock(at)) {
            return false;
        }
        for (EnumFacing face : EnumFacing.values()) {
            for (int i = 1; i < distance; i++) {
                if (!world.isAirBlock(at.offset(face, i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Vec3d randomPosition(Vec3d from, Random random) {
        return new Vec3d(from.x + (random.nextFloat() * 2.0F - 1.0F) * 16.0F, from.y + (random.nextFloat() * 2.0F - 1.0F) * 16.0F,
                from.z + (random.nextFloat() * 2.0F - 1.0F) * 16.0F);
    }

    @Nullable
    private static Vec3d randomWithRestriction(EntityHappyGhast ghast, Vec3d from, Random random) {
        Vec3d to = randomPosition(from, random);
        return ghast.hasHome() && !ghast.isWithinHomeDistanceFromPosition(new BlockPos(to)) ? null : to;
    }

    // ------------------------------------------------------------------------------------------
    // Controls
    // ------------------------------------------------------------------------------------------

    /** Blocks the adult keeps away from (1.21's {@code happy_ghast_avoids}; the family's copies when installed). */
    private static Set<Block> avoids;

    static Set<Block> avoids() {
        if (avoids == null) {
            Set<Block> set = new HashSet<Block>();
            set.add(Blocks.CACTUS);
            set.add(Blocks.MAGMA);
            set.add(Blocks.FIRE);
            for (String name : new String[]{"sweet_berry_bush", "wither_rose", "pointed_dripstone"}) {
                Block b = com.exiledradio.trickytrialsbackport.common.Owners.block(name);
                if (b != null && b != Blocks.AIR) {
                    set.add(b);
                }
            }
            avoids = set;
        }
        return avoids;
    }

    /**
     * 1.21's {@code GhastMoveControl} (careful): every 2-6 ticks a push of flying speed x 5/3 toward the wanted spot,
     * if the spot and everything a block around it is clear of solid blocks, fluids and avoided blocks, and the way
     * there is clear; stopped in place while on its still timeout.
     */
    static final class MoveHelper extends EntityMoveHelper {
        private final EntityHappyGhast ghast;
        private int floatDuration;

        MoveHelper(EntityHappyGhast ghast) {
            super(ghast);
            this.ghast = ghast;
        }

        void setWait() {
            this.action = Action.WAIT;
        }

        @Override
        public void onUpdateMoveHelper() {
            if (this.ghast.isOnStillTimeout()) {
                this.action = Action.WAIT;
                this.ghast.stopInPlace();
            }
            if (this.action == Action.MOVE_TO) {
                if (this.floatDuration-- <= 0) {
                    this.floatDuration += this.ghast.getRNG().nextInt(5) + 2;
                    Vec3d d = new Vec3d(this.posX - this.ghast.posX, this.posY - this.ghast.posY, this.posZ - this.ghast.posZ);
                    if (this.canReach(d)) {
                        Vec3d push = d.normalize().scale(this.ghast.flyingSpeed() * 5.0D / 3.0D);
                        this.ghast.motionX += push.x;
                        this.ghast.motionY += push.y;
                        this.ghast.motionZ += push.z;
                    } else {
                        this.action = Action.WAIT;
                    }
                }
            }
        }

        boolean canReach(Vec3d d) {
            World world = this.ghast.world;
            AxisAlignedBB box = this.ghast.getEntityBoundingBox();
            AxisAlignedBB moved = box.offset(d.x, d.y, d.z);
            for (BlockPos.MutableBlockPos p : BlockPos.getAllInBoxMutable(floor(moved.minX - 1), floor(moved.minY - 1),
                    floor(moved.minZ - 1), floor(moved.maxX + 1), floor(moved.maxY + 1), floor(moved.maxZ + 1))) {
                if (!this.clearAt(world, p, null, false, false)) {
                    return false;
                }
            }
            boolean inWater = this.ghast.isInWater();
            boolean inLava = this.ghast.isInLava();
            // The way there, swept in steps of at most a quarter block.
            double len = d.length();
            int steps = Math.max(1, MathHelper.ceil(len / 0.25D));
            Set<BlockPos> seen = new HashSet<BlockPos>();
            for (int i = 1; i <= steps; i++) {
                double f = (double) i / steps;
                AxisAlignedBB at = box.offset(d.x * f, d.y * f, d.z * f);
                for (BlockPos.MutableBlockPos p : BlockPos.getAllInBoxMutable(floor(at.minX), floor(at.minY), floor(at.minZ),
                        floor(at.maxX - 1.0E-7), floor(at.maxY - 1.0E-7), floor(at.maxZ - 1.0E-7))) {
                    BlockPos q = p.toImmutable();
                    if (box.intersects(new AxisAlignedBB(q)) || !seen.add(q)) {
                        continue;
                    }
                    if (!this.clearAt(world, q, at, inWater, inLava)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /** 1.21's {@code blockTraversalPossible}; {@code sweep} null for the check around the destination. */
        private boolean clearAt(World world, BlockPos pos, @Nullable AxisAlignedBB sweep, boolean inWater, boolean inLava) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock().isAir(state, world, pos)) {
                return true;
            }
            AxisAlignedBB shape = state.getCollisionBoundingBox(world, pos);
            boolean free = sweep == null ? shape == null || shape == Block.NULL_AABB : shape == null || !shape.offset(pos).intersects(sweep);
            if (avoids().contains(state.getBlock())) {
                return false;
            }
            Material m = state.getMaterial();
            boolean water = m == Material.WATER || com.exiledradio.backportcore.waterlog.Waterlogging.hasWater(world, pos);
            if (water || m == Material.LAVA) {
                if (sweep == null || sweep.intersects(new AxisAlignedBB(pos))) {
                    return water ? inWater : inLava;
                }
            }
            return free;
        }

        private static int floor(double v) {
            return MathHelper.floor(v);
        }
    }

    /**
     * 1.21's {@code HappyGhastLookControl}: on its still timeout it squares up to the nearest quarter turn (so it is
     * a level platform), else faces what it was told to look at for 2 ticks, else the way it moves.
     */
    static final class LookHelper extends EntityLookHelper {
        private final EntityHappyGhast ghast;
        private double wantedX;
        private double wantedZ;
        private int lookAtCooldown;

        LookHelper(EntityHappyGhast ghast) {
            super(ghast);
            this.ghast = ghast;
        }

        @Override
        public void setLookPositionWithEntity(Entity entity, float deltaYaw, float deltaPitch) {
            super.setLookPositionWithEntity(entity, deltaYaw, deltaPitch);
            this.wantedX = entity.posX;
            this.wantedZ = entity.posZ;
            this.lookAtCooldown = 2;
        }

        @Override
        public void setLookPosition(double x, double y, double z, float deltaYaw, float deltaPitch) {
            super.setLookPosition(x, y, z, deltaYaw, deltaPitch);
            this.wantedX = x;
            this.wantedZ = z;
            this.lookAtCooldown = 2;
        }

        @Override
        public void onUpdateLook() {
            if (this.ghast.isPlayerControlled()) {
                // The rider's side turns it (tickRidden) and the server takes that turn from the vehicle-move packets;
                // facing its server-side motion (zero under a rider) would turn it away for everyone watching.
                return;
            }
            if (this.ghast.isOnStillTimeout()) {
                float off = wrapDegrees90(this.ghast.rotationYaw);
                this.ghast.rotationYaw -= off;
                this.ghast.rotationYawHead = this.ghast.rotationYaw;
            } else if (this.lookAtCooldown > 0) {
                this.lookAtCooldown--;
                double dx = this.wantedX - this.ghast.posX;
                double dz = this.wantedZ - this.ghast.posZ;
                this.ghast.rotationYaw = -((float) MathHelper.atan2(dx, dz)) * (180.0F / (float) Math.PI);
                this.ghast.renderYawOffset = this.ghast.rotationYaw;
                this.ghast.rotationYawHead = this.ghast.renderYawOffset;
            } else {
                faceMovementDirection(this.ghast);
            }
        }

        static float wrapDegrees90(float yaw) {
            float f = yaw % 90.0F;
            if (f >= 45.0F) {
                f -= 90.0F;
            }
            if (f < -45.0F) {
                f += 90.0F;
            }
            return f;
        }
    }

    /** 1.21's {@code Ghast.faceMovementDirection}, without an attack target. */
    static void faceMovementDirection(EntityLiving mob) {
        EntityLivingBase target = mob.getAttackTarget();
        if (target == null) {
            mob.rotationYaw = -((float) MathHelper.atan2(mob.motionX, mob.motionZ)) * (180.0F / (float) Math.PI);
            mob.renderYawOffset = mob.rotationYaw;
        } else if (target.getDistanceSq(mob) < 4096.0D) {
            mob.rotationYaw = -((float) MathHelper.atan2(target.posX - mob.posX, target.posZ - mob.posZ)) * (180.0F / (float) Math.PI);
            mob.renderYawOffset = mob.rotationYaw;
        }
    }

    /** 1.21's {@code FlyingMoveControl(mob, 180, true)}: turns up to 90 degrees a tick, hovers when it has nowhere to go. */
    static final class BabyMoveHelper extends EntityMoveHelper {
        private static final float MAX_TURN = 180.0F;

        BabyMoveHelper(EntityLiving mob) {
            super(mob);
        }

        @Override
        public void onUpdateMoveHelper() {
            if (this.action == Action.MOVE_TO) {
                this.action = Action.WAIT;
                this.entity.setNoGravity(true);
                double dx = this.posX - this.entity.posX;
                double dy = this.posY - this.entity.posY;
                double dz = this.posZ - this.entity.posZ;
                double d = dx * dx + dy * dy + dz * dz;
                if (d < 2.500000277905201E-7D) {
                    this.entity.setMoveVertical(0.0F);
                    this.entity.setMoveForward(0.0F);
                    return;
                }
                float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
                this.entity.rotationYaw = this.limitAngle(this.entity.rotationYaw, yaw, 90.0F);
                float speed;
                if (this.entity.onGround) {
                    speed = (float) (this.speed * this.entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                } else {
                    speed = (float) (this.speed * this.entity.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).getAttributeValue());
                }
                this.entity.setAIMoveSpeed(speed);
                double h = Math.sqrt(dx * dx + dz * dz);
                if (Math.abs(dy) > 1.0E-5D || Math.abs(h) > 1.0E-5D) {
                    float pitch = (float) (-(MathHelper.atan2(dy, h) * (180.0D / Math.PI)));
                    this.entity.rotationPitch = this.limitAngle(this.entity.rotationPitch, pitch, MAX_TURN);
                    this.entity.setMoveVertical(dy > 0.0D ? speed : -speed);
                }
            } else {
                this.entity.setMoveVertical(0.0F);
                this.entity.setMoveForward(0.0F);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // The ghastling's brain
    // ------------------------------------------------------------------------------------------

    /** 1.21's {@code followable_friendly_mobs}, by registry path (vanilla's and the family's). */
    static final Set<String> FOLLOWABLE = ImmutableSet.of("armadillo", "bee", "camel", "cat", "chicken", "cow", "donkey", "fox",
            "goat", "happy_ghast", "horse", "skeleton_horse", "llama", "mule", "ocelot", "panda", "parrot", "pig", "polar_bear",
            "rabbit", "sheep", "sniffer", "strider", "villager", "wolf");

    /**
     * {@code HappyGhastAi} as one goal: core (swim; panic at 2x for 100-120 ticks after a hurt; look at the look target
     * for 45-90 ticks; walk to the walk target; the temptation cooldown) and idle in order (follow a snowball at 1.25 to 3
     * blocks; follow the nearest visible player, then the nearest visible adult friendly mob, from 3 to 16 blocks at
     * 1.1; else fly to a random spot within 10 at 1.0, or to the look target).
     */
    static final class Brain extends EntityAIBase {
        private final EntityHappyGhast ghast;
        private long now;
        private long panicUntil = -1L;
        private long temptCooldownUntil;
        private boolean tempted;
        @Nullable
        private Entity lookTarget;
        private long lookUntil;
        @Nullable
        private Entity walkEntity;
        @Nullable
        private Vec3d walkPos;
        private double walkSpeed;
        private double closeEnough;
        private int repath;

        Brain(EntityHappyGhast ghast) {
            this.ghast = ghast;
            this.setMutexBits(7);
        }

        @Override
        public boolean shouldExecute() {
            return this.ghast.isChild();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.ghast.isChild();
        }

        boolean panicking() {
            return this.panicUntil >= 0L;
        }

        boolean isTempted() {
            return this.tempted;
        }

        @Nullable
        Entity walkEntity() {
            return this.walkEntity;
        }

        @Override
        public void updateTask() {
            this.now++;
            Random random = this.ghast.getRNG();
            // Swim
            if ((this.ghast.isInWater() || this.ghast.isInLava()) && random.nextFloat() < 0.8F) {
                this.ghast.getJumpHelper().setJumping();
            }
            // AnimalPanic(2.0, 0)
            DamageSource hurt = this.ghast.getLastDamageSource();
            if (!this.panicking() && hurt != null && causesPanic(hurt)) {
                this.panicUntil = this.now + 100 + random.nextInt(21);
                this.clearWalk();
                this.ghast.getNavigator().clearPath();
            }
            if (this.panicking()) {
                if (this.now >= this.panicUntil) {
                    this.panicUntil = -1L;
                } else if (this.ghast.getNavigator().noPath()) {
                    Vec3d pos = airAndWaterPos(this.ghast, 5, 4, 0);
                    if (pos != null) {
                        this.walkTo(pos, 2.0D, 0.0D);
                    }
                }
            }
            if (!this.panicking()) {
                this.idle(random);
            }
            this.lookAtTarget();
            this.moveToTarget();
        }

        private void idle(Random random) {
            EntityPlayer tempting = this.now >= this.temptCooldownUntil ? temptingPlayer(this.ghast) : null;
            if (tempting != null) {
                this.tempted = true;
                this.look(tempting, random);
                if (this.ghast.getDistanceSq(tempting) < 9.0D) {
                    this.clearWalk();
                } else {
                    this.walkTo(tempting, 1.25D, 2.0D);
                }
                return;
            }
            if (this.tempted) {
                this.tempted = false;
                this.temptCooldownUntil = this.now + 100;
                this.clearWalk();
                this.lookTarget = null;
            }
            if (this.hasWalk()) {
                return;
            }
            Entity follow = this.nearestVisiblePlayer();
            if (follow == null || !this.followable(follow)) {
                follow = this.nearestVisibleAdult();
                if (follow != null && !this.followable(follow)) {
                    follow = null;
                }
            }
            if (follow != null) {
                this.lookTarget = follow;
                this.lookUntil = this.now + 45 + random.nextInt(46);
                this.walkTo(follow, 1.1D, 2.0D);
                return;
            }
            // RunOne: stroll or walk to the look target, equally likely; only the stroll can run without a look target.
            if (this.lookTarget != null && this.lookTarget.isEntityAlive() && random.nextBoolean()) {
                this.walkTo(this.lookTarget, 1.0D, 3.0D);
            } else {
                Vec3d pos = airAndWaterPos(this.ghast, 10, 7, -2);
                if (pos != null) {
                    this.walkTo(pos, 1.0D, 0.0D);
                }
            }
        }

        /** BabyFollowAdult(3..16): from 3 to 17 blocks away. */
        private boolean followable(Entity e) {
            double d = this.ghast.getDistance(e);
            return d < 17.0D && d >= 3.0D;
        }

        @Nullable
        private EntityPlayer nearestVisiblePlayer() {
            double range = this.ghast.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
            EntityPlayer best = null;
            double bestD = range * range;
            for (EntityPlayer p : this.ghast.world.playerEntities) {
                double d = this.ghast.getDistanceSq(p);
                if (!p.isSpectator() && p.isEntityAlive() && d < bestD && this.ghast.getEntitySenses().canSee(p)) {
                    best = p;
                    bestD = d;
                }
            }
            return best;
        }

        @Nullable
        private EntityLivingBase nearestVisibleAdult() {
            EntityLivingBase best = null;
            double bestD = Double.MAX_VALUE;
            for (EntityLivingBase e : this.ghast.world.getEntitiesWithinAABB(EntityLivingBase.class, this.ghast.getEntityBoundingBox().grow(16.0D))) {
                if (e == this.ghast || !e.isEntityAlive() || e.isChild() || !isFollowable(e)) {
                    continue;
                }
                double d = this.ghast.getDistanceSq(e);
                if (d < 256.0D && d < bestD && this.ghast.getEntitySenses().canSee(e)) {
                    best = e;
                    bestD = d;
                }
            }
            return best;
        }

        private void look(Entity target, Random random) {
            this.lookTarget = target;
            this.lookUntil = this.now + 45 + random.nextInt(46);
        }

        private void lookAtTarget() {
            if (this.lookTarget == null) {
                return;
            }
            if (this.now > this.lookUntil || !this.lookTarget.isEntityAlive() || this.lookTarget.world != this.ghast.world) {
                this.lookTarget = null;
                return;
            }
            this.ghast.getLookHelper().setLookPositionWithEntity(this.lookTarget, 30.0F, (float) this.ghast.getVerticalFaceSpeed());
        }

        private boolean hasWalk() {
            return this.walkEntity != null || this.walkPos != null;
        }

        private void clearWalk() {
            this.walkEntity = null;
            this.walkPos = null;
        }

        private void walkTo(Entity e, double speed, double close) {
            boolean same = this.walkEntity == e;
            this.walkEntity = e;
            this.walkPos = null;
            this.walkSpeed = speed;
            this.closeEnough = close;
            if (!same) {
                this.repath = 0;
            }
        }

        private void walkTo(Vec3d pos, double speed, double close) {
            this.walkEntity = null;
            this.walkPos = pos;
            this.walkSpeed = speed;
            this.closeEnough = close;
            this.repath = 0;
        }

        /** MoveToTargetSink: path to the walk target, drop it once within reach or when no path is found. */
        private void moveToTarget() {
            if (!this.hasWalk()) {
                return;
            }
            Vec3d to;
            if (this.walkEntity != null) {
                if (!this.walkEntity.isEntityAlive() || this.walkEntity.world != this.ghast.world) {
                    this.clearWalk();
                    return;
                }
                to = new Vec3d(this.walkEntity.posX, this.walkEntity.posY, this.walkEntity.posZ);
            } else {
                to = this.walkPos;
            }
            if (this.ghast.getDistance(to.x, to.y, to.z) <= Math.max(this.closeEnough, 0.5D)) {
                this.clearWalk();
                this.ghast.getNavigator().clearPath();
                return;
            }
            if (this.repath-- <= 0 || this.ghast.getNavigator().noPath()) {
                this.repath = 10;
                boolean ok = this.walkEntity != null ? this.ghast.getNavigator().tryMoveToEntityLiving(this.walkEntity, this.walkSpeed)
                        : this.ghast.getNavigator().tryMoveToXYZ(to.x, to.y, to.z, this.walkSpeed);
                if (!ok) {
                    this.clearWalk();
                }
            }
        }
    }

    static boolean isFollowable(Entity e) {
        ResourceLocation id = EntityList.getKey(e);
        return id != null && FOLLOWABLE.contains(id.getPath()) && (!(e instanceof EntityAgeable) || !((EntityAgeable) e).isChild());
    }

    /** 1.21's {@code panic_causes}: what hurts it from outside, not starving, drowning or the void. */
    static boolean causesPanic(DamageSource source) {
        return source != DamageSource.OUT_OF_WORLD && source != DamageSource.STARVE && source != DamageSource.DROWN
                && source != DamageSource.IN_WALL && source != DamageSource.CRAMMING && source != DamageSource.GENERIC;
    }

    /**
     * 1.21's {@code AirAndWaterRandomPos.getPos}: ten tries at a spot up to {@code horizontal} away within 90 degrees of
     * where it looks, {@code vertical} up or down plus {@code yOffset}, moved up out of solid blocks, kept within home;
     * the best by path weight.
     */
    private static final WalkNodeProcessor NODES = new WalkNodeProcessor();

    @Nullable
    static Vec3d airAndWaterPos(EntityHappyGhast mob, int horizontal, int vertical, int yOffset) {
        Random random = mob.getRNG();
        Vec3d look = mob.getLook(0.0F);
        boolean restricted = mob.hasHome()
                && mob.getHomePosition().distanceSq(mob.posX, mob.posY, mob.posZ) < Math.pow(mob.getMaximumHomeDistance() + horizontal + 1, 2);
        BlockPos best = null;
        float bestWeight = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 10; i++) {
            double base = MathHelper.atan2(look.z, look.x) - Math.PI / 2;
            double angle = base + (2.0F * random.nextFloat() - 1.0F) * (Math.PI / 2);
            double len = Math.sqrt(random.nextDouble()) * Math.sqrt(2.0D) * horizontal;
            double x = -len * Math.sin(angle);
            double z = len * Math.cos(angle);
            if (Math.abs(x) > horizontal || Math.abs(z) > horizontal) {
                continue;
            }
            int y = random.nextInt(2 * vertical + 1) - vertical + yOffset;
            BlockPos dir = new BlockPos(x, y, z);
            int ox = 0;
            int oz = 0;
            if (mob.hasHome() && horizontal > 1) {
                BlockPos home = mob.getHomePosition();
                ox = mob.posX > home.getX() ? -random.nextInt(horizontal / 2) : random.nextInt(horizontal / 2);
                oz = mob.posZ > home.getZ() ? -random.nextInt(horizontal / 2) : random.nextInt(horizontal / 2);
            }
            BlockPos pos = new BlockPos(mob).add(dir).add(ox, 0, oz);
            if (pos.getY() < 0 || pos.getY() >= mob.world.getHeight() || restricted && !mob.isWithinHomeDistanceFromPosition(pos)) {
                continue;
            }
            while (pos.getY() < mob.world.getHeight() - 1 && mob.world.getBlockState(pos).getMaterial().isSolid()) {
                pos = pos.up();
            }
            PathNodeType type = NODES.getPathNodeType(mob.world, pos.getX(), pos.getY(), pos.getZ());
            if (mob.getPathPriority(type) != 0.0F) {
                continue;
            }
            float w = mob.getBlockPathWeight(pos);
            if (w > bestWeight) {
                bestWeight = w;
                best = pos;
            }
        }
        return best == null ? null : new Vec3d(best.getX() + 0.5D, best.getY(), best.getZ() + 0.5D);
    }
}
