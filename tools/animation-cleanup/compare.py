"""Validate the exported GLB and report original-vs-cleaned motion metrics."""
import sys
import json
import struct
import numpy as np
from pygltflib import GLTF2

from anim_data import Anim, qangle_between, qnorm
from gltf_io import read_accessor

SRC, DST = sys.argv[1], sys.argv[2]
GOOD = 351          # frames 0..350 are common to both (tail is rebuilt)
FINGERS = ("Thumb", "Index", "Middle", "Ring", "Pinky")


def load(p):
    g = GLTF2().load(p)
    return g, Anim(g, 0)


gs, As = load(SRC)
gd, Ad = load(DST)
nm = As.skel.names
I = {k: i for i, k in enumerate(nm)}

print("=" * 78)
print("STRUCTURAL VALIDATION OF EXPORTED GLB")
print("=" * 78)
ok = True


def chk(label, cond, detail=""):
    global ok
    ok = ok and bool(cond)
    print(f"  [{'PASS' if cond else 'FAIL'}] {label}{('  ' + detail) if detail else ''}")


raw = open(DST, "rb").read()
magic, ver, total = struct.unpack("<III", raw[:12])
chk("GLB magic/version", magic == 0x46546C67 and ver == 2, f"version={ver}")
chk("declared length matches file", total == len(raw), f"{total} == {len(raw)}")
chk("loads via glTF parser", gd is not None)
chk("mesh present", len(gd.meshes or []) == len(gs.meshes or []),
    f"{len(gd.meshes or [])} mesh(es)")
chk("skeleton node count preserved", len(gd.nodes) == len(gs.nodes), f"{len(gd.nodes)} nodes")
chk("skins preserved", len(gd.skins or []) == len(gs.skins or []),
    f"{len(gd.skins or [])} skin(s), joints={len(gd.skins[0].joints)}")
chk("materials preserved", len(gd.materials or []) == len(gs.materials or []))
chk("images preserved", len(gd.images or []) == len(gs.images or []))

ps, pd = gs.meshes[0].primitives[0], gd.meshes[0].primitives[0]
vs = read_accessor(gs, ps.attributes.POSITION)
vd = read_accessor(gd, pd.attributes.POSITION)
chk("vertex data untouched", vs.shape == vd.shape and np.array_equal(vs, vd),
    f"{vd.shape[0]} verts")
for at in ("JOINTS_0", "WEIGHTS_0", "JOINTS_1", "WEIGHTS_1", "NORMAL", "TEXCOORD_0"):
    a, b = getattr(ps.attributes, at), getattr(pd.attributes, at)
    if a is not None:
        chk(f"skin/attr {at} untouched", np.array_equal(read_accessor(gs, a), read_accessor(gd, b)))
ibm_s = read_accessor(gs, gs.skins[0].inverseBindMatrices)
ibm_d = read_accessor(gd, gd.skins[0].inverseBindMatrices)
chk("inverse bind matrices untouched", np.array_equal(ibm_s, ibm_d))

rest_ok = (np.allclose(As.skel.rest_t, Ad.skel.rest_t) and
           np.allclose(As.skel.rest_r, Ad.skel.rest_r) and
           np.allclose(As.skel.rest_s, Ad.skel.rest_s))
chk("rest pose unmodified", rest_ok)
chk("bone names unchanged", As.skel.names == Ad.skel.names)
chk("animation name preserved", Ad.name == As.name, f"'{Ad.name}'")
chk("duration preserved", abs(Ad.duration - As.duration) < 1e-6,
    f"{Ad.duration:.6f}s vs {As.duration:.6f}s")
chk("frame count preserved", Ad.F == As.F, f"{Ad.F} frames @ {Ad.fps:.2f} fps")
chk("channel count preserved", len(gd.animations[0].channels) == len(gs.animations[0].channels),
    f"{len(gd.animations[0].channels)} channels")
srcset = {(c.target.node, c.target.path) for c in gs.animations[0].channels}
dstset = {(c.target.node, c.target.path) for c in gd.animations[0].channels}
chk("all intended channels present", srcset == dstset)
chk("sampler timeline identical",
    np.array_equal(read_accessor(gs, gs.animations[0].samplers[0].input),
                   read_accessor(gd, gd.animations[0].samplers[0].input)))

