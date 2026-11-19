# Stream 20: copper tools and items (1.21.9)

Package `coppergear`. Config `copperGear` (`enabled`, `mobArmor`, `horseArmorLoot`). No entities; packet ids 100-104
unused. Branch `claude/tr-coppergear-29567a`, dev-server port 25901 (row 20 wraps onto row 1's). Patch P16
(`docs/PATCHES.md`). Reference: minecraft.wiki (*Java Edition 1.21.9*, *Copper Armor*, *Copper Sword*, *Armor*),
Mojang bug MC-182478, and the 1.21.11 jar's recipes, loot tables, blockstates, models, textures and sounds.

## Changelog section

*   **Copper tools**: a copper sword, pickaxe, axe, shovel and hoe, between stone and iron. They hit as hard as
    stone tools, mine what stone tools mine, a little faster (speed 5), and last 190 uses. Copper ingots repair them.
*   **Copper armor**: helmet, chestplate, leggings and boots, 10 armor points in all. Copper ingots repair it, and it
    clonks when you put it on.
*   **Copper horse armor**: 4 armor points. Not craftable: it turns up wherever iron horse armor does (dungeons,
    desert and jungle temples, Nether fortresses, strongholds, End cities and village blacksmiths).
*   **Copper nuggets**: nine from an ingot, and nine make an ingot. Every copper tool and piece of copper armor
    smelts down to a nugget.
*   **Copper torch**: a nugget, coal or charcoal and a stick make four. It burns with a green flame and gives as much
    light as a torch.
*   **Copper lantern**: eight nuggets around a copper torch. It stands or hangs like a lantern, light 15.
*   **Copper bars** (six ingots make sixteen) and **copper chains** (an ingot between two nuggets): like iron bars and
    chains. Copper bars, chains and lanterns oxidize like other copper; an axe scrapes them and honeycomb waxes them.
*   **Mobs wear copper**: zombies, husks, zombie villagers, skeletons, strays and the bogged that spawn in armor can
    wear copper, the most common kind. They now put on their helmet first, as in current Minecraft.

## How it works

*   **Tools** (`CopperGearContent`): one `ToolMaterial` (harvest level 1, 190 uses, efficiency 5, +1 damage,
    enchantability 13), so the sword hits for 5 at 1.6, the pickaxe 3 at 1.2, the axe 9 at 0.8 (`ItemAxe`'s own
    table has no row for a new material, so the axe passes 8 and -3.2 itself), the shovel 3.5 at 1.0 and the hoe 1
    at 2.0: 1.21.9's numbers, the same as stone's.
*   **Armor**: one `ArmorMaterial`: durability factor 11 (helmet 121, chestplate 176, leggings 165, boots 143),
    defense 2/4/3/1, toughness 0, enchantability 8, `item.armor.equip_copper`. The worn textures are 1.21.11's
    `equipment/humanoid(_leggings)/copper.png` as 1.12.2's `copper_layer_1/2.png` (the same 64x32 layout).
*   **Repair**: both materials take the copper ingot, set when recipes register (the owner's items exist by then).
*   **Horse armor**: a Forge `HorseArmorType` with protection 4. 1.14 remade the horse model, so 1.21.11's 64x64
    horse texture does not fit 1.12.2's 128x128 one (Backport Core leaves horses out for that reason). The worn
    texture is 1.12.2's iron horse armor recolored by the generator: every iron shade of 1.21.11's
    `horse_body/iron.png` is matched to the copper shade at the same pixels of `horse_body/copper.png` (27 shades),
    and each pixel of 1.12.2's iron armor takes the copper shade of its nearest iron shade. Copper's teal face plates
    come through.
*   **Loot** (`lootTables`): into the pool that holds `minecraft:iron_horse_armor`, with 1.21.11's weight: simple
    dungeon 15, desert pyramid 15, Nether fortress 5, jungle temple, stronghold corridor, End city 1, and 1.12.2's
    village blacksmith 1 (1.21.11's weaponsmith; 1.12.2 has one smith chest). Behind `horseArmorLoot`.
*   **Nuggets**: `nuggetCopper` in the ore dictionary. Smelting (`copper_nugget_from_smelting`): the ten copper
    tools and armor pieces and the horse armor, worn or not (1.12.2's wildcard metadata), to one nugget, 0.1
    experience. The copper spear and nautilus armor in that recipe belong to streams 22 and 23.
*   **Recipes** (1.21.11's): tools, armor, nuggets, ingot from nuggets, 16 bars, chain, 4 torches (coal or
    charcoal), lantern, and honeycomb waxing in the grid (`honeycomb` ore name) for all twelve unwaxed blocks.
*   **The ingot (Rule 1)**: `Owners.item(cavesandcliffsbackport, "copper_ingot")`: Caves & Cliffs' when installed,
    else the copper stream's copy. With neither (Caves & Cliffs absent and `copper.enabled=false`) nothing made
    of ingots can be crafted and the log says so; the torch and lantern recipes need only nuggets but nothing makes
    nuggets either then.
*   **Torch** (`CopperGearBlocks.Torch`): 1.12.2's `BlockTorch` (floor and wall in one block by `facing`), light 14,
    with its smoke and a green `copper_fire_flame` (`client/CopperGearClient`: a flame particle drawn with the
    1.21.11 sprite). Its blockstate joins 1.21.11's `copper_torch` and `copper_wall_torch`.
*   **Bars** (`Bars`): 1.12.2's `BlockPane` of copper: hardness 5, resistance 6, copper sounds, pickaxe; they join
    other bars, glass panes and solid faces. 1.21.11's multipart blockstates.
*   **Chain** (`Chain`): `axis` from the clicked face, a 3x3-pixel pole, hardness 5, resistance 6, chain sounds;
    its ends are `CENTER_SMALL` faces, so lanterns hang from it. Nether Update owns the iron chain; this one is ours.
*   **Lantern** (`Lantern`): `hanging`, light 15, hardness 3.5, lantern sounds, 1.14's shape and placement (the
    clicked bottom face hangs it when something above holds its middle, else it stands, else it hangs) and drops
    when its support goes. Village & Pillage owns the lantern; this one is ours.
*   **Oxidation**: bars, chains and lanterns are three chains of four stages and their waxed forms in Backport
    Core's `copper.Oxidation` table, so they age by its rule (and wait for or are hurried by any mod's nearby
    copper) while `copper.oxidation` is on. Right click (`CopperGearEvents`): honeycomb (`copper.waxItems` or the
    `honeycomb` ore name) waxes, an axe takes wax off or scrapes a stage, state kept. The copper stream's handler
    covers this mod's copper too; whichever runs first cancels the click, so it happens once. Lightning cleans them
    through the copper stream's handler, or this stream's copy of it while the copper stream is off.
*   **Mobs** (`CopperGearHooks`, patch P16): `EntityLiving.setEquipmentBasedOnDifficulty` starts with 1.21.9's armor
    roll: tier `nextInt(3)` over leather, copper and gold, three raises at 0.1087, pieces in the order helmet,
    chestplate, leggings, boots (Mojang fixed the boots-first order in 24w33a, MC-182478), the same stop chances.
    Tiers come out leather 23.60%, copper 32.23%, gold 33.29%, chainmail 9.73%, iron 1.10%, diamond 0.04%. Weapons and
    enchanting are untouched (1.21.9 gives no mob copper tools). `mobArmor` off, or the stream off, runs 1.12.2's roll.

## Tested headlessly

`./gradlew runServer -Pselftest=coppergear`, then `-Pselftest=all` from a fresh world, alone and with Caves &
Cliffs; `python -I tools/check_assets.py`; the release build.

*   **`-Pselftest=coppergear`: `SELFTEST DONE pass=252 fail=0`.**
*   **Alone, `-Pselftest=all`, fresh world (Core 1.3.0, as `gradle.properties` builds): `SELFTEST DONE pass=3395
    fail=0`**; **173 blocks, 2648 states, 209 item models, 840 sound files, 0 errors** from the asset check.
*   **With Caves & Cliffs 1.1.0's dev jar** in `eclipse/mods` (fresh world; it needs Core 1.4.0, so
    `-Pcore_version=1.4.0`): `SELFTEST DONE pass=3244 fail=6`, **all of the coppergear test passing** (the ingot is
    Caves & Cliffs'). The six failures are in other streams' tests and are the same with `copperGear.enabled=false`
    (pass=2987 fail=7, the seventh being this test's own `enabled` check), so they come from Caves & Cliffs 1.1.0,
    not this stream: copper (`waxing the top waxes both halves`, then a door read as air), flora (`every firefly
    bush stands on grass`), flat (three checks expecting this mod's copper and tuff generators, which are not
    registered when Caves & Cliffs owns copper and tuff).
*   **Release** (`./gradlew build --rerun-tasks --offline -PdeployDir= -PcoreDeployDir=`): built, 0 `/selftest/`
    entries in any jar.

What the coppergear test checks:

*   Registration: 25 blocks (a torch and 3 shapes x 4 stages x waxed), each declared ours in Core's table with an
    item; the 11 items; `nuggetCopper`; the ingot is Caves & Cliffs' when installed, the copper stream's copy alone.
*   Tools: material 190 uses, speed 5, +1, enchantability 13, level 1; each tool's damage and speed modifiers
    (5/1.6, 3/1.2, 9/0.8, 3.5/1.0, 1/2.0) and uses; the pickaxe mines iron ore but not diamond ore, stone at 5; the
    axe is an axe; an ingot repairs tools and armor, iron does not.
*   Armor: defense 2/4/3/1, durability 121/176/165/143, toughness 0, enchantability 8, the equip sound; horse armor
    fits a horse, protection 4, stacks to 1.
*   Recipes with whichever ingot is installed: nuggets both ways, the five tools, four armor pieces (5/8/7/4
    ingots), 16 bars, the chain, 4 torches with coal and with charcoal, the lantern, waxing a chain, lantern and bars.
*   Smelting: each tool, armor piece and the horse armor, worn, to one nugget at 0.1 experience.
*   Blocks: torch light 14 and instant; lanterns light 15, hardness 3.5; bars and chains hardness 5; bars need a
    pickaxe; a torch on a wall; bars joining copper bars, iron bars and stone; a chain's axis from the clicked
    face; a lantern hanging under a chain, standing on the floor, not placeable in mid-air, and dropping when its
    chain is removed.
*   Right click: an axe scrapes a lantern a stage (wearing the axe), honeycomb waxes it (using one), an axe scrapes
    a chain keeping its axis.
*   Aging: the stage and wax chains; oxidized bars stop ticking; a lone lantern oxidized after 3000 random ticks.
*   Mobs: P16 applied; 30000 rolls on Hard at full local difficulty: 15% armored (within 1.5%), never armor without a
    helmet, leather/copper/gold/chainmail within 3.5% of 23.60/32.23/33.29/9.73%; and 3000 zombies through
    `onInitialSpawn` (the patched path) wear copper helmets more than 50 times.
*   Loot: copper horse armor in all seven chest tables.

## Not tested

*   Anything drawn: models, the torch's flame, the worn armor and horse armor textures in game, the lantern's and
    chain's look, bars' connections on screen.
*   Sounds playing (the equip clonk, chain and lantern sounds).
*   Lightning cleaning these blocks (the copper stream's handler, tested there for its blocks).
*   Armor trims on copper armor (Trails & Tales with Core's `ArmorTrims`).
*   Our config text in the 17 other languages (English, as in every stream so far); Mojang's names are in all 18.
