package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.MenuMusic;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.Splashes;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.lookfeel.LookAndFeel;
import com.exiledradio.trickytrialsbackport.lookfeel.Rarities;
import com.exiledradio.trickytrialsbackport.lookfeel.Rules;
import com.exiledradio.trickytrialsbackport.lookfeel.SpawnerSounds;

import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

/** Stream Lf: what 1.21.x hands Backport Core's presentation module, and the rules the client follows. */
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
        framerate(check);
        bubbles(check);
        fog(check);
        chat(check);
        loading(check);
        rarities(check);
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
        for (String path : new String[]{"textures/gui/title/mojangstudios.png", "textures/gui/menu_background.png",
                "textures/gui/sprites/hud/air.png", "textures/gui/sprites/hud/air_bursting.png", "textures/gui/sprites/hud/air_empty.png"}) {
            check.ok(pngSize(new ResourceLocation(ID, path)) != null, path + " in the jar");
        }
        int[] bubble = pngSize(new ResourceLocation(ID, "textures/gui/sprites/hud/air.png"));
        check.ok(bubble != null && bubble[0] == 9 && bubble[1] == 9, "air bubble sprite is 9 by 9: " + Arrays.toString(bubble));
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
        check.eq(7, read("/assets/" + ID + "/texts/splashes_1.21.6_added.txt").size(), "1.21.6 adds 7");
        check.eq(3, read("/assets/" + ID + "/texts/splashes_1.21.6_removed.txt").size(), "1.21.6 removes 3");
        check.eq(2, read("/assets/" + ID + "/texts/splashes_1.21.11_added.txt").size(), "1.21.11 adds 2");
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
        check.eq("options.inactivityFpsLimit.afk.tooltip", LookAndFeel.INACTIVITY_FPS_LIMIT.tooltipKey(), "AFK's own tooltip");
        check.ok(MenuMusic.changed(Presentation.installed()), "menu pool changed");
        check.eq(12, LookAndFeel.OLDER_MENU_TRACKS.length, "1.18, 1.19 and 1.20 menu tracks removed");
    }

    private static void options(Check check) {
        check.eq("afk", LookAndFeel.INACTIVITY_FPS_LIMIT.get(), "Reduce FPS when defaults to AFK");
        check.eq(false, LookAndFeel.HIGH_CONTRAST_OUTLINE.get(), "High Contrast Block Outlines off");
        check.eq(false, LookAndFeel.INVERT_MOUSE_X.get(), "Invert Mouse X off");
        check.eq(7.0, LookAndFeel.SPRINT_WINDOW.get(), "Sprint Window 7");
        check.eq(false, LookAndFeel.SAVE_CHAT_DRAFTS.get(), "Save Unsent Chats off (1.21.11's default)");
        check.eq(OptionScreen.VIDEO, LookAndFeel.INACTIVITY_FPS_LIMIT.screen(), "Reduce FPS when on Video Settings");
        check.eq("ENABLE_VSYNC", LookAndFeel.INACTIVITY_FPS_LIMIT.after(), "Reduce FPS when after VSync");
        check.eq(OptionScreen.ACCESSIBILITY, LookAndFeel.HIGH_CONTRAST_OUTLINE.screen(), "outline option on Accessibility");
        check.eq(OptionScreen.MOUSE, LookAndFeel.INVERT_MOUSE_X.screen(), "Invert Mouse X on Mouse Settings");
        check.eq(OptionScreen.CONTROLS, LookAndFeel.SPRINT_WINDOW.screen(), "Sprint Window on Controls");
        check.eq(OptionScreen.CHAT, LookAndFeel.SAVE_CHAT_DRAFTS.screen(), "Save Unsent Chats on Chat Settings");
        for (com.exiledradio.backportcore.presentation.option.PresentationOption o : Arrays.asList(LookAndFeel.INACTIVITY_FPS_LIMIT,
                LookAndFeel.HIGH_CONTRAST_OUTLINE, LookAndFeel.INVERT_MOUSE_X, LookAndFeel.SPRINT_WINDOW, LookAndFeel.SAVE_CHAT_DRAFTS)) {
            check.ok(Presentation.option(o.id()) == o, o.id() + " registered with Core");
            check.ok(o.isShown(), o.id() + " shown at the newest version");
        }
        check.eq("OFF", zero(), "Sprint Window reads OFF at 0");
    }

    private static String zero() {
        double before = LookAndFeel.SPRINT_WINDOW.get();
        LookAndFeel.SPRINT_WINDOW.set(0.0);
        boolean isZero = LookAndFeel.SPRINT_WINDOW.isZero();
        LookAndFeel.SPRINT_WINDOW.set(before);
        return isZero && "options.off".equals(LookAndFeel.SPRINT_WINDOW.zeroKey()) ? "OFF" : "not off";
    }

    private static void sounds(Check check) {
        check.ok(Blocks.MOB_SPAWNER.getSoundType() == SpawnerSounds.SPAWNER, "the spawner has 1.21.2's sound type");
        for (String part : new String[]{"break", "fall", "hit", "place", "step"}) {
            SoundEvent e = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(ID, "block.spawner." + part));
            check.ok(e != null, "block.spawner." + part + " registered");
        }
        check.eq(ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(ID, "ui.hud.bubble_pop")), LookAndFeel.BUBBLE_POP,
                "ui.hud.bubble_pop registered");
        check.ok(LookfeelSelfTest.class.getResource("/assets/" + ID + "/sounds/ui/hud/hud_bubble.ogg") != null, "hud_bubble.ogg in the jar");
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
        check.ok(Presentation.vanillaTexts().contains(new ResourceLocation(ID, "vanilla_text")), "vanilla text folder registered");
        List<String> en = read("/assets/" + ID + "/vanilla_text/en_us.lang");
        check.ok(en.contains("soundCategory.hostile=Hostile Mobs"), "Hostile Mobs");
        check.ok(en.contains("options.showSubtitles=Closed Captions"), "Closed Captions");
        check.ok(en.contains("options.invertMouse=Invert Mouse Y"), "Invert Mouse Y");
        check.ok(en.contains("key.forward=Walk Forward"), "Walk Forward");
        check.eq(en.size(), read("/assets/" + ID + "/vanilla_text/de_de.lang").size(), "German vanilla text has every key");
        List<String> lang = read("/assets/" + ID + "/lang/en_us.lang");
        for (String key : new String[]{"options.inactivityFpsLimit", "options.inactivityFpsLimit.afk", "options.inactivityFpsLimit.minimized",
                "options.accessibility.high_contrast_block_outline", "options.invertMouseX", "options.sprintWindow", "options.chat.drafts"}) {
            boolean found = false;
            for (String l : lang) {
                found |= l.startsWith(key + "=");
            }
            check.ok(found, key + " in en_us.lang");
        }
    }

    private static void framerate(Check check) {
        check.eq(10, Rules.framerateLimit(false, 0, true, 120), "minimized: 10");
        check.eq(10, Rules.framerateLimit(true, 0, true, 120), "minimized with AFK: 10");
        check.eq(0, Rules.framerateLimit(true, 59999, false, 120), "under a minute: no throttle");
        check.eq(30, Rules.framerateLimit(true, 60001, false, 120), "a minute idle: 30");
        check.eq(20, Rules.framerateLimit(true, 60001, false, 20), "a minute idle under a 20 limit: 20");
        check.eq(10, Rules.framerateLimit(true, 600001, false, 120), "ten minutes idle: 10");
        check.eq(0, Rules.framerateLimit(false, 600001, false, 120), "Minimized option ignores idling");
    }

    private static void bubbles(Check check) {
        int full = 0;
        for (int i = 1; i <= 10; i++) {
            full += Rules.bubble(i, 300, 300, true) == Rules.Bubble.FULL ? 1 : 0;
        }
        check.eq(10, full, "full air: ten full bubbles");
        check.eq(Rules.Bubble.EMPTY, Rules.bubble(10, 0, 300, true), "no air: the last slot is an empty bubble");
        check.eq(Rules.Bubble.EMPTY, Rules.bubble(1, 0, 300, true), "no air: the first slot is an empty bubble");
        // 1.21.11's numbers at air 28 (just past a bubble's edge): ceil(26 * 10 / 300) = 1 full, ceil(28 / 30) = 1.
        check.eq(Rules.Bubble.FULL, Rules.bubble(1, 28, 300, true), "air 28: one full");
        // At air 31 the second bubble is bursting: full = ceil(29/30) = 1, current = ceil(31/30) = 2.
        check.eq(Rules.Bubble.BURSTING, Rules.bubble(2, 31, 300, true), "air 31: the second bursting");
        check.eq(Rules.Bubble.NONE, Rules.bubble(2, 31, 300, false), "out of water nothing bursts");
        check.eq(0.5F, Rules.bubblePopVolume(1), "pop volume with one empty");
        check.ok(Math.abs(Rules.bubblePopVolume(5) - 0.8F) < 0.0001F, "pop volume with five empty: " + Rules.bubblePopVolume(5));
        check.eq(1.0F, Rules.bubblePopPitch(4), "pop pitch with four empty");
        check.ok(Math.abs(Rules.bubblePopPitch(6) - 1.2F) < 0.0001F, "pop pitch with six empty: " + Rules.bubblePopPitch(6));
    }

    private static void fog(Check check) {
        check.eq(1.0F, Rules.rainFogTarget(1.0F, 15, true), "full rain under the sky");
        check.eq(0.5F, Rules.rainFogTarget(1.0F, 15, false), "a biome without rain halves it");
        check.eq(0.0F, Rules.rainFogTarget(1.0F, 8, true), "no rain fog at sky light 8");
        check.eq(-16.0F, Rules.rainFogStart(144.0F, 1.0F), "rain fog start at 12 chunks");
        check.eq(96.0F, Rules.rainFogEnd(192.0F, 1.0F), "rain fog end never nearer than 96");
        check.eq(64.0F, Rules.rainFogEnd(64.0F, 1.0F), "rain fog end keeps a nearer render distance");
        check.eq(160.0F, Rules.rainFogEnd(192.0F, 0.125F), "light rain pulls the end in a little");
        float[] glow = {1.0F, 0.8F, 0.6F, 0.5F};
        Rules.weatherSunrise(glow, 1.0F, 0.0F);
        check.ok(Math.abs(glow[0] - 0.5F) < 1e-6 && Math.abs(glow[2] - 0.36F) < 1e-6 && glow[3] == 0.5F, "full rain dims the sunrise: " + Arrays.toString(glow));
        float[] storm = {1.0F, 1.0F, 1.0F, 1.0F};
        Rules.weatherSunrise(storm, 0.0F, 1.0F);
        check.ok(Math.abs(storm[0] - 0.25F) < 1e-6 && Math.abs(storm[2] - 0.3F) < 1e-6, "thunder dims it more: " + Arrays.toString(storm));
        check.eq(10.0F, Rules.NETHER_FOG_START, "nether fog start");
        check.eq(96.0F, Rules.NETHER_FOG_END, "nether fog end");
    }

    private static void chat(Check check) {
        check.ok(Rules.keepsDraft(Rules.ChatExit.INTERRUPTED, false, "hi"), "interrupted chat keeps its draft");
        check.ok(!Rules.keepsDraft(Rules.ChatExit.INTENTIONAL, false, "hi"), "Escape drops it with the option off");
        check.ok(Rules.keepsDraft(Rules.ChatExit.INTENTIONAL, true, "hi"), "Escape keeps it with the option on");
        check.ok(!Rules.keepsDraft(Rules.ChatExit.DONE, true, "hi"), "sending drops it");
        check.ok(!Rules.keepsDraft(Rules.ChatExit.INTERRUPTED, true, "  "), "a blank chat is no draft");
        check.ok(Rules.restoresDraft("", "/gamemode"), "plain chat restores a command draft");
        check.ok(Rules.restoresDraft("/", "/gamemode"), "the command key restores a command draft");
        check.ok(!Rules.restoresDraft("/", "hello"), "the command key leaves a message draft");
        check.ok(!Rules.restoresDraft("hello", "draft"), "chat opened with a mod's text is left as it is");
    }

    private static void loading(Check check) {
        check.eq(16, Rules.LOADING_VIEW_RADIUS, "1.21.9's view radius");
        check.eq(240 / 2 - 32 - 27, Rules.loadingTextY(240, true), "text above the grid");
        check.eq(240 / 2 - 50, Rules.loadingTextY(240, false), "text before the grid");
    }

    private static void rarities(Check check) {
        check.eq(EnumRarity.COMMON, Rarities.of(new ItemStack(Items.GOLDEN_APPLE)), "golden apple");
        check.eq(EnumRarity.RARE, Rarities.of(new ItemStack(Items.GOLDEN_APPLE, 1, 1)), "enchanted golden apple");
        check.eq(EnumRarity.UNCOMMON, Rarities.of(new ItemStack(Items.RECORD_13)), "disc 13");
        check.eq(EnumRarity.EPIC, Rarities.of(new ItemStack(Items.SKULL, 1, 5)), "dragon head");
        check.eq(EnumRarity.RARE, Rarities.of(new ItemStack(Items.SKULL, 1, 1)), "wither skeleton skull");
        check.eq(EnumRarity.UNCOMMON, Rarities.of(new ItemStack(Items.SKULL, 1, 3)), "player head");
        check.eq(EnumRarity.EPIC, Rarities.of(new ItemStack(Items.ELYTRA)), "elytra");
        check.eq(EnumRarity.RARE, Rarities.of(new ItemStack(Items.NETHER_STAR)), "nether star");
        check.eq(EnumRarity.RARE, Rarities.of(new ItemStack(Items.ENCHANTED_BOOK)), "enchanted book");
        check.eq(EnumRarity.UNCOMMON, Rarities.of(new ItemStack(Items.CHAINMAIL_HELMET)), "chainmail helmet");
        check.eq(EnumRarity.RARE, Rarities.of(new ItemStack(Blocks.BEACON)), "beacon");
        check.eq(EnumRarity.EPIC, Rarities.of(new ItemStack(Blocks.COMMAND_BLOCK)), "command block");
        check.eq(EnumRarity.COMMON, Rarities.of(new ItemStack(Blocks.STONE)), "stone");
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 1);
        check.eq(EnumRarity.RARE, Rarities.of(sword), "enchanted sword");
        ItemStack elytra = new ItemStack(Items.ELYTRA);
        elytra.addEnchantment(Enchantments.UNBREAKING, 1);
        check.eq(EnumRarity.EPIC, Rarities.of(elytra), "enchanted elytra");
        ItemStack chain = new ItemStack(Items.CHAINMAIL_BOOTS);
        chain.addEnchantment(Enchantments.PROTECTION, 1);
        check.eq(EnumRarity.RARE, Rarities.of(chain), "enchanted chainmail");
        Item ours = null;
        for (Item item : ForgeRegistries.ITEMS) {
            if (ID.equals(item.getRegistryName().getNamespace())) {
                ours = item;
                break;
            }
        }
        if (ours != null) {
            check.eq(null, Rarities.of(new ItemStack(ours)), "another mod's item keeps its own rarity");
        }
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
