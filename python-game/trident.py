"""
TRIDENT - a modern special-forces shooter written in pure Python.

You play a Navy SEAL operator clearing a facility room by room. It is a
first person shooter with mouse aiming, built with nothing but the Python
standard library, so it runs on any normal Python install (including IDLE
at school) with no downloads and no `pip`.

    HOW TO RUN IN IDLE
    ------------------
    1. Open this file in IDLE  (File -> Open...)
    2. Press F5  (or Run -> Run Module)
    3. Click inside the game window to take control of the mouse.

    CONTROLS
    --------
    MOUSE ................. aim  (click the window to capture it)
    LEFT MOUSE or Space ... fire
    RIGHT MOUSE ........... aim down the sight - zooms in and steadies you
    W / S or Up / Down .... move forward / backward
    A / D ................. strafe (step sideways)
    Q / E ................. lean left / right round a corner
    Left / Right .......... turn without the mouse
    Shift ................. sprint
    M ..................... turn mouse aiming on / off
    Tab ................... toggle the tac-map
    R ..................... restart the mission
    N ..................... next mission (after you clear one)
    Esc ................... release the mouse, or quit if it is already free

    THE MISSION
    -----------
    Neutralise every hostile in the compound, then reach the green
    extraction beacon. Grab medkits and rifle magazines on the way.

    The hostiles are a fictional armed group - masked figures in plain
    tactical gear. There is deliberately nothing identifying about them.

HOW THE 3D EFFECT WORKS (the short version)
-------------------------------------------
The world is really a flat 2D grid of characters - a "map" made of text.
There is no 3D geometry at all. To draw the view we use "raycasting":

    For each vertical stripe of the screen, shoot an imaginary ray out
    from the player until it bumps into a wall. Measure how far it
    travelled. Near walls get a TALL stripe, far walls get a SHORT one.

Draw 320 stripes side by side and your brain reads it as a 3D corridor.
This is exactly the trick Wolfenstein 3D used in 1992, and it is fast
enough to run in plain Python.

Written for a school project.  See README.md for a full write-up.
"""

import math
import random
import time
import tkinter as tk


# ===========================================================================
#  SECTION 1 - SETTINGS
#  Tweak these numbers to change how the game looks and feels.
# ===========================================================================

SCREEN_W = 640          # width of the 3D view, in pixels
SCREEN_H = 400          # height of the 3D view, in pixels
HUD_H = 78              # height of the status bar underneath the view

# How many rays we shoot per frame. This is the single most important
# performance dial. 320 = one ray every 2 pixels (sharp). If the game feels
# slow on your computer, drop it to 160 or 128 - it will still look fine.
NUM_COLUMNS = 320

FOV_DEGREES = 66.0      # field of view, like a camera lens. Doom used ~90.
MAX_DEPTH = 26.0        # rays give up after this many grid squares

TARGET_FPS = 30
FRAME_MS = int(1000 / TARGET_FPS)

# Movement, measured in grid squares per second.
WALK_SPEED = 2.9
RUN_SPEED = 4.7
STRAFE_SCALE = 0.78     # strafing is slightly slower than walking forward
TURN_SPEED = 2.6        # radians per second

PLAYER_RADIUS = 0.22    # stops you clipping into wall corners
PLAYER_MAX_HP = 100
START_AMMO = 90         # rounds for the carbine
MAX_AMMO = 180
MAG_SIZE = 30           # rounds in one magazine, used by the HUD readout

# Mouse aiming
MOUSE_SENSITIVITY = 0.0016   # radians of turn per pixel of mouse movement
MOUSE_INVERT_Y = False
PITCH_SENSITIVITY = 1.0      # vertical responds exactly like horizontal
# How far up and down you can look, measured in pixels of screen shift. The
# view is 400px tall and covers about 44 degrees, so 150px is around 16.
#
# The two limits differ on purpose. Shearing the picture upwards slides the
# carbine down with the horizon, and past about 100px there is none of it
# left on screen. Looking down has no such problem - it brings MORE of the
# weapon into view - so it gets the longer leash. In practice looking down is
# the more useful direction anyway: bodies, dropped magazines and the
# extraction pad are all on the floor.
MAX_PITCH_UP = 100
MAX_PITCH_DOWN = 150
MAX_PITCH = MAX_PITCH_DOWN      # the larger of the two, for sizing the sky

# Aiming down the sight (hold the right mouse button)
ADS_FOV_DEGREES = 38.0  # narrower view = magnified
ADS_SPEED = 9.0         # how fast the sight comes up, in "per second"
ADS_SENSITIVITY = 0.55  # the mouse slows down while you are zoomed in
ADS_MOVE_SCALE = 0.48   # so does your walk
ADS_SPREAD_SCALE = 0.18 # and the rifle tightens right up
ADS_SQUEEZE_X = 0.42    # how far the weapon narrows as it lines up with you
ADS_SQUEEZE_Y = 0.78    # and how much it shortens

# The sight picture: looking through the optic, you see a dark tube with a
# ring of housing around the outside and a red dot floating in the middle.
SIGHT_RADIUS = 92       # radius of the clear glass, in pixels
SIGHT_RING_WIDTH = 40   # thickness of the housing around it
SIGHT_SHOW_AT = 0.40    # how far into the ADS movement the tube appears
ADS_GUN_DROP = SIGHT_RADIUS + 26   # how far the weapon sits below the tube

# Hit marker: a small X that flicks up on the reticle when a round connects.
HITMARK_TIME = 0.13
LOW_AMMO = 10           # rounds left before the counter starts warning you

# Weapon sway. A real rifle has weight: swing the view and it lags behind,
# then settles. SWAY_TURN is how many pixels it trails per radian you turn.
SWAY_TURN = 90.0
SWAY_PITCH = 0.34       # the same idea for looking up and down
SWAY_RETURN = 8.0       # how fast it catches back up, in "per second"
SWAY_MAX = 30.0         # it never trails further than this
SWAY_BREATH = 2.4       # gentle drift while you stand still
ADS_SWAY_SCALE = 0.25   # shouldered against your body, it barely moves
RETICLE_CLEARANCE = 16  # pixels of clear air kept between muzzle and reticle

# Leaning around corners (Q and E)
LEAN_DISTANCE = 0.42    # how far out you peek, in grid squares
LEAN_SPEED = 7.0        # how fast you lean, in "per second"
LEAN_GUN_SHIFT = 26     # pixels the carbine slides across as you lean

# Blood
BLOOD_ON_HIT = 9        # specks thrown up by one round
BLOOD_ON_DEATH = 22
BLOOD_GRAVITY = 5.2
BLOOD_LIFETIME = 0.75
MAX_BLOOD = 90          # size of the particle pool
POOL_GROW_TIME = 1.2    # seconds for a pool under a body to spread out

# M4 carbine: fully automatic, one accurate round at a time.
SHOT_COOLDOWN = 0.105   # seconds between rounds - about 570 rounds per minute
SHOT_PELLETS = 1        # a rifle fires one bullet, not a spread
SHOT_SPREAD = 0.012     # radians of scatter, so long shots need real aim
SHOT_DAMAGE = 24
SHOT_RANGE = 22.0

# Fog / lighting. Walls fade towards black as they get further away.
FOG_NEAR = 1.0
FOG_FAR = 17.0
SHADE_LEVELS = 32       # how many brightness steps we pre-compute


# ===========================================================================
#  SECTION 2 - COLOURS AND SHADING
#
#  Tkinter wants colours as strings like "#ff8800". Building those strings
#  every single frame would be slow, so instead we build every brightness of
#  every colour ONCE at start-up and just look them up later.
# ===========================================================================

def make_shade_table(rgb, dimmest=0.16):
    """Return SHADE_LEVELS colour strings, from almost black up to full rgb."""
    red, green, blue = rgb
    table = []
    for step in range(SHADE_LEVELS):
        # brightness goes from `dimmest` (far away) up to 1.0 (right in front)
        brightness = dimmest + (1.0 - dimmest) * (step / (SHADE_LEVELS - 1))
        table.append("#%02x%02x%02x" % (int(red * brightness),
                                        int(green * brightness),
                                        int(blue * brightness)))
    return tuple(table)


def dim(rgb, factor):
    """Multiply an (r, g, b) tuple by a factor, e.g. dim(c, 0.7) is darker."""
    return (int(rgb[0] * factor), int(rgb[1] * factor), int(rgb[2] * factor))


def shade_index(distance):
    """Turn a distance into a brightness step (0 = pitch black, 15 = bright)."""
    if distance <= FOG_NEAR:
        return SHADE_LEVELS - 1
    if distance >= FOG_FAR:
        return 0
    fraction = (distance - FOG_NEAR) / (FOG_FAR - FOG_NEAR)
    return int((1.0 - fraction) * (SHADE_LEVELS - 1))


# Every character that counts as a solid wall, and the colour it is painted.
WALL_COLOURS = {
    "#": (146, 146, 150),   # bare concrete
    "O": ( 96, 104,  70),   # olive drab painted wall
    "S": (108, 124, 136),   # steel bulkhead
    "R": (124,  74,  50),   # rusted plate
    "Y": (176, 152,  54),   # yellow hazard panel
}

# Two shade tables per wall type. Walls we hit on a north/south face are drawn
# slightly darker than east/west faces - that fake "sunlight" is what makes
# the corners of the corridor pop out instead of looking like flat wallpaper.
WALL_SHADES_BRIGHT = {c: make_shade_table(rgb) for c, rgb in WALL_COLOURS.items()}
WALL_SHADES_DARK = {c: make_shade_table(dim(rgb, 0.66)) for c, rgb in WALL_COLOURS.items()}

CEILING_RGB = (34, 38, 50)      # night sky / dark ceiling
FLOOR_RGB = (62, 62, 60)        # poured concrete


# ---------------------------------------------------------------------------
#  WALL TEXTURE
#
#  Sampling a real bitmap texture per pixel is out of reach in pure Python -
#  it is roughly a hundred thousand extra operations a frame. So walls are
#  textured a cheaper way, in two halves:
#
#  ACROSS the wall, detail is FREE. The raycaster already tells us where on
#  the wall face the ray landed, so we just look that position up in a table
#  of brightness offsets and nudge the shade. That buys vertical seams,
#  mortar joints and panel edges for the cost of one list lookup.
#
#  DOWN the wall, detail costs rectangles. Each column is split into a few
#  horizontal bands with their own brightness, giving courses of brick, the
#  lip of a steel panel, a strip of rust. Bands are measured 0 (top of the
#  wall) to 1 (bottom), so they stay glued to the wall at any distance.
#
#  The numbers are brightness *steps*, added to the shade index - positive is
#  lighter, negative is darker.
# ---------------------------------------------------------------------------

#                  v0    v1   step
WALL_BANDS = {
    "#": (  # concrete: shuttering seams a third of the way down
        (0.00, 0.04,  3), (0.04, 0.34,  0), (0.34, 0.38, -4),
        (0.38, 0.70,  1), (0.70, 0.74, -4), (0.74, 0.96,  0),
        (0.96, 1.00, -5),
    ),
    "O": (  # painted breeze block: chunky courses
        (0.00, 0.05,  2), (0.05, 0.30,  0), (0.30, 0.35, -5),
        (0.35, 0.60,  1), (0.60, 0.65, -5), (0.65, 0.95,  0),
        (0.95, 1.00, -5),
    ),
    "S": (  # steel bulkhead: bright top lip, ribbed middle, dark skirt
        (0.00, 0.07,  4), (0.07, 0.12, -3), (0.12, 0.44,  1),
        (0.44, 0.50, -4), (0.50, 0.82,  1), (0.82, 0.88, -3),
        (0.88, 1.00, -5),
    ),
    "R": (  # rusted plate: blotchy, darker towards the bottom
        (0.00, 0.06,  2), (0.06, 0.28, -1), (0.28, 0.33, -4),
        (0.33, 0.58,  1), (0.58, 0.66, -3), (0.66, 0.92, -1),
        (0.92, 1.00, -5),
    ),
    "Y": (  # hazard panel: bold stripes
        (0.00, 0.06,  3), (0.06, 0.26, -6), (0.26, 0.46,  2),
        (0.46, 0.66, -6), (0.66, 0.86,  2), (0.86, 0.94, -6),
        (0.94, 1.00, -4),
    ),
}

MAX_BANDS = max(len(bands) for bands in WALL_BANDS.values())

