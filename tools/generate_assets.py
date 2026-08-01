#!/usr/bin/env python3
"""
Asset generator for Create: Jet Engines.

Everything visual comes from this one file:

  * 16x16 PNG textures (deliberate low-noise pixel art, Create-adjacent palette)
  * block models (JSON)      -> assets/.../models/block
  * blockstates (JSON)       -> assets/.../blockstates
  * item models (JSON)       -> assets/.../models/item
  * editable Blockbench      -> assets_src/*.bbmodel

Geometry notes
--------------
Minecraft only allows an element to be rotated about ONE axis by one of
-45/-22.5/0/22.5/45 degrees. A clean octagonal tube is therefore built from eight
*non-overlapping* wall segments: four axis-aligned (top/bottom/left/right) and four
made by rotating the top and bottom segments by +-45 degrees. Each segment spans only
the flat side length, so they meet at the corners instead of crossing through each
other — which is what made the first version look like a pile of scrap.

Models are authored pointing NORTH (-Z); the blockstate rotates them for the other
five facings, matching the vanilla end_rod/furnace convention.

Run:  python3 tools/generate_assets.py
      python3 tools/preview_model.py --all      # to look at the result
"""

import base64
import json
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = "create_jet_engines"
ASSETS = os.path.join(ROOT, "src/main/resources/assets", MODID)
TEX_DIR = os.path.join(ASSETS, "textures/block")
PARTICLE_TEX_DIR = os.path.join(ASSETS, "textures/particle")
PARTICLE_DEF_DIR = os.path.join(ASSETS, "particles")
MODEL_DIR = os.path.join(ASSETS, "models/block")
ITEM_MODEL_DIR = os.path.join(ASSETS, "models/item")
STATE_DIR = os.path.join(ASSETS, "blockstates")
BB_DIR = os.path.join(ROOT, "assets_src")

for d in (TEX_DIR, MODEL_DIR, ITEM_MODEL_DIR, STATE_DIR, BB_DIR,
          PARTICLE_TEX_DIR, PARTICLE_DEF_DIR):
    os.makedirs(d, exist_ok=True)

S = 16
C = 8.0  # block centre in model units


# =========================================================================
# texture helpers — deliberately flat and low-noise
# =========================================================================

def _c(v):
    return max(0, min(255, int(v)))


def shade(col, f):
    return (_c(col[0] * f), _c(col[1] * f), _c(col[2] * f), 255)


def new_img():
    return Image.new("RGBA", (S, S), (0, 0, 0, 255))


def fill_bands(img, base, steps=(1.14, 1.0, 0.88, 0.76)):
    """Horizontal value banding: reads as a curved metal surface without noise."""
    d = ImageDraw.Draw(img)
    n = len(steps)
    for i, f in enumerate(steps):
        y0 = round(i * S / n)
        y1 = round((i + 1) * S / n)
        d.rectangle([0, y0, S - 1, y1 - 1], fill=shade(base, f))


def specular(img, base, y, f=1.35):
    ImageDraw.Draw(img).line([(0, y), (S - 1, y)], fill=shade(base, f))


def seam(img, base, y, f=0.55):
    ImageDraw.Draw(img).line([(0, y), (S - 1, y)], fill=shade(base, f))


def vseams(img, base, xs, f=0.62):
    d = ImageDraw.Draw(img)
    for x in xs:
        d.line([(x, 0), (x, S - 1)], fill=shade(base, f))


def bolts(img, base, ys, xs, f=1.5):
    d = ImageDraw.Draw(img)
    for y in ys:
        for x in xs:
            d.point((x, y), fill=shade(base, f))


