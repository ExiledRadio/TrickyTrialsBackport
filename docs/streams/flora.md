# Stream 15: spring plants (1.21.5)

Package `flora` (client parts in `flora/client`), patches in `asm` (this mod's first coremod, `docs/PATCHES.md`).
Config `flora` (`enabled`; no other settings). No entities; packet ids 75-79 unused. Branch `tr-flora`, dev-server
port 25917. Reference: 1.21.11's decompiled `BushBlock`, `FireflyBushBlock`, `CactusFlowerBlock`, `CactusBlock`,
`DryVegetationBlock`, `ShortDryGrassBlock`, `TallDryGrassBlock`, `FlowerBedBlock`, `LeafLitterBlock`,
`SegmentableBlock`, `AmbientDesertBlockSoundsPlayer`, `FallenTreeFeature`, `AttachedToLogsDecorator`,
`TrunkVineDecorator`, `PlaceOnGroundDecorator`, `RandomPatchFeature`, `BlockColumnFeature`, `FireflyParticle`,
`BlockColors`, and the jar's `worldgen` and `tags` data; 1.21.5's data is the same except one filter (below).

## Changelog section

*   **Bush**: a leafy green bush in plains, forests, birch forests, windswept hills and along rivers. Bone meal grows
    another beside it. Shears or Silk Touch to collect it.
*   **Firefly bush**: grows beside water nearly everywhere and in swamps. It glows faintly (light level 2), buzzes at
    night under open sky, and fireflies drift around it wherever it is dark enough. Bone meal grows another beside
    it.
*   **Cactus flower**: cacti grow pink flowers on top as they age, more often on tall cacti, and deserts and badlands
    have flowered cacti. A cactus with a flower stops growing. Places on cacti, farmland and the middle of any top
    face. Makes pink dye.
*   **Short and tall dry grass**: in deserts and badlands, on sand, terracotta, dirt and farmland. Bone meal turns
    short into tall, and tall spreads short. Shears or Silk Touch to collect it. On sand or terracotta, the wind is
    heard in it now and then.
*   **Wildflowers**: yellow and white flower beds in birch forests (and meadows, with 1.18 terrain), placed 1-4 to a
    block like pink petals; more placed on a bed add to it. Bone meal adds one. Makes yellow dye.
*   **Leaf litter**: dry leaves on the ground, 1-4 to a block, in dark forests and around the trees of forests and
    dark forests; tinted by the biome's dry foliage color. Placed on any solid top; built over like grass. Smelt any
    leaves into leaf litter.
*   **Fallen trees**: oak, birch, spruce and jungle logs lying on the ground beside their stumps, with mushrooms on
    them and vines on oak and jungle stumps, in the forests, plains, taigas, jungles, savannas and windswept hills.
*   **Dead bushes** stand on farmland, grass and every kind of dirt, and rustle on sand and terracotta.
*   **Sand** makes an idle sound in deserts.
*   **Falling leaves**: oak, birch, jungle, acacia and dark oak leaves, and other mods' leaves, now and then drop a
    leaf in their own color. Spruce drops none; pale oak, cherry and poplar keep their own falling leaves. Switch it
    off with `flora.fallingLeaves`.
*   With a composter installed, all of the new plants compost at 30%. Dry grass and leaf litter burn in furnaces.
*   Only new chunks get the new plants.

## How it works

*   **Blocks** (`BlockVegetation`, 1.21.5's `VegetationBlock`): 1.12.2 `BlockBush`es on 1.21.5's ground
    (`mayPlaceOn`): `#dirt` (dirt, coarse dirt, podzol, grass, mycelium, and other mods' moss, pale moss, rooted dirt,
    mud by name) or farmland; dry grass on `#dry_vegetation_may_place_on` (adds sand, red sand, terracotta, plant type
    Desert); leaf litter on any sturdy top face; the cactus flower on a cactus, farmland or a top face that holds its
    middle (full, fence or wall post). Outlines are 1.21.5's columns (bush 16x13, cactus flower 14x12, short dry grass
    12x10, tall 14x16, firefly bush a full block), dry grass offset in x, y and z. Replaceable (`Material.VINE`): bush,
    dry grasses, leaf litter. Fire 60/100 for all. Shears-only drops (bush, dry grasses) are given in
    `harvestBlock` to shears (any item with the `shears` tool class) or Silk Touch, never both.
*   **Wildflowers and leaf litter** (`BlockSegmented`, `ItemSegmented`): `facing` and `flower_amount` /
    `segment_amount` in metadata, so vanilla's multipart blockstates are used as they are. The outline is the union of
    the filled quarters, starting at the facing's quarter and going counterclockwise
    (`SegmentableBlock.getShapeCalculator`). The item adds one to a block of its kind that is not full, at the clicked
    block or the block the click would place into, unless the player sneaks; vanilla's rule that a block is never
    replaced by its own item is kept for leaf litter (a full bed is clicked past, not replaced). The item overrides
    the client's `canPlaceBlockOnSide` (`@SideOnly(CLIENT)`), as Trails & Tales' pink petals do: without it the client
    refuses a click on a bed, since a bed cannot stand on a bed.
*   **Tints** (`FloraClient`): bush and wildflower stems take the biome grass color (items: grass at 0.5/1.0); leaf
    litter takes 1.21.5's **dry foliage** color: its color map (`textures/colormap/dry_foliage.png`, 1.21.11's) at
    the biome's temperature and downfall, as foliage, with 1.21.5's overrides (`#7B5334` dark forest, swamp, mangrove
    swamp; `#A0A69C` pale garden), averaged over 3x3 as 1.12.2 averages grass.
