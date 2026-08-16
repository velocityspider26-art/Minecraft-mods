"""C.O.R.E. left-thigh CAT tourniquet - animation cleanup / reconstruction pass.

Stages
  1  de-chatter        bone-group weighted robust local-polynomial manifold filter
  2  contact model     height + vertical-velocity planting detection (a skating
                       foot slides fast while staying planted, so total speed is
                       the wrong test)
  3  root stabilise    footlock integrator: cancels per-frame slide of planted
                       contacts, holds the accumulated offset between contacts so
                       genuine repositioning survives and nothing ever snaps
  4  ending rebuild    f351-424 reconstructed from the performer's own descent
  5  contact IK        per-limb planting with eased constraint blending
  6  ground + seams    floor clamp and cross-seam continuity
"""
from __future__ import annotations

import sys
import numpy as np
from pygltflib import GLTF2

from anim_data import Anim, qmul, qconj, qnorm, slerp, qangle_between
from filters import (smooth_quat_track, smooth_vec_track, gauss_smooth, qlog, qexp,
                     smootherstep, two_bone_ik, aim_rotation)
from gltf_io import quat_to_mat
from glb_write import write_animation

SRC = sys.argv[1] if len(sys.argv) > 1 else "source.glb"
DST = sys.argv[2] if len(sys.argv) > 2 else "out.glb"

LAST_GOOD = 350
FINGERS = ("Thumb", "Index", "Middle", "Ring", "Pinky")

g = GLTF2().load(SRC)
A = Anim(g, 0)
nm = A.skel.names
I = {k: i for i, k in enumerate(nm)}
F, dt = A.F, A.dt
par = A.skel.parent
HIPS = I["Hips"]

print(f"[load] {SRC} frames={F} fps={A.fps:.2f} dur={A.duration:.4f}s anim='{A.name}'")


def group_of(name):
    if any(t in name for t in FINGERS):
        return "finger"
    for key, tag in [("Shoulder", "shoulder"), ("ForeArm", "forearm"), ("Arm", "arm"),
                     ("Hand", "hand"), ("ToeBase", "toe"), ("Foot", "foot"),
                     ("UpLeg", "upleg"), ("Leg", "leg"), ("Neck", "neck"),
                     ("Head", "head"), ("Spine", "spine"), ("Hips", "hips")]:
        if key in name:
            return tag
    return "other"


# Pass A (outlier/spike rejection) window half-widths.
HALF = {"finger": 2, "hand": 3, "forearm": 3, "arm": 3, "shoulder": 2,
        "spine": 4, "neck": 4, "head": 4, "hips": 4, "upleg": 4,
        "leg": 4, "foot": 4, "toe": 5, "other": 3}

# Pass B: fraction of the high-frequency band that survives. 1.0 = untouched,
# 0.0 = fully smoothed. Fingers keep most of their detail per the brief; arms
# and hands keep enough to preserve strap pulls and windlass twists; torso,
# head and feet are pushed hardest because that is where chatter reads worst.
KEEP = {"finger": 0.78, "hand": 0.50, "forearm": 0.46, "arm": 0.44, "shoulder": 0.55,
        "spine": 0.30, "neck": 0.26, "head": 0.24, "hips": 0.34, "upleg": 0.34,
        "leg": 0.34, "foot": 0.30, "toe": 0.26, "other": 0.40}
HF_HALF = {"finger": 3, "hand": 4, "forearm": 4, "arm": 4, "shoulder": 3,
           "spine": 5, "neck": 5, "head": 5, "hips": 5, "upleg": 5,
           "leg": 5, "foot": 5, "toe": 6, "other": 4}

rot, trans = A.rot.copy(), A.trans.copy()


def rough_rms(q):
    mid = q[:-2] + q[2:]
    mid /= np.linalg.norm(mid, axis=-1, keepdims=True)
    r = np.rad2deg(qangle_between(q[1:-1], mid))
    return float(np.sqrt((r ** 2).mean()))


def fk_from(r_arr, t_arr):
    B = A.copy()
    B.rot, B.trans, B._world = r_arr, t_arr, None
    return B.fk()


