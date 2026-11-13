# Stream 8: effects and potions (1.21)

Package `effects`. Config `effects` (`enabled`). Packet id 40. No entity ids. Branch `tr-effects`, dev-server
port 25912.

## Changelog section

*   **Weaving**: when you die with it, 2-3 cobwebs appear around where you fell (for mobs, only if
    `mobGriefing` is on). Cobwebs slow you down half as much.
*   **Oozing**: when you die with it, two medium slimes appear (fewer if many slimes are already crowded
    there). Slimes can't get it.
*   **Wind Charged**: when you die with it, you burst like a wind charge, knocking everything nearby away.
*   **Infested**: every hit that hurts you has a 10% chance to release one or two silverfish. Silverfish can't
    get it.
*   **Potions** of Weaving, Oozing, Wind Charging and Infestation (3:00), with splash and lingering (0:45)
    forms and tipped arrows (0:22). Brew an awkward potion with a cobweb, a slime block, a breeze rod or
    stone.
*   Each effect has its own icon and its own particles: cobweb bits, slime bits, small gusts and a gray
    swirl.

## How it works

*   **Effects** (`EffectPotion`, 1.21.11's `MobEffects`): `trickytrialsbackport:wind_charged` (color
    12438015), `weaving` (7891290), `oozing` (10092451), `infested` (9214860), all harmful. Each draws its
    1.21 18x18 icon (`textures/mob_effect/`) in the inventory and on the HUD through Forge's
    `renderInventoryEffect`/`renderHUDEffect`. These are client-only and stripped on a server.
