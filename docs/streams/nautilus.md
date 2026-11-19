# Stream 23: the nautilus (1.21.11)

Package `nautilus` (client parts in `nautilus/client`). Config `nautilus` (`enabled`, `naturalSpawn`, `drownedRiders`,
`chestLoot`). Entity ids 60 (`nautilus`) and 61 (`zombie_nautilus`); 62-64 unused. Packet 115 (the inventory key while
riding); 116-119 unused. GUI id 23. No patches. Branch `claude/tr-nautilus-dfdefe`, dev-server port 25904. Generator
`tools/nautilus/generate.py`.

Behavior is 1.21.11's (`AbstractNautilus`, `Nautilus`, `ZombieNautilus`, `NautilusAi`, `ZombieNautilusAi`,
`ZombieNautilusVariants`, `SmoothSwimmingMoveControl`, `SmoothSwimmingLookControl`, `ChargeAttack`, `AnimalPanic`,
`FollowTemptation`, `Drowned.finalizeSpawn`, `MobEffectUtil`, `ArmorMaterials`, `Item.Properties.nautilusArmor`,
`NautilusInventoryMenu`, `AbstractMountInventoryMenu`, `NautilusInventoryScreen`, `NautilusModel` and its armor, saddle
and coral relatives, `NautilusAnimation`, `NautilusRenderer`, `ZombieNautilusRenderer`, `RidingEntitySoundInstance`),
decompiled from the 1.21.11 client jar (`G:\MCDownloads\tr-mech-decomp`, `G:\MCDownloads\tr-nautilus-decomp`); spawn
lists, loot tables and item tags from its data; textures, names and sounds from the same jar.

## Changelog section

*   **Nautilus**: a neutral sea creature of every ocean, 5 to 25 blocks below sea level. It has 15 health, swims
    smoothly, dries out on land, is immune to poison, and now and then hunts pufferfish. Hit it and it charges you.
*   **Taming**: feed a wild nautilus pufferfish (or a bucket of pufferfish): one in three tames it. Tamed nautiluses stay
    near where you leave them, heal on fish, and breed on fish or fish buckets; babies grow faster when fed.
*   **Riding**: use a tamed nautilus to ride it. Put on a saddle to steer: it swims where you look, backwards at half
    speed. Its rider gets **Breath of the Nautilus**, which stops the air bar from running down under water.
*   **The dash**: charge the jump bar and let go to dash where you look (weaker on land), then wait two seconds.
*   **Nautilus armor** in copper, iron, gold, diamond and netherite (4, 5, 7, 11 and 19 armor; diamond and netherite
    add toughness). Found in shipwreck, buried treasure and ocean ruin chests; netherite is upgraded from diamond at a
    smithing table when a netherite mod is installed. Put it on by use, in the nautilus's inventory (sneak and use, or
    the inventory key while riding), or with a dispenser; shears take off the armor, then the saddle.
*   **Zombie nautilus**: an undead nautilus that a drowned with a trident often rides out of the ocean depths. It burns
    in daylight unless it wears armor, never breeds, and can be tamed and ridden too. In warm oceans it is grown over
    with coral. Drops rotten flesh.
*   All of 1.21.11's nautilus, baby nautilus and zombie nautilus sounds, the saddle and armor sounds, and the riding
    sound under water.

## How it works

