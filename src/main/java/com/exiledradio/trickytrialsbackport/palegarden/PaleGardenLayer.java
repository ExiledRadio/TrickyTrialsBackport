package com.exiledradio.trickytrialsbackport.palegarden;

import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Places the pale garden on 1.12.2's biome layers: part of every roofed forest becomes pale garden.
 *
 * <h2>Why this way</h2>
 * 1.21.4 put the pale garden in the dark forest's climate: the plateau table's temperate, most humid slot,
 * which was the dark forest's until then, so pale gardens are dark forest ground on higher land. 1.12.2's
 * biome layers have no climate or plateau to read, so (the user's decision of 2026-11-07, plan "Decisions"
 * #2) a second, independent layer of patches about 256 blocks across is laid over the biome map, and a
 * roofed forest under one of them is a pale garden. Roughly one patch in three is pale, so a large roofed
 * forest usually holds a pale garden and small ones often do not; nothing else changes. The terrain is
 * the roofed forest's, so the boundary needs no blending. Chunks already generated keep their biome.
 *
 * <h2>How</h2>
 * Forge's {@link WorldTypeEvent.InitBiomeGens} hands over the two final layers (the 1:4 biome map that
 * shapes terrain and the 1:1 map stored in chunks). Each is wrapped by {@link Pale}, which reads the patch
 * mask at the same scale: the 1:1 mask is the 1:4 mask through vanilla's own voronoi zoom with vanilla's
 * seed (10), so both maps agree block for block exactly as vanilla's two layers do.
 *
 * <p>Caves &amp; Cliffs' 1.18 terrain places biomes by climate and does not use these layers; it places the
 * pale garden itself where 1.21.4 does and decorates it through {@link PaleGardenFeatures}.
 */
public final class PaleGardenLayer {

    /** One mask cell in this many is pale. */
    static final int CHANCE = 3;

    private PaleGardenLayer() {
    }

    /** Pre-init: the event is on Forge's terrain bus, which annotations do not reach. */
    public static void register() {
        MinecraftForge.TERRAIN_GEN_BUS.register(PaleGardenLayer.class);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onInitBiomeGens(WorldTypeEvent.InitBiomeGens event) {
        BiomePaleGarden pale = PaleGardenContent.biome;
        GenLayer[] layers = event.getNewBiomeGens();
        if (pale == null || layers == null || layers.length < 2) {
            return;
        }
        long seed = event.getSeed();
        GenLayer[] out = layers.clone();
        out[0] = new Pale(layers[0], mask(), pale);
        out[1] = new Pale(layers[1], new GenLayerVoronoiZoom(10L, mask()), pale);
        out[0].initWorldGenSeed(seed);
        out[1].initWorldGenSeed(seed);
        event.setNewBiomeGens(out);
    }

    /** The patch mask at the biome map's 1:4 scale: cells of 1:256, fuzzy then plain zooms, smoothed. */
    static GenLayer mask() {
        GenLayer mask = new Cells(73912L);
        mask = new GenLayerFuzzyZoom(2000L, mask);
        mask = GenLayerZoom.magnify(2001L, mask, 5);
        return new GenLayerSmooth(1000L, mask);
    }

    /** 1 in {@link #CHANCE} cells pale (1), the rest not (0). */
    static final class Cells extends GenLayer {
        Cells(long seed) {
            super(seed);
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int[] out = IntCache.getIntCache(width * depth);
            for (int dz = 0; dz < depth; dz++) {
                for (int dx = 0; dx < width; dx++) {
                    initChunkSeed(x + dx, z + dz);
                    out[dx + dz * width] = nextInt(CHANCE) == 0 ? 1 : 0;
                }
            }
            return out;
        }
    }

    /** A roofed forest where the mask is pale becomes the pale garden. */
    static final class Pale extends GenLayer {
        private final GenLayer biomes;
        private final GenLayer mask;
        private final BiomePaleGarden pale;

        Pale(GenLayer biomes, GenLayer mask, BiomePaleGarden pale) {
            super(0L);
            this.biomes = biomes;
            this.mask = mask;
            this.pale = pale;
            this.parent = biomes;
        }

        @Override
        public void initWorldGenSeed(long seed) {
            this.biomes.initWorldGenSeed(seed);
            this.mask.initWorldGenSeed(seed);
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int[] in = this.biomes.getInts(x, z, width, depth);
            int[] cells = this.mask.getInts(x, z, width, depth);
            int forest = Biome.getIdForBiome(Biomes.ROOFED_FOREST);
            int paleId = Biome.getIdForBiome(this.pale);
            int[] out = IntCache.getIntCache(width * depth);
            for (int i = 0; i < width * depth; i++) {
                out[i] = in[i] == forest && cells[i] == 1 ? paleId : in[i];
            }
            return out;
        }
    }
}
