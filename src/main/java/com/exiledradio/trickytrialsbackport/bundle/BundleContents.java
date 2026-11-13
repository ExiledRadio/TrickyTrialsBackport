package com.exiledradio.trickytrialsbackport.bundle;

import net.minecraft.block.BlockShulkerBox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.math.Fraction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What a bundle holds: 1.21.11's {@code BundleContents}, kept in the bundle's NBT.
 *
 * <h2>NBT</h2>
 * {@code BundleContents}: the stacks, newest first (a list of ordinary item stack compounds). {@code
 * SelectedItem}: the index the scroll wheel picked, absent when nothing is picked. Vanilla keeps the
 * selection out of saves and the network; here it is a tag that is only present while a player hovers the
 * bundle in a screen, and every way out of that (moving off it, clicking it, closing the screen) clears it.
 *
 * <h2>Weight</h2>
 * Exactly vanilla's, as fractions of a full bundle: one item weighs 1/(its stack size), a bundle weighs
 * 1/16 plus what it holds, a beehive or bee nest with bees weighs a whole bundle. A bundle takes as much of
 * a stack as fits under 1.
 */
public final class BundleContents {

    public static final String ITEMS = "BundleContents";
    public static final String SELECTED = "SelectedItem";

    /** Vanilla's {@code BUNDLE_IN_BUNDLE_WEIGHT}: 4/64. */
    static final Fraction BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction(1, 16);

    private BundleContents() {
    }

    public static boolean isBundle(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemBundle;
    }

    /** @return a copy of the stacks a bundle holds, newest first */
    public static List<ItemStack> items(ItemStack bundle) {
        NBTTagCompound tag = bundle.getTagCompound();
        if (tag == null || !tag.hasKey(ITEMS, Constants.NBT.TAG_LIST)) {
            return Collections.emptyList();
        }
        NBTTagList list = tag.getTagList(ITEMS, Constants.NBT.TAG_COMPOUND);
        List<ItemStack> out = new ArrayList<ItemStack>(list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) {
            ItemStack stack = new ItemStack(list.getCompoundTagAt(i));
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        return out;
    }

    public static Fraction weight(ItemStack bundle) {
        return weightOf(items(bundle));
    }

    static Fraction weightOf(List<ItemStack> items) {
        Fraction weight = Fraction.ZERO;
        for (ItemStack stack : items) {
            weight = weight.add(weightOfOne(stack).multiplyBy(Fraction.getFraction(stack.getCount(), 1)));
        }
        return weight;
    }

    /** Vanilla's {@code getWeight}: what one item of this stack weighs. */
    public static Fraction weightOfOne(ItemStack stack) {
        if (isBundle(stack)) {
            return BUNDLE_IN_BUNDLE_WEIGHT.add(weight(stack));
        }
        return hasBees(stack) ? Fraction.ONE : Fraction.getFraction(1, stack.getMaxStackSize());
    }

    /** A beehive or bee nest item with bees in it (1.15's {@code BlockEntityTag.Bees}, any mod's). */
    private static boolean hasBees(ItemStack stack) {
        NBTTagCompound tag = stack.getSubCompound("BlockEntityTag");
        return tag != null && tag.getTagList("Bees", Constants.NBT.TAG_COMPOUND).tagCount() > 0;
    }

    /** Vanilla's {@code canFitInsideContainerItems}: everything but shulker boxes. */
    public static boolean canItemBeInBundle(ItemStack stack) {
        return !stack.isEmpty() && !(stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).getBlock() instanceof BlockShulkerBox);
    }

    public static int selectedItem(ItemStack bundle) {
        NBTTagCompound tag = bundle.getTagCompound();
        return tag != null && tag.hasKey(SELECTED, Constants.NBT.TAG_INT) ? tag.getInteger(SELECTED) : -1;
    }

    /** @return the stack the scroll wheel picked, or empty */
    public static ItemStack selectedStack(ItemStack bundle) {
        int selected = selectedItem(bundle);
        if (selected < 0) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> items = items(bundle);
        return selected < items.size() ? items.get(selected) : ItemStack.EMPTY;
    }

    /** Vanilla's {@code BundleItem.toggleSelectedItem}: picks this index, or clears the pick (-1, or the same). */
    public static void toggleSelectedItem(ItemStack bundle, int index) {
        if (!isBundle(bundle)) {
            return;
        }
        Mutable m = new Mutable(bundle);
        m.toggleSelectedItem(index);
        m.save(bundle);
    }

    /** Vanilla's {@code getNumberOfItemsToShow}: how many stacks the tooltip grid shows. */
    public static int numberOfItemsToShow(int size) {
        int max = size > 12 ? 11 : 12;
        int remainder = size % 4;
        int pad = remainder == 0 ? 0 : 4 - remainder;
        return Math.min(size, max - pad);
    }

