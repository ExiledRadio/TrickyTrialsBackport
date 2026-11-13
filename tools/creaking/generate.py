"""Creaking stream assets: textures, sounds, names, the loot table and Heart Transplanter, from the 1.21.11 jar.

    python -I tools/creaking/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

assets.texture("entity/creaking/creaking")
assets.texture("entity/creaking/creaking_eyes")

# 1.21.11's entities/creaking has no pools.
assets.write_json("loot_tables/entities/creaking.json", {"pools": []})

# 1.21.6's adventure/heart_transplanter: any one of a heart placed awake, a heart placed dormant, or the pale
# oak log that completes one. 1.12.2's placed_block cannot look at neighbors, so one trigger of ours checks all three.
assets.write_json("advancements/adventure/heart_transplanter.json", {
    "parent": "minecraft:adventure/root",
    "display": {
        "icon": {"item": "%s:creaking_heart" % M},
        "title": {"translate": "advancements.adventure.heart_transplanter.title"},
        "description": {"translate": "advancements.adventure.heart_transplanter.description"},
    },
    "criteria": {
        "heart_transplanter": {
            "trigger": "%s:heart_transplanted" % M,
            "conditions": {"item": {"item": "%s:creaking_heart" % M}},
        }
    },
})

events = {}
for e in ("entity.creaking.activate", "entity.creaking.ambient", "entity.creaking.attack", "entity.creaking.deactivate",
          "entity.creaking.death", "entity.creaking.freeze", "entity.creaking.spawn", "entity.creaking.step",
          "entity.creaking.sway", "entity.creaking.twitch", "entity.creaking.unfreeze",
          "block.creaking_heart.hurt", "block.creaking_heart.spawn"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("creaking", events)

lines = ["entity.%s.creaking.name=%s" % (M, assets.name("entity.minecraft.creaking"))]
for k in ("title", "description"):
    key = "advancements.adventure.heart_transplanter." + k
    lines.append("%s=%s" % (key, assets.name(key)))
assets.merge_lang("creaking", sorted(lines) + assets.subtitle_lines(events))
print("creaking: %d sound events" % len(events))