# Anything shorter than this on screen is too far away for the banding to
# read as anything but noise, so distant walls collapse to a single stripe.
# That also keeps the cost down, because most columns in view are distant.
BAND_MIN_HEIGHT = 30
FLAT_BAND = ((0.0, 1.0, 0),)

SEAM_STEPS = 32         # how finely we sample across the wall face

# ---------------------------------------------------------------------------
#  THE CARBINE
#
#  A rifle held in first person does not sit upright in the middle of the
#  screen - it runs diagonally, butt down by your shoulder on the right, muzzle
#  up and away towards the centre. Canvas rectangles cannot be rotated, so the
#  weapon is built as a STAIRCASE: each length of it is a short stack of blocks
#  that each step a little to the side, and at this size the steps read as a
#  clean slope rather than as stairs.
#
#  Everything is measured from the bottom centre of the view, and negative y is
#  further up the screen. The muzzle stops at -180 so there is always clear air
#  between it and the reticle.
# ---------------------------------------------------------------------------

GUN_MUZZLE_X, GUN_MUZZLE_Y = 6, -180     # tip of the flash hider
GUN_BUTT_X, GUN_BUTT_Y = 74, 30          # bottom of the buttstock


def gun_axis(y):
    """Where the centre-line of the weapon sits at this height."""
    along = (y - GUN_MUZZLE_Y) / (GUN_BUTT_Y - GUN_MUZZLE_Y)
    return GUN_MUZZLE_X + (GUN_BUTT_X - GUN_MUZZLE_X) * along


def gun_run(y_top, y_bottom, half_width, colour, steps=5, sideways=0.0,
            drift=0.0):
    """
    One length of the weapon, as a short staircase along its axis.

    `sideways` shifts the whole run off the centre-line, which is how the
    highlights down one edge and the shadows down the other are made. `drift`
    leans the run away from the axis as it goes - the magazine and the pistol
    grip both hang off at their own angle rather than following the barrel.
    """
    parts = []
    span = (y_bottom - y_top) / steps
    for step in range(steps):
        y0 = y_top + step * span
        y1 = y0 + span + 1              # +1 so blocks overlap, no hairlines
        fraction = (step + 0.5) / steps
        centre = gun_axis((y0 + y1) * 0.5) + sideways + drift * fraction
        parts.append((centre - half_width, y0, centre + half_width, y1, colour))
    return parts


def build_carbine():
    """The whole weapon, back to front the way a painter would lay it down."""
    poly, poly_hi = "#2a2c31", "#40444b"        # black polymer furniture
    alu, alu_hi, alu_lo = "#41454c", "#5b606a", "#2c2f34"    # receiver
    steel, steel_hi = "#25272b", "#767c86"      # barrel and gas block
    slot = "#17191c"
    optic, optic_hi, glass = "#2d3035", "#474b53", "#6d2a26"

    parts = []
    # buttstock, wide and low by your shoulder
    parts += gun_run(-24, 30, 23, poly, steps=4)
    parts += gun_run(-24, 30, 7, poly_hi, steps=4, sideways=-11)
    parts += gun_run(-24, 30, 5, alu_lo, steps=4, sideways=15)

    # receiver extension
    parts += gun_run(-54, -20, 12, poly, steps=3)
    parts += gun_run(-54, -20, 4, poly_hi, steps=3, sideways=-6)

    # pistol grip, angling back and down out of the receiver
    parts += gun_run(-44, 14, 10, poly, steps=4, sideways=6, drift=12)
    parts += gun_run(-44, 14, 3, poly_hi, steps=4, sideways=1, drift=12)

    # magazine, angling forward and down, curving as it goes
    parts += gun_run(-46, 40, 12, poly, steps=5, sideways=-14, drift=-13)
    parts += gun_run(-46, 40, 4, poly_hi, steps=5, sideways=-22, drift=-13)
    parts += gun_run(30, 44, 14, alu_lo, steps=2, sideways=-27)      # floorplate

    # lower and upper receiver - the solid middle of the weapon
    parts += gun_run(-104, -46, 19, alu, steps=5)
    parts += gun_run(-104, -46, 5, alu_hi, steps=5, sideways=-13)
    parts += gun_run(-104, -46, 5, alu_lo, steps=5, sideways=14)
    parts += gun_run(-92, -70, 9, alu_lo, steps=2, sideways=6)       # ejection port
    parts += gun_run(-112, -100, 17, steel, steps=2)                 # charging handle
    parts += gun_run(-110, -104, 14, slot, steps=2)                  # rail

    # handguard, with the rail slots cut into it
    parts += gun_run(-162, -106, 16, poly, steps=5)
    parts += gun_run(-162, -106, 4, poly_hi, steps=5, sideways=-10)
    for slot_y in (-152, -140, -128, -116):
        parts += gun_run(slot_y, slot_y + 4, 11, slot, steps=1)

    # gas block, front sight, muzzle
    parts += gun_run(-172, -158, 11, steel, steps=2)
    parts += gun_run(-178, -168, 5, steel, steps=1)                  # sight post
    parts += gun_run(-180, -170, 9, steel_hi, steps=1)               # flash hider

    # optic last, so it sits on top of the receiver
    parts += gun_run(-138, -106, 14, optic, steps=3)
    parts += gun_run(-138, -130, 14, optic_hi, steps=1)
    parts += gun_run(-132, -112, 9, glass, steps=2)
    return parts


CARBINE_PARTS = build_carbine()

# Where the middle of the optic ends up, so aiming down the sight can line it
# up with the aim point. Read off the axis rather than typed in by hand, so it
# stays correct if the weapon is ever redrawn.
OPTIC_CENTRE_Y = -120
OPTIC_CENTRE_X = round(gun_axis(OPTIC_CENTRE_Y))

# Anything higher up the weapon than this counts as being in front of the
# optic, and folds away when you look down the sight.
GUN_FORWARD_OF_OPTIC = OPTIC_CENTRE_Y - 14



def make_seam_table(joints, depth=-5, width=0.035):
    """
    Brightness offsets across one wall square.

    `joints` are positions from 0 to 1 where a groove runs down the wall.
    Everything else is left alone. Sampled into a small lookup table so the
    render loop only does an integer index.
    """
    table = []
    for step in range(SEAM_STEPS):
        position = step / SEAM_STEPS
        offset = 0
        for joint in joints:
            gap = abs(position - joint)
            gap = min(gap, 1.0 - gap)       # the wall wraps around
            if gap < width:
                offset = depth
        table.append(offset)
    return tuple(table)


WALL_SEAMS = {
    "#": make_seam_table((0.0, 0.5), depth=-3),
    "O": make_seam_table((0.0, 0.25, 0.5, 0.75), depth=-5),
    "S": make_seam_table((0.0,), depth=-4, width=0.05),
    "R": make_seam_table((0.0, 0.5), depth=-2),
    "Y": make_seam_table((0.0,), depth=-3),
}
SKY_BANDS = 8           # the ceiling/floor gradient is drawn as N flat bands


# ===========================================================================
#  SECTION 3 - SPRITES (monsters and pickups)
#
#  A sprite is described as a little list of coloured boxes in "unit space",
#  where (0, 0) is its top-left corner and (1, 1) is its bottom-right. At draw
#  time we scale that unit square to however big the sprite should appear.
#
#  Order matters: things listed first are drawn first, so they end up BEHIND
#  the things listed after them. Legs and arms first, eyes last.
# ===========================================================================

# A hostile: a masked figure in plain tactical gear. Deliberately generic -
# a balaclava, a chest rig and a weapon, and nothing that identifies it as
# belonging to any real country, group or people.
#
#            shape    x0    y0    x1    y1   colour key
HOSTILE = (
    ("rect", 0.30, 0.66, 0.45, 1.00, "limb"),   # left leg
    ("rect", 0.55, 0.66, 0.70, 1.00, "limb"),   # right leg
    ("rect", 0.05, 0.30, 0.24, 0.66, "limb"),   # left arm
    ("rect", 0.76, 0.30, 0.95, 0.66, "limb"),   # right arm
    ("rect", 0.21, 0.23, 0.79, 0.70, "body"),   # torso
    ("oval", 0.30, 0.00, 0.70, 0.28, "head"),   # balaclava
    ("rect", 0.31, 0.10, 0.69, 0.17, "visor"),  # goggles
    ("rect", 0.29, 0.31, 0.71, 0.52, "rig"),    # chest rig / plate carrier
    ("rect", 0.02, 0.44, 0.44, 0.51, "gun"),    # weapon held across the body
)

# Every sprite slot on screen owns this fixed run of shapes, so the shapes at
# each index must match between sprite types. (Canvas items cannot change from
# a rectangle into an oval once created, so we lock the layout in here.)
SPRITE_SHAPES = ("rect", "rect", "rect", "rect", "rect", "oval", "rect", "rect", "rect")

MONSTERS = {
    "t": {
        # Rushes you with a submachine gun. Dangerous up close, poor at range.
        "name": "Tango",
        "max_hp": 46,
        "speed": 2.3,
        "radius": 0.30,
        "sight": 10.0,
        "score": 100,
        "parts": HOSTILE,
        "width_ratio": 0.60,        # how wide the sprite is vs. how tall
        "height_scale": 0.88,       # 0.88 of a full wall's height
        "attack_cooldown": 0.9,
        "melee_damage": 7,
        "melee_range": 1.3,
        "ranged": True,
        "ranged_damage": 4,
        "ranged_range": 5.5,        # only accurate at short range
        "keep_distance": 0.0,       # closes the distance on you
        "colours": {
            "body": ( 52,  54,  58), "limb": ( 40,  42,  46),
            "head": ( 30,  30,  34), "visor": (214, 138,  46),
            "rig":  ( 96,  90,  70), "gun":  ( 34,  34,  38),
        },
    },
    "m": {
        # Holds position at range and puts accurate fire on you.
        "name": "Marksman",
        "max_hp": 62,
        "speed": 1.55,
        "radius": 0.32,
        "sight": 13.0,
        "score": 150,
        "parts": HOSTILE,
        "width_ratio": 0.62,
        "height_scale": 0.92,
        "attack_cooldown": 1.6,
        "melee_damage": 6,
        "melee_range": 1.1,
        "ranged": True,
        "ranged_damage": 7,
        "ranged_range": 14.0,
        "keep_distance": 6.0,       # backs off to keep you at rifle range
        "colours": {
            "body": ( 62,  66,  48), "limb": ( 46,  50,  36),
            "head": ( 34,  36,  30), "visor": (198,  74,  54),
            "rig":  (104,  98,  74), "gun":  ( 30,  30,  34),
        },
    },
}

MEDIKIT = (
    ("rect", 0.00, 0.00, 1.00, 1.00, "box"),
    ("rect", 0.41, 0.14, 0.59, 0.86, "mark"),
    ("rect", 0.14, 0.41, 0.86, 0.59, "mark"),
)

# A rifle magazine standing on its base.
AMMO_MAG = (
    ("rect", 0.28, 0.00, 0.72, 0.30, "mark"),
    ("rect", 0.22, 0.26, 0.78, 1.00, "box"),
    ("rect", 0.34, 0.40, 0.48, 0.90, "mark"),
)

PICKUP_SHAPES = ("rect", "rect", "rect")

# The extraction point: a marker panel with a green chemlight standing on it,
# so you can spot it from across the compound instead of hunting the tac-map.
EXIT_PAD = (
    ("rect", 0.00, 0.62, 1.00, 1.00, "pad"),
    ("rect", 0.14, 0.34, 0.86, 0.68, "glow"),
    ("rect", 0.34, 0.00, 0.66, 0.40, "beam"),
)

EXIT_SPRITE = {
    "name": "exit", "parts": EXIT_PAD,
    "width_ratio": 1.35, "height_scale": 0.62,
    "colours": {"pad": (28, 108, 52), "glow": (64, 214, 96),
                "beam": (198, 255, 206)},
}

MAX_EXIT_SLOTS = 4

PICKUPS = {
    "h": {
        "name": "medkit", "parts": MEDIKIT, "heal": 25, "ammo": 0,
        "width_ratio": 1.0, "height_scale": 0.30,
        "colours": {"box": (216, 220, 222), "mark": (198, 44, 44)},
    },
    "a": {
        "name": "magazine", "parts": AMMO_MAG, "heal": 0, "ammo": MAG_SIZE,
        "width_ratio": 0.55, "height_scale": 0.30,
        "colours": {"box": (58, 62, 54), "mark": (128, 132, 118)},
    },
}

