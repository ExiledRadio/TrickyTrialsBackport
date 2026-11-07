"""Bogged stream assets: textures, sounds, names and the loot table.

    python -I tools/bogged/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

assets.texture("entity/skeleton/bogged")
assets.texture("entity/skeleton/bogged_overlay")


def looted(name, lo, hi, extra=None, limit=None):
    looting = {"function": "looting_enchant", "count": {"min": 0, "max": 1}}
    if limit is not None:
        looting["limit"] = limit
    functions = [{"function": "set_count", "count": {"min": lo, "max": hi}}, looting] + (extra or [])
    return {"type": "item", "name": name, "weight": 1, "functions": functions}


# 1.21.11's entities/bogged: 0-2 arrows and 0-2 bones (Looting +0-1 each), and 0-1 Arrow of Poison
# (Looting +0-1, at most 1) when a player kills it.
assets.write_json("loot_tables/entities/bogged.json", {"pools": [
    {"name": "arrow", "rolls": 1, "entries": [looted("minecraft:arrow", 0, 2)]},
    {"name": "bone", "rolls": 1, "entries": [looted("minecraft:bone", 0, 2)]},
    {"name": "tipped_arrow", "rolls": 1, "conditions": [{"condition": "killed_by_player"}], "entries": [
        looted("minecraft:tipped_arrow", 0, 1, [{"function": "set_nbt", "tag": "{Potion:\"minecraft:poison\"}"}], limit=1)]},
]})

events = {}
for e in ("entity.bogged.ambient", "entity.bogged.death", "entity.bogged.hurt", "entity.bogged.shear", "entity.bogged.step"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("bogged", events)

lines = ["entity.%s.bogged.name=%s" % (M, assets.name("entity.minecraft.bogged")),
         "config.%s.bogged.spawn=Spawn in Swamps" % M]
assets.merge_lang("bogged", sorted(lines) + assets.subtitle_lines(events))
print("bogged: %d sound events" % len(events))