def radial_disc(img, blades, r_hub, r_tip, blade, gap, hub, twist=1.1):
    """A bladed disc seen face-on, for intake and interstage faces."""
    cx = cy = (S - 1) / 2.0
    px = img.load()
    for y in range(S):
        for x in range(S):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy) / (S / 2.0)
            if r > r_tip:
                px[x, y] = shade(gap, 0.35)
                continue
            if r < r_hub:
                px[x, y] = shade(hub, 1.0 - 0.25 * r)
                continue
            a = math.atan2(dy, dx) + r * twist
            frac = (a * blades / (2 * math.pi)) % 1.0
            if frac < 0.55:
                px[x, y] = shade(blade, 0.72 + 0.5 * math.sin(frac / 0.55 * math.pi))
            else:
                px[x, y] = shade(gap, 0.5)


def save(img, name):
    img.save(os.path.join(TEX_DIR, name + ".png"))
    return name


# =========================================================================
# textures
# =========================================================================

STEEL = (156, 162, 172)
DARK_STEEL = (104, 110, 122)
BRASS = (168, 140, 96)
SOOT = (74, 70, 70)
TITANIUM = (140, 132, 124)


def build_textures():
    made = []

    # --- plain nacelle skin ------------------------------------------------
    img = new_img()
    fill_bands(img, STEEL)
    specular(img, STEEL, 3)
    seam(img, STEEL, 8)
    bolts(img, STEEL, (1, 14), (2, 7, 12))
    made.append(save(img, "casing"))

    # --- ribbed skin, used for collars and bands ---------------------------
    img = new_img()
    fill_bands(img, DARK_STEEL, steps=(1.2, 0.95, 0.8))
    for y in (2, 6, 10, 13):
        seam(img, DARK_STEEL, y, 0.6)
    made.append(save(img, "casing_ribbed"))

    # --- compressor barrel: tight banding ---------------------------------
    img = new_img()
    fill_bands(img, DARK_STEEL, steps=(1.15, 1.0, 0.86))
    for y in range(1, S, 3):
        seam(img, DARK_STEEL, y, 0.66)
    specular(img, DARK_STEEL, 2, 1.3)
    made.append(save(img, "compressor_casing"))

    # --- combustion barrel: hot-side titanium with straw temper -----------
    img = new_img()
    fill_bands(img, TITANIUM, steps=(1.16, 1.0, 0.86, 0.74))
    specular(img, TITANIUM, 3, 1.3)
    px = img.load()
    for y in range(S):
        t = (y / (S - 1)) ** 1.6
        for x in range(S):
            r, g, b, a = px[x, y]
            px[x, y] = (_c(r + 46 * t), _c(g + 16 * t), _c(b - 14 * t), 255)
    bolts(img, BRASS, (1, 14), (3, 8, 13), 1.0)
    made.append(save(img, "combustion_casing"))

    # --- afterburner barrel: heat-darkened -------------------------------
    img = new_img()
    fill_bands(img, SOOT, steps=(1.25, 1.0, 0.82, 0.7))
    px = img.load()
    for y in range(S):
        t = (y / (S - 1)) ** 1.3
        for x in range(S):
            r, g, b, a = px[x, y]
            px[x, y] = (_c(r + 30 * t), _c(g + 8 * t), _c(b + 26 * t), 255)
    for y in (4, 11):
        seam(img, SOOT, y, 0.55)
    made.append(save(img, "afterburner_casing"))

    # --- nozzle petal: heat-stained, streaked along the flow --------------
    img = new_img()
    fill_bands(img, TITANIUM, steps=(1.1, 0.94, 0.8, 0.68))
    px = img.load()
    for y in range(S):
        t = (y / (S - 1)) ** 1.4
        for x in range(S):
            r, g, b, a = px[x, y]
            k = 1.0 + 0.10 * math.sin(x * 1.7)
            px[x, y] = (_c((r + 52 * t) * k), _c((g + 10 * t) * k), _c((b - 8 * t) * k), 255)
    vseams(img, TITANIUM, (3, 8, 12), 0.6)
    made.append(save(img, "nozzle_petal"))

    # --- structural collar -------------------------------------------------
    img = new_img()
    fill_bands(img, BRASS, steps=(1.2, 1.0, 0.82))
    for y in (5, 10):
        seam(img, BRASS, y, 0.62)
    made.append(save(img, "collar"))

    # --- fan face: big wide-chord blades ---------------------------------
    img = new_img()
    radial_disc(img, 8, 0.16, 1.02, (196, 202, 214), (44, 46, 54), (188, 190, 196), twist=1.3)
    made.append(save(img, "fan_face"))

    # --- compressor face: many small blades ------------------------------
    img = new_img()
    radial_disc(img, 14, 0.24, 1.02, (162, 168, 180), (38, 40, 48), (150, 154, 164), twist=0.9)
    made.append(save(img, "compressor_face"))

    # --- blade metal (edge-on) -------------------------------------------
    img = new_img()
    fill_bands(img, (196, 202, 214), steps=(1.16, 1.0, 0.84))
    vseams(img, (196, 202, 214), (2, 5, 8, 11, 14), 0.72)
    made.append(save(img, "fan_blade"))

    img = new_img()
    fill_bands(img, (168, 174, 186), steps=(1.14, 1.0, 0.86))
    vseams(img, (168, 174, 186), (1, 3, 5, 7, 9, 11, 13, 15), 0.74)
    made.append(save(img, "compressor_blade"))

    # --- spinner / shaft ---------------------------------------------------
    img = new_img()
    fill_bands(img, (206, 208, 216), steps=(1.18, 1.0, 0.82))
    ImageDraw.Draw(img).line([(0, 7), (S - 1, 7)], fill=shade((206, 208, 216), 0.55))
    made.append(save(img, "spinner"))

    # --- flame holder ------------------------------------------------------
    img = new_img()
    fill_bands(img, (118, 96, 74), steps=(1.2, 1.0, 0.82))
    for y in (3, 7, 11):
        seam(img, (118, 96, 74), y, 0.6)
    made.append(save(img, "flame_holder"))

    # --- dark interiors ----------------------------------------------------
    img = new_img()
    fill_bands(img, (30, 32, 38), steps=(1.5, 1.1, 0.8, 0.6))
    made.append(save(img, "intake_inner"))

    img = new_img()
    fill_bands(img, (46, 38, 36), steps=(1.4, 1.05, 0.8, 0.62))
    px = img.load()
    for y in range(S):
        for x in range(S):
            r, g, b, a = px[x, y]
            px[x, y] = (_c(r * (1 + 0.5 * (y / S))), _c(g), _c(b), 255)
    made.append(save(img, "nozzle_inner"))

    # --- glows -------------------------------------------------------------
    img = new_img()
    for y in range(S):
        for x in range(S):
            f = 0.80 + 0.20 * math.sin(x * 0.8) * math.cos(y * 0.6)
            img.putpixel((x, y), (_c(255 * f), _c(178 * f), _c(72 * f), 255))
    made.append(save(img, "combustion_glow"))

    img = new_img()
    for y in range(S):
        for x in range(S):
            f = 0.82 + 0.18 * math.sin(x * 0.9 + y * 0.4)
            img.putpixel((x, y), (_c(150 * f), _c(190 * f), _c(255 * f), 255))
    made.append(save(img, "afterburner_glow"))

    return made


