package com.exiledradio.trickytrialsbackport.mace;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The heavy core (1.21): an 8-pixel cube of metal, hardness 10 and blast resistance 1200, mined by hand in
 * 15 seconds (no tool needed or helps), dropping itself unless blown up.
 */
public final class BlockHeavyCore extends Block {

    /** Metal that any hand can mine, as 1.21's heavy core is. */
    static final Material METAL = new Material(MapColor.IRON) {
        @Override
        public EnumPushReaction getPushReaction() {
            return EnumPushReaction.NORMAL;
        }
    };

    static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.25, 0.0, 0.25, 0.75, 0.5, 0.75);

    public BlockHeavyCore(SoundType sound) {
        super(METAL);
        setHardness(10.0F);
        // 1.12.2 stores resistance tripled and reports it fifthed: 2000 here is a modern 1200.
        setResistance(2000.0F);
        setSoundType(sound);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.DOWN ? BlockFaceShape.CENTER_BIG : BlockFaceShape.UNDEFINED;
    }
}
