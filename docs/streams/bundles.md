# Stream 11: bundles (1.21.2)

Package `bundle` (client side in `bundle/client`). Config `bundles` (`enabled`). Packet ids 55 and 56 (block
55-59). No entities. Branch `tr-bundles`. Reference: 1.21.11's decompiled `BundleItem`, `BundleContents`,
`BundleMouseActions`, `ScrollWheelHandler`, `ClientBundleTooltip`, `AbstractContainerMenu` and
`CreativeModeInventoryScreen` (Mojang's mappings), and the 1.21.11 jar's models, recipes and loot tables.

## Changelog section

*   **Bundle**: string over leather. Holds a mixed stack of items, up to one stack's worth: 64 of an item
    that stacks to 64, 16 of one that stacks to 16, one of an unstackable item. A bundle inside a bundle
    counts as 4 plus what it holds. Shulker boxes do not go in.
*   **Sixteen dyed bundles**: any bundle and a dye, keeping what is inside.
*   In any inventory: left-click an item with a bundle to pack it, right-click an empty slot to unpack the
    newest stack. Left-click a bundle with an item to pack it, right-click a bundle with an empty hand to
    take a stack out.
*   Scroll over a bundle to choose which stack comes out next. The bundle opens to show it.
*   The tooltip shows what is inside as a grid with a fullness bar, also while you carry an item. Once
    more than 12 stacks are in, it shows how many items are hidden.
*   A bar under the bundle shows how full it is. It turns red when full.
*   Hold use to empty the bundle onto the ground, one stack at a time.
*   A bundle burned in fire or lava, or blown up, drops what it held.
*   Village cartographer, tannery, weaponsmith and house chests can have a bundle.

## How it works

*   **Contents** (`BundleContents`): the bundle's NBT `BundleContents`, a list of stacks, newest first.
    Weights are vanilla's fractions (commons-lang3's `Fraction`, which 1.21 also uses): 1/stack size per
    item, 1/16 + contents for a bundle, 1 for an item with bees in its `BlockEntityTag`. `Mutable` is
    vanilla's: `tryInsert` (merges a matching stack and moves it to the front), `tryTransfer` from a slot,
    `removeOne` (the picked stack, else the newest), `toggleSelectedItem`.
*   **The pick** is NBT `SelectedItem`. Vanilla keeps it out of saves and the network. Here it exists only
    while a player hovers the bundle in a screen: moving off it, a shift-click, a hotbar key, a plain click
    and closing the screen all clear it. The server also clears every pick in a closing container. When the
    server records a pick it updates its copy of what the client was sent, so a pick is never echoed back
    (it would flicker while the wheel turns).
*   **Clicks** (`BundleClicks`): `decide` gives vanilla's outcome for a plain left or right click:
    `overrideStackedOnOther` with the bundle on the cursor first, then `overrideOtherStackedOnMe` with the
    bundle in the slot. 1.12.2's `Container.slotClick` has no hook for these. The client
    (`BundleClient.mouse`, `GuiScreenEvent.MouseInputEvent.Pre`) cancels the press and its release before
    the screen sees them. It plays the click out locally (vanilla predicts clicks the same way) and sends
    `BundleClickMessage`. The server plays it out on the real container on the server thread, in order with
    vanilla's click packets, then sends the slots (`detectAndSendChanges`) and the cursor
    (`updateHeldItem`). This runs outside `processClickWindow`, so `isChangingQuantityOnly` swallows
    nothing. A click the server sees differently resends the whole window.
    The slot rules are vanilla's `safeTake`, `safeInsert` and `allowModification` in 1.12.2 terms
    (`BundleSlots`). A crafting result goes into a bundle whole or not at all, and a bundle in the result
    slot takes nothing.
*   **Creative screen**: as in 1.21.11, bundle clicks work only on the player's own inventory slots (the
    inventory tab and the hotbar), never on the item list. They are played out on the client, and the
    inventory sends its changed slots the way every creative click does.
*   **The wheel**: vanilla's `ScrollWheelHandler` (fractional notches add up, a change of direction resets).
    One notch is 120 in LWJGL 2 and 1 in some replacements; either counts as one notch. Wheel up picks
    the stack before, wrapping. The pick goes to the server in `BundleSelectMessage`. A wheel over a bundle
    with something inside never scrolls the creative list.
*   **Use** (`ItemBundle`): 200 ticks. One stack at once, then one every 2 ticks after the first 10. The
    server throws it (`dropItem(stack, false, true)`) and plays `item.bundle.drop_contents` for everyone.
    Both sides take it out and play `remove_one` (the player hears its own side). `canContinueUsing` keeps
    the use going while the bundle's NBT changes. The use animation is plain: 1.12.2 has no `BUNDLE`
    animation.
