package com.exiledradio.trickytrialsbackport.ghast;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The dried ghast (1.21.6's {@code DriedGhastBlock}): 10 x 10 x 10 pixels, breaks at once, faces away from the
 * player who placed it. Random ticks schedule a tick 5000 ticks ahead while it is in water or not yet dry; that
 * tick hydrates it one stage in water (0 to 3, with the "feels better" sound) and hatches a ghastling facing its
 * way from stage 3, or dries it back one stage out of water. Rehydrating from dry takes four of those ticks, about
 * 20 minutes at the default random tick speed.
 *
 * <p>The water is Fluidlogged API's (Backport Core's {@link Waterloggable}), so this block has no
 * {@code waterlogged} property; its four facings and four stages fill the metadata.
 */
public class BlockDriedGhast extends BlockHorizontal implements Waterloggable {

    public static final int MAX_HYDRATION_LEVEL = 3;
    public static final PropertyInteger HYDRATION = PropertyInteger.create("hydration", 0, MAX_HYDRATION_LEVEL);
    public static final int HYDRATION_TICK_DELAY = 5000;
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(3 / 16.0D, 0.0D, 3 / 16.0D, 13 / 16.0D, 10 / 16.0D, 13 / 16.0D);

    static final Material MATERIAL = new Material(MapColor.GRAY) {
        @Override
        public EnumPushReaction getPushReaction() {
            return EnumPushReaction.NORMAL;
        }
    };

    public BlockDriedGhast(SoundType sound) {
        super(MATERIAL, MapColor.GRAY);
        this.setSoundType(sound);
        this.setHardness(0.0F);
        this.setTickRandomly(true);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(HYDRATION, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{FACING, HYDRATION});
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(HYDRATION, meta >> 2);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(HYDRATION) << 2;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
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

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Nullable
    @Override
    public PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos) {
        return PathNodeType.BLOCKED;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        world.playSound(null, pos, Waterlogging.hasWater(world, pos) ? GhastContent.DRIED_PLACE_IN_WATER : GhastContent.DRIED_PLACE,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void onWaterChanged(World world, BlockPos pos, IBlockState state, boolean hasWater) {
        // 1.21's placeLiquid: water poured in plays the soak sound.
        if (hasWater && !world.isRemote) {
            world.playSound(null, pos, GhastContent.DRIED_PLACE_IN_WATER, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static int hydration(IBlockState state) {
        return state.getValue(HYDRATION);
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        // 1.21 checks hasScheduledTick first; 1.12.2's scheduler already keeps one tick per block and position (and its
        // isBlockTickPending sees only the ticks being run now), so scheduling again changes nothing.
        if (Waterlogging.hasWater(world, pos) || state.getValue(HYDRATION) > 0) {
            world.scheduleUpdate(pos, this, HYDRATION_TICK_DELAY);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (Waterlogging.hasWater(world, pos)) {
            if (state.getValue(HYDRATION) < MAX_HYDRATION_LEVEL) {
                world.playSound(null, pos, GhastContent.DRIED_TRANSITION, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.setBlockState(pos, state.withProperty(HYDRATION, state.getValue(HYDRATION) + 1), 2);
            } else {
                this.spawnGhastling(world, pos, state);
            }
        } else {
            int level = state.getValue(HYDRATION);
            if (level > 0) {
                world.setBlockState(pos, state.withProperty(HYDRATION, level - 1), 2);
            }
        }
    }

    /** The block goes (its water stays), and a ghastling hatches at its bottom center, facing the block's way. */
    void spawnGhastling(World world, BlockPos pos, IBlockState state) {
        boolean water = Waterlogging.hasWater(world, pos);
        world.setBlockState(pos, water ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
        EntityHappyGhast ghast = new EntityHappyGhast(world);
        ghast.setGrowingAge(-24000);
        float yaw = state.getValue(FACING).getHorizontalAngle();
        ghast.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        ghast.rotationYawHead = yaw;
        ghast.renderYawOffset = yaw;
        world.spawnEntity(ghast);
        world.playSound(null, ghast.posX, ghast.posY, ghast.posZ, GhastContent.GHASTLING_SPAWN, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        if (!Waterlogging.hasWater(world, pos)) {
            if (random.nextInt(40) == 0 && isSoulGround(world.getBlockState(pos.down()))) {
                world.playSound(x, y, z, GhastContent.DRIED_AMBIENT, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
            }
            if (random.nextInt(6) == 0) {
                // 1.21's white smoke; 1.12.2's nearest is the white cloud puff.
                world.spawnParticle(EnumParticleTypes.CLOUD, x, y, z, 0.0D, 0.02D, 0.0D);
            }
        } else {
            if (random.nextInt(40) == 0) {
                world.playSound(x, y, z, GhastContent.DRIED_AMBIENT_WATER, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
            }
            if (random.nextInt(6) == 0) {
                world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, x + (random.nextFloat() * 2.0F - 1.0F) / 3.0F, y + 0.4D,
                        z + (random.nextFloat() * 2.0F - 1.0F) / 3.0F, 0.0D, random.nextFloat(), 0.0D);
            }
        }
    }

    /** Soul sand and Nether Update's soul soil (1.21's {@code triggers_ambient_dried_ghast_block_sounds}). */
    static boolean isSoulGround(IBlockState state) {
        Block b = state.getBlock();
        return b == Blocks.SOUL_SAND || b == Owners.block("soul_soil");
    }
}
