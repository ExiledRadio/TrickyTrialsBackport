package com.exiledradio.trickytrialsbackport.effects;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client, packet id 40: which of the four effects' particles an entity shows (1.21's
 * {@code DATA_EFFECT_PARTICLES} and {@code DATA_EFFECT_AMBIENCE_ID}, for our effects). The state is
 * {@link EffectEvents#particleState}'s; 0 stops them.
 *
 * <p>The handler names no client type: it hands plain numbers to {@code effects.client.EffectsClient}.
 */
public final class EffectParticlesMessage implements IMessage {

    int entityId;
    int state;

    public EffectParticlesMessage() {
    }

    EffectParticlesMessage(int entityId, int state) {
        this.entityId = entityId;
        this.state = state;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.state = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeInt(this.state);
    }

    public static final class Handler implements IMessageHandler<EffectParticlesMessage, IMessage> {
        @Override
        public IMessage onMessage(EffectParticlesMessage message, MessageContext ctx) {
            com.exiledradio.trickytrialsbackport.effects.client.EffectsClient.schedule(message.entityId, message.state);
            return null;
        }
    }
}
