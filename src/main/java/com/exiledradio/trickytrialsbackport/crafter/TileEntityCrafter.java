package com.exiledradio.trickytrialsbackport.crafter;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.PositionImpl;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

/**
 * The crafter's nine slots, its switched-off slots, and its two shown states (triggered by redstone,
 * crafting for 6 ticks after a craft).
 *
 * <h2>Hoppers</h2>
 * Items go in only (1.21's crafter cannot be emptied by a hopper) and spread out as 1.21 spreads them: a
 * stack goes into a slot only if no later enabled slot is empty or holds less of the same item, so a
 * hopper fills the grid evenly instead of piling one slot.
 */
public final class TileEntityCrafter extends TileEntity implements ITickable {

    public static final int SLOTS = 9;
    static final int CRAFTING_TICKS = 6;

    final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    int disabled;
    boolean triggered;
    int crafting;

    private final IItemHandler handler = new Handler();

    // ------------------------------------------------------------------------------------------

    public boolean isDisabled(int slot) {
        return (this.disabled & (1 << slot)) != 0;
    }

    public void setDisabled(int slot, boolean off) {
        if (off) {
            this.disabled |= 1 << slot;
        } else {
            this.disabled &= ~(1 << slot);
        }
        markDirty();
    }

    public ItemStack get(int slot) {
        return this.items.get(slot);
    }

    public void set(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        markDirty();
    }

