# Stream 19: the copper golem (1.21.9)

Package `coppergolem` (client parts in `coppergolem/client`). Config `copperGolem` (`enabled`, `building`, `sorting`,
`oxidation`, `statues`, `ironGolemFlowers`). Entity id 50 (`copper_golem`); ids 51-54 unused. No packets: 95-99 unused
(everything the client draws is synced entity data or block state). Branch `claude/tr-coppergolem-c05fbb`, dev-server
port 25919. Generators `tools/coppergolem/generate.py` (assets, sounds, names) and `tools/coppergolem/animations.py`
(the keyframes). No patches.

Behavior is 1.21.11's (`CopperGolem`, `CopperGolemAi`, `TransportItemsBetweenContainers`, `CopperChestBlock`,
`WeatheringCopperChestBlock`, `CopperGolemStatueBlock`, `WeatheringCopperGolemStatueBlock`,
`CopperGolemStatueBlockEntity`, `CarvedPumpkinBlock`, `OfferFlowerGoal`, `CopperGolemModel`, `CopperGolemAnimation`,
`CopperGolemRenderer`, `CopperGolemStatueBlockRenderer`, `ChestModel`, `ChestRenderer`), decompiled from the 1.21.11
client jar (`G:\MCDownloads\tr-coppergolem-decomp`); assets, names and sounds from the same jar; the data tags
(`#copper`, `#copper_chests`, `#shearable_from_copper_golem`, `#accepts_iron_golem_gift`, `#can_breathe_under_water`,
`#fall_damage_immune`) and loot tables from it too.

## Changelog section

*   **Copper golem**: put a carved pumpkin or a jack o'lantern on a block of copper (any stage, waxed or not) to build
    one. The copper block turns into a copper chest, and the golem takes the copper's stage. It has 6 hearts, never
    fights, runs off when hurt, opens and closes wooden doors, breathes under water and takes no fall damage.
*   **Sorting**: an empty-handed copper golem walks to the nearest copper chest with something in it and takes up to 16
    of the first stack. Then it carries them to the nearest chest or trapped chest that is empty or already holds that
    item. It searches 32 blocks around and 8 up and down, spends 3 seconds at each chest, tries up to 10 chests before
    resting, and waits its turn when someone else is using a chest. Use it with an empty hand to take what it carries.
*   **Oxidation**: copper golems age a stage every 21 to 23 days (even far away) and their head spin sounds rustier.
    Honeycomb waxes one, an axe takes the wax off or scrapes a stage off, and lightning scrapes a stage off.
*   **Copper golem statue**: a fully oxidized golem standing in the open soon freezes into a statue in a random pose,
    keeping its name. Use a statue to change its pose (standing, sitting, running, star); a comparator reads the pose.
    Statues age, wax and scrape like copper, and an axe on a fresh unwaxed statue brings the golem back.
*   **Copper chest**: 8 copper ingots around a chest, in four stages and waxed. Copper chests join each other into
    large chests (never with wooden ones), oxidize, take honeycomb and an axe like other copper, and have their own
    open and close sounds.
*   **Iron golems** sometimes give a copper golem a poppy for its head. Shears take it off.

## How it works

