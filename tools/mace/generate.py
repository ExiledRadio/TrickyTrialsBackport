"""Mace stream assets: the heavy core and mace models, sounds, names and Over-Overkill.

    python -I tools/mace/generate.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

# 1.21's heavy core model names its faces' texture "all" without the '#', which 1.12.2 needs.
core = assets.port_model("block/heavy_core")
path = os.path.join(assets.ASSETS, "models", "block", "heavy_core.json")
data = json.load(open(path, encoding="utf-8"))
for element in data["elements"]:
    for face in element["faces"].values():
        if not face["texture"].startswith("#"):
            face["texture"] = "#" + face["texture"]
data.pop("texture_size", None)
assets.write_json("models/block/heavy_core.json", data)
assets.write_json("blockstates/heavy_core.json", {"variants": {"normal": {"model": core.replace(":block/", ":")}}})
assets.item_from_block("heavy_core")

assets.port_model("item/mace")

names = {
    "item.%s.mace.name" % M: assets.name("item.minecraft.mace"),
    "tile.%s.heavy_core.name" % M: assets.name("block.minecraft.heavy_core"),
    "enchantment.%s.density" % M: assets.name("enchantment.minecraft.density"),
    "enchantment.%s.breach" % M: assets.name("enchantment.minecraft.breach"),
    "enchantment.%s.wind_burst" % M: assets.name("enchantment.minecraft.wind_burst"),
    "advancements.adventure.overoverkill.title": assets.name("advancements.adventure.overoverkill.title"),
    "advancements.adventure.overoverkill.description": assets.name("advancements.adventure.overoverkill.description"),
}

assets.write_json("advancements/adventure/overoverkill.json", {
    "parent": "minecraft:adventure/root",
    "display": {"icon": {"item": "%s:mace" % M},
                "title": {"translate": "advancements.adventure.overoverkill.title"},
                "description": {"translate": "advancements.adventure.overoverkill.description"},
                "frame": "challenge"},
    "criteria": {"overoverkill": {"trigger": "%s:mace_smash" % M, "conditions": {"min_damage": 100}}},
    "rewards": {"experience": 50}})

# Wind Burst's sound, entity.wind_charge.wind_burst, ships with the breeze stream's sounds.
events = {}
for e in ("block.heavy_core.break", "block.heavy_core.fall", "block.heavy_core.hit", "block.heavy_core.place",
          "block.heavy_core.step", "item.mace.smash_air", "item.mace.smash_ground", "item.mace.smash_ground_heavy"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("mace", events)
assets.merge_lang("mace", sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events))
print("mace: %d names, %d sound events" % (len(names), len(events)))
