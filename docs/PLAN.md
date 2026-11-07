# Tricky Trials Backport: plan

Everything Java Edition 1.21 (Tricky Trials) and the later 1.21 drops added or changed, through 1.21.11,
grouped into streams, each in its own package, config category and branch. Mod id `trickytrialsbackport`
(permanent). The display name is not final ("Tricky Trials Backport" is taken on CurseForge); it lives
only in `gradle.properties` (`mod_name`).

Sources: minecraft.wiki's *Java Edition 1.21*, *1.21.2*, *1.21.4*, *1.21.5*, *1.21.6*, *1.21.9* and
*1.21.11* pages, current Java behavior winning, and the client jars themselves. What each drop added was
checked by diffing the English language files and blockstate folders of 1.20.6, 1.21, 1.21.1, 1.21.3,
1.21.4, 1.21.5, 1.21.8, 1.21.10 and 1.21.11 (`G:\MCDownloads\minecraft-<version>\client.jar`). Assets and
names come from 1.21.11 (`tools/assets.py`).

Content counts as 1.21 when it first shipped **outside experiments** in a 1.21.x release. The 1.21 blocks
(trial spawner, crafter, tuff and copper sets) were already in 1.20.5's experiment; the pale garden was in
1.21.2's experiment and shipped in 1.21.4; bundles were an experiment since 1.17 and shipped in 1.21.2.

## Scope at a glance

| In | Out (and why) |
| --- | --- |
| Every new block, item, mob, effect, enchantment, potion, painting, disc, biome and structure of 1.21-1.21.11, and their changes to existing things that players see | Data-driven enchantments, item components, dialogs, the server management protocol, test blocks, game rule renames, the mannequin (command-only), and other technical changes |
| Small visible changes to vanilla 1.12.2 things (stream `tweaks`) | Options-menu, music-frequency, cloud and fog rendering changes |
| | 26.1 and later (a new version line; see *Still open*) |

## Content owned by another mod of the family (Rule 1) and its integrations (Rule 2)

