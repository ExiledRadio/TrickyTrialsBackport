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

**Placement:** vanilla puts the chamber's start piece at y -40 to -20 (world -64 to 320). Proposed for
1.12.2's 0-255, the same proportion of the way up from bedrock: start at **y 16-29**, the whole
structure within y 1-55; vanilla's own range when `WorldHeights` reports a 1.18-height world. Terrain
adapters are only needed to keep the entrance corridors from breaking into oceans or ravines
(`TerrainHeights.isWater`), not for the surface height.

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
and wall (tuff, polished tuff, tuff brick, resin brick; pale oak stairs, slab, fence and trapdoor); the
heavy core; resin clumps; copper chests, copper golem statues, copper bars, chains and lanterns; shelves;
the dried ghast. Not waterloggable in vanilla: copper doors and bulbs, the crafter, trial spawner and
vault, pale moss carpet, pale hanging moss, leaf litter, copper torches.
