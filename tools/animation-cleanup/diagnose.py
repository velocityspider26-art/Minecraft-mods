"""Stage 2: quantitative motion diagnostics on a GLB animation."""
import sys
import numpy as np
from pygltflib import GLTF2

from anim_data import (Anim, ang_speed, derivatives, qangle_between, robust_z,
                       qnorm)
from gltf_io import read_accessor

np.set_printoptions(suppress=True, precision=4)

path = sys.argv[1]
label = sys.argv[2] if len(sys.argv) > 2 else path
g = GLTF2().load(path)
A = Anim(g, 0)
sk = A.skel
nm = sk.names

print("=" * 78)
print(f"MOTION DIAGNOSTICS : {label}")
print("=" * 78)
print(f"anim='{A.name}'  frames={A.F}  fps={A.fps:.3f}  duration={A.duration:.4f}s")

# ----------------------------------------------------- raw quaternion health
print("\n--- RAW QUATERNION HEALTH (pre-continuity-fix, straight from accessors) ---")
an = g.animations[0]
tot_flips = 0
worst_norm = 0.0
flip_detail = []
for ch in an.channels:
    if ch.target.path != "rotation":
        continue
    s = an.samplers[ch.sampler]
    out = read_accessor(g, s.output)
    n = np.linalg.norm(out, axis=-1)
    worst_norm = max(worst_norm, float(np.abs(n - 1).max()))
    q = qnorm(out)
    dots = np.sum(q[1:] * q[:-1], axis=-1)
    fl = np.where(dots < 0)[0]
    if len(fl):
        tot_flips += len(fl)
        flip_detail.append((nm[ch.target.node], len(fl), fl[:6] + 1))
print(f"max |‖q‖-1| across all rotation channels : {worst_norm:.3e}")
print(f"hemisphere sign flips (dot<0) total      : {tot_flips}")
for name, c, fr in sorted(flip_detail, key=lambda x: -x[1])[:12]:
    print(f"    {name:<22s} flips={c:<4d} first at frames {list(fr)}")
if not flip_detail:
    print("    (none — file is already hemisphere-continuous)")

# scale sanity
sc = A.scale
print(f"\nscale channels animated: {sorted(nm[i] for i in A.animated['scale']) or 'none'}")
print(f"scale range over all nodes/frames: {sc.min():.6f} .. {sc.max():.6f}")
print(f"non-unit scale nodes: {sorted({nm[i] for i in np.unique(np.where(np.abs(sc-1)>1e-4)[1])})}")

# ------------------------------------------------------------ FK world space
W, P, Q = A.fk()
dt = A.dt

KEY = ["Hips", "Spine", "Spine1", "Spine2", "Neck", "Head",
       "LeftShoulder", "LeftArm", "LeftForeArm", "LeftHand",
       "RightShoulder", "RightArm", "RightForeArm", "RightHand",
       "LeftUpLeg", "LeftLeg", "LeftFoot", "LeftToeBase",
       "RightUpLeg", "RightLeg", "RightFoot", "RightToeBase"]
IDX = {k: nm.index(k) for k in KEY if k in nm}

print("\n--- WORLD-SPACE JOINT MOTION (FK) ---")
print(f"{'joint':<16s} {'ymin':>7s} {'ymax':>7s} {'pathlen':>8s} "
      f"{'vmax':>8s} {'amax':>9s} {'jmax':>10s} {'ω̇max':>9s}")
rows = {}
for k, i in IDX.items():
    p = P[:, i]
    v, a, j = derivatives(p, dt)
    sp = np.linalg.norm(v, axis=-1)
    ac = np.linalg.norm(a, axis=-1)
    jk = np.linalg.norm(j, axis=-1)
    w = ang_speed(Q[:, i], dt)
    wdot = np.abs(np.gradient(w, dt))
    plen = float(np.sum(np.linalg.norm(np.diff(p, axis=0), axis=-1)))
    rows[k] = dict(v=sp, a=ac, j=jk, w=w, wdot=wdot, p=p)
    print(f"{k:<16s} {p[:,1].min():>7.3f} {p[:,1].max():>7.3f} {plen:>8.3f} "
          f"{sp.max():>8.3f} {ac.max():>9.1f} {jk.max():>10.1f} {wdot.max():>9.1f}")

# ------------------------------------------------------- per-bone local spikes
print("\n--- LOCAL ROTATION SPIKE DETECTION (per-frame geodesic delta) ---")
spikes = []
for i in sorted(A.animated["rotation"]):
    q = A.rot[:, i]
    d = qangle_between(q[1:], q[:-1])  # (F-1,)
    if d.max() < 1e-9:
        continue
    z = robust_z(d[:, None], win=15)[:, 0]
    for f in np.where((z > 6.0) & (d > np.deg2rad(2.0)))[0]:
        prev = d[f - 1] if f > 0 else 0.0
        cur = d[f]
        nxt = d[f + 1] if f + 1 < len(d) else 0.0
        spikes.append((float(z[f]), nm[i], int(f + 1), np.rad2deg(prev),
                       np.rad2deg(cur), np.rad2deg(nxt)))
