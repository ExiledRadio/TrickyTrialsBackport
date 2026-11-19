package com.exiledradio.trickytrialsbackport.shelf;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.21.9's shelf ({@code ShelfBlock}).
 *
 * <ul>
 *   <li>Three slots across its front. Using an unpowered shelf swaps the main-hand item with the slot clicked;
 *       using a powered one swaps its whole row (up to three shelves, {@link ShelfChain}) with the rightmost three,
 *       six or nine hotbar slots.</li>
 *   <li>Facing and power are metadata; the place in a powered row ({@link #SIDE_CHAIN}) is the tile entity's.</li>
 *   <li>A comparator behind it reads 1, 2 and 4 for the left, middle and right slots.</li>
 * </ul>
 * Waterloggable through Backport Core's adapter ({@link Waterloggable}), so with Fluidlogged API it holds water.
 */
public class BlockShelf extends Block implements Waterloggable {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyEnum<SideChainPart> SIDE_CHAIN = PropertyEnum.create("side_chain", SideChainPart.class);

    /** North-facing boxes (front toward -z): the top board, the back, the bottom board. */
    private static final AxisAlignedBB[] NORTH_BOXES = {
            box(0, 12, 11, 16, 16, 13), box(0, 0, 13, 16, 16, 16), box(0, 0, 11, 16, 4, 13)};
    private static final AxisAlignedBB NORTH_OUTLINE = box(0, 0, 11, 16, 16, 16);

    public final ShelfWood wood;

    public BlockShelf(ShelfWood wood, SoundType sound) {
        super(Material.WOOD, wood.mapColor);
        this.wood = wood;
        setHardness(2.0F);
        // 1.12.2 stores resistance tripled and reports it fifthed: 5 here is a modern 3.
        setResistance(5.0F);
        setSoundType(sound);
        setLightOpacity(0);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
                .withProperty(POWERED, false).withProperty(SIDE_CHAIN, SideChainPart.UNCONNECTED));
    }

    private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }

    /** Turns a north-facing box to face this way, about the block's center. */
    static AxisAlignedBB rotate(AxisAlignedBB b, EnumFacing facing) {
        switch (facing) {
            case SOUTH:
                return new AxisAlignedBB(1 - b.maxX, b.minY, 1 - b.maxZ, 1 - b.minX, b.maxY, 1 - b.minZ);
            case WEST:
                return new AxisAlignedBB(b.minZ, b.minY, 1 - b.maxX, b.maxZ, b.maxY, 1 - b.minX);
            case EAST:
                return new AxisAlignedBB(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX);
            default:
                return b;
        }
    }

    // --- states ----------------------------------------------------------------------------------------------------

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED, SIDE_CHAIN);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(POWERED, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(POWERED) ? 4 : 0);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        SideChainPart part = SideChainPart.UNCONNECTED;
        if (state.getValue(POWERED)) {
            TileEntity te = world instanceof ChunkCache
                    ? ((ChunkCache) world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK)
                    : world.getTileEntity(pos);
            if (te instanceof TileEntityShelf) {
                part = ((TileEntityShelf) te).chain();
            }
        }
        return state.withProperty(SIDE_CHAIN, part);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite())
                .withProperty(POWERED, world.isBlockPowered(pos));
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityShelf();
    }

    // --- shape -----------------------------------------------------------------------------------------------------

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
        return face == state.getValue(FACING).getOpposite() ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return rotate(NORTH_OUTLINE, state.getValue(FACING));
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox,
                                      List<AxisAlignedBB> boxes, @Nullable Entity entity, boolean actualState) {
        for (AxisAlignedBB b : NORTH_BOXES) {
            addCollisionBoxToList(pos, entityBox, boxes, rotate(b, state.getValue(FACING)));
        }
    }

    /** The three boards, so a click between them goes on to the back, as vanilla's shape does. */
    @Override
    @Nullable
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        RayTraceResult best = null;
        double bestDistance = Double.MAX_VALUE;
        for (AxisAlignedBB b : NORTH_BOXES) {
            RayTraceResult hit = rayTrace(pos, start, end, rotate(b, state.getValue(FACING)));
            if (hit != null) {
                double d = hit.hitVec.squareDistanceTo(start);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = hit;
                }
            }
        }
        return best;
    }

    // --- redstone --------------------------------------------------------------------------------------------------

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        // Vanilla's onPlace. 1.12.2 calls this only when the block appears; a change of power runs it from
        // neighborChanged instead.
        afterPowerChange(world, pos, state);
    }

    private static void afterPowerChange(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) {
            return;
        }
        if (state.getValue(POWERED)) {
            ShelfChain.updateSelfAndNeighborsOnPoweringUp(world, pos, state);
        } else {
            ShelfChain.updateNeighborsAfterPoweringDown(world, pos, state.getValue(FACING));
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (world.isRemote) {
            return;
        }
        boolean powered = world.isBlockPowered(pos);
        if (state.getValue(POWERED) != powered) {
            IBlockState now = state.withProperty(POWERED, powered);
            if (!powered) {
                ShelfChain.setPart(world, pos, SideChainPart.UNCONNECTED);
            }
            world.setBlockState(pos, now, 3);
            playSound(world, pos, powered ? ShelfContent.activate : ShelfContent.deactivate);
            afterPowerChange(world, pos, now);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityShelf) {
            InventoryHelper.dropInventoryItems(world, pos, (TileEntityShelf) te);
            world.updateComparatorOutputLevel(pos, this);
        }
        ShelfChain.updateNeighborsAfterPoweringDown(world, pos, state.getValue(FACING));
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    /**
     * Vanilla answers only a comparator reading from behind the shelf ({@code direction == facing.getOpposite()}).
     * 1.12.2 does not say which comparator asks, so the shelf answers when a comparator behind it (directly, or
     * through one solid block, as comparators read) faces it, and 0 otherwise.
     */
    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        if (!readFromBehind(world, pos, facing)) {
            return 0;
        }
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileEntityShelf ? signal((TileEntityShelf) te) : 0;
    }

    /** @return 1, 2 and 4 for the filled left, middle and right slots */
    public static int signal(TileEntityShelf shelf) {
        int out = 0;
        for (int i = 0; i < TileEntityShelf.MAX_ITEMS; i++) {
            if (!shelf.getStackInSlot(i).isEmpty()) {
                out |= 1 << i;
            }
        }
        return out;
    }

    private static boolean readFromBehind(World world, BlockPos pos, EnumFacing facing) {
        BlockPos behind = pos.offset(facing.getOpposite());
        if (isComparatorFacing(world.getBlockState(behind), facing)) {
            return true;
        }
        IBlockState between = world.getBlockState(behind);
        return between.isNormalCube() && isComparatorFacing(world.getBlockState(behind.offset(facing.getOpposite())), facing);
    }

    /** A comparator's FACING points at the block it reads. */
    private static boolean isComparatorFacing(IBlockState state, EnumFacing towardShelf) {
        return state.getBlock() instanceof BlockRedstoneComparator && state.getValue(BlockHorizontal.FACING) == towardShelf;
    }

    // --- use -------------------------------------------------------------------------------------------------------

    /**
     * {@code SelectableSlotContainer.getHitSlot}: the slot under the click on the shelf's front, left to right as seen
     * from in front; -1 for any other face.
     */
    public static int hitSlot(EnumFacing face, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (face != facing) {
            return -1;
        }
        float u;
        switch (face) {
            case NORTH: u = 1.0F - hitX; break;
            case SOUTH: u = hitX; break;
            case WEST: u = hitZ; break;
            case EAST: u = 1.0F - hitZ; break;
            default: return -1;
        }
        int section = (int) Math.floor(u * 16.0F / (16.0F / 3.0F));
        return Math.max(0, Math.min(2, section));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing face, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) {
            return false;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityShelf)) {
            return false;
        }
        int slot = hitSlot(face, state.getValue(FACING), hitX, hitY, hitZ);
        if (slot < 0) {
            return false;
        }
        ItemStack held = player.getHeldItemMainhand();
        if (world.isRemote) {
            // Vanilla's client answers PASS with an empty hand; the server still hears the click.
            return !held.isEmpty();
        }
        TileEntityShelf shelf = (TileEntityShelf) te;
        if (!state.getValue(POWERED)) {
            boolean took = swapSingleItem(held, player, shelf, slot);
            if (took) {
                playSound(world, pos, held.isEmpty() ? ShelfContent.takeItem : ShelfContent.singleSwap);
            } else {
                if (held.isEmpty()) {
                    return false;
                }
                playSound(world, pos, ShelfContent.placeItem);
            }
            return true;
        }
        if (swapHotbar(world, pos, player.inventory)) {
            playSound(world, pos, ShelfContent.multiSwap);
        }
        return true;
    }

    /** @return true if the slot had an item */
    static boolean swapSingleItem(ItemStack held, EntityPlayer player, TileEntityShelf shelf, int slot) {
        ItemStack old = shelf.swapItemNoUpdate(slot, held);
        ItemStack inHand = player.capabilities.isCreativeMode && old.isEmpty() ? held.copy() : old;
        player.inventory.setInventorySlotContents(player.inventory.currentItem, inHand);
        player.inventory.markDirty();
        shelf.markDirty();
        return !old.isEmpty();
    }

    /** The row's slots, left to right, against the hotbar's last three, six or nine. */
    static boolean swapHotbar(World world, BlockPos pos, InventoryPlayer inventory) {
        List<BlockPos> row = ShelfChain.allConnectedTo(world, pos);
        boolean changed = false;
        for (int i = 0; i < row.size(); i++) {
            TileEntity te = world.getTileEntity(row.get(i));
            if (!(te instanceof TileEntityShelf)) {
                continue;
            }
            TileEntityShelf shelf = (TileEntityShelf) te;
            for (int j = 0; j < shelf.getSizeInventory(); j++) {
                int hotbar = 9 - (row.size() - i) * shelf.getSizeInventory() + j;
                if (hotbar >= 0 && hotbar < InventoryPlayer.getHotbarSize()) {
                    ItemStack fromPlayer = inventory.removeStackFromSlot(hotbar);
                    ItemStack fromShelf = shelf.swapItemNoUpdate(j, fromPlayer);
                    if (!fromPlayer.isEmpty() || !fromShelf.isEmpty()) {
                        inventory.setInventorySlotContents(hotbar, fromShelf);
                        changed = true;
                    }
                }
            }
            inventory.markDirty();
            shelf.markDirty();
        }
        return changed;
    }

    private static void playSound(World world, BlockPos pos, SoundEvent sound) {
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    // --- fire ------------------------------------------------------------------------------------------------------

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return this.wood.flammable ? 20 : 0;
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return this.wood.flammable ? 30 : 0;
    }
}