# Pre-shade every sprite colour, the same way we did for walls.
SPRITE_SHADES = {}
for _table in (MONSTERS, PICKUPS, {"X": EXIT_SPRITE}):
    for _key, _info in _table.items():
        SPRITE_SHADES[_key] = {name: make_shade_table(rgb, dimmest=0.16)
                               for name, rgb in _info["colours"].items()}
        # Goggle lenses catch the light, so they ignore the fog and stay
        # readable at any distance - it is what makes a hostile easy to spot.
        if "visor" in _info["colours"]:
            SPRITE_SHADES[_key]["visor"] = make_shade_table(
                _info["colours"]["visor"], dimmest=0.80)

MAX_SPRITE_SLOTS = 14   # most hostiles/pickups we will ever draw at once

# Blood: bright and wet in flight, dark and tacky once it has pooled.
BLOOD_SHADES = make_shade_table((178, 22, 24), dimmest=0.22)
BLOOD_POOL_SHADES = make_shade_table((96, 12, 14), dimmest=0.20)


# ===========================================================================
#  SECTION 4 - THE LEVELS
#
#  Each level is just a list of equal-length strings. One character = one
#  square of the world, one square = one "metre".
#
#      #  O  S  R  Y ... solid wall (concrete, olive, steel, rust, hazard)
#      .  ............. empty floor
#      P  ............. where the operator starts
#      t  ............. a Tango       (rushes you)
#      m  ............. a Marksman    (holds back and shoots)
#      h  ............. a medkit
#      a  ............. a rifle magazine
#      X  ............. the extraction point
#
#  Want to design your own level? Copy one of these, redraw it with a text
#  editor, and add it to the LEVELS list. Keep every row the same length and
#  keep a solid wall all the way around the outside.
# ===========================================================================

LEVELS = [
    {
        "name": "DOCKSIDE WAREHOUSE",
        "start_angle": 90,          # degrees; 90 means facing "down" the map
        "grid": [
            "########################",
            "#......#.........#.....#",
            "#..P...#....h....#..t..#",
            "#......#.........#.....#",
            "#......#..OOOOO..#.....#",
            "#......#..O...O..#..a..#",
            "#.........O.a.O........#",
            "#......#..O...O..#.....#",
            "#..a...#..OO.OO..#.....#",
            "#......#.........#.....#",
            "#......###########.....#",
            "#......#.........#.....#",
            "#..t...#..SSSSS..#..h..#",
            "#......#..S...S..#.....#",
            "#.........S.h.S........#",
            "#......#..S...S..#.....#",
            "#..h...#..SS.SS..#..t..#",
            "#......#.........#.....#",
            "#..m...#....X....#..m..#",
            "########################",
        ],
    },
    {
        "name": "REFINERY CORE",
        "start_angle": 90,
        "grid": [
            "########################",
            "#....##..........##....#",
            "#.P..##....RR....##..a.#",
            "#....##....RR....##....#",
            "#..........mm..........#",
            "#..SS..............SS..#",
            "#..SS.....t..t.....SS..#",
            "#......................#",
            "##.##....OOOOOO....##.##",
            "#........O....O........#",
            "#...h....O.XX.O....h...#",
            "#........O....O........#",
            "##.##....OO..OO....##.##",
            "#......................#",
            "#..SS.....t..t.....SS..#",
            "#..SS..............SS..#",
            "#..........mm..........#",
            "#....##....RR....##....#",
            "#.a..##....RR....##..a.#",
            "########################",
        ],
    },
]


# ===========================================================================
#  SECTION 5 - GAME OBJECTS
# ===========================================================================

class Monster:
    """One enemy. Knows where it is, how hurt it is, and what it is doing."""

    def __init__(self, kind, x, y):
        info = MONSTERS[kind]
        self.kind = kind
        self.info = info
        self.x = x
        self.y = y
        self.hp = info["max_hp"]
        self.alive = True
        self.awake = False          # has it spotted the player yet?
        self.attack_timer = 0.0     # counts down to its next attack
        self.hurt_flash = 0.0       # >0 means "draw me white, I just got shot"
        self.death_timer = 0.0      # how long ago it died (used for the fade)
        # A tiny random wobble so a pack of monsters does not walk in a
        # perfect single-file line towards you.
        self.wobble = random.uniform(-0.55, 0.55)
        self.wobble_timer = random.uniform(0.5, 2.0)


class Pickup:
    """A medikit or a box of shells sitting on the floor."""

    def __init__(self, kind, x, y):
        self.kind = kind
        self.info = PICKUPS[kind]
        self.x = x
        self.y = y
        self.taken = False


# ===========================================================================
#  SECTION 6 - THE GAME
# ===========================================================================

# The game is always in exactly one of these states.
STATE_TITLE = "title"
STATE_PLAYING = "playing"
STATE_DEAD = "dead"
STATE_CLEARED = "cleared"
STATE_WON = "won"


