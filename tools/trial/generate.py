"""Trials stream assets: trial spawner and vault blockstates and models, keys, the ominous bottle copy,
effect icons, particle sprites, sounds, names, advancements, and vanilla's own trial data files.

    python -I tools/trial/generate.py

The data files (spawner configs per mob pool, the spawner, vault and equipment loot tables) are copied from
the 1.21.11 jar unchanged into assets/trickytrialsbackport/trials/minecraft/, where trial.TrialData reads
them.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

# --- Blocks: the property names and values are 1.21's, so the blockstate files are 1.21's own.
for block in ("trial_spawner", "vault"):
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % block)
    models = set()
    for variant in data["variants"].values():
        path = variant["model"].split(":", 1)[-1]
        models.add(path)
        variant["model"] = "%s:%s" % (M, path.split("/", 1)[1])
    assets.write_json("blockstates/%s.json" % block, data)
    for path in sorted(models):
        assets.port_model(path)
    assets.item_from_block(block)

# --- Items.
for item in ("trial_key", "ominous_trial_key", "ominous_bottle"):
    assets.item_generated(item, assets.texture("item/" + item))

# --- Effect icons and particle sprites.
for effect in ("trial_omen", "bad_omen"):
    assets.texture("mob_effect/" + effect)
for sprite in ["flame", "soul_fire_flame", "trial_omen", "ominous_spawning", "vault_connection"] + \
        ["trial_spawner_detection_%d" % i for i in range(5)] + ["trial_spawner_detection_ominous_%d" % i for i in range(5)]:
    assets.texture("particle/" + sprite)

# --- Vanilla's data, unchanged.
DATA = ("data/minecraft/trial_spawner/", "data/minecraft/loot_table/spawners/", "data/minecraft/loot_table/equipment/trial_chamber",
        "data/minecraft/loot_table/chests/trial_chambers/reward")
copied = 0
for name in assets.JAR.namelist():
    if name.endswith(".json") and name.startswith(DATA):
        rel = name[len("data/minecraft/"):]
        assets.write("trials/minecraft/" + rel, assets.jar_bytes(name).decode("utf-8").replace("\r\n", "\n"))
        copied += 1

# --- Advancements. Under Lock and Key hangs from Minecraft: Trial(s) Edition (entering trial chambers),
# which stream 10 (chambers) writes.
for name, parent, key, trigger, frame in (
        ("under_lock_and_key", "%s:adventure/minecraft_trials_edition" % M, "trial_key", "vault_used", None),
        ("revaulting", "%s:adventure/under_lock_and_key" % M, "ominous_trial_key", "ominous_vault_used", "goal")):
    display = {"icon": {"item": "%s:%s" % (M, key)},
               "title": {"translate": "advancements.adventure.%s.title" % name},
               "description": {"translate": "advancements.adventure.%s.description" % name}}
    if frame:
        display["frame"] = frame
    assets.write_json("advancements/adventure/%s.json" % name, {
        "parent": parent,
        "display": display,
        "criteria": {name: {"trigger": "%s:%s" % (M, trigger), "conditions": {"item": {"item": "%s:%s" % (M, key)}}}}})

names = {
    "tile.%s.trial_spawner.name" % M: assets.name("block.minecraft.trial_spawner"),
    "tile.%s.vault.name" % M: assets.name("block.minecraft.vault"),
    "item.%s.trial_key.name" % M: assets.name("item.minecraft.trial_key"),
    "item.%s.ominous_trial_key.name" % M: assets.name("item.minecraft.ominous_trial_key"),
    "item.%s.ominous_bottle.name" % M: assets.name("item.minecraft.ominous_bottle"),
    "effect.%s.trial_omen" % M: assets.name("effect.minecraft.trial_omen"),
    "effect.%s.bad_omen" % M: assets.name("effect.minecraft.bad_omen"),
    "entity.%s.ominous_item_spawner.name" % M: assets.name("entity.minecraft.ominous_item_spawner"),
}
for adv in ("under_lock_and_key", "revaulting"):
    for part in ("title", "description"):
        key = "advancements.adventure.%s.%s" % (adv, part)
        names[key] = assets.name(key)

events = {}
for e in ("about_to_spawn_item", "ambient", "ambient_ominous", "break", "close_shutter", "detect_player", "eject_item", "fall", "hit",
          "ominous_activate", "open_shutter", "place", "spawn_item", "spawn_item_begin", "spawn_mob", "step"):
    events["block.trial_spawner." + e] = assets.copy_sound("block.trial_spawner." + e)
for e in ("activate", "ambient", "break", "close_shutter", "deactivate", "eject_item", "fall", "hit", "insert_item", "insert_item_fail",
          "open_shutter", "place", "reject_rewarded_player", "step"):
    events["block.vault." + e] = assets.copy_sound("block.vault." + e)
for e in ("event.mob_effect.trial_omen", "event.mob_effect.bad_omen", "item.ominous_bottle.dispose"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("trial", events)
assets.merge_lang("trial", sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events))
print("trial: %d names, %d sound events, %d data files" % (len(names), len(events), copied))
