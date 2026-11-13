package com.exiledradio.trickytrialsbackport.bundle;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import org.apache.commons.lang3.math.Fraction;

import javax.annotation.Nullable;

/**
 * The bundle and its sixteen dyed copies: 1.21.11's {@code BundleItem}.
 *
 * <ul>
 *   <li>One per stack; what it holds is {@link BundleContents}.</li>
 *   <li>Held down (use), it drops its stacks one at a time: the first at once, then one every 2 ticks
 *       after the first 10, for up to 200 ticks.</li>
 *   <li>The fullness bar: 1 + 12 x weight pixels, blue, red once full.</li>
 *   <li>Clicks in screens: {@link BundleClicks}. The tooltip grid and the open look: {@code bundle.client}.</li>
 * </ul>
 */
public class ItemBundle extends Item {

    /** Vanilla's {@code FULL_BAR_COLOR} and {@code BAR_COLOR} ({@code ARGB.colorFromFloat}, floored). */
    static final int FULL_BAR_COLOR = 0xFF5454;
    static final int BAR_COLOR = 0x7087FF;
    static final int TICKS_AFTER_FIRST_THROW = 10;
    static final int TICKS_MAX_THROW_DURATION = 200;

    /** null for the plain bundle */
    @Nullable
    public final EnumDyeColor color;

    public ItemBundle(@Nullable EnumDyeColor color) {
        this.color = color;
        this.setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        player.setActiveHand(hand);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return TICKS_MAX_THROW_DURATION;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;
    }

    /** {@code onUseTick}: the count is the ticks left, as vanilla's. */
    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase entity, int count) {
        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        int duration = this.getMaxItemUseDuration(stack);
        boolean first = count == duration;
        if (first || count < duration - TICKS_AFTER_FIRST_THROW && count % 2 == 0) {
            dropContent(stack, (EntityPlayer) entity);
        }
    }

    /** Dropping one stack changes the held bundle; vanilla keeps using it (it compares the item only). */
    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() == newStack.getItem();
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    /**
     * Takes out one stack and throws it the way a player drops an item. Both sides take it out (the client
     * hears its own sound and swings); only the server throws it and plays the emptying sound for everyone.
     */
    public static boolean dropContent(ItemStack bundle, EntityPlayer player) {
        BundleContents.Mutable m = new BundleContents.Mutable(bundle);
        ItemStack one = m.removeOne();
        if (one == null) {
            return false;
        }
        BundleClicks.playRemoveOneSound(player);
        m.save(bundle);
        if (player.world.isRemote) {
            player.swingArm(EnumHand.MAIN_HAND);
            return true;
        }
        player.dropItem(one, false, true);
        player.world.playSound(null, player.posX, player.posY, player.posZ, BundleContent.dropContents, SoundCategory.PLAYERS,
                0.8F, 0.8F + player.world.rand.nextFloat() * 0.4F);
        StatBase stat = StatList.getObjectUseStats(bundle.getItem());
        if (stat != null) {
            player.addStat(stat);
        }
        return true;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return BundleContents.weight(stack).compareTo(Fraction.ZERO) > 0;
    }

    /** Vanilla's bar width, {@code min(1 + 12 x weight, 13)}, as the share of 13 pixels 1.12.2 leaves off. */
    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return (13 - barWidth(stack)) / 13.0D;
    }

    public static int barWidth(ItemStack stack) {
        return Math.min(1 + BundleContents.mulAndTruncate(BundleContents.weight(stack), 12), 13);
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return BundleContents.weight(stack).compareTo(Fraction.ONE) >= 0 ? FULL_BAR_COLOR : BAR_COLOR;
    }
}
