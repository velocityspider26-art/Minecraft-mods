"""AAA performance polish pass.

The cleanup pipeline produces geometrically correct motion; this pass adds the
performance layer that separates shipped animation from cleaned mocap:

  A  head/eye line     he must watch what his hands are doing
  B  shoulder girdle   clavicles follow the arm (scapulohumeral rhythm)
  C  chest engagement  the thorax participates in the reach instead of being rigid
  D  breathing         phase-dependent respiration, incl. exertion and recovery
  E  effort + bracing  the windlass turns cost him something
  F  micro-texture     nothing in a living body is ever perfectly still

Every change is an additive local-rotation delta on the upper body, and the arms
are re-solved afterwards so the hands stay exactly where the tourniquet work put
them.
"""
from __future__ import annotations

import sys
import numpy as np
from pygltflib import GLTF2

from anim_data import Anim, qmul, qconj, qnorm, slerp, qangle_between
from filters import (smooth_quat_track, gauss_smooth, smootherstep, qlog, qexp,
                     two_bone_ik, aim_rotation)
from gltf_io import quat_to_mat
from glb_write import write_animation

SRC = sys.argv[1]
DST = sys.argv[2]

g = GLTF2().load(SRC)
A = Anim(g, 0)
nm = A.skel.names
I = {k: i for i, k in enumerate(nm)}
F, dt = A.F, A.dt
par = A.skel.parent
rot, trans = A.rot.copy(), A.trans.copy()
IDENT = np.array([0.0, 0.0, 0.0, 1.0])
rng = np.random.default_rng(20240816)     # fixed: pipeline stays deterministic

print(f"[polish] {SRC}  frames={F} fps={A.fps:.2f}")


def fk_from(r, t):
    B = A.copy()
    B.rot, B.trans, B._world = r, t, None
    return B.fk()


def clamp_rot(q, max_deg):
    """Limit a rotation's magnitude, preserving its axis."""
    v = qlog(q)
    n = np.linalg.norm(v, axis=-1, keepdims=True)
    m = np.deg2rad(max_deg)
    scale = np.where(n > m, m / np.maximum(n, 1e-12), 1.0)
    return qexp(v * scale)


def band(f0, f1, rise, fall, F=F):
    m = np.zeros(F)
    for f in range(max(0, f0), min(F, f1)):
        a = smootherstep(min(1.0, (f - f0 + 1) / max(rise, 1)))
        b = smootherstep(min(1.0, (f1 - 1 - f) / max(fall, 1)))
        m[f] = min(a, b)
    return m


W, P, Q = fk_from(rot, trans)

# Record the hands so the tourniquet interaction can be restored after the
# torso/girdle come alive underneath them.
HANDS = {"L": (I["LeftShoulder"], I["LeftArm"], I["LeftForeArm"], I["LeftHand"]),
         "R": (I["RightShoulder"], I["RightArm"], I["RightForeArm"], I["RightHand"])}
hand_world_p = {s: P[:, v[3]].copy() for s, v in HANDS.items()}
hand_world_q = {s: Q[:, v[3]].copy() for s, v in HANDS.items()}

# ============================================================== A  HEAD / EYELINE
print("\n[A] head + eye line")
NECK, HEAD = I["Neck"], I["Head"]
work = (P[:, I["LeftHand"]] + P[:, I["RightHand"]]) / 2.0
# aim slightly at the thigh itself rather than the exact hand midpoint, and lag
# it - a head never snaps onto a moving target
thigh = (P[:, I["LeftUpLeg"]] + P[:, I["LeftLeg"]]) / 2.0
target = 0.55 * work + 0.45 * thigh
# Heavily damped: a head tracks the general area of the work, not every twitch
# of the hands. A tight follow re-introduces exactly the chatter the cleanup
# removed, straight into the most noticeable joint on the character.
target = gauss_smooth(target, sigma=11.0)

