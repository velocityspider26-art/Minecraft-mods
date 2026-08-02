#!/usr/bin/env python3
"""Write src/**/*.lua back into the .rbxlx place file.

    python3 tools/pack.py [--place place/Blacksite.rbxlx]

Scripts that already exist in the place are replaced in situ; modules that are
new are inserted into ReplicatedStorage/Modules. Nothing else in the XML is
touched, so the diff stays limited to script sources.
"""
import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blacksite import place

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

# Order matters only for readability in the explorer.
NEW_MODULES = ['SoldierRigData', 'KSVRMesh', 'HumanRig', 'HumanPose',
               'HumanArms', 'WeaponGrips', 'OperatorShell']
NEW_LOCALSCRIPTS = ['FeelPanel']


def collect(srcdir):
    out = {}
    for base, _dirs, files in os.walk(srcdir):
        for f in files:
            if not f.endswith('.lua'):
                continue
            name = f
            for ext in ('.server.lua', '.client.lua', '.lua'):
                if name.endswith(ext):
                    name = name[:-len(ext)]
                    break
            out[name] = open(os.path.join(base, f), encoding='utf-8').read()
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--place', default=os.path.join(ROOT, 'place', 'Blacksite.rbxlx'))
    ap.add_argument('--src', default=os.path.join(ROOT, 'src'))
    args = ap.parse_args()

    sources = collect(args.src)
    existing = {p.rsplit('/', 1)[-1] for _c, p, _s in place.scripts(args.place)}

    fresh = [(n, sources[n]) for n in NEW_MODULES
             if n in sources and n not in existing]
    if fresh:
        added = place.add_modules(args.place, 'Modules', fresh)
        print('added   %s' % ', '.join(added))

    fresh = [(n, sources[n]) for n in NEW_LOCALSCRIPTS
             if n in sources and n not in existing]
    if fresh:
        added = place.add_modules(args.place, 'StarterPlayerScripts', fresh,
                                  cls='LocalScript')
        print('added   %s (LocalScript)' % ', '.join(added))

    replaced = place.repack(args.place, sources)
    print('updated %d script(s)' % len(replaced))

    # Verify the result parses and every source round-trips byte for byte.
    after = {p.rsplit('/', 1)[-1]: s for _c, p, s in place.scripts(args.place)}
    bad = [n for n, text in sources.items()
           if n in after and after[n] != text]
    missing = [n for n in NEW_MODULES + NEW_LOCALSCRIPTS if n not in after]
    if bad:
        print('MISMATCH after repack: %s' % ', '.join(bad))
        return 1
    if missing:
        print('MISSING after repack: %s' % ', '.join(missing))
        return 1
    print('verified %d script(s) round-trip exactly' % len(sources))
    return 0


if __name__ == '__main__':
    sys.exit(main())
