package com.exiledradio.trickytrialsbackport.palegarden;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pale hanging moss: 1.21.4's {@code HangingMossBlock}. Hangs under any block with a full bottom face, or
 * under more of itself; {@code tip} is true at the lowest block (worked out, not stored). No collision, moss
 * carpet sounds, breaks instantly, drops itself only to shears or Silk Touch. Bone meal adds a block under
 * the tip. Under pale oak logs or leaves it sometimes rustles (client, one time in 500).
 */
public class BlockPaleHangingMoss extends Block implements IGrowable, IShearable {

    public static final PropertyBool TIP = PropertyBool.create("tip");

    /** Block.column(14, 0, 16) and the tip's column(14, 2, 16). */
    private static final AxisAlignedBB BASE = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 1.0D, 0.9375D);
    private static final AxisAlignedBB TIP_BOX = new AxisAlignedBB(0.0625D, 0.125D, 0.0625D, 0.9375D, 1.0D, 0.9375D);

    public BlockPaleHangingMoss() {
        super(Material.VINE, MapColor.SILVER);
        setSoundType(PaleGardenContent.MOSS_CARPET);
        setHardness(0.0F);
        setDefaultState(this.blockState.getBaseState().withProperty(TIP, true));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TIP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.withProperty(TIP, world.getBlockState(pos.down()).getBlock() != this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos.down()).getBlock() != this ? TIP_BOX : BASE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    /** {@code canStayAtPosition}: a full face above, or more hanging moss. */
    public boolean canStay(IBlockAccess world, BlockPos pos) {
        BlockPos up = pos.up();
        IBlockState above = world.getBlockState(up);
        return above.getBlock() == this || above.getBlockFaceShape(world, up, EnumFacing.DOWN) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && canStay(world, pos);
    }

    /** Vanilla schedules a tick and breaks then; a falling strand comes down a block per tick, as there. */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!canStay(world, pos)) {
            world.scheduleUpdate(pos, this, 1);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!canStay(world, pos)) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (rand.nextInt(500) == 0) {
            IBlockState above = world.getBlockState(pos.up());
            Block b = above.getBlock();
            if (PaleGardenContent.set != null && (b == PaleGardenContent.set.log || b == PaleGardenContent.set.wood
                    || b == PaleGardenContent.set.strippedLog || b == PaleGardenContent.set.strippedWood
                    || b == PaleGardenContent.leaves)) {
                world.playSound(pos.getX(), pos.getY(), pos.getZ(), PaleGardenContent.HANGING_MOSS_IDLE, SoundCategory.AMBIENT,
                        1.0F, 1.0F, false);
            }
        }
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
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    // Drops: itself only with shears or Silk Touch.

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(this));
    }

    // Bone meal: one more block under the tip.

    private BlockPos below(World world, BlockPos pos) {
        BlockPos p = pos;
        while (world.getBlockState(p.down()).getBlock() == this) {
            p = p.down();
        }
        return p.down();
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return world.isAirBlock(below(world, pos));
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        BlockPos at = below(world, pos);
        if (world.isAirBlock(at)) {
            world.setBlockState(at, getDefaultState(), 3);
        }
    }
}
