package com.exiledradio.trickytrialsbackport.animals;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 1.21.5's three farm animal variants ({@code cow_variant}, {@code pig_variant}, {@code chicken_variant}: temperate,
 * warm and cold, each picked by the same two biome tags), and the biome rule that picks one.
 *
 * <p>Vanilla's tags {@code #spawns_warm_variant_farm_animals} and {@code #spawns_cold_variant_farm_animals} name 1.21's
 * biomes. 1.12.2 calls many of them by older names, and the family's other mods add 1.13-1.19 biomes under their own
 * namespaces, so a biome is matched by its registry path in any namespace, under both its 1.12.2 and its 1.18 names.
 * Vanilla's {@code #is_nether} and {@code #is_end} cover every biome of those dimensions; here that is Forge's
 * {@code NETHER} and {@code END} biome dictionary types. Any other biome is temperate, as in vanilla.
 */
public enum FarmVariant {
    TEMPERATE("temperate"),
    WARM("warm"),
    COLD("cold");

    /** As vanilla saves it: {@code minecraft:temperate}. */
    public final ResourceLocation id;
    public final String name;

    FarmVariant(String name) {
        this.name = name;
        this.id = new ResourceLocation("minecraft", name);
    }

    private static final FarmVariant[] VALUES = values();

    public static FarmVariant byOrdinal(int i) {
        return i >= 0 && i < VALUES.length ? VALUES[i] : TEMPERATE;
    }

    /** @return the variant a saved id names ({@code minecraft:cold}, or {@code cold}), or null for anything else */
    @Nullable
    public static FarmVariant byId(String id) {
        ResourceLocation key = new ResourceLocation(id);
        for (FarmVariant v : VALUES) {
            if (v.id.equals(key)) {
                return v;
            }
        }
        return null;
    }

    /** 1.21.11's {@code #spawns_warm_variant_farm_animals}, by path, with 1.12.2's names for the same biomes. */
    static final Set<String> WARM_PATHS = new HashSet<String>(Arrays.asList(
            "desert", "warm_ocean", "mangrove_swamp", "deep_lukewarm_ocean", "lukewarm_ocean",
            // 1.12.2's deserts (desert_hills became desert in 1.18)
            "desert_hills", "mutated_desert", "desert_lakes",
            // #is_jungle
            "jungle", "sparse_jungle", "bamboo_jungle", "jungle_hills", "jungle_edge", "mutated_jungle",
            "mutated_jungle_edge", "bamboo_jungle_hills", "modified_jungle", "modified_jungle_edge",
            // #is_savanna
            "savanna", "savanna_plateau", "windswept_savanna", "savanna_rock", "mutated_savanna",
            "mutated_savanna_rock", "shattered_savanna", "shattered_savanna_plateau",
            // #is_badlands
            "badlands", "eroded_badlands", "wooded_badlands", "mesa", "mesa_rock", "mesa_clear_rock", "mutated_mesa",
            "mutated_mesa_rock", "mutated_mesa_clear_rock", "badlands_plateau", "wooded_badlands_plateau",
            "modified_badlands_plateau", "modified_wooded_badlands_plateau",
            // #is_nether by name, for a Nether biome missing the NETHER type
            "hell", "nether", "nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas"));

    /** 1.21.11's {@code #spawns_cold_variant_farm_animals}, by path, with 1.12.2's names for the same biomes. */
    static final Set<String> COLD_PATHS = new HashSet<String>(Arrays.asList(
            "snowy_plains", "ice_spikes", "frozen_peaks", "jagged_peaks", "snowy_slopes", "frozen_ocean",
            "deep_frozen_ocean", "grove", "deep_dark", "frozen_river", "snowy_taiga", "snowy_beach", "cold_ocean",
            "deep_cold_ocean", "old_growth_pine_taiga", "old_growth_spruce_taiga", "taiga", "windswept_forest",
            "windswept_gravelly_hills", "windswept_hills", "stony_peaks",
            // 1.12.2's names (1.18 merged the hills and mountains into the biomes above)
            "ice_flats", "ice_mountains", "mutated_ice_flats", "snowy_tundra", "snowy_mountains", "cold_beach",
            "taiga_cold", "taiga_cold_hills", "mutated_taiga_cold", "snowy_taiga_hills", "snowy_taiga_mountains",
            "redwood_taiga", "redwood_taiga_hills", "mutated_redwood_taiga", "mutated_redwood_taiga_hills",
            "giant_tree_taiga", "giant_tree_taiga_hills", "giant_spruce_taiga", "giant_spruce_taiga_hills",
            "taiga_hills", "mutated_taiga", "taiga_mountains",
            "extreme_hills", "smaller_extreme_hills", "extreme_hills_with_trees", "mutated_extreme_hills",
            "mutated_extreme_hills_with_trees", "mountains", "mountain_edge", "wooded_mountains", "gravelly_mountains",
            "modified_gravelly_mountains",
            // #is_end by name
            "sky", "the_end", "small_end_islands", "end_midlands", "end_highlands", "end_barrens"));

    /** @return the variant 1.21.5 spawns in this biome */
    public static FarmVariant forBiome(@Nullable Biome biome) {
        if (biome == null || biome.getRegistryName() == null) {
            return TEMPERATE;
        }
        String path = biome.getRegistryName().getPath();
        if (WARM_PATHS.contains(path)) {
            return WARM;
        }
        if (COLD_PATHS.contains(path)) {
            return COLD;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.NETHER)) {
            return WARM;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.END)) {
            return COLD;
        }
        return TEMPERATE;
    }
}
