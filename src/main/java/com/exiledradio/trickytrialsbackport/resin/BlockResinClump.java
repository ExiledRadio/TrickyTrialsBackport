package com.exiledradio.trickytrialsbackport.resin;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The resin clump: 1.21.4's {@code MultifaceBlock} with resin's properties. A thin layer on any of the six
 * faces of its space, each held by a full face behind it; no collision; replaceable; instant to break; drops
 * one clump per face with any tool or none; pushed pistons destroy it; lava can set it alight.
 *
 * <h2>Four blocks, one item</h2>
 * Six faces are 64 combinations and 1.12 gives a block 16 metadata values. So there are four blocks that
 * differ in whether they have the up and the down face ({@code resin_clump}, {@code resin_clump_down},
 * {@code resin_clump_up}, {@code resin_clump_up_down}), each keeping its four side faces in metadata. Everything
 * outside this class goes through {@link #of(int)} and {@link #faces(IBlockState)}, which speak in one six-bit
 * face mask (bit = {@link EnumFacing#getIndex()}); all four share the item of the first.
 *
 * <p>Not waterloggable yet (Core's adapter, {@code docs/CORE_REQUESTS.md} #7): it is never placed in water.
 */
public class BlockResinClump extends Block {

    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool EAST = PropertyBool.create("east");
    private static final PropertyBool[] SIDES = {NORTH, SOUTH, WEST, EAST};
    private static final EnumFacing[] SIDE_FACES = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};

    /** The registry name of each of the four, indexed by (down ? 1 : 0) | (up ? 2 : 0). */
    public static final String[] NAMES = {"resin_clump", "resin_clump_down", "resin_clump_up", "resin_clump_up_down"};

    /** The four blocks, in {@link #NAMES} order; filled as they are constructed. */
    private static final BlockResinClump[] BLOCKS = new BlockResinClump[4];

    /** {@code Block.boxZ(16, 0, 1)} turned to each face: one pixel thick. */
    private static final AxisAlignedBB[] FACE_BOXES = new AxisAlignedBB[6];

    static {
        double t = 1.0 / 16.0;
        FACE_BOXES[EnumFacing.DOWN.getIndex()] = new AxisAlignedBB(0, 0, 0, 1, t, 1);
        FACE_BOXES[EnumFacing.UP.getIndex()] = new AxisAlignedBB(0, 1 - t, 0, 1, 1, 1);
        FACE_BOXES[EnumFacing.NORTH.getIndex()] = new AxisAlignedBB(0, 0, 0, 1, 1, t);
        FACE_BOXES[EnumFacing.SOUTH.getIndex()] = new AxisAlignedBB(0, 0, 1 - t, 1, 1, 1);
        FACE_BOXES[EnumFacing.WEST.getIndex()] = new AxisAlignedBB(0, 0, 0, t, 1, 1);
        FACE_BOXES[EnumFacing.EAST.getIndex()] = new AxisAlignedBB(1 - t, 0, 0, 1, 1, 1);
    }

    private final int vertical;

    /** @param index 0-3, the place in {@link #NAMES} */
    public BlockResinClump(int index, SoundType sound) {
        // Material.VINE: replaceable, destroyed by pistons, alight from lava (ignitedByLava), no tool needed.
        super(Material.VINE, MapColor.ORANGE_STAINED_HARDENED_CLAY);
        this.vertical = index;
        BLOCKS[index] = this;
        setHardness(0.0F);
        setSoundType(sound);
        setDefaultState(this.blockState.getBaseState().withProperty(NORTH, false).withProperty(SOUTH, false)
                .withProperty(WEST, false).withProperty(EAST, false));
    }

    // ---------------------------------------------------------------------------------------------
    // Faces

    /** @return true once the four blocks exist (the stream is on) */
    public static boolean registered() {
        return BLOCKS[0] != null;
    }

    /** @return the first of the four blocks, whose item is the resin clump; null with the stream off */
    @Nullable
    public static BlockResinClump base() {
        return BLOCKS[0];
    }

    /** @return the state with exactly these faces, or air for none */
    public static IBlockState of(int mask) {
        mask &= 63;
        if (mask == 0) {
            return Blocks.AIR.getDefaultState();
        }
        IBlockState s = BLOCKS[mask & 3].getDefaultState();
        for (int i = 0; i < 4; i++) {
            s = s.withProperty(SIDES[i], (mask & (1 << SIDE_FACES[i].getIndex())) != 0);
        }
        return s;
    }

    /** @return the six-bit face mask of a state; 0 if it is not a resin clump */
    public static int faces(IBlockState state) {
        if (!(state.getBlock() instanceof BlockResinClump)) {
            return 0;
        }
        int mask = ((BlockResinClump) state.getBlock()).vertical;
        for (int i = 0; i < 4; i++) {
            if (state.getValue(SIDES[i])) {
                mask |= 1 << SIDE_FACES[i].getIndex();
            }
        }
        return mask;
    }

    public static boolean has(int mask, EnumFacing face) {
        return (mask & (1 << face.getIndex())) != 0;
    }

    /**
     * {@code MultifaceBlock.canAttachTo}: the block on the {@code face} side of {@code pos} shows a full face
     * toward it.
     */
    public static boolean supported(IBlockAccess world, BlockPos pos, EnumFacing face) {
        BlockPos behind = pos.offset(face);
        return world.getBlockState(behind).getBlockFaceShape(world, behind, face.getOpposite()) == BlockFaceShape.SOLID;
    }

    /** @return true if a face of resin can be added on {@code face} in {@code pos} (air, or a clump without it) */
    public static boolean canAdd(IBlockAccess world, BlockPos pos, EnumFacing face) {
        IBlockState s = world.getBlockState(pos);
        boolean open = s.getBlock() instanceof BlockResinClump || s.getBlock().isAir(s, world, pos)
                || s.getBlock().isReplaceable(world, pos) && !s.getMaterial().isLiquid();
        return open && !has(faces(s), face) && supported(world, pos, face);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, NORTH, SOUTH, WEST, EAST);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState s = getDefaultState();
        for (int i = 0; i < 4; i++) {
            s = s.withProperty(SIDES[i], (meta & (1 << i)) != 0);
        }
        return s;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = 0;
        for (int i = 0; i < 4; i++) {
            if (state.getValue(SIDES[i])) {
                meta |= 1 << i;
            }
        }
        return meta;
    }

    private static IBlockState turned(IBlockState state, java.util.function.Function<EnumFacing, EnumFacing> turn) {
        int mask = faces(state);
        int out = mask & 3;
        for (EnumFacing f : SIDE_FACES) {
            if (has(mask, f)) {
                out |= 1 << turn.apply(f).getIndex();
            }
        }
        return of(out);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return turned(state, rot::rotate);
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return turned(state, mirror::mirror);
    }

    // ---------------------------------------------------------------------------------------------
    // Block

    /** As an item it goes wherever one of the six neighbors offers a full face ({@link ItemResinClump}). */
    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        if (!s.getBlock().isReplaceable(world, pos) || s.getMaterial().isLiquid()) {
            return false;
        }
        for (EnumFacing f : EnumFacing.values()) {
            if (supported(world, pos, f)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    /**
     * {@code updateShape}: a face whose support goes is lost without a drop; when the last one goes the block is
     * destroyed as vanilla's {@code updateOrDestroy} does, dropping what it had.
     */
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        int mask = faces(state);
        int keep = 0;
        for (EnumFacing f : EnumFacing.values()) {
            if (has(mask, f) && supported(world, pos, f)) {
                keep |= 1 << f.getIndex();
            }
        }
        if (keep == mask) {
            return;
        }
        if (keep == 0) {
            world.destroyBlock(pos, true);
        } else {
            world.setBlockState(pos, of(keep), 3);
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
        return BlockFaceShape.UNDEFINED;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        int mask = faces(state);
        AxisAlignedBB box = null;
        for (EnumFacing f : EnumFacing.values()) {
            if (has(mask, f)) {
                box = box == null ? FACE_BOXES[f.getIndex()] : box.union(FACE_BOXES[f.getIndex()]);
            }
        }
        return box == null ? FULL_BLOCK_AABB : box;
    }

    /** Aimed at by face, so the empty middle of a clump on two faces is not part of it. */
    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        int mask = faces(state);
        RayTraceResult best = null;
        double bestDistance = Double.MAX_VALUE;
        for (EnumFacing f : EnumFacing.values()) {
            if (has(mask, f)) {
                RayTraceResult hit = rayTrace(pos, start, end, FACE_BOXES[f.getIndex()]);
                if (hit != null) {
                    double d = hit.hitVec.squareDistanceTo(start);
                    if (d < bestDistance) {
                        bestDistance = d;
                        best = hit;
                    }
                }
            }
        }
        return best;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /** {@code blocks/resin_clump}: one clump per face, with or without a tool. */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        int n = Integer.bitCount(faces(state));
        if (n > 0) {
            drops.add(new ItemStack(BLOCKS[0], n));
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(BLOCKS[0]);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(BLOCKS[0]);
    }
}
