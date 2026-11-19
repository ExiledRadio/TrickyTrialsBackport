package com.exiledradio.trickytrialsbackport.coppergolem;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 1.21.11's {@code TransportItemsBetweenContainers} with the copper golem's settings ({@code CopperGolemAi}): an
 * empty-handed golem walks to the nearest copper chest that has anything in it and takes up to 16 of the first stack;
 * holding something, it walks to the nearest chest or trapped chest that is empty or already holds that item and puts
 * it in. It searches 32 blocks across and 8 up and down (1 while riding), stands 60 ticks (3 s) at each chest with its
 * lid open, plays its sound on the 9th, remembers up to 10 chests it tried for 6000 ticks (50 it could not reach), and
 * when nothing fits it rests 140 ticks. It waits its turn behind another golem or a player at a chest.
 *
 * <p>Like a 1.21 brain behavior it is run every tick ({@link EntityCopperGolem#updateAITasks}), not through 1.12.2's
 * task list, which only looks at starting a task every third tick. Its memories live on the golem.
 */
public final class CopperGolemTransport {

    public static final int TARGET_INTERACTION_TIME = 60;
    static final int VISITED_POSITIONS_MEMORY_TIME = 6000;
    static final int TRANSPORTED_ITEM_MAX_STACK_SIZE = 16;
    static final int MAX_VISITED_POSITIONS = 10;
    static final int MAX_UNREACHABLE_POSITIONS = 50;
    static final int IDLE_COOLDOWN = 140;
    static final int HORIZONTAL_SEARCH = 32;
    static final int VERTICAL_SEARCH = 8;
    static final int TICK_TO_PLAY_ON_REACHED_SOUND = 9;
    static final double CLOSE_ENOUGH_TO_START_QUEUING_DISTANCE = 3.0D;
    static final double CLOSE_ENOUGH_TO_CONTINUE_INTERACTING_WITH_TARGET = 2.0D;

    public enum State {
        TRAVELLING,
        QUEUING,
        INTERACTING
    }

    enum Interaction {
        PICKUP_ITEM,
        PICKUP_NO_ITEM,
        PLACE_ITEM,
        PLACE_NO_ITEM
    }

    /** A chest the golem is going to: its position, container (both halves of a double chest) and block. */
    static final class Target {
        final BlockPos pos;
        final IInventory container;
        final TileEntityChest blockEntity;
        final IBlockState state;

        Target(BlockPos pos, IInventory container, TileEntityChest blockEntity, IBlockState state) {
            this.pos = pos;
            this.container = container;
            this.blockEntity = blockEntity;
            this.state = state;
        }

        @Nullable
        static Target tryCreate(World world, BlockPos pos) {
            TileEntity te = world.getTileEntity(pos);
            return te instanceof TileEntityChest ? tryCreate(world, (TileEntityChest) te) : null;
        }

        @Nullable
        static Target tryCreate(World world, TileEntityChest te) {
            BlockPos pos = te.getPos();
            IBlockState state = world.getBlockState(pos);
            IInventory container = null;
            if (state.getBlock() instanceof BlockChest) {
                container = ((BlockChest) state.getBlock()).getContainer(world, pos, false);
            }
            return container == null ? null : new Target(pos, container, te, state);
        }
    }

    private final EntityCopperGolem golem;
    private boolean running;
    @Nullable
    private Target target;
    private State state = State.TRAVELLING;
    @Nullable
    private Interaction interactionState;
    private int ticksSinceReachingTarget;
    /** The position the navigator was last sent to, so a path is not recomputed every tick. */
    @Nullable
    private BlockPos walkingTo;
    private int repathCooldown;

    CopperGolemTransport(EntityCopperGolem golem) {
        this.golem = golem;
    }

    public int ticksSinceReachingTarget() {
        return this.ticksSinceReachingTarget;
    }

    @Nullable
    public BlockPos targetPos() {
        return this.target == null ? null : this.target.pos;
    }

    public State state() {
        return this.state;
    }

    public boolean isRunning() {
        return this.running;
    }

    // ------------------------------------------------------------------------------------------
    // The behavior's life: start, tick, stop (1.21's Behavior).
    // ------------------------------------------------------------------------------------------

    /** One tick of the brain: start if the memories allow it, else tick or stop. */
    void update(WorldServer world) {
        if (!this.running) {
            if (canStart()) {
                this.running = true;
            } else {
                return;
            }
        }
        if (!canStillUse()) {
            stop();
            return;
        }
        tick(world);
    }

    private boolean canStart() {
        return this.golem.transportCooldown <= 0 && !this.golem.isPanicking() && !this.golem.getLeashed();
    }

    private boolean canStillUse() {
        return this.golem.transportCooldown <= 0 && !this.golem.isPanicking() && !this.golem.getLeashed();
    }

    void stop() {
        onStartTravelling();
        this.running = false;
    }

    private void tick(WorldServer world) {
        boolean changed = updateInvalidTarget(world);
        if (this.target == null) {
            stop();
        } else if (!changed) {
            if (this.state == State.QUEUING) {
                onQueuingForTarget(this.target, world);
            }
            if (this.state == State.TRAVELLING) {
                onTravelToTarget(this.target, world);
            }
            if (this.state == State.INTERACTING) {
                onReachedTarget(this.target, world);
            }
        }
    }

    private boolean updateInvalidTarget(WorldServer world) {
        if (!hasValidTarget(world)) {
            stopTargetingCurrentTarget();
            Target found = getTransportTarget(world);
            if (found != null) {
                this.target = found;
                onStartTravelling();
                setVisitedBlockPos(found.pos);
            } else {
                enterCooldownAfterNoMatchingTargetFound();
            }
            return true;
        }
        return false;
    }

    private void onQueuingForTarget(Target t, World world) {
        if (!isAnotherMobInteractingWithTarget(t, world)) {
            resumeTravelling();
        }
    }

    private void onTravelToTarget(Target t, World world) {
        if (isWithinTargetDistance(CLOSE_ENOUGH_TO_START_QUEUING_DISTANCE, t, world, getCenterPos())
                && isAnotherMobInteractingWithTarget(t, world)) {
            startQueuing();
        } else if (isWithinTargetDistance(getInteractionRange(), t, world, getCenterPos())) {
            startOnReachedTargetInteraction(t);
        } else {
            walkTowardsTarget();
        }
    }

    private Vec3d getCenterPos() {
        return setMiddleYPosition(this.golem.getPositionVector());
    }

    private void onReachedTarget(Target t, World world) {
        if (!isWithinTargetDistance(CLOSE_ENOUGH_TO_CONTINUE_INTERACTING_WITH_TARGET, t, world, getCenterPos())) {
            onStartTravelling();
        } else {
            this.ticksSinceReachingTarget++;
            onTargetInteraction(t);
            if (this.ticksSinceReachingTarget >= TARGET_INTERACTION_TIME) {
                if (isPickingUpItems()) {
                    if (!t.container.isEmpty()) {
                        pickUpItems(t.container);
                    } else {
                        stopTargetingCurrentTarget();
                    }
                } else if (matchesLeavingItemsRequirement(t.container)) {
                    putDownItem(t.container);
                } else {
                    stopTargetingCurrentTarget();
                }
                onStartTravelling();
            }
        }
    }

    private void startQueuing() {
        stopInPlace();
        this.state = State.QUEUING;
    }

    private void resumeTravelling() {
        this.state = State.TRAVELLING;
        walkTowardsTarget();
    }

    /** 1.21's walk and look target memories, followed by its move-to-target sink. */
    private void walkTowardsTarget() {
        if (this.target == null) {
            return;
        }
        BlockPos p = this.target.pos;
        this.golem.getLookHelper().setLookPosition(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D,
                this.golem.getHorizontalFaceSpeed(), this.golem.getVerticalFaceSpeed());
        if (this.repathCooldown > 0) {
            this.repathCooldown--;
        }
        if (!p.equals(this.walkingTo) || (this.golem.getNavigator().noPath() && this.repathCooldown <= 0)) {
            this.walkingTo = p;
            boolean found = this.golem.getNavigator().tryMoveToXYZ(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D, 1.0D);
            // 1.21's MoveToTargetSink waits a while before trying an unreachable target again.
            this.repathCooldown = found ? 10 : 40;
        }
    }

    private void startOnReachedTargetInteraction(Target t) {
        if (isPickingUpItems()) {
            this.interactionState = !t.container.isEmpty() ? Interaction.PICKUP_ITEM : Interaction.PICKUP_NO_ITEM;
        } else {
            this.interactionState = matchesLeavingItemsRequirement(t.container) ? Interaction.PLACE_ITEM : Interaction.PLACE_NO_ITEM;
        }
        this.state = State.INTERACTING;
    }

    /** 1.21's {@code onStartTravelling}, with the copper golem's callback: its chest closes and it goes idle. */
    private void onStartTravelling() {
        this.golem.clearOpenedChestPos();
        this.golem.setGolemState(EntityCopperGolem.STATE_IDLE);
        this.state = State.TRAVELLING;
        this.interactionState = null;
        this.ticksSinceReachingTarget = 0;
    }

    /** Looking at the chest, standing still, and the copper golem's interaction (lid, state, sound). */
    private void onTargetInteraction(Target t) {
        BlockPos p = t.pos;
        this.golem.getLookHelper().setLookPosition(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D,
                this.golem.getHorizontalFaceSpeed(), this.golem.getVerticalFaceSpeed());
        stopInPlace();
        if (this.interactionState == null) {
            return;
        }
        int ticks = this.ticksSinceReachingTarget;
        if (ticks == 1) {
            this.golem.openChest(t.pos);
            this.golem.setGolemState(golemState(this.interactionState));
        } else {
            this.golem.keepChestOpen();
        }
        SoundEvent sound = sound(this.interactionState);
        if (ticks == TICK_TO_PLAY_ON_REACHED_SOUND && sound != null) {
            this.golem.playSound(sound, 1.0F, 1.0F);
        }
        if (ticks == TARGET_INTERACTION_TIME) {
            this.golem.clearOpenedChestPos();
        }
    }

    static int golemState(Interaction i) {
        switch (i) {
            case PICKUP_ITEM:
                return EntityCopperGolem.STATE_GETTING_ITEM;
            case PICKUP_NO_ITEM:
                return EntityCopperGolem.STATE_GETTING_NO_ITEM;
            case PLACE_ITEM:
                return EntityCopperGolem.STATE_DROPPING_ITEM;
            default:
                return EntityCopperGolem.STATE_DROPPING_NO_ITEM;
        }
    }

    static SoundEvent sound(Interaction i) {
        switch (i) {
            case PICKUP_ITEM:
                return CopperGolemContent.ITEM_GET;
            case PICKUP_NO_ITEM:
                return CopperGolemContent.ITEM_NO_GET;
            case PLACE_ITEM:
                return CopperGolemContent.ITEM_DROP;
            default:
                return CopperGolemContent.ITEM_NO_DROP;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Finding a chest
    // ------------------------------------------------------------------------------------------

    /** For the self-test: where {@link #getTransportTarget} would go now. */
    @Nullable
    public BlockPos findTarget(WorldServer world) {
        Target t = getTransportTarget(world);
        return t == null ? null : t.pos;
    }

    /** The nearest wanted chest within the search box, in loaded chunks only, not yet tried and not locked. */
    @Nullable
    Target getTransportTarget(WorldServer world) {
        int h = getHorizontalSearchDistance();
        int v = getVerticalSearchDistance();
        BlockPos at = new BlockPos(this.golem);
        Set<BlockPos> visited = this.golem.visitedPositions();
        Set<BlockPos> unreachable = this.golem.unreachablePositions();
        int range = Math.floorDiv(h, 16) + 1;
        int cx = at.getX() >> 4;
        int cz = at.getZ() >> 4;
        Target best = null;
        double bestDist = Float.MAX_VALUE;
        for (int x = cx - range; x <= cx + range; x++) {
            for (int z = cz - range; z <= cz + range; z++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
                if (chunk == null) {
                    continue;
                }
                for (TileEntity te : new ArrayList<TileEntity>(chunk.getTileEntityMap().values())) {
                    if (!(te instanceof TileEntityChest) || te.isInvalid()) {
                        continue;
                    }
                    double d = te.getPos().distanceSqToCenter(this.golem.posX, this.golem.posY, this.golem.posZ);
                    if (d < bestDist) {
                        Target t = isTargetValidToPick(world, (TileEntityChest) te, visited, unreachable, at, h, v);
                        if (t != null) {
                            best = t;
                            bestDist = d;
                        }
                    }
                }
            }
        }
        return best;
    }

    @Nullable
    private Target isTargetValidToPick(World world, TileEntityChest te, Set<BlockPos> visited, Set<BlockPos> unreachable,
                                       BlockPos at, int h, int v) {
        BlockPos p = te.getPos();
        // new AABB(blockPosition).inflate(h, v, h).contains(x, y, z): min inclusive, max exclusive.
        if (p.getX() < at.getX() - h || p.getX() >= at.getX() + 1 + h || p.getY() < at.getY() - v || p.getY() >= at.getY() + 1 + v
                || p.getZ() < at.getZ() - h || p.getZ() >= at.getZ() + 1 + h) {
            return null;
        }
        Target t = Target.tryCreate(world, te);
        if (t == null) {
            return null;
        }
        return isWantedBlock(t.state) && !isPositionAlreadyVisited(visited, unreachable, t, world) && !isContainerLocked(t) ? t : null;
    }

    private static boolean isContainerLocked(Target t) {
        return t.blockEntity.isLocked();
    }

    private boolean hasValidTarget(World world) {
        boolean valid = this.target != null && isWantedBlock(this.target.state) && targetHasNotChanged(world, this.target);
        if (valid && !isTargetBlocked(world, this.target)) {
            if (this.state != State.TRAVELLING) {
                return true;
            }
            if (hasValidTravellingPath(world, this.target)) {
                return true;
            }
            markVisitedBlockPosAsUnreachable(this.target.pos);
        }
        return false;
    }

    private boolean hasValidTravellingPath(World world, Target t) {
        Path path = this.golem.getNavigator().getPath();
        if (path == null) {
            path = this.golem.getNavigator().getPathToPos(t.pos);
        }
        Vec3d from = getPositionToReachTargetFrom(path);
        boolean within = isWithinTargetDistance(getInteractionRange(), t, world, from);
        boolean noPathAndNotThere = path == null && !within;
        return noPathAndNotThere || targetIsReachableFromPosition(world, within, from, t);
    }

    private Vec3d getPositionToReachTargetFrom(@Nullable Path path) {
        PathPoint end = path == null ? null : path.getFinalPathPoint();
        Vec3d v = end == null ? this.golem.getPositionVector() : new Vec3d(end.x + 0.5D, end.y, end.z + 0.5D);
        return setMiddleYPosition(v);
    }

    private Vec3d setMiddleYPosition(Vec3d v) {
        return v.add(0.0D, (this.golem.getEntityBoundingBox().maxY - this.golem.getEntityBoundingBox().minY) / 2.0D, 0.0D);
    }

    private static boolean isTargetBlocked(World world, Target t) {
        return BlockCopperChest.isChestBlockedAt(world, t.pos);
    }

    private static boolean targetHasNotChanged(World world, Target t) {
        return t.blockEntity == world.getTileEntity(t.pos) && !t.blockEntity.isInvalid();
    }

    /** The chest and, for a double chest, its other half. */
    static List<Target> getConnectedTargets(Target t, World world) {
        List<Target> out = new ArrayList<Target>(2);
        out.add(t);
        BlockPos other = otherHalf(world, t.pos);
        if (other != null) {
            Target o = Target.tryCreate(world, other);
            if (o != null) {
                out.add(o);
            }
        }
        return out;
    }

    /** @return the other half of a double chest (a chest of the same type beside it), or null */
    @Nullable
    static BlockPos otherHalf(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        if (!(block instanceof BlockChest)) {
            return null;
        }
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            Block b = world.getBlockState(pos.offset(side)).getBlock();
            if (b == block || (block instanceof BlockCopperChest && b instanceof BlockCopperChest)) {
                return pos.offset(side);
            }
        }
        return null;
    }

    private int getHorizontalSearchDistance() {
        return this.golem.isRiding() ? 1 : HORIZONTAL_SEARCH;
    }

    private int getVerticalSearchDistance() {
        return this.golem.isRiding() ? 1 : VERTICAL_SEARCH;
    }

    private static boolean isPositionAlreadyVisited(Set<BlockPos> visited, Set<BlockPos> unreachable, Target t, World world) {
        for (Target c : getConnectedTargets(t, world)) {
            if (visited.contains(c.pos) || unreachable.contains(c.pos)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFinishedPath() {
        Path path = this.golem.getNavigator().getPath();
        return path != null && path.isFinished();
    }

    void setVisitedBlockPos(BlockPos pos) {
        Set<BlockPos> visited = new HashSet<BlockPos>(this.golem.visitedPositions());
        visited.add(pos);
        if (visited.size() > MAX_VISITED_POSITIONS) {
            enterCooldownAfterNoMatchingTargetFound();
        } else {
            this.golem.setVisited(visited, VISITED_POSITIONS_MEMORY_TIME);
        }
    }

    void markVisitedBlockPosAsUnreachable(BlockPos pos) {
        Set<BlockPos> visited = new HashSet<BlockPos>(this.golem.visitedPositions());
        visited.remove(pos);
        Set<BlockPos> unreachable = new HashSet<BlockPos>(this.golem.unreachablePositions());
        unreachable.add(pos);
        if (unreachable.size() > MAX_UNREACHABLE_POSITIONS) {
            enterCooldownAfterNoMatchingTargetFound();
        } else {
            this.golem.setVisited(visited, VISITED_POSITIONS_MEMORY_TIME);
            this.golem.setUnreachable(unreachable, VISITED_POSITIONS_MEMORY_TIME);
        }
    }

    /** Empty-handed: a copper chest; holding something: a (trapped) chest, 1.21's two blocks, not modded chests. */
    private boolean isWantedBlock(IBlockState state) {
        Block b = state.getBlock();
        return isPickingUpItems() ? BlockCopperChest.isCopperChest(b) : b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST;
    }

    private double getInteractionRange() {
        return hasFinishedPath() ? 1.0D : 0.5D;
    }

    /** The chest's collision box, grown by {@code range} sideways and 0.5 up and down, meets the golem's box at {@code at}. */
    private boolean isWithinTargetDistance(double range, Target t, World world, Vec3d at) {
        AxisAlignedBB own = this.golem.getEntityBoundingBox();
        double w = own.maxX - own.minX;
        double hgt = own.maxY - own.minY;
        double d = own.maxZ - own.minZ;
        AxisAlignedBB box = new AxisAlignedBB(at.x - w / 2, at.y - hgt / 2, at.z - d / 2, at.x + w / 2, at.y + hgt / 2, at.z + d / 2);
        AxisAlignedBB shape = t.state.getBoundingBox(world, t.pos);
        return shape.grow(range, 0.5D, range).offset(t.pos).intersects(box);
    }

    private boolean targetIsReachableFromPosition(World world, boolean within, Vec3d from, Target t) {
        return within && canSeeAnyTargetSide(t, world, from);
    }

    /** A clear line from the golem to the middle of any of the chest's six faces that ends on the chest. */
    private static boolean canSeeAnyTargetSide(Target t, World world, Vec3d from) {
        Vec3d center = new Vec3d(t.pos.getX() + 0.5D, t.pos.getY() + 0.5D, t.pos.getZ() + 0.5D);
        for (EnumFacing f : EnumFacing.values()) {
            Vec3d to = center.add(0.5D * f.getXOffset(), 0.5D * f.getYOffset(), 0.5D * f.getZOffset());
            RayTraceResult hit = world.rayTraceBlocks(from, to, false, true, false);
            if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && t.pos.equals(hit.getBlockPos())) {
                return true;
            }
        }
        return false;
    }

    /** Someone (a player or another golem) has this chest or its other half open: 1.21's copper golem queues. */
    static boolean isAnotherMobInteractingWithTarget(Target t, World world) {
        for (Target c : getConnectedTargets(t, world)) {
            if (c.blockEntity.numPlayersUsing > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean isPickingUpItems() {
        return this.golem.getHeldItemMainhand().isEmpty();
    }

    private boolean matchesLeavingItemsRequirement(IInventory container) {
        return container.isEmpty() || hasItemMatchingHandItem(container);
    }

    /** 1.21's {@code ItemStack.isSameItem}: the same item, whatever its damage, enchantments, name or contents. */
    public static boolean isSameItem(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) {
            return false;
        }
        // 1.12.2 tells some items apart by metadata where 1.13 made them separate items (wool colors, dyes, ...).
        return !a.getHasSubtypes() || a.getMetadata() == b.getMetadata();
    }

    private boolean hasItemMatchingHandItem(IInventory container) {
        ItemStack hand = this.golem.getHeldItemMainhand();
        for (int i = 0; i < container.getSizeInventory(); i++) {
            if (isSameItem(container.getStackInSlot(i), hand)) {
                return true;
            }
        }
        return false;
    }

    private void pickUpItems(IInventory container) {
        this.golem.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, pickupItemFromContainer(container));
        this.golem.setGuaranteedDrop();
        container.markDirty();
        clearMemoriesAfterMatchingTargetFound();
    }

    private void putDownItem(IInventory container) {
        ItemStack left = addItemsToContainer(this.golem.getHeldItemMainhand(), container);
        container.markDirty();
        this.golem.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, left);
        if (left.isEmpty()) {
            clearMemoriesAfterMatchingTargetFound();
        } else {
            stopTargetingCurrentTarget();
        }
    }

    /** Up to 16 of the first stack, from the first slot that has one. */
    public static ItemStack pickupItemFromContainer(IInventory container) {
        for (int i = 0; i < container.getSizeInventory(); i++) {
            ItemStack s = container.getStackInSlot(i);
            if (!s.isEmpty()) {
                return container.decrStackSize(i, Math.min(s.getCount(), TRANSPORTED_ITEM_MAX_STACK_SIZE));
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 1.21's {@code addItemsToContainer}: slot by slot, the whole held stack into the first empty slot, or as much as
     * fits onto a matching stack (same item and data) before it.
     *
     * @return what is still held
     */
    public static ItemStack addItemsToContainer(ItemStack hand, IInventory container) {
        for (int i = 0; i < container.getSizeInventory(); i++) {
            ItemStack s = container.getStackInSlot(i);
            if (s.isEmpty()) {
                container.setInventorySlotContents(i, hand);
                return ItemStack.EMPTY;
            }
            if (ItemStack.areItemsEqual(s, hand) && ItemStack.areItemStackTagsEqual(s, hand) && s.getCount() < s.getMaxStackSize()) {
                int space = s.getMaxStackSize() - s.getCount();
                int moved = Math.min(space, hand.getCount());
                s.setCount(s.getCount() + moved);
                // 1.21 takes the free space, not the amount moved, off the held stack: the same when it fills up.
                hand.setCount(hand.getCount() - space);
                container.setInventorySlotContents(i, s);
                if (hand.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return hand;
    }

    void stopTargetingCurrentTarget() {
        this.ticksSinceReachingTarget = 0;
        this.target = null;
        this.walkingTo = null;
        this.golem.getNavigator().clearPath();
    }

    void clearMemoriesAfterMatchingTargetFound() {
        stopTargetingCurrentTarget();
        this.golem.clearVisited();
    }

    void enterCooldownAfterNoMatchingTargetFound() {
        stopTargetingCurrentTarget();
        this.golem.transportCooldown = IDLE_COOLDOWN;
        this.golem.clearVisited();
    }

    private void stopInPlace() {
        this.golem.getNavigator().clearPath();
        this.walkingTo = null;
        this.golem.getMoveHelper().setMoveTo(this.golem.posX, this.golem.posY, this.golem.posZ, 0.0D);
        this.golem.setMoveForward(0.0F);
        this.golem.setMoveStrafing(0.0F);
        this.golem.setAIMoveSpeed(0.0F);
        this.golem.motionX = 0.0D;
        this.golem.motionZ = 0.0D;
    }

    /** @return the chest's container as the golem sees it (both halves of a double chest) */
    static IInventory containerAt(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        if (s.getBlock() instanceof BlockChest) {
            ILockableContainer c = ((BlockChest) s.getBlock()).getContainer(world, pos, true);
            return c;
        }
        TileEntity te = world.getTileEntity(pos);
        return te instanceof IInventory ? (IInventory) te : null;
    }

    static boolean isPartOf(IInventory inv, TileEntityChest te) {
        return inv == te || inv instanceof InventoryLargeChest && ((InventoryLargeChest) inv).isPartOfLargeChest(te);
    }
}