class Game:

    # -- set-up ------------------------------------------------------------

    def __init__(self, root):
        self.root = root
        root.title("TRIDENT")
        root.resizable(False, False)

        self.canvas = tk.Canvas(root, width=SCREEN_W, height=SCREEN_H + HUD_H,
                                bg="black", highlightthickness=0)
        self.canvas.pack()

        # Camera maths that never changes
        self.half_h = SCREEN_H * 0.5
        self.column_w = SCREEN_W / NUM_COLUMNS
        self.plane_len = math.tan(math.radians(FOV_DEGREES) * 0.5)
        self.ads = 0.0          # set before the first camera update

        # The depth buffer: how far away the wall is in each screen column.
        # Sprites use it to work out whether they are hidden behind a wall.
        self.zbuffer = [MAX_DEPTH] * NUM_COLUMNS

        self.keys = set()
        self._pending_release = {}
        self.mouse_enabled = True
        self.mouse_captured = False
        self.firing = False
        self.aiming = False         # right mouse button held
        self.sway_x = 0.0           # how far the weapon is trailing the view
        self.sway_y = 0.0
        self._turn_accum = 0.0      # view movement waiting to be turned into sway
        self._pitch_accum = 0.0
        self.breath = 0.0
        self.hitmark_timer = 0.0
        self._sight_visible = False
        self._hitmark_visible = False
        self.ads = 0.0              # 0 = hip fire, 1 = fully sighted in
        self.lean = 0.0             # -1 hard left, +1 hard right
        self.lean_applied = 0.0     # how much of it the walls allowed
        self._warping = False
        self.pitch = 0.0
        self.show_minimap = True
        self.frame_times = []
        self.fps = 0.0
        self.running = True
        self.horizon = self.half_h

        # Build every canvas item once, up front. During the game we only
        # ever MOVE and RECOLOUR them - creating and deleting thousands of
        # canvas items every frame would be far too slow.
        self._build_background()
        self._build_wall_columns()
        self._build_sprite_pool()
        self._build_weapon()
        self._build_overlay()
        self._build_hud()
        self._build_minimap()

        self._bind_keys()

        self.level_index = 0
        self.total_score = 0
        self.state = STATE_TITLE
        self.load_level(0)
        self._set_overlay("TRIDENT",
                          "MOUSE aim   CLICK fire   RIGHT-CLICK sight   QE lean",
                          "click the window to begin")

        self.last_time = time.perf_counter()
        self.tick()

    # -- building the canvas items ----------------------------------------

    def _build_background(self):
        """The ceiling and floor: a stack of flat bands faking a gradient."""
        self.bg_items = []          # (item_id, base_y0, base_y1)
        band_h = self.half_h / SKY_BANDS

        for i in range(SKY_BANDS):
            # i = 0 is the band touching the horizon (far away, so dark)
            fraction = i / (SKY_BANDS - 1)
            bright = 0.20 + 0.80 * (fraction ** 1.25)

            # The outermost band is stretched well past the edge of the screen.
            # The whole horizon slides up and down when you look around, and
            # without the overhang that would drag a bare gap into view.
            overhang = MAX_PITCH + 16

            y1 = self.half_h - i * band_h
            y0 = y1 - band_h - (overhang if i == SKY_BANDS - 1 else 0)
            colour = "#%02x%02x%02x" % dim(CEILING_RGB, bright)
            item = self.canvas.create_rectangle(0, y0, SCREEN_W, y1,
                                                fill=colour, outline="")
            self.bg_items.append((item, y0, y1))

            y0 = self.half_h + i * band_h
            y1 = y0 + band_h + (overhang if i == SKY_BANDS - 1 else 0)
            colour = "#%02x%02x%02x" % dim(FLOOR_RGB, bright)
            item = self.canvas.create_rectangle(0, y0, SCREEN_W, y1,
                                                fill=colour, outline="")
            self.bg_items.append((item, y0, y1))

    def _build_wall_columns(self):
        """
        The wall stripes.

        Each ray owns a small stack of rectangles rather than a single one, so
        a column can be painted as several horizontal bands - see the WALL
        TEXTURE notes up in Section 2. They are all made once here and only
        ever moved afterwards.
        """
        self.column_items = []
        self.column_fill = []       # each band's current colour
        self.column_used = []       # how many bands the column drew last frame
        for i in range(NUM_COLUMNS):
            x0 = i * self.column_w
            # +1 pixel of overlap so there are no hairline gaps between stripes
            x1 = x0 + self.column_w + 1
            self.column_items.append([
                self.canvas.create_rectangle(x0, 0, x1, 0,
                                             fill="#000000", outline="")
                for _ in range(MAX_BANDS)])
            self.column_fill.append(["#000000"] * MAX_BANDS)
            self.column_used.append(0)

    def _build_sprite_pool(self):
        """
        A fixed pool of reusable shapes for monsters, pickups and exits.

        Slot 0 was created first, so tkinter always draws it underneath slot 1,
        and so on. We take advantage of that: each frame we hand the FARTHEST
        sprite slot 0 and the NEAREST sprite the last slot, which makes near
        monsters correctly overlap far ones for free.

        The three pools are created in the order exits, pickups, monsters for
        the same reason - an exit pad is flat on the floor, so anything walking
        about in front of it should be drawn over the top of it.
        """
        self.exit_slots = []
        for _ in range(MAX_EXIT_SLOTS):
            slot = [self.canvas.create_rectangle(-40, -40, -30, -30,
                                                 fill="#000000", outline="")
                    for _ in EXIT_PAD]
            self.exit_slots.append(slot)

        self.pickup_slots = []
        for _ in range(MAX_SPRITE_SLOTS):
            slot = [self.canvas.create_rectangle(-40, -40, -30, -30,
                                                 fill="#000000", outline="")
                    for _ in PICKUP_SHAPES]
            self.pickup_slots.append(slot)

        self.monster_slots = []
        for _ in range(MAX_SPRITE_SLOTS):
            slot = []
            for shape in SPRITE_SHAPES:
                if shape == "oval":
                    item = self.canvas.create_oval(-40, -40, -30, -30,
                                                   fill="#000000", outline="")
                else:
                    item = self.canvas.create_rectangle(-40, -40, -30, -30,
                                                        fill="#000000", outline="")
                slot.append(item)
            self.monster_slots.append(slot)

        # Blood: a pool of small rectangles, plus the flat pools that spread
        # out under a body. Pools are created first so they stay on the floor,
        # underneath everything else.
        self.pool_items = [self.canvas.create_oval(-40, -40, -30, -30,
                                                   fill="#000000", outline="")
                           for _ in range(MAX_SPRITE_SLOTS)]
        self.blood_items = [self.canvas.create_rectangle(-40, -40, -30, -30,
                                                         fill="#000000",
                                                         outline="")
                            for _ in range(MAX_BLOOD)]
        self._blood_shown = 0       # how many were visible last frame
        self._pools_shown = 0

        # Which shapes are currently sitting on screen. Anything not in here is
        # already parked out of sight, so we never waste a call re-hiding it.
        self._on_screen = set()

    def _build_weapon(self):
        """The carbine held at the bottom of the screen, plus its muzzle flash."""
        # An M4-style carbine, built out of nothing but rectangles. Each one is
        # (x0, y0, x1, y1, colour) measured from the bottom-centre of the 3D
        # view, so negative y means "further up the screen". They are listed
        # back-to-front, the way a painter would lay them down.
        self.gun_parts = CARBINE_PARTS
        self.muzzle_y = -180        # where the flash appears
        # The highest point on the weapon, worked out from the model itself so
        # it stays right if the model is ever redrawn. Used to keep the
        # reticle clear of the barrel.
        self.gun_top = -min(part[1] for part in self.gun_parts)
        self.gun_items = [self.canvas.create_rectangle(0, 0, 0, 0,
                                                       fill=colour, outline="")
                          for *_, colour in self.gun_parts]

        # The sight picture, drawn over the weapon. A hollow ring stands in for
        # the optic housing you are looking through - tkinter can draw an oval
        # outline of any thickness, which is exactly the shape needed - and the
        # red dot floats in the clear middle.
        self.sight_ring = self.canvas.create_oval(
            0, 0, 0, 0, outline="#0b0d11", width=SIGHT_RING_WIDTH, fill="",
            state="hidden")
        self.sight_edge = self.canvas.create_oval(
            0, 0, 0, 0, outline="#565c66", width=2, fill="", state="hidden")
        self.dot_halo = self.canvas.create_oval(0, 0, 0, 0, fill="#7a1a14",
                                                outline="", state="hidden")
        self.dot_item = self.canvas.create_oval(0, 0, 0, 0, fill="#ff3b30",
                                                outline="", state="hidden")

        # Hit marker - four short diagonals. Lines can be drawn at any angle,
        # unlike the rectangles everything else is made of.
        self.hit_items = [self.canvas.create_line(0, 0, 0, 0, fill="#f2f2f2",
                                                  width=2, state="hidden")
                          for _ in range(4)]

        # An eight-pointed star that pops for a moment when you fire.
        self.flash_item = self.canvas.create_polygon(0, 0, 0, 0, 0, 0,
                                                     fill="#ffe680", outline="")
        self.canvas.itemconfigure(self.flash_item, state="hidden")

        # Crosshair. It rides the horizon rather than sitting at a fixed spot
        # on the screen, because looking up and down only shears the picture -
        # rounds still travel level, straight along the horizon line. Keeping
        # the reticle there means it always covers what you will actually hit.
        self.cross_parts = [
            (-8, -1, -3, 1),
            ( 3, -1,  8, 1),
            (-1, -8, 1, -3),
            (-1,  3, 1,  8),
        ]
        self.cross_items = [self.canvas.create_rectangle(0, 0, 0, 0,
                                                         fill="#7de07d",
                                                         outline="")
                            for _ in self.cross_parts]

    def _build_overlay(self):
        """Full-screen tints and the big centred text used for menus."""
        # `stipple` draws the rectangle as a dot pattern, which is how you fake
        # transparency on a tkinter canvas (it has no real alpha channel).
        self.pain_item = self.canvas.create_rectangle(
            0, 0, SCREEN_W, SCREEN_H, fill="#e01818", outline="",
            stipple="gray12", state="hidden")

        self.dark_item = self.canvas.create_rectangle(
            0, 0, SCREEN_W, SCREEN_H, fill="#000000", outline="",
            stipple="gray75", state="hidden")

        self.title_item = self.canvas.create_text(
            SCREEN_W / 2, self.half_h - 44, text="", fill="#ffcc33",
            font=("Courier", 34, "bold"), state="hidden")
        self.subtitle_item = self.canvas.create_text(
            SCREEN_W / 2, self.half_h + 6, text="", fill="#e8e8e8",
            font=("Courier", 13, "bold"), state="hidden")
        self.prompt_item = self.canvas.create_text(
            SCREEN_W / 2, self.half_h + 48, text="", fill="#9fd39f",
            font=("Courier", 12), state="hidden")

    def _build_hud(self):
        """The status bar along the bottom: health, ammo, kills."""
        top = SCREEN_H
        self.canvas.create_rectangle(0, top, SCREEN_W, top + HUD_H,
                                     fill="#1a1a1f", outline="")
        self.canvas.create_rectangle(0, top, SCREEN_W, top + 3,
                                     fill="#55555f", outline="")

        label_font = ("Courier", 9, "bold")
        big_font = ("Courier", 26, "bold")

        self.canvas.create_text(70, top + 16, text="HEALTH", fill="#8a8a96",
                                font=label_font)
        self.hp_text = self.canvas.create_text(70, top + 42, text="100",
                                               fill="#54d454", font=big_font)
        self.canvas.create_rectangle(18, top + 58, 122, top + 68,
                                     fill="#101014", outline="#44444e")
        self.hp_bar = self.canvas.create_rectangle(19, top + 59, 121, top + 67,
                                                   fill="#54d454", outline="")

        self.canvas.create_text(200, top + 16, text="ROUNDS", fill="#8a8a96",
                                font=label_font)
        self.ammo_text = self.canvas.create_text(200, top + 42, text="24",
                                                 fill="#e0c24a", font=big_font)

        self.canvas.create_text(330, top + 16, text="HOSTILES", fill="#8a8a96",
                                font=label_font)
        self.kills_text = self.canvas.create_text(330, top + 42, text="0/0",
                                                  fill="#d8d8e0", font=big_font)

        self.canvas.create_text(455, top + 16, text="SCORE", fill="#8a8a96",
                                font=label_font)
        self.score_text = self.canvas.create_text(455, top + 42, text="0",
                                                  fill="#d8d8e0", font=big_font)

        self.level_text = self.canvas.create_text(
            SCREEN_W - 12, top + 16, text="", fill="#8a8a96",
            font=label_font, anchor="e")
        self.fps_text = self.canvas.create_text(
            SCREEN_W - 12, top + 34, text="", fill="#55555f",
            font=("Courier", 8), anchor="e")

        self.message_text = self.canvas.create_text(
            SCREEN_W / 2, top + 70, text="", fill="#ffcc55",
            font=("Courier", 10, "bold"))

        # Cache the last value we wrote to each text item. Comparing a string
        # is much cheaper than asking tkinter to redraw text that has not
        # actually changed.
        self._hud_cache = {}

    def _build_minimap(self):
        """A little top-down map in the corner. Walls are drawn only once."""
        self.map_cell = 5
        self.map_x = 8
        self.map_y = 8
        self.minimap_items = []
        self.minimap_wall_items = []
        self.minimap_dots = []

        self.map_bg = self.canvas.create_rectangle(0, 0, 0, 0, fill="#000000",
                                                   outline="#3c3c46",
                                                   stipple="gray50")
        self.minimap_items.append(self.map_bg)

        # Enough wall squares for the biggest level we might load.
        biggest = max(len(lvl["grid"]) * len(lvl["grid"][0]) for lvl in LEVELS)
        for _ in range(biggest):
            item = self.canvas.create_rectangle(-20, -20, -10, -10,
                                                fill="#5a5a6a", outline="",
                                                state="hidden")
            self.minimap_wall_items.append(item)
            self.minimap_items.append(item)

        # Dots for monsters and pickups, plus the player arrow on top.
        for _ in range(MAX_SPRITE_SLOTS * 2):
            item = self.canvas.create_rectangle(-20, -20, -10, -10,
                                                fill="#cc4444", outline="",
                                                state="hidden")
            self.minimap_dots.append(item)
            self.minimap_items.append(item)

        self.map_player = self.canvas.create_oval(-20, -20, -10, -10,
                                                  fill="#ffffff", outline="")
        self.map_facing = self.canvas.create_line(0, 0, 0, 0, fill="#ffffff",
                                                  width=1)
        self.minimap_items.append(self.map_player)
        self.minimap_items.append(self.map_facing)

    # -- keyboard and mouse ------------------------------------------------

    def _bind_keys(self):
        self.canvas.focus_set()
        self.root.bind("<KeyPress>", self._on_key_press)
        self.root.bind("<KeyRelease>", self._on_key_release)
        self.canvas.bind("<Button-1>", self._on_mouse_down)
        self.canvas.bind("<ButtonRelease-1>", self._on_mouse_up)
        self.canvas.bind("<Button-3>", self._on_aim_down)
        self.canvas.bind("<ButtonRelease-3>", self._on_aim_up)
        self.canvas.bind("<Motion>", self._on_mouse_move)
        # If the window loses focus (you alt-tabbed, or a dialog opened) let
        # the mouse go, otherwise the pointer stays trapped in a dead window.
        self.root.bind("<FocusOut>", lambda _e: self._release_mouse())

    # Mouse aiming
    #
    # Real games ask the operating system to lock the pointer and then read
    # raw movement. Tkinter has no such thing, so we fake it: every time the
    # mouse moves we measure how far it got from the centre of the window,
    # turn by that much, and then teleport the pointer back to the centre.
    # That way it never reaches an edge and can keep turning forever.
    #
    # Warping the pointer generates *another* Motion event, which would look
    # like the player yanking the mouse back the other way. `_warping` marks
    # that echo so it can be thrown away.

    def _capture_mouse(self):
        if self.mouse_captured or not self.mouse_enabled:
            return
        self.mouse_captured = True
        self.canvas.configure(cursor="none")
        self._centre_pointer()
        self.say("Mouse captured - press Esc to release it", 2.0)

    def _release_mouse(self):
        if not self.mouse_captured:
            return
        self.mouse_captured = False
        self.canvas.configure(cursor="")

    def _centre_pointer(self):
        """Put the pointer back in the middle of the view."""
        self._warping = True
        self.canvas.event_generate("<Motion>", warp=True,
                                   x=int(SCREEN_W // 2), y=int(SCREEN_H // 2))

    def _on_mouse_down(self, _event):
        if self.state == STATE_TITLE:
            self.state = STATE_PLAYING
            self._hide_overlay()
        if not self.mouse_captured:
            # The first click is what grabs the pointer, so it does not also
            # fire - otherwise clicking on the window would waste a round.
            self._capture_mouse()
            return
        self.firing = True

    def _on_mouse_up(self, _event):
        self.firing = False

    def _on_aim_down(self, _event):
        if self.mouse_captured:
            self.aiming = True

    def _on_aim_up(self, _event):
        self.aiming = False

    def _on_mouse_move(self, event):
        if self._warping:
            self._warping = False       # this is our own teleport echoing back
            return
        if not self.mouse_captured or self.state != STATE_PLAYING:
            return

        move_x = event.x - SCREEN_W // 2
        move_y = event.y - SCREEN_H // 2
        if move_x == 0 and move_y == 0:
            return

        sensitivity = MOUSE_SENSITIVITY * (1.0 + (ADS_SENSITIVITY - 1.0) * self.ads)
        turn = move_x * sensitivity
        self.angle = (self.angle + turn) % (2 * math.pi)
        self._turn_accum += turn
        self._update_camera_vectors()

        # Looking up and down is a cheat: a grid raycaster has no idea what
        # "up" means, so we just slide the whole horizon line instead. It is
        # the same trick Doom used, and it holds up fine over a small range.
        step = move_y * sensitivity * PITCH_SENSITIVITY * SCREEN_H
        before = self.pitch
        self.pitch -= -step if MOUSE_INVERT_Y else step
        self.pitch = max(-MAX_PITCH_DOWN, min(MAX_PITCH_UP, self.pitch))
        self._pitch_accum += self.pitch - before

        self._centre_pointer()

    @staticmethod
    def _key_name(event):
        # keysym is "w", "Left", "space", "Shift_L" ... - lowercase it all so
        # we only ever have to compare against one spelling.
        return event.keysym.lower()

    def _on_key_press(self, event):
        key = self._key_name(event)
        # If a release was waiting to happen for this key, cancel it - see
        # _on_key_release below for why.
        self._pending_release.pop(key, None)

        if key not in self.keys:
            self.keys.add(key)
            self._on_key_tapped(key)

    def _on_key_release(self, event):
        key = self._key_name(event)
        # On Linux, holding a key down makes X11 send a fake KeyRelease
        # immediately before every repeated KeyPress. If we believed it, held
        # keys would stutter. So we delay acting on releases by 35ms; a repeat
        # press arrives first and cancels it. Windows and macOS do not send the
        # fake release at all, so this costs them nothing but 35ms.
        token = object()
        self._pending_release[key] = token
        self.root.after(35, lambda k=key, t=token: self._commit_release(k, t))

    def _commit_release(self, key, token):
        if self._pending_release.get(key) is token:
            del self._pending_release[key]
            self.keys.discard(key)

    def _on_key_tapped(self, key):
        """Handle keys that should fire once per press, not once per frame."""
        if key == "escape":
            # First Esc hands the pointer back, so you are never trapped in
            # the window. A second Esc actually quits.
            if self.mouse_captured:
                self._release_mouse()
                self.say("Mouse released - click the window to take it back")
                return
            # Stop the loop first: if we destroyed the window while a tick was
            # still booked in, that tick would wake up and find nothing to draw.
            self.running = False
            self.root.destroy()
            return

        if self.state == STATE_TITLE:
            self.state = STATE_PLAYING
            self._hide_overlay()
            self._capture_mouse()
            return

        if key == "tab":
            self.show_minimap = not self.show_minimap
            self._apply_minimap_visibility()
        elif key == "m":
            self.mouse_enabled = not self.mouse_enabled
            if self.mouse_enabled:
                self.say("Mouse aiming ON - click the window to capture it")
            else:
                self._release_mouse()
                self.pitch = 0.0
                self.say("Mouse aiming OFF - turn with the arrow keys")
        elif key == "r":
            self.load_level(self.level_index)
            self.state = STATE_PLAYING
            self._hide_overlay()
        elif key == "n" and self.state == STATE_CLEARED:
            self.level_index += 1
            self.load_level(self.level_index)
            self.state = STATE_PLAYING
            self._hide_overlay()
        elif key == "n" and self.state == STATE_WON:
            self.total_score = 0
            self.level_index = 0
            self.load_level(0)
            self.state = STATE_PLAYING
            self._hide_overlay()

    def _held(self, *names):
        """True if any of these keys is currently held down."""
        return any(n in self.keys for n in names)

    # -- loading a level ---------------------------------------------------

    def load_level(self, index):
        level = LEVELS[index]
        self.level = level
        self.level_index = index
        self.grid = [list(row) for row in level["grid"]]
        self.map_h = len(self.grid)
        self.map_w = len(self.grid[0])

        self.monsters = []
        self.pickups = []
        self.exits = []
        start_x = start_y = 1.5

        # Walk the map once, pulling out everything that is not a wall or floor
        # and replacing it with plain floor so the raycaster never sees it.
        for row in range(self.map_h):
            for col in range(self.map_w):
                char = self.grid[row][col]
                centre_x = col + 0.5
                centre_y = row + 0.5

                if char == "P":
                    start_x, start_y = centre_x, centre_y
                elif char in MONSTERS:
                    self.monsters.append(Monster(char, centre_x, centre_y))
                elif char in PICKUPS:
                    self.pickups.append(Pickup(char, centre_x, centre_y))
                elif char == "X":
                    self.exits.append((col, row))
                    continue        # the exit pad stays in the grid
                else:
                    continue        # wall or floor: leave it alone

                self.grid[row][col] = "."

        # Player state
        self.px = start_x
        self.py = start_y
        self.angle = math.radians(level["start_angle"])
        self.hp = PLAYER_MAX_HP
        self.ammo = START_AMMO
        self.kills = 0
        self.level_score = 0
        self.total_monsters = len(self.monsters)

        # Timers and effects
        self.shot_timer = 0.0
        self.flash_timer = 0.0
        self.pain_timer = 0.0
        self.recoil = 0.0
        self.bob_phase = 0.0
        self.bob_offset = 0.0
        self.message = ""
        self.message_timer = 0.0
        self.exit_pulse = 0.0
        self.pitch = 0.0
        self.lean = 0.0
        self.lean_applied = 0.0
        self.sway_x = 0.0
        self.sway_y = 0.0
        self._turn_accum = 0.0
        self._pitch_accum = 0.0
        self.hitmark_timer = 0.0
        self.ads = 0.0
        self.aiming = False
        self.blood = []
        self.cam_x = self.px
        self.cam_y = self.py

        self._update_camera_vectors()
        self._draw_minimap_walls()
        self._apply_minimap_visibility()
        self._hud_cache.clear()
        self.say("OP %d - %s: neutralise %d hostiles"
                 % (index + 1, level["name"], self.total_monsters), 4.0)

    def _update_camera_vectors(self):
        """
        Recompute the camera basis.

        `dir` is the unit vector the player faces. `plane` is perpendicular to
        it and its length sets the field of view - this pair is what lets us
        sweep rays across the screen without any fisheye distortion.
        """
        self.dir_x = math.cos(self.angle)
        self.dir_y = math.sin(self.angle)
        # The camera plane's length IS the field of view, so zooming in when
        # you raise the sight is just a matter of shortening it.
        fov = FOV_DEGREES + (ADS_FOV_DEGREES - FOV_DEGREES) * self.ads
        self.plane_len = math.tan(math.radians(fov) * 0.5)
        self.plane_x = -self.dir_y * self.plane_len
        self.plane_y = self.dir_x * self.plane_len

    def _update_camera_position(self):
        """
        Work out where the eye is, which is not always where the feet are.

        Leaning slides the viewpoint sideways along the camera plane while the
        body stays put, which is what lets you peek past a corner without
        stepping into the open. If the lean would put your head inside a wall
        we shorten it until it does not - so leaning into a wall just stops.
        """
        if self.lean == 0.0:
            self.cam_x = self.px
            self.cam_y = self.py
            self.lean_applied = 0.0
            return

        # The camera plane already points along the player's right-hand side.
        side_x = -self.dir_y
        side_y = self.dir_x

        reach = self.lean * LEAN_DISTANCE
        while abs(reach) > 0.01:
            test_x = self.px + side_x * reach
            test_y = self.py + side_y * reach
            if self.can_stand(test_x, test_y, PLAYER_RADIUS * 0.6):
                self.cam_x = test_x
                self.cam_y = test_y
                self.lean_applied = reach / LEAN_DISTANCE
                return
            reach *= 0.6            # blocked - try leaning out less far

        self.cam_x = self.px
        self.cam_y = self.py
        self.lean_applied = 0.0

    # -- world queries -----------------------------------------------------

    def is_wall(self, x, y):
        col = int(x)
        row = int(y)
        if col < 0 or row < 0 or col >= self.map_w or row >= self.map_h:
            return True
        return self.grid[row][col] in WALL_COLOURS

    def can_stand(self, x, y, radius):
        """True if a circle of this radius at (x, y) does not touch a wall."""
        for off_x in (-radius, radius):
            for off_y in (-radius, radius):
                if self.is_wall(x + off_x, y + off_y):
                    return False
        return True

    def has_line_of_sight(self, x0, y0, x1, y1):
        """Walk from A to B in small steps; blocked if we cross a wall."""
        dx = x1 - x0
        dy = y1 - y0
        distance = math.hypot(dx, dy)
        if distance < 0.001:
            return True
        steps = int(distance / 0.18) + 1
        step_x = dx / steps
        step_y = dy / steps
        x, y = x0, y0
        for _ in range(steps):
            x += step_x
            y += step_y
            if self.is_wall(x, y):
                return False
        return True

    def project(self, world_x, world_y):
        """
        Turn a world position into (screen x in pixels, depth).

        Depth is distance measured straight out from the camera, in the same
        units as the depth buffer, so the two can be compared directly.
        A depth of zero or less means the point is behind the player.
        """
        rel_x = world_x - self.cam_x
        rel_y = world_y - self.cam_y
        determinant = self.plane_x * self.dir_y - self.dir_x * self.plane_y
        if abs(determinant) < 1e-9:
            return 0.0, -1.0
        inv = 1.0 / determinant
        side = inv * (self.dir_y * rel_x - self.dir_x * rel_y)
        depth = inv * (-self.plane_y * rel_x + self.plane_x * rel_y)
        if depth <= 0.0001:
            return 0.0, -1.0
        screen_x = (SCREEN_W * 0.5) * (1.0 + side / depth)
        return screen_x, depth

    # -- the raycaster -----------------------------------------------------

    def cast_rays(self):
        """
        The heart of the whole game.

        For each screen column we shoot one ray and use the DDA algorithm to
        find the first wall it hits. DDA is clever: instead of creeping along
        the ray in tiny steps, it jumps straight from one grid line to the
        next, so the cost depends on how many squares the ray crosses rather
        than on how far it travels.

        Returns a list of (distance, side, wall_character), one per column,
        and fills in self.zbuffer as a side effect.
        """
        results = []
        grid = self.grid
        map_w = self.map_w
        map_h = self.map_h
        px, py = self.cam_x, self.cam_y
        dir_x, dir_y = self.dir_x, self.dir_y
        plane_x, plane_y = self.plane_x, self.plane_y
        zbuffer = self.zbuffer

        for column in range(NUM_COLUMNS):
            # camera_x runs from -1 at the left edge to +1 at the right edge
            camera_x = 2.0 * column / NUM_COLUMNS - 1.0
            ray_x = dir_x + plane_x * camera_x
            ray_y = dir_y + plane_y * camera_x

            map_col = int(px)
            map_row = int(py)

            # How far the ray travels to cross one whole square, in x and in y
            delta_x = abs(1.0 / ray_x) if ray_x != 0.0 else 1e30
            delta_y = abs(1.0 / ray_y) if ray_y != 0.0 else 1e30

            # Distance from the player to the first grid line in each direction
            if ray_x < 0:
                step_col = -1
                side_x = (px - map_col) * delta_x
            else:
                step_col = 1
                side_x = (map_col + 1.0 - px) * delta_x

            if ray_y < 0:
                step_row = -1
                side_y = (py - map_row) * delta_y
            else:
                step_row = 1
                side_y = (map_row + 1.0 - py) * delta_y

            hit_char = "#"
            side = 0
            distance = MAX_DEPTH

            # Repeatedly hop to whichever grid line is nearer
            while True:
                if side_x < side_y:
                    if side_x > MAX_DEPTH:
                        distance = MAX_DEPTH
                        break
                    map_col += step_col
                    side = 0
                    if map_col < 0 or map_col >= map_w:
                        distance = MAX_DEPTH
                        break
                    char = grid[map_row][map_col]
                    if char in WALL_COLOURS:
                        distance = side_x
                        hit_char = char
                        break
                    side_x += delta_x
                else:
                    if side_y > MAX_DEPTH:
                        distance = MAX_DEPTH
                        break
                    map_row += step_row
                    side = 1
                    if map_row < 0 or map_row >= map_h:
                        distance = MAX_DEPTH
                        break
                    char = grid[map_row][map_col]
                    if char in WALL_COLOURS:
                        distance = side_y
                        hit_char = char
                        break
                    side_y += delta_y

            # `distance` here is already the PERPENDICULAR distance to the wall
            # (distance along the view direction, not along the ray). That is
            # exactly what we want: using the true ray length would bend the
            # walls outwards into a fisheye lens.
            if distance < 0.0001:
                distance = 0.0001

            # Where along the face of that wall square did we hit? Follow the
            # ray out to the wall and keep the fractional part. This is the
            # texture's horizontal coordinate, and it is what lets a wall show
            # vertical seams that stay put as you walk past.
            if side == 0:
                wall_x = py + distance * ray_y
            else:
                wall_x = px + distance * ray_x
            wall_x -= int(wall_x)

            zbuffer[column] = distance
            results.append((distance, side, hit_char, wall_x))

        return results

    # -- updating the world ------------------------------------------------

    def update_player(self, dt):
        forward = 0.0
        if self._held("w", "up"):
            forward += 1.0
        if self._held("s", "down"):
            forward -= 1.0

        strafe = 0.0
        if self._held("d"):
            strafe += 1.0
        if self._held("a"):
            strafe -= 1.0

        turn = 0.0
        if self._held("right"):
            turn += 1.0
        if self._held("left"):
            turn -= 1.0

        if turn:
            step = turn * TURN_SPEED * dt
            self.angle = (self.angle + step) % (2 * math.pi)
            self._turn_accum += step

        # --- aiming down the sight ---
        # `ads` slides towards 0 or 1 rather than snapping, so the sight comes
        # up smoothly. Everything else about aiming reads off that one number.
        target = 1.0 if self.aiming else 0.0
        self.ads += (target - self.ads) * min(1.0, ADS_SPEED * dt)
        if abs(self.ads - target) < 0.002:
            self.ads = target

        # --- leaning ---
        # Q and E tip you out to the side to peek round a corner. It moves
        # where you look from without moving where you stand.
        lean_target = 0.0
        if self._held("e"):
            lean_target += 1.0
        if self._held("q"):
            lean_target -= 1.0
        self.lean += (lean_target - self.lean) * min(1.0, LEAN_SPEED * dt)
        if abs(self.lean) < 0.002:
            self.lean = 0.0

        self._update_camera_vectors()

        running = self._held("shift_l", "shift_r") and self.ads < 0.5
        speed = RUN_SPEED if running else WALK_SPEED
        speed *= 1.0 + (ADS_MOVE_SCALE - 1.0) * self.ads
        move_x = (self.dir_x * forward + (-self.dir_y) * strafe * STRAFE_SCALE) * speed * dt
        move_y = (self.dir_y * forward + (self.dir_x) * strafe * STRAFE_SCALE) * speed * dt

        # Try each axis on its own. If one is blocked the other still works,
        # which is what makes you slide along a wall instead of sticking to it.
        if move_x and self.can_stand(self.px + move_x, self.py, PLAYER_RADIUS):
            self.px += move_x
        if move_y and self.can_stand(self.px, self.py + move_y, PLAYER_RADIUS):
            self.py += move_y

        self._update_camera_position()

        # Head bob while walking
        if forward or strafe:
            self.bob_phase += dt * (14.0 if running else 9.5)
            self.bob_offset = math.sin(self.bob_phase) * 3.0
        else:
            self.bob_offset *= max(0.0, 1.0 - dt * 8.0)

        self._update_sway(dt)

        if self.firing or self._held("space", "control_l", "control_r"):
            self.shoot()

        self.check_pickups()
        self.check_exit()

    def _update_sway(self, dt):
        """
        Make the carbine feel like it weighs something.

        Swing the view and the weapon does not snap round with it - it trails
        behind for a moment and then catches up. We do that by banking however
        far the view turned this frame (from the mouse and from the arrow keys
        alike) and pushing the weapon the opposite way, then letting it spring
        back to centre. Standing still, a slow drift stands in for breathing.
        """
        damp = 1.0 + (ADS_SWAY_SCALE - 1.0) * self.ads

        self.sway_x -= self._turn_accum * SWAY_TURN * damp
        self.sway_y -= self._pitch_accum * SWAY_PITCH * damp
        self._turn_accum = 0.0
        self._pitch_accum = 0.0

        limit = SWAY_MAX * damp
        self.sway_x = max(-limit, min(limit, self.sway_x))
        self.sway_y = max(-limit, min(limit, self.sway_y))

        catch_up = min(1.0, SWAY_RETURN * dt)
        self.sway_x -= self.sway_x * catch_up
        self.sway_y -= self.sway_y * catch_up

        self.breath += dt

    def shoot(self):
        if self.shot_timer > 0.0:
            return
        if self.ammo <= 0:
            # With no melee attack, an empty gun and no shells left on the
            # floor is a dead end - so say so rather than leaving them stuck.
            if any(not p.taken and p.info["ammo"] for p in self.pickups):
                self.say("Dry! Find a magazine.")
            else:
                self.say("Dry, and no magazines left on site. Press R.", 4.0)
            self.shot_timer = 0.35
            return

        self.ammo -= 1
        self.shot_timer = SHOT_COOLDOWN
        self.flash_timer = 0.10
        self.recoil = 16.0

        # A shotgun sprays pellets, so fire several rays with a little scatter.
        spread_scale = 1.0 + (ADS_SPREAD_SCALE - 1.0) * self.ads
        for _ in range(SHOT_PELLETS):
            spread = random.uniform(-SHOT_SPREAD, SHOT_SPREAD) * spread_scale
            victim = self._pellet_target(spread)
            if victim is not None:
                self.damage_monster(victim, SHOT_DAMAGE)
                self.hitmark_timer = HITMARK_TIME

    def _pellet_target(self, spread):
        """Find the nearest living monster this pellet hits, or None."""
        aim = self.angle + spread
        best = None
        best_depth = SHOT_RANGE

        for monster in self.monsters:
            if not monster.alive:
                continue

            dx = monster.x - self.cam_x
            dy = monster.y - self.cam_y
            distance = math.hypot(dx, dy)
            if distance > best_depth or distance < 0.0001:
                continue

            # Is the pellet pointing at it? A monster covers a smaller angle
            # the further away it is, so the tolerance shrinks with distance.
            difference = (math.atan2(dy, dx) - aim + math.pi) % (2 * math.pi) - math.pi
            if abs(difference) > math.atan2(monster.info["radius"], distance):
                continue

            # Is there a wall in the way?
            screen_x, depth = self.project(monster.x, monster.y)
            if depth <= 0:
                continue
            column = int(screen_x / self.column_w)
            if 0 <= column < NUM_COLUMNS and self.zbuffer[column] < depth:
                continue

            best = monster
            best_depth = distance

        return best

    def spray_blood(self, x, y, count, force=1.0):
        """
        Throw a handful of specks off a hit.

        Each speck lives in the world, not on the screen - it has a position,
        a height above the floor and a velocity, and falls under gravity. That
        costs a little more than screen-space specks but it means the spray
        stays where it was thrown when you turn away and look back.
        """
        spare = MAX_BLOOD - len(self.blood)
        if spare <= 0:
            return
        for _ in range(min(count, spare)):
            angle = random.uniform(0, 2 * math.pi)
            speed = random.uniform(0.5, 2.4) * force
            self.blood.append([
                x, y,                                   # where on the map
                random.uniform(0.42, 0.72),             # height off the floor
                math.cos(angle) * speed,                # velocity
                math.sin(angle) * speed,
                random.uniform(1.2, 3.4) * force,       # upward kick
                random.uniform(0.55, 1.0) * BLOOD_LIFETIME,
            ])

    def damage_monster(self, monster, amount):
        monster.hp -= amount
        monster.hurt_flash = 0.09
        monster.awake = True
        self.spray_blood(monster.x, monster.y, BLOOD_ON_HIT)
        if monster.hp <= 0 and monster.alive:
            monster.alive = False
            monster.death_timer = 0.0
            self.spray_blood(monster.x, monster.y, BLOOD_ON_DEATH, force=1.35)
            self.kills += 1
            self.level_score += monster.info["score"]
            self.say("%s down." % monster.info["name"], 1.4)
            if self.kills >= self.total_monsters:
                self.say("All hostiles neutralised - move to extraction!", 4.0)

    def update_monsters(self, dt):
        for monster in self.monsters:
            if not monster.alive:
                monster.death_timer += dt
                continue

            if monster.hurt_flash > 0:
                monster.hurt_flash -= dt
            if monster.attack_timer > 0:
                monster.attack_timer -= dt

            dx = self.px - monster.x
            dy = self.py - monster.y
            distance = math.hypot(dx, dy)
            info = monster.info

            # Wake up when it can actually see you
            if not monster.awake:
                if distance < info["sight"] and self.has_line_of_sight(
                        monster.x, monster.y, self.px, self.py):
                    monster.awake = True
                else:
                    continue

            can_see = self.has_line_of_sight(monster.x, monster.y,
                                             self.px, self.py)

            # --- attack ---
            if monster.attack_timer <= 0 and can_see:
                if info["ranged"] and distance <= info["ranged_range"]:
                    self.hurt_player(info["ranged_damage"])
                    monster.attack_timer = info["attack_cooldown"]
                elif distance <= info["melee_range"] and info["melee_damage"]:
                    self.hurt_player(info["melee_damage"])
                    monster.attack_timer = info["attack_cooldown"]

            # --- move ---
            if distance < 0.001:
                continue

            # Guards stop at their preferred range and hold it; imps charge.
            if info["keep_distance"] and can_see and distance < info["keep_distance"]:
                direction = -1.0     # back away
            elif info["keep_distance"] and can_see and distance < info["keep_distance"] + 1.0:
                direction = 0.0      # happy where it is
            else:
                direction = 1.0      # close in

            if direction == 0.0:
                continue

            monster.wobble_timer -= dt
            if monster.wobble_timer <= 0:
                monster.wobble = random.uniform(-0.6, 0.6)
                monster.wobble_timer = random.uniform(0.6, 1.8)

            heading = math.atan2(dy, dx) + monster.wobble * 0.5
            speed = info["speed"] * direction * dt
            step_x = math.cos(heading) * speed
            step_y = math.sin(heading) * speed
            radius = info["radius"]

            if self.can_stand(monster.x + step_x, monster.y, radius):
                monster.x += step_x
            if self.can_stand(monster.x, monster.y + step_y, radius):
                monster.y += step_y

    def update_blood(self, dt):
        """Move every speck, and drop the ones that have landed or dried up."""
        alive = []
        for speck in self.blood:
            speck[6] -= dt
            if speck[6] <= 0:
                continue
            speck[5] -= BLOOD_GRAVITY * dt      # gravity pulls the kick down
            speck[0] += speck[3] * dt
            speck[1] += speck[4] * dt
            speck[2] += speck[5] * dt
            if speck[2] <= 0.02:                # hit the floor
                continue
            if self.is_wall(speck[0], speck[1]):
                continue                        # splattered on a wall
            alive.append(speck)
        self.blood = alive

    def hurt_player(self, amount):
        self.hp -= amount
        self.pain_timer = 0.14
        if self.hp <= 0:
            self.hp = 0
            self.state = STATE_DEAD
            self._set_overlay("OPERATOR DOWN",
                              "score this mission: %d" % self.level_score,
                              "press R to reinsert")

    def check_pickups(self):
        for pickup in self.pickups:
            if pickup.taken:
                continue
            if math.hypot(pickup.x - self.px, pickup.y - self.py) > 0.55:
                continue

            info = pickup.info
            if info["heal"]:
                if self.hp >= PLAYER_MAX_HP:
                    continue        # leave it on the floor for later
                self.hp = min(PLAYER_MAX_HP, self.hp + info["heal"])
                self.say("Medkit applied  (+%d)" % info["heal"])
            if info["ammo"]:
                if self.ammo >= MAX_AMMO:
                    continue
                self.ammo = min(MAX_AMMO, self.ammo + info["ammo"])
                self.say("Magazine recovered  (+%d rounds)" % info["ammo"])

            pickup.taken = True
            self.level_score += 25

    def check_exit(self):
        for col, row in self.exits:
            if int(self.px) != col or int(self.py) != row:
                continue
            if self.kills < self.total_monsters:
                self.say("Extraction is on hold - %d hostiles still up"
                         % (self.total_monsters - self.kills))
                return

            # Bank this level's points into the running total. Zero the level
            # score afterwards, otherwise the status bar - which shows
            # total + level - would count these points twice.
            earned = self.level_score
            self.total_score += earned
            self.level_score = 0

            if self.level_index + 1 < len(LEVELS):
                self.state = STATE_CLEARED
                self._set_overlay("AREA SECURE",
                                  "score: %d      total: %d"
                                  % (earned, self.total_score),
                                  "press N for the next mission")
            else:
                self.state = STATE_WON
                self._set_overlay("MISSION COMPLETE",
                                  "final score: %d" % self.total_score,
                                  "press N to run it again")
            return

    def say(self, text, seconds=2.2):
        self.message = text
        self.message_timer = seconds

    # -- drawing -----------------------------------------------------------

    def draw_background(self):
        """Slide the ceiling/floor bands to wherever the horizon now is."""
        offset = self.horizon - self.half_h
        for item, y0, y1 in self.bg_items:
            self.canvas.coords(item, 0, y0 + offset, SCREEN_W, y1 + offset)

    def draw_walls(self, rays):
        canvas = self.canvas
        column_w = self.column_w
        horizon = self.horizon
        items = self.column_items
        fills = self.column_fill

        used = self.column_used

        for i in range(NUM_COLUMNS):
            distance, side, char, wall_x = rays[i]
            slot = items[i]

            if distance >= MAX_DEPTH:
                # Nothing hit: collapse the stripe so only sky/floor shows
                for band in range(used[i]):
                    canvas.coords(slot[band], 0, 0, 0, 0)
                used[i] = 0
                continue

            height = SCREEN_H / distance
            top = horizon - height * 0.5
            x0 = i * column_w
            x1 = x0 + column_w + 1

            shades = (WALL_SHADES_BRIGHT if side == 0 else WALL_SHADES_DARK)[char]
            # Distance fog, plus the free across-the-wall detail: a groove in
            # the wall face simply reads as a few steps darker.
            base = shade_index(distance) + WALL_SEAMS[char][int(wall_x * SEAM_STEPS)]

            # Close walls get the full banding. Distant ones would be drawing
            # bands a pixel or two tall, so they collapse to one flat stripe -
            # which is also what keeps the frame cost down, since most of the
            # columns on screen at any moment are far away.
            bands = WALL_BANDS[char] if height >= BAND_MIN_HEIGHT else FLAT_BAND

            drawn = 0
            column_fills = fills[i]
            for band_top, band_bottom, step in bands:
                y0 = top + height * band_top
                if y0 >= SCREEN_H:
                    continue
                y1 = top + height * band_bottom
                if y1 <= 0:
                    continue
                if y0 < 0:
                    y0 = 0
                if y1 > SCREEN_H:
                    y1 = SCREEN_H
                if y1 <= y0:
                    continue

                level = base + step
                if level < 0:
                    level = 0
                elif level >= SHADE_LEVELS:
                    level = SHADE_LEVELS - 1

                item = slot[drawn]
                canvas.coords(item, x0, y0, x1, y1)
                colour = shades[level]
                # Only bother tkinter if the colour actually changed.
                if colour != column_fills[drawn]:
                    canvas.itemconfigure(item, fill=colour)
                    column_fills[drawn] = colour
                drawn += 1

            for band in range(drawn, used[i]):
                canvas.coords(slot[band], 0, 0, 0, 0)
            used[i] = drawn

    def draw_sprites(self):
        """
        Draw exits, pickups and monsters as flat cut-outs that always face the
        player (the technique is called billboarding).

        Within each pool everything is sorted far-to-near and handed out to the
        pre-made canvas slots in that order, which gets the overlapping right
        automatically.
        """
        used = set()

        # Exit pads first: they lie flat on the floor and never move, so they
        # belong behind everything else.
        visible_exits = []
        for col, row in self.exits:
            screen_x, depth = self.project(col + 0.5, row + 0.5)
            if depth <= 0.25:
                continue
            visible_exits.append((depth, screen_x))
        visible_exits.sort(key=lambda entry: -entry[0])

        # The pad breathes between half and full brightness so it reads as a
        # beacon rather than a bit of scenery, and it does not dim with fog.
        glow = 0.62 + 0.38 * (0.5 + 0.5 * math.sin(self.exit_pulse))
        glow_level = int((SHADE_LEVELS - 1) * glow)

        for slot_index, (depth, screen_x) in enumerate(visible_exits[-MAX_EXIT_SLOTS:]):
            self._draw_one_sprite(self.exit_slots[slot_index], "X", EXIT_SPRITE,
                                  depth, screen_x, used, level_override=glow_level)

        visible_monsters = []
        for monster in self.monsters:
            screen_x, depth = self.project(monster.x, monster.y)
            if depth <= 0.25 or depth >= FOG_FAR + 4:
                continue
            visible_monsters.append((depth, screen_x, monster))

        visible_pickups = []
        for pickup in self.pickups:
            if pickup.taken:
                continue
            screen_x, depth = self.project(pickup.x, pickup.y)
            if depth <= 0.25 or depth >= FOG_FAR + 4:
                continue
            visible_pickups.append((depth, screen_x, pickup))

        # Sort furthest first, then keep only as many as we have slots for.
        visible_monsters.sort(key=lambda entry: -entry[0])
        visible_pickups.sort(key=lambda entry: -entry[0])
        visible_monsters = visible_monsters[-MAX_SPRITE_SLOTS:]
        visible_pickups = visible_pickups[-MAX_SPRITE_SLOTS:]

        for slot_index, (depth, screen_x, monster) in enumerate(visible_monsters):
            self._draw_one_sprite(
                self.monster_slots[slot_index], monster.kind, monster.info,
                depth, screen_x, used,
                flash=monster.hurt_flash > 0,
                squash=0.0 if monster.alive else min(1.0, monster.death_timer * 2.2))

        for slot_index, (depth, screen_x, pickup) in enumerate(visible_pickups):
            self._draw_one_sprite(
                self.pickup_slots[slot_index], pickup.kind, pickup.info,
                depth, screen_x, used)

        # Park anything that was on screen last frame but is not any more.
        for item in self._on_screen - used:
            self.canvas.coords(item, -40, -40, -30, -30)
        self._on_screen = used

    def _draw_one_sprite(self, slot, kind, info, depth, screen_x, used,
                         flash=False, squash=0.0, level_override=None):
        canvas = self.canvas
        horizon = self.horizon

        full_height = SCREEN_H / depth
        height = full_height * info["height_scale"]
        width = height * info["width_ratio"]

        # Sprites stand ON the floor, so anchor the bottom to where the floor
        # meets a wall at this distance rather than to the middle of the screen.
        floor_y = horizon + full_height * 0.5

        # A dying monster flattens into the ground.
        if squash > 0.0:
            height *= (1.0 - 0.8 * squash)
            width *= (1.0 + 0.25 * squash)

        top = floor_y - height
        left = screen_x - width * 0.5

        # --- work out how much of this sprite the walls are hiding ---
        centre_col = int(screen_x / self.column_w)
        if centre_col < 0 or centre_col >= NUM_COLUMNS:
            return
        if self.zbuffer[centre_col] < depth:
            return              # its middle is behind a wall: skip it entirely

        # Widen out from the centre for as long as the wall behind is still
        # further away than we are. There is no point scanning past the edges
        # of the sprite itself, so the search is capped to its own width.
        scan_lo = max(0, int((screen_x - width) / self.column_w))
        scan_hi = min(NUM_COLUMNS - 1, int((screen_x + width) / self.column_w))
        first_col = centre_col
        while first_col > scan_lo and self.zbuffer[first_col - 1] > depth:
            first_col -= 1
        last_col = centre_col
        while last_col < scan_hi and self.zbuffer[last_col + 1] > depth:
            last_col += 1
        clip_left = first_col * self.column_w
        clip_right = (last_col + 1) * self.column_w

        shades = SPRITE_SHADES[kind]
        level = shade_index(depth) if level_override is None else level_override

        for index, (_shape, ux0, uy0, ux1, uy1, colour_key) in enumerate(info["parts"]):
            item = slot[index]
            x0 = left + ux0 * width
            x1 = left + ux1 * width
            y0 = top + uy0 * height
            y1 = top + uy1 * height

            # Clip against the wall edges and against the screen. Anything that
            # clips away to nothing is simply left out of `used`, and the
            # caller parks it off screen for us.
            if x0 < clip_left:
                x0 = clip_left
            if x1 > clip_right:
                x1 = clip_right
            if y0 < 0:
                y0 = 0
            if y1 > SCREEN_H:
                y1 = SCREEN_H
            if x1 <= x0 or y1 <= y0 or x1 < 0 or x0 > SCREEN_W:
                continue

            canvas.coords(item, x0, y0, x1, y1)
            canvas.itemconfigure(item,
                                 fill="#ffffff" if flash else shades[colour_key][level])
            used.add(item)

    def draw_blood(self):
        """
        Draw the specks in flight and the pools spreading under the bodies.

        Both are projected exactly like a sprite is, and both are checked
        against the depth buffer so blood behind a wall stays hidden.
        """
        canvas = self.canvas
        horizon = self.horizon
        column_w = self.column_w
        zbuffer = self.zbuffer

        # --- pools under the bodies ---
        slot = 0
        for monster in self.monsters:
            if monster.alive or slot >= len(self.pool_items):
                continue
            screen_x, depth = self.project(monster.x, monster.y)
            if depth <= 0.25 or depth >= FOG_FAR + 4:
                continue
            column = int(screen_x / column_w)
            if not (0 <= column < NUM_COLUMNS) or zbuffer[column] < depth:
                continue

            full_height = SCREEN_H / depth
            spread = min(1.0, monster.death_timer / POOL_GROW_TIME)
            width = full_height * 0.62 * spread
            if width < 1.5:
                continue
            floor_y = horizon + full_height * 0.5
            item = self.pool_items[slot]
            canvas.coords(item, screen_x - width * 0.5, floor_y - width * 0.16,
                          screen_x + width * 0.5, floor_y + width * 0.16)
            canvas.itemconfigure(item, state="normal",
                                 fill=BLOOD_POOL_SHADES[shade_index(depth)])
            slot += 1

        # Only hide the ones that were on screen last frame. Blindly hiding
        # the whole pool every frame is a hundred wasted calls to tkinter.
        for spare in range(slot, self._pools_shown):
            canvas.itemconfigure(self.pool_items[spare], state="hidden")
        self._pools_shown = slot

        # --- specks in flight ---
        slot = 0
        for speck_x, speck_y, height, _vx, _vy, _vz, life in self.blood:
            if slot >= MAX_BLOOD:
                break
            screen_x, depth = self.project(speck_x, speck_y)
            if depth <= 0.2 or depth >= FOG_FAR + 4:
                continue
            column = int(screen_x / column_w)
            if not (0 <= column < NUM_COLUMNS) or zbuffer[column] < depth:
                continue

            full_height = SCREEN_H / depth
            # Same anchoring as a sprite: find the floor at this depth, then
            # climb by however high off the ground the speck is.
            screen_y = horizon + full_height * (0.5 - height)
            size = max(1.0, full_height * 0.022)
            if screen_y < -size or screen_y > SCREEN_H + size:
                continue

            item = self.blood_items[slot]
            canvas.coords(item, screen_x - size, screen_y - size,
                          screen_x + size, screen_y + size)
            # Specks darken as they dry, so a spray fades rather than blinking.
            fade = max(0.0, min(1.0, life / BLOOD_LIFETIME))
            canvas.itemconfigure(
                item, state="normal",
                fill=BLOOD_SHADES[int(fade * (SHADE_LEVELS - 1))])
            slot += 1

        for spare in range(slot, self._blood_shown):
            canvas.itemconfigure(self.blood_items[spare], state="hidden")
        self._blood_shown = slot

    def draw_weapon(self):
        canvas = self.canvas
        ads = self.ads
        aim_x = SCREEN_W * 0.5
        aim_y = self.horizon

        # Hip fire: the carbine sits low, sways with your stride and slides
        # across as you lean. It tracks the pitch alongside the horizon, so
        # the reticle keeps a fixed distance above the muzzle.
        # A slow figure-of-eight drift so the weapon is never perfectly still.
        damp = 1.0 + (ADS_SWAY_SCALE - 1.0) * ads
        breath_x = math.sin(self.breath * 1.3) * SWAY_BREATH * damp
        breath_y = math.sin(self.breath * 2.1) * SWAY_BREATH * 0.6 * damp

        hip_x = (aim_x + self.bob_offset * 2.2
                 + self.lean_applied * LEAN_GUN_SHIFT
                 + self.sway_x + breath_x)
        hip_y = (SCREEN_H + self.bob_offset + self.recoil + self.pitch
                 + self.sway_y + breath_y)

        # Looking a long way up slides the weapon down with the horizon, and
        # sway can shove it back up again. Between them they could park the
        # barrel right over the reticle, so the hip pose is not allowed any
        # higher than leaves a clear gap above the muzzle. (The sighted pose
        # is exempt - putting the optic ON the aim point is the whole idea.)
        highest = aim_y + self.gun_top + RETICLE_CLEARANCE
        if hip_y < highest:
            hip_y = highest

        # Bringing the weapon in line with your eye foreshortens it: you stop
        # seeing it side-on and start looking along it, so it narrows a lot and
        # shortens a little. Squeezing the sprite is a cheap stand-in for a
        # view we cannot actually rotate.
        squeeze_x = 1.0 + (ADS_SQUEEZE_X - 1.0) * ads
        squeeze_y = 1.0 + (ADS_SQUEEZE_Y - 1.0) * ads

        # Sighted in: the weapon comes up and across until the optic sits
        # exactly on the aim point, and the bob and the lean shift damp away.
        # The optic moves with the squeeze, so line up against the squeezed
        # position or the sight ends up off to one side.
        # Once the tube is up it IS the sight, so the weapon's own little optic
        # block is redundant - and leaving the receiver sitting in the middle
        # of the glass looks like you are aiming through your own rifle. So the
        # sighted pose drops far enough for the body to sit below the housing,
        # with just the top of it showing, which is what you actually see.
        sight_x = (aim_x - OPTIC_CENTRE_X * squeeze_x
                   + self.lean_applied * LEAN_GUN_SHIFT * 0.3
                   + self.sway_x * 0.35 + breath_x * 0.5)
        sight_y = (aim_y - OPTIC_CENTRE_Y * squeeze_y + ADS_GUN_DROP
                   + self.recoil * 0.5
                   + self.sway_y * 0.35 + breath_y * 0.5)

        centre = hip_x + (sight_x - hip_x) * ads
        base = hip_y + (sight_y - hip_y) * ads

        # Everything in front of the optic - handguard, gas block, front sight,
        # muzzle - shrinks away towards the optic as the sight comes up. Behind
        # a red dot you are looking straight down the weapon, so the barrel is
        # pointing away from you and should recede, not sit there side-on.
        recede = 1.0 - ads
        optic_x = OPTIC_CENTRE_X * squeeze_x
        optic_y = OPTIC_CENTRE_Y * squeeze_y

        for item, part in zip(self.gun_items, self.gun_parts):
            x0, y0, x1, y1 = part[0], part[1], part[2], part[3]
            x0 *= squeeze_x
            x1 *= squeeze_x
            y0 *= squeeze_y
            y1 *= squeeze_y
            if part[1] < GUN_FORWARD_OF_OPTIC:
                x0 = optic_x + (x0 - optic_x) * recede
                x1 = optic_x + (x1 - optic_x) * recede
                y0 = optic_y + (y0 - optic_y) * recede
                y1 = optic_y + (y1 - optic_y) * recede
            canvas.coords(item, centre + x0, base + y0, centre + x1, base + y1)

        # --- the sight picture ---
        if ads > SIGHT_SHOW_AT:
            # The tube starts oversized and settles to its proper size, which
            # reads as the optic coming up to your eye.
            settle = (ads - SIGHT_SHOW_AT) / (1.0 - SIGHT_SHOW_AT)
            radius = SIGHT_RADIUS * (1.55 - 0.55 * settle)
            outer = radius + SIGHT_RING_WIDTH * 0.5
            canvas.coords(self.sight_ring, aim_x - outer, aim_y - outer,
                          aim_x + outer, aim_y + outer)
            canvas.coords(self.sight_edge, aim_x - radius, aim_y - radius,
                          aim_x + radius, aim_y + radius)
            canvas.coords(self.dot_halo, aim_x - 7, aim_y - 7,
                          aim_x + 7, aim_y + 7)
            canvas.coords(self.dot_item, aim_x - 3, aim_y - 3,
                          aim_x + 3, aim_y + 3)
            if not self._sight_visible:
                for item in (self.sight_ring, self.sight_edge,
                             self.dot_halo, self.dot_item):
                    canvas.itemconfigure(item, state="normal")
                self._sight_visible = True
        elif self._sight_visible:
            for item in (self.sight_ring, self.sight_edge,
                         self.dot_halo, self.dot_item):
                canvas.itemconfigure(item, state="hidden")
            self._sight_visible = False

        # --- hit marker ---
        if self.hitmark_timer > 0:
            reach = 12 - 4 * (self.hitmark_timer / HITMARK_TIME)
            near = reach * 0.45
            for item, (sx, sy) in zip(self.hit_items,
                                      ((-1, -1), (1, -1), (-1, 1), (1, 1))):
                canvas.coords(item,
                              aim_x + sx * near, aim_y + sy * near,
                              aim_x + sx * reach, aim_y + sy * reach)
            if not self._hitmark_visible:
                for item in self.hit_items:
                    canvas.itemconfigure(item, state="normal")
                self._hitmark_visible = True
        elif self._hitmark_visible:
            for item in self.hit_items:
                canvas.itemconfigure(item, state="hidden")
            self._hitmark_visible = False

        # The crosshair fades out as the optic takes over - once you are looking
        # down the sight, the optic itself is the reticle.
        if ads > 0.55:
            for item in self.cross_items:
                canvas.coords(item, 0, 0, 0, 0)
        else:
            for item, (x0, y0, x1, y1) in zip(self.cross_items, self.cross_parts):
                canvas.coords(item, aim_x + x0, aim_y + y0, aim_x + x1, aim_y + y1)

        if self.flash_timer > 0:
            muzzle_x = centre - 3
            muzzle_y = base + self.muzzle_y
            size = (26 + random.uniform(-4, 4)) * (1.0 - 0.55 * ads)
            points = []
            for i in range(8):
                angle = i * math.pi / 4
                radius = size if i % 2 == 0 else size * 0.42
                points.append(muzzle_x + math.cos(angle) * radius)
                points.append(muzzle_y + math.sin(angle) * radius)
            canvas.coords(self.flash_item, *points)
            canvas.itemconfigure(self.flash_item, state="normal")
        else:
            canvas.itemconfigure(self.flash_item, state="hidden")

    def draw_hud(self):
        self._set_text(self.hp_text, str(self.hp))
        self._set_text(self.ammo_text, str(self.ammo))
        ammo_colour = "#d45454" if self.ammo <= LOW_AMMO else "#e0c24a"
        if self._hud_cache.get("ammo_colour") != ammo_colour:
            self.canvas.itemconfigure(self.ammo_text, fill=ammo_colour)
            self._hud_cache["ammo_colour"] = ammo_colour
        self._set_text(self.kills_text, "%d/%d" % (self.kills, self.total_monsters))
        self._set_text(self.score_text, str(self.total_score + self.level_score))
        self._set_text(self.level_text, "OP %d" % (self.level_index + 1))
        self._set_text(self.fps_text, "%d fps  %d rays" % (self.fps, NUM_COLUMNS))

        fraction = self.hp / PLAYER_MAX_HP
        if fraction > 0.55:
            colour = "#54d454"
        elif fraction > 0.25:
            colour = "#d4c054"
        else:
            colour = "#d45454"

        if self._hud_cache.get("hp_colour") != colour:
            self.canvas.itemconfigure(self.hp_text, fill=colour)
            self.canvas.itemconfigure(self.hp_bar, fill=colour)
            self._hud_cache["hp_colour"] = colour

        self.canvas.coords(self.hp_bar, 19, SCREEN_H + 59,
                           19 + max(0.0, 102 * fraction), SCREEN_H + 67)

        self._set_text(self.message_text, self.message if self.message_timer > 0 else "")

    def _set_text(self, item, text):
        if self._hud_cache.get(item) != text:
            self.canvas.itemconfigure(item, text=text)
            self._hud_cache[item] = text

    def draw_minimap(self):
        if not self.show_minimap:
            return

        canvas = self.canvas
        cell = self.map_cell
        ox, oy = self.map_x, self.map_y

        index = 0
        for monster in self.monsters:
            if not monster.alive or index >= len(self.minimap_dots):
                continue
            item = self.minimap_dots[index]
            x = ox + monster.x * cell
            y = oy + monster.y * cell
            canvas.coords(item, x - 1.5, y - 1.5, x + 1.5, y + 1.5)
            canvas.itemconfigure(item, state="normal",
                                 fill="#ff5555" if monster.awake else "#a04040")
            index += 1

        for pickup in self.pickups:
            if pickup.taken or index >= len(self.minimap_dots):
                continue
            item = self.minimap_dots[index]
            x = ox + pickup.x * cell
            y = oy + pickup.y * cell
            canvas.coords(item, x - 1.5, y - 1.5, x + 1.5, y + 1.5)
            canvas.itemconfigure(item, state="normal",
                                 fill="#55dd55" if pickup.kind == "h" else "#ddbb44")
            index += 1

        while index < len(self.minimap_dots):
            canvas.itemconfigure(self.minimap_dots[index], state="hidden")
            index += 1

        x = ox + self.px * cell
        y = oy + self.py * cell
        canvas.coords(self.map_player, x - 2, y - 2, x + 2, y + 2)
        canvas.coords(self.map_facing, x, y,
                      x + self.dir_x * cell * 1.9, y + self.dir_y * cell * 1.9)

    def _draw_minimap_walls(self):
        """Redrawn only when a level loads, never during play."""
        cell = self.map_cell
        ox, oy = self.map_x, self.map_y
        self.canvas.coords(self.map_bg, ox - 3, oy - 3,
                           ox + self.map_w * cell + 3,
                           oy + self.map_h * cell + 3)

        index = 0
        for row in range(self.map_h):
            for col in range(self.map_w):
                char = self.grid[row][col]
                if char == ".":
                    continue
                if index >= len(self.minimap_wall_items):
                    break
                item = self.minimap_wall_items[index]
                x = ox + col * cell
                y = oy + row * cell
                self.canvas.coords(item, x, y, x + cell, y + cell)
                self.canvas.itemconfigure(
                    item, state="normal",
                    fill="#e8c832" if char == "X" else "#5a5a6a")
                index += 1

        while index < len(self.minimap_wall_items):
            self.canvas.itemconfigure(self.minimap_wall_items[index],
                                      state="hidden")
            index += 1

    def _apply_minimap_visibility(self):
        if self.show_minimap:
            self._draw_minimap_walls()
        else:
            for item in self.minimap_items:
                self.canvas.itemconfigure(item, state="hidden")

    def _set_overlay(self, title, subtitle, prompt):
        self.canvas.itemconfigure(self.dark_item, state="normal")
        for item, text in ((self.title_item, title),
                           (self.subtitle_item, subtitle),
                           (self.prompt_item, prompt)):
            self.canvas.itemconfigure(item, text=text, state="normal")
        self.canvas.tag_raise(self.dark_item)
        for item in (self.title_item, self.subtitle_item, self.prompt_item):
            self.canvas.tag_raise(item)

    def _hide_overlay(self):
        for item in (self.dark_item, self.title_item,
                     self.subtitle_item, self.prompt_item):
            self.canvas.itemconfigure(item, state="hidden")

    # -- the main loop -----------------------------------------------------

    def tick(self):
        """
        Called about 30 times a second. Everything happens here: read the
        clock, move the world forward, redraw, then book the next call.

        We use root.after() rather than a `while True:` loop because tkinter
        needs to get back to its own event loop to process keys and repaint
        the window. A busy loop would just freeze the whole program.
        """
        if not self.running:
            return

        frame_start = time.perf_counter()
        dt = frame_start - self.last_time
        self.last_time = frame_start
        # If the computer stalls (or you drag the window), pretend it was a
        # normal frame. Otherwise one huge dt would teleport you through walls.
        if dt > 0.1:
            dt = 0.1

        if self.state == STATE_PLAYING:
            self.update_player(dt)
            self.update_monsters(dt)
            self.update_blood(dt)

        # Timers keep running even when the game is paused on a menu, so an
        # effect that was mid-flight when you died does not freeze on screen.
        self.shot_timer = max(0.0, self.shot_timer - dt)
        self.flash_timer = max(0.0, self.flash_timer - dt)
        self.pain_timer = max(0.0, self.pain_timer - dt)
        self.hitmark_timer = max(0.0, self.hitmark_timer - dt)
        self.message_timer = max(0.0, self.message_timer - dt)
        self.recoil *= max(0.0, 1.0 - dt * 9.0)
        self.exit_pulse = (self.exit_pulse + dt * 3.2) % (2 * math.pi)

        # Where the eye line sits this frame: the middle of the view, nudged
        # by the walking bob and by however far you are looking up or down.
        self.horizon = self.half_h + self.bob_offset + self.pitch

        rays = self.cast_rays()
        self.draw_background()
        self.draw_walls(rays)
        self.draw_sprites()
        self.draw_blood()
        self.draw_weapon()
        self.draw_hud()
        self.draw_minimap()

        hurting = "normal" if self.pain_timer > 0 else "hidden"
        if hurting != self._hud_cache.get("pain"):
            self.canvas.itemconfigure(self.pain_item, state=hurting)
            self._hud_cache["pain"] = hurting

        # Rolling average of the real gap between frames, shown in the corner
        # of the status bar. This is the honest frame rate, not just how long
        # our own drawing took.
        elapsed = time.perf_counter() - frame_start
        self.frame_times.append(dt)
        if len(self.frame_times) > 30:
            self.frame_times.pop(0)
        if len(self.frame_times) >= 5:
            average = sum(self.frame_times) / len(self.frame_times)
            self.fps = 1.0 / average if average > 0 else 0.0

        # Aim for a steady frame rate by subtracting the time we just spent.
        delay = max(1, FRAME_MS - int(elapsed * 1000))
        self.root.after(delay, self.tick)


# ===========================================================================
#  SECTION 7 - START THE GAME
# ===========================================================================

def main():
    print(__doc__.split("HOW THE 3D EFFECT")[0])
    root = tk.Tk()
    Game(root)
    root.mainloop()


if __name__ == "__main__":
    main()