    /** 1.21's comparator signal: how many slots hold something or are switched off, 0-9. */
    public int signal() {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (isDisabled(i) || !this.items.get(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public boolean isTriggered() {
        return this.triggered;
    }

    public boolean isCrafting() {
        return this.crafting > 0;
    }

    void setTriggered(boolean triggered) {
        if (this.triggered != triggered) {
            this.triggered = triggered;
            markDirty();
            sync();
        }
    }

    /** May a hopper put this stack in this slot? 1.21's {@code canPlaceItem}. */
    boolean accepts(int slot, ItemStack stack) {
        if (isDisabled(slot)) {
            return false;
        }
        ItemStack here = this.items.get(slot);
        if (here.isEmpty()) {
            return true;
        }
        if (!ItemHandlerHelper.canItemStacksStack(here, stack) || here.getCount() >= here.getMaxStackSize()) {
            return false;
        }
        for (int k = slot + 1; k < SLOTS; k++) {
            if (!isDisabled(k)) {
                ItemStack later = this.items.get(k);
                if (later.isEmpty() || later.getCount() < here.getCount() && ItemHandlerHelper.canItemStacksStack(later, stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The grid as a crafting inventory, for recipes and the screen's preview. */
    public InventoryCrafting grid() {
        InventoryCrafting inv = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        for (int i = 0; i < SLOTS; i++) {
            inv.setInventorySlotContents(i, this.items.get(i).copy());
        }
        return inv;
    }

    /**
     * One craft, on a redstone pulse: the recipe the grid makes, ejected out of the front (into a container
     * there if it takes it), one of each ingredient used and its remainder ejected too. Players within 17
     * blocks are told what was made (<i>Crafters Crafting Crafters</i>).
     *
     * @return true if something was made
     */
    public boolean craft() {
        if (this.world == null || this.world.isRemote) {
            return false;
        }
        InventoryCrafting inv = grid();
        IRecipe recipe = CraftingManager.findMatchingRecipe(inv, this.world);
        ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(inv);
        if (result.isEmpty()) {
            this.world.playSound(null, this.pos, Content.sound("block.crafter.fail"), SoundCategory.BLOCKS, 1.0F, 1.0F);
            return false;
        }
        NonNullList<ItemStack> remainders = recipe.getRemainingItems(inv);
        for (EntityPlayerMP player : this.world.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(this.pos).grow(8.0))) {
            CrafterContent.CRAFTED.trigger(player, result);
        }
        eject(result.copy());
        for (int i = 0; i < SLOTS; i++) {
            ItemStack here = this.items.get(i);
            if (!here.isEmpty()) {
                here.shrink(1);
                if (here.isEmpty()) {
                    this.items.set(i, ItemStack.EMPTY);
                }
            }
            ItemStack rest = remainders.get(i);
            if (!rest.isEmpty()) {
                eject(rest.copy());
            }
        }
        this.crafting = CRAFTING_TICKS;
        markDirty();
        sync();
        this.world.playSound(null, this.pos, Content.sound("block.crafter.craft"), SoundCategory.BLOCKS, 1.0F, 1.0F);
        this.world.updateComparatorOutputLevel(this.pos, getBlockType());
        return true;
    }

    private void eject(ItemStack stack) {
        EnumFacing front = this.world.getBlockState(this.pos).getValue(BlockCrafter.ORIENTATION).front;
        BlockPos at = this.pos.offset(front);
        TileEntity there = this.world.getTileEntity(at);
        if (there != null && there.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, front.getOpposite())) {
            IItemHandler target = there.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, front.getOpposite());
            if (target != null) {
                stack = ItemHandlerHelper.insertItemStacked(target, stack, false);
            }
        }
        if (!stack.isEmpty()) {
            PositionImpl p = new PositionImpl(this.pos.getX() + 0.5 + 0.7 * front.getXOffset(), this.pos.getY() + 0.5 + 0.7 * front.getYOffset(),
                    this.pos.getZ() + 0.5 + 0.7 * front.getZOffset());
            BehaviorDefaultDispenseItem.doDispense(this.world, stack, 6, front, p);
        }
    }

    @Override
    public void update() {
        if (this.crafting > 0 && --this.crafting == 0 && !this.world.isRemote) {
            sync();
        }
    }

    void sync() {
        if (this.world != null && !this.world.isRemote) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }

    // ------------------------------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        for (int i = 0; i < SLOTS; i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        net.minecraft.inventory.ItemStackHelper.loadAllItems(tag, this.items);
        this.disabled = tag.getInteger("disabled_slots");
        this.triggered = tag.getBoolean("triggered");
        this.crafting = tag.getInteger("crafting_ticks_remaining");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        net.minecraft.inventory.ItemStackHelper.saveAllItems(tag, this.items);
        tag.setInteger("disabled_slots", this.disabled);
        tag.setBoolean("triggered", this.triggered);
        tag.setInteger("crafting_ticks_remaining", this.crafting);
        return tag;
    }

    /** Clients need the two shown states (and the switched-off slots, for the screen). */
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setInteger("disabled_slots", this.disabled);
        tag.setBoolean("triggered", this.triggered);
        tag.setInteger("crafting_ticks_remaining", this.crafting);
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        this.disabled = tag.getInteger("disabled_slots");
        this.triggered = tag.getBoolean("triggered");
        this.crafting = tag.getInteger("crafting_ticks_remaining");
    }

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
        if (this.world != null) {
            this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.handler);
        }
        return super.getCapability(capability, facing);
    }

    /** Hoppers, droppers and pipes: insert by 1.21's even-spreading rule, never extract. */
    private final class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return TileEntityCrafter.this.items.get(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !accepts(slot, stack)) {
                return stack;
            }
            ItemStack here = TileEntityCrafter.this.items.get(slot);
            // One at a time, as a hopper moves them, so the spread rule is asked again for the next item.
            if (!simulate) {
                if (here.isEmpty()) {
                    ItemStack one = stack.copy();
                    one.setCount(1);
                    TileEntityCrafter.this.items.set(slot, one);
                } else {
                    here.grow(1);
                }
                markDirty();
                TileEntityCrafter.this.world.updateComparatorOutputLevel(TileEntityCrafter.this.pos, getBlockType());
            }
            ItemStack rest = stack.copy();
            rest.shrink(1);
            return rest;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    }
}
