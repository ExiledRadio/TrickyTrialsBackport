"""Writes coppergolem/client/CopperGolemAnimations.java from 1.21.11's decompiled CopperGolemAnimation.

The keyframes are copied as they are; only the names change to the stream's own animation classes (a copy of the
creaking stream's port of 1.19's animation code, so no stream links against another's).

    python -I tools/coppergolem/animations.py <decompiled CopperGolemAnimation.java>
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "java", "com", "exiledradio", "trickytrialsbackport", "coppergolem", "client",
                   "CopperGolemAnimations.java")

src = open(sys.argv[1], encoding="utf-8").read()
body = src[src.index("public class CopperGolemAnimation {") + len("public class CopperGolemAnimation {"):]
body = body[:body.rindex("}")]
body = body.replace("Builder.withLength", "AnimationDefinition.withLength")
body = re.sub(r"\bTargets\.", "AnimationChannel.Targets.", body)
body = re.sub(r"\bInterpolations\.", "AnimationChannel.Interpolations.", body)
body = body.replace("public static final AnimationDefinition", "static final AnimationDefinition")
body = body.replace("\t", "    ")
body = "\n".join(l.replace("   ", "    ", 1) if l.startswith("   ") and not l.startswith("    ") else l
                 for l in body.split("\n"))

head = """package com.exiledradio.trickytrialsbackport.coppergolem.client;

import com.exiledradio.trickytrialsbackport.coppergolem.client.anim.AnimationChannel;
import com.exiledradio.trickytrialsbackport.coppergolem.client.anim.AnimationDefinition;
import com.exiledradio.trickytrialsbackport.coppergolem.client.anim.Keyframe;
import com.exiledradio.trickytrialsbackport.coppergolem.client.anim.KeyframeAnimations;

/**
 * 1.21.11's {@code CopperGolemAnimation}, keyframe for keyframe (written by {@code tools/coppergolem/animations.py}):
 * walk and walk with an item (0.8333 s, looping), the idle head spin (3.5 s) and the four chest interactions (3 s,
 * looping).
 */
final class CopperGolemAnimations {

    private CopperGolemAnimations() {
    }
"""
with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write(head + body.rstrip() + "\n}\n")
print("wrote", OUT)
