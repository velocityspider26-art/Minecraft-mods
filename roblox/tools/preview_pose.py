#!/usr/bin/env python3
"""Render the soldier holding the rifle, using the same math the Luau runtime
uses, so the grip alignment can be checked without opening Studio.

    python3 tools/preview_pose.py --pose ads --out preview_ads.png

This is a verification tool, not part of the game. If the hands sit on the
grips here they sit on them in Roblox: the frames, the IK and the weapon
placement are the same construction, only written in numpy.
"""
import argparse
import os
import sys

import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.collections import PolyCollection

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blacksite.rig import SoldierRig, SEGMENTS, CM_PER_STUD
from blacksite import grips as G

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FBX = os.path.join(ROOT, 'assets', 'swat_operator.fbx')


# ---------------------------------------------------------------- transforms
def T(p):
    M = np.eye(4)
    M[:3, 3] = p
    return M


def axis_angle(axis, ang):
    axis = np.asarray(axis, float)
    axis = axis / max(np.linalg.norm(axis), 1e-12)
    x, y, z = axis
    c, s = np.cos(ang), np.sin(ang)
    C = 1 - c
    M = np.eye(4)
    M[:3, :3] = np.array([
        [c + x * x * C, x * y * C - z * s, x * z * C + y * s],
        [y * x * C + z * s, c + y * y * C, y * z * C - x * s],
        [z * x * C - y * s, z * y * C + x * s, c + z * z * C]])
    return M


def swing(a, b):
    """Minimal rotation taking vector a onto vector b."""
    a = a / max(np.linalg.norm(a), 1e-12)
    b = b / max(np.linalg.norm(b), 1e-12)
    v = np.cross(a, b)
    c = float(np.dot(a, b))
    if np.linalg.norm(v) < 1e-9:
        if c > 0:
            return np.eye(4)
        p = np.cross(a, [1.0, 0, 0])
        if np.linalg.norm(p) < 1e-6:
            p = np.cross(a, [0, 0, 1.0])
        return axis_angle(p, np.pi)
    return axis_angle(v, np.arctan2(np.linalg.norm(v), c))


def solve_elbow(shoulder, wrist, l1, l2, pole):
    """Elbow position for a two-bone chain, bending toward `pole`."""
    d = wrist - shoulder
    dist = float(np.linalg.norm(d))
    dist = max(min(dist, l1 + l2 - 1e-3), abs(l1 - l2) + 1e-3)
    dirv = d / max(np.linalg.norm(d), 1e-9)
    cos_a = np.clip((l1 * l1 + dist * dist - l2 * l2) / (2 * l1 * dist), -1, 1)
    a = np.arccos(cos_a)
    side = np.cross(dirv, pole)
    if np.linalg.norm(side) < 1e-6:
        side = np.cross(dirv, [0, 1.0, 0])
    bend = np.cross(side / np.linalg.norm(side), dirv)
    return shoulder + (dirv * np.cos(a) + bend / max(np.linalg.norm(bend), 1e-9) * np.sin(a)) * l1


