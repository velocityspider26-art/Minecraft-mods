"""C.O.R.E. left-thigh CAT tourniquet - animation cleanup / reconstruction pass.

Stages (in execution order)
  1   de-chatter       bone-group weighted robust local-polynomial manifold
                       filter, plus damping of the performer's overhead bracing
                       reach during the descent
  2   contact model    height + vertical-velocity planting detection (a skating
                       foot slides fast while staying planted, so total speed is
                       the wrong test)
  3   root stabilise   anchored to the toe's provably-planted window; removes the
                       ~40 cm secular slide while keeping weight shifts
  4   ending rebuild   f351-424 reconstructed from the performer's own descent
  3b  posture retarget seated cross-legged -> crouched half-kneel with the left
                       leg extended, hands re-solved onto the thigh. Runs AFTER
                       the ending rebuild so the rebuilt tail inherits the kneel
                       instead of fighting it.
  5   contact IK       per-limb planting with eased constraint blending
  6   ground + seams   local foot-floor correction, then a floor clamp
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


def stable_pole(Pp, root_j, mid_j, end_j, sigma=4.0, scale=0.3):
    """Temporally-smoothed bend-plane reference for a 2-bone chain.

    Using the raw mid-joint as the pole fails as a limb approaches straight: the
    component of (mid - root) perpendicular to the limb axis shrinks toward zero
    and its DIRECTION becomes unstable, so the solver flips the bend plane
    between frames and the joint visibly pops.
    """
    axis = Pp[:, end_j] - Pp[:, root_j]
    u = axis / np.maximum(np.linalg.norm(axis, axis=-1, keepdims=True), 1e-9)
    v = Pp[:, mid_j] - Pp[:, root_j]
    perp = v - u * np.sum(v * u, axis=-1, keepdims=True)
    perp = gauss_smooth(perp, sigma=sigma)
    pn = np.linalg.norm(perp, axis=-1, keepdims=True)
    perp = np.where(pn < 1e-6, np.array([0.0, 1.0, 0.0]), perp / np.maximum(pn, 1e-9))
    return Pp[:, root_j] + perp * scale


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

# ---- descent reach damping ------------------------------------------------
# While lowering, the performer threw the left arm up to head height (hand Y
# 1.31 m at f80) to brace against the floor. Against the kneeling body that
# excursion reads as a wave rather than a reach for the tourniquet, so the arm
# is flattened onto a calmer arc across the descent. Timing is untouched - only
# the amplitude of the swing changes.
DESC_A, DESC_B, DESC_EDGE = 52, 152, 22
_dw = np.zeros(DESC_B - DESC_A)
for k in range(len(_dw)):
    a_ = smootherstep(min(1.0, k / DESC_EDGE))
    b_ = smootherstep(min(1.0, (len(_dw) - 1 - k) / DESC_EDGE))
    _dw[k] = min(a_, b_) * 0.94
_before_reach = None
for i in [I["LeftArm"], I["LeftForeArm"], I["LeftHand"]]:
    seg = rot[DESC_A:DESC_B, i]
    sm = smooth_quat_track(seg, half=22, degree=0, robust=False)
    rot[DESC_A:DESC_B, i] = slerp(seg, sm, _dw)
_p = fk_from(rot, trans)[1]
print(f"  descent reach damped: left hand peak height over f{DESC_A}-{DESC_B} "
      f"{A.fk()[1][DESC_A:DESC_B, I['LeftHand'], 1].max():.3f} -> "
      f"{_p[DESC_A:DESC_B, I['LeftHand'], 1].max():.3f} m")

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

# ===================================================================== STAGE 3B
print("\n[stage 3b] posture retarget: seated -> crouched half-kneel, left leg extended")
# The capture's treatment section is already half-kneeling in its joint angles
# (left knee ~159 deg, foot 0.80 m forward) but the pelvis rides at 0.358 m and
# the right leg is folded CROSS-LEGGED - knee ~24 cm lateral of the hip, foot
# tucked across the midline. So the support leg is not adjusted, it is re-posed.
#
# It is driven by a continuous world-space TRAJECTORY rather than by blending
# between the captured pose and a target pose. Every blend formulation tried
# before this one either snapped the leg (interpolating rotations passes through
# poses that are valid but wrong) or drove it through the floor (interpolating
# positions lets the ankle-to-toe spacing collapse). A trajectory has neither
# failure mode: it starts exactly on the captured pose, so there is nothing to
# blend, and it carries its own floor clearance.
RT_A, RT_B = 96, F
RT_RISE = 74
E0, E1 = RT_A, RT_A + RT_RISE      # leg repositions over this window
KNEE_GROUND = 0.052                # knee radius - resting height on the floor
ANKLE_H = 0.072                    # ankle height with the shin laid along the floor
LEG_EXT = 0.985                    # left leg extension as a fraction of full reach
HIP_LIFT = 0.085                   # pelvis lift onto the knee (bounded by femur length)
ARC_KNEE, ARC_TOE = 0.09, 0.13     # lift over the swing, so the leg clears the floor
MIN_KNEE_DEG = 42.0                # headroom: the post-swing smoothing re-folds it a little

W, P, Q = fk_from(rot, trans)
RUP, RLEG, RFOOT, RTOE = I["RightUpLeg"], I["RightLeg"], I["RightFoot"], I["RightToeBase"]
LUP, LLEG, LFOOT = I["LeftUpLeg"], I["LeftLeg"], I["LeftFoot"]
ARMS = {"L": (I["LeftArm"], I["LeftForeArm"], I["LeftHand"]),
        "R": (I["RightArm"], I["RightForeArm"], I["RightHand"])}

# Record the hands relative to the LEFT THIGH so the tourniquet interaction can
# be restored bone-for-bone after the base pose moves underneath it.
thigh_R = quat_to_mat(Q[:, LUP])
hand_rel = {}
for s, (_, _, hnd) in ARMS.items():
    off = np.einsum('fji,fj->fi', thigh_R, P[:, hnd] - P[:, LUP])
    hand_rel[s] = (off, qmul(qconj(Q[:, LUP]), Q[:, hnd]))

wmask = np.array([smootherstep(min(1.0, max(0.0, (f - E0) / (E1 - E0))))
                  for f in range(F)])

# Body facing = where the extended left leg points, smoothed so the rebuilt
# support leg cannot inherit per-frame noise from the working leg.
fwd = P[:, LFOOT] - P[:, LUP]
fwd[:, 1] = 0.0
fwd = gauss_smooth(fwd, sigma=6.0)
fwd /= np.maximum(np.linalg.norm(fwd, axis=-1, keepdims=True), 1e-9)

l_thigh = float(np.linalg.norm(A.skel.rest_t[RLEG]))
l_shin = float(np.linalg.norm(A.skel.rest_t[RFOOT]))
l_foot = float(np.linalg.norm(A.skel.rest_t[RTOE]))
lu_thigh = float(np.linalg.norm(A.skel.rest_t[LLEG]))
lu_shin = float(np.linalg.norm(A.skel.rest_t[LFOOT]))
ank_keep = P[:, LFOOT].copy()

trans[:, HIPS, 1] += HIP_LIFT * wmask
W, P, Q = fk_from(rot, trans)


def _place(origin, direction, drop_to, length):
    """Point exactly `length` from origin, aimed along `direction` but landing at
    height `drop_to` - keeps the bone rigid while resting on the floor."""
    d = np.asarray(direction, dtype=np.float64)
    dy = float(np.clip((drop_to - origin[1]) / length, -1.0, 1.0))
    h = np.array([d[0], 0.0, d[2]])
    hn = float(np.linalg.norm(h))
    h = h / hn if hn > 1e-9 else np.array([0.0, 0.0, 1.0])
    v = h * float(np.sqrt(max(1.0 - dy * dy, 0.0)))
    v[1] = dy
    return origin + v * length


# Where the kneel lands: derived from the settled pelvis, then held in world
# space. These are floor contacts and belong to the floor, not to the pelvis.
CORE_S = slice(180, 326)
back_ref = -np.median(fwd[CORE_S], axis=0)
back_ref /= np.linalg.norm(back_ref)
hip_ref = np.median(P[CORE_S, RUP], axis=0)
drop_r = max(hip_ref[1] - KNEE_GROUND, 0.05)
horiz_r = float(np.sqrt(max(l_thigh ** 2 - drop_r ** 2, 1e-4)))
knee_ref = hip_ref + back_ref * horiz_r + np.array([0.0, -drop_r, 0.0])
ank_ref = _place(knee_ref, back_ref, ANKLE_H, l_shin)
toe_ref = _place(ank_ref, back_ref, 0.0, l_foot)

# The trajectory itself: from wherever the capture has the leg at E0, on an arc,
# to the planted kneel. Continuous by construction at both ends.
knee_start, toe_start = P[E0, RLEG].copy(), P[E0, RTOE].copy()
ank_start = P[E0, RFOOT].copy()
knee_traj = np.zeros((F, 3))
ank_traj = np.zeros((F, 3))
toe_traj = np.zeros((F, 3))
for f in range(F):
    if f < E0:
        knee_traj[f], ank_traj[f], toe_traj[f] = P[f, RLEG], P[f, RFOOT], P[f, RTOE]
        continue
    t = min(1.0, (f - E0) / float(E1 - E0))
    u = smootherstep(t)
    sw = float(np.sin(np.pi * t))
    knee_traj[f] = knee_start * (1 - u) + knee_ref * u
    ank_traj[f] = ank_start * (1 - u) + ank_ref * u
    toe_traj[f] = toe_start * (1 - u) + toe_ref * u
    knee_traj[f, 1] = max(knee_traj[f, 1] + ARC_KNEE * sw, KNEE_GROUND)
    ank_traj[f, 1] = max(ank_traj[f, 1] + ARC_TOE * sw, ANKLE_H)
    toe_traj[f, 1] = max(toe_traj[f, 1] + ARC_TOE * sw, 0.0)
knee_traj = gauss_smooth(knee_traj, sigma=1.5)
ank_traj = gauss_smooth(ank_traj, sigma=1.5)
toe_traj = gauss_smooth(toe_traj, sigma=1.5)

lpole = stable_pole(P, LUP, LLEG, LFOOT)

# Left leg goal: near-straight extension with the heel just off the floor.
lank_goal = np.zeros((F, 3))
for f in range(F):
    lhip_f = P[f, LUP]
    dv = ank_keep[f] - lhip_f
    dv[1] = 0.0
    hn = float(np.linalg.norm(dv))
    dv = dv / hn if hn > 1e-6 else np.array([0.0, 0.0, 1.0])
    span = (lu_thigh + lu_shin) * LEG_EXT
    drop = lhip_f[1] - 0.088
    lank_goal[f] = lhip_f + dv * float(np.sqrt(max(span ** 2 - drop ** 2, 0.04)))
    lank_goal[f, 1] = 0.088
lank_goal = gauss_smooth(lank_goal, sigma=3.0)

UPV = np.array([0.0, 0.35, 0.0])
for f in range(E0, F):
    # ---- right leg: follow the trajectory, no blending anywhere ----------
    hip = P[f, RUP]
    d = knee_traj[f] - hip
    knee_pos = hip + d / max(float(np.linalg.norm(d)), 1e-9) * l_thigh
    q1 = aim_rotation(P[f, RLEG] - hip, knee_pos - hip)
    Qup = qmul(q1, Q[f, RUP])

    # Shin and foot are aimed explicitly rather than solved as a 2-bone chain to
    # the toe. Laid out flat the knee-to-toe span is 98% of shin+foot, and at
    # that extension the chain's bend plane is ill-conditioned - the solver flips
    # it between frames and the leg snaps ~60 deg. Aiming each bone in turn has
    # no bend plane to flip, and keeps both bones exactly rigid.
    # Anatomical knee limit. Lerping the knee and ankle goals independently lets
    # the shin swing back toward the hip mid-reposition and folds the joint to
    # ~8 deg - well past the ~30 deg a real knee bottoms out at. Push the ankle
    # goal back out to the limit when that happens.
    ank_t = ank_traj[f].copy()
    _vt = hip - knee_pos
    _vt /= max(float(np.linalg.norm(_vt)), 1e-9)
    _vs = ank_t - knee_pos
    _ls = max(float(np.linalg.norm(_vs)), 1e-9)
    _vs /= _ls
    _ka = float(np.degrees(np.arccos(np.clip(np.dot(_vt, _vs), -1.0, 1.0))))
    if _ka < MIN_KNEE_DEG:
        _ax = np.cross(_vt, _vs)
        _an = float(np.linalg.norm(_ax))
        if _an < 1e-6:
            _ax = np.cross(_vt, np.array([0.0, 1.0, 0.0]))
            _an = float(np.linalg.norm(_ax))
        _ax = _ax / max(_an, 1e-9)
        _th = np.deg2rad(MIN_KNEE_DEG)
        _c, _sn = np.cos(_th), np.sin(_th)
        _vs = (_vt * _c + np.cross(_ax, _vt) * _sn +
               _ax * float(np.dot(_ax, _vt)) * (1 - _c))
        ank_t = knee_pos + _vs * _ls

    d_old = quat_to_mat(q1) @ (P[f, RFOOT] - P[f, RLEG])
    q2 = aim_rotation(d_old, ank_t - knee_pos)
    Qlg = qmul(q2, qmul(q1, Q[f, RLEG]))
    ank_pos = knee_pos + (ank_t - knee_pos) / max(
        float(np.linalg.norm(ank_t - knee_pos)), 1e-9) * l_shin
    d_old2 = quat_to_mat(qmul(q2, q1)) @ (P[f, RTOE] - P[f, RFOOT])
    q3 = aim_rotation(d_old2, toe_traj[f] - ank_pos)
    Qft = qmul(q3, qmul(q2, qmul(q1, Q[f, RFOOT])))

    # Ease the solve in over the first few frames: the trajectory starts on the
    # captured position, but the bone ORIENTATIONS are rederived, so a hard
    # switch still shows as a pop at the entry frame.
    ein = smootherstep(min(1.0, (f - E0) / 12.0))
    rot[f, RUP] = slerp(rot[f, RUP], qmul(qconj(Q[f, par[RUP]]), Qup), ein)
    rot[f, RLEG] = slerp(rot[f, RLEG], qmul(qconj(Qup), Qlg), ein)
    rot[f, RFOOT] = slerp(rot[f, RFOOT], qmul(qconj(Qlg), Qft), ein)

    # ---- left leg: eased out to a near-straight extension ----------------
    w = wmask[f]
    if w <= 1e-4:
        continue
    lhip = P[f, LUP]
    lank = P[f, LFOOT] * (1 - w) + lank_goal[f] * w
    mid_new, eff_new = two_bone_ik(lhip[None], None, None, lank[None],
                                   lpole[f][None], lu_thigh, lu_shin)
    qa = aim_rotation(P[f, LLEG] - lhip, mid_new[0] - lhip)
    Qlu = qmul(qa, Q[f, LUP])
    rot[f, LUP] = qmul(qconj(Q[f, par[LUP]]), Qlu)
    dl = quat_to_mat(qa) @ (P[f, LFOOT] - P[f, LLEG])
    qb = aim_rotation(dl, eff_new[0] - mid_new[0])
    Qll = qmul(qb, qmul(qa, Q[f, LLEG]))
    rot[f, LLEG] = qmul(qconj(Qlu), Qll)
    rot[f, LFOOT] = qmul(qconj(Qll), Q[f, LFOOT])

# The support leg is airborne through the swing (the arc lifts it clear), so a
# light smoothing there cannot break a contact - and it takes the last of the
# solver ripple out of the reposition. Once the leg is planted it is left exact.
_sa, _sb = E0, max(E0 + 1, E1 - 6)
for _i in (RUP, RLEG, RFOOT):
    _seg = rot[_sa:_sb, _i]
    rot[_sa:_sb, _i] = slerp(
        smooth_quat_track(_seg, half=5, degree=1, robust=False), _seg,
        np.full(len(_seg), 0.42))

# Restore the hands onto the thigh: the base pose moved under them, so re-solve
# each arm to the transform it originally held relative to the left thigh.
W, P, Q = fk_from(rot, trans)
thigh_R2 = quat_to_mat(Q[:, LUP])
arm_fix = []
for s, (sh, el, hnd) in ARMS.items():
    la = float(np.linalg.norm(A.skel.rest_t[el]))
    lb = float(np.linalg.norm(A.skel.rest_t[hnd]))
    apole = stable_pole(P, sh, el, hnd)
    # Smoothed hand goals, for the same reason as the leg goals above.
    want_all = P[:, LUP] + np.einsum('fij,fj->fi', thigh_R2, hand_rel[s][0])
    want_all = gauss_smooth(want_all, sigma=2.0)
    for f in range(RT_A, min(RT_B, F)):
        w = wmask[f]
        if w <= 1e-4:
            continue
        tgt = P[f, hnd] * (1 - w) + want_all[f] * w
        arm_fix.append(float(np.linalg.norm(tgt - P[f, hnd])))
        m2, e2 = two_bone_ik(P[f, sh][None], None, None, tgt[None], apole[f][None], la, lb)
        qa = aim_rotation(P[f, el] - P[f, sh], m2[0] - P[f, sh])
        Qs = qmul(qa, Q[f, sh])
        rot[f, sh] = qmul(qconj(Q[f, par[sh]]), Qs)
        dl = quat_to_mat(qa) @ (P[f, hnd] - P[f, el])
        qb = aim_rotation(dl, e2[0] - m2[0])
        Qe = qmul(qb, qmul(qa, Q[f, el]))
        rot[f, el] = qmul(qconj(Qs), Qe)
        want_q = qmul(Q[f, LUP], hand_rel[s][1][f])
        rot[f, hnd] = qmul(qconj(Qe), slerp(Q[f, hnd], want_q, w))

# An IK solve is a per-frame operation and carries no temporal guarantee, so the
# retargeted chains get one more high-frequency attenuation pass over the span
# that was touched. Gross motion is untouched; only solver-introduced ripple goes.
RT_SM = slice(max(0, RT_A - 6), min(F, RT_B + 6))
# The support leg is excluded: it is driven onto exact world contacts, and
# averaging it afterwards pulls it straight back off them and through the floor.
for i in [LUP, LLEG, LFOOT,
          I["LeftArm"], I["LeftForeArm"], I["LeftHand"],
          I["RightArm"], I["RightForeArm"], I["RightHand"]]:
    seg_q = rot[RT_SM, i]
    ref = smooth_quat_track(seg_q, half=4, degree=0, robust=False)
    rot[RT_SM, i] = slerp(ref, seg_q, np.full(len(seg_q), 0.55))
# The support leg is deliberately NOT smoothed here: it sits on an exact
# trajectory, and averaging pulls it straight back off its contacts.

W, P, Q = fk_from(rot, trans)
print(f"  pelvis lift {HIP_LIFT*100:.1f} cm applied over f{RT_A}-{RT_B} "
      f"(eased in over {RT_RISE} frames)")
print(f"  hips Y     f170-350: {np.median(A.fk()[1][170:351, HIPS,1]):.3f} -> "
      f"{np.median(P[170:351, HIPS,1]):.3f} m")
print(f"  right knee f170-350: {np.median(A.fk()[1][170:351, RLEG,1])*100:.1f} -> "
      f"{np.median(P[170:351, RLEG,1])*100:.1f} cm above floor")


def _ang(pp, a, b, c, rng):
    v1 = pp[rng, a] - pp[rng, b]
    v2 = pp[rng, c] - pp[rng, b]
    cs = np.sum(v1 * v2, -1) / (np.linalg.norm(v1, axis=-1) * np.linalg.norm(v2, axis=-1) + 1e-12)
    return float(np.median(np.rad2deg(np.arccos(np.clip(cs, -1, 1)))))


rng = slice(170, 351)
print(f"  left knee  f170-350: {_ang(A.fk()[1], LUP, LLEG, LFOOT, rng):.1f} -> "
      f"{_ang(P, LUP, LLEG, LFOOT, rng):.1f} deg (extended)")
print(f"  mean hand re-solve to hold thigh contact: "
      f"{np.mean(arm_fix)*100 if arm_fix else 0:.2f} cm")

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
    if side == "R":
        # The posture retarget already pins this leg to fixed world contacts.
        # A second contact solver on top of that only fights it - it re-detects
        # "contacts" from the retargeted motion and drags the foot off the pins.
        print("  R leg: pinned by the posture retarget, contact IK skipped")
        continue
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

# Local foot-floor correction, applied BEFORE any global lift. A toe that dips
# through the floor is a foot-orientation problem, so fix it by rotating the
# foot about its own ankle. Lifting the whole root instead would hoist the
# pelvis by the depth of the worst single dip and float the entire character.
FOOT_CHAINS = [(I["RightFoot"], I["RightToeBase"], I["RightLeg"]),
               (I["LeftFoot"], I["LeftToeBase"], I["LeftLeg"])]
_foot_before = float(min(min(P[:, t, 1].min() for _, t, _ in FOOT_CHAINS), 0.0))
for _ in range(3):
    fixed_any = False
    for foot_j, toe_j, leg_j in FOOT_CHAINS:
        hits = np.where((P[:, toe_j, 1] < -0.001) & (P[:, foot_j, 1] > 0.005))[0]
        if not len(hits):
            continue
        fixed_any = True
        for f in hits:
            ank, toe = P[f, foot_j], P[f, toe_j]
            v = toe - ank
            L = float(np.linalg.norm(v))
            if L < 1e-6:
                continue
            dy = float(np.clip(-ank[1] / L, -1.0, 1.0))
            h = v.copy()
            h[1] = 0.0
            hn = float(np.linalg.norm(h))
            h = h / hn if hn > 1e-9 else np.array([0.0, 0.0, 1.0])
            tgt = h * float(np.sqrt(max(1.0 - dy * dy, 0.0)))
            tgt[1] = dy
            qf = aim_rotation(v, tgt * L)
            rot[f, foot_j] = qmul(qconj(Q[f, leg_j]), qmul(qf, Q[f, foot_j]))
    W, P, Q = fk_from(rot, trans)
    if not fixed_any:
        break
# one gentle pass so the corrected frames blend with their neighbours
for foot_j, _, _ in FOOT_CHAINS:
    sm = smooth_quat_track(rot[:, foot_j], half=2, degree=0, robust=False)
    rot[:, foot_j] = slerp(sm, rot[:, foot_j], np.full(F, 0.85))
W, P, Q = fk_from(rot, trans)
print(f"  foot-floor correction: worst toe {_foot_before*100:.2f} cm -> "
      f"{min(min(P[:, t, 1].min() for _, t, _ in FOOT_CHAINS), 0.0)*100:.2f} cm")
_pre = np.minimum(P[:, :, 1].min(axis=1), 0.0)
_wf = np.argsort(_pre)[:6]
print("  pre-clamp worst frames: " +
      ", ".join(f"f{int(f)}={_pre[f]*100:.1f}cm({nm[int(P[f,:,1].argmin())]})" for f in _wf))
print("  seam trace f344-376 (hipY / RtoeY / RankY / RkneeY, cm):")
for _f in range(344, 377, 4):
    print(f"    f{_f}: hip {P[_f,HIPS,1]*100:6.1f}  Rtoe {P[_f,I['RightToeBase'],1]*100:7.1f}"
          f"  Rank {P[_f,I['RightFoot'],1]*100:6.1f}  Rknee {P[_f,I['RightLeg'],1]*100:6.1f}")
pen0 = float(np.minimum(P[:, :, 1].min(axis=1), 0.0).min())
total_lift = np.zeros(F)
for _ in range(4):
    pen = np.minimum(P[:, :, 1].min(axis=1), 0.0)
    if pen.min() >= -0.003:
        break
    # Capped: the root lift exists to absorb millimetre dips, not to answer a
    # limb passing through the floor. Uncapped it hoists the entire character.
    need = np.minimum(dilate(np.maximum(-pen, 0.0), 4), 0.05)
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
