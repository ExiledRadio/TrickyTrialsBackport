package com.exiledradio.trickytrialsbackport.locator;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.network.NetworkHandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Stream 18, the locator bar (1.21.6): the server side. Players get the waypoint transmit and receive range
 * attributes, the {@code locatorBar} game rule is added to every world (on, as vanilla), and each world's
 * {@link WaypointManager} sends other players' waypoints to each player once a tick.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class LocatorBar {

    /** {@code GameRules.LOCATOR_BAR}, under its 1.21.6 name. */
    public static final String RULE = "locatorBar";

    private static final Map<World, WaypointManager> MANAGERS = new WeakHashMap<>();

    private static final WaypointManager.Sender NETWORK = (to, message) -> {
        if (to.connection != null && to.connection.getNetworkManager().isChannelOpen()) {
            NetworkHandler.channel.sendTo(message, to);
        }
    };

    private LocatorBar() {
    }

    public static boolean on() {
        return ModConfig.locatorBar.enabled;
    }

    /** Invisibility hides its wearer ({@code MobEffects.INVISIBILITY}'s {@code waypoint_transmit_range_hide} modifier). */
    public static void init() {
        MobEffects.INVISIBILITY.registerPotionAttributeModifier(Waypoints.TRANSMIT_RANGE, Waypoints.INVISIBILITY_ID.toString(), -1.0D, 2);
    }

    /** The rule's value: true where a world lacks it. */
    public static boolean rule(World world) {
        GameRules rules = world.getGameRules();
        return !rules.hasRule(RULE) || rules.getBoolean(RULE);
    }

    @SubscribeEvent
    public static void onLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote && !event.getWorld().getGameRules().hasRule(RULE)) {
            event.getWorld().getGameRules().addGameRule(RULE, "true", GameRules.ValueType.BOOLEAN_VALUE);
        }
    }

    @SubscribeEvent
    public static void onUnload(WorldEvent.Unload event) {
        MANAGERS.remove(event.getWorld());
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayer) {
            Waypoints.addAttributes((EntityPlayer) event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)) {
            return;
        }
        WorldServer world = (WorldServer) event.world;
        WaypointManager manager = MANAGERS.get(world);
        if (!on()) {
            if (manager != null) {
                manager.breakAll();
                MANAGERS.remove(world);
            }
            return;
        }
        if (manager == null) {
            manager = new WaypointManager(NETWORK);
            MANAGERS.put(world, manager);
        }
        List<EntityPlayerMP> players = new ArrayList<>();
        for (EntityPlayer p : world.playerEntities) {
            if (p instanceof EntityPlayerMP && !(p instanceof FakePlayer)) {
                players.add((EntityPlayerMP) p);
            }
        }
        manager.tick(players, rule(world), world.getMinecraftServer().getPlayerList().getViewDistance());
    }
}