# =========================================================================
# particle sprites
# =========================================================================

PS = 16  # particle sprite size


def _radial(fn):
    """Builds a PSxPS RGBA sprite from fn(distance_0_1, x, y) -> (r,g,b,a)."""
    img = Image.new("RGBA", (PS, PS), (0, 0, 0, 0))
    cx = cy = (PS - 1) / 2.0
    px = img.load()
    for y in range(PS):
        for x in range(PS):
            d = math.hypot(x - cx, y - cy) / (PS / 2.0)
            px[x, y] = fn(d, x, y)
    return img


def build_particle_textures():
    made = []

    # Heat haze: almost invisible, just enough to disturb what is behind it.
    def haze(d, x, y):
        if d > 1.0:
            return (0, 0, 0, 0)
        a = int(70 * (1.0 - d) ** 1.6)
        v = 226 + int(18 * math.sin(x * 1.9 + y * 1.3))
        return (_c(v), _c(v), _c(v), a)

    _radial(haze).save(os.path.join(PARTICLE_TEX_DIR, "exhaust_haze.png"))
    made.append("exhaust_haze")

    # Soot: soft dark puff with a broken edge so it does not read as a circle.
    def soot(d, x, y):
        edge = 0.86 + 0.14 * math.sin(x * 2.1) * math.cos(y * 1.7)
        if d > edge:
            return (0, 0, 0, 0)
        a = int(210 * (1.0 - d / edge) ** 0.9)
        v = 96 + int(26 * math.sin(x * 1.3 + y * 0.9))
        return (_c(v), _c(v * 0.97), _c(v * 0.95), a)

    _radial(soot).save(os.path.join(PARTICLE_TEX_DIR, "exhaust_soot.png"))
    made.append("exhaust_soot")

    # Flame: hot white core falling off fast. Tinted per particle at runtime,
    # so the sprite itself stays neutral.
    def flame(d, x, y):
        if d > 1.0:
            return (0, 0, 0, 0)
        core = max(0.0, 1.0 - d / 0.42) ** 1.5
        halo = max(0.0, 1.0 - d) ** 2.4
        a = int(255 * min(1.0, core + 0.55 * halo))
        v = 200 + 55 * core
        return (_c(v), _c(v), _c(v), a)

    _radial(flame).save(os.path.join(PARTICLE_TEX_DIR, "jet_flame.png"))
    made.append("jet_flame")

    # Shock diamond: a soft bright pulse. An actual hard ring reads as a floating
    # bubble in-game, so this is a filled falloff with only a faint ring hint.
    def ring(d, x, y):
        if d > 1.0:
            return (0, 0, 0, 0)
        core = max(0.0, 1.0 - d / 0.55) ** 1.3
        hint = 0.28 * math.exp(-((d - 0.60) ** 2) / 0.020)
        a = int(215 * min(1.0, core + hint))
        return (255, 255, 255, _c(a))

    _radial(ring).save(os.path.join(PARTICLE_TEX_DIR, "shock_diamond.png"))
    made.append("shock_diamond")

    for name in made:
        with open(os.path.join(PARTICLE_DEF_DIR, name + ".json"), "w") as fh:
            json.dump({"textures": [f"{MODID}:{name}"]}, fh, indent=2)

    return made


