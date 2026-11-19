package com.exiledradio.trickytrialsbackport.coppergolem;

import com.exiledradio.backportcore.copper.Oxidation;

import net.minecraft.block.Block;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A copper chest's item. Placed beside a single copper chest of another stage, both halves become the least oxidized
 * of the two (unwaxed when only one was waxed), as 1.21's {@code CopperChestBlock.getStateForPlacement} does.
 */
public class ItemCopperChest extends ItemBlock {

    public ItemCopperChest(BlockCopperChest block) {
        super(block);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX,
                                float hitY, float hitZ, IBlockState newState) {
        BlockCopperChest placing = (BlockCopperChest) this.block;
        BlockCopperChest to = placing;
        BlockPos other = null;
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            BlockPos p = pos.offset(f);
            Block b = world.getBlockState(p).getBlock();
            if (b instanceof BlockCopperChest && BlockCopperChest.partner(world, p) == null) {
                other = p;
                to = BlockCopperChest.leastOxidized(placing, (BlockCopperChest) b);
                break;
            }
        }
        if (other != null) {
            BlockCopperChest.setChestBlock(world, other, to, 2);
        }
        IBlockState state = to == placing ? newState : Oxidation.copy(newState, to);
        if (!world.setBlockState(pos, state, 11)) {
            return false;
        }
        IBlockState placed = world.getBlockState(pos);
        if (placed.getBlock() == to) {
            setTileEntityNBT(world, player, pos, stack);
            to.onBlockPlacedBy(world, pos, placed, player, stack);
            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
            }
        }
        return true;
    }
}
