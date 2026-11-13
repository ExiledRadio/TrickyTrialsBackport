package com.exiledradio.trickytrialsbackport.trial;

import com.exiledradio.trickytrialsbackport.network.NetworkHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client: one of 1.21's trial level events, with its number and data, to players within 64
 * blocks as vanilla sends level events. Packet id 35 (the trials block).
 *
 * <p>The client plays the sound and draws the particles, as 1.21's {@code LevelEventHandler} does; the
 * handler hands plain values to {@code trial.client.TrialFx}, which only a client loads.
 */
public final class TrialMessage implements IMessage {

    /** A mob spawned: flames at the spawner. */
    public static final int SPAWN_PARTICLES = 3011;
    /** A mob spawned: sound and flames where it stands. */
    public static final int SPAWN_MOB = 3012;
    public static final int DETECT_PLAYER = 3013;
    public static final int EJECT_ITEM = 3014;
    public static final int VAULT_ACTIVATE = 3015;
    public static final int VAULT_DEACTIVATE = 3016;
    public static final int VAULT_EJECT_ITEM = 3017;
    public static final int DETECT_PLAYER_OMINOUS = 3019;
    public static final int BECOME_OMINOUS = 3020;
    public static final int SPAWN_ITEM = 3021;

    int event;
    BlockPos pos;
    int data;

    public TrialMessage() {
    }

    TrialMessage(int event, BlockPos pos, int data) {
        this.event = event;
        this.pos = pos;
        this.data = data;
    }

    public static void send(World world, BlockPos pos, int event, int data) {
        if (world.isRemote) {
            return;
        }
        NetworkHandler.channel.sendToAllAround(new TrialMessage(event, pos, data),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 64));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.event = buf.readInt();
        this.pos = BlockPos.fromLong(buf.readLong());
        this.data = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.event);
        buf.writeLong(this.pos.toLong());
        buf.writeInt(this.data);
    }

    public static final class Handler implements IMessageHandler<TrialMessage, IMessage> {
        @Override
        public IMessage onMessage(TrialMessage message, MessageContext ctx) {
            com.exiledradio.trickytrialsbackport.trial.client.TrialFx.schedule(message.event, message.pos.toLong(), message.data);
            return null;
        }
    }
}
