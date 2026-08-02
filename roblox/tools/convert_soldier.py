#!/usr/bin/env python3
"""swat_operator.fbx  ->  SoldierRigData.lua  (skeleton + per-bone geometry)

Run:  python3 tools/convert_soldier.py [--tris 10000]

The output module carries two things the runtime needs:

  BONES     the real bind pose of the Mixamo skeleton, expressed as
            parent-relative CFrames in Roblox studs.  This is what replaces
            R15's joint layout -- it has clavicles, a two-segment spine and
            toes, none of which R15 has.

  SEGMENTS  one rigid chunk of the soldier mesh per bone, with its vertices
            already in that bone's local space, so the runtime only has to
            write bone world CFrames.  Triangles near a joint are emitted into
            BOTH neighbouring segments so the seam overlaps instead of cracking
            open when the joint bends.
"""
import argparse
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blacksite.rig import SoldierRig, SEGMENTS, DRIVEN, CM_PER_STUD
from blacksite import meshconv
from blacksite import grips as gripsmod

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

# Base colours for the runtime-built body. Only used when the real textured
# mesh has not been imported; the FBX itself ships flat material colours.
COLORS = {
    'Head': (36, 38, 40), 'Neck': (52, 45, 40),
    'Chest': (30, 33, 34), 'Abdomen': (34, 37, 36), 'Pelvis': (33, 36, 35),
    'LeftClavicle': (30, 33, 34), 'RightClavicle': (30, 33, 34),
    'LeftUpperArm': (38, 42, 39), 'RightUpperArm': (38, 42, 39),
    'LeftForeArm': (38, 42, 39), 'RightForeArm': (38, 42, 39),
    'LeftHand': (26, 28, 29), 'RightHand': (26, 28, 29),
    'LeftThigh': (40, 44, 41), 'RightThigh': (40, 44, 41),
    'LeftShin': (37, 41, 38), 'RightShin': (37, 41, 38),
    'LeftFoot': (24, 25, 26), 'RightFoot': (24, 25, 26),
}
MATERIAL = {
    'Head': 'Metal', 'LeftHand': 'Fabric', 'RightHand': 'Fabric',
    'LeftFoot': 'Fabric', 'RightFoot': 'Fabric',
}

OVERLAP = 0.22      # a triangle joins a segment when its mean weight clears this
DOMINANT = 0.001


def fmt(x, nd=3):
    s = ('%.*f' % (nd, x)).rstrip('0').rstrip('.')
    return '0' if s in ('', '-0') else s


def flat(rows, nd=3, per_line=8):
    """Emit a flat numeric array, wrapped, without trailing float noise."""
    vals = [fmt(v, nd) for v in np.asarray(rows).ravel()]
    out, step = [], per_line * (rows.shape[1] if rows.ndim > 1 else 1)
    for i in range(0, len(vals), step):
        out.append('\t\t' + ','.join(vals[i:i + step]) + ',')
    return '\n'.join(out)


def ints(rows, per_line=12):
    vals = [str(int(v)) for v in np.asarray(rows).ravel()]
    out, step = [], per_line * (rows.shape[1] if rows.ndim > 1 else 1)
    for i in range(0, len(vals), step):
        out.append('\t\t' + ','.join(vals[i:i + step]) + ',')
    return '\n'.join(out)


