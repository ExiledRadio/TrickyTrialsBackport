# Stream 6: crafter (1.21)

Package `crafter` (screen in `crafter/client`). Config `crafter` (`enabled`). Gui id 6. Branch `tr-crafter`.

## Changelog section

*   **Crafter**: five iron ingots, a crafting table, two redstone dust and a dropper. Put a recipe in its
    3x3 grid and give it a redstone pulse: it crafts once and pushes the result out of its front, into a
    chest or other container there if there is one. Leftovers such as empty buckets come out too.
*   Click an empty slot to switch it off (click again to switch it back on). Switched-off slots take
    nothing, so hoppers feeding the crafter fill only the slots the recipe needs, spreading items evenly.
*   A comparator reads how many slots are filled or switched off. The screen shows what the grid would make
    and whether the crafter is powered.
*   Place it facing any of six ways; its face lights up while powered and while crafting.
*   Advancement **Crafters Crafting Crafters**: be near a crafter when it crafts a crafter.

## How it works

*   **Block** (`BlockCrafter`): 1.21's `orientation` (front and top, twelve values, the metadata), plus
    `triggered` and `crafting` read from the tile entity, so the blockstate file is 1.21.11's own (48
    variants). Placement as 1.21: the front faces the player; looking straight down or up, the top points
    the way they face or back toward them. Hardness 1.5, blast resistance 3.5, pickaxe.
*   **Redstone**: a rising edge marks it triggered and crafts 4 ticks later; while powered nothing more
    happens; a falling edge clears the trigger.
*   **Craft** (`TileEntityCrafter.craft`): the recipe the grid makes (vanilla's and every mod's, through
    `CraftingManager`), one of each ingredient used, the result and every remainder ejected out of the
    front: into an `IItemHandler` facing it, else dispensed as a dropper does. The crafting look lasts 6
    ticks. Craft and fail sounds. Players within a 17-block box get `trickytrialsbackport:crafter_crafted`
    with the result (a new `common/ItemTrigger`).
*   **Hoppers**: through Forge's item capability, insert only, one item at a time, by 1.21's rule: a slot
    takes a stack only if no later enabled slot is empty or holds less of the same item. Switched-off slots
    take nothing.
*   **Screen**: 1.21.11's background and sprites. Switching slots off happens in the container's click
    handling, on both sides (the client predicts, the server decides); the server sends the switched-off
    slots and the power as two window properties. The result preview is worked out on the client, never a
    real slot.
*   **Gui handler**: `common/Guis` is new, one handler for the mod; streams register their container
    (both sides) and screen (client class) by gui id.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25906), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=665 fail=0`** (crafter alone: 73), **19 blocks, 375 states, 19 item models, 172 sound
files, 0 errors**.

*   The recipe; all 12 orientations survive metadata; placement's three cases; hardness and resistance.
*   Redstone: a redstone block beside it triggers it (and shows it); after the delay a log becomes four
    planks out of the east face and one log is used; crafting shows for 6 ticks; a neighbor update while
    still powered crafts nothing; power off clears the trigger; the next pulse crafts again.
*   An empty grid fails.
*   A cake facing up into a chest: one cake and three empty buckets in the chest, every slot used up.
*   NBT round trip of items and switched-off slots; breaking it drops its items.
*   Hoppers (the capability): 16 sticks spread exactly two to each of 8 enabled slots, none in the switched-
    off one; nothing can be taken out; a different item finds no room; the comparator reads 9.
*   Screen container: clicking an empty slot switches it off and on again; clicking with an item places it;
    a switched-off slot takes nothing.
*   *Crafters Crafting Crafters*: not for a stick; earned by a player 3 blocks away when it makes a crafter.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything visual: the screen (background, switched-off marks, redstone dot, preview), the block's six
    facings and its lit faces.
*   The client side of slot switching (predicted locally, then corrected by the window property).
*   A real hopper's transfer cooldown and droppers feeding it (they use the same capability).
*   Sounds playing.
