package com.exiledradio.trickytrialsbackport.trial;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The vault block: {@code facing} (toward the player who placed it), {@code vault_state} and
 * {@code ominous}; light 6 inactive, 12 otherwise. Hardness and blast resistance 50, mined with a
 * pickaxe, drops nothing, never pushed. Its 32 states don't fit 16 metadata values: the facing and
 * the phase are the metadata, and {@code ominous}, which never changes once a vault is made, is kept by
 * its tile entity and shown through {@link #getActualState}.
 */
public class BlockVault extends Block {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyEnum<VaultPhase> STATE = PropertyEnum.create("vault_state", VaultPhase.class);
    public static final PropertyBool OMINOUS = PropertyBool.create("ominous");

    public BlockVault(SoundType sound) {
        super(Material.ROCK, MapColor.STONE);
        setHardness(50.0F);
        setResistance(50.0F * 5.0F / 3.0F);
        setSoundType(sound);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(STATE, VaultPhase.INACTIVE)
                .withProperty(OMINOUS, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, STATE, OMINOUS);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(STATE).ordinal() << 2;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(STATE, VaultPhase.values()[meta >> 2 & 3]);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileEntityVault ? state.withProperty(OMINOUS, ((TileEntityVault) te).ominousFlag()) : state;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getBlock() == this ? state.getValue(STATE).light : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightValue(IBlockState state) {
        return state.getValue(STATE).light;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    /**
     * Placing {@code ominous=true} (as a structure does) makes an ominous vault: the flag, the ominous
     * trial key and the ominous reward table.
     */
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        TileEntityVault vault = new TileEntityVault();
        if (state.getValue(OMINOUS)) {
            vault.setConfig(VaultSettings.ominous());
            vault.setOminous(true);
        }
        return vault;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return net.minecraft.init.Items.AIR;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return false;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.BLOCK;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /** 1.21's {@code useItemOn}: any item, while active, is tried as the key. */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty() || state.getValue(STATE) != VaultPhase.ACTIVE) {
            return false;
        }
        if (world instanceof WorldServer) {
            TileEntity te = world.getTileEntity(pos);
            if (!(te instanceof TileEntityVault)) {
                return false;
            }
            if (player instanceof EntityPlayerMP) {
                boolean ominous = ((TileEntityVault) te).ominousFlag();
                (ominous ? TrialContent.OMINOUS_VAULT_USED : TrialContent.VAULT_USED).trigger((EntityPlayerMP) player, held.copy());
            }
            ((TileEntityVault) te).tryInsertKey((WorldServer) world, player, held);
        }
        return true;
    }
}
