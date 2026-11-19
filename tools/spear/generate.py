"""Spear stream assets: the seven spears' icon and in-hand models, their sounds, names, Lunge, the spear death
messages and Mob Kabob, from the 1.21.11 jar.

1.21.11's items/<spear>.json shows models/item/<spear>.json in the GUI, on the ground, in frames and on shelves,
and models/item/<spear>_in_hand.json everywhere else; both are ported, and the client picks by perspective.

    python -I tools/spear/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
SPEARS = ["wooden_spear", "stone_spear", "copper_spear", "iron_spear", "golden_spear", "diamond_spear", "netherite_spear"]

names = {}
for spear in SPEARS:
    model = assets.jar_json("assets/minecraft/items/%s.json" % spear)["model"]
    assert model["cases"][0]["model"]["model"] == "minecraft:item/" + spear, spear
    assert model["fallback"]["model"] == "minecraft:item/%s_in_hand" % spear, spear
    assets.port_model("item/" + spear)
    assets.port_model("item/%s_in_hand" % spear)
    names["item.%s.%s.name" % (M, spear)] = assets.name("item.minecraft." + spear)

names["enchantment.%s.lunge" % M] = assets.name("enchantment.minecraft.lunge")
# Our damage type is 1.21.11's "spear", so 1.12.2 asks for these very keys.
names["death.attack.spear"] = assets.name("death.attack.spear")
names["death.attack.spear.item"] = assets.name("death.attack.spear.item")
names["advancements.adventure.spear_many_mobs.title"] = assets.name("advancements.adventure.spear_many_mobs.title")
names["advancements.adventure.spear_many_mobs.description"] = assets.name("advancements.adventure.spear_many_mobs.description")
names["entity.%s.fireproof_item.name" % M] = assets.name("entity.minecraft.item")

# Config option names (their tooltips come from build_langs.py config).
names["config.%s.spears.mobSpears" % M] = "Mobs Spawn with Spears"
names["config.%s.spears.mobCharges" % M] = "Mobs Charge with Spears"

advancement = assets.jar_json("data/minecraft/advancement/adventure/spear_many_mobs.json")
assets.write_json("advancements/adventure/spear_many_mobs.json", {
    # 1.21.11's parent, adventure/kill_a_mob, is 1.12.2's adventure/kill_a_mob too.
    "parent": "minecraft:adventure/kill_a_mob",
    "display": {"icon": {"item": "%s:iron_spear" % M},
                "title": {"translate": "advancements.adventure.spear_many_mobs.title"},
                "description": {"translate": "advancements.adventure.spear_many_mobs.description"},
                "frame": advancement["display"]["frame"]},
    "criteria": {"spear_many_mobs": {"trigger": "%s:spear_mobs" % M,
                                     "conditions": {"count": advancement["criteria"]["spear_many_mobs"]["conditions"]["count"]}}}})

events = {}
for e in ("item.spear.attack", "item.spear.hit", "item.spear.use", "item.spear.lunge_1", "item.spear.lunge_2",
          "item.spear.lunge_3", "item.spear_wood.attack", "item.spear_wood.hit", "item.spear_wood.use"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("spear", events)
assets.merge_lang("spear", sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events))
print("spear: %d names, %d sound events" % (len(names), len(events)))
