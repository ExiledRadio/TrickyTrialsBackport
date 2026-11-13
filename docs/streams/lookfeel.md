# Stream 33: look and feel (1.21 to 1.21.11)

Package `lookfeel` (client side in `lookfeel/client`). No config category: the options are Backport Core's
presentation options, on vanilla's screens. Packet ids 165-169 (none used). Dev-server port 25933. Branch
`tr-lookfeel`. Requires Backport Core 1.2.0 (`presentation`).

Backport Core shows the newest installed version's single pieces (panorama, splashes, menu music, loading
screen, options screens); with this mod installed the newest version is 1.21.11. Sources: the 1.21.11 client jar
and asset index (`tools/assets.py`), the 1.21.5, 1.21.8 and 1.21.11 jars for the splash changes, and 1.21.11's
decompiled client for the numbers (`FramerateLimitTracker`, `Gui.renderAirBubbles`, `LevelLoadingScreen`,
`ChatScreen`, `LevelRenderer.renderBlockOutline`, `AtmosphericFogEnvironment`, `Items`).

## Changelog section

*   **Title screen**: the Mounts of Mayhem panorama, the new splashes ("Chicken Jockey!", "Flint and Steel!",
    "One does not simply walk to the Far Lands" and more), and the 1.21.6 menu music: "Below and Above", "Broken
    Clocks", "Fireflies", "Lilypad" and "O's Piano". Older updates' menu tracks no longer play there, as in 1.21.
*   **Loading a world**: the panorama behind a dark screen, "Loading terrain...", a progress bar, and the chunks
    being prepared.
*   **Air bubbles**: once you run low, empty bubbles stay in the bar, each bubble bursts as it goes with a pop
    that grows louder and higher, and the empty bar shakes when you are out of air.
*   **Options**: Reduce FPS when (Video Settings: AFK or Minimized), High Contrast Block Outlines (Accessibility
    Settings), Invert Mouse X (Mouse Settings), Sprint Window (Controls) and Save Unsent Chats (Chat Settings).
*   **Chat**: an unsent message comes back the next time chat opens if something else closed the chat (or
    always, with Save Unsent Chats). Chat stays open in a nether portal and opens again after changing
    dimension.
*   **Item rarities** as in 1.21.2: golden apples Common, enchanted golden apples Rare, music discs, chainmail
    and mob heads Uncommon, the dragon head, elytra and command blocks Epic, and the rest.
*   **Sounds**: the spawner's own sounds, four new cave ambience sounds.
*   **Fog**: thicker in rain; the nether's fog no longer depends on the render distance.
*   Vanilla text as 1.21.11 words it: Hostile Mobs, Friendly Mobs, Narrator/Voice, Closed Captions, Invert
    Mouse Y, Walk Forward and others.

## How it works

