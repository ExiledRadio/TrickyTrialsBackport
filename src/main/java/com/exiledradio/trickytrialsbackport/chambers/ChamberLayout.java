package com.exiledradio.trickytrialsbackport.chambers;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * 1.21.11's jigsaw assembly ({@code JigsawPlacement.addPieces} and its {@code Placer}) for trial chambers, draw for
 * draw: the start height, the start piece's rotation and element, then the children, each jigsaw in shuffled order
 * sorted by {@code selection_priority}, the pieces they open queued by {@code placement_priority}
 * ({@code SequencedPriorityIterator}), the pools looked up through the structure's {@code pool_aliases}, all inside a
 * box {@code max_distance_from_center} out from the start and kept {@code dimension_padding} off the world's floor and
 * roof. Every chamber piece is rigid, so the ground is never asked.
 *
 * <p>The layout is always worked out in 1.21's own heights (y -64 to 319), so it is vanilla's piece for piece;
 * {@link TrialChambersStructure} then moves it to the world's start height. Backport Core's {@code Jigsaw} is
 * 1.16.5's (no priorities, no aliases, fastutil's pool shuffle), so this is our own; the pools, elements,
 * templates and pieces are Core's.
 */
public final class ChamberLayout {

    /** 1.21's overworld: {@code getMinY()} and {@code getMaxY()} (inclusive). */
    public static final int MIN_Y = -64;
    public static final int MAX_Y = 319;

    private ChamberLayout() {
    }

    /** The {@code GenerationStub}: where the biome is checked, and the start piece. */
    public static final class Stub {
        /** 1.21's stub position: the start piece's middle at the start height, in 1.21's heights. */
        public final BlockPos position;
        /** The sampled start height, in 1.21's heights. */
        public final int startY;
        final ChamberPiece start;
        final Map<String, String> aliases;

        Stub(BlockPos position, int startY, ChamberPiece start, Map<String, String> aliases) {
            this.position = position;
            this.startY = startY;
            this.start = start;
            this.aliases = aliases;
        }
    }

    /**
     * {@code JigsawStructure.findGenerationPoint} up to the stub: the start height from the start random, the aliases,
     * the start piece. Draws from {@code random} exactly as 1.21 does.
     *
     * @param random {@code StructureRandoms.start(seed, chunkX, chunkZ)}, 1.21's {@code makeRandom}
     * @return null where 1.21 gives no start (an empty start element, the padding)
     */
    @Nullable
    public static Stub stub(long seed, int chunkX, int chunkZ, Random random) {
        ChamberSettings s = ChamberSettings.get();
        int y = random.nextInt(s.startMaxY - s.startMinY + 1) + s.startMinY;
        BlockPos origin = new BlockPos(chunkX << 4, y, chunkZ << 4);
        Map<String, String> aliases = aliases(s, origin, seed);

        Rotation rotation = Rotation.values()[random.nextInt(4)];
        JigsawPools.Pool pool = JigsawPools.pool(new ResourceLocation(lookup(aliases, s.startPool.toString())));
        if (pool == null || pool.size() == 0) {
            TrickyTrialsBackport.LOGGER.warn("The trial chambers start pool {} is missing.", s.startPool);
            return null;
        }
        JigsawElement element = pool.elements().get(random.nextInt(pool.size()));
        if (element.isEmpty()) {
            return null;
        }
        ChamberPiece first = new ChamberPiece(element, origin, element.groundLevelDelta(), rotation, element.box(origin, rotation));
        StructureBoundingBox box = first.getBoundingBox();
        int midX = (box.maxX + box.minX) / 2;
        int midZ = (box.maxZ + box.minZ) / 2;
        int floor = box.minY + first.groundLevelDelta();
        first.offset(0, y - floor, 0);
        StructureBoundingBox moved = first.getBoundingBox();
        if ((s.paddingBottom != 0 || s.paddingTop != 0)
                && (moved.minY < MIN_Y + s.paddingBottom || moved.maxY > MAX_Y - s.paddingTop)) {
            return null;
        }
        return new Stub(new BlockPos(midX, y, midZ), y, first, aliases);
    }

    /** {@code PoolAliasLookup.create}: {@code RandomSource.create(seed).forkPositional().at(origin)}. */
    static Map<String, String> aliases(ChamberSettings s, BlockPos origin, long seed) {
        Map<String, String> out = new HashMap<String, String>();
        if (s.aliases.isEmpty()) {
            return out;
        }
        long positional = new Random(seed).nextLong();
        ChamberSettings.LegacyRandom random = new ChamberSettings.LegacyRandom(
                ChamberSettings.LegacyRandom.positionSeed(origin.getX(), origin.getY(), origin.getZ()) ^ positional);
        for (ChamberSettings.Alias a : s.aliases) {
            a.resolve(random, out);
        }
        return out;
    }

