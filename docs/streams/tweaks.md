# Stream 25: smaller changes (tweaks)

1.21 to 1.21.11's changes to things 1.12.2 already has. Package `tweaks`, config category `tweaks` (one switch per
change, on by default, under the stream's `enabled`), self-tests `-Pselftest=tweaks` (P4) and `-Pselftest=mechanics`
(everything below). Patches: `docs/PATCHES.md` (P4, P5-P15). Values were read from 1.21.11's decompiled classes
(`G:/MCDownloads/tr-mech-decomp/out`) and its data, and checked against minecraft.wiki's version pages.

## Built

| Setting | Version | What changes | How |
| --- | --- | --- | --- |
| `ridersThroughPortals` | 1.21 | A mount or vehicle with riders goes through nether and end portals; the riders follow and mount it again. Leaving the End, a player rider who has not seen the credits keeps everyone there (1.21's `canTeleport`). Nether portals take a ridden vehicle at once (1.21's transition time 0 for non-players). | `PortalTravel`: after each world's entity tick, ridden root vehicles touching `portal` / `end_portal` blocks are sent with 1.12.2's own `changeDimension`, then each rider (recursively) to the vehicle's new spot. |
| `pearlsAcrossDimensions` | 1.21 | A pearl that went through a portal still teleports its thrower, into the pearl's dimension (5 damage for a player, through the dimension change's grace as 1.21 allows; 5% endermite). | `PortalTravel`: thrower remembered by UUID and given back to the pearl's copy; `ProjectileImpactEvent.Throwable`. |
| `pearlsLoadChunks` | 1.21.2 | A player's pearl keeps its 3x3 chunks loaded and ticking; chunks it leaves stay 40 ticks (the ticket timeout). Pearls leave with their thrower on logout and come back on login. | `EnderPearlChunks`: one `ForgeChunkManager` ticket per pearl, never kept over a restart. |
| `tntThroughPortals` | 1.21 | Primed TNT goes through nether portals and, once through, cannot break portal blocks. | `PortalTravel` (1.12.2's TNT never runs the portal countdown). |
| `portalCooldowns` | 1.21.2 | Boats and minecarts 0.5 s, projectiles 0.1 s, a player-ridden mount its rider's. | P5. |
| `saddleRecipe` | 1.21.6 | 3 leather around an iron ingot make a saddle; unlocked with the first leather. | Recipe registry event, recipe advancement. Restart. |
| `saddleLoot` | 1.21.6 | Dungeon, desert and jungle temple, stronghold corridor chests: 1-5 leather (same weight) for the saddle. | `LootTableLoadEvent`. Wild Update's ancient city table already has 1.21.11's leather. |
| `shearsRemoveEquipment` | 1.21.6 | Shears cut a mob's leads (its own and those it holds), else take off horse armor or a llama's carpet, else a saddle (horses, donkeys, mules, pigs); not while ridden or sneaking. 1.21.6's unequip and snip sounds. | `EntityInteract`. |
| `horsesEatCarrots` | 1.21.6 | Horses, donkeys and mules eat carrots as apples: 3 health, 60 ticks of growth, 3 temper. | `EntityInteract`. |
| `leads` | 1.21.6 | Sneak-use a mob to tie the mobs you lead to it (within 12 blocks, center to center); leads snap at 12 blocks. | `EntityInteract`; P7. |
| `leadRecipe` | 1.21.6 | 5 string make 2 leads; 1.12.2's slimeball recipe is removed. | Recipe registry event. Restart. |
| `projectileKnockback` | 1.21 | Projectiles knock back along their flight (potions and fireworks away from themselves), shot by anyone or by a dispenser. | P6. |
| `axeShield` | 1.21 | Axe in the main hand, shield in the off hand, not sneaking: use raises the shield. | `RightClickBlock` at highest priority, ahead of the family's stripping and scraping events. |
| `witchRedstone` | 1.21 | Witches always drop 4-8 redstone (Looting +0-1), in a pool of its own. | `LootTableLoadEvent`. |
| `noSpawnChunks` | 1.21.9 | No spawn chunks and no start area. With Trails & Tales, its radius is set to 0. | P15, or T&T's P10. Restart. |
| `noSpawnInvulnerability` | 1.21.4 | No 3 s of immunity after joining or respawning. | `EntityJoinWorldEvent`. |
| `waterBreathingRefillsAir` | 1.21.4 | Water Breathing (and Update Aquatic's Conduit Power) add 4 air a tick under water. | `LivingUpdateEvent`. |
| `elytraEndsSprint` | 1.21.4 | Gliding ends sprinting. | Client player tick. |
| `elytraClimbables` | 1.21.5, 1.21.11 | Gliding into a ladder or other climbable ends the flight; vines (any `*vines*`) are glided through. | `LivingUpdateEvent`. |
| `protectionStacks` | 1.21 | Fire Protection's burn time (-15% a level) and Blast Protection's knockback (-15% a level, at most 100%) sum over the armor worn. | P10. |
| `panic` | 1.21, 1.21.11 | Panic for 2 s after a panic-causing damage type (1.21's `#panic_causes`), adult polar bears only from fire, cactus, freezing and lightning; mounts steered by a mob never panic. | 1.12.2's panic goals swapped on join. |
| `crystalsFireImmune` | 1.21 | End crystals ignore fire damage. | P11. |
| `mixedDoubleDoors` | 1.21 | Doors of any two materials pair into a double door. | P8. |
| `splashDistance` | 1.21.2 | Splash strength from the potion's box (moved to the hit) to the mob's box, grown by the projectile margin. | P12. |
| `pumpkinHidesMapMarker` | 1.21.2 | A pumpkin on the head hides you from other players' maps. | P13. |
| `toolFreeDrops` | 1.21.4 | Iron doors, the four pressure plates, brewing stands, ender chests (obsidian), and the family's bells, lanterns, soul lanterns and copper doors drop when broken without the right tool. | `HarvestCheck`. |
| `saddleThorns` | 1.21.5 | Thorns on a horse's saddle hurts its attackers. | `LivingHurtEvent`. |
| `portalsDropBlocks` | 1.21 | The End's obsidian platform breaks what it replaces as if mined. | P14. |
| `fireNearPlayers` | 1.21.5 | Fire neither burns nor spreads unless a player is within 128 blocks, or `allowFireTicksAwayFromPlayer` is true. | P9. |
| `headNames` | 1.21.9 | A mob head keeps its custom name when placed and mined. | Place event and drops event; the name in the skull's Forge tile data. |
| `gameRules` | 1.21.5, 1.21.9 | `tntExplodes`, `allowFireTicksAwayFromPlayer`, `allowEnteringNetherUsingPortals`, `spawnMonsters`, `pvp`, `commandBlocksEnabled`, `spawnerBlocksEnabled`, with 1.21's names of the time (1.21.11's renames are out of scope). | `GameRuleTweaks`. |

## Not the same as vanilla

- `tntExplodes false`: 1.21.5 never primes the TNT block; here it primes and then fizzles (the block is used up).
- `spawnerBlocksEnabled false` stops 1.12.2's spawners; this mod's trial spawners do not read it yet.
- Saddle Thorns: Curse of Binding on a saddle and the saddle's enchantment glint on the mob are not done.
- Shears in a dispenser do not cut leads (1.21.6 lets them).
- The End exit portal and the eye-filled end portal (1.21.5's "drop the blocks in their radius") still overwrite blocks;
  only the obsidian platform drops them.

## Owned elsewhere or left

- 1.20 and 1.20.5 changes (player head names, hopper and mob tweaks, `enderPearlsVanishOnDeath`,
  `playersNetherPortal*Delay`, `projectilesCanBreakBlocks`): Trails & Tales.
- Copper equipment on geared mobs (1.21.9): needs stream 20's copper armor and tools.
- Mob weapon preference (1.21.4): needs a rewrite of 1.12.2's pickup decision in `EntityLiving.updateEquipmentIfNeeded`; left.
- Leashed boats (1.21): 1.12.2 leashes only `EntityLiving`; needs its own lead entity link and renderer; left.
- The world border stopping projectiles (1.21.2): left.
- `locatorBar` (1.21.6): stream 18. `disablePlayerMovementCheck` (1.21.2): server anti-cheat, technical.
- Streams 16 (farm animal variants), 22 (spear zombies) and 24 (zombie horses): their own streams.
