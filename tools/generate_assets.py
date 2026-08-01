#!/usr/bin/env python3
"""
Asset generator for Create: Jet Engines.

Everything visual is produced from one source of truth in this file:

  * 32x32 PNG textures (procedural panelled metal, blades, heat staining, glow)
  * block models (JSON)      -> src/main/resources/assets/.../models/block
  * blockstates (JSON)       -> src/main/resources/assets/.../blockstates
  * item models (JSON)       -> src/main/resources/assets/.../models/item
  * editable Blockbench      -> assets_src/*.bbmodel

Models are authored pointing NORTH (-Z); the blockstate rotates them for the other
five facings, matching the vanilla convention used by end_rod/furnace.

Run:  python3 tools/generate_assets.py
"""

import base64
import json
import math
import os
import random
from io import BytesIO

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = "create_jet_engines"
ASSETS = os.path.join(ROOT, "src/main/resources/assets", MODID)
TEX_DIR = os.path.join(ASSETS, "textures/block")
MODEL_DIR = os.path.join(ASSETS, "models/block")
ITEM_MODEL_DIR = os.path.join(ASSETS, "models/item")
STATE_DIR = os.path.join(ASSETS, "blockstates")
BB_DIR = os.path.join(ROOT, "assets_src")

for d in (TEX_DIR, MODEL_DIR, ITEM_MODEL_DIR, STATE_DIR, BB_DIR):
    os.makedirs(d, exist_ok=True)

S = 32  # texture size
rng = random.Random(20260801)


# ---------------------------------------------------------------------------
# texture helpers
# ---------------------------------------------------------------------------

def _clamp(v):
    return max(0, min(255, int(v)))


def base_metal(img, top, bottom, grain=10):
    """Vertical gradient with fine grain noise."""
    d = ImageDraw.Draw(img)
    for y in range(S):
        t = y / (S - 1)
        c = [top[i] + (bottom[i] - top[i]) * t for i in range(3)]
        for x in range(S):
            n = rng.randint(-grain, grain)
            d.point((x, y), fill=(_clamp(c[0] + n), _clamp(c[1] + n), _clamp(c[2] + n), 255))


def panel_lines(img, color=(40, 42, 46), step=8, offset=0):
    d = ImageDraw.Draw(img)
    for x in range(offset, S, step):
        d.line([(x, 0), (x, S - 1)], fill=color + (255,))
    for y in range(offset, S, step):
        d.line([(0, y), (S - 1, y)], fill=color + (255,))


def rivets(img, color=(150, 152, 158), step=8, inset=3):
    d = ImageDraw.Draw(img)
    for x in range(inset, S, step):
        for y in range(inset, S, step):
            d.point((x, y), fill=color + (255,))


def heat_stain(img, strength=1.0):
    """Blue/straw tempering colours, strongest toward the bottom of the texture."""
    px = img.load()
    for y in range(S):
        t = (y / (S - 1)) ** 1.5 * strength
        for x in range(S):
            r, g, b, a = px[x, y]
            px[x, y] = (
                _clamp(r + 70 * t + rng.randint(-6, 6)),
                _clamp(g + 30 * t - 10 * t),
                _clamp(b - 25 * t + 40 * t * (0.4 + 0.6 * math.sin(x * 0.7))),
                a,
            )


def radial_blades(img, blades, inner, outer, blade_col, gap_col, hub_col):
    """Draws a bladed disc seen face-on — used for fan/compressor end caps."""
    cx = cy = (S - 1) / 2.0
    px = img.load()
    for y in range(S):
        for x in range(S):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy) / (S / 2.0)
            if r > outer:
                continue
            if r < inner:
                px[x, y] = hub_col + (255,)
                continue
            a = math.atan2(dy, dx)
            # twisted blade: angle offset grows with radius
            phase = (a + r * 1.4) * blades / (2 * math.pi)
            frac = phase - math.floor(phase)
            shade = 0.55 + 0.45 * math.sin(frac * math.pi)
            col = blade_col if frac < 0.62 else gap_col
            px[x, y] = (
                _clamp(col[0] * shade), _clamp(col[1] * shade), _clamp(col[2] * shade), 255)


def new_img(alpha=False):
    return Image.new("RGBA", (S, S), (0, 0, 0, 0) if alpha else (0, 0, 0, 255))


def save(img, name):
    img.save(os.path.join(TEX_DIR, name + ".png"))
    return name


