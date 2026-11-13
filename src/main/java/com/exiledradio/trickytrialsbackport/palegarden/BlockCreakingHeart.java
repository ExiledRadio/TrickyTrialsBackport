package com.exiledradio.trickytrialsbackport.palegarden;

import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The creaking heart: 1.21.4's {@code CreakingHeartBlock}, without the creaking (stream 13).
 *
 * <ul>
 *   <li>{@code axis} as a log; {@code creaking_heart_state} {@code uprooted}, {@code dormant} or
 *       {@code awake}: uprooted unless a pale oak log (or wood, stripped or not) of the same axis is on both
 *       ends; then awake at night (day time 12600-23400 in a world with a day) and dormant otherwise.
 *       Placed, it takes the clicked face's axis.</li>
 *   <li>Its tile ({@link TileCreakingHeart}) rechecks every 20-24 ticks while not uprooted, and a neighbor
 *       change rechecks it next tick, as vanilla's scheduled tick.</li>
 *   <li>Strength 10, the creaking heart's sounds; a heart in the trunk sometimes creaks at night (client,
 *       one time in 16, when it has pale oak logs on all six sides).</li>
 *   <li>Drops itself with Silk Touch, otherwise 1-3 resin clumps (more with Fortune, at most 9) when resin
 *       (stream 14) exists. A heart a tree grew ({@code natural}, kept in the tile because 1.12.2's
 *       metadata holds axis and state only) gives 20-24 experience to the player who breaks it, Silk Touch
 *       or not.</li>
 *   <li>Its comparator output comes from the tile: 0 without a creaking.</li>
 * </ul>
 */
public class BlockCreakingHeart extends Block {

    public enum State implements IStringSerializable {
        UPROOTED("uprooted"), DORMANT("dormant"), AWAKE("awake");

        private final String name;

        State(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    public static final PropertyEnum<EnumFacing.Axis> AXIS = BlockRotatedPillar.AXIS;
    public static final PropertyEnum<State> STATE = PropertyEnum.create("creaking_heart_state", State.class);

    public BlockCreakingHeart() {
        super(Material.WOOD, MapColor.ADOBE);
        setHardness(10.0F);
        setSoundType(PaleGardenContent.CREAKING_HEART);
        setDefaultState(this.blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y).withProperty(STATE, State.UPROOTED));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS, STATE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        int axis = meta & 3;
        int state = (meta >> 2) & 3;
        return getDefaultState().withProperty(AXIS, EnumFacing.Axis.values()[Math.min(axis, 2)])
                .withProperty(STATE, State.values()[Math.min(state, 2)]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AXIS).ordinal() | state.getValue(STATE).ordinal() << 2;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileCreakingHeart();
    }

    /** @return true when hearts are awake here now (modern {@code creaking_active}) */
    public static boolean active(World world) {
        if (!world.provider.isSurfaceWorld()) {
            return false;
        }
        long time = world.getWorldTime() % 24000L;
        if (time < 0) {
            time += 24000L;
        }
        return time >= 12600L && time < 23401L;
    }

    /** Pale oak logs, wood and their stripped forms: modern {@code #pale_oak_logs}. */
    public static boolean isPaleOakLog(IBlockState state) {
        Block b = state.getBlock();
        return PaleGardenContent.set != null && (b == PaleGardenContent.set.log || b == PaleGardenContent.set.wood
                || b == PaleGardenContent.set.strippedLog || b == PaleGardenContent.set.strippedWood);
    }

    /** {@code hasRequiredLogs}: a pale oak log of the same axis on both ends. */
    public static boolean hasRequiredLogs(IBlockState state, IBlockAccess world, BlockPos pos) {
        EnumFacing.Axis axis = state.getValue(AXIS);
        for (EnumFacing.AxisDirection dir : EnumFacing.AxisDirection.values()) {
            IBlockState s = world.getBlockState(pos.offset(EnumFacing.getFacingFromAxis(dir, axis)));
            if (!isPaleOakLog(s) || !s.getPropertyKeys().contains(AXIS) || s.getValue(AXIS) != axis) {
                return false;
            }
        }
        return true;
    }

    private static boolean surroundedByLogs(IBlockAccess world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.values()) {
            if (!isPaleOakLog(world.getBlockState(pos.offset(face)))) {
                return false;
            }
        }
        return true;
    }

    /** {@code CreakingHeartBlock.updateState}: an uprooted heart with its logs wakes or sleeps by the time. */
    static IBlockState updateState(IBlockState state, World world, BlockPos pos) {
        if (hasRequiredLogs(state, world, pos) && state.getValue(STATE) == State.UPROOTED) {
            return state.withProperty(STATE, active(world) ? State.AWAKE : State.DORMANT);
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return updateState(getDefaultState().withProperty(AXIS, facing.getAxis()), world, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        world.scheduleUpdate(pos, this, 1);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        IBlockState now = updateState(state, world, pos);
        if (now != state) {
            world.setBlockState(pos, now, 3);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (active(world) && state.getValue(STATE) != State.UPROOTED && rand.nextInt(16) == 0 && surroundedByLogs(world, pos)) {
            world.playSound(pos.getX(), pos.getY(), pos.getZ(), PaleGardenContent.HEART_IDLE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        if (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90) {
            EnumFacing.Axis axis = state.getValue(AXIS);
            if (axis == EnumFacing.Axis.X) {
                return state.withProperty(AXIS, EnumFacing.Axis.Z);
            }
            if (axis == EnumFacing.Axis.Z) {
                return state.withProperty(AXIS, EnumFacing.Axis.X);
            }
        }
        return state;
    }

    // ---------------------------------------------------------------------------------------------
    // Comparator

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        if (state.getValue(STATE) == State.UPROOTED) {
            return 0;
        }
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileCreakingHeart ? ((TileCreakingHeart) tile).outputSignal() : 0;
    }

    // ---------------------------------------------------------------------------------------------
    // Breaking

    /** {@code playerWillDestroy}: tells the creaking (stream 13) and gives a natural heart's experience. */
    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileCreakingHeart) {
            TileCreakingHeart heart = (TileCreakingHeart) tile;
            heart.removed(player);
            if (!world.isRemote && heart.isNatural() && !player.capabilities.isCreativeMode && !player.isSpectator()) {
                int xp = MathHelper.getInt(world.rand, 20, 24);
                while (xp > 0) {
                    int orb = EntityXPOrb.getXPSplit(xp);
                    xp -= orb;
                    world.spawnEntity(new EntityXPOrb(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, orb));
                }
            }
        }
        super.onBlockHarvested(world, pos, state, player);
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileCreakingHeart) {
            ((TileCreakingHeart) tile).removed(explosion.getExplosivePlacedBy());
        }
        super.onBlockExploded(world, pos, explosion);
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        Item resin = Owners.ours("resin_clump");
        return resin != null ? resin : net.minecraft.init.Items.AIR;
    }

    /** {@code blocks/creaking_heart}: 1-3 resin clumps, plus 0 to Fortune more, at most 9. */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Item resin = Owners.ours("resin_clump");
        if (resin == null) {
            return;
        }
        Random rand = world instanceof World ? ((World) world).rand : RANDOM;
        int count = 1 + rand.nextInt(3);
        if (fortune > 0) {
            count += rand.nextInt(fortune + 1);
        }
        drops.add(new ItemStack(resin, Math.min(count, 9)));
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this);
    }
}
