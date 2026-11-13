package com.exiledradio.trickytrialsbackport.resin;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;

/**
 * The one resin clump item for the four clump blocks: 1.21.4's {@code MultifaceBlock.getStateForPlacement}.
 * It puts a face of resin on the block that was clicked, joining a clump already in that space; when that face
 * cannot take it, the nearest face the player is looking toward that can.
 */
public class ItemResinClump extends ItemBlock {

    public ItemResinClump(BlockResinClump block) {
        super(block);
    }

    /** Where a click puts resin: {@code {position, face}}, or null when it puts none. */
    @Nullable
    public static Object[] addTarget(World world, BlockPos pos, EnumFacing side, @Nullable EntityPlayer player) {
        IBlockState clicked = world.getBlockState(pos);
        // BlockPlaceContext: the clicked space itself when it can be replaced (a clump with a vacant face, grass),
        // otherwise the space in front of the clicked face.
        boolean replaceClicked = clicked.getBlock() instanceof BlockResinClump
                ? Integer.bitCount(BlockResinClump.faces(clicked)) < 6
                : clicked.getBlock().isReplaceable(world, pos) && !clicked.getMaterial().isLiquid();
        BlockPos at = replaceClicked ? pos : pos.offset(side);
        EnumFacing[] order = nearestLooking(player);
        if (!replaceClicked) {
            // getNearestLookingDirections: the face behind the click comes first.
            EnumFacing direct = side.getOpposite();
            EnumFacing[] withDirect = new EnumFacing[6];
            withDirect[0] = direct;
            int i = 1;
            for (EnumFacing f : order) {
                if (f != direct) {
                    withDirect[i++] = f;
                }
            }
            order = withDirect;
        }
        for (EnumFacing f : order) {
            if (BlockResinClump.canAdd(world, at, f)) {
                return new Object[] {at, f};
            }
        }
        return null;
    }

    /** {@code Direction.orderedByNearest}: the six faces in the order the player looks toward them. */
    private static EnumFacing[] nearestLooking(@Nullable EntityPlayer player) {
        EnumFacing[] order = EnumFacing.values().clone();
        if (player == null) {
            return order;
        }
        final Vec3d look = player.getLookVec();
        Arrays.sort(order, new Comparator<EnumFacing>() {
            @Override
            public int compare(EnumFacing a, EnumFacing b) {
                return Double.compare(dot(b, look), dot(a, look));
            }
        });
        return order;
    }

    private static double dot(EnumFacing f, Vec3d v) {
        return f.getXOffset() * v.x + f.getYOffset() * v.y + f.getZOffset() * v.z;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side,
                                      float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        Object[] target = addTarget(world, pos, side, player);
        if (stack.isEmpty() || target == null || !player.canPlayerEdit((BlockPos) target[0], side, stack)) {
            return EnumActionResult.FAIL;
        }
        BlockPos at = (BlockPos) target[0];
        EnumFacing face = (EnumFacing) target[1];
        IBlockState there = world.getBlockState(at);
        IBlockState state = BlockResinClump.of(BlockResinClump.faces(there) | (1 << face.getIndex()));
        if (!world.isRemote) {
            world.setBlockState(at, state, 3);
            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, at, stack);
            }
        }
        SoundType sound = state.getBlock().getSoundType(state, world, at, player);
        world.playSound(player, at, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }

    /**
     * The client asks this before it sends a click to the server. ItemBlock's answer looks at the block alone and
     * refuses a click that only adds a face to a clump, so in a real game that click would never reach the server.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
        return addTarget(world, pos, side, player) != null;
    }
}
