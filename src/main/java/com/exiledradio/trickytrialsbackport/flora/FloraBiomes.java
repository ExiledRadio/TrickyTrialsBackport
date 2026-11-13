package com.exiledradio.trickytrialsbackport.flora;

import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which of this stream's placed features each biome has, in 1.21.5's order within the vegetal step (read from the
 * 1.21.11 jar's {@code worldgen/biome} files; 1.21.5's are the same), and 1.12.2's biomes by their modern names.
 *
 * <p>A name in brackets is a tree feature whose random selector holds fallen trees or leaf-litter trees
 * ({@link FallenTrees#SELECTORS}): the trees themselves are the terrain's, only what 1.21.5 added to them is ours.
 */
public final class FloraBiomes {

    private static final String FIREFLY = "patch_firefly_bush_near_water";
    private static final Map<String, List<String>> FEATURES = new HashMap<String, List<String>>();
    private static final Map<Biome, String> OLD = new IdentityHashMap<Biome, String>();

    static {
        put("badlands", "patch_dry_grass_badlands", "patch_cactus_decorated", FIREFLY);
        put("bamboo_jungle", FIREFLY);
        put("beach", FIREFLY);
        put("birch_forest", "wildflowers_birch_forest", "[trees_birch]", "patch_bush", FIREFLY);
        put("cold_ocean", FIREFLY);
        put("dark_forest", "[dark_forest_vegetation]", "patch_leaf_litter", FIREFLY);
        put("deep_cold_ocean", FIREFLY);
        put("deep_dark", "[trees_plains]");
        put("deep_frozen_ocean", FIREFLY);
        put("deep_lukewarm_ocean", FIREFLY);
        put("deep_ocean", FIREFLY);
        put("desert", "patch_dry_grass_desert", "patch_cactus_desert");
        put("dripstone_caves", "[trees_plains]");
        put("eroded_badlands", "patch_dry_grass_badlands", "patch_cactus_decorated", FIREFLY);
        put("flower_forest", "[trees_flower_forest]", FIREFLY);
        put("forest", "[trees_birch_and_oak_leaf_litter]", "patch_bush", FIREFLY);
        put("frozen_ocean", FIREFLY);
        put("frozen_river", "patch_bush", FIREFLY);
        put("ice_spikes", "[trees_snowy]", FIREFLY);
        put("jungle", "[trees_jungle]", FIREFLY);
        put("lukewarm_ocean", FIREFLY);
        put("mangrove_swamp", FIREFLY);
        put("meadow", "wildflowers_meadow");
        put("mushroom_fields", FIREFLY);
        put("ocean", FIREFLY);
        put("old_growth_birch_forest", "wildflowers_birch_forest", "[birch_tall]", "patch_bush", FIREFLY);
        put("old_growth_pine_taiga", "[trees_old_growth_pine_taiga]", FIREFLY);
        put("old_growth_spruce_taiga", "[trees_old_growth_spruce_taiga]", FIREFLY);
        put("pale_garden", FIREFLY);
        put("plains", "patch_bush", "[trees_plains]", FIREFLY);
        put("river", "patch_bush", FIREFLY);
        put("savanna", "[trees_savanna]", FIREFLY);
        put("savanna_plateau", "[trees_savanna]", FIREFLY);
        put("snowy_beach", FIREFLY);
        put("snowy_plains", "[trees_snowy]", FIREFLY);
        put("snowy_taiga", "[trees_taiga]", FIREFLY);
        put("sparse_jungle", "[trees_sparse_jungle]", FIREFLY);
        put("stony_shore", FIREFLY);
        put("sunflower_plains", "[trees_plains]", FIREFLY);
        put("swamp", "patch_firefly_bush_swamp", "patch_firefly_bush_near_water_swamp");
        put("taiga", "[trees_taiga]", FIREFLY);
        put("warm_ocean", FIREFLY);
        put("windswept_forest", "[trees_windswept_forest]", "patch_bush", FIREFLY);
        put("windswept_gravelly_hills", "[trees_windswept_hills]", "patch_bush", FIREFLY);
        put("windswept_hills", "[trees_windswept_hills]", "patch_bush", FIREFLY);
        put("windswept_savanna", "[trees_windswept_savanna]", FIREFLY);
        put("wooded_badlands", "[trees_badlands]", "patch_dry_grass_badlands", "patch_cactus_decorated", FIREFLY);

        // 1.12.2's biomes as 1.18 renamed them (the Caves & Cliffs update's biome merge).
        old("ocean", Biomes.OCEAN);
        old("deep_ocean", Biomes.DEEP_OCEAN);
        old("frozen_ocean", Biomes.FROZEN_OCEAN);
        old("plains", Biomes.PLAINS);
        old("sunflower_plains", Biomes.MUTATED_PLAINS);
        old("desert", Biomes.DESERT, Biomes.DESERT_HILLS, Biomes.MUTATED_DESERT);
        old("windswept_hills", Biomes.EXTREME_HILLS, Biomes.EXTREME_HILLS_EDGE);
        old("windswept_gravelly_hills", Biomes.MUTATED_EXTREME_HILLS, Biomes.MUTATED_EXTREME_HILLS_WITH_TREES);
        old("windswept_forest", Biomes.EXTREME_HILLS_WITH_TREES);
        old("forest", Biomes.FOREST, Biomes.FOREST_HILLS);
        old("flower_forest", Biomes.MUTATED_FOREST);
        old("taiga", Biomes.TAIGA, Biomes.TAIGA_HILLS, Biomes.MUTATED_TAIGA);
        old("swamp", Biomes.SWAMPLAND, Biomes.MUTATED_SWAMPLAND);
        old("river", Biomes.RIVER);
        old("frozen_river", Biomes.FROZEN_RIVER);
        old("snowy_plains", Biomes.ICE_PLAINS, Biomes.ICE_MOUNTAINS);
        old("ice_spikes", Biomes.MUTATED_ICE_FLATS);
        old("mushroom_fields", Biomes.MUSHROOM_ISLAND, Biomes.MUSHROOM_ISLAND_SHORE);
        old("beach", Biomes.BEACH);
        old("snowy_beach", Biomes.COLD_BEACH);
        old("stony_shore", Biomes.STONE_BEACH);
        old("jungle", Biomes.JUNGLE, Biomes.JUNGLE_HILLS, Biomes.MUTATED_JUNGLE);
        old("sparse_jungle", Biomes.JUNGLE_EDGE, Biomes.MUTATED_JUNGLE_EDGE);
        old("birch_forest", Biomes.BIRCH_FOREST, Biomes.BIRCH_FOREST_HILLS);
        old("old_growth_birch_forest", Biomes.MUTATED_BIRCH_FOREST, Biomes.MUTATED_BIRCH_FOREST_HILLS);
        old("dark_forest", Biomes.ROOFED_FOREST, Biomes.MUTATED_ROOFED_FOREST);
        old("snowy_taiga", Biomes.COLD_TAIGA, Biomes.COLD_TAIGA_HILLS, Biomes.MUTATED_TAIGA_COLD);
        old("old_growth_pine_taiga", Biomes.REDWOOD_TAIGA, Biomes.REDWOOD_TAIGA_HILLS);
        old("old_growth_spruce_taiga", Biomes.MUTATED_REDWOOD_TAIGA, Biomes.MUTATED_REDWOOD_TAIGA_HILLS);
        old("savanna", Biomes.SAVANNA);
        old("savanna_plateau", Biomes.SAVANNA_PLATEAU);
        old("windswept_savanna", Biomes.MUTATED_SAVANNA, Biomes.MUTATED_SAVANNA_ROCK);
        old("badlands", Biomes.MESA, Biomes.MESA_CLEAR_ROCK, Biomes.MUTATED_MESA_CLEAR_ROCK);
        old("eroded_badlands", Biomes.MUTATED_MESA);
        old("wooded_badlands", Biomes.MESA_ROCK, Biomes.MUTATED_MESA_ROCK);
    }

    private FloraBiomes() {
    }

    private static void put(String biome, String... features) {
        FEATURES.put(biome, Collections.unmodifiableList(Arrays.asList(features)));
    }

    private static void old(String name, Biome... biomes) {
        for (Biome b : biomes) {
            OLD.put(b, name);
        }
    }

    /**
     * @return the biome's 1.21.5 name: 1.12.2's own biomes by the table above, any other mod's biome by its registry
     * path when 1.21.5 has a biome of that name (the family's pale garden, Caves &amp; Cliffs' meadow, Wild Update's
     * mangrove swamp), else null
     */
    @Nullable
    public static String modernName(@Nullable Biome biome) {
        if (biome == null) {
            return null;
        }
        String name = OLD.get(biome);
        if (name != null) {
            return name;
        }
        ResourceLocation id = biome.getRegistryName();
        if (id == null || "minecraft".equals(id.getNamespace())) {
            return null;
        }
        return FEATURES.containsKey(id.getPath()) ? id.getPath() : null;
    }

    /** @return this stream's features in a biome (1.21.5 name), in vanilla's order; empty for none */
    public static List<String> features(@Nullable String biome) {
        List<String> list = biome == null ? null : FEATURES.get(biome);
        return list == null ? Collections.<String>emptyList() : list;
    }

    /** Does the biome's tree selector give its trees leaf litter (the forest, the dark forest)? */
    static boolean hasLitterTrees(@Nullable Biome biome) {
        for (String f : features(modernName(biome))) {
            if (f.startsWith("[") && FallenTrees.dropsLitter(f.substring(1, f.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    /** The {@code biome} placement filter: does the biome at a position have this placed feature? */
    static boolean has(@Nullable Biome biome, String feature) {
        return features(modernName(biome)).contains(feature);
    }
}
