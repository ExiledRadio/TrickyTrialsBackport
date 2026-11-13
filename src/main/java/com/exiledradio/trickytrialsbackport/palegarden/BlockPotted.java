package com.exiledradio.trickytrialsbackport.palegarden;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A flower pot holding one of this mod's plants (1.13's {@code potted_*} blocks).
 *
 * <p>1.12.2's pot refuses anything not on its own list, so right-clicking an empty vanilla pot with one
 * of our plants is caught by {@link Events} first and the pot becomes this block. Right-clicking it
 * empty-handed gives the plant back and leaves an empty vanilla pot (1.14's rule); breaking it drops both.
 */
public class BlockPotted extends Block {

    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.375D, 0.6875D);

    /** plant item -> its potted block */
    private static final Map<Item, BlockPotted> BY_PLANT = new HashMap<Item, BlockPotted>();

    private Item plant;

    public BlockPotted() {
        super(Material.CIRCUITS);
        setHardness(0.0F);
    }

    /** Links this pot with its plant's item; call once the item exists. */
    public BlockPotted setPlant(Item plant) {
        this.plant = plant;
        BY_PLANT.put(plant, this);
        return this;
    }

    public Item plant() {
        return this.plant;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
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
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return Blocks.FLOWER_POT.canPlaceBlockAt(world, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!world.getBlockState(pos.down()).isTopSolid()) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty()) {
            // 1.14: a full pot ignores whatever you hold, so nothing is placed on top of it either.
            return true;
        }
        if (!world.isRemote) {
            player.setHeldItem(hand, new ItemStack(this.plant));
            world.setBlockState(pos, Blocks.FLOWER_POT.getDefaultState(), 3);
        }
        return true;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(new ItemStack(Items.FLOWER_POT));
        drops.add(new ItemStack(this.plant));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.FLOWER_POT;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.plant);
    }

    /** Putting our plants into vanilla's empty pot. */
    @Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            ItemStack held = event.getItemStack();
            BlockPotted potted = held.isEmpty() ? null : BY_PLANT.get(held.getItem());
            if (potted == null) {
                return;
            }
            World world = event.getWorld();
            BlockPos pos = event.getPos();
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() != Blocks.FLOWER_POT || !(state.getBlock() instanceof BlockFlowerPot)) {
                return;
            }
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof TileEntityFlowerPot) || !((TileEntityFlowerPot) tile).getFlowerItemStack().isEmpty()) {
                return;
            }
            EntityPlayer player = event.getEntityPlayer();
            if (!world.isRemote) {
                world.setBlockState(pos, potted.getDefaultState(), 3);
                player.addStat(StatList.FLOWER_POTTED);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
            }
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }
}
