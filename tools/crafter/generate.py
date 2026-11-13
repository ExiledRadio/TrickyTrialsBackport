"""Crafter stream assets: the 48-state blockstate, models, textures, the screen, sounds, names, advancement.

    python -I tools/crafter/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

for model in ("crafter", "crafter_triggered", "crafter_crafting", "crafter_crafting_triggered"):
    assets.port_model("block/" + model)

# 1.12.2 cannot resolve "particle": "#north" against the same model's own north (it logs "upward reference" and the
# break particles are the missing texture), so name the texture itself.
import json, os
with open(os.path.join(assets.ASSETS, "models", "block", "crafter.json"), encoding="utf-8") as f:
    base = json.load(f)
base["textures"]["particle"] = base["textures"]["north"]
assets.write_json("models/block/crafter.json", base)
# The block's properties carry 1.21's names and values, so its blockstate file is 1.21's own.
data = assets.jar_json("assets/minecraft/blockstates/crafter.json")
for variant in data["variants"].values():
    variant["model"] = "%s:%s" % (M, variant["model"].split("/", 1)[1])
assets.write_json("blockstates/crafter.json", data)
assets.item_from_block("crafter")

assets.texture("gui/container/crafter")
for sprite in ("disabled_slot", "powered_redstone", "unpowered_redstone"):
    assets.texture("gui/sprites/container/crafter/" + sprite)

assets.write_json("advancements/adventure/crafters_crafting_crafters.json", {
    "parent": "minecraft:adventure/root",
    "display": {"icon": {"item": "%s:crafter" % M},
                "title": {"translate": "advancements.adventure.crafters_crafting_crafters.title"},
                "description": {"translate": "advancements.adventure.crafters_crafting_crafters.description"}},
    "criteria": {"crafter_crafted_crafter": {"trigger": "%s:crafter_crafted" % M,
                                             "conditions": {"item": {"item": "%s:crafter" % M}}}}})

names = {
    "tile.%s.crafter.name" % M: assets.name("block.minecraft.crafter"),
    "container.%s.crafter" % M: assets.name("container.crafter"),
    "advancements.adventure.crafters_crafting_crafters.title": assets.name("advancements.adventure.crafters_crafting_crafters.title"),
    "advancements.adventure.crafters_crafting_crafters.description":
        assets.name("advancements.adventure.crafters_crafting_crafters.description"),
}
events = {}
for e in ("block.crafter.craft", "block.crafter.fail"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("crafter", events)
assets.merge_lang("crafter", sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events))
print("crafter: %d names, %d sound events" % (len(names), len(events)))
