# Requests for Backport Core

**Status (2026-11-13):** built in Backport Core 1.2.0 on its branch `tr-core-requests` (not merged): #1, #2, #4 (the
hook), #5, #8 and most of #9; #3 and #7 were already in Core and #7 is now used here; #6 is answered below. What
is still open is marked **Open**.

What Tricky Trials Backport needs from Backport Core (`backportcore` 1.0.0) or from another family mod
through Core. Nothing here is built in Core's repo by this mod; each request says what we do meanwhile.

## 1. Caves & Cliffs' 1.17 blocks in the ownership table

**Done (Core 1.2.0):** in `BlockOwnership`'s table, with a per-name `provides`; items and effects in the new
`ItemOwnership`. `Owners.declareOlderOwners` is gone.

**Want:** `BlockOwnership`'s static table to list Caves & Cliffs (`cavesandcliffsbackport`) as the owner
of tuff and the copper set, as it lists Village & Pillage's 1.13 blocks:
`tuff`, `copper_ore`, `raw_copper_block`, and for each of `""`, `exposed_`, `weathered_`, `oxidized_`,
with and without `waxed_`: the block (`copper_block` / `<stage>copper`), `<stage>cut_copper`,
`<stage>cut_copper_stairs`, `<stage>cut_copper_slab`, `<stage>cut_copper_double_slab`.

**Meanwhile:** `common/Owners.declareOlderOwners()` declares them for Caves & Cliffs from our mod
constructor. `declare` keeps the first claim, so this is harmless once Core or the owner does it.

Also wanted: an item ownership lookup next to the block one (copper ingot, raw copper, honeycomb, the
ominous bottle), so `Owners.item` can move into Core.

## 2. Owner remapping of our copies (Rule 1, third bullet)

**Done (Core 1.2.0):** `ownership.CopyRemapping` renames a family mod's copies to the owner's on `MissingMappings`
(blocks, items, effects).

When Caves & Cliffs or Village & Pillage is added to a world that has our copies, the owner (or Core's
migration code) should remap `trickytrialsbackport:<name>` to `<owner>:<name>` in
`RegistryEvent.MissingMappings` for blocks and items. Our copies, all with the owner's states and metadata:

- Caves & Cliffs: every name in request 1, plus the items `raw_copper` and `copper_ingot`.
- Village & Pillage: the item `ominous_bottle` (metadata 0-4) and the effect `bad_omen`.

The reverse (owner removed, our copy takes over) is ours to write, in each stream that copies.

## 3. Trial chambers: structure templates and placement

**In Core already:** Core 1.1.0/1.2.0 has the modern template loader, jigsaw pools, processors, `StartRules` and
`TerrainAdaptation` (`docs/API.md`, Structures). Stream 10 can start on them; nothing more is asked of Core.

**Want:** Core's modern structure template loader (1.21 NBT structure files with block palettes,
jigsaw pools and processors read into 1.12.2 states), plus a way to place a jigsaw structure underground
from its start piece.

**Placement (decided 2026-11-07):** with Caves & Cliffs installed (real height -64 to 319), vanilla's y -40 to -20
for the start piece, read through `WorldHeights`; without it, y 16-29, the whole structure within y 1-55.
Terrain adapters are only needed to keep the entrance corridors from breaking into oceans or ravines
(`TerrainHeights.isWater`), not for the surface height. 26.3's abandoned camps (stream 32) need the same
loader, on the surface.

**Meanwhile:** stream 10 is not started. Trial spawners and vaults (stream 7) work on their own when
placed.

## 4. Lightning rods oxidize (1.21.9) - a change to Caves & Cliffs' block (Rule 2)

**Hook done (Core 1.2.0):** `copper.Oxidation` is the shared table; stream 20 can add the rods' stages to it.

1.21.9 gives the lightning rod four oxidation stages and waxed forms, like other copper. The rod is
Caves & Cliffs' block. **Want:** either Caves & Cliffs registers the eleven new rod blocks itself when
Tricky Trials is installed, or Core offers a hook such as
`Oxidation.register(Block from, Block to)` / `Oxidation.next(IBlockState)` that every family mod's copper
uses, so our copper and theirs scrape, wax and age through one table. We would then add the rods in
stream 20 only when Caves & Cliffs is installed and does not.

