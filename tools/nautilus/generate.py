"""Nautilus stream assets, from the 1.21.11 jar: the nautilus, baby and zombie nautilus textures, the worn armor and
saddle, the five nautilus armor items, the inventory screen and its slot sprites, Breath of the Nautilus's icon, the
sounds, the loot tables and the names.

    python -I tools/nautilus/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
names = {}

# --- textures -----------------------------------------------------------------------------------------------------
for t in ("nautilus", "nautilus_baby", "zombie_nautilus", "zombie_nautilus_coral"):
    assets.texture("entity/nautilus/" + t)
TIERS = (("copper", "copper"), ("iron", "iron"), ("golden", "gold"), ("diamond", "diamond"), ("netherite", "netherite"))
for item, asset in TIERS:
    assets.texture("entity/equipment/nautilus_body/" + asset)
    assets.port_model("item/%s_nautilus_armor" % item)
    names["item.%s.%s_nautilus_armor.name" % (M, item)] = assets.name("item.minecraft.%s_nautilus_armor" % item)
assets.texture("entity/equipment/nautilus_saddle/saddle")
assets.texture("gui/container/nautilus")
for s in ("container/slot", "container/slot/saddle", "container/slot/nautilus_armor_inventory"):
    assets.texture("gui/sprites/" + s)
assets.texture("mob_effect/breath_of_the_nautilus")

# --- loot ---------------------------------------------------------------------------------------------------------
# entities/nautilus: its one pool (a shell, 5% for a player kill, Looting +1%) is added in code when Update Aquatic's
# shell exists, since a table naming a missing item fails to load in 1.12.2.
assets.write_json("loot_tables/entities/nautilus.json", {"pools": []})
# 1.21.11's entities/zombie_nautilus: 0-3 rotten flesh for a player kill, Looting +0-1 each level.
assets.write_json("loot_tables/entities/zombie_nautilus.json", {"pools": [
    {"name": "rotten_flesh", "rolls": 1, "conditions": [{"condition": "killed_by_player"}], "entries": [
        {"type": "item", "name": "minecraft:rotten_flesh", "weight": 1, "functions": [
            {"function": "set_count", "count": {"min": 0, "max": 3}},
            {"function": "looting_enchant", "count": {"min": 0, "max": 1}}]}]}]})

# --- sounds -------------------------------------------------------------------------------------------------------
EVENTS = []
for mob in ("nautilus", "baby_nautilus", "zombie_nautilus"):
    for e in sorted(assets.VANILLA_SOUNDS):
        if e.startswith("entity.%s." % mob):
            EVENTS.append(e)
EVENTS += ["item.armor.equip_nautilus", "item.armor.unequip_nautilus", "item.nautilus_saddle_equip",
           "item.nautilus_saddle_underwater_equip"]
events = {}
for e in EVENTS:
    spec = assets.copy_sound(e)
    # An event-type entry (the zombie nautilus's dash is the nautilus's) names our event, not a missing vanilla one.
    for s in spec["sounds"]:
        if isinstance(s, dict) and s.get("type") == "event" and ":" not in s["name"]:
            s["name"] = "%s:%s" % (M, s["name"])
    events[e] = spec
assets.merge_sounds("nautilus", events)

# --- names --------------------------------------------------------------------------------------------------------
names["entity.%s.nautilus.name" % M] = assets.name("entity.minecraft.nautilus")
names["entity.%s.zombie_nautilus.name" % M] = assets.name("entity.minecraft.zombie_nautilus")
names["effect.%s.breath_of_the_nautilus" % M] = assets.name("effect.minecraft.breath_of_the_nautilus")
names["death.attack.dryout"] = assets.name("death.attack.dryout")
settings = {
    "naturalSpawn": "Spawn Nautiluses in Oceans",
    "drownedRiders": "Drowned Ride Zombie Nautiluses",
    "chestLoot": "Nautilus Armor in Ocean Chests",
}
for key, text in settings.items():
    names["config.%s.nautilus.%s" % (M, key)] = text

lines = sorted("%s=%s" % kv for kv in names.items())
lines += assets.subtitle_lines(events)
assets.merge_lang("nautilus", lines)
print("nautilus: %d names, %d sound events" % (len(names), len(events)))