# =========================================================================
# geometry primitives
# =========================================================================

ALL_FACES = ["north", "east", "south", "west", "up", "down"]


def faces(tex):
    return {f: {"texture": "#" + tex} for f in ALL_FACES}


def cap_faces(side, cap):
    """Barrel sides get one texture; the two ends (north/south) get another."""
    out = {}
    for f in ALL_FACES:
        out[f] = {"texture": "#" + (cap if f in ("north", "south") else side)}
    return out


def elem(frm, to, tex, rotation=None, shade_on=True, per_face=None):
    e = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to],
         "faces": per_face or faces(tex)}
    if rotation:
        e["rotation"] = rotation
    if not shade_on:
        e["shade"] = False
    return e


def rot(z_centre, angle):
    return {"origin": [C, C, round(z_centre, 3)], "axis": "z", "angle": angle}


def tube(r_out, r_in, z0, z1, tex, per_face=None):
    """
    A clean octagonal tube: eight wall segments that meet at the corners rather
    than overlapping. Segment half-length is derived from the octagon side length
    (2*R*tan(22.5) ~= 0.828*R) with a small overlap so the seams stay watertight.
    """
    # 0.414*R is the exact regular-octagon side half-length. Sit just above it:
    # lower leaves hairline gaps at the corners, higher makes the axis-aligned
    # segments protrude past the diagonals and the silhouette turns into a gear.
    s = r_out * 0.43
    zc = (z0 + z1) / 2.0
    top = ((C - s, C + r_in, z0), (C + s, C + r_out, z1))
    bot = ((C - s, C - r_out, z0), (C + s, C - r_in, z1))
    rgt = ((C + r_in, C - s, z0), (C + r_out, C + s, z1))
    lft = ((C - r_out, C - s, z0), (C - r_in, C + s, z1))

    es = [elem(a, b, tex, per_face=per_face) for a, b in (top, bot, rgt, lft)]
    for ang in (45, -45):
        es.append(elem(top[0], top[1], tex, rotation=rot(zc, ang), per_face=per_face))
        es.append(elem(bot[0], bot[1], tex, rotation=rot(zc, ang), per_face=per_face))
    return es


