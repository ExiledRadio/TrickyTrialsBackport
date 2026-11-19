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
| P5 | tweaks | `Entity.getPortalCooldown` (`func_82147_ab`) | its `300` becomes `PortalTravel.cooldown(this)` |
| P6 | tweaks | `EntityLivingBase.attackEntityFrom` (`func_70097_a`) | before `if (attacker != null) { ... knockBack(...) }`: `if (MechanicsHooks.projectileKnockback(this, source, attacker)) goto <after the if-else>` |
| P7 | tweaks | `EntityCreature.updateLeashedState` (`func_110159_bB`) | its two `10.0F` become `Leads.snapDistance()` |
| P8 | tweaks | `ItemDoor.placeDoor` (`func_179235_a`) | after each of the four `getBlock()` compared with `door`: `MechanicsHooks.doorMatch(found, door)` |
| P9 | tweaks | `BlockFire.updateTick` (`func_180650_b`) | head: `if (MechanicsHooks.fireIdle(this, world, pos, rand)) return;` |
| P10 | tweaks | `EnchantmentProtection.getFireTimeForEntity` / `getBlastDamageReduction` (`func_92093_a` / `func_92092_a`) | head: `if (MechanicsHooks.protectionStacks()) return MechanicsHooks.fireTime(...)` / `blastKnockback(...)` |
| P11 | tweaks | `EntityEnderCrystal.attackEntityFrom` (`func_70097_a`) | head: `if (MechanicsHooks.crystalIgnores(source)) return false;` |
| P12 | tweaks | `EntityPotion.applySplash` (`func_190543_a`) | `this.getDistanceSq(entity)` becomes `MechanicsHooks.splashDistanceSq(this, entity)` |
| P13 | tweaks | `MapData.updateVisiblePlayers` (`func_76191_a`) | before each return: `MechanicsHooks.hideMapMarkers(this, player)` |
| P14 | tweaks | `Teleporter.placeInPortal` (`func_180266_a`) | its `world.setBlockState(pos, state)` become `MechanicsHooks.platformBlock(world, pos, state)` |
| P15 | tweaks | `World.isSpawnChunk` (`func_72916_c`); `MinecraftServer.initialWorldChunkLoad` (`func_71222_d`) | the body becomes `return SpawnChunks.isSpawnChunk(this, x, z)`; the `192`, `-192` and `625` become `SpawnChunks.prepareBlocks()`, `prepareStart()`, `prepareCount()`. Not applied when Trails & Tales Backport is installed (its P10 patches the same sites) |
| P16 | copperGear | `EntityLiving.setEquipmentBasedOnDifficulty` (`func_180481_a`) | head: `if (CopperGearHooks.equipArmor(this, difficulty)) return;` |

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

## P5-P15: the 2026-11-16 audit's 1.21 behavior changes (stream 25)

All eleven live in `asm/MechanicsTransformer` (one method per patch, dispatched by class) and call
`tweaks/MechanicsHooks`, `PortalTravel`, `Leads` or `SpawnChunks`; each hook returns 1.12.2's value while its
`tweaks.*` setting or `tweaks.enabled` is off. What each change is, and where vanilla's numbers come from, is in
`docs/streams/tweaks.md`.

- **P5** 1.21.2's `getDimensionChangingDelay`: boats and minecarts 10 ticks, projectiles 2, a mount whose first rider
  is a player that player's 10, anything else 300. 1.12.2's subclasses that override it (the player's 10) are kept.
- **P6** adds the one jump of these patches, to the label after 1.12.2's knockback if-else, which already carries a frame
  (the else branch reaches it too); nothing is pushed across the jump. The hook calls `knockBack` itself, so Forge's
  `LivingKnockBackEvent` and Nether Update's P5 (which replaces `knockBack`'s body) still apply.
- **P7** and **P12** swap a constant or a call for a static call with the same stack.
- **P8** inserts after each `getBlock()` an `ALOAD 3` and a call taking two blocks and returning one.
- **P9**, **P10**, **P11** use `Asm.headReturnIf`'s shape: the new label at the head carries `F_SAME`.
- **P13** inserts a void call before each `RETURN`.
- **P14** swaps `World.setBlockState(BlockPos, IBlockState)Z` for a static call with the same stack.
- **P15** replaces `isSpawnChunk`'s whole body (no jump left) and swaps three `SIPUSH` constants for static calls. The
  transformer asks the class loader for Trails & Tales' `mechanics/SpawnChunks.class` (its coremod jar is on the class
  path before any game class loads) and leaves both classes alone when it is there; `SpawnChunks.postInit` then sets
  that mod's `radius` to 0.

## P16: mobs wear copper (1.21.9)

1.21.9's `Mob.populateDefaultEquipmentSlots`: with chance `0.15 * clampedAdditionalDifficulty` a mob gets armor of one
tier for every piece it wears: the first roll is `nextInt(3)` over leather, copper and gold (1.12.2: `nextInt(2)`,
leather or gold), raised three times with chance 0.1087 each (1.12.2: 0.095), through chainmail, iron and diamond. The
pieces go on helmet, chestplate, leggings, boots in that order (1.12.2 starts at the boots: MC-182478, fixed in 24w33a for 1.21.2); after the first, each
further piece stops the roll with chance 0.1 on Hard, 0.25 otherwise, as before. The tiers come out leather 23.60%,
copper 32.23%, gold 33.29%, chainmail 9.73%, iron 1.10%, diamond 0.04%, minecraft.wiki's figures.

`EntityLiving.setEquipmentBasedOnDifficulty` is where 1.12.2's zombies, husks, zombie villagers, skeletons, strays and
the bogged get their armor (each calls it before choosing its weapon), the same mobs 1.21.9 puts copper on. The hook
returns false, and 1.12.2's roll runs, while `copperGear.enabled` or `copperGear.mobArmor` is off. Weapons and
enchanting are unchanged.
