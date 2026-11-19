package com.exiledradio.trickytrialsbackport.spear;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client to server, packet id 110: the attack button with a spear in the main hand (1.21.11's
 * {@code ServerboundPlayerActionPacket.Action.STAB}). Carries nothing; the server checks the spear and the attack
 * charge (5 ticks' leeway, as 1.21.11 allows) and jabs.
 */
public final class SpearJabMessage implements IMessage {

    public SpearJabMessage() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static void apply(EntityPlayerMP player) {
        if (!SpearContent.enabled() || player.isSpectator() || player.isHandActive()
                || !(player.getHeldItemMainhand().getItem() instanceof ItemSpear) || !SpearAttacks.charged(player, 5.0F)) {
            return;
        }
        player.markPlayerActive();
        SpearAttacks.jab(player);
    }

    public static final class Handler implements IMessageHandler<SpearJabMessage, IMessage> {
        @Override
        public IMessage onMessage(SpearJabMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> apply(player));
            return null;
        }
    }
}
