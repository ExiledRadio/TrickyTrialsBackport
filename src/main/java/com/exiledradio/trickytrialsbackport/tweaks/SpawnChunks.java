package com.exiledradio.trickytrialsbackport.tweaks;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Field;

/**
 * 1.21.9 removed spawn chunks: nothing around the world spawn stays loaded for its own sake, and no area is prepared
 * when the server starts.
 *
 * <p>Trails &amp; Tales Backport already patches the same two places for 1.20.5's radius of 2 (its patch P10) and keeps
 * one value, {@code mechanics.SpawnChunks.radius}, for this mod to set: with it installed, {@link #postInit} sets that
 * radius to 0 and this mod's own patch P15 stands aside (the coremod looks for its class), so the two never patch the
 * same sites. Without it, P15 makes {@code World.isSpawnChunk} and {@code MinecraftServer.initialWorldChunkLoad} ask
 * this class.
 */
public final class SpawnChunks {

    static final String TT_MOD = "trailsandtalesbackport";
    static final String TT_CLASS = "com.exiledradio.trailsandtalesbackport.mechanics.SpawnChunks";

    private SpawnChunks() {
    }

    static boolean on() {
        return Tweaks.on(ModConfig.tweaks.noSpawnChunks);
    }

    /** With Trails &amp; Tales: its radius becomes 0 (1.21.9), or stays its own 2 with the setting off. */
    public static void postInit() {
        if (!Loader.isModLoaded(TT_MOD)) {
            return;
        }
        try {
            Field radius = Class.forName(TT_CLASS).getField("radius");
            radius.setInt(null, on() ? 0 : 2);
            TrickyTrialsBackport.LOGGER.info("Spawn chunks: Trails & Tales' radius set to {}", radius.getInt(null));
        } catch (ReflectiveOperationException | RuntimeException e) {
            TrickyTrialsBackport.LOGGER.warn("Spawn chunks: could not set Trails & Tales' radius", e);
        }
    }

    /** P15: the whole body of {@code World.isSpawnChunk(x, z)}. 1.12.2 keeps 17 by 17 chunks; 1.21.9 none. */
    public static boolean isSpawnChunk(World world, int x, int z) {
        if (on()) {
            return false;
        }
        BlockPos spawn = world.getSpawnPoint();
        int dx = x * 16 + 8 - spawn.getX();
        int dz = z * 16 + 8 - spawn.getZ();
        return dx >= -128 && dx <= 128 && dz >= -128 && dz <= 128;
    }

    /** P15: {@code initialWorldChunkLoad}'s 192, how far the start area reaches; -16 runs its loops no times. */
    public static int prepareBlocks() {
        return on() ? -16 : 192;
    }

    /** P15: {@code initialWorldChunkLoad}'s -192, where its loops start. */
    public static int prepareStart() {
        return -prepareBlocks();
    }

    /** P15: {@code initialWorldChunkLoad}'s 625, the count its progress is out of (never 0: it divides). */
    public static int prepareCount() {
        return on() ? 1 : 625;
    }
}
