# Stream 12: pale garden (1.21.4)

Package `palegarden` (wood set code in `palegarden/wood`, the boat in `palegarden/boat`, client parts in
`palegarden/client`). Config `paleGarden` (`enabled`, `generateBiome`). Entity id 30 (`boat`); ids 31-34 and
packet ids 60-64 unused. Branch `tr-palegarden`, dev-server port 25914.

## Changelog section

*   **Pale oak wood**: pale oak logs, wood, stripped logs and stripped wood, planks, stairs, slabs, fences,
    fence gates, doors, trapdoors, pressure plates, buttons, signs and boats. Strip logs and wood with any
    axe. Pale oak planks work in every recipe that takes any planks.
*   **Pale oak leaves** drop pale oak saplings and sticks, never apples, and let pale leaves drift down.
*   **Pale oak saplings** grow only four together, in a 2x2, into a pale oak: a thick trunk with branches and
    a wide, flat canopy, like a dark oak. Saplings go into flower pots.
*   **Pale moss block**: bone meal on it spreads a patch of pale moss with pale moss carpets and grass.
    Saplings, grass and flowers grow on it.
*   **Pale moss carpet**: grows up the sides of walls next to it, and sometimes onto the block above; bone
    meal on a carpet beside a wall grows that upper part. Two pale moss blocks make three carpets.
*   **Pale hanging moss** hangs under blocks and under itself; bone meal makes it longer. Shears or Silk Touch
    to collect it.
*   **Eyeblossoms** open at nightfall and close at dawn, a few at a time across a field. Open ones make orange
    dye, closed ones gray dye. They go into flower pots, where they open and close too. Bees that touch one
    are poisoned.
*   **Creaking heart**: placed between two pale oak logs lined up with it, it is dormant by day and awake by
    night; without them it is uprooted. Hearts grow inside some pale oaks in the pale garden and give
    experience when broken. Silk Touch keeps the heart; otherwise it drops resin (when resin exists).
*   **Pale garden** biome: part of roofed forests is now pale garden in newly generated chunks: gray-green
    grass, a pale sky and gray fog, pale oaks hung with moss, pale moss on the ground, eyeblossoms and no
    animals.
*   With a composter installed, the pale garden's leaves, sapling, carpet and hanging moss compost at 30%,
    pale moss and eyeblossoms at 65%.

## How it works

*   **Wood set** (`palegarden/wood`): Trails & Tales' `common/wood` code, copied in (the family's own code):
    each block extends vanilla's block of that shape. `WoodKind` gained a vanilla-sound form: 1.21.4's pale
    oak uses `BlockSetType`'s defaults, which are 1.12.2's wooden door, trapdoor, gate, button and plate
    events, so the buttons and plates call vanilla's own click methods. Recipes as 1.21.4, fuel 300/150/200/100,
    flammability 5/20 and 5/5, ore names `logWood`, `plankWood`, `slabWood`, `stairWood`, `fenceWood`,
    `fenceGateWood`, `treeLeaves`, `treeSapling`. 26.3's poplar (stream 30) can move this to `common`.
*   **Signs and boat**: our own `TileEntitySign` subclass `trickytrialsbackport:sign` drawn by our renderer;
    the boat is one entity (`trickytrialsbackport:boat`, id 30) with its wood in synced data (`BoatWood`), so
    later woods reuse it.
*   **Tree** (`PaleGardenFeatures.tree`): 1.21.11's `TreeFeature` with `dark_oak_trunk_placer(6, 2, 1)`,
    `dark_oak_foliage_placer(0, 0)` and `three_layers_feature_size(1, 1, 0, 1, 2)`, step for step and in the
    same order of random draws: height 6-9, the free-space check in three layers, dirt under the 2x2 trunk
    (grass and mycelium become dirt), the lean of up to two blocks in one of four directions, 2-4 log
    branches around the trunk each one time in three, the canopy rows and their skipped corners.
    `pale_oak` adds the pale moss decorator (80% a pale moss patch at the foot, hanging moss under 40% of
    logs and 15% of leaves, each strand stopping with chance one half per block); `pale_oak_creaking` adds the
    creaking heart decorator (the first log, in a shuffled order, with logs on all six sides becomes a
    dormant natural heart); the sapling's `pale_oak_bonemeal` has neither.