**Meanwhile:** nothing; stream 20 leaves rods alone.

## 5. Shared oxidation and waxing

**Done (Core 1.2.0):** `copper.Oxidation` (table and aging rule); our `copper/Weathering` delegates to it. **Open
for Caves & Cliffs:** entering its own copper there (until then `Weathering.addOwnersBlocks` enters it by name).

Copper blocks come from three mods (Caves & Cliffs' 1.17 set, our 1.21 and 1.21.9 sets). **Want:** one
oxidation table in Core (block to next stage, waxed form, scraped form) so an axe, honeycomb and lightning
treat all of them alike, and the "nearby less-oxidized copper slows aging" rule counts every mod's copper.

**Meanwhile:** `copper` keeps its own table and also recognises blocks named `*copper*` under
`cavesandcliffsbackport` by name for the nearby-copper count.

## 6. Retextures for Core's modern pack

**Answered:** not Core's modern pack (that is the 1.14 Texture Update, on whatever is installed). 1.21.6's ghast is
the same layout at twice the size, so this mod replaces it by version through `replaceAsset` (stream 33). 1.21.5's
cows, mooshrooms and spawn eggs are new models, not textures: they belong with stream 16's renderers. The
iron-block and iron-door sounds are block sound types: stream 25 (tweaks), as the spawner's are in stream 33.

1.21's changes to vanilla textures 1.12.2 already has belong in Core's `modern` pack: none in 1.21 itself
that we know of; 1.21.5's new cow, mooshroom and spawn-egg textures and 1.21.6's new ghast texture are
the ones to add (`manual_map.py`), and 1.21.5's iron-block and iron-door sounds are a resound.

## 7. Waterloggable blocks (for the adapter, later)

**Done here:** Core's adapter exists; stairs, slabs (not double slabs), copper grates and trapdoors, the heavy core
and the walls (Core's `ModernWall`) implement `Waterloggable`. Later streams' blocks do the same as they come.

1.21-1.21.11 blocks that vanilla makes waterloggable, to move onto Core's `Waterloggable` adapter when it
exists (until then they are not waterloggable): copper grates and copper trapdoors; every stairs, slab
and wall (tuff, polished tuff, tuff brick, resin brick; pale oak stairs, slab, fence and trapdoor); the
heavy core; resin clumps; copper chests, copper golem statues, copper bars, chains and lanterns; shelves;
the dried ghast. Not waterloggable in vanilla: copper doors and bulbs, the crafter, trial spawner and
vault, pale moss carpet, pale hanging moss, leaf litter, copper torches.

## 8. One modern wall class for the family

**Done (Core 1.2.0):** `block.ModernWall`; tuff walls use it (1.21.11's multipart blockstates). **Open for Caves &
Cliffs:** moving its `BlockCCWall` walls onto it.

Our tuff walls extend 1.12.2's `BlockWall` (low sides only); Caves & Cliffs has a 1.16+ wall with none,
low and tall sides (`common/BlockCCWall` in its repo, all actual state, metadata 0) and is listing it for
Core too. **Want:** that wall in Core, so tuff, deepslate, resin brick and every later wall look and join
alike. **Meanwhile:** ours stay vanilla-style; moving them is a blockstate and class swap with no change to
saved worlds (both store metadata 0).


## 9. Presentation: what 1.21.x's look and feel needs from Core

**Status:** done in Core 1.2.0: 1 (volume), 2 (removal by path), 3 (Music Frequency, music while paused, Music
Toast, UI volume), 4 (volume preview), 6's Hold/Toggle for attack and use, 7 (per-value tooltips), 8 (blurred
panorama), 9 (P22), 10 (P23), 12 (snap on release), 13 (P24 sunrise, P25 clouds). **Open:** 5 (1.21.11's Video
Settings layout and presets, Cloud Distance, Chunk Fade, texture filtering), 6's moves between Accessibility and
Controls and Allow Cursor Changes, 11 (1.21.9's F3 rework and Debug key category), 12's arrow keys (1.12.2 screens
have no keyboard focus), 14 (the 1.20 logo layout, which Trails & Tales' own request #10.1 also asks for), and the
music note's color cycling in the toast (drawn white).

Stream 33 (`lookfeel`, `docs/streams/lookfeel.md`) hands Core everything its API takes. These pieces of 1.21 to
1.21.11 are Core's single slots (or need a Core hook) and are not built:

1. **Menu track volume.** `addMenuMusic` plays every track at full volume; 1.21.6's five menu tracks are `music.menu`
   entries at volume 0.4 (1.21's were 0.4 and 0.8; 1.20's 0.4). **Want:** `addMenuMusic(sound, weight, volume)`.
   **Meanwhile:** full volume.
2. **Removing another backport's menu track by its vanilla path.** Vanilla 1.21 has no 1.18, 1.19 or 1.20 tracks
   in the menu, but Core applies a version's removals only while that version is installed, so a pack of Caves &
   Cliffs and Tricky Trials would still play 1.18's. **Want:** `removeMenuMusic` matching the path whatever the
   namespace (`music/game/wending`), or `replaceMenuMusic(...)` for "the pool is exactly these". **Meanwhile:**
   `LookAndFeel.OLDER_MENU_TRACKS` removes the twelve by the names the family uses
   (`<modid>:music/game/<vanilla path>`); a backport that names them otherwise keeps them.
3. **1.21.6's music options and ticker** (Sounds screen and Core's music ticker): Music Frequency (Default 10-20
   minutes, Frequent, Constant), Music Toast (Never, Pause Menu, Toast and Pause Menu: the track's title),
   music playing on while paused, and the "UI" volume slider for button clicks (with Friendly/Hostile Mobs, now
   vanilla text). **Meanwhile:** none.
4. **1.21.9's Sounds screen**: volume sliders play a preview sound outside a world.
5. **1.21.11's Video Settings**: the Preset slider (Fast, Fancy, Fabulous! as presets) and the three sections
   (Display, Quality & Performance, Preferences), Cloud Distance (1.21.6, 2-128 chunks, Core's cloud renderer),
   Chunk Fade, texture filtering. **Meanwhile:** our Reduce FPS when sits after VSync in 1.12.2's list.
6. **1.21.9's Accessibility and Controls layout**: Auto-Jump, Toggle Sneak and Toggle Sprint leave Accessibility
   for Controls, which gains a "Controls..." button there; Attack/Destroy and Use Item/Place Block get Hold/Toggle
   (needs Core's key state handling, as Toggle Sneak); Allow Cursor Changes (LWJGL 2 has no system cursors).
7. **Per-value tooltips on `CycleOption`.** 1.21.2's Reduce FPS when explains each value. **Meanwhile:** the AFK
   value's tooltip.
8. **1.20.5's blurred panorama.** 1.21.9's loading screen (and every 1.20.5+ menu) draws the panorama blurred.
   **Want:** `PanoramaRenderer.renderBlurred(...)`. **Meanwhile:** our loading screen draws it sharp under the
   menu tile.
9. **Menus without a world at 60 frames a second** (1.21.2's `FramerateLimitTracker`; 1.12.2 holds them at 30 in
   `Minecraft.getLimitFramerate`, a patch).
10. **1.21.2's slot highlight** (`slot_highlight_back` under the item, `slot_highlight_front` over it, so the
    item stays visible): `GuiContainer.drawScreen` draws 1.12.2's white overlay; a patch or a Core-drawn slot.
11. **1.21.9's F3 rework** (F3 in menus, the Debug Options screen on F3+F6, each part off, on with F3 or always)
    and **1.21.11's Debug key category** (rebindable debug keys). Core owns F3.
12. **Slider snapping** (1.21.11: sliders snap on release, arrow keys move one value) in Core's option widgets.
13. **1.21.11's sky**: sunrise and sunset color react to rain and thunder (`WorldProvider.calcSunriseSunsetColors`
    is not in `FogPipeline.Sky`); clouds following world time. **Want:** a `sunrise(...)` step, a cloud offset.
14. **The 1.20 title logo layout** (shared with Trails & Tales): 1.21.11's `minecraft.png` and `edition.png` only
    fit 1.20's layout, which Core does not draw, so this mod does not register a title logo.
