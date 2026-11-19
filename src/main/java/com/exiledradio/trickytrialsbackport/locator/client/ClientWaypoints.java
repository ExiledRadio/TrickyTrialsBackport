package com.exiledradio.trickytrialsbackport.locator.client;

import com.exiledradio.trickytrialsbackport.locator.WaypointMessage;
import com.exiledradio.trickytrialsbackport.locator.Waypoints;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** {@code ClientWaypointManager}: the waypoints the server sent, by UUID. */
@SideOnly(Side.CLIENT)
public final class ClientWaypoints {

    /** One {@code TrackedWaypoint}. */
    static final class Tracked {
        final UUID id;
        int color;
        byte type;
        int x;
        int y;
        int z;
        float angle;

        Tracked(UUID id) {
            this.id = id;
        }
    }

    static final Map<UUID, Tracked> WAYPOINTS = new ConcurrentHashMap<>();

    ClientWaypoints() {
    }

    /** Called by {@link WaypointMessage.Handler} on the network thread; applied on the client thread. */
    public static void receive(byte operation, UUID id, int color, byte type, int x, int y, int z, float angle) {
        Minecraft.getMinecraft().addScheduledTask(() -> apply(operation, id, color, type, x, y, z, angle));
    }

    static void apply(byte operation, UUID id, int color, byte type, int x, int y, int z, float angle) {
        if (operation == WaypointMessage.UNTRACK) {
            WAYPOINTS.remove(id);
            return;
        }
        Tracked t = WAYPOINTS.get(id);
        if (operation == WaypointMessage.TRACK || t == null) {
            if (operation != WaypointMessage.TRACK) {
                // Vanilla's update of an unknown waypoint fails; there is nothing to update.
                return;
            }
            t = new Tracked(id);
            t.color = color;
            t.type = type;
            WAYPOINTS.put(id, t);
        } else if (t.type != type) {
            // TrackedWaypoint.update: an update of another kind is ignored.
            return;
        }
        if (type == Waypoints.BLOCK || type == Waypoints.CHUNK) {
            t.x = x;
            t.y = y;
            t.z = z;
        } else if (type == Waypoints.AZIMUTH) {
            t.angle = angle;
        }
    }

    public static boolean hasWaypoints() {
        return !WAYPOINTS.isEmpty();
    }

    /** The waypoints go with the connection. */
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        WAYPOINTS.clear();
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        WAYPOINTS.clear();
    }
}
