"""Pale garden stream assets: the pale oak wood set, leaves, sapling, boat, pale moss, hanging moss, eyeblossoms,
the creaking heart, their sounds, particles and names, all from the 1.21.11 jar.

    python -I tools/palegarden/generate.py

Blockstates are 1.21.11's wherever 1.12.2's properties carry the same names (stairs, fences, gates, doors,
pressure plates, the carpet's multipart, hanging moss, the creaking heart); trapdoors and buttons are mapped
from 1.12.2's states; slabs are the family's two-block form. Models and textures are ported with their parents.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
HORIZONTAL = ["north", "south", "west", "east"]
BOOL = ["true", "false"]


def bs_model(model):
    """1.12.2 blockstates add the block/ folder: 'trickytrialsbackport:block/x' -> 'trickytrialsbackport:x'."""
    return model.replace(":block/", ":")


def port_variant(v):
    """A 1.21 variant entry (or a weighted list of them) with its model ported to ours."""
    if isinstance(v, list):
        return [port_variant(x) for x in v]
    out = dict(v)
    ns, _, path = out["model"].rpartition(":")
    out["model"] = bs_model(assets.port_model(path))
    return out


def states(props):
    combos = [{}]
    for key in sorted(props):
        combos = [dict(c, **{key: v}) for c in combos for v in props[key]]
    return combos


def variant_string(state):
    if not state:
        return "normal"
    return ",".join("%s=%s" % (k, state[k]) for k in sorted(state))


def port_blockstate(vanilla, ours, props, convert=None):
    """blockstates/<ours>.json from 1.21.11's <vanilla>: multipart kept, variants mapped per 1.12.2 state."""
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % vanilla)
    if "multipart" in data:
        parts = []
        for part in data["multipart"]:
            p = dict(part)
            p["apply"] = port_variant(part["apply"])
            parts.append(p)
        assets.write_json("blockstates/%s.json" % ours, {"multipart": parts})
        return
    table = []
    for key, v in data["variants"].items():
        want = dict(kv.split("=", 1) for kv in key.split(",")) if key else {}
        table.append((want, v))
    out = {}
    for state in states(props):
        modern = convert(dict(state)) if convert else state
        modern = {k: str(v) for k, v in modern.items()}
        for want, v in table:
            if all(modern.get(k) == val for k, val in want.items()):
                out[variant_string(state)] = port_variant(v)
                break
        else:
            raise KeyError("%s: no 1.21 variant for %s (as %s)" % (vanilla, state, modern))
    assets.write_json("blockstates/%s.json" % ours, {"variants": out})


def port_item_model(name):
    """models/item/<name>: 1.21.11's item model if it has one, else the block model its items/ file names."""
    if ("assets/minecraft/models/item/%s.json" % name) in assets.JAR.namelist():
        return assets.port_model("item/" + name)
    ref = assets.jar_json("assets/minecraft/items/%s.json" % name)["model"]["model"]
    ns, _, path = ref.rpartition(":")
    assets.write_json("models/item/%s.json" % name, {"parent": assets.port_model(path)})


def _button(state):
    f = state.pop("facing")
    if f == "up":
        state.update(face="floor", facing="north")
    elif f == "down":
        state.update(face="ceiling", facing="north")
    else:
        state.update(face="wall", facing=f)
    return state


def _slab_half(state):
    state["type"] = state.pop("half")
    state.pop("variant")
    return state


def _slab_double(state):
    state.pop("variant")
    state["type"] = "double"
    return state


def _trapdoor(state):
    state["powered"] = "false"
    state["waterlogged"] = "false"
    return state


lang = []


def named(ours, vanilla=None, kind="tile"):
    key = ("block.minecraft." if kind == "tile" else "item.minecraft.") + (vanilla or ours)
    if key not in assets.VANILLA_LANG:
        key = "block.minecraft." + (vanilla or ours)
    lang.append("%s.%s.%s.name=%s" % (kind, M, ours, assets.name(key)))


# --- the wood set ---------------------------------------------------------------------------------
W = "pale_oak"
pillar = {"axis": ["x", "y", "z"]}
for s in ("log", "wood"):
    for name in (W + "_" + s, "stripped_%s_%s" % (W, s)):
        port_blockstate(name, name, pillar)
        port_item_model(name)
        named(name)
port_blockstate(W + "_planks", W + "_planks", {})
port_item_model(W + "_planks")
named(W + "_planks")
port_blockstate(W + "_stairs", W + "_stairs", {"facing": HORIZONTAL, "half": ["top", "bottom"],
                                                "shape": ["straight", "inner_left", "inner_right", "outer_left", "outer_right"]})
