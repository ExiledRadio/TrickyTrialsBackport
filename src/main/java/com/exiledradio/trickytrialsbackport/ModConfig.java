package com.exiledradio.trickytrialsbackport;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Configuration: one category per stream, each with an {@code enabled} switch.
 *
 * <h2>Rules for streams</h2>
 * <ul>
 *   <li>A stream edits only inside its own category class, between its markers; the category list and
 *       the {@code enabled} fields are written by {@code tools/common/streams.py}.</li>
 *   <li>Category classes extend {@code Object} directly. Forge 1.12 crashes at startup ("Unknown type")
 *       on a category class that extends another one.</li>
 *   <li>{@code enabled} is read once, during registration, and needs a restart: switching a stream off
 *       means its blocks and items are never registered, so a world that has them loses them.</li>
 *   <li>A changed default never reaches an installed game (Forge keeps the value in the file), so a
 *       default that must change gets a new key.</li>
 *   <li>Every field has an explicit {@code @Config.LangKey}; its name and {@code .tooltip} lines are in
 *       en_us.lang (family Rule 4).</li>
 * </ul>
 */
@Config(modid = TrickyTrialsBackport.MOD_ID, name = TrickyTrialsBackport.MOD_ID)
public final class ModConfig {

    @Config.LangKey("config.trickytrialsbackport.tuff")
    @Config.Comment("1.21's tuff stairs, slabs and walls, polished tuff, tuff bricks and the chiseled forms.")
    public static final Tuff tuff = new Tuff();

    @Config.LangKey("config.trickytrialsbackport.copper")
    @Config.Comment("Copper bulbs, doors, trapdoors, grates and chiseled copper, with their oxidation and waxing.")
    public static final Copper copper = new Copper();

    @Config.LangKey("config.trickytrialsbackport.breeze")
    @Config.Comment("The breeze, breeze rods and wind charges.")
    public static final Breeze breeze = new Breeze();

    @Config.LangKey("config.trickytrialsbackport.bogged")
    @Config.Comment("The bogged, the skeleton of swamps.")
    public static final Bogged bogged = new Bogged();

    @Config.LangKey("config.trickytrialsbackport.mace")
    @Config.Comment("The mace, the heavy core, and Density, Breach and Wind Burst.")
    public static final Mace mace = new Mace();

    @Config.LangKey("config.trickytrialsbackport.crafter")
    @Config.Comment("The crafter.")
    public static final Crafter crafter = new Crafter();

    @Config.LangKey("config.trickytrialsbackport.trials")
    @Config.Comment("Trial spawners, vaults, trial keys, ominous trials and Trial Omen.")
    public static final Trials trials = new Trials();

    @Config.LangKey("config.trickytrialsbackport.effects")
    @Config.Comment("Weaving, Oozing, Infested and Wind Charged, and their potions and arrows.")
    public static final Effects effects = new Effects();

    @Config.LangKey("config.trickytrialsbackport.decor")
    @Config.Comment("New paintings, banner patterns, music discs, pottery sherds and armor trim templates.")
    public static final Decor decor = new Decor();

    @Config.LangKey("config.trickytrialsbackport.chambers")
    @Config.Comment("Trial chambers.")
    public static final Chambers chambers = new Chambers();

    @Config.LangKey("config.trickytrialsbackport.bundles")
    @Config.Comment("Bundles and the sixteen dyed bundles.")
    public static final Bundles bundles = new Bundles();

    @Config.LangKey("config.trickytrialsbackport.paleGarden")
    @Config.Comment("The pale garden: pale oak, pale moss, hanging moss and eyeblossoms.")
    public static final PaleGarden paleGarden = new PaleGarden();

    @Config.LangKey("config.trickytrialsbackport.creaking")
    @Config.Comment("The creaking and the creaking heart.")
    public static final Creaking creaking = new Creaking();

    @Config.LangKey("config.trickytrialsbackport.resin")
    @Config.Comment("Resin clumps, resin blocks and resin bricks.")
    public static final Resin resin = new Resin();

    @Config.LangKey("config.trickytrialsbackport.flora")
    @Config.Comment("Spring's plants: bushes, firefly bushes, cactus flowers, dry grass, leaf litter, wildflowers and fallen trees.")
    public static final Flora flora = new Flora();

    @Config.LangKey("config.trickytrialsbackport.animals")
    @Config.Comment("Warm and cold cows, pigs and chickens, blue and brown eggs, and wolf sound variants.")
    public static final Animals animals = new Animals();

    @Config.LangKey("config.trickytrialsbackport.happyGhast")
    @Config.Comment("The happy ghast, ghastlings, the dried ghast and harnesses.")
    public static final HappyGhast happyGhast = new HappyGhast();

