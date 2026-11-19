package com.exiledradio.trickytrialsbackport.animals;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client, packet id 80 (the animals' block): a cow's, pig's or chicken's variant, sent when a player starts
 * tracking it and whenever it changes. The handler names no client type: it hands the numbers to
 * {@code animals.client.AnimalsClient}.
 */
public final class AnimalVariantMessage implements IMessage {

    public static final int ID = 80;

    int entity;
    byte variant;

    public AnimalVariantMessage() {
    }

    AnimalVariantMessage(int entity, FarmVariant variant) {
        this.entity = entity;
        this.variant = (byte) variant.ordinal();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entity = buf.readInt();
        this.variant = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeByte(this.variant);
    }

    public static final class Handler implements IMessageHandler<AnimalVariantMessage, IMessage> {
        @Override
        public IMessage onMessage(AnimalVariantMessage m, MessageContext ctx) {
            com.exiledradio.trickytrialsbackport.animals.client.AnimalsClient.schedule(m.entity, m.variant);
            return null;
        }
    }
}
