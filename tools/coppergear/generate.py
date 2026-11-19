"""Copper gear stream assets: the copper nugget, tools, armor (items and worn textures), horse armor, the copper
torch, and copper bars, chains and lanterns in four stages and waxed, with their sounds and names, from the
1.21.11 jar.

The horse armor's worn texture cannot come from the jar: 1.14 remade the horse model and its 64x64 textures do not fit
1.12.2's 128x128 layout. It is 1.12.2's iron horse armor recolored pixel by pixel with 1.21.11's own iron-to-copper
colors (both horse_body textures share one layout, so each iron shade has its copper shade).

    python -I tools/coppergear/generate.py
"""
import io
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
from PIL import Image  # noqa: E402

M = assets.MODID
STAGES = ("", "exposed_", "weathered_", "oxidized_")
names = {}


def named(key, vanilla):
    names[key] = assets.name(vanilla)


def ours(model):
    """'minecraft:block/x' -> our ported 'trickytrialsbackport:x' as a blockstate names it."""
    path = model.rpartition(":")[2]
    return assets.port_model(path).replace(":block/", ":")


def blockstate(src, dst):
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % src)
    for v in data.get("variants", {}).values():
        v["model"] = ours(v["model"])
    for part in data.get("multipart", []):
        part["apply"]["model"] = ours(part["apply"]["model"])
    assets.write_json("blockstates/%s.json" % dst, data)


def item_like(src, dst):
    """Our item model dst from 1.21.11's items/<src>.json (waxed items use their unwaxed model)."""
    model = assets.jar_json("assets/minecraft/items/%s.json" % src)["model"]["model"].rpartition(":")[2]
    ported = assets.port_model(model)
    if model != "item/" + dst:
        assets.write_json("models/item/%s.json" % dst, {"parent": ported})


# --- torch: 1.12.2 keeps floor and wall torches in one block, by facing ---------------------------------------------
floor = ours("minecraft:block/copper_torch")
wall = assets.jar_json("assets/minecraft/blockstates/copper_wall_torch.json")["variants"]
variants = {"facing=up": {"model": floor}}
for key, v in wall.items():
    variants[key] = dict(v, model=ours(v["model"]))
assets.write_json("blockstates/copper_torch.json", {"variants": variants})
item_like("copper_torch", "copper_torch")
named("tile.%s.copper_torch.name" % M, "block.minecraft.copper_torch")
assets.texture("particle/copper_fire_flame")

# --- bars, chains, lanterns ---------------------------------------------------------------------------------------
for shape in ("copper_bars", "copper_chain", "copper_lantern"):
    for waxed in ("", "waxed_"):
        for stage in STAGES:
            name = waxed + stage + shape
            blockstate(name, name)
            item_like(name, name)
            named("tile.%s.%s.name" % (M, name), "block.minecraft." + name)

# --- items --------------------------------------------------------------------------------------------------------
ITEMS = ["copper_nugget", "copper_sword", "copper_pickaxe", "copper_axe", "copper_shovel", "copper_hoe",
         "copper_helmet", "copper_chestplate", "copper_leggings", "copper_boots", "copper_horse_armor"]
for item in ITEMS:
    # Armor's items/*.json picks a trim model; the plain one is models/item/<name>.json.
    assets.port_model("item/" + item)
    named("item.%s.%s.name" % (M, item), "item.minecraft." + item)

# --- worn armor: 1.21.11's equipment textures in 1.12.2's layer files (the same 64x32 layout) ----------------------
assets.texture("entity/equipment/humanoid/copper", "models/armor/copper_layer_1")
assets.texture("entity/equipment/humanoid_leggings/copper", "models/armor/copper_layer_2")


# --- horse armor: 1.12.2's iron horse armor in 1.21.11's copper colors ----------------------------------------------
def rgba(data):
    return Image.open(io.BytesIO(data)).convert("RGBA")


iron = rgba(assets.jar_bytes("assets/minecraft/textures/entity/equipment/horse_body/iron.png"))
copper = rgba(assets.jar_bytes("assets/minecraft/textures/entity/equipment/horse_body/copper.png"))
votes = {}
for xy in ((x, y) for x in range(iron.width) for y in range(iron.height)):
    a, b = iron.getpixel(xy), copper.getpixel(xy)
    if a[3] and b[3]:
        votes.setdefault(a[:3], {}).setdefault(b[:3], 0)
        votes[a[:3]][b[:3]] += 1
shade = {k: max(v, key=v.get) for k, v in votes.items()}
old = rgba(assets.OLD_JAR.read("assets/minecraft/textures/entity/horse/armor/horse_armor_iron.png"))
out = old.copy()
for xy in ((x, y) for x in range(old.width) for y in range(old.height)):
    p = old.getpixel(xy)
    if not p[3]:
        continue
    near = min(shade, key=lambda c: sum((c[i] - p[i]) ** 2 for i in range(3)))
    out.putpixel(xy, shade[near] + (p[3],))
buf = io.BytesIO()
out.save(buf, "PNG")
assets.write("textures/entity/horse/armor/horse_armor_copper.png", buf.getvalue())

# --- sounds -------------------------------------------------------------------------------------------------------
events = {"item.armor.equip_copper": assets.copy_sound("item.armor.equip_copper")}
for kind in ("break", "step", "place", "hit", "fall"):
    events["block.chain." + kind] = assets.copy_sound("block.chain." + kind)
    events["block.lantern." + kind] = assets.copy_sound("block.lantern." + kind)
assets.merge_sounds("coppergear", events)

lines = sorted("%s=%s" % kv for kv in names.items())
lines += assets.subtitle_lines(events)
assets.merge_lang("coppergear", lines)
print("coppergear: %d names, %d sound events, %d horse armor shades" % (len(names), len(events), len(shade)))
