package com.exiledradio.trickytrialsbackport.palegarden;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDeadBush;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Everything the pale garden places in the world, in one place: the pale oak tree, the pale moss patch, the
 * biome's whole vegetation step. Other terrain (Caves &amp; Cliffs' 1.18 worlds) calls {@link #vegetation}
 * for a pale garden chunk, or {@link #tree} and {@link #mossPatch} on their own; nothing here depends on
 * how the biome was placed or on the world's height range.
 *
 * <h2>Source</h2>
 * 1.21.11's data and code: {@code configured_feature/pale_oak}, {@code pale_oak_creaking},
 * {@code pale_oak_bonemeal}, {@code pale_moss_patch(_bonemeal)}, {@code pale_moss_vegetation},
 * {@code pale_forest_flowers}, {@code flower_pale_garden} and the pale garden biome's vegetal step, with
 * {@code TreeFeature}, {@code DarkOakTrunkPlacer}, {@code DarkOakFoliagePlacer},
 * {@code ThreeLayersFeatureSize}, {@code PaleMossDecorator}, {@code CreakingHeartDecorator} and
 * {@code VegetationPatchFeature}, followed step by step and in the same order of random draws.
 */
public final class PaleGardenFeatures {

    /** The three pale oak configured features. */
    public enum TreeKind {
        /** {@code pale_oak}: with the pale moss decorator (worldgen). */
        PALE_OAK,
        /** {@code pale_oak_creaking}: also a creaking heart in the trunk. */
        CREAKING,
        /** {@code pale_oak_bonemeal}: a sapling's tree, no decorators. */
        BONEMEAL
    }

    /** Modern {@code Plane.HORIZONTAL}'s order, which a random direction is drawn from. */
    private static final EnumFacing[] HORIZONTAL = {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};

    /** Block-update flags for worldgen: send to clients, no neighbor updates (vanilla's 19 minus observers). */
    private static final int FLAGS = 2;

    private PaleGardenFeatures() {
    }

    // ===========================================================================================
    // The tree
    // ===========================================================================================

    /**
     * Places a pale oak whose 2x2 trunk has its north-west column at {@code origin} (the block above the
     * ground). @return false, changing nothing, where there is no room; true once anything is placed
     */
    public static boolean tree(World world, Random rand, BlockPos origin, TreeKind kind) {
        if (PaleGardenContent.set == null) {
            return false;
        }
        // With Dynamic Trees - Backport, a dynamic pale oak grows in a generated pale oak's place. A creaking heart's
        // tree stays static (the heart lives in its trunk); saplings become dynamic through Forge's sapling event.
        if (kind == TreeKind.PALE_OAK && com.exiledradio.backportcore.compat.DynamicTrees.grow(world, origin, rand, "dtbackport:paleoak")) {
            return true;
        }
        // DarkOakTrunkPlacer(6, 2, 1): 6 + [0, 2] + [0, 1].
        int height = 6 + rand.nextInt(3) + rand.nextInt(2);
        // DarkOakFoliagePlacer: foliage height 4, radius and offset 0 - no draws.
        if (origin.getY() < 1 || origin.getY() + height + 1 > world.getHeight()) {
            return false;
        }
        if (maxFreeHeight(world, height, origin) < height) {
            return false;
        }
        Set<BlockPos> logs = new LinkedHashSet<BlockPos>();
        Set<BlockPos> leaves = new LinkedHashSet<BlockPos>();
        List<Object[]> attachments = trunk(world, rand, height, origin, logs);
        for (Object[] a : attachments) {
            foliage(world, rand, (BlockPos) a[0], (Boolean) a[1], leaves);
        }
        if (logs.isEmpty() && leaves.isEmpty()) {
            return false;
        }
        if (kind != TreeKind.BONEMEAL) {
            // TreeDecorator.Context sorts both lists by height.
            List<BlockPos> logList = byHeight(logs);
            List<BlockPos> leafList = byHeight(leaves);
            paleMoss(world, rand, logList, leafList);
            if (kind == TreeKind.CREAKING) {
                creakingHeart(world, rand, logList);
            }
        }
        return true;
    }

    private static List<BlockPos> byHeight(Set<BlockPos> set) {
        List<BlockPos> list = new ArrayList<BlockPos>(set);
        Collections.sort(list, new Comparator<BlockPos>() {
            @Override
            public int compare(BlockPos a, BlockPos b) {
                return Integer.compare(a.getY(), b.getY());
            }
        });
        return list;
    }

    /** {@code TreeFeature.getMaxFreeTreeHeight} with {@code three_layers_feature_size(1, 1, 0, 1, 2)}. */
    private static int maxFreeHeight(World world, int height, BlockPos origin) {
        for (int y = 0; y <= height + 1; y++) {
            int size = y < 1 ? 0 : (y >= height - 1 ? 2 : 1);
            for (int dx = -size; dx <= size; dx++) {
                for (int dz = -size; dz <= size; dz++) {
                    if (!isFree(world, origin.add(dx, y, dz))) {
                        return y - 2;
                    }
                }
            }
        }
        return height;
    }

    /** {@code DarkOakTrunkPlacer.placeTrunk}. @return the foliage attachments: {pos, double trunk} */
    private static List<Object[]> trunk(World world, Random rand, int height, BlockPos origin, Set<BlockPos> logs) {
        BlockPos below = origin.down();
        setDirt(world, below);
        setDirt(world, below.east());
        setDirt(world, below.south());
        setDirt(world, below.south().east());
        EnumFacing lean = HORIZONTAL[rand.nextInt(4)];
        int leanFrom = height - rand.nextInt(4);
        int leans = 2 - rand.nextInt(3);
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        int x = ox;
        int z = oz;
        int top = oy + height - 1;
        for (int i = 0; i < height; i++) {
            if (i >= leanFrom && leans > 0) {
                x += lean.getXOffset();
                z += lean.getZOffset();
                leans--;
            }
            BlockPos p = new BlockPos(x, oy + i, z);
            if (isAirOrLeaves(world, p)) {
                log(world, p, logs);
                log(world, p.east(), logs);
                log(world, p.south(), logs);
                log(world, p.east().south(), logs);
            }
        }
        List<Object[]> attachments = new ArrayList<Object[]>();
        attachments.add(new Object[] {new BlockPos(x, top, z), Boolean.TRUE});
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                if ((dx < 0 || dx > 1 || dz < 0 || dz > 1) && rand.nextInt(3) <= 0) {
                    int length = rand.nextInt(3) + 2;
                    for (int k = 0; k < length; k++) {
                        log(world, new BlockPos(ox + dx, top - k - 1, oz + dz), logs);
                    }
                    attachments.add(new Object[] {new BlockPos(ox + dx, top, oz + dz), Boolean.FALSE});
                }
            }
        }
        return attachments;
    }

    /** {@code DarkOakFoliagePlacer.createFoliage} at radius 0. */
    private static void foliage(World world, Random rand, BlockPos pos, boolean doubleTrunk, Set<BlockPos> leaves) {
        if (doubleTrunk) {
            leavesRow(world, pos, 2, -1, true, leaves);
            leavesRow(world, pos, 3, 0, true, leaves);
            leavesRow(world, pos, 2, 1, true, leaves);
            if (rand.nextBoolean()) {
                leavesRow(world, pos, 0, 2, true, leaves);
            }
        } else {
            leavesRow(world, pos, 2, -1, false, leaves);
            leavesRow(world, pos, 1, 0, false, leaves);
        }
    }

    private static void leavesRow(World world, BlockPos center, int range, int dy, boolean doubleTrunk, Set<BlockPos> leaves) {
        int extra = doubleTrunk ? 1 : 0;
        for (int dx = -range; dx <= range + extra; dx++) {
            for (int dz = -range; dz <= range + extra; dz++) {
                if (!skipLeaf(dx, dy, dz, range, doubleTrunk)) {
                    leaf(world, center.add(dx, dy, dz), leaves);
                }
            }
        }
    }

    /** {@code DarkOakFoliagePlacer.shouldSkipLocationSigned} and {@code shouldSkipLocation}. */
    static boolean skipLeaf(int dx, int dy, int dz, int range, boolean doubleTrunk) {
        if (dy == 0 && doubleTrunk && (dx == -range || dx >= range) && (dz == -range || dz >= range)) {
            return true;
        }
        int ax = doubleTrunk ? Math.min(Math.abs(dx), Math.abs(dx - 1)) : Math.abs(dx);
        int az = doubleTrunk ? Math.min(Math.abs(dz), Math.abs(dz - 1)) : Math.abs(dz);
        if (dy == -1 && !doubleTrunk) {
            return ax == range && az == range;
        }
        return dy == 1 && ax + az > range * 2 - 2;
    }

    private static void log(World world, BlockPos pos, Set<BlockPos> logs) {
        if (validTreePos(world, pos)) {
            world.setBlockState(pos, PaleGardenContent.set.log.getDefaultState()
                    .withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Y), FLAGS);
            logs.add(pos.toImmutable());
        }
    }

    /** {@code FoliagePlacer.tryPlaceLeaf}: never over persistent leaves. */
    private static void leaf(World world, BlockPos pos, Set<BlockPos> leaves) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockLeaves && state.getPropertyKeys().contains(BlockLeaves.DECAYABLE)
                && !state.getValue(BlockLeaves.DECAYABLE)) {
            return;
        }
        if (validTreePos(world, pos)) {
            world.setBlockState(pos, PaleGardenContent.leaves.getDefaultState()
                    .withProperty(BlockLeaves.CHECK_DECAY, false).withProperty(BlockLeaves.DECAYABLE, true), FLAGS);
            leaves.add(pos.toImmutable());
        }
    }

    /** {@code TrunkPlacer.setDirtAt}: dirt unless it already is dirt (grass and mycelium count as not). */
    private static void setDirt(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        if (!(isDirtTag(state) && b != Blocks.GRASS && b != Blocks.MYCELIUM)) {
            world.setBlockState(pos, Blocks.DIRT.getDefaultState(), FLAGS);
        }
    }

    // ===========================================================================================
    // Decorators
    // ===========================================================================================

    /** {@code PaleMossDecorator(ground 0.8, trunk 0.4, leaves 0.15)}. */
    private static void paleMoss(World world, Random rand, List<BlockPos> logs, List<BlockPos> leaves) {
        List<BlockPos> shuffled = new ArrayList<BlockPos>(logs);
        shuffle(shuffled, rand);
        if (shuffled.isEmpty()) {
            return;
        }
        BlockPos lowest = shuffled.get(0);
        for (BlockPos p : shuffled) {
            if (p.getY() < lowest.getY()) {
                lowest = p;
            }
        }
        if (rand.nextFloat() < 0.8F) {
            mossPatch(world, rand, lowest.up(), false);
        }
        for (BlockPos log : logs) {
            if (rand.nextFloat() < 0.4F && world.isAirBlock(log.down())) {
                hanger(world, rand, log.down());
            }
        }
        for (BlockPos leaf : leaves) {
            if (rand.nextFloat() < 0.15F && world.isAirBlock(leaf.down())) {
                hanger(world, rand, leaf.down());
            }
        }
    }

    /** {@code PaleMossDecorator.addMossHanger}: a strand that stops at each block with chance one half. */
    private static void hanger(World world, Random rand, BlockPos pos) {
        IBlockState moss = PaleGardenContent.hangingMoss.getDefaultState();
        while (world.isAirBlock(pos.down()) && !(rand.nextFloat() < 0.5F)) {
            world.setBlockState(pos, moss, FLAGS);
            pos = pos.down();
        }
        world.setBlockState(pos, moss, FLAGS);
    }

    /** {@code CreakingHeartDecorator(1.0)}: the first log, in a shuffled order, with a log on all six sides. */
    private static void creakingHeart(World world, Random rand, List<BlockPos> logs) {
        if (logs.isEmpty() || rand.nextFloat() >= 1.0F) {
            return;
        }
        List<BlockPos> shuffled = new ArrayList<BlockPos>(logs);
        shuffle(shuffled, rand);
        for (BlockPos p : shuffled) {
            boolean enclosed = true;
            for (EnumFacing face : EnumFacing.values()) {
                if (!isLog(world, p.offset(face))) {
                    enclosed = false;
                    break;
                }
            }
            if (enclosed) {
                world.setBlockState(p, PaleGardenContent.creakingHeart.getDefaultState()
                        .withProperty(BlockCreakingHeart.STATE, BlockCreakingHeart.State.DORMANT), FLAGS);
                net.minecraft.tileentity.TileEntity tile = world.getTileEntity(p);
                if (tile instanceof TileCreakingHeart) {
                    ((TileCreakingHeart) tile).setNatural(true);
                }
                return;
            }
        }
    }

    /** Modern {@code Util.shuffle}. */
    private static <T> void shuffle(List<T> list, Random rand) {
        for (int i = list.size(); i > 1; i--) {
            int j = rand.nextInt(i);
            list.set(i - 1, list.set(j, list.get(i - 1)));
        }
    }

    // ===========================================================================================
    // Pale moss patch (VegetationPatchFeature)
    // ===========================================================================================

    /**
     * {@code pale_moss_patch} (radius 2-4, vegetation 30%) or, for bone meal, {@code pale_moss_patch_bonemeal}
     * (radius 1-2, 60%): pale moss replaces the ground in a rounded square around {@code origin}, then
     * pale moss carpets, short grass and tall grass grow on some of it.
     *
     * @return true if any ground was turned to pale moss
     */
    public static boolean mossPatch(World world, Random rand, BlockPos origin, boolean bonemeal) {
        if (PaleGardenContent.mossBlock == null) {
            return false;
        }
        int min = bonemeal ? 1 : 2;
        int max = bonemeal ? 2 : 4;
        float vegetation = bonemeal ? 0.6F : 0.3F;
        int rx = min + rand.nextInt(max - min + 1) + 1;
        int rz = min + rand.nextInt(max - min + 1) + 1;
        Set<BlockPos> ground = new HashSet<BlockPos>();
        for (int dx = -rx; dx <= rx; dx++) {
            boolean edgeX = dx == -rx || dx == rx;
            for (int dz = -rz; dz <= rz; dz++) {
                boolean edgeZ = dz == -rz || dz == rz;
                boolean corner = edgeX && edgeZ;
                boolean edge = (edgeX || edgeZ) && !corner;
                if (corner || edge && rand.nextFloat() > 0.75F) {
                    continue;
                }
                BlockPos p = origin.add(dx, 0, dz);
                for (int i = 0; world.isAirBlock(p) && i < 5; i++) {
                    p = p.down();
                }
                for (int i = 0; !world.isAirBlock(p) && i < 5; i++) {
                    p = p.up();
                }
                BlockPos below = p.down();
                if (world.isAirBlock(p) && sturdyTop(world, below) && placeMossGround(world, below)) {
                    ground.add(below);
                }
            }
        }
        for (BlockPos g : ground) {
            if (rand.nextFloat() < vegetation) {
                mossVegetation(world, rand, g.up());
            }
        }
        return !ground.isEmpty();
    }

    /** {@code VegetationPatchFeature.placeGround} at depth 1. */
    private static boolean placeMossGround(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == PaleGardenContent.mossBlock) {
            return true;
        }
        if (!isMossReplaceable(state)) {
            return false;
        }
        world.setBlockState(pos, PaleGardenContent.mossBlock.getDefaultState(), FLAGS);
        return true;
    }

    /** {@code pale_moss_vegetation}: pale moss carpet 25, short grass 25, tall grass 10. */
    private static void mossVegetation(World world, Random rand, BlockPos pos) {
        int pick = rand.nextInt(60);
        if (pick < 25) {
            if (!world.isAirBlock(pos.down())) {
                BlockPaleMossCarpet.placeAt(world, pos, world.rand, FLAGS);
            }
        } else if (pick < 50) {
            IBlockState grass = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);
            if (Blocks.TALLGRASS.canBlockStay(world, pos, grass)) {
                world.setBlockState(pos, grass, FLAGS);
            }
        } else {
            doublePlant(world, pos, BlockDoublePlant.EnumPlantType.GRASS);
        }
    }

    private static void doublePlant(World world, BlockPos pos, BlockDoublePlant.EnumPlantType type) {
        if (Blocks.DOUBLE_PLANT.canBlockStay(world, pos, Blocks.DOUBLE_PLANT.getDefaultState()) && world.isAirBlock(pos.up())) {
            Blocks.DOUBLE_PLANT.placeAt(world, pos, type, FLAGS);
        }
    }

    // ===========================================================================================
    // The biome's vegetation step
    // ===========================================================================================

    /**
     * The pale garden's vegetal decoration for one 1.12.2 population area: 16 tree tries (one in ten a
     * creaking tree), a pale moss patch, closed eyeblossom patches (1 in 8), a single eyeblossom (1 in 32),
     * two short grass patches, pumpkins (1 in 300) and sugar cane (1 in 6). Positions are drawn in the 16x16
     * square at {@code corner} + 8, as 1.12.2's decorators do, so nothing reaches into an ungenerated chunk.
     *
     * @param corner the chunk's north-west corner (block coordinates, y ignored)
     */
    public static void vegetation(World world, Random rand, BlockPos corner, Biome biome) {
        if (PaleGardenContent.set == null) {
            return;
        }
        BlockPos base = new BlockPos(corner.getX() + 8, 0, corner.getZ() + 8);
        // pale_garden_vegetation: count 16, in_square, surface water depth 0, OCEAN_FLOOR, biome.
        for (int i = 0; i < 16; i++) {
            BlockPos column = base.add(rand.nextInt(16), 0, rand.nextInt(16));
            BlockPos pos = oceanFloor(world, column);
            if (pos == null || waterDepth(world, pos) > 0 || world.getBiome(pos) != biome) {
                continue;
            }
            // random_selector: 0.1 creaking, 0.9 plain, default plain; each checked by sapling survival.
            TreeKind kind = rand.nextFloat() < 0.1F ? TreeKind.CREAKING : TreeKind.PALE_OAK;
            if (kind == TreeKind.PALE_OAK) {
                rand.nextFloat();
            }
            if (saplingSurvives(world, pos)) {
                tree(world, rand, pos, kind);
            }
        }
        // pale_moss_patch: count 1, in_square, MOTION_BLOCKING_NO_LEAVES, biome.
        {
            BlockPos pos = motionBlockingNoLeaves(world, base.add(rand.nextInt(16), 0, rand.nextInt(16)));
            if (world.getBiome(pos) == biome) {
                mossPatch(world, rand, pos, false);
            }
        }
        // pale_garden_flowers: rarity 8, in_square, MOTION_BLOCKING_NO_LEAVES, biome; 96 closed eyeblossoms.
        if (rand.nextFloat() < 1.0F / 8.0F) {
            BlockPos pos = motionBlockingNoLeaves(world, base.add(rand.nextInt(16), 0, rand.nextInt(16)));
            if (world.getBiome(pos) == biome) {
                for (int i = 0; i < 96; i++) {
                    eyeblossom(world, offset(rand, pos, 7, 3));
                }
            }
        }
        // flower_pale_garden: rarity 32, in_square, MOTION_BLOCKING, biome; one eyeblossom (spread 0).
        if (rand.nextFloat() < 1.0F / 32.0F) {
            BlockPos pos = motionBlocking(world, base.add(rand.nextInt(16), 0, rand.nextInt(16)));
            if (world.getBiome(pos) == biome) {
                eyeblossom(world, offset(rand, pos, 0, 0));
            }
        }
        // patch_grass_forest: count 2, in_square, WORLD_SURFACE_WG, biome; 32 short grass, spread 7/3.
        for (int n = 0; n < 2; n++) {
            BlockPos pos = worldSurface(world, base.add(rand.nextInt(16), 0, rand.nextInt(16)));
            if (world.getBiome(pos) != biome) {
                continue;
            }
            IBlockState grass = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);
            for (int i = 0; i < 32; i++) {
                BlockPos p = offset(rand, pos, 7, 3);
                if (world.isAirBlock(p) && Blocks.TALLGRASS.canBlockStay(world, p, grass)) {
                    world.setBlockState(p, grass, FLAGS);
                }
            }
        }
        // patch_pumpkin: rarity 300, in_square, MOTION_BLOCKING, biome; 96 tries on grass.
        if (rand.nextFloat() < 1.0F / 300.0F) {
            BlockPos pos = motionBlocking(world, base.add(rand.nextInt(16), 0, rand.nextInt(16)));
            if (world.getBiome(pos) == biome) {
                for (int i = 0; i < 96; i++) {
                    BlockPos p = offset(rand, pos, 7, 3);
                    if (world.isAirBlock(p) && world.getBlockState(p.down()).getBlock() == Blocks.GRASS) {
                        world.setBlockState(p, Blocks.PUMPKIN.getDefaultState(), FLAGS);
                    }
                }
            }
        }
        // patch_sugar_cane: rarity 6, in_square, MOTION_BLOCKING, biome; 20 tries, spread 4/0, columns 2-4.
        if (rand.nextFloat() < 1.0F / 6.0F) {
            BlockPos pos = motionBlocking(world, base.add(rand.nextInt(16), 0, rand.nextInt(16)));
            if (world.getBiome(pos) == biome) {
                for (int i = 0; i < 20; i++) {
                    BlockPos p = offset(rand, pos, 4, 0);
                    int height = 2 + rand.nextInt(rand.nextInt(3) + 1);
                    if (world.isAirBlock(p) && Blocks.REEDS.canPlaceBlockAt(world, p) && waterBeside(world, p.down())) {
                        for (int k = 0; k < height && world.isAirBlock(p.up(k)); k++) {
                            world.setBlockState(p.up(k), Blocks.REEDS.getDefaultState(), FLAGS);
                        }
                    }
                }
            }
        }
    }

    /** {@code pale_forest_flowers}' block: a closed eyeblossom on air where it can stay, ticked once soon. */
    private static void eyeblossom(World world, BlockPos pos) {
        BlockEyeblossom closed = PaleGardenContent.closedEyeblossom;
        if (world.isAirBlock(pos) && closed.canBlockStay(world, pos, closed.getDefaultState())) {
            world.setBlockState(pos, closed.getDefaultState(), FLAGS);
            world.scheduleUpdate(pos, closed, 1);
        }
    }

    /** {@code RandomPatchFeature}'s offset: a triangular spread of xz and y. */
    private static BlockPos offset(Random rand, BlockPos pos, int xz, int y) {
        return pos.add(rand.nextInt(xz + 1) - rand.nextInt(xz + 1), rand.nextInt(y + 1) - rand.nextInt(y + 1),
                rand.nextInt(xz + 1) - rand.nextInt(xz + 1));
    }

    private static boolean waterBeside(World world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            if (world.getBlockState(pos.offset(face)).getMaterial() == Material.WATER) {
                return true;
            }
        }
        return false;
    }

    // ===========================================================================================
    // Heightmaps (modern names; 1.12.2's getHeight and getTopSolidOrLiquidBlock answer different things)
    // ===========================================================================================

    /** OCEAN_FLOOR: above the highest block that blocks motion (leaves count). */
    static BlockPos oceanFloor(World world, BlockPos column) {
        for (int y = world.getActualHeight() - 1; y >= 0; y--) {
            BlockPos p = new BlockPos(column.getX(), y, column.getZ());
            if (world.getBlockState(p).getMaterial().blocksMovement()) {
                return p.up();
            }
        }
        return null;
    }

    /** MOTION_BLOCKING: above the highest block that blocks motion or holds a fluid. */
    static BlockPos motionBlocking(World world, BlockPos column) {
        return scan(world, column, false);
    }

    /** MOTION_BLOCKING_NO_LEAVES: as {@link #motionBlocking}, leaves left out. */
    static BlockPos motionBlockingNoLeaves(World world, BlockPos column) {
        return scan(world, column, true);
    }

    private static BlockPos scan(World world, BlockPos column, boolean skipLeaves) {
        for (int y = world.getActualHeight() - 1; y >= 0; y--) {
            BlockPos p = new BlockPos(column.getX(), y, column.getZ());
            IBlockState s = world.getBlockState(p);
            Material m = s.getMaterial();
            if ((m.blocksMovement() || m.isLiquid()) && !(skipLeaves && s.getBlock().isLeaves(s, world, p))) {
                return p.up();
            }
        }
        return new BlockPos(column.getX(), 0, column.getZ());
    }

    /** WORLD_SURFACE: above the highest block that is not air. */
    static BlockPos worldSurface(World world, BlockPos column) {
        return world.getHeight(column);
    }

    /** {@code SurfaceWaterDepthFilter}: WORLD_SURFACE minus OCEAN_FLOOR at this column. */
    private static int waterDepth(World world, BlockPos floor) {
        return worldSurface(world, floor).getY() - floor.getY();
    }

    private static boolean saplingSurvives(World world, BlockPos pos) {
        BlockPos below = pos.down();
        IBlockState ground = world.getBlockState(below);
        BlockPaleOakSapling sapling = PaleGardenContent.sapling;
        return sapling != null && ground.getBlock().canSustainPlant(ground, world, below, EnumFacing.UP, sapling);
    }

    // ===========================================================================================
    // Block tags, as 1.12.2 blocks
    // ===========================================================================================

    /** {@code TreeFeature.validTreePos}: air, or a block in {@code #replaceable_by_trees}. */
    static boolean validTreePos(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        if (b.isAir(state, world, pos) || b.isLeaves(state, world, pos)) {
            return true;
        }
        return b instanceof BlockFlower || b instanceof BlockTallGrass || b instanceof BlockDoublePlant
                || b instanceof BlockDeadBush || b instanceof BlockVine || b instanceof BlockEyeblossom
                || b == PaleGardenContent.mossCarpet || state.getMaterial() == Material.WATER
                || named(b, "glow_lichen", "hanging_roots", "seagrass", "tall_seagrass", "leaf_litter", "bush",
                "firefly_bush", "pitcher_plant", "short_dry_grass", "tall_dry_grass", "warped_roots", "crimson_roots",
                "nether_sprouts");
    }

    /** {@code TrunkPlacer.isFree}: a valid tree position or a log. */
    private static boolean isFree(World world, BlockPos pos) {
        return validTreePos(world, pos) || isLog(world, pos);
    }

    private static boolean isAirOrLeaves(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        return s.getBlock().isAir(s, world, pos) || s.getBlock().isLeaves(s, world, pos);
    }

    /** {@code #logs}: vanilla's and every mod's logs, wood and stripped forms. */
    static boolean isLog(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        Block b = s.getBlock();
        return b instanceof BlockLog || b.isWood(world, pos) || b.canSustainLeaves(s, world, pos);
    }

    /** {@code #dirt}: dirt (and coarse dirt, podzol), grass, mycelium, moss and pale moss, rooted dirt, mud. */
    static boolean isDirtTag(IBlockState state) {
        Block b = state.getBlock();
        return b == Blocks.DIRT || b == Blocks.GRASS || b == Blocks.MYCELIUM || b == PaleGardenContent.mossBlock
                || named(b, "moss_block", "rooted_dirt", "mud", "muddy_mangrove_roots");
    }

    /** {@code #moss_replaceable}: {@code #base_stone_overworld}, {@code #cave_vines} and {@code #dirt}. */
    static boolean isMossReplaceable(IBlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.STONE) {
            BlockStone.EnumType t = state.getValue(BlockStone.VARIANT);
            return t == BlockStone.EnumType.STONE || t == BlockStone.EnumType.GRANITE || t == BlockStone.EnumType.DIORITE
                    || t == BlockStone.EnumType.ANDESITE;
        }
        return isDirtTag(state) || named(b, "tuff", "deepslate", "cave_vines", "cave_vines_plant", "cave_vines_body",
                "cave_vines_head");
    }

    /** A block of any mod whose registry path is one of these (the owners' blocks this mod does not link against). */
    private static boolean named(Block b, String... paths) {
        if (b.getRegistryName() == null) {
            return false;
        }
        String path = b.getRegistryName().getPath();
        for (String p : paths) {
            if (p.equals(path)) {
                return true;
            }
        }
        return false;
    }

    /** {@code isFaceSturdy(UP)}. */
    static boolean sturdyTop(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        return s.getBlockFaceShape(world, pos, EnumFacing.UP) == BlockFaceShape.SOLID;
    }
}
