package com.exiledradio.trickytrialsbackport.chambers;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernJigsaw;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.21.11's jigsaw assembly ({@code JigsawPlacement.addPieces} and its {@code Placer}) for trial chambers, draw for
 * draw: the start height, the start piece's rotation and element, then the children, each jigsaw in shuffled order
 * sorted by {@code selection_priority}, the pieces they open queued by {@code placement_priority}
 * ({@code SequencedPriorityIterator}), the pools looked up through the structure's {@code pool_aliases}, all inside a
 * box {@code max_distance_from_center} out from the start and kept {@code dimension_padding} off the world's floor and
 * roof (Backport Core's {@link ModernJigsaw}). Every chamber piece is rigid, so the ground is never asked.
 *
 * <p>The layout is always worked out in 1.21's own heights (y -64 to 319), so it is vanilla's piece for piece;
 * {@link TrialChambersStructure} then moves it to the world's start height. The stub is ours; the placer, pools,
 * elements, templates and pool aliases are Core's.
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
        Map<String, String> aliases = s.aliases.resolve(seed, origin);

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

    /**
     * The children of a stub already made from {@code random}: Backport Core's {@link ModernJigsaw} with 1.21's rules
     * (priorities, the stub's pool aliases, no expansion hack), kept {@code dimension_padding} off the floor and roof.
     */
    public static List<ChamberPiece> assemble(Stub stub, Random random) {
        ChamberSettings s = ChamberSettings.get();
        BlockPos c = stub.position;
        int h = s.maxDistanceHorizontal;
        int v = s.maxDistanceVertical;
        int low = Math.max(c.getY() - v, MIN_Y + s.paddingBottom);
        // AABB max (exclusive) min(y + v + 1, maxY + 1 - top): the last block inside is one less.
        int high = Math.min(c.getY() + v + 1, MAX_Y + 1 - s.paddingTop) - 1;
        return ModernJigsaw.of(ChamberPiece::new).maxDepth(s.maxDepth).priorities(true).aliases(stub.aliases)
                .assemble(stub.start, new StructureBoundingBox(c.getX() - h, low, c.getZ() - h, c.getX() + h, high, c.getZ() + h), random);
    }
}
