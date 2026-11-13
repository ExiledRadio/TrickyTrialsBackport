"""Resin stream assets: the resin clump (four blocks), block of resin, resin bricks and their shapes, chiseled resin
bricks, the resin brick item, their sounds and names, all from the 1.21.11 jar.

    python -I tools/resin/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID


def bs(model):
    """Blockstate model names drop the block/ folder; 1.12.2 adds it back."""
    return model.replace(":block/", ":")


def full(name):
    model = assets.port_model("block/" + name)
    assets.write_json("blockstates/%s.json" % name, {"variants": {"normal": {"model": bs(model)}}})
    assets.item_from_block(name)


def stairs(name):
    for suffix in ("", "_inner", "_outer"):
        assets.port_model("block/%s%s" % (name, suffix))
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % name)
    for variant in data["variants"].values():
        variant["model"] = "%s:%s" % (M, variant["model"].split("/", 1)[1])
    assets.write_json("blockstates/%s.json" % name, data)
    assets.item_from_block(name)


def slab(name, double_model):
    bottom = assets.port_model("block/" + name)
    top = assets.port_model("block/%s_top" % name)
    assets.write_json("blockstates/%s.json" % name, {"variants": {
        "half=bottom": {"model": bs(bottom)}, "half=top": {"model": bs(top)}}})
    double = name.replace("_slab", "_double_slab")
    assets.write_json("blockstates/%s.json" % double, {"variants": {"normal": {"model": "%s:%s" % (M, double_model)}}})
    assets.item_from_block(name)


def wall(name):
    """Backport Core's ModernWall has 1.16+'s states (up, and none/low/tall per side): 1.21.11's own multipart."""
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % name)
    for part in data["multipart"]:
        model = part["apply"]["model"].rpartition(":")[2]
        part["apply"]["model"] = bs(assets.port_model(model))
    # A wall with no sides always stands up as a post (ModernWall.getActualState); the OR also covers the stored
    # state, which 1.12.2 never draws but lists among the valid ones.
    lone = {"north": "none", "east": "none", "south": "none", "west": "none"}
    for part in data["multipart"]:
        if part.get("when") == {"up": "true"}:
            part["when"] = {"OR": [{"up": "true"}, lone]}
    assets.port_model("block/%s_inventory" % name)
    assets.write_json("blockstates/%s.json" % name, data)
    assets.item_from_block(name, name + "_inventory")


names = {}


def named(key, vanilla):
    names[key] = assets.name(vanilla)


# --- resin clump: four blocks (none, down, up, up and down), each with its four side faces as properties -------
clump = bs(assets.port_model("block/resin_clump"))
vanilla = assets.jar_json("assets/minecraft/blockstates/resin_clump.json")["multipart"]
SIDES = ("north", "south", "west", "east")
for index, name in enumerate(("resin_clump", "resin_clump_down", "resin_clump_up", "resin_clump_up_down")):
    down, up = bool(index & 1), bool(index & 2)
    parts = []
    for part in vanilla:
        apply = dict(part["apply"], model=clump)
        when = part["when"]
        if len(when) == 1:
            face = next(iter(when))
            if face in SIDES:
                parts.append({"when": when, "apply": apply})
            elif (face == "up" and up) or (face == "down" and down):
                parts.append({"apply": apply})
        elif not up and not down:
            # The state with no face at all (never placed): all six, as vanilla draws it.
            parts.append({"when": {s: "false" for s in SIDES}, "apply": apply})
    assets.write_json("blockstates/%s.json" % name, {"multipart": parts})
    named("tile.%s.%s.name" % (M, name), "block.minecraft.resin_clump")
assets.port_model("item/resin_clump")

# --- block of resin, bricks, shapes, chiseled ------------------------------------------------------------------
for block in ("resin_block", "resin_bricks", "chiseled_resin_bricks"):
    full(block)
    named("tile.%s.%s.name" % (M, block), "block.minecraft." + block)
stairs("resin_brick_stairs")
slab("resin_brick_slab", "resin_bricks")
wall("resin_brick_wall")
for shape in ("resin_brick_stairs", "resin_brick_slab", "resin_brick_wall"):
    named("tile.%s.%s.name" % (M, shape), "block.minecraft." + shape)
named("tile.%s.resin_brick_double_slab.name" % M, "block.minecraft.resin_brick_slab")
assets.port_model("item/resin_brick")
named("item.%s.resin_brick.name" % M, "item.minecraft.resin_brick")

# --- sounds -----------------------------------------------------------------------------------------------------
events = {}
for kind in ("break", "step", "place", "fall"):
    events["block.resin." + kind] = assets.copy_sound("block.resin." + kind)
for kind in ("break", "step", "place", "hit", "fall"):
    events["block.resin_bricks." + kind] = assets.copy_sound("block.resin_bricks." + kind)
assets.merge_sounds("resin", events)

lines = sorted("%s=%s" % kv for kv in names.items())
lines += assets.subtitle_lines(events)
assets.merge_lang("resin", lines)
print("resin: %d names, %d sound events" % (len(names), len(events)))
