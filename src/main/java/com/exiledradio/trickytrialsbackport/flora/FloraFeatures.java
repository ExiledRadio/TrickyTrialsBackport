package com.exiledradio.trickytrialsbackport.flora;

import com.exiledradio.backportcore.terrain.WorldHeights;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

import java.util.Random;

/**
 * Every placed feature of this stream, as 1.21.11 places it (1.21.5's numbers are the same, except that 1.21.11's
 * leaf litter patch also wants grass below), with no dependence on how the world's terrain or biomes were made or on
 * its height. 1.12.2's decoration ({@link FloraGen}) and any other terrain (Caves &amp; Cliffs' 1.18 generator) call
 * the same code:
 *
 * <ul>
 *   <li>{@link #vegetation(World, Random, BlockPos, String)}: every patch the biome has, in vanilla's order.</li>
 *   <li>{@link #place}: one placed feature by its vanilla name.</li>
 *   <li>{@link FallenTrees}: fallen trees and the leaf litter under trees, for a terrain's own tree selectors.</li>
 * </ul>
 *
 * <p>Random draws follow vanilla's placement chain step for step (count, rarity, {@code in_square}, heightmap, biome
 * filter, then the feature's own draws), so the same random gives the same plants.
 */
public final class FloraFeatures {

    /** Worldgen block flags: send to clients, no neighbor updates (vanilla's 2). */
    static final int FLAGS = 2;

    /** {@code Biome.BIOME_INFO_NOISE}: one octave of simplex noise seeded 2345 (1.12.2's grass color noise). */
    private static final NoiseGeneratorPerlin INFO_NOISE = new NoiseGeneratorPerlin(new Random(2345L), 1);

    private FloraFeatures() {
    }

    /**
     * Places every patch of this stream that the biome has, in 1.21.5's order. Tree features (fallen trees, leaf
     * litter under trees) are left to {@link FallenTrees}, since they belong to the terrain's own tree selectors, and
     * the cactus patches to {@link #place}: 1.21.5's {@code patch_cactus_desert} and {@code patch_cactus_decorated} are
     * the older cactus patches with flowers added, so a terrain that places its own calls {@code place} instead.
     *
     * @param origin the north-west corner of the 16x16 square positions are drawn in (1.12.2's decoration: the chunk
     *               corner + 8; a modern generator: the chunk corner); y is ignored
     * @param biome  the biome's 1.21.5 name ({@link FloraBiomes#modernName})
     */
    public static void vegetation(World world, Random rand, BlockPos origin, String biome) {
        if (!FloraContent.enabled() || FloraContent.bush == null) {
            return;
        }
        for (String feature : FloraBiomes.features(biome)) {
            if (!feature.startsWith("[") && !feature.startsWith("patch_cactus")) {
                place(world, rand, origin, feature);
            }
        }
    }

    /**
     * Places one of this stream's placed features by its 1.21.5 name ({@code patch_bush},
     * {@code patch_firefly_bush_near_water}, {@code patch_firefly_bush_swamp}, {@code patch_firefly_bush_near_water_swamp},
     * {@code patch_dry_grass_desert}, {@code patch_dry_grass_badlands}, {@code wildflowers_birch_forest},
     * {@code wildflowers_meadow}, {@code patch_leaf_litter}, {@code patch_cactus_desert}, {@code patch_cactus_decorated}).
     *
     * @return false for a name this stream does not place
     */
    public static boolean place(World world, Random rand, BlockPos origin, String feature) {
        if (FloraContent.bush == null) {
            return false;
        }
        switch (feature) {
            case "patch_bush":
                if (rarity(rand, 4)) {
                    BlockPos p = filtered(world, motionBlocking(world, square(rand, origin), false), feature);
                    if (p != null) {
                        patch(world, rand, p, 24, 5, 3, Kind.BUSH);
                    }
                }
                return true;
            case "patch_firefly_bush_near_water":
                fireflyNearWater(world, rand, origin, feature, 2, true);
                return true;
            case "patch_firefly_bush_near_water_swamp":
                fireflyNearWater(world, rand, origin, feature, 3, false);
                return true;
            case "patch_firefly_bush_swamp":
                if (rarity(rand, 8)) {
                    BlockPos p = filtered(world, motionBlocking(world, square(rand, origin), false), feature);
                    if (p != null) {
                        patch(world, rand, p, 20, 4, 3, Kind.FIREFLY_BUSH);
                    }
                }
                return true;
            case "patch_dry_grass_desert":
            case "patch_dry_grass_badlands":
                if (rarity(rand, feature.endsWith("desert") ? 3 : 6)) {
                    BlockPos p = filtered(world, motionBlocking(world, square(rand, origin), false), feature);
                    if (p != null) {
                        patch(world, rand, p, 64, 7, 3, Kind.DRY_GRASS);
                    }
                }
                return true;
            case "wildflowers_birch_forest":
                for (int i = 0; i < 3; i++) {
                    if (rarity(rand, 2)) {
                        BlockPos p = filtered(world, motionBlocking(world, square(rand, origin), false), feature);
                        if (p != null) {
                            patch(world, rand, p, 64, 6, 2, Kind.WILDFLOWERS);
                        }
                    }
                }
                return true;
            case "wildflowers_meadow": {
                // noise_threshold_count: below -0.8 five, else ten.
                double noise = INFO_NOISE.getValue(origin.getX() / 200.0D, origin.getZ() / 200.0D);
                int count = noise < -0.8D ? 5 : 10;
                for (int i = 0; i < count; i++) {
                    BlockPos p = filtered(world, motionBlocking(world, square(rand, origin), false), feature);
                    if (p != null) {
                        patch(world, rand, p, 8, 6, 2, Kind.WILDFLOWERS);
                    }
                }
                return true;
            }
            case "patch_leaf_litter":
                for (int i = 0; i < 2; i++) {
                    BlockPos p = filtered(world, worldSurface(world, square(rand, origin)), feature);
                    if (p != null) {
                        patch(world, rand, p, 32, 7, 3, Kind.LEAF_LITTER);
                    }
                }
                return true;
            case "patch_cactus_desert":
            case "patch_cactus_decorated":
                if (rarity(rand, feature.endsWith("desert") ? 6 : 13)) {
                    BlockPos p = filtered(world, motionBlocking(world, square(rand, origin), false), feature);
                    if (p != null) {
                        patch(world, rand, p, 10, 7, 3, Kind.CACTUS);
                    }
                }
                return true;
            default:
                return false;
        }
    }

