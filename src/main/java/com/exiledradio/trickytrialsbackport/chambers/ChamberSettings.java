package com.exiledradio.trickytrialsbackport.chambers;

import com.exiledradio.backportcore.structure.ModernJigsaw;
import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 1.21.11's {@code worldgen/structure/trial_chambers.json} and {@code worldgen/structure_set/trial_chambers.json}, read
 * from this jar's copies (written by {@code tools/chambers/generate.py}), so every number is vanilla's file.
 */
public final class ChamberSettings {

    static final String ROOT = "/assets/" + TrickyTrialsBackport.MOD_ID + "/";

    /** The structure's settings. */
    public final ResourceLocation startPool;
    public final int maxDepth;
    public final int maxDistanceHorizontal;
    public final int maxDistanceVertical;
    public final int startMinY;
    public final int startMaxY;
    public final int paddingBottom;
    public final int paddingTop;
    /** {@code pool_aliases}, in file order (Backport Core resolves them). */
    public final ModernJigsaw.PoolAliases aliases;
    /** The structure set's grid. */
    public final ScatteredPlacement grid;
    /** {@code #minecraft:has_structure/trial_chambers}: today's biome names. */
    public final Set<String> biomes;

    private static ChamberSettings instance;

    private ChamberSettings(JsonObject structure, JsonObject set, JsonObject tag) {
        this.startPool = new ResourceLocation(structure.get("start_pool").getAsString());
        this.maxDepth = structure.get("size").getAsInt();
        JsonElement distance = structure.get("max_distance_from_center");
        if (distance.isJsonObject()) {
            this.maxDistanceHorizontal = distance.getAsJsonObject().get("horizontal").getAsInt();
            // DimensionType.Y_SIZE when the file leaves it out
            this.maxDistanceVertical = distance.getAsJsonObject().has("vertical") ? distance.getAsJsonObject().get("vertical").getAsInt() : 4064;
        } else {
            this.maxDistanceHorizontal = distance.getAsInt();
            this.maxDistanceVertical = distance.getAsInt();
        }
        JsonObject height = structure.getAsJsonObject("start_height");
        if (!"minecraft:uniform".equals(height.get("type").getAsString())) {
            throw new IllegalStateException("trial_chambers start_height is not uniform: " + height);
        }
        this.startMinY = height.getAsJsonObject("min_inclusive").get("absolute").getAsInt();
        this.startMaxY = height.getAsJsonObject("max_inclusive").get("absolute").getAsInt();
        JsonElement padding = structure.get("dimension_padding");
        if (padding == null) {
            this.paddingBottom = 0;
            this.paddingTop = 0;
        } else if (padding.isJsonObject()) {
            this.paddingBottom = padding.getAsJsonObject().has("bottom") ? padding.getAsJsonObject().get("bottom").getAsInt() : 0;
            this.paddingTop = padding.getAsJsonObject().has("top") ? padding.getAsJsonObject().get("top").getAsInt() : 0;
        } else {
            this.paddingBottom = padding.getAsInt();
            this.paddingTop = padding.getAsInt();
        }
        this.aliases = ModernJigsaw.PoolAliases.read(structure.has("pool_aliases") ? structure.getAsJsonArray("pool_aliases") : null);

        JsonObject placement = set.getAsJsonObject("placement");
        if (!"minecraft:random_spread".equals(placement.get("type").getAsString())) {
            throw new IllegalStateException("trial_chambers placement is not random_spread: " + placement);
        }
        boolean triangular = placement.has("spread_type") && "triangular".equals(placement.get("spread_type").getAsString());
        this.grid = new ScatteredPlacement(placement.get("spacing").getAsInt(), placement.get("separation").getAsInt(),
                placement.get("salt").getAsInt(), triangular);

        Set<String> biomes = new HashSet<String>();
        for (JsonElement e : tag.getAsJsonArray("values")) {
            biomes.add(e.getAsString());
        }
        this.biomes = Collections.unmodifiableSet(biomes);
    }

    public static synchronized ChamberSettings get() {
        if (instance == null) {
            instance = new ChamberSettings(read("worldgen/structure/trial_chambers.json"), read("worldgen/structure_set/trial_chambers.json"),
                    read("tags/worldgen/biome/has_structure/trial_chambers.json"));
        }
        return instance;
    }

    static JsonObject read(String path) {
        try (InputStream in = ChamberSettings.class.getResourceAsStream(ROOT + path)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + ROOT + path);
            }
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read " + ROOT + path, e);
        }
    }

    static JsonArray readArray(String path) {
        try (InputStream in = ChamberSettings.class.getResourceAsStream(ROOT + path)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + ROOT + path);
            }
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read " + ROOT + path, e);
        }
    }

    static String full(String name) {
        return name.indexOf(':') < 0 ? "minecraft:" + name : name;
    }
}
