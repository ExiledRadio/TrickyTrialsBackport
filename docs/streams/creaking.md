# Stream 13: the creaking (1.21.4)

Package `creaking` (client parts in `creaking/client`, the keyframe animation code in `creaking/client/anim`).
Config `creaking` (`enabled`). Entity id 35 (`creaking`); ids 36-39 unused. Packet id 65
(`CreakingFxMessage`); 66-69 unused. Branch `tr-creaking`, dev-server port 25915.

Behavior is 1.21.11's (`Creaking`, `CreakingAi`, `CreakingHeartBlockEntity`, `CreakingModel`, `CreakingAnimation`,
`CreakingRenderer`, `TrailParticle`), decompiled from the 1.21.11 client jar.

## Changelog section

*   **Creaking**: a tall, wooden mob that moves only while no one is looking at it. When a player looks at it
    from within 12 blocks it wakes, its eyes glow, and it hunts that player, freezing in place whenever
    someone looks at it. A carved pumpkin on your head keeps an awake creaking from freezing. It hits for 3.
*   **Creaking hearts spawn creakings**: an awake heart (at night, between its two pale oak logs) spawns a
    creaking within 16 blocks when a player comes within 32, on any difficulty but Peaceful. The heart and its creaking are linked: a comparator by the heart reads 15 with the creaking at
    the heart, down to 0 at 32 blocks. The creaking crumbles at dawn, when it strays more than 34 blocks
    from its heart, or when a player stands inside it.
*   **A heart's creaking can't be hurt**: hitting it only makes it sway, and the heart sends streaks of
    particles to it and gives off a sound for five seconds; with resin installed, resin grows on the logs
    around an awake heart. Break the heart and the creaking twitches, its eyes flickering, then crumbles.
*   Creakings from spawn eggs have no heart and die to one hit.
*   **Advancement *Heart Transplanter***: place a creaking heart between two pale oak logs lined up with it
    (or the log that completes one).

## How it works

*   **Entity** (`EntityCreaking`): 1 health, speed 0.4, 3 damage, follow range 32, step height 1.0625, 0.9 x
    2.7 with eyes at 2.3, no experience, no loot. Synced `CAN_MOVE`, `IS_ACTIVE`, `IS_TEARING_DOWN`,
    `HOME_POS`; `home_pos` saved. While frozen its move, look and jump helpers, its body turning and its
    navigation do nothing, and knockback and pushing are ignored. Fire immunity follows `HOME_POS` (1.12.2's
    `isImmuneToFire()` is final; the field is set on both sides). A heart's creaking never uses a portal.
    The walk animation speed is 1.21's (25 times the distance, up to 3), set in `travel` after 1.12.2's.
*   **Freezing** (`checkCanMove`, every tick on the server): over the players the sensor found within 32
    (every 20 ticks), each one who can be attacked and is not on its team: if the creaking is asleep, or the
    player wears no carved pumpkin, and the player looks at it (`isLookingAtMe(0.5)`: the look within 60
    degrees of any of three points, its eyes, 0.5 above its feet and between, with a clear line), it freezes;
    asleep and within 12 blocks it also wakes on that player. No attackable player left: it sleeps. The line
    of sight is 1.21's visual clip: glass, stained glass, panes and iron bars (empty visual shapes) do not
    block it; 128 blocks at most.