look_mask = np.maximum(band(96, 372, 40, 34), 0.0)
MAX_LOOK = 50.0
before_ang = []
after_ang = []
for f in range(F):
    Rh = quat_to_mat(Q[f, HEAD])
    fwd = Rh[:, 2]
    d = target[f] - P[f, HEAD]
    dn = np.linalg.norm(d)
    if dn < 1e-6:
        continue
    d = d / dn
    before_ang.append(np.degrees(np.arccos(np.clip(np.dot(fwd, d), -1, 1))))
    if look_mask[f] <= 1e-4:
        continue
    q_full = aim_rotation(fwd, d)
    q_use = clamp_rot(q_full, MAX_LOOK)
    q_use = qexp(qlog(q_use) * look_mask[f])
    # Bend the neck by part of the correction so the flexion is shared, but the
    # HEAD still ends on the full corrected orientation - otherwise the neck's
    # contribution cancels out of the head's world transform and only the head's
    # own share ever reaches the eyeline.
    q_n = qexp(qlog(q_use) * 0.42)
    Qn_new = qmul(q_n, Q[f, NECK])
    rot[f, NECK] = qmul(qconj(Q[f, par[NECK]]), Qn_new)
    rot[f, HEAD] = qmul(qconj(Qn_new), qmul(q_use, Q[f, HEAD]))

for i in (NECK, HEAD):
    rot[:, i] = slerp(smooth_quat_track(rot[:, i], half=6, degree=0, robust=False),
                      rot[:, i], np.full(F, 0.45))
W, P, Q = fk_from(rot, trans)
for f in range(F):
    Rh = quat_to_mat(Q[f, HEAD])
    d = target[f] - P[f, HEAD]
    d = d / max(np.linalg.norm(d), 1e-9)
    after_ang.append(np.degrees(np.arccos(np.clip(np.dot(Rh[:, 2], d), -1, 1))))
ba, aa = np.array(before_ang), np.array(after_ang)
sl = slice(150, 351)
print(f"  head-to-work angle, f150-350: {ba[sl].mean():.1f}° -> {aa[sl].mean():.1f}°"
      f"  (min {aa[sl].min():.1f}°, max {aa[sl].max():.1f}°)")

# ========================================================= B  SHOULDER GIRDLE
print("\n[B] shoulder girdle (scapulohumeral rhythm)")
# Clavicles are frozen in the capture (1.3 deg over the whole clip). Drive them
# from the humerus: beyond ~25 deg of elevation the girdle contributes roughly a
# third of further arm travel. Without this, every reach looks like a doll's.
GIRDLE_GAIN, GIRDLE_MAX, GIRDLE_KNEE = 0.30, 13.0, 25.0
for side, (clav, arm, fore, hand) in HANDS.items():
    rest_dir = A.skel.rest_t[fore] / np.linalg.norm(A.skel.rest_t[fore])
    add = np.zeros((F, 4))
    add[:] = IDENT
    for f in range(F):
        cur = quat_to_mat(rot[f, arm]) @ rest_dir      # arm dir in clavicle space
        dot = float(np.clip(np.dot(cur, rest_dir), -1, 1))
        ang = np.degrees(np.arccos(dot))
        drive = max(0.0, ang - GIRDLE_KNEE) * GIRDLE_GAIN
        if drive <= 1e-3:
            continue
        axis = np.cross(rest_dir, cur)
        an = np.linalg.norm(axis)
        if an < 1e-6:
            continue
        add[f] = qexp(axis / an * np.deg2rad(min(drive, GIRDLE_MAX)))
    add = qnorm(gauss_smooth(add, sigma=3.0))
    rot[:, clav] = qmul(add, rot[:, clav])
    d = np.rad2deg(qangle_between(rot[1:, clav], rot[:-1, clav]))
    print(f"  {nm[clav]:<15s} travel 1.3° -> {d.sum():.1f}°  (max {d.max():.3f}°/frame)")

# ========================================================== C  CHEST ENGAGEMENT
print("\n[C] chest engagement")
SP2, SP1 = I["Spine2"], I["Spine1"]
# Spine2 carries 0.8 deg across the whole clip - a rigid thorax. Give it a share
# of the reach so the upper body bends as a chain instead of a hinge at the waist.
chest_drive = np.zeros((F, 4))
chest_drive[:] = IDENT
armL = quat_to_mat(rot[:, HANDS["L"][1]])
armR = quat_to_mat(rot[:, HANDS["R"][1]])
for f in range(F):
    vL = qlog(rot[f, HANDS["L"][1]])
    vR = qlog(rot[f, HANDS["R"][1]])
    chest_drive[f] = qexp(np.clip((vL + vR) * 0.055, -0.14, 0.14))
chest_drive = qnorm(gauss_smooth(chest_drive, sigma=5.0))
rot[:, SP2] = qmul(chest_drive, rot[:, SP2])
d = np.rad2deg(qangle_between(rot[1:, SP2], rot[:-1, SP2]))
print(f"  Spine2 travel 0.8° -> {d.sum():.1f}°  (max {d.max():.3f}°/frame)")

