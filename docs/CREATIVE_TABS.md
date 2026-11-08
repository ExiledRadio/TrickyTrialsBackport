# Creative tabs

Tricky Trials Backport has no creative tab of its own. Each block and item is in the vanilla tab Minecraft 1.19.2 gives it, next to its vanilla neighbor in 1.19.2's order, through Backport Core's creative placement (`creative.CreativePlacement`; the rule and the table are in Backport Core's `docs/CREATIVE_TABS.md`). Items are placed by their vanilla name: `common/Content` places every block with an item and every item, and the main class's pre-init the spawn eggs. Items newer than 1.19.2 (tuff and copper sets, the crafter, breeze rod, heavy core, wind charge, mace) go where Backport Core's table puts them.

"After" and "Before" are the stacks either side of it with this mod and Backport Core alone, from a dev client's `-Dbackportcore.creativeDump` (an item is put after its nearest 1.19.2 neighbor before it, or before the one after it, whichever is of its kind). With other backports installed an item moves to stay by its 1.19.2 neighbor: stripped logs follow the warped stem once the Nether Update adds it, and so on.

## Building Blocks

| Item | After | Before |
|---|---|---|
| `tuff` | `polished_andesite` | `tuff_slab` (this mod) |
| `tuff_slab` | `tuff` (this mod) | `tuff_stairs` (this mod) |
| `tuff_stairs` | `tuff_slab` (this mod) | `tuff_wall` (this mod) |
| `tuff_wall` | `tuff_stairs` (this mod) | `chiseled_tuff` (this mod) |
| `chiseled_tuff` | `tuff_wall` (this mod) | `polished_tuff` (this mod) |
| `polished_tuff` | `chiseled_tuff` (this mod) | `polished_tuff_slab` (this mod) |
| `polished_tuff_slab` | `polished_tuff` (this mod) | `polished_tuff_stairs` (this mod) |
| `polished_tuff_stairs` | `polished_tuff_slab` (this mod) | `polished_tuff_wall` (this mod) |
| `polished_tuff_wall` | `polished_tuff_stairs` (this mod) | `tuff_bricks` (this mod) |
| `tuff_bricks` | `polished_tuff_wall` (this mod) | `tuff_brick_slab` (this mod) |
| `tuff_brick_slab` | `tuff_bricks` (this mod) | `tuff_brick_stairs` (this mod) |
| `tuff_brick_stairs` | `tuff_brick_slab` (this mod) | `tuff_brick_wall` (this mod) |
| `tuff_brick_wall` | `tuff_brick_stairs` (this mod) | `chiseled_tuff_bricks` (this mod) |
| `chiseled_tuff_bricks` | `tuff_brick_wall` (this mod) | `grass_block` |
| `copper_ore` | `iron_ore` | `coal_ore` |
| `copper_block` | `iron_block` | `smooth_stone_slab` |
| `exposed_copper` | `diamond_block` | `weathered_copper` (this mod) |
| `weathered_copper` | `exposed_copper` (this mod) | `oxidized_copper` (this mod) |
| `oxidized_copper` | `weathered_copper` (this mod) | `chiseled_copper` (this mod) |
| `chiseled_copper` | `oxidized_copper` (this mod) | `exposed_chiseled_copper` (this mod) |
| `exposed_chiseled_copper` | `chiseled_copper` (this mod) | `weathered_chiseled_copper` (this mod) |
| `weathered_chiseled_copper` | `exposed_chiseled_copper` (this mod) | `oxidized_chiseled_copper` (this mod) |
| `oxidized_chiseled_copper` | `weathered_chiseled_copper` (this mod) | `copper_grate` (this mod) |
| `copper_grate` | `oxidized_chiseled_copper` (this mod) | `exposed_copper_grate` (this mod) |
| `exposed_copper_grate` | `copper_grate` (this mod) | `weathered_copper_grate` (this mod) |
| `weathered_copper_grate` | `exposed_copper_grate` (this mod) | `oxidized_copper_grate` (this mod) |
| `oxidized_copper_grate` | `weathered_copper_grate` (this mod) | `cut_copper` (this mod) |
| `cut_copper` | `oxidized_copper_grate` (this mod) | `exposed_cut_copper` (this mod) |
| `exposed_cut_copper` | `cut_copper` (this mod) | `weathered_cut_copper` (this mod) |
| `weathered_cut_copper` | `exposed_cut_copper` (this mod) | `oxidized_cut_copper` (this mod) |
| `oxidized_cut_copper` | `weathered_cut_copper` (this mod) | `cut_copper_stairs` (this mod) |
| `cut_copper_stairs` | `oxidized_cut_copper` (this mod) | `exposed_cut_copper_stairs` (this mod) |
| `exposed_cut_copper_stairs` | `cut_copper_stairs` (this mod) | `weathered_cut_copper_stairs` (this mod) |
| `weathered_cut_copper_stairs` | `exposed_cut_copper_stairs` (this mod) | `oxidized_cut_copper_stairs` (this mod) |
| `oxidized_cut_copper_stairs` | `weathered_cut_copper_stairs` (this mod) | `cut_copper_slab` (this mod) |
| `cut_copper_slab` | `oxidized_cut_copper_stairs` (this mod) | `exposed_cut_copper_slab` (this mod) |
| `exposed_cut_copper_slab` | `cut_copper_slab` (this mod) | `weathered_cut_copper_slab` (this mod) |
| `weathered_cut_copper_slab` | `exposed_cut_copper_slab` (this mod) | `oxidized_cut_copper_slab` (this mod) |
| `oxidized_cut_copper_slab` | `weathered_cut_copper_slab` (this mod) | `waxed_copper_block` (this mod) |
| `waxed_copper_block` | `oxidized_cut_copper_slab` (this mod) | `waxed_exposed_copper` (this mod) |
| `waxed_exposed_copper` | `waxed_copper_block` (this mod) | `waxed_weathered_copper` (this mod) |
| `waxed_weathered_copper` | `waxed_exposed_copper` (this mod) | `waxed_oxidized_copper` (this mod) |
| `waxed_oxidized_copper` | `waxed_weathered_copper` (this mod) | `waxed_chiseled_copper` (this mod) |
| `waxed_chiseled_copper` | `waxed_oxidized_copper` (this mod) | `waxed_exposed_chiseled_copper` (this mod) |
| `waxed_exposed_chiseled_copper` | `waxed_chiseled_copper` (this mod) | `waxed_weathered_chiseled_copper` (this mod) |
| `waxed_weathered_chiseled_copper` | `waxed_exposed_chiseled_copper` (this mod) | `waxed_oxidized_chiseled_copper` (this mod) |
| `waxed_oxidized_chiseled_copper` | `waxed_weathered_chiseled_copper` (this mod) | `waxed_copper_grate` (this mod) |
| `waxed_copper_grate` | `waxed_oxidized_chiseled_copper` (this mod) | `waxed_exposed_copper_grate` (this mod) |
| `waxed_exposed_copper_grate` | `waxed_copper_grate` (this mod) | `waxed_weathered_copper_grate` (this mod) |
| `waxed_weathered_copper_grate` | `waxed_exposed_copper_grate` (this mod) | `waxed_oxidized_copper_grate` (this mod) |
| `waxed_oxidized_copper_grate` | `waxed_weathered_copper_grate` (this mod) | `waxed_cut_copper` (this mod) |
| `waxed_cut_copper` | `waxed_oxidized_copper_grate` (this mod) | `waxed_exposed_cut_copper` (this mod) |
| `waxed_exposed_cut_copper` | `waxed_cut_copper` (this mod) | `waxed_weathered_cut_copper` (this mod) |
| `waxed_weathered_cut_copper` | `waxed_exposed_cut_copper` (this mod) | `waxed_oxidized_cut_copper` (this mod) |
| `waxed_oxidized_cut_copper` | `waxed_weathered_cut_copper` (this mod) | `waxed_cut_copper_stairs` (this mod) |
| `waxed_cut_copper_stairs` | `waxed_oxidized_cut_copper` (this mod) | `waxed_exposed_cut_copper_stairs` (this mod) |
| `waxed_exposed_cut_copper_stairs` | `waxed_cut_copper_stairs` (this mod) | `waxed_weathered_cut_copper_stairs` (this mod) |
| `waxed_weathered_cut_copper_stairs` | `waxed_exposed_cut_copper_stairs` (this mod) | `waxed_oxidized_cut_copper_stairs` (this mod) |
| `waxed_oxidized_cut_copper_stairs` | `waxed_weathered_cut_copper_stairs` (this mod) | `waxed_cut_copper_slab` (this mod) |
| `waxed_cut_copper_slab` | `waxed_oxidized_cut_copper_stairs` (this mod) | `waxed_exposed_cut_copper_slab` (this mod) |
| `waxed_exposed_cut_copper_slab` | `waxed_cut_copper_slab` (this mod) | `waxed_weathered_cut_copper_slab` (this mod) |
| `waxed_weathered_cut_copper_slab` | `waxed_exposed_cut_copper_slab` (this mod) | `waxed_oxidized_cut_copper_slab` (this mod) |
| `waxed_oxidized_cut_copper_slab` | `waxed_weathered_cut_copper_slab` (this mod) | `cobblestone_stairs` |
| `raw_copper_block` | `coal_block` | `packed_ice` |

## Redstone

| Item | After | Before |
|---|---|---|
| `copper_trapdoor` | `oak_trapdoor` | `exposed_copper_trapdoor` (this mod) |
| `exposed_copper_trapdoor` | `copper_trapdoor` (this mod) | `weathered_copper_trapdoor` (this mod) |
| `weathered_copper_trapdoor` | `exposed_copper_trapdoor` (this mod) | `oxidized_copper_trapdoor` (this mod) |
| `oxidized_copper_trapdoor` | `weathered_copper_trapdoor` (this mod) | `waxed_copper_trapdoor` (this mod) |
| `waxed_copper_trapdoor` | `oxidized_copper_trapdoor` (this mod) | `waxed_exposed_copper_trapdoor` (this mod) |
| `waxed_exposed_copper_trapdoor` | `waxed_copper_trapdoor` (this mod) | `waxed_weathered_copper_trapdoor` (this mod) |
| `waxed_weathered_copper_trapdoor` | `waxed_exposed_copper_trapdoor` (this mod) | `waxed_oxidized_copper_trapdoor` (this mod) |
| `waxed_oxidized_copper_trapdoor` | `waxed_weathered_copper_trapdoor` (this mod) | `oak_fence_gate` |
| `copper_bulb` | `redstone_lamp` | `exposed_copper_bulb` (this mod) |
| `exposed_copper_bulb` | `copper_bulb` (this mod) | `weathered_copper_bulb` (this mod) |
| `weathered_copper_bulb` | `exposed_copper_bulb` (this mod) | `oxidized_copper_bulb` (this mod) |
| `oxidized_copper_bulb` | `weathered_copper_bulb` (this mod) | `waxed_copper_bulb` (this mod) |
| `waxed_copper_bulb` | `oxidized_copper_bulb` (this mod) | `waxed_exposed_copper_bulb` (this mod) |
| `waxed_exposed_copper_bulb` | `waxed_copper_bulb` (this mod) | `waxed_weathered_copper_bulb` (this mod) |
| `waxed_weathered_copper_bulb` | `waxed_exposed_copper_bulb` (this mod) | `waxed_oxidized_copper_bulb` (this mod) |
| `waxed_oxidized_copper_bulb` | `waxed_weathered_copper_bulb` (this mod) | `tripwire_hook` |
| `crafter` | `dropper` | `iron_trapdoor` |
| `copper_door` | `dark_oak_door` | `exposed_copper_door` (this mod) |
| `exposed_copper_door` | `copper_door` (this mod) | `weathered_copper_door` (this mod) |
| `weathered_copper_door` | `exposed_copper_door` (this mod) | `oxidized_copper_door` (this mod) |
| `oxidized_copper_door` | `weathered_copper_door` (this mod) | `waxed_copper_door` (this mod) |
| `waxed_copper_door` | `oxidized_copper_door` (this mod) | `waxed_exposed_copper_door` (this mod) |
| `waxed_exposed_copper_door` | `waxed_copper_door` (this mod) | `waxed_weathered_copper_door` (this mod) |
| `waxed_weathered_copper_door` | `waxed_exposed_copper_door` (this mod) | `waxed_oxidized_copper_door` (this mod) |
| `waxed_oxidized_copper_door` | `waxed_weathered_copper_door` (this mod) | (end of the tab) |

## Miscellaneous

| Item | After | Before |
|---|---|---|
| `raw_copper` | `iron_ingot` | `copper_ingot` (this mod) |
| `copper_ingot` | `raw_copper` (this mod) | `gold_ingot` |
| `breeze_rod` | `blaze_rod` | `heavy_core` (this mod) |
| `heavy_core` | `breeze_rod` (this mod) | `gold_nugget` |
| `bogged spawn egg` | `blaze spawn egg` | `breeze spawn egg` (this mod) |
| `breeze spawn egg` | `bogged spawn egg` (this mod) | `cave_spider spawn egg` |
| `wind_charge` | `fire_charge` | `writable_book` |

## Combat

| Item | After | Before |
|---|---|---|
| `mace` | `totem_of_undying` | (end of the tab) |
