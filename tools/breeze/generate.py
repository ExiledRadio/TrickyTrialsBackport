"""Breeze stream assets: textures, item models, sounds, names, the loot table and two advancements.

    python -I tools/breeze/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

for t in ("entity/breeze/breeze", "entity/breeze/breeze_eyes", "entity/breeze/breeze_wind", "entity/projectiles/wind_charge"):
    assets.texture(t)
for i in range(12):
    assets.texture("particle/gust_%d" % i)
for i in range(7):
    assets.texture("particle/small_gust_%d" % i)

assets.item_generated("breeze_rod", assets.texture("item/breeze_rod"))
# 1.21 draws the breeze rod held like a stick; 1.12.2's handheld parent does the same.
assets.write_json("models/item/breeze_rod.json", {"parent": "item/handheld", "textures": {"layer0": "%s:item/breeze_rod" % M}})
assets.item_generated("wind_charge", assets.texture("item/wind_charge"))

names = {
    "entity.%s.breeze.name" % M: assets.name("entity.minecraft.breeze"),
    "entity.%s.wind_charge.name" % M: assets.name("entity.minecraft.wind_charge"),
    "entity.%s.breeze_wind_charge.name" % M: assets.name("entity.minecraft.breeze_wind_charge"),
    "item.%s.breeze_rod.name" % M: assets.name("item.minecraft.breeze_rod"),
    "item.%s.wind_charge.name" % M: assets.name("item.minecraft.wind_charge"),
}
for adv in ("blowback", "who_needs_rockets"):
    for part in ("title", "description"):
        key = "advancements.adventure.%s.%s" % (adv, part)
        names[key] = assets.name(key)

assets.write_json("loot_tables/entities/breeze.json", {"pools": [{
    "name": "breeze_rod",
    "rolls": 1,
    "conditions": [{"condition": "killed_by_player"}],
    "entries": [{"type": "item", "name": "%s:breeze_rod" % M, "weight": 1, "functions": [
        {"function": "set_count", "count": {"min": 1, "max": 2}},
        {"function": "looting_enchant", "count": {"min": 1, "max": 2}}]}]}]})

# Parent: adventure's root until the trials stream adds 1.21's "Minecraft: Trial(s) Edition".
assets.write_json("advancements/adventure/blowback.json", {
    "parent": "minecraft:adventure/root",
    "display": {"icon": {"item": "%s:wind_charge" % M},
                "title": {"translate": "advancements.adventure.blowback.title"},
                "description": {"translate": "advancements.adventure.blowback.description"},
                "frame": "challenge"},
    "criteria": {"blowback": {"trigger": "minecraft:player_killed_entity", "conditions": {
        "entity": {"type": "%s:breeze" % M},
        "killing_blow": {"direct_entity": {"type": "%s:breeze_wind_charge" % M}, "is_projectile": True}}}},
    "rewards": {"experience": 40}})
assets.write_json("advancements/adventure/who_needs_rockets.json", {
    "parent": "minecraft:adventure/root",
    "display": {"icon": {"item": "%s:wind_charge" % M},
                "title": {"translate": "advancements.adventure.who_needs_rockets.title"},
                "description": {"translate": "advancements.adventure.who_needs_rockets.description"}},
    "criteria": {"who_needs_rockets": {"trigger": "%s:fall_after_wind_charge" % M, "conditions": {"min_height": 7}}}})

events = {}
for e in ("entity.breeze.charge", "entity.breeze.death", "entity.breeze.deflect", "entity.breeze.hurt",
          "entity.breeze.idle_air", "entity.breeze.idle_ground", "entity.breeze.inhale", "entity.breeze.jump",
          "entity.breeze.land", "entity.breeze.shoot", "entity.breeze.slide", "entity.breeze.whirl",
          "entity.breeze.wind_burst", "entity.wind_charge.throw", "entity.wind_charge.wind_burst"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("breeze", events)

lines = sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events)
assets.merge_lang("breeze", lines)
print("breeze: %d names, %d sound events" % (len(names), len(events)))
