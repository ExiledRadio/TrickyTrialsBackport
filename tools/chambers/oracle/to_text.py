"""Writes tools/chambers/oracle121.txt from oracle.py's <seed>.json files, for ChambersSelfTest.

    python -I tools/chambers/oracle/to_text.py <runs dir> <seed> [<seed> ...]

Lines: "seed <seed>", then per candidate chunk "start <cx> <cz> valid|invalid", then its pieces in saved order,
"p <template> <PosX> <PosY> <PosZ> <rotation>", the template without its "minecraft:trial_chambers/".
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
runs = sys.argv[1]
out = []
for seed in sys.argv[2:]:
    d = json.load(open(os.path.join(runs, '%s.json' % seed)))
    out.append('seed %s' % seed)
    for s in d['starts']:
        cx, cz = s['chunk']
        out.append('start %d %d %s' % (cx, cz, 'valid' if s.get('valid') else 'invalid'))
        for p in s.get('pieces', []):
            out.append('p %s %d %d %d %s' % (p['location'].replace('minecraft:trial_chambers/', '', 1), p['pos'][0], p['pos'][1], p['pos'][2], p['rotation']))
with open(os.path.join(ROOT, 'tools', 'chambers', 'oracle121.txt'), 'w', newline='\n') as f:
    f.write('\n'.join(out) + '\n')
print(len(out), 'lines')
