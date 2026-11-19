"""Shelf stream assets: the twelve shelves' multipart blockstates, models, textures, item models, sounds and names,
all from the 1.21.11 jar.

    python -I tools/shelf/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
WOODS = ("acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak", "jungle", "mangrove", "oak", "pale_oak",
         "spruce", "warped")
PARTS = ("", "_unpowered", "_unconnected", "_left", "_center", "_right", "_inventory")


def bs(model):
    """Blockstate model names drop the block/ folder; 1.12.2 adds it back."""
    return model.replace(":block/", ":")


names = {}
for wood in WOODS:
    shelf = wood + "_shelf"
    for part in PARTS:
        assets.port_model("block/%s%s" % (shelf, part))
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % shelf)
    for entry in data["multipart"]:
        model = entry["apply"]["model"].rpartition(":")[2]
        entry["apply"]["model"] = bs("%s:%s" % (M, model))
    assets.write_json("blockstates/%s.json" % shelf, data)
    assets.item_from_block(shelf, shelf + "_inventory")
    names["tile.%s.%s.name" % (M, shelf)] = assets.name("block.minecraft." + shelf)

events = {}
for kind in ("activate", "deactivate", "multi_swap", "place_item", "single_swap", "take_item",
             "break", "step", "place", "hit", "fall"):
    events["block.shelf." + kind] = assets.copy_sound("block.shelf." + kind)
assets.merge_sounds("shelf", events)

lines = sorted("%s=%s" % kv for kv in names.items())
lines += assets.subtitle_lines(events)
assets.merge_lang("shelf", lines)
print("shelf: %d names, %d sound events" % (len(names), len(events)))
