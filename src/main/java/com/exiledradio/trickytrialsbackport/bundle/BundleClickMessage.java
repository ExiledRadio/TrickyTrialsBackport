package com.exiledradio.trickytrialsbackport.bundle;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client to server: a bundle click on a slot of the open screen ({@link BundleClicks}). Packet id 55
 * (the bundles block, 55-59).
 *
 * <p>Handled on the server thread, in order with the vanilla click packets around it. The server sends the
 * changed slots and the cursor back itself, outside a vanilla click, so no update is swallowed as a
 * predicted one.
 */
public final class BundleClickMessage implements IMessage {

    int windowId;
    int slot;
    boolean primary;

    public BundleClickMessage() {
    }

    public BundleClickMessage(int windowId, int slot, boolean primary) {
        this.windowId = windowId;
        this.slot = slot;
        this.primary = primary;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        this.slot = buf.readShort();
        this.primary = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeShort(this.slot);
        buf.writeBoolean(this.primary);
    }

    /** Plays the click out on the player's open container. @return whether it was a bundle click there */
    public static boolean apply(EntityPlayerMP player, int windowId, int slot, boolean primary) {
        Container container = player.openContainer;
        if (container == null || container.windowId != windowId || slot < 0 || slot >= container.inventorySlots.size()
                || player.isSpectator()) {
            return false;
        }
        boolean done = BundleClicks.click(container.getSlot(slot), primary, player);
        if (done) {
            container.detectAndSendChanges();
            player.updateHeldItem();
        } else {
            // The client saw something else: give it the real slots and cursor.
            player.sendContainerToPlayer(container);
        }
        return done;
    }

    public static final class Handler implements IMessageHandler<BundleClickMessage, IMessage> {
        @Override
        public IMessage onMessage(final BundleClickMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> apply(player, message.windowId, message.slot, message.primary));
            return null;
        }
    }
}