# ---------------------------------------------------------------------------
# textures
# ---------------------------------------------------------------------------

def build_textures():
    made = []

    # --- generic casings ---------------------------------------------------
    img = new_img()
    base_metal(img, (150, 154, 162), (96, 100, 108))
    panel_lines(img)
    rivets(img)
    made.append(save(img, "casing"))

    img = new_img()
    base_metal(img, (128, 132, 140), (78, 82, 90))
    panel_lines(img, step=16, offset=0)
    rivets(img, step=16, inset=5)
    made.append(save(img, "casing_plain"))

    # --- fan ---------------------------------------------------------------
    img = new_img()
    base_metal(img, (60, 63, 70), (34, 36, 42), grain=6)
    radial_blades(img, 9, 0.14, 1.05, (176, 180, 190), (58, 61, 68), (120, 124, 132))
    made.append(save(img, "fan_face"))

    img = new_img()
    base_metal(img, (186, 190, 200), (132, 136, 146), grain=8)
    d = ImageDraw.Draw(img)
    for i in range(0, S, 4):
        d.line([(i, 0), (i, S - 1)], fill=(96, 100, 108, 255))
    made.append(save(img, "fan_blade"))

    img = new_img()
    base_metal(img, (200, 202, 210), (140, 143, 150))
    d = ImageDraw.Draw(img)
    d.ellipse([6, 6, S - 7, S - 7], outline=(90, 93, 100, 255))
    made.append(save(img, "fan_hub"))

    # --- compressor --------------------------------------------------------
    img = new_img()
    base_metal(img, (52, 55, 62), (30, 32, 38), grain=6)
    radial_blades(img, 15, 0.20, 1.05, (150, 154, 164), (48, 51, 58), (104, 108, 116))
    made.append(save(img, "compressor_face"))

    img = new_img()
    base_metal(img, (140, 144, 152), (92, 96, 104))
    panel_lines(img, color=(52, 55, 60), step=4)
    rivets(img, step=8, inset=2)
    made.append(save(img, "compressor_casing"))

    img = new_img()
    base_metal(img, (168, 172, 182), (120, 124, 132), grain=6)
    d = ImageDraw.Draw(img)
    for i in range(0, S, 3):
        d.line([(i, 0), (i, S - 1)], fill=(84, 88, 96, 255))
    made.append(save(img, "compressor_blade"))

    # --- combustion --------------------------------------------------------
    img = new_img()
    base_metal(img, (132, 122, 112), (92, 84, 78))
    panel_lines(img, color=(58, 52, 48), step=8)
    rivets(img, color=(178, 166, 152))
    heat_stain(img, 0.5)
    made.append(save(img, "combustion_casing"))

    img = new_img()
    base_metal(img, (255, 214, 138), (255, 132, 40), grain=14)
    px = img.load()
    for y in range(S):
        for x in range(S):
            r, g, b, a = px[x, y]
            f = 0.75 + 0.25 * math.sin(x * 0.9) * math.cos(y * 0.7)
            px[x, y] = (_clamp(r * f), _clamp(g * f * 0.95), _clamp(b * f * 0.8), 255)
    made.append(save(img, "combustion_glow"))

    # --- afterburner -------------------------------------------------------
    img = new_img()
    base_metal(img, (96, 90, 88), (62, 58, 58))
    panel_lines(img, color=(38, 35, 35), step=8)
    heat_stain(img, 1.0)
    rivets(img, color=(150, 138, 128))
    made.append(save(img, "afterburner_casing"))

    img = new_img()
    base_metal(img, (170, 205, 255), (86, 120, 255), grain=16)
    made.append(save(img, "afterburner_glow"))

    img = new_img()
    base_metal(img, (120, 112, 108), (74, 70, 68))
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, S - 3, S - 3], outline=(190, 150, 90, 255), width=2)
    d.ellipse([9, 9, S - 10, S - 10], outline=(190, 150, 90, 255), width=1)
    heat_stain(img, 0.8)
    made.append(save(img, "flame_holder"))

    # --- nozzle ------------------------------------------------------------
    img = new_img()
    base_metal(img, (128, 120, 116), (86, 80, 78))
    panel_lines(img, color=(44, 40, 40), step=6)
    heat_stain(img, 1.2)
    made.append(save(img, "nozzle_petal"))

    img = new_img()
    base_metal(img, (58, 50, 48), (26, 22, 22), grain=8)
    heat_stain(img, 0.6)
    made.append(save(img, "nozzle_inner"))

    # --- dark intake depth --------------------------------------------------
    img = new_img()
    base_metal(img, (34, 36, 42), (12, 13, 16), grain=4)
    made.append(save(img, "intake_inner"))

    return made


