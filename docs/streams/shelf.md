# Stream 21: shelves (1.21.9)

Package `shelf`. Config `shelves` (`enabled`). No entities; packet ids 105-109 unused (the tile entity's own update
packet carries everything). Branch `claude/tr-shelves-6b2391`, dev-server port 25902. Reference: 1.21.11's decompiled
`ShelfBlock`, `ShelfBlockEntity`, `SideChainPartBlock`, `SideChainPart`, `SelectableSlotContainer`, `ShelfRenderer`,
`Blocks`, `FireBlock`, `FuelValues`; blockstates, models, recipes, loot tables, tags and sounds from the 1.21.11 jar;
minecraft.wiki's *Shelf*.

## Changelog section

*   **Shelves** in every wood: oak, spruce, birch, jungle, acacia and dark oak always; pale oak with the pale garden;
    cherry and bamboo with Trails & Tales; mangrove with Wild Update; crimson and warped with Nether Update. Six
    stripped logs (two rows of three) make six shelves.
*   A shelf holds three items, shown on its front. Use it to swap the item in your hand with the slot you point at;
    with an empty hand you take the item.
*   A **powered** shelf swaps all three of its slots with the three rightmost hotbar slots. Powered shelves side by
    side, facing the same way, join into a row of up to three, and the row swaps with the six or nine rightmost
    hotbar slots, whichever shelf you use. The front shows when a shelf is powered and how it is joined.
*   A comparator behind a shelf reads 1, 2 and 4 for its left, middle and right slots (up to 7).
*   Hoppers fill and empty shelves. Breaking a shelf drops its items. Overworld shelves burn and are fuel (1.5 items);
    crimson and warped ones do neither.

## How it works

*   **Woods** (`ShelfWood`, vanilla's twelve in vanilla's order): a wood exists when its planks do: 1.12.2's six
    always, `trickytrialsbackport:pale_oak_planks` (stream 12), and the owners' `cherry_planks`, `bamboo_planks`
    (Trails & Tales), `mangrove_planks` (Wild Update), `crimson_planks`, `warped_planks` (Nether Update), each only
    when `Owners.present(owner)` and the owner registered them. The shelves register at `EventPriority.LOWEST`, after
    every mod's normal-priority blocks, so the answer is the owners' own. Every shelf name is declared ours in Core's
    `BlockOwnership` (1.21.9 shipped them first).
*   **Block** (`BlockShelf`): wood, hardness 2, blast resistance 3, axe (drops by hand), `block.shelf.*` sound type,
    the wood's map color, three collision boxes (top board, back, bottom board) and a ray trace through them, so a
    click between the boards lands on the back as with vanilla's shape. Solid back face only. Fire 30/20 (vanilla's
    `setFlammable(shelf, 30, 20)`) for overworld woods, 0 for crimson and warped; lava ignites all (vanilla's
    `ignitedByLava`). Waterloggable through Backport Core's `Waterloggable` (Fluidlogged API holds the water).
*   **States**: `facing` and `powered` in metadata (8 of 16 values); `side_chain` (unconnected, left, center, right)
    comes from the tile entity in `getActualState`, since 4 x 2 x 4 does not fit in 1.12.2's metadata. The
    blockstate is 1.21.11's own multipart file. Unpowered shelves always read unconnected, as vanilla resets them.
*   **Tile entity** (`TileEntityShelf`): three stacks saved as vanilla's `Items` list with `Slot` bytes, vanilla's
    `align_items_to_bottom`, and `side_chain`. Every change (`markDirty`) saves, updates comparators and sends the tile
    to the clients. It survives the metadata changes of powering (`shouldRefresh` only on a block change). It is an
    `IInventory` (vanilla's `ListBackedContainer`), so hoppers put items into the first slot that takes them and take
    from it.
*   **Use** (`onBlockActivated`, main hand only, front face only): vanilla's `getHitSlot` (thirds of the face, left to
    right as seen from in front). Unpowered: `swapSingleItem` (a creative player keeps a copy when the slot was
    empty); sounds `place_item`, `single_swap`, `take_item`; an empty hand on an empty slot passes. Powered:
    `swapHotbar`, shelf `i` of `n` slot `j` against hotbar slot `9 - (n - i) * 3 + j`, `multi_swap` when anything
    moved; consumed either way. The client answers as vanilla's (pass with an empty hand; the server still acts).
    Sneaking with an item in hand uses the item, not the shelf, as vanilla (and 1.12.2) do.
