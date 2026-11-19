# Stream 16: farm animal variants (1.21.5)

Package `animals` (client parts in `animals/client`). Config `animals`: `enabled` and six switches, all on by
default (`cowVariants`, `pigVariants`, `chickenVariants`, `coloredEggs`, `sheepColors`, `wolfSoundVariants`).
Entity id 40 (`egg`, the thrown blue or brown egg); ids 41-44 unused. Packet id 80 (`AnimalVariantMessage`); 81-84
unused. Patches P16-P19 (`asm/AnimalsTransformer`, `docs/PATCHES.md`). Branch `claude/tr-animals-b06d87`, dev-server
port 25916.

Behavior is 1.21.11's (`Cow`, `Pig`, `Chicken`, `CowVariant(s)`, `PigVariant(s)`, `ChickenVariant(s)`, `VariantUtils`,
`ThrownEgg`, `EggItem`, `SheepColorSpawnRules`, `Wolf`, `WolfSoundVariants`, and the client's `CowModel`,
`WarmCowModel`, `ColdCowModel`, `PigModel`, `ColdPigModel`, `ChickenModel`, `ColdChickenModel` and their renderers),
decompiled from the 1.21.11 client jar, with the data files `cow_variant/`, `pig_variant/`, `chicken_variant/`,
`wolf_sound_variant/`, `tags/worldgen/biome/spawns_{warm,cold}_variant_farm_animals`, `loot_table/gameplay/chicken_lay`,
`recipe/cake` and `recipe/pumpkin_pie`.

## Changelog section

*   **Warm and cold farm animals**: cows, pigs and chickens now come in temperate, warm and cold variants, picked by
    the biome they spawn in. Warm ones live in deserts, jungles, savannas, badlands, warm and lukewarm oceans,
    mangrove swamps and the Nether; cold ones in snowy biomes, taigas, windswept hills and mountains, cold and frozen
    oceans and the End. Each has its own look: the cold cow is shaggy with long horns, the warm cow has wide horns,
    the cold pig a woolly coat, the cold chicken a fluffy crest and tail.
*   **New cow, pig and chicken models and textures**: every cow, pig and chicken is drawn with 1.21.5's model and
    texture, temperate ones included.
*   **Babies take a parent's variant**: a baby from breeding is the variant of one of its parents, at random.
*   Spawn eggs and `/summon` pick the variant from the biome; `/summon minecraft:cow ~ ~ ~ {variant:"minecraft:cold"}`
    or a spawn egg's `EntityTag` sets it.
*   **Blue and brown eggs**: cold chickens lay blue eggs and warm chickens brown eggs. Thrown, they hatch chicks of
    that variant (one time in eight, sometimes four), and they work in cakes and pumpkin pies. Dispensers throw them.
*   **Sheep colors by biome**: sheep spawning in warm biomes are mostly brown, in cold biomes mostly black.
*   **Wolf sound variants**: every wolf has one of seven voices, classic, puglin, sad, angry, grumpy, big or cute,
    used for its barks, growls, whines, pants, hurt and death sounds. Puppies get a voice of their own.

## How it works

