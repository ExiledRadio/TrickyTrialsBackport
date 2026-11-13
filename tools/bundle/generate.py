"""Bundles stream assets: the seventeen bundles' models (closed, open back, open front), the tooltip
sprites, sounds, names and recipe advancements.

    python -I tools/bundle/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
          "purple", "blue", "brown", "green", "red", "black"]
NAMES = ["bundle"] + ["%s_bundle" % c for c in COLORS]

for name in NAMES:
    for model in (name, name + "_open_back", name + "_open_front"):
        assets.port_model("item/" + model)

# 1.21's GUI sprites, without their .mcmeta: 1.12.2 has no GUI sprite scaling, the tooltip slices them itself.
for sprite in ("bundle_progressbar_border", "bundle_progressbar_fill", "bundle_progressbar_full",
               "slot_background", "slot_highlight_back", "slot_highlight_front"):
    path = "gui/sprites/container/bundle/" + sprite
    assets.write("textures/%s.png" % path, assets.jar_bytes("assets/minecraft/textures/%s.png" % path))

names = {"item.%s.%s.name" % (M, n): assets.name("item.minecraft." + n) for n in NAMES}
for key in ("full", "empty", "empty.description"):
    names["item.%s.bundle.%s" % (M, key)] = assets.name("item.minecraft.bundle." + key)


def recipe_advancement(name, criterion, ore):
    assets.write_json("advancements/recipes/tools/%s.json" % name, {
        "parent": "minecraft:recipes/root",
        "criteria": {
            criterion: {"trigger": "minecraft:inventory_changed",
                        "conditions": {"items": [{"type": "forge:ore_dict", "ore": ore}]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": "%s:%s" % (M, name)}}},
        "requirements": [["has_the_recipe", criterion]],
        "rewards": {"recipes": ["%s:%s" % (M, name)]}})


recipe_advancement("bundle", "has_string", "string")
for color in COLORS:
    recipe_advancement("%s_bundle" % color, "has_%s_dye" % color, "dye" + "".join(p.capitalize() for p in color.split("_")))

events = {}
for e in ("item.bundle.insert", "item.bundle.insert_fail", "item.bundle.remove_one", "item.bundle.drop_contents"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("bundle", events)
assets.merge_lang("bundle", sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events))
print("bundle: %d models, %d names, %d sound events" % (len(NAMES) * 3, len(names), len(events)))
