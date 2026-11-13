package com.exiledradio.trickytrialsbackport.flora;

import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The wildflowers and leaf litter items. Used on a block of the same kind that is not yet full, while not sneaking
 * (the clicked block, or the block the click would place into), they add one piece instead of placing a new block
 * ({@code SegmentableBlock.canBeReplaced}). A full block, or a sneaking click, places on the next block out, as
 * vanilla's same-item rule keeps even replaceable leaf litter from being replaced by its own item.
 */
public class ItemSegmented extends ItemBlock {

    public ItemSegmented(BlockSegmented block) {
        super(block);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        BlockPos target = addTarget(world, pos, facing, player);
        ItemStack stack = player.getHeldItem(hand);
        if (target != null) {
            if (!player.canPlayerEdit(target, facing, stack)) {
                return EnumActionResult.FAIL;
            }
            IBlockState state = world.getBlockState(target);
            BlockSegmented block = (BlockSegmented) state.getBlock();
            if (!world.isRemote) {
                world.setBlockState(target, state.withProperty(block.amount(), state.getValue(block.amount()) + 1), 3);
            }
            SoundType sound = block.getSoundType(state, world, target, player);
            world.playSound(player, target, sound.getPlaceSound(), SoundCategory.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
        if (world.getBlockState(pos).getBlock() == this.block) {
            // Our own block, full or under a sneaking click: never replaced by its own item; place beyond it.
            return super.onItemUse(player, world, pos.offset(facing), hand, facing, hitX, hitY, hitZ);
        }
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }

    /**
     * The block a click adds one piece to: the clicked block, or the block the click would place into, while it is
     * this item's block, not yet full, and the player is not sneaking; null when the click places a new block (or
     * nothing).
     */
    @Nullable
    public BlockPos addTarget(World world, BlockPos pos, EnumFacing facing, @Nullable EntityPlayer player) {
        if (player != null && player.isSneaking()) {
            return null;
        }
        if (BlockSegmented.canAddTo(world.getBlockState(pos), this)) {
            return pos;
        }
        BlockPos next = pos.offset(facing);
        return BlockSegmented.canAddTo(world.getBlockState(next), this) ? next : null;
    }

    /**
     * The client asks this before it sends a click to the server (as it does for slabs): without it, a click on
     * wildflowers is refused on the client because wildflowers cannot stand on wildflowers, and adding one never
     * happens in a real game.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
        if (addTarget(world, pos, side, player) != null) {
            return true;
        }
        if (world.getBlockState(pos).getBlock() == this.block) {
            BlockPos next = pos.offset(side);
            return world.mayPlace(this.block, next, false, side, null);
        }
        return super.canPlaceBlockOnSide(world, pos, side, player, stack);
    }
}