# ================================================================= D  BREATHING
print("\n[D] breathing")
# Rate and depth by phase: wound reaction -> settling -> work -> windlass
# exertion (fast, shallow, with holds) -> recovery (deep, slow).
ctrl_f = np.array([0, 60, 150, 250, 300, 330, 360, 424])
ctrl_hz = np.array([0.38, 0.34, 0.29, 0.30, 0.46, 0.44, 0.24, 0.20])
ctrl_am = np.array([1.05, 1.15, 1.00, 1.05, 0.55, 0.60, 1.55, 1.35])
hz = np.interp(np.arange(F), ctrl_f, ctrl_hz)
amp = np.interp(np.arange(F), ctrl_f, ctrl_am)
phase = np.cumsum(2 * np.pi * hz * dt)
breath = np.sin(phase)
# a held breath as he commits to each windlass turn
hold = 1.0 - 0.55 * band(258, 300, 12, 12)
breath_sig = breath * amp * hold

BREATH_AX = np.array([1.0, 0.0, 0.0])       # chest flexion/extension
for j, gain in [(SP1, 0.55), (SP2, 0.75), (I["Spine"], 0.30)]:
    add = qexp(BREATH_AX[None, :] * np.deg2rad(breath_sig * gain)[:, None])
    rot[:, j] = qmul(add, rot[:, j])
for side, (clav, *_rest) in HANDS.items():
    sgn = 1.0 if side == "L" else -1.0
    add = qexp(np.array([0.0, 0.0, sgn])[None, :] * np.deg2rad(breath_sig * 0.30)[:, None])
    rot[:, clav] = qmul(add, rot[:, clav])
print(f"  rate {hz.min():.2f}-{hz.max():.2f} Hz, depth {amp.min():.2f}-{amp.max():.2f}°, "
      f"breath-hold over the windlass turns")

# ============================================================ E  EFFORT/BRACING
print("\n[E] effort + bracing over the windlass")
eff = band(248, 332, 26, 30)
# brace: compress slightly, lean into the work, tension through the girdle
trans[:, I["Hips"], 1] -= 0.010 * eff
lean = qexp(np.array([1.0, 0.0, 0.0])[None, :] * np.deg2rad(2.4 * eff)[:, None])
rot[:, I["Spine"]] = qmul(lean, rot[:, I["Spine"]])
rot[:, SP1] = qmul(qexp(np.array([1.0, 0.0, 0.0])[None, :] * np.deg2rad(1.6 * eff)[:, None]),
                   rot[:, SP1])
for side, (clav, *_r) in HANDS.items():
    sgn = 1.0 if side == "L" else -1.0
    rot[:, clav] = qmul(
        qexp(np.array([0.0, 0.0, sgn])[None, :] * np.deg2rad(2.0 * eff)[:, None]),
        rot[:, clav])
# head dips toward the work as he bears down
rot[:, HEAD] = qmul(qexp(np.array([1.0, 0.0, 0.0])[None, :] * np.deg2rad(3.0 * eff)[:, None]),
                    rot[:, HEAD])
print(f"  brace over f248-332: 1.0 cm compression, 2.4° torso lean, girdle tension")

# ==================================================== E2  SETTLE / FOLLOW-THROUGH
print("\n[E2] weight settle + follow-through")


def damped(f0, amp, tau, hz):
    """Decaying oscillation starting at f0 - weight arriving and settling."""
    s = np.zeros(F)
    t = np.arange(F - f0) * dt
    s[f0:] = amp * np.exp(-t / tau) * np.sin(2 * np.pi * hz * t)
    return s


# weight arriving on the knee, and the release when the windlass lets go
settle = damped(150, -0.011, 0.42, 2.1) + damped(334, 0.006, 0.34, 2.4)
trans[:, I["Hips"], 1] += settle
# torso follows the weight a beat later (overlap, not lockstep)
lag = np.concatenate([np.zeros(3), settle[:-3]])
# Scale chosen so an 11 mm settle produces ~2.5 deg of spine lag. The earlier
# factor turned it into 26 deg, which swung the head a quarter of a metre at
# 2 Hz and put more chatter into the head than the cleanup had taken out.
rot[:, I["Spine"]] = qmul(
    qexp(np.array([1.0, 0.0, 0.0])[None, :] * (-lag * 4.0)[:, None]), rot[:, I["Spine"]])
print(f"  knee-plant settle {abs(settle[150:200]).max()*1000:.1f} mm, "
      f"windlass release {abs(settle[334:380]).max()*1000:.1f} mm, torso lags 3 frames")