*   **Leaves**: 1.12.2 leaves (`decayable`/`check_decay`) that decay only when no log is reachable through
    leaves within 6 steps (modern distance 7), untinted, with 1.21.11's falling pale leaf particle (one time
    in fifty, when nothing under the block has a full top).
*   **Pale moss patch** (`PaleGardenFeatures.mossPatch`): `VegetationPatchFeature` as 1.21.11, radius 2-4 and
    30% vegetation (worldgen) or 1-2 and 60% (bone meal), replacing `#moss_replaceable` (stone, granite,
    diorite, andesite, any mod's tuff and deepslate, `#dirt`); the vegetation is carpet 25, short grass 25,
    tall grass 10.
*   **Pale moss carpet**: 1.21's properties (`bottom`, and `north`/`east`/`south`/`west` as `none`, `low`,
    `tall`), so the multipart blockstate is vanilla's. Metadata 0 is a carpet, whose sides always follow the
    walls; 1-15 a topper, one bit per side it has, since vanilla keeps a topper's lost side lost. Low or
    tall is read from the block above. Only the carpet collides; only the carpet drops.
*   **Hanging moss**: `tip` is worked out from the block below; support lost, it breaks on the next tick, so
    a strand comes down a block per tick, as vanilla's.
*   **Eyeblossoms**: open from day time 12600 to 23400 (1.21.11's `timeline/day`); a world with no day keeps
    them as they are. Random ticks switch them with the long sound; a switch schedules every eyeblossom of
    the old kind within 3 across and 2 up or down after 5-10 ticks per block of distance (the short sound).
    Potted ones switch on random ticks.
*   **Creaking heart**: `axis` and `creaking_heart_state` in metadata; `natural` in the tile (1.12.2 has 16
    metadata values; the three properties need 18). The tile does what `CreakingHeartBlockEntity.serverTick`
    does apart from the creaking: every 20-24 ticks it rechecks its state, and while uprooted it does nothing.
    Experience 20-24 for a natural heart, given in `onBlockHarvested` so Silk Touch still gives it, as
    vanilla's `playerWillDestroy`.
*   **Hook for the creaking (stream 13)**: by name only. If
    `com.exiledradio.trickytrialsbackport.creaking.CreakingHeartHook` exists, the heart calls its static
    `boolean tick(TileEntity, NBTTagCompound)` each server tick while not uprooted (it spawns, links and
    unlinks the creaking; returns whether one is linked), `int signal(TileEntity, NBTTagCompound)` for the
    comparator, and `void removed(TileEntity, NBTTagCompound, Entity)` when the heart is broken or blown up.
    The compound is the tile's own, saved under `Creaking`. Resin drops and the heart's recipe look up
    stream 14's `resin_clump` and `resin_block` by name.
*   **Biome** (`BiomePaleGarden`): 1.21.11's numbers (temperature 0.7, downfall 0.8, grass `#778272`, foliage
    `#878D76`, water `#76889D`, sky `#B9B9B9`, no animals, bats, the usual monsters), the roofed forest's
    height. Its decorator does 1.12.2's ores, disks and springs, and 1.21.11's vegetal step
    (`PaleGardenFeatures.vegetation`): 16 tree tries (OCEAN_FLOOR, no water above, sapling survival; one in
    ten a creaking tree), one pale moss patch, closed eyeblossom patches (1 in 8, 96 tries), one eyeblossom
    (1 in 32), two grass patches, pumpkins (1 in 300), sugar cane (1 in 6). Positions are drawn in the 16x16
    square offset by 8, as 1.12.2 decorates, so nothing reaches an ungenerated chunk. The fog (`#817770`) is
    blended in on the client by how much of a 5x5 of 4-block cells around the camera is pale garden.
*   **Placement** (`PaleGardenLayer`): the plan's decision #2 (2026-11-07). 1.21.4 put the pale garden in the
    dark forest's climate, in the plateau table's temperate, most humid slot. 1.12.2's biome layers have no
    climate, so a separate mask of patches (cells of 256 blocks, one in three pale, fuzzy-zoomed and
    smoothed) is laid over the two final biome layers through Forge's `InitBiomeGens`; a roofed forest under
    a pale patch is a pale garden. The 1:1 mask is the 1:4 mask through vanilla's voronoi zoom with vanilla's
    seed, so the two maps agree as vanilla's do. Existing chunks keep their biome. `generateBiome` off leaves
    the biome unregistered.
