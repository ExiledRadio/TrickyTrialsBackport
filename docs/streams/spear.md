# Stream 22: spears and Lunge (1.21.11)

Package `spear`. Config `spears` (`enabled`, `mobSpears`, `mobCharges`). Entity id 55 (`fireproof_item`; 56-59
unused). Packet ids 110 (jab, client to server) and 111 (charge hit, server to client); 112-114 unused. Patches P20
and P21 (`docs/PATCHES.md`). Branch `claude/tr-spears-5744ff`, dev-server port 25903 (row 22 wraps onto row 3's).
Reference: the 1.21.11 jar (`Items`, `Item.Properties.spear`, `ToolMaterial`, `KineticWeapon`, `PiercingWeapon`,
`AttackRange`, `LivingEntity`/`Player.stabAttack`, `SpearUseGoal`, `SpearAnimations`, `ItemInHandRenderer`,
`HumanoidModel`, `Zombie`, `ZombifiedPiglin`, `enchantment/lunge.json`, the item tags, recipes, models, sounds and
`adventure/spear_many_mobs`), decompiled with Vineflower, and minecraft.wiki's *Java Edition 1.21.11* and *Spear*.

## Changelog section

*   **Spears**: wooden, stone, copper, iron, golden, diamond and netherite. Two sticks and the material on a diagonal
    make one; a netherite spear is a diamond spear upgraded at a smithing table, when a mod adds netherite. Spears never
    mine. A dropped netherite spear doesn't burn.
*   **Jab** (attack): hits everything in line between 2 and 4.5 blocks away (6.5 in creative), for the spear's damage
    (1 wooden and golden, 2 stone and copper, 3 iron, 4 diamond, 5 netherite), with knockback. Only at a full attack
    charge; slower tiers jab slower (0.65 s wooden to 1.15 s netherite).
*   **Charge** (hold use): after a short delay (0.75 s wooden down to 0.4 s netherite), whatever you run into is hit.
    While you are fast enough it knocks riders off their mounts and knocks things back, and for longest it deals
    damage by how much faster than the target you are moving: running, riding a horse, falling. Charging doesn't slow
    you down, and you can sprint. Each target can be hit again after half a second.
*   **Lunge** (I-III, spears only): a jab also pushes you forward along your look, more each level, at the cost of
    hunger and one more point of durability. Not while riding, gliding or swimming, or when you're too hungry to sprint.
*   Spears take Sharpness, Smite, Bane of Arthropods, Knockback, Fire Aspect, Looting, Unbreaking, Mending, Curse of
    Vanishing and Lunge, at the enchanting table and the anvil, but not Sweeping Edge.
*   **Mobs with spears**: zombies, husks and zombie villagers that spawn armed sometimes hold an iron spear, and one
    zombie pigman in 20 holds a golden spear. They charge at their target with it, back off and charge again.
*   Advancement **Mob Kabob**: hit five mobs in the same charge.

## How it works

*   **Items** (`ItemSpear`, `SpearTier`): 1.21.11's `Item.Properties.spear(material, swing, multiplier, delay, ...)`
    per tier. Attack damage `0 + material bonus` over a player's 1, attack speed `1 / swing - 4` over 4 (1.54, 1.33,
    1.18, 1.05, 1.05, 0.95, 0.87), the material's durability (59, 131, 190, 250, 32, 1561, 2031) and enchantability,
    repaired with planks, cobblestone, the copper ingot, iron, gold, diamond, netherite (`ingotNetherite`). Use lasts
    72000 ticks; the use action is `NONE` and the poses are drawn by the client code below. Every hit wears it by one
    (1.21.11's `Weapon(1)`). Times are kept as vanilla turns them into ticks, `(int) (seconds * 20.0F)` in float.
*   **Recipes**: 1.21.11's shape `"  X", " # ", "#  "` with `minecraft:stick` (mirrored too): `plankWood`,
    `cobblestone`, the copper ingot (Caves & Cliffs' or the copper stream's copy, through `Owners`; with neither, no
    copper recipe), `ingotIron`, `ingotGold`, `gemDiamond`. The netherite spear goes to Backport Core's
    `SmithingUpgrades.add` (diamond spear plus any netherite ingot), which a smithing table offers once a netherite mod
    is installed.
*   **The jab** (`SpearAttacks.jab`, 1.21.11's `PiercingWeapon.attack`): the client (`SpearClient`) takes the attack
    button whenever a spear is in the main hand, so it never mines or makes a normal hit (`MouseEvent` cancelled; a
    button already held is let go; `LeftClickBlock` is cancelled on both sides too). At full charge it sends packet
    110, restarts the attack timer, plays the jab and swings. The server checks the charge with vanilla's 5 ticks of
    leeway. Targets: `hitsAlong`, 1.21.11's `ProjectileUtil.getHitEntitiesAlong`: every entity whose box grown by 0.125
    the line from the eye crosses between 2 and 4.5 blocks (creative 2-6.5; mobs half, 1-2.25), the far end pushed out
    by the wielder's move along their look; a block in the way cuts the line, a block nearer than 2 stops the jab.
    Each target gets `stabAttack` (below) with the attack damage, knockback and no dismount.
*   **`stabAttack`** (`SpearAttacks.stab`, 1.21.11's `Player`/`LivingEntity.stabAttack`): enchantment bonus
    (Sharpness, Smite, Bane), and outside a charge the attack strength's scaling; Fire Aspect, damage of type `spear`
    ("was speared by", difficulty-scaled for mobs as 1.21.11's type says), knockback `0.4 + Knockback level / 2` along
    the wielder's facing (the wielder slows to 0.6), dismounting, thorns and Bane's slowness, the item's hit, the
    stats, the crit particles, 0.1 exhaustion. 1.12.2's own attack would make the hit a sword's, so this is a copy of
    1.12.2's attack order with 1.21.11's numbers; Forge's `AttackEntityEvent` still fires for each target.
*   **The charge** (`SpearAttacks.chargeTick`, 1.21.11's `KineticWeapon.damageEntities`), from the item's
    `onUsingTick` for players and mobs alike. After the tier's delay, each target in line not hit in the last 10 ticks
    is tested against three conditions, each with an end time and a speed: **dismount** while the wielder's speed along
    their look is at least the tier's (14 b/s wooden to 7 netherite), **knockback** while it is at least 5.1, and
    **damage** while the wielder closes on the target at 4.6 b/s or more (all five times less for a mob). Damage is the
    wielder's *base* attack damage (a player's 1, a zombie's 3) plus `floor(closing speed x multiplier)` (0.7 to 1.2),
    plus enchantments, unscaled. 0.7F is a hair under 0.7, so wood at exactly 10 b/s does 1 + 6 = 7, as in vanilla.
    Windows, after the delay: dismount 2.5-5 s, knockback 5.5-10 s, damage 8.75-15 s by tier.
*   **Speed**: 1.21.11's `getKnownSpeed` is the last tick's move. 1.12.2 resets its previous positions before a mob's
    item use runs, so `SpearEvents` samples every living entity's position at the start of its update and keeps the
    difference (`SpearAttacks.movement`); anything else (a boat, a minecart) uses `pos - lastTickPos`. A mob riding
    something takes the root vehicle's move, a player their own (which moves with the vehicle).
*   **The charge survives its own wear** (`ItemSpear.canContinueUsing`): 1.21.11 keeps a use going while the hand holds
    the same item (`ItemStack.isSameItem`). Forge's default wants the very same stack object, so before this a real
    client ended the charge at every hit (the server's slot update after the wear hands it a new stack) and the held
    button restarted it four ticks later: the charge sound again, the delay again, every target's cooldown forgotten,
    a hit or more a second with the sounds piling up. Fixed 2026-11-20 (branch `tr-spear-hitrate`).
*   **Hit feedback** (`SpearAttacks.feedback`, 1.21.11's `onKineticHit`): the hit sound and the view's jolt (packet 111)
    come at most once every 11 ticks a wielder, however many targets or hits.
*   **A new charge** forgets the last one's targets (`LivingEntityUseItemEvent.Start`). A charge that hits sends packet
    111 to everyone near: the hit sound at the wielder, and the wielder's view jolts (1.21.11 uses entity event 2, which
    1.12.2 reads as "hurt"). *Mob Kabob*'s trigger `trickytrialsbackport:spear_mobs` gets the number of living things
    this charge has hit; the advancement asks for 5.
*   **Moving while charging** (1.21.11's `UseEffects(canSprint true, speed 1)`): 1.12.2 cuts the walk to a fifth while
    an item is in use, right after `InputUpdateEvent`, so the input is raised five times there; sprinting may start
    mid-charge (1.12.2 forbids it while using an item).
*   **Lunge** (`EnchantmentLunge`, `SpearAttacks.lunge`): weight 5 (uncommon), max level 3, costs `5 + 8(l-1)` to
    `25 + 8(l-1)`, its own enchantment type (spears), on books and at the table, no exclusive set. After every jab
    (hit or not): not riding, gliding or in water, and a player needs more than 6 food or creative flight; then one more
    point of wear, `4 x level` exhaustion, a push of `0.458 x level` along the level part of the look (looking up 60
    degrees pushes half as far), and `item.spear.lunge_<level>`.
*   **Enchanting**: `ItemSpear.canApplyAtEnchantingTable` lets in 1.21.11's `enchantable/melee_weapon` set (Sharpness,
    Smite, Bane of Arthropods, Knockback, Fire Aspect, Looting) and whatever 1.12.2's types already allow on a
    damageable item (Unbreaking, Mending, Curse of Vanishing, Lunge). Sweeping Edge (swords only) stays out.
*   **Anvil** (`SpearEvents.onAnvil`): 1.12.2's anvil asks each enchantment's type, and the melee ones fit swords only,
    so a spear with a book or a second spear of its kind is worked out as the mace's anvil does: vanilla's merge, levels
    by 1.12.2's rarity costs (halved from a book), incompatible ones charged a level, prior work, renaming, durability.
*   **Mobs with spears** (P20): see `docs/PATCHES.md`. Zombie-kind mobs (`EntityZombie` and its subclasses: husks,
    zombie villagers, zombie pigmen) get `EntityAISpearUse` at priority 1, above 1.12.2's melee at 2, on joining a
    world: 1.21.11's `SpearUseGoal` (approach within 10 blocks, start the charge, run in, turn away 6-7 blocks once
    within 2, back off 9-11 when the charge (`delay + damage window` ticks) runs out, for at most 5 s, then again). It
    acts only while the mob holds a spear and has a target; 1.21.11's goal timers are halved for its every-other-tick
    goals, 1.12.2 runs a task every tick, so these are the real tick counts.
*   **Netherite spear on the ground** (`EntityFireproofItem`, entity id 55): 1.21.11's `fireResistant()`, Nether Update
    Backport's rule for its netherite items: no burning, fire damage ignored, floats up in lava.
*   **Models** (`tools/spear/generate.py`, `SpearClient.ByPerspective`): 1.21.11's `items/<spear>.json` picks the flat
    `item/<spear>` in the GUI, on the ground and in frames (and on shelves) and `item/<spear>_in_hand` (parent
    `item/spear_in_hand`, its long texture and display transforms) everywhere else. Both are ported and the item's
    baked model hands each perspective the right one.
*   **First person** (`SpearClient.onRenderHand`, `SpearPoses`): 1.21.11's spear branch of `ItemInHandRenderer`:
    while charging, `SpearAnimations.firstPersonUse` (raise, sway as the windows close, lower, the jolt of a hit);
    otherwise the hand's usual place and, for a jab, `firstPersonAttack`'s thrust over the tier's swing time.
*   **Third person** (P21, `SpearModelHooks`): the arm held out along the look while charging
    (`thirdPersonHandUse`), and the jab's thrust (`thirdPersonAttackHand`) instead of the sword's swing.
*   **Public for other code** (stream 24's mounted mobs): `SpearContent.spear(SpearTier)`, `SpearAttacks.jab`,
    `chargeTick`, `startCharge`, `stab`, `hitsAlong`, `motion`/`movement`/`setMotion`, `lunge`, `setChargeSpeed`
    (1.21.11's `Mob.chargeSpeedModifier`, by vehicle class) and `EntityAISpearUse`, which any `EntityCreature` can take.

## Tested headlessly

`./gradlew runServer -Pselftest=spears` and `-Pselftest=all` from a fresh world (port 25903, Core 1.3.0 as
`gradle.properties` builds), then `python -I tools/check_assets.py`, `python -I tools/lang/build_langs.py build`, and
the release build.

*   **`-Pselftest=spears`: `SELFTEST DONE pass=160 fail=0`.**
*   **`-Pselftest=all`, fresh world: `SELFTEST DONE pass=4456 fail=0`**, and again from another fresh world
    `pass=4450 fail=0` (other streams' tests vary their count). A first fresh-world run had 7 failures, all in the
    happy ghast's still-timeout check (`player on top: still timeout 0` and the six that follow from it). They did not
    come back in the two later runs, `-Pselftest=ghast` alone passes (260/0), and nothing in this stream touches the
    ghast or `world.playerEntities`; with `spears.enabled=false` that run passed too (4291/1, the one being this test's
    own `enabled` check). It looks like a flaky check in stream 17's test, not this stream.
*   **Assets: 181 blocks, 2888 states, 242 item models, 1044 sound files, 0 errors.**
*   **Release** (`./gradlew build --rerun-tasks --offline -PdeployDir= -PcoreDeployDir=`): built, 0 `/selftest/` entries in any of the three jars.

What the spears test checks:

*   Items: the seven registered, durability, one to a stack, enchantability, attack damage 1/2/2/3/1/4/5 and speed
    1/swing, 72000-tick use, no mining; iron repairs iron and diamond does not, any planks repair wood, cobblestone
    stone, the copper ingot copper; the netherite spear drops as a fireproof item lava doesn't hurt, the diamond one
    doesn't; the item entity is registered; the `spear` damage type and its death message key.
*   Tiers: delays (15, 12, 8 ticks), windows (wood dismount 100, iron 50 / 135 / 225, netherite damage 175), a mob's
    hold (237 for iron); the damage formula (iron at sprint speed 6, wood at 10 b/s 7, netherite at 14 b/s 17, a
    zombie's base with diamond 14); the speed conditions for players and for mobs (a fifth).
*   Recipes: wooden (any planks), stone, iron, golden, diamond, copper with whichever ingot is installed, the mirrored
    shape, and one stick not being enough.
*   Lunge: III, uncommon, not treasure, costs 5/13/21 to 25/41, spears only (not swords or bows), compatible with
    Sharpness, Knockback and Unbreaking; the table puts the melee set and Unbreaking on a spear, not Sweeping Edge,
    Efficiency or Power; 300 rolls at level 30 offer Lunge on a spear and on a book and never Sweeping Edge.
*   Anvil: a Sharpness V book on a spear (5 levels), a Lunge III book (3 levels), not Sweeping Edge.
*   Real jabs by a spawned server player through the jab packet's handler: reach (1.5 and 5 blocks out of reach, 3 in),
    an iron jab does 3 and knocks back and wears the spear, no second jab before the timer is full, creative reaching
    5 blocks, one diamond Sharpness V jab hitting two zombies in line for 7 each, a block within 2 blocks stopping it.
*   Charges: nothing before the delay; at 5.6 b/s 6 damage and knockback; the same target waits 10 ticks; 10 b/s does
    10; a target running away at 4 b/s cuts it to 6; walking (2 b/s) does nothing; still damaging after 7 s, nothing
    after 11.5 s; a zombie riding a pig knocked off at 9 b/s.
*   Lunge II pushing 0.916 along the look, level only, one more wear, 8 exhaustion; none at 6 food; half as far
    looking 60 degrees up; nothing without Lunge.
*   Hit rate: a worn copy of the spear in hand keeps the charge going (the client's case), another spear ends it; a
    40-tick sprinting charge into a standing zombie hits it 4 times (every 10 ticks), a second zombie stepping in 5 ticks
    later keeps its own clock (4 hits), the hit sound plays 3 times (0, 15, 30), and the charge is still held. With the
    fix: `-Pselftest=spears` 166/0 and `-Pselftest=all` 5490/0 from a fresh world, pinned to the E-cores.
*   *Mob Kabob*: one netherite charge through four zombies, not yet; a fifth reached thanks to the move ahead, earned.
*   Mobs: a zombie has the spear charge task; a zombie charging at 2 b/s hits a zombie 1.7 blocks ahead (not one at 3)
    for its base 3 + 1; P20 applied to both classes; half of 1.12.2's iron swords become spears (4000 rolls), shovels
    stay, 1 zombie pigman in 20 gets a golden spear (6000 rolls); 6000 zombies spawned on Hard through
    `onInitialSpawn` come out swords, spears and shovels about 1 : 1 : 4.

## Not tested

*   Anything drawn: the icon and in-hand models, first-person raise, sway and thrust, third-person arm poses (P21 is
    client-only and a headless server never loads `ModelBiped`), the hit jolt.
*   The client's input: the attack button jabbing and never mining, the held button let go, walking at full speed and
    starting a sprint while charging. All of it is client code a headless server does not run.
*   Sounds playing, and packet 111 reaching a client.
*   A real player's speed from movement packets (the test sets the sampled move directly), and mobs' charge AI
    actually walking, turning away and backing off over many ticks.
*   The netherite smithing upgrade: no netherite mod is in the dev run, so Core's table has nothing to offer it with.
*   With Caves & Cliffs installed (its copper ingot): the copper spear's recipe and repair use `Owners`, as the copper
    gear stream's do, but this stream was not run with it.
*   Languages: Mojang's names (spears, Lunge, *Mob Kabob*, the death messages, the subtitles) are in all 18 files; our
    two config options' names and tooltips are English, as every stream's config text so far.

## Not the same as 1.21.11

*   **The third-person jab** runs on 1.12.2's swing length (6 ticks) rather than the tier's 13-23; first person uses the
    tier's own swing time. The third-person charge pose has no in-hand item motion (`thirdPersonUseItem`), only the arm.
*   **Zombie-shaped models** (zombies, husks, zombie villagers, zombie pigmen) keep their arms-forward pose when charging,
    since `ModelZombie` sets its arms after `ModelBiped`'s method.
*   **Mobs' plain melee with a spear** uses 1.12.2's mob attack (damage type `mob`, no Lunge). It rarely happens: the
    spear charge task outranks melee whenever a zombie has a target.
*   **Piglins** carry golden spears in 1.21.11 (1 crossbow-less piglin in 10); piglins are Nether Update Backport's, so
    that is theirs to add with this stream's item (`trickytrialsbackport:golden_spear`).
*   **Husks riding camel husks** with iron spears are stream 24's.
*   An attack bound to a keyboard key rather than a mouse button still mines and hits normally with a spear in hand; only
    mouse-bound attack is turned into the jab.
