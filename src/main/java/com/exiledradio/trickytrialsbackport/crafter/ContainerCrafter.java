package com.exiledradio.trickytrialsbackport.crafter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * The crafter's screen on the server and the client: its nine slots at 1.21's places (26, 17), the
 * player's inventory below.
 *
 * <h2>Switching slots off</h2>
 * Clicking an empty grid slot with nothing on the cursor switches it off; clicking a switched-off slot
 * switches it back on, as 1.21. Container clicks run on both sides (the client predicts, the server
 * decides), so {@link #slotClick} does it on both. The server tells the client the switched-off slots and
 * whether the crafter is powered through two window properties.
 */
public final class ContainerCrafter extends Container {

    static final int PROPERTY_DISABLED = 0;
    static final int PROPERTY_POWERED = 1;

    final TileEntityCrafter crafter;
    /** On a client: the server's switched-off slots and power, from window properties. */
    int clientDisabled;
    boolean clientPowered;
    private int sentDisabled = -1;
    private int sentPowered = -1;

    public ContainerCrafter(InventoryPlayer player, TileEntityCrafter crafter) {
        this.crafter = crafter;
        this.clientDisabled = crafter.disabled;
        InventoryBasic view = new InventoryBasic("crafter", false, TileEntityCrafter.SLOTS) {
            @Override
            public ItemStack getStackInSlot(int index) {
                return ContainerCrafter.this.crafter.get(index);
            }

            @Override
            public void setInventorySlotContents(int index, ItemStack stack) {
                ContainerCrafter.this.crafter.set(index, stack);
            }

            @Override
            public ItemStack decrStackSize(int index, int count) {
                ItemStack here = ContainerCrafter.this.crafter.get(index);
                ItemStack out = here.splitStack(count);
                ContainerCrafter.this.crafter.markDirty();
                return out;
            }

            @Override
            public ItemStack removeStackFromSlot(int index) {
                ItemStack here = ContainerCrafter.this.crafter.get(index);
                ContainerCrafter.this.crafter.set(index, ItemStack.EMPTY);
                return here;
            }

            @Override
            public void markDirty() {
                ContainerCrafter.this.crafter.markDirty();
            }
        };
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                final int index = col + row * 3;
                addSlotToContainer(new Slot(view, index, 26 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return !disabled(index);
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(player, col, 8 + col * 18, 142));
        }
    }

    /** @return whether a grid slot is switched off, as this side knows it */
    public boolean disabled(int slot) {
        int mask = this.crafter.getWorld() != null && this.crafter.getWorld().isRemote ? this.clientDisabled : this.crafter.disabled;
        return (mask & (1 << slot)) != 0;
    }

    public boolean powered() {
        return this.crafter.getWorld() != null && this.crafter.getWorld().isRemote ? this.clientPowered
                : this.crafter.getWorld() != null && this.crafter.getWorld().isBlockPowered(this.crafter.getPos());
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId >= 0 && slotId < TileEntityCrafter.SLOTS && clickType == ClickType.PICKUP && !player.isSpectator()) {
            Slot slot = this.inventorySlots.get(slotId);
            boolean off = disabled(slotId);
            if (!slot.getHasStack() && (off || player.inventory.getItemStack().isEmpty())) {
                if (this.crafter.getWorld().isRemote) {
                    this.clientDisabled ^= 1 << slotId;
                } else {
                    this.crafter.setDisabled(slotId, !off);
                    this.crafter.getWorld().updateComparatorOutputLevel(this.crafter.getPos(), this.crafter.getBlockType());
                }
                return ItemStack.EMPTY;
            }
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return !this.crafter.isInvalid() && player.getDistanceSq(this.crafter.getPos().add(0.5, 0.5, 0.5)) <= 64.0;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int powered = powered() ? 1 : 0;
        for (IContainerListener listener : this.listeners) {
            if (this.sentDisabled != this.crafter.disabled) {
                listener.sendWindowProperty(this, PROPERTY_DISABLED, this.crafter.disabled);
            }
            if (this.sentPowered != powered) {
                listener.sendWindowProperty(this, PROPERTY_POWERED, powered);
            }
        }
        this.sentDisabled = this.crafter.disabled;
        this.sentPowered = powered;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendWindowProperty(this, PROPERTY_DISABLED, this.crafter.disabled);
        listener.sendWindowProperty(this, PROPERTY_POWERED, powered() ? 1 : 0);
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == PROPERTY_DISABLED) {
            this.clientDisabled = data;
        } else if (id == PROPERTY_POWERED) {
            this.clientPowered = data != 0;
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack before = stack.copy();
        if (index < TileEntityCrafter.SLOTS) {
            if (!mergeItemStack(stack, TileEntityCrafter.SLOTS, this.inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < TileEntityCrafter.SLOTS && !stack.isEmpty(); i++) {
                Slot grid = this.inventorySlots.get(i);
                if (!disabled(i) && grid.isItemValid(stack)) {
                    moved |= mergeItemStack(stack, i, i + 1, false);
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        return stack.getCount() == before.getCount() ? ItemStack.EMPTY : before;
    }
}