spikes.sort(key=lambda x: -x[0])
print(f"total flagged bone-frames: {len(spikes)}")
print(f"{'bone':<22s} {'frame':>6s} {'prevΔ°':>8s} {'curΔ°':>8s} {'nextΔ°':>8s} {'z':>7s}  spike?")
for z, bone, f, pr, cu, nx in spikes[:35]:
    likely = "YES" if (cu > 2.5 * max(pr, nx, 1e-6)) else "maybe"
    print(f"{bone:<22s} {f:>6d} {pr:>8.2f} {cu:>8.2f} {nx:>8.2f} {z:>7.1f}  {likely}")

# per-bone summary of worst delta
print("\n--- WORST PER-FRAME LOCAL ROTATION DELTA, BY BONE (top 20) ---")
worst = []
for i in sorted(A.animated["rotation"]):
    d = np.rad2deg(qangle_between(A.rot[1:, i], A.rot[:-1, i]))
    worst.append((d.max(), float(np.median(d)), nm[i], int(np.argmax(d)) + 1))
worst.sort(key=lambda x: -x[0])
for mx, md, bone, f in worst[:20]:
    print(f"{bone:<22s} max={mx:>7.2f}°/frame @f{f:<4d} median={md:>6.3f}°  ratio={mx/max(md,1e-6):>7.1f}x")

# --------------------------------------------------------------- root motion
print("\n--- ROOT / HIPS TRANSLATION ---")
hips = nm.index("Hips")
ht = A.trans[:, hips]
hv = np.gradient(ht, dt, axis=0)
print(f"Hips local translation range: x[{ht[:,0].min():.4f},{ht[:,0].max():.4f}] "
      f"y[{ht[:,1].min():.4f},{ht[:,1].max():.4f}] z[{ht[:,2].min():.4f},{ht[:,2].max():.4f}]")
print(f"max per-frame hips step: {np.abs(np.diff(ht,axis=0)).max()*100:.3f} cm")
print(f"max hips speed: {np.linalg.norm(hv,axis=-1).max():.3f} m/s")

# ------------------------------------------------------- foot contact analysis
print("\n--- FOOT / CONTACT ANALYSIS ---")
for k in ["LeftFoot", "LeftToeBase", "RightFoot", "RightToeBase", "LeftLeg", "RightLeg"]:
    if k not in IDX:
        continue
    p = P[:, IDX[k]]
    sp = np.linalg.norm(np.gradient(p, dt, axis=0), axis=-1)
    lo = np.percentile(p[:, 1], 5)
    near = p[:, 1] < lo + 0.05
    slow = sp < 0.08
    contact = near & slow
    # longest run
    best = cur = 0
    bstart = cstart = 0
    for i, c in enumerate(contact):
        if c:
            if cur == 0:
                cstart = i
            cur += 1
            if cur > best:
                best, bstart = cur, cstart
        else:
            cur = 0
    frac = contact.mean()
    print(f"{k:<14s} ymin={p[:,1].min():.3f} y5%={lo:.3f} "
          f"contact_frames={contact.sum():>4d} ({frac*100:>4.1f}%) "
          f"longest_run={best:>4d} @f{bstart}..{bstart+best}")
    if best > 8:
        seg = p[bstart:bstart + best]
        drift = np.linalg.norm(seg - seg.mean(0), axis=-1)
        step = np.linalg.norm(np.diff(seg, axis=0), axis=-1)
        print(f"{'':<14s}   during longest contact: drift_rms={drift.mean()*100:.2f} cm "
              f"max_dev={drift.max()*100:.2f} cm  per-frame slide max={step.max()*100:.2f} cm")

# ----------------------------------------------------------------- phase hint
print("\n--- ACTIVITY PROFILE (for phase segmentation) ---")
tot = np.zeros(A.F)
for i in sorted(A.animated["rotation"]):
    tot += ang_speed(A.rot[:, i], dt)
hy = P[:, hips, 1]
print(f"{'frame':>6s} {'t(s)':>7s} {'hipY':>7s} {'Σang(rad/s)':>12s}")
for f in range(0, A.F, 15):
    bar = "#" * int(min(40, tot[f] / max(tot.max(), 1e-9) * 40))
    print(f"{f:>6d} {A.times[f]:>7.2f} {hy[f]:>7.3f} {tot[f]:>12.2f} {bar}")

print("\n--- NaN / Inf CHECK ---")
for nmm, arr in [("rot", A.rot), ("trans", A.trans), ("scale", A.scale), ("worldpos", P)]:
    print(f"{nmm:<10s} nan={int(np.isnan(arr).sum())} inf={int(np.isinf(arr).sum())}")