    @Config.LangKey("config.trickytrialsbackport.locatorBar")
    @Config.Comment("The locator bar, which shows other players' directions.")
    public static final LocatorBar locatorBar = new LocatorBar();

    @Config.LangKey("config.trickytrialsbackport.copperGolem")
    @Config.Comment("The copper golem, copper golem statues and copper chests.")
    public static final CopperGolem copperGolem = new CopperGolem();

    @Config.LangKey("config.trickytrialsbackport.copperGear")
    @Config.Comment("Copper tools, armor and nuggets; copper torches, lanterns, bars and chains.")
    public static final CopperGear copperGear = new CopperGear();

    @Config.LangKey("config.trickytrialsbackport.shelves")
    @Config.Comment("Wooden shelves.")
    public static final Shelves shelves = new Shelves();

    @Config.LangKey("config.trickytrialsbackport.spears")
    @Config.Comment("Spears and Lunge.")
    public static final Spears spears = new Spears();

    @Config.LangKey("config.trickytrialsbackport.nautilus")
    @Config.Comment("The nautilus, the zombie nautilus, nautilus armor and Breath of the Nautilus.")
    public static final Nautilus nautilus = new Nautilus();

    @Config.LangKey("config.trickytrialsbackport.mounts")
    @Config.Comment("The camel husk, the parched, zombie horses in the wild and netherite horse armor.")
    public static final Mounts mounts = new Mounts();

    @Config.LangKey("config.trickytrialsbackport.tweaks")
    @Config.Comment("1.21's smaller changes to things 1.12.2 already has.")
    public static final Tweaks tweaks = new Tweaks();

    @Config.LangKey("config.trickytrialsbackport.babies")
    @Config.Comment("26.1's golden dandelion, baby mob changes and new animal sounds.")
    public static final Babies babies = new Babies();

    @Config.LangKey("config.trickytrialsbackport.sulfur")
    @Config.Comment("26.2's sulfur and cinnabar blocks, potent sulfur and sulfur spikes.")
    public static final Sulfur sulfur = new Sulfur();

    @Config.LangKey("config.trickytrialsbackport.sulfurCube")
    @Config.Comment("26.2's sulfur cube.")
    public static final SulfurCube sulfurCube = new SulfurCube();

    @Config.LangKey("config.trickytrialsbackport.sulfurCaves")
    @Config.Comment("26.2's sulfur caves and sulfur springs.")
    public static final SulfurCaves sulfurCaves = new SulfurCaves();

    @Config.LangKey("config.trickytrialsbackport.poplar")
    @Config.Comment("26.3's poplar wood, red shrubs, shelf mushrooms and the dappled forest.")
    public static final Poplar poplar = new Poplar();

    @Config.LangKey("config.trickytrialsbackport.softBlocks")
    @Config.Comment("26.3's wool and concrete stairs and slabs, cushions and straw beds.")
    public static final SoftBlocks softBlocks = new SoftBlocks();

    @Config.LangKey("config.trickytrialsbackport.camps")
    @Config.Comment("26.3's abandoned camps and explorer maps.")
    public static final Camps camps = new Camps();

    // ------------------------------------------------------------------------------------------
    // Categories. Each stream owns exactly one.
    // ------------------------------------------------------------------------------------------

    public static final class Tuff {
        @Config.RequiresMcRestart
        @Config.Comment("The tuff blocks. Off, nothing is registered, and a world that has them loses them.")
        @Config.LangKey("config.trickytrialsbackport.tuff.enabled")
        public boolean enabled = true;

        // --- Tuff fields ---
        @Config.RequiresMcRestart
        @Config.Comment({"Without another installed mod that adds tuff, tuff generates in stone below y 16 in newly",
                "generated chunks, as in 1.17."})
        @Config.LangKey("config.trickytrialsbackport.tuff.generate")
        public boolean generate = true;
        // --- end Tuff ---
    }

    public static final class Copper {
        @Config.RequiresMcRestart
        @Config.Comment("The copper blocks. Off, nothing is registered, and a world that has them loses them.")
        @Config.LangKey("config.trickytrialsbackport.copper.enabled")
        public boolean enabled = true;

        // --- Copper fields ---
        @Config.Comment("Unwaxed copper blocks slowly oxidize: exposed, weathered, then oxidized.")
        @Config.LangKey("config.trickytrialsbackport.copper.oxidation")
        public boolean oxidation = true;

        @Config.RequiresMcRestart
        @Config.Comment({"Items that wax copper when used on it or crafted with it (registry names). Any item with the",
                "ore name honeycomb works too."})
        @Config.LangKey("config.trickytrialsbackport.copper.waxItems")
        public String[] waxItems = {"buzzybeesbackport:honeycomb"};

        @Config.Comment({"Without another installed mod that adds copper, copper ore generates in newly generated",
                "chunks, as in 1.17."})
        @Config.LangKey("config.trickytrialsbackport.copper.generate")
        public boolean generate = true;

