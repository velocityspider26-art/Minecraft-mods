#!/usr/bin/env python3
"""
Offline preview of the exhaust plume mesh.

Reimplements the parcel model from PlumeTrail.java with the same constants, so the
plume's *shape* can be checked without launching the game — in particular that it
trails when the aircraft moves, bends when it turns, and flattens against terrain.

This validates the geometry, not the shader: the real thing is additively blended
in-game, which this approximates by accumulating colour.

Run:  python3 tools/preview_plume.py
"""

import math
import os

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "build/preview")
os.makedirs(OUT_DIR, exist_ok=True)

# --- constants mirrored from PlumeTrail.java -----------------------------------
MAX_AGE = 12
MAX_SAMPLES = 20
RING = 12
SHOCK_NODES = 5
SHOCK_AMPLITUDE = 0.52
TURBULENCE = 0.42
WORLD_UP = np.array([0.0, 1.0, 0.0])

W, H = 640, 360


def drift_speed(throttle, lit):
    return (0.34 if lit else 0.15) + 0.30 * throttle


def parcel_position(p, partial=0.0):
    t = p["age"] + partial
    travel = min(p["clearance"], drift_speed(p["throttle"], p["lit"]) * t)
    rise = 0.0075 * t * t * (1.0 - 0.75 * p["throttle"])
    return p["origin"] + p["dir"] * travel + WORLD_UP * rise


def hash01(seed, k):
    h = (seed * 374761393 + k * 668265263) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def parcel_radius(p, partial=0.0):
    u = min(1.0, (p["age"] + partial) / MAX_AGE)
    r0 = 0.26 + 0.10 * p["throttle"]
    if p["lit"]:
        spindle = r0 * (1.0 + 0.9 * u) * (1.0 - u) ** 0.45
        # mach diamonds
        shock = 1.0 + SHOCK_AMPLITUDE * math.exp(-1.2 * u) * math.sin(u * math.pi * SHOCK_NODES)
        return spindle * shock
    return r0 * (1.0 + 1.9 * u)


def parcel_colour(p, alpha_scale):
    t = min(1.0, p["age"] / MAX_AGE)
    fade = (1.0 - t) ** 1.4
    if p["lit"]:
        r = 0.55 + 0.45 * min(1.0, t * 1.8)
        g = 0.74 - 0.34 * t
        b = max(0.0, 1.0 - 1.7 * t)
        a = (1.0 - t) ** 2.2 * (0.65 + 0.35 * p["throttle"])
        a *= 0.78 + 0.22 * hash01(p["seed"], 7)
        a *= 1.0 + 0.40 * math.exp(-1.2 * t) * math.sin(t * math.pi * SHOCK_NODES)
    else:
        r, g, b = 1.0, 0.86, 0.72
        a = fade * 0.10 * (0.4 + 0.6 * p["throttle"])
    return np.array([r, g, b]), a * alpha_scale


def make_frame(prev_side, direction):
    ref = prev_side
    if ref is None or abs(np.dot(ref, direction)) > 0.999:
        ref = np.array([1.0, 0, 0]) if abs(direction[1]) > 0.9 else WORLD_UP
    side = ref - direction * np.dot(ref, direction)
    if np.dot(side, side) < 1e-6:
        side = np.array([1.0, 0, 0]) if abs(direction[1]) > 0.9 else WORLD_UP
        side = side - direction * np.dot(side, direction)
    side = side / np.linalg.norm(side)
    other = np.cross(direction, side)
    other = other / np.linalg.norm(other)
    return side, other


def simulate(path_fn, ticks=26, throttle=1.0, lit=True, ground_y=None):
    """path_fn(tick) -> (nozzle_pos, exhaust_dir)."""
    parcels = []
    side = None
    for tick in range(ticks):
        pos, direction = path_fn(tick)
        direction = direction / np.linalg.norm(direction)
        side, other = make_frame(side, direction)

        clearance = 7.0
        if ground_y is not None and direction[1] < -1e-6:
            clearance = max(0.15, (pos[1] - ground_y) / -direction[1])
        elif ground_y is not None:
            # flat flight just above the deck: gas still splashes when it sinks
            clearance = 7.0

        for p in parcels:
            p["age"] += 1
        parcels = [p for p in parcels if p["age"] <= MAX_AGE]
        parcels.insert(0, {"origin": pos, "dir": direction, "side": side, "other": other,
                           "throttle": throttle, "lit": lit, "clearance": clearance,
                           "seed": tick + 1, "age": 0})
        parcels = parcels[:MAX_SAMPLES]
    return parcels


def ring_point(p, centre, radius, theta, k=0, partial=0.0):
    u = min(1.0, (p["age"] + partial) / MAX_AGE)
    amount = TURBULENCE * u * u
    n = (hash01(p["seed"], k) - 0.5) * 2.0
    r = radius * (1.0 + amount * n)
    return centre + p["side"] * (math.cos(theta) * r) + p["other"] * (math.sin(theta) * r)


