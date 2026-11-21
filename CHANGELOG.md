# 0.1.0 (unreleased)

*   **Open eyeblossoms glow**: at night an open eyeblossom, planted or potted, gives off a dim light level 5, and the
    light goes out when it closes at dawn. Not in Minecraft; turn off **Glowing Eyeblossoms** on the Pale Garden
    settings page for vanilla's unlit flowers.
*   **Fixed: waxing or scraping a copper door broke it with Caves & Cliffs installed.** Backport Core's copper table is
    shared, so Caves & Cliffs' own right-click also took this mod's copper and changed only the clicked half; the other
    half then broke. This mod now handles its own copper first, both halves together.
*   **Fixed: copper doors and the pale oak door vanished in water.** Placed in water, or with water spreading next to
    them, Fluidlogged API's flow replaced them. Now, as in vanilla, a door pushes the water out and water stops at it
    (patch P25).
*   **1.21's changes to existing things** (stream 25, each with its own setting under Smaller Changes): mounts and
    vehicles go through nether and end portals with their riders; ender pearls teleport across dimensions, load the
    chunks around them and leave with their thrower; primed TNT uses nether portals; 1.21.2's portal cooldowns; the
    1.21.6 saddle and lead recipes, leather for saddles in chest loot, shears taking off saddles, horse armor, carpets
    and leads, carrots for horses, leads between mobs that snap at 12 blocks; projectile knockback along the flight
    (dispensers too); axe and shield; 4-8 redstone from witches; no spawn chunks (with Trails & Tales, its radius
    set to 0); no spawn invulnerability; Water Breathing refills air; elytra ending sprints and stopping at ladders;
    Fire and Blast Protection adding up; 1.21's panic rules; fire-immune end crystals; mixed double doors; splash
    potion distance; pumpkins hiding map markers; tool-free drops; saddle Thorns; the End platform dropping what it
    replaces; fire only near players; head names; and the 1.21.5 and 1.21.9 game rules. `docs/streams/tweaks.md`,
    patches P5-P15.
*   **The trial explorer map** (1.21): journeyman cartographers sell it for 12 emeralds and a compass. It points at
    the nearest trial chambers, with 1.21's trial chambers marker and map color. With Village & Pillage Backport, its
    current cartographer table already sells the map; with its 1.14 table, this mod adds it there. Setting
    `chambers.explorerMap` (on). Backport Core 1.4.0 is now required.
*   **Superflat worlds** generate only what 1.12 does for their preset: never trial chambers, and copper ore and tuff
    only with `decoration` (Backport Core 1.3.0's `FlatWorlds`; Core 1.3.0 is now required).
