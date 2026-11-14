package com.exiledradio.trickytrialsbackport.chambers;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
    /** {@code pool_aliases}, in file order. */
    public final List<Alias> aliases;
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
        List<Alias> aliases = new ArrayList<Alias>();
        if (structure.has("pool_aliases")) {
            for (JsonElement e : structure.getAsJsonArray("pool_aliases")) {
                aliases.add(Alias.read(e.getAsJsonObject()));
            }
        }
        this.aliases = Collections.unmodifiableList(aliases);

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

    /** A pool alias binding: {@code direct}, {@code random} or {@code random_group}. */
    public static final class Alias {
        final String type;
        /** direct and random: the alias. */
        final String alias;
        /** direct: the one target; random: the weighted targets. */
        final List<String> targets;
        /** random and random_group: the weights, in order. */
        final List<Integer> weights;
        /** random_group: each group's bindings. */
        final List<List<Alias>> groups;

        private Alias(String type, String alias, List<String> targets, List<Integer> weights, List<List<Alias>> groups) {
            this.type = type;
            this.alias = alias;
            this.targets = targets;
            this.weights = weights;
            this.groups = groups;
        }

        static Alias read(JsonObject o) {
            String type = o.get("type").getAsString();
            if (type.indexOf(':') < 0) {
                type = "minecraft:" + type;
            }
            List<String> targets = new ArrayList<String>();
            List<Integer> weights = new ArrayList<Integer>();
            List<List<Alias>> groups = new ArrayList<List<Alias>>();
            String alias = o.has("alias") ? o.get("alias").getAsString() : null;
            switch (type) {
                case "minecraft:direct":
                    targets.add(o.get("target").getAsString());
                    break;
                case "minecraft:random":
                    for (JsonElement e : o.getAsJsonArray("targets")) {
                        targets.add(e.getAsJsonObject().get("data").getAsString());
                        weights.add(e.getAsJsonObject().get("weight").getAsInt());
                    }
                    break;
                case "minecraft:random_group":
                    for (JsonElement e : o.getAsJsonArray("groups")) {
                        List<Alias> group = new ArrayList<Alias>();
                        for (JsonElement b : e.getAsJsonObject().getAsJsonArray("data")) {
                            group.add(read(b.getAsJsonObject()));
                        }
                        groups.add(group);
                        weights.add(e.getAsJsonObject().get("weight").getAsInt());
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown pool alias type " + type);
            }
            return new Alias(type, alias, targets, weights, groups);
        }

        /** {@code PoolAliasBinding.forEachResolved}. */
        void resolve(LegacyRandom random, java.util.Map<String, String> out) {
            switch (this.type) {
                case "minecraft:direct":
                    put(out, this.alias, this.targets.get(0));
                    break;
                case "minecraft:random":
                    put(out, this.alias, this.targets.get(pick(this.weights, random)));
                    break;
                default:
                    for (Alias a : this.groups.get(pick(this.weights, random))) {
                        a.resolve(random, out);
                    }
            }
        }

        /** {@code ImmutableMap.Builder.put} then {@code build}: a key given twice is an error in vanilla too. */
        private static void put(java.util.Map<String, String> out, String alias, String target) {
            if (out.put(full(alias), full(target)) != null) {
                throw new IllegalStateException("Pool alias " + alias + " is bound twice");
            }
        }

        /** {@code WeightedList.getRandom}: {@code nextInt(totalWeight)}, then the entry that range falls in. */
        private static int pick(List<Integer> weights, LegacyRandom random) {
            int total = 0;
            for (int w : weights) {
                total += w;
            }
            int r = random.nextInt(total);
            for (int i = 0; i < weights.size(); i++) {
                r -= weights.get(i);
                if (r < 0) {
                    return i;
                }
            }
            return weights.size() - 1;
        }
    }

    static String full(String name) {
        return name.indexOf(':') < 0 ? "minecraft:" + name : name;
    }

    /** {@code LegacyRandomSource}: java.util.Random's generator, as today's code draws from it. */
    static final class LegacyRandom extends java.util.Random {
        LegacyRandom(long seed) {
            super(seed);
        }

        /** {@code Mth.getSeed(x, y, z)}. */
        static long positionSeed(int x, int y, int z) {
            long l = (long) (x * 3129871) ^ (long) z * 116129781L ^ (long) y;
            l = l * l * 42317861L + l * 11L;
            return l >> 16;
        }
    }
}