# ---------------------------------------------------------------------------
# model geometry
# ---------------------------------------------------------------------------

ALL = ["north", "east", "south", "west", "up", "down"]


def faces(tex, uv=None, cull=None, tint=None, light=None):
    out = {}
    for f in ALL:
        d = {"texture": "#" + tex}
        if uv:
            d["uv"] = uv
        if cull == f or cull == "all":
            d["cullface"] = f
        out[f] = d
    return out


def elem(frm, to, tex, rotation=None, uv=None, shade=True, per_face=None):
    e = {"from": list(frm), "to": list(to), "faces": per_face or faces(tex, uv)}
    if rotation:
        e["rotation"] = rotation
    if not shade:
        e["shade"] = False
    return e


def rot(origin, axis, angle):
    return {"origin": list(origin), "axis": axis, "angle": angle}


def octagon_ring(r_out, r_in, z0, z1, tex):
    """
    Approximates a cylindrical casing with 4 axis-aligned slabs plus 4 slabs rotated
    45 degrees, giving an octagonal shell that reads as round from any angle.
    """
    c = 8.0
    es = []
    # axis-aligned top/bottom/left/right slabs
    es.append(elem((c - r_out, c + r_in, z0), (c + r_out, c + r_out, z1), tex))
    es.append(elem((c - r_out, c - r_out, z0), (c + r_out, c - r_in, z1), tex))
    es.append(elem((c - r_out, c - r_in, z0), (c - r_in, c + r_in, z1), tex))
    es.append(elem((c + r_in, c - r_in, z0), (c + r_out, c + r_in, z1), tex))
    # 45-degree corner slabs, rotated about the engine axis (Z)
    d = (r_out + r_in) / 2.0
    t = (r_out - r_in) / 2.0
    for ang, off in ((45, (0, d)), (45, (0, -d)), (-45, (0, d)), (-45, (0, -d))):
        es.append(elem(
            (c - r_out * 0.78, c + off[1] - t, z0),
            (c + r_out * 0.78, c + off[1] + t, z1),
            tex, rotation=rot((c, c, (z0 + z1) / 2.0), "z", ang)))
    return es


def blade_disc(radius, thickness, z_center, tex, count8=True):
    """
    Eight blades: four axis-aligned, four rotated 45 degrees about the engine axis.
    The whole disc is spun by the block entity renderer.
    """
    c = 8.0
    z0, z1 = z_center - thickness / 2.0, z_center + thickness / 2.0
    es = []
    # vertical + horizontal blade pairs
    es.append(elem((c - 0.6, c, z0), (c + 0.6, c + radius, z1), tex))
    es.append(elem((c - 0.6, c - radius, z0), (c + 0.6, c, z1), tex))
    es.append(elem((c, c - 0.6, z0), (c + radius, c + 0.6, z1), tex))
    es.append(elem((c - radius, c - 0.6, z0), (c, c + 0.6, z1), tex))
    if count8:
        for ang in (45, -45):
            es.append(elem((c - 0.6, c, z0), (c + 0.6, c + radius, z1), tex,
                           rotation=rot((c, c, z_center), "z", ang)))
            es.append(elem((c - 0.6, c - radius, z0), (c + 0.6, c, z1), tex,
                           rotation=rot((c, c, z_center), "z", ang)))
    return es


def model(textures, elements, parent=None, ao=True):
    m = {}
    if parent:
        m["parent"] = parent
    m["textures"] = textures
    m["elements"] = elements
    if not ao:
        m["ambientocclusion"] = False
    return m


# ---------------------------------------------------------------------------
# the five modules  (authored facing NORTH: exhaust toward -Z)
# ---------------------------------------------------------------------------

