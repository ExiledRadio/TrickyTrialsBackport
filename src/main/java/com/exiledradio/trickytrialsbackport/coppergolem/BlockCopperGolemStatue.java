package com.exiledradio.trickytrialsbackport.coppergolem;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.trickytrialsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Random;

/**
 * 1.21.9's copper golem statue ({@code CopperGolemStatueBlock}, {@code WeatheringCopperGolemStatueBlock}): what a
 * fully oxidized copper golem turns into. Four poses (standing, sitting, running, star), cycled by using it with
 * anything but an axe (honeycomb waxes an unwaxed one instead); a comparator reads the pose (1-4). 10 x 14 x 10 pixels,
 * hardness 3, resistance 6, its own sounds, drops by hand, broken by pistons. The unwaxed ones oxidize, an axe scrapes
 * a stage off and brings an unaffected unwaxed statue back to life as a copper golem ({@link CopperGolemEvents}).
 *
 * <p>Metadata: facing (2 bits) and pose (2 bits), all sixteen values. The item keeps the pose in its damage value and
 * the custom name the golem had. The water is Fluidlogged API's (Backport Core's {@link Waterloggable}).
 */
public class BlockCopperGolemStatue extends Block implements Waterloggable {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyEnum<Pose> POSE = PropertyEnum.create("copper_golem_pose", Pose.class);
    /** {@code Block.column(10, 0, 14)}. */
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(3.0D / 16, 0.0D, 3.0D / 16, 13.0D / 16, 14.0D / 16, 13.0D / 16);

    /** 1.21's statue material: solid, mined by hand, destroyed by pistons. */
    private static final Material STATUE = new Material(MapColor.ADOBE) {
        @Override
        public EnumPushReaction getPushReaction() {
            return EnumPushReaction.DESTROY;
        }
    };

    public final int stage;
    public final boolean waxed;

    public BlockCopperGolemStatue(int stage, boolean waxed) {
        super(STATUE, CopperGolemContent.MAP_COLORS[stage]);
        this.stage = stage;
        this.waxed = waxed;
        setHardness(3.0F);
        // 1.21's blast resistance 6: 1.12.2 stores three times this and divides by five.
        setResistance(10.0F);
        setSoundType(CopperGolemContent.STATUE_SOUND);
        setCreativeTab(CreativeTabs.DECORATIONS);
        setTickRandomly(!waxed && stage < 3);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(POSE, Pose.STANDING));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POSE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(POSE, Pose.byId(meta >> 2));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(POSE).ordinal() << 2;
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return CopperGolemContent.MAP_COLORS[this.stage];
    }

    /** The pose comes from the item's damage value. */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(POSE, Pose.byId(meta));
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCopperGolemStatue && stack.hasDisplayName()) {
            ((TileEntityCopperGolemStatue) te).customName = stack.getDisplayName();
            te.markDirty();
        }
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE;
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

    /** Drawn whole by its tile entity renderer; the block model is only the breaking particle. */
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean hasCustomBreakingProgress(IBlockState state) {
        return true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityCopperGolemStatue();
    }

    /** Anything but an axe turns the statue to its next pose (honeycomb on an unwaxed one waxes it instead). */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (CopperGolemEvents.isAxe(held)) {
            return false;
        }
        if (!world.isRemote) {
            updatePose(world, state, pos);
        }
        return true;
    }

    static void updatePose(World world, IBlockState state, BlockPos pos) {
        world.playSound(null, pos, CopperGolemContent.BECOME_STATUE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.setBlockState(pos, state.withProperty(POSE, state.getValue(POSE).next()), 3);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return state.getValue(POSE).ordinal() + 1;
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!this.waxed && ModConfig.copperGolem.oxidation && !world.isRemote) {
            Oxidation.randomTick(world, pos, state, random);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    }

    /** Aging, waxing and scraping keep the tile entity (and so the name); only a real removal drops anything. */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (world.getBlockState(pos).getBlock() instanceof BlockCopperGolemStatue) {
            return;
        }
        world.updateComparatorOutputLevel(pos, this);
        super.breakBlock(world, pos, state);
    }

    // Drops: the tile entity (the name) must still be there when the drops are made.

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        return willHarvest || super.removedByPlayer(state, world, pos, player, false);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack tool) {
        super.harvestBlock(world, player, pos, state, te, tool);
        world.setBlockToAir(pos);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(item(world, pos, state));
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return item(world, pos, state);
    }

    /** 1.21's loot table and clone stack: this statue with its pose and the golem's name. */
    ItemStack item(IBlockAccess world, BlockPos pos, IBlockState state) {
        ItemStack stack = new ItemStack(Item.getItemFromBlock(this), 1, state.getValue(POSE).ordinal());
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCopperGolemStatue && ((TileEntityCopperGolemStatue) te).customName != null) {
            stack.setStackDisplayName(((TileEntityCopperGolemStatue) te).customName);
        }
        return stack;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(POSE).ordinal();
    }

    /** 1.21.9's {@code CopperGolemStatueBlock.Pose}. */
    public enum Pose implements IStringSerializable {
        STANDING,
        SITTING,
        RUNNING,
        STAR;

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** 1.21's {@code getNextPose}: star wraps round to standing. */
        public Pose next() {
            return byId(ordinal() + 1);
        }

        /** Out of range is standing (1.21's {@code OutOfBoundsStrategy.ZERO}). */
        public static Pose byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : STANDING;
        }
    }
}
