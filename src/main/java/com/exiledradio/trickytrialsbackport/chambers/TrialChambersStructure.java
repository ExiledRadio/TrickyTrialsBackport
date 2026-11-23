package com.exiledradio.trickytrialsbackport.chambers;

import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.StructureRandoms;
import com.exiledradio.backportcore.terrain.TerrainHeights;
import com.exiledradio.backportcore.terrain.WorldHeights;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * 1.21's trial chambers: structure set {@code trial_chambers} (its {@code random_spread} grid, read from vanilla's file
 * by {@link ChamberSettings}), the jigsaw structure of {@link ChamberLayout}, valid where the stub's noise biome is in
 * {@code #has_structure/trial_chambers} ({@link ChamberBiomes}). Built from the populate step by Backport Core's
 * scattered driver, so it works on any terrain.
 *
 * <p>Heights (decided 2026-11-07): in a world that goes down to y -64 (Caves &amp; Cliffs' extended height) the
 * structure is where 1.21 puts it. In a world that starts at y 0 the start height is mapped proportionally,
 * {@code (y + 64) * 256 / 384}: 1.21's y -40 to -20 become y 16 to 29, and the whole layout moves with the start, so
 * it is still vanilla's piece for piece.
 *
 * <p>The mapping squeezes 1.21's depth but not the layout, so a tall layout started high can reach y 60 and more,
 * where 1.21's never come near the surface. Where its top would reach the bare terrain's ground (or a sea bed) less
 * than {@value #COVER} blocks over it, the whole layout goes down until it does not ({@link #bury}): flat land
 * near sea level, as Realistic Terrain Generation makes, otherwise showed its top floor in the grass. A layout too tall
 * to go under the ground there at all (a sea bed, or low land over a tall layout) does not start; generation,
 * {@code /locate} and the explorer map all ask {@link #canStartAt}, so they agree.
 *
 * <p>Where a piece still meets open space (a sea bed in a world that keeps 1.21's heights, a cave or an aquifer),
 * 1.21's {@code encapsulate} wraps it in rock ({@link ChamberShell}).
 */
public final class TrialChambersStructure extends ScatteredStructure {

    public static final String SAVE_NAME = "TTB_TrialChambers";

    public TrialChambersStructure() {
        super(SAVE_NAME, ChamberSettings.get().grid);
        // A start is laid out by every chunk populated within this many chunks of it. 1.21's pieces stay inside a box
        // 116 blocks out from the start piece's middle, which may be up to nine chunks from the start chunk.
        this.range = 9;
    }

    /** @return how far 1.21's heights move in this world: 0 where it goes down to y -64, else the proportional shift */
    public static int shift(World world, int startY121) {
        int min = WorldHeights.minY(world);
        if (min <= ChamberLayout.MIN_Y) {
            return 0;
        }
        int height = WorldHeights.maxYExclusive(world) - min;
        int mapped = min + Math.floorDiv((startY121 - ChamberLayout.MIN_Y) * height, ChamberLayout.MAX_Y + 1 - ChamberLayout.MIN_Y);
        return mapped - startY121;
    }

    /** 1.21's stub for this chunk, from a fresh start random, or null. */
    public static ChamberLayout.Stub stub(World world, int chunkX, int chunkZ) {
        Random random = StructureRandoms.start(world.getSeed(), chunkX, chunkZ);
        return ChamberLayout.stub(world.getSeed(), chunkX, chunkZ, random);
    }

    @Override
    protected boolean canStartAt(World world, int chunkX, int chunkZ) {
        if (!ChambersContent.generates()) {
            return false;
        }
        ChamberLayout.Stub stub = stub(world, chunkX, chunkZ);
        if (stub == null) {
            return false;
        }
        BlockPos p = stub.position;
        return ChamberBiomes.valid(world, new BlockPos(p.getX(), p.getY() + shift(world, stub.startY), p.getZ()))
                && !exposed(world, chunkX, chunkZ);
    }

    /** True while this places starts (iterating its start map), during which a new chunk must not add to the map. */
    private boolean building;

    @Override
    public synchronized boolean generateStructure(World world, Random random, ChunkPos chunk) {
        this.building = true;
        try {
            return super.generateStructure(world, random, chunk);
        } finally {
            this.building = false;
        }
    }

    /** How far, in chunks, a start's pieces and the terrain's 12-block reach around them go from its start chunk. */
    private static final int REACH_CHUNKS = 10;

    /**
     * Backport Core's terrain adaptation planner: every start whose box, grown by 12 blocks, reaches chunk (cx, cz),
     * laid out (and kept) as the populate step will lay it out. Loads no chunks.
     */
    public void startsNear(World world, int cx, int cz, Consumer<StructureStart> out) {
        this.world = world;
        if (!ChambersContent.generates()) {
            return;
        }
        int s = placement().spacing();
        for (int gx = Math.floorDiv(cx - REACH_CHUNKS, s); gx <= Math.floorDiv(cx + REACH_CHUNKS, s); gx++) {
            for (int gz = Math.floorDiv(cz - REACH_CHUNKS, s); gz <= Math.floorDiv(cz + REACH_CHUNKS, s); gz++) {
                ChunkPos c = placement().candidate(world.getSeed(), gx * s, gz * s);
                if (Math.abs(c.x - cx) > REACH_CHUNKS || Math.abs(c.z - cz) > REACH_CHUNKS) {
                    continue;
                }
                long key = ChunkPos.asLong(c.x, c.z);
                StructureStart start = this.structureMap.get(key);
                if (start == null) {
                    if (!canSpawnStructureAtCoords(c.x, c.z)) {
                        continue;
                    }
                    if (this.building) {
                        start = getStructureStart(c.x, c.z);
                    } else {
                        generate(world, c.x, c.z, null);
                        start = this.structureMap.get(key);
                    }
                }
                if (start != null && start.isSizeableStructure() && reaches(start.getBoundingBox(), cx, cz)) {
                    out.accept(start);
                }
            }
        }
    }

    static boolean reaches(StructureBoundingBox b, int cx, int cz) {
        int x = cx << 4;
        int z = cz << 4;
        return b != null && b.maxX + 12 >= x && b.minX - 12 <= x + 15 && b.maxZ + 12 >= z && b.minZ - 12 <= z + 15;
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        return new Start(this.world, chunkX, chunkZ);
    }

    /** The pieces of the chambers starting in this chunk, at this world's heights. Loads nothing. */
    public static List<ChamberPiece> layout(World world, int chunkX, int chunkZ) {
        Random random = StructureRandoms.start(world.getSeed(), chunkX, chunkZ);
        ChamberLayout.Stub stub = ChamberLayout.stub(world.getSeed(), chunkX, chunkZ, random);
        if (stub == null) {
            return new java.util.ArrayList<ChamberPiece>();
        }
        List<ChamberPiece> pieces = ChamberLayout.assemble(stub, random);
        int dy = shift(world, stub.startY);
        if (dy != 0) {
            dy += fit(world, chunkX, chunkZ, pieces, dy)[0];
            for (ChamberPiece p : pieces) {
                p.offset(0, dy, 0);
            }
        }
        return pieces;
    }

    /**
     * @return how far the layout starting in this chunk is moved up from 1.21's heights: {@link #shift}, then down
     *         under the ground ({@link #bury}); 0 without a start
     */
    public static int moved(World world, int chunkX, int chunkZ) {
        Random random = StructureRandoms.start(world.getSeed(), chunkX, chunkZ);
        ChamberLayout.Stub stub = ChamberLayout.stub(world.getSeed(), chunkX, chunkZ, random);
        if (stub == null) {
            return 0;
        }
        int dy = shift(world, stub.startY);
        return dy == 0 ? 0 : dy + fit(world, chunkX, chunkZ, ChamberLayout.assemble(stub, random), dy)[0];
    }

    /** @return whether the layout starting in this chunk would break through the ground even moved down ({@link #bury}) */
    public static boolean exposed(World world, int chunkX, int chunkZ) {
        Random random = StructureRandoms.start(world.getSeed(), chunkX, chunkZ);
        ChamberLayout.Stub stub = ChamberLayout.stub(world.getSeed(), chunkX, chunkZ, random);
        if (stub == null) {
            return false;
        }
        int dy = shift(world, stub.startY);
        if (dy == 0) {
            return false;
        }
        int[] fit = FITS.cached(world, chunkX, chunkZ);
        if (fit == null) {
            fit = fit(world, chunkX, chunkZ, ChamberLayout.assemble(stub, random), dy);
        }
        return fit[1] != 0;
    }

    /** The last layouts' fits per world, as {@link #bury} gives them: a start is asked about, then laid out. */
    private static final Fits FITS = new Fits();

    private static final class Fits {
        private final Map<World, Map<Long, int[]>> byWorld = new WeakHashMap<World, Map<Long, int[]>>();

        synchronized int[] cached(World world, int chunkX, int chunkZ) {
            Map<Long, int[]> fits = this.byWorld.get(world);
            return fits == null ? null : fits.get(ChunkPos.asLong(chunkX, chunkZ));
        }

        synchronized void put(World world, int chunkX, int chunkZ, int[] fit) {
            Map<Long, int[]> fits = this.byWorld.get(world);
            if (fits == null) {
                fits = new LinkedHashMap<Long, int[]>(64, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
                        return size() > 256;
                    }
                };
                this.byWorld.put(world, fits);
            }
            fits.put(ChunkPos.asLong(chunkX, chunkZ), fit);
        }
    }

    private static int[] fit(World world, int chunkX, int chunkZ, List<ChamberPiece> pieces, int dy) {
        int[] fit = FITS.cached(world, chunkX, chunkZ);
        if (fit == null) {
            fit = bury(world, pieces, dy);
            FITS.put(world, chunkX, chunkZ, fit);
        }
        return fit;
    }

    /** Blocks of ground kept over every piece of a layout moved to this world's heights. */
    public static final int COVER = 6;

    /**
     * @return how much further down (zero or less) a layout moved by {@code dy} must go for every piece to have
     *         {@link #COVER} blocks of the bare terrain over it, never below a block over the world's bottom; then 1
     *         if a piece still reaches the ground there, else 0. {0, 0} where the terrain cannot be predicted
     */
    static int[] bury(World world, List<ChamberPiece> pieces, int dy) {
        TerrainHeights terrain = TerrainHeights.forWorld(world);
        if (terrain == null) {
            return new int[] {0, 0};
        }
        int down = 0;
        int lowest = Integer.MAX_VALUE;
        for (ChamberPiece p : pieces) {
            StructureBoundingBox b = p.getBoundingBox();
            lowest = Math.min(lowest, b.minY + dy);
            int top = b.maxY + dy;
            // Every column of the piece.
            for (int x = b.minX; ; x++) {
                for (int z = b.minZ; ; z++) {
                    int ground = terrain.floor(x, z) - 1;
                    down = Math.max(down, top + COVER - ground);
                    if (z == b.maxZ) {
                        break;
                    }
                }
                if (x == b.maxX) {
                    break;
                }
            }
        }
        int extra = -Math.max(0, Math.min(down, lowest - (WorldHeights.minY(world) + 1)));
        // down - COVER is how far the highest piece top reaches above its ground; moved by extra, at or above is open.
        return new int[] {extra, down - COVER + extra >= 0 ? 1 : 0};
    }

    public static final class Start extends StructureStart {
        /** For loading. */
        public Start() {
        }

        Start(World world, int chunkX, int chunkZ) {
            super(chunkX, chunkZ);
            this.components.addAll(layout(world, chunkX, chunkZ));
            updateBoundingBox();
        }

        @Override
        public void generateStructure(World world, Random random, StructureBoundingBox box) {
            // 1.12.2's MapGenStructure clips each chunk to y 1-255; a world that goes down to -64 builds the whole height.
            StructureBoundingBox chunk = new StructureBoundingBox(box.minX, Math.min(box.minY, WorldHeights.minY(world)), box.minZ,
                    box.maxX, Math.max(box.maxY, WorldHeights.maxYExclusive(world) - 1), box.maxZ);
            try (AutoCloseable full = WorldHeights.enterFullHeight()) {
                QuietGen.run(world, () -> {
                    // 1.12.2's terrain got the shell in its primer (ChamberShell.PrimerShell); a 1.18 terrain that
                    // does not encapsulate gets it here, before the pieces.
                    if (ChamberShell.modern(world) && !ChamberShell.terrainEncapsulates(world)) {
                        ChamberShell.wrap(world, this.components, chunk);
                    }
                    super.generateStructure(world, random, chunk);
                });
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /** @return the pieces, start piece first */
        public List<StructureComponent> pieces() {
            return this.components;
        }
    }
}
