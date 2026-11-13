"""Look and feel assets: 1.21.x's panorama, splashes, menu music, HUD sprites, sounds and option names.

    python -I tools/lookfeel/generate.py

Sources: the 1.21.11 client jar and asset index (tools/assets.py), and, for the splash changes, the 1.21.5,
1.21.8 and 1.21.11 jars' splashes.txt (G:/MCDownloads/minecraft-<version>/client.jar).

* textures/gui/title/background/panorama_0..5.png and panorama_overlay.png: 1.21.11's (Mounts of Mayhem), from
  the asset index (the jar's are placeholders). Backport Core shows the newest installed version's panorama.
* textures/gui/title/mojangstudios.png: the startup logo as 1.21.11 ships it.
* textures/gui/menu_background.png: the dark tile 1.21's menus draw over the panorama (the 1.21.9 loading screen).
* textures/gui/sprites/hud/air*.png: 1.21.2's air bubbles (full, bursting, empty).
* texts/splashes_<version>_added.txt / _removed.txt: each drop's splash changes, as the jars differ.
* sounds/music/game/*.ogg: 1.21.6's five menu tracks (the menu pool from 1.21.6 to 1.21.11).
* sounds/ambient/cave/cave20-23.ogg and assets/minecraft/sounds.json: 1.21's four new cave ambience sounds,
  added to 1.12.2's own ambient.cave event.
* sounds.json: ui.hud.bubble_pop (1.21.2's air bubble pop) and block.spawner.* (1.21.2's spawner sounds).
* en_us.lang: the new options' names, values and tooltips, from 1.21.11's en_us.json.
"""
import json
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

PANORAMA = "minecraft/textures/gui/title/background/panorama_%s.png"
for face in ["0", "1", "2", "3", "4", "5", "overlay"]:
    assets.write("textures/gui/title/background/panorama_%s.png" % face, assets._object(PANORAMA % face))

assets.write("textures/gui/title/mojangstudios.png", assets.jar_bytes("assets/minecraft/textures/gui/title/mojangstudios.png"))
assets.write("textures/gui/menu_background.png", assets.jar_bytes("assets/minecraft/textures/gui/menu_background.png"))
# Vanilla assets 1.21.x replaced, served at their minecraft: paths through Backport Core's replaceAsset: 1.21.2's
# slot highlight, 1.21.6's ghast (the same layout at twice the size) and its music toast sprites.
VANILLA = ["textures/gui/sprites/container/slot_highlight_back.png", "textures/gui/sprites/container/slot_highlight_front.png",
           "textures/entity/ghast/ghast.png", "textures/entity/ghast/ghast_shooting.png",
           "textures/gui/sprites/toast/now_playing.png", "textures/gui/sprites/icon/music_notes.png"]
for path in VANILLA:
    assets.write("vanilla/" + path, assets.jar_bytes("assets/minecraft/" + path))
for sprite in ("air", "air_bursting", "air_empty"):
    assets.write("textures/gui/sprites/hud/%s.png" % sprite, assets.jar_bytes("assets/minecraft/textures/gui/sprites/hud/%s.png" % sprite))


def splashes(version):
    jar = zipfile.ZipFile("G:/MCDownloads/minecraft-%s/client.jar" % version)
    return [l for l in jar.read("assets/minecraft/texts/splashes.txt").decode("utf-8").splitlines() if l.strip()]


def write_lines(rel, lines):
    assets.write(rel, "".join(l + "\n" for l in lines))


# 1.21 to 1.21.5 kept 1.20.6's list; 1.21.6 (as 1.21.8 ships it) and 1.21.11 changed it. 1.21.9's OpenGL line is
# one Core ignores (it names a graphics version 1.12.2 does not run on).
for before, after, version in (("1.21.5", "1.21.8", "1.21.6"), ("1.21.10", "1.21.11", "1.21.11")):
    old, new = splashes(before), splashes(after)
    write_lines("texts/splashes_%s_added.txt" % version, [s for s in new if s not in old])
    write_lines("texts/splashes_%s_removed.txt" % version, [s for s in old if s not in new])

# 1.21.6's menu tracks: music.menu in 1.21.6 to 1.21.11 is these five plus the four permanent menu tracks.
MENU_TRACKS = ["below_and_above", "broken_clocks", "fireflies", "lilypad", "os_piano"]
menu = [s if isinstance(s, str) else s["name"] for s in assets.vanilla_sound("music.menu")["sounds"]]
for track in MENU_TRACKS:
    assert "music/game/" + track in menu, track
    assets.write("sounds/music/game/%s.ogg" % track, assets._object("minecraft/sounds/music/game/%s.ogg" % track))
write_lines("texts/menu_music_1.21.6.txt", ["%s:music/game/%s" % (assets.MODID, t) for t in MENU_TRACKS])

# 1.21's cave ambience, added to vanilla's ambient.cave (1.12.2 has cave1 to cave13; the ones between are other
# versions').
CAVE = ["cave20", "cave21", "cave22", "cave23"]
cave = [s if isinstance(s, str) else s["name"] for s in assets.vanilla_sound("ambient.cave")["sounds"]]
for c in CAVE:
    assert "ambient/cave/" + c in cave, c
    assets.write("sounds/ambient/cave/%s.ogg" % c, assets._object("minecraft/sounds/ambient/cave/%s.ogg" % c))
minecraft_sounds = os.path.join(assets.ROOT, "src", "main", "resources", "assets", "minecraft", "sounds.json")
os.makedirs(os.path.dirname(minecraft_sounds), exist_ok=True)
with open(minecraft_sounds, "w", encoding="utf-8", newline="\n") as f:
    f.write(json.dumps({"ambient.cave": {"replace": False, "sounds": ["%s:ambient/cave/%s" % (assets.MODID, c) for c in CAVE]}},
                       indent=2) + "\n")

events = {"ui.hud.bubble_pop": assets.copy_sound("ui.hud.bubble_pop")}
for part in ("break", "fall", "hit", "place", "step"):
    events["block.spawner." + part] = assets.copy_sound("block.spawner." + part)
assets.merge_sounds("lookfeel", events)

OPTION_KEYS = [
    "options.inactivityFpsLimit", "options.inactivityFpsLimit.afk", "options.inactivityFpsLimit.minimized",
    "options.inactivityFpsLimit.afk.tooltip", "options.inactivityFpsLimit.minimized.tooltip",
    "options.accessibility.high_contrast_block_outline", "options.accessibility.high_contrast_block_outline.tooltip",
    "options.invertMouseX",
    "options.sprintWindow", "options.sprintWindow.tooltip", "options.value",
    "options.chat.drafts", "options.chat.drafts.tooltip",
]
lines = []
for key in OPTION_KEYS:
    assert key not in assets.OLD_LANG, key
    lines.append("%s=%s" % (key, assets.name(key).replace("\n", "\\n")))
lines += assets.subtitle_lines(events)
assets.merge_lang("lookfeel", sorted(set(lines)))
print("lookfeel: panorama, %d sound events, %d menu tracks, %d cave sounds, %d lang lines"
      % (len(events), len(MENU_TRACKS), len(CAVE), len(lines)))
