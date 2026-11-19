# Stream 24: mounts (1.21.11)

Package `mounts` (client parts in `mounts/client`). Config `mounts` (`enabled`, `zombieHorses`, `zombieHorseSpawn`,
`parchedSpawn`, `netheriteHorseArmor`, `floatWhileRidden`, `mountedReach`). Entity id 65 (`parched`); 66 is kept for the
camel husk; 67-69 unused. No packets: 120-124 unused. Patch P24a (`asm/ZombieHorseTransformer`, docs/PATCHES.md).
Branch `claude/tr-mounts-e9c6eb`, dev-server port 25905. Generator `tools/mounts/generate.py`.

Behavior is 1.21.11's (`ZombieHorse`, `AbstractHorse`, `RunAroundLikeCrazyGoal`, `Husk.finalizeSpawn`, `Parched`,
`CamelHusk`, `LivingEntity.floatInWaterWhileRidden`, `Mob.getAttackBoundingBox`, `Mob.burnUndead`, `ArmorMaterials`,
`SkeletonModel.createSingleModelDualBodyLayer`, `ParchedRenderer`), decompiled from the 1.21.11 client jar
(`G:\MCDownloads\tr-mech-decomp`, `G:\MCDownloads\tr-mounts-decomp`); spawn lists, loot tables and item tags from its
data; textures, names and sounds from the same jar.

## Changelog section

*   **Zombie horses** now spawn in plains and savannas in the dark, each ridden by a zombie. With spears installed, the
    zombie holds an iron spear: a **zombie horseman**.
*   **Zombie horses can be tamed** by riding them, like horses. Red mushrooms tempt and heal them. Once tamed they take
    a saddle and **horse armor**, and can be ridden. They have 25 health, and their speed and jump vary from horse to
    horse.
*   Zombie horses **burn in daylight** unless they wear horse armor, and wild ones despawn like monsters. They drop 2-3
    rotten flesh; a zombie riding one may drop a red mushroom.
*   A zombie riding a mount can now reach you: its reach is as wide as its mount.
*   **Parched**: a desert skeleton under the open sky that shoots arrows of Weakness, more slowly than a skeleton. It
    has 16 health, does not burn in daylight, and Weakness does not affect it. It takes half the desert's skeleton
    spawns.
*   **Netherite horse armor**: 19 armor, 3 toughness and knockback resistance. Upgrade diamond horse armor with a
    netherite ingot at a smithing table (needs a mod that adds netherite).
*   Horses, donkeys, mules and camels no longer sink in deep water while ridden.

## How it works

