package com.exiledradio.trickytrialsbackport.trial;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Vanilla's data files for the trials, as 1.21.11 ships them: {@code trial_spawner/} (spawner configs, one
 * normal and one ominous per mob pool) and {@code loot_table/} (what spawners eject, what vaults give, the
 * ominous mobs' equipment).
 *
 * <h2>Where a file is read from</h2>
 * For {@code <namespace>:<path>} in folder {@code <kind>}, the first of:
 * <ol>
 *   <li>{@code config/trickytrialsbackport/trials/<namespace>/<kind>/<path>.json} - a pack's own file, which
 *       replaces ours or adds a new one (a new mob pool for trial chambers, say);</li>
 *   <li>this jar's {@code assets/trickytrialsbackport/trials/<namespace>/<kind>/<path>.json}, vanilla's
 *       own files copied unchanged by {@code tools/trial/generate.py}.</li>
 * </ol>
 * Files are read once and kept; a missing one is remembered as missing.
 */
public final class TrialData {

    private static final Map<String, JsonElement> CACHE = new HashMap<String, JsonElement>();
    private static final JsonElement MISSING = new com.google.gson.JsonPrimitive("missing");

    private TrialData() {
    }

    /** @return the parsed file, or null when neither place has it */
    @Nullable
    public static synchronized JsonElement read(String kind, String id) {
        ResourceLocation rl = new ResourceLocation(id);
        String key = kind + "|" + rl;
        JsonElement cached = CACHE.get(key);
        if (cached == null) {
            cached = load(kind, rl);
            CACHE.put(key, cached == null ? MISSING : cached);
        }
        return cached == MISSING ? null : cached;
    }

    @Nullable
    private static JsonElement load(String kind, ResourceLocation rl) {
        String rel = "trials/" + rl.getNamespace() + "/" + kind + "/" + rl.getPath() + ".json";
        try {
            File dir = Loader.instance().getConfigDir();
            if (dir != null) {
                File file = new File(new File(dir, TrickyTrialsBackport.MOD_ID), rel);
                if (file.isFile()) {
                    return parse(new FileInputStream(file));
                }
            }
            InputStream in = TrialData.class.getResourceAsStream("/assets/" + TrickyTrialsBackport.MOD_ID + "/" + rel);
            return in == null ? null : parse(in);
        } catch (Exception e) {
            TrickyTrialsBackport.LOGGER.error("Could not read trial data {} {}", kind, rl, e);
            return null;
        }
    }

    private static JsonElement parse(InputStream in) throws Exception {
        try {
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
        } finally {
            in.close();
        }
    }
}
