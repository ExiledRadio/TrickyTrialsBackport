package com.exiledradio.trickytrialsbackport.nautilus;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Packet 115 (client to server): the inventory key while riding a nautilus opens its inventory, as 1.21.11's
 * {@code HasCustomInventoryScreen} does (1.12.2 does it only for horses).
 */
public class NautilusOpenMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<NautilusOpenMessage, IMessage> {
        @Override
        public IMessage onMessage(NautilusOpenMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> NautilusContent.openRidden(player));
            return null;
        }
    }
}