    static String lookup(Map<String, String> aliases, String pool) {
        String full = ChamberSettings.full(pool);
        String to = aliases.get(full);
        return to == null ? full : to;
    }

    /**
     * The whole structure in 1.21's heights: {@link #stub}, then its children. Empty when there is no start.
     */
    public static List<ChamberPiece> assemble(long seed, int chunkX, int chunkZ, Random random) {
        List<ChamberPiece> pieces = new ArrayList<ChamberPiece>();
        Stub stub = stub(seed, chunkX, chunkZ, random);
        if (stub == null) {
            return pieces;
        }
        return assemble(stub, random);
    }

    /** The children of a stub already made from {@code random}. */
    public static List<ChamberPiece> assemble(Stub stub, Random random) {
        ChamberSettings s = ChamberSettings.get();
        List<ChamberPiece> pieces = new ArrayList<ChamberPiece>();
        ChamberPiece first = stub.start;
        pieces.add(first);
        if (s.maxDepth > 0) {
            BlockPos c = stub.position;
            int h = s.maxDistanceHorizontal;
            int v = s.maxDistanceVertical;
            int low = Math.max(c.getY() - v, MIN_Y + s.paddingBottom);
            // AABB max (exclusive) min(y + v + 1, maxY + 1 - top): the last block inside is one less.
            int high = Math.min(c.getY() + v + 1, MAX_Y + 1 - s.paddingTop) - 1;
            Space cube = new Space(new StructureBoundingBox(c.getX() - h, low, c.getZ() - h, c.getX() + h, high, c.getZ() + h));
            cube.taken.add(new StructureBoundingBox(first.getBoundingBox()));
            Placing placing = new Placing();
            Placer placer = new Placer(s.maxDepth, pieces, random, stub.aliases, placing);
            placer.tryPlacingChildren(first, cube, 0);
            State next;
            while ((next = placing.next()) != null) {
                placer.tryPlacingChildren(next.piece, next.free, next.depth);
            }
        }
        // StructureStart.placeInChunk: every piece's pivot is the first piece's middle at its floor.
        StructureBoundingBox b = first.getBoundingBox();
        BlockPos pivot = new BlockPos(b.minX + (b.maxX - b.minX + 1) / 2, b.minY, b.minZ + (b.maxZ - b.minZ + 1) / 2);
        for (ChamberPiece piece : pieces) {
            piece.setPivot(pivot);
        }
        return pieces;
    }

    // ---- free space: VoxelShapes of whole-block boxes

    static final class Space {
        @Nullable
        final StructureBoundingBox outside;
        final List<StructureBoundingBox> taken = new ArrayList<StructureBoundingBox>();

        Space(@Nullable StructureBoundingBox outside) {
            this.outside = outside;
        }

        /** "The box, shrunk by a quarter block, is all inside the free shape." */
        boolean fits(StructureBoundingBox box) {
            if (this.outside != null && !(this.outside.minX <= box.minX && box.maxX <= this.outside.maxX
                    && this.outside.minY <= box.minY && box.maxY <= this.outside.maxY
                    && this.outside.minZ <= box.minZ && box.maxZ <= this.outside.maxZ)) {
                return false;
            }
            for (StructureBoundingBox t : this.taken) {
                if (t.intersectsWith(box)) {
                    return false;
                }
            }
            return true;
        }
    }

    static final class State {
        final ChamberPiece piece;
        final Space free;
        final int depth;

        State(ChamberPiece piece, Space free, int depth) {
            this.piece = piece;
            this.free = free;
            this.depth = depth;
        }
    }

    /** {@code SequencedPriorityIterator}: first in, first out within a priority, the highest priority first. */
    static final class Placing {
        private final TreeMap<Integer, ArrayDeque<State>> queues = new TreeMap<Integer, ArrayDeque<State>>();

        void add(State s, int priority) {
            ArrayDeque<State> q = this.queues.get(priority);
            if (q == null) {
                q = new ArrayDeque<State>();
                this.queues.put(priority, q);
            }
            q.addLast(s);
        }

        @Nullable
        State next() {
            while (!this.queues.isEmpty()) {
                Map.Entry<Integer, ArrayDeque<State>> top = this.queues.lastEntry();
                State s = top.getValue().pollFirst();
                if (top.getValue().isEmpty()) {
                    this.queues.remove(top.getKey());
                }
                if (s != null) {
                    return s;
                }
            }
            return null;
        }
    }

    /** A jigsaw block of a placed element, with 1.21's priorities. */
    static final class Joint {
        final JigsawElement.Connector c;
        final int placementPriority;
        final int selectionPriority;

