"""Targeted QC: hand-thigh interaction, seam continuity, preserved windlass motion."""
import sys
import numpy as np
from pygltflib import GLTF2
from anim_data import Anim, qangle_between, qmul, qconj

SRC, DST = sys.argv[1], sys.argv[2]
gs, gd = GLTF2().load(SRC), GLTF2().load(DST)
As, Ad = Anim(gs, 0), Anim(gd, 0)
nm = As.skel.names
I = {k: i for i, k in enumerate(nm)}
Ws, Ps, Qs = As.fk()
Wd, Pd, Qd = Ad.fk()
dt = As.dt

print("=" * 78)
print("HAND <-> LEFT THIGH INTERACTION (the tourniquet contact)")
print("=" * 78)
# thigh midpoint = between LeftUpLeg and LeftLeg
for lbl, P in [("ORIGINAL", Ps), ("CLEANED ", Pd)]:
    thigh = (P[:, I["LeftUpLeg"]] + P[:, I["LeftLeg"]]) / 2
    for h in ["LeftHand", "RightHand"]:
        d = np.linalg.norm(P[:, I[h]] - thigh, axis=-1)
        seg = d[160:351]
        # jitter of the RELATIVE vector is what makes hands look detached
        rel = P[160:351, I[h]] - thigh[160:351]
        acc = np.linalg.norm(rel[:-2] - 2 * rel[1:-1] + rel[2:], axis=-1) * 100
        print(f"  {lbl} {h:<10s} dist f160-350: mean {seg.mean()*100:5.1f} cm "
              f"std {seg.std()*100:4.1f} cm   relative-motion chatter "
              f"rms {np.sqrt((acc**2).mean()):.3f} cm  max {acc.max():.3f} cm")

print("\n" + "=" * 78)
print("SEAM CONTINUITY AT f350/351 (treatment -> reconstructed recovery)")
print("=" * 78)
for lbl, A, P in [("ORIGINAL", As, Ps), ("CLEANED ", Ad, Pd)]:
    # per-frame whole-body world speed around the seam
    v = np.linalg.norm(np.gradient(P, dt, axis=0), axis=-1).mean(axis=1)
    a = np.abs(np.gradient(v, dt))
    print(f"  {lbl} mean-joint speed f345..356: " +
          " ".join(f"{v[f]:.2f}" for f in range(345, 357)))
    print(f"  {lbl} peak |accel| in f345-360: {a[345:361].max():.1f} m/s²")
# local rotation discontinuity at the seam
worst = []
for i in sorted(Ad.animated["rotation"]):
    d = np.rad2deg(qangle_between(Ad.rot[351, i], Ad.rot[350, i]))
    worst.append((float(d), nm[i]))
worst.sort(reverse=True)
print(f"  CLEANED  largest single-bone rotation step across seam: "
      f"{worst[0][0]:.3f}° ({worst[0][1]})")
print(f"  CLEANED  next four: " + ", ".join(f"{a:.3f}°({b})" for a, b in worst[1:5]))
th = np.linalg.norm(Ad.trans[351, I["Hips"]] - Ad.trans[350, I["Hips"]])
print(f"  CLEANED  root translation step across seam: {th*100:.4f} cm")

print("\n" + "=" * 78)
print("WINDLASS / TIGHTENING MOTION PRESERVED (f250-330)")
print("=" * 78)
for h in ["RightHand", "RightForeArm", "LeftHand"]:
    for lbl, A in [("ORIGINAL", As), ("CLEANED ", Ad)]:
        q = A.rot[250:331, I[h]]
        step = np.rad2deg(qangle_between(q[1:], q[:-1]))
        tot = float(step.sum())
        print(f"  {lbl} {h:<13s} total local rotation travel {tot:7.1f}°   "
              f"mean {step.mean():.3f}°/f   peak {step.max():.2f}°/f")
    print()

print("=" * 78)
print("TORSO / HEAD BEHAVIOUR")
print("=" * 78)
for j in ["Spine", "Spine1", "Spine2", "Neck", "Head"]:
    for lbl, A in [("ORIG", As), ("CLEAN", Ad)]:
        q = A.rot[:351, I[j]]
        step = np.rad2deg(qangle_between(q[1:], q[:-1]))
        print(f"  {lbl:<6s} {j:<8s} travel {step.sum():7.1f}°  peak {step.max():5.2f}°/f", end="")
    print()

print("\nHead world-position stability (f160-350, should not vibrate):")
for lbl, P in [("ORIGINAL", Ps), ("CLEANED ", Pd)]:
    p = P[160:351, I["Head"]]
    acc = np.linalg.norm(p[:-2] - 2 * p[1:-1] + p[2:], axis=-1) * 100
    print(f"  {lbl} chatter rms {np.sqrt((acc**2).mean()):.3f} cm  max {acc.max():.3f} cm")

print("\n" + "=" * 78)
print("KNEE / ELBOW SANITY (no reversals, no impossible bends)")
print("=" * 78)


def joint_angle(P, a, b, c):
    v1 = P[:, I[a]] - P[:, I[b]]
    v2 = P[:, I[c]] - P[:, I[b]]
    cs = np.sum(v1 * v2, -1) / (np.linalg.norm(v1, axis=-1) * np.linalg.norm(v2, axis=-1) + 1e-12)
    return np.rad2deg(np.arccos(np.clip(cs, -1, 1)))


for (a, b, c) in [("LeftUpLeg", "LeftLeg", "LeftFoot"), ("RightUpLeg", "RightLeg", "RightFoot"),
                  ("LeftArm", "LeftForeArm", "LeftHand"), ("RightArm", "RightForeArm", "RightHand")]:
    for lbl, P in [("ORIG", Ps), ("CLEAN", Pd)]:
        ang = joint_angle(P, a, b, c)
        d = np.abs(np.diff(ang))
        print(f"  {lbl:<6s} {b:<13s} range [{ang.min():5.1f},{ang.max():5.1f}]°  "
              f"max change {d.max():5.2f}°/f", end="")
    print()
