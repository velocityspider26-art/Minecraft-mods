"""Diagnose what reads as 'cheap' in the current clip: dead joints, head aim,
absent breathing, balance."""
import sys
import numpy as np
from pygltflib import GLTF2
from anim_data import Anim, qangle_between
from gltf_io import quat_to_mat

g = GLTF2().load(sys.argv[1])
A = Anim(g, 0)
nm = A.skel.names
I = {k: i for i, k in enumerate(nm)}
W, P, Q = A.fk()
dt = A.dt
F = A.F
TREAT = slice(150, 351)

print("=" * 74)
print("PER-BONE ACTIVITY  (total local rotation travel over clip)")
print("=" * 74)
rows = []
for i in sorted(A.animated["rotation"]):
    d = np.rad2deg(qangle_between(A.rot[1:, i], A.rot[:-1, i]))
    rows.append((float(d.sum()), float(d[TREAT].mean()), nm[i]))
rows.sort()
print("  DEAD / near-static bones (these are the AAA tells):")
for tot, mean, n in rows[:14]:
    print(f"    {n:<22s} total {tot:8.1f}°   mean {mean:6.4f}°/f")
print("  most active:")
for tot, mean, n in rows[-6:]:
    print(f"    {n:<22s} total {tot:8.1f}°   mean {mean:6.4f}°/f")

print("\n" + "=" * 74)
print("HEAD AIM  (is he looking at what his hands are doing?)")
print("=" * 74)
# head forward axis: derive from the rest offset of Head->(no child), use +Y of head
hd = I["Head"]
Rh = quat_to_mat(Q[:, hd])
# In this rig the bone's local +Y points along the bone (up through the skull);
# facing is approximately local +Z.
fwd = Rh[:, :, 2]
hands = (P[:, I["LeftHand"]] + P[:, I["RightHand"]]) / 2
to_h = hands - P[:, hd]
to_h /= np.maximum(np.linalg.norm(to_h, axis=-1, keepdims=True), 1e-9)
ang = np.rad2deg(np.arccos(np.clip(np.sum(fwd * to_h, axis=-1), -1, 1)))
print(f"  angle between head facing and direction-to-hands:")
print(f"    treatment phase f150-350: mean {ang[TREAT].mean():.1f}°  "
      f"min {ang[TREAT].min():.1f}°  max {ang[TREAT].max():.1f}°")
print(f"    (0° = looking straight at the hands; >60° = looking away)")

print("\n" + "=" * 74)
print("BREATHING  (low-frequency torso oscillation)")
print("=" * 74)
for j in ["Spine", "Spine1", "Spine2"]:
    q = A.rot[TREAT, I[j]]
    d = np.rad2deg(qangle_between(q[1:], q[:-1]))
    sig = d - d.mean()
    n = len(sig)
    sp = np.abs(np.fft.rfft(sig * np.hanning(n)))
    fr = np.fft.rfftfreq(n, dt)
    band = (fr > 0.15) & (fr < 0.6)          # 0.15-0.6 Hz = 1.7-6.7 s per breath
    tot = sp[1:].sum()
    print(f"  {j:<8s} energy in breathing band: "
          f"{100*sp[band].sum()/max(tot,1e-9):5.1f}%   peak {fr[np.argmax(sp[1:])+1]:.2f} Hz   "
          f"amplitude {d.std():.4f}°/f")

print("\n" + "=" * 74)
print("BALANCE  (approx centre of mass vs support)")
print("=" * 74)
SEG = {"Hips": 0.14, "Spine": 0.10, "Spine1": 0.12, "Spine2": 0.10, "Head": 0.08,
       "LeftArm": 0.03, "LeftForeArm": 0.02, "LeftHand": 0.01,
       "RightArm": 0.03, "RightForeArm": 0.02, "RightHand": 0.01,
       "LeftUpLeg": 0.10, "LeftLeg": 0.05, "LeftFoot": 0.02,
       "RightUpLeg": 0.10, "RightLeg": 0.05, "RightFoot": 0.02}
tot_w = sum(SEG.values())
com = np.zeros((F, 3))
for k, w in SEG.items():
    com += w * P[:, I[k]]
com /= tot_w
sup = [I["RightLeg"], I["RightToeBase"], I["LeftFoot"], I["RightFoot"]]
sx = np.stack([P[:, s] for s in sup], axis=1)
lo = sx[:, :, [0, 2]].min(axis=1)
hi = sx[:, :, [0, 2]].max(axis=1)
inside = ((com[:, [0, 2]] >= lo) & (com[:, [0, 2]] <= hi)).all(axis=1)
print(f"  COM inside support footprint: {100*inside[TREAT].mean():.1f}% of treatment frames")
mx = np.maximum(lo - com[:, [0, 2]], com[:, [0, 2]] - hi).max(axis=1)
print(f"  worst COM excursion beyond support: {mx[TREAT].max()*100:.1f} cm")
print(f"  COM height range in treatment: "
      f"{com[TREAT,1].min():.3f}-{com[TREAT,1].max():.3f} m")

print("\n" + "=" * 74)
print("MOTION TEXTURE  (is it too clean / sterile?)")
print("=" * 74)
for j in ["Hips", "Spine1", "Spine2", "Neck", "Head", "LeftShoulder", "RightShoulder",
          "LeftHand", "RightHand"]:
    q = A.rot[TREAT, I[j]]
    d = np.rad2deg(qangle_between(q[1:], q[:-1]))
    print(f"  {j:<15s} mean {d.mean():7.4f}°/f   std {d.std():7.4f}   max {d.max():7.4f}")