*   **Fullness bar**: width `min(1 + 12 x weight, 13)` given to 1.12.2 as the share it leaves off. Colors
    0x7087FF and 0xFF5454 (vanilla's float colors, floored as `ARGB` does).
*   **Tooltip** (`BundleTooltip`): every size, offset and sprite is `ClientBundleTooltip`'s: 24-pixel
    slots, a 4-wide grid filled from the bottom right, the "+N" cell, a 96x13 bar with 94 pixels of fill,
    "Empty" / "Full", and the empty bundle's wrapped description. 1.12.2 tooltips are text only, so
    `ItemTooltipEvent` inserts blank placeholder lines after the name (where vanilla puts the image), 96
    pixels wide and as many as the image is tall (10 pixels a line, rounded up). `RenderTooltipEvent.PostText`
    draws the image over them. The picked stack's name gets its own small tooltip box above, centered.
    The sprites are 1.21's GUI sprites, nine-sliced in code (1.12.2 has no sprite scaling). A bundle's
    tooltip also shows while carrying an item (`showTooltipWithItemInHand`), drawn after the screen.
*   **Open model** (`BundleModels`): 1.21.11's `items/<bundle>.json` shows the open back, the picked item
    and the open front in the GUI only, and the closed bundle anywhere else. At model bake the bundle's
    baked model is wrapped. Its overrides give an open model while a stack is picked. That model draws
    through the item's `TileEntityItemStackRenderer` in the GUI (each layer with its own GUI transform:
    the back 16 behind, the front 16 in front, from 1.21's `template_bundle_open_*` models), and is the
    closed model in any other perspective. Only the 51 exact model names are looked up, never the
    registry's keys.
*   **Dyeing** (`RecipeBundleDye`): shapeless, any of the 17 bundles plus any `dye<Color>` ore item, NBT
    copied. A bundle that is already that color makes nothing (vanilla's transmute refuses an unchanged
    result). Group `bundle_dye`, in the recipe book. Recipe advancements unlock the recipes from string or
    each dye (1.21.11's `recipes/tools/*_bundle`).
*   **Destroyed bundles**: vanilla's `onDestroyed` runs when an item entity dies of damage. 1.12.2 has no
    hook for that, so a world listener (`IWorldEventListener.onEntityRemoved`, server worlds) spills the
    contents of a removed bundle item whose health is 0 or less. A bundle that was picked up, merged,
    despawned or fell out of the world still has health, so it spills nothing. The health field is read by
    reflection (`health` / `field_70291_e`).
*   **Loot**: 1.21.5's pool (one roll: bundle weight 1, nothing weight 2) is added to Village & Pillage's
    `chests/village/` cartographer, desert/plains/savanna/snowy/taiga house, tannery and weaponsmith
    tables. Without it, the pool goes in 1.12.2's `village_blacksmith` (1.14's weaponsmith).
*   **Dispensers**: 1.21.11 has no bundle behavior, so a dispenser drops the bundle whole. That is 1.12.2's
    default; nothing is registered.
*   **Creative tab**: Tools, after the compass, in Backport Core's order (`docs/CREATIVE_TABS.md`).
*   **Assets** (`tools/bundle/generate.py`): 17 x 3 item models and the two templates, the six tooltip
    sprites, the four sounds (`insert`, `insert_fail`, `remove_one`, `drop_contents`) with subtitles, names
    and tooltip text in 18 languages (Mojang's), and 17 recipe advancements.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25911), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=1245 fail=0`** (bundles alone: 134), **101 blocks, 1425 states, 112 item models, 321
sound files, 0 errors**. The release jar (`build --rerun-tasks`) carries `func_` and `field_` names and no
test classes.

*   Registration: 17 bundles, stack size 1, light gray is 1.12.2's silver, keys.
*   Weights: cobblestone 1/64, pearl 1/16, sword 1, empty bundle 1/16, half-full bundle 9/16, bees 1, no
    shulker boxes. A full bundle does not fit in another, a half-full one does, then 7 pearls fill the rest.
*   Insert and remove: partial inserts, newest first, merging moves to the front, the pick comes out and is
    cleared, an emptied bundle loses its tag.
*   Grid: vanilla's shown-stack counts for 0-64 stacks. Wheel selection wraps both ways.
*   Clicks through the server's path (`BundleClickMessage.apply`) and `BundleClicks` on the player's real
    inventory container:
    *   Cursor bundle onto cobblestone takes 48 of 64.
    *   A full bundle still takes the click.
    *   Right-clicking an empty slot unpacks 48.
    *   A bundle in a slot packs the cursor, and a right-click unpacks into the cursor.
    *   A shulker box is refused.
    *   The crafting result goes in only whole and uses the log.
    *   The ordinary cases are decided ordinary.
    *   Another window id is ignored.
*   Picks: the server records them without echoing, the same index toggles off, out of range picks nothing,
    the picked stack comes out first, closing clears.
*   Use: drops at ticks 200, 188 and 186 (three stacks thrown), the use survives NBT changes and not a
    different item.
*   Recipes: string over leather, dyeing keeps the contents, the same color makes nothing, re-dyeing and
    light gray work, two bundles make nothing.
*   Bar: none when empty, 1 / 7 / 13 pixels (and 1.12.2 draws 7), blue and red.
*   Dispenser: the default behavior throws the bundle out whole with its NBT.
*   Destroyed: lava kills a bundle item and both stacks spill. A bundle removed without damage spills
    nothing.
*   Loot: the blacksmith chest has the pool (no Village & Pillage installed). 3000 rolls give about a third
    (900-1100). Dungeons have no bundle.
*   NBT round trip of a nested bundle and its weights.

## Not tested

*   Everything client-side: the mouse hook, the wheel, the hover and close clearing, the tooltip image, the
    open model, the bar's look, the creative screen path, and the client's prediction. None of it can run
    headlessly. The server half of each is tested above.
*   With Village & Pillage installed: its eight village tables getting the pool. The names come from its
    repo's `loot_tables/chests/village/`.
*   Sounds playing.
*   Another mod's GUI that handles clicks itself instead of through `GuiContainer` (it never sees our hook
    and gets plain vanilla clicks).

## For the integrator

*   Packets 55 and 56 are registered in `network/NetworkHandler`. `BundleClient.init()` is one line in the
    main class's client block.
*   `docs/CREATIVE_TABS.md` has a new Tools section from Core's table. A dev client's creative dump should
    confirm it.
*   The lang build still reports every stream's untranslated config keys (as before this stream). The
    bundle names and tooltip text are Mojang's in all 18 files.
