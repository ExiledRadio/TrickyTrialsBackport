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
| P3 | flora | `WorldClient` (client only) | the ambient loop's `block.randomDisplayTick(...)` becomes `FloraHooks.displayTick(block, ...)` (also the falling leaves) |
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
| P16 | animals | `EntityLiving.onInitialSpawn` (`func_180482_a`) | before each return: `AnimalsHooks.initialSpawn(this)` |
| P17 | animals | `EntitySheep.onInitialSpawn` (`func_180482_a`) | before each return: `AnimalsHooks.sheepSpawn(this)` |
| P18 | animals | `EntityAnimal.readEntityFromNBT` / `writeEntityToNBT` (`func_70037_a` / `func_70014_b`) | head: `AnimalsHooks.read(this, tag)` / `write(this, tag)` |
| P19 | animals | `EntityWolf.getAmbientSound` / `getHurtSound` / `getDeathSound` (`func_184639_G` / `func_184601_bQ` / `func_184615_bR`) | each returned sound becomes `AnimalsHooks.wolfSound(sound, this)` |
| P16 | copperGear | `EntityLiving.setEquipmentBasedOnDifficulty` (`func_180481_a`) | head: `if (CopperGearHooks.equipArmor(this, difficulty)) return;` |
| P20 | spears | `EntityZombie.setEquipmentBasedOnDifficulty` / `EntityPigZombie.setEquipmentBasedOnDifficulty` (`func_180481_a`) | before each return: `SpearEvents.spawnWeapon(this, difficulty)` |
| P21 | spears (client) | `ModelBiped.setRotationAngles` (`func_78087_a`) | head: `SpearModelHooks.pre(this, entity)`; before each return: `SpearModelHooks.post(this)` (entity stashed by `pre`; Mojang frames drop the parameter by the returns); `ModelZombie` / `ModelZombieVillager.setRotationAngles`, before each return: `SpearModelHooks.zombieArms(this)` |
| P24a | mounts | `EntityZombieHorse`: `processInteract` (`func_184645_a`); new `wearsArmor` (`func_190677_dK`), `isArmor` (`func_190682_f`), `canDespawn` (`func_70692_ba`) | head of `processInteract`: `switch (MountsHooks.zombieHorseInteract(this, player, hand))` (1 returns true, 2 false, 0 runs 1.12.2's body); the three new overrides return `MountsHooks.zombieHorseWearsArmor()`, `zombieHorseIsArmor(stack)`, `zombieHorseCanDespawn(this)` |
| P25 | copper | Fluidlogged API's `ISpecializedFluidNeighborInfo$Vanilla.canDisplaceI` (not a vanilla class) | the door/sign/ladder/sugar cane branch's `return true` becomes `return DoorWater.displaced(state)` (false for our doors) |
| P26 | decor | `ItemHangingEntity`: new `getSubItems` (`func_150895_a`) | `super.getSubItems(tab, items); PaintingItems.subItems(this, tab, items);` (the variant paintings after the random one) |
| P27 | decor | `Entity.getPickedResult` (Forge's, same name in SRG) | head: `ItemStack s = PaintingItems.pickedResult(this); if (s != null) return s;` (a vanilla painting gives its variant) |

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

The same hook gives every leaves block 1.21.5's falling leaf (`TintedParticleLeavesBlock`, 26.3's chance 0.01), other
mods' too, after the block's own display tick; spruce, pale oak, cherry and poplar are left out
(`flora/FallingLeaves`, `docs/streams/flora.md`).

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

## P16-P19: farm animal variants (1.21.5, stream 16)

All four live in `asm/AnimalsTransformer` and call `animals/AnimalsHooks`; each hook does nothing while `animals.enabled`
or its own setting is off. None adds a jump: P16, P17 and P18 insert void calls (before each `ARETURN`, or at the head),
P19 an `ALOAD 0` and a call taking the returned sound and the wolf and returning a sound, before each `ARETURN`.

- **P16** is 1.21.5's `finalizeSpawn` for cows, pigs and chickens (the variant of the biome at the animal's block) and
  wolves (a random voice). 1.12.2 calls `onInitialSpawn` where 1.21.5 calls `finalizeSpawn`: natural and chunk
  generation spawns, spawn eggs, `/summon` without NBT, spawners whose entry names only the mob, and a zombie's chicken
  jockey. Forge's `LivingSpawnEvent.SpecialSpawn` reaches only the natural spawns, hence the patch.