chk("no NaN", not np.isnan(Ad.rot).any() and not np.isnan(Ad.trans).any())
chk("no Inf", not np.isinf(Ad.rot).any() and not np.isinf(Ad.trans).any())
qn = np.abs(np.linalg.norm(Ad.rot, axis=-1) - 1).max()
chk("quaternions normalised", qn < 1e-5, f"max |‖q‖-1| = {qn:.2e}")
chk("no bone scaling", np.abs(Ad.scale - 1).max() < 1e-6,
    f"max |scale-1| = {np.abs(Ad.scale-1).max():.2e}")

Wd, Pd, Qd = Ad.fk()
Ws, Ps, Qs = As.fk()
chk("no non-finite world transforms", np.isfinite(Pd).all())

# limb lengths must be rigid
bad_len = []
for a, b in [("LeftUpLeg", "LeftLeg"), ("LeftLeg", "LeftFoot"), ("RightUpLeg", "RightLeg"),
             ("RightLeg", "RightFoot"), ("LeftArm", "LeftForeArm"), ("LeftForeArm", "LeftHand"),
             ("RightArm", "RightForeArm"), ("RightForeArm", "RightHand")]:
    d = np.linalg.norm(Pd[:, I[b]] - Pd[:, I[a]], axis=-1)
    if d.std() > 1e-6:
        bad_len.append((a, b, d.std()))
chk("limb lengths rigid", not bad_len, f"max std {max([x[2] for x in bad_len]) if bad_len else 0:.2e} m")

print(f"\n  ==> {'ALL STRUCTURAL CHECKS PASSED' if ok else 'SOME CHECKS FAILED'}")

# ---------------------------------------------------------------- motion metrics
print("\n" + "=" * 78)
print("MOTION METRICS  (frames 0-350: directly comparable; tail is rebuilt)")
print("=" * 78)


def worst_ang(A, hi):
    w = []
    for i in sorted(A.animated["rotation"]):
        d = np.rad2deg(qangle_between(A.rot[1:hi, i], A.rot[:hi - 1, i]))
        w.append((d.max(), nm[i], int(np.argmax(d)) + 1))
    return sorted(w, key=lambda x: -x[0])


ws, wd = worst_ang(As, GOOD), worst_ang(Ad, GOOD)
print(f"\nLargest per-frame angular jump (local rotation), f0-350:")
print(f"  ORIGINAL {ws[0][0]:7.2f}°/frame  ({ws[0][1]} @f{ws[0][2]})")
print(f"  CLEANED  {wd[0][0]:7.2f}°/frame  ({wd[0][1]} @f{wd[0][2]})")
print(f"  top-5 original: " + ", ".join(f"{a:.1f}°({b})" for a, b, _ in ws[:5]))
print(f"  top-5 cleaned : " + ", ".join(f"{a:.1f}°({b})" for a, b, _ in wd[:5]))

print(f"\nLargest per-frame world positional jump, f0-350:")
js = np.linalg.norm(np.diff(Ps[:GOOD], axis=0), axis=-1)
jd = np.linalg.norm(np.diff(Pd[:GOOD], axis=0), axis=-1)
si = np.unravel_index(np.argmax(js), js.shape)
di = np.unravel_index(np.argmax(jd), jd.shape)
print(f"  ORIGINAL {js.max()*100:6.2f} cm/frame ({nm[si[1]]} @f{si[0]+1})")
print(f"  CLEANED  {jd.max()*100:6.2f} cm/frame ({nm[di[1]]} @f{di[0]+1})")

print(f"\nWhole-body jerk (world, RMS over all joints), f0-350:")
for lbl, Pp in [("ORIGINAL", Ps), ("CLEANED ", Pd)]:
    v = np.gradient(Pp[:GOOD], As.dt, axis=0)
    a = np.gradient(v, As.dt, axis=0)
    j = np.gradient(a, As.dt, axis=0)
    print(f"  {lbl} jerk RMS {np.sqrt((np.linalg.norm(j,axis=-1)**2).mean()):8.1f} m/s³   "
          f"peak {np.linalg.norm(j,axis=-1).max():8.1f}")


def rough(A, i, hi):
    q = A.rot[:hi, i]
    mid = q[:-2] + q[2:]
    mid /= np.linalg.norm(mid, axis=-1, keepdims=True)
    r = np.rad2deg(qangle_between(q[1:-1], mid))
    return float(np.sqrt((r ** 2).mean()))


