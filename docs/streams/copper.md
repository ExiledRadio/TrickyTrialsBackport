# Stream 2: copper blocks (1.21), with copies of Caves & Cliffs' copper

Package `copper`. Config `copper` (`enabled`, `oxidation`, `waxItems`, `generate`, `veinsPerChunk`, `veinSize`,
`minY`, `maxY`). Branch `tr-copper`.

## Changelog section

*   **Copper bulbs**: each redstone pulse switches one on or off. They glow less as they oxidize (15, 12, 8,
    4), and a comparator reads 15 while one is lit.
*   **Copper doors** and **copper trapdoors**: metal, but they open by hand, and wind charges open them too.
*   **Copper grates**: see-through copper blocks.
*   **Chiseled copper**: two cut copper slabs.
*   Every one of them comes in four oxidation stages and waxed. They oxidize over time like all copper.
    An axe scrapes a stage off or takes the wax off, honeycomb waxes them, and lightning cleans them.
*   Copper ore, copper ingots and the 1.17 copper blocks come from the 1.17 blocks when they are installed.
    Without them this mod adds them, with copper ore in the ground of newly generated chunks.

## How it works

*   **1.21's blocks** (40): `chiseled_copper`, `copper_grate`, `copper_bulb`, `copper_door`,
    `copper_trapdoor`, each `<stage>` and `waxed_<stage>`. Hardness 3, blast resistance 6, Material.IRON,
    a stone pickaxe (doors drop with anything, 1.21.4). Bulbs: `lit` and `powered`, flipped in the same tick
    as a rising edge (1.20.4 dropped the delay), light 15/12/8/4 by stage. Doors and trapdoors extend
    vanilla's but open by hand, have the copper door and trapdoor sounds, and are registered with
    `WindBurst.openable`. Grates let light through and draw cut out. Their blockstate files are 1.21.11's
    own (the property names match).
*   **Recipes** (1.21.11): bulbs from three copper blocks of the same stage (waxed from waxed), a blaze rod
    and redstone (4); grates from four copper blocks of the stage (4); chiseled from two cut copper slabs
    of the stage; doors 6 ingots to 3; trapdoors 4 ingots to 1 (1.21.9); waxing in the grid with honeycomb.
    Every recipe takes the owner's copper when it is installed, our copies when not.
*   **The copies** (Rule 1): with Caves & Cliffs absent, its copper ore, raw copper block, the 32 copper blocks
    and 8 double slabs, `copper_ingot` and `raw_copper`, by its published table
    (`caves-and-cliffs-copper-states.md`): the same names, properties, metadata (slab top = 8), material,
    hardness, resistance, harvest levels, map colors and sounds, its recipes, smelting, ore dictionary
    names and its copper ore veins (stone only; deepslate is its). With it present nothing of this is
    registered. The decision is Backport Core's `BlockOwnership`, with Caves & Cliffs declared owner by our
    shim.
*   **Aging** (`Weathering`): the same table and rule as Caves & Cliffs' (a 64/1125 try per random tick,
    stopped by less-oxidized copper within 4, sped by more-oxidized, x0.75 while unaffected), states carried
    across by property, a door's halves together (the lower half decides, the upper half is set first).
    With Caves & Cliffs installed its blocks are entered in our table by name, so our copper waits for its
    copper and ages with it; its table does not know ours yet (`CORE_REQUESTS.md` #5).
*   **Right click and lightning** (`CopperEvents`): for this mod's blocks only (the owner handles its own):
    honeycomb waxes (one used), an axe takes wax off or scrapes a stage (one durability); a real bolt on our
    copper, or on any `lightning_rod` standing on it, cleans that block and walks 3-5 random paths
    scraping our copper. Wax is any item in `copper.waxItems` (Buzzy Bees' honeycomb by default) or with
    the ore name `honeycomb`, as Caves & Cliffs decided.
*   **Shared fix**: `common/WindBurst`'s rays now pass through every block but barrier and bedrock, as 1.21's
    wind charge calculator gives other blocks no resistance (copper's 6 had stopped them).

## Tested headlessly

Twice, both through `./gradlew runServer -Pselftest=all` (port 25902), then `python -I tools/check_assets.py`:

*   **Alone: `SELFTEST DONE pass=1113 fail=0`**, **101 blocks, 1425 states, 95 item models, 296 sound files,
    0 errors**.
*   **With Caves & Cliffs** (its current `main`, 4ebd8c9, built from a clone on G: and put in
    `eclipse/mods`, a fresh world): **`SELFTEST DONE pass=967 fail=0`**.

What the copper test checks:

*   Ownership: copies only without the owner (82 blocks alone, 40 with it); every 1.17 name copied alone and
    the owner's with it; our 1.21 names declared ours in Core's table; slab top metadata 8; double slabs have
    no item.
*   Every block: metal, hardness 3, resistance 6, aging only if unwaxed and not oxidized; bulb and grate
    sounds; grates see-through; doors drop without a tool; a stone pickaxe for the rest.
*   Recipes: exposed bulbs from exposed copper, waxed bulbs from waxed copper, grates, chiseled from slabs,
    doors 6 to 3, trapdoors 4 to 1, waxing in the grid; alone also the copies' block, stairs and smelting.
*   Aging over 3000 random ticks: a lone lit bulb ends oxidized, still lit, light 4; waxed copper never
    changes; copper waits while less-oxidized copper is near; the owner's (or copied) copper counts.
*   Right click: an axe scrapes a stage and wears; honeycomb waxes using one; an axe unwaxes first; nothing
    to scrape from unaffected.
*   Bulbs: a pulse lights one in the same tick (light 15, comparator 15), it stays lit when the power goes,
    the next pulse turns it off (comparator 0); exposed 12, weathered 8.
*   Doors: open by hand; the upper half's ticks do nothing; the lower's age both halves together, still
    open; waxing the top waxes both; each drops its own door item. A wind burst opens a copper trapdoor and
    a hand closes it.
*   Lightning on oxidized chiseled copper cleans it and scrapes the grate beside it.
*   Alone: copper ore around spawn (over 200 blocks), dropping raw copper.
*   The tuff test now also runs both ways (with the owner its tuff is used and ours not registered).

## Not tested

*   Anything visual: models (1.21.11's), the doors' and trapdoors' four stages, grates' cut-out drawing,
    bulbs' lit faces.
*   Sounds playing.
*   Caves & Cliffs' own copper waiting for our bulbs (its table does not know ours until Core has one table).
*   Caves & Cliffs installed but with its copper switched off: it tells Backport Core it provides its blocks
    from its block registry event, which can run after ours; with Forge's mod order that left no copper at
    all in one test with an old jar. Asking it to say so from its constructor or pre-init, as Core's docs say.
*   The Wax On / Wax Off advancements for our copies (1.17's, Caves & Cliffs').
*   Languages: Mojang's names in all 18 files; our config text is still English.
