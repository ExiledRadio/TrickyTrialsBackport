package com.exiledradio.trickytrialsbackport.bundle;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Clicking with or on a bundle in a screen: 1.21.11's {@code BundleItem.overrideStackedOnOther} and
 * {@code overrideOtherStackedOnMe}, run on both sides from the same numbers.
 *
 * <p>1.12.2's container has no hook for these, so the client decides with {@link #decide} before its
 * screen sees the click, plays the click out locally with {@link #click} (as vanilla predicts clicks), and
 * sends {@link BundleClickMessage}; the server plays it out again on the real container and sends the slots
 * and the cursor back. In the creative screen the client alone decides, as it does for every creative click.
 */
public final class BundleClicks {

    /** What a plain left or right click on a slot does. */
    public enum Outcome {
        /** A bundle click: played out here, the screen never sees it. */
        BUNDLE,
        /** An ordinary click on a bundle: its pick is cleared first (vanilla's {@code toggleSelectedItem(-1)}). */
        UNSELECT,
        /** Nothing to do with bundles. */
        NONE
    }

    private BundleClicks() {
    }

    /** @param primary left button (vanilla's {@code ClickAction.PRIMARY}), else right */
    public static Outcome decide(ItemStack carried, ItemStack inSlot, boolean primary) {
        if (BundleContents.isBundle(carried)) {
            if (primary && !inSlot.isEmpty() || !primary && inSlot.isEmpty()) {
                return Outcome.BUNDLE;
            }
        }
        if (BundleContents.isBundle(inSlot)) {
            if (primary && carried.isEmpty()) {
                return Outcome.UNSELECT;
            }
            if (primary || carried.isEmpty()) {
                return Outcome.BUNDLE;
            }
            return Outcome.UNSELECT;
        }
        return Outcome.NONE;
    }

    /**
     * Plays out a click that {@link #decide} called {@link Outcome#BUNDLE}.
     *
     * @return whether it was one (false when the slot or cursor changed in between)
     */
    public static boolean click(Slot slot, boolean primary, EntityPlayer player) {
        ItemStack carried = player.inventory.getItemStack();
        ItemStack inSlot = slot.getStack();
        if (decide(carried, inSlot, primary) != Outcome.BUNDLE) {
            return false;
        }
        if (BundleContents.isBundle(carried) && (primary ? !inSlot.isEmpty() : inSlot.isEmpty())) {
            // overrideStackedOnOther: the bundle on the cursor takes from the slot, or gives one to it.
            BundleContents.Mutable m = new BundleContents.Mutable(carried);
            if (primary) {
                if (m.tryTransfer(slot, player) > 0) {
                    playInsertSound(player);
                } else {
                    playInsertFailSound(player);
                }
            } else {
                ItemStack one = m.removeOne();
                if (one != null) {
                    ItemStack rest = BundleSlots.safeInsert(slot, one);
                    if (rest.getCount() > 0) {
                        m.tryInsert(rest);
                    } else {
                        playRemoveOneSound(player);
                    }
                }
            }
            m.save(carried);
            return true;
        }
        // overrideOtherStackedOnMe: the bundle in the slot takes the cursor's stack, or gives one to it.
        BundleContents.Mutable m = new BundleContents.Mutable(inSlot);
        if (primary) {
            if (BundleSlots.allowModification(slot, player) && m.tryInsert(carried) > 0) {
                playInsertSound(player);
            } else {
                playInsertFailSound(player);
            }
            if (carried.isEmpty()) {
                player.inventory.setItemStack(ItemStack.EMPTY);
            }
        } else if (BundleSlots.allowModification(slot, player)) {
            ItemStack one = m.removeOne();
            if (one != null) {
                playRemoveOneSound(player);
                player.inventory.setItemStack(one);
            }
        }
        m.save(inSlot);
        slot.onSlotChanged();
        return true;
    }

    // Vanilla's sounds: the player's own (heard by that player from its own side, by everyone else from the
    // server's), volume 0.8 and pitch 0.8-1.2, the failure at 1 and 1.

    static void playRemoveOneSound(Entity entity) {
        entity.playSound(BundleContent.removeOne, 0.8F, 0.8F + entity.world.rand.nextFloat() * 0.4F);
    }

    static void playInsertSound(Entity entity) {
        entity.playSound(BundleContent.insert, 0.8F, 0.8F + entity.world.rand.nextFloat() * 0.4F);
    }

    static void playInsertFailSound(Entity entity) {
        entity.playSound(BundleContent.insertFail, 1.0F, 1.0F);
    }
}