# ======================================================================= STAGE 1
print("\n[stage 1] de-chatter (robust local-polynomial, manifold tangent space)")
b_rough, a_rough = {}, {}
for i in sorted(A.animated["rotation"]):
    grp = group_of(nm[i])
    b_rough[nm[i]] = rough_rms(rot[:LAST_GOOD + 1, i])
    # Pass A: robust quadratic fit rejects isolated corrupted samples without
    # flattening genuine velocity or curvature.
    q = smooth_quat_track(rot[:, i], half=HALF[grp], degree=2, robust=True)
    # Pass B: split into a smooth reference plus a high-frequency residual and
    # attenuate only the residual. This removes frame-to-frame chatter while
    # leaving the gross trajectory - and its timing - completely intact.
    ref = smooth_quat_track(q, half=HF_HALF[grp], degree=0, robust=False)
    rot[:, i] = slerp(ref, q, np.full(len(q), KEEP[grp]))
    a_rough[nm[i]] = rough_rms(rot[:LAST_GOOD + 1, i])


def _mean(d, fing):
    v = [x for k, x in d.items() if (any(t in k for t in FINGERS) == fing)]
    return float(np.mean(v))


print(f"  body   roughness {_mean(b_rough,False):.3f}° -> {_mean(a_rough,False):.3f}°"
      f"  ({100*(1-_mean(a_rough,False)/_mean(b_rough,False)):.1f}% reduction)")
print(f"  finger roughness {_mean(b_rough,True):.3f}° -> {_mean(a_rough,True):.3f}°"
      f"  ({100*(1-_mean(a_rough,True)/_mean(b_rough,True)):.1f}% reduction, kept light)")

trans[:, HIPS] = smooth_vec_track(trans[:, HIPS], half=4, degree=2)

# ======================================================================= STAGE 2
print("\n[stage 2] contact detection (height + vertical velocity)")
W, P, Q = fk_from(rot, trans)

CONTACTS = ["RightToeBase", "RightFoot", "LeftFoot", "LeftToeBase"]


def detect(joint_name, y_tol=0.055, vy_tol=0.22, min_run=8):
    """Planted = near this joint's own resting height with little vertical motion.

    Deliberately ignores horizontal speed: a foot that skates across the floor
    is still in contact, and those are exactly the frames we must repair.
    """
    p = P[:, I[joint_name]]
    vy = np.abs(np.gradient(p[:, 1], dt))
    floor = np.percentile(p[:, 1], 10)
    c = (p[:, 1] < floor + y_tol) & (vy < vy_tol)
    runs, s = [], None
    for f in range(F):
        if c[f] and s is None:
            s = f
        elif not c[f] and s is not None:
            if f - s >= min_run:
                runs.append((s, f))
            s = None
    if s is not None and F - s >= min_run:
        runs.append((s, F))
    return runs


contacts = {}
for cn in CONTACTS:
    runs = [r for r in detect(cn) if r[0] <= LAST_GOOD]
    contacts[cn] = runs
    tot = sum(b - a for a, b in runs)
    print(f"  {cn:<14s} {len(runs)} interval(s), {tot} frames: {runs}")

# ======================================================================= STAGE 3
print("\n[stage 3] root stabilisation (treatment-phase anchor)")
# Measured structure of this capture: the right toe is genuinely planted from
# f170-280 (net travel 0.05 cm) and then the ENTIRE body slides ~40 cm to f350
# while the toe never leaves the floor (max height 1.75 cm). That is skate, not
# a step, so the anchor is the toe's own position during its clean window.
ANCH = I["RightToeBase"]
CORE_A, CORE_B = 170, 281                  # provably-planted reference window
STAB_A, STAB_B = 162, LAST_GOOD + 1        # span to stabilise
RAMP = 16

toe = P[:, ANCH]
ref_xz = np.median(toe[CORE_A:CORE_B][:, [0, 2]], axis=0)
print(f"  anchor reference XZ = ({ref_xz[0]:+.4f}, {ref_xz[1]:+.4f}) "
      f"from f{CORE_A}-{CORE_B} (net travel "
      f"{np.linalg.norm(toe[CORE_B-1,[0,2]]-toe[CORE_A,[0,2]])*100:.2f} cm)")

err = np.zeros((F, 3))
err[:, 0] = toe[:, 0] - ref_xz[0]
err[:, 2] = toe[:, 2] - ref_xz[1]

mask = np.zeros(F)
mask[STAB_A:STAB_B] = 1.0
for k in range(RAMP):
    f = STAB_A + k
    if f < F:
        mask[f] = smootherstep(k / RAMP)

corr = -err * mask[:, None]
# Freeze the correction past the last trustworthy frame. The broken tail's toe
# error is enormous, and letting it through here would both blow up the offset
# and bleed backwards through the smoothing into the good frames.
corr[STAB_B:] = corr[STAB_B - 1]
# light smoothing only: enough to keep per-frame toe noise out of the root,
# not enough to reintroduce the slide or freeze balance corrections
corr = gauss_smooth(corr, sigma=2.0)
corr[:, 1] = 0.0
before_xz = toe[CORE_A:STAB_B][:, [0, 2]]
trans[:, HIPS] += corr

