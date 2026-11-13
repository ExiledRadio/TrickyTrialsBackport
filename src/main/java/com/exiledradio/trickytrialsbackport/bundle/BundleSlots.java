package com.exiledradio.trickytrialsbackport.bundle;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

/** 1.21's {@code Slot} helpers that 1.12.2's slots lack, in 1.12.2's slot calls. */
final class BundleSlots {

    private BundleSlots() {
    }

    /** {@code Slot.allowModification}: the player may take from it and put its own item back. */
    static boolean allowModification(Slot slot, EntityPlayer player) {
        return slot.canTakeStack(player) && slot.isItemValid(slot.getStack());
    }

    /** {@code Slot.safeTake}: up to {@code max} of {@code count}, the whole stack only from a take-only slot. */
    static ItemStack safeTake(Slot slot, int count, int max, EntityPlayer player) {
        if (!slot.canTakeStack(player)) {
            return ItemStack.EMPTY;
        }
        if (!allowModification(slot, player) && max < slot.getStack().getCount()) {
            return ItemStack.EMPTY;
        }
        int amount = Math.min(count, max);
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = slot.decrStackSize(amount);
        if (taken.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return slot.onTake(player, taken);
    }

    /** {@code Slot.safeInsert}: puts what fits of the stack into the slot (shrinking it). @return the rest */
    static ItemStack safeInsert(Slot slot, ItemStack stack) {
        if (stack.isEmpty() || !slot.isItemValid(stack)) {
            return stack;
        }
        ItemStack current = slot.getStack();
        int limit = Math.min(slot.getItemStackLimit(stack), stack.getMaxStackSize());
        int amount = Math.min(stack.getCount(), limit - current.getCount());
        if (amount <= 0) {
            return stack;
        }
        if (current.isEmpty()) {
            slot.putStack(stack.splitStack(amount));
        } else if (ItemHandlerHelper.canItemStacksStack(current, stack)) {
            stack.shrink(amount);
            current.grow(amount);
            slot.putStack(current);
        }
        return stack;
    }
}
