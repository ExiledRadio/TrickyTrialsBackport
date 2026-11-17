"""Smaller changes stream (25) assets: the 1.21.6 sounds its changes play (taking off a saddle, horse armor or a
carpet with shears, snipping a lead, tying a lead to a mob), the setting names on the settings screen, and the recipe
advancements of the saddle and lead recipes.

    python -I tools/tweaks/generate.py

The settings' tooltips come from their @Config.Comment, written by tools/lang/build_langs.py config.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

SETTINGS = [
    ("ridersThroughPortals", "Riders Through Portals"),
    ("pearlsAcrossDimensions", "Ender Pearls Across Dimensions"),
    ("pearlsLoadChunks", "Ender Pearls Load Chunks"),
    ("tntThroughPortals", "TNT Through Portals"),
    ("portalCooldowns", "Portal Cooldowns"),
    ("saddleRecipe", "Saddle Recipe"),
    ("saddleLoot", "Leather Instead of Saddles in Chests"),
    ("shearsRemoveEquipment", "Shears Remove Saddles and Armor"),
    ("horsesEatCarrots", "Horses Eat Carrots"),
    ("leads", "Leads Between Mobs"),
    ("leadRecipe", "Lead Recipe"),
    ("projectileKnockback", "Projectile Knockback"),
    ("axeShield", "Shield Before Axe"),
    ("witchRedstone", "Witch Redstone"),
    ("noSpawnChunks", "No Spawn Chunks"),
    ("noSpawnInvulnerability", "No Spawn Invulnerability"),
    ("waterBreathingRefillsAir", "Water Breathing Refills Air"),
    ("elytraEndsSprint", "Elytra Ends Sprinting"),
    ("elytraClimbables", "Elytra and Climbable Blocks"),
    ("protectionStacks", "Protection Enchantments Add Up"),
    ("panic", "Panic Rules"),
    ("crystalsFireImmune", "Fire-Immune End Crystals"),
    ("mixedDoubleDoors", "Mixed Double Doors"),
    ("splashDistance", "Splash Potion Distance"),
    ("pumpkinHidesMapMarker", "Pumpkin Hides Map Marker"),
    ("toolFreeDrops", "Drops Without a Tool"),
    ("saddleThorns", "Thorns on Saddles"),
    ("portalsDropBlocks", "End Platform Drops Blocks"),
    ("fireNearPlayers", "Fire Only Near Players"),
    ("headNames", "Heads Keep Their Names"),
    ("gameRules", "New Game Rules"),
]

EVENTS = ["item.saddle.unequip", "item.horse_armor.unequip", "item.llama_carpet.unequip", "item.lead.tied",
          "item.shears.snip"]


def recipe_advancement(name, criterion, item):
    assets.write_json("advancements/recipes/tools/%s.json" % name, {
        "parent": "minecraft:recipes/root",
        "criteria": {
            criterion: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"item": item}]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": "%s:%s" % (M, name)}}},
        "requirements": [["has_the_recipe", criterion]],
        "rewards": {"recipes": ["%s:%s" % (M, name)]}})


# 1.21.6: the saddle recipe unlocks with the first leather; the lead's with the first string, as before.
recipe_advancement("saddle", "has_leather", "minecraft:leather")
recipe_advancement("lead", "has_string", "minecraft:string")

events = {}
for e in EVENTS:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("tweaks", events)

lines = ["config.%s.tweaks.%s=%s" % (M, key, text) for key, text in SETTINGS]
assets.merge_lang("tweaks", sorted(lines) + assets.subtitle_lines(events))
print("tweaks: %d settings, %d sound events" % (len(SETTINGS), len(events)))
