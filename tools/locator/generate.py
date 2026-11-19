"""Locator bar assets: 1.21.6's HUD sprites and the two attributes' names, from the 1.21.11 jar.

    python -I tools/locator/generate.py

* textures/gui/sprites/hud/locator_bar_background.png: the bar (a 12 x 5 nine-slice sprite, drawn by
  locator.client.LocatorBarHud at 182 x 5).
* textures/gui/sprites/hud/locator_bar_arrow_up.png and _down.png: the arrows (two 7 x 5 frames each).
* textures/gui/sprites/hud/locator_bar_dot/default_0..3.png: the default style's dots, nearest first.
* en_us.lang: attribute.name.waypoint_transmit_range and _receive_range (shown on items and effects that change them).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

HUD = "textures/gui/sprites/hud/"
SPRITES = ["locator_bar_background", "locator_bar_arrow_up", "locator_bar_arrow_down"]
SPRITES += ["locator_bar_dot/default_%d" % i for i in range(4)]
for sprite in SPRITES:
    assets.write(HUD + sprite + ".png", assets.jar_bytes("assets/minecraft/" + HUD + sprite + ".png"))

KEYS = ["attribute.name.waypoint_transmit_range", "attribute.name.waypoint_receive_range"]
lines = []
for key in KEYS:
    assert key not in assets.OLD_LANG, key
    lines.append("%s=%s" % (key, assets.name(key)))
assets.merge_lang("locator", lines)
print("locator: %d sprites, %d lang lines" % (len(SPRITES), len(lines)))
