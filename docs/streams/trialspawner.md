# Stream 7: trial spawner, vault, keys and omens (1.21)

Package `trial` (effects, particles and renderers in `trial/client`). Config `trials` (`enabled`). Entity id
15 (`ominous_item_spawner`), packet id 35 (`TrialMessage`). Branch `tr-trialspawner`, dev-server port 25907.

Behavior and numbers are 1.21.11's, read from its decompiled code (`TrialSpawner`, `TrialSpawnerState`,
`TrialSpawnerStateData`, `TrialSpawnerConfig`, `PlayerDetector`, `VaultBlockEntity`, `VaultState`,
`VaultServerData`/`SharedData`/`ClientData`, `VaultConfig`, `OminousItemSpawner`, the renderers and
`LevelEventHandler`); its spawner configs and loot tables are vanilla's own files, copied unchanged.

## Changelog section

*   **Trial Spawner**: a spawner that runs a trial. When it sees a player in survival or adventure within 14
    blocks, it spawns a wave of its mob - more for each extra player - and when every mob of the wave is
    dead it ejects loot (a trial key, or food and potions) once for each player who took part, then rests
    for 30 minutes. A spawn egg sets its mob.
*   **Ominous trials**: a player with Bad Omen near a trial spawner turns it into Trial Omen (15 minutes per
    level of Bad Omen), and a player with Trial Omen makes trial spawners ominous: their mobs are tougher
    and wear trial armor and weapons, ominous item spawners drop lingering potions, arrows, fire charges
    and wind charges above players and mobs every 8 seconds, and the loot is the ominous kind, with
    ominous trial keys. After its rest the spawner turns normal again.
*   **Vault** and **ominous vault**: they light up when a player who hasn't opened them comes near, showing
    a turning sample of their loot. A trial key opens a vault, an ominous trial key an ominous vault, once
    per player: the vault ejects its reward an item at a time.
*   **Trial Key** and **Ominous Trial Key**.
*   **Trial Omen** effect. **Bad Omen** and the **Ominous Bottle** (I to V, 100 minutes of Bad Omen) are
    included when no other installed mod adds them.
*   Advancements **Under Lock and Key** (open a vault with a trial key) and **Revaulting** (open an ominous
    vault with an ominous trial key).
*   Trial chambers, which place these blocks, come in a later update; until then trial spawners and vaults
    are creative-only.

## How it works