*   **Zombie horse** (1.12.2's own entity, `ZombieHorses`, behind `zombieHorses`):
    *   On its first join the server sets 25 health (a full horse stays full) and rolls 1.21.11's speed
        `(9 + 3 random) / 42.16` and jump strength `0.5 + 3 random / 15`, once (Forge entity data `rolled`). Only a
        speed of exactly 1.12.2's 0.2 is rolled again, so a zombie horse from an older world gets the new spread once.
    *   Goals: 1.12.2's horse goals, with the crazy run swapped for one that skips a mob-controlled horse (1.21.11's
        `RunAroundLikeCrazyGoal.canUse`; a mob rider is never thrown), and a red mushroom tempt at 1.25 x speed, priority
        3.
    *   Interaction (P24a, `MountsHooks.zombieHorseInteract`): any use makes it persistent; a red mushroom heals 3 and
        adds 3 temper (1.21.11's `handleEating`), used up only if it did something; any other item on an untamed one
        makes it mad. Tamed: sneaking opens the inventory; a saddle or horse armor goes straight on (1.21.11 equips by
        use; the sounds are 1.12.2's horse saddle and armor sounds); otherwise the player gets on. Untamed with an empty
        hand the player gets on too and is tamed by temper, as on a horse. Spawn eggs keep 1.12.2's handling.
    *   Armor: P24a makes `wearsArmor` true and `isArmor` take any horse armor, so 1.12.2's horse inventory shows the
        armor slot. Each server tick the armor in slot 1 is mirrored to the horse's chest equipment slot (synced to
        clients by 1.12.2's equipment packets, drop chance 0 since the horse's chest drops it), and gives the armor
        value as a horse's does (`HorseArmor.apply`). The client draws the main model again in the armor's texture
        (1.12.2's horses draw armor as a texture layer on the same model).
    *   Sun: each server tick 1.21.11's `burnUndead` (daytime, brightness over 0.5, the 1-in-30 roll, sky above its
        eyes, not wet): set on fire for 8 seconds unless it wears armor (1.12.2's horse armor has no durability).
    *   Despawning: P24a's `canDespawn` is true while untamed and not leashed, so 1.12.2's `despawnEntity` treats it as
        a monster (instantly beyond 128 blocks, sometimes beyond 32 after 30 seconds idle); a player riding it is at
        distance 0.
    *   Loot: `entities/zombie_horse` is replaced on load: 2-3 rotten flesh, Looting +0-1 each level. A plain zombie
        riding a zombie horse adds 0-1 red mushroom, Looting rounded `level x random` (1.21.11's `entities/zombie`
        pool with the vehicle predicate), when mob loot is on.
*   **Zombie horse spawning** (`zombieHorseSpawn`): weight 5, groups of 1, in 1.12.2's plains, sunflower plains,
    savanna, savanna plateau and both shattered savannas (1.21.11's plains, sunflower plains, savanna, savanna plateau
    and windswept savanna), in the monster list. A natural spawn (not a spawner) is checked by 1.12.2's monster rules
    in `CheckSpawn` (not Peaceful, the sky-light and light rolls of `EntityMob.isValidLightLevel`, a block it may spawn
    on, not colliding): allowed or denied outright, since `EntityAnimal`'s own rule wants grass and daylight. A natural
    spawn is marked in `SpecialSpawn` and gets its rider on its first tick: a zombie with its own spawn roll (a chicken
    it would have ridden is removed or freed: 1.21.11 creates it as a jockey), holding stream 22's iron spear when
    `trickytrialsbackport:iron_spear` exists, then made to ride.
*   **Mounted reach** (`mountedReach`): every zombie (husks and zombie villagers too) has 1.12.2's `EntityAIZombieAttack`
    swapped on join for one that, while riding, hits when 1.21.11's attack box (its own box widened to the vehicle's and
    reaching down to the vehicle's feet, grown by `sqrt(2.04) - 0.6` sideways) touches the target's box. On foot it is
    1.12.2's reach. Needed because 1.12.2 measures from the rider's feet, a block up.
*   **Floating while ridden** (`floatWhileRidden`): a ridden horse, zombie horse, donkey, mule, Trails & Tales camel or
    camel husk (1.21.11's `#can_float_while_ridden`) in water deeper than 0.4 at its feet (the column's water surface,
    from 1.12.2's liquid level) gets +0.04 motion up at the start of its tick, which is where 1.21.11's addition after
    the previous tick's water move lands. Runs on both sides, since a player's client moves its mount.
*   **Parched** (`EntityParched`, an `AbstractSkeleton`): 16 health; bow cooldown 50 ticks on Hard, 70 otherwise (set
    after 1.12.2's `setCombatTask`, as the bogged); tipped arrows with Weakness for 600 ticks; `isPotionApplicable`
    refuses Weakness. No sunburn: 1.12.2's `AbstractSkeleton.onLivingUpdate` sets the fire before anything else, so a
    flag is up from the start of the parched's `onLivingUpdate` until `updateArmSwingProgress` (the next call
    `EntityMob` makes), and `setFire` is ignored only while it is up; fire from lava, fire blocks or Fire Aspect still
    burns. Sounds: 1.21.11's four parched events. Loot (`entities/parched`): 0-2 arrows, 0-2 bones, Looting +0-1 each,
    and with a player kill 0-1 arrow of Weakness, Looting +0-1, at most 1.
*   **Parched spawning** (`parchedSpawn`): weight 50, groups of 4, in desert, desert hills and the desert M, and the
    skeleton's weight there lowered from 100 to 50 (1.21.11's desert list). A natural spawn needs the sky above
    (`checkSurfaceMonstersSpawnRules`); the light rule is 1.12.2's, as for the bogged.
*   **Parched model** (`ModelParched`): 1.21.11's 64 x 64 dual-body skeleton (each part has its wrap box in the lower
    half), the arms at 1.21.11's 5.5 out; skeleton armor layers and held-item offset as the bogged's.
*   **Netherite horse armor** (`HorseArmor`, `netheriteHorseArmor`): a Forge `HorseArmorType` with protection 19, one to
    a stack. 3 toughness and 0.1 knockback resistance (1.21.11's netherite `ArmorMaterial` for the body) are attribute
    modifiers added each server tick to a horse whose armor type is netherite, and to a zombie horse wearing it, and
    taken off otherwise. Recipe: `SmithingUpgrades.add(diamond horse armor, any netherite ingot)`, so the upgrade exists
    only when Backport Core finds a netherite mod. Worn texture: 1.12.2's diamond horse armor recolored by 1.21.11's own
    diamond-to-netherite colors (19 shades), as the copper gear stream made copper's; the item texture is 1.21.11's.
*   **Spears**: looked up by name each time (`MountsContent.lookup`, `Owners.ours`), so the stream builds and runs with or
    without stream 22.

## Tested headlessly

`./gradlew runServer -Pselftest=mounts` (port 25905): **`SELFTEST DONE pass=374 fail=0`**. `-Pselftest=all` from a
fresh world: **`SELFTEST DONE pass=4662 fail=0`**; `python -I tools/check_assets.py`: **181 blocks, 2888 states, 236 item
models, 1040 sound files, 0 errors**. Release jar built last with `--rerun-tasks`: reobfuscated, no `/selftest/`
entries.

An earlier full run failed 7 checks in the ghast test's still timeout (and 1 random cobweb count in effects). The ghast
checks fail the same way on `main` without this stream (1 of 3 runs of `-Pselftest=ghast` at ad9c1d8): a flaky test,
not this stream's.

*   Registration: the parched (name, id 65, egg, loot table), netherite horse armor (stack 1, its armor type,
    protection 19), P24a applied, the spear lookup agreeing with whether stream 22's iron spear exists.
*   Parched: skeleton, 16 health, a bow, cooldowns 70 and 50, an arrow carrying exactly Weakness for 600 ticks; Weakness
    refused, Slowness taken; in full daylight a skeleton beside it burns and the parched does not; the guard is down
    after the tick; `setFire` from elsewhere still burns it.
*   Zombie horse: 25 health at full, speed and jump in 1.21.11's ranges, the tempt and the crazy-run goals, armor slot
    and armor check (a saddle is not armor), despawning untamed and not tamed; saved and loaded keeps the rolled speed
    and health; a skeleton horse unchanged (15 health, no armor slot).
*   Taming: a red mushroom heals 3, adds 3 temper and is used; a full, fully tempered horse refuses it and keeps it;
    a saddle on an untamed horse is taken but not put on; an empty hand mounts it untamed; the crazy run starts with a
    player on and, at full temper, tames it to that player; with a zombie on it never starts.
*   Equipment: a saddle and iron horse armor go on by use and are used up; the armor shows as chest equipment and gives
    5 armor; netherite gives 19 armor, 3 toughness, 0.1 knockback resistance, on a zombie horse and on a horse, and
    changing a horse back to diamond takes the extras off; armor out, nothing shown and 0 armor; the shown copy's drop
    chance is 0. A real player sneaking opens the horse inventory, whose armor slot takes horse armor and not an
    apple; not sneaking, the tamed saddled horse is ridden.
*   Sun: burns in daylight, not with armor, not at night.
*   Spawning: parched 50 x 4 and skeletons 50 in the three deserts; zombie horses 5 x 1 in the six plains and savannas,
    not in forests; monster rules pass in a sealed dark box every time, `CheckSpawn` allows there, denies on Peaceful
    and beside glowstone, leaves spawners alone; a parched without sky is denied, under the sky left to the usual rules.
*   Riders: a natural spawn is marked and gets a zombie rider on its first tick; with no spear the zombie rides with its
    own gear; with a stand-in for stream 22's iron spear it holds it; the real lookup in this build (stream 22 absent:
    no spear); a spawn-egg horse comes alone; 40 riders all ride, and no jockey chicken is left behind.
*   Drops: 200 rolls of the zombie horse table give only rotten flesh, 2 to 3; a riding zombie drops a red mushroom
    about half the time, one at most without Looting, a zombie on foot none.
*   Floating: three-deep water is over the threshold; a ridden horse in it rises 0.04, unridden not, a ridden skeleton
    horse not; a shallow pool's depth is measured as such.
*   Reach: zombies get the mounted attack; on a zombie horse a pig 1.9 blocks from the horse's middle is in reach and
    one at 3 is not; on foot the reach is 1.12.2's.

## Not tested

*   Anything drawn or heard (no client): the parched model and texture, the zombie horse's armor layer, the netherite
    armor texture on a horse, the parched's sounds.
*   A real natural spawn by the world's spawner over time (each step is tested: the lists, `CheckSpawn`,
    `SpecialSpawn`, the first tick).
