package com.exiledradio.trickytrialsbackport.trial;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

/**
 * 1.21's {@code TrialSpawnerConfig}: one mob pool's numbers, normal or ominous. Read from vanilla's own
 * files ({@code trial_spawner/trial_chamber/<pool>/<mob>/normal.json} and {@code ominous.json}, see
 * {@link TrialData}) or from a spawner's NBT; every field is optional, with vanilla's defaults.
 */
public final class SpawnerConfig {

    public static final String CONSUMABLES = "minecraft:spawners/trial_chamber/consumables";
    public static final String KEY = "minecraft:spawners/trial_chamber/key";
    public static final String ITEMS_TO_DROP_WHEN_OMINOUS = "minecraft:spawners/trial_chamber/items_to_drop_when_ominous";

    public static final SpawnerConfig DEFAULT = new SpawnerConfig(new JsonObject(), null);

    /** The registry name this config came from (stored by name), or null for one written inline. */
    @Nullable
    final String id;
    final JsonObject json;

    final int spawnRange;
    final float totalMobs;
    final float simultaneousMobs;
    final float totalMobsAddedPerPlayer;
    final float simultaneousMobsAddedPerPlayer;
    final int ticksBetweenSpawn;
    final Weighted<SpawnData> spawnPotentials = new Weighted<SpawnData>();
    final Weighted<String> lootTablesToEject = new Weighted<String>();
    final String itemsToDropWhenOminous;

    SpawnerConfig(JsonObject json, @Nullable String id) {
        this.id = id;
        this.json = json;
        this.spawnRange = MathHelper.clamp(intOr(json, "spawn_range", 4), 1, 128);
        this.totalMobs = floatOr(json, "total_mobs", 6.0F);
        this.simultaneousMobs = floatOr(json, "simultaneous_mobs", 2.0F);
        this.totalMobsAddedPerPlayer = floatOr(json, "total_mobs_added_per_player", 2.0F);
        this.simultaneousMobsAddedPerPlayer = floatOr(json, "simultaneous_mobs_added_per_player", 1.0F);
        this.ticksBetweenSpawn = Math.max(0, intOr(json, "ticks_between_spawn", 40));
        if (json.has("spawn_potentials")) {
            for (JsonElement e : json.getAsJsonArray("spawn_potentials")) {
                JsonObject o = e.getAsJsonObject();
                this.spawnPotentials.add(SpawnData.fromJson(o.getAsJsonObject("data")), intOr(o, "weight", 1));
            }
        }
        if (json.has("loot_tables_to_eject")) {
            for (JsonElement e : json.getAsJsonArray("loot_tables_to_eject")) {
                JsonObject o = e.getAsJsonObject();
                this.lootTablesToEject.add(o.get("data").getAsString(), intOr(o, "weight", 1));
            }
        } else {
            this.lootTablesToEject.add(CONSUMABLES, 1).add(KEY, 1);
        }
        this.itemsToDropWhenOminous = json.has("items_to_drop_when_ominous")
                ? json.get("items_to_drop_when_ominous").getAsString() : ITEMS_TO_DROP_WHEN_OMINOUS;
    }

    int targetTotalMobs(int additionalPlayers) {
        return (int) Math.floor(this.totalMobs + this.totalMobsAddedPerPlayer * additionalPlayers);
    }

    int targetSimultaneousMobs(int additionalPlayers) {
        return (int) Math.floor(this.simultaneousMobs + this.simultaneousMobsAddedPerPlayer * additionalPlayers);
    }

    /** Between ominous item spawners, always 8 seconds in 1.21. */
    long ticksBetweenItemSpawners() {
        return 160L;
    }

    /** As 1.21's {@code withSpawning}: this config with its mobs replaced by one entity (a spawn egg's). */
    SpawnerConfig withSpawning(String entityId) {
        JsonObject copy = new com.google.gson.JsonParser().parse(this.json.toString()).getAsJsonObject();
        JsonObject entity = new JsonObject();
        entity.addProperty("id", entityId);
        JsonObject data = new JsonObject();
        data.add("entity", entity);
        JsonObject potential = new JsonObject();
        potential.add("data", data);
        potential.addProperty("weight", 1);
        com.google.gson.JsonArray list = new com.google.gson.JsonArray();
        list.add(potential);
        copy.add("spawn_potentials", list);
        return new SpawnerConfig(copy, null);
    }

    private static int intOr(JsonObject o, String key, int fallback) {
        return o.has(key) ? o.get(key).getAsInt() : fallback;
    }

    private static float floatOr(JsonObject o, String key, float fallback) {
        return o.has(key) ? Math.max(0.0F, o.get(key).getAsFloat()) : fallback;
    }

    /**
     * @return the registered config of that name (for example
     * {@code minecraft:trial_chamber/melee/zombie/normal}), or null when no file has it
     */
    @Nullable
    public static SpawnerConfig named(String id) {
        JsonElement json = TrialData.read("trial_spawner", id);
        return json == null || !json.isJsonObject() ? null : new SpawnerConfig(json.getAsJsonObject(), id);
    }
}
