package com.exiledradio.trickytrialsbackport.palegarden;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Pale moss carpet: 1.21.4's {@code MossyCarpetBlock}.
 *
 * <h2>States</h2>
 * {@code bottom} (a carpet on the ground) and a wall side per horizontal face, {@code none}, {@code low} or
 * {@code tall}, under 1.21's names, so its multipart blockstate is vanilla's. 1.12.2 stores four bits:
 * <ul>
 *   <li>meta 0, a carpet: its sides always grow up any wall beside it ({@code getUpdatedState(..., true)}),
 *       so they are worked out from the neighbors each time, never stored;</li>
 *   <li>meta 1-15, a "topper" (no bottom, sitting on a carpet): one bit per face that has a side, since
 *       vanilla keeps a lost side lost and draws only some at random when it grows.</li>
 * </ul>
 * {@code low} or {@code tall} is then picked from the block above, and a topper drops a side the carpet
 * under it lacks, as vanilla's {@code getUpdatedState}.
 *
 * <h2>Growing</h2>
 * Placed by a player or by worldgen ({@link #placeAt}), a carpet next to a wall may grow a topper above it,
 * each side with chance one half; bone meal grows one with every side it can have. A topper drops nothing.
 */
public class BlockPaleMossCarpet extends Block implements IGrowable {

    public enum Side implements IStringSerializable {
        NONE("none"), LOW("low"), TALL("tall");

        private final String name;

        Side(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    public static final PropertyBool BOTTOM = PropertyBool.create("bottom");
    public static final PropertyEnum<Side> NORTH = PropertyEnum.create("north", Side.class);
    public static final PropertyEnum<Side> EAST = PropertyEnum.create("east", Side.class);
    public static final PropertyEnum<Side> SOUTH = PropertyEnum.create("south", Side.class);
    public static final PropertyEnum<Side> WEST = PropertyEnum.create("west", Side.class);

    private static final AxisAlignedBB CARPET = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.0625D, 1.0D);

    public BlockPaleMossCarpet() {
        super(Material.CARPET, MapColor.SILVER);
        setHardness(0.1F);
        setSoundType(PaleGardenContent.MOSS_CARPET);
        setDefaultState(this.blockState.getBaseState().withProperty(BOTTOM, true).withProperty(NORTH, Side.NONE)
                .withProperty(EAST, Side.NONE).withProperty(SOUTH, Side.NONE).withProperty(WEST, Side.NONE));
    }

    public static PropertyEnum<Side> property(EnumFacing face) {
        switch (face) {
            case NORTH:
                return NORTH;
            case EAST:
                return EAST;
            case SOUTH:
                return SOUTH;
            default:
                return WEST;
        }
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BOTTOM, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        if (meta == 0) {
            return getDefaultState();
        }
        IBlockState state = getDefaultState().withProperty(BOTTOM, false);
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            if ((meta & (1 << face.getHorizontalIndex())) != 0) {
                state = state.withProperty(property(face), Side.LOW);
            }
        }
        return state;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        if (state.getValue(BOTTOM)) {
            return 0;
        }
        int meta = 0;
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            if (state.getValue(property(face)) != Side.NONE) {
                meta |= 1 << face.getHorizontalIndex();
            }
        }
        return meta;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return updated(state, world, pos, false);
    }

    // ---------------------------------------------------------------------------------------------
    // MossyCarpetBlock's rules

    private static boolean hasFaces(IBlockState state) {
        if (state.getValue(BOTTOM)) {
            return true;
        }
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            if (state.getValue(property(face)) != Side.NONE) {
                return true;
            }
        }
        return false;
    }

    /** {@code MultifaceBlock.canAttachTo}: the neighbor's facing side is a full face. */
    private static boolean canSupportAtFace(IBlockAccess world, BlockPos pos, EnumFacing face) {
        BlockPos at = pos.offset(face);
        IBlockState s = world.getBlockState(at);
        return s.getBlockFaceShape(world, at, face.getOpposite()) == BlockFaceShape.SOLID;
    }

    /** {@code getUpdatedState}. */
    static IBlockState updated(IBlockState state, IBlockAccess world, BlockPos pos, boolean placing) {
        IBlockState above = null;
        IBlockState below = null;
        placing |= state.getValue(BOTTOM);
        Block carpet = state.getBlock();
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            PropertyEnum<Side> prop = property(face);
            Side side = canSupportAtFace(world, pos, face) ? (placing ? Side.LOW : state.getValue(prop)) : Side.NONE;
            if (side == Side.LOW || side == Side.TALL && !placing) {
                // The stored side is "present"; whether it reads low or tall is the block above's business.
                side = Side.LOW;
                if (above == null) {
                    above = world.getBlockState(pos.up());
                }
                if (above.getBlock() == carpet && !above.getValue(BOTTOM) && storedSide(above, face)) {
                    side = Side.TALL;
                }
                if (!state.getValue(BOTTOM)) {
                    if (below == null) {
                        below = world.getBlockState(pos.down());
                    }
                    if (below.getBlock() == carpet && !hasSideBelow(world, pos.down(), below, face)) {
                        side = Side.NONE;
                    }
                }
            }
            state = state.withProperty(prop, side);
        }
        return state;
    }

    /** A topper's stored side (its metadata bit), as the block above sees it. */
    private static boolean storedSide(IBlockState topper, EnumFacing face) {
        return topper.getValue(property(face)) != Side.NONE;
    }

    /** Whether the carpet below shows a side on this face (worked out, since a carpet stores none). */
    private static boolean hasSideBelow(IBlockAccess world, BlockPos pos, IBlockState below, EnumFacing face) {
        if (below.getValue(BOTTOM)) {
            return canSupportAtFace(world, pos, face);
        }
        return below.getValue(property(face)) != Side.NONE && canSupportAtFace(world, pos, face);
    }

    /** {@code createTopperWithSideChance}: the topper this carpet would grow above itself, or null. */
    @Nullable
    private IBlockState topper(IBlockAccess world, BlockPos pos, Random rand, boolean always) {
        BlockPos up = pos.up();
        IBlockState above = world.getBlockState(up);
        boolean carpetAbove = above.getBlock() == this;
        if ((carpetAbove && above.getValue(BOTTOM)) || !carpetAbove && !above.getBlock().isReplaceable(world, up)) {
            return null;
        }
        IBlockState top = updated(getDefaultState().withProperty(BOTTOM, false), world, up, true);
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            PropertyEnum<Side> prop = property(face);
            if (top.getValue(prop) != Side.NONE && !(always || rand.nextBoolean())) {
                top = top.withProperty(prop, Side.NONE);
            }
        }
        if (!hasFaces(top) || carpetAbove && getMetaFromState(top) == getMetaFromState(above)) {
            return null;
        }
        return top;
    }

    /** {@code MossyCarpetBlock.placeAt}: a carpet, and a topper with sides at chance one half. */
    public static void placeAt(World world, BlockPos pos, Random rand, int flags) {
        BlockPaleMossCarpet block = PaleGardenContent.mossCarpet;
        world.setBlockState(pos, block.getDefaultState(), flags);
        IBlockState top = block.topper(world, pos, rand, false);
        if (top != null) {
            world.setBlockState(pos.up(), top, flags);
        }
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote) {
            IBlockState top = topper(world, pos, world.rand, false);
            if (top != null) {
                world.setBlockState(pos.up(), top, 3);
            }
        }
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && !world.isAirBlock(pos.down());
    }

    private boolean canStay(IBlockState state, World world, BlockPos pos) {
        IBlockState below = world.getBlockState(pos.down());
        return state.getValue(BOTTOM) ? !world.isAirBlock(pos.down()) : below.getBlock() == this && below.getValue(BOTTOM);
    }

    /** {@code updateShape}: gone when unsupported or left with no face; a lost topper side stays lost. */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!canStay(state, world, pos)) {
            world.destroyBlock(pos, true);
            return;
        }
        if (state.getValue(BOTTOM)) {
            return;
        }
        IBlockState now = updated(state, world, pos, false);
        if (!hasFaces(now)) {
            world.destroyBlock(pos, true);
        } else if (getMetaFromState(now) != getMetaFromState(state)) {
            world.setBlockState(pos, now, 2);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Shape

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        IBlockState actual = state.getActualState(world, pos);
        double minX = 1, minY = 1, minZ = 1, maxX = 0, maxY = 0, maxZ = 0;
        boolean any = false;
        if (actual.getValue(BOTTOM)) {
            minX = 0; minY = 0; minZ = 0; maxX = 1; maxY = 0.0625D; maxZ = 1;
            any = true;
        }
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            Side side = actual.getValue(property(face));
            if (side == Side.NONE) {
                continue;
            }
            AxisAlignedBB box = wall(face, side);
            minX = Math.min(minX, box.minX); minY = Math.min(minY, box.minY); minZ = Math.min(minZ, box.minZ);
            maxX = Math.max(maxX, box.maxX); maxY = Math.max(maxY, box.maxY); maxZ = Math.max(maxZ, box.maxZ);
            any = true;
        }
        return any ? new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ) : FULL_BLOCK_AABB;
    }

    /** A side: 1/16 thick on its face, 10/16 high when low, full when tall. */
    private static AxisAlignedBB wall(EnumFacing face, Side side) {
        double h = side == Side.TALL ? 1.0D : 0.625D;
        switch (face) {
            case NORTH:
                return new AxisAlignedBB(0, 0, 0, 1, h, 0.0625D);
            case SOUTH:
                return new AxisAlignedBB(0, 0, 0.9375D, 1, h, 1);
            case WEST:
                return new AxisAlignedBB(0, 0, 0, 0.0625D, h, 1);
            default:
                return new AxisAlignedBB(0.9375D, 0, 0, 1, h, 1);
        }
    }

    /** Only the carpet itself collides; its sides are walked through. */
    @Override
    @SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox,
                                      List<AxisAlignedBB> boxes, @Nullable Entity entity, boolean actual) {
        if (state.getValue(BOTTOM)) {
            addCollisionBoxToList(pos, entityBox, boxes, CARPET);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(BOTTOM) ? CARPET : NULL_AABB;
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
    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
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

    // ---------------------------------------------------------------------------------------------
    // Drops: only a carpet drops (blocks/pale_moss_carpet: bottom=true)

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return state.getValue(BOTTOM) ? Item.getItemFromBlock(this) : net.minecraft.init.Items.AIR;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target, World world, BlockPos pos,
                                  net.minecraft.entity.player.EntityPlayer player) {
        return new ItemStack(this);
    }

    // ---------------------------------------------------------------------------------------------
    // Bone meal: a carpet grows its topper with every side it can have.

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return state.getValue(BOTTOM) && topper(world, pos, world.rand, true) != null;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        IBlockState top = topper(world, pos, rand, true);
        if (top != null) {
            world.setBlockState(pos.up(), top, 3);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Rotation and mirroring move the stored sides.

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        IBlockState out = state;
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            out = out.withProperty(property(rot.rotate(face)), state.getValue(property(face)));
        }
        return out;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        IBlockState out = state;
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            out = out.withProperty(property(mirror.mirror(face)), state.getValue(property(face)));
        }
        return out;
    }
}
