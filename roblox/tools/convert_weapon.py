#!/usr/bin/env python3
"""swat_operator.fbx  ->  KSVRMesh.lua  (weapon geometry + derived grip anchors)

Run:  python3 tools/convert_weapon.py [--tris 5000]

The FBX ships the operator and his weapon as separate objects: the character is
in its bind pose and the rifle is parked in the scene, NOT held.  So the grips
are not read off the pose -- they are measured from the weapon's own geometry,
which is both more reliable and independent of whatever pose the artist saved:

  pistol grip   the part of the receiver behind the trigger that hangs below it
  foregrip      the vertical front grip, plus the tape wrapped around it
  sight line    rear aperture -> front post, which ADS aligns to the eye
  muzzle        furthest point down the bore axis

Output is in this project's weapon-local convention: origin at the firing-hand
grip, -Z down the bore, +Y up, studs.
"""
import argparse
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blacksite.scene import Scene
from blacksite import meshconv
from blacksite import grips as gripsmod

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CM_PER_STUD = 35.0

BODY_MESH = 'sol_8_low'

# Visual grouping of the 31 weapon meshes, so the runtime can colour them and
# so the animation system can move the moving ones.
GROUPS = {
    'receiver': ['topGun_low', 'bottomGun_low', 'sideKnobs_low', 'pin_low',
                 'stockButton_low', 'boneShape_low', 'tShape_low', 'pipe_low',
                 'eyeShape_low', 'handleButton_low'],
    'rail': ['topRail_low', 'sideRail_low', 'frontClamp_low', 'backClamp_low',
             'backScrew_low'],
    'stock': ['stockThing_low', 'stockFrame_low', 'stockBack_low', 'cheekPad_low'],
    'barrel': ['barrel_low', 'light_low', 'cable_low'],
    'sights': ['frontSIght_low', 'backSight_low', 'backAim_low'],
    'grip': ['frontGrip_low', 'tape_low'],
    'trigger': ['trigger_low'],
    'mag': ['mag_low'],
    'charge': ['handle_low', 'slider_low'],
}
COLOR = {
    'receiver': (44, 47, 50), 'rail': (38, 40, 43), 'stock': (52, 55, 51),
    'barrel': (30, 31, 33), 'sights': (36, 38, 40), 'grip': (40, 42, 44),
    'trigger': (58, 60, 63), 'mag': (46, 49, 52), 'charge': (54, 57, 60),
}
MATERIAL = {'stock': 'SmoothPlastic', 'grip': 'Fabric', 'mag': 'Metal'}

# Bones the weapon animation may drive independently of the receiver.
BONE_OF = {'mag': 'Magazine', 'charge': 'Bolt', 'trigger': 'Trigger'}


def fmt(x, nd=4):
    s = ('%.*f' % (nd, x)).rstrip('0').rstrip('.')
    return '0' if s in ('', '-0') else s


def flat(rows, nd=4, per_line=8):
    vals = [fmt(v, nd) for v in np.asarray(rows).ravel()]
    step = per_line * 3
    return '\n'.join('\t\t' + ','.join(vals[i:i + step]) + ','
                     for i in range(0, len(vals), step))


def ints(rows, per_line=12):
    vals = [str(int(v)) for v in np.asarray(rows).ravel()]
    step = per_line * 3
    return '\n'.join('\t\t' + ','.join(vals[i:i + step]) + ','
                     for i in range(0, len(vals), step))