*   **For other terrain**: everything the biome places is in `PaleGardenFeatures` (`vegetation`, `tree`,
    `mossPatch`), with no dependence on how the biome was placed or on the world's height. Caves & Cliffs'
    1.18 terrain (`cc118-all`) can place `trickytrialsbackport:pale_garden` where 1.21.4 does and call
    `PaleGardenFeatures.vegetation(world, rand, chunkCorner, biome)` for its vegetal step.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25914), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=1298 fail=0`** (pale garden alone: 187), **128 blocks, 1828 states, 118 item models,
411 sound files, 0 errors**. Release jar built with `--rerun-tasks`: reobfuscated (`func_` names, no MCP
names, no test classes).

*   Every block and item registered under this mod and owned by it in Core's table; no wall sign item; the
    boat entity at mod id 30; the biome registered.
*   Hardness (planks, log, leaves, moss, carpet, heart), flammability, burn times (planks, slab, boat,
    sapling; the carpet none), vanilla wood sounds; metadata round trips of the heart, carpet, sapling and
    leaves; axe stripping keeps the axis.
*   Recipes: planks from a log and from stripped wood, boat, carpet, both dyes, sign, pressure plate, sticks
    from pale oak planks; no heart recipe without resin.
*   Saplings: a lone one never grows; a 2x2 grows a tree from its corner, 2x2 trunk, grass under it turned to
    dirt, 24-80 logs, leaves all decayable, no hanging moss (the bone meal tree has no decorators).
*   Worldgen trees (12 seeds, plain and creaking): all placed in open space, hanging moss and pale moss at
    their feet; 66 creaking trees held 22 hearts (a heart needs a trunk log with logs on all six sides, so
    only trees whose branches flank the trunk get one), every heart natural, dormant and enclosed. No tree
    under a low roof, ground untouched.
*   Pale moss: bone meal spreads a patch; none with a block above; saplings and grass grow on it.
*   Carpet: a side up the wall, none without one; bone meal grows a topper, the carpet's side then tall; no
    second topper; a topper drops nothing and goes with its carpet, which drops one; no topper without a
    wall; not placeable on air; collision.
*   Hanging moss: hangs under leaves, not in mid air; bone meal adds one below; tip worked out; no drop by
    hand, shearable; falls block by block without support.
*   Eyeblossoms: closed by day, open at night by a random tick, a neighbor scheduled and following on its
    tick, closed the next morning; open from 12600 to 23400; a potted one opens at night.
*   Creaking heart: uprooted alone, dormant by day between logs, awake at night from its tile, signal 0, uprooted
    with a log lying the wrong way, stripped wood counts; a real player breaking a natural heart gets 20-24
    experience, and no heart drops without Silk Touch.
*   Biome numbers (temperature, downfall, no animals, monsters, the roofed forest's height). The layer: on a
    map of only roofed forest about a third turns pale, in patches; the block map agrees with the 1:4 map
    everywhere; a map of plains stays plains.
*   Decoration of 6 pale garden chunks on flat grass: about 257 pale oak logs a chunk, pale moss on about a
    quarter of the ground, eyeblossoms, a creaking heart.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything drawn (no client): every model and texture, the sign renderer, the boat model, the falling leaf
    particle, the fog blend, the sky and grass colors.
*   Sounds playing (events registered and their files checked).
*   The biome in a real world: a pregenerated map of where pale gardens fall in roofed forests.
*   Composting (needs Village & Pillage).
*   Waterlogging: pale oak stairs, slab, fence, trapdoor, leaves and signs are waterloggable in vanilla; they
    wait for Core's adapter (`CORE_REQUESTS.md` #7).

## Left out, and why

*   **The creaking** (stream 13) and **resin** (stream 14). The heart is ready for both, by name.
*   **Pale oak hanging sign and chest boat**: Trails & Tales' hanging signs and Wild Update's chest boats;
    left to those integrations (plan, Rule 2).
*   **Glow lichen** in the pale garden's caves: Caves & Cliffs' block. **Firefly bushes**: 1.21.5 (stream 15).
*   **The silence**: 1.21.4 plays no music in the pale garden; 1.12.2 has no per-biome music.
*   **Eyeblossom particles** (the trail particle when they switch): 1.12.2 has no such particle.
*   **Suspicious stew** from eyeblossoms: the stew is Village & Pillage's.
*   **Experience from blowing up a natural heart**: vanilla gives it to the player who caused the explosion;
    here only breaking gives it.