def cframe_lua(M):
    """4x4 -> Roblox CFrame.new(x,y,z, R00,R01,R02, R10,...) argument list."""
    p = M[:3, 3]
    R = M[:3, :3]
    nums = [p[0], p[1], p[2],
            R[0, 0], R[0, 1], R[0, 2],
            R[1, 0], R[1, 1], R[1, 2],
            R[2, 0], R[2, 1], R[2, 2]]
    return ','.join(fmt(v, 5) for v in nums)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--fbx', default=os.path.join(ROOT, 'assets', 'swat_operator.fbx'))
    ap.add_argument('--out', default=os.path.join(ROOT, 'src', 'ReplicatedStorage',
                                                  'Modules', 'SoldierRigData.lua'))
    ap.add_argument('--tris', type=int, default=10000)
    args = ap.parse_args()

    rig = SoldierRig(args.fbx)
    verts = rig.body_vertices()
    tris = rig.body_triangles()
    segw = rig.segment_weights()
    seg_names = [s[0] for s in SEGMENTS]

    # ---- ground the rig: origin at the floor under the hips ---------------
    floor = float(verts[:, 1].min())
    hips = rig.pos('Hips')
    shift = np.array([hips[0], floor, hips[2]])
    verts = verts - shift

    def bindM(name):
        M = rig.bind(name).copy()
        M[:3, 3] -= shift
        return M

    height = float(verts[:, 1].max() - verts[:, 1].min())

    # ---- triangle -> segment assignment ----------------------------------
    keep_mask, dominant, _ = rig.segment_triangles(tris, min_keep=0.15, overlap=OVERLAP)

    # Triangle budget per segment. Allocating by source triangle count alone
    # hands most of the budget to the head, which is by far the densest part of
    # the scan, and leaves the torso and limbs faceted. Weight by surface area
    # instead -- that is what actually decides whether a chunk reads as smooth
    # -- then cap any single segment so nothing can starve the rest.
    a, b, c = verts[tris[:, 0]], verts[tris[:, 1]], verts[tris[:, 2]]
    area = 0.5 * np.linalg.norm(np.cross(b - a, c - a), axis=1)
    counts = np.array([(dominant == i).sum() for i in range(len(seg_names))], float)
    areas = np.array([area[dominant == i].sum() for i in range(len(seg_names))])
    share = 0.72 * (areas / areas.sum()) + 0.28 * (counts / counts.sum())

    cap = 0.19
    for _ in range(8):
        over = share > cap
        if not over.any():
            break
        excess = (share[over] - cap).sum()
        share[over] = cap
        room = ~over
        share[room] += excess * share[room] / share[room].sum()

    budget = {}
    for i, n in enumerate(seg_names):
        budget[n] = max(150, min(int(counts[i]), int(args.tris * share[i])))

    out_segments = []
    total_v = total_t = 0
    for i, name in enumerate(seg_names):
        bone = SEGMENTS[i][1]
        st = tris[keep_mask[:, i]]
        if len(st) == 0:
            continue
        used = np.unique(st)
        remap = np.full(len(verts), -1, np.int64)
        remap[used] = np.arange(len(used))
        sv, sti = verts[used], remap[st]
        sv, sti = meshconv.weld(sv, sti)
        sv, sti = meshconv.decimate_to(sv, sti, budget[name])
        if len(sti) == 0:
            continue

        # into bone-local space, then centre the chunk on its own bbox so the
        # runtime MeshPart pivot and this offset agree exactly
        B = bindM(bone)
        inv = np.linalg.inv(B)
        h = np.hstack([sv, np.ones((len(sv), 1))])
        lv = (h @ inv.T)[:, :3]
        centre = (lv.min(0) + lv.max(0)) * 0.5
        lv = lv - centre
        size = lv.max(0) - lv.min(0)

        out_segments.append(dict(name=name, bone=bone, v=lv, t=sti + 1,
                                 centre=centre, size=size))
        total_v += len(lv)
        total_t += len(sti)

    # ---- emit -------------------------------------------------------------
    L = []
    L.append('--!nonstrict')
    L.append('-- SoldierRigData -- GENERATED by roblox/tools/convert_soldier.py.')
    L.append('-- Do not hand-edit; re-run the tool instead.')
    L.append('--')
    L.append('-- Source model: "S.W.A.T. Operator - Remastered" by SpatialNeglect')
    L.append('-- (Sketchfab, CC-BY). See roblox/ATTRIBUTION.md.')
    L.append('--')
    L.append('-- Units are Roblox studs at 1 stud = 0.35 m, matching GameConfig.')
    L.append('-- The rig origin is the floor directly under the hips, so a bone\'s')
    L.append('-- bind CFrame is also its height off the ground.')
    L.append('')
    L.append('local M = {}')
    L.append('')
    L.append('M.HEIGHT = %s' % fmt(height, 4))
    L.append('M.SOURCE = "SWAT Operator (Mixamo rig, %d bones)"' % len(rig.bones))
    L.append('')
    L.append('-- Bones the animation system drives, parents before children.')
    L.append('M.ORDER = {')
    for b in DRIVEN:
        L.append('\t"%s",' % b)
    L.append('}')
    L.append('')
    L.append('-- [bone] = { parent, bind (world CFrame), offset (parent-relative) }')
    L.append('M.BONES = {')
    for b in DRIVEN:
        info = rig.bones[b]
        Bm = bindM(b)
        par = info['parent']
        if par in DRIVEN:
            off = np.linalg.inv(bindM(par)) @ Bm
            ps = '"%s"' % par
        else:
            off = Bm
            ps = 'nil'
        L.append('\t%s = {parent=%s, bind={%s}, offset={%s}},'
                 % (b, ps, cframe_lua(Bm), cframe_lua(off)))
    L.append('}')
    L.append('')
    L.append('-- Palm frames, in each wrist bone\'s local space. X runs along the')
    L.append('-- knuckle line toward the index finger, Z points out of the palm.')
    L.append('-- A hand meets a grip when this frame is laid on the weapon\'s')
    L.append('-- matching frame, which is what puts the fingers on the grip')
    L.append('-- instead of near it:  wrist = weaponCF * weaponGrip * palm:Inverse()')
    L.append('M.HANDS = {')
    for side in ('Left', 'Right'):
        L.append('\t%s = {%s},' % (side, cframe_lua(gripsmod.hand_frame(rig, side))))
    L.append('}')
    L.append('')
    L.append('-- Rigid render chunks. `v` is already in the driving bone\'s local')
    L.append('-- space and centred on its own bounding box, so the runtime does')
    L.append('-- part.CFrame = boneWorldCF * CFrame.new(unpack(centre)).')
    L.append('M.SEGMENTS = {')
    for s in out_segments:
        L.append('\t"%s",' % s['name'])
    L.append('}')
    L.append('')
    for s in out_segments:
        c = COLORS.get(s['name'], (40, 43, 41))
        L.append("M['%s'] = {" % s['name'])
        L.append('\tbone = "%s",' % s['bone'])
        L.append('\tcolor = {%d,%d,%d},' % c)
        L.append('\tmaterial = "%s",' % MATERIAL.get(s['name'], 'Fabric'))
        L.append('\tcentre = {%s},' % ','.join(fmt(x, 4) for x in s['centre']))
        L.append('\tsize = {%s},' % ','.join(fmt(x, 4) for x in s['size']))
        L.append('\tv = {')
        L.append(flat(s['v'], 3, 8))
        L.append('\t},')
        L.append('\tt = {')
        L.append(ints(s['t'], 12))
        L.append('\t},')
        L.append('}')
        L.append('')
    L.append('return M')
    L.append('')

    text = '\n'.join(L)
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, 'w') as fh:
        fh.write(text)

    print('height  %.3f studs (%.1f cm)' % (height, height * CM_PER_STUD))
    print('%d segments, %d verts, %d tris' % (len(out_segments), total_v, total_t))
    print('wrote %s  (%.0f KB)' % (args.out, len(text) / 1024))
    for s in out_segments:
        print('   %-14s %-14s %5d v %5d t' % (s['name'], s['bone'], len(s['v']), len(s['t'])))


if __name__ == '__main__':
    main()