def render(parcels, title, yaw=-40.0, pitch=-18.0, span=9.0, centre=None):
    ry = math.radians(yaw)
    rx = math.radians(pitch)
    Ry = np.array([[math.cos(ry), 0, math.sin(ry)], [0, 1, 0], [-math.sin(ry), 0, math.cos(ry)]])
    Rx = np.array([[1, 0, 0], [0, math.cos(rx), -math.sin(rx)], [0, math.sin(rx), math.cos(rx)]])
    M = Rx @ Ry

    if centre is None:
        pts = np.array([parcel_position(p) for p in parcels])
        centre = pts.mean(axis=0)
    scale = min(W, H) / span

    img = np.zeros((H, W, 3), dtype=np.float32)
    img[:] = np.array([0.06, 0.07, 0.09])

    def project(v):
        q = M @ (v - centre)
        return np.array([q[0] * scale + W / 2.0, -q[1] * scale + H / 2.0])

    # additive accumulation, inner core then outer halo
    for radius_scale, alpha_scale in ((0.55, 1.0), (1.0, 0.40)):
        for i in range(len(parcels) - 1):
            a, b = parcels[i], parcels[i + 1]
            pa, pb = parcel_position(a), parcel_position(b)
            ra = parcel_radius(a) * radius_scale
            rb = parcel_radius(b) * radius_scale
            ca, aa = parcel_colour(a, alpha_scale)
            cb, ab = parcel_colour(b, alpha_scale)
            if aa <= 0.002 and ab <= 0.002:
                continue
            for k in range(RING):
                t0 = (k / RING) * 2 * math.pi
                t1 = ((k + 1) / RING) * 2 * math.pi
                quad = [project(ring_point(a, pa, ra, t0, k)),
                        project(ring_point(a, pa, ra, t1, k + 1)),
                        project(ring_point(b, pb, rb, t1, k + 1)),
                        project(ring_point(b, pb, rb, t0, k))]
                col = (ca * aa + cb * ab) * 0.5
                fill_quad(img, quad, col)

    img = np.clip(img, 0, 1)
    out = Image.fromarray((img * 255).astype(np.uint8))
    from PIL import ImageDraw
    ImageDraw.Draw(out).text((10, 8), title, fill=(235, 235, 240))
    return out


def fill_quad(img, quad, colour):
    xs = [q[0] for q in quad]
    ys = [q[1] for q in quad]
    minx, maxx = int(max(0, math.floor(min(xs)))), int(min(W, math.ceil(max(xs)) + 1))
    miny, maxy = int(max(0, math.floor(min(ys)))), int(min(H, math.ceil(max(ys)) + 1))
    if minx >= maxx or miny >= maxy:
        return
    gx, gy = np.meshgrid(np.arange(minx, maxx) + 0.5, np.arange(miny, maxy) + 0.5)
    inside = np.ones(gx.shape, dtype=bool)
    for i in range(4):
        x0, y0 = quad[i]
        x1, y1 = quad[(i + 1) % 4]
        inside &= ((x1 - x0) * (gy - y0) - (y1 - y0) * (gx - x0)) >= -1e-6
    if not inside.any():
        # try the opposite winding
        inside = np.ones(gx.shape, dtype=bool)
        for i in range(4):
            x0, y0 = quad[i]
            x1, y1 = quad[(i + 1) % 4]
            inside &= ((x1 - x0) * (gy - y0) - (y1 - y0) * (gx - x0)) <= 1e-6
        if not inside.any():
            return
    sub = img[miny:maxy, minx:maxx]
    sub[inside] += colour


def main():
    scenes = []

    # 1. stationary aircraft at full reheat: straight tapering cone
    scenes.append((simulate(lambda t: (np.array([0.0, 0.0, 0.0]), np.array([1.0, 0.0, 0.0]))),
                   "stationary, full reheat"))

    # 2. flying straight: emission points spread along the flight path, so the plume
    #    stretches out behind instead of sitting on the nozzle
    scenes.append((simulate(lambda t: (np.array([-0.45 * t, 0.0, 0.0]), np.array([1.0, 0.0, 0.0]))),
                   "flying straight (plume trails)"))

    # 3. banking turn: each parcel keeps the direction it was emitted with, so the
    #    plume bends through the turn
    def turn(t):
        a = math.radians(6.0 * t)
        r = 7.0
        pos = np.array([math.sin(a) * r, 0.0, (1 - math.cos(a)) * r])
        d = np.array([-math.cos(a), 0.0, -math.sin(a)])
        return pos, d
    scenes.append((simulate(turn), "banking turn (plume bends)"))

    # 4. low pass over terrain: drift clamped by the raycast, so it splashes flat
    def low(t):
        return np.array([-0.35 * t, 1.1, 0.0]), np.array([0.82, -0.57, 0.0])
    scenes.append((simulate(low, ground_y=0.0), "impinging on terrain"))

    tiles = [render(p, title) for p, title in scenes]
    sheet = Image.new("RGB", (W * 2, H * 2))
    for i, tile in enumerate(tiles):
        sheet.paste(tile, ((i % 2) * W, (i // 2) * H))
    path = os.path.join(OUT_DIR, "plume.png")
    sheet.save(path)
    print("wrote", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    main()