W, P, Q = fk_from(rot, trans)
after_xz = P[CORE_A:STAB_B, ANCH][:, [0, 2]]
print(f"  root correction: max {np.linalg.norm(corr,axis=-1).max()*100:.2f} cm, "
      f"max per-frame {np.linalg.norm(np.diff(corr,axis=0),axis=-1).max()*100:.3f} cm")
print(f"  planted-toe XZ range f{CORE_A}-{STAB_B}: "
      f"{np.linalg.norm(before_xz.max(0)-before_xz.min(0))*100:.2f} cm -> "
      f"{np.linalg.norm(after_xz.max(0)-after_xz.min(0))*100:.2f} cm")
hb = A.fk()[1][CORE_A:STAB_B, HIPS][:, [0, 2]]
ha = P[CORE_A:STAB_B, HIPS][:, [0, 2]]
print(f"  hips XZ range          f{CORE_A}-{STAB_B}: "
      f"{np.linalg.norm(hb.max(0)-hb.min(0))*100:.2f} cm -> "
      f"{np.linalg.norm(ha.max(0)-ha.min(0))*100:.2f} cm (weight shift preserved)")

# ======================================================================= STAGE 4
print("\n[stage 4] ending reconstruction f351-424")
# The capture's own descent (f10 -> f140) is exactly "stable crouch -> seated".
# Played backwards it is a rise built from this performer's real body mechanics,
# proportions and weight, and it lands on f10 - measured to be the best recovery
# pose in the clip (hips highest at 0.571, both toes grounded, hands clear of the
# thigh, near-static). Ending on the opening pose also lets the asset blend back
# into idle.
REBUILD = np.arange(LAST_GOOD + 1, F)
n_out = len(REBUILD)
# Span chosen so the rebuilt segment plays at ~1:1 with the capture. Reversing a
# longer span meant compressing it ~2.6x, which multiplied the source's own fast
# knee extension into a 34 deg/frame snap - faster than anything in the real
# take. Near-unity playback keeps limb speeds inside the captured envelope.
SRC_HI, SRC_LO = 92, 12
# A pure smootherstep warp peaks at 1.875x its mean rate; mixing in a linear
# term flattens that peak while keeping an eased release and settle.
_u = np.linspace(0.0, 1.0, n_out)
s = 0.70 * _u + 0.30 * smootherstep(_u)
src_t = SRC_HI - s * (SRC_HI - SRC_LO)
i0 = np.clip(np.floor(src_t).astype(int), 0, F - 2)
frac = src_t - i0

BLEND_IN = 30
decay = 1.0 - smootherstep(np.clip(np.arange(n_out) / BLEND_IN, 0, 1))
anchor_pose = rot[LAST_GOOD].copy()
anchor_tr = trans[LAST_GOOD, HIPS].copy()

UPPER = ("Shoulder", "Arm", "ForeArm", "Hand")     # incl. fingers via "Hand*"
IDENT = np.array([0.0, 0.0, 0.0, 1.0])
# Arms get the gross path replaced. Reversing the descent works for the body,
# but the performer braced with a hand while sitting down, and that reversed
# into a flail. Arms therefore follow a monotonic release from the treatment
# pose to the recovery pose, re-injected with the real high-frequency detail so
# the result is not a dead robotic interpolation.
u = np.linspace(0.0, 1.0, n_out)
arm_ease = smootherstep(np.clip((u - 0.08) / 0.92, 0, 1))   # slight follow-through
detail_gain = 0.35 * smootherstep(np.clip(np.arange(n_out) / 18.0, 0, 1))

for i in sorted(A.animated["rotation"]):
    q_src = slerp(rot[i0, i], rot[i0 + 1, i], frac)
    delta = qmul(anchor_pose[i], qconj(q_src[0]))
    dq = slerp(np.tile(IDENT, (n_out, 1)), np.tile(delta, (n_out, 1)), decay)
    q_path = qnorm(qmul(dq, q_src))

    if any(t in nm[i] for t in UPPER):
        q_tgt = q_path[-1]
        q_base = slerp(np.tile(anchor_pose[i], (n_out, 1)),
                       np.tile(q_tgt, (n_out, 1)), arm_ease)
        q_sm = smooth_quat_track(q_path, half=12, degree=2, robust=False)
        det = qmul(qconj(q_sm), q_path)                  # high-frequency residual
        det = qexp(qlog(det) * detail_gain[:, None])
        rot[REBUILD, i] = qnorm(qmul(q_base, det))
    else:
        rot[REBUILD, i] = q_path