*   **Firefly bush**: the sound one tick in 30 between day time 12600 and 23400 (1.21.11's day timeline,
    `firefly_bush_sounds`) in a surface world, when nothing but leaves is above; a firefly 7 ticks in 10 within 5
    blocks across and 5 up where the block's raw brightness is 13 or less. `FireflyParticle`: 200-300 ticks, friction
    0.96, alpha in over 30% and out over the last 50%, its own light in over 10% and out over 30%, a new random
    heading one tick in 20, gone inside a block.
*   **Dry grass sound**: `playAmbientDryGrassSounds`, one tick in 200 when the two blocks below are sand or terracotta,
    played at the player.
*   **Bytecode patches** (`docs/PATCHES.md`): P1 replaces 1.12.2's cactus tick with 1.21.5's (cacti three tall keep
    ageing; at age 8 a flower one time in ten, one in four at three tall or more, where a cactus could grow); P2 lets
    the dead bush stand on `#dry_vegetation_may_place_on`; P3 routes `WorldClient`'s ambient display-tick call
    through `FloraHooks.displayTick` for the sand and dead bush sounds (`AmbientDesertBlockSoundsPlayer`), and for
    the falling leaves below.
*   **Falling leaves** (`FallingLeaves`, `FloraClient.fallingLeaf`, `client/FallingLeafParticle`), from 26.3's
    client jar (`TintedParticleLeavesBlock`, `FallingParticlesLeavesBlock`, `FallingLeavesParticle`,
    `FallingParticle`, `Blocks`): after the block's own display tick, `rand.nextFloat() < 0.01F`; then nothing if the
    block below has a full top face (1.12.2: `getBlockFaceShape(UP) == SOLID`, as pale oak already checks); then a
    leaf at a random x and z of the block, 0.05 under it (`ParticleUtils.spawnParticleBelow`). Its color is the
    leaves' tint 0 (`getClientLeafTintColor`: 1.12.2's `BlockColors.colorMultiplier(state, world, pos, 0)`, white
    where a block has no handler). The particle is `TintedLeavesProvider`'s, the same numbers as pale oak's (gravity
    0.07 x 1.2 x 0.0025, wind 10, swirl, no flow-away, size 2 x (0.05 or 0.075), falling at 0.021 from the start, up
    to 300 ticks, gone on landing or when stopped), so pale oak now uses the same class. Sprites
    `particle/leaf_0` to `leaf_11` (`particles/tinted_leaves.json`), copied from the jar.
    *   Every `BlockLeaves` gets it, other mods' and Dynamic Trees' included, except: spruce (26.3's spruce leaves are
        a plain `LeavesBlock`), this mod's pale oak, Trails & Tales Backport's cherry and Game Drops Backport's three
        poplars (their own particle types, drawn by their own blocks). A Dynamic Trees leaves block is judged by its
        primitive leaves (`getProperties(state).getPrimitiveLeaves()`, by reflection), since Dynamic Trees - Backport
        hands the display tick to the primitive's. The answer is kept per block state.
    *   Caves & Cliffs Backport's azalea and flowering azalea leaves have no color handler; 26.3 gives theirs a fixed
        `-9399763` (`#70922D`), and so does this.
    *   `flora.fallingLeaves` (client, no restart) switches it off; with `flora.enabled` off P3 adds nothing.
