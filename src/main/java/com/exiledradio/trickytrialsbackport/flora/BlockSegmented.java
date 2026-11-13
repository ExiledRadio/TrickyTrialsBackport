package com.exiledradio.trickytrialsbackport.flora;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 1.21.5's {@code SegmentableBlock}s on the ground: wildflowers ({@code FlowerBedBlock}, like pink petals) and leaf
 * litter ({@code LeafLitterBlock}). One to four pieces, laid down facing away from the player who placed the first;
 * more placed on the block add one each ({@link ItemSegmented}). They drop as many as they hold.
 *
 * <ul>
 *   <li>Wildflowers: {@code flower_amount}, 3 high, on {@code #dirt} or farmland; bone meal adds one, and on a full
 *       block drops one; the stems take the grass color.</li>
 *   <li>Leaf litter: {@code segment_amount}, 1 high, on any sturdy top face; replaceable; no bone meal; tinted by
 *       the dry foliage color.</li>
 * </ul>
 *
 * <p>The outline covers the quarters the pieces fill, in the models' order: the facing's quarter (north-west for
 * north), then each next one counterclockwise ({@code SegmentableBlock.getShapeCalculator}).
 */
public class BlockSegmented extends BlockVegetation implements IGrowable {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyInteger FLOWER_AMOUNT = PropertyInteger.create("flower_amount", 1, 4);
    public static final PropertyInteger SEGMENT_AMOUNT = PropertyInteger.create("segment_amount", 1, 4);

    /** Set by the constructor before createBlockState runs: which amount property this instance uses. */
    private static PropertyInteger building;

    private final PropertyInteger amount;
    private final boolean flowers;
    private final AxisAlignedBB[][] boxes = new AxisAlignedBB[4][4];
    private Item item;

    private static Material prepare(PropertyInteger amount, Material material) {
        building = amount;
        return material;
    }

    BlockSegmented(boolean flowers) {
        super(prepare(flowers ? FLOWER_AMOUNT : SEGMENT_AMOUNT, flowers ? Material.PLANTS : Material.VINE),
                flowers ? MapColor.FOLIAGE : MapColor.BROWN, flowers ? FloraContent.PINK_PETALS : FloraContent.LEAF_LITTER,
                FULL_BLOCK_AABB, !flowers, false);
        this.flowers = flowers;
        this.amount = flowers ? FLOWER_AMOUNT : SEGMENT_AMOUNT;
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(this.amount, 1));
        double height = flowers ? 3.0D : 1.0D;
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            for (int n = 1; n <= 4; n++) {
                double minX = 16, minZ = 16, maxX = 0, maxZ = 0;
                EnumFacing d = facing;
                for (int i = 0; i < n; i++) {
                    int x = d == EnumFacing.EAST || d == EnumFacing.SOUTH ? 8 : 0;
                    int z = d == EnumFacing.SOUTH || d == EnumFacing.WEST ? 8 : 0;
                    minX = Math.min(minX, x);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x + 8);
                    maxZ = Math.max(maxZ, z + 8);
                    d = d.rotateYCCW();
                }
                this.boxes[facing.getHorizontalIndex()][n - 1] =
                        new AxisAlignedBB(minX / 16, 0, minZ / 16, maxX / 16, height / 16, maxZ / 16);
            }
        }
    }

    public PropertyInteger amount() {
        return this.amount;
    }

    public boolean isFlowers() {
        return this.flowers;
    }

    void setItem(Item item) {
        this.item = item;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, building);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(this.amount, 1 + (meta >> 2));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(this.amount) - 1) << 2;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    /** {@code SegmentableBlock.getStateForPlacement}: the first piece faces away from the placer. */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return this.boxes[state.getValue(FACING).getHorizontalIndex()][state.getValue(this.amount) - 1];
    }

    /** Leaf litter stands on any sturdy top face ({@code LeafLitterBlock.canSurvive}). */
    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        if (this.flowers) {
            return super.canBlockStay(world, pos, state);
        }
        return sturdyTop(world, pos.down());
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        if (this.flowers) {
            return super.canPlaceBlockAt(world, pos);
        }
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos) && sturdyTop(world, pos.down());
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(new ItemStack(this.item, state.getValue(this.amount)));
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.item);
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return this.flowers;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return this.flowers;
    }

    /** {@code FlowerBedBlock.performBonemeal}. */
    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        int n = state.getValue(this.amount);
        if (n < 4) {
            world.setBlockState(pos, state.withProperty(this.amount, n + 1), 2);
        } else {
            spawnAsEntity(world, pos, new ItemStack(this.item));
        }
    }

    /** @return true if one more piece can be added to this state */
    public static boolean canAddTo(IBlockState state, Item held) {
        if (!(state.getBlock() instanceof BlockSegmented)) {
            return false;
        }
        BlockSegmented block = (BlockSegmented) state.getBlock();
        return block.item == held && state.getValue(block.amount) < 4;
    }
}
