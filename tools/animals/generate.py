"""Farm animal variants stream assets: the cow, pig and chicken textures, the blue and brown eggs, the six new wolf
voices, names and the cake and pumpkin pie recipe advancements, from the 1.21.11 jar.

    python -I tools/animals/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

# 1.21.5's textures: cows and pigs 64x64, chickens 64x32, at the same paths under our namespace.
for mob in ("cow", "pig", "chicken"):
    for variant in ("temperate", "warm", "cold"):
        assets.texture("entity/%s/%s_%s" % (mob, variant, mob))

EGGS = ("blue_egg", "brown_egg")
for egg in EGGS:
    assets.item_generated(egg, assets.texture("item/" + egg))

lines = ["item.%s.%s.name=%s" % (M, egg, assets.name("item.minecraft." + egg)) for egg in EGGS]
lines.append("entity.%s.egg.name=%s" % (M, assets.name("entity.minecraft.egg")))

# The settings' names; their tooltips come from @Config.Comment through tools/lang/build_langs.py config.
SETTINGS = [("cowVariants", "Cow Variants"), ("pigVariants", "Pig Variants"), ("chickenVariants", "Chicken Variants"),
            ("coloredEggs", "Blue and Brown Eggs"), ("sheepColors", "Sheep Colors by Biome"),
            ("wolfSoundVariants", "Wolf Sound Variants")]
lines += ["config.%s.animals.%s=%s" % (M, key, text) for key, text in SETTINGS]


# 1.21.11's cake and pumpkin pie take #eggs; ours are the same recipes with a blue or brown egg, unlocked by holding one.
def recipe_advancement(name):
    criteria = {"has_" + egg: {"trigger": "minecraft:inventory_changed",
                               "conditions": {"items": [{"item": "%s:%s" % (M, egg)}]}} for egg in EGGS}
    criteria["has_the_recipe"] = {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": "%s:%s" % (M, name)}}
    assets.write_json("advancements/recipes/food/%s.json" % name, {
        "parent": "minecraft:recipes/root",
        "criteria": criteria,
        "requirements": [["has_the_recipe"] + ["has_" + egg for egg in EGGS]],
        "rewards": {"recipes": ["%s:%s" % (M, name)]}})


recipe_advancement("cake")
recipe_advancement("pumpkin_pie")

# 1.21.5's wolf_sound_variant: classic is 1.12.2's own wolf; the other six voices are ours.
events = {}
for voice in ("puglin", "sad", "angry", "grumpy", "big", "cute"):
    for kind in ("ambient", "death", "growl", "hurt", "pant", "whine"):
        e = "entity.wolf_%s.%s" % (voice, kind)
        events[e] = assets.copy_sound(e)
assets.merge_sounds("animals", events)
assets.merge_lang("animals", sorted(lines) + assets.subtitle_lines(events))
print("animals: 9 entity textures, %d eggs, %d sound events" % (len(EGGS), len(events)))
