"""Locate the exact corruption boundary and characterise ground contacts."""
import sys
import numpy as np
from pygltflib import GLTF2
from anim_data import Anim, ang_speed, qangle_between
np.set_printoptions(suppress=True, precision=3)

g = GLTF2().load(sys.argv[1])
A = Anim(g, 0)
nm = A.skel.names
W, P, Q = A.fk()
dt = A.dt
F = A.F
I = {k: nm.index(k) for k in nm}

# ---- global implausibility score per frame -------------------------------
# Sum of local angular speed over the body (fingers excluded: they are noisy
# by nature and would mask real body-level corruption).
body = [i for i in sorted(A.animated["rotation"])
        if not any(t in nm[i] for t in ("Hand" + "Thumb", "HandIndex", "HandMiddle",
                                        "HandRing", "HandPinky"))]
ang = np.zeros(F)
for i in body:
    ang += ang_speed(A.rot[:, i], dt)

hips = I["Hips"]
hipY = P[:, hips, 1]
hip_step = np.concatenate([[0], np.linalg.norm(np.diff(P[:, hips], axis=0), axis=-1)])

# Ground penetration: how far below y=0 does any joint go?
pen = np.minimum(P[:, :, 1].min(axis=1), 0.0)

# Limb-length sanity as an FK invariant check (should be constant).
print("=" * 78)
print("PER-FRAME CORRUPTION SCAN  (frames 330-424)")
print("=" * 78)
print(f"{'f':>4s} {'t':>6s} {'hipY':>7s} {'hipStep':>8s} {'Σang':>8s} {'minY':>7s} "
      f"{'headY':>7s} {'LftY':>6s} {'RftY':>6s}")
for f in range(330, F):
    flag = ""
    if ang[f] > 60 or hip_step[f] > 0.03 or pen[f] < -0.02:
        flag = "  <== SUSPECT"
    print(f"{f:>4d} {A.times[f]:>6.2f} {hipY[f]:>7.3f} {hip_step[f]*100:>7.2f}c "
          f"{ang[f]:>8.1f} {pen[f]:>7.3f} {P[f,I['Head'],1]:>7.3f} "
          f"{P[f,I['LeftFoot'],1]:>6.3f} {P[f,I['RightFoot'],1]:>6.3f}{flag}")

# ---- where does it first go bad, statistically? --------------------------
base = ang[100:340]
thr = np.median(base) + 4.0 * (np.median(np.abs(base - np.median(base))) * 1.4826)
bad = np.where(ang > thr)[0]
bad_late = bad[bad > 300]
print(f"\nbaseline Σang (f100-340): median={np.median(base):.1f} thr(4·MAD)={thr:.1f}")
print(f"first sustained breach after f300: {bad_late[:12] if len(bad_late) else 'none'}")

print("\n" + "=" * 78)
print("GROUND CONTACT SURVEY - mean height (m) per phase, key joints")
print("=" * 78)
JOINTS = ["Hips", "LeftFoot", "LeftToeBase", "RightFoot", "RightToeBase",
          "LeftLeg", "RightLeg", "LeftHand", "RightHand", "LeftUpLeg", "RightUpLeg"]
PHASES = [("A start", 0, 40), ("B descend", 40, 110), ("C settle", 110, 160),
          ("D strap", 160, 250), ("E windlass", 250, 320), ("F secure", 320, 356),
          ("G BROKEN", 356, F)]
hdr = f"{'joint':<14s}" + "".join(f"{p[0]:>11s}" for p in PHASES)
print(hdr)
for j in JOINTS:
    row = f"{j:<14s}"
    for _, a, b in PHASES:
        row += f"{P[a:b, I[j], 1].mean():>11.3f}"
    print(row)

print("\n--- per-phase joint SPEED (m/s, mean) ---")
print(hdr)
for j in JOINTS:
    row = f"{j:<14s}"
    sp = np.linalg.norm(np.gradient(P[:, I[j]], dt, axis=0), axis=-1)
    for _, a, b in PHASES:
        row += f"{sp[a:b].mean():>11.3f}"
    print(row)

# ---- treatment-phase stability: how much does the seated base drift? -----
print("\n" + "=" * 78)
print("SEATED BASE STABILITY  (f110-355)")
print("=" * 78)
seg = slice(110, 356)
for j in ["Hips", "LeftFoot", "RightFoot", "LeftToeBase", "RightToeBase"]:
    p = P[seg, I[j]]
    print(f"{j:<14s} mean=({p[:,0].mean():+.3f},{p[:,1].mean():+.3f},{p[:,2].mean():+.3f}) "
          f"std=({p[:,0].std()*100:.2f},{p[:,1].std()*100:.2f},{p[:,2].std()*100:.2f})cm "
          f"range={np.linalg.norm(p.max(0)-p.min(0))*100:.2f}cm "
          f"maxstep={np.linalg.norm(np.diff(p,axis=0),axis=-1).max()*100:.2f}cm")

# ---- isolated spikes inside the GOOD region only -------------------------
print("\n" + "=" * 78)
print("ISOLATED SPIKES IN GOOD REGION (f0-355), body bones, >3x local median")
print("=" * 78)
found = []
for i in sorted(A.animated["rotation"]):
    d = np.rad2deg(qangle_between(A.rot[1:, i], A.rot[:-1, i]))
    for f in range(2, 355):
        loc = np.median(np.concatenate([d[max(0, f - 7):f - 1], d[f + 2:f + 8]]))
        if d[f] > max(3.0, 4.0 * loc) and d[f] > 2.0 * max(d[f - 1], d[f + 1]):
            found.append((d[f] / max(loc, 1e-6), nm[i], f + 1, d[f], loc))
found.sort(key=lambda x: -x[0])
print(f"count={len(found)}")
for r, b, f, dv, loc in found[:30]:
    print(f"  {b:<22s} f{f:<4d} delta={dv:>6.2f}° local_median={loc:>5.2f}° ratio={r:>6.1f}x")
