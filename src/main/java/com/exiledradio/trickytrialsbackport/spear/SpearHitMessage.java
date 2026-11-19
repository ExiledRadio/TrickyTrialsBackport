package com.exiledradio.trickytrialsbackport.spear;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client, packet id 111: a charge hit something (1.21.11's entity event 2 for a kinetic hit, which in
 * 1.12.2 would flash the wielder red). Each client plays the hit sound at the wielder, and the wielder's own view
 * jolts the spear. The handler names no client type.
 */
public final class SpearHitMessage implements IMessage {

    int entityId;
    boolean wood;

    public SpearHitMessage() {
    }

    public SpearHitMessage(int entityId, boolean wood) {
        this.entityId = entityId;
        this.wood = wood;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.wood = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeBoolean(this.wood);
    }

    public static final class Handler implements IMessageHandler<SpearHitMessage, IMessage> {
        @Override
        public IMessage onMessage(final SpearHitMessage m, MessageContext ctx) {
            com.exiledradio.trickytrialsbackport.spear.client.SpearClient.kineticHit(m.entityId, m.wood);
            return null;
        }
    }
}