*   **AI** (`CreakingBrain`): 1.21.11's brain as fields and code, ticked from `updateAITasks`; nothing is put in
    `tasks`. Sensors every 20 ticks from a random start (players within the follow range; living things within
    16, visible ones by 1.21's targeting conditions, with sneaking and invisibility scaling the range). Core:
    swim (0.8), look at the look target 45-90 ticks, walk to the walk target. Idle: an active creaking takes
    the nearest attackable player; a look target within 8 blocks every 30-60 ticks; RunOne (vanilla's weighted
    shuffle) of stroll at 0.3, walk to the look target at 0.3, or wait 30-60 ticks. Fight (only while it can
    move): walk at full speed to a target out of reach, attack in reach (box grown by sqrt(2.04) - 0.6
    across) every 40 ticks, drop a target that is no longer a visible attackable player or out of reach for
    200 ticks. Its navigation passes doors, may swim, and blocks path nodes more than 32 blocks from home
    unless they bring the creaking nearer; a heart's creaking costs 8 for lava and damaging blocks, 0 for fire.
*   **Invulnerability** (`attackEntityFrom`): with a home, only `OUT_OF_WORLD` (the void, `/kill`) does damage.
    Anything else, when a living or projectile hit it or a player is behind it, plays the 8-tick invulnerable
    animation (entity event 66) and the sway sound, and, when a player is behind it, calls the heart.
    `blameSourceForDamage` sets the revenge target and the attacking player as a real hit would.
*   **The heart's side** (`CreakingHeartHook`, reached by name from stream 12's `TileCreakingHeart`): the link
    (the creaking's UUID under `creaking` in the heart's `Creaking` compound, looked up by UUID with a
    30-tick grace after loading), spawning (every 20-24 ticks while awake: `SpawnUtil.trySpawnMob` with 5
    tries, 16 across, 8 up and down, standing on a full top that is not leaves, with a free box), the
    comparator, the despawn checks, the hurt call (20 gray trails from the creaking to the heart, then for
    100 ticks one orange and one gray a tick while above 50, and the heart's hurt sound every 10 ticks moving
    from the creaking to the heart, louder at first), and removal. Vanilla's unsaved fields are kept per tile
    in a weak map.
*   **Resin**: the hurt call runs stream 14's `com.exiledradio.trickytrialsbackport.resin.ResinSpread
    .creakingHurt(TileEntity)` by name; without resin nothing grows.
*   **Death**: the heart broken by a player or an explosion (stream 12's `removed` hook) kills the creaking
    with that damage (kill credit, the twitch sound), marks it tearing down, and after 45 ticks it crumbles
    (100 pale oak wood and 10 awake heart crumble particles, the death sound) without the usual death. Its
    heart gone any other way, or no longer naming it, it dies an ordinary death on its next tick. Dawn, the
    34-block leash and a player inside it crumble it at once.
*   **Particles** (`CreakingFxMessage`, packet 65): `trail` (a particle that glides to its target and arrives
    as it dies, full bright, half size 0.26, the generic texture, each channel of its color scaled 0.875-1.125;
    sent within 512 blocks, as vanilla's forced particles) and `block_crumble` (still terrain particles living
    1-10 ticks, within 32 blocks).
*   **Model and animations**: 1.21.11's model part for part (64x64) and its four keyframe animations converted
    keyframe for keyframe (`CreakingAnimations`), played through Wild Update's warden animation code (family
    code, copied in), extended with 1.21's scale target. The pose is worked out in plain numbers by
    `CreakingPoser` (1.21's `setupAnim`: reset, head, walk by `limbSwing`/`limbSwingAmount` at 50 ms a unit and
    the speed capped at 1, then attack, invulnerable and death from their start ticks) and copied onto the boxes by
    `ModelCreaking`, so the self-test can sample it on a server. The walk plays only while it can move. The eyes are the
    head alone in `creaking_eyes.png`, additive and full bright, while active, and flickering while it twitches;
    a twitching creaking has no death tilt or red tint.
*   **Heart Transplanter** (`heart_transplanted`, an `ItemTrigger` fired from Forge's place event): 1.21.6's
    three criteria (a heart placed awake or dormant; a pale oak log placed in line with a heart of its axis
    that has another such log beyond). 1.12.2's `placed_block` cannot look at neighbors, so one trigger of ours
    checks all three. Parent `adventure/root`, a task, no reward.
*   Sounds: the 11 creaking events and the heart's `hurt` and `spawn`, files and subtitles from 1.21.11.
    Spawn egg 1.21.4's gray `#5F5F5F` and orange `#FC7812`.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25915), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=2231 fail=0`** (creaking alone: 94; on 2026-11-19, on main a962cc2 with branch
`tr-creaking-anim`, on E-cores: `pass=5202 fail=0`, creaking 146, check_assets 197 blocks 0 errors), **130 blocks, 1872 states, 158 item models,
610 sound files, 0 errors**. Release jar built after the last test run with `--rerun-tasks`: reobfuscated
(`func_` names), no test classes.

