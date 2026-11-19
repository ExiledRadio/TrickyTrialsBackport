package com.exiledradio.trickytrialsbackport.locator;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One world's {@code ServerWaypointManager}: the connections between players who transmit a waypoint and players
 * who receive them, each sending its own packets.
 *
 * <p>Vanilla updates a player's connections whenever an entity moves ({@code Entity.setPos}) and each tick through
 * {@code ServerChunkCache.move}; here {@link #tick} does the same once a tick over the world's players. A connection
 * sends only when its waypoint changed: a block position moved by one block, a chunk changed, or the angle turned by
 * more than half a degree (vanilla's rates).
 */
public final class WaypointManager {

    /** Where packets go: the network in game, a recorder in the self-test. */
    public interface Sender {
        void send(EntityPlayerMP to, WaypointMessage message);
    }

    /** One connection ({@code WaypointTransmitter.Entity*Connection}). */
    static final class Connection {
        final EntityLivingBase source;
        final EntityPlayerMP receiver;
        final int type;
        final int color;
        BlockPos lastBlock;
        int lastChunkX;
        int lastChunkZ;
        float lastAngle;

        Connection(EntityLivingBase source, EntityPlayerMP receiver, int type, int color) {
            this.source = source;
            this.receiver = receiver;
            this.type = type;
            this.color = color;
            this.lastBlock = new BlockPos(source);
            this.lastChunkX = MathHelper.floor(source.posX) >> 4;
            this.lastChunkZ = MathHelper.floor(source.posZ) >> 4;
            this.lastAngle = Waypoints.azimuth(source, receiver);
        }

        WaypointMessage message(byte op) {
            switch (this.type) {
                case Waypoints.AZIMUTH:
                    return WaypointMessage.azimuth(op, this.source.getUniqueID(), this.color, this.lastAngle);
                case Waypoints.CHUNK:
                    return WaypointMessage.chunk(op, this.source.getUniqueID(), this.color, this.lastChunkX, this.lastChunkZ);
                default:
                    return WaypointMessage.block(op, this.source.getUniqueID(), this.color, this.lastBlock.getX(), this.lastBlock.getY(),
                            this.lastBlock.getZ());
            }
        }

        boolean broken(int viewDistance) {
            if (this.color != Waypoints.teamColor(this.source)) {
                // ServerScoreboard.updatePlayerWaypoint: a team change remakes the connections.
                return true;
            }
            switch (this.type) {
                case Waypoints.AZIMUTH:
                    return Waypoints.ignores(this.source, this.receiver)
                            || Waypoints.chunkVisible(MathHelper.floor(this.source.posX) >> 4, MathHelper.floor(this.source.posZ) >> 4,
                            this.receiver, viewDistance)
                            || !Waypoints.reallyFar(this.source, this.receiver);
                case Waypoints.CHUNK: {
                    int cx = MathHelper.floor(this.source.posX) >> 4;
                    int cz = MathHelper.floor(this.source.posZ) >> 4;
                    int chessboard = Math.max(Math.abs(cx - this.lastChunkX), Math.abs(cz - this.lastChunkZ));
                    return chessboard > 1 || Waypoints.ignores(this.source, this.receiver)
                            || Waypoints.chunkVisible(this.lastChunkX, this.lastChunkZ, this.receiver, viewDistance);
                }
                default: {
                    BlockPos now = new BlockPos(this.source);
                    int manhattan = Math.abs(now.getX() - this.lastBlock.getX()) + Math.abs(now.getY() - this.lastBlock.getY())
                            + Math.abs(now.getZ() - this.lastBlock.getZ());
                    return manhattan > 1 || Waypoints.ignores(this.source, this.receiver);
                }
            }
        }

        /** @return the update to send, or null when nothing changed */
        WaypointMessage update() {
            switch (this.type) {
                case Waypoints.AZIMUTH: {
                    float angle = Waypoints.azimuth(this.source, this.receiver);
                    if (Math.abs(angle - this.lastAngle) > Waypoints.AZIMUTH_STEP) {
                        this.lastAngle = angle;
                        return message(WaypointMessage.UPDATE);
                    }
                    return null;
                }
                case Waypoints.CHUNK: {
                    int cx = MathHelper.floor(this.source.posX) >> 4;
                    int cz = MathHelper.floor(this.source.posZ) >> 4;
                    if (cx != this.lastChunkX || cz != this.lastChunkZ) {
                        this.lastChunkX = cx;
                        this.lastChunkZ = cz;
                        return message(WaypointMessage.UPDATE);
                    }
                    return null;
                }
                default: {
                    BlockPos now = new BlockPos(this.source);
                    if (!now.equals(this.lastBlock)) {
                        this.lastBlock = now;
                        return message(WaypointMessage.UPDATE);
                    }
                    return null;
                }
            }
        }
    }

    private final Sender sender;
    /** Receiver to (source to connection). Identity maps: a respawned player is a new object with the same UUID. */
    private final Map<EntityPlayerMP, Map<EntityLivingBase, Connection>> connections = new IdentityHashMap<>();
    private final Set<EntityLivingBase> transmitters = new HashSet<>();
    private boolean ruleWasOn = true;

    public WaypointManager(Sender sender) {
        this.sender = sender;
    }

    /**
     * One tick of the world.
     *
     * @param players      the world's players (fake players excluded by the caller)
     * @param ruleOn       the {@code locatorBar} game rule
     * @param viewDistance the server's view distance in chunks
     */
    public void tick(List<EntityPlayerMP> players, boolean ruleOn, int viewDistance) {
        Set<EntityPlayerMP> present = java.util.Collections.newSetFromMap(new IdentityHashMap<EntityPlayerMP, Boolean>());
        present.addAll(players);
        // ServerPlayer.updatePlayerAttributes and the head slot's modifier, before anything reads the ranges.
        for (EntityPlayerMP p : players) {
            Waypoints.updateModifiers(p);
        }
        // Players who left the world (logged out, changed dimension, respawned): ServerWaypointManager.removePlayer
        // and untrackWaypoint. Done before anything is added, so a respawned player's old waypoint goes first.
        for (Iterator<Map.Entry<EntityPlayerMP, Map<EntityLivingBase, Connection>>> it = this.connections.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<EntityPlayerMP, Map<EntityLivingBase, Connection>> e = it.next();
            if (!present.contains(e.getKey()) || !Waypoints.receives(e.getKey())) {
                for (Connection c : e.getValue().values()) {
                    disconnect(c);
                }
                it.remove();
            }
        }
        for (EntityLivingBase source : new ArrayList<>(this.transmitters)) {
            if (!present.contains(source) || !Waypoints.transmits(source)) {
                untrack(source);
            }
        }
        // MinecraftServer.onGameRuleChanged: turned off, every connection breaks; turned on, players are updated.
        if (!ruleOn) {
            if (this.ruleWasOn) {
                breakAll();
            }
            this.ruleWasOn = false;
            return;
        }
        this.ruleWasOn = true;
        for (EntityPlayerMP p : players) {
            if (Waypoints.transmits(p)) {
                this.transmitters.add(p);
            }
        }
        for (EntityPlayerMP receiver : players) {
            if (!Waypoints.receives(receiver)) {
                continue;
            }
            Map<EntityLivingBase, Connection> row = this.connections.get(receiver);
            if (row == null) {
                row = new IdentityHashMap<>();
                this.connections.put(receiver, row);
            }
            for (EntityLivingBase source : this.transmitters) {
                if (source == receiver) {
                    continue;
                }
                Connection c = row.get(source);
                if (c != null && !c.broken(viewDistance)) {
                    WaypointMessage m = c.update();
                    if (m != null) {
                        this.sender.send(receiver, m);
                    }
                } else {
                    // createConnection / updateConnection's remake: a new connection replaces the old, or none removes it.
                    Connection made = make(source, receiver, viewDistance);
                    if (made != null) {
                        row.put(source, made);
                        this.sender.send(receiver, made.message(WaypointMessage.TRACK));
                    } else if (c != null) {
                        row.remove(source);
                        disconnect(c);
                    }
                }
            }
        }
    }

    private static Connection make(EntityLivingBase source, EntityPlayerMP receiver, int viewDistance) {
        int type = Waypoints.connectionType(source, receiver, viewDistance);
        return type < 0 ? null : new Connection(source, receiver, type, Waypoints.teamColor(source));
    }

    private void untrack(EntityLivingBase source) {
        for (Map<EntityLivingBase, Connection> row : this.connections.values()) {
            Connection c = row.remove(source);
            if (c != null) {
                disconnect(c);
            }
        }
        this.transmitters.remove(source);
    }

    private void disconnect(Connection c) {
        this.sender.send(c.receiver, WaypointMessage.remove(c.source.getUniqueID()));
    }

    /** {@code breakAllConnections}. */
    public void breakAll() {
        for (Map<EntityLivingBase, Connection> row : this.connections.values()) {
            for (Connection c : row.values()) {
                disconnect(c);
            }
            row.clear();
        }
    }

    /** For the self-test: the kind of connection from a source to a receiver, or -1 for none. */
    public int connection(EntityLivingBase source, EntityPlayerMP receiver) {
        Map<EntityLivingBase, Connection> row = this.connections.get(receiver);
        Connection c = row == null ? null : row.get(source);
        return c == null ? -1 : c.type;
    }

    /** For the self-test: how many connections there are. */
    public int size() {
        int n = 0;
        for (Map<EntityLivingBase, Connection> row : this.connections.values()) {
            n += row.size();
        }
        return n;
    }
}
