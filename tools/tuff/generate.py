"""Tuff stream assets: blockstates, models, textures, sounds and names, from the 1.21.11 jar.

    python -I tools/tuff/generate.py
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
    """1.12.2's stairs states (facing, half, shape) are named as 1.21's: copy its variants, our models."""
    for suffix in ("", "_inner", "_outer"):
        assets.port_model("block/%s%s" % (name, suffix))
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % name)
    for variant in data["variants"].values():
        variant["model"] = "%s:%s" % (M, variant["model"].split("/", 1)[1])
    assets.write_json("blockstates/%s.json" % name, data)
    assets.item_from_block(name)


def slab(name, double_model):
    """The family's two-block slab: half=bottom/top on the slab, the full block's model on the double."""
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


def named(block, vanilla=None):
    names["tile.%s.%s.name" % (M, block)] = assets.name("block.minecraft." + (vanilla or block))


# Tuff itself is ours only without its owner, but its assets are always shipped: the double tuff slab
# draws tuff's model, and the copy needs them.
full("tuff")
named("tuff")
for base, prefix in (("tuff", "tuff"), ("polished_tuff", "polished_tuff"), ("tuff_bricks", "tuff_brick")):
    if base != "tuff":
        full(base)
        named(base)
    stairs(prefix + "_stairs")
    slab(prefix + "_slab", base)
    wall(prefix + "_wall")
    named(prefix + "_stairs")
    named(prefix + "_slab")
    named(prefix + "_double_slab", prefix + "_slab")
    named(prefix + "_wall")
for block in ("chiseled_tuff", "chiseled_tuff_bricks"):
    full(block)
    named(block)

events = {}
for sound in ("tuff", "polished_tuff", "tuff_bricks"):
    for kind in ("break", "step", "place", "hit", "fall"):
        event = "block.%s.%s" % (sound, kind)
        events[event] = assets.copy_sound(event)
assets.merge_sounds("tuff", events)

lines = sorted("%s=%s" % kv for kv in names.items())
lines += assets.subtitle_lines(events)
lines.append("config.%s.tuff.generate=Generate Tuff" % M)
assets.merge_lang("tuff", lines)
print("tuff: %d names, %d sound events" % (len(names), len(events)))