def build_models():
    out = {}

    # ---------------- FAN --------------------------------------------------
    # Intake at +Z (rear/upstream), exhaust toward -Z.
    els = []
    els += octagon_ring(8.0, 6.0, 0, 16, "casing")          # outer casing
    els += octagon_ring(6.0, 5.0, 12, 16, "fan_hub")        # intake lip
    els.append(elem((3, 3, 1), (13, 13, 3), "intake_inner"))  # dark depth plate
    # rear stator vanes (static)
    els += blade_disc(5.0, 0.6, 3.0, "compressor_blade", count8=False)
    out["fan_module"] = model({"casing": f"{MODID}:block/casing",
                               "fan_hub": f"{MODID}:block/fan_hub",
                               "intake_inner": f"{MODID}:block/intake_inner",
                               "compressor_blade": f"{MODID}:block/compressor_blade",
                               "particle": f"{MODID}:block/casing"}, els, ao=False)

    # rotating rotor: spinner cone + 8 blades, drawn by the BER
    rotor = []
    rotor += blade_disc(5.2, 1.0, 11.0, "fan_blade")
    rotor.append(elem((6.5, 6.5, 9.5), (9.5, 9.5, 14.5), "fan_hub"))   # hub
    rotor.append(elem((7.2, 7.2, 14.0), (8.8, 8.8, 15.6), "fan_hub"))  # spinner tip
    out["fan_rotor"] = model({"fan_blade": f"{MODID}:block/fan_blade",
                              "fan_hub": f"{MODID}:block/fan_hub",
                              "particle": f"{MODID}:block/fan_hub"}, rotor, ao=False)

    # ---------------- COMPRESSOR ------------------------------------------
    els = []
    els += octagon_ring(8.0, 5.5, 0, 16, "compressor_casing")
    els.append(elem((6.8, 6.8, 0), (9.2, 9.2, 16), "fan_hub"))  # shaft
    # static stator rings between rotating stages
    els += blade_disc(5.0, 0.5, 4.0, "compressor_blade", count8=False)
    els += blade_disc(5.0, 0.5, 12.0, "compressor_blade", count8=False)
    out["compressor_module"] = model({"compressor_casing": f"{MODID}:block/compressor_casing",
                                      "compressor_blade": f"{MODID}:block/compressor_blade",
                                      "fan_hub": f"{MODID}:block/fan_hub",
                                      "particle": f"{MODID}:block/compressor_casing"}, els, ao=False)

    rotor = []
    rotor += blade_disc(4.8, 0.8, 8.0, "compressor_blade")
    rotor += blade_disc(4.4, 0.8, 2.0, "compressor_blade")
    rotor += blade_disc(4.4, 0.8, 14.0, "compressor_blade")
    out["compressor_rotor"] = model({"compressor_blade": f"{MODID}:block/compressor_blade",
                                     "particle": f"{MODID}:block/compressor_blade"}, rotor, ao=False)

    # ---------------- COMBUSTION CORE -------------------------------------
    els = []
    els += octagon_ring(8.0, 6.0, 0, 16, "combustion_casing")
    # structural ribs
    for z in (2.0, 8.0, 14.0):
        els += octagon_ring(8.3, 7.6, z - 0.7, z + 0.7, "casing_plain")
    # fuel manifold detail
    els.append(elem((1.2, 7.2, 3), (2.6, 8.8, 13), "casing_plain"))
    els.append(elem((13.4, 7.2, 3), (14.8, 8.8, 13), "casing_plain"))
    els.append(elem((7.2, 13.4, 3), (8.8, 14.8, 13), "casing_plain"))
    els.append(elem((6.8, 6.8, 0), (9.2, 9.2, 16), "fan_hub"))  # shaft continuation
    out["combustion_core"] = model({"combustion_casing": f"{MODID}:block/combustion_casing",
                                    "casing_plain": f"{MODID}:block/casing_plain",
                                    "fan_hub": f"{MODID}:block/fan_hub",
                                    "particle": f"{MODID}:block/combustion_casing"}, els, ao=False)

    # inner glowing combustion volume, scaled by the BER with spool/throttle
    out["combustion_glow"] = model(
        {"combustion_glow": f"{MODID}:block/combustion_glow",
         "particle": f"{MODID}:block/combustion_glow"},
        [elem((4.5, 4.5, 2.0), (11.5, 11.5, 14.0), "combustion_glow", shade=False)], ao=False)

    # ---------------- AFTERBURNER -----------------------------------------
    els = []
    els += octagon_ring(8.0, 6.2, 0, 16, "afterburner_casing")
    els += octagon_ring(6.2, 4.6, 3.0, 4.4, "flame_holder")   # flame holder rings
    els += octagon_ring(6.2, 4.6, 9.0, 10.4, "flame_holder")
    # fuel spray bars
    els.append(elem((2.0, 7.4, 6.0), (14.0, 8.6, 7.0), "casing_plain"))
    els.append(elem((7.4, 2.0, 6.0), (8.6, 14.0, 7.0), "casing_plain"))
    out["afterburner_module"] = model({"afterburner_casing": f"{MODID}:block/afterburner_casing",
                                       "flame_holder": f"{MODID}:block/flame_holder",
                                       "casing_plain": f"{MODID}:block/casing_plain",
                                       "particle": f"{MODID}:block/afterburner_casing"}, els, ao=False)

    out["afterburner_glow"] = model(
        {"afterburner_glow": f"{MODID}:block/afterburner_glow",
         "particle": f"{MODID}:block/afterburner_glow"},
        [elem((5.0, 5.0, 1.0), (11.0, 11.0, 15.0), "afterburner_glow", shade=False)], ao=False)

    # ---------------- NOZZLE ----------------------------------------------
    els = []
    els += octagon_ring(8.0, 6.4, 10.0, 16.0, "nozzle_petal")   # fixed collar
    els.append(elem((3.4, 3.4, 12.0), (12.6, 12.6, 13.0), "nozzle_inner"))
    out["nozzle_module"] = model({"nozzle_petal": f"{MODID}:block/nozzle_petal",
                                  "nozzle_inner": f"{MODID}:block/nozzle_inner",
                                  "particle": f"{MODID}:block/nozzle_petal"}, els, ao=False)

    # one petal, instanced 8x and hinged open/closed by the BER
    out["nozzle_petal"] = model(
        {"nozzle_petal": f"{MODID}:block/nozzle_petal",
         "nozzle_inner": f"{MODID}:block/nozzle_inner",
         "particle": f"{MODID}:block/nozzle_petal"},
        [elem((6.4, 11.0, 0.0), (9.6, 13.4, 6.5), "nozzle_petal")], ao=False)

    # exhaust tunnel interior
    out["nozzle_inner"] = model(
        {"nozzle_inner": f"{MODID}:block/nozzle_inner",
         "particle": f"{MODID}:block/nozzle_inner"},
        [elem((4.6, 4.6, 0.0), (11.4, 11.4, 11.0), "nozzle_inner", shade=False)], ao=False)

    return out