        Joint(JigsawElement.Connector c, int placementPriority, int selectionPriority) {
            this.c = c;
            this.placementPriority = placementPriority;
            this.selectionPriority = selectionPriority;
        }
    }

    static final Comparator<Joint> HIGHEST_SELECTION_PRIORITY_FIRST = Comparator.comparingInt((Joint j) -> j.selectionPriority).reversed();

    /** {@code StructurePoolElement.getShuffledJigsawBlocks}: shuffled, then stably sorted by selection priority. */
    static List<Joint> shuffledJoints(JigsawElement element, BlockPos pos, Rotation rotation, Random random) {
        List<JigsawElement.Connector> connectors = element.connectors(pos, rotation);
        List<Joint> joints = new ArrayList<Joint>(connectors.size());
        Map<BlockPos, int[]> priorities = priorities(element, pos, rotation);
        for (JigsawElement.Connector c : connectors) {
            int[] p = priorities.get(c.pos);
            joints.add(new Joint(c, p == null ? 0 : p[0], p == null ? 0 : p[1]));
        }
        Collections.shuffle(joints, random);
        joints.sort(HIGHEST_SELECTION_PRIORITY_FIRST);
        return joints;
    }

    /** Template-local jigsaw priorities, by template: local position to {placement, selection}. */
    private static final Map<ResourceLocation, Map<BlockPos, int[]>> PRIORITIES = new HashMap<ResourceLocation, Map<BlockPos, int[]>>();

    /** @return the priorities of the element's jigsaws, by their position for this placement */
    static Map<BlockPos, int[]> priorities(JigsawElement element, BlockPos pos, Rotation rotation) {
        ResourceLocation template = element.template();
        if (template == null) {
            return Collections.emptyMap();
        }
        Map<BlockPos, int[]> local = localPriorities(JigsawPools.templateId(template));
        if (local.isEmpty()) {
            return local;
        }
        TemplatePlacement placement = TemplatePlacement.of(rotation, Mirror.NONE);
        Map<BlockPos, int[]> out = new HashMap<BlockPos, int[]>();
        for (Map.Entry<BlockPos, int[]> e : local.entrySet()) {
            out.put(ModernTemplate.worldPos(pos, e.getKey(), placement), e.getValue());
        }
        return out;
    }

    static synchronized Map<BlockPos, int[]> localPriorities(ResourceLocation id) {
        Map<BlockPos, int[]> known = PRIORITIES.get(id);
        if (known != null) {
            return known;
        }
        Map<BlockPos, int[]> out = new HashMap<BlockPos, int[]>();
        for (String folder : new String[] {"structure", "structures"}) {
            String path = "/assets/" + id.getNamespace() + "/" + folder + "/" + id.getPath() + ".nbt";
            try (InputStream in = ChamberLayout.class.getResourceAsStream(path)) {
                if (in == null) {
                    continue;
                }
                NBTTagCompound tag = CompressedStreamTools.readCompressed(in);
                NBTTagList blocks = tag.getTagList("blocks", 10);
                for (int i = 0; i < blocks.tagCount(); i++) {
                    NBTTagCompound b = blocks.getCompoundTagAt(i);
                    if (!b.hasKey("nbt", 10)) {
                        continue;
                    }
                    NBTTagCompound nbt = b.getCompoundTag("nbt");
                    if (!"minecraft:jigsaw".equals(nbt.getString("id"))) {
                        continue;
                    }
                    int p = nbt.getInteger("placement_priority");
                    int q = nbt.getInteger("selection_priority");
                    if (p != 0 || q != 0) {
                        NBTTagList at = b.getTagList("pos", 3);
                        out.put(new BlockPos(at.getIntAt(0), at.getIntAt(1), at.getIntAt(2)), new int[] {p, q});
                    }
                }
            } catch (Exception e) {
                TrickyTrialsBackport.LOGGER.error("Could not read the jigsaw priorities of {}.", path, e);
            }
            break;
        }
        PRIORITIES.put(id, out);
        return out;
    }

    /** {@code JigsawPlacement.Placer}. */
    static final class Placer {
        final int maxDepth;
        final List<ChamberPiece> pieces;
        final Random random;
        final Map<String, String> aliases;
        final Placing placing;

        Placer(int maxDepth, List<ChamberPiece> pieces, Random random, Map<String, String> aliases, Placing placing) {
            this.maxDepth = maxDepth;
            this.pieces = pieces;
            this.random = random;
            this.aliases = aliases;
            this.placing = placing;
        }