# ---------------------------------------------------------------- the scene
class Preview:
    def __init__(self):
        self.rig = SoldierRig(FBX)
        S = self.rig.S
        verts = self.rig.body_vertices()
        floor = float(verts[:, 1].min())
        hips = self.rig.pos('Hips')
        self.shift = np.array([hips[0], floor, hips[2]])
        self.verts = verts - self.shift
        self.tris = self.rig.body_triangles()

        # Same assignment the shipped data uses, so the preview shows the
        # artefacts the game would actually have.
        keep, _dom, _names = self.rig.segment_triangles(self.tris)
        self.segments = []
        for i, (name, bone, _absorb) in enumerate(SEGMENTS):
            st = self.tris[keep[:, i]]
            if len(st) == 0:
                continue
            inv = np.linalg.inv(self.bind(bone))
            used = np.unique(st)
            remap = np.full(len(self.verts), -1, np.int64)
            remap[used] = np.arange(len(used))
            h = np.hstack([self.verts[used], np.ones((len(used), 1))])
            self.segments.append((name, bone, (h @ inv.T)[:, :3], remap[st]))

        # ---- weapon, in weapon-local space (origin at the pistol grip)
        parts, tri = {}, {}
        for gid in S.geos:
            nm = S.name[S.model_of_geo(gid)]
            if nm == 'sol_8_low':
                continue
            parts[nm] = S.world_verts(gid)
            tri[nm] = np.array(S.triangles(gid), int)
        barrel = parts['barrel_low']
        fwd = np.linalg.svd(barrel - barrel.mean(0), full_matrices=False)[2][0]
        if np.dot(barrel.mean(0) - parts['stockBack_low'].mean(0), fwd) < 0:
            fwd = -fwd
        ug = parts['topRail_low'].mean(0) - parts['mag_low'].mean(0)
        up = ug - fwd * np.dot(ug, fwd)
        up /= np.linalg.norm(up)
        B = np.stack([np.cross(up, fwd), up, fwd])
        origin = np.vstack(list(parts.values())).mean(0)

        def loc(v):
            return (np.atleast_2d(v) - origin) @ B.T

        LOC = {k: loc(v) for k, v in parts.items()}
        g = LOC['topGun_low']
        gripPts = g[(g[:, 2] >= -16.5) & (g[:, 2] <= -8.5) & (g[:, 1] >= -11) & (g[:, 1] <= 1)]
        GRIP = gripPts.mean(0)
        f = LOC['frontGrip_low']
        forePts = np.vstack([f[f[:, 1] <= -4.0], LOC['tape_low']])

        def to_rbx(p):
            p = np.atleast_2d(p) - GRIP
            return np.stack([-p[:, 0], p[:, 1], -p[:, 2]], 1) / CM_PER_STUD

        self.weapon = [(nm, to_rbx(LOC[nm]), tri[nm]) for nm in sorted(parts)]

        FWD = np.array([0.0, 0.0, -1.0])
        UP = np.array([0.0, 1.0, 0.0])
        self.Wg = {'right': G.weapon_grip_frame(to_rbx(gripPts), FWD, UP),
                   'left': G.weapon_grip_frame(to_rbx(forePts), FWD, UP)}
        rs, fs = to_rbx(LOC['backSight_low']), to_rbx(LOC['frontSIght_low'])
        d = 0.9 / CM_PER_STUD
        self.rear = np.array([rs[:, 0].mean(), rs[:, 1].max() - d, rs[:, 2].mean()])
        self.front = np.array([fs[:, 0].mean(), fs[:, 1].max() - d, fs[:, 2].mean()])
        self.Hg = {s: G.hand_frame(self.rig, s.capitalize()) for s in ('left', 'right')}

    def bind(self, name):
        M = self.rig.bind(name).copy()
        M[:3, 3] -= self.shift
        return M

    # ------------------------------------------------------------ posing
    def stance(self, ads):
        """Bone deltas for the shooting stance, in character space.

        A shooter does not aim with a square chest and neutral shoulders. The
        stance is bladed so the support shoulder leads, both clavicles protract
        and shrug so the shoulders come forward and up around the weapon, and
        the chest rounds over it. That is what puts the support hand within
        reach of a handguard 30 cm ahead of the pistol grip -- without it the
        arm is simply too short, which is exactly what an R15 soldier looks
        like when it holds a rifle.
        """
        world = {name: self.bind(name) for name in self.rig.bones}
        if ads <= 0:
            return world

        def rot_about(name, R, parentDelta=np.eye(4)):
            b = self.bind(name)
            o = (parentDelta @ np.append(b[:3, 3], 1.0))[:3]
            M = T(o) @ R @ np.linalg.inv(T(b[:3, 3])) @ parentDelta @ b
            return M, T(o) @ R @ np.linalg.inv(T(b[:3, 3])) @ parentDelta

        blade = np.radians(-19) * ads      # left shoulder leads
        rnd = np.radians(9) * ads          # chest rounds over the gun
        Dspine = axis_angle([0, 1.0, 0], blade * 0.35) @ axis_angle([1.0, 0, 0], rnd * 0.35)
        Dspine = T(self.bind('Spine')[:3, 3]) @ Dspine @ np.linalg.inv(T(self.bind('Spine')[:3, 3]))
        world['Spine'] = Dspine @ self.bind('Spine')

        Dchest = axis_angle([0, 1.0, 0], blade * 0.65) @ axis_angle([1.0, 0, 0], rnd * 0.65)
        p = (Dspine @ np.append(self.bind('Spine1')[:3, 3], 1.0))[:3]
        Dchest = T(p) @ Dchest @ np.linalg.inv(T(p)) @ Dspine
        world['Spine1'] = Dchest @ self.bind('Spine1')

        for side, sign in (('Left', 1.0), ('Right', -1.0)):
            prot = np.radians(16) * ads          # clavicle forward
            shrug = np.radians(7) * ads
            if side == 'Right':
                prot *= 0.55                      # firing side seats the stock
            cl = self.bind(side + 'Shoulder')
            o = (Dchest @ np.append(cl[:3, 3], 1.0))[:3]
            R = axis_angle([0, 1.0, 0], -sign * prot) @ axis_angle([0, 0, 1.0], sign * shrug)
            D = T(o) @ R @ np.linalg.inv(T(o)) @ Dchest
            world[side + 'Shoulder'] = D @ cl
            world[side + 'Arm'] = D @ self.bind(side + 'Arm')
            world[side + 'ForeArm'] = D @ self.bind(side + 'ForeArm')
            world[side + 'Hand'] = D @ self.bind(side + 'Hand')

        world['Neck'] = Dchest @ self.bind('Neck')
        world['Head'] = Dchest @ self.bind('Head')
        return world

    def pose(self, mode='ads'):
        ads = 1.0 if mode == 'ads' else 0.0
        world = self.stance(ads)

        # The dominant eye, not the centre of the face: the rifle comes to the
        # right eye and the head comes over the stock to meet it.
        head = world['Head']
        eye = (head @ np.append(np.array([0.0, 0.0, 0.0]), 1.0))[:3] \
            + head[:3, :3] @ np.array([0.085, 0.26, 0.0]) + np.array([0.0, 0.0, -0.30])

        if mode == 'ads':
            sightdir = self.front - self.rear
            Rw = swing(sightdir, np.array([0.0, 0.0, -1.0]))
            rear_w = Rw[:3, :3] @ self.rear
            Wworld = T(eye + np.array([0.0, 0.0, -0.30]) - rear_w) @ Rw
        else:
            chest = world['Spine1'][:3, 3]
            Wworld = T(chest + np.array([0.10, -0.30, -0.52])) @ axis_angle([1.0, 0, 0], np.radians(-32))

        for side in ('left', 'right'):
            Side = side.capitalize()
            target = Wworld @ self.Wg[side] @ np.linalg.inv(self.Hg[side])
            shoulder = world[Side + 'Arm'][:3, 3]
            wrist = target[:3, 3]
            l1 = np.linalg.norm(self.bind(Side + 'ForeArm')[:3, 3] - self.bind(Side + 'Arm')[:3, 3])
            l2 = np.linalg.norm(self.bind(Side + 'Hand')[:3, 3] - self.bind(Side + 'ForeArm')[:3, 3])
            # Never let the hand leave the arm. If the grip is genuinely out of
            # reach the arm straightens and the hand stops at full extension --
            # a slightly short grip reads as a grip, a detached hand does not.
            span = wrist - shoulder
            dist = float(np.linalg.norm(span))
            reach = (l1 + l2) * 0.995
            if dist > reach:
                wrist = shoulder + span * (reach / dist)
                target = target.copy()
                target[:3, 3] = wrist

            pole = np.array([-0.55 if side == 'left' else 0.55, -1.0, 0.30])
            elbow = solve_elbow(shoulder, wrist, l1, l2, pole)

            bindArm = self.bind(Side + 'Arm')
            A = swing(self.bind(Side + 'ForeArm')[:3, 3] - bindArm[:3, 3], elbow - shoulder) @ bindArm
            A[:3, 3] = shoulder
            world[Side + 'Arm'] = A

            bindFore = self.bind(Side + 'ForeArm')
            F = swing(self.bind(Side + 'Hand')[:3, 3] - bindFore[:3, 3], wrist - elbow) @ bindFore
            F[:3, 3] = elbow
            world[Side + 'ForeArm'] = F
            world[Side + 'Hand'] = target
        return world, Wworld

    # ----------------------------------------------------------- rendering
    def draw(self, mode, out):
        world, Wworld = self.pose(mode)
        fig, axes = plt.subplots(1, 3, figsize=(19, 10))
        for ax, (a, b, title) in zip(axes, [(0, 1, 'FRONT'), (2, 1, 'SIDE (faces -Z)'), (0, 2, 'TOP')]):
            for name, bone, lv, st in self.segments:
                M = world.get(bone, self.bind(bone))
                h = np.hstack([lv, np.ones((len(lv), 1))])
                w = (h @ M.T)[:, :3]
                ax.add_collection(PolyCollection(w[st][:, :, [a, b]], facecolors='#aab4bd',
                                                 edgecolors='#6d7b88', linewidths=.06, alpha=.95))
            for nm, v, t in self.weapon:
                h = np.hstack([v, np.ones((len(v), 1))])
                w = (h @ Wworld.T)[:, :3]
                ax.add_collection(PolyCollection(w[t][:, :, [a, b]], facecolors='#c4562f',
                                                 edgecolors='#7d3418', linewidths=.06, alpha=.98))
            ax.set_aspect('equal')
            ax.autoscale_view()
            ax.set_title('%s   [%s]' % (title, mode))
            ax.grid(alpha=.22)
        plt.tight_layout()
        plt.savefig(out, dpi=100)
        print('wrote', out)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--pose', default='ads', choices=['ads', 'ready'])
    ap.add_argument('--out', default='preview.png')
    a = ap.parse_args()
    Preview().draw(a.pose, a.out)
