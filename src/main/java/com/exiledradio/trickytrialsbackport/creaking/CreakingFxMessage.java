package com.exiledradio.trickytrialsbackport.creaking;

import com.exiledradio.trickytrialsbackport.network.NetworkHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

/**
 * Server to client, packet id 65 (the creaking's block): the creaking's two modern particles, which 1.12.2 lacks.
 *
 * <ul>
 *   <li>{@link #TRAIL}: 1.21.4's {@code trail} particle, one per entry (start, target, duration), all one color.
 *       Sent to every player within 512 blocks, as vanilla's forced {@code sendParticles}.</li>
 *   <li>{@link #CRUMBLE}: {@code block_crumble}: a count of still terrain particles of one block state, spread
 *       by a gaussian around a center, each living 1-10 ticks. Sent within 32 blocks, as vanilla's unforced
 *       one.</li>
 * </ul>
 * The handler names no client type: it hands plain numbers to {@code creaking.client.CreakingParticles}.
 */
public final class CreakingFxMessage implements IMessage {

    public static final int ID = 65;
    static final byte TRAIL = 0;
    static final byte CRUMBLE = 1;

    byte kind;
    int color;
    /** TRAIL: x, y, z, tx, ty, tz, duration per entry; CRUMBLE: x, y, z, dx, dy, dz. */
    double[] data = new double[0];
    int count;
    int stateId;

    public CreakingFxMessage() {
    }

    static void trails(WorldServer world, BlockPos at, int color, List<double[]> trails) {
        if (trails.isEmpty()) {
            return;
        }
        CreakingFxMessage m = new CreakingFxMessage();
        m.kind = TRAIL;
        m.color = color;
        m.count = trails.size();
        m.data = new double[trails.size() * 7];
        for (int i = 0; i < trails.size(); i++) {
            System.arraycopy(trails.get(i), 0, m.data, i * 7, 7);
        }
        NetworkHandler.channel.sendToAllAround(m, new NetworkRegistry.TargetPoint(world.provider.getDimension(), at.getX() + 0.5D,
                at.getY() + 0.5D, at.getZ() + 0.5D, 512.0D));
    }

    static void crumble(WorldServer world, Vec3d c, double dx, double dy, double dz, int count, IBlockState state) {
        CreakingFxMessage m = new CreakingFxMessage();
        m.kind = CRUMBLE;
        m.count = count;
        m.stateId = Block.getStateId(state);
        m.data = new double[]{c.x, c.y, c.z, dx, dy, dz};
        NetworkHandler.channel.sendToAllAround(m, new NetworkRegistry.TargetPoint(world.provider.getDimension(), c.x, c.y, c.z, 32.0D));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.kind = buf.readByte();
        this.color = buf.readInt();
        this.count = buf.readShort();
        this.stateId = buf.readInt();
        int n = buf.readShort();
        this.data = new double[n];
        for (int i = 0; i < n; i++) {
            this.data[i] = buf.readDouble();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.kind);
        buf.writeInt(this.color);
        buf.writeShort(this.count);
        buf.writeInt(this.stateId);
        buf.writeShort(this.data.length);
        for (double d : this.data) {
            buf.writeDouble(d);
        }
    }

    public static final class Handler implements IMessageHandler<CreakingFxMessage, IMessage> {
        @Override
        public IMessage onMessage(CreakingFxMessage m, MessageContext ctx) {
            com.exiledradio.trickytrialsbackport.creaking.client.CreakingParticles.schedule(m.kind, m.color, m.count, m.stateId, m.data);
            return null;
        }
    }
}
