"""Lang lines every stream shares: the creative tab's name and the config categories' names.

    python tools/common/generate.py

The tab is named after mod_name in gradle.properties, the one place the (not yet final) display name
is written.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import assets  # noqa: E402
import streams  # noqa: E402

props = {}
for line in open(os.path.join(assets.ROOT, "gradle.properties"), encoding="utf-8"):
    if "=" in line and not line.startswith("#"):
        k, _, v = line.strip().partition("=")
        props[k] = v

lines = ["itemGroup.%s=%s" % (assets.MODID, props["mod_name"])] + streams.lang_lines()
assets.merge_lang("common", lines)
print("common: %d lines" % len(lines))
