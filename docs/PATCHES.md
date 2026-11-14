# Bytecode patches

Tricky Trials Backport's own small coremod (`com.exiledradio.trickytrialsbackport.asm.TrickyTrialsCore`, named in the
jar's manifest with `FMLCorePluginContainsFMLMod`, and by `-Dfml.coreMods.load` in the dev run tasks). One transformer
per patch. No MixinBooter. Each patch only rewrites a call or adds a call into the mod's own code; the hook keeps
1.12.2's behavior while its stream is switched off. If a target method is not found (another mod rewrote it), the patch
is skipped and logged, never half-applied.

Names are matched as MCP (dev) or SRG (a real instance). SortingIndex 1001, after FML's deobfuscation.

| # | Stream | Class | What changes |
| --- | --- | --- | --- |
| P1 | flora | `BlockCactus.updateTick` (`func_180650_b`) | head: `if (FloraHooks.cactusTick(this, world, pos, state, rand)) return;` |
| P2 | flora | `BlockDeadBush.canSustainBush` (`func_185514_i`) | head: `if (FloraHooks.deadBushSoil(state)) return true;` |
| P3 | flora | `WorldClient` (client only) | the ambient loop's `block.randomDisplayTick(...)` becomes `FloraHooks.displayTick(block, ...)` |
| P4 | tweaks | `EntityMoveHelper.onUpdateMoveHelper` (`func_75641_c`) | the blocked strafe's `f1 = f` becomes `f1 = TweaksHooks.blockedStrafeSpeed(f, f1)` |

## P1: cacti grow flowers (1.21.5)

1.21.5's `CactusBlock.randomTick`: a cactus with air above ages to 15 whatever its height (1.12.2's tick did nothing
on a cactus three tall); at age 8, where a cactus could grow above, a cactus flower opens on top one time in ten, or one
time in four on a cactus three or more tall; at 15 a cactus under three tall grows one taller and starts again at 0.
Forge's crop growth events wrap the tick as they wrap 1.12.2's. A flowered cactus stops growing (its top is no longer
air), as in 1.21.5.

## P2: dead bushes on farmland (1.21.5)

1.21.5's dead bush is a `DryVegetationBlock` and stands on `#dry_vegetation_may_place_on`: sand, red sand, terracotta,
`#dirt` (grass, mycelium, podzol, moss, mud too) and farmland. The hook answers true for those; anything else falls
through to 1.12.2's list. Placement and survival both go through `canSustainBush` (Forge's `canSustainPlant` asks it
too), so one patch covers both.

Not the same as 1.21.5: farmland under a dead bush stays farmland (1.12.2 keeps farmland under any plant its
`canSustainPlant` accepts; 1.21.5 keeps it only under `#maintains_farmland`, the crops, and lets it dry to dirt, where
the dead bush still stands).

## P3: desert ambience (1.21.5, client)

1.21.5's `AmbientDesertBlockSoundsPlayer` gives sand (and red sand) an idle sound, one time in 2100 under open air when
sand lies at the surface 8 blocks away on all four sides, and the dead bush its dry rustle, one time in 130 on sand or
terracotta two blocks deep (two times in three skipped on red sand or terracotta). Both are `animateTick` sounds, and
1.12.2's sand and dead bush have no display tick of their own to override, so the one call `WorldClient` makes for
every block of its ambient loop is routed through `FloraHooks.displayTick`, which makes the same call and then plays
these. The stack is unchanged, so nothing else in the method moves. The dry grass's own sound is in its block class.

## P4: a blocked strafe keeps the strafing speed (1.16 and later, through 1.21.11)

1.12.2's `EntityMoveHelper`, strafing (`strafe(forward, right)`, which a skeleton's `EntityAIAttackRangedBow` calls every
tick near its target): where the side step's block is not walkable, the mob walks forward instead, and 1.12.2 also sets
the speed to the full movement speed attribute (`f1 = f`). That is four times the strafing speed (0.25 against
0.25 x 0.25 for a skeleton), so a strafing skeleton next to a wall, an edge or a grate darts at its target and swings
round. Today's `MoveControl` (1.16.5's and 1.21.11's) keeps the strafing speed there. The site is found by its shape
(the float field set to 0, then a float local copied to another), not by field names. The two floats are locals the
method already holds; no parameter is read. Checked with `family-notes/tools/verify` on Mojang's SRG classes: vanilla
and patched both verify. With `tweaks.enabled` off the hook returns 1.12.2's value.
