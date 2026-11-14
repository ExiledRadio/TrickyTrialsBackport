package com.exiledradio.trickytrialsbackport.chambers;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 1.21's {@code Structure.isValidBiome} for trial chambers: the noise biome of the stub's quart must be in
 * {@code #has_structure/trial_chambers} (every overworld biome but the deep dark).
 *
 * <ul>
 *   <li>Caves &amp; Cliffs' 1.18 terrain has three-dimensional noise biomes. Its generator keeps its terrain core's
 *       {@code BiomeSource} behind {@code biomes3d}, reached by reflection as Wild Update's ancient cities reach it
 *       (Backport Core has no public way yet); the quart's biome name is checked against the tag.</li>
 *   <li>1.12.2's terrain has one biome per column, all of them in the tag (oceans, rivers, beaches and mushroom
 *       fields included), so any overworld biome passes, except where Wild Update's deep dark is at the stub (its
 *       {@code DeepDark.at}, found by name when it is installed).</li>
 * </ul>
 */
final class ChamberBiomes {

    private ChamberBiomes() {
    }

    /**
     * @param world the world
     * @param pos the stub at this world's heights (1.21's own in a world that goes down to y -64)
     */
    static boolean valid(World world, BlockPos pos) {
        String noise = noiseBiome(world, pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
        if (noise != null) {
            return ChamberSettings.get().biomes.contains(noise);
        }
        Biome biome = world.getBiomeProvider().getBiome(pos, null);
        if (biome == null || BiomeDictionary.hasType(biome, BiomeDictionary.Type.NETHER)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.END) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.VOID)) {
            return false;
        }
        return !deepDark(world, pos);
    }

    private static boolean deepDarkLooked;
    @Nullable
    private static Method deepDarkAt;

    /** Wild Update's {@code DeepDark.at(World, BlockPos)}, where installed. */
    static boolean deepDark(World world, BlockPos pos) {
        synchronized (ChamberBiomes.class) {
            if (!deepDarkLooked) {
                deepDarkLooked = true;
                try {
                    deepDarkAt = Class.forName("com.exiledradio.wildupdatebackport.deepdark.DeepDark", false, ChamberBiomes.class.getClassLoader())
                            .getMethod("at", World.class, BlockPos.class);
                } catch (ReflectiveOperationException | LinkageError e) {
                    deepDarkAt = null;
                }
            }
        }
        if (deepDarkAt == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(deepDarkAt.invoke(null, world, pos));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static boolean noiseLooked;
    private static Field biomes3d, source;
    private static Method sourceBiome, biomeName;

    /** @return the 1.18 terrain's noise biome at a quart ("minecraft:..."), or null in a world without it */
    @Nullable
    static String noiseBiome(World world, int qx, int qy, int qz) {
        if (!(world instanceof WorldServer)) {
            return null;
        }
        Object generator = ((WorldServer) world).getChunkProvider().chunkGenerator;
        if (generator == null || !generator.getClass().getName().equals("com.exiledradio.cavesandcliffsbackport.terrain.ModernGenerator")) {
            return null;
        }
        try {
            synchronized (ChamberBiomes.class) {
                if (!noiseLooked) {
                    noiseLooked = true;
                    biomes3d = generator.getClass().getDeclaredField("biomes3d");
                    biomes3d.setAccessible(true);
                    source = biomes3d.getType().getDeclaredField("source");
                    source.setAccessible(true);
                    sourceBiome = source.getType().getMethod("biome", int.class, int.class, int.class);
                    biomeName = Class.forName(source.getType().getPackage().getName() + ".Biomes").getMethod("name", int.class);
                }
            }
            if (sourceBiome == null) {
                return null;
            }
            Object chunks = biomes3d.get(generator);
            synchronized (chunks) {
                Object src = source.get(chunks);
                int id = (Integer) sourceBiome.invoke(src, qx, qy, qz);
                return "minecraft:" + biomeName.invoke(null, id);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            synchronized (ChamberBiomes.class) {
                if (sourceBiome != null) {
                    TrickyTrialsBackport.LOGGER.warn("Caves & Cliffs' noise biome is out of reach; trial chambers check the column's biome.", e);
                }
                sourceBiome = null;
            }
            return null;
        }
    }
}