*   **Generation, one place** (`FloraFeatures`, `FallenTrees`, `FloraBiomes`): every placed feature of 1.21.11 as
    data says, random draw for random draw (count, rarity, `in_square`, heightmap, biome filter, the patch's offsets,
    its inner filter, the state), heights through Core's `WorldHeights`:
    *   `patch_bush` (1 in 4, 24 tries, 5/3), `patch_firefly_bush_near_water` (2 a chunk, MOTION_BLOCKING_NO_LEAVES,
        only where it could stand with water beside the block below; 20 tries, 4/3), `patch_firefly_bush_swamp`
        (1 in 8), `patch_firefly_bush_near_water_swamp` (3 a chunk), `patch_dry_grass_desert` / `_badlands` (1 in 3 /
        1 in 6, 64 tries, 7/3, short or tall), `wildflowers_birch_forest` (3 times 1 in 2, 64 tries, 6/2, any facing
        and 1-4), `wildflowers_meadow` (5 or 10 by the biome info noise at -0.8, 8 tries), `patch_leaf_litter` (2 a
        chunk, WORLD_SURFACE, 32 tries, 7/3, 1-3 pieces on grass), `patch_cactus_desert` / `_decorated` (1 in 6 /
        1 in 13, 10 tries, 7/3; 1-3 cacti biased low, then one time in four a flower; cut from the top where blocked).
    *   Which biome has which, in vanilla's order: `FloraBiomes` (from the jar's `worldgen/biome` files). 1.12.2's
        biomes go by their 1.18 names (`MUTATED_BIRCH_FOREST` is `old_growth_birch_forest`, `MESA_ROCK`
        `wooded_badlands`, ...); another mod's biome by its registry path when 1.21.5 has that name (pale garden,
        meadow, mangrove swamp).
    *   Fallen trees (`FallenTreeFeature`): an upright stump, then 2-3 blocks away in one of the four directions a log
        of length-2 lying along the ground if every block of it is a valid tree position with no gap over two blocks
        long; mushrooms (red 2, brown 1) on one log in ten; `trunk_vine` on oak and jungle stumps. Lengths 4-7 oak,
        5-8 birch, 5-15 super birch, 6-10 spruce, 4-11 jungle. Selectors with their counts and chances:
        `FallenTrees.SELECTORS`.
    *   Leaf litter under trees: 1.21.5's two `place_on_ground` decorators (96 tries within 4 and 2 of 1-3 pieces,
        150 within 2 and 2 of 1-4), on a full opaque non-leaf block with air or vine above and nothing but leaves
        over it.
