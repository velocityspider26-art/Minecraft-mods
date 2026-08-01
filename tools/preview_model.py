#!/usr/bin/env python3
"""
Offline renderer for Minecraft block-model JSON.

Exists so the engine models can be iterated on without launching the game: it
rasterises the same JSON Minecraft loads, with the same element rotations and
textures, so what you see here is what the game will draw.

Usage:
    python3 tools/preview_model.py fan_module
    python3 tools/preview_model.py fan_module+fan_rotor          # overlay parts
    python3 tools/preview_model.py --row fan_module,compressor_module,combustion_core
    python3 tools/preview_model.py --all
"""

import json
import math
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = "create_jet_engines"
ASSETS = os.path.join(ROOT, "src/main/resources/assets", MODID)
MODEL_DIR = os.path.join(ASSETS, "models/block")
TEX_DIR = os.path.join(ASSETS, "textures/block")
OUT_DIR = os.path.join(ROOT, "build/preview")
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 620, 620
YAW = math.radians(-38.0)
PITCH = math.radians(-22.0)

# Minecraft shades a face by which way it points, not by a light vector.
# Matching that here is what makes the preview representative of the game.
MC_FACE_SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8,
                 "east": 0.6, "west": 0.6}


def mc_shade(normal):
    """Nearest cardinal face brightness for a (possibly rotated) normal."""
    n = normal / (np.linalg.norm(normal) + 1e-9)
    axes = (("east", np.array([1, 0, 0])), ("west", np.array([-1, 0, 0])),
            ("up", np.array([0, 1, 0])), ("down", np.array([0, -1, 0])),
            ("south", np.array([0, 0, 1])), ("north", np.array([0, 0, -1])))
    best = max(axes, key=lambda a: float(np.dot(n, a[1])))
    return MC_FACE_SHADE[best[0]]

FACE_DEFS = {
    "down":  ((0, -1, 0), lambda a, b: [(a[0], a[1], b[2]), (b[0], a[1], b[2]), (b[0], a[1], a[2]), (a[0], a[1], a[2])]),
    "up":    ((0, 1, 0),  lambda a, b: [(a[0], b[1], a[2]), (b[0], b[1], a[2]), (b[0], b[1], b[2]), (a[0], b[1], b[2])]),
    "north": ((0, 0, -1), lambda a, b: [(b[0], b[1], a[2]), (a[0], b[1], a[2]), (a[0], a[1], a[2]), (b[0], a[1], a[2])]),
    "south": ((0, 0, 1),  lambda a, b: [(a[0], b[1], b[2]), (b[0], b[1], b[2]), (b[0], a[1], b[2]), (a[0], a[1], b[2])]),
    "west":  ((-1, 0, 0), lambda a, b: [(a[0], b[1], a[2]), (a[0], b[1], b[2]), (a[0], a[1], b[2]), (a[0], a[1], a[2])]),
    "east":  ((1, 0, 0),  lambda a, b: [(b[0], b[1], b[2]), (b[0], b[1], a[2]), (b[0], a[1], a[2]), (b[0], a[1], b[2])]),
}


def auto_uv(face, a, b):
    """Approximates vanilla's generated UVs when a face has no explicit uv."""
    x1, y1, z1 = a
    x2, y2, z2 = b
    if face in ("north", "south"):
        u0, v0, u1, v1 = x1, 16 - y2, x2, 16 - y1
    elif face in ("east", "west"):
        u0, v0, u1, v1 = z1, 16 - y2, z2, 16 - y1
    else:
        u0, v0, u1, v1 = x1, z1, x2, z2
    return [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]


def rot_matrix(axis, degrees):
    r = math.radians(degrees)
    c, s = math.cos(r), math.sin(r)
    if axis == "x":
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == "y":
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def load_texture(path_ref, cache):
    name = path_ref.split("/")[-1]
    if name in cache:
        return cache[name]
    p = os.path.join(TEX_DIR, name + ".png")
    if not os.path.exists(p):
        img = Image.new("RGBA", (16, 16), (255, 0, 255, 255))
    else:
        img = Image.open(p).convert("RGBA")
    arr = np.asarray(img).astype(np.float32) / 255.0
    cache[name] = arr
    return arr


