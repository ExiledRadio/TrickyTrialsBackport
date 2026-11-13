"""Effects stream assets: the four effect icons, the Infested particle, the cobweb sound and every name.

    python -I tools/effects/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

# 1.21's order (MobEffects / Potions): Wind Charged, Weaving, Oozing, Infested.
EFFECTS = ("wind_charged", "weaving", "oozing", "infested")

for effect in EFFECTS:
    assets.texture("mob_effect/" + effect)
assets.texture("particle/infested")
# The small gust frames (Wind Charged's particle) already ship with the breeze stream's assets.

names = {}
for effect in EFFECTS:
    names["effect.%s.%s" % (M, effect)] = assets.name("effect.minecraft." + effect)
    for kind in ("potion", "splash_potion", "lingering_potion", "tipped_arrow"):
        names["%s.effect.%s.%s" % (kind, M, effect)] = assets.name("item.minecraft.%s.effect.%s" % (kind, effect))

# Weaving's cobwebs play 1.21's cobweb placing sound (level event 3018); 1.12.2's cobweb has none of its own.
# Wind Charged bursts with entity.breeze.wind_burst, which ships with the breeze stream's sounds.
events = {"block.cobweb.place": assets.copy_sound("block.cobweb.place")}
assets.merge_sounds("effects", events)
assets.merge_lang("effects", sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events))
print("effects: %d names, %d sound events" % (len(names), len(events)))
