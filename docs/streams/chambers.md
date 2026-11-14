# Stream 10: trial chambers (1.21)

Package `chambers`. Config `chambers` (`enabled`). No entities; packet ids 50-54 unused. Branch `tr-chambers`, dev-server
port 25910. Reference: 1.21.11's decompiled `JigsawPlacement`, `JigsawStructure`, `Structure`, `SinglePoolElement`,
`StructureTemplatePool`, `PoolAliasLookup` and its bindings, `SequencedPriorityIterator`, `WorldgenRandom`, `Util`
(`G:\MCDownloads\tt-1.21.11-decomp\src5`, `src6`). Every file and number is from the 1.21.11 jar.

## Changelog section

*   **Trial chambers** generate underground in new chunks, where 1.21 puts them for the same seed (Chunkbase 1.21).
    In a Caves & Cliffs extended-height world they start at vanilla's y -40 to -20; in a 0-255 world at y 16 to 29.
*   Their trial spawners hold one melee, one small melee and one ranged mob kind per structure, picked as in 1.21
    (zombie, husk or spider; slime, cave spider, silverfish or baby zombie; skeleton, stray or poison skeleton),
    plus breezes. Vaults and ominous vaults, chests, barrels, dispensers and decorated pots hold 1.21's loot.
*   No mobs spawn naturally inside the chambers.
*   `/locate structure minecraft:trial_chambers`.
*   Advancement **Minecraft: Trial(s) Edition** (step into trial chambers); *Under Lock and Key* now follows it.

## How it works

*   **Data** (`python -I tools/chambers/generate.py`): 1.21.11's `structure/trial_chambers/**` (191 templates),
    `worldgen/template_pool/trial_chambers/**` (51 pools), the processor list `trial_chambers_copper_bulb_degradation`,
    `worldgen/structure/trial_chambers.json`, `worldgen/structure_set/trial_chambers.json`, the biome tag
    `has_structure/trial_chambers`, and the 17 chest, dispenser and pot loot tables, all unchanged. The mod is a
    Backport Core `JigsawPools` and `ModernPalette` source, so `minecraft:` names are read from this jar.
*   **Settings** (`ChamberSettings`): read from the copied structure and structure set files: grid 34/12, salt
    94251327, linear; depth 20; max distance 116; start height uniform -40 to -20; padding 10; three pool aliases;
    the biome tag. Nothing is typed in by hand.
*   **Placement** (`TrialChambersStructure`, a Core `ScatteredStructure`): Core's populate-step driver lays out and
    builds the starts on any terrain. The start random is `StructureRandoms.start` (1.21's `makeRandom`). The stub's
    biome is checked as 1.21 does, before the children (`ChamberBiomes`): in Caves & Cliffs' 1.18 terrain the noise
    biome of the stub's quart, reached by reflection as Wild Update's ancient cities reach it, against the tag;
    in 1.12 terrain every overworld biome is in the tag, except where Wild Update's deep dark is (its `DeepDark.at`,
    by name). Starts are laid out from 9 chunks away (`range`), since a piece may reach that far.
*   **Assembly** (`ChamberLayout`): 1.21.11's `JigsawPlacement` draw for draw. Core's `Jigsaw` is 1.16.5's, so this
    is our own, on Core's pools, elements and templates. What 1.21 adds: jigsaws shuffled then stably sorted by
    `selection_priority`; opened pieces queued by `placement_priority` (FIFO within a priority); pools looked up
    through the aliases (`RandomSource.create(seed).forkPositional().at(chunk corner, start y)`); the free box 116
    out from the start piece's middle, clamped to the world's floor and roof less the padding; the start piece
    refused if it is within the padding. Shuffles are `Util.shuffle` (n - 1 draws; `Collections.shuffle` draws the
    same). The priorities are read from the templates' jigsaw NBT, which Core's `ModernTemplate.Jigsaw` does not keep.
*   **Heights**: the layout is always made in 1.21's y -64 to 319, so it is vanilla's piece for piece. In a world
    that goes down to -64 (`WorldHeights.minY`) it stays there. Elsewhere the whole layout moves by the
    proportional shift of its start, `(y + 64) * 256 / 384` (y -40 to -20 become 16 to 29; the shift is 49 to 56).
    The test world's chambers span y 7-57. `Start.generateStructure` widens 1.12's y 1-255 chunk box to the world's
    full height, as the ancient city does.