def disc(r, z0, z1, tex, per_face=None):
    """
    A filled octagonal plate: four crossed bars whose union covers the circle.

    The bars necessarily pass through each other at the centre. Left exactly coincident
    their caps and side faces share planes, and Minecraft z-fights them into a flickering
    mess. Each bar is therefore nudged by a hair in z and in radius so no two faces are
    ever coplanar — invisible at block scale, and it removes the artefact entirely.
    """
    zc = (z0 + z1) / 2.0
    es = []
    for k, ang in enumerate((0, 90, 45, -45)):
        eps = 0.012 * k
        rr = r * (1.0 - 0.018 * k)
        ss = rr * 0.46
        za, zb = z0 + eps, z1 - eps
        if ang == 90:
            box = ((C - ss, C - rr, za), (C + ss, C + rr, zb))
            es.append(elem(box[0], box[1], tex, per_face=per_face))
        else:
            box = ((C - rr, C - ss, za), (C + rr, C + ss, zb))
            es.append(elem(box[0], box[1], tex,
                           rotation=None if ang == 0 else rot(zc, ang), per_face=per_face))
    return es


# Blade counts reachable with Minecraft's single-axis, fixed-angle element rotation.
# Four axis-aligned blades sit at 0/90/180/270; rotating each of them by the offsets
# below fills in the rest, so the total is always 4 x len(offsets).
_BLADE_OFFSETS = {
    4:  (0,),
    8:  (0, 45),
    12: (0, 22.5, 45),
    16: (0, 22.5, -22.5, 45),
}


def blade_ring(r_hub, r_tip, half_w, z0, z1, tex, count=8):
    """
    A ring of radial blades, staggered into two axial layers.

    Blades are boxes, so near the hub neighbouring blades overlap: at radius r the gap
    between blades is 2*r*sin(pi/count), and once that drops below the blade width
    2*half_w they interpenetrate and z-fight into a mess of shards. Sixteen blades at a
    usable width simply cannot fit in one plane.

    Splitting them into two axial layers halves the count per layer, which doubles the
    available gap, and any pair that still overlaps radially now sits at a different
    depth so no faces are coplanar. Real fans are staggered for the same packing reason.
    """
    offsets = _BLADE_OFFSETS[count]
    bases_for = lambda z0_, z1_: (
        ((C - half_w, C + r_hub, z0_), (C + half_w, C + r_tip, z1_)),   # top    (0 deg)
        ((C + r_hub, C - half_w, z0_), (C + r_tip, C + half_w, z1_)),   # right  (90)
        ((C - half_w, C - r_tip, z0_), (C + half_w, C - r_hub, z1_)),   # bottom (180)
        ((C - r_tip, C - half_w, z0_), (C - r_hub, C + half_w, z1_)),   # left   (270)
    )
    mid = (z0 + z1) / 2.0
    # front layer takes the even offsets, rear layer the odd ones
    layers = ((z0, mid + 0.05, offsets[0::2]), (mid - 0.05, z1, offsets[1::2]))

    es = []
    for za, zb, angs in layers:
        if not angs:
            continue
        zc = (za + zb) / 2.0
        for a, b in bases_for(za, zb):
            for ang in angs:
                es.append(elem(a, b, tex, rotation=None if ang == 0 else rot(zc, ang)))
    return es


