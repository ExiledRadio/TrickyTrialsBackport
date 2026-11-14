# Stream 3: breeze and wind charges (1.21)

Package `breeze` (client classes in `breeze/client`); the burst is shared code in `common/WindBurst`.
Config `breeze` (`enabled`). Entity ids 1-3; packet 0 (common's `GustMessage`). Branch `tr-breeze`.

## Changelog section

*   **Breeze**: a wind spirit with 30 health. It leaps around its target, shoots wind charges at players and
    iron golems within 16 blocks, takes no fall damage, and sends arrows, snowballs, fireballs and every
    other projectile straight back. Killed by a player, it drops one or two **breeze rods**
    (Looting adds more). Breezes come from trial spawners; use the spawn egg to test them for now.
*   **Wind charge**: four from a breeze rod. Throw it (half a second between throws) or shoot it from a
    dispenser. Its burst knocks everything nearby back without hurting anything: it launches you if it
    bursts at your feet, and a fall after the launch only hurts for the part below where it burst. It
    opens and closes wooden doors, trapdoors and fence gates (not iron ones, and not ones held by
    redstone), presses buttons, flips levers and breaks chorus flowers. A direct hit does one damage.
*   Hit a wind charge in flight to send it the way you are looking.
*   Mobs from trial spawners don't turn on each other when a breeze's charge catches them.
*   Advancements **Blowback** (kill a breeze with its own deflected charge) and **Who Needs Rockets?**
    (land at least 7 blocks above or below where a wind charge launched you).

## How it works

*   **`common/WindBurst`** is 1.21's wind explosion without the explosion: vanilla's 1,352 rays find the
    blocks it reaches (as 1.21's wind charge calculator, they fade with distance and only barrier and
    bedrock stop them; corrected in stream 2), and vanilla's
    knockback rule pushes entities within twice the radius by (1 - distance / (2 radius)) times their
    exposure times the burst's multiplier, less Blast Protection. Spectators and flying creative players
    are left alone; breezes ignore both kinds of charge. A player it pushes is remembered until landing:
    `LivingFallEvent` cuts the fall to the part below the burst, and landing fires the
    `trickytrialsbackport:fall_after_wind_charge` trigger with the height gained. Each two-block door is
    flipped once per burst. Gusts are drawn on clients from one `GustMessage`: 1.21's gust and small-gust
    frames stitched into the block atlas.
*   **Numbers** (1.21.11): player charge radius 1.2, knockback 1.22, speed 1.5, cooldown 10 ticks,
    dispenser speed 1.0 with 6.67 spread; breeze charge radius 3, knockback 1, speed 0.7; one damage on a
    direct hit (1.21's `wind_charge` damage: message "mob", a projectile). No gravity and no air drag;
    slowed in water; bursts if it climbs 30 blocks over the build limit.
*   **Breeze**: 30 health, speed 0.63, follow range 24, 10 experience, never despawns. The fight is 1.21.11's
    `BreezeAi` (`BreezeAI.Fight`, rewritten on `tr-chambers-fixes`): each tick `Shoot`, then `LongJump`, then `Slide`,
    with the brain's memories as timers (one tick longer than the expiry, as `Brain.forgetOutdatedMemories` erases
    before it counts down). **Long jump**: from the ground (or water) with four free blocks above and the target more
    than 4 blocks away: a 10-tick inhale, then a jump to the surface 4-8 blocks behind where the target's head faces
    (`randomPointBehindTarget`, `snapToSurface`, line of sight to it or 4 above), at the first of the shuffled angles
    40, 55, 60, 75, 80 that reaches it under 1.4 blocks a tick (`LongJumpUtil`, scaled 0.95), flying without drag
    (`discardFriction`: `travel` adds gravity and nothing else). Landing: 10 ticks of jump cooldown (2 when hurt in the
    last 40 ticks) and a 100-tick `BREEZE_SHOOT` window. **Shoot**: only in that window, under 16 blocks, standing:
    15 ticks charging, one charge, 4 ticks recovering, then 10 ticks of cooldown and the window closes. **Slide**: when
    it can neither: to a spot away from a close target, behind it, or in the 4-8 block ring, at 0.6; a 60-tick shoot
    window when it ends. So with a target 4-16 blocks away it jumps, shoots once, and jumps again: 10-12 jumps in 30
    seconds in the self-test arena (the old goals jumped every 2-5 s only when the target was out of 3-8 blocks).
*   **Deflection**: `ProjectileImpactEvent` on a breeze is cancelled and the projectile reversed at half
    speed and turned 170-190 degrees (1.21's `REVERSE`), put back where it was a tick earlier so it does
    not hit again. Wind charges are not deflected.
*   **No anger**: 1.21's `no_anger_from_wind_charge` list (breeze, skeleton, stray, bogged, zombie, husk,
    spider, cave spider, slime). Hit by a mob's wind charge, they forget that mob on their next tick.
*   **Loot** (`loot_tables/entities/breeze.json`): 1.21's table, `killed_by_player`, 1-2 rods, Looting +1-2
    per level.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25903), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=482 fail=0`** (breeze alone: 57), **17 blocks, 326 states, 16 item models, 124 sound
files, 0 errors**.

*   Registration: both items, all three entities by name, the breeze's spawn egg, the recipe (one rod, four
    charges), the dispenser behavior, the trigger, both advancements loaded.
*   Breeze: 30 health, no fall damage, an NBT round trip; 30 player kills drop between 1 and 2 rods, an
    unowned kill drops none.
*   Charge flight: exactly 1.5 blocks a tick for 10 ticks, no gravity; a direct hit takes one health from a
    pig and the charge is gone; hitting a breeze's charge sends it the hitter's way at its own speed and
    makes the hitter its owner.
*   Blocks: an oak door, trapdoor and fence gate open; an iron door does not; a button is pressed and a
    lever flipped; a chorus flower breaks; the floor stays; a door held by redstone is not flipped; a
    second burst closes the door again.
*   Knockback: a pig beside the burst flies off; a pig behind obsidian and one past twice the radius do not
    move; a breeze does not move; a breeze charge pushes a pig 4 blocks away.
*   Falls: a burst at a real player's feet launches them; landing above the burst counts no fall, landing
    below counts only the part below; landing 7.5 blocks up grants *Who Needs Rockets?* and forgets the launch.
*   Deflection: an arrow fired into a breeze comes back and does no harm. A zombie hit by a breeze's charge
    forgets the breeze; hit by a player's, it turns on the player.
*   AI, 300 ticks against a target 12 blocks away: the breeze long-jumped (rose 4.9 blocks), shot 5
    charges, ended 3.9 blocks from its target, unhurt.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything visual: the breeze model and its wind and eyes layers, the wind charge model, the gust
    particles. Both models are written from 1.21's part layout and need a look in game.
*   Sounds playing.
*   *Blowback* granted by a real kill (the kill and the trigger's conditions are vanilla's; only the
    advancement file is ours).
*   Bells, candles and decorated pots in a burst: they belong to other mods of the family, and the burst
    finds them by name; Village & Pillage's bell does not ring yet.
*   The breeze's shot spread uses a guess (`max(1, 6 - 2 x difficulty)`); 1.21's exact figure was not
    available.
*   Languages: Mojang's names for everything new are in all 18 files; our config text is still English.

## For the integrator

*   New shared code: `common/WindBurst` (with the fall rule and the trigger), `common/HeightTrigger`,
    `common/GustMessage` (packet 0), `common/client/ClientGust` and `ParticleGust`; one line each in the mod
    class (trigger registration, `BreezeClient.init`) and `ClientSetup` (`ClientGust.init`).
*   The advancements' parent is `minecraft:adventure/root` until the trials stream adds 1.21's
    *Minecraft: Trial(s) Edition*, which should become their parent.
