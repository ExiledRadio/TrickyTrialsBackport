package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.MenuMusic;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.Splashes;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.lookfeel.LookAndFeel;
import com.exiledradio.trickytrialsbackport.lookfeel.SpawnerSounds;

import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Stream Lf: what 1.21.x hands Backport Core's presentation module (versions, panorama, menu music, spawner sounds), and
 * that Core's own 1.21.x pieces (Core 1.4.7: options, loading screen, splashes, wording, bubble pop) apply with this mod.
 * Core's self-tests check those pieces' rules.
 */
public final class LookfeelSelfTest implements SelfTest {

    private static final String ID = TrickyTrialsBackport.MOD_ID;

    @Override
    public String name() {
        return "lookfeel";
    }

    @Override
    public void run(Check check) throws Exception {
        contributions(check);
        panorama(check);
        splashes(check);
        menuMusic(check);
        options(check);
        sounds(check);
        vanillaText(check);
    }

    private static void contributions(Check check) {
        for (String v : new String[]{"1.21", "1.21.2", "1.21.6", "1.21.9", "1.21.11"}) {
            Contribution c = Presentation.contribution(v);
            check.ok(c != null && c.owners().contains(ID), "version " + v + " registered by us: " + (c == null ? null : c.owners()));
        }
        check.ok(Presentation.newest().isAtLeast(GameVersion.parse("1.21.11")), "newest is at least 1.21.11: " + Presentation.newest());
        check.eq(LoadingRevision.SPAWN_VIEW_1_21_9, Presentation.loadingRevision(), "loading revision at 1.21.11");
    }

    private static void panorama(Check check) throws Exception {
        Presentation.Panorama ours = null;
        for (Presentation.Panorama p : Presentation.panoramas()) {
            if (p.version.equals(GameVersion.parse("1.21.11"))) {
                ours = p;
            }
        }
        if (!check.ok(ours != null, "1.21.11's panorama registered")) {
            return;
        }
        check.eq(6, ours.faces.length, "panorama faces");
        int side = -1;
        for (ResourceLocation face : ours.faces) {
            int[] size = pngSize(face);
            check.ok(size != null && size[0] == size[1], face + " is a square PNG: " + Arrays.toString(size));
            if (size != null) {
                if (side < 0) {
                    side = size[0];
                }
                check.eq(side, size[0], face + " size matches the others");
            }
        }
        ResourceLocation overlay = new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png");
        check.ok(pngSize(overlay) != null, "panorama overlay in the jar");
        check.eq(overlay, Presentation.panoramaOverlay(), "the newest overlay is 1.21.11's");
        // Backport Core 1.4.7 owns the startup logo, the loading screen's tile and the air bubble sprites.
        int[] bubble = pngSize(new ResourceLocation("backportcore", "look/1.21.2/textures/gui/sprites/hud/air.png"));
        check.ok(bubble != null && bubble[0] == 9 && bubble[1] == 9, "Core's air bubble sprite is 9 by 9: " + Arrays.toString(bubble));
    }

    private static void splashes(Check check) {
        List<String> base = Arrays.asList("Minors welcome!", "Pumpa kungen!", "Hello!", "Now Java 6!");
        List<String> pool = Splashes.pool(base, Presentation.installed(), file -> read("/assets/" + file.getNamespace() + "/" + file.getPath()));
        for (String added : new String[]{"Pumpakungen!", "15 years of Mining and Crafting!", "Music by Aaron Cherof!", "Music by Kumi Tanioka!",
                "Music by Amos Roddy!", "Flint and Steel!", "Chicken Jockey!", "One does not simply walk to the Far Lands", "Music by Hyper Potions!"}) {
            check.ok(pool.contains(added), "splash added: " + added);
        }
        check.ok(!pool.contains("Minors welcome!"), "1.21.6 removed \"Minors welcome!\"");
        check.ok(!pool.contains("Pumpa kungen!"), "1.21.6 changed \"Pumpa kungen!\"");
        check.ok(pool.contains("Hello!"), "1.12.2's other splashes stay");
        // The splash files are Backport Core's since 1.4.7; this mod no longer has its own.
        check.ok(read("/assets/" + ID + "/texts/splashes_1.21.6_added.txt").isEmpty(), "no splash file of our own");
        check.eq(7, read("/assets/backportcore/look/1.21.6/texts/splashes_1.21.6_added.txt").size(), "1.21.6 adds 7 (Core)");
    }

