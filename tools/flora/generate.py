"""Spring plants stream assets: bush, firefly bush, cactus flower, short and tall dry grass, leaf litter and
wildflowers, their sounds, the firefly particle, the dry foliage color map and their names, all from the
1.21.11 jar.

    python -I tools/flora/generate.py

Blockstates are 1.21.11's: the plants have one state each; leaf litter and wildflowers keep vanilla's multipart
files, whose property names (facing, segment_amount, flower_amount) are ours too. Models are ported with their
parents (cross_emissive, flowerbed_1-4, template_leaf_litter_1-4 are not 1.12.2's).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID


def bs_model(model):
    """1.12.2 blockstates add the block/ folder: 'trickytrialsbackport:block/x' -> 'trickytrialsbackport:x'."""
    return model.replace(":block/", ":")


def port_variant(v):
    if isinstance(v, list):
        return [port_variant(x) for x in v]
    out = dict(v)
    ns, _, path = out["model"].rpartition(":")
    out["model"] = bs_model(assets.port_model(path))
    return out


def port_blockstate(name):
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % name)
    if "multipart" in data:
        parts = []
        for part in data["multipart"]:
            p = dict(part)
            p["apply"] = port_variant(part["apply"])
            parts.append(p)
        assets.write_json("blockstates/%s.json" % name, {"multipart": parts})
    else:
        assets.write_json("blockstates/%s.json" % name, {"variants": {"normal": port_variant(data["variants"][""])}})


def port_item_model(name):
    """models/item/<name>: 1.21.11's item model if it has one, else the block model its items/ file names."""
    if ("assets/minecraft/models/item/%s.json" % name) in assets.JAR.namelist():
        return assets.port_model("item/" + name)
    ref = assets.jar_json("assets/minecraft/items/%s.json" % name)["model"]["model"]
    ns, _, path = ref.rpartition(":")
    assets.write_json("models/item/%s.json" % name, {"parent": assets.port_model(path)})


lang = []
BLOCKS = ["bush", "firefly_bush", "cactus_flower", "short_dry_grass", "tall_dry_grass", "leaf_litter", "wildflowers"]
for b in BLOCKS:
    port_blockstate(b)
    port_item_model(b)
    lang.append("tile.%s.%s.name=%s" % (M, b, assets.name("block.minecraft." + b)))

assets.texture("particle/firefly")
# 1.21.5's third color map: leaf litter is tinted by it (DryFoliageColor), as leaves are by foliage.png.
assets.texture("colormap/dry_foliage")

events = {}
for prefix in ("block.pink_petals", "block.leaf_litter"):
    for part in ("break", "step", "place", "hit", "fall"):
        events[prefix + "." + part] = assets.copy_sound(prefix + "." + part)
for e in ("block.cactus_flower.break", "block.cactus_flower.place", "block.sweet_berry_bush.break",
          "block.sweet_berry_bush.place", "block.firefly_bush.idle", "block.deadbush.idle", "block.dry_grass.ambient",
          "block.sand.idle"):
    events[e] = assets.copy_sound(e)
# Vanilla's SoundEvents.EMPTY ("intentionally_empty"): the cactus flower's step, hit and fall.
events["intentionally_empty"] = {"sounds": []}
assets.merge_sounds("flora", events)

assets.merge_lang("flora", sorted(set(lang)) + assets.subtitle_lines(events))
print("flora: %d lang lines, %d sound events" % (len(set(lang)), len(events)))
