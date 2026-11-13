package com.exiledradio.trickytrialsbackport.palegarden;

import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.Random;

/**
 * The pale oak sapling: 1.21.4's {@code SaplingBlock} with {@code TreeGrower.PALE_OAK}, which has a mega tree
 * only. It grows like any sapling (a random tick with light 9 or more, one time in seven; bone meal 45%),
 * first to stage 1, then into a tree - but only as the corner of a 2x2 of pale oak saplings at stage 1 or
 * not, exactly as dark oak. Alone it never grows; bone meal still takes it to stage 1 and is used up.
 */
public class BlockPaleOakSapling extends BlockBush implements IGrowable {

    public static final PropertyInteger STAGE = PropertyInteger.create("stage", 0, 1);
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.1D, 0.0D, 0.1D, 0.9D, 0.8D, 0.9D);

    public BlockPaleOakSapling() {
        setDefaultState(this.blockState.getBaseState().withProperty(STAGE, 0));
        setSoundType(SoundType.PLANT);
        setHardness(0.0F);
        setTickRandomly(true);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, STAGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(STAGE, meta & 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STAGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return MapColor.IRON;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote) {
            return;
        }
        super.updateTick(world, pos, state, rand);
        if (!world.isAreaLoaded(pos, 1)) {
            return;
        }
        if (world.getLightFromNeighbors(pos.up()) >= 9 && rand.nextInt(7) == 0) {
            grow(world, rand, pos, state);
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return world.rand.nextFloat() < 0.45D;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        if (state.getValue(STAGE) == 0) {
            world.setBlockState(pos, state.cycleProperty(STAGE), 4);
            return;
        }
        if (!TerrainGen.saplingGrowTree(world, rand, pos)) {
            return;
        }
        // TreeGrower.growTree: the 2x2 whose north-west corner is pos + (i, 0, j), i and j from 0 down to -1.
        for (int i = 0; i >= -1; i--) {
            for (int j = 0; j >= -1; j--) {
                if (isTwoByTwo(world, pos, i, j)) {
                    BlockPos corner = pos.add(i, 0, j);
                    IBlockState[] old = new IBlockState[4];
                    BlockPos[] at = {corner, corner.east(), corner.south(), corner.south().east()};
                    for (int k = 0; k < 4; k++) {
                        old[k] = world.getBlockState(at[k]);
                        world.setBlockState(at[k], Blocks.AIR.getDefaultState(), 4);
                    }
                    if (PaleGardenFeatures.tree(world, rand, corner, PaleGardenFeatures.TreeKind.BONEMEAL)) {
                        return;
                    }
                    for (int k = 0; k < 4; k++) {
                        world.setBlockState(at[k], old[k], 4);
                    }
                    return;
                }
            }
        }
    }

    private boolean isTwoByTwo(World world, BlockPos pos, int i, int j) {
        return world.getBlockState(pos.add(i, 0, j)).getBlock() == this
                && world.getBlockState(pos.add(i + 1, 0, j)).getBlock() == this
                && world.getBlockState(pos.add(i, 0, j + 1)).getBlock() == this
                && world.getBlockState(pos.add(i + 1, 0, j + 1)).getBlock() == this;
    }
}
