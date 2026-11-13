package com.exiledradio.trickytrialsbackport.trial;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The trial spawner block: {@code trial_spawner_state} (six phases) and {@code ominous}, light 0, 4 or 8
 * by phase. Hardness and blast resistance 50, mined with a pickaxe, drops nothing (not even with Silk
 * Touch), never pushed. Its 12 states take 12 of 16 metadata values.
 */
public class BlockTrialSpawner extends Block implements ITileEntityProvider {

    public static final PropertyEnum<TrialSpawnerPhase> STATE = PropertyEnum.create("trial_spawner_state", TrialSpawnerPhase.class);
    public static final PropertyBool OMINOUS = PropertyBool.create("ominous");

    public BlockTrialSpawner(SoundType sound) {
        super(Material.ROCK, MapColor.STONE);
        setHardness(50.0F);
        setResistance(50.0F * 5.0F / 3.0F);
        setSoundType(sound);
        setDefaultState(this.blockState.getBaseState().withProperty(STATE, TrialSpawnerPhase.INACTIVE).withProperty(OMINOUS, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, STATE, OMINOUS);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STATE).ordinal() | (state.getValue(OMINOUS) ? 8 : 0);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        TrialSpawnerPhase[] phases = TrialSpawnerPhase.values();
        return getDefaultState().withProperty(STATE, phases[Math.min(meta & 7, phases.length - 1)]).withProperty(OMINOUS, (meta & 8) != 0);
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
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityTrialSpawner();
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

    /** A spawn egg sets the mob of both configs, as 1.21's {@code SpawnEggItem.useOn}. */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (!(held.getItem() instanceof ItemMonsterPlacer)) {
            return false;
        }
        ResourceLocation id = ItemMonsterPlacer.getNamedIdFrom(held);
        if (id == null) {
            return false;
        }
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityTrialSpawner) {
                ((TileEntityTrialSpawner) te).overrideEntityToSpawn(id.toString());
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
            }
        }
        return true;
    }
}
