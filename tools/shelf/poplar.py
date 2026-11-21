"""Game Drops Backport's poplar shelf (its request 2): the same files as mangrove's (tools/shelf/generate.py), with
26.3's texture and name. Poplar is not in the 1.21.11 jar the other shelves come from.

    python -I tools/shelf/poplar.py <26.3 client.jar>
"""
import io
import json
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "trickytrialsbackport")
FILES = ["blockstates/%s.json", "models/block/%s.json", "models/block/%s_center.json", "models/block/%s_inventory.json",
         "models/block/%s_left.json", "models/block/%s_right.json", "models/block/%s_unconnected.json",
         "models/block/%s_unpowered.json", "models/item/%s.json"]


def main(jar_path):
    jar = zipfile.ZipFile(jar_path)
    for f in FILES:
        text = open(os.path.join(ASSETS, f % "mangrove_shelf"), encoding="utf-8").read().replace("mangrove", "poplar")
        with open(os.path.join(ASSETS, f % "poplar_shelf"), "w", encoding="utf-8", newline="\n") as out:
            out.write(text)
    # As RGBA, as the other shelves (keyed transparency draws black in 1.12.2).
    Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/poplar_shelf.png"))).convert("RGBA").save(
        os.path.join(ASSETS, "textures", "block", "poplar_shelf.png"))
    name = json.loads(jar.read("assets/minecraft/lang/en_us.json").decode("utf-8"))["block.minecraft.poplar_shelf"]
    lang = os.path.join(ASSETS, "lang", "en_us.lang")
    lines = open(lang, encoding="utf-8").read().rstrip("\n").split("\n")
    key = "tile.trickytrialsbackport.poplar_shelf.name"
    lines = [l for l in lines if not l.startswith(key + "=")]
    at = max(i for i, l in enumerate(lines) if l.startswith("tile.trickytrialsbackport.") and l.split("=")[0].endswith("_shelf.name")
             and l < key + "=") + 1
    lines.insert(at, key + "=" + name)
    with open(lang, "w", encoding="utf-8", newline="\n") as out:
        out.write("\n".join(lines) + "\n")
    print("poplar shelf:", name)


if __name__ == "__main__":
    main(sys.argv[1])