def struts(count_axis, r_in, r_out, half_w, z0, z1, tex):
    """Four external pipes/bars running along the barrel."""
    es = []
    es.append(elem((C - half_w, C + r_in, z0), (C + half_w, C + r_out, z1), tex))
    es.append(elem((C - half_w, C - r_out, z0), (C + half_w, C - r_in, z1), tex))
    es.append(elem((C + r_in, C - half_w, z0), (C + r_out, C + half_w, z1), tex))
    es.append(elem((C - r_out, C - half_w, z0), (C - r_in, C + half_w, z1), tex))
    return es


def model(textures, elements, ao=False):
    return {
        # Inheriting block/block gives the standard display transforms, without
        # which the item form renders with an identity transform in the GUI.
        "parent": "minecraft:block/block",
        "ambientocclusion": ao,
        "textures": textures,
        "elements": elements,
    }


def tex(**kw):
    out = {k: f"{MODID}:block/{v}" for k, v in kw.items()}
    out.setdefault("particle", list(out.values())[0])
    return out


# =========================================================================
# the five modules (authored facing NORTH: exhaust toward -Z)
# =========================================================================

R_JOIN = 7.0  # shared outer radius at module joints, so a chain lines up


def build_models():
    out = {}

    # ------------------------------------------------------------ FAN ----
    # Widest module: a nacelle with a pronounced intake lip at the rear (+Z).
    els = []
    els += tube(R_JOIN, 5.6, 1.0, 13.0, "casing")
    els += tube(7.6, 5.2, 13.0, 16.0, "collar")          # intake lip, flared
    els += tube(R_JOIN, 6.2, 0.0, 1.0, "casing_ribbed")  # rear collar
    els += tube(5.6, 5.0, 1.0, 13.0, "intake_inner")     # dark liner down the duct
    els += disc(5.2, 1.4, 2.2, "fan_face")               # stator face, seen down the intake
    els += blade_ring(2.3, 5.0, 0.55, 3.0, 3.9, "compressor_blade", count=12)  # stator vanes
    out["fan_module"] = model(tex(casing="casing", collar="collar",
                                  casing_ribbed="casing_ribbed", fan_face="fan_face",
                                  intake_inner="intake_inner",
                                  compressor_blade="compressor_blade",
                                  particle="casing"), els)

    rotor = []
    rotor += blade_ring(2.5, 5.35, 0.80, 8.7, 11.7, "fan_blade", count=16)
    rotor += disc(2.1, 8.4, 11.8, "spinner")             # hub
    # stepped nose cone — more, smaller steps read as a smooth spinner
    for i, (r, za, zb) in enumerate(((1.75, 11.8, 12.7), (1.35, 12.7, 13.5),
                                     (0.95, 13.5, 14.2), (0.55, 14.2, 14.8))):
        rotor += disc(r, za, zb, "spinner")
    out["fan_rotor"] = model(tex(fan_blade="fan_blade", spinner="spinner",
                                 particle="spinner"), rotor)

    # ----------------------------------------------------- COMPRESSOR ----
    # Slimmer barrel with raised bands, so it reads differently from the fan.
    els = []
    els += tube(6.4, 5.2, 0.0, 16.0, "compressor_casing")
    for z in (1.0, 7.4, 14.0):
        els += tube(R_JOIN, 6.4, z, z + 1.4, "casing_ribbed")   # proud bands
    els += disc(1.5, 0.0, 16.0, "spinner")                       # shaft
    els += blade_ring(2.1, 4.9, 0.50, 4.3, 5.0, "compressor_blade", count=8)   # stators
    els += blade_ring(2.1, 4.9, 0.50, 10.9, 11.6, "compressor_blade", count=8)
    els += disc(5.0, 15.2, 15.8, "compressor_face")               # interstage face
    out["compressor_module"] = model(tex(compressor_casing="compressor_casing",
                                         casing_ribbed="casing_ribbed",
                                         spinner="spinner",
                                         compressor_blade="compressor_blade",
                                         compressor_face="compressor_face",
                                         particle="compressor_casing"), els)

    rotor = []
    for z in (2.2, 8.2, 12.6):
        rotor += blade_ring(2.2, 4.9, 0.60, z, z + 1.5, "compressor_blade", count=12)
    out["compressor_rotor"] = model(tex(compressor_blade="compressor_blade",
                                        particle="compressor_blade"), rotor)

    # ------------------------------------------------ COMBUSTION CORE ----
    # Bulged barrel with external fuel manifolds — the fattest silhouette.
    els = []
    els += tube(7.4, 5.4, 2.0, 14.0, "combustion_casing")
    els += tube(R_JOIN, 5.8, 0.0, 2.0, "casing_ribbed")
    els += tube(R_JOIN, 5.8, 14.0, 16.0, "casing_ribbed")
    els += struts(4, 7.4, 8.0, 0.9, 3.0, 13.0, "collar")   # fuel manifold pipes
    els += disc(1.4, 0.0, 16.0, "spinner")                  # shaft continuation
    out["combustion_core"] = model(tex(combustion_casing="combustion_casing",
                                       casing_ribbed="casing_ribbed",
                                       collar="collar", spinner="spinner",
                                       particle="combustion_casing"), els)

    out["combustion_glow"] = model(
        tex(combustion_glow="combustion_glow", particle="combustion_glow"),
        disc(5.0, 2.5, 13.5, "combustion_glow"))

    # ----------------------------------------------------- AFTERBURNER ---
    els = []
    els += tube(6.6, 5.4, 0.0, 16.0, "afterburner_casing")
    els += tube(R_JOIN, 6.6, 0.6, 2.0, "casing_ribbed")
    els += tube(R_JOIN, 6.6, 14.0, 15.4, "casing_ribbed")
    els += tube(5.2, 4.0, 3.4, 4.8, "flame_holder")       # flame holder rings
    els += tube(5.2, 4.0, 9.0, 10.4, "flame_holder")
    els += blade_ring(1.8, 5.2, 0.45, 6.3, 7.0, "flame_holder", count=8)   # spray bars
    out["afterburner_module"] = model(tex(afterburner_casing="afterburner_casing",
                                          casing_ribbed="casing_ribbed",
                                          flame_holder="flame_holder",
                                          particle="afterburner_casing"), els)

    out["afterburner_glow"] = model(
        tex(afterburner_glow="afterburner_glow", particle="afterburner_glow"),
        disc(4.6, 1.0, 15.0, "afterburner_glow"))

    # ---------------------------------------------------------- NOZZLE ---
    # Fixed collar at the rear; the petals are instanced and hinged by the renderer.
    els = []
    els += tube(R_JOIN, 5.6, 11.5, 16.0, "casing_ribbed")
    els += tube(6.6, 5.4, 9.0, 11.5, "nozzle_petal")
    els += disc(5.4, 10.4, 11.0, "nozzle_inner")
    out["nozzle_module"] = model(tex(casing_ribbed="casing_ribbed",
                                     nozzle_petal="nozzle_petal",
                                     nozzle_inner="nozzle_inner",
                                     particle="nozzle_petal"), els)

    # One petal, authored at the top of the ring; the renderer instances it eight
    # times around the axis and hinges it at the collar end.
    out["nozzle_petal"] = model(
        tex(nozzle_petal="nozzle_petal", particle="nozzle_petal"),
        [elem((C - 2.4, C + 4.4, 1.0), (C + 2.4, C + 6.4, 9.6), "nozzle_petal")])

    out["nozzle_inner"] = model(
        tex(nozzle_inner="nozzle_inner", particle="nozzle_inner"),
        disc(4.6, 0.5, 10.5, "nozzle_inner"))

    return out