    /**
     * {@code patch_firefly_bush_near_water(_swamp)}: count, {@code in_square}, the heightmap, the biome, then only where
     * a firefly bush could stand in air with water beside the block under it.
     */
    private static void fireflyNearWater(World world, Random rand, BlockPos origin, String feature, int count, boolean noLeaves) {
        for (int i = 0; i < count; i++) {
            BlockPos p = filtered(world, motionBlocking(world, square(rand, origin), noLeaves), feature);
            if (p != null && world.isAirBlock(p) && FloraContent.fireflyBush.canBlockStay(world, p, FloraContent.fireflyBush.getDefaultState())
                    && waterBeside(world, p.down())) {
                patch(world, rand, p, 20, 4, 3, Kind.FIREFLY_BUSH);
            }
        }
    }

    // ===========================================================================================
    // Random patch and its blocks
    // ===========================================================================================

    public enum Kind {
        BUSH, FIREFLY_BUSH, DRY_GRASS, WILDFLOWERS, LEAF_LITTER, CACTUS
    }

    /** {@code RandomPatchFeature}: tries at a triangular spread around the origin, each through the inner filter. */
    public static void patch(World world, Random rand, BlockPos origin, int tries, int xz, int y, Kind kind) {
        for (int i = 0; i < tries; i++) {
            BlockPos p = origin.add(rand.nextInt(xz + 1) - rand.nextInt(xz + 1), rand.nextInt(y + 1) - rand.nextInt(y + 1),
                    rand.nextInt(xz + 1) - rand.nextInt(xz + 1));
            if (!isLoaded(world, p) || !world.isAirBlock(p)) {
                continue;
            }
            switch (kind) {
                case BUSH:
                    simple(world, p, FloraContent.bush.getDefaultState());
                    break;
                case FIREFLY_BUSH:
                    simple(world, p, FloraContent.fireflyBush.getDefaultState());
                    break;
                case DRY_GRASS:
                    // weighted_state_provider: short 1, tall 1.
                    simple(world, p, (rand.nextInt(2) == 0 ? FloraContent.shortDryGrass : FloraContent.tallDryGrass).getDefaultState());
                    break;
                case WILDFLOWERS:
                    simple(world, p, segmented(FloraContent.wildflowers, rand.nextInt(16)));
                    break;
                case LEAF_LITTER:
                    if (world.getBlockState(p.down()).getBlock() == Blocks.GRASS) {
                        simple(world, p, segmented(FloraContent.leafLitter, rand.nextInt(12)));
                    }
                    break;
                case CACTUS:
                    if (Blocks.CACTUS.canBlockStay(world, p)) {
                        cactusColumn(world, rand, p);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /** The weighted list's entry: north, east, south, west at one, then the same at two, ... */
    static IBlockState segmented(BlockSegmented block, int index) {
        EnumFacing[] order = {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};
        return block.getDefaultState().withProperty(BlockSegmented.FACING, order[index & 3]).withProperty(block.amount(), 1 + (index >> 2));
    }

    /** {@code SimpleBlockFeature}: the state, where it can stand. */
    static boolean simple(World world, BlockPos pos, IBlockState state) {
        Block b = state.getBlock();
        boolean survives = b instanceof BlockVegetation ? ((BlockVegetation) b).canBlockStay(world, pos, state) : b.canPlaceBlockAt(world, pos);
        if (!survives) {
            return false;
        }
        world.setBlockState(pos, state, FLAGS);
        return true;
    }

    /**
     * {@code patch_cactus}'s {@code block_column}: 1-3 cacti (biased to the bottom) and, one time in four, a cactus
     * flower on top; the column is cut from the top where the block above a layer is not air (the flower first).
     */
    static void cactusColumn(World world, Random rand, BlockPos origin) {
        int cacti = 1 + rand.nextInt(rand.nextInt(3) + 1);
        int flowers = rand.nextInt(4) < 3 ? 0 : 1;
        int total = cacti + flowers;
        for (int i = 0; i < total; i++) {
            if (!world.isAirBlock(origin.up(1 + i))) {
                int remove = total - i;
                int cut = Math.min(flowers, remove);
                flowers -= cut;
                remove -= cut;
                cacti -= Math.min(cacti, remove);
                break;
            }
        }
        BlockPos p = origin;
        for (int i = 0; i < cacti; i++) {
            world.setBlockState(p, Blocks.CACTUS.getDefaultState(), FLAGS);
            p = p.up();
        }
        if (flowers > 0) {
            world.setBlockState(p, FloraContent.cactusFlower.getDefaultState(), FLAGS);
        }
    }

    // ===========================================================================================
    // Placement modifiers and heightmaps
    // ===========================================================================================

    /** {@code rarity_filter}: one time in {@code chance}. */
    static boolean rarity(Random rand, int chance) {
        return rand.nextFloat() < 1.0F / chance;
    }

    /** {@code in_square}. */
    static BlockPos square(Random rand, BlockPos origin) {
        int x = rand.nextInt(16) + origin.getX();
        int z = rand.nextInt(16) + origin.getZ();
        return new BlockPos(x, origin.getY(), z);
    }

    /** The {@code biome} filter: the position, if its biome has the feature. */
    static BlockPos filtered(World world, BlockPos pos, String feature) {
        return FloraBiomes.has(world.getBiome(pos), feature) ? pos : null;
    }

    private static boolean isLoaded(World world, BlockPos pos) {
        return pos.getY() >= WorldHeights.minY(world) && pos.getY() < WorldHeights.maxYExclusive(world) && world.isBlockLoaded(pos);
    }

    private static int top(World world, BlockPos column) {
        int top = WorldHeights.topOfFilledSections(world, world.getChunk(column)) + 15;
        return Math.min(top, WorldHeights.maxYExclusive(world) - 1);
    }

    /** MOTION_BLOCKING (or MOTION_BLOCKING_NO_LEAVES): above the highest block that blocks motion or holds a fluid. */
    static BlockPos motionBlocking(World world, BlockPos column, boolean noLeaves) {
        int min = WorldHeights.minY(world);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = top(world, column); y >= min; y--) {
            p.setPos(column.getX(), y, column.getZ());
            IBlockState s = world.getBlockState(p);
            Material m = s.getMaterial();
            if ((m.blocksMovement() || m.isLiquid()) && !(noLeaves && s.getBlock().isLeaves(s, world, p))) {
                return new BlockPos(column.getX(), y + 1, column.getZ());
            }
        }
        return new BlockPos(column.getX(), min, column.getZ());
    }

    /** OCEAN_FLOOR: above the highest block that blocks motion (leaves count). */
    static BlockPos oceanFloor(World world, BlockPos column) {
        int min = WorldHeights.minY(world);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = top(world, column); y >= min; y--) {
            p.setPos(column.getX(), y, column.getZ());
            if (world.getBlockState(p).getMaterial().blocksMovement()) {
                return new BlockPos(column.getX(), y + 1, column.getZ());
            }
        }
        return new BlockPos(column.getX(), min, column.getZ());
    }

    /** WORLD_SURFACE(_WG): above the highest block that is not air. */
    static BlockPos worldSurface(World world, BlockPos column) {
        int min = WorldHeights.minY(world);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = top(world, column); y >= min; y--) {
            p.setPos(column.getX(), y, column.getZ());
            if (!world.isAirBlock(p)) {
                return new BlockPos(column.getX(), y + 1, column.getZ());
            }
        }
        return new BlockPos(column.getX(), min, column.getZ());
    }

    /** {@code matching_fluids water} at one of the four sides of this block. */
    static boolean waterBeside(World world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            if (world.getBlockState(pos.offset(face)).getMaterial() == Material.WATER) {
                return true;
            }
        }
        return false;
    }
}