- **P17**: 1.12.2's sheep picks its color after `super.onInitialSpawn`, so the biome's color is set at its return.
- **P18** writes and reads vanilla's own keys, `variant` (`minecraft:temperate`, `warm`, `cold`) and the wolf's
  `sound_variant`, at the entity's root, so `/summon minecraft:cow ~ ~ ~ {variant:"minecraft:cold"}` and a spawn
  egg's `EntityTag` work as in 1.21.5. Forge's capabilities save under `ForgeCaps` only, which is why the
  `AnimalData` capability itself is not saved.
- **P19**: 1.12.2's mob sounds reach no Forge event with the entity (`PlaySoundAtEntityEvent` gets a null entity
  from `Entity.playSound`), so the three getters are wrapped. Shaking and steps stay every wolf's, as in 1.21.5.
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

## P20: zombies and zombie pigmen spawn with spears (1.21.11, stream 22)

1.21.11's `Zombie.populateDefaultEquipmentSlots` arms a zombie with chance 0.05 on Hard (0.01 otherwise), as 1.12.2
does, but picks `nextInt(6)`: 0 an iron sword, 1 an iron spear, else an iron shovel, where 1.12.2 picks `nextInt(3)`:
0 a sword, else a shovel. Half of 1.12.2's swords becoming spears gives exactly 1.21.11's 1 : 1 : 4, so the hook runs
after 1.12.2's own roll and swaps an iron sword for an iron spear with chance 1/2. `ZombifiedPiglin` holds a golden
spear 1 time in 20 instead of its golden sword; 1.12.2's zombie pigman always gets the sword, which the hook swaps with
chance 1/20. Husks and zombie villagers roll in `EntityZombie`'s method, as they inherit it in 1.21.11. Enchanting
comes after (`setEnchantmentBasedOnDifficulty`), so a spear is enchanted as the sword would have been. Off
(`spears.enabled` or `spears.mobSpears`), nothing changes. Forge's `SpecialSpawn` would reach only natural spawns, not
spawn eggs, `/summon`, reinforcements or spawners, hence the patch.

## P21: the spear's third-person arm (1.21.11, stream 22, client)

1.21.11's `HumanoidModel` has an `ArmPose.SPEAR` (the arm held out along the look while charging, swaying as the
charge wears on: `SpearAnimations.thirdPersonHandUse`) and a `STAB` swing (a thrust in place of the sword's swing:
`thirdPersonAttackHand`). 1.12.2's `ModelBiped` picks its arm angles in `setRotationAngles` with nothing to hook
between that and drawing, so the head call hides a spear's swing from 1.12.2's angles (`swingProgress` to 0) and the
return call restores it and sets the spear's angles. Armor models pose from the same entity through the same method,
and `ModelPlayer` copies the arms to its sleeves after `ModelBiped`'s method returns, so both follow.

