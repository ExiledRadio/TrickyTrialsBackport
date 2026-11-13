package com.exiledradio.trickytrialsbackport.flora;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 1.21.5's fallen trees ({@code FallenTreeFeature}) and the leaf litter that trees drop
 * ({@code PlaceOnGroundDecorator}), with the tree selectors that hold them.
 *
 * <p>1.21.5 put the fallen trees inside the biomes' tree selectors, at chances of 1 in 80 to 1 in 400 per tree, and
 * gave the forest's and dark forest's trees leaf litter around their foot. The trees themselves are the terrain's
 * (1.12.2's own, or another generator's), so:
 *
 * <ul>
 *   <li>{@link #place(World, Random, BlockPos, Fallen)} and {@link #leafLitter(World, Random, Collection)} are the two
 *       pieces a terrain with its own copy of 1.21.5's selectors calls;</li>
 *   <li>{@link #fallenTreeAttempts} runs one selector's draws as extra tree attempts, placing only the fallen trees
 *       (1.12.2's decoration: the trees it already placed stay as they are);</li>
 *   <li>{@link #litterUnderTrees} finds the trees standing in a decorated square and gives each its leaf litter.</li>
 * </ul>
 */
public final class FallenTrees {

    /** The four fallen trees of 1.21.5 ({@code TreeFeatures.FALLEN_*}): trunk, length, and whether the stump gets vines. */
    public enum Fallen {
        OAK(BlockPlanks.EnumType.OAK, 4, 7, true),
        BIRCH(BlockPlanks.EnumType.BIRCH, 5, 8, false),
        SUPER_BIRCH(BlockPlanks.EnumType.BIRCH, 5, 15, false),
        SPRUCE(BlockPlanks.EnumType.SPRUCE, 6, 10, false),
        JUNGLE(BlockPlanks.EnumType.JUNGLE, 4, 11, true);

        final BlockPlanks.EnumType wood;
        final int minLength;
        final int maxLength;
        final boolean stumpVines;

        Fallen(BlockPlanks.EnumType wood, int minLength, int maxLength, boolean stumpVines) {
            this.wood = wood;
            this.minLength = minLength;
            this.maxLength = maxLength;
            this.stumpVines = stumpVines;
        }

        IBlockState log(BlockLog.EnumAxis axis) {
            IBlockState s = this.wood.getMetadata() < 4
                    ? Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, this.wood)
                    : Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, this.wood);
            return s.withProperty(BlockLog.LOG_AXIS, axis);
        }
    }

    /** One tree selector of 1.21.5: its count, and its entries in order (a null kind is a tree of the terrain's). */
    static final class Selector {
        final int count;
        final int extraWeight;
        final float[] chances;
        final Fallen[] kinds;

        /** Count {@code count} nine times in ten and {@code count + 1} one time in ten, or exactly {@code count}. */
        Selector(int count, boolean weighted, Object... entries) {
            this.count = count;
            this.extraWeight = weighted ? 1 : 0;
            this.chances = new float[entries.length / 2];
            this.kinds = new Fallen[entries.length / 2];
            for (int i = 0; i < this.chances.length; i++) {
                this.chances[i] = ((Number) entries[2 * i]).floatValue();
                this.kinds[i] = (Fallen) entries[2 * i + 1];
            }
        }

        /** The count, as {@code weighted_list}: {@code data: count, weight: w0; data: count+1, weight: 1}. */
        int count(Random rand, int bigWeight) {
            if (this.extraWeight == 0) {
                return this.count;
            }
            return rand.nextInt(bigWeight + 1) < bigWeight ? this.count : this.count + 1;
        }
    }

    /** Selector name -> its count weights (nine in ten, or nineteen in twenty for plains) and entries. */
    static final Map<String, Selector> SELECTORS = new HashMap<String, Selector>();
    private static final Map<String, Integer> BIG_WEIGHT = new HashMap<String, Integer>();

    private static final Fallen T = null;

    static {
        sel("trees_plains", 0, 19, 0.33333334F, T, 0.0125F, Fallen.OAK);
        sel("trees_birch_and_oak_leaf_litter", 10, 9, 0.0025F, Fallen.BIRCH, 0.2F, T, 0.1F, T, 0.0125F, Fallen.OAK);
        sel("trees_birch", 10, 9, 0.0125F, Fallen.BIRCH);
        sel("birch_tall", 10, 9, 0.00625F, Fallen.SUPER_BIRCH, 0.5F, T, 0.0125F, Fallen.BIRCH);
        sel("trees_flower_forest", 6, 9, 0.0025F, Fallen.BIRCH, 0.2F, T, 0.1F, T);
        sel("dark_forest_vegetation", 16, 0, 0.025F, T, 0.05F, T, 0.6666667F, T, 0.0025F, Fallen.BIRCH, 0.2F, T,
                0.0125F, Fallen.OAK, 0.1F, T);
        sel("trees_badlands", 5, 9, 0.0125F, Fallen.OAK);
        sel("trees_jungle", 50, 9, 0.1F, T, 0.5F, T, 0.33333334F, T, 0.0125F, Fallen.JUNGLE);
        sel("trees_old_growth_pine_taiga", 10, 9, 0.025641026F, T, 0.30769232F, T, 0.33333334F, T, 0.0125F, Fallen.SPRUCE);
        sel("trees_old_growth_spruce_taiga", 10, 9, 0.33333334F, T, 0.33333334F, T, 0.0125F, Fallen.SPRUCE);
        sel("trees_savanna", 1, 9, 0.8F, T, 0.0125F, Fallen.OAK);
        sel("trees_windswept_savanna", 2, 9, 0.8F, T, 0.0125F, Fallen.OAK);
        sel("trees_snowy", 0, 9, 0.0125F, Fallen.SPRUCE);
        sel("trees_sparse_jungle", 2, 9, 0.1F, T, 0.5F, T, 0.0125F, Fallen.JUNGLE);
        sel("trees_taiga", 10, 9, 0.33333334F, T, 0.0125F, Fallen.SPRUCE);
        sel("trees_windswept_forest", 3, 9, 0.008325F, Fallen.SPRUCE, 0.666F, T, 0.1F, T, 0.0125F, Fallen.OAK);
        sel("trees_windswept_hills", 0, 9, 0.008325F, Fallen.SPRUCE, 0.666F, T, 0.1F, T, 0.0125F, Fallen.OAK);
    }

    private static void sel(String name, int count, int bigWeight, Object... entries) {
        SELECTORS.put(name, new Selector(count, bigWeight > 0, entries));
        BIG_WEIGHT.put(name, bigWeight);
    }

    /** The selectors whose trees (all but the fallen ones) drop leaf litter: the forest's and the dark forest's. */
    static boolean dropsLitter(String selector) {
        return "trees_birch_and_oak_leaf_litter".equals(selector) || "dark_forest_vegetation".equals(selector);
    }

    private FallenTrees() {
    }

    // ===========================================================================================
    // 1.12.2's decoration: extra attempts that place only the fallen trees
    // ===========================================================================================

    /**
     * Runs a selector's placement and draws as 1.21.5 does ({@code count}, {@code in_square}, water depth 0,
     * OCEAN_FLOOR, the biome), but places only the attempts that pick a fallen tree: the other entries are the
     * terrain's trees, which it has placed itself.
     */
    static void fallenTreeAttempts(World world, Random rand, BlockPos origin, String selectorName, String biomeFeature) {
        Selector selector = SELECTORS.get(selectorName);
        if (selector == null) {
            return;
        }
        int count = selector.count(rand, BIG_WEIGHT.get(selectorName));
        for (int i = 0; i < count; i++) {
            BlockPos column = FloraFeatures.square(rand, origin);
            BlockPos pos = FloraFeatures.oceanFloor(world, column);
            if (FloraFeatures.worldSurface(world, column).getY() - pos.getY() > 0) {
                continue;
            }
            if (FloraFeatures.filtered(world, pos, biomeFeature) == null) {
                continue;
            }
            Fallen picked = null;
            boolean any = false;
            for (int e = 0; e < selector.chances.length; e++) {
                if (rand.nextFloat() < selector.chances[e]) {
                    picked = selector.kinds[e];
                    any = true;
                    break;
                }
            }
            if (any && picked != null && saplingSurvives(world, pos)) {
                place(world, rand, pos, picked);
            }
        }
    }

    /** {@code would_survive <sapling>}: every fallen tree's sapling stands on {@code #dirt} or farmland. */
    static boolean saplingSurvives(World world, BlockPos pos) {
        IBlockState ground = world.getBlockState(pos.down());
        return BlockVegetation.isDirtTag(ground) || ground.getBlock() == Blocks.FARMLAND;
    }

    // ===========================================================================================
    // FallenTreeFeature
    // ===========================================================================================

    /**
     * {@code FallenTreeFeature.place}: an upright one-block stump at {@code origin} (vines on its sides for oak and
     * jungle), and, two or three blocks away in a random direction, a log lying on the ground, its length minus two
     * blocks long, if it fits along valid tree positions with no more than two blocks of it over a gap; mushrooms on
     * one log in ten (red two times in three).
     */
    public static void place(World world, Random rand, BlockPos origin, Fallen kind) {
        // placeStump, with its decorators.
        world.setBlockState(origin, kind.log(BlockLog.EnumAxis.Y), FloraFeatures.FLAGS);
        if (kind.stumpVines) {
            trunkVines(world, rand, origin);
        }
        EnumFacing[] horizontal = {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};
        EnumFacing dir = horizontal[rand.nextInt(4)];
        int length = kind.minLength + rand.nextInt(kind.maxLength - kind.minLength + 1) - 2;
        BlockPos start = origin.offset(dir, 2 + rand.nextInt(2));
        // setGroundHeightForFallenLogStartPos
        start = start.up();
        for (int i = 0; i < 6; i++) {
            if (validTreePos(world, start) && overSolidGround(world, start)) {
                break;
            }
            start = start.down();
        }
        // canPlaceEntireFallenLog
        int gap = 0;
        BlockPos p = start;
        for (int i = 0; i < length; i++) {
            if (!validTreePos(world, p)) {
                return;
            }
            if (!overSolidGround(world, p)) {
                if (++gap > 2) {
                    return;
                }
            } else {
                gap = 0;
            }
            p = p.offset(dir);
        }
        // placeFallenLog
        BlockLog.EnumAxis axis = dir.getAxis() == EnumFacing.Axis.X ? BlockLog.EnumAxis.X : BlockLog.EnumAxis.Z;
        Set<BlockPos> logs = new LinkedHashSet<BlockPos>();
        p = start;
        for (int i = 0; i < length; i++) {
            world.setBlockState(p, kind.log(axis), FloraFeatures.FLAGS);
            logs.add(p);
            p = p.offset(dir);
        }
        // attached_to_logs: probability 0.1, red mushroom 2 / brown 1, up.
        List<BlockPos> shuffled = new ArrayList<BlockPos>(logs);
        for (int i = shuffled.size(); i > 1; i--) {
            int j = rand.nextInt(i);
            shuffled.set(i - 1, shuffled.set(j, shuffled.get(i - 1)));
        }
        for (BlockPos log : shuffled) {
            rand.nextInt(1);
            BlockPos above = log.up();
            if (rand.nextFloat() <= 0.1F && world.isAirBlock(above)) {
                world.setBlockState(above, (rand.nextInt(3) < 2 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).getDefaultState(),
                        FloraFeatures.FLAGS);
            }
        }
    }

    /** {@code TrunkVineDecorator} on one log: each side two times in three, a vine hanging up to four more down. */
    private static void trunkVines(World world, Random rand, BlockPos log) {
        if (rand.nextInt(3) > 0) {
            vine(world, log.west(), BlockVine.EAST);
        }
        if (rand.nextInt(3) > 0) {
            vine(world, log.east(), BlockVine.WEST);
        }
        if (rand.nextInt(3) > 0) {
            vine(world, log.north(), BlockVine.SOUTH);
        }
        if (rand.nextInt(3) > 0) {
            vine(world, log.south(), BlockVine.NORTH);
        }
    }

    /** {@code TreeDecorator.Context.placeVine}. */
    private static void vine(World world, BlockPos pos, PropertyBool side) {
        if (!world.isAirBlock(pos)) {
            return;
        }
        IBlockState state = Blocks.VINE.getDefaultState().withProperty(side, true);
        world.setBlockState(pos, state, FloraFeatures.FLAGS);
        BlockPos p = pos.down();
        for (int i = 4; world.isAirBlock(p) && i > 0; i--) {
            world.setBlockState(p, state, FloraFeatures.FLAGS);
            p = p.down();
        }
    }

    private static boolean overSolidGround(World world, BlockPos pos) {
        return BlockVegetation.sturdyTop(world, pos.down());
    }

    /** {@code TreeFeature.validTreePos}: air, or a block in {@code #replaceable_by_trees}. */
    static boolean validTreePos(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        if (b.isAir(state, world, pos) || b.isLeaves(state, world, pos)) {
            return true;
        }
        return b instanceof BlockFlower || b instanceof BlockTallGrass || b instanceof BlockDoublePlant
                || b == Blocks.DEADBUSH || b instanceof BlockVine || b instanceof BlockVegetation
                || state.getMaterial() == Material.WATER
                || BlockVegetation.named(b, "glow_lichen", "hanging_roots", "seagrass", "tall_seagrass", "pitcher_plant",
                "pale_moss_carpet", "warped_roots", "crimson_roots", "nether_sprouts", "open_eyeblossom", "closed_eyeblossom",
                "cornflower", "lily_of_the_valley", "wither_rose", "torchflower", "pink_petals", "sweet_berry_bush");
    }

    // ===========================================================================================
    // PlaceOnGroundDecorator: leaf litter around a tree
    // ===========================================================================================

    /**
     * The two {@code place_on_ground} decorators of 1.21.5's leaf-litter trees, around the lowest logs of one tree:
     * 96 tries within 4 blocks across and 2 up or down of one to three pieces, then 150 tries within 2 and 2 of one
     * to four. A piece lands on top of a full opaque block, in air or vines, with nothing but leaves above.
     *
     * @param trunk the tree's lowest logs (one for most trees, four for a dark oak)
     */
    public static void leafLitter(World world, Random rand, Collection<BlockPos> trunk) {
        if (trunk.isEmpty() || FloraContent.leafLitter == null) {
            return;
        }
        int y = trunk.iterator().next().getY();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : trunk) {
            if (p.getY() == y) {
                minX = Math.min(minX, p.getX());
                maxX = Math.max(maxX, p.getX());
                minZ = Math.min(minZ, p.getZ());
                maxZ = Math.max(maxZ, p.getZ());
            }
        }
        Map<Long, Integer> heights = new HashMap<Long, Integer>();
        placeOnGround(world, rand, heights, 96, 4, 2, minX, maxX, y, minZ, maxZ, 12);
        placeOnGround(world, rand, heights, 150, 2, 2, minX, maxX, y, minZ, maxZ, 16);
    }

    private static void placeOnGround(World world, Random rand, Map<Long, Integer> heights, int tries, int radius, int height,
                                      int minX, int maxX, int y, int minZ, int maxZ, int states) {
        int x0 = minX - radius, x1 = maxX + radius, y0 = y - height, y1 = y + height, z0 = minZ - radius, z1 = maxZ + radius;
        for (int i = 0; i < tries; i++) {
            int x = x0 + rand.nextInt(x1 - x0 + 1);
            int py = y0 + rand.nextInt(y1 - y0 + 1);
            int z = z0 + rand.nextInt(z1 - z0 + 1);
            BlockPos pos = new BlockPos(x, py, z);
            BlockPos above = pos.up();
            if (!world.isBlockLoaded(pos)) {
                continue;
            }
            IBlockState top = world.getBlockState(above);
            if (!(top.getBlock().isAir(top, world, above) || top.getBlock() == Blocks.VINE)) {
                continue;
            }
            IBlockState ground = world.getBlockState(pos);
            // isSolidRender: a full opaque block; leaves are never one in 1.21.5 (1.12.2's server calls fast leaves opaque).
            if (!ground.isOpaqueCube() || ground.getBlock().isLeaves(ground, world, pos)) {
                continue;
            }
            long key = ((long) x << 32) ^ (z & 0xFFFFFFFFL);
            Integer h = heights.get(key);
            if (h == null) {
                h = FloraFeatures.motionBlocking(world, pos, true).getY();
                heights.put(key, h);
            }
            if (h <= above.getY()) {
                world.setBlockState(above, FloraFeatures.segmented(FloraContent.leafLitter, rand.nextInt(states)), FloraFeatures.FLAGS);
            }
        }
    }

    /**
     * 1.12.2's decoration: every tree standing in the decorated square (a log on {@code #dirt}, the lowest of its
     * column) gets its leaf litter, a dark oak's four trunk logs as one tree. Runs before the fallen trees of the same
     * chunk, which 1.21.5 does not give litter.
     */
    public static void litterUnderTrees(World world, Random rand, BlockPos origin) {
        List<BlockPos> bases = new ArrayList<BlockPos>();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                BlockPos column = origin.add(dx, 0, dz);
                BlockPos top = FloraFeatures.oceanFloor(world, column);
                // Down through leaves, logs and air to the first log standing on the ground.
                BlockPos p = top.down();
                BlockPos base = null;
                for (int n = 0; n < 48 && p.getY() > 0; n++, p = p.down()) {
                    IBlockState s = world.getBlockState(p);
                    if (s.getBlock() instanceof BlockLog || s.getBlock().isWood(world, p)) {
                        base = p;
                        continue;
                    }
                    if (s.getBlock().isAir(s, world, p) || s.getBlock().isLeaves(s, world, p) || s.getBlock() == Blocks.VINE) {
                        base = null;
                        continue;
                    }
                    break;
                }
                if (base != null && BlockVegetation.isDirtTag(world.getBlockState(base.down()))
                        && FloraBiomes.hasLitterTrees(world.getBiome(base))) {
                    bases.add(base);
                }
            }
        }
        // Group touching bases at the same height (a dark oak's 2x2) into one tree.
        List<List<BlockPos>> trees = new ArrayList<List<BlockPos>>();
        for (BlockPos b : bases) {
            List<BlockPos> into = null;
            for (List<BlockPos> tree : trees) {
                for (BlockPos o : tree) {
                    if (o.getY() == b.getY() && Math.abs(o.getX() - b.getX()) <= 1 && Math.abs(o.getZ() - b.getZ()) <= 1) {
                        into = tree;
                        break;
                    }
                }
                if (into != null) {
                    break;
                }
            }
            if (into == null) {
                into = new ArrayList<BlockPos>();
                trees.add(into);
            }
            into.add(b);
        }
        for (List<BlockPos> tree : trees) {
            leafLitter(world, rand, tree);
        }
    }
}
