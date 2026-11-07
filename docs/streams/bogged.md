# Stream 4: bogged (1.21)

Package `bogged` (renderer in `bogged/client`). Config `bogged` (`enabled`, `spawn`). Entity id 10.
Branch `tr-bogged`.

## Changelog section

*   **Bogged**: a mossy skeleton of the swamps, with 16 health. It shoots more slowly than a skeleton
    (every 4.5 seconds), but its arrows poison you for 5 seconds.
*   Shear one for two mushrooms (brown or red); it keeps its bare head afterwards.
*   It drops bones and arrows like a skeleton, and sometimes an Arrow of Poison when a player kills it.
*   Bogged spawn in swamps and in mangrove swamps, in groups of four, taking about a third of the skeleton
    spawns there.

## How it works

*   `EntityBogged` extends vanilla's `AbstractSkeleton`, so everything a skeleton does (bow AI, burning in
    daylight, fleeing wolves, armor and held items) carries over. 1.21's numbers: 16 health; the bow's
    cooldown after the 20-tick draw is 50 ticks on Hard and 70 otherwise (a skeleton's is 20/40), set on the
    skeleton's own bow goal (`aiArrowAttack`, by reflection with its SRG name) every time vanilla rebuilds
    the combat task; arrows carry Poison for 100 ticks, as the stray's carry Slowness.
*   Shearing is Forge's `IShearable`: once, two rolls of brown or red mushroom (1.21's `shearing/bogged`), the
    entity's shear sound; the sheared flag is synced to clients (the mushrooms vanish from the model) and saved.
*   Loot (`loot_tables/entities/bogged.json`): 1.21.11's table: 0-2 arrows and 0-2 bones, Looting +0-1 each;
    0-1 Arrow of Poison when a player kills it, Looting +0-1 to at most 1.
*   Spawning (1.21.11's swamp and mangrove swamp lists): bogged weight 30, groups of 4; the skeleton's weight
    there goes from 100 to 70. Vanilla's swamp and swamp hills, and Wild Update's `mangrove_swamp` when it is
    installed, set in post-init once every mod's biomes exist.
*   Renderer: vanilla's skeleton model with 1.21's six mushroom planes on the head, the skeleton's armor
    layer, and the mossy overlay drawn as the stray's clothing is.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25904), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=532 fail=0`**, **17 blocks, 326 states, 16 item models, 138 sound files, 0 errors**.

*   Registration by name, spawn egg, an `AbstractSkeleton`, 16 health, spawns holding a bow.
*   Bow cooldown 70 on Normal, 50 on Hard, and a plain skeleton keeps 20 or 40.
*   A ranged attack shoots one tipped arrow carrying Poison (id 19) for exactly 100 ticks.
*   Shearing: shearable once, two mushrooms, then not shearable; the sheared flag survives saving.
*   Loot over 40 player kills: arrows and bones each between 0 and 2 (both ends seen); Arrows of Poison
    dropped, never more than one; an unowned kill drops none.
*   Spawn lists: both swamps have bogged at 30 in groups of 4 and skeletons at 70; plains has no bogged and
    its skeletons stay at 100; without Wild Update the list of swamps is the two vanilla ones.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything visual: the model, its mushrooms (written from 1.21's layout), the overlay, the hidden mushrooms
    after shearing.
*   Sounds playing.
*   Natural spawning in a real swamp over time (the spawn lists are checked, not a spawn cycle).
*   Wild Update's mangrove swamp: that mod is not built yet; it is found by its biome name.
*   Dispensers shearing a bogged (1.12.2's dispensers shear only sheep).
*   Languages: Mojang's name in all 18 files; our config text is still English.
