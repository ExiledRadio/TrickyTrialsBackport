"""Copper stream assets: every copper block's blockstate and models from 1.21.11, items, sounds and names.

    python -I tools/copper/generate.py

1.21's blockstate files are copied as they are wherever 1.12.2's properties carry the same names (full
blocks, stairs, doors, trapdoors, bulbs); slabs become the family's two-block form. Waxed blocks use the
unwaxed models, as 1.21's own waxed blockstates do.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
STAGES = ["", "exposed_", "weathered_", "oxidized_"]
ported = set()


def model(name):
    """Ports block/<name> once; @return our blockstate model name."""
    if name not in ported:
        assets.port_model("block/" + name)
        ported.add(name)
    return "%s:%s" % (M, name)


def copy_blockstate(block):
    """1.21.11's blockstate for a block, its models ported and renamed; @return the models it uses."""
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % block)
    if "" in data.get("variants", {}):
        # 1.21 names a block without properties ""; 1.12.2 names it "normal".
        data["variants"]["normal"] = data["variants"].pop("")
    used = []
    for variant in data["variants"].values():
        for v in (variant if isinstance(variant, list) else [variant]):
            name = v["model"].split("/", 1)[1]
            v["model"] = model(name)
            used.append(name)
    assets.write_json("blockstates/%s.json" % block, data)
    return used


def slab(block):
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % block)["variants"]
    bottom = data["type=bottom"]["model"].split("/", 1)[1]
    top = data["type=top"]["model"].split("/", 1)[1]
    full = data["type=double"]["model"].split("/", 1)[1]
    assets.write_json("blockstates/%s.json" % block, {"variants": {
        "half=bottom": {"model": model(bottom)}, "half=top": {"model": model(top)}}})
    double = block[:-len("_slab")] + "_double_slab"
    assets.write_json("blockstates/%s.json" % double, {"variants": {"normal": {"model": model(full)}}})
    assets.write_json("models/item/%s.json" % block, {"parent": "%s:block/%s" % (M, bottom)})
    return double


names = {}


def named(block, vanilla=None, item=False):
    key = ("item.%s.%s.name" if item else "tile.%s.%s.name") % (M, block)
    names[key] = assets.name("block.minecraft." + (vanilla or block))


def item_model(block, from_model):
    assets.write_json("models/item/%s.json" % block, {"parent": "%s:block/%s" % (M, from_model)})


# Caves & Cliffs' set, for our copies (registered only without it; assets always shipped).
for ore in ("copper_ore", "raw_copper_block"):
    copy_blockstate(ore)
    item_model(ore, ore)
    named(ore)
assets.item_generated("copper_ingot", assets.texture("item/copper_ingot"))
assets.item_generated("raw_copper", assets.texture("item/raw_copper"))
names["item.%s.copper_ingot.name" % M] = assets.name("item.minecraft.copper_ingot")
names["item.%s.raw_copper.name" % M] = assets.name("item.minecraft.raw_copper")
for waxed in ("", "waxed_"):
    for stage in STAGES:
        full = waxed + ("copper_block" if stage == "" else stage + "copper")
        used = copy_blockstate(full)
        item_model(full, used[0])
        named(full)
        cut = waxed + stage + "cut_copper"
        item_model(cut, copy_blockstate(cut)[0])
        named(cut)
        stairs = waxed + stage + "cut_copper_stairs"
        used = copy_blockstate(stairs)
        item_model(stairs, used[0] if not used[0].endswith(("_inner", "_outer")) else used[0].rsplit("_", 1)[0])
        named(stairs)
        sl = waxed + stage + "cut_copper_slab"
        double = slab(sl)
        named(sl)
        named(double, sl)

# 1.21's five shapes.
for waxed in ("", "waxed_"):
    for stage in STAGES:
        for shape in ("chiseled_copper", "copper_grate", "copper_bulb", "copper_trapdoor"):
            block = waxed + stage + shape
            used = copy_blockstate(block)
            if shape == "copper_trapdoor":
                item_model(block, [u for u in used if u.endswith("_bottom")][0])
            else:
                item_model(block, used[0])
            named(block)
        door = waxed + stage + "copper_door"
        copy_blockstate(door)
        assets.item_generated(door, assets.texture("item/%scopper_door" % stage))
        named(door)
        named(door, item=True)

events = {}
for e in ("block.copper.break", "block.copper.step", "block.copper.place", "block.copper.hit", "block.copper.fall",
          "block.copper_bulb.break", "block.copper_bulb.step", "block.copper_bulb.place", "block.copper_bulb.hit",
          "block.copper_bulb.fall", "block.copper_bulb.turn_on", "block.copper_bulb.turn_off",
          "block.copper_grate.break", "block.copper_grate.step", "block.copper_grate.place", "block.copper_grate.hit",
          "block.copper_grate.fall", "block.copper_door.open", "block.copper_door.close", "block.copper_trapdoor.open",
          "block.copper_trapdoor.close", "item.honeycomb.wax_on", "item.axe.wax_off", "item.axe.scrape"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("copper", events)

lines = sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events)
for key, text in (("oxidation", "Oxidation"), ("waxItems", "Wax Items"), ("generate", "Generate Copper Ore"),
                  ("veinsPerChunk", "Veins per Chunk"), ("veinSize", "Vein Size"), ("minY", "Lowest Vein"),
                  ("maxY", "Highest Vein")):
    lines.append("config.%s.copper.%s=%s" % (M, key, text))
assets.merge_lang("copper", lines)
print("copper: %d names, %d models, %d sound events" % (len(names), len(ported), len(events)))