*   **1.12.2 worlds** (`FloraGen`, `DecorateBiomeEvent.Post`, overworld, new chunks only, the chunk's population
    random, positions in the square at the chunk corner + 8): the biome's patches in order; at a tree selector's place
    in that order, the forest's and dark forest's standing trees get their leaf litter (trunk bases found by scanning
    the square; a dark oak's 2x2 is one tree), then the selector's own count and draws run as extra attempts that place
    only the fallen trees (1.12.2's trees stay as they are, so fallen trees come at 1.21.5's rate per tree); 1.12.2's
    own cacti in the square get a flower one time in four, as 1.21.5's columns do.
*   **Caves & Cliffs' 1.18 terrain** (`cc118-all`): its generator does not run 1.12.2's biome decorator, so
    `FloraGen` never fires there. It calls `FloraFeatures.vegetation(world, rand, chunkCorner, biome)` for the
    patches, `FloraFeatures.place(..., "patch_cactus_desert")` in place of its own cactus patches, and
    `FallenTrees.place` / `FallenTrees.leafLitter` from its tree selectors (`CORE_REQUESTS.md` #11). Until it does,
    nothing from this stream grows in 1.18-terrain worlds.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25917), then `python -I tools/check_assets.py`: **`SELFTEST DONE pass=2349 fail=0`** (flora
alone: 211), **137 blocks, 1909 states, 165 item models, 700 sound files, 0 errors**. Release jar built afterwards
with `--rerun-tasks`: reobfuscated (`func_` names), no test classes, `FMLCorePlugin` in its manifest.

*   All seven blocks and items registered and owned by this mod in Core's table.
*   Hardness 0, fire 60/100, no collision, burn times (dry grass and leaf litter 100, wildflowers none), light 2 on the
    firefly bush only, replaceable or not, the dry grass offset, sound types, metadata round trips, outlines (one and
    two wildflowers, an east-facing one, four leaf litter, cactus flower, firefly bush).
*   Recipes: pink and yellow dye; birch and dark oak leaves smelt to leaf litter for 0.1 experience.
*   Ground: every plant on and off its grounds (grass, farmland, mycelium, sand, red sand, terracotta, stained, dirt,
    stone, a cactus, a fence post, a bottom slab, air); a bush without ground breaks.
*   Items through a fake player's `onItemUse`: placed facing away from the player, added in place to 2, 3 and 4,
    stops at four without stacking on top, added through the block below, nothing added while sneaking, one item per
    piece.
*   Drops: beds drop their count; firefly bush and cactus flower themselves; bush and dry grasses nothing by hand and
    themselves to shears (through `harvestBlock`).
*   Bone meal: bush and firefly bush spread one beside, none when walled in; short dry grass grows tall; tall spreads
    short; wildflowers add one to four, then drop one; leaf litter takes none.
*   P1 applied: a three-tall cactus's top ages; at age 8 flowers on three-tall cacti and on single cacti at their
    rates over 400 tries each; the cactus ages past 8 either way; growth at 15; no flower where a cactus could not
    stand.
*   P2 applied: dead bushes on farmland, grass and mycelium, still on sand, not on stone.
*   Fallen trees of every kind on grass: upright stumps, every log on its side, vines on oak and jungle stumps; over a
    drop only the stump.
*   Leaf litter around a forest tree, only on the ground within its reach; none under a plains tree.
*   Generation through `FloraGen.decorate` in squares with set biomes: bushes on grass in plains, firefly bushes by
    water channels, dry grass in deserts and about one 1.12.2 cactus in four flowered, wildflowers in birch forests,
    leaf litter in dark forests; the full cactus patch places columns topped with flowers.
*   The test world's own populated spawn chunks: 587 populated chunks around spawn on seed 1013 (483 deep
    ocean, 56 ocean, 36 beach, 12 plains) went through `FloraGen` and grew bushes in the plains; no exceptions in the
    log.
*   The server booting at all proves no client type is reached from server code (P3's target and hook are client
    only).

`./gradlew runClient -Pselftest=client` (the falling leaves scene in `ClientScenes`): P3 applied; 20000 display
ticks of oak leaves through `FloraHooks.displayTick` made 214 leaves (one in a hundred: 140-260 accepted) in oak's
foliage color (`77ab2f`, the block's own); birch 194, in birch's `80a755`; spruce, leaves over stone and leaves with
`flora.fallingLeaves` off none; pale oak no tinted leaves but still its own (405 more particles); the game's own
ambient loop under a 9x9 oak canopy dropped 22-23 in 15 seconds. The photo `falling_leaves.png` shows them.
`CLIENT SELFTEST DONE pass=99 fail=0`. The server suite checks which leaves get them (`FloraSelfTest.fallingLeaves`).

## Not tested

*   Anything drawn: models, the tints, the firefly particle, the cactus flower's animated texture.
*   Sounds playing: the firefly buzz, dry grass wind, sand and dead bush (P3 runs only in a client).
*   The 1.18-terrain path (Caves & Cliffs has not wired it yet).
*   Falling leaves with Dynamic Trees, Trails & Tales Backport's cherry, Game Drops Backport's poplars or Caves &
    Cliffs Backport's azaleas installed (none of them is in the dev run; Dynamic Trees needs Forge 2859).
*   Composting (needs Village & Pillage).

## Left out, and why

*   **Flowers, saplings and grass on farmland** (1.21.5's `VegetationBlock.mayPlaceOn` for every plant): a change to
    vanilla blocks beyond this stream's plan; only the dead bush (planned) gets it.
*   **The firefly bush's glow layer** (`cross_emissive`): 1.12.2's models have no emissive quads, so the glowing
    texture draws lit like the rest.
*   **Farmland under a dead bush** stays farmland (1.21.5 dries it to dirt; `docs/PATCHES.md` P2).
*   **Eighteen languages**: only `en_us` is written here (vanilla names); `build_langs.py build` is the integrator's
    step, as for the other streams.