*   `LookAndFeel.preInit` (both sides) hands Core one `Contribution` per drop:
    *   **1.21**: removes the menu tracks of 1.18, 1.19 and 1.20 by the names the family's backports give them
        (`OLDER_MENU_TRACKS`), since Core applies a version's removals only while that version is installed and
        vanilla 1.21's menu has none of them.
    *   **1.21.2**: Reduce FPS when (after VSync) and High Contrast Block Outlines.
    *   **1.21.6**: the splash changes (as 1.21.8's jar has them) and the five menu tracks
        (`texts/menu_music_1.21.6.txt`). 1.21's nine menu tracks are not shipped: 1.21.6 removed them from the
        menu, so they never play there with this mod installed.
    *   **1.21.9**: Invert Mouse X, Sprint Window (0 to 10, OFF at 0) and Save Unsent Chats (off, 1.21.11's
        default).
    *   **1.21.11**: the panorama and overlay, two splashes, the startup logo, and `vanilla_text/` (1.12.2 keys
        whose English changed between 1.20.6 and 1.21.11, plus Invert Mouse Y, in the 18 languages from
        Mojang's 1.21.11 files through `tools/lang/vanilla_text.py`).
*   `LookAndFeel.init`: the monster spawner's sound type becomes 1.21.2's (`block.spawner.*`), by its SRG field.
*   Cave ambience: `assets/minecraft/sounds.json` adds cave20-23 to 1.12.2's own `ambient.cave` (`replace`
    false).
*   `Rules` holds every number and decision the client uses, so the self-test checks the same code.
*   Client (`lookfeel.client`, all events, no patch):
    *   `FramerateLimiter`: 1.21.2's `FramerateLimitTracker` (10 when minimized; with AFK, 30 after a minute
        without input and 10 after ten), by waiting out each frame at the end of the render tick.
    *   `HighContrastOutline`: in place of 1.12.2's selection box while the option is on, a 7-wide black outline
        and a 2-wide `0x57FFE1` one.
    *   `AirBubbles`: in place of Forge's air bar (same place, `right_height`), 1.21.2's full, bursting and empty
        sprites, the wobble and the pop sound; the bar's post event still fires.
    *   `ItemRarityTooltips` + `Rarities`: the tooltip's name line takes the 1.21.11 rarity's color, with 1.21's
        enchanted rule (Common and Uncommon to Rare, Rare to Epic). Only vanilla items; their own `getRarity`
        is untouched.
    *   `InvertMouseX`: the game's `MouseHelper` is wrapped once (whatever it is), negating the x delta.
    *   `SprintWindow`: the player's `sprintToggleTimer` is set to the option's value on the tick a first tap sets
        it to 7.
    *   `ChatDrafts`: 1.21.9's `ChatScreen`/`ChatComponent` drafts (interrupted, or Escape with the option on;
        gray until edited; Backspace clears an untouched draft; "/" restores only a command); the portal's
        screen-closing is cancelled for the chat; a chat a dimension change interrupts opens again once
        `GuiDownloadTerrain` closes.
    *   `LoadingScreen1219`: Core's `SPAWN_VIEW_1_21_9` drawer: the panorama, 1.20.5's menu tile, the text,
        the 200 by 2 bar (eased a fifth per tick) and the 33 by 33 grid in 1.21.9's colors, centered on the
        spawn area 1.12.2 prepares.
    *   `Fog`: 1.21.6's rain fog (start up to 160 nearer, end up to 256 nearer but not below 96, by rain and
        sky light, eased) and 1.21.11's nether fog (10 to 96, within the render distance), on Core's fog
        pipeline.
    *   F3: 1.21.2's entity line without ", B: n" (Core's `DebugText`).

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25933), then `python -I tools/check_assets.py`:
**`SELFTEST DONE pass=1277 fail=0`**, **101 blocks, 1425 states, 95 item models, 320 sound files, 0 errors**.
`python -I tools/lang/vanilla_text.py check`: 18 languages, 27 keys each, 0 errors. `build_langs.py check`: no
missing key of this stream (the 2628 errors left are other streams', the same count as on `main`).

*   Every drop's contribution is ours; the newest version is 1.21.11 and the loading revision 1.21.9's.
*   The six faces are square PNGs of one size; the overlay is the newest; the startup logo, menu tile and the
    three 9 by 9 bubble sprites are in the jar.
*   Core's splash pool with our files: nine added, "Minors welcome!" and "Pumpa kungen!" gone, others kept.
*   Core's menu pool with a 1.18 track installed before us: exactly menu1-4 and the five 1.21.6 tracks; every
    `.ogg` is in the jar.
*   The five options: defaults, screens, placement after VSync, registered and shown; Sprint Window's OFF.
*   The spawner's sound type and its five events, `ui.hud.bubble_pop`, the cave sounds in `ambient.cave`.
*   Vanilla text: Hostile Mobs, Closed Captions, Invert Mouse Y, Walk Forward; every language has every key.
*   `Rules`: the frame-rate limits (minimized, a minute, ten minutes, a limit under 30, Minimized ignoring idling),
    the bubble states at 300, 0, 28 and 31 air, pop volume and pitch, rain fog numbers, nether fog, chat draft
    keeping and restoring, the loading text's place.
*   `Rarities`: golden apples, disc, three heads, elytra, nether star, enchanted book, chainmail, beacon, command
    block, stone, enchanted sword, elytra and chainmail, and another mod's item left alone.
*   The server booting at all proves no client type is reached from server code.

## Not tested (client only, nothing headless can show)

*   Everything drawn: the panorama on the title screen, the loading screen, the bubbles, the outline, tooltip
    colors, the options on their screens.
*   The frame-rate limiter's real effect (AFK and minimized), Invert Mouse X, Sprint Window in play, the chat
    drafts, the portal and dimension change, the fog in rain and in the nether, the cave and spawner sounds,
    the menu music playing.

## Not in this stream

*   **Core 1.2.0** (`CORE_REQUESTS.md` #9, branch `tr-core-requests`) now gives: menu tracks at vanilla's 0.4 volume
    and older tracks removed by path; Music Frequency, Music Toast (this mod supplies the sprites and its track names),
    music while paused, the UI volume and the 1.21.9 volume preview; Hold/Toggle for attack and use; per-value
    tooltips (Reduce FPS when); the blurred panorama on the loading screen; 60 fps menus; the 1.21.2 slot highlight
    (sprites from this mod); slider snapping; the sunrise dimmed by rain and thunder and clouds by world time (this
    mod's 1.21.11 step). **Still open in Core:** 1.21.11's Video Settings (presets, sections, Cloud Distance, Chunk
    Fade, texture filtering), 1.21.9's Accessibility/Controls moves and Allow Cursor Changes, the F3 rework and Debug
    key category, slider arrow keys, the 1.20 logo layout, the toast's note colors.
*   **Other streams**: in-game music of 1.21 and 1.21.6 (biome music, with Core's game music request, Mu-2 in
    Caves & Cliffs), the Bad Omen icon (effects), spawn invulnerability, sprint-ending actions, piston break
    sounds and desert ambience (tweaks; dry grass is flora's), crossbow and firework tooltips, the locator bar.
*   **Out of scope on 1.12.2**: the disconnection report screen, server links and dialogs (server-driven), the
    credits text, crash report units, languages (the family's 18), Rotate with Minecarts (only with the
    experimental minecarts), the recipe book's Space/Enter, throttled creative throwing, the narrator's volume,
    the fullscreen reset after a crash, Allow Cursor Changes and shared key binds (LWJGL 2), resource pack search,
    the statistics screen's tabs, the End's flashes, and the rendering optimizations.
*   The loading screen's grid is the spawn area 1.12.2 prepares (around the world spawn), not the player's own
    chunks: 1.12.2 loads those after the screen closes.