        @Config.Comment("Copper ore veins per chunk.")
        @Config.LangKey("config.trickytrialsbackport.copper.veinsPerChunk")
        @Config.RangeInt(min = 0, max = 64)
        public int veinsPerChunk = 6;

        @Config.Comment("Blocks of copper ore per vein, at most.")
        @Config.LangKey("config.trickytrialsbackport.copper.veinSize")
        @Config.RangeInt(min = 1, max = 64)
        public int veinSize = 10;

        @Config.Comment("Lowest height of a copper ore vein.")
        @Config.LangKey("config.trickytrialsbackport.copper.minY")
        @Config.RangeInt(min = 1, max = 255)
        public int minY = 1;

        @Config.Comment("Highest height of a copper ore vein.")
        @Config.LangKey("config.trickytrialsbackport.copper.maxY")
        @Config.RangeInt(min = 1, max = 255)
        public int maxY = 96;
        // --- end Copper ---
    }

    public static final class Breeze {
        @Config.RequiresMcRestart
        @Config.Comment("The breeze, the breeze rod and the wind charge. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.breeze.enabled")
        public boolean enabled = true;

        // --- Breeze fields ---
        // --- end Breeze ---
    }

    public static final class Bogged {
        @Config.RequiresMcRestart
        @Config.Comment("The bogged. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.bogged.enabled")
        public boolean enabled = true;

        // --- Bogged fields ---
        @Config.RequiresMcRestart
        @Config.Comment({"Bogged spawn in swamps and mangrove swamps, and take 30 of the 100 skeleton spawns there,",
                "as in 1.21."})
        @Config.LangKey("config.trickytrialsbackport.bogged.spawn")
        public boolean spawn = true;
        // --- end Bogged ---
    }

    public static final class Mace {
        @Config.RequiresMcRestart
        @Config.Comment("The mace, the heavy core and the mace's three enchantments. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.mace.enabled")
        public boolean enabled = true;

        // --- Mace fields ---
        // --- end Mace ---
    }

    public static final class Crafter {
        @Config.RequiresMcRestart
        @Config.Comment("The crafter. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.crafter.enabled")
        public boolean enabled = true;

        // --- Crafter fields ---
        // --- end Crafter ---
    }

    public static final class Trials {
        @Config.RequiresMcRestart
        @Config.Comment("Trial spawners, vaults, both trial keys and Trial Omen. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.trials.enabled")
        public boolean enabled = true;

        // --- Trials fields ---
        // --- end Trials ---
    }

    public static final class Effects {
        @Config.RequiresMcRestart
        @Config.Comment("The four effects and their potions. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.effects.enabled")
        public boolean enabled = true;

        // --- Effects fields ---
        // --- end Effects ---
    }

    public static final class Decor {
        @Config.RequiresMcRestart
        @Config.Comment("Paintings, banner patterns, music discs, sherds and trim templates. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.decor.enabled")
        public boolean enabled = true;

        // --- Decor fields ---
        // --- end Decor ---
    }

    public static final class Chambers {
        @Config.RequiresMcRestart
        @Config.Comment("Trial chambers generate underground in newly generated chunks.")
        @Config.LangKey("config.trickytrialsbackport.chambers.enabled")
        public boolean enabled = true;

        // --- Chambers fields ---
        // --- end Chambers ---
    }

    public static final class Bundles {
        @Config.RequiresMcRestart
        @Config.Comment("Bundles. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.bundles.enabled")
        public boolean enabled = true;

        // --- Bundles fields ---
        // --- end Bundles ---
    }

    public static final class PaleGarden {
        @Config.RequiresMcRestart
        @Config.Comment("The pale garden's blocks and items. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.paleGarden.enabled")
        public boolean enabled = true;

        // --- PaleGarden fields ---
        // --- end PaleGarden ---
    }

    public static final class Creaking {
        @Config.RequiresMcRestart
        @Config.Comment("The creaking and the creaking heart. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.creaking.enabled")
        public boolean enabled = true;

        // --- Creaking fields ---
        // --- end Creaking ---
    }

    public static final class Resin {
        @Config.RequiresMcRestart
        @Config.Comment("The resin blocks and items. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.resin.enabled")
        public boolean enabled = true;

        // --- Resin fields ---
        // --- end Resin ---
    }

    public static final class Flora {
        @Config.RequiresMcRestart
        @Config.Comment("The new plants. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.flora.enabled")
        public boolean enabled = true;

        // --- Flora fields ---
        // --- end Flora ---
    }

    public static final class Animals {
        @Config.RequiresMcRestart
        @Config.Comment("The farm animal variants and the two eggs. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.animals.enabled")
        public boolean enabled = true;

