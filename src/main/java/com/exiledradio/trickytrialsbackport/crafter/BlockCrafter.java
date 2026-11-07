package com.exiledradio.trickytrialsbackport.crafter;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The crafter (1.21): a 3x3 grid that crafts once per redstone pulse and ejects the result from its front.
 *
 * <p>Stored: {@link #ORIENTATION} (metadata 0-11). Shown: {@link #TRIGGERED} and {@link #CRAFTING}, read from
 * the tile entity, which tells clients when they change. 1.21.11's numbers: hardness 1.5, blast resistance
 * 3.5, pickaxe, a craft 4 ticks after the pulse.
 */
public final class BlockCrafter extends BlockContainer {

    public static final PropertyEnum<CrafterOrientation> ORIENTATION = PropertyEnum.create("orientation", CrafterOrientation.class);
    public static final PropertyBool TRIGGERED = PropertyBool.create("triggered");
    public static final PropertyBool CRAFTING = PropertyBool.create("crafting");

    static final int DELAY = 4;

    public BlockCrafter() {
        super(Material.ROCK);
        setHardness(1.5F);
        setResistance(3.5F * 5.0F / 3.0F);
        setSoundType(SoundType.STONE);
        setHarvestLevel("pickaxe", 0);
        setDefaultState(this.blockState.getBaseState().withProperty(ORIENTATION, CrafterOrientation.NORTH_UP)
                .withProperty(TRIGGERED, false).withProperty(CRAFTING, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ORIENTATION, TRIGGERED, CRAFTING);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        CrafterOrientation[] all = CrafterOrientation.values();
        return getDefaultState().withProperty(ORIENTATION, all[meta % all.length]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(ORIENTATION).ordinal();
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCrafter) {
            TileEntityCrafter crafter = (TileEntityCrafter) te;
            return state.withProperty(TRIGGERED, crafter.isTriggered()).withProperty(CRAFTING, crafter.isCrafting());
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        EnumFacing toward = EnumFacing.getDirectionFromEntityLiving(pos, placer);
        return getDefaultState().withProperty(ORIENTATION, CrafterOrientation.forPlacement(toward, placer.getHorizontalFacing()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        CrafterOrientation o = state.getValue(ORIENTATION);
        return state.withProperty(ORIENTATION, CrafterOrientation.of(rot.rotate(o.front), rot.rotate(o.top)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        CrafterOrientation o = state.getValue(ORIENTATION);
        return state.withProperty(ORIENTATION, CrafterOrientation.of(mirror.mirror(o.front), mirror.mirror(o.top)));
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCrafter();
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            com.exiledradio.trickytrialsbackport.common.Guis.open(CrafterContent.GUI, player, world, pos);
        }
        return true;
    }

    /** A rising redstone edge triggers a craft 4 ticks later; a falling one clears the trigger. */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityCrafter) || world.isRemote) {
            return;
        }
        TileEntityCrafter crafter = (TileEntityCrafter) te;
        boolean powered = world.isBlockPowered(pos);
        if (powered && !crafter.isTriggered()) {
            world.scheduleUpdate(pos, this, DELAY);
            crafter.setTriggered(true);
        } else if (!powered && crafter.isTriggered()) {
            crafter.setTriggered(false);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCrafter) {
            ((TileEntityCrafter) te).craft();
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCrafter) {
            for (int i = 0; i < TileEntityCrafter.SLOTS; i++) {
                ItemStack stack = ((TileEntityCrafter) te).get(i);
                if (!stack.isEmpty()) {
                    EntityItem item = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
                    item.setDefaultPickupDelay();
                    world.spawnEntity(item);
                }
            }
            world.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileEntityCrafter ? ((TileEntityCrafter) te).signal() : 0;
    }
}