# =========================================================================
# blockstates / item models / bbmodel
# =========================================================================

FACING_ROT = {
    "north": {},
    "east": {"y": 90},
    "south": {"y": 180},
    "west": {"y": 270},
    "up": {"x": 270},
    "down": {"x": 90},
}

BLOCK_MODULES = ["fan_module", "compressor_module", "combustion_core",
                 "afterburner_module", "nozzle_module"]


def build_blockstate(model_name):
    variants = {}
    for facing, r in FACING_ROT.items():
        v = {"model": f"{MODID}:block/{model_name}"}
        v.update(r)
        variants[f"facing={facing}"] = v
    return {"variants": variants}


def to_bbmodel(name, m):
    textures = []
    tex_index = {}
    for key, path in sorted(m["textures"].items()):
        if key == "particle":
            continue
        rel = path.split("block/")[-1] + ".png"
        png = os.path.join(TEX_DIR, rel)
        data = ""
        if os.path.exists(png):
            with open(png, "rb") as fh:
                data = "data:image/png;base64," + base64.b64encode(fh.read()).decode()
        tex_index[key] = len(textures)
        textures.append({
            "id": str(len(textures)), "name": rel, "folder": "block",
            "namespace": MODID, "particle": False, "render_mode": "normal",
            "visible": True, "mode": "bitmap", "saved": False,
            "uuid": f"00000000-0000-4000-8000-{len(textures):012d}",
            "source": data,
        })

    elements = []
    for i, e in enumerate(m["elements"]):
        f = {}
        for face, fd in e["faces"].items():
            key = fd["texture"].lstrip("#")
            f[face] = {"uv": fd.get("uv", [0, 0, 16, 16]), "texture": tex_index.get(key, 0)}
        el = {"name": f"part_{i}", "from": e["from"], "to": e["to"],
              "autouv": 0, "color": i % 8, "visibility": True,
              "uuid": f"10000000-0000-4000-8000-{i:012d}", "faces": f}
        if "rotation" in e:
            el["rotation"] = [0, 0, 0]
            el["rotation"][{"x": 0, "y": 1, "z": 2}[e["rotation"]["axis"]]] = e["rotation"]["angle"]
            el["origin"] = e["rotation"]["origin"]
        elements.append(el)

    return {
        "meta": {"format_version": "4.5", "model_format": "java_block", "box_uv": False},
        "name": name, "parent": "",
        "ambientocclusion": m.get("ambientocclusion", False),
        "front_gui_light": False, "visible_box": [1, 1, 0],
        "variable_placeholders": "", "variable_placeholder_buttons": [],
        "resolution": {"width": S, "height": S},
        "elements": elements,
        "outliner": [e["uuid"] for e in elements],
        "textures": textures,
    }


