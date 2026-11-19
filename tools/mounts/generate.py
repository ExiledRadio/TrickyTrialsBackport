"""Mounts stream assets: the parched (texture, sounds, loot), netherite horse armor (item and worn texture), names and
setting names, from the 1.21.11 jar.

The worn netherite horse armor cannot come from the jar: 1.14 remade the horse model, and its 64x64 textures do not fit
1.12.2's 128x128 layout. It is 1.12.2's diamond horse armor recolored pixel by pixel with 1.21.11's own diamond-to-
netherite colors (both horse_body textures share one layout), as the copper gear stream made copper's from iron.

    python -I tools/mounts/generate.py
"""
import io
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
from PIL import Image  # noqa: E402

M = assets.MODID
names = {}

# --- parched ------------------------------------------------------------------------------------------------------
assets.texture("entity/skeleton/parched")
names["entity.%s.parched.name" % M] = assets.name("entity.minecraft.parched")


def looted(name, lo, hi, extra=None, limit=None):
    looting = {"function": "looting_enchant", "count": {"min": 0, "max": 1}}
    if limit is not None:
        looting["limit"] = limit
    functions = [{"function": "set_count", "count": {"min": lo, "max": hi}}, looting] + (extra or [])
    return {"type": "item", "name": name, "weight": 1, "functions": functions}


# 1.21.11's entities/parched: 0-2 arrows and 0-2 bones (Looting +0-1 each), and 0-1 Arrow of Weakness (Looting +0-1,
# at most 1) when a player kills it.
assets.write_json("loot_tables/entities/parched.json", {"pools": [
    {"name": "arrow", "rolls": 1, "entries": [looted("minecraft:arrow", 0, 2)]},
    {"name": "bone", "rolls": 1, "entries": [looted("minecraft:bone", 0, 2)]},
    {"name": "tipped_arrow", "rolls": 1, "conditions": [{"condition": "killed_by_player"}], "entries": [
        looted("minecraft:tipped_arrow", 0, 1, [{"function": "set_nbt", "tag": "{Potion:\"minecraft:weakness\"}"}], limit=1)]},
]})

# --- netherite horse armor -------------------------------------------------------------------------------------------
assets.port_model("item/netherite_horse_armor")
names["item.%s.netherite_horse_armor.name" % M] = assets.name("item.minecraft.netherite_horse_armor")


def rgba(data):
    return Image.open(io.BytesIO(data)).convert("RGBA")


diamond = rgba(assets.jar_bytes("assets/minecraft/textures/entity/equipment/horse_body/diamond.png"))
netherite = rgba(assets.jar_bytes("assets/minecraft/textures/entity/equipment/horse_body/netherite.png"))
votes = {}
for xy in ((x, y) for x in range(diamond.width) for y in range(diamond.height)):
    a, b = diamond.getpixel(xy), netherite.getpixel(xy)
    if a[3] and b[3]:
        votes.setdefault(a[:3], {}).setdefault(b[:3], 0)
        votes[a[:3]][b[:3]] += 1
shade = {k: max(v, key=v.get) for k, v in votes.items()}
old = rgba(assets.OLD_JAR.read("assets/minecraft/textures/entity/horse/armor/horse_armor_diamond.png"))
out = old.copy()
for xy in ((x, y) for x in range(old.width) for y in range(old.height)):
    p = old.getpixel(xy)
    if not p[3]:
        continue
    near = min(shade, key=lambda c: sum((c[i] - p[i]) ** 2 for i in range(3)))
    out.putpixel(xy, shade[near] + (p[3],))
buf = io.BytesIO()
out.save(buf, "PNG")
assets.write("textures/entity/horse/armor/horse_armor_netherite.png", buf.getvalue())

# --- sounds -------------------------------------------------------------------------------------------------------
events = {}
for e in ("entity.parched.ambient", "entity.parched.death", "entity.parched.hurt", "entity.parched.step"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("mounts", events)

# --- setting names ------------------------------------------------------------------------------------------------
settings = {
    "zombieHorses": "Zombie Horse Changes",
    "zombieHorseSpawn": "Zombie Horses in the Wild",
    "parchedSpawn": "Spawn Parched in Deserts",
    "netheriteHorseArmor": "Netherite Horse Armor",
    "floatWhileRidden": "Mounts Float While Ridden",
    "mountedReach": "Mounted Mob Reach",
}
for key, text in settings.items():
    names["config.%s.mounts.%s" % (M, key)] = text

lines = sorted("%s=%s" % kv for kv in names.items())
lines += assets.subtitle_lines(events)
assets.merge_lang("mounts", lines)
print("mounts: %d names, %d sound events, %d armor shades" % (len(names), len(events), len(shade)))
