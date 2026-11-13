"""The stream table: one row per stream, the single source for ModConfig's categories and their lang lines.

    python tools/common/streams.py      (rewrites ModConfig.java's category list; run after adding a row)

Each row: (config field, category class, category comment, enabled tooltip, category display name).
Streams add their own extra fields by hand inside their category class, between the markers.
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODID = "trickytrialsbackport"

STREAMS = [
    ("tuff", "Tuff", "1.21's tuff stairs, slabs and walls, polished tuff, tuff bricks and the chiseled forms.",
     "The tuff blocks. Off, nothing is registered, and a world that has them loses them.", "Tuff"),
    ("copper", "Copper", "Copper bulbs, doors, trapdoors, grates and chiseled copper, with their oxidation and waxing.",
     "The copper blocks. Off, nothing is registered, and a world that has them loses them.", "Copper Blocks"),
    ("breeze", "Breeze", "The breeze, breeze rods and wind charges.",
     "The breeze, the breeze rod and the wind charge. Off, nothing is registered.", "Breeze"),
    ("bogged", "Bogged", "The bogged, the skeleton of swamps.",
     "The bogged. Off, nothing is registered.", "Bogged"),
    ("mace", "Mace", "The mace, the heavy core, and Density, Breach and Wind Burst.",
     "The mace, the heavy core and the mace's three enchantments. Off, nothing is registered.", "Mace"),
    ("crafter", "Crafter", "The crafter.",
     "The crafter. Off, nothing is registered.", "Crafter"),
    ("trials", "Trials", "Trial spawners, vaults, trial keys, ominous trials and Trial Omen.",
     "Trial spawners, vaults, both trial keys and Trial Omen. Off, nothing is registered.", "Trial Spawners and Vaults"),
    ("effects", "Effects", "Weaving, Oozing, Infested and Wind Charged, and their potions and arrows.",
     "The four effects and their potions. Off, nothing is registered.", "Effects and Potions"),
    ("decor", "Decor", "New paintings, banner patterns, music discs, pottery sherds and armor trim templates.",
     "Paintings, banner patterns, music discs, sherds and trim templates. Off, nothing is registered.",
     "Paintings, Patterns and Discs"),
    ("chambers", "Chambers", "Trial chambers.",
     "Trial chambers generate underground in newly generated chunks.", "Trial Chambers"),
    ("bundles", "Bundles", "Bundles and the sixteen dyed bundles.",
     "Bundles. Off, nothing is registered.", "Bundles"),
    ("paleGarden", "PaleGarden", "The pale garden: pale oak, pale moss, hanging moss, eyeblossoms, the creaking heart and the biome.",
     "The pale garden's blocks and items. Off, nothing is registered.", "Pale Garden"),
    ("creaking", "Creaking", "The creaking.",
     "The creaking. Off, nothing is registered.", "Creaking"),
    ("resin", "Resin", "Resin clumps, resin blocks and resin bricks.",
     "The resin blocks and items. Off, nothing is registered.", "Resin"),
    ("flora", "Flora", "Spring's plants: bushes, firefly bushes, cactus flowers, dry grass, leaf litter, wildflowers and fallen trees.",
     "The new plants. Off, nothing is registered.", "Spring Plants"),
    ("animals", "Animals", "Warm and cold cows, pigs and chickens, blue and brown eggs, and wolf sound variants.",
     "The farm animal variants and the two eggs. Off, nothing is registered.", "Farm Animal Variants"),
    ("happyGhast", "HappyGhast", "The happy ghast, ghastlings, the dried ghast and harnesses.",
     "The happy ghast, the dried ghast and harnesses. Off, nothing is registered.", "Happy Ghast"),
    ("locatorBar", "LocatorBar", "The locator bar, which shows other players' directions.",
     "Show the locator bar in place of the experience bar while other players are near.", "Locator Bar"),
    ("copperGolem", "CopperGolem", "The copper golem, copper golem statues and copper chests.",
     "The copper golem, its statue and the copper chest. Off, nothing is registered.", "Copper Golem"),
    ("copperGear", "CopperGear", "Copper tools, armor and nuggets; copper torches, lanterns, bars and chains.",
     "Copper tools, armor and the other copper items. Off, nothing is registered.", "Copper Tools and Items"),
    ("shelves", "Shelves", "Wooden shelves.",
     "Shelves. Off, nothing is registered.", "Shelves"),
    ("spears", "Spears", "Spears and Lunge.",
     "The seven spears and Lunge. Off, nothing is registered.", "Spears"),
    ("nautilus", "Nautilus", "The nautilus, the zombie nautilus, nautilus armor and Breath of the Nautilus.",
     "The nautilus, its armor and its effect. Off, nothing is registered.", "Nautilus"),
    ("mounts", "Mounts", "The camel husk, the parched, zombie horses in the wild and netherite horse armor.",
     "The camel husk, the parched and netherite horse armor. Off, nothing is registered.", "Mounts"),
    ("tweaks", "Tweaks", "1.21's smaller changes to things 1.12.2 already has.",
     "1.21's smaller changes to existing blocks, items and mobs.", "Smaller Changes"),
    ("babies", "Babies", "26.1's golden dandelion, baby mob changes and new animal sounds.",
     "The golden dandelion and the other 26.1 changes. Off, nothing is registered.", "Golden Dandelion and Babies"),
    ("sulfur", "Sulfur", "26.2's sulfur and cinnabar blocks, potent sulfur and sulfur spikes.",
     "The sulfur and cinnabar blocks. Off, nothing is registered.", "Sulfur and Cinnabar"),
    ("sulfurCube", "SulfurCube", "26.2's sulfur cube.",
     "The sulfur cube and its bucket. Off, nothing is registered.", "Sulfur Cube"),
    ("sulfurCaves", "SulfurCaves", "26.2's sulfur caves and sulfur springs.",
     "Sulfur caves and sulfur springs generate in newly generated chunks.", "Sulfur Caves"),
    ("poplar", "Poplar", "26.3's poplar wood, red shrubs, shelf mushrooms and the dappled forest.",
     "The poplar blocks and the dappled forest's plants. Off, nothing is registered.", "Poplar and Dappled Forest"),
    ("softBlocks", "SoftBlocks", "26.3's wool and concrete stairs and slabs, cushions and straw beds.",
     "The wool and concrete shapes, cushions and straw beds. Off, nothing is registered.", "Wool, Concrete and Cushions"),
    ("camps", "Camps", "26.3's abandoned camps and explorer maps.",
     "Abandoned camps generate in newly generated chunks.", "Abandoned Camps"),
]

# Restart-free: read every tick, so the switch takes effect at once.
LIVE = {"locatorBar", "tweaks"}

# Backport Core's settings screen: the item drawn on each category's row (own item where the stream has one).
ICONS = {
    "tuff": "trickytrialsbackport:tuff_bricks",
    "copper": "trickytrialsbackport:copper_bulb",
    "breeze": "trickytrialsbackport:breeze_rod",
    "bogged": "minecraft:brown_mushroom",
    "mace": "trickytrialsbackport:mace",
    "crafter": "trickytrialsbackport:crafter",
    "trials": "minecraft:mob_spawner",
    "effects": "minecraft:brewing_stand",
    "decor": "minecraft:painting",
    "chambers": "trickytrialsbackport:chiseled_tuff_bricks",
    "bundles": "minecraft:leather",
    "paleGarden": "trickytrialsbackport:pale_oak_sapling",
    "creaking": "minecraft:lit_pumpkin",
    "resin": "minecraft:dye@14",
    "flora": "minecraft:red_flower@8",
    "animals": "minecraft:egg",
    "happyGhast": "minecraft:ghast_tear",
    "locatorBar": "minecraft:compass",
    "copperGolem": "minecraft:chest",
    "copperGear": "minecraft:golden_pickaxe",
    "shelves": "minecraft:bookshelf",
    "spears": "minecraft:arrow",
    "nautilus": "minecraft:prismarine_shard",
    "mounts": "minecraft:diamond_horse_armor",
    "tweaks": "minecraft:book",
    "babies": "minecraft:yellow_flower",
    "sulfur": "minecraft:stained_hardened_clay@4",
    "sulfurCube": "minecraft:magma_cream",
    "sulfurCaves": "minecraft:water_bucket",
    "poplar": "minecraft:sapling",
    "softBlocks": "minecraft:wool",
    "camps": "minecraft:map",
}

# Switches that only reach newly generated chunks (Backport Core's New chunks badge).
NEW_CHUNKS = {"chambers", "sulfurCaves", "camps"}

CONFIG = os.path.join(ROOT, "src", "main", "java", "com", "exiledradio", MODID, "ModConfig.java")

HEAD = '''package com.exiledradio.trickytrialsbackport;

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

'''

TAIL = '''    // ------------------------------------------------------------------------------------------

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
'''


def java_string(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def existing_extras():
    """@return category class -> the hand-written lines between its markers, kept across rewrites."""
    extras = {}
    if not os.path.exists(CONFIG):
        return extras
    text = open(CONFIG, encoding="utf-8").read()
    for m in re.finditer(r"        // --- (\w+) fields ---\n(.*?)        // --- end \1 ---\n", text, re.S):
        extras[m.group(1)] = m.group(2)
    return extras


def write_config():
    extras = existing_extras()
    out = [HEAD]
    for field, cls, comment, tip, _ in STREAMS:
        out.append('    @Setting.Feature(icon = "%s")\n' % ICONS[field])
        out.append('    @Config.LangKey("config.%s.%s")\n' % (MODID, field))
        out.append("    @Config.Comment(%s)\n" % java_string(comment))
        out.append("    public static final %s %s = new %s();\n\n" % (cls, field, cls))
    out.append("    // ------------------------------------------------------------------------------------------\n")
    out.append("    // Categories. Each stream owns exactly one.\n")
    out.append("    // ------------------------------------------------------------------------------------------\n\n")
    for field, cls, comment, tip, _ in STREAMS:
        out.append("    public static final class %s {\n" % cls)
        if field not in LIVE:
            out.append("        @Config.RequiresMcRestart\n")
        if field in NEW_CHUNKS:
            out.append("        @Setting.NewChunks\n")
        out.append("        @Config.Comment(%s)\n" % java_string(tip))
        out.append('        @Config.LangKey("config.%s.%s.enabled")\n' % (MODID, field))
        out.append("        public boolean enabled = true;\n\n")
        out.append("        // --- %s fields ---\n" % cls)
        out.append(extras.get(cls, ""))
        out.append("        // --- end %s ---\n" % cls)
        out.append("    }\n\n")
    out.append(TAIL)
    with open(CONFIG, "w", encoding="utf-8", newline="\n") as f:
        f.write("".join(out))


def lang_lines():
    """@return the config category and enabled-switch names for en_us.lang.

    Tooltips are not here: the family lang tool's `config` command writes every `.tooltip` line from the
    @Config comments.
    """
    lines = []
    for field, cls, comment, tip, name in STREAMS:
        lines.append("config.%s.%s=%s" % (MODID, field, name))
        lines.append("config.%s.%s.enabled=Enabled" % (MODID, field))
    return lines


if __name__ == "__main__":
    write_config()
    print("ModConfig: %d categories" % len(STREAMS))
