# Stream 17: the happy ghast (1.21.6)

Package `ghast` (client parts in `ghast/client`). Config `happyGhast` (`enabled`, `riderCameraDistance`). Entity id 45
(`happy_ghast`, the ghastling is its baby); ids 46-49 unused. No packets: 85-89 unused (a rider's client steers, as
1.12.2's horses and boats do). Branch `claude/tr-ghast-98b5ae`, dev-server port 25917. Generator `tools/ghast/generate.py`.

Behavior is 1.21.11's (`HappyGhast`, `HappyGhastAi`, `DriedGhastBlock`, `Ghast.GhastMoveControl`,
`Ghast.RandomFloatAroundGoal`, `TemptGoal.ForNonPathfinders`, `FlyingMoveControl`, `HappyGhastModel`,
`HappyGhastHarnessModel`, `HappyGhastRenderer`, `RidingEntitySoundInstance`, the `Equippable.harness` component and
`Entity.attemptToShearEquipment`), decompiled from the 1.21.11 client jar; assets, names and sounds from the same jar.

## Changelog section

*   **Dried ghast**: crafted from 8 ghast tears around soul sand. Place it in water and it slowly rehydrates through
    three stages, then hatches a **ghastling** (about 20 minutes). Out of water it dries out again. It breaks at
    once, sounds dry and smokes on soul sand, and bubbles happily in water.
*   **Ghastling**: a tiny, friendly ghast that floats after nearby players and friendly mobs, follows anyone holding
    a snowball and panics when hurt. It grows up into a happy ghast in 20 minutes; each snowball you feed it takes
    10% off the time left. It can breathe underwater.
*   **Happy ghast**: a big, gentle ghast with 20 health that floats around (never too close to blocks), follows
    snowballs and harnesses, and heals slowly, quickly in rain, snow or the clouds.
*   **Harnesses** in all sixteen colors: 3 leather over glass, wool and glass; dye any harness another color. Put
    one on a happy ghast by hand or with a dispenser; take it off with shears (not while anyone rides).
*   **Riding**: a harnessed happy ghast carries up to four players. The first one steers: forward and back go where
    they look (back at half speed), strafing works, and jump climbs. The goggles drop when someone gets on.
    Players get off on its top. While a player stands on top it stops and squares up, so it makes a platform.
*   The riding sound, and a camera 8 blocks back in third person while riding (setting `riderCameraDistance`).
*   **Advancement *Stay Hydrated!***: place a dried ghast into water.

## How it works