port_item_model(W + "_stairs")
named(W + "_stairs")
port_blockstate(W + "_slab", W + "_slab", {"half": ["top", "bottom"], "variant": ["default"]}, _slab_half)
port_blockstate(W + "_slab", W + "_double_slab", {"variant": ["default"]}, _slab_double)
port_item_model(W + "_slab")
named(W + "_slab")
named(W + "_double_slab", W + "_slab")
port_blockstate(W + "_fence", W + "_fence", None)
port_item_model(W + "_fence")
named(W + "_fence")
port_blockstate(W + "_fence_gate", W + "_fence_gate", {"facing": HORIZONTAL, "in_wall": BOOL, "open": BOOL, "powered": BOOL})
port_item_model(W + "_fence_gate")
named(W + "_fence_gate")
port_blockstate(W + "_door", W + "_door", {"facing": HORIZONTAL, "half": ["upper", "lower"], "hinge": ["left", "right"],
                                          "open": BOOL, "powered": BOOL})
named(W + "_door")
named(W + "_door", kind="item")
port_item_model(W + "_door")
port_blockstate(W + "_trapdoor", W + "_trapdoor", {"facing": HORIZONTAL, "half": ["top", "bottom"], "open": BOOL}, _trapdoor)
port_item_model(W + "_trapdoor")
named(W + "_trapdoor")
port_blockstate(W + "_pressure_plate", W + "_pressure_plate", {"powered": BOOL})
port_item_model(W + "_pressure_plate")
named(W + "_pressure_plate")
port_blockstate(W + "_button", W + "_button", {"facing": ["down", "up"] + HORIZONTAL, "powered": BOOL}, _button)
port_item_model(W + "_button")
named(W + "_button")
port_blockstate(W + "_sign", W + "_sign", {"rotation": [str(i) for i in range(16)]})
port_blockstate(W + "_wall_sign", W + "_wall_sign", {"facing": HORIZONTAL})
named(W + "_sign")
named(W + "_wall_sign", W + "_sign")
named(W + "_sign", kind="item")
port_item_model(W + "_sign")
assets.texture("entity/signs/" + W)

# --- leaves, sapling, pot, boat ---------------------------------------------------------------------
for name in ("pale_oak_leaves", "pale_oak_sapling"):
    port_blockstate(name, name, {})
    port_item_model(name)
    named(name)
port_blockstate("potted_pale_oak_sapling", "potted_pale_oak_sapling", {})
named("potted_pale_oak_sapling")
for i in range(12):
    assets.texture("particle/pale_oak_%d" % i)
port_item_model("pale_oak_boat")
named("pale_oak_boat", kind="item")
assets.texture("entity/boat/" + W)
lang.append("entity.%s.boat.name=%s" % (M, assets.name("entity.minecraft.boat")))

# --- pale moss, eyeblossoms, creaking heart ---------------------------------------------------------
port_blockstate("pale_moss_block", "pale_moss_block", {})
port_item_model("pale_moss_block")
named("pale_moss_block")
port_blockstate("pale_moss_carpet", "pale_moss_carpet", None)
port_item_model("pale_moss_carpet")
# 1.12.2's block/carpet names no particle (each 1.12 carpet sets its own; 1.21's parent has "particle": "#wool"),
# so without this the carpet's break particles, and its item's, are the missing texture.
assets.write_json("models/block/pale_moss_carpet.json", {"parent": "block/carpet", "textures": {
    "particle": M + ":block/pale_moss_carpet", "wool": M + ":block/pale_moss_carpet"}})
named("pale_moss_carpet")
port_blockstate("pale_hanging_moss", "pale_hanging_moss", {"tip": BOOL})
port_item_model("pale_hanging_moss")
named("pale_hanging_moss")
for name in ("open_eyeblossom", "closed_eyeblossom"):
    port_blockstate(name, name, {})
    port_item_model(name)
    named(name)
    port_blockstate("potted_" + name, "potted_" + name, {})
    named("potted_" + name)
port_blockstate("creaking_heart", "creaking_heart", {"axis": ["x", "y", "z"],
                                                     "creaking_heart_state": ["uprooted", "dormant", "awake"]})
port_item_model("creaking_heart")
named("creaking_heart")

# --- sounds -----------------------------------------------------------------------------------------
events = {}
for prefix in ("block.moss", "block.moss_carpet", "block.creaking_heart"):
    for part in ("break", "step", "place", "hit", "fall"):
        events[prefix + "." + part] = assets.copy_sound(prefix + "." + part)
for e in ("block.creaking_heart.idle", "block.pale_hanging_moss.idle", "block.eyeblossom.idle", "block.eyeblossom.open",
          "block.eyeblossom.open_long", "block.eyeblossom.close", "block.eyeblossom.close_long", "item.axe.strip"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("palegarden", events)

lang.append("config.%s.paleGarden.generateBiome=Generate Pale Gardens" % M)
assets.merge_lang("palegarden", sorted(set(lang)) + assets.subtitle_lines(events))
print("palegarden: %d lang lines, %d sound events" % (len(set(lang)), len(events)))
