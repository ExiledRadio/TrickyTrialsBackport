package com.exiledradio.trickytrialsbackport.common;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client: a wind burst went off here; draw its gust. Packet id 0 (common's block).
 *
 * <p>The handler names no client type: it hands plain numbers to {@code common.client.ClientGust}, which
 * only a client ever loads.
 */
public final class GustMessage implements IMessage {

    double x;
    double y;
    double z;
    boolean large;

    public GustMessage() {
    }

    GustMessage(double x, double y, double z, boolean large) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.large = large;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.large = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeBoolean(this.large);
    }

    public static final class Handler implements IMessageHandler<GustMessage, IMessage> {
        @Override
        public IMessage onMessage(GustMessage message, MessageContext ctx) {
            com.exiledradio.trickytrialsbackport.common.client.ClientGust.schedule(message.x, message.y, message.z, message.large);
            return null;
        }
    }
}
