package com.exiledradio.trickytrialsbackport.chambers;

import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.terrain.WorldHeights;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraftforge.event.terraingen.ChunkGeneratorEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.21's {@code terrain_adaptation: encapsulate} for a terrain that cannot add it to its own density (1.12.2's, and
 * Caves &amp; Cliffs' 1.18 generator, whose Beardifier has no encapsulate). 1.21.11's {@code Beardifier.compute}
 * gives every rigid chamber piece {@code getBuryContribution(dx / 2, dy / 2, dz / 2) * 0.8}, the distances outside
 * its box, and every junction {@code getBeardContribution * 0.4}; the terrain adds that to its density, so where a
 * chamber reaches the sea or a cave it is wrapped in rock rather than left open. Here the sum is added, exactly, to a
 * density read off the blocks already there, {@link #ROCK} for rock and minus it for open space (air or liquid), as
 * Wild Update Backport's ancient city beard does: open space within about five blocks of a piece becomes rock, and a
 * junction's slope carves a little rock away above its ground.
 *
 * <p>Without it, a chamber whose top rises above the sea bed (1.21's layouts reach 70 blocks above the start, which
 * 1.12.2's shallower world puts at the sea bed or above) stands bare in the water.
 */
public final class ChamberShell {

    /** {@code squeeze(1)}: 1/2 - 1/24, solid rock's final density. */
    static final double ROCK = 0.5 - 1.0 / 24.0;

    /** 1.21.11's BEARD_KERNEL: exp(-lengthSquared(x, y + 0.5, z) / 16), x, y and z -12..11, as floats. */
    private static final float[] KERNEL = new float[24 * 24 * 24];

    static {
        for (int i = 0; i < 24; i++) {
            for (int j = 0; j < 24; j++) {
                for (int k = 0; k < 24; k++) {
                    double x = j - 12;
                    double y = (double) (k - 12) + 0.5D;
                    double z = i - 12;
                    KERNEL[i * 24 * 24 + j * 24 + k] = (float) Math.pow(Math.E, -(x * x + y * y + z * z) / 16.0D);
                }
            }
        }
    }

    private ChamberShell() {
    }

    /** Where a shell reads and writes blocks: the world, or a chunk's primer. */
    interface Blocks2 {
        IBlockState get(int x, int y, int z);

        void set(int x, int y, int z, IBlockState state);
    }

    /** @return true where the chunk generator is Caves &amp; Cliffs' 1.18 terrain, which has no 1.12 primer */
    static boolean modern(World world) {
        if (!(world instanceof WorldServer)) {
            return false;
        }
        Object generator = ((WorldServer) world).getChunkProvider().chunkGenerator;
        return generator != null && generator.getClass().getName().equals("com.exiledradio.cavesandcliffsbackport.terrain.ModernGenerator");
    }

    private static Boolean terrainEncapsulates;

    /** @return true where the 1.18 terrain's Beardifier encapsulates itself (an ENCAPSULATE effect), so nothing is left to do */
    static synchronized boolean terrainEncapsulates(World world) {
        if (!modern(world)) {
            return false;
        }
        if (terrainEncapsulates == null) {
            boolean found;
            try {
                Class.forName("com.exiledradio.cavesandcliffsbackport.terrain.Beardifier", false, ChamberShell.class.getClassLoader())
                        .getField("ENCAPSULATE");
                found = true;
            } catch (ReflectiveOperationException | LinkageError e) {
                found = false;
            }
            terrainEncapsulates = found;
        }
        return terrainEncapsulates;
    }

    /**
     * 1.21.11's Beardifier sum at a block over chambers' pieces, every rigid one {@code encapsulate}, the junctions those
     * of its chunk's Beardifier (sources inside the chunk grown by 12, exclusive).
     */
    public static double density(List<? extends StructureComponent> components, int x, int y, int z) {
        List<int[]> rigids = new ArrayList<int[]>();
        List<int[]> junctions = new ArrayList<int[]>();
        collect(components, null, rigids, junctions);
        return compute(rigids, junctions, x, y, z);
    }

    /**
     * Wraps a chunk as 1.12.2's generator makes it (Forge's {@code ReplaceBiomeBlocks}, before the surface, the carvers
     * and decoration), the nearest 1.12 has to 1.21 adding it to the noise: the biome's surface then covers it as 1.21's
     * surface rules cover its rock.
     */
    public static final class PrimerShell {
        @SubscribeEvent
        public void replaceBiomeBlocks(ChunkGeneratorEvent.ReplaceBiomeBlocks event) {
            World world = event.getWorld();
            ChunkPrimer primer = event.getPrimer();
            if (world == null || world.isRemote || primer == null || world.provider.getDimension() != 0 || !ChambersContent.generates()
                    || modern(world) || WorldHeights.minY(world) < 0) {
                return;
            }
            ScatteredStructure s = ScatteredStructure.of(world, Structures.TRIAL_CHAMBERS);
            if (!(s instanceof TrialChambersStructure)) {
                return;
            }
            List<StructureComponent> pieces = new ArrayList<StructureComponent>();
            ((TrialChambersStructure) s).startsNear(world, event.getX(), event.getZ(), start -> pieces.addAll(start.getComponents()));
            if (pieces.isEmpty()) {
                return;
            }
            int bx = event.getX() << 4;
            int bz = event.getZ() << 4;
            wrap(pieces, new StructureBoundingBox(bx, 0, bz, bx + 15, 255, bz + 15), 1, 255, false, new Blocks2() {
                @Override
                public IBlockState get(int x, int y, int z) {
                    return primer.getBlockState(x - bx, y, z - bz);
                }

                @Override
                public void set(int x, int y, int z, IBlockState state) {
                    primer.setBlockState(x - bx, y, z - bz, state);
                }
            });
        }
    }

    /**
     * Wraps one populate area (x and z of {@code area}) in the world, for a terrain that neither encapsulates nor lets
     * this into its primer. Before the pieces are placed.
     */
    static void wrap(World world, List<StructureComponent> components, StructureBoundingBox area) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        wrap(components, area, WorldHeights.minY(world) + 1, WorldHeights.maxYExclusive(world) - 1, WorldHeights.minY(world) < 0, new Blocks2() {
            @Override
            public IBlockState get(int x, int y, int z) {
                return world.getBlockState(p.setPos(x, y, z));
            }

            @Override
            public void set(int x, int y, int z, IBlockState state) {
                world.setBlockState(p.setPos(x, y, z), state, 2);
            }
        });
    }

    private static void collect(List<? extends StructureComponent> components, StructureBoundingBox area, List<int[]> rigids, List<int[]> junctions) {
        for (StructureComponent c : components) {
            StructureBoundingBox b = c.getBoundingBox();
            if (area != null && (b.maxX + 12 < area.minX || b.minX - 12 > area.maxX || b.maxZ + 12 < area.minZ || b.minZ - 12 > area.maxZ)) {
                continue;
            }
            if (c instanceof ChamberPiece) {
                ChamberPiece piece = (ChamberPiece) c;
                if (piece.rigid()) {
                    rigids.add(new int[] {b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ});
                }
                int[] j = piece.junctions();
                for (int n = 0; n < j.length; n += 3) {
                    junctions.add(new int[] {j[n], j[n + 1], j[n + 2]});
                }
            } else {
                rigids.add(new int[] {b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ});
            }
        }
    }

    private static Block deepslate;
    private static boolean deepslateLooked;

    /** 1.21's rock at a height: deepslate below y 0 in a world that goes that deep (where there is any), else stone. */
    private static IBlockState rock(boolean extended, int y) {
        if (extended && y < 0) {
            if (!deepslateLooked) {
                deepslateLooked = true;
                Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesandcliffsbackport", "deepslate"));
                deepslate = b == Blocks.AIR ? null : b;
            }
            if (deepslate != null) {
                return deepslate.getDefaultState();
            }
        }
        return Blocks.STONE.getDefaultState();
    }

    static void wrap(List<StructureComponent> components, StructureBoundingBox area, int minY, int maxY, boolean extended, Blocks2 blocks) {
        List<int[]> rigids = new ArrayList<int[]>();
        List<int[]> junctions = new ArrayList<int[]>();
        collect(components, area, rigids, junctions);
        if (rigids.isEmpty() && junctions.isEmpty()) {
            return;
        }
        int lowY = Integer.MAX_VALUE;
        int highY = Integer.MIN_VALUE;
        for (int[] r : rigids) {
            lowY = Math.min(lowY, r[1] - 11);
            highY = Math.max(highY, r[4] + 11);
        }
        for (int[] j : junctions) {
            lowY = Math.min(lowY, j[1] - 12);
            highY = Math.max(highY, j[1] + 11);
        }
        lowY = Math.max(lowY, minY);
        highY = Math.min(highY, maxY);
        IBlockState air = Blocks.AIR.getDefaultState();
        for (int x = area.minX; x <= area.maxX; x++) {
            for (int z = area.minZ; z <= area.maxZ; z++) {
                for (int y = lowY; y <= highY; y++) {
                    double shell = compute(rigids, junctions, x, y, z);
                    if (shell == 0.0D) {
                        continue;
                    }
                    IBlockState s = blocks.get(x, y, z);
                    if (s.getBlock() == Blocks.BEDROCK || ChamberPiece.cannotReplace().contains(s.getBlock())) {
                        continue;
                    }
                    Material m = s.getMaterial();
                    boolean open = m == Material.AIR || m.isLiquid();
                    if (open && shell - ROCK > 0.0D) {
                        blocks.set(x, y, z, rock(extended, y));
                    } else if (!open && ROCK + shell < 0.0D) {
                        blocks.set(x, y, z, air);
                    }
                }
            }
        }
    }

    /** 1.21.11's {@code Beardifier.compute} at a block: every rigid piece ENCAPSULATE, then the junctions. */
    static double compute(List<int[]> rigids, List<int[]> junctions, int x, int y, int z) {
        double d = 0.0D;
        for (int[] r : rigids) {
            int dx = Math.max(0, Math.max(r[0] - x, x - r[3]));
            int dz = Math.max(0, Math.max(r[2] - z, z - r[5]));
            int dy = Math.max(0, Math.max(r[1] - y, y - r[4]));
            d += bury((double) dx / 2.0D, (double) dy / 2.0D, (double) dz / 2.0D) * 0.8D;
        }
        int cx = x >> 4 << 4;
        int cz = z >> 4 << 4;
        for (int[] j : junctions) {
            if (j[0] > cx - 12 && j[2] > cz - 12 && j[0] < cx + 15 + 12 && j[2] < cz + 15 + 12) {
                int dy = y - j[1];
                d += beard(x - j[0], dy, z - j[2], dy) * 0.4D;
            }
        }
        return d;
    }

    /** 1.21.11's {@code getBuryContribution}: {@code clampedMap(length, 0, 6, 1, 0)}. */
    static double bury(double x, double y, double z) {
        double len = Math.sqrt(x * x + y * y + z * z);
        return len >= 6.0D ? 0.0D : 1.0D - len / 6.0D;
    }

    /** 1.21.11's {@code getBeardContribution}. */
    static double beard(int x, int y, int z, int yToGround) {
        int i = x + 12;
        int j = y + 12;
        int k = z + 12;
        if (i < 0 || i >= 24 || j < 0 || j >= 24 || k < 0 || k >= 24) {
            return 0.0D;
        }
        double d = (double) yToGround + 0.5D;
        double e = (double) x * (double) x + d * d + (double) z * (double) z;
        double f = -d * fastInvSqrt(e / 2.0D) / 2.0D;
        return f * (double) KERNEL[k * 24 * 24 + i * 24 + j];
    }

    /** {@code Mth.fastInvSqrt}. */
    private static double fastInvSqrt(double v) {
        double half = 0.5D * v;
        long i = Double.doubleToRawLongBits(v);
        i = 6910469410427058090L - (i >> 1);
        v = Double.longBitsToDouble(i);
        return v * (1.5D - half * v * v);
    }
}
