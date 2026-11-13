package com.exiledradio.trickytrialsbackport.flora;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.21.5's cactus flower ({@code CactusFlowerBlock}): grows on top of cacti, and stands on a cactus, farmland, or
 * the center of any top face (a full block, a fence or wall post). 14 wide and 12 high, always drops itself, makes
 * pink dye.
 */
public class BlockCactusFlower extends BlockVegetation {

    BlockCactusFlower() {
        super(Material.PLANTS, MapColor.PINK, FloraContent.CACTUS_FLOWER, column(14.0D, 0.0D, 12.0D), false, false);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        IBlockState here = world.getBlockState(pos);
        return here.getBlock().isReplaceable(world, pos) && canBlockStay(world, pos, getDefaultState());
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        BlockPos below = pos.down();
        IBlockState ground = world.getBlockState(below);
        if (ground.getBlock() == Blocks.CACTUS || ground.getBlock() == Blocks.FARMLAND) {
            return true;
        }
        // isFaceSturdy(UP, SupportType.CENTER): the face covers the middle of the block.
        BlockFaceShape shape = ground.getBlockFaceShape(world, below, EnumFacing.UP);
        return shape == BlockFaceShape.SOLID || shape == BlockFaceShape.CENTER || shape == BlockFaceShape.CENTER_BIG
                || shape == BlockFaceShape.MIDDLE_POLE || shape == BlockFaceShape.MIDDLE_POLE_THICK;
    }
}
