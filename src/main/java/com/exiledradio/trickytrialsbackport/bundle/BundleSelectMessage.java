package com.exiledradio.trickytrialsbackport.bundle;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client to server: the scroll wheel picked a stack in a bundle of the open screen, or the pick was cleared
 * (-1): vanilla's {@code ServerboundSelectBundleItemPacket}. Packet id 56.
 *
 * <p>The client already shows the pick, so the server records it in its own copy of what that client was
 * last sent: the pick alone sends nothing back, which would flicker while the wheel turns.
 */
public final class BundleSelectMessage implements IMessage {

    int windowId;
    int slot;
    int index;

    public BundleSelectMessage() {
    }

    public BundleSelectMessage(int windowId, int slot, int index) {
        this.windowId = windowId;
        this.slot = slot;
        this.index = index;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        this.slot = buf.readShort();
        this.index = buf.readShort();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeShort(this.slot);
        buf.writeShort(this.index);
    }

    /** Vanilla's {@code AbstractContainerMenu.setSelectedBundleItemIndex}. */
    public static void apply(EntityPlayerMP player, int windowId, int slot, int index) {
        Container container = player.openContainer;
        if (container == null || container.windowId != windowId || slot < 0 || slot >= container.inventorySlots.size()) {
            return;
        }
        ItemStack stack = container.getSlot(slot).getStack();
        if (!BundleContents.isBundle(stack)) {
            return;
        }
        BundleContents.toggleSelectedItem(stack, index);
        if (slot < container.inventoryItemStacks.size()) {
            container.inventoryItemStacks.set(slot, stack.copy());
        }
    }

    public static final class Handler implements IMessageHandler<BundleSelectMessage, IMessage> {
        @Override
        public IMessage onMessage(final BundleSelectMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> apply(player, message.windowId, message.slot, message.index));
            return null;
        }
    }
}
