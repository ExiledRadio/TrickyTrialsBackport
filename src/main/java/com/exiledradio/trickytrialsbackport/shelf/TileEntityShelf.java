package com.exiledradio.trickytrialsbackport.shelf;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.21.9's {@code ShelfBlockEntity}: three items, saved as vanilla saves them ({@code Items} with {@code Slot}, and
 * {@code align_items_to_bottom}), and an inventory for hoppers. Every change is sent to the clients, which draw the
 * items ({@code ShelfRenderer}).
 *
 * <p>It also keeps the shelf's place in a powered row ({@code side_chain}; see {@link ShelfChain}), which vanilla keeps
 * in the block state.
 */
public class TileEntityShelf extends TileEntity implements IInventory {

    public static final int MAX_ITEMS = 3;
    private static final String ALIGN_ITEMS_TO_BOTTOM_TAG = "align_items_to_bottom";
    private static final String SIDE_CHAIN_TAG = "side_chain";

    private NonNullList<ItemStack> items = NonNullList.withSize(MAX_ITEMS, ItemStack.EMPTY);
    private boolean alignItemsToBottom;
    private SideChainPart chain = SideChainPart.UNCONNECTED;

    public NonNullList<ItemStack> items() {
        return this.items;
    }

    public boolean alignItemsToBottom() {
        return this.alignItemsToBottom;
    }

    public SideChainPart chain() {
        return this.chain;
    }

    void setChain(SideChainPart part) {
        this.chain = part;
        // Vanilla's setBlock of the new part posts no game event.
        setChanged(null);
    }

    /** {@code swapItemNoUpdate}: puts a stack in a slot and returns what was there, without telling anyone. */
    public ItemStack swapItemNoUpdate(int slot, ItemStack stack) {
        ItemStack old = this.items.get(slot);
        this.items.set(slot, stack);
        return old;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.items = NonNullList.withSize(MAX_ITEMS, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(tag, this.items);
        this.alignItemsToBottom = tag.getBoolean(ALIGN_ITEMS_TO_BOTTOM_TAG);
        this.chain = SideChainPart.byName(tag.getString(SIDE_CHAIN_TAG));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        ItemStackHelper.saveAllItems(tag, this.items, true);
        tag.setBoolean(ALIGN_ITEMS_TO_BOTTOM_TAG, this.alignItemsToBottom);
        tag.setString(SIDE_CHAIN_TAG, this.chain.getName());
        return tag;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        SideChainPart before = this.chain;
        readFromNBT(packet.getNbtCompound());
        if (before != this.chain && this.world != null) {
            this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
        }
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    /** Power changes swap the metadata under this tile entity; it is only replaced when the block goes. */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    /** {@code setChanged()}: a change of the contents, posting {@code block_activate}. */
    @Override
    public void markDirty() {
        setChanged(ShelfGameEvents.BLOCK_ACTIVATE);
    }

    /**
     * {@code setChanged(event)}: saved, comparators told, the game event posted (none for null), and the clients sent
     * the new contents.
     */
    void setChanged(@Nullable String gameEvent) {
        super.markDirty();
        if (this.world != null && !this.world.isRemote) {
            IBlockState state = this.world.getBlockState(this.pos);
            if (gameEvent != null) {
                ShelfGameEvents.post(this.world, this.pos, gameEvent, state);
            }
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }

    // --- IInventory (vanilla's ListBackedContainer), for hoppers ---------------------------------------------------

    @Override
    public int getSizeInventory() {
        return MAX_ITEMS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index >= 0 && index < MAX_ITEMS ? this.items.get(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack out = ItemStackHelper.getAndSplit(this.items, index, count);
        if (!out.isEmpty()) {
            markDirty();
        }
        return out;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(this.items, index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.items.set(index, stack);
        if (stack.getCount() > getInventoryStackLimit()) {
            stack.setCount(getInventoryStackLimit());
        }
        markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this
                && player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        this.items.clear();
    }

    @Override
    public String getName() {
        return getBlockType() == null ? "container.trickytrialsbackport.shelf" : getBlockType().getTranslationKey() + ".name";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation(getName());
    }
}
