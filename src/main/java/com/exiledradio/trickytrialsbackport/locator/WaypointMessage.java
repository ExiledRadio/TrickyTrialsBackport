package com.exiledradio.trickytrialsbackport.locator;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

/**
 * Server to client, packet id 90 (the locator bar's block): 1.21.6's {@code ClientboundTrackedWaypointPacket}, one
 * waypoint's track, untrack or update. The waypoint is a player's UUID, its color (or none) and one of
 * {@code TrackedWaypoint}'s kinds: a block position, a chunk, or an angle for players more than 332 blocks away.
 *
 * <p>The handler names no client type: it hands plain values to {@code locator.client.ClientWaypoints}.
 */
public final class WaypointMessage implements IMessage {

    public static final int ID = 90;

    public static final byte TRACK = 0;
    public static final byte UNTRACK = 1;
    public static final byte UPDATE = 2;

    public byte operation;
    public UUID id;
    /** RGB, or -1 for none (the client then colors it by the UUID). */
    public int color = -1;
    /** {@link Waypoints#EMPTY}, {@link Waypoints#BLOCK}, {@link Waypoints#CHUNK} or {@link Waypoints#AZIMUTH}. */
    public byte type;
    /** BLOCK: x, y, z; CHUNK: chunk x, z. */
    public int x;
    public int y;
    public int z;
    /** AZIMUTH: radians. */
    public float angle;

    public WaypointMessage() {
    }

    static WaypointMessage of(byte operation, UUID id, int color, byte type) {
        WaypointMessage m = new WaypointMessage();
        m.operation = operation;
        m.id = id;
        m.color = color;
        m.type = type;
        return m;
    }

    static WaypointMessage block(byte operation, UUID id, int color, int x, int y, int z) {
        WaypointMessage m = of(operation, id, color, Waypoints.BLOCK);
        m.x = x;
        m.y = y;
        m.z = z;
        return m;
    }

    static WaypointMessage chunk(byte operation, UUID id, int color, int x, int z) {
        WaypointMessage m = of(operation, id, color, Waypoints.CHUNK);
        m.x = x;
        m.z = z;
        return m;
    }

    static WaypointMessage azimuth(byte operation, UUID id, int color, float angle) {
        WaypointMessage m = of(operation, id, color, Waypoints.AZIMUTH);
        m.angle = angle;
        return m;
    }

    /** {@code removeWaypoint}: an untrack with an empty waypoint. */
    static WaypointMessage remove(UUID id) {
        return of(UNTRACK, id, -1, Waypoints.EMPTY);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer b = new PacketBuffer(buf);
        this.operation = b.readByte();
        this.id = b.readUniqueId();
        this.color = b.readBoolean() ? b.readMedium() & 0xFFFFFF : -1;
        this.type = b.readByte();
        if (this.type == Waypoints.BLOCK) {
            this.x = b.readVarInt();
            this.y = b.readVarInt();
            this.z = b.readVarInt();
        } else if (this.type == Waypoints.CHUNK) {
            this.x = b.readVarInt();
            this.z = b.readVarInt();
        } else if (this.type == Waypoints.AZIMUTH) {
            this.angle = b.readFloat();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer b = new PacketBuffer(buf);
        b.writeByte(this.operation);
        b.writeUniqueId(this.id);
        b.writeBoolean(this.color >= 0);
        if (this.color >= 0) {
            b.writeMedium(this.color);
        }
        b.writeByte(this.type);
        if (this.type == Waypoints.BLOCK) {
            b.writeVarInt(this.x);
            b.writeVarInt(this.y);
            b.writeVarInt(this.z);
        } else if (this.type == Waypoints.CHUNK) {
            b.writeVarInt(this.x);
            b.writeVarInt(this.z);
        } else if (this.type == Waypoints.AZIMUTH) {
            b.writeFloat(this.angle);
        }
    }

    public static final class Handler implements IMessageHandler<WaypointMessage, IMessage> {
        @Override
        public IMessage onMessage(WaypointMessage m, MessageContext ctx) {
            com.exiledradio.trickytrialsbackport.locator.client.ClientWaypoints.receive(m.operation, m.id, m.color, m.type, m.x, m.y, m.z, m.angle);
            return null;
        }
    }
}
