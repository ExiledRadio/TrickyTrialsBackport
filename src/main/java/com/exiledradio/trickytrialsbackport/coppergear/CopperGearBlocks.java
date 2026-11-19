package com.exiledradio.trickytrialsbackport.coppergear;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.trickytrialsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 1.21.9's copper blocks: the copper torch, and copper bars, chains and lanterns in four stages, waxed or not.
 *
 * <p>Numbers are 1.21.11's {@code Blocks}: bars and chains hardness 5, resistance 6; lanterns 3.5 and light 15; the
 * torch breaks at once and gives light 14. Bars, chains and lanterns need a pickaxe (any) to drop.
 */
public final class CopperGearBlocks {

    private CopperGearBlocks() {
    }

    /** The random tick of an unwaxed, not fully oxidized block: Backport Core's rule, while copper oxidizes. */
    static void age(World world, BlockPos pos, IBlockState state, Random rand) {
        if (ModConfig.copper.oxidation) {
            Oxidation.randomTick(world, pos, state, rand);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Torch

    /**
     * The copper torch: 1.12.2's torch block (floor and wall in one, by {@code facing}) with 1.21.9's copper flame.
     * Its {@code randomDisplayTick} is the torch's: smoke and a flame at the tip, the flame the green copper one.
     */
    public static final class Torch extends BlockTorch {
        public Torch() {
            setHardness(0.0F);
            setLightLevel(0.9375F);
            setSoundType(SoundType.WOOD);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
            EnumFacing facing = state.getValue(FACING);
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 0.7D;
            double z = pos.getZ() + 0.5D;
            if (facing.getAxis().isHorizontal()) {
                EnumFacing back = facing.getOpposite();
                x += 0.27D * back.getXOffset();
                y += 0.22D;
                z += 0.27D * back.getZOffset();
            }
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0.0D, 0.0D, 0.0D);
            com.exiledradio.trickytrialsbackport.coppergear.client.CopperGearClient.flame(world, x, y, z);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Bars

    /** Copper bars: 1.12.2's iron bars (a pane) of copper, connecting to bars, panes and solid faces alike. */
    public static final class Bars extends BlockPane {
        public Bars(SoundType sound, boolean ages) {
            super(Material.IRON, true);
            setHardness(5.0F);
            setResistance(6.0F);
            setSoundType(sound);
            setTickRandomly(ages);
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
            age(world, pos, state, rand);
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        }

        @Override
        public BlockRenderLayer getRenderLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Chain

    /** The copper chain: 1.16's chain (Nether Update's in 1.12.2) of copper: a 3x3 pole along {@code axis}. */
    public static final class Chain extends Block {
        public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class);
        private static final AxisAlignedBB Y = new AxisAlignedBB(6.5D / 16, 0.0D, 6.5D / 16, 9.5D / 16, 1.0D, 9.5D / 16);
        private static final AxisAlignedBB Z = new AxisAlignedBB(6.5D / 16, 6.5D / 16, 0.0D, 9.5D / 16, 9.5D / 16, 1.0D);
        private static final AxisAlignedBB X = new AxisAlignedBB(0.0D, 6.5D / 16, 6.5D / 16, 1.0D, 9.5D / 16, 9.5D / 16);

        public Chain(SoundType sound, boolean ages) {
            super(Material.IRON);
            setHardness(5.0F);
            setResistance(6.0F);
            setSoundType(sound);
            setTickRandomly(ages);
            setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, AXIS);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(AXIS, EnumFacing.Axis.values()[Math.min(meta, 2)]);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(AXIS).ordinal();
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                                int meta, EntityLivingBase placer) {
            return getDefaultState().withProperty(AXIS, facing.getAxis());
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            switch (state.getValue(AXIS)) {
                case X:
                    return X;
                case Z:
                    return Z;
                default:
                    return Y;
            }
        }

        @Override
        public boolean isOpaqueCube(IBlockState state) {
            return false;
        }

        @Override
        public boolean isFullCube(IBlockState state) {
            return false;
        }

        @Override
        public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
            return face.getAxis() == state.getValue(AXIS) ? BlockFaceShape.CENTER_SMALL : BlockFaceShape.UNDEFINED;
        }

        @Override
        public BlockRenderLayer getRenderLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }

        @Override
        public IBlockState withRotation(IBlockState state, net.minecraft.util.Rotation rot) {
            if (rot == net.minecraft.util.Rotation.CLOCKWISE_90 || rot == net.minecraft.util.Rotation.COUNTERCLOCKWISE_90) {
                EnumFacing.Axis a = state.getValue(AXIS);
                return a == EnumFacing.Axis.X ? state.withProperty(AXIS, EnumFacing.Axis.Z)
                        : a == EnumFacing.Axis.Z ? state.withProperty(AXIS, EnumFacing.Axis.X) : state;
            }
            return state;
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
            age(world, pos, state, rand);
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Lantern

    /**
     * The copper lantern: 1.14's lantern (Village &amp; Pillage's in 1.12.2) of copper. It stands on a block whose top
     * holds its center, or hangs under one whose bottom does (placed against a bottom face it hangs if it can),
     * and drops when that support goes. Light 15.
     */
    public static final class Lantern extends Block {
        public static final PropertyBool HANGING = PropertyBool.create("hanging");
        private static final AxisAlignedBB STANDING = new AxisAlignedBB(5.0D / 16, 0.0D, 5.0D / 16, 11.0D / 16, 9.0D / 16, 11.0D / 16);
        private static final AxisAlignedBB HUNG = new AxisAlignedBB(5.0D / 16, 1.0D / 16, 5.0D / 16, 11.0D / 16, 10.0D / 16, 11.0D / 16);

        public Lantern(SoundType sound, boolean ages) {
            super(Material.IRON);
            setHardness(3.5F);
            setResistance(3.5F);
            setLightLevel(1.0F);
            setSoundType(sound);
            setTickRandomly(ages);
            setDefaultState(blockState.getBaseState().withProperty(HANGING, false));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, HANGING);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(HANGING, (meta & 1) != 0);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(HANGING) ? 1 : 0;
        }

        /** {@code Block.canSupportCenter}: a face that holds the middle 2x2, as a fence post, wall, bars or chain end do. */
        static boolean holdsCenter(IBlockAccess world, BlockPos pos, EnumFacing face) {
            IBlockState s = world.getBlockState(pos);
            BlockFaceShape shape = s.getBlockFaceShape(world, pos, face);
            return shape == BlockFaceShape.SOLID || shape == BlockFaceShape.CENTER || shape == BlockFaceShape.CENTER_BIG
                    || shape == BlockFaceShape.CENTER_SMALL;
        }

        static boolean canStay(IBlockAccess world, BlockPos pos, boolean hanging) {
            return hanging ? holdsCenter(world, pos.up(), EnumFacing.DOWN) : holdsCenter(world, pos.down(), EnumFacing.UP);
        }

        @Override
        public boolean canPlaceBlockAt(World world, BlockPos pos) {
            return super.canPlaceBlockAt(world, pos) && (canStay(world, pos, false) || canStay(world, pos, true));
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                                int meta, EntityLivingBase placer) {
            // LanternBlock.getStateForPlacement: the clicked face first (DOWN means hanging), then the other way.
            boolean first = facing == EnumFacing.DOWN;
            if (canStay(world, pos, first)) {
                return getDefaultState().withProperty(HANGING, first);
            }
            return getDefaultState().withProperty(HANGING, !first);
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
            if (!canStay(world, pos, state.getValue(HANGING))) {
                dropBlockAsItem(world, pos, state, 0);
                world.setBlockToAir(pos);
            }
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            return state.getValue(HANGING) ? HUNG : STANDING;
        }

        @Override
        public boolean isOpaqueCube(IBlockState state) {
            return false;
        }

        @Override
        public boolean isFullCube(IBlockState state) {
            return false;
        }

        @Override
        public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
            return BlockFaceShape.UNDEFINED;
        }

        @Override
        public BlockRenderLayer getRenderLayer() {
            return BlockRenderLayer.CUTOUT;
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
            age(world, pos, state, rand);
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        }
    }
}
