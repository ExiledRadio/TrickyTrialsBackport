# Stream 9: paintings, banner patterns, music discs, sherds and templates (1.21-1.21.7)

Package `decor`. Config `decor` (`enabled`). Entity id 25 (ids 25-29 reserved). No packets (45-49 reserved).
Branch `tr-decor`, dev-server port 25913.

## Changelog section

*   **21 new paintings**: Baroque, Humble, Meditative, Prairie Ride, Unpacked, Backyard, Bouquet, Cavebird,
    Changing, Cotan, Endboss, Fern, Finding, Lowmist, Orb, Owlemons, Passage, Pond, Sunflowers, Tides and Dennis.
    A painting placed on a wall can now be any of them: the largest paintings that fit, old or new, one at random.
*   **Banner patterns** Flow and Guster (from trial chambers): applied at the loom, or in the crafting grid
    with a banner and dye, where the pattern is not used up.
*   **Field Masoned** (paper + bricks) and **Bordure Indented** (paper + vines) banner patterns: the bricks and
    curly border designs now need them.
*   **Music discs**: Creator, Creator (Music Box), Precipice (from trial chambers), **Tears** (a ghast killed by a
    fireball you sent back) and **Lava Chicken** (a baby zombie riding a chicken).
*   **Pottery sherds** Flow, Guster and Scrape, and the **Flow** and **Bolt** armor trim smithing templates, which
    duplicate with 7 diamonds and a breeze rod or a copper block. Sherds go on decorated pots and templates trim
    armor when the decorated pot and armor trims are installed.

## How it works

*   **Paintings** (`Paintings`, `EntityModernPainting`, `DecorEvents.placePainting`, `client/RenderModernPainting`):
    1.12.2's painting atlas is fixed, so the new paintings are our own hanging entity
    (`trickytrialsbackport:painting`), vanilla's in every other respect: tracked to 160 blocks, breaks into a
    painting item (none for creative players), vanilla's place and break sounds, picked as a painting. The variant
    is saved as `variant` (its name) and sent with the spawn packet with the facing and position. Sizes are 1.21.11's
    `painting_variant` files (the generator prints them). 1.12.2's painting item already chooses as 1.21 does
    (every fitting painting, keep the largest, one at random); when the item places a vanilla painting, a
    right-click on a wall having marked that spot this tick, the choice is made again over 1.12.2's 26 and our 21
    and, if ours wins, the vanilla join is cancelled and ours spawned. Summoned or loaded paintings are untouched.
    The renderer is 1.12.2's per-block painting draw with 1.21's textures (`painting/<name>.png` front,
    `painting/back.png` back and edges).
*   **Banner patterns** (`Patterns`, `LoomPatterns`, `ItemPattern`): Flow (`flw`) and Guster (`gus`) join 1.12.2's
    `BannerPattern` enum in the block registry event (nothing switches on that enum); textures under
    `minecraft:textures/entity/banner|shield/` where 1.12.2 draws every pattern; color names `item.banner.<p>.<color>`.
    *   With Village & Pillage, the four items are made from its `ItemBannerPattern` class by name, so its loom's
        pattern slot takes them; then `arrangeLoom` (post-init) moves Flow, Guster, bricks and curly border from its
        free list to the patterns that need an item, by reordering its `BannerPatterns.ORDER` in place and lowering
        `FREE_COUNT` (static final, set through `sun.misc.Unsafe` before any loom code has read it). Each step is
        checked; on any mismatch nothing changes. `CORE_REQUESTS.md` Decor-3 asks V&P for a proper call.
    *   Without it, our `ItemPattern` items become the grid recipe's pattern items (`BannerPattern.patternItem`) and
        return themselves as container items, so they stay in the grid; bricks and curly border then need Field
        Masoned and Bordure Indented instead of a brick block and vines.
*   **Discs** (`ItemDecorDisc`): 1.12.2 `ItemRecord`s, record names `item.record.<name>.desc`, 1.21.11's rarities, and
    1.21's comparator signal kept as `comparatorSignal()` for the family jukebox patch (`CORE_REQUESTS.md` Decor-1).
    Sounds `music_disc.<name>` (streamed) from the 1.21.11 asset index. Drops (`DecorEvents.drops`, from 1.21.11's
    loot tables): Tears when a ghast's killing blow is a large fireball (projectile) and a player hurt it last
    (`attackingPlayer`, 1.21's `killed_by_player`); Lava Chicken from a `minecraft:zombie` that is a baby riding a
    chicken, killed by a player. Creator, Creator (Music Box) and Precipice come from trial chamber loot (streams 7
    and 10 look items up by vanilla name).
*   **Sherds**: plain uncommon items in the `potterySherd` ore name; Trails & Tales' decorated pot takes any item in
    it and draws `<ns>:textures/entity/decorated_pot/<x>_pottery_pattern.png`, which we ship.
*   **Templates** (`ItemTrimTemplate`): uncommon, vanilla's tooltip lines, `armorTrimTemplate` ore name, 1.21.11's
    grayscale trim textures shipped under `textures/trims/entity/humanoid[_leggings]/`. The trim itself is Trails &
    Tales' smithing stream (not built yet); `CORE_REQUESTS.md` Decor-2 states what it should read. Duplication
    recipes: diamonds around the template over a breeze rod (the breeze stream's, by name) or a copper / waxed
    copper block (`Owners.block`, Caves & Cliffs' or our copy).
*   **Assets**: `python -I tools/decor/generate.py` (textures, models, sounds, 60 lang lines; English from 1.21.11).

## Tested (`./gradlew runServer -Pselftest=decor`, `DecorSelfTest`)

*   Registration, names, stack sizes, rarities, ore names; disc signals 12/11/13/10/9 and sound events.
*   Recipes: Field Masoned, Bordure Indented, both template duplications (and not with an iron block).
*   Flow and Guster in the enum once, hashes, texture file names.
*   Without Village & Pillage: each of the four patterns applies in the grid (pattern hash and color on the banner),
    the pattern item stays, and vines alone no longer make the curly border.
*   With Village & Pillage 2.5.0's dev jar in `eclipse/mods` (a separate run, 176/176): our items are its pattern
    items, its loom lists the four as needing an item, stripes stay free, the free count is 4 lower.
    (2.3.0's dev jar does not start with Core 1.1.0: a `FutureMcRemoved$Saved` class clash, not ours.)
*   Paintings through the real item path (`ForgeHooks.onRightClickBlock` then `ItemPainting.onItemUse`): a 3x3 wall
    always gets one of ours, all nine 3x3 paintings appear in 40 tries; NBT and spawn-data round trips; breaking
    drops a painting. `choose` on a 4x4 wall gives ours about 2 in 5 (two of five 4x4 paintings), a 1x1 spot
    Meditative about 1 in 8; a summoned vanilla painting stays vanilla.
*   Tears: dropped for a fireball a player sent back; not for a fireball no player touched, nor an arrow.
    Lava Chicken: baby zombie on a chicken killed by a player only; not adult, on foot, without a player, or a husk.
*   Full suite `-Pselftest=all`: 1299 passed, 0 failed. `python -I tools/check_assets.py`: 0 errors.
    `./gradlew build --rerun-tasks`: the jar's decor classes carry `func_` names and no test classes.

## Not tested

*   Anything drawn: the painting renderer, banner and shield patterns, item icons (headless server).
*   Trails & Tales together: its pot is on an unmerged branch and its smithing is not built.
*   The jukebox comparator value (1.12.2 still counts it from the item id until Decor-1 lands).
*   Painting variants in the creative menu (1.21 lists each painting as its own item; not done).
