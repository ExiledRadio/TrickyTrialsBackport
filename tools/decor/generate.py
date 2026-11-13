"""Decor stream assets: paintings, banner patterns, music discs, sherds and trim templates.

    python -I tools/decor/generate.py

Everything comes from the 1.21.11 client jar and asset index (tools/assets.py). Banner and shield pattern
textures go under the minecraft domain, where 1.12.2's banner renderer looks for every pattern.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
names = {}

# --- paintings: the picture of each, and the shared back ---
PAINTINGS = ["baroque", "humble", "meditative", "prairie_ride", "unpacked", "backyard", "bouquet", "cavebird",
             "changing", "cotan", "endboss", "fern", "finding", "lowmist", "orb", "owlemons", "passage", "pond",
             "sunflowers", "tides", "dennis"]
SIZES = {}
for p in PAINTINGS:
    variant = assets.jar_json("data/minecraft/painting_variant/%s.json" % p)
    SIZES[p] = (variant["width"], variant["height"])
    assets.texture("painting/" + p)
assets.texture("painting/back")
placeable = [v.split(":")[1] for v in assets.jar_json("data/minecraft/tags/painting_variant/placeable.json")["values"]]
names["entity.%s.painting.name" % M] = assets.name("entity.minecraft.painting")

# --- music discs ---
DISCS = ["creator", "creator_music_box", "precipice", "tears", "lava_chicken"]
events = {}
for d in DISCS:
    song = assets.jar_json("data/minecraft/jukebox_song/%s.json" % d)
    assets.item_generated("music_disc_" + d, assets.texture("item/music_disc_" + d))
    names["item.%s.music_disc_%s.name" % (M, d)] = assets.name("item.minecraft.music_disc_" + d)
    names["item.record.%s.desc" % d] = assets.name(song["description"]["translate"])
    events["music_disc." + d] = assets.copy_sound("music_disc." + d)

# --- pottery sherds, with their decorated pot faces for Trails & Tales' pot ---
for s in ["flow", "guster", "scrape"]:
    assets.item_generated(s + "_pottery_sherd", assets.texture("item/%s_pottery_sherd" % s))
    assets.texture("entity/decorated_pot/%s_pottery_pattern" % s)
    names["item.%s.%s_pottery_sherd.name" % (M, s)] = assets.name("item.minecraft.%s_pottery_sherd" % s)

# --- armor trim templates, with the grayscale trims for Trails & Tales' smithing ---
for t in ["flow", "bolt"]:
    item = "%s_armor_trim_smithing_template" % t
    assets.item_generated(item, assets.texture("item/" + item))
    assets.texture("trims/entity/humanoid/" + t)
    assets.texture("trims/entity/humanoid_leggings/" + t)
    names["item.%s.%s.name" % (M, item)] = assets.name("item.minecraft." + item)
    names["trim_pattern.%s.%s" % (M, t)] = assets.name("trim_pattern.minecraft." + t)
for k in ["applies_to", "ingredients", "armor_trim.applies_to", "armor_trim.ingredients"]:
    names["item.%s.smithing_template.%s" % (M, k)] = assets.name("item.minecraft.smithing_template." + k)

# --- banner patterns ---
COLORS = {"white": "white", "orange": "orange", "magenta": "magenta", "light_blue": "lightBlue", "yellow": "yellow",
          "lime": "lime", "pink": "pink", "gray": "gray", "light_gray": "silver", "cyan": "cyan", "purple": "purple",
          "blue": "blue", "brown": "brown", "green": "green", "red": "red", "black": "black"}
for p in ["flow", "guster"]:
    for kind in ["banner", "shield"]:
        assets.write("../minecraft/textures/entity/%s/%s.png" % (kind, p),
                     assets.jar_bytes("assets/minecraft/textures/entity/%s/%s.png" % (kind, p)))
    for modern, old in COLORS.items():
        names["item.banner.%s.%s" % (p, old)] = assets.name("block.minecraft.banner.%s.%s" % (p, modern))
    names["item.%s.%s_banner_pattern.name" % (M, p)] = assets.name("item.minecraft.%s_banner_pattern" % p)
    names["item.%s.%s_banner_pattern.desc" % (M, p)] = assets.name("item.minecraft.%s_banner_pattern.desc" % p)
for p in ["flow", "guster", "field_masoned", "bordure_indented"]:
    assets.item_generated(p + "_banner_pattern", assets.texture("item/%s_banner_pattern" % p))
for p in ["field_masoned", "bordure_indented"]:
    names["item.%s.%s_banner_pattern.name" % (M, p)] = assets.name("item.minecraft.%s_banner_pattern" % p)

if __name__ == "__main__":
    for p, (w, h) in sorted(SIZES.items()):
        print("  %-13s %dx%d" % (p, w, h))
    assert set(placeable) - set(PAINTINGS) <= set(
        "kebab aztec alban aztec2 bomb plant wasteland pool courbet sea sunset creebet wanderer graham match bust stage "
        "void skull_and_roses wither fighters pointer pigscene burning_skull skeleton donkey_kong".split()), \
        "1.21.11 has a placeable painting this list lacks"
    assets.merge_sounds("decor", events)
    assets.merge_lang("decor", sorted("%s=%s" % kv for kv in names.items()))
    print("decor: %d paintings, %d names, %d sound events" % (len(PAINTINGS), len(names), len(events)))