        void tryPlacingChildren(ChamberPiece parent, Space outerFree, int depth) {
            JigsawElement element = parent.element();
            BlockPos position = parent.position();
            Rotation rotation = parent.rotation();
            boolean parentRigid = element.rigid();
            Space inner = null;
            StructureBoundingBox parentBox = parent.getBoundingBox();
            int parentMinY = parentBox.minY;

            connectorLoop:
            for (Joint joint : shuffledJoints(element, position, rotation, this.random)) {
                JigsawElement.Connector connector = joint.c;
                EnumFacing front = connector.front;
                BlockPos at = connector.pos;
                BlockPos target = at.offset(front);
                int connectorY = at.getY() - parentMinY;
                ResourceLocation poolId = new ResourceLocation(lookup(this.aliases, connector.pool));
                JigsawPools.Pool pool = JigsawPools.pool(poolId);
                if (pool == null || (pool.size() == 0 && !poolId.equals(JigsawPools.EMPTY))) {
                    TrickyTrialsBackport.LOGGER.warn("Empty or non-existent pool: {}", poolId);
                    continue;
                }
                ResourceLocation fallbackId = pool.fallback();
                JigsawPools.Pool fallback = JigsawPools.pool(fallbackId);
                if (fallback == null || (fallback.size() == 0 && !fallbackId.equals(JigsawPools.EMPTY))) {
                    TrickyTrialsBackport.LOGGER.warn("Empty or non-existent fallback pool: {}", fallbackId);
                    continue;
                }
                Space free;
                if (parentBox.isVecInside(target)) {
                    if (inner == null) {
                        inner = new Space(new StructureBoundingBox(parentBox));
                    }
                    free = inner;
                } else {
                    free = outerFree;
                }
                List<JigsawElement> candidates = new ArrayList<JigsawElement>();
                if (depth != this.maxDepth) {
                    candidates.addAll(shuffled(pool.elements(), this.random));
                }
                candidates.addAll(shuffled(fallback.elements(), this.random));
                int placementPriority = joint.placementPriority;

                for (JigsawElement candidate : candidates) {
                    if (candidate.isEmpty()) {
                        break;
                    }
                    for (Rotation r : shuffled(Arrays.asList(Rotation.values()), this.random)) {
                        List<Joint> theirs = shuffledJoints(candidate, BlockPos.ORIGIN, r, this.random);
                        // The expansion hack (use_expansion_hack) is off for trial chambers: nothing to measure.
                        for (Joint theirJoint : theirs) {
                            JigsawElement.Connector their = theirJoint.c;
                            if (!connector.canAttach(their)) {
                                continue;
                            }
                            BlockPos theirPos = their.pos;
                            BlockPos offset = target.subtract(theirPos);
                            StructureBoundingBox childBox = candidate.box(offset, r);
                            boolean childRigid = candidate.rigid();
                            int theirY = theirPos.getY();
                            int delta = connectorY - theirY + front.getYOffset();
                            int targetMinY;
                            if (parentRigid && childRigid) {
                                targetMinY = parentMinY + delta;
                            } else {
                                // No trial chamber piece is terrain matching; the joint's own height stands in.
                                targetMinY = at.getY() - theirY;
                            }
                            int dy = targetMinY - childBox.minY;
                            StructureBoundingBox moved = new StructureBoundingBox(childBox);
                            moved.offset(0, dy, 0);
                            BlockPos childPos = offset.add(0, dy, 0);
                            if (free.fits(moved)) {
                                free.taken.add(new StructureBoundingBox(moved));
                                int parentGround = parent.groundLevelDelta();
                                int childGround = childRigid ? parentGround - delta : candidate.groundLevelDelta();
                                ChamberPiece child = new ChamberPiece(candidate, childPos, childGround, r, moved);
                                int sourceGroundY;
                                if (parentRigid) {
                                    sourceGroundY = parentMinY + connectorY;
                                } else if (childRigid) {
                                    sourceGroundY = targetMinY + theirY;
                                } else {
                                    sourceGroundY = at.getY() + delta / 2;
                                }
                                parent.addJunction(target.getX(), sourceGroundY - connectorY + parentGround, target.getZ());
                                child.addJunction(at.getX(), sourceGroundY - theirY + childGround, at.getZ());
                                this.pieces.add(child);
                                if (depth + 1 <= this.maxDepth) {
                                    this.placing.add(new State(child, free, depth + 1), placementPriority);
                                }
                                continue connectorLoop;
                            }
                        }
                    }
                }
            }
        }
    }

    /** {@code Util.shuffledCopy}: a copy, shuffled with n - 1 draws (Collections.shuffle draws the same). */
    static <T> List<T> shuffled(List<T> in, Random random) {
        List<T> out = new ArrayList<T>(in);
        Collections.shuffle(out, random);
        return out;
    }
}