A mob (1.21.11's `HumanoidMobRenderer`) takes `ArmPose.SPEAR` on any arm holding a spear, idle too: the arm about 44
degrees down from level along the look (`-pi/2 + head pitch + 0.8`), which with `spear_in_hand`'s tilt holds the spear
upright; a charge raises it from there. A player (`AvatarRenderer`) takes it only while charging, keeping the plain
item pose otherwise. Zombie-kind renderers (`AbstractZombieRenderer`) give the other arm the spear pose too, and
`AnimationUtils.animateZombieArms` skips the arms-out pose while the held item jabs. 1.12.2's `ModelZombie` and
`ModelZombieVillager` set their arms out after `ModelBiped`'s method, so a third hook before their returns puts back the
arms `post` set (Update Aquatic's drowned model extends `ModelZombie`; its swimming still applies after). It takes only
`this`, so no frame changes; family-notes' `ScanAll` verifies `ModelBiped`, `ModelZombie` and `ModelZombieVillager`
patched against the real client bytes. Off (`spears.enabled`), every call returns at once.

## P24a: the 1.21.11 zombie horse (stream 24)

1.12.2's zombie horse cannot be tamed (its `processInteract` returns false while it is untamed), has no armor slot
(`AbstractHorse.wearsArmor` is false and only `EntityHorse` overrides it) and never despawns (`EntityAnimal.canDespawn`
is false). 1.21.11's `ZombieHorse` is tamed by riding, eats red mushrooms, takes horse armor in its body slot, and
despawns while untamed (`removeWhenFarAway` true; taming or a player's use makes it persistent). None of these has a
Forge event: the GUI's armor slot asks `wearsArmor`, the slot itself and `replaceItemInInventory` ask `isArmor`, and
`despawnEntity` asks `canDespawn`. So the class gets the three overrides it lacks, each a one-line call to
`mounts/MountsHooks`, and its `processInteract` a head that runs 1.21.11's `mobInteract` (feeding, making it mad,
equipping a saddle or armor directly as 1.21.11 does, opening the inventory when sneaking, mounting). The names are MCP
or SRG to match the class being patched (`processInteract`'s own name says which). Not applied if another mod already
overrides one of the three. With `mounts.zombieHorses` (or `mounts.enabled`) off the hooks answer 1.12.2's way: no
armor, no despawning, and 0 (1.12.2's own body).

## P25: water stops at our doors (1.13 and later, through 1.21.11)

Reported on a real client: a copper door placed in water vanished at once. Every door of ours did it (copper doors at
every stage, waxed or not, and the pale oak door); vanilla's did not. In vanilla a door is never waterlogged and water
never flows into it (1.12.2's `BlockDynamicLiquid.isBlocked`, 1.21.11's `FlowingFluid.canHoldFluid`, both refusing any
door).

Fluidlogged API 3.3.3 (which Backport Core requires) replaces vanilla's flow. Its `canDisplaceI` is `isBlocked` turned
around, except the door/sign/ladder/sugar cane branch, which still says `true`: water may replace the block. A block is
replaced when it is displaceable and cannot hold the water. Fluidlogged API's own list makes every door waterloggable,
so vanilla's doors take the water instead (that is its choice for vanilla blocks, left alone). Backport Core's
`WaterRules` keeps the family's doors dry, as 1.13 and later do, so ours were displaceable and unable to hold water: the
water next to a door placed in water flowed into it and replaced both halves, and water spreading beside a door did the
same. Flow overrides (`canFluidFlow`) are not asked on that path, so no event or setting reaches it.

The patch puts `DoorWater.displaced(state)` in place of that branch's `return true`. It answers `false` for our doors
(`CopperBlocks.Door`, `WoodBlocks.Door`) and `true` for everything else, so nothing but our doors changes. The state's
local is live at the branch (javac's own frame keeps it), so no frame is added. Skipped, and logged, if the branch is not
found. Without Fluidlogged API the class does not exist and nothing is patched; vanilla's `isBlocked` already stops
water at a door.

Every family door Backport Core keeps dry has the same problem; the place for a fix covering all of them is Core.

## P26, P27: painting items with their variant (1.19.4)

1.19.4 (23w06a) lists every placeable painting in the creative inventory as a painting item that carries its variant,
`EntityTag:{variant:"minecraft:<name>"}` (1.21.5 keeps the id in its `painting/variant` component), after the random
painting, ordered by height then width. `ItemHangingEntity` (the painting's and the item frame's class) never overrode
`getSubItems`, so P26 adds it; the hook adds nothing for the item frame, or while `decor` is off. Placing such a painting
is an event (`DecorEvents.placeVariant`), not a patch.

Forge's `Entity.getPickedResult` answers a plain painting for any `EntityPainting`. P27's hook gives the painting's
variant instead, as pick-block does in 1.21.11; our own painting entity overrides the method itself.

