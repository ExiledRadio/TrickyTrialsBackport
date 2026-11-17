package com.exiledradio.trickytrialsbackport.tweaks;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityMinecartTNT;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The game rules 1.21.5 and 1.21.9 added, under the names they had then (1.21.11's snake-case renames are out of scope):
 * {@code tntExplodes}, {@code allowFireTicksAwayFromPlayer} (1.21.5), {@code allowEnteringNetherUsingPortals},
 * {@code spawnMonsters}, {@code pvp}, {@code commandBlocksEnabled} and {@code spawnerBlocksEnabled} (1.21.9). Each is
 * added to a world that lacks it, with vanilla's default; with the setting off they exist but do nothing.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class GameRuleTweaks {

    public static final String TNT_EXPLODES = "tntExplodes";
    public static final String FIRE_AWAY = "allowFireTicksAwayFromPlayer";
    public static final String NETHER_PORTALS = "allowEnteringNetherUsingPortals";
    public static final String SPAWN_MONSTERS = "spawnMonsters";
    public static final String PVP = "pvp";
    public static final String COMMAND_BLOCKS = "commandBlocksEnabled";
    public static final String SPAWNER_BLOCKS = "spawnerBlocksEnabled";

    private static final String[][] RULES = {
            {TNT_EXPLODES, "true"}, {FIRE_AWAY, "false"}, {NETHER_PORTALS, "true"}, {SPAWN_MONSTERS, "true"},
            {PVP, "true"}, {COMMAND_BLOCKS, "true"}, {SPAWNER_BLOCKS, "true"},
    };

    private GameRuleTweaks() {
    }

    @SubscribeEvent
    public static void onLoad(WorldEvent.Load event) {
        if (Tweaks.server(event.getWorld())) {
            addRules(event.getWorld().getGameRules());
        }
    }

    public static void addRules(GameRules rules) {
        for (String[] rule : RULES) {
            if (!rules.hasRule(rule[0])) {
                rules.addGameRule(rule[0], rule[1], GameRules.ValueType.BOOLEAN_VALUE);
            }
        }
    }

    /** @return the rule's value, or its default where the world lacks it or the setting is off */
    static boolean rule(World world, String name, boolean byDefault) {
        if (!Tweaks.on(ModConfig.tweaks.gameRules)) {
            return byDefault;
        }
        GameRules rules = world.getGameRules();
        return rules.hasRule(name) ? rules.getBoolean(name) : byDefault;
    }

    public static boolean netherPortalsAllowed(World world) {
        return rule(world, NETHER_PORTALS, true);
    }

    public static boolean fireAwayFromPlayers(World world) {
        return rule(world, FIRE_AWAY, false);
    }

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        Entity e = event.getEntity();
        if (event.getDimension() == -1 && e.dimension == 0 && !netherPortalsAllowed(e.world)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Start event) {
        Entity exploder = Tweaks.exploder(event.getExplosion());
        if ((exploder instanceof EntityTNTPrimed || exploder instanceof EntityMinecartTNT)
                && !rule(event.getWorld(), TNT_EXPLODES, true)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.getWorld().isRemote) {
            return;
        }
        if (event.isSpawner()) {
            if (!rule(event.getWorld(), SPAWNER_BLOCKS, true)) {
                event.setResult(Event.Result.DENY);
            }
        } else if (event.getEntityLiving().isCreatureType(EnumCreatureType.MONSTER, false)
                && !rule(event.getWorld(), SPAWN_MONSTERS, true)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer && !event.getEntityLiving().world.isRemote) {
            Entity source = event.getSource().getTrueSource();
            if (source instanceof EntityPlayer && source != event.getEntityLiving()
                    && !rule(event.getEntityLiving().world, PVP, true)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (event.getSender() instanceof CommandBlockBaseLogic
                && !rule(event.getSender().getEntityWorld(), COMMAND_BLOCKS, true)) {
            event.setCanceled(true);
        }
    }
}