*   **Copper chests** (`BlockCopperChest`, `TileEntityCopperChest`, `ItemCopperChest`): 1.12.2's `BlockChest` with a
    chest type of their own (`BlockChest.Type` extended through Forge's `EnumHelper`), so 1.12.2's chest code joins
    copper chests only with copper chests. Hardness 3, resistance 6, the copper sound type, a pickaxe needed for drops
    (1.12.2's wood material would not ask), map colors by stage (the copper stream's). The tile entity is 1.12.2's
    chest with the stage's open and close sounds (unaffected and exposed share the plain ones), the block's name as
    the default title, and golems counted in the 200-tick recount of who has it open. Hoppers, comparators and the
    double-chest screen are 1.12.2's own.
*   **Two halves, one block**: 1.21 joins copper chests of different stages and makes both the least oxidized of the
    two (unwaxed when only one was waxed); afterwards a half copies its partner whenever the partner changes. 1.12.2's
    chest code joins only identical blocks, so the item places the least oxidized block on both sides, and a half
    follows a partner that turns into another copper chest (`neighborChanged`). Changing block keeps the tile entity
    and contents (`shouldRefresh`, and `breakBlock` does nothing when a copper chest replaces a copper chest). A double
    chest ages from its left half only (1.21 skips the `right` half), never while anyone has it open.
*   **Statues** (`BlockCopperGolemStatue`, `TileEntityCopperGolemStatue`, `ItemCopperGolemStatue`): facing and pose in
    the 16 metadata values; 10 x 14 x 10 pixels; hardness 3, resistance 6, the statue sound type, dropped by hand,
    destroyed by pistons; drawn by a tile entity renderer (the block model is the breaking particle only). The item's
    damage value is its pose; drops and pick-block keep the pose and the golem's name. Using one with anything but an
    axe turns it to its next pose (the statue sound; honeycomb on an unwaxed one waxes it instead); a comparator reads
    pose + 1. Water is Fluidlogged API's through Backport Core's `Waterloggable`, as for the chests.
*   **Oxidation**: chests and statues are four-stage chains with waxed forms in Backport Core's `copper.Oxidation`
    table, so they age by its rule (and wait for or are hurried by any mod's copper nearby) while
    `copperGolem.oxidation` is on. Right click (`CopperGolemEvents.rightClick`, before the copper streams' handlers):
    honeycomb (`copper.waxItems` or the `honeycomb` ore name) waxes, an axe takes wax off or scrapes; an axe on an
    unaffected unwaxed statue revives the golem (at the statue's bottom center, facing its way, with its name, the
    spawn sound) and wears once. Lightning cleans them through the copper stream's handler when that stream is on.
*   **Building** (`CopperGolemEvents.trySpawnCopperGolem`): 1.21's pattern (a pumpkin or jack o'lantern, `#copper`
    below it in any of the pattern's orientations), checked on Forge's neighbor notification for a pumpkin's position,
    which 1.12.2 sends right after the pumpkin's own snow and iron golem check; so a pumpkin placed by anyone or
    anything counts, and snow and iron golems win as in 1.21. The pattern is cleared (with break particles), the golem
    spawns at the pumpkin 0.05 up, nearby players get the summoned-entity trigger, the copper block becomes the copper
    chest of its stage (unwaxed) facing the pumpkin's way, joining a single copper chest beside it, and the golem takes
    the stage and plays its spawn sound. Copper blocks are Caves &amp; Cliffs' or the copper stream's copies by name
    (`Owners.block`). Dispensers place a pumpkin where it would build a copper golem (1.21's `canSpawnGolem`), else do
    what they did.
*   **The golem** (`EntityCopperGolem`, an `EntityGolem`): 0.49 x 0.98, eyes at 0.8125, 12 health, speed 0.2, follow
    range 48, step height 1, fire paths avoided as in 1.21, persistent, wooden doors opened and closed, no fall damage,
    breathes under water. Synced: weathering state, golem state (idle, getting item, getting no item, dropping item,
    dropping no item) and the antenna stack. Saved: `next_weather_age`, `weather_state` (by name), `antenna`, the
    transport cooldown and the visited and unreachable chest memories with their expiry.
    *   Aging: `next_weather_age` -1 is set to world time + 504000-552000; at that time it ages a stage and times the
        next from the last; fully oxidized it is 0, and each tick standing in air it turns to a statue with chance
        0.0058 (dropping what it carried and its poppy, keeping its name, its leash dropped by `doEntityDrops`). -2 is
        waxed. Lightning scrapes one stage per bolt (the bolt's UUID remembered) and re-times its aging.
    *   Using it: an empty hand takes its item (thrown toward the player); shears take the poppy (1.21's shear sound,
        dropped 1.5 up); honeycomb waxes; an axe unwaxes, else scrapes.
    *   Death: 1-3 copper ingots, plus round(Looting x random) more (`entities/copper_golem`), the owner's ingot.
    *   AI: sorting runs every tick from `updateAITasks` like a 1.21 brain behavior (1.12.2's task list only looks at
        starting a task every third tick, which would shift its timings); panic (1.5x, while hurt in the last 40 ticks
        or burning), doors, looking at players (6 blocks), and, only while resting from sorting, strolling 2 blocks or
        standing 30-60 ticks, even odds, are 1.12.2 tasks.
*   **Sorting** (`CopperGolemTransport`, 1.21.11's `TransportItemsBetweenContainers` with `CopperGolemAi`'s settings):
    the nearest chest (by distance from the golem to the chest's center) in loaded chunks within 3 chunks, inside the
    box 32 across and 8 up and down from the golem's block (1 while riding), not locked, not tried in the last 6000
    ticks: a copper chest when empty-handed, a chest or trapped chest when holding something (1.21's two blocks, not
    modded chests). While travelling it checks its path ends where it can reach and see a face of the chest, else marks
    it unreachable. Within 3 blocks of a chest that a player or golem has open it queues. Within reach (0.5 blocks
    around the chest's box, 1 once its path is done) it stops, looks at the chest and spends 60 ticks: on the first the
    lid opens and its animation starts, on the 9th its sound plays (picking up, nothing to pick up, placing, cannot
    place), on the 60th the lid shuts and it acts: up to 16 of the first stack; or the held stack into the first slot
    that is empty or holds the same item and data with room (1.21's arithmetic, which takes the free space off the
    held stack). A chest counts as matching when it is empty or holds the same item (1.21's `isSameItem`: damage,
    enchantments and names ignored; 1.12.2 tells some items apart by metadata, so those count as different items, as
    1.13 made them). Success clears its memories; over 10 tried chests (or 50 unreachable), or no chest found, it rests
    140 ticks. The first rest after it appears is 60-100 ticks. Leashed or panicking, it stops.
*   **Iron golems** (`CopperGolemEvents.OfferFlower`): their `EntityAILookAtVillager` is swapped for 1.21.9's
    `OfferFlowerGoal` at the same priority: by day, 1 in 8000 ticks, a poppy held out for 400 ticks to the nearest
    villager or copper golem within 6 blocks; if the offer runs out with a bare-headed copper golem still within reach,
    the poppy goes onto its antenna (a guaranteed drop). Villagers take the rose as before.
*   **Client** (`coppergolem/client`): the golem model (1.21.11's parts and its seven keyframe animations, through a
    copy of the creaking stream's animation code), its stage's texture, the glowing eyes layer, the held item
    (1.21's hand transform) and the antenna block layer. The idle head spin starts every 200-240 ticks with its
    sound 10 ticks in, per stage (weathered and oxidized have their own). The chest renderer is 1.21.11's chest model
    (single, left and right halves, the 1.15+ 64 x 64 textures as they are), drawn without a flip as 1.21 does; the
    statue renderer is the golem model in each pose's layer, turned and flipped as 1.21's statue model is. Items are
    drawn by an item renderer: a closed chest facing south (1.12.2's chest item display), the statue in its pose
    (1.21.11's statue item display).
*   **Sounds**: the 30 events of 1.21.11 (golem by stage, chest by stage, statue sound type, become statue, shear),
    and the copper stream's `block.copper.*`, `item.honeycomb.wax_on`, `item.axe.scrape` and `item.axe.wax_off`.
*   **Recipes**: the copper chest (8 copper ingots around a chest, the owner's ingot) and waxing in the grid
    (`honeycomb` ore) for the 4 chests and 4 statues, with their recipe advancements.

## Tested headlessly

`./gradlew runServer -Pselftest=coppergolem`, then `-Pselftest=all` from a fresh world, alone and with Caves &
Cliffs; `python -I tools/check_assets.py`; the release build.

*   **`-Pselftest=coppergolem`: `SELFTEST DONE pass=331 fail=0`.**
*   **Alone, `-Pselftest=all`, fresh world (Core 1.3.0, as `gradle.properties` builds): `SELFTEST DONE pass=4625
    fail=0`**; **197 blocks, 3048 states, 275 item models, 1137 sound files, 0 errors** from the asset check.
*   **With Caves & Cliffs 1.1.0's dev jar** in `eclipse/mods` (fresh world, `-Pcore_version=1.4.0`): **`SELFTEST DONE
    pass=4471 fail=6`**, all of the coppergolem test passing (copper blocks and ingot are Caves & Cliffs'). The six are
    the known ones in other streams' tests (copper: `waxing the top waxes both halves` and the door read as air; flora:
    `every firefly bush stands on grass`; flat: the three copper and tuff generator checks).
*   **Flaky, not this stream**: in two of five full runs (one alone on a reused world, one with Caves & Cliffs) the
    ghast test's `stillTimeout` failed its 7 checks (`player on top: still timeout 0` and the six after it); the same
    setups passed on the next run. That test runs before this one and touches nothing of it.
*   **Release** (`./gradlew build --rerun-tasks --offline -PdeployDir= -PcoreDeployDir=`): built, 0 `/selftest/`
    entries in any jar.

What the coppergolem test checks:

*   Registration: 16 blocks, each with an item, owned by this mod, hardness 3, resistance 6, aging only unwaxed and not
    oxidized; the oxidation and wax chains; their own chest type; entity id 50; the tile entities; the copper blocks
    (Caves &amp; Cliffs' when installed) and their stages; the ingot is the owner's.
*   Recipes: the copper chest from 8 ingots and a chest; honeycomb waxing every chest and statue in the grid.
*   Copper chests: a pickaxe needed; a weathered and a waxed exposed chest placed side by side both become unwaxed
    exposed; 1.12.2's chest code sees one double chest; no third chest beside it; a wooden chest never joins one; an
    axe scrapes one half and the other follows, the tile entity and contents stay; honeycomb waxes both using one; an
    axe unwaxes both; 3000 random ticks oxidize a lone chest keeping its contents; a waxed chest never ages; an open
    chest does not age; a double chest's right half never ages it, its left half ages both, facing kept; drops; broken,
    its contents spill; the double chest holds 54.
*   Statues: the pose from the item's damage; all 16 metadata values; using it cycles the poses; comparator 1 and 2;
    facing kept; the drop keeps pose and name; 3000 random ticks oxidize it keeping name and pose; an axe scrapes,
    honeycomb waxes, honeycomb on a waxed statue poses it, an axe unwaxes and scrapes back to unaffected; the axe on an
    unaffected statue removes it and brings back a golem with its name, unaffected, at the bottom center, facing its
    way; the axe wears once per use.
*   Building: a pumpkin on exposed copper builds an exposed golem where the pumpkin was (0.05 up), 12 health, speed
    0.2, unwaxed and untimed; the pumpkin is gone; the copper is an exposed copper chest facing the pumpkin's way; a
    jack o'lantern beside waxed weathered copper builds a weathered golem and an unwaxed weathered chest; beside a
    single oxidized copper chest the new one joins it as unaffected; a copper block put under a pumpkin builds nothing;
    iron does not; the setting off builds nothing; a dispenser places a pumpkin onto copper and the golem is built
    (upside down).
*   The golem: honeycomb waxes (one used), an axe unwaxes then scrapes, nothing to scrape from unaffected; one stage
    per lightning bolt however often it hits; an empty hand takes its item (thrown); breathes under water; no fall
    damage; saving and loading its stage, aging time and antenna; shears take the poppy (one dropped); 40 deaths drop
    between 1 and 3 copper ingots.
*   Aging: the first stage timed 504000-552000 ticks ahead, not a tick early, the next timed from the last, oxidized
    after three, then 0; waxed never ages nor turns to a statue; oxidized it turns into an oxidized statue at about
    0.58% a tick, facing its way, with its name, dropping its carried stack and poppy; never under water; the setting
    off keeps it a golem.
*   Sorting (a real golem walking): empty-handed it targets the copper chest; it reaches it and takes the item exactly
    59 ticks after the interaction began (the first tick to the 60th), the lid open all that time and shut after; 16
    of the first stack, 4 left, the other stack untouched; memories cleared; holding cobblestone it targets the nearest
    chest, which holds only dirt: it cannot place, goes on to the trapped chest and tops its stack up to 64 with the
    rest in the next slot; memories cleared; it waits behind someone using a chest, then places apples onto the apple
    stack; with nothing to fetch it rests 140 ticks.
*   Sorting rules: a copper chest 30 away and 8 up is found, 9 up is not, 32 across is, 33 is not; an empty-handed
    golem ignores wooden chests, one holding an item wants them and never copper chests; a locked chest is skipped;
    riding it searches 1 block; matching ignores damage but tells 1.12.2's wool colors apart; placing fills the first
    empty slot (not the later stack), tops up a matching stack and keeps the rest; taking is at most 16 from the first
    stack; the setting off never takes anything.
*   Iron golems: the flower offer replaces 1.12.2's at priority 5; a full offer puts the poppy on a bare copper golem;
    not on one already wearing something, not when cut short, not when it walked off.

## Not tested

*   Anything drawn or heard (no client): the golem model, its animations, eyes, held item and poppy; the chest and
    statue renderers and items; the head spin sound; the chest sounds.
*   The 9th-tick interaction sounds (played, not observed headlessly).
*   Golems pathing through doors, and long walks over real terrain.
*   Lightning cleaning chests and statues (the copper stream's handler).

## Left out, and why

*   **Copper golems pressing copper buttons**: the mob vote's design; 1.21.9's golem sorts chests instead, and there
    is no copper button.
*   **Game events** (container open and close, shear, block change): Wild Update's; Core's gameevent module is in
    Core 1.4.0, while `main` builds against 1.3.0.
*   **Wax particles** (1.21's level events 3003-3005): 1.12.2 has no such particles; the sounds play.
*   **Copper chests joining only same-facing chests**: 1.21 joins a chest only to one facing the same way (and not
    while sneaking); 1.12.2's chest code joins any two side by side and turns them alike. Copper chests follow 1.12.2
    here, as wooden chests do.
*   **Slot order in a double chest**: the golem uses the container the double-chest screen shows (1.12.2's: the north
    or west half first), where 1.21 puts its right half first, so "the first slot" is the first slot the player sees.
*   **A golem's open lid**: 1.21 lets a chest notice within 5 ticks that a golem walked off; here the golem shuts it at
    once.
*   **The chest's custom name on its drop**: 1.12.2 chests do not keep names when broken; neither do these.
*   **The copper golem's `SADDLE` slot**: 1.12.2 has none; the antenna is its own synced stack.
*   Our config text is English in the other 17 languages (as in every stream so far); Mojang's names are in all 18.
*   Packet ids 95-99 and entity ids 51-54 are unused.