def main():
    made_tex = build_textures()
    made_particles = build_particle_textures()
    models = build_models()

    for name, m in models.items():
        with open(os.path.join(MODEL_DIR, name + ".json"), "w") as fh:
            json.dump(m, fh, indent=2)
        with open(os.path.join(BB_DIR, name + ".bbmodel"), "w") as fh:
            json.dump(to_bbmodel(name, m), fh, indent=1)

    for name in BLOCK_MODULES:
        with open(os.path.join(STATE_DIR, name + ".json"), "w") as fh:
            json.dump(build_blockstate(name), fh, indent=2)
        with open(os.path.join(ITEM_MODEL_DIR, name + ".json"), "w") as fh:
            json.dump({"parent": f"{MODID}:block/{name}"}, fh, indent=2)

    # every texture must be referenced by something, or it is dead weight
    referenced = set()
    for m in models.values():
        for v in m["textures"].values():
            referenced.add(v.split("/")[-1])
    unused = sorted(set(made_tex) - referenced)

    print(f"textures: {len(made_tex)}  particles: {len(made_particles)}  "
          f"models: {len(models)}  states: {len(BLOCK_MODULES)}")
    print(f"elements: " + ", ".join(f"{n}={len(m['elements'])}" for n, m in models.items()))
    print(f"unused textures: {unused if unused else 'none'}")


if __name__ == "__main__":
    main()