*   A real client riding through deep water (the float runs on both sides; tested on the server).
*   The netherite smithing upgrade itself: no netherite mod in the test run, so Core's upgrade table is inactive.
*   Stream 22's real spear on a rider (stream 22 is not on this branch); a stand-in item checks the path.
*   Despawning over time (the `canDespawn` answer is tested, 1.12.2's `despawnEntity` does the rest).

## Left out, and why

*   **The camel husk** (32 health, light 0 in deserts, ridden by a husk with a spear in front and a parched behind,
    rabbit-foot food, the husk's rabbit-foot drop, the camel husk saddle). It is a `Camel`, and camels are Trails &
    Tales' (PLAN.md's owner table: "the camel husk ... exist only when Trails & Tales provides the systems"), and no
    family mod compiles against another (each builds with only Core's dev jar; the integrator, 2026-11-19), so it
    cannot subclass Trails & Tales' `EntityCamel`. A camel of our own on `AbstractHorse` would re-port that whole
    camel (about 3,300 lines: brain, keyframe animations, sitting, dash, two seats) as a copy of an owned mob, which is
    not worth it for one variant. Entity id 66 stays reserved, and the float list already names it. A desert husk
    never brings a camel husk or a parched with it (parched still spawn on their own).
*   **The darker zombie horse texture**: 1.21.11's is the 64 x 64 layout of 1.14's horse model; 1.12.2's horse is
    128 x 128 (the same reason Backport Core's texture packs leave horses out).
*   **Hostile mob cap**: 1.12.2 counts monsters by `IMob`, which the zombie horse is not, so it does not count towards
    the monster cap (and counts towards the animal cap as an `EntityAnimal`). Changing that needs a patch to the
    vanilla class hierarchy check.
*   **Spears on zombies, husks and piglins on foot**: stream 22's (only the riders' spear is this stream's).
*   **Carrots on tamed zombie horses**: stream 25's `horsesEatCarrots` still feeds them (1.21.6's list); 1.21.11's
    zombie horse eats only red mushrooms. Left to stream 25.
*   **Advancements** (*Monster Hunter*, *Best Friends Forever*): 1.12.2's *Monster Hunter* lists its own mobs; taming
    a zombie horse already counts for *Best Friends Forever* through 1.12.2's `tame_animal` trigger.
*   Netherite horse armor is not made fire-proof as an item (1.21's netherite items float on lava); 1.12.2 has no such
    item property, and Nether Update owns netherite's.
*   Diamond horse armor's 1.21 toughness of 2 (1.12.2 gives 0): a change to a vanilla item, for stream 25 if wanted.
*   The quad-leash offsets and the zombie horse's charge speed modifier (spear charging is stream 22's).
