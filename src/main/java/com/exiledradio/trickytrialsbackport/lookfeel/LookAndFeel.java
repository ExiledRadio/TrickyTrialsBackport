package com.exiledradio.trickytrialsbackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.option.CycleOption;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.backportcore.presentation.option.SliderOption;
import com.exiledradio.backportcore.presentation.option.ToggleOption;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Stream Lf: the look and feel of 1.21 to 1.21.11, the changes those versions made beyond their content. Backport
 * Core owns every piece only one version can have at a time (the panorama, the splash list, the menu music, the
 * loading screen, the options screens) and shows the newest installed version's; this class hands Core 1.21.x's.
 *
 * <ul>
 *   <li>1.21: the menu music no longer has 1.18's, 1.19's or 1.20's tracks (vanilla had dropped them by 1.21; Core
 *       applies a version's removals only while that version is installed, so this version drops them too);</li>
 *   <li>1.21.2: Reduce FPS when (Video Settings), High Contrast Block Outlines (Accessibility Settings), the air
 *       bubbles' empty and bursting states, the spawner's own sounds, item rarities, and the F3 entity line
 *       without "B" (client side, {@code lookfeel.client});</li>
 *   <li>1.21.6: the splash changes and its five menu tracks (the menu pool from 1.21.6 on, which also drops
 *       1.21's nine), and fog that thickens in rain;</li>
 *   <li>1.21.9: Invert Mouse X (Mouse Settings), Sprint Window (Controls), Save Unsent Chats (Chat Settings),
 *       chat that stays open in a nether portal and through a dimension change, and the loading screen with
 *       the panorama behind it and a progress bar;</li>
 *   <li>1.21.11: the Mounts of Mayhem panorama and its overlay, two more splashes, nether fog from 10 to 96
 *       blocks whatever the render distance, the startup logo, and vanilla's own text as 1.21.11 words it
 *       ({@code vanilla_text/}: Hostile Mobs, Closed Captions, Walk Forward ...).</li>
 * </ul>
 */
public final class LookAndFeel {

    private static final String ID = TrickyTrialsBackport.MOD_ID;

    /** 1.21.2's "Reduce FPS when": AFK (30 after a minute without input, 10 after ten) or only when minimized. */
    public static final CycleOption INACTIVITY_FPS_LIMIT = new CycleOption(ID + ":inactivity_fps_limit",
            "options.inactivityFpsLimit", OptionScreen.VIDEO, new String[]{"minimized", "afk"},
            new String[]{"options.inactivityFpsLimit.minimized", "options.inactivityFpsLimit.afk"}, "afk");

    /** 1.21.2's High Contrast Block Outlines (Accessibility Settings). */
    public static final ToggleOption HIGH_CONTRAST_OUTLINE = new ToggleOption(ID + ":high_contrast_block_outline",
            "options.accessibility.high_contrast_block_outline", OptionScreen.ACCESSIBILITY, false);

    /** 1.21.9's Invert Mouse X (Mouse Settings); 1.12.2's Invert Mouse reads "Invert Mouse Y" beside it. */
    public static final ToggleOption INVERT_MOUSE_X = new ToggleOption(ID + ":invert_mouse_x",
            "options.invertMouseX", OptionScreen.MOUSE, false);

    /** 1.21.9's Sprint Window (Controls): the ticks in which a second tap of forward starts a sprint, 0 (off) to 10. */
    public static final SliderOption SPRINT_WINDOW = new SliderOption(ID + ":sprint_window",
            "options.sprintWindow", OptionScreen.CONTROLS, 0.0, 10.0, 1.0, 7.0);

    /** 1.21.9's Save Unsent Chats (Chat Settings): keep a message closed with Escape as a draft. */
    public static final ToggleOption SAVE_CHAT_DRAFTS = new ToggleOption(ID + ":save_chat_drafts",
            "options.chat.drafts", OptionScreen.CHAT, false);

    /** The menu tracks of the versions before 1.21, as the family's backports name them; vanilla 1.21 has none. */
    public static final String[] OLDER_MENU_TRACKS = {
            // 1.18 (1.19 removed them)
            "cavesandcliffsbackport:music/game/infinite_amethyst", "cavesandcliffsbackport:music/game/left_to_bloom",
            "cavesandcliffsbackport:music/game/one_more_day", "cavesandcliffsbackport:music/game/stand_tall",
            "cavesandcliffsbackport:music/game/wending",
            // 1.19 (1.20 removed them)
            "wildupdatebackport:music/game/swamp/aerie", "wildupdatebackport:music/game/swamp/firebugs",
            "wildupdatebackport:music/game/swamp/labyrinthine",
            // 1.20 (1.21 removed them)
            "trailsandtalesbackport:music/game/a_familiar_room", "trailsandtalesbackport:music/game/bromeliad",
            "trailsandtalesbackport:music/game/crescent_dunes", "trailsandtalesbackport:music/game/echo_in_the_wind",
    };

    /** 1.21.2's {@code ui.hud.bubble_pop}: an air bubble bursting while the player is under water. */
    public static SoundEvent BUBBLE_POP;

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
        BUBBLE_POP = Content.sound("ui.hud.bubble_pop");
        SpawnerSounds.SPAWNER.getClass();

        Contribution v121 = Presentation.version("1.21");
        for (String track : OLDER_MENU_TRACKS) {
            v121.removeMenuMusic(track);
        }

        // 1.21.2: Video Settings has Reduce FPS when after VSync; Accessibility Settings ends with the outline.
        INACTIVITY_FPS_LIMIT.tooltip("options.inactivityFpsLimit.afk.tooltip");
        HIGH_CONTRAST_OUTLINE.tooltip("options.accessibility.high_contrast_block_outline.tooltip");
        Presentation.version("1.21.2")
                .option(INACTIVITY_FPS_LIMIT.after("ENABLE_VSYNC"))
                .option(HIGH_CONTRAST_OUTLINE);

        Contribution v1216 = Presentation.version("1.21.6")
                .addSplashes(new ResourceLocation(ID, "texts/splashes_1.21.6_added.txt"))
                .removeSplashes(new ResourceLocation(ID, "texts/splashes_1.21.6_removed.txt"));
        for (String track : lines("/assets/" + ID + "/texts/menu_music_1.21.6.txt")) {
            v1216.addMenuMusic(track, 1);
        }

        // 1.21.9: Mouse Settings' Invert Mouse X sits before Invert Mouse Y (1.12.2's Invert Mouse).
        SPRINT_WINDOW.decimals(0).valueKey("options.value");
        SPRINT_WINDOW.zeroKey("options.off");
        SPRINT_WINDOW.tooltip("options.sprintWindow.tooltip");
        SAVE_CHAT_DRAFTS.tooltip("options.chat.drafts.tooltip");
        Presentation.version("1.21.9")
                .option(INVERT_MOUSE_X)
                .option(SPRINT_WINDOW)
                .option(SAVE_CHAT_DRAFTS);

        Presentation.version("1.21.11")
                .panorama(ID + ":textures/gui/title/background/panorama_")
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"))
                .addSplashes(new ResourceLocation(ID, "texts/splashes_1.21.11_added.txt"))
                .startupLogo("textures/gui/title/mojangstudios.png", new ResourceLocation(ID, "textures/gui/title/mojangstudios.png"))
                .vanillaText(new ResourceLocation(ID, "vanilla_text"));
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
