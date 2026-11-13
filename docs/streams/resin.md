# Stream 14: resin (1.21.4)

Package `resin`. Config `resin` (`enabled`). No entities; packet ids 70-74 unused. Branch `tr-resin`, dev-server
port 25916. Reference: 1.21.11's decompiled `MultifaceBlock`, `CreakingHeartBlockEntity.creakingHurt`/`spreadResin`,
`BlockPos.breadthFirstTraversal`, `Blocks`, `Items`, `SoundType`; recipes and loot tables from the 1.21.11 jar.

## Changelog section

*   **Resin clump**: placed on any face of a block, several to a space (one per face). Drops one clump per face
    with any tool. A face whose block goes falls away; pistons destroy clumps; lava can set them alight.
*   **Block of resin**: nine clumps, and back. Breaks at once.
*   **Resin brick**: smelted from a resin clump. Four make **resin bricks**, cut into **resin brick stairs**,
    **slabs** and **walls**; two slabs make **chiseled resin bricks**. Resin bricks need a pickaxe.
*   **Creaking heart**: a hurt creaking makes its awake heart grow 2-3 resin clumps on the pale oak logs around it.
    A broken heart drops 1-3 resin clumps, and a block of resin between two pale oak logs crafts a heart.

## How it works

*   **Clump** (`BlockResinClump`): `MultifaceBlock` with resin's properties: replaceable, no collision, hardness 0,
    `PushReaction.DESTROY`, `ignitedByLava` (Material.VINE: lava ignites it, fire does not spread to it), orange
    terracotta on maps. Six faces are 64 combinations against 1.12's 16 metadata values, so it is four blocks
    (`resin_clump`, `_down`, `_up`, `_up_down`) keeping the four side faces in metadata, one six-bit face mask
    through `of`/`faces`, one item; the same shape as Caves & Cliffs' glow lichen. A face holds where the block
    behind shows a solid face (`canAttachTo`). Losing a face's support removes the face without a drop; losing the
    last one destroys the block with its drops, as vanilla's `updateOrDestroy`. Hit box per face.
*   **Item** (`ItemResinClump`): `MultifaceBlock.getStateForPlacement` with `BlockPlaceContext`'s order: the
    clicked space when it can be replaced (a clump with a vacant face, grass), otherwise the space in front with
    the clicked face first, then the faces in the order the player looks toward them. `addTarget` is the shared
    answer for the server's `onItemUse` and the client's `@SideOnly(CLIENT) canPlaceBlockOnSide` override, without
    which 1.12's client refuses a click that only adds a face.
*   **Blocks**: block of resin (`Properties.of()`: hardness 0, resistance 0, rock for the note block's bass drum,
    drops by hand); resin bricks, their shapes (`common/building/Shapes`) and chiseled resin bricks: rock,
    hardness 1.5, resistance 6, pickaxe. Sounds `block.resin.*` (break, step, place, fall; vanilla has no hit
    sound, and nothing resin takes long enough to break to play one) and `block.resin_bricks.*`.
*   **Recipes** at normal priority (never shadowed by another mod's generic recipes): `resin_block`, `resin_clump`
    (shapeless, 9), `resin_bricks`, stairs 4, slab 6, wall 6, `chiseled_resin_bricks`; smelting 0.1 experience.
*   **Resin from a hurt creaking** (`ResinSpread`): `creakingHurt(TileEntity)`: when the heart is awake, 2-3
    times `spreadResin`: a breadth-first walk from the heart through pale oak logs (depth 2, at most 64 places,
    directions shuffled each time); the first log with an air neighbor (or a clump missing that face) gets a face
    of resin against it, with `block.resin.place` at volume 1, pitch 1. The creaking (stream 13) calls it by name
    (`ResinSpread.CLASS`), vanilla types only, when vanilla does: a player hurt a linked creaking while the heart's
    emitter is 0. The emitter and the trail particles are the creaking's. Null-safe (null tile, client, resin off).
*   The heart's own drops and recipe (stream 12) find `resin_clump` and `resin_block` by name; with this stream
    they exist.
*   **Assets**: `python -I tools/resin/generate.py` (textures, models, the four multipart blockstates from 1.21.11's,
    sounds, 12 English names).

## Not done

*   **Waterlogging**: vanilla clumps are waterloggable, and resin spreads into water sources; here clumps never go
    into water (Core's adapter, `docs/CORE_REQUESTS.md` #7, which already lists clumps and resin brick shapes).
*   **Resin armor trim material** (the plan's "with Trails & Tales"): needs Trails & Tales' trim registry.
*   The other 17 languages: `build_langs.py build` reports keys missing from earlier streams too (2988 errors
    before this stream), so the lang files were left for one family-wide pass.
*   Stonecutting: 1.12 has none; a 1.14 stonecutter derives the cuts from the crafting recipes.

## Tested headlessly

`./gradlew runServer -Pselftest=resin` and `-Pselftest=all` (port 25916), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=2317 fail=0`** (resin alone: 180), **141 blocks, 2046 states, 166 item models, 603 sound
files, 0 errors**. Release jar built last with `--rerun-tasks`: reobfuscated (`func_` names, no MCP names, no test
classes).

*   Registration and Core ownership of every block; no items for the three other clump blocks or the double slab;
    metadata round trips; every face mask 1-63 a clump state and back.
*   Numbers: hardness, blast resistance, tools, sounds, map color, push reaction, lava, no collision, replaceable.
*   Recipes and smelting (including 0.1 experience); the heart recipe exists with resin.
*   Placing on a floor, on a wall's side, joining a second face to a clump; none in mid-air or in water; drops one
    per face by hand; a lost face drops nothing, the last face's loss drops the clump.
*   `creakingHurt` by name: nothing for null or a dormant heart; 2-3 per call when awake; every face against a pale
    oak log; covers exactly the 14 open faces of three logs within two steps, then stops; nothing on a log three
    steps away. A broken heart drops 1-3 clumps.

## Not tested

*   The client: clump models and rotations, the cutout layer, the client-side click check
    (`canPlaceBlockOnSide` is stripped on a dedicated server; its logic, `addTarget`, is tested).
*   A real creaking calling the hook (stream 13 is being built at the same time).
