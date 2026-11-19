package com.exiledradio.trickytrialsbackport;

import com.exiledradio.backportcore.settings.Setting;
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

    @Setting.Feature(icon = "trickytrialsbackport:tuff_bricks")
    @Config.LangKey("config.trickytrialsbackport.tuff")
    @Config.Comment("1.21's tuff stairs, slabs and walls, polished tuff, tuff bricks and the chiseled forms.")
    public static final Tuff tuff = new Tuff();

    @Setting.Feature(icon = "trickytrialsbackport:copper_bulb")
    @Config.LangKey("config.trickytrialsbackport.copper")
    @Config.Comment("Copper bulbs, doors, trapdoors, grates and chiseled copper, with their oxidation and waxing.")
    public static final Copper copper = new Copper();

    @Setting.Feature(icon = "trickytrialsbackport:breeze_rod")
    @Config.LangKey("config.trickytrialsbackport.breeze")
    @Config.Comment("The breeze, breeze rods and wind charges.")
    public static final Breeze breeze = new Breeze();

    @Setting.Feature(icon = "minecraft:brown_mushroom")
    @Config.LangKey("config.trickytrialsbackport.bogged")
    @Config.Comment("The bogged, the skeleton of swamps.")
    public static final Bogged bogged = new Bogged();

    @Setting.Feature(icon = "trickytrialsbackport:mace")
    @Config.LangKey("config.trickytrialsbackport.mace")
    @Config.Comment("The mace, the heavy core, and Density, Breach and Wind Burst.")
    public static final Mace mace = new Mace();

    @Setting.Feature(icon = "trickytrialsbackport:crafter")
    @Config.LangKey("config.trickytrialsbackport.crafter")
    @Config.Comment("The crafter.")
    public static final Crafter crafter = new Crafter();

    @Setting.Feature(icon = "trickytrialsbackport:trial_key")
    @Config.LangKey("config.trickytrialsbackport.trials")
    @Config.Comment("Trial spawners, vaults, trial keys, ominous trials and Trial Omen.")
    public static final Trials trials = new Trials();

    @Setting.Feature(icon = "trickytrialsbackport:ominous_bottle")
    @Config.LangKey("config.trickytrialsbackport.effects")
    @Config.Comment("Weaving, Oozing, Infested and Wind Charged, and their potions and arrows.")
    public static final Effects effects = new Effects();

    @Setting.Feature(icon = "minecraft:painting")
    @Config.LangKey("config.trickytrialsbackport.decor")
    @Config.Comment("New paintings, banner patterns, music discs, pottery sherds and armor trim templates.")
    public static final Decor decor = new Decor();

    @Setting.Feature(icon = "trickytrialsbackport:chiseled_tuff_bricks")
    @Config.LangKey("config.trickytrialsbackport.chambers")
    @Config.Comment("Trial chambers.")
    public static final Chambers chambers = new Chambers();

    @Setting.Feature(icon = "trickytrialsbackport:bundle")
    @Config.LangKey("config.trickytrialsbackport.bundles")
    @Config.Comment("Bundles and the sixteen dyed bundles.")
    public static final Bundles bundles = new Bundles();

    @Setting.Feature(icon = "trickytrialsbackport:pale_oak_sapling")
    @Config.LangKey("config.trickytrialsbackport.paleGarden")
    @Config.Comment("The pale garden: pale oak, pale moss, hanging moss, eyeblossoms, the creaking heart and the biome.")
    public static final PaleGarden paleGarden = new PaleGarden();

    @Setting.Feature(icon = "trickytrialsbackport:creaking_heart")
    @Config.LangKey("config.trickytrialsbackport.creaking")
    @Config.Comment("The creaking.")
    public static final Creaking creaking = new Creaking();

    @Setting.Feature(icon = "trickytrialsbackport:resin_clump")
    @Config.LangKey("config.trickytrialsbackport.resin")
    @Config.Comment("Resin clumps, resin blocks and resin bricks.")
    public static final Resin resin = new Resin();

    @Setting.Feature(icon = "trickytrialsbackport:wildflowers")
    @Config.LangKey("config.trickytrialsbackport.flora")
    @Config.Comment("Spring's plants: bushes, firefly bushes, cactus flowers, dry grass, leaf litter, wildflowers and fallen trees.")
    public static final Flora flora = new Flora();

    @Setting.Feature(icon = "minecraft:egg")
    @Config.LangKey("config.trickytrialsbackport.animals")
    @Config.Comment("Warm and cold cows, pigs and chickens, blue and brown eggs, and wolf sound variants.")
    public static final Animals animals = new Animals();

    @Setting.Feature(icon = "minecraft:ghast_tear")
    @Config.LangKey("config.trickytrialsbackport.happyGhast")
    @Config.Comment("The happy ghast, ghastlings, the dried ghast and harnesses.")
    public static final HappyGhast happyGhast = new HappyGhast();

    @Setting.Feature(icon = "minecraft:compass")
    @Config.LangKey("config.trickytrialsbackport.locatorBar")
    @Config.Comment("The locator bar, which shows other players' directions.")
    public static final LocatorBar locatorBar = new LocatorBar();

    @Setting.Feature(icon = "minecraft:chest")
    @Config.LangKey("config.trickytrialsbackport.copperGolem")
    @Config.Comment("The copper golem, copper golem statues and copper chests.")
    public static final CopperGolem copperGolem = new CopperGolem();

    @Setting.Feature(icon = "minecraft:golden_pickaxe")
    @Config.LangKey("config.trickytrialsbackport.copperGear")
    @Config.Comment("Copper tools, armor and nuggets; copper torches, lanterns, bars and chains.")
    public static final CopperGear copperGear = new CopperGear();

    @Setting.Feature(icon = "minecraft:bookshelf")
    @Config.LangKey("config.trickytrialsbackport.shelves")
    @Config.Comment("Wooden shelves.")
    public static final Shelves shelves = new Shelves();

    @Setting.Feature(icon = "minecraft:arrow")
    @Config.LangKey("config.trickytrialsbackport.spears")
    @Config.Comment("Spears and Lunge.")
    public static final Spears spears = new Spears();

    @Setting.Feature(icon = "minecraft:prismarine_shard")
    @Config.LangKey("config.trickytrialsbackport.nautilus")
    @Config.Comment("The nautilus, the zombie nautilus, nautilus armor and Breath of the Nautilus.")
    public static final Nautilus nautilus = new Nautilus();

    @Setting.Feature(icon = "minecraft:diamond_horse_armor")
    @Config.LangKey("config.trickytrialsbackport.mounts")
    @Config.Comment("The camel husk, the parched, zombie horses in the wild and netherite horse armor.")
    public static final Mounts mounts = new Mounts();

    @Setting.Feature(icon = "minecraft:book")
    @Config.LangKey("config.trickytrialsbackport.tweaks")
    @Config.Comment("1.21's smaller changes to things 1.12.2 already has.")
    public static final Tweaks tweaks = new Tweaks();

    @Setting.Feature(icon = "minecraft:yellow_flower")
    @Config.LangKey("config.trickytrialsbackport.babies")
    @Config.Comment("26.1's golden dandelion, baby mob changes and new animal sounds.")
    public static final Babies babies = new Babies();

    @Setting.Feature(icon = "minecraft:stained_hardened_clay@4")
    @Config.LangKey("config.trickytrialsbackport.sulfur")
    @Config.Comment("26.2's sulfur and cinnabar blocks, potent sulfur and sulfur spikes.")
    public static final Sulfur sulfur = new Sulfur();

    @Setting.Feature(icon = "minecraft:magma_cream")
    @Config.LangKey("config.trickytrialsbackport.sulfurCube")
    @Config.Comment("26.2's sulfur cube.")
    public static final SulfurCube sulfurCube = new SulfurCube();

    @Setting.Feature(icon = "minecraft:water_bucket")
    @Config.LangKey("config.trickytrialsbackport.sulfurCaves")
    @Config.Comment("26.2's sulfur caves and sulfur springs.")
    public static final SulfurCaves sulfurCaves = new SulfurCaves();

    @Setting.Feature(icon = "minecraft:sapling")
    @Config.LangKey("config.trickytrialsbackport.poplar")
    @Config.Comment("26.3's poplar wood, red shrubs, shelf mushrooms and the dappled forest.")
    public static final Poplar poplar = new Poplar();

    @Setting.Feature(icon = "minecraft:wool")
    @Config.LangKey("config.trickytrialsbackport.softBlocks")
    @Config.Comment("26.3's wool and concrete stairs and slabs, cushions and straw beds.")
    public static final SoftBlocks softBlocks = new SoftBlocks();

    @Setting.Feature(icon = "minecraft:map")
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
        @Setting.NewChunks
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
        @Setting.ListOf(Setting.Kind.ITEM)
        @Config.LangKey("config.trickytrialsbackport.copper.waxItems")
        public String[] waxItems = {"buzzybeesbackport:honeycomb"};

        @Config.Comment({"Without another installed mod that adds copper, copper ore generates in newly generated",
                "chunks, as in 1.17."})
        @Setting.NewChunks
        @Config.LangKey("config.trickytrialsbackport.copper.generate")
        public boolean generate = true;

        @Config.Comment("Copper ore veins per chunk.")
        @Setting.NewChunks
        @Config.LangKey("config.trickytrialsbackport.copper.veinsPerChunk")
        @Config.RangeInt(min = 0, max = 64)
        public int veinsPerChunk = 6;

        @Config.Comment("Blocks of copper ore per vein, at most.")
        @Setting.NewChunks
        @Config.LangKey("config.trickytrialsbackport.copper.veinSize")
        @Config.RangeInt(min = 1, max = 64)
        public int veinSize = 10;

        @Config.Comment("Lowest height of a copper ore vein.")
        @Setting.NewChunks
        @Config.LangKey("config.trickytrialsbackport.copper.minY")
        @Config.RangeInt(min = 1, max = 255)
        public int minY = 1;

        @Config.Comment("Highest height of a copper ore vein.")
        @Setting.NewChunks
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
        @Setting.NewChunks
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
        @Config.RequiresMcRestart
        @Config.Comment({"Part of every roofed forest becomes a pale garden, in newly generated chunks, as 1.21.4 put",
                "the pale garden in the dark forest's climate."})
        @Setting.NewChunks
        @Config.LangKey("config.trickytrialsbackport.paleGarden.generateBiome")
        public boolean generateBiome = true;
        // --- end PaleGarden ---
    }

    public static final class Creaking {
        @Config.RequiresMcRestart
        @Config.Comment("The creaking. Off, nothing is registered.")
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
        @Config.RequiresMcRestart
        @Config.Comment({"Cows spawn warm or cold by biome, with 1.21.5's cow models and textures.",
                "Off, every cow stays as 1.12.2 draws it."})
        @Config.LangKey("config.trickytrialsbackport.animals.cowVariants")
        public boolean cowVariants = true;

        @Config.RequiresMcRestart
        @Config.Comment({"Pigs spawn warm or cold by biome, with 1.21.5's pig models and textures.",
                "Off, every pig stays as 1.12.2 draws it."})
        @Config.LangKey("config.trickytrialsbackport.animals.pigVariants")
        public boolean pigVariants = true;

        @Config.RequiresMcRestart
        @Config.Comment({"Chickens spawn warm or cold by biome, with 1.21.5's chicken models and textures.",
                "Off, every chicken stays as 1.12.2 draws it."})
        @Config.LangKey("config.trickytrialsbackport.animals.chickenVariants")
        public boolean chickenVariants = true;

        @Config.RequiresMcRestart
        @Config.Comment({"Blue and brown eggs: cold chickens lay blue eggs and warm chickens brown ones, which hatch",
                "chicks of that kind and bake into cakes and pumpkin pies. Off, the eggs are not registered."})
        @Config.LangKey("config.trickytrialsbackport.animals.coloredEggs")
        public boolean coloredEggs = true;

        @Config.Comment({"Sheep in warm biomes are mostly brown and in cold biomes mostly black, as in 1.21.5.",
                "Off, sheep spawn mostly white everywhere."})
        @Config.LangKey("config.trickytrialsbackport.animals.sheepColors")
        public boolean sheepColors = true;

        @Config.Comment({"Each wolf gets one of 1.21.5's seven voices (classic, puglin, sad, angry, grumpy, big, cute).",
                "Off, every wolf sounds as in 1.12.2."})
        @Config.LangKey("config.trickytrialsbackport.animals.wolfSoundVariants")
        public boolean wolfSoundVariants = true;
        // --- end Animals ---
    }

    public static final class HappyGhast {
        @Config.RequiresMcRestart
        @Config.Comment("The happy ghast, the dried ghast and harnesses. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.happyGhast.enabled")
        public boolean enabled = true;

        // --- HappyGhast fields ---
        @Config.Comment("While riding a happy ghast, the third-person camera stays 8 blocks back instead of 4.")
        @Config.LangKey("config.trickytrialsbackport.happyGhast.riderCameraDistance")
        public boolean riderCameraDistance = true;
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
        @Config.Comment("A carved pumpkin or jack o'lantern placed on a copper block builds a copper golem, and the copper block turns into a copper chest.")
        @Config.LangKey("config.trickytrialsbackport.copperGolem.building")
        public boolean building = true;

        @Config.Comment("Copper golems carry items from copper chests to chests and trapped chests nearby.")
        @Config.LangKey("config.trickytrialsbackport.copperGolem.sorting")
        public boolean sorting = true;

        @Config.Comment("Copper golems, copper chests and copper golem statues oxidize over time.")
        @Config.LangKey("config.trickytrialsbackport.copperGolem.oxidation")
        public boolean oxidation = true;

        @Config.Comment("A fully oxidized copper golem sometimes turns into a copper golem statue.")
        @Config.LangKey("config.trickytrialsbackport.copperGolem.statues")
        public boolean statues = true;

        @Config.Comment("Iron golems sometimes put a poppy on a copper golem's head.")
        @Config.LangKey("config.trickytrialsbackport.copperGolem.ironGolemFlowers")
        public boolean ironGolemFlowers = true;
        // --- end CopperGolem ---
    }

    public static final class CopperGear {
        @Config.RequiresMcRestart
        @Config.Comment("Copper tools, armor and the other copper items. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.copperGear.enabled")
        public boolean enabled = true;

        // --- CopperGear fields ---
        @Config.Comment({"Zombies, skeletons and their kinds that spawn wearing armor pick it as 1.21.9 does: leather, copper",
                "or gold first, then a better tier now and then. Off, 1.12.2's roll (no copper)."})
        @Config.LangKey("config.trickytrialsbackport.copperGear.mobArmor")
        public boolean mobArmor = true;

        @Config.Comment("Copper horse armor in the chests that have iron horse armor (dungeons, temples, strongholds, ...).")
        @Config.LangKey("config.trickytrialsbackport.copperGear.horseArmorLoot")
        public boolean horseArmorLoot = true;
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
        @Config.Comment({"Zombies, husks and zombie villagers that spawn with a weapon sometimes hold an iron spear, and zombie",
                "pigmen a golden spear one time in 20, as in 1.21.11. Off, 1.12.2's weapons."})
        @Config.LangKey("config.trickytrialsbackport.spears.mobSpears")
        public boolean mobSpears = true;

        @Config.Comment("Zombies and zombie pigmen holding a spear charge at their target with it, as in 1.21.11.")
        @Config.LangKey("config.trickytrialsbackport.spears.mobCharges")
        public boolean mobCharges = true;
        // --- end Spears ---
    }

    public static final class Nautilus {
        @Config.RequiresMcRestart
        @Config.Comment("The nautilus, its armor and its effect. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.nautilus.enabled")
        public boolean enabled = true;

        // --- Nautilus fields ---
        @Config.RequiresMcRestart
        @Config.Comment("Nautiluses spawn in oceans, 5 to 25 blocks below sea level, as in 1.21.11.")
        @Config.LangKey("config.trickytrialsbackport.nautilus.naturalSpawn")
        public boolean naturalSpawn = true;

        @Config.Comment("With Update Aquatic, half of the drowned that spawn with a trident outside rivers ride a zombie nautilus.")
        @Config.LangKey("config.trickytrialsbackport.nautilus.drownedRiders")
        public boolean drownedRiders = true;

        @Config.Comment("With Update Aquatic, shipwreck, buried treasure and ocean ruin chests can hold nautilus armor.")
        @Config.LangKey("config.trickytrialsbackport.nautilus.chestLoot")
        public boolean chestLoot = true;
        // --- end Nautilus ---
    }

    public static final class Mounts {
        @Config.RequiresMcRestart
        @Config.Comment("The camel husk, the parched and netherite horse armor. Off, nothing is registered.")
        @Config.LangKey("config.trickytrialsbackport.mounts.enabled")
        public boolean enabled = true;

        // --- Mounts fields ---
        @Config.Comment({"Zombie horses as in 1.21.11: 25 health, tamed by riding, red mushrooms tempt and heal them, they take a saddle and",
                "horse armor, burn in daylight without armor, and despawn while untamed."})
        @Config.LangKey("config.trickytrialsbackport.mounts.zombieHorses")
        public boolean zombieHorses = true;

        @Config.RequiresMcRestart
        @Config.Comment("Zombie horses spawn in plains and savannas in the dark, each ridden by a zombie (with an iron spear when spears exist).")
        @Config.LangKey("config.trickytrialsbackport.mounts.zombieHorseSpawn")
        public boolean zombieHorseSpawn = true;

        @Config.RequiresMcRestart
        @Config.Comment("Parched spawn in deserts under the open sky, taking 50 of the 100 skeleton spawns there, as in 1.21.11.")
        @Config.LangKey("config.trickytrialsbackport.mounts.parchedSpawn")
        public boolean parchedSpawn = true;

        @Config.RequiresMcRestart
        @Config.Comment({"Netherite horse armor: 19 armor, 3 toughness, knockback resistance; made from diamond horse armor at a",
                "smithing table when a netherite mod is installed."})
        @Config.LangKey("config.trickytrialsbackport.mounts.netheriteHorseArmor")
        public boolean netheriteHorseArmor = true;

        @Config.Comment("Horses, donkeys, mules and camels float up in deep water while ridden instead of sinking (1.21.11).")
        @Config.LangKey("config.trickytrialsbackport.mounts.floatWhileRidden")
        public boolean floatWhileRidden = true;

        @Config.Comment("A zombie riding a mount reaches as far as its mount is wide, as in current versions.")
        @Config.LangKey("config.trickytrialsbackport.mounts.mountedReach")
        public boolean mountedReach = true;
        // --- end Mounts ---
    }

    public static final class Tweaks {
        @Config.Comment("1.21's smaller changes to existing blocks, items and mobs.")
        @Config.LangKey("config.trickytrialsbackport.tweaks.enabled")
        public boolean enabled = true;

        // --- Tweaks fields ---
        @Config.Comment("Mounts and vehicles go through nether and end portals with their riders (1.21).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.ridersThroughPortals")
        public boolean ridersThroughPortals = true;

        @Config.Comment("An ender pearl thrown through a portal teleports its thrower to the other dimension (1.21).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.pearlsAcrossDimensions")
        public boolean pearlsAcrossDimensions = true;

        @Config.Comment({"Thrown ender pearls load and tick the 3x3 chunks around them, and leave with their thrower when",
                "they log out (1.21.2)."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.pearlsLoadChunks")
        public boolean pearlsLoadChunks = true;

        @Config.Comment("Primed TNT goes through nether portals, and cannot break portal blocks afterwards (1.21).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.tntThroughPortals")
        public boolean tntThroughPortals = true;

        @Config.Comment({"Portal cooldowns: 0.5 seconds for boats and minecarts, 0.1 seconds for projectiles, a rider's",
                "own for a ridden mount (1.21.2)."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.portalCooldowns")
        public boolean portalCooldowns = true;

        @Config.RequiresMcRestart
        @Config.Comment("Saddles are crafted from 3 leather and an iron ingot (1.21.6).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.saddleRecipe")
        public boolean saddleRecipe = true;

        @Config.Comment("Dungeon, temple and stronghold chests have 1-5 leather where they had a saddle (1.21.6).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.saddleLoot")
        public boolean saddleLoot = true;

        @Config.Comment({"Shears take off a mob's leads, then its horse armor or carpet, then its saddle, when no one rides",
                "it (1.21.6)."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.shearsRemoveEquipment")
        public boolean shearsRemoveEquipment = true;

        @Config.Comment("Horses, donkeys and mules eat carrots, as they eat apples (1.21.6).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.horsesEatCarrots")
        public boolean horsesEatCarrots = true;

        @Config.Comment({"Sneak-use a mob to tie the mobs you lead to it; leads snap at 12 blocks instead of 10",
                "(1.21.6)."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.leads")
        public boolean leads = true;

        @Config.RequiresMcRestart
        @Config.Comment("Leads are crafted from 5 string, without a slimeball (1.21.6).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.leadRecipe")
        public boolean leadRecipe = true;

        @Config.Comment({"Projectiles knock back in the direction they fly (potions away from themselves), and",
                "projectiles from dispensers knock back too (1.21)."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.projectileKnockback")
        public boolean projectileKnockback = true;

        @Config.Comment("With an axe in the main hand and a shield in the off hand, use raises the shield (1.21).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.axeShield")
        public boolean axeShield = true;

        @Config.Comment("Witches always drop 4-8 redstone dust (1.21).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.witchRedstone")
        public boolean witchRedstone = true;

        @Config.RequiresMcRestart
        @Config.Comment({"No spawn chunks: the area around the world spawn is not kept loaded nor prepared at start",
                "(1.21.9). With Trails & Tales, its spawn chunk radius is set to 0."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.noSpawnChunks")
        public boolean noSpawnChunks = true;

        @Config.Comment("Players get no damage immunity when they spawn, respawn or join (1.21.4).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.noSpawnInvulnerability")
        public boolean noSpawnInvulnerability = true;

        @Config.Comment("Water Breathing and Conduit Power refill air under water (1.21.4).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.waterBreathingRefillsAir")
        public boolean waterBreathingRefillsAir = true;

        @Config.Comment("Gliding with an elytra ends sprinting (1.21.4).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.elytraEndsSprint")
        public boolean elytraEndsSprint = true;

        @Config.Comment({"Gliding into a ladder or other climbable block ends the flight; vines can be glided through",
                "(1.21.5, 1.21.11)."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.elytraClimbables")
        public boolean elytraClimbables = true;

        @Config.Comment({"Fire Protection's shorter burning and Blast Protection's lower knockback add up over every",
                "piece worn (1.21)."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.protectionStacks")
        public boolean protectionStacks = true;

        @Config.Comment({"Animals panic for 2 seconds after damage that causes panic, not after any hit; adult polar",
                "bears only from fire, cactus and lightning; mounts ridden by mobs never (1.21, 1.21.11)."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.panic")
        public boolean panic = true;

        @Config.Comment("End crystals are immune to fire (1.21).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.crystalsFireImmune")
        public boolean crystalsFireImmune = true;

        @Config.Comment("Doors of different materials placed side by side form a double door (1.21).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.mixedDoubleDoors")
        public boolean mixedDoubleDoors = true;

        @Config.Comment({"A splash potion's strength depends on the distance from where it lands to the nearest part of",
                "a mob, not to its feet (1.21.2)."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.splashDistance")
        public boolean splashDistance = true;

        @Config.Comment("A pumpkin worn on the head hides you from other players' maps (1.21.2).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.pumpkinHidesMapMarker")
        public boolean pumpkinHidesMapMarker = true;

        @Config.Comment({"Iron doors, pressure plates, brewing stands, ender chests, bells and lanterns drop when",
                "broken without the right tool (1.21.4)."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.toolFreeDrops")
        public boolean toolFreeDrops = true;

        @Config.Comment("Thorns on a saddle hurts whoever attacks the mob wearing it (1.21.5).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.saddleThorns")
        public boolean saddleThorns = true;

        @Config.Comment("The End's obsidian platform drops the blocks it replaces instead of deleting them (1.21).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.portalsDropBlocks")
        public boolean portalsDropBlocks = true;

        @Config.Comment({"Fire only burns and spreads within 128 blocks of a player, unless the game rule",
                "allowFireTicksAwayFromPlayer is true (1.21.5)."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.fireNearPlayers")
        public boolean fireNearPlayers = true;

        @Config.Comment("A mob head keeps its custom name when placed and mined again (1.21.9).")
        @Config.LangKey("config.trickytrialsbackport.tweaks.headNames")
        public boolean headNames = true;

        @Config.Comment({"The game rules of 1.21.5 and 1.21.9: tntExplodes, allowFireTicksAwayFromPlayer,",
                "allowEnteringNetherUsingPortals, spawnMonsters, pvp, commandBlocksEnabled, spawnerBlocksEnabled."})
        @Config.LangKey("config.trickytrialsbackport.tweaks.gameRules")
        public boolean gameRules = true;
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
        @Setting.NewChunks
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
        @Setting.NewChunks
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