*   **Entity** (`EntityAbstractNautilus`, an `EntityTameable` and `IJumpingMount`): 0.875 x 0.95, eyes at 0.2751, a baby
    half that; health 15, speed 1 (the zombie 1.1), attack 3, knockback resistance 0.3; breathes under water; never
    pushed by water; its riders are not thrown off under water (Forge's `shouldDismountInWater`); poison refused.
    `canDespawn` is true (1.21.11's `removeWhenFarAway`), and any use, taming or a tamed parent's baby makes it
    persistent. Synced: dashing, saddled, the body armor stack.
*   **Swimming**: 1.21.11's `SmoothSwimmingMoveControl` (`NautilusAi.SwimMoveHelper`: turn 10 degrees a tick, pitch up to
    85, speed x 0.011 in water and 0 out of it, +0.005 up each tick in water) and `SmoothSwimmingLookControl`; 1.12.2's
    `PathNavigateSwimmer`. In water its travel is 1.21.11's `travelInWater`: the input at its speed, moved, then 0.9 of
    the motion kept, with no gravity. Out of water 1.12.2's own travel.
*   **Air** (the nautilus only): out of water it loses one air a tick; at -20 it takes 2 `dryout` damage and its air goes
    back to 0; in water it is 300.
*   **Brain** (1.12.2 goals in 1.21.11's activity order): the charge (FIGHT), panic (core, the nautilus only), breeding
    at 0.4 (`EntityAIMate`), temptation, swimming about. Memories are fields: whom it is angry at (400 ticks, set when
    hurt by anything it may attack), the hunting cooldown (2400-3600 ticks, set on spawn and each time it looks), the
    charge cooldown (80 ticks after each charge), panic (100-120 ticks after a hurt).
    *   `StartAttacking`: an untamed adult in water, not in love, takes the one it is angry at if that one is in water,
        else when the hunting cooldown is over rolls a new one and half the time goes for the nearest visible
        pufferfish (Update Aquatic's `pufferfish`, 1.21.11's `#nautilus_hostiles`) in water within 16.
    *   Charge (`ChargeAttack`): a fixed velocity of 0.6 (the zombie 0.5) toward where the target was, for up to 60
        ticks, looking at it; ends when it has gone 12, the target is 11 away or out of sight, or it is tamed. The first
        living thing it touches (not its rider; armor stands only with mob griefing) takes its attack damage and 1.21's
        extra knockback (`clamp(speed x 1, 0.2, 2) x 2`, Speed and Slowness counted), and the charge ends.
    *   Panic: swims to water within 5 x 4 at 1.6 (1.12.2 has no `LandRandomPos`; water spots stand in, as the panic's
        own target is water anyway).
    *   Temptation: the nearest player within 10 holding nautilus food in either hand, followed at 1.3 (the zombie 0.9)
        to 3.5 blocks (a baby 2.5), 100 ticks of cooldown after; not while panicking or in love.
    *   Swimming about: 1.21.11's `RandomStroll.swim`: with no path, a water block within 10 x 7 (and home), at 1.0.
    *   Home: a tamed nautilus not leashed or ridden keeps within 32 (16 when saddled or a baby), moved when it strays 8
        past.
*   **Food** (`NautilusItems`): taming items are 1.12.2's pufferfish and Update Aquatic's bucket of pufferfish; food is
    1.12.2's raw and cooked fish (cod, salmon, pufferfish, tropical fish) and Update Aquatic's four fish buckets. A
    bucket leaves a water bucket.
*   **Interaction** (1.21.11's `mobInteract`): babies go to 1.12.2's animal interaction (food speeds growth). A tamed
    adult opens its inventory to a sneaking player. Shears (not sneaking, nobody riding) take the armor, else the saddle.
    A wild adult eats a taming item and is tamed one time in three (hearts or smoke). A hurt one eats food and heals 2 x
    its food value (1 for a bucket). A saddle or nautilus armor goes into an empty slot of a tamed adult. Otherwise a
    tamed adult not offered food is ridden; food on a full-health tamed adult starts love.
*   **Equipment**: a two-slot inventory (saddle, armor), saved as `saddle` and `body_armor_item`. The armor's protection,
    toughness and knockback resistance are unsaved attribute modifiers; equip sounds play for changes after the first
    tick (the saddle's underwater sound when its eyes are in water). Dropped on death; shears drop them at the seat's
    height with `item.armor.unequip_nautilus` or the saddle's unequip sound (stream 25's `item.saddle.unequip` event).
*   **Inventory** (`ContainerNautilus`, `GuiNautilus`): 1.21.11's layout (saddle at 8, 18, armor at 8, 36, both only for
    a tamed adult; shift-click as `AbstractMountInventoryMenu`), its background and slot sprites, the nautilus drawn
    following the mouse. Opened by sneak-use, or the inventory key while riding a tamed one (the client cancels the
    player inventory and sends packet 115).
*   **Riding**: one passenger, at 1.21.11's height (the seat 1.1375 up, a player's 0.6 attachment). The first passenger
    steers when it is a player and the nautilus is saddled; the rider's client moves it (1.12.2's vehicle packets, as
    horses), with 1.12.2's client 0.98 slow-down undone. `canBeSteered` is true then: 1.12.2's
    `EntityLiving.canPassengerSteer` asks it too, and without it no side moved the nautilus (fixed 2026-11-20, the
    ghast's same miss). While a player steers, its goals, its swim move control and its look control stand down (1.21's
    `updateControlFlags`), so nothing of the AI turns or lifts it. Input: strafe as pressed; forward along the look (down and up
    included), back mirrored at half. Speed 0.0325 x speed in water (top about 0.29 a tick), 0.02 on land. Each tick
    it turns half way to the rider's yaw and takes half their pitch, body and head with it.
