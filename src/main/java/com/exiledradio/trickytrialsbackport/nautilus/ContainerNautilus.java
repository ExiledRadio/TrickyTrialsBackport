package com.exiledradio.trickytrialsbackport.nautilus;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * 1.21.11's {@code NautilusInventoryMenu}: the saddle slot at (8, 18), the armor slot at (8, 36), both only for a tamed
 * adult, then the player's inventory at (8, 84); shift-clicks as {@code AbstractMountInventoryMenu.quickMoveStack}.
 */
public class ContainerNautilus extends Container {

    public final EntityAbstractNautilus nautilus;
    private final IInventory mount;

    public ContainerNautilus(InventoryPlayer player, final EntityAbstractNautilus nautilus) {
        this.nautilus = nautilus;
        this.mount = nautilus.inventory;
        this.mount.openInventory(player.player);
        this.addSlotToContainer(new Slot(this.mount, EntityAbstractNautilus.SLOT_SADDLE, 8, 18) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return EntityAbstractNautilus.isSaddle(stack) && !this.getHasStack() && nautilus.canUseEquipment();
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }

            @Override
            public boolean isEnabled() {
                return nautilus.canUseEquipment();
            }
        });
        this.addSlotToContainer(new Slot(this.mount, EntityAbstractNautilus.SLOT_ARMOR, 8, 36) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof ItemNautilusArmor && nautilus.canUseEquipment();
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }

            @Override
            public boolean isEnabled() {
                return nautilus.canUseEquipment();
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(player, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.nautilus.isEntityAlive() && this.nautilus.inventory == this.mount && this.nautilus.getDistance(player) < 8.0F;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        this.mount.closeInventory(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return result;
        }
        ItemStack stack = slot.getStack();
        result = stack.copy();
        int mountEnd = 2;
        if (index < mountEnd) {
            if (!this.mergeItemStack(stack, mountEnd, this.inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.getSlot(1).isItemValid(stack) && !this.getSlot(1).getHasStack()) {
            if (!this.mergeItemStack(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.getSlot(0).isItemValid(stack) && !this.getSlot(0).getHasStack()) {
            if (!this.mergeItemStack(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            int hotbarStart = mountEnd + 27;
            int hotbarEnd = hotbarStart + 9;
            if (index >= hotbarStart && index < hotbarEnd) {
                if (!this.mergeItemStack(stack, mountEnd, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= mountEnd && index < hotbarStart) {
                if (!this.mergeItemStack(stack, hotbarStart, hotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        return result;
    }
}
