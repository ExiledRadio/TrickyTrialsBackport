# Stream 1: tuff sets (1.21)

Package `tuff`, shared shapes in `common/building`. Config `tuff` (`enabled`, `generate`). Branch `tr-tuff`.

## Changelog section

*   **Tuff stairs, slabs and walls**, **polished tuff** with its own stairs, slabs and walls, **chiseled
    tuff**, **tuff bricks** with their stairs, slabs and walls, and **chiseled tuff bricks**.
*   Four tuff make four polished tuff; four polished tuff make four tuff bricks; two slabs stacked make the
    chiseled block. Stairs, slabs and walls craft as usual, and a stonecutter cuts them.
*   Each family has its own sounds, as in 1.21.
*   Walls join every other wall, vanilla's cobblestone walls included.
*   **Tuff** itself comes from the 1.17 blocks when they are installed. Without them, this mod adds tuff and
    it generates deep underground (below y 16) in newly generated chunks.

## How it works

*   **Shapes** (`common/building`): `BlockShapedStairs` opens vanilla's stairs; `BlockShapedSlab` is the
    family's two-block slab (`<name>_slab` and `<name>_double_slab`, as Village & Pillage's), joined by
    vanilla's `ItemSlab`; `BlockShapedWall` extends vanilla's wall so it connects to every wall, with
    vanilla's `variant` pinned to 0 and hidden from the client (`Content.ignoreProperties`). `Shapes`
    registers any of the three for a base block and copies its strength, sound, tool and map color. Resin
    bricks and pale oak will use the same classes.
*   **Tuff ownership** (Rule 1): `Owners.copy("tuff")` asks Backport Core. Without Caves & Cliffs our
    `tuff` is registered with its generation; with it, nothing is registered and the shapes are cut from
    an unregistered twin with the same numbers, while recipes take the owner's block (`Owners.block`).
*   **Numbers** from 1.21.11: every block hardness 1.5, blast resistance 6, pickaxe, gray terracotta on
    maps. Sound types tuff, polished tuff and tuff bricks (volume and pitch 1), events copied from the
    1.21.11 asset index under our namespace (`block.tuff.*` etc.).
*   **Generation of the copy**: 1.17's `ore_tuff`, two blobs of up to 33 per chunk, y 0-16, replacing stone,
    Overworld only, behind `tuff.generate`.
*   **Models**: 1.21.11's models ported as they are (their `block/slab` and `template_wall_*` parents come
    along into our namespace). Wall blockstates are 1.12.2 multipart files on `up` and four booleans: no
    tall sides, since 1.12.2's walls have none.

## Tested headlessly

`./gradlew runServer -Pselftest=scaffold,tuff` (port 25901), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=425 fail=0`**, **17 blocks, 326 states, 14 item models, 82 sound files, 0 errors**.

*   Ownership: the tuff copy is registered without Caves & Cliffs; Core's table names Caves & Cliffs as the
    owner of tuff and the copper set (the shim).
*   All 17 blocks and 14 items registered; double slabs have no item; every stored state survives a
    metadata round trip (stairs and walls by metadata, since their shapes are actual state).
*   Hardness 1.5, blast resistance 6 and pickaxe on every kind (full block, stairs, slab, double slab,
    wall); each family's sound events; double slabs drop two slabs.
*   Every recipe: polished, bricks, both chiseled blocks, stairs (4), slabs (6) and walls (6) of all three
    families; vanilla's cobblestone wall recipe unchanged.
*   A player places a slab, then a second on it, making the double slab and using both; places stairs
    facing their way; a tuff wall joins a cobblestone wall and a polished tuff wall, and the cobblestone
    wall joins back; polished tuff needs a pickaxe.
*   Generation: the spawn chunks of a new world have tuff below y 20 (more than 10 blocks per chunk on
    average) and none above.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything visual: models, textures, the stairs' corner shapes, walls' looks, slab top halves.
*   Sounds playing.
*   With Caves & Cliffs installed: that mod does not exist yet. The path (no copy, shapes from the twin,
    recipes from the owner's tuff) is written but unexercised.
*   The stonecutter's cuts (needs Village & Pillage in `eclipse/mods`). Its derivation skips recipes that
    take slabs, so chiseled tuff from tuff will not be offered by it; 1.21's stonecutter does offer it.
*   Languages: the 18 files carry Mojang's names for every block; our own config text is still English in
    them (falls back in game) until the translation batch.

## For the integrator

*   `common/building` and the `Content` additions (`sound`, `soundType`, `ignoreProperties`,
    `multipartFile`), the client state mapper for ignored properties and the self-test dump skipping them
    are new shared code from this stream.
