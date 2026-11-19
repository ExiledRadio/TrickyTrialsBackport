"""Copper golem stream assets, from the 1.21.11 jar: the copper golem's textures, the copper chests' and statues'
blockstates, particle models and item models, the copper chest textures, sounds, names and recipe advancements.

    python -I tools/coppergolem/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

STAGES = ["", "exposed_", "weathered_", "oxidized_"]
# 1.21.11's particle textures, already ours (the copper stream copies them).
PARTICLES = ["copper_block", "exposed_copper", "weathered_copper", "oxidized_copper"]
FACINGS = (("south", 0), ("west", 1), ("north", 2), ("east", 3))
POSES = ["standing", "sitting", "running", "star"]

# The golem's textures by stage, and its glowing eyes.
for s in STAGES:
    assets.texture("entity/copper_golem/%scopper_golem" % s)
    assets.texture("entity/copper_golem/%scopper_golem_eyes" % s)
# The chests' textures: single, left and right halves, 1.15+'s layout (drawn by our own model).
for t in ("copper", "copper_exposed", "copper_weathered", "copper_oxidized"):
    for half in ("", "_left", "_right"):
        assets.texture("entity/chest/%s%s" % (t, half))

# 1.12.2's chest item display (builtin/entity, drawn by the item renderer as 1.12.2's chest is).
CHEST_ITEM = {
    "parent": "builtin/entity",
    "display": {
        "gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        "head": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
        "thirdperson_righthand": {"rotation": [75, 315, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 315, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    },
}
# 1.21.11's template_copper_golem_statue display (its item renderer is the same as ours).
STATUE_TEMPLATE = assets.jar_json("assets/minecraft/models/item/template_copper_golem_statue.json")
STATUE_ITEM = {"parent": "builtin/entity", "display": {k: v for k, v in STATUE_TEMPLATE["display"].items() if k != "on_shelf"}}

lines = []
for i, s in enumerate(STAGES):
    particle = "%s:block/%s" % (M, PARTICLES[i])
    for waxed in ("", "waxed_"):
        chest = waxed + s + "copper_chest"
        statue = waxed + s + "copper_golem_statue"
        # Particle-only block models, shared by the waxed and unwaxed blocks as in 1.21.11.
        assets.write_json("models/block/%scopper_chest.json" % s, {"textures": {"particle": particle}})
        assets.write_json("models/block/%scopper_golem_statue.json" % s, {"textures": {"particle": particle}})
        assets.write_json("blockstates/%s.json" % chest, {"variants": {
            "facing=%s" % f: {"model": "%s:%scopper_chest" % (M, s)} for f, _ in FACINGS}})
        assets.write_json("blockstates/%s.json" % statue, {"variants": {
            "copper_golem_pose=%s,facing=%s" % (p, f): {"model": "%s:%scopper_golem_statue" % (M, s)}
            for p in POSES for f, _ in FACINGS}})
        assets.write_json("models/item/%s.json" % chest, CHEST_ITEM)
        assets.write_json("models/item/%s.json" % statue, STATUE_ITEM)
        for p in POSES[1:]:
            assets.write_json("models/item/%s_%s.json" % (statue, p), STATUE_ITEM)
        lines.append("tile.%s.%s.name=%s" % (M, chest, assets.name("block.minecraft." + chest)))
        lines.append("tile.%s.%s.name=%s" % (M, statue, assets.name("block.minecraft." + statue)))
lines.append("entity.%s.copper_golem.name=%s" % (M, assets.name("entity.minecraft.copper_golem")))


def recipe_advancement(folder, name, criterion, item):
    assets.write_json("advancements/recipes/%s/%s.json" % (folder, name), {
        "parent": "minecraft:recipes/root",
        "criteria": {
            criterion: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"item": item}]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": "%s:%s" % (M, name)}}},
        "requirements": [["has_the_recipe", criterion]],
        "rewards": {"recipes": ["%s:%s" % (M, name)]}})


# The copper ingot is Caves & Cliffs' or ours, so the chest's recipe unlocks with a chest in hand.
recipe_advancement("decorations", "copper_chest", "has_chest", "minecraft:chest")
for s in STAGES:
    for shape in ("copper_chest", "copper_golem_statue"):
        plain = s + shape
        recipe_advancement("building_blocks", "waxed_%s_from_honeycomb" % plain, "has_" + plain, "%s:%s" % (M, plain))

events = {}
for e in ("entity.copper_golem.death", "entity.copper_golem.hurt", "entity.copper_golem.item_drop",
          "entity.copper_golem.item_no_drop", "entity.copper_golem.no_item_get", "entity.copper_golem.no_item_no_get",
          "entity.copper_golem.shear", "entity.copper_golem.spawn", "entity.copper_golem.spin", "entity.copper_golem.step",
          "entity.copper_golem_become_statue",
          "entity.copper_golem_weathered.death", "entity.copper_golem_weathered.hurt", "entity.copper_golem_weathered.spin",
          "entity.copper_golem_weathered.step",
          "entity.copper_golem_oxidized.death", "entity.copper_golem_oxidized.hurt", "entity.copper_golem_oxidized.spin",
          "entity.copper_golem_oxidized.step",
          "block.copper_chest.open", "block.copper_chest.close", "block.copper_chest_weathered.open",
          "block.copper_chest_weathered.close", "block.copper_chest_oxidized.open", "block.copper_chest_oxidized.close",
          "block.copper_golem_statue.break", "block.copper_golem_statue.fall", "block.copper_golem_statue.hit",
          "block.copper_golem_statue.place", "block.copper_golem_statue.step"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("coppergolem", events)

# The settings' names; their tooltips come from @Config.Comment through tools/lang/build_langs.py config.
for key, text in (("building", "Building"), ("sorting", "Sorting"), ("oxidation", "Oxidation"), ("statues", "Statues"),
                  ("ironGolemFlowers", "Iron Golem Flowers")):
    lines.append("config.%s.copperGolem.%s=%s" % (M, key, text))
assets.merge_lang("coppergolem", sorted(lines) + assets.subtitle_lines(events))
print("coppergolem: %d sound events, %d lines" % (len(events), len(lines)))
