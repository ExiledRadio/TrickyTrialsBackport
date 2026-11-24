package com.exiledradio.trickytrialsbackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Stream Lf: the look and feel of 1.21 to 1.21.11 that stays with this mod. It declares the versions it brings (1.21,
 * 1.21.2, 1.21.6, 1.21.9, 1.21.11); Backport Core owns the rest of their look and feel since Core 1.4.7 (the options,
 * air bubbles, chat drafts, rarities, fog, loading screen, F3 line, sprites, splashes, startup logo and wording), so
 * they apply whichever backport is the newest installed one.
 *
 * <ul>
 *   <li>1.21: the menu music no longer has 1.18's, 1.19's or 1.20's tracks (vanilla had dropped them by 1.21; Core
 *       applies a version's removals only while that version is installed, so this version drops them too);</li>
 *   <li>1.21.2: the spawner's own sounds ({@link SpawnerSounds});</li>
 *   <li>1.21.6: its five menu tracks (the menu pool from 1.21.6 on, which also drops 1.21's nine);</li>
 *   <li>1.21.11: the Mounts of Mayhem panorama and its overlay.</li>
 * </ul>
 */
public final class LookAndFeel {

    private static final String ID = TrickyTrialsBackport.MOD_ID;

    /**
     * The menu tracks of the versions before 1.21, by vanilla path, whichever backport plays them (Core 1.2.0's removal
     * by path); vanilla 1.21 has none of them.
     */
    public static final String[] OLDER_MENU_TRACKS = {
            // 1.18 (1.19 removed them)
            "music/game/infinite_amethyst", "music/game/left_to_bloom", "music/game/one_more_day", "music/game/stand_tall",
            "music/game/wending",
            // 1.19 (1.20 removed them)
            "music/game/swamp/aerie", "music/game/swamp/firebugs", "music/game/swamp/labyrinthine",
            // 1.20 (1.21 removed them)
            "music/game/a_familiar_room", "music/game/bromeliad", "music/game/crescent_dunes", "music/game/echo_in_the_wind",
    };

    private static boolean registered;

    private LookAndFeel() {
    }

    /** Pre-init, both sides: hands Core 1.21.x's pieces. */
    public static void preInit() {
        if (registered) {
            return;
        }
        registered = true;
        // Sound events are made before the sound registry event, which registers every one Content made.
        SpawnerSounds.SPAWNER.getClass();

        Contribution v121 = Presentation.version("1.21");
        for (String track : OLDER_MENU_TRACKS) {
            v121.removeMenuMusic(track);
        }

        // The versions this mod brings (Backport Core owns their look and feel pieces since 1.4.7).
        Presentation.version("1.21.2");
        Contribution v1216 = Presentation.version("1.21.6");
        for (String track : lines("/assets/" + ID + "/texts/menu_music_1.21.6.txt")) {
            // music.menu's 1.21.6 entries: weight 1, volume 0.4.
            v1216.addMenuMusic(track, 1, 0.4F);
        }

        Presentation.version("1.21.9");

        Presentation.version("1.21.11")
                .panorama(ID + ":textures/gui/title/background/panorama_")
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"));
    }

    /** Init, both sides: the spawner's 1.21.2 sounds. */
    public static void init() {
        SpawnerSounds.apply();
    }

    /** @return the jar resource's non-blank lines, or none when it is missing */
    static List<String> lines(String resource) {
        List<String> out = new ArrayList<String>();
        InputStream in = LookAndFeel.class.getResourceAsStream(resource);
        if (in == null) {
            TrickyTrialsBackport.LOGGER.warn("Look and feel: {} is missing from the jar.", resource);
            return out;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    out.add(line.trim());
                }
            }
        } catch (java.io.IOException e) {
            TrickyTrialsBackport.LOGGER.warn("Look and feel: could not read {}.", resource, e);
        }
        return out;
    }
}