*   **Dried ghast** (`BlockDriedGhast`): `facing` and `hydration` 0-3 fill the 16 metadata values; the water is
    Fluidlogged API's through Backport Core's `Waterloggable`, so there is no `waterlogged` property (Core 1.1.0 and
    later require Fluidlogged API). 10 x 10 x 10 pixels, hardness 0, gray, pushable, never a path node, the cutout
    layer. Faces away from the placer. A random tick schedules a tick 5000 ticks ahead while it holds water or is
    above stage 0; that tick hydrates one stage in water (the transition sound) or dries one stage out of water; a
    stage-3 block in water is replaced by its water and hatches a baby happy ghast at its bottom center, facing its
    way (the spawn sound). 1.21's `hasScheduledTick` check is not needed: 1.12.2's scheduler keeps one tick per block
    and position, and its `isBlockTickPending` only sees ticks being run that tick. Sounds: 1.21's sound type (break,
    step, fall; place and hit are `intentionally_empty`), and the block's own place sound, dry or in water; water
    poured in later plays the soak sound (Core's `onWaterChanged`). Client: the dry ambient sound on soul sand or
    Nether Update's soul soil, the wet one in water; 1.21's white smoke is 1.12.2's white cloud particle, the wet
    sparkles are happy-villager particles as in 1.21.
*   **Entity** (`EntityHappyGhast`, an `EntityAnimal`): 20 health, flying and movement speed 0.05, follow range 16,
    4 x 4 with eyes at 2.6, a baby 0.2375 of that; no fall damage, no ladders, no breeding (snowballs are its food,
    but it never falls in love), no experience, an empty loot table; neutral sounds at volume 4 (a ghastling's at 1),
    pitch 1, ambient sounds six times rarer while ridden. Synced: the harness (`body_armor_item` in NBT), the
    still flag and the leash-holder flag; `still_timeout` saved.
*   **Ages**: when the baby flag changes, the server swaps the controls as 1.21's `ageBoundaryReached` does: an adult
    gets the ghast move control, its look control and goals (float 3, tempt 4, float around 5); a ghastling a
    flying move control, a flying navigation and its brain (`GhastAi.Brain`).
*   **Adult movement** (`GhastAi.MoveHelper`): every 2-6 ticks a push of flying speed x 5/3 toward the wanted spot if
    the destination box, grown by a block, holds no solid block, fluid or avoided block (cactus, magma, fire, and
    the family's sweet berry bush, wither rose and pointed dripstone), and the way there (swept in quarter blocks)
    is clear; else it gives up. On its still timeout it stops in place. Travel: input x 0.0833, friction 0.91 (0.8
    in water, 0.5 in lava), no gravity. `RandomFloatAroundGoal`: a new spot within 16 on each axis when it has none,
    reached it or is 60 away, chosen within home and near blocks (an air spot with a block within 15 in some
    direction), lowered when above the terrain. Home: 64 blocks unharnessed, 32 harnessed or as a ghastling, moved
    when it strays 16 past; cleared when the last rider gets off. Look control: squares up to the nearest quarter
    turn on its still timeout, faces what it was told to look at for 2 ticks, else faces the way it moves.
*   **Tempt**: within 16 blocks, line of sight not needed; snowballs, and harnesses for an unharnessed adult. The
    adult flies at a random point on the line to the player's eyes, stops within 7, and calms down for 100 ticks.
*   **Ghastling brain**: swim; panic for 100-120 ticks after a hurt from outside (fly to random spots within 5 x 4 at
    2x); look at its look target 45-90 ticks; then in order: follow a snowball holder at 1.25 to 3 blocks (100-tick
    cooldown after), follow the nearest visible player, else the nearest visible adult of 1.21's
    `followable_friendly_mobs` (by registry name, vanilla's and the family's), from 3 to 16 blocks at 1.1, else fly to
    a random spot within 10 at 1.0 (up to 90 degrees from its look), or to its look target. Its move control turns
    up to 90 degrees a tick and climbs and sinks at its speed.
*   **Healing**: 1 health every 600 ticks, every 20 in rain or snow (raining, open to the sky, a biome with
    precipitation) or in the clouds (the overworld's y 128 to 132, 1.12.2's cloud height).
*   **Harness** (`ItemHarness`, 16 items, stack 1): used on an adult, living ghast with none, it takes one off the
    stack (creative keeps it, as 1.12.2's interaction restores the count) with the equip sound; a dispenser puts one
    on a ghast in front, else drops it. Shears take it off when no one rides and the player is not sneaking: dropped
    4 blocks up (the middle of the seats), the unequip sound, 1 shears damage. Dropped on death. Crafting:
    `LLL`/`G#G` (leather, glass, wool of the color, group `harness`); dyeing: a dye ore item and any harness of
    another color (`harness_dye`); recipe advancements as 1.21.6's (a dried ghast, the dye).
*   **Riding**: using a harnessed adult without sneaking mounts it (after the held item's own interaction, so name
    tags, leads and harnesses still work); four seats, 1.21's attachments at 4 up and 1.7 front, left, back and
    right, turned with the body; a player sits 0.6 lower (1.21's player vehicle attachment). The first passenger
    steers when it is a player, the ghast is harnessed and not on its still timeout. Input (`riddenInput`): strafe
    as pressed; forward or back (only the sign counts) along the look, back mirrored at half; jump +0.5 up; x 3.9 x
    flying speed, flown with the travel above: top speed about 3.6 blocks a second. Each tick it turns 8% of the way
    to the rider's yaw and takes half their pitch. Goggles down when the first rider gets on, up when the last
    leaves; the rider dismounts under water. Getting off puts the rider on the middle of its top the next tick (1.21's
    dismount location; 1.12.2 would look for ground nearby) and starts a 10-tick still timeout.
*   **Still timeout**: a player within a block of its sides and up to half its height above its top (not riding a
    happy ghast) holds it at 10 ticks; it counts down after its first 60 ticks. While on it the ghast stops and
    squares up, no one steers, and it is solid (a boat-like collision box); on a client it is also solid to a player
    at or above its top, and a ridden ghast bumps into other ridden ghasts. A rider getting on clears it unless
    someone stands on top.
*   **Client steering**: the rider's client moves the ghast and sends 1.12.2's vehicle-move packet, as for horses
    and boats; the server zeroes its own motion under a player. 1.12.2 slows a client-moved entity by 0.98 before it
    travels; that is undone, since 1.21 does not. The rider's jump key is read from 1.12.2's `isJumping` (synced to
    the server by the input packet).
*   **Floating kick** (`GhastContent.onPlayerTick`): 1.12.2 kicks a player whose vehicle floats 80 ticks with flight
    off; 1.21 exempts flying vehicles. For riders of an adult happy ghast the server player's `vehicleFloating` and
    its count are cleared each player tick (reflection on `NetHandlerPlayServer`, no patch).
*   **Leash**: held by a lead, it is pulled only past 10 blocks and the lead snaps past 16 (1.21's elastic and snap
    distances), and it never walks to its holder. A non-llama horse leashed to it makes it a leash holder (1.21.6's
    quad leash flag, synced).
*   **Model** (`ModelHappyGhast`): 1.21's 64 x 64 body and nine tentacles swaying as the ghast's, drawn 4 times larger
    about the feet (0.95 for the ghastling), the body at 0.9375 inside a harness. The harness layer
    (`ModelHappyGhastHarness`): harness and goggles, the goggles raised and tipped 45 degrees until someone rides.
    Shadow 2 (scaled for the ghastling); culled with a box reaching half its height below its feet.
*   **Riding sound** (`GhastClient`): looping, without distance, 5 x the ghast's speed (up to 1), silent under water.
*   **Camera** (`riderCameraDistance`): in third person while riding an adult, the camera is pushed back from 1.12.2's
    4 blocks to 8, clipped against blocks by the same eight rays `orientCamera` uses (Forge's `CameraSetup`).
*   **Stay Hydrated!** (`husbandry/place_dried_ghast_in_water`, our trigger `dried_ghast_placed_in_water` from Forge's
    place event when the placed block holds water): 1.12.2's `placed_block` cannot see Fluidlogged API's water.
*   Spawn egg: 1.21.6's egg is a texture; its two commonest colors, white `#F0F0F0` and gray `#736D6D`.
*   Sounds: the 20 events of the happy ghast, ghastling and dried ghast, files and subtitles from 1.21.11.

## Tested headlessly

`./gradlew runServer -Pselftest=all` from a fresh world (port 25917), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=3399 fail=0`** (ghast alone: 260), **149 blocks, 2491 states, 190 item models, 845 sound
files, 0 errors**. Release jar built after the last test run with `--rerun-tasks`: reobfuscated (`func_` names), no
`/selftest/` entries and no test classes.

*   Registration: block, 16 harnesses with their colors, stack size and dispense behavior, entity id 45, egg, all 20
    sounds, the loot table, attributes, adult size and eyes, the block's metadata both ways, hardness 0.
*   Crafting: the dried ghast, all 16 harnesses, all 16 dye recipes, and no recipe for dyeing a harness its own color.
*   Rehydration: placed with water; a random tick schedules exactly one tick 5000 ahead; stages 1, 2, 3 in water,
    the water kept; then the block is gone, its water stays and one ghastling (age -24000) hatches at the bottom
    center facing east, 0.95 wide, breathing underwater. Out of water it dries one stage per tick down to 0, and a
    dry stage-0 block schedules nothing.
*   A real player placing it into a water source: the block keeps the water, faces the player, and grants
    *Stay Hydrated!*; a dry placement does not.
*   Growth: a snowball takes 10% off (-24000 to -21600, then -19440) and is used up; grown up it is 4 wide with the
    adult's controls and goals; an adult ignores snowballs and never falls in love.
*   Harness: none on a ghastling; put on by a player's use, used up; no second one; saved and loaded; shears do
    nothing while sneaking or ridden (using them on a ridden ghast mounts you, as in 1.21); off with shears, dropped
    at the seats' height, shears damaged 1; a dispenser puts one on.
*   Riding: no harness, no ride; four players get on, a fifth does not fit; the first steers; seat positions front
    and left, turning with the body; sneaking does not mount; a rider getting off starts the 10-tick still timeout,
    is put on top the next tick, and no one steers meanwhile.
*   Flight: the ridden input for forward, back (half), looking up, back while looking down, jump and strafe; one
    tick of ridden flight moves input x 0.0833 then keeps 0.91; the terminal speed; no gravity; 8% turning and half
    pitch with body and head following; the server does not steer for a real player and zeroes its motion.
*   Still timeout: a player on top sets 10, the ghast is solid, stopped and squared to a quarter turn (30 to 0, 50 to
    90); still for 10 ticks after they step off, then free; no countdown in the first 60 ticks.
*   Healing: 1 per 600 ticks, not on other beats, every 20 in the clouds; above the clouds not.
*   Floating: a rider's 79-tick floating count is cleared; a player off the ghast is left alone.
*   Adult AI: snowballs tempt, harnesses tempt only an unharnessed adult, not beyond 16 blocks; careful flight
    reaches open air, not a spot within a block of a wall nor through it; home radius 64 and 32; wander spots
    within 16.
*   Leash: no pull within 10, pulled and still held at 13, snapped past 16.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything drawn or heard (no client): the model, the harness and goggles, the scale and culling, the particles,
    the riding sound, the camera distance.
*   A real client steering it: input, the vehicle-move packets and the server's "moved wrongly" checks. The
    flight math is tested on the server through the same method the client calls.
*   The ghastling's brain over time (following players and mobs, panicking, strolling): set up and run only as far
    as the selftest's single ticks go.
*   The real 20-minute hatch through random ticks (each step is tested by calling the ticks).

## Left out, and why

*   **The ropes** on a leash-holding ghast and the four-point lead from 1.21.6's quad leash: 1.12.2 draws one lead
    from the holder's center; the flag is synced for a later renderer.
*   **Game events** (block change, entity actions): Wild Update's; 1.12.2 has none.
*   **"Distance by Happy Ghast"** statistic: 1.12.2's statistics are a fixed list.
*   **White smoke**: 1.12.2 has no such particle; the white cloud particle stands in.
*   **The ghastling's exact pathfinding**: 1.12.2's flying navigation stands in for 1.21's, with the same targets.
*   **Leashed elastic physics**: 1.21.6's spring is 1.12.2's pull formula past 10 blocks.
*   The other 17 languages get the vanilla names from `build_langs.py build` (which also filled earlier streams'
    names); our own text (the camera setting) is English everywhere, as there is no `tools/lang/translations/` yet.
*   Packet ids 85-89 and entity ids 46-49 are unused. Tears (the disc) is stream 9's.