| Needed here | Owner | What this mod does |
| --- | --- | --- |
| Copper blocks, cut copper and its stairs and slabs, copper ore, raw copper and copper ingot (1.17) | Caves & Cliffs (`cavesandcliffsbackport`) | Uses the owner's when installed; registers identical copies (same names, states, metadata, oxidation and waxing) when not, as Update Aquatic's `woodset` does. Copper generation is copied with them. |
| Tuff (1.17) | Caves & Cliffs | Same: copy only without the owner, with its ore-style generation. |
| Lightning rod (1.17); 1.21.9 makes it oxidize | Caves & Cliffs | Rule 2: a change to the owner's block. `CORE_REQUESTS.md` #4. |
| Honeycomb for waxing (1.15) | Buzzy Bees | Waxing takes Buzzy Bees' honeycomb, or any `honeycomb` ore-dictionary item; without either, no waxing. Still open #5. |
| Bad Omen, raids, Raid Omen, the ominous bottle (1.14; V&P already has current Java's Raid Omen) | Village & Pillage | Raid Omen needs nothing from us: V&P 2.3 turns Bad Omen into 30 s of Raid Omen in a village. Trial Omen reads V&P's `bad_omen` effect and its ominous bottle (`villagerbackport:ominous_bottle`, metadata 0-4 = I-V); without V&P, copies of both are registered (no raids). |
| Loom, smithing table, composter, lantern, stonecutter (1.14) | Village & Pillage | Banner patterns go through V&P's loom; copper lantern copies its lantern's shape; compost values register with its composter; its stonecutter finds our stairs, slabs and walls by itself. Each optional. |
| Decorated pots and pottery sherds, armor trims and smithing templates, hanging signs, cherry and bamboo wood, camels, wolf armor (1.20) | Trails & Tales | Our flow/guster/scrape sherds and flow/bolt templates are our items; they do something only with Trails & Tales. Pale oak's hanging sign, the camel husk and our shelves for cherry and bamboo exist only when Trails & Tales provides the systems. |
| Mangrove wood and swamps, chest boats (1.19) | Wild Update | Bogged spawn in its mangrove swamps; pale oak chest boat and mangrove shelf only with it. |
| Netherite, chains, crimson and warped wood (1.16) | Nether Update | Netherite spear, nautilus armor and horse armor through Core's `SmithingUpgrades` when netherite exists; the copper chain matches its chain; crimson and warped shelves only with it. |
| Cod, salmon, pufferfish, drowned, nautilus shell (1.13) | Update Aquatic | Salmon sizes (1.21.2), nautilus taming with pufferfish and zombie nautilus riders are Rule 2 integrations with its mobs. |

When an owner is added to a world that has our copies, the owner remaps them (Rule 1, third bullet).
That handler is the owner's or Core's, not ours; `CORE_REQUESTS.md` #2 lists our copies' names.

## Streams

Each stream owns one package under `com.exiledradio.trickytrialsbackport`, one config category with an
`enabled` switch (already in `ModConfig`, written by `tools/common/streams.py`), one block of packet ids
and one block of entity ids. No stream calls another's classes; other streams' items are looked up with
`common/Owners.ours(name)`.

| # | Stream | Version | Package | Config | Entity ids | Packet ids | Needs |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Tuff sets | 1.21 | `tuff` | `tuff` | none | 5-9 | nothing |
| 2 | Copper blocks | 1.21 | `copper` | `copper` | none | 10-14 | nothing |
| 3 | Breeze and wind charges | 1.21 | `breeze` | `breeze` | 1-9 | 15-19 | nothing |
| 4 | Bogged | 1.21 | `bogged` | `bogged` | 10-14 | 20-24 | nothing |
| 5 | Mace, heavy core, enchantments | 1.21 | `mace` | `mace` | none | 25-29 | breeze rod by name |
| 6 | Crafter | 1.21 | `crafter` | `crafter` | none | 30-34 | nothing |
| 7 | Trial spawner, vault, keys, omens | 1.21 | `trial` | `trials` | 15-24 | 35-39 | nothing |
| 8 | Effects and potions | 1.21 | `effects` | `effects` | none | 40-44 | nothing |
| 9 | Paintings, patterns, discs, sherds, templates | 1.21-1.21.7 | `decor` | `decor` | 25-29 | 45-49 | nothing |
| 10 | Trial chambers | 1.21 | `chambers` | `chambers` | none | 50-54 | Backport Core (templates, terrain); Still open #1 |
| 11 | Bundles | 1.21.2 | `bundle` | `bundles` | none | 55-59 | nothing |
| 12 | Pale garden | 1.21.4 | `palegarden` | `paleGarden` | 30-34 | 60-64 | Still open #2 for the biome only |
| 13 | Creaking | 1.21.4 | `creaking` | `creaking` | 35-39 | 65-69 | pale oak by name |
| 14 | Resin | 1.21.4 | `resin` | `resin` | none | 70-74 | nothing |
| 15 | Spring plants | 1.21.5 | `flora` | `flora` | none | 75-79 | nothing |
| 16 | Farm animal variants | 1.21.5 | `animals` | `animals` | 40-44 | 80-84 | nothing |
| 17 | Happy ghast | 1.21.6 | `ghast` | `happyGhast` | 45-49 | 85-89 | nothing |
| 18 | Locator bar | 1.21.6 | `locator` | `locatorBar` | none | 90-94 | nothing |
| 19 | Copper golem | 1.21.9 | `coppergolem` | `copperGolem` | 50-54 | 95-99 | copper blocks by name |
| 20 | Copper tools and items | 1.21.9 | `coppergear` | `copperGear` | none | 100-104 | copper ingot via `Owners` |
| 21 | Shelves | 1.21.9 | `shelf` | `shelves` | none | 105-109 | nothing |
| 22 | Spears and Lunge | 1.21.11 | `spear` | `spears` | 55-59 | 110-114 | nothing |
| 23 | Nautilus | 1.21.11 | `nautilus` | `nautilus` | 60-64 | 115-119 | nothing |
| 24 | Mounts | 1.21.11 | `mounts` | `mounts` | 65-69 | 120-124 | nothing |
| 25 | Smaller changes | all | `tweaks` | `tweaks` | none | 125-129 | nothing |

Ports: the main worktree's dev server uses 25900; a stream worktree uses `25900 + row` for rows 1-19,
and rows 20-25 take the port of a stream already merged (`25900 + row - 19`). Streams are built one at a
time while the machine is shared, so in practice one test server runs at once.

### 1. Tuff sets (1.21)

- Tuff stairs, slab, wall; polished tuff with its stairs, slab, wall; chiseled tuff; tuff bricks with
  stairs, slab, wall; chiseled tuff bricks. 1.21.11's numbers: hardness 1.5, blast resistance 6, tuff's
  sounds for plain tuff, polished tuff's and tuff bricks' own sound sets.
- Crafting as vanilla (4 tuff to 4 polished tuff, 4 polished to 4 bricks, 2 slabs to chiseled), and
  Village & Pillage's stonecutter finds the cuts by itself from these recipes.
- **Tuff** itself is Caves & Cliffs': copied (block and generation in stone below y 16, 1.17's rule) only
  when it is absent.