    /** Vanilla's {@code ScrollWheelHandler.getNextScrollWheelSelection}: wheel up picks the stack before. */
    public static int nextScrollWheelSelection(double amount, int current, int count) {
        int next = current - (int) Math.signum(amount);
        next = Math.max(-1, next);
        while (next < 0) {
            next += count;
        }
        while (next >= count) {
            next -= count;
        }
        return next;
    }

    /** Vanilla's {@code Mth.mulAndTruncate}. */
    public static int mulAndTruncate(Fraction fraction, int factor) {
        return (int) ((long) fraction.getNumerator() * factor / fraction.getDenominator());
    }

    /** Vanilla's {@code BundleContents.Mutable}, read from and saved back to a bundle's NBT. */
    public static final class Mutable {
        final List<ItemStack> items;
        Fraction weight;
        int selectedItem;

        public Mutable(ItemStack bundle) {
            this.items = new ArrayList<ItemStack>(BundleContents.items(bundle));
            this.weight = weightOf(this.items);
            this.selectedItem = BundleContents.selectedItem(bundle);
        }

        public Fraction weight() {
            return this.weight;
        }

        public List<ItemStack> items() {
            return Collections.unmodifiableList(this.items);
        }

        private int findStackIndex(ItemStack stack) {
            if (!stack.isStackable()) {
                return -1;
            }
            for (int i = 0; i < this.items.size(); i++) {
                if (ItemHandlerHelper.canItemStacksStack(this.items.get(i), stack)) {
                    return i;
                }
            }
            return -1;
        }

        private int getMaxAmountToAdd(ItemStack stack) {
            Fraction room = Fraction.ONE.subtract(this.weight);
            return Math.max(room.divideBy(weightOfOne(stack)).intValue(), 0);
        }

        /** Takes as much of the stack as fits (shrinking it). @return how many went in */
        public int tryInsert(ItemStack stack) {
            if (!canItemBeInBundle(stack)) {
                return 0;
            }
            int amount = Math.min(stack.getCount(), this.getMaxAmountToAdd(stack));
            if (amount == 0) {
                return 0;
            }
            this.weight = this.weight.add(weightOfOne(stack).multiplyBy(Fraction.getFraction(amount, 1)));
            int index = this.findStackIndex(stack);
            if (index != -1) {
                ItemStack old = this.items.remove(index);
                ItemStack merged = old.copy();
                merged.setCount(old.getCount() + amount);
                stack.shrink(amount);
                this.items.add(0, merged);
            } else {
                this.items.add(0, stack.splitStack(amount));
            }
            return amount;
        }

        /** Takes what fits out of a slot, as the slot allows. @return how many went in */
        public int tryTransfer(Slot slot, EntityPlayer player) {
            ItemStack stack = slot.getStack();
            int max = this.getMaxAmountToAdd(stack);
            return canItemBeInBundle(stack) ? this.tryInsert(BundleSlots.safeTake(slot, stack.getCount(), max, player)) : 0;
        }

        public void toggleSelectedItem(int index) {
            this.selectedItem = this.selectedItem != index && !this.outOfBounds(index) ? index : -1;
        }

        private boolean outOfBounds(int index) {
            return index < 0 || index >= this.items.size();
        }

        /** Takes out the picked stack, or the newest. @return it, or null when empty */
        @Nullable
        public ItemStack removeOne() {
            if (this.items.isEmpty()) {
                return null;
            }
            int index = this.outOfBounds(this.selectedItem) ? 0 : this.selectedItem;
            ItemStack stack = this.items.remove(index).copy();
            this.weight = this.weight.subtract(weightOfOne(stack).multiplyBy(Fraction.getFraction(stack.getCount(), 1)));
            this.toggleSelectedItem(-1);
            return stack;
        }

        public void save(ItemStack bundle) {
            NBTTagCompound tag = bundle.getTagCompound();
            if (tag == null) {
                if (this.items.isEmpty() && this.selectedItem < 0) {
                    return;
                }
                tag = new NBTTagCompound();
                bundle.setTagCompound(tag);
            }
            if (this.items.isEmpty()) {
                tag.removeTag(ITEMS);
            } else {
                NBTTagList list = new NBTTagList();
                for (ItemStack stack : this.items) {
                    list.appendTag(stack.writeToNBT(new NBTTagCompound()));
                }
                tag.setTag(ITEMS, list);
            }
            if (this.selectedItem < 0) {
                tag.removeTag(SELECTED);
            } else {
                tag.setInteger(SELECTED, this.selectedItem);
            }
            if (tag.isEmpty()) {
                bundle.setTagCompound(null);
            }
        }
    }
}