*   **Rows** (`ShelfChain`, vanilla's `SideChainPartBlock` line by line): a shelf powered by `isBlockPowered` (vanilla's
    `hasNeighborSignal`) joins its left neighbor first, then its right, while the row stays at three or fewer; a
    shelf losing power, or broken, lets go of both neighbors. Only powered wooden shelves facing the same way join;
    woods mix. Vanilla runs this from `onPlace` on every state change; 1.12.2 calls `onBlockAdded` only for a new
    block, so `neighborChanged` runs it after a power change. `activate` / `deactivate` sounds.
*   **Comparator**: vanilla answers only a comparator reading from behind. 1.12.2's `getComparatorInputOverride` is
    not told who asks, so the shelf answers when a comparator behind it faces it (directly, or through one solid
    block, the two ways a comparator reads), and 0 otherwise.
*   **Item**: `ItemShelf` (fuel by 1.12.2's wooden-material rule, 300 ticks; 0 for crimson and warped).
*   **Recipes**: `###`, `   `, `###` of the wood's stripped log, 6 shelves, group `shelf`. Stripped logs are 1.13's:
    the owner from Core's table (Village & Pillage's for the six vanilla woods), else the wood's owner, else any family
    mod's copy (Update Aquatic, Trails & Tales, ...), else, for 1.12.2's six woods only, the plain log of that wood, so
    shelves can be made in a game with no stripped logs at all.