*   Registration: entity name, class, egg and its colors, the attributes, size, eye height, step height, all 13
    sounds, the loot table, the trigger.
*   A creaking from nowhere burns, is hurt and dies to one hit.
*   Freezing with a real player in the world: looking at it from 6 blocks freezes and wakes it, targeting that
    player; turned away it moves and stays active, walks up to the player and hits for 3; glass does not hide
    it, stone does; a carved pumpkin keeps an active creaking moving; frozen it takes no knockback and cannot
    be pushed; a creative player neither freezes nor keeps it awake.
*   A heart at night with no player near spawns nothing; with one, one creaking linked to it, standing on the
    floor within 16 blocks, fire immune, comparator by distance, and no second one; the creaking's `home_pos`
    and the heart's UUID saved.
*   A player's hit does no damage, starts the 100-tick hurt call, and a second hit within 8 ticks does
    nothing; fire and lava are not hits; a mob's hit flashes and does no damage.
*   A real player breaking the heart: the creaking twitches, is still there after 40 ticks and gone after 45.
*   The heart removed without breaking: the creaking dies on its next tick, without the twitch.
*   Dawn: the heart goes dormant, the creaking crumbles and is unlinked, and no new one comes by day.
*   Heart Transplanter's checks: uprooted heart no, dormant heart yes, completing log yes, a log across a
    heart of another axis no.
*   Animation (2026-11-19, after a report of a stiff walk in a real client): the pose sampled against numbers typed
    in from 1.21.11's `CreakingModel` and `CreakingAnimation`: every rest pivot; walk keyframes at 0, 0.125, 0.25 and
    0.875 s (rotation and `posVec` position, y negated); linear interpolation between keyframes; the 1.125 s loop;
    half speed halving every channel and speed over 1 capped; the head look plus the walk's head twitch; frozen means
    no walk at all; death keyframes at 0.5 and 1.0 s and holding after 2.25 s; the attack's 1.3 head stretch; the
    invulnerable twitch interpolated; nothing without a start tick. The walk feed, tick by tick while chasing: speed
    and position follow 1.21's `updateWalkAnimation` (toward min(25 d, 3) by 0.4) exactly, reaching 3.0. Frozen:
    head yaw and pitch, body yaw and position unchanged over 10 ticks while the watcher walks round it, walk speed
    down to 0; unfrozen it eases back in. No difference from vanilla was found in the model, keyframes,
    interpolation, part transform order (translate, rotate Z-Y-X, scale) or the walk feed.
*   Client side (same day, for a "gliding" report): a client copy of a chasing creaking on a client world
    (`isRemote`, the server's chunks), moved by 1.12.2's 3-step position interpolation and given the tracker's
    entity data every 3 ticks. Its walk runs on the client from its own positions (1.21's formula every tick,
    reaching 3.0); it never sets `CAN_MOVE` itself and matches the server's within a data update; fed to the model
    as `RenderLivingBase` feeds it, the legs swing past 10 degrees in 37 of 60 ticks; watched, both copies freeze
    and the client's legs stand at rest. The freeze test is 1.21.11's `isLookingAtMe(player, 0.5, false, true, ...)`:
    with `false` the threshold is `1 - 0.5`, a fixed 60 degree cone (the distance-scaled form is the enderman's,
    `true`), as ported.
*   Pathing: lava costs 8, fire 0; a node over 32 blocks from home is blocked unless it brings a lost
    creaking nearer.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything drawn (no client): the model, the animations, the eyes layer, the twitch, the trail and crumble
    particles.
*   Sounds playing (events registered and their files checked).
*   The advancement granted in game (the trigger and its checks are tested, not a real placement).
*   Resin spreading: stream 14 is not in this branch; the call is by name and safe without it.

## Left out, and why

*   **Game events** (`ENTITY_ACTION`, `ENTITY_PLACE`): sculk sensors are Wild Update's; 1.12.2 has no game
    events.
*   **Parrots imitating the creaking** (`entity.parrot.imitate.creaking`): 1.12.2's parrot imitation list is
    vanilla's own; not patched.
*   **Experience from blowing up a natural heart**: stream 12's (see its doc).
*   Packet ids 66-69 and entity ids 36-39 are unused.
