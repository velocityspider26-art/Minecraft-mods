"""Quantify high-frequency chatter and contact-point drift in the good region."""
import sys
import numpy as np
from pygltflib import GLTF2
from anim_data import Anim, qangle_between
np.set_printoptions(suppress=True, precision=3)

g = GLTF2().load(sys.argv[1])
A = Anim(g, 0)
nm = A.skel.names
W, P, Q = A.fk()
dt = A.dt
I = {k: i for i, k in enumerate(nm)}
GOOD = slice(0, 351)

FINGERS = ("Thumb", "Index", "Middle", "Ring", "Pinky")


def is_finger(n):
    return any(t in n for t in FINGERS)


print("=" * 78)
print("HIGH-FREQUENCY CHATTER  (2nd-difference roughness, good region f0-350)")
print("=" * 78)
print("roughness = RMS geodesic |q[t-1] - 2q[t] + q[t+1]| in deg; high => frame-to-frame chatter")
rows = []
for i in sorted(A.animated["rotation"]):
    q = A.rot[GOOD, i]
    # geodesic second difference: angle from the midpoint of neighbours
    mid = (q[:-2] + q[2:])
    mid /= np.linalg.norm(mid, axis=-1, keepdims=True)
    rough = np.rad2deg(qangle_between(q[1:-1], mid))
    d1 = np.rad2deg(qangle_between(q[1:], q[:-1]))
    rows.append((float(np.sqrt((rough ** 2).mean())), float(rough.max()),
                 float(np.median(d1)), nm[i]))
rows.sort(key=lambda r: -r[0])
print(f"{'bone':<24s} {'rough_rms':>10s} {'rough_max':>10s} {'med_step':>9s}  kind")
for rms, mx, md, n in rows:
    if rms > 0.12 or not is_finger(n):
        print(f"{n:<24s} {rms:>10.3f} {mx:>10.3f} {md:>9.3f}  {'finger' if is_finger(n) else 'BODY'}")

body_rms = np.mean([r[0] for r in rows if not is_finger(r[3])])
fing_rms = np.mean([r[0] for r in rows if is_finger(r[3])])
print(f"\nmean roughness: BODY={body_rms:.3f}°  FINGERS={fing_rms:.3f}°")

print("\n" + "=" * 78)
print("WORLD-SPACE POSITION CHATTER (2nd difference, cm) f0-350")
print("=" * 78)
KEY = ["Hips", "Spine2", "Neck", "Head", "LeftHand", "RightHand", "LeftForeArm",
       "RightForeArm", "LeftFoot", "RightFoot", "LeftToeBase", "RightToeBase",
       "LeftLeg", "RightLeg"]
print(f"{'joint':<16s} {'rms(cm)':>9s} {'max(cm)':>9s}")
for k in KEY:
    p = P[GOOD, I[k]]
    acc = p[:-2] - 2 * p[1:-1] + p[2:]
    n = np.linalg.norm(acc, axis=-1) * 100
    print(f"{k:<16s} {np.sqrt((n**2).mean()):>9.3f} {n.max():>9.3f}")

print("\n" + "=" * 78)
print("CONTACT-POINT DRIFT during stable treatment window f160-340")
print("=" * 78)
win = slice(160, 341)
for k in ["RightToeBase", "RightFoot", "LeftFoot", "LeftToeBase", "RightLeg", "Hips"]:
    p = P[win, I[k]]
    sp = np.linalg.norm(np.gradient(p, dt, axis=0), axis=-1)
    horiz = p[:, [0, 2]]
    print(f"{k:<14s} Ymean={p[:,1].mean():+.4f} Ystd={p[:,1].std()*100:>5.2f}cm "
          f"XZ_std=({horiz[:,0].std()*100:>5.2f},{horiz[:,1].std()*100:>5.2f})cm "
          f"XZ_range={np.linalg.norm(horiz.max(0)-horiz.min(0))*100:>6.2f}cm "
          f"meanspeed={sp.mean():.3f} m/s")

# net displacement of the whole body over the treatment phase
print("\n--- NET BODY DISPLACEMENT (should be ~0 for a seated, planted character) ---")
for k in ["Hips", "RightToeBase", "LeftFoot"]:
    p = P[:, I[k]]
    a = p[160:180].mean(0)
    b = p[320:340].mean(0)
    print(f"{k:<14s} f160-180 mean=({a[0]:+.3f},{a[1]:+.3f},{a[2]:+.3f})  "
          f"f320-340 mean=({b[0]:+.3f},{b[1]:+.3f},{b[2]:+.3f})  "
          f"net move={np.linalg.norm(b-a)*100:.2f} cm")

# Hips trajectory over the whole good region, decimated
print("\n--- HIPS WORLD TRAJECTORY (every 20 frames, good region) ---")
for f in range(0, 351, 20):
    p = P[f, I["Hips"]]
    print(f"  f{f:<4d} ({p[0]:+.3f}, {p[1]:+.3f}, {p[2]:+.3f})")

print("\n--- GROUND PENETRATION (lowest joint per phase) ---")
for name, a, b in [("A", 0, 40), ("B", 40, 110), ("C", 110, 160), ("D", 160, 250),
                   ("E", 250, 320), ("F", 320, 351)]:
    sub = P[a:b, :, 1]
    j = np.unravel_index(np.argmin(sub), sub.shape)
    print(f"  phase {name}: min Y={sub.min():+.4f} at f{a+j[0]} joint={nm[j[1]]}")