# =============================================================== F  MICRO-TEXTURE
print("\n[F] micro-texture")
# Perfectly static bones read as dead. This is well below the mocap chatter that
# was removed (which peaked over 1.4 deg/frame) - it is stabiliser tone, not noise.
TEX = {"Hips": 0.055, "Spine": 0.05, "Spine1": 0.05, "Spine2": 0.045,
       "Neck": 0.05, "Head": 0.045, "LeftShoulder": 0.035, "RightShoulder": 0.035}
for j, a_deg in TEX.items():
    n = rng.normal(size=(F, 3))
    n = gauss_smooth(n, sigma=5.0)
    n /= max(np.abs(n).max(), 1e-9)
    rot[:, I[j]] = qmul(qexp(n * np.deg2rad(a_deg)), rot[:, I[j]])
print(f"  {len(TEX)} bones given {min(TEX.values()):.3f}-{max(TEX.values()):.3f}° "
      f"of low-frequency stabiliser tone")

# ====================================================== RESTORE HAND PLACEMENT
print("\n[G] re-solving arms to hold the tourniquet contact")
W, P, Q = fk_from(rot, trans)
moved = []
for side, (clav, arm, fore, hand) in HANDS.items():
    la = float(np.linalg.norm(A.skel.rest_t[fore]))
    lb = float(np.linalg.norm(A.skel.rest_t[hand]))
    axis = P[:, hand] - P[:, arm]
    u = axis / np.maximum(np.linalg.norm(axis, axis=-1, keepdims=True), 1e-9)
    v = P[:, fore] - P[:, arm]
    perp = gauss_smooth(v - u * np.sum(v * u, axis=-1, keepdims=True), sigma=4.0)
    pn = np.linalg.norm(perp, axis=-1, keepdims=True)
    perp = np.where(pn < 1e-6, np.array([0.0, 1.0, 0.0]), perp / np.maximum(pn, 1e-9))
    pole = P[:, arm] + perp * 0.3
    for f in range(F):
        tgt = hand_world_p[side][f]
        moved.append(float(np.linalg.norm(tgt - P[f, hand])))
        m2, e2 = two_bone_ik(P[f, arm][None], None, None, tgt[None], pole[f][None], la, lb)
        qa = aim_rotation(P[f, fore] - P[f, arm], m2[0] - P[f, arm])
        Qs = qmul(qa, Q[f, arm])
        rot[f, arm] = qmul(qconj(Q[f, par[arm]]), Qs)
        dl = quat_to_mat(qa) @ (P[f, hand] - P[f, fore])
        qb = aim_rotation(dl, e2[0] - m2[0])
        Qe = qmul(qb, qmul(qa, Q[f, fore]))
        rot[f, fore] = qmul(qconj(Qs), Qe)
        rot[f, hand] = qmul(qconj(Qe), hand_world_q[side][f])
    for i in (arm, fore, hand):
        rot[:, i] = slerp(smooth_quat_track(rot[:, i], half=3, degree=0, robust=False),
                          rot[:, i], np.full(F, 0.80))
print(f"  mean arm re-solve: {np.mean(moved)*100:.2f} cm  (max {np.max(moved)*100:.2f} cm)")

W, P, Q = fk_from(rot, trans)
res = [float(np.linalg.norm(P[f, HANDS[s][3]] - hand_world_p[s][f]))
       for s in HANDS for f in range(F)]
print(f"  residual hand placement error: mean {np.mean(res)*100:.3f} cm  "
       f"max {np.max(res)*100:.3f} cm")

# ============================================================ FLOOR SAFETY NET
low = np.minimum(P[:, :, 1].min(axis=1), 0.0)
if low.min() < -0.003:
    lift = gauss_smooth(np.maximum(-low, 0.0)[:, None], sigma=2.5)[:, 0]
    trans[:, I["Hips"], 1] += lift
    W, P, Q = fk_from(rot, trans)
print(f"\n[floor] lowest joint {low.min()*100:.2f} cm -> {min(P[:,:,1].min(),0)*100:.2f} cm")

for i in sorted(A.animated["rotation"]):
    rot[:, i] = qnorm(rot[:, i])
assert np.all(np.isfinite(rot)) and np.all(np.isfinite(trans))
write_animation(SRC, DST,
                {i: rot[:, i] for i in sorted(A.animated["rotation"])},
                {i: trans[:, i] for i in sorted(A.animated["translation"])},
                anim_index=0)
print("[polish done]")