    private static void menuMusic(Check check) {
        List<MenuMusic.Track> base = new ArrayList<MenuMusic.Track>();
        for (int i = 1; i <= 4; i++) {
            base.add(new MenuMusic.Track("minecraft:music/menu/menu" + i, 1));
        }
        // An earlier backport's 1.18 track, under any mod id: 1.21 removes it by path.
        Contribution older = Presentation.detached("1.18").addMenuMusic("somebackport:music/game/wending", 1);
        List<Contribution> installed = new ArrayList<Contribution>();
        installed.add(older);
        installed.addAll(Presentation.installed());
        List<String> pool = new ArrayList<String>();
        for (MenuMusic.Track t : MenuMusic.pool(base, installed)) {
            pool.add(t.soundName);
        }
        List<String> expected = new ArrayList<String>();
        for (int i = 1; i <= 4; i++) {
            expected.add("minecraft:music/menu/menu" + i);
        }
        for (String t : new String[]{"below_and_above", "broken_clocks", "fireflies", "lilypad", "os_piano"}) {
            expected.add(ID + ":music/game/" + t);
            check.ok(LookfeelSelfTest.class.getResource("/assets/" + ID + "/sounds/music/game/" + t + ".ogg") != null, t + ".ogg in the jar");
        }
        // Other installed family mods may add tracks of their own versions after 1.21.11; only ours and vanilla's here.
        List<String> mine = new ArrayList<String>();
        for (String s : pool) {
            if (s.startsWith("minecraft:") || s.startsWith(ID + ":") || s.startsWith("somebackport:")) {
                mine.add(s);
            }
        }
        check.eq(expected, mine, "menu pool at 1.21.11 (1.21 dropped 1.18's wending)");
        for (MenuMusic.Track t : MenuMusic.pool(base, installed)) {
            if (t.soundName.startsWith(ID + ":")) {
                check.ok(Math.abs(t.volume - 0.4F) < 1e-6, t.soundName + " at volume 0.4");
            }
        }
        check.ok(MenuMusic.changed(Presentation.installed()), "menu pool changed");
        check.eq(12, LookAndFeel.OLDER_MENU_TRACKS.length, "1.18, 1.19 and 1.20 menu tracks removed");
    }

    /** The options are Backport Core's since 1.4.7, under this mod's ids; with this mod installed they show. */
    private static void options(Check check) {
        for (String id : new String[]{"inactivity_fps_limit", "high_contrast_block_outline", "invert_mouse_x", "sprint_window",
                "save_chat_drafts"}) {
            com.exiledradio.backportcore.presentation.option.PresentationOption o = Presentation.option(ID + ":" + id);
            check.ok(o != null && o.isCoreOwned(), id + " is Core's option under our id");
            check.ok(o != null && o.isShown(), id + " shown at the newest version");
        }
        check.eq(LoadingRevision.SPAWN_VIEW_1_21_9, Presentation.loadingRevision(), "1.21.9's loading screen (Core's drawer)");
    }

    private static void sounds(Check check) {
        check.ok(Blocks.MOB_SPAWNER.getSoundType() == SpawnerSounds.SPAWNER, "the spawner has 1.21.2's sound type");
        for (String part : new String[]{"break", "fall", "hit", "place", "step"}) {
            SoundEvent e = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(ID, "block.spawner." + part));
            check.ok(e != null, "block.spawner." + part + " registered");
        }
        check.ok(ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("backportcore", "ui.hud.bubble_pop")) != null,
                "Core's ui.hud.bubble_pop registered");
        check.ok(ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(ID, "ui.hud.bubble_pop")) == null, "no bubble pop of our own");
        for (int i = 1; i <= 4; i++) {
            check.ok(LookfeelSelfTest.class.getResource("/assets/" + ID + "/sounds/block/spawner/break" + i + ".ogg") != null, "spawner break" + i);
        }
        String caves = String.join("\n", read("/assets/minecraft/sounds.json"));
        check.ok(caves.contains("\"replace\": false"), "ambient.cave is added to, not replaced");
        for (int i = 20; i <= 23; i++) {
            check.ok(caves.contains(ID + ":ambient/cave/cave" + i), "cave" + i + " in ambient.cave");
            check.ok(LookfeelSelfTest.class.getResource("/assets/" + ID + "/sounds/ambient/cave/cave" + i + ".ogg") != null, "cave" + i + ".ogg in the jar");
        }
    }

    private static void vanillaText(Check check) {
        check.ok(Presentation.vanillaTexts().contains(new ResourceLocation("backportcore", "look/1.21.11/vanilla_text")),
                "Core's 1.21.11 vanilla text applies with this mod installed");
        check.ok(!Presentation.vanillaTexts().contains(new ResourceLocation(ID, "vanilla_text")), "no vanilla text folder of our own");
        List<String> en = read("/assets/backportcore/look/1.21.11/vanilla_text/en_us.lang");
        check.ok(en.contains("soundCategory.hostile=Hostile Mobs"), "Hostile Mobs");
    }

    // ---- helpers ----

    private static List<String> read(String resource) {
        List<String> out = new ArrayList<String>();
        InputStream in = LookfeelSelfTest.class.getResourceAsStream(resource);
        if (in == null) {
            return Collections.emptyList();
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    out.add(line);
                }
            }
        } catch (java.io.IOException e) {
            return Collections.emptyList();
        }
        return out;
    }

    /** @return a PNG's width and height from its header, or null when it is missing or not a PNG */
    private static int[] pngSize(ResourceLocation texture) {
        InputStream in = LookfeelSelfTest.class.getResourceAsStream("/assets/" + texture.getNamespace() + "/" + texture.getPath());
        if (in == null) {
            return null;
        }
        try (DataInputStream d = new DataInputStream(in)) {
            byte[] sig = new byte[8];
            d.readFully(sig);
            if ((sig[1] & 0xFF) != 'P' || (sig[2] & 0xFF) != 'N' || (sig[3] & 0xFF) != 'G') {
                return null;
            }
            d.readInt();
            d.readInt();
            return new int[]{d.readInt(), d.readInt()};
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