*   **Renderer** (`client.RenderShelf`, `ShelfRenderer`'s numbers): items at a quarter size, 5/16 apart, a quarter block
    toward the back, centered on the middle (or on the bottom board with `align_items_to_bottom`), turned with the
    shelf. 1.12.2 has no `on_shelf` display transform, so `client.OnShelfTransform` carries 1.21.11's: none for
    flat items and for special-rendered ones without one (chests, banners, shulker boxes); a half turn for block
    models (`block/block.json`); a quarter turn for fences, walls (Core's `ModernWall` too) and anvils; a half turn and
    4 pixels back for shelves; heads, the dragon head, the shield and beds with their own. It is applied as
    `ItemTransform.apply` does (translate, rotate X Y Z, scale). The item is lifted by its extents *after* that
    transform, as `ItemStackRenderState.getModelBoundingBox` does: the quads of a baked model, or the shape 1.12.2's
    special renderer draws (heads, shield, bed).
*   **Game events** (`ShelfGameEvents`), where 1.21.9 posts them, with the shelf's state as context and no source
    entity: `block_activate` for any change of the contents (`setChanged()`: hoppers, commands) and on being powered,
    `block_deactivate` on losing power, `item_interact_finish` for a single swap, `entity_interact` for each shelf of
    a row swap. A row part changing posts nothing, as vanilla's `setBlock`. They go through Backport Core's game event
    module (`GameEvents.emit`), which Wild Update's sculk sensors listen to. The module is Core's from 1.4.0 and this
    mod builds against 1.3.0, so it is reached by name; every listener's mod (Wild Update) needs 1.4.0, so the module
    is there whenever something can hear.
*   **Assets**: `python -I tools/shelf/generate.py` (12 multipart blockstates, 84 block models and the 7 templates,
    12 shelf and 12 stripped log textures, item models, 11 sound events, names and 6 subtitles).

## Not done

*   **Compasses and clocks on a shelf** point as in a hand without an entity. Vanilla's shelf is an `ItemOwner`
    (its position and facing). 1.12.2's compass and clock properties read an entity or an item frame only.
*   **Special-rendered extents** (heads, the dragon head, the shield, beds) are taken from what 1.12.2's renderers
    draw, measured from their models, not from vanilla's special-model extents. Small differences in height are
    possible there.
*   Vanilla skips `item_interact_finish` for an item whose `use_effects` say no interact vibrations; 1.12.2 items
    have no such component, so it is always posted.
*   **Ctrl + pick block** with contents (vanilla's `container` component on the item): 1.12.2's pick block copies no
    tile data; a placed shelf item that carries `BlockEntityTag` does keep its items.
*   **Pathfinding** (`isPathfindable` only through water): 1.12.2's walk node processor already treats the shelf as a
    non-full block.
*   Comparator corner case: with a comparator behind the shelf, one beside or in front of it that also faces it reads
    the same value (vanilla gives it 0). A patch in `BlockRedstoneComparator.calculateInputStrength` would fix it.
*   **Languages**: Mojang's names and subtitles are in all 18 files; our two config strings are still English in the
    other 17, as every stream's are until the family-wide translation pass.

## Tested headlessly

`./gradlew runServer -Pselftest=shelves` and `-Pselftest=all` (port 25902, fresh world, pinned to the E-cores with
`start /affinity FFFF0000`), then `python -I tools/check_assets.py`. **2026-11-20** (branch `tr-shelves-finish`, on main
b53e3f2): **`SELFTEST DONE pass=5532 fail=0`** (all); shelves alone **338/0**; with the family mods **460/0**;
**197 blocks, 3048 states, 288 item models, 1299 sound files, 0 errors**. Release jar built last with
`--rerun-tasks --offline -PdeployDir= -PcoreDeployDir=`: 0 `/selftest/` entries, reobfuscated (`func_` names).
First build (2026-11-18): all 3472/0, alone 324, family 443, without Village & Pillage 437.

*   Which woods exist: alone (Core 1.3.0, no family mod): oak, spruce, birch, jungle, acacia, dark oak, pale oak, with
    the plain logs in the six vanilla recipes. With Trails & Tales 1.0.0, Wild Update 1.1.0, Nether Update 1.1.0 and
    Village & Pillage 2.7.0 (Core 1.4.0, `-Pcore_version=1.4.0`; on 2026-11-20 Wild Update 1.1.1, Village & Pillage
    2.7.1, Core 1.4.2): all twelve, Village & Pillage's stripped logs for
    the vanilla six, each owner's for its own. Without Village & Pillage: all twelve, Trails & Tales' stripped log
    copies for the vanilla six.
*   Registration exactly when the wood exists, ownership, the item, metadata round trips, 32 states.
*   Numbers: hardness, blast resistance, axe, drops by hand, sounds, fire 30/20 or none, fuel 300 or none, lava, all
    11 sound events.
*   Recipes: six logs make six shelves; a filled middle row makes none.
*   Placement: facing the player for all four directions, powered on a redstone block, through the item; back face
    solid, front open, three collision boxes; waterlogged when placed into water (Fluidlogged API in the dev run).
*   Hit slots for all four facings, the edges, the top and back.
*   Single swap: place, swap, take, pass on an empty slot, the back, the top, the off hand; slot 2; creative keeps the
    item. Through the server's right-click path with a real player; sneaking with a block places it instead.
*   Comparator: 0 empty, 1/2/4 and sums for the slots, a real comparator putting out 7, 0 from in front or beside,
    7 through a solid block.
*   Rows: three shelves join as left / center / right; a fourth stays alone; the row swaps hotbar 0-8 left to right and
    back from either end; a lone powered shelf swaps hotbar 6-8; a pair 3-8; a shelf facing another way does not join;
    spruce joins oak; losing power and breaking let the neighbors go; re-powering rejoins.
*   NBT: vanilla's `Items`, `align_items_to_bottom` and the row part read back; clients get the items; powering keeps
    the tile; breaking drops the contents and the shelf.
*   Hoppers: one item at a time into the first slot that takes it, merging; taking from it; signal 7.
*   Game events: placing and taking (`item_interact_finish`), a contents change (`block_activate`), power on and off
    (`block_activate`, `block_deactivate`), a row swap (`entity_interact`, by the row's last shelf), nothing for a row
    part changing. With the family mods (Core 1.4.2's module switched on by Wild Update), an immediate Core listener
    beside the shelf hears all four. Without Core's module (Core 1.3.0) nothing is sent and nothing fails.

## Not tested

*   The client: the models, the powered and joined fronts, the items drawn on the shelf (`OnShelfTransform`, the
    lift), the sounds as heard. The release checks' client test covers the shelf display.
*   A real Wild Update sculk sensor reacting (it listens through the same Core module the test listener uses).
*   A real hopper block ticking (its insert and extract calls are tested).