*   **No new mobs.** 1.12.2's `EntityCow`, `EntityPig`, `EntityChicken` and `EntityWolf` stay. What 1.21.5 added lives
    in a capability, `AnimalData` (`trickytrialsbackport:animal`: the farm variant, and a wolf's voice), attached to
    cows (not mooshrooms), pigs, chickens and wolves while `animals.enabled` is on. Forge does not save it: patch P18
    writes vanilla's own keys at the entity's root, `variant` (`minecraft:temperate`, `minecraft:warm`,
    `minecraft:cold`) and on wolves `sound_variant` (`minecraft:classic`, ...), and reads them back, so NBT reads as in
    1.21.5. A missing or unknown id leaves the default (temperate, classic), as vanilla's `readVariant` does, so
    animals from before this mod stay temperate.
*   **Spawning** (P16, 1.21.5's `finalizeSpawn`): before `EntityLiving.onInitialSpawn` returns, a cow, pig or chicken
    takes the variant of the biome at its block (`FarmVariant.forBiome`), and a wolf one of the seven voices evenly.
    1.12.2 calls `onInitialSpawn` where 1.21.5 calls `finalizeSpawn`: natural and generation spawns, spawn eggs,
    `/summon` without NBT, spawners whose entry names only the mob, chicken jockeys. Each kind's switch gates its
    variant; with it off, new animals stay temperate.
*   **Biomes** (`FarmVariant`): vanilla's two tags as sets of registry paths, matched in any namespace, with 1.12.2's
    names for the same biomes (`ice_flats`, `taiga_cold`, `redwood_taiga`, `extreme_hills`, `mesa`, `savanna_rock`,
    the `mutated_` forms, ...) and the 1.13-1.19 names the family's other mods register (`warm_ocean`,
    `mangrove_swamp`, `grove`, `snowy_slopes`, `deep_dark`, `windswept_*`, ...). `#is_nether` and `#is_end` are Forge's
    `NETHER` and `END` biome dictionary types (and their vanilla names). Every other biome is temperate.
*   **Breeding** (Forge's `BabyEntitySpawnEvent`): the baby takes either parent's variant, evenly (1.21.5's
    `getBreedOffspring`); a puppy gets a random voice.
*   **Syncing**: packet 80 sends an animal's variant to a player when they start tracking it and to every tracking
    player when it changes. The client keeps it in the same capability.
*   **Drawing** (`animals/client`): our renderers replace 1.12.2's `RenderCow`, `RenderPig` and `RenderChicken` (each
    only while its switch is on) and extend them, so the pig keeps 1.12.2's saddle layer and the chicken its flapping.
    Each picks its model and texture by variant. `ModelFarmCow` (three models) and `ModelFarmPig` (plain and cold) are
    1.21.5's 64x64 models part for part on 1.12.2's `ModelQuadruped`; `ModelColdChicken` is 1.12.2's chicken (the
    same parts and layout as 1.21.5's) with the cold chicken's two boxes. Babies are drawn as 1.21.5's
    `BABY_TRANSFORMER`s draw them (cow head 8 down and 6 forward, pig 4 and 4, chicken 5 and 2; the head unscaled,
    the rest at half size). The nine textures are 1.21.11's, under our namespace.
*   **Laying** (`LivingUpdateEvent`, 1.21.5's `gameplay/chicken_lay`): 1.12.2's chicken lays on its living update,
    which comes after this event in the same tick. A warm or cold adult chicken whose timer is about to run out lays a
    brown or blue egg (with 1.12.2's egg sound) and restarts the timer at 6000-11999 ticks, so 1.12.2's own egg is not
    laid. Temperate chickens lay 1.12.2's egg.
*   **The eggs** (`ItemColoredEgg`, `EntityColoredEgg`): 1.12.2's egg item (16 to a stack, thrown at 1.5 with its
    sound) and a thrown egg (`egg`, tracked as 1.12.2's: 64 blocks, every 10 ticks) carrying its variant in synced
    data and in NBT. On impact it does 0 damage to what it hits, then one time in eight hatches one chick, or four one
    time in 32 of those, each a baby (age -24000) of the egg's variant; its item's crack particles. Dispensers throw
    them (`BehaviorProjectileDispense`).
*   **Cake and pumpkin pie**: 1.21.5's recipes take `#eggs`; 1.12.2's take its egg, so ours (`trickytrialsbackport:cake`,
    `trickytrialsbackport:pumpkin_pie`) are the same recipes with a blue or brown egg, unlocked in the recipe book by
    holding either egg.
*   **Sheep** (P17, 1.21.5's `SheepColorSpawnRules`): 1.12.2's sheep picks its color after `super.onInitialSpawn`, so
    the patch sets the biome's color before it returns. Warm: gray, light gray and white 5% each, black 3%, else
    brown; cold: light gray, gray and white 5%, brown 3%, else black; one in 500 of the common color is pink.
    Temperate biomes keep 1.12.2's pick, which has the same weights.
*   **Wolf voices** (P19): 1.12.2's mob sounds reach no Forge event that names the entity, so the wolf's
    `getAmbientSound`, `getHurtSound` and `getDeathSound` hand what they return to `AnimalsHooks.wolfSound`, which
    swaps 1.12.2's ambient, death, growl, hurt, pant and whine events for the voice's. Classic is 1.12.2's own sounds;
    the six others are 1.21.11's 36 events and files under our namespace, with 1.21.11's subtitles.

## Tested headlessly

`./gradlew runServer -Pselftest=all` from a fresh world (port 25916), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=3305 fail=0`** (animals alone: 167), **148 blocks, 2475 states, 175 item
models, 850 sound files, 0 errors**. Release jar built after the last test run with `--rerun-tasks`: reobfuscated (`func_` names), 0 `/selftest/` entries.

*   Registration: P16-P19 applied, both eggs (names, stack of 16), the thrown egg entity, the dispenser behavior, the
    capability on cows, pigs, chickens and wolves and not on mooshrooms or sheep, new animals temperate and classic,
    the 36 voice sound events, every setting on by default.
*   Biomes: 14 warm, 18 cold and 14 temperate 1.12.2 biomes classified; variant ids parsed with and without the
    namespace; unknown ids refused.
*   Spawning by biome (the chunk's biome array set to desert, ice plains, plains, savanna, taiga): cows, pigs and
    chickens through `onInitialSpawn`; a mooshroom gets nothing; a spawn egg (`ItemMonsterPlacer.spawnCreature`) picks
    by biome and its `EntityTag` variant overrides; `/summon` without NBT picks by biome, `/summon ... {variant:...}`
    sets it.
*   NBT round trip for each kind and variant through `EntityList.createEntityFromNBT`; unknown and missing ids stay
    temperate; a cow from `{id:"minecraft:cow",variant:"minecraft:warm"}`; mooshrooms and sheep save no `variant`.
*   Breeding: warm x cold calves split about evenly and are never temperate; like parents give like babies; puppies
    get all seven voices.
*   Eggs: cold, warm and temperate hens lay blue, brown and 1.12.2's eggs, and the timer restarts; a chick lays
    nothing; about one blue egg in eight hatches (800 throws), every chick a cold baby; the thrown egg's item and
    saved variant; cake and pumpkin pie with blue, brown and 1.12.2's eggs.
*   Sheep: 20000 picks per climate, 82% the common color and six colors in all; 200 sheep spawned in a desert, ice
    plains and plains biome are mostly brown, black and white.
*   Wolves: 200 spawned wolves get all seven voices; `sound_variant` saved and loaded, an old wolf classic; the hook
    and the patched `getDeathSound` and `getAmbientSound` give a puglin wolf's sounds, a classic wolf 1.12.2's, and
    shaking stays every wolf's.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything drawn (no client): the models, textures, baby proportions, the saddle on a cold pig, the thrown egg.
*   Packet 80 reaching a client (the dev server has no client; the message is sent on tracking and on change).
*   Sounds playing (events registered and their files checked by `check_assets.py`).
*   Natural spawning in generated chunks (the same `onInitialSpawn` path as the tests, not run through
    `WorldEntitySpawner`).

## Left out, and why

*   **The mooshroom's new model**: 1.21.5 draws mooshrooms with the new cow model. Village & Pillage owns the brown
    mooshroom (1.14) and may replace 1.12.2's mooshroom renderer, so ours stays out; mooshrooms keep 1.12.2's look.
*   **Our own text in other languages**: the 18 language files carry 1.21.11's translations of every vanilla name
    (the eggs, the thrown egg, the subtitles). The settings' names and tooltips are English in all of them, as every
    stream's are: the repository has no `tools/lang/translations/` yet.
*   **Item components** (`chicken/variant` on an egg or spawn egg): 1.12.2 has none; the two egg items carry their
    variant, and spawn eggs take `EntityTag`.
*   **Game events** (`ENTITY_PLACE` when laying): sculk sensors are Wild Update's; 1.12.2 has no game events.
*   Entity ids 41-44 and packet ids 81-84 are unused.