print("\nRotational chatter (2nd-difference roughness RMS, deg), f0-350:")
bb = [rough(As, i, GOOD) for i in sorted(As.animated["rotation"]) if not any(t in nm[i] for t in FINGERS)]
ba = [rough(Ad, i, GOOD) for i in sorted(Ad.animated["rotation"]) if not any(t in nm[i] for t in FINGERS)]
fb = [rough(As, i, GOOD) for i in sorted(As.animated["rotation"]) if any(t in nm[i] for t in FINGERS)]
fa = [rough(Ad, i, GOOD) for i in sorted(Ad.animated["rotation"]) if any(t in nm[i] for t in FINGERS)]
print(f"  body    {np.mean(bb):.4f} -> {np.mean(ba):.4f}  ({100*(1-np.mean(ba)/np.mean(bb)):+.1f}%)")
print(f"  fingers {np.mean(fb):.4f} -> {np.mean(fa):.4f}  ({100*(1-np.mean(fa)/np.mean(fb)):+.1f}%)  [intentionally light]")

print("\nWorld-space trajectory noise (2nd difference, cm), f0-350:")
print(f"  {'joint':<15s} {'orig rms':>9s} {'clean rms':>10s} {'orig max':>9s} {'clean max':>10s}")
for k in ["LeftHand", "RightHand", "LeftForeArm", "RightForeArm", "Head", "Neck",
          "Spine2", "Hips", "LeftFoot", "RightFoot", "LeftToeBase", "RightToeBase"]:
    def acc(Pp):
        p = Pp[:GOOD, I[k]]
        n = np.linalg.norm(p[:-2] - 2 * p[1:-1] + p[2:], axis=-1) * 100
        return np.sqrt((n ** 2).mean()), n.max()
    a1, m1 = acc(Ps)
    a2, m2 = acc(Pd)
    print(f"  {k:<15s} {a1:>9.3f} {a2:>10.3f} {m1:>9.3f} {m2:>10.3f}")

print("\nPlanted-contact drift (right toe, its provably-planted window f170-350):")
for lbl, Pp in [("ORIGINAL", Ps), ("CLEANED ", Pd)]:
    p = Pp[170:351, I["RightToeBase"]][:, [0, 2]]
    step = np.linalg.norm(np.diff(p, axis=0), axis=-1)
    print(f"  {lbl} XZ range {np.linalg.norm(p.max(0)-p.min(0))*100:6.2f} cm   "
          f"mean slide/frame {step.mean()*100:5.3f} cm   net travel "
          f"{np.linalg.norm(p[-1]-p[0])*100:6.2f} cm")

print("\nRoot / hip drift during treatment phase (f170-350):")
for lbl, Pp in [("ORIGINAL", Ps), ("CLEANED ", Pd)]:
    h = Pp[170:351, I["Hips"]][:, [0, 2]]
    print(f"  {lbl} hips XZ net travel {np.linalg.norm(h[-1]-h[0])*100:6.2f} cm   "
          f"range {np.linalg.norm(h.max(0)-h.min(0))*100:6.2f} cm")

print("\nWrist trajectory noise during tourniquet interaction (f160-350):")
for k in ["LeftHand", "RightHand"]:
    def wn(Pp):
        p = Pp[160:351, I[k]]
        return np.linalg.norm(p[:-2] - 2 * p[1:-1] + p[2:], axis=-1).mean() * 100
    print(f"  {k:<10s} ORIGINAL {wn(Ps):.3f} cm  ->  CLEANED {wn(Pd):.3f} cm")

print("\nGround contact / floating:")
for lbl, Pp in [("ORIGINAL", Ps), ("CLEANED ", Pd)]:
    print(f"  {lbl} lowest joint Y over clip: {Pp[:,:,1].min()*100:+7.2f} cm   "
          f"frames with penetration < -1cm: {int((Pp[:,:,1].min(axis=1) < -0.01).sum())}")

print("\nBroken-tail comparison (f351-424, reconstructed):")
for lbl, Pp, Aa in [("ORIGINAL", Ps, As), ("CLEANED ", Pd, Ad)]:
    seg = Pp[351:]
    v = np.linalg.norm(np.gradient(seg, As.dt, axis=0), axis=-1)
    print(f"  {lbl} peak joint speed {v.max():6.2f} m/s   hips Y range "
          f"[{seg[:,I['Hips'],1].min():+.3f},{seg[:,I['Hips'],1].max():+.3f}]   "
          f"min joint Y {seg[:,:,1].min()*100:+6.2f} cm")