tr_src = (trans[i0, HIPS] * (1 - frac[:, None]) + trans[i0 + 1, HIPS] * frac[:, None])
offset = anchor_tr - tr_src[0]
off = np.tile(offset, (n_out, 1))
# Horizontal offset persists (the character rises where they are), but the
# VERTICAL offset must decay to zero or the whole rebuilt segment floats: it
# would carry the seam's hip height for the rest of the clip and lift the feet
# clean off the floor.
Y_BLEND = 34
off[:, 1] = offset[1] * (1.0 - smootherstep(np.clip(np.arange(n_out) / Y_BLEND, 0, 1)))
trans[REBUILD, HIPS] = tr_src + off

# Settle into the hold without going completely inert - a dead-frozen tail reads
# as a broken clip, and the asset still needs a little life to blend out of.
TAIL = 13
for k in range(TAIL):
    f = F - TAIL + k
    w = smootherstep(k / (TAIL - 1)) * 0.55
    for i in sorted(A.animated["rotation"]):
        rot[f, i] = slerp(rot[f, i], rot[F - 1, i], w)
    trans[f, HIPS] = trans[f, HIPS] * (1 - w) + trans[F - 1, HIPS] * w

seam = slice(LAST_GOOD - 10, LAST_GOOD + 22)
for i in sorted(A.animated["rotation"]):
    rot[seam, i] = smooth_quat_track(rot[seam, i], half=3, degree=2, robust=False)
trans[seam, HIPS] = smooth_vec_track(trans[seam, HIPS], half=3, degree=2)
print(f"  rebuilt {n_out} frames ({n_out/A.fps:.2f}s) from descent span f{SRC_LO}-{SRC_HI}")

# ======================================================================= STAGE 5
print("\n[stage 5] contact planting IK")
W, P, Q = fk_from(rot, trans)
REST = A.skel.rest_t
LIMB = {"R": (I["RightUpLeg"], I["RightLeg"], I["RightFoot"], I["RightToeBase"], "RightToeBase"),
        "L": (I["LeftUpLeg"], I["LeftLeg"], I["LeftFoot"], I["LeftToeBase"], "LeftFoot")}

def detect_planted(joint_name, min_run=20):
    """Strict planting test, valid only AFTER the root slide has been removed.

    With the global skate gone, horizontal speed is meaningful again, so it can
    be used to separate a truly stationary support foot from one that is being
    repositioned.
    """
    p = P[:, I[joint_name]]
    v = np.linalg.norm(np.gradient(p, dt, axis=0), axis=-1)
    vy = np.abs(np.gradient(p[:, 1], dt))
    floor = np.percentile(p[:, 1], 10)
    c = (p[:, 1] < floor + 0.05) & (vy < 0.18) & (v < 0.30)
    runs, s = [], None
    for f in range(F):
        if c[f] and s is None:
            s = f
        elif not c[f] and s is not None:
            if f - s >= min_run:
                runs.append((s, f))
            s = None
    if s is not None and F - s >= min_run:
        runs.append((s, F))
    return runs


MAX_PIN = 0.06   # never yank an ankle further than this; beyond it the contact
                 # model is wrong and forcing it would deform the pose

