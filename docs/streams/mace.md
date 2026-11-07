# Stream 5: mace, heavy core and the mace enchantments (1.21)

Package `mace`. Config `mace` (`enabled`). No entity ids or packets. Branch `tr-mace`.

## Changelog section

*   **Mace**: a heavy core on a breeze rod. 6 damage, slow (0.6 attacks a second), 500 durability, repaired
    with breeze rods. Hit something after falling more than 1.5 blocks for a **smash attack**: 4 extra damage
    per block for the first 3 blocks, 2 per block for the next 5, then 1 per block. A smash ends your fall
    (no fall damage) and knocks back everything else close by, harder after 5 blocks.
*   **Heavy core**: a small, very hard block, found in ominous vaults. Mined by hand in 15 seconds.
*   **Density** (I-V): a smash does 0.5 more damage per block fallen per level.
*   **Breach** (I-IV): your target's armor protects 15% less per level.
*   **Wind Burst** (I-III, from vaults only): a smash launches you back up with a gust.
*   Maces also take Smite, Bane of Arthropods, Fire Aspect, Unbreaking, Mending and Curse of Vanishing, at
    the enchanting table and the anvil. Density and Breach don't go with each other or with Sharpness,
    Smite, Bane of Arthropods or Impaling.
*   Advancement **Over-Overkill**: deal 50 hearts of damage in one smash.

## How it works

*   **`ItemMace`**: attack modifiers +5 damage and -3.4 speed over a player's base (6 and 0.6), epic, 500
    durability, enchantability 15, wears 1 per hit and 2 per hard block mined, breeze rods (the breeze
    stream's, by name) repair it.
*   **Smash** (1.21.11's formula): in `LivingHurtEvent` a melee hit with a mace whose wielder has fallen over
    1.5 blocks without gliding gains `4f` (f up to 3), `12 + 2(f-3)` (up to 8), `22 + (f-8)` after, plus
    Density's `0.5 x level x f`, after vanilla's critical hit as 1.21 adds it. Then `hitEntity`: the
    wielder's fall resets and their vertical speed drops to 0.01, the fall afterward counts only below
    this height (`WindBurst.trackImpulse`), the smash sounds (ground, heavy over 5 blocks, air), block dust,
    and 1.21's knockback of every other living thing within 3.5 blocks of the target (not the wielder,
    their team, their tamed pets, marker armor stands or spectators): `(3.5 - distance) x 0.7`, doubled
    over 5 blocks, less knockback resistance, plus 0.7 up.
*   **Wind Burst**: after a smash, a `common/WindBurst` of radius 3.5 at the wielder's feet, knockback
    1.2/1.75/2.2 by level (1.21.11's lookup), which launches the wielder and pushes the rest.
*   **Breach**: 1.21 scales armor's protection by `1 - 0.15 x level`. 1.12.2's armor step can't be told
    that, so the hit is raised (found by halving) until vanilla's own armor step leaves exactly what the
    weaker armor would; Resistance, Protection and absorption then work on it as usual.
*   **Enchantments**: 1.21.11's costs (Density 5+8/level to 25+8, Breach and Wind Burst 15+9 to 65+9),
    weights as 1.12.2 rarities (Density uncommon, the others rare), Wind Burst treasure. A new enchantment
    type covers maces only. The mace's `canApplyAtEnchantingTable` also lets Smite, Bane and Fire Aspect
    through (1.21's tags).
*   **Anvil**: 1.12.2's anvil asks each enchantment, and Smite, Bane and Fire Aspect only fit swords, so
    `AnvilUpdateEvent` works out a mace with a book or a second mace itself: vanilla's merge (equal levels
    go up one, capped), incompatible ones skipped and charged a level, 1.12.2's rarity costs (halved from a
    book), prior work, renaming, and two maces' durability.
*   **Over-Overkill**: `trickytrialsbackport:mace_smash` fires from `LivingDamageEvent` with the damage a
    smash actually dealt; the advancement asks for 100.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25905), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=591 fail=0`** (mace alone: 55), **18 blocks, 327 states, 18 item models, 166 sound
files, 0 errors**.

*   Items: registration, 500 durability, one to a stack, epic, +5 damage and -3.4 speed, repaired with a breeze
    rod and not iron, the recipe. Heavy core: hardness 10, blast resistance 1200, no tool needed, epic item,
    not a full cube.
*   Enchantments: levels, treasure, rarities, enchanting costs, exclusions both ways, Wind Burst and
    Unbreaking compatible with either; the table puts Density, Smite, Bane, Fire Aspect and Unbreaking on a
    mace and not Sharpness, Knockback or Looting; 200 rolls at level 30 offer Density and never Wind Burst.
*   Anvil: a Smite V book on a mace gives Smite V for 5 levels; a Sharpness book gives nothing; Density II
    and II make III.
*   Real attacks by a spawned player through vanilla's attack code, at full charge, on a 1000-health target
    with no armor: a plain hit 6, a 1.4-block fall no smash, a 4-block smash 20 (fall ended, vertical speed
    0.01, fall tracked, a pig 2 blocks off knocked up and away, mace worn), 10 blocks 30, Density III at 4
    blocks 26, Wind Burst I launches the wielder and does nothing without a smash.
*   Breach against full diamond (20 armor, 8 toughness): a plain hit matches vanilla's armor step exactly, and
    Breach IV leaves exactly what armor at 40% would.
*   *Over-Overkill*: a 40-block smash (60) does not earn it; with Density V (160) it does.

## Not tested

*   Anything visual: the mace's held model (1.21's `handheld_mace`), the heavy core model, the dust.
*   Sounds playing.
*   Critical hits combined with a smash (the test hits from the ground so the numbers are exact; 1.21 adds
    the smash after the crit, as this does).
*   The smash's death message: 1.21.2's "was smashed by" needs its own damage type, and 1.12.2 cannot
    change a hit's damage source on the way; smash kills read as ordinary melee kills.
*   Languages: Mojang's names in all 18 files; our config text is still English.

## For the integrator

*   `common/HeightTrigger` became `common/ValueTrigger` (the key it reads is a constructor argument);
    `WindBurst.trackImpulse` is new and public.
*   `tools/assets.py`'s `merge_lang` now drops a line whose key another section already has (shared
    subtitles such as `block.generic.fall`).
*   The advancement's parent is `minecraft:adventure/root` until the trials stream adds
    *Minecraft: Trial(s) Edition*.
