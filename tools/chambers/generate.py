"""Trial chambers stream data: vanilla's structure files, template pools, processor list, structure and
structure set, the chambers' container loot tables, the advancement, and names.

    python -I tools/chambers/generate.py

Everything is 1.21.11's own file, unchanged, so no number is written by hand:
  * structure/trial_chambers/**.nbt and worldgen/template_pool/trial_chambers/**.json, read by Backport
    Core's JigsawPools and ModernTemplate (this mod is a JigsawPools source for minecraft: names);
  * worldgen/processor_list/trial_chambers_copper_bulb_degradation.json;
  * worldgen/structure/trial_chambers.json and worldgen/structure_set/trial_chambers.json, which
    chambers.ChamberSettings reads for the start height, depth, distance, padding, pool aliases and the grid;
  * tags/worldgen/biome/has_structure/trial_chambers.json, the biomes the start may be in;
  * the chests', barrels', dispensers' and pots' loot tables, into trials/minecraft/loot_table/ where
    trial.ModernLoot rolls them. 1.12.2 needs a table file to fire its load event, so each also gets an
    empty loot_tables/chambers/<path>.json that chambers.ChamberLoot fills at load.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

COPY = (
    ("data/minecraft/structure/trial_chambers/", "structure/trial_chambers/"),
    ("data/minecraft/worldgen/template_pool/trial_chambers/", "worldgen/template_pool/trial_chambers/"),
    ("data/minecraft/worldgen/processor_list/trial_chambers_copper_bulb_degradation.json",
     "worldgen/processor_list/trial_chambers_copper_bulb_degradation.json"),
    ("data/minecraft/worldgen/structure/trial_chambers.json", "worldgen/structure/trial_chambers.json"),
    ("data/minecraft/worldgen/structure_set/trial_chambers.json", "worldgen/structure_set/trial_chambers.json"),
    ("data/minecraft/tags/worldgen/biome/has_structure/trial_chambers.json",
     "tags/worldgen/biome/has_structure/trial_chambers.json"),
)
LOOT = ("data/minecraft/loot_table/chests/trial_chambers/", "data/minecraft/loot_table/dispensers/trial_chambers/",
        "data/minecraft/loot_table/pots/trial_chambers/")

counts = {"structure": 0, "pool": 0, "loot": 0}
tables = []
for name in sorted(assets.JAR.namelist()):
    if name.endswith("/"):
        continue
    for src, dst in COPY:
        if name == src or (src.endswith("/") and name.startswith(src)):
            rel = dst + name[len(src):] if src.endswith("/") else dst
            data = assets.jar_bytes(name)
            if name.endswith(".json"):
                data = data.decode("utf-8").replace("\r\n", "\n")
            assets.write(rel, data)
            counts["structure" if name.endswith(".nbt") else "pool"] += 1
    if name.endswith(".json") and name.startswith(LOOT):
        rel = name[len("data/minecraft/loot_table/"):-len(".json")]
        assets.write("trials/minecraft/loot_table/%s.json" % rel, assets.jar_bytes(name).decode("utf-8").replace("\r\n", "\n"))
        assets.write_json("loot_tables/chambers/%s.json" % rel, {"pools": []})
        tables.append(rel)
        counts["loot"] += 1
# The reward chests' tables were already copied by the trials stream (vaults roll them); the list of the
# tables this stream's containers name is written for ChamberLoot.
assets.write_json("worldgen/trial_chambers_loot.json", tables)

# --- Advancement: Minecraft: Trial(s) Edition, entering trial chambers. 1.21's trigger is a location
# predicate on the structure; ours is the stream's own trigger, fired once a second while a player is
# inside a chamber's piece.
adv = assets.jar_json("data/minecraft/advancement/adventure/minecraft_trials_edition.json")
display = adv["display"]
assets.write_json("advancements/adventure/minecraft_trials_edition.json", {
    "parent": "minecraft:adventure/root",
    "display": {"icon": {"item": "%s:chiseled_tuff" % M},
                "title": {"translate": display["title"]["translate"]},
                "description": {"translate": display["description"]["translate"]}},
    "criteria": {"minecraft_trials_edition": {"trigger": "%s:entered_trial_chambers" % M}}})

names = {}
for part in ("title", "description"):
    key = "advancements.adventure.minecraft_trials_edition.%s" % part
    names[key] = assets.name(key)
# The trial explorer map's name (chambers/ExplorerMap: the cartographer's journeyman trade).
names["filled_map.trial_chambers"] = assets.name("filled_map.trial_chambers")
names["config.%s.chambers.explorerMap" % M] = "Trial Explorer Map"
assets.merge_lang("chambers", ["%s=%s" % (k, v) for k, v in sorted(names.items())])
print("chambers: %(structure)d structure files, %(pool)d pool and worldgen files, %(loot)d loot tables" % counts)
print("vanilla icon: %s" % display["icon"])