*   **Trial spawner** (`BlockTrialSpawner`, `TileEntityTrialSpawner`): 1.21's state property
    `trial_spawner_state` (six phases, light 0/4/8) and `ominous`, 12 states in the metadata, so the
    blockstate file is 1.21.11's own. Hardness and blast resistance 50, pickaxe, no drops, not pushable,
    cutout. The tile entity is 1.21's `TrialSpawner` + `TrialSpawnerStateData` + the `TrialSpawnerState`
    machine, line for line: player scans every 20 ticks on the block's phase (`(pos + time) % 20`), line of
    sight from the player's eyes to the block's center, `required_player_range` 14, mobs untracked when dead,
    unloaded or over 47 blocks away, wave size `floor(total_mobs + total_mobs_added_per_player x extra
    players)` and likewise for simultaneous mobs, 40-tick buffer after a new player, 40 ticks before the
    shutter opens, one ejection every 30 ticks per detected player, `target_cooldown_length` 36000.
    Spawning: up to `spawn_range` (4) blocks around, y -1..+1, no block collision, line of sight to the
    spawner, light limits if the config has them, `isNotColliding`, a mob given only by its id is
    finalized as a natural spawn (Forge's special-spawn event first), every mob persistent, equipment from
    the config's loot table with its drop chances. Peaceful or `doMobSpawning false` stops it, as 1.21.
*   **Ominous**: Bad Omen (any level) is replaced by Trial Omen I for 18000 x level ticks with its sound; the
    block turns ominous, current mobs vanish (dropping equipment they picked up), the ominous config takes
    over. While active, every 160 ticks an ominous item spawner goes 2-5 blocks above a random detected
    player or (half the time) one of its mobs, if a clip upward finds room, with an item from the config's
    `items_to_drop_when_ominous`, rolled once per spawner from a seed of the world seed and the spawner's
    30x20x30 cell, as 1.21. After the cooldown it turns back.
*   **Configs** (`SpawnerConfig`, `TrialData`): 1.21's `TrialSpawnerConfig` fields with vanilla's defaults.
    `normal_config`/`ominous_config` in the tile entity's NBT are config names
    (`minecraft:trial_chamber/<pool>/<mob>/normal`, as 1.21.11's registry) or inline objects; the 28 vanilla
    configs (14 mob pools) are read from `assets/trickytrialsbackport/trials/minecraft/trial_spawner/`.
    A file at `config/trickytrialsbackport/trials/<namespace>/trial_spawner/<path>.json` replaces ours or
    adds a new pool, so packs and stream 10 can define chamber pools as data. `configure(normal, ominous)`
    sets them by name. Entity ids in configs are 1.21's (`minecraft:breeze`, `minecraft:bogged` are found
    under this mod's id) and their NBT is passed through (`IsBaby`, slime `Size`).
*   **Loot** (`ModernLoot`, `ModernIds`): 1.21's loot table JSON is rolled by our own reader, because 1.12.2's
    tables reject names that don't exist when they load. Each `minecraft:` name is found as vanilla's, then
    under this mod, then under each family mod (Village & Pillage, Update Aquatic, Buzzy Bees, Nether
    Update, Caves & Cliffs, Wild Update, Trails & Tales); an entry whose item, potion or enchantment is
    missing everywhere leaves its pool, so the rest keep their relative weights. Strong Slowness (1.13) is
    written as its effect. `enchant_with_levels` uses 1.12.2's enchanting-table pick for
    `#on_random_loot`. Armor trims (`set_components`) are skipped. A name with no 1.21 file is rolled as
    1.12.2's own loot table.
*   **Vault** (`BlockVault`, `TileEntityVault`, `VaultSettings`): `facing`, `vault_state` (light 6/12) and
    `ominous`; 32 states, so the metadata holds facing and phase and the tile entity holds `ominous`
    (fixed for a vault's life), shown through the actual state; placing `ominous=true` (as a structure
    does) makes an ominous vault with the ominous key and reward table. Reading NBT keeps that flag when the data has
    no `ominous` (1.21's block entity data, as a structure file's, never has it); and a vault saved as normal with the
    ominous key and table (trial chambers generated before this fix, `tr-chambers-fixes`) is read back as ominous. Its NBT is 1.21's: `config`
    (`loot_table`, `activation_range` 4, `deactivation_range` 4.5, `key_item`,
    `override_loot_table_to_display`; `key_item` also read in 1.21's form with a 1.21 name),
    `server_data` (`rewarded_players`, the last 128; `items_to_eject`; ...), `shared_data` (sent to
    clients: `display_item`, `connected_players`). Players count within range in any mode but spectator,
    no sight needed. State updates every 20 ticks; the display item changes every second while active; a
    key: same item, at least the key's count, unopened by this player, rolls the table with the player's
    luck, 14 ticks unlocking, 20 before the first ejection, one item per 20 ticks, eject pitch
    `0.8 + 0.4 x progress`; failure sounds at most every 15 ticks. Creative players keep the key, as 1.21.
*   **Ominous item spawner** (`EntityOminousItemSpawner`): no physics, invulnerable; after 60-120 ticks
    (warning sound 36 before) it fires a projectile item straight down with its 1.21 dispenser numbers
    (arrows 1.1/6, thrown potions 1.375/3, fire charges as a blaze's, wind charges 1.0/6.67) or drops any
    other item; NBT `item` and `spawn_item_after_ticks`.
*   **Omens** (`Omens`, `TrialEffect`, `ItemOminousBottle`): Bad Omen is Village & Pillage's
    (`villagerbackport:bad_omen`), and its ominous bottle drops from our loot, when it is installed; its
    raids turn Bad Omen into Raid Omen in a village, as 1.21. Without it, `trickytrialsbackport:bad_omen`
    and `trickytrialsbackport:ominous_bottle` (metadata 0-4, 120000 ticks, uncommon, drink sound and
    Bad Omen's sound) are registered; `CORE_REQUESTS.md` #2 already lists them for the owner's remap.
    Trial Omen (`trickytrialsbackport:trial_omen`, neutral, 1.21's color) and our Bad Omen draw 1.21's
    icons.
*   **Client** (`trial/client/TrialFx`): 1.21's level events 3011-3021 arrive as `TrialMessage` (64 blocks,
    as level events) and play their sounds and particles client-side; the spawner's phase particles,
    spin (`speed / (ticks to next spawn + 200)`) and 2% crackle; the vault's idle smoke and flames,
    connection motes to connected players every second, and crackle; the item spawner's streaks.
    Flame, small flame, soul fire flame, detection puffs (5 frames, normal and ominous), Trial Omen,
    ominous spawning and vault connection are our own particles with 1.21's motion, from 1.21.11's sprites
    in the block atlas. Renderers: the spinning mob (as the mob spawner, 1.21's scale), the vault's item
    (0.4 up, 10 degrees a tick), the item spawner's item (grows over 50 ticks, 40 degrees a tick, full
    bright).
*   **Advancements**: `trickytrialsbackport:vault_used` / `ominous_vault_used` (`ItemTrigger`) fire with the
    held item whenever an item is used on an active vault, as 1.21's `item_used_on_block`. Their 1.21
    parent, *Minecraft: Trial(s) Edition* (entering trial chambers), belongs to stream 10, so *Under Lock
    and Key* hangs from Adventure until then.

## For stream 10 (trial chambers)

*   Place `trickytrialsbackport:trial_spawner` and call `TileEntityTrialSpawner.configure(normal, ominous)`
    with the pool's config names, or write 1.21's NBT (`normal_config`/`ominous_config` as strings).
*   Place the vault with `ominous=true` for the ominous vault (or call `TileEntityVault.makeOminous()`);
    a 1.21 `config` compound in the structure's NBT is read as it is.
*   The trial explorer map and *Minecraft: Trial(s) Edition* are stream 10's; reparent
    `adventure/under_lock_and_key` to it then.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25907, fresh world), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=1270 fail=0`** (trials alone: 157), **103 blocks, 1469 states, 104 item models, 396 sound
files, 0 errors**. `./gradlew build --rerun-tasks`: the release jar's classes carry `func_` names.

*   Registration: both blocks, keys, Trial Omen (neutral, 1.21's color), the entity; without Village &
    Pillage our Bad Omen and bottle exist and are the ones looked up; 1.21 names resolve (`trial_key`,
    `ominous_bottle`, `breeze` to ours, `enchanted_golden_apple` to meta 1).
*   Blocks: every spawner state (12) and vault facing/phase (16) survives metadata; light by phase;
    hardness and blast resistance 50; no drops.
*   All 28 vanilla configs load by name; an unknown one is missing.
*   Loot: the key tables give the right key; consumables are always one of their five; a vault reward
    always has items and its ominous bottles are I or II; the ominous drops include arrows and lingering
    potions; melee equipment gives a sword and enchanted armor.
*   A zombie trial, one player: shows the zombie, waits, detects the player in sight on its scan tick, first
    mob 40 ticks later, six zombies in all and three at most at once, cooldown 36000 from the last kill,
    a roll ejected for the player, back to waiting after the cooldown. Two players: eight zombies. NBT round
    trip (configs saved by name, players, cooldown, ranges).
*   Ominous: Bad Omen III turns into 45 minutes of Trial Omen I, the block and config go ominous, the
    trial starts, ominous zombies carry equipment, one to three ominous item spawners in 20 seconds.
*   A spawn egg on an empty spawner: used up, the spawner waits with a skeleton.
*   Vault: lights for a player within 4 blocks and shows loot; a diamond and an ominous key are refused and
    kept; a trial key is taken (one of two), the player remembered, *Under Lock and Key* earned, every
    rolled item ejected one per second, then it goes out; the same player can't open it again; NBT round
    trip. Ominous vault placed as `ominous=true`: looks ominous, refuses a trial key, takes an ominous key,
    *Revaulting* earned, NBT keeps it ominous.
*   Ominous item spawner: delay 60-120, NBT round trip, an arrow fired downward, bread just dropped.
*   Ominous bottle (IV): Bad Omen IV for 120000 ticks, bottle used up.
*   The dedicated server booting proves no client type is reached from server code.

## Not tested

*   Anything visual: models of every state, the spinning mob, the vault's item, particles, effect icons,
    the item spawner's item; sounds playing.
*   With Village & Pillage installed (its Bad Omen and bottle instead of our copies): the dev run has no
    V&P jar; the lookup goes through `Owners.effect`/`Owners.item`, which the copper stream tested for
    Caves & Cliffs.
*   Loot entries that need streams not built yet (Weaving, Oozing, Infested and Wind Charged potions from
    stream 8; banner patterns, music discs and trim templates from stream 9) drop out of their pools for
    now and come in when those streams register them; Slow Falling, the trident, honey bottle and crossbow
    come from their family owners when installed.