for side, (up, leg, foot, toe_j, cname) in LIMB.items():
    l1 = float(np.linalg.norm(REST[leg]))
    l2 = float(np.linalg.norm(REST[foot]))
    # Only sustained contacts get pinned; brief dips are the injured leg being
    # worked on, and hard-locking those would kill real treatment motion.
    runs = [r for r in detect_planted(cname) if (r[1] - r[0]) >= 20]
    moved = []
    for (a, b) in runs:
        seg = np.arange(a, b)
        ank = P[seg, foot]
        ref = np.median(ank, axis=0)
        tgt = ank.copy()
        e = min(9, max(2, len(seg) // 4))
        w = np.ones(len(seg))
        w[:e] = smootherstep(np.arange(e) / e)
        w[-e:] = smootherstep(np.arange(e - 1, -1, -1) / e)
        for k in (0, 2):
            tgt[:, k] = ank[:, k] * (1 - w) + ref[k] * w
        # clamp the residual correction
        d = tgt - ank
        dn = np.linalg.norm(d, axis=-1, keepdims=True)
        d = np.where(dn > MAX_PIN, d * (MAX_PIN / np.maximum(dn, 1e-9)), d)
        tgt = ank + d

        hip, knee = P[seg, up], P[seg, leg]
        mid_new, eff_new = two_bone_ik(hip, knee, ank, tgt, knee, l1, l2)
        moved.append(float(np.linalg.norm(eff_new - ank, axis=-1).mean()))

        q1 = aim_rotation(knee - hip, mid_new - hip)
        Qup_new = qmul(q1, Q[seg, up])
        rot[seg, up] = qmul(qconj(Q[seg, par[up]]), Qup_new)

        R1 = quat_to_mat(q1)
        d_old2 = np.einsum('fij,fj->fi', R1, P[seg, foot] - knee)
        q2 = aim_rotation(d_old2, eff_new - mid_new)
        Qleg_new = qmul(q2, qmul(q1, Q[seg, leg]))
        rot[seg, leg] = qmul(qconj(Qup_new), Qleg_new)

        # Sole stays flat AND stops shimmering: damp the planted foot's world
        # orientation, otherwise the ankle is pinned but the toe still chatters.
        Qf = Q[seg, foot]
        Qf_use = slerp(smooth_quat_track(Qf, half=5, degree=0, robust=False), Qf,
                       np.full(len(seg), 0.32))

        # Pin the actual contact point. Smoothing the leg in LOCAL space moves
        # the toe in WORLD space even when the capture had it perfectly still,
        # so aim the foot to hold the toe on its reference instead of trusting
        # the ankle pin to carry through the chain.
        ankle_new = eff_new
        toe_off = P[seg, toe_j] - P[seg, foot]
        toe_cur = ankle_new + np.einsum(
            'fij,fj->fi', quat_to_mat(qmul(Qf_use, qconj(Q[seg, foot]))), toe_off)
        toe_ref = np.median(P[seg, toe_j], axis=0)
        toe_tgt = toe_cur.copy()
        for k in (0, 2):
            toe_tgt[:, k] = toe_cur[:, k] * (1 - w) + toe_ref[k] * w
        d = toe_tgt - ankle_new
        dn = np.linalg.norm(d, axis=-1, keepdims=True)
        cn_ = np.linalg.norm(toe_cur - ankle_new, axis=-1, keepdims=True)
        # only re-aim; the toe stays at its rigid distance from the ankle
        toe_tgt = ankle_new + d / np.maximum(dn, 1e-9) * cn_
        q3 = aim_rotation(toe_cur - ankle_new, toe_tgt - ankle_new)
        Qf_final = qmul(q3, Qf_use)
        rot[seg, foot] = qmul(qconj(Qleg_new), Qf_final)
    print(f"  {side} leg: {len(runs)} interval(s), mean ankle correction "
          f"{np.mean(moved)*100 if moved else 0:.2f} cm")

# ======================================================================= STAGE 6
print("\n[stage 6] floor clamp + normalisation")
def dilate(x, rad):
    """Max-filter, so smoothing the lift afterwards cannot undershoot the dip."""
    out = x.copy()
    for k in range(1, rad + 1):
        out = np.maximum(out, np.concatenate([x[k:], np.repeat(x[-1:], k)]))
        out = np.maximum(out, np.concatenate([np.repeat(x[:1], k), x[:-k]]))
    return out


W, P, Q = fk_from(rot, trans)
pen0 = float(np.minimum(P[:, :, 1].min(axis=1), 0.0).min())
total_lift = np.zeros(F)
for _ in range(4):
    pen = np.minimum(P[:, :, 1].min(axis=1), 0.0)
    if pen.min() >= -0.003:
        break
    need = dilate(np.maximum(-pen, 0.0), 4)
    lift = gauss_smooth(need[:, None], sigma=2.5)[:, 0]
    trans[:, HIPS, 1] += lift
    total_lift += lift
    W, P, Q = fk_from(rot, trans)
print(f"  penetration {pen0*100:.2f} cm -> {min(P[:,:,1].min(),0)*100:.2f} cm "
      f"(max lift {total_lift.max()*100:.2f} cm)")

for i in sorted(A.animated["rotation"]):
    rot[:, i] = qnorm(rot[:, i])
assert np.all(np.isfinite(rot)) and np.all(np.isfinite(trans))
print(f"  quat norm max deviation: {np.abs(np.linalg.norm(rot,axis=-1)-1).max():.3e}")

write_animation(SRC, DST,
                {i: rot[:, i] for i in sorted(A.animated["rotation"])},
                {i: trans[:, i] for i in sorted(A.animated["translation"])},
                anim_index=0)
print("[done]")
