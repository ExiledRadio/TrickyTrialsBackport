# Requests for Backport Core

What Tricky Trials Backport needs from Backport Core (`backportcore` 1.0.0) or from another family mod
through Core. Nothing here is built in Core's repo by this mod; each request says what we do meanwhile.

## 1. Caves & Cliffs' 1.17 blocks in the ownership table

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

When Caves & Cliffs or Village & Pillage is added to a world that has our copies, the owner (or Core's
migration code) should remap `trickytrialsbackport:<name>` to `<owner>:<name>` in
`RegistryEvent.MissingMappings` for blocks and items. Our copies, all with the owner's states and metadata:

- Caves & Cliffs: every name in request 1, plus the items `raw_copper` and `copper_ingot`.
- Village & Pillage: the item `ominous_bottle` (metadata 0-4) and the effect `bad_omen`.

The reverse (owner removed, our copy takes over) is ours to write, in each stream that copies.

## 3. Trial chambers: structure templates and placement

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

1.21.9 gives the lightning rod four oxidation stages and waxed forms, like other copper. The rod is
Caves & Cliffs' block. **Want:** either Caves & Cliffs registers the eleven new rod blocks itself when
Tricky Trials is installed, or Core offers a hook such as
`Oxidation.register(Block from, Block to)` / `Oxidation.next(IBlockState)` that every family mod's copper
uses, so our copper and theirs scrape, wax and age through one table. We would then add the rods in
stream 20 only when Caves & Cliffs is installed and does not.

**Meanwhile:** nothing; stream 20 leaves rods alone.

## 5. Shared oxidation and waxing

Copper blocks come from three mods (Caves & Cliffs' 1.17 set, our 1.21 and 1.21.9 sets). **Want:** one
oxidation table in Core (block to next stage, waxed form, scraped form) so an axe, honeycomb and lightning
treat all of them alike, and the "nearby less-oxidized copper slows aging" rule counts every mod's copper.

**Meanwhile:** `copper` keeps its own table and also recognises blocks named `*copper*` under
`cavesandcliffsbackport` by name for the nearby-copper count.

## 6. Retextures for Core's modern pack

1.21's changes to vanilla textures 1.12.2 already has belong in Core's `modern` pack: none in 1.21 itself
that we know of; 1.21.5's new cow, mooshroom and spawn-egg textures and 1.21.6's new ghast texture are
the ones to add (`manual_map.py`), and 1.21.5's iron-block and iron-door sounds are a resound.

## 7. Waterloggable blocks (for the adapter, later)

1.21-1.21.11 blocks that vanilla makes waterloggable, to move onto Core's `Waterloggable` adapter when it
exists (until then they are not waterloggable): copper grates and copper trapdoors; every stairs, slab
and wall (tuff, polished tuff, tuff brick, resin brick; pale oak stairs, slab, fence, trapdoor, leaves and
signs); the
heavy core; resin clumps; copper chests, copper golem statues, copper bars, chains and lanterns; shelves;
the dried ghast. Not waterloggable in vanilla: copper doors and bulbs, the crafter, trial spawner and
vault, pale moss carpet, pale hanging moss, leaf litter, copper torches.

## 8. One modern wall class for the family

Our tuff walls extend 1.12.2's `BlockWall` (low sides only); Caves & Cliffs has a 1.16+ wall with none,
low and tall sides (`common/BlockCCWall` in its repo, all actual state, metadata 0) and is listing it for
Core too. **Want:** that wall in Core, so tuff, deepslate, resin brick and every later wall look and join
alike. **Meanwhile:** ours stay vanilla-style; moving them is a blockstate and class swap with no change to
saved worlds (both store metadata 0).

## 9. One wood set and one boat entity for the family

Pale oak (stream 12) needed a whole wood set, signs with their own tile and renderer, and a boat entity.
Trails & Tales has the same code for cherry and bamboo (`common/wood`, `boat`), Wild Update has mangrove, and
26.3's poplar (stream 30) will need it again. **Want:** Core's own wood set (blocks extending vanilla's
shapes, sign tile and renderer, axe stripping, boat and chest boat entities with the wood in their data),
so each mod registers a wood by name and every boat and sign is one entity and one tile across the family.
**Meanwhile:** `palegarden/wood` and `palegarden/boat` are copies of Trails & Tales' code under our ids
(`trickytrialsbackport:sign`, `trickytrialsbackport:boat`, entity id 30); moving onto Core's is a rename
in `MissingMappings`.