# ---------------------------------------------------------------------------
# blockstates / item models / bbmodel
# ---------------------------------------------------------------------------

FACING_ROT = {
    "north": {},
    "east": {"y": 90},
    "south": {"y": 180},
    "west": {"y": 270},
    "up": {"x": 270},
    "down": {"x": 90},
}


def build_blockstate(model_name):
    variants = {}
    for facing, r in FACING_ROT.items():
        v = {"model": f"{MODID}:block/{model_name}"}
        v.update(r)
        variants[f"facing={facing}"] = v
    return {"variants": variants}


def to_bbmodel(name, m):
    """Emit an editable Blockbench project from the same element data."""
    textures = []
    tex_index = {}
    for i, (key, path) in enumerate(sorted(m["textures"].items())):
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
            f[face] = {"uv": fd.get("uv", [0, 0, 16, 16]),
                       "texture": tex_index.get(key, 0)}
        el = {"name": f"part_{i}", "from": e["from"], "to": e["to"],
              "autouv": 0, "color": i % 8, "visibility": True,
              "uuid": f"10000000-0000-4000-8000-{i:012d}", "faces": f}
        if "rotation" in e:
            el["rotation"] = [0, 0, 0]
            axis = e["rotation"]["axis"]
            idx = {"x": 0, "y": 1, "z": 2}[axis]
            el["rotation"][idx] = e["rotation"]["angle"]
            el["origin"] = e["rotation"]["origin"]
        elements.append(el)

    return {
        "meta": {"format_version": "4.5", "model_format": "java_block", "box_uv": False},
        "name": name,
        "parent": "",
        "ambientocclusion": m.get("ambientocclusion", True),
        "front_gui_light": False,
        "visible_box": [1, 1, 0],
        "variable_placeholders": "", "variable_placeholder_buttons": [],
        "resolution": {"width": S, "height": S},
        "elements": elements,
        "outliner": [e["uuid"] for e in elements],
        "textures": textures,
    }


BLOCK_MODULES = ["fan_module", "compressor_module", "combustion_core",
                 "afterburner_module", "nozzle_module"]


def main():
    tex = build_textures()
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

    print(f"textures: {len(tex)}")
    print(f"models:   {len(models)}")
    print(f"states:   {len(BLOCK_MODULES)}")
    print(f"bbmodels: {len(models)} -> assets_src/")


if __name__ == "__main__":
    main()