- Slabs in the family's two-block form (`<name>_slab` + `<name>_double_slab`), as Village & Pillage's.

### 2. Copper blocks (1.21)

- Copper bulb (light 15/12/8/4 by stage when lit, toggles on each rising redstone edge, comparator 15
  when lit), copper door, copper trapdoor, copper grate (see-through), chiseled copper; each in four
  oxidation stages and waxed, 40 blocks. Doors and trapdoors open by hand and by redstone.
- Oxidation by random tick exactly as 1.17's rule (the chance from nearby less-oxidized copper blocks),
  axe scraping a stage off, honeycomb waxing (see the table), lightning striking copper (or a lightning
  rod standing on it) scraping oxidation off it and nearby copper, as 1.17 does.
- Recipes: bulb from 3 copper blocks, a blaze rod and redstone (4 bulbs); grate 4 copper blocks to 4;
  chiseled copper from 2 cut copper slabs; door 6 ingots to 3; trapdoor 4 ingots to 1 (1.21.9's).
- **Copies of Caves & Cliffs' copper** when it is absent: copper ore (stone only; deepslate is theirs),
  raw copper, raw copper block, copper ingot, the four copper blocks, cut copper, cut stairs and slabs,
  waxed forms, smelting and generation. Names and states published in the shared notes folder
  (`tricky-trials-copper-states.md`) so Caves & Cliffs matches them or tells us to match theirs.

### 3. Breeze and wind charges (1.21)

- **Breeze**: 30 HP, hovers and jumps around its target, shoots breeze wind charges, deflects every
  projectile back at its shooter, immune to fall damage, attacks iron golems; drops 1-2 breeze rods on a
  player kill (Looting adds).
- **Wind charge** item (4 from a breeze rod; 0.5 s cooldown) and its projectile: the wind burst knocks
  entities back, flips wooden doors, trapdoors and fence gates, presses buttons, flips levers, rings
  bells, puts out candles (when a candle exists), breaks decorated pots and chorus flowers; self-launch
  fall damage only counts below the burst. The burst lives in `common/WindBurst`, shared with Wind Burst
  and Wind Charged.
- Advancements *Blowback* and *Who Needs Rockets?*; breeze spawn egg.

### 4. Bogged (1.21)

- 16 HP skeleton, shoots Poison arrows (4.5 s between shots), spawns in swamps (and Wild Update's
  mangrove swamps) at half the skeleton rate, also without sky; shears give 1-2 brown and 1-2 red
  mushrooms; drops Arrows of Poison on a player kill. Spawn egg.

### 5. Mace, heavy core and the mace enchantments (1.21)

- **Heavy core** block (in ominous vaults; obsidian-like blast resistance, pickaxe, 15 s).
- **Mace** (heavy core + breeze rod): 6 damage, 0.6 speed, 500 durability, repaired with breeze rods;
  smash attack after falling 1.5 blocks: +4 per block for the first 3, +2 for the next 5, +1 after,
  cancels fall damage and resets fall, knocks back nearby entities (more after 5 blocks).
- **Density** I-V (+0.5 smash damage per block fallen per level), **Breach** I-IV (15% less armor per
  level), **Wind Burst** I-III (launches the wielder up after a smash). Advancements *Over-Overkill*
  and *Heart Transplanter*'s mace parts where they apply.

### 6. Crafter (1.21)

- The crafter block: 3x3 grid with slots that can be switched off, crafts and ejects one result per
  redstone pulse (into a container in front, else dropped), comparator 0-9 by filled or disabled slots,
  remainders ejected. Advancement *Crafters Crafting Crafters*.

### 7. Trial spawner, vault, keys and omens (1.21)

- **Trial spawner**: detects players in range (14 blocks, line of sight), spawns a wave sized by the
  number of players, ejects loot per player, 30-minute cooldown; configured mob per spawner; normal and
  ominous states. **Ominous** when a player with Trial Omen is near: stronger mobs with equipment,
  ominous item spawners dropping potions or arrows above players, the ominous loot table.
- **Vault** and **ominous vault**: one key per player per vault, the 1.21 loot tables, the keyhole's
  states.
- **Trial key** and **ominous trial key**.
- **Trial Omen** (15 minutes per level of Bad Omen, from Bad Omen near a trial spawner), Bad Omen from
  Village & Pillage or our copy, the ominous bottle (V&P's or our copy, I-V).
- Advancements *Minecraft: Trial(s) Edition*, *Under Lock and Key*, *Revaulting*.
- The trial explorer map waits for stream 10.

### 8. Effects and potions (1.21)

- **Weaving** (cobwebs on death, faster through cobwebs), **Oozing** (two slimes on death), **Infested**
  (10% chance to release silverfish when hurt), **Wind Charged** (a wind burst on death); potions brewed
  from an awkward potion with cobweb, slime block, stone and breeze rod; splash, lingering and tipped
  arrows; durations 3:00 / 0:45 lingering / 0:22 arrows.

### 9. Paintings, banner patterns, music discs, sherds and templates (1.21-1.21.7)

- 21 paintings (1.21's twenty and 1.21.7's *Dennis*) as our own painting entity with vanilla's sizes,
  since 1.12.2's painting atlas is fixed.
- Banner patterns *Flow*, *Guster* (1.21), *Field Masoned* and *Bordure Indented* (1.21.2), through
  V&P's loom when present, else 1.12.2's crafting-grid pattern recipes.
- Music discs *Precipice*, *Creator*, *Creator (Music Box)* (1.21), *Tears* (1.21.6, from a ghast killed
  by its own deflected fireball), *Lava Chicken* (1.21.7, from a baby zombie riding a chicken).
- Flow, guster and scrape pottery sherds; flow and bolt armor trim templates (useful with Trails & Tales).

### 10. Trial chambers (1.21; needs Backport Core)

- The structure, its pieces and loot, trial spawners for each mob pool, vaults, the trial explorer map.
  See *Still open* #1 and `CORE_REQUESTS.md` #3.

### 11. Bundles (1.21.2)

- The bundle and sixteen dyed bundles: 64 "weight" of mixed items, the fill bar, the tooltip contents
  grid, putting in and taking out by clicking in inventories (a client mouse hook plus a packet; no
  patching), dropping one stack at a time on use, nesting rules (no shulker boxes, bundles count 4 +
  contents). In village chests (1.21.5).

### 12. Pale garden (1.21.4)

- **Pale oak** set: log, wood, stripped log and wood, planks, stairs, slab, fence, gate, door, trapdoor,
  button, pressure plate, sign, leaves, sapling (2x2 tree), potted sapling, boat; hanging sign and chest
  boat only with their owners' systems (Trails & Tales, Wild Update).
- **Pale moss block** and **carpet** (spreading with bone meal, carpet side walls), **pale hanging moss**,
  **open and closed eyeblossoms** (open at night, close by day, orange and gray dye, poison bees).
- The **pale garden** biome: Still open #2.

### 13. Creaking (1.21.4)

- **Creaking heart** (between two pale oak logs; active at night; dormant by day; spawns and links one
  creaking within 32 blocks; shows the link by particles; drops resin when the creaking is hit; 20-24 XP
  naturally, silk touch only).
- **Creaking**: freezes while a survival player looks at it (a carved pumpkin hides you), 3 damage,
  invulnerable while its heart lives, crumbles when the heart breaks; spawn-egg creakings are standalone
  (1 HP to kill). Advancement *Heart Transplanter* (1.21.6).

### 14. Resin (1.21.4)

- Resin clump (placeable on any face like glow lichen), block of resin, resin bricks (smelted resin
  brick item), stairs, slab, wall, chiseled resin bricks; resin trim material with Trails & Tales.

### 15. Spring plants (1.21.5)

- Bush, firefly bush (firefly particles at light 13 or below), cactus flower (grows on cacti), short and
  tall dry grass (desert ambience), leaf litter and wildflowers (1-4 per block, like pink petals),
  fallen trees (oak, spruce, birch, jungle; mushrooms and vines), sand and terracotta ambient sounds,
  dead bush on farmland. Generation through decoration events, added only in new chunks.

### 16. Farm animal variants (1.21.5)

- Warm and cold cows, pigs and chickens by biome (on vanilla's entities, a saved variant and our
  renderers), blue and brown eggs hatching the matching chicken, eggs and cakes taking them; wolf sound
  variants; sheep colors by biome.

### 17. Happy ghast (1.21.6)

- **Dried ghast** block (rehydrates in water over ~20 minutes, hatching a ghastling), **ghastling**,
  **happy ghast** (20 HP, up to four riders, flies with the first, can be stood on, tempted by snowballs
  and harnesses, heals in rain), **harnesses** (16 colors plus white). Leads that join two mobs, the new
  saddle recipe, shears taking off saddles, horse armor and carpets, carrots for horses.
  Advancement *Stay Hydrated!*.

### 18. Locator bar (1.21.6)

- A client HUD in place of the experience bar showing other players' directions (120 degree cone),
  hidden by sneaking, a carved pumpkin or invisibility; one packet of nearby player positions.

### 19. Copper golem (1.21.9)

- **Copper golem** (carved pumpkin on a copper block; 12 HP; oxidizes; sorts items from copper chests into
  wooden chests within 32 x 8 blocks; turns into a statue when fully oxidized), **copper golem statue**
  (poses, comparator), **copper chest** (four stages, waxed).

### 20. Copper tools and items (1.21.9)

- Copper nugget, sword, axe, pickaxe, shovel, hoe (stone-tier mining), helmet, chestplate, leggings,
  boots, horse armor; copper torch and wall torch, copper lantern, copper bars, copper chain (four stages,
  waxed); copper armor on mobs.

### 21. Shelves (1.21.9)

- Shelves in every wood present (vanilla's six, pale oak with stream 12, the others with their owners):
  3 slots, swap with the hotbar, powered shelves linking up to three, comparator output.

### 22. Spears and Lunge (1.21.11)

- Wood, stone, copper, iron, gold, diamond and netherite spears: jab (quick, several targets) and charge
  (held, speed-based damage, dismounts riders); **Lunge** I-III. Mobs carrying spears. *Mob Kabob*.

### 23. Nautilus (1.21.11)

- **Nautilus** (neutral, tamed with pufferfish, rideable underwater, Breath of the Nautilus for its rider),
  **zombie nautilus** (with a drowned rider when Update Aquatic provides one), nautilus armor in five
  tiers, chest loot.

### 24. Mounts (1.21.11)

- **Camel husk** (32 HP, desert at light 0, husk and parched riders), **parched** (desert skeleton with
  Weakness arrows), zombie horses spawning in plains and savannas (sometimes ridden), netherite horse
  armor (via Core's smithing when netherite exists), horses not sinking while ridden.

### 25. Smaller changes (all drops)

One switch per change, each listed in `docs/streams/tweaks.md`: bats spawning at any height in the dark,
squid babies, wolves eating fish, Nether portal cooldowns for boats and minecarts, ender pearls loading
chunks, mobs preferring their weapons, experience orbs bouncing, water breathing refilling air, no
spawn invulnerability, iron sounds, end portals dropping blocks, lodestone from iron (with Nether
Update's lodestone), and the rest.

## Still open (the user's call)

1. **Trial chambers** need Backport Core's modern structure template loader and its terrain adapters. In
   vanilla the chambers' start sits at y -40 to -20 in a world of -64 to 320. Proposed for 1.12.2's 0-255:
   the same proportion of the way up from bedrock, a start at **y 16-29**, so the whole structure stays in
   y 1-55: under every village and most caves' floors, above bedrock, in diamond depth. With a
   1.18-height mod installed (Core's `WorldHeights`), vanilla's own range. See `CORE_REQUESTS.md` #3.
2. **Where the pale garden generates.** Vanilla makes it a rare variant inside dark forests. Options:
   (a) its own biome entry in Forge's biome list, so it appears as separate patches anywhere warm
   biomes go; (b) turning some roofed forest into pale garden through Forge's biome layer event (no
   patching, but it changes vanilla's biome map in new chunks); (c) no biome: pale oaks and creaking
   hearts generate inside roofed forests. Proposed: (b) behind a switch, with (a) when the switch is off.
3. **26.1 and later** (26.1, 26.2, 26.3 are out) are a new version line the family's ownership table does
   not cover. Do they belong here, or to a new mod?
4. **The display name**, to replace "Tricky Trials Backport".
5. **Waxing without Buzzy Bees**: honeycomb only comes from bees. Proposed: any `honeycomb` ore-dictionary
   item waxes, and with none installed the waxed blocks are craftable but waxing by hand is unavailable.
6. **The locator bar** replaces the experience bar while players are near. Proposed: on, with a switch.

## Shared files and rules

- **`ModConfig.java`**: categories and `enabled` switches come from `tools/common/streams.py`; a stream
  adds fields only between its own markers. Category classes extend `Object`.
- **`NetworkHandler.java`**: packets only with ids from your block, in id order.
- **`TrickyTrialsBackport.java`**: at most one line per lifecycle hook, calling your package.
- **`selftest/SelfTestRunner.java`**: one line in `TESTS`.
- **`en_us.lang`, `sounds.json`**: written only by your generator through `tools/assets.py`
  (`merge_lang` / `merge_sounds`), which replaces your own `# --- <stream> ---` block.
- **`common/`**: read freely; changes are the integrator's (this session).
- **Registration**: from your own `@Mod.EventBusSubscriber` class, through `common/Content`.
- **Owned things**: `Owners.copy(path)` before registering a copy, `Owners.block(path)` /
  `Owners.item(owner, name)` to use one.
- **Languages (Rule 4)**: every user-facing string is an `en_us.lang` key from a generator; vanilla names
  through `assets.name(...)`; config tooltips from `python -I tools/lang/build_langs.py config`; the 18
  languages from `build_langs.py build`, our own text translated in `tools/lang/translations/`.
- **No mixins, no coremod, no ASM.** A part that cannot be done without patching stops and goes to the
  user first.

## Build and test

- `./gradlew build` never deploys unless `ttbDeployDir` is set in `~/.gradle/gradle.properties`.
- `deps/` holds Backport Core's dev jar (not committed): copy `BackportCore/build/libs/BackportCore-1.0.0-dev.jar`.
- Test: `./gradlew runServer -Pselftest=<stream>` (port in `eclipse/server.properties`), then
  `python -I tools/check_assets.py` with 0 errors, then `./gradlew build` last. Check free RAM first
  (8 GB or wait). Never `cleanRunServer`. Kill only our own server, by PID.

## Order

1. Scaffold and this plan: **done**.
2. Blocks, items and mobs of 1.21 first: tuff, copper, breeze, bogged, mace, crafter, trials, effects,
   decor. Then the later drops in version order: bundles, pale garden, creaking, resin, flora, animals,
   happy ghast, copper golem, copper gear, shelves, spears, nautilus, mounts. Tweaks and the locator bar
   alongside.
3. Trial chambers once Backport Core has the template loader and the user has settled the placement.