def gather_triangles(model, offset=(0, 0, 0), tex_cache=None):
    """Returns a list of (verts(3x3), uvs(3x2), texture array, normal)."""
    tex_cache = tex_cache if tex_cache is not None else {}
    textures = model.get("textures", {})
    tris = []

    for el in model.get("elements", []):
        a = np.array(el["from"], dtype=np.float64)
        b = np.array(el["to"], dtype=np.float64)
        rotation = el.get("rotation")
        R = None
        origin = None
        if rotation:
            R = rot_matrix(rotation["axis"], rotation["angle"])
            origin = np.array(rotation["origin"], dtype=np.float64)

        for face, fd in el.get("faces", {}).items():
            normal_def, corner_fn = FACE_DEFS[face]
            corners = [np.array(c, dtype=np.float64) for c in corner_fn(a, b)]
            uvs = fd.get("uv") or None
            if uvs and len(uvs) == 4:
                u0, v0, u1, v1 = uvs
                uv = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
            else:
                uv = auto_uv(face, a, b)

            normal = np.array(normal_def, dtype=np.float64)
            if R is not None:
                corners = [R @ (c - origin) + origin for c in corners]
                normal = R @ normal

            corners = [c + np.array(offset, dtype=np.float64) for c in corners]

            ref = textures.get(fd["texture"].lstrip("#"), "")
            tex = load_texture(ref, tex_cache)

            for i0, i1, i2 in ((0, 1, 2), (0, 2, 3)):
                tris.append((
                    np.array([corners[i0], corners[i1], corners[i2]]),
                    np.array([uv[i0], uv[i1], uv[i2]], dtype=np.float64),
                    tex, normal))
    return tris


def render(tris, span=16.0, bg=(150, 156, 166)):
    Ry = rot_matrix("y", math.degrees(YAW))
    Rx = rot_matrix("x", math.degrees(PITCH))
    M = Rx @ Ry

    centre = np.array([span / 2.0, 8.0, 8.0])
    scale = min(W, H) / (span * 1.55)

    colour = np.zeros((H, W, 3), dtype=np.float32)
    colour[:] = np.array(bg, dtype=np.float32) / 255.0
    zbuf = np.full((H, W), -1e9, dtype=np.float32)

    for verts, uv, tex, normal in tris:
        p = (M @ (verts - centre).T).T
        sx = p[:, 0] * scale + W / 2.0
        sy = -p[:, 1] * scale + H / 2.0
        sz = p[:, 2]

        lam = mc_shade(normal)

        minx = max(int(np.floor(sx.min())), 0)
        maxx = min(int(np.ceil(sx.max())) + 1, W)
        miny = max(int(np.floor(sy.min())), 0)
        maxy = min(int(np.ceil(sy.max())) + 1, H)
        if minx >= maxx or miny >= maxy:
            continue

        x0, y0 = sx[0], sy[0]
        x1, y1 = sx[1], sy[1]
        x2, y2 = sx[2], sy[2]
        denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
        if abs(denom) < 1e-9:
            continue

        xs = np.arange(minx, maxx) + 0.5
        ys = np.arange(miny, maxy) + 0.5
        gx, gy = np.meshgrid(xs, ys)

        l0 = ((y1 - y2) * (gx - x2) + (x2 - x1) * (gy - y2)) / denom
        l1 = ((y2 - y0) * (gx - x2) + (x0 - x2) * (gy - y2)) / denom
        l2 = 1.0 - l0 - l1
        inside = (l0 >= -1e-6) & (l1 >= -1e-6) & (l2 >= -1e-6)
        if not inside.any():
            continue

        depth = l0 * sz[0] + l1 * sz[1] + l2 * sz[2]
        sub_z = zbuf[miny:maxy, minx:maxx]
        visible = inside & (depth > sub_z)
        if not visible.any():
            continue

        u = (l0 * uv[0, 0] + l1 * uv[1, 0] + l2 * uv[2, 0]) / 16.0
        v = (l0 * uv[0, 1] + l1 * uv[1, 1] + l2 * uv[2, 1]) / 16.0
        th, tw = tex.shape[0], tex.shape[1]
        tx = np.clip((u * tw).astype(np.int32), 0, tw - 1)
        ty = np.clip((v * th).astype(np.int32), 0, th - 1)
        texel = tex[ty, tx]

        alpha = texel[..., 3]
        visible &= alpha > 0.35
        if not visible.any():
            continue

        rgb = texel[..., :3] * lam
        sub_c = colour[miny:maxy, minx:maxx]
        sub_c[visible] = rgb[visible]
        sub_z[visible] = depth[visible]

    return Image.fromarray((np.clip(colour, 0, 1) * 255).astype(np.uint8))


def load_model(name):
    with open(os.path.join(MODEL_DIR, name + ".json")) as fh:
        return json.load(fh)


def main():
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        return 1

    if args[0] == "--all":
        names = [os.path.basename(p)[:-5] for p in sorted(
            os.listdir(MODEL_DIR)) if p.endswith(".json")]
        names = [n for n in names if not n.endswith(("_glow", "_inner", "_petal"))]
        args = ["--row", ",".join(names)]

    cache = {}
    if args[0] == "--row":
        names = args[1].split(",")
        tris = []
        for i, n in enumerate(names):
            tris += gather_triangles(load_model(n), offset=(i * 16, 0, 0), tex_cache=cache)
        img = render(tris, span=16.0 * len(names))
        out = os.path.join(OUT_DIR, "row.png")
    else:
        parts = args[0].split("+")
        tris = []
        for p in parts:
            tris += gather_triangles(load_model(p), tex_cache=cache)
        img = render(tris)
        out = os.path.join(OUT_DIR, parts[0] + ".png")

    img.save(out)
    print("wrote", os.path.relpath(out, ROOT))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