*   **Dash** (1.12.2's `IJumpingMount`, the horse's jump bar): a saddled nautilus shows the bar; letting go at power p
    queues `p >= 90 ? 1 : 0.4 + 0.4 p / 90` unless the cooldown runs; the next ridden tick adds the rider's look x 1.2
    (0.5 on land) x that x speed, and starts 40 ticks of cooldown. The server hears it through 1.12.2's riding-jump
    packet: the dash sound and the synced dash flag, which starts the cooldown on every side; the flag drops after 5
    ticks, and the end of the cooldown plays the dash-ready sound.
*   **Breath of the Nautilus** (`PotionBreath`, color `#00FFEE`, 1.21.11's 18 x 18 icon): a player riding a nautilus
    gets 60 ticks of it (ambient) when missing and every 40 game ticks. With its eyes in water and no other way to
    breathe (water breathing, Update Aquatic's Conduit Power), the air neither drops nor refills: a player's air is
    put back at the end of its tick if it dropped; another mob's at the start of its next.
*   **Zombie nautilus** (`EntityZombieNautilus`): undead, never a baby, never breeds, a monster for the caps. Its variant
    (`minecraft:temperate` / `minecraft:warm` in NBT, as 1.21.11 names them) is warm in any mod's `warm_ocean` biome.
    1.21.11's `burnUndead` with the body slot as sun protection: daytime, brightness over 0.5, the usual roll, dry, sky
    above its eyes: 8 seconds of fire unless it wears armor. A mob riding it steers it in 1.21.11 (the rider's
    navigation is its mount's); here it follows its rider's attack target.
*   **Drowned riders** (`drownedRiders`, with Update Aquatic): a drowned that spawns naturally (not from a spawner) is
    marked, and on its first tick, if it is an adult holding Update Aquatic's trident outside a river (1.21.11's
    `#more_frequent_drowned_spawns`), half the time it gets a fresh zombie nautilus under it.
*   **Spawning** (`naturalSpawn`): a water creature, weight 5 in 1.12.2's ocean and deep ocean (and Update Aquatic's warm
    and lukewarm oceans), 2 in cold and frozen ones (Forge's `COLD` or `SNOWY`), groups of 1, placed in water; 1.21.11's
    rule: 5 to 25 below sea level with water below and above; obstruction checked against entities only.
*   **Loot**: the zombie nautilus 0-3 rotten flesh for a player kill, Looting +0-1 each level. The nautilus's table is
    empty in its file: Update Aquatic's shell is added in code when it exists (5% for a player kill, +1% each Looting
    level, the same numbers as 1.21.11's), since a 1.12.2 table naming a missing item fails to load. `chestLoot` adds
    1.21.11's pool (nothing 148, copper 20, iron 10, gold 5, diamond 2) to Update Aquatic's buried treasure, three
    shipwreck and two ocean ruin chests.
*   **Netherite nautilus armor**: `SmithingUpgrades.add(diamond nautilus armor, netherite)`, active only when Backport
    Core finds a netherite mod.
*   **Model** (`ModelNautilus`): 1.21.11's adult (128 x 128) and baby (64 x 64) meshes, the armor layer (shell +0.01),
    the saddle layer (the shell's top +0.2) and the coral mesh; the head turns up to 10 degrees with the look; the swim
    animation is `NautilusAnimation.SWIMMING` as a walk animation. Its scale channels need part scaling, which 1.12.2's
    `ModelRenderer` lacks: `ModelNautilus.Part` adds it. Shadow 0.7 (half for a baby). Corals are hidden under armor.
*   **Riding sound**: 1.21.11's `RidingEntitySoundInstance` for `entity.nautilus.riding`: looping, everywhere at once,
    heard only with the nautilus under water, 5 x its speed up to full.
*   Spawn eggs: the two commonest colors of each 1.21.11 egg texture.

## Tested headlessly

`./gradlew runServer -Pselftest=nautilus` (port 25904): **`SELFTEST DONE pass=252 fail=0`**. `-Pselftest=all` from a fresh
world: **`SELFTEST DONE pass=5405 fail=0`**; `python -I tools/check_assets.py`: **197 blocks, 3048 states, 288 item models,
1299 sound files, 0 errors**. Release jar built last with `--rerun-tasks`: reobfuscated, no `/selftest/` entries and no
test classes. Two other `selftest=all` runs on this branch failed only the ghast test's 7 still-timeout checks, the
flaky test the mounts stream found failing on `main` as well (the ghast runs before this stream's test, and nothing here
touches ghasts); a fourth run started on a half-deleted world and is not counted.

*   Registration: both entities (names, ids 60 and 61, eggs), the five armors (stack 1, no durability, 4/5/7/11/19),
    the effect (name, color, not harmful), the sounds, the zombie loot table; size 0.875 x 0.95, eyes 0.2751, a baby
    half; health 15, speed 1, attack 3, knockback resistance 0.3; breathing, riders kept under water, poison refused;
    swim navigation, move and look controls.
*   Spawning: the depth rule (5 and 25 below allowed, 4 and 26 not, no water above or below not), placement in water,
    the water-creature cap type (not when persistent), the zombie as a monster; weights 5 in ocean and deep ocean, 2 in
    frozen ocean, none on plains or rivers, groups of 1.
*   Without Update Aquatic: pufferfish tames and cod does not, fish are food, no bucket is, no shell, no rider ever
    gets a mount. With Update Aquatic's items stood in: the pufferfish bucket tames (and leaves a water bucket each
    time), a fish bucket heals 1 and leaves a water bucket, the shell is its; 200 drowned stand-ins with the trident get
    a zombie nautilus about half the time (seated at the right height, facing their way, mob controlled, not steering,
    not leashable), a baby never.
*   Taming: a wild adult refuses cod (kept), a saddle and riding; any use makes it persistent; pufferfish tame it, one
    eaten per try, owned by the player; creative keeps the pufferfish; 300 tries tame about a third; a hurt tamed one
    heals 4 from a cod.
*   Equipment: a saddle and armor by use (one each, a second saddle mounts the player instead, as in 1.21.11); iron 5
    armor; netherite 19, toughness 3, knockback resistance +0.1, gone again on diamond (11, 2); saved and loaded with
    owner and armor; a real player sneaking opens the inventory (1.21.11's slot places, what each slot takes, 38 slots,
    nothing for a wild one) and does not mount; shears take the armor, then the saddle, damage the shears and drop
    both, and do nothing while ridden; a dispenser equips armor only into an empty slot of a tamed one; both drop on
    death.
*   Riding: used, a tamed one carries one player; no saddle no steering and no dash; food does not mount; the seat
    height; the ridden input (forward, up, back at half mirrored, strafe); speed 0.0325 in water, the first tick and the
    top speed (about 0.29), no gravity; half the rider's yaw and pitch, body and head following; the dash (full and
    half charges, the look x 1.2, the 40-tick cooldown with no dash during it, the flag dropping after 5 ticks, the
    server hearing it); 0.02 out of water; the server zeroing its motion under a player; packet 115's handler opening
    the ridden nautilus's inventory.
*   Breath of the Nautilus: given to the rider for 60 ticks, ambient, level I; not refreshed between beats, refreshed
    on the 40-tick beat; air held under water for a player across a tick, not with water breathing or without the
    effect; a pig with the effect keeps its air over 20 ticks.
*   Breeding: fish make tamed adults fall in love (one each), the baby is tamed to the owner (a wild parent's is wild),
    no mating with a zombie nautilus, fish speed a baby up, a baby takes no saddle.
*   Behavior: temptation (fish in either hand, not a stick, not from 11 blocks); hurt by a player: panic, anger for 400
    ticks, not leashable, it goes for the player in the water, not a creative player, not once tamed; the charge (0.6
    straight at the target, 3 damage on touch, ends on the hit, 80-tick cooldown, target forgotten, no charge during
    the cooldown, a target 12 away too far); the 2400-3600 hunting cooldown and its new roll; a water spot to flee to;
    drying out (299, then 2 damage at -20); full air in water; home 32, 16 saddled.
*   Zombie nautilus: speed 1.1, 15 health, undead, never a baby, never breeds; temperate and warm by biome name,
    saved as `minecraft:warm` and loaded; tamed with pufferfish and saddled; burns in daylight, not with armor, not at
    night; a zombie put on a fresh one rides it.
*   Loot: the zombie nautilus 0-3 rotten flesh and nothing else over 200 player kills, nothing without a player; the
    nautilus's shell pool only with the shell, about 5% over 4000 player kills, none without a player; the armor pool in
    all six of Update Aquatic's chest names and not in a dungeon; 18,500 rolls of a shipwreck supply chest give
    copper, iron, gold and diamond at 20/10/5/2 of 185, never netherite.
*   Steering (added with the 2026-11-20 fix): a player that reports itself as the local user (`isUser`, as a client's
    own player does) riding a saddled nautilus: unsaddled no one steers; saddled it is steerable and the rider's side
    steers, with its swim, tempt and panic goals standing down; 40 ordinary ticks of forward looking 30 degrees up and
    40 to the side move it over 6 blocks along the rider's look (cosine above 0.999), under water all the way, facing
    the rider's yaw with half their pitch; it then turns half way to a new yaw in one tick; a dash in the same tick
    surges over a block along the look and starts the cooldown; on land the same rider crawls forward (0.3 to 3 blocks
    in 40 ticks); a dedicated server's own (remote) rider does not move it on the server.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything drawn or heard (no client): the models, layers, animation, inventory screen, the effect icon, the riding
    sound, the bubbles.
*   A real client steering it: the input, the jump bar, the vehicle-move packets and the server's checks. The 2026-11-20
    fix (`canBeSteered`) was found by a real client ("won't let me control it") and needs a look in game: swimming
    along the look, the dash on the jump bar, the server not rubber-banding the rider, and getting off under water. The ridden
    swim and the dash are tested on the server through the methods the client calls; packet 115's work is tested by
    calling its handler's method.
*   Update Aquatic itself: it is not in the test run. Its items are stood in by vanilla items through
    `NautilusItems.uaItem` (pufferfish and fish buckets, the shell, the trident), and drowned by zombies holding the
    stand-in trident; the real lookups are tested to give nothing without it.
*   A natural spawn by the world's spawner over time (the rule, the lists, the placement and the cap type are tested).
*   The brain over time (wandering, following, breeding to a baby, a full charge run): each goal is tested by single
    ticks.
*   The netherite upgrade (no netherite mod in the test run).

## Left out, and why

*   **Drowned in ocean ruins** (1.21.11's `STRUCTURE` reason, which also makes their mounts persistent): Update
    Aquatic's ruin pieces create their drowned without a spawn event this stream can tell apart, so only natural spawns
    get mounts.
*   **The drowned's own steering**: 1.21.11 hands the rider's navigation to its mount. Here the zombie nautilus follows
    its rider's attack target; how the rider attacks from it is Update Aquatic's drowned AI.
*   **The parrot's zombie nautilus imitation**: parrots' imitation table is 1.12.2's fixed map (Update Aquatic has its
    own for its mobs).
*   **The dash cooldown on the jump bar**: 1.12.2's bar shows only the charge; a dash during the cooldown is ignored.
*   **Game events** (Wild Update's).
*   **The nautilus shell without Update Aquatic**: it is not in Core's `ItemOwnership` table, so no copy is registered
    (`CORE_REQUESTS.md` #13); without Update Aquatic the nautilus drops nothing.
*   The other 17 languages get the vanilla names from `build_langs.py build`; the three setting names are English
    everywhere, as for the other streams.
*   Entity ids 62-64 and packets 116-119 are unused.

## What stream 24 (mounts) could share

The riding code stays in this package. Shareable later, through `common/` (the integrator's): the ridden-travel shape
(`travel` / `travelRidden` / `tickRidden`, 1.12.2's 0.98 client slow-down undone, the server zeroing motion under a
player), the 1.21.6 shears-take-off-equipment order (body, then saddle; not while ridden), equipping by use into an
empty slot, the "inventory key opens the mount's inventory while riding" client hook with its packet, and a single
`RidingEntitySoundInstance` (the ghast and the nautilus each have one).