def cframe_lua(M):
    """4x4 -> Roblox CFrame.new(x,y,z, R00,R01,R02, R10,...) argument list."""
    p, R = M[:3, 3], M[:3, :3]
    return ','.join(fmt(v, 5) for v in [
        p[0], p[1], p[2],
        R[0, 0], R[0, 1], R[0, 2],
        R[1, 0], R[1, 1], R[1, 2],
        R[2, 0], R[2, 1], R[2, 2]])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--fbx', default=os.path.join(ROOT, 'assets', 'swat_operator.fbx'))
    ap.add_argument('--out', default=os.path.join(ROOT, 'src', 'ReplicatedStorage',
                                                  'Modules', 'KSVRMesh.lua'))
    ap.add_argument('--tris', type=int, default=5000)
    args = ap.parse_args()

    S = Scene(args.fbx)
    parts, tris_of = {}, {}
    for gid in S.geos:
        nm = S.name[S.model_of_geo(gid)]
        if nm == BODY_MESH:
            continue
        parts[nm] = S.world_verts(gid)
        tris_of[nm] = np.array(S.triangles(gid), int)

    # ---- weapon frame: axis0 = gun's LEFT, axis1 = up, axis2 = toward muzzle
    barrel = parts['barrel_low']
    fwd = np.linalg.svd(barrel - barrel.mean(0), full_matrices=False)[2][0]
    if np.dot(barrel.mean(0) - parts['stockBack_low'].mean(0), fwd) < 0:
        fwd = -fwd
    ug = parts['topRail_low'].mean(0) - parts['mag_low'].mean(0)
    up = ug - fwd * np.dot(ug, fwd)
    up /= np.linalg.norm(up)
    left = np.cross(up, fwd)
    B = np.stack([left, up, fwd])
    world_origin = np.vstack(list(parts.values())).mean(0)

    def to_local(v):
        return (np.atleast_2d(v) - world_origin) @ B.T

    LOC = {k: to_local(v) for k, v in parts.items()}

    def region(name, bore=None, upr=None):
        v = LOC[name]
        m = np.ones(len(v), bool)
        if bore:
            m &= (v[:, 2] >= bore[0]) & (v[:, 2] <= bore[1])
        if upr:
            m &= (v[:, 1] >= upr[0]) & (v[:, 1] <= upr[1])
        return v[m]

    grip_pts = region('topGun_low', bore=(-16.5, -8.5), upr=(-11.0, 1.0))
    fore_pts = np.vstack([region('frontGrip_low', upr=(-12, -4.0)), LOC['tape_low']])
    GRIP = grip_pts.mean(0)
    FORE = fore_pts.mean(0)
    TRIG = LOC['trigger_low'].mean(0)
    rs, fs = LOC['backSight_low'], LOC['frontSIght_low']
    REAR = np.array([rs[:, 0].mean(), rs[:, 1].max() - 0.9, rs[:, 2].mean()])
    FRONT = np.array([fs[:, 0].mean(), fs[:, 1].max() - 0.9, fs[:, 2].mean()])
    bar = LOC['barrel_low']
    MUZZLE = np.array([bar[:, 0].mean(), bar[:, 1].mean(), bar[:, 2].max()])
    mag = LOC['mag_low']
    MAGTOP = mag[mag[:, 1] > mag[:, 1].max() - 1.5].mean(0)
    CHARGE = LOC['handle_low'].mean(0)
    BUTT = LOC['stockBack_low'].mean(0)

    # ---- gun-local cm -> Roblox weapon-local studs, origin at the pistol grip
    def rbx(p):
        p = np.asarray(p, float) - GRIP
        return np.array([-p[0], p[1], -p[2]]) / CM_PER_STUD

    group_of = {}
    for g, names in GROUPS.items():
        for n in names:
            group_of[n] = g

    # ---- decimate, grouped, into weapon-local space
    budget_total = args.tris
    all_area = {}
    for nm, v in parts.items():
        t = tris_of[nm]
        a, b, c = v[t[:, 0]], v[t[:, 1]], v[t[:, 2]]
        all_area[nm] = float(0.5 * np.linalg.norm(np.cross(b - a, c - a), axis=1).sum())
    tot_area = sum(all_area.values())

    out = []
    total_v = total_t = 0
    for nm in sorted(parts):
        v, t = LOC[nm], tris_of[nm]
        # (left, up, fwd) -> (right, up, back) negates two axes, which is a 180
        # degree turn about up rather than a reflection, so triangle winding is
        # preserved and the indices are left alone.
        rv = np.stack([-(v[:, 0] - GRIP[0]), v[:, 1] - GRIP[1],
                       -(v[:, 2] - GRIP[2])], 1) / CM_PER_STUD
        rv, t = meshconv.weld(rv, t)
        want = max(60, int(budget_total * all_area[nm] / tot_area))
        rv, t = meshconv.decimate_to(rv, t, want)
        if len(t) == 0:
            continue
        centre = (rv.min(0) + rv.max(0)) * 0.5
        size = rv.max(0) - rv.min(0)
        out.append(dict(name=nm.replace('_low', ''), group=group_of.get(nm, 'receiver'),
                        v=rv - centre, t=t + 1, centre=centre, size=size))
        total_v += len(rv)
        total_t += len(t)

    L = []
    L.append('--!nonstrict')
    L.append('-- KSVRMesh -- GENERATED by roblox/tools/convert_weapon.py.')
    L.append('-- Do not hand-edit; re-run the tool instead.')
    L.append('--')
    L.append('-- Geometry of the rifle that ships inside the SWAT Operator FBX')
    L.append('-- (material name "KSVR"). See roblox/ATTRIBUTION.md.')
    L.append('--')
    L.append('-- Weapon-local space, matching MK18Mesh: origin at the firing-hand')
    L.append('-- grip, -Z down the bore, +Y up, studs.')
    L.append('')
    L.append('local M = {}')
    L.append('')
    L.append('-- Anchors measured off the geometry, not off the artist\'s pose.')
    L.append('M.ANCHORS = {')
    for key, p in [('rightGrip', GRIP), ('leftGrip', FORE), ('trigger', TRIG),
                   ('rearSight', REAR), ('frontSight', FRONT), ('muzzle', MUZZLE),
                   ('magwell', MAGTOP), ('chargingHandle', CHARGE), ('buttPad', BUTT)]:
        q = rbx(p)
        L.append('\t%s = {%s},' % (key, ','.join(fmt(x, 4) for x in q)))
    L.append('}')
    L.append('')
    sight_len = float(np.linalg.norm(FRONT - REAR) / CM_PER_STUD)
    # Full grip frames, not just points: a hand has to arrive at the right
    # ROLL as well as the right place, or the fingers close on air. X runs up
    # the grip's long axis (the trigger end), Z points out of the face the
    # fingers wrap onto. These pair with SoldierRigData.HANDS.
    FWD = np.array([0.0, 0.0, -1.0])
    UP = np.array([0.0, 1.0, 0.0])

    def to_rbx_pts(p):
        p = np.atleast_2d(p) - GRIP
        return np.stack([-p[:, 0], p[:, 1], -p[:, 2]], 1) / CM_PER_STUD

    grip_frames = {
        'right': gripsmod.weapon_grip_frame(to_rbx_pts(grip_pts), FWD, UP),
        'left': gripsmod.weapon_grip_frame(to_rbx_pts(fore_pts), FWD, UP),
    }
    L.append('M.GRIPS = {')
    for k in ('right', 'left'):
        L.append('\t%s = {%s},' % (k, cframe_lua(grip_frames[k])))
    L.append('}')
    L.append('')
    L.append('M.SIGHT_RADIUS = %s' % fmt(sight_len, 4))
    L.append('M.LENGTH = %s' % fmt(float(
        (np.vstack(list(LOC.values()))[:, 2].max() -
         np.vstack(list(LOC.values()))[:, 2].min()) / CM_PER_STUD), 4))
    L.append('')
    L.append('M.parts = {')
    for s in out:
        L.append("\t'%s'," % s['name'])
    L.append('}')
    L.append('')
    for s in out:
        col = COLOR.get(s['group'], (44, 47, 50))
        L.append("M['%s'] = {" % s['name'])
        L.append('\tgroup = "%s",' % s['group'])
        bone = BONE_OF.get(s['group'])
        if bone:
            L.append('\tbone = "%s",' % bone)
        L.append('\tcolor = {%d,%d,%d},' % col)
        L.append('\tmaterial = "%s",' % MATERIAL.get(s['group'], 'Metal'))
        L.append('\tcentre = {%s},' % ','.join(fmt(x, 4) for x in s['centre']))
        L.append('\tsize = {%s},' % ','.join(fmt(x, 4) for x in s['size']))
        L.append('\tv = {')
        L.append(flat(s['v'], 4, 8))
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
    open(args.out, 'w').write(text)

    print('weapon-local anchors (studs):')
    for key, p in [('rightGrip', GRIP), ('leftGrip', FORE), ('trigger', TRIG),
                   ('rearSight', REAR), ('frontSight', FRONT), ('muzzle', MUZZLE),
                   ('magwell', MAGTOP), ('chargingHandle', CHARGE), ('buttPad', BUTT)]:
        print('   %-16s %s' % (key, np.round(rbx(p), 4)))
    print('sight radius %.4f  length %.4f studs' % (sight_len, float(
        (np.vstack(list(LOC.values()))[:, 2].max() -
         np.vstack(list(LOC.values()))[:, 2].min()) / CM_PER_STUD)))
    print('%d parts, %d verts, %d tris' % (len(out), total_v, total_t))
    print('wrote %s  (%.0f KB)' % (args.out, len(text) / 1024))


if __name__ == '__main__':
    main()