*   **Potions** (1.21.11's `Potions`): one potion type per effect, 3600 ticks, level I, no long or strong
    form. 1.12.2 builds the splash, lingering and arrow items and their scaling from every potion type by
    itself (lingering clouds a quarter, arrows an eighth), the same as 1.21. Names are
    `potion.effect.trickytrialsbackport.<name>` and the splash, lingering and arrow forms, with Mojang's
    text (`tools/lang/vanilla_keys.py` now maps them to `item.minecraft.<kind>.effect.<name>`).
*   **Brewing** (1.21.11's `PotionBrewing.addStartMix`): awkward + breeze rod, slime block, stone (plain stone
    only, not granite, diorite or andesite) or cobweb gives each potion; water + any of them gives mundane.
    The breeze rod is the breeze stream's (`Owners.ours`). With that stream off there is no Wind Charging
    recipe.
*   **On death** (`EffectEvents`): 1.21 runs `onMobRemoved` when a dead entity is removed, on its 20th
    death tick (players too). Here that is the `LivingUpdateEvent` with `deathTime` 19, right before
    vanilla's death update removes the entity in the same tick. Then our effects are cleared, as 1.21 clears
    them.
    *   Weaving (`WeavingMobEffect`): 2-3 webs (`randomBetweenInclusive(2, 3)`) at the first distinct spots,
        among 15 random ones in the 3x3x3 cube around the entity's block, that are replaceable over a solid
        top. Players always do this, mobs only with `mobGriefing`. Each web gets level event 3018's ten puffs and
        1.21's `block.cobweb.place` sound.
    *   Oozing (`OozingMobEffect`): 2 slimes of size 2, half a block up, at random yaw.
        `numberOfSlimesToSpawn` limits them to `maxEntityCramming` minus the slimes (exactly slimes, not
        magma cubes) within 2 blocks.
    *   Wind Charged (`WindChargedMobEffect`): `common/WindBurst` from the middle of the body, radius 3 +
        0-2, the wind charge's knockback 1.22, the large gust and the breeze's burst sound. The dying entity
        is not pushed. It uses no class of the breeze stream, so it works with that stream off.
*   **Infested** (`InfestedMobEffect.onMobHurt`): `LivingHurtEvent` at the lowest priority, when a hit really
    does damage. `nextFloat() <= 0.1` releases 1-2 silverfish from the middle of the body. Each is thrown
    along the look vector x 0.3 (vertical x 1.5), turned up to 90 degrees about Y, with the silverfish hurt
    sound.
*   **Immunity** (1.21's `immune_to_infested`/`immune_to_oozing` tags): `PotionApplicableEvent` denies
    Infested to silverfish and Oozing to slimes (exact classes).
*   **Weaving in cobwebs** (1.21.11's `WebBlock`): a Weaving entity is slowed to (0.5, 0.25, 0.5) instead
    of (0.25, 0.05, 0.25). 1.12.2 scales the move inside `Entity.move`. Instead of patching that, after a
    web-slowed move the remainder is moved again on the next update: once more horizontally, four times
    more vertically, through the same collision. Mobs do this on the server; the player does it on their own
    client, which moves them.
*   **Particles** (1.21's `tickEffects`): each of these effects shows its own particle instead of the colored
    swirl. Wind Charged shows `small_gust` (the breeze stream's small gust frames, 0.15 across). Weaving shows
    `item_cobweb` and Oozing `item_slime` (item crumbs). Infested shows `infested`, a rising spell particle
    with its own texture. The server takes our effects out of the entity's swirl color, so with only our
    effects there is no swirl. It sends watching clients which of ours to draw (`EffectParticlesMessage`,
    id 40, plus ambient and how many other visible effects there are). Each client tick has a 1 in 4 chance
    (1 in 15 when invisible, five times less when everything is ambient) of drawing one particle at a random
    point on the body, picked among all visible effects. Draws that fall on vanilla effects are left to
    1.12.2's swirl.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25912), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=1310 fail=0`** (effects alone: 196), **101 blocks, 1425 states, 95 item models, 302
sound files, 0 errors**. Release build with `--rerun-tasks`: the effects classes carry `func_` names, no
test classes.

*   Registration: ids, lookups (also through `Owners.effect`), harmful, colors, name keys; each potion type
    is one 3600-tick level I effect.
*   Assets: every name in `en_us.lang` with Mojang's English, the four icons, the Infested texture, the
    small gust frames, the cobweb sound.
*   Brewing: each ingredient is a reagent and turns awkward into its potion in all three bottles. Water
    gives mundane and thick does nothing. Redstone and glowstone don't change our potions. Gunpowder makes
    splash and dragon's breath makes lingering. Granite, diorite and cobblestone are not stone.
*   Forms: an Arrow of Weaving gives 450 ticks; a lingering Oozing cloud gives 900.
*   Immunity: silverfish not Infested, slimes not Oozing, magma cubes can be; a zombie takes all four.
*   Weaving: the cube rule gives 2-3 webs within one block on the floor and none over bottom slabs. A dying
    zombie weaves on its 20th death tick and not on the 19th, is removed that tick and loses the effect.
    Without `mobGriefing` it weaves nothing, but a dying player does.
*   Oozing: `numberOfSlimesToSpawn` cases. A dying zombie gives two size-2 slimes (4 health) half a block
    up. With `maxEntityCramming` 3 and two slimes plus a magma cube nearby, it gives one.
*   Wind Charged: a pig 2 blocks off is blown away, the dying zombie isn't, and a plain death does nothing.
*   Infested: a real hit at a 5% roll releases one silverfish from mid-body with the expected turned
    velocity. A 50% roll releases none, and neither does a hit doing no damage. In 2000 seeded hits about
    10% release silverfish, 1-2 each.
*   Webs: a Weaving zombie in a cobweb moves 0.2 of a 0.4 step across (0.1 without) and falls 0.1 of 0.4
    (0.02 without).
*   Particles: the particle state for Weaving + Infested + Speed. The swirl color becomes Speed's alone,
    then none when only ours are left. The state clears when the effects end, and hidden effects stay
    hidden.

## Not tested

*   Anything drawn: icons in the inventory and HUD, the four particles, the cobweb puffs. Only a client
    shows them.
*   The player's own web movement (client side) and how the server takes it. The server's moved-wrongly
    tolerance (0.25 blocks) is far above the extra web step, but no client was run.
*   Splash potions on mobs (vanilla's own code, unchanged).
*   Mods' entities whose death doesn't run vanilla's 20-tick death update (the ender dragon's own death
    animation, for one). Their death effects don't trigger.
*   Languages: Mojang's names map to all 18 files through `vanilla_keys.py`; the family tool is still to run.
