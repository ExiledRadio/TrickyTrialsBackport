"""Happy ghast stream assets, from the 1.21.11 jar: the dried ghast's models, textures and blockstate, the sixteen
harnesses (items and their worn textures), the happy ghast's textures, sounds, names, the loot table, recipe
advancements and Stay Hydrated!.

    python -I tools/ghast/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
          "purple", "blue", "brown", "green", "red", "black"]

# The dried ghast: four hydration stages, each a model on 1.21's shared block/dried_ghast shape.
for stage in range(4):
    assets.port_model("block/dried_ghast_hydration_%d" % stage)
variants = {}
for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    for stage in range(4):
        v = {"model": "%s:dried_ghast_hydration_%d" % (M, stage)}
        if y:
            v["y"] = y
        variants["facing=%s,hydration=%d" % (facing, stage)] = v
assets.write_json("blockstates/dried_ghast.json", {"variants": variants})
assets.item_from_block("dried_ghast", "dried_ghast_hydration_0")

# Harnesses: the item sprites and the worn layer.
for c in COLORS:
    assets.port_model("item/%s_harness" % c)
    assets.texture("entity/equipment/happy_ghast_body/%s_harness" % c)

for t in ("happy_ghast", "happy_ghast_baby"):
    assets.texture("entity/ghast/" + t)

# 1.21.11's entities/happy_ghast has no pools.
assets.write_json("loot_tables/entities/happy_ghast.json", {"pools": []})


def recipe_advancement(folder, name, criterion, item, data=None):
    cond = {"item": item}
    if data is not None:
        cond["data"] = data
    assets.write_json("advancements/recipes/%s/%s.json" % (folder, name), {
        "parent": "minecraft:recipes/root",
        "criteria": {
            criterion: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [cond]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": "%s:%s" % (M, name)}}},
        "requirements": [["has_the_recipe", criterion]],
        "rewards": {"recipes": ["%s:%s" % (M, name)]}})


# 1.12.2 dye metadata by 1.21 color name (ink sac 0 ... bone meal 15).
DYE_META = {c: 15 - i for i, c in enumerate(COLORS)}
recipe_advancement("building_blocks", "dried_ghast", "has_ghast_tear", "minecraft:ghast_tear")
for c in COLORS:
    recipe_advancement("combat", "%s_harness" % c, "has_dried_ghast", "%s:dried_ghast" % M)
    recipe_advancement("combat", "dye_%s_harness" % c, "has_needed_dye", "minecraft:dye", DYE_META[c])

# 1.21.6's husbandry/place_dried_ghast_in_water, on our trigger (1.12.2's placed_block cannot see the water).
assets.write_json("advancements/husbandry/place_dried_ghast_in_water.json", {
    "parent": "minecraft:husbandry/root",
    "display": {
        "icon": {"item": "%s:dried_ghast" % M},
        "title": {"translate": "advancements.husbandry.place_dried_ghast_in_water.title"},
        "description": {"translate": "advancements.husbandry.place_dried_ghast_in_water.description"},
    },
    "criteria": {
        "place_dried_ghast_in_water": {
            "trigger": "%s:dried_ghast_placed_in_water" % M,
            "conditions": {"item": {"item": "%s:dried_ghast" % M}},
        }
    },
})

events = {}
for e in ("entity.happy_ghast.ambient", "entity.happy_ghast.death", "entity.happy_ghast.hurt", "entity.happy_ghast.riding",
          "entity.happy_ghast.equip", "entity.happy_ghast.unequip", "entity.happy_ghast.harness_goggles_down",
          "entity.happy_ghast.harness_goggles_up", "entity.ghastling.ambient", "entity.ghastling.death",
          "entity.ghastling.hurt", "entity.ghastling.spawn", "block.dried_ghast.ambient", "block.dried_ghast.ambient_water",
          "block.dried_ghast.break", "block.dried_ghast.fall", "block.dried_ghast.place", "block.dried_ghast.place_in_water",
          "block.dried_ghast.step", "block.dried_ghast.transition"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("ghast", events)

# The setting's name; its tooltip comes from @Config.Comment through tools/lang/build_langs.py config.
lines = ["config.%s.happyGhast.riderCameraDistance=Rider Camera Distance" % M,
         "tile.%s.dried_ghast.name=%s" % (M, assets.name("block.minecraft.dried_ghast")),
         "entity.%s.happy_ghast.name=%s" % (M, assets.name("entity.minecraft.happy_ghast"))]
for c in COLORS:
    lines.append("item.%s.%s_harness.name=%s" % (M, c, assets.name("item.minecraft.%s_harness" % c)))
for k in ("title", "description"):
    key = "advancements.husbandry.place_dried_ghast_in_water." + k
    lines.append("%s=%s" % (key, assets.name(key)))
assets.merge_lang("ghast", sorted(lines) + assets.subtitle_lines(events))
print("ghast: %d sound events, %d lines" % (len(events), len(lines)))
