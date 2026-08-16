# Mocap cleanup pipeline — C.O.R.E. left-thigh CAT tourniquet

Cleanup and partial reconstruction of the rough tourniquet mocap capture
(`video_edit`, 425 frames @ 30 fps, 14.133 s, FBX2glTF v0.13.1, 53-joint
Mixamo-style rig).

Output: `assets/core/animation/CORE_left_thigh_CAT_professional.glb`

## Why the animation edits are surgical

The exporter rewrites **only** the bytes of the 53 animation sampler output
accessors. Each has its own tightly-packed, non-interleaved float32 bufferView,
so mesh, skin weights, inverse bind matrices, materials, images, node hierarchy,
bone names and the rest pose come through byte-identical. The asset stays
loadable by the same pipeline as the original.

## What the capture actually contained

- The performer is **seated on the ground**, right leg folded and toe planted,
  left leg bent with the thigh presented — correct posture for self-applying a
  CAT, not the kneeling pose the brief assumed.
- Frames **0–350** are usable. Frames **351–424** are unrecoverable: the hips
  fall to floor level (Y ≈ −0.01 m), a foot reaches 0.64 m while the pelvis is
  at 0.12 m, and body-wide angular speed hits 6× baseline.
- The dominant defect is **not** per-frame spikes — a strict isolated-spike test
  finds none in f0–350. It is a ~40 cm whole-body slide across f280–350 with the
  right toe never leaving the floor, plus broadband chatter.
- `Spine2` and both clavicles are essentially unanimated (0.005°/frame) in the
  source. That is left alone rather than "fixed".

## Stages (`clean.py`)

1. **De-chatter** — two passes per bone in the quaternion tangent space: a
   robust local quadratic fit (rejects corrupted samples without flattening real
   velocity), then a smooth/residual split that attenuates only the
   high-frequency band. Per-group strength; fingers keep 78 % of their detail.
2. **Contact detection** — planting is tested on height + *vertical* velocity.
   Total speed is the wrong test: a skating foot slides fast while still in
   contact, and those are precisely the frames needing repair.
3. **Root stabilisation** — anchored to the right toe's own provably-planted
   window (f170–280, net travel 0.19 cm). Removes the secular slide only, so
   weight shifts and balance corrections survive.
4. **Ending reconstruction** — f351–424 rebuilt by reversing the performer's own
   descent (f12–92) at ~1:1 playback, landing on the f10 pose, measured to be the
   clip's best recovery pose. Exact pose continuity at the seam via a decaying
   delta; the vertical offset decays to zero so the feet do not float. Arms use a
   monotonic release with real high-frequency detail re-injected, because
   reversing the descent turned a bracing motion into a flail.
5. **Contact IK** — analytic two-bone IK with the original knee plane as pole,
   eased in/out. Pins the **toe**, not just the ankle: local-space smoothing
   moves the contact point in world space even where the capture was perfect.
6. **Floor clamp** — iterative, dilated before smoothing so it cannot undershoot.

## Scripts

| File | Purpose |
|---|---|
| `gltf_io.py` | accessor decoding (stride/sparse/normalized), skeleton, quat↔matrix |
| `anim_data.py` | dense TRS sampling, FK, quaternion utilities, SLERP |
| `filters.py` | manifold local-poly smoothing, log/exp, two-bone IK, easing |
| `clean.py` | the six-stage pipeline |
| `glb_write.py` | surgical animation-only GLB rewrite |
| `inspect_glb.py` / `diagnose.py` / `regions.py` / `jitter.py` | inspection + diagnostics |
| `compare.py` / `qc.py` | validation and before/after metrics |
| `render_sheet.py` / `sheet.py` / `ab.py` | Blender headless contact sheets |

## Reproduce

```bash
pip install numpy scipy pygltflib pillow
apt-get install -y blender python3-numpy libegl1 libgl1-mesa-dri xvfb

python3 clean.py source.glb CORE_left_thigh_CAT_professional.glb
python3 compare.py source.glb CORE_left_thigh_CAT_professional.glb
python3 qc.py      source.glb CORE_left_thigh_CAT_professional.glb

# visual review (Blender needs numpy on its own interpreter, hence python3-numpy)
LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a blender --background --python render_sheet.py -- \
    CORE_left_thigh_CAT_professional.glb ./shots out "0,60,150,250,350,424" track
python3 sheet.py ./shots side sheet.png 6 0.62
```

The pipeline is deterministic — the same input reproduces the same bytes.