*   **Pieces** (`ChamberPiece`, a Core `JigsawPiece`, saved as `TTB_TrialChambersPiece`): placed as 1.21's
    `SinglePoolElement`: the element's processor list (Core's `rule` processors, the bulb weathering), then
    `protected_blocks` (`#features_cannot_replace`, which Core skips) for the elements that name that list;
    `ignore_waterlogging` (`keepWater(false)`); air placed. Block changes are quiet (`QuietGen`: no neighbor updates,
    as 1.21's `WorldGenRegion`). The pieces are `TerrainAdaptation.AdaptingPiece`s with 1.21's junctions.
*   **Contents**: trial spawners and vaults are the trials stream's blocks; their 1.21 block entity data
    (`normal_config`, `ominous_config`, the vault `config`) is read as is. Containers' `LootTable` names are renamed by
    Core's palette to `trickytrialsbackport:chambers/<path>` (`ChamberLoot`): empty 1.12 tables that get, at load, one
    entry rolling vanilla's file through `trial.ModernLoot`.
*   **Blocks other family mods own** come from the owner when installed. Without it, stand-ins (`FALLBACKS`): barrel to
    chest (with its loot), cobbled deepslate to cobblestone, moss block to mossy cobblestone, powder snow to snow
    block, mangrove log, wood and leaves to oak's, mud to dirt; decorated pots, candles, chains, moss carpet, pointed
    dripstone and mangrove roots are left as air.
*   **Spawns** (1.21's `spawn_overrides`, every category empty, by piece): `WorldEvent.PotentialSpawns` is emptied
    inside a piece. **Advancement**: a player inside a piece fires `trickytrialsbackport:entered_trial_chambers`,
    checked once a second (1.21's location trigger).
*   **Terrain**: registered with Core's `TerrainAdaptation` as `ENCAPSULATE`, with `startsNear` as the planner.

## Shared files touched

*   `TrickyTrialsBackport.java`: one preInit and one init line. `SelfTestRunner`: one line. `en_us.lang`: the
    `chambers` block (2 keys).
*   `tools/trial/generate.py` and `advancements/adventure/under_lock_and_key.json`: the parent is now
    `trickytrialsbackport:adventure/minecraft_trials_edition` (stream 7's hand-over asked for it).
*   `deps/`: built against `TrickyTrialsBackport-playtest/deps/BackportCore-1.2.0-dev.jar` (Core playtest). Main's
    copy of Core main's 1.2.0 dev jar lacks `block.ModernWall` and `copper.Oxidation`, so 9415c2b does not compile
    against it.

## Not done

*   **Trial explorer map** (PLAN row 10). 1.12's `MapDecoration.Type` has no trial chambers icon, and the cartographer
    trade is Village & Pillage's. Needs a map decoration hook and a trade.
*   **Encapsulate** in Caves & Cliffs' 1.18 terrain: its `StructureBeards` maps `BEARD_THIN`, `BURY`, `BEARD_BOX`
    only and logs that `ENCAPSULATE` is unsupported. Caves pass right by the chambers' walls. A C&C request.
*   In 1.12 terrain the stub's biome is the column's. Vanilla's deep dark exclusion applies there only with Wild
    Update's deep dark. Four of the oracle's 48 candidates are invalid in vanilla (the deep dark is the only overworld
    biome the tag leaves out); without Wild Update they are chambers here.
*   The other 17 languages: the family-wide `build_langs.py build` pass (it reports missing keys from earlier streams).

## Tested headlessly

`./gradlew runServer -Pselftest=all -PdeployDir= -PcoreDeployDir=` (Default world, seed 1013, port 25910):
**`SELFTEST DONE pass=2975 fail=0`** (chambers alone: 55). In a `cavesandcliffs` world (C&C playtest's release jar
in `eclipse/mods`, `level-type=cavesandcliffs`, `level-name=selftest-cc`, undone afterwards): chambers **55/0**,
the chambers at y -44 to 6. `python -I tools/check_assets.py`: **148 blocks, 2475 states, 173 item models, 771 sound
files, 0 errors**. Release jar built last with `--rerun-tasks`: 0 `/selftest/` entries, `func_` names.

*   **Against vanilla 1.21.11** (`tools/chambers/oracle121.txt`): a real 1.21.11 server, seeds 1013, 123456789 and
    -5584399987456711267, force-loaded every candidate chunk of regions -2..1 and saved; the saved starts were read
    from the region files (`tools/chambers/oracle/`: `oracle.py`, `nbtread.py`, `to_text.py`). 48 of 48 candidate
    chunks are ours; all 44 valid starts lay out piece for piece (template, position, rotation): 11021 of 11021
    pieces. The test world's own chambers match too, moved up by their shift.
*   Data: the settings read from the files; 51 pools and 191 templates load; this mod's blocks resolve (tuff,
    copper, bulbs, grates, doors, trapdoors, trial spawner, vault).
*   A built chamber (251 pieces): `/locate structure minecraft:trial_chambers` and Core's ring-order check; the
    saved start; 14 trial spawners, each with its 1.21 normal and ominous config, one mob per kind (husk, slime,
    stray, breeze here), ranged and slow ranged from one alias group; 27 vaults with 1.21's keys and reward tables;
    containers name our tables and a chest fills; nothing dropped while building; no natural monsters inside a
    piece, as before outside; the advancement and its child.

## Not tested

*   The client: how the chambers look, the bulbs' light.
*   A player walking in (the advancement trigger is called from a player tick; the test checks the advancement tree).
*   The cascading-worldgen warnings in the Default world (383 in the chambers run, 382 in the playtest suite
    before this stream) are not this stream's: the chamber driver loads no chunk (checked by counting loaded
    chunks around its layout and build), and the C&C world run had none.

## Fixes after the first playtest (`tr-chambers-fixes`)

*   **Floating buttons.** The wall dispensers' button is a floor button facing east. Backport Core's palette reads
    Minecraft's Flattening table backwards, which has floor and ceiling buttons only facing north; `face=floor,
    facing=east` scored the same against the wall button facing east, which came first, so it was placed as a wall
    button on air. `FaceAttached` maps every `face`/`facing` of oak and stone buttons and levers to 1.12.2's one
    `facing` (floor `up`, ceiling `down`; levers `up_x`/`up_z`/`down_x`/`down_z` by axis). `CORE_REQUESTS.md` #12.
*   **Ominous vaults looking normal.** A structure places the vault with `ominous=true`, and `createTileEntity` set the
    tile's flag from it, but the template's block entity data (no `ominous` key) was then read over it as false. The
    vault kept the ominous key and table, so it looked normal and refused a trial key. That is the playtest's vault: a
    normal trial key refused by an ominous vault drawn as a normal one. The click itself reaches the server (a key is
    not an `ItemBlock`; 1.12.2's client sends the click). Fixed in `TileEntityVault`, with a repair for vaults already
    generated (ominous settings mean ominous).
*   **Fast, spinning skeletons.** Not the spawner (it sets up mobs as 1.21's: `onInitialSpawn`, persistence,
    equipment). 1.12.2's strafing gives a mob that cannot step aside its full speed; patch P4 (`docs/PATCHES.md`).
    It applies to every strafing mob, as in 1.16 onwards.
*   **Breezes jumping too little.** The breeze's goals were not 1.21's; `docs/streams/breeze.md` has the port of
    `BreezeAi`.

Tests added: `chambers` checks the buttons and levers against the palette, every button, lever, ladder, hook and
torch of a built chamber for its support (100 checked, 30 floor buttons, none floating), every vault's look against
its key, through a save and load, the repair, and a real player's click through `processRightClickBlock` (a key from
the spawner's key table opens a normal vault; an ominous vault refuses it and takes an ominous key). `breeze` pins
`LongJump`'s and `Shoot`'s numbers, a hand-worked jump vector, the drag-free flight, and the cycle (at least 6 jumps in
600 ticks, one charge per landing). `tweaks` (new) pins P4: 0.0625 for a blocked strafe, 0.25 with the tweak off.

Results (`tr-chambers-fixes`): `-Pselftest=all` **3011 pass, 0 fail** (Default world, rerun on the same world);
`check_assets` 0 errors; P4 verified on Mojang's SRG `EntityMoveHelper`; release jar built last with `--rerun-tasks`.