        // --- Animals fields ---
        // --- end Animals ---
    }

    public static final class HappyGhast {
        @Config.RequiresMcRestart
        @Config.Comment("The happy ghast, the dried ghast and harnesses. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.happyGhast.enabled")
        public boolean enabled = true;

        // --- HappyGhast fields ---
        // --- end HappyGhast ---
    }

    public static final class LocatorBar {
        @Config.Comment("Show the locator bar in place of the experience bar while other players are near.")
        @Config.LangKey("config.trickytrialsbackport.locatorBar.enabled")
        public boolean enabled = true;

        // --- LocatorBar fields ---
        // --- end LocatorBar ---
    }

    public static final class CopperGolem {
        @Config.RequiresMcRestart
        @Config.Comment("The copper golem, its statue and the copper chest. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.copperGolem.enabled")
        public boolean enabled = true;

        // --- CopperGolem fields ---
        // --- end CopperGolem ---
    }

    public static final class CopperGear {
        @Config.RequiresMcRestart
        @Config.Comment("Copper tools, armor and the other copper items. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.copperGear.enabled")
        public boolean enabled = true;

        // --- CopperGear fields ---
        // --- end CopperGear ---
    }

    public static final class Shelves {
        @Config.RequiresMcRestart
        @Config.Comment("Shelves. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.shelves.enabled")
        public boolean enabled = true;

        // --- Shelves fields ---
        // --- end Shelves ---
    }

    public static final class Spears {
        @Config.RequiresMcRestart
        @Config.Comment("The seven spears and Lunge. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.spears.enabled")
        public boolean enabled = true;

        // --- Spears fields ---
        // --- end Spears ---
    }

    public static final class Nautilus {
        @Config.RequiresMcRestart
        @Config.Comment("The nautilus, its armor and its effect. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.nautilus.enabled")
        public boolean enabled = true;

        // --- Nautilus fields ---
        // --- end Nautilus ---
    }

    public static final class Mounts {
        @Config.RequiresMcRestart
        @Config.Comment("The camel husk, the parched and netherite horse armor. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.mounts.enabled")
        public boolean enabled = true;

        // --- Mounts fields ---
        // --- end Mounts ---
    }

    public static final class Tweaks {
        @Config.Comment("1.21's smaller changes to existing blocks, items and mobs.")
        @Config.LangKey("config.trickytrialsbackport.tweaks.enabled")
        public boolean enabled = true;

        // --- Tweaks fields ---
        // --- end Tweaks ---
    }

    public static final class Babies {
        @Config.RequiresMcRestart
        @Config.Comment("The golden dandelion and the other 26.1 changes. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.babies.enabled")
        public boolean enabled = true;

        // --- Babies fields ---
        // --- end Babies ---
    }

    public static final class Sulfur {
        @Config.RequiresMcRestart
        @Config.Comment("The sulfur and cinnabar blocks. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.sulfur.enabled")
        public boolean enabled = true;

        // --- Sulfur fields ---
        // --- end Sulfur ---
    }

    public static final class SulfurCube {
        @Config.RequiresMcRestart
        @Config.Comment("The sulfur cube and its bucket. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.sulfurCube.enabled")
        public boolean enabled = true;

        // --- SulfurCube fields ---
        // --- end SulfurCube ---
    }

    public static final class SulfurCaves {
        @Config.RequiresMcRestart
        @Config.Comment("Sulfur caves and sulfur springs generate in newly generated chunks.")
        @Config.LangKey("config.trickytrialsbackport.sulfurCaves.enabled")
        public boolean enabled = true;

        // --- SulfurCaves fields ---
        // --- end SulfurCaves ---
    }

    public static final class Poplar {
        @Config.RequiresMcRestart
        @Config.Comment("The poplar blocks and the dappled forest's plants. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.poplar.enabled")
        public boolean enabled = true;

        // --- Poplar fields ---
        // --- end Poplar ---
    }

    public static final class SoftBlocks {
        @Config.RequiresMcRestart
        @Config.Comment("The wool and concrete shapes, cushions and straw beds. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.softBlocks.enabled")
        public boolean enabled = true;

        // --- SoftBlocks fields ---
        // --- end SoftBlocks ---
    }

    public static final class Camps {
        @Config.RequiresMcRestart
        @Config.Comment("Abandoned camps generate in newly generated chunks.")
        @Config.LangKey("config.trickytrialsbackport.camps.enabled")
        public boolean enabled = true;

        // --- Camps fields ---
        // --- end Camps ---
    }

    // ------------------------------------------------------------------------------------------

    /** Saves edits made in the in-game config screen. */
    @Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
    public static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (TrickyTrialsBackport.MOD_ID.equals(event.getModID())) {
                ConfigManager.sync(TrickyTrialsBackport.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }

    private ModConfig() {
    }
}
