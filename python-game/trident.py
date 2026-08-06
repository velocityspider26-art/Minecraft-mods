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

import json
import math
import os
import random
import time
import tkinter as tk
import traceback


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
MIN_FRAME_GAP_MS = 4    # always leave tkinter this long to itself

# Quality ladder: (name, stripe width in rays, wall texture tier).
#
# The stripe width is the important one. What actually costs time here is not
# the arithmetic - it is how many separate shapes the drawing library has to
# track and repaint, and there are thousands. Drawing one stripe per two rays
# instead of per ray halves that at a stroke.
QUALITY_LEVELS = (
    ("LOW",    4, 3),
    ("MEDIUM", 2, 1),
    ("HIGH",   1, 0),
)
DEFAULT_QUALITY = 1     # start at MEDIUM; the game moves itself from there

# Automatic quality, judged on the real measured frame rate.
QUALITY_DROP_FPS = 24.0     # below this, give something up
QUALITY_RAISE_FPS = 29.0    # above this, there is room for more
DETAIL_PATIENCE = 25        # frames of agreement before anything changes

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

# Difficulty. Every setting is a multiplier on the normal game, so one number
# per line says exactly how much easier or harder that level is.
DIFFICULTIES = (
    {"name": "RECRUIT",  "health": 1.35, "ammo": 1.35,
     "enemy_damage": 0.60, "enemy_health": 0.80, "enemy_speed": 0.85,
     "score": 0.75},
    {"name": "OPERATOR", "health": 1.00, "ammo": 1.00,
     "enemy_damage": 1.00, "enemy_health": 1.00, "enemy_speed": 1.00,
     "score": 1.00},
    {"name": "VETERAN",  "health": 0.70, "ammo": 0.75,
     "enemy_damage": 1.50, "enemy_health": 1.30, "enemy_speed": 1.15,
     "score": 1.50},
)
DEFAULT_DIFFICULTY = 1

# Where settings and progress are kept. Sitting next to the game file means it
# travels with the game if you copy it onto a memory stick.
SAVE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "trident_save.json")

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
    "W": (206, 208, 214),   # white habitat panel
    "V": ( 54,  70, 104),   # viewport glass
}

# Two shade tables per wall type. Walls we hit on a north/south face are drawn
# slightly darker than east/west faces - that fake "sunlight" is what makes
# the corners of the corridor pop out instead of looking like flat wallpaper.
WALL_SHADES_BRIGHT = {c: make_shade_table(rgb) for c, rgb in WALL_COLOURS.items()}
WALL_SHADES_DARK = {c: make_shade_table(dim(rgb, 0.66)) for c, rgb in WALL_COLOURS.items()}

CEILING_RGB = (34, 38, 50)      # night sky / dark ceiling
FLOOR_RGB = (62, 62, 60)        # poured concrete

# Each mission can override what the place looks like and how it behaves.
# `accel` is how quickly you reach walking speed, in "per second": on Earth it
# is high enough to be instant, and dropping it is what makes the moon feel
# like the moon - you drift up to speed and glide to a stop.
DEFAULT_ENV = {
    "ceiling": CEILING_RGB,
    "floor": FLOOR_RGB,
    # Big enough that accel * dt is always past 1.0, so on Earth you reach
    # walking speed within a single frame at any frame rate - exactly as the
    # game behaved before momentum existed.
    "accel": 1000.0,
    "gravity": 1.0,     # multiplies how fast thrown blood falls
    "stars": False,
}

MOON_ENV = {
    "ceiling": (8, 8, 14),          # airless black
    "floor": (122, 120, 116),       # regolith grey
    "accel": 2.4,                   # one sixth of a g, near enough
    "gravity": 0.17,
    "stars": True,
}

STAR_COUNT = 70


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
    "W": (  # habitat panel: clean seams, a dark rubber skirt
        (0.00, 0.05,  4), (0.05, 0.32,  1), (0.32, 0.36, -5),
        (0.36, 0.64,  2), (0.64, 0.68, -5), (0.68, 0.92,  1),
        (0.92, 1.00, -7),
    ),
    "V": (  # viewport: a bright frame around dark glass
        (0.00, 0.10,  6), (0.10, 0.16, -2), (0.16, 0.46, -6),
        (0.46, 0.52, -3), (0.52, 0.84, -6), (0.84, 0.90, -2),
        (0.90, 1.00,  5),
    ),
    "Y": (  # hazard panel: bold stripes
        (0.00, 0.06,  3), (0.06, 0.26, -6), (0.26, 0.46,  2),
        (0.46, 0.66, -6), (0.66, 0.86,  2), (0.86, 0.94, -6),
        (0.94, 1.00, -4),
    ),
}

MAX_BANDS = max(len(bands) for bands in WALL_BANDS.values())


def simplify_bands(bands):
    """
    Halve a band list by merging neighbours in pairs.

    Each merged pair keeps the shade of whichever of the two covered more of
    the wall, so the boldest features survive and the fine ones drop out
    first. That is exactly the right thing to lose as a wall shrinks into the
    distance.
    """
    merged = []
    for i in range(0, len(bands) - 1, 2):
        first, second = bands[i], bands[i + 1]
        taller = first if (first[1] - first[0]) >= (second[1] - second[0]) else second
        merged.append((first[0], second[1], taller[2]))
    if len(bands) % 2:
        last = bands[-1]
        merged.append((merged[-1][1], last[1], last[2]) if merged else last)
    return tuple(merged)


def band_tiers(bands):
    """Full detail, then progressively coarser versions, down to one stripe."""
    tiers = [tuple(bands)]
    while len(tiers[-1]) > 1:
        tiers.append(simplify_bands(tiers[-1]))
    tiers.append(((0.0, 1.0, 0),))          # a single flat stripe
    return tuple(tiers)


# The banding is only worth drawing while each band is still a few pixels
# tall. As a wall shrinks into the distance we step down through coarser
# versions of its texture - which is also most of what keeps the frame cheap,
# since a corridor is mostly made of columns that are some way off.
WALL_BAND_TIERS = {char: band_tiers(bands) for char, bands in WALL_BANDS.items()}
MAX_TIER = max(len(t) for t in WALL_BAND_TIERS.values()) - 1

# Smallest on-screen wall height that still earns each tier, biggest first.
# The numbers keep every band at roughly 15 pixels or more.
TIER_HEIGHTS = (105.0, 60.0, 30.0, 0.0)


def tier_for_height(height):
    """Which detail tier a wall this tall on screen deserves."""
    for tier, minimum in enumerate(TIER_HEIGHTS):
        if height >= minimum:
            return tier
    return len(TIER_HEIGHTS) - 1


# Distance -> brightness step, worked out once instead of per column per frame.
SHADE_STEPS_PER_UNIT = 8
SHADE_LOOKUP = tuple(
    shade_index(i / SHADE_STEPS_PER_UNIT)
    for i in range(int(MAX_DEPTH * SHADE_STEPS_PER_UNIT) + 4))

SEAM_STEPS = 32         # how finely we sample across the wall face

# The main menu.
MENU_ENTRIES = ("continue", "new", "difficulty", "quality", "fullscreen", "quit")
MENU_TOP = 150
MENU_SPACING = 34

# Where shapes go when they are not needed. Well off the canvas, so tkinter
# throws them out on a bounding-box test instead of trying to paint them.
PARKED = -4000

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


def _scaled_font(spec, scale):
    """Multiply the point size in a Tk font string, leaving the rest alone."""
    parts = str(spec).replace("{", "").replace("}", "").split()
    out = []
    for token in parts:
        if token.lstrip("-").isdigit():
            out.append(str(abs(int(token)) * scale))
        else:
            out.append(token)
    return " ".join(out)


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
    "W": make_seam_table((0.0, 0.5), depth=-6, width=0.045),
    "V": make_seam_table((0.0,), depth=-4, width=0.06),
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

# The high-value target. Same nine-shape layout as an ordinary hostile so it
# can share the sprite pool, but heavier everywhere: broader shoulders, a
# helmet instead of a balaclava, and a plate carrier across the chest.
BLACKJACK_BODY = (
    ("rect", 0.28, 0.68, 0.46, 1.00, "limb"),   # left leg
    ("rect", 0.54, 0.68, 0.72, 1.00, "limb"),   # right leg
    ("rect", 0.00, 0.26, 0.20, 0.70, "limb"),   # left arm
    ("rect", 0.80, 0.26, 1.00, 0.70, "limb"),   # right arm
    ("rect", 0.16, 0.20, 0.84, 0.72, "body"),   # torso
    ("oval", 0.32, 0.00, 0.68, 0.26, "head"),   # helmet
    ("rect", 0.33, 0.09, 0.67, 0.16, "visor"),  # visor
    ("rect", 0.24, 0.28, 0.76, 0.56, "rig"),    # plate carrier
    ("rect", 0.00, 0.42, 0.52, 0.52, "gun"),    # heavy weapon
)

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
    "b": {
        # The final target. Slow, enormous, and hits hard enough that standing
        # still in front of it is not a plan.
        "name": "BLACKJACK",
        "max_hp": 900,
        "speed": 1.35,
        "radius": 0.55,
        "sight": 22.0,
        "score": 3000,
        "parts": BLACKJACK_BODY,
        "width_ratio": 0.86,
        "height_scale": 1.80,       # towers over you
        "attack_cooldown": 1.15,
        "melee_damage": 24,
        "melee_range": 2.3,
        "ranged": True,
        "ranged_damage": 10,
        "ranged_range": 13.0,
        "keep_distance": 0.0,       # comes straight at you
        "boss": True,
        "colours": {
            "body": ( 46,  44,  50), "limb": ( 34,  33,  38),
            "head": ( 58,  54,  46), "visor": (236,  96,  40),
            "rig":  (112,  46,  34), "gun":  ( 26,  26,  30),
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
#      b  ............. BLACKJACK, the final target
#      W  ............. white habitat panel   (moon)
#      V  ............. viewport glass        (moon)
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
    {
        "name": "TUNNEL NETWORK",
        "start_angle": 90,
        "grid": [
            "########################",
            "#....#........#........#",
            "#.P..#...t....#....h...#",
            "#....#........#........#",
            "#....####.#####....#####",
            "#.......#.....#........#",
            "#..a....#..m..#...t....#",
            "#.......#.....#........#",
            "####.####.#####.####.###",
            "#......................#",
            "#..t......h.......m....#",
            "#......................#",
            "###.####.#####.####.####",
            "#.....#........#.......#",
            "#..h..#...t....#...a...#",
            "#.....#........#.......#",
            "#####.#####.####...#####",
            "#......................#",
            "#...m........X.....t...#",
            "########################",
        ],
    },
    {
        "name": "RELAY STATION",
        "start_angle": 0,
        "grid": [
            "########################",
            "#..........SSSS........#",
            "#.P........S..S....t...#",
            "#..........S..S........#",
            "#...OOOO...SSSS...OOO..#",
            "#...O..O..........O.O..#",
            "#...O..O....a.....O.O..#",
            "#...OO.OO.........OOO..#",
            "#......................#",
            "#..t......m..t.........#",
            "#......................#",
            "#..OOOOOO......OOOOOO..#",
            "#..O..h..O....O..a...O.#",
            "#..O.....O....O......O.#",
            "#..OO.OOOO....OOOO.OOO.#",
            "#......................#",
            "#.....m.....X.....m....#",
            "#......................#",
            "#..h................a..#",
            "########################",
        ],
    },
    {
        "name": "THE MOTOR POOL",
        "start_angle": 90,
        "grid": [
            "########################",
            "#......................#",
            "#..P...RR...RR...RR....#",
            "#......RR...RR...RR....#",
            "#......................#",
            "#..t.......m.......t...#",
            "#......................#",
            "#..RR...RR...RR...RR...#",
            "#..RR...RR...RR...RR...#",
            "#......................#",
            "#....a...........h.....#",
            "#......................#",
            "#..RR...RR...RR...RR...#",
            "#..RR...RR...RR...RR...#",
            "#......................#",
            "#....m....t.....m......#",
            "#......................#",
            "#......RR...RR...RR....#",
            "#..h...RR.X.RR...RR..a.#",
            "########################",
        ],
    },
    {
        "name": "COLD STORAGE",
        "start_angle": 90,
        "grid": [
            "########################",
            "#..P....#........#.....#",
            "#.......#...h....#..t..#",
            "#..SSS..#..SSSS..#.....#",
            "#..S....#..S..S..#..SS.#",
            "#..S..a.....S..S....S..#",
            "#..SSS..#..SSSS..#..S..#",
            "#.......#........#.....#",
            "#..#############.......#",
            "#......................#",
            "#...t.......m......t...#",
            "#......................#",
            "#.......#############..#",
            "#.....#........#.......#",
            "#..a..#..SSSS..#...h...#",
            "#.....#..S..S..#.......#",
            "#..m..#..S..S..#...m...#",
            "#.....#..SS.SS.#.......#",
            "#.....#....X...#..t....#",
            "########################",
        ],
    },
    {
        "name": "THE CATWALKS",
        "start_angle": 0,
        "grid": [
            "########################",
            "#.P....................#",
            "#....YYYYYYYYYYYYYY....#",
            "#....Y............Y....#",
            "#....Y...a....h...Y.t..#",
            "#....Y............Y....#",
            "#....YY.YYYYYYYY.YY....#",
            "#......................#",
            "#.......m........m.....#",
            "#......................#",
            "#..YYYYYY......YYYYYY..#",
            "#..Y....Y......Y....Y..#",
            "#..Y..h.Y..t...Y.a..Y..#",
            "#..Y....Y......Y....Y..#",
            "#..YY.YYY......YYY.YY..#",
            "#......................#",
            "#....t...........t.....#",
            "#......................#",
            "#........m..X..m.......#",
            "########################",
        ],
    },
    {
        "name": "AMMUNITION DUMP",
        "start_angle": 90,
        "grid": [
            "########################",
            "#....#............#....#",
            "#.P..#....t..a....#..m.#",
            "#....#............#....#",
            "#....#..RRRRRRRR..#....#",
            "#.......R......R.......#",
            "#..a....R..h...R....h..#",
            "#.......R......R.......#",
            "#....#..RRR..RRR..#....#",
            "#....#............#....#",
            "#....##.########.##....#",
            "#......................#",
            "#..t.....m....m.....t..#",
            "#......................#",
            "#..RRRR..........RRRR..#",
            "#..R..R....X.....R..R..#",
            "#..R..R..........R..R..#",
            "#..RRRR..........RRRR..#",
            "#..h................a..#",
            "########################",
        ],
    },
    {
        "name": "THE INNER KEEP",
        "start_angle": 90,
        "grid": [
            "########################",
            "#......................#",
            "#.P...............t..m.#",
            "#......................#",
            "#..OOOOOOOOOOOOOOOOOO..#",
            "#..O................O..#",
            "#..O..a..........h..O..#",
            "#..O....OOOOOOOO....O..#",
            "#..O....O......O....O..#",
            "#..O....O..h...O....O..#",
            "#..O....O......O....O..#",
            "#..O....OOO..OOO....O..#",
            "#..O................O..#",
            "#..O..m..........m..O..#",
            "#..O................O..#",
            "#..OOOOOO......OOOOOO..#",
            "#......................#",
            "#....t......X......t...#",
            "#..a................h..#",
            "########################",
        ],
    },
    {
        "name": "COMPOUND - TARGET BLACKJACK",
        "start_angle": 90,
        "grid": [
            "########################",
            "#......................#",
            "#..RRRRRRRRRRRRRRRRRR..#",
            "#..R................R..#",
            "#..R..a....m.....h..R..#",
            "#..R................R..#",
            "#..R....RRR..RRR....R..#",
            "#..R....R......R....R..#",
            "#..R....R..b...R....R..#",
            "#..R....R......R....R..#",
            "#..R....RRRRRRRR....R..#",
            "#..R................R..#",
            "#..R..t..........t..R..#",
            "#..R................R..#",
            "#..RRRRRR......RRRRRR..#",
            "#......................#",
            "#..RR.........RR.......#",
            "#.P..a....RR..m....h...#",
            "#..........X...........#",
            "########################",
        ],
    },
    {
        # A bonus mission, and the only one not on Earth. Low gravity, a black
        # airless sky full of stars, and white habitat panelling instead of
        # concrete - all of it driven by the level's `env` block rather than
        # by anything special-cased in the game.
        "name": "TRANQUILLITY OUTPOST",
        "start_angle": 90,
        "env": MOON_ENV,
        "grid": [
            "########################",
            "#..VV....WWWWWW....VV..#",
            "#.P......W....W........#",
            "#........W..t.W....a...#",
            "#..WWWW..W....W..WWWW..#",
            "#..W..W..WW..WW..W..W..#",
            "#..W..W..........W..W..#",
            "#..W..WWWW....WWWW..W..#",
            "#..W.......m........W..#",
            "#..WWWW..........WWWW..#",
            "#......................#",
            "#..t.....WWWWWW.....t..#",
            "#........W....W........#",
            "#..VV....W.h..W....VV..#",
            "#........WW..WW........#",
            "#..WWWW..........WWWW..#",
            "#..W..W....m.....W..W..#",
            "#..W..W....X.....W..W..#",
            "#..a.................h.#",
            "########################",
        ],
    },
]


# ===========================================================================
#  SECTION 5 - SAVED SETTINGS AND PROGRESS
#
#  One small JSON file next to the game. It has to survive being missing,
#  being empty, being edited by hand into nonsense, and living somewhere the
#  computer will not let us write - a school machine may well refuse. None of
#  those are allowed to stop the game starting, so every one of them just
#  falls back to the defaults.
# ===========================================================================

DEFAULT_SAVE = {
    "difficulty": DEFAULT_DIFFICULTY,
    "quality": DEFAULT_QUALITY,
    "fullscreen": False,
    "best_score": 0,
    "missions_cleared": 0,
    "continue_level": 0,        # furthest mission unlocked
    "continue_score": 0,
}


def load_save():
    """Read the save file, falling back to defaults for anything missing."""
    data = dict(DEFAULT_SAVE)
    try:
        with open(SAVE_FILE, "r") as handle:
            stored = json.load(handle)
    except (OSError, ValueError):
        return data             # no file yet, or it is not readable JSON
    if not isinstance(stored, dict):
        return data

    for key, fallback in DEFAULT_SAVE.items():
        value = stored.get(key, fallback)
        # Only take a value that is the same shape as the default. A hand
        # edited file should not be able to crash the game.
        if isinstance(fallback, bool):
            data[key] = bool(value)
        elif isinstance(fallback, int) and isinstance(value, (int, float)):
            data[key] = int(value)

    data["difficulty"] = max(0, min(len(DIFFICULTIES) - 1, data["difficulty"]))
    data["quality"] = max(0, min(len(QUALITY_LEVELS) - 1, data["quality"]))
    data["continue_level"] = max(0, min(len(LEVELS) - 1, data["continue_level"]))
    return data


def write_save(data):
    """Write the save file. Returns False if the computer would not let us."""
    try:
        with open(SAVE_FILE, "w") as handle:
            json.dump(data, handle, indent=2)
        return True
    except OSError:
        return False            # read-only stick, locked-down profile, ...


# ===========================================================================
#  SECTION 6 - GAME OBJECTS
# ===========================================================================

class Monster:
    """One enemy. Knows where it is, how hurt it is, and what it is doing."""

    def __init__(self, kind, x, y, difficulty=DEFAULT_DIFFICULTY):
        info = MONSTERS[kind]
        rules = DIFFICULTIES[difficulty]
        self.kind = kind
        self.info = info
        self.x = x
        self.y = y
        # The difficulty multipliers are baked in here rather than applied
        # every time they are read, so the rest of the game never has to think
        # about which difficulty it is running at.
        self.max_hp = max(1, int(info["max_hp"] * rules["enemy_health"]))
        self.speed = info["speed"] * rules["enemy_speed"]
        self.melee_damage = max(1, int(round(info["melee_damage"]
                                             * rules["enemy_damage"])))
        self.ranged_damage = max(1, int(round(info.get("ranged_damage", 0)
                                              * rules["enemy_damage"])))
        self.hp = self.max_hp
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
#  SECTION 7 - THE GAME
# ===========================================================================

# The game is always in exactly one of these states.
STATE_MENU = "menu"
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
        # A direct line to Tcl. Going through canvas.coords() costs an extra
        # layer of Python argument handling on every one of the thousands of
        # calls the wall loop makes, and it adds up to milliseconds.
        self._tk_call = self.canvas.tk.call
        self._canvas_name = self.canvas._w
        self.detail_tier = 0        # coarser wall texture when things are tight
        self.column_step = 1        # how many rays share one drawn stripe
        self.quality = DEFAULT_QUALITY
        self.scale = 1
        self.env = dict(DEFAULT_ENV)
        self.vel_x = 0.0
        self.vel_y = 0.0
        self._blood_pull = BLOOD_GRAVITY
        self._blood_hang = 1.0
        self._detail_score = 0
        self._reported_error = False

        # Build every canvas item once, up front. During the game we only
        # ever MOVE and RECOLOUR them - creating and deleting thousands of
        # canvas items every frame would be far too slow.
        self._build_background()
        self._build_stars()
        self._build_wall_columns()
        self._build_sprite_pool()
        self._build_weapon()
        self._build_overlay()
        self._build_boss_bar()
        self._build_menu()
        self._build_hud()
        self._build_minimap()

        self._capture_base_style()

        self._bind_keys()
        # Closing the window with its X button has to stop the loop too, or
        # the next scheduled frame wakes up and tries to draw into a window
        # that is not there any more.
        root.protocol("WM_DELETE_WINDOW", self.quit)

        # Settings and progress from last time, if there are any.
        self.save = load_save()
        self._save_warned = False
        self.difficulty = self.save["difficulty"]
        self.menu_index = 1 if self.save["continue_level"] == 0 else 0
        self.fullscreen = False
        self.set_quality(self.save["quality"])
        if self.save["fullscreen"]:
            self.toggle_fullscreen()

        self.level_index = 0
        self.total_score = 0
        self.load_level(0)
        self.state = STATE_MENU
        self.open_menu()

        self.last_time = time.perf_counter()
        self.tick()

    # -- building the canvas items ----------------------------------------

    def _build_background(self):
        """The ceiling and floor: a stack of flat bands faking a gradient."""
        self.bg_items = []          # (item_id, base_y0, base_y1)
        self.bg_shade = []          # (item_id, is_ceiling, brightness)
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
            item = self.canvas.create_rectangle(0, y0, SCREEN_W, y1,
                                                fill="#000000", outline="")
            self.bg_items.append((item, y0, y1))
            # Remember which half it is and how bright, so a mission can
            # repaint the sky and the ground without rebuilding anything.
            self.bg_shade.append((item, True, bright))

            y0 = self.half_h + i * band_h
            y1 = y0 + band_h + (overhang if i == SKY_BANDS - 1 else 0)
            item = self.canvas.create_rectangle(0, y0, SCREEN_W, y1,
                                                fill="#000000", outline="")
            self.bg_items.append((item, y0, y1))
            self.bg_shade.append((item, False, bright))

    def _recolour_background(self, ceiling, floor):
        """Repaint the sky and the ground for whichever mission is loading."""
        for item, is_ceiling, bright in self.bg_shade:
            rgb = ceiling if is_ceiling else floor
            self.canvas.itemconfigure(
                item, fill="#%02x%02x%02x" % dim(rgb, bright))

    def _build_stars(self):
        """
        A sky full of stars, for the missions that have one.

        Stars are infinitely far away, so they have no position - only a
        bearing and a height. That makes them cheaper than anything else on
        screen: turning slides them across, looking up and down carries them
        with the horizon, and walking does not move them at all, which is
        exactly right.
        """
        maker = random.Random(20250806)     # fixed, so the sky never reshuffles
        self.star_field = []
        self.star_items = []
        for _ in range(STAR_COUNT):
            bearing = maker.uniform(0, 2 * math.pi)
            height = maker.uniform(0.06, 0.92)      # up from the horizon
            size = maker.choice((1, 1, 1, 2, 2, 3))
            shade = maker.randint(150, 255)
            self.star_field.append((bearing, height, size))
            self.star_items.append(self.canvas.create_rectangle(
                0, 0, 0, 0, fill="#%02x%02x%02x" % (shade, shade, min(255, shade + 6)),
                outline="", state="hidden"))
        self._stars_shown = False

    def draw_stars(self):
        """Place every star that is currently in front of you."""
        if not self.env["stars"]:
            if self._stars_shown:
                for item in self.star_items:
                    self.canvas.itemconfigure(item, state="hidden")
                self._stars_shown = False
            return

        angle = self.angle
        horizon = self.horizon
        plane_len = self.plane_len
        half_w = SCREEN_W * 0.5
        shown = 0

        for index, (bearing, height, size) in enumerate(self.star_field):
            # How far round from straight ahead is it?
            offset = (bearing - angle + math.pi) % (2 * math.pi) - math.pi
            if offset < -1.2 or offset > 1.2:       # behind you, or nearly
                continue
            screen_x = half_w * (1.0 + math.tan(offset) / plane_len)
            if screen_x < 0 or screen_x > SCREEN_W:
                continue
            screen_y = horizon - height * SCREEN_H * 0.5
            if screen_y < 0 or screen_y > horizon:
                continue
            item = self.star_items[index]
            self._place(item, screen_x, screen_y,
                        screen_x + size, screen_y + size)
            if not self._stars_shown:
                self.canvas.itemconfigure(item, state="normal")
            shown += 1

        if not self._stars_shown and shown:
            self._stars_shown = True

    def _build_wall_columns(self):
        """
        The wall stripes.

        Each ray owns a small stack of rectangles rather than a single one, so
        a column can be painted as several horizontal bands - see the WALL
        TEXTURE notes up in Section 2. They are all made once here and only
        ever moved afterwards.
        """
        self.column_items = []
        self.column_used = [0] * NUM_COLUMNS    # bands each column drew last frame
        for i in range(NUM_COLUMNS):
            x0 = i * self.column_w
            # +1 pixel of overlap so there are no hairline gaps between stripes
            x1 = x0 + self.column_w + 1
            self.column_items.append([
                self.canvas.create_rectangle(x0, 0, x1, 0,
                                             fill="#000000", outline="")
                for _ in range(MAX_BANDS)])

        # Where every band was put last frame, and what colour it was, so an
        # unchanged band can be skipped. Kept as three flat lists rather than
        # a list of lists - one index calculation beats two lookups.
        self.column_fill = ["#000000"] * (NUM_COLUMNS * MAX_BANDS)
        self._band_y0 = [-1] * (NUM_COLUMNS * MAX_BANDS)
        self._band_y1 = [-1] * (NUM_COLUMNS * MAX_BANDS)

    # -- display scaling ---------------------------------------------------
    #
    # Everything in the game thinks in a fixed 640x400 view. Blowing that up
    # for fullscreen happens right at the last moment: every coordinate is
    # multiplied on its way out to the canvas, and nothing upstream has to
    # know. That keeps all the maths, the collision and the sprite work in one
    # coordinate system - and, crucially, it does not draw a single extra
    # shape, so a bigger picture costs almost nothing in frame rate.

    def _place(self, item, *coords):
        """Move a shape, scaling from view coordinates to screen ones."""
        k = self.scale
        if k != 1:
            coords = tuple(value * k for value in coords)
        self._tk_call(self._canvas_name, "coords", item, *coords)

    def _capture_base_style(self):
        """
        Remember every font and line width, so scaling can multiply them.

        Text items are deliberately left out of the width list. On a text item
        `width` is not a line thickness at all - it is the WRAP width, and 0
        means "never wrap". Scaling that to 1 tells Tk to wrap after a single
        pixel, which stands every label on its end, one letter per line.
        """
        self._base_fonts = {}
        self._base_widths = {}
        for item in self.canvas.find_all():
            if self.canvas.type(item) == "text":
                self._base_fonts[item] = str(self.canvas.itemcget(item, "font"))
                continue
            try:
                width = float(self.canvas.itemcget(item, "width"))
            except (TypeError, ValueError):
                continue
            if width > 0:
                self._base_widths[item] = width

    def apply_scale(self, scale):
        """
        Resize the whole picture by a whole-number factor.

        Whole numbers only, on purpose: at 2x every pixel becomes an exact
        2x2 block, so the art stays crisp instead of going soft the way a
        fractional stretch would.
        """
        scale = max(1, int(scale))
        if scale == self.scale:
            return
        factor = scale / self.scale
        self.scale = scale

        self.canvas.configure(width=SCREEN_W * scale,
                              height=(SCREEN_H + HUD_H) * scale)
        # Everything already on the canvas moves with one call. Anything the
        # game repositions every frame goes through _place and is scaled there.
        self.canvas.scale("all", 0, 0, factor, factor)

        for item, font in self._base_fonts.items():
            self.canvas.itemconfigure(item, font=_scaled_font(font, scale))
        for item, width in self._base_widths.items():
            self.canvas.itemconfigure(item, width=width * scale)

        # The wall stripes cache their positions as whole screen pixels, so
        # those really are stale and have to go. The sprite and blood
        # bookkeeping must NOT be cleared: it records what is currently
        # visible, not where things are, and canvas.scale has already moved
        # them. Forgetting it would leave whatever was on screen at the moment
        # of the switch stranded there for good.
        self._reset_column_pool()
        self._hud_cache.clear()

    def _reset_column_pool(self):
        """
        Put every wall stripe away and forget where they were.

        Called when the stripe width changes. Whichever ones the new width
        needs will be placed again on the very next frame; the rest stay parked
        off the canvas where they cost nothing to skip over.
        """
        tk_call = self._tk_call
        canvas = self._canvas_name
        for column in self.column_items:
            for item in column:
                tk_call(canvas, "coords", item, PARKED, PARKED, PARKED, PARKED)
        for i in range(len(self._band_y0)):
            self._band_y0[i] = PARKED
            self._band_y1[i] = PARKED
        for i in range(len(self.column_used)):
            self.column_used[i] = 0

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

    def _build_boss_bar(self):
        """A health bar across the top, shown only while a boss is awake."""
        self.boss_items = []
        self.boss_name = self.canvas.create_text(
            SCREEN_W / 2, 20, text="", fill="#e8b24a",
            font=("Courier", 11, "bold"), state="hidden")
        self.boss_back = self.canvas.create_rectangle(
            120, 30, SCREEN_W - 120, 42, fill="#140c0c", outline="#5a3a34",
            state="hidden")
        self.boss_bar = self.canvas.create_rectangle(
            121, 31, SCREEN_W - 121, 41, fill="#c8402c", outline="",
            state="hidden")
        self.boss_items = [self.boss_name, self.boss_back, self.boss_bar]
        self._boss_shown = False

    def draw_boss_bar(self):
        """Find the boss, if there is one awake, and show how it is doing."""
        boss = None
        for monster in self.monsters:
            if monster.alive and monster.awake and monster.info.get("boss"):
                boss = monster
                break

        if boss is None:
            if self._boss_shown:
                for item in self.boss_items:
                    self.canvas.itemconfigure(item, state="hidden")
                self._boss_shown = False
            return

        if not self._boss_shown:
            for item in self.boss_items:
                self.canvas.itemconfigure(item, state="normal")
            self.canvas.itemconfigure(self.boss_name,
                                      text="HIGH VALUE TARGET - %s"
                                           % boss.info["name"])
            self._boss_shown = True

        fraction = max(0.0, boss.hp / boss.max_hp)
        width = (SCREEN_W - 242) * fraction
        self._place(self.boss_bar, 121, 31, 121 + width, 41)

    def _build_menu(self):
        """The main menu: a title, a stack of entries, and a footer."""
        self.menu_items = []
        # Covers the status bar as well as the view - a health bar and an
        # ammo count sitting under the main menu look like a bug.
        self.menu_bg = self.canvas.create_rectangle(
            0, 0, SCREEN_W, SCREEN_H + HUD_H, fill="#05070c", outline="",
            state="hidden")
        self.menu_title = self.canvas.create_text(
            SCREEN_W / 2, 62, text="TRIDENT", fill="#ffcc33",
            font=("Courier", 38, "bold"), state="hidden")
        self.menu_sub = self.canvas.create_text(
            SCREEN_W / 2, 100, text="", fill="#6f7683",
            font=("Courier", 9), state="hidden")

        self.menu_rows = []
        for index in range(len(MENU_ENTRIES)):
            y = MENU_TOP + index * MENU_SPACING
            marker = self.canvas.create_polygon(0, 0, 0, 0, 0, 0,
                                                fill="#ffcc33", outline="",
                                                state="hidden")
            label = self.canvas.create_text(
                SCREEN_W / 2 - 20, y, text="", fill="#d8d8e0", anchor="e",
                font=("Courier", 15, "bold"), state="hidden")
            value = self.canvas.create_text(
                SCREEN_W / 2 + 20, y, text="", fill="#7de07d", anchor="w",
                font=("Courier", 15, "bold"), state="hidden")
            self.menu_rows.append((marker, label, value))

        self.menu_hint = self.canvas.create_text(
            SCREEN_W / 2, SCREEN_H + 22, text="", fill="#6f7683",
            font=("Courier", 9), state="hidden")
        self.menu_best = self.canvas.create_text(
            SCREEN_W / 2, SCREEN_H + 46, text="", fill="#8a8a96",
            font=("Courier", 9), state="hidden")

        self.menu_items = [self.menu_bg, self.menu_title, self.menu_sub,
                           self.menu_hint, self.menu_best]
        for row in self.menu_rows:
            self.menu_items.extend(row)

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

        # Solid, not stippled. A half-dotted backdrop looked fine over dark
        # concrete but vanished against the moon's white habitat panels, which
        # showed straight through the gaps and left the map unreadable.
        self.map_bg = self.canvas.create_rectangle(0, 0, 0, 0, fill="#0a0c11",
                                                   outline="#3c3c46")
        self.minimap_items.append(self.map_bg)

        # Enough bars for the worst case: a row that alternates wall, floor,
        # wall, floor all the way across needs one bar per two squares.
        biggest = max(len(lvl["grid"]) * (len(lvl["grid"][0]) // 2 + 1)
                      for lvl in LEVELS)
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
        self.root.bind("<FocusOut>", lambda _e: self._on_focus_lost())

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

    def _on_focus_lost(self):
        """
        Alt-tabbing away must not leave you running forwards for ever.

        Keys held when the window loses focus never send a release to us, so
        without this you come back to find yourself jammed against a wall.
        """
        self._release_mouse()
        self.keys.clear()
        self._pending_release.clear()
        self.firing = False
        self.aiming = False

    def quit(self):
        """Shut down cleanly, from the Esc key or the window's close button."""
        self.running = False
        try:
            self.root.destroy()
        except tk.TclError:
            pass            # already gone

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
        """
        Put the pointer back in the middle of the view.

        This is the one part of the game that depends on something the
        operating system might refuse to do. If warping is not available we
        fall back to keyboard turning rather than leaving the player with a
        view that will not move.
        """
        self._warping = True
        try:
            self.canvas.event_generate("<Motion>", warp=True,
                                       x=int(SCREEN_W * self.scale // 2),
                                       y=int(SCREEN_H * self.scale // 2))
        except tk.TclError:
            self._warping = False
            self.mouse_enabled = False
            self._release_mouse()
            self.say("Mouse aiming is not available here - use the arrow keys",
                     5.0)

    def _on_mouse_down(self, event):
        if self.state == STATE_MENU:
            row = int(round((event.y / self.scale - MENU_TOP) / MENU_SPACING))
            if 0 <= row < len(MENU_ENTRIES) and self._menu_enabled(
                    MENU_ENTRIES[row]):
                self.menu_index = row
                self.draw_menu()
                self._menu_choose()
            return
        if self.state == STATE_TITLE:
            self.open_menu()
            return
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

        # The pointer reports screen pixels; the game thinks in view units.
        k = self.scale
        move_x = (event.x - SCREEN_W * k // 2) / k
        move_y = (event.y - SCREEN_H * k // 2) / k
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
        if self.state == STATE_MENU:
            self._menu_key(key)
            return

        if key == "escape":
            # First Esc hands the pointer back, so you are never trapped in
            # the window. A second Esc actually quits.
            if self.mouse_captured:
                self._release_mouse()
                self.say("Mouse released - click the window to take it back")
                return
            # Stop the loop first: if we destroyed the window while a tick was
            # still booked in, that tick would wake up and find nothing to draw.
            self.quit()
            return

        if self.state == STATE_TITLE:
            self.open_menu()
            return

        # Backspace or Tab-out of a finished mission returns to the menu.
        if key in ("m", "backspace") and self.state in (STATE_DEAD,
                                                        STATE_CLEARED,
                                                        STATE_WON):
            self.open_menu()
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

    def _menu_key(self, key):
        if key in ("up", "w"):
            self._menu_move(-1)
        elif key in ("down", "s"):
            self._menu_move(1)
        elif key in ("left", "a"):
            self._menu_adjust(-1)
        elif key in ("right", "d"):
            self._menu_adjust(1)
        elif key in ("return", "kp_enter", "space"):
            self._menu_choose()
        elif key == "escape":
            # Esc backs out of the menu if there is a game to go back to,
            # otherwise it quits.
            if self.level_index is not None and self.total_monsters:
                self.close_menu()
                self.state = STATE_PLAYING
                self._capture_mouse()
            else:
                self.quit()

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
                    self.monsters.append(
                        Monster(char, centre_x, centre_y, self.difficulty))
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
        rules = DIFFICULTIES[self.difficulty]
        self.max_hp = int(PLAYER_MAX_HP * rules["health"])
        self.hp = self.max_hp
        self.ammo = int(START_AMMO * rules["ammo"])
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
        self.vel_x = 0.0
        self.vel_y = 0.0
        self.env = dict(DEFAULT_ENV)
        self.env.update(level.get("env", {}))
        self._recolour_background(self.env["ceiling"], self.env["floor"])
        # Specks fall slower in low gravity, so they also have to be allowed
        # to live longer - otherwise they wink out mid-air on their timer and
        # the gravity setting makes no visible difference at all.
        gravity = max(0.05, self.env["gravity"])
        self._blood_pull = BLOOD_GRAVITY * gravity
        self._blood_hang = min(3.0, 1.0 / gravity ** 0.5)
        # Actually take the bar down, rather than just forgetting it is up -
        # otherwise it stays on screen into a mission that has no boss.
        for item in getattr(self, "boss_items", ()):
            self.canvas.itemconfigure(item, state="hidden")
        self._boss_shown = False
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
            # A defensive floor. Walls cannot actually get this close - you
            # would have to be standing inside one - but dividing the screen
            # height by a near-zero distance would produce an absurd number
            # to hand to the drawing code.
            if distance < 0.05:
                distance = 0.05

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
        want_x = (self.dir_x * forward + (-self.dir_y) * strafe * STRAFE_SCALE) * speed
        want_y = (self.dir_y * forward + (self.dir_x) * strafe * STRAFE_SCALE) * speed

        # Momentum. On Earth `accel` is high enough that this reaches the
        # wanted speed within a single frame and behaves exactly as it always
        # has. Turn it down and you get the moon: a slow drift up to speed and
        # a long glide to a stop, because there is very little holding you to
        # the ground.
        catch_up = min(1.0, self.env["accel"] * dt)
        self.vel_x += (want_x - self.vel_x) * catch_up
        self.vel_y += (want_y - self.vel_y) * catch_up
        move_x = self.vel_x * dt
        move_y = self.vel_y * dt

        # Try each axis on its own. If one is blocked the other still works,
        # which is what makes you slide along a wall instead of sticking to it.
        if move_x:
            if self.can_stand(self.px + move_x, self.py, PLAYER_RADIUS):
                self.px += move_x
            else:
                self.vel_x = 0.0        # you stopped; stop drifting into it
        if move_y:
            if self.can_stand(self.px, self.py + move_y, PLAYER_RADIUS):
                self.py += move_y
            else:
                self.vel_y = 0.0

        self._update_camera_position()

        # Head bob while walking
        if forward or strafe or abs(self.vel_x) + abs(self.vel_y) > 0.4:
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
                random.uniform(0.55, 1.0) * BLOOD_LIFETIME * self._blood_hang,
            ])

    def damage_monster(self, monster, amount):
        monster.hp -= amount
        monster.hurt_flash = 0.09
        monster.awake = True
        big = monster.info.get("boss", False)
        self.spray_blood(monster.x, monster.y,
                         BLOOD_ON_HIT * (2 if big else 1),
                         force=1.5 if big else 1.0)
        if monster.hp <= 0 and monster.alive:
            monster.alive = False
            monster.death_timer = 0.0
            self.spray_blood(monster.x, monster.y,
                             BLOOD_ON_DEATH * (2 if big else 1),
                             force=2.0 if big else 1.35)
            self.kills += 1
            self.level_score += int(monster.info["score"]
                                    * DIFFICULTIES[self.difficulty]["score"])
            if monster.info.get("boss"):
                self.say("%s IS DOWN. Move to extraction."
                         % monster.info["name"], 5.0)
            else:
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
                    self.hurt_player(monster.ranged_damage)
                    monster.attack_timer = info["attack_cooldown"]
                elif distance <= info["melee_range"] and info["melee_damage"]:
                    self.hurt_player(monster.melee_damage)
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
            speed = monster.speed * direction * dt
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
            speck[5] -= self._blood_pull * dt   # gravity pulls the kick down
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
                              "press R to reinsert, M for the menu")

    def check_pickups(self):
        for pickup in self.pickups:
            if pickup.taken:
                continue
            if math.hypot(pickup.x - self.px, pickup.y - self.py) > 0.55:
                continue

            info = pickup.info
            if info["heal"]:
                if self.hp >= self.max_hp:
                    continue        # leave it on the floor for later
                self.hp = min(self.max_hp, self.hp + info["heal"])
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
            self.record_progress(self.level_index, self.total_score)

            if self.level_index + 1 < len(LEVELS):
                self.state = STATE_CLEARED
                self._set_overlay("AREA SECURE",
                                  "score: %d      total: %d"
                                  % (earned, self.total_score),
                                  "press N for the next mission, M for the menu")
            else:
                self.state = STATE_WON
                self._set_overlay("MISSION COMPLETE",
                                  "final score: %d" % self.total_score,
                                  "press N to run it again, M for the menu")
            return

    def say(self, text, seconds=2.2):
        self.message = text
        self.message_timer = seconds

    # -- drawing -----------------------------------------------------------

    def draw_background(self):
        """Slide the ceiling/floor bands to wherever the horizon now is."""
        offset = self.horizon - self.half_h
        for item, y0, y1 in self.bg_items:
            self._place(item, 0, y0 + offset, SCREEN_W, y1 + offset)

    def draw_walls(self, rays):
        """
        Paint the wall stripes. This is where nearly all the frame time goes,
        so it is written for speed rather than for looks.

        Three things earn most of that speed:

        * It talks to Tcl directly instead of going through tkinter's Canvas
          methods. Those methods are convenient, but every one of them costs a
          layer of Python argument handling, and there are thousands of calls
          in here.
        * Coordinates are whole numbers. Integers marshal into Tcl faster than
          floats, and a wall that has only shifted a fraction of a pixel now
          rounds to the same place it was last frame - which feeds straight
          into the third trick.
        * Nothing that has not moved is touched at all. Each band remembers
          where it was put last frame; if it has not changed, the call is
          skipped entirely. Standing still costs almost nothing.
        """
        tk_call = self._tk_call
        canvas = self._canvas_name
        horizon = self.horizon
        column_w = self.column_w
        items = self.column_items
        fills = self.column_fill
        cache_y0 = self._band_y0
        cache_y1 = self._band_y1
        used = self.column_used
        tiers = WALL_BAND_TIERS
        seams = WALL_SEAMS
        bright = WALL_SHADES_BRIGHT
        dark = WALL_SHADES_DARK
        lookup = SHADE_LOOKUP
        top_tier = self.detail_tier          # raised when the machine struggles
        step = self.column_step
        half_step = step // 2
        last_ray = NUM_COLUMNS - 1
        k = self.scale                      # view units -> screen units
        span = (int(step * column_w) + 1) * k
        limit = SCREEN_H * k

        for i in range(0, NUM_COLUMNS, step):
            # One stripe can stand for several rays. Take the middle one so the
            # error is shared evenly across the stripe rather than piling up at
            # one edge. Rays are all still cast either way - they are cheap,
            # and the sprites need the full depth buffer to clip against.
            distance, side, char, wall_x = rays[min(i + half_step, last_ray)]
            slot = items[i]
            base_index = i * MAX_BANDS

            if distance >= MAX_DEPTH:
                # Nothing hit: collapse the stripe so only sky/floor shows
                for band in range(used[i]):
                    spot = base_index + band
                    if cache_y0[spot] != PARKED:
                        tk_call(canvas, "coords", slot[band],
                                PARKED, PARKED, PARKED, PARKED)
                        cache_y0[spot] = PARKED
                        cache_y1[spot] = PARKED
                used[i] = 0
                continue

            height = SCREEN_H / distance
            top = horizon - height * 0.5
            x0 = int(i * column_w) * k
            x1 = x0 + span

            shades = (bright if side == 0 else dark)[char]
            # Distance fog, plus the free across-the-wall detail: a groove in
            # the wall face simply reads as a few steps darker.
            base = (lookup[int(distance * SHADE_STEPS_PER_UNIT)]
                    + seams[char][int(wall_x * SEAM_STEPS)])

            wall_tiers = tiers[char]
            tier = tier_for_height(height) + top_tier
            if tier >= len(wall_tiers):
                tier = len(wall_tiers) - 1
            bands = wall_tiers[tier]

            drawn = 0
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
                y0 = int(y0 * k)
                y1 = int(y1 * k)
                if y1 <= y0:
                    continue

                level = base + step
                if level < 0:
                    level = 0
                elif level >= SHADE_LEVELS:
                    level = SHADE_LEVELS - 1

                spot = base_index + drawn
                if cache_y0[spot] != y0 or cache_y1[spot] != y1:
                    tk_call(canvas, "coords", slot[drawn], x0, y0, x1, y1)
                    cache_y0[spot] = y0
                    cache_y1[spot] = y1

                colour = shades[level]
                if colour != fills[spot]:
                    tk_call(canvas, "itemconfigure", slot[drawn], "-fill", colour)
                    fills[spot] = colour
                drawn += 1

            for band in range(drawn, used[i]):
                spot = base_index + band
                if cache_y0[spot] != PARKED:
                    tk_call(canvas, "coords", slot[band],
                            PARKED, PARKED, PARKED, PARKED)
                    cache_y0[spot] = PARKED
                    cache_y1[spot] = PARKED
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
            self._place(item, -40, -40, -30, -30)
        self._on_screen = used

    def _draw_one_sprite(self, slot, kind, info, depth, screen_x, used,
                         flash=False, squash=0.0, level_override=None):
        canvas = self.canvas
        place = self._place
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

            place(item, x0, y0, x1, y1)
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
            self._place(item, screen_x - width * 0.5, floor_y - width * 0.16,
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
            self._place(item, screen_x - size, screen_y - size,
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
            self._place(item, centre + x0, base + y0, centre + x1, base + y1)

        # --- the sight picture ---
        if ads > SIGHT_SHOW_AT:
            # The tube starts oversized and settles to its proper size, which
            # reads as the optic coming up to your eye.
            settle = (ads - SIGHT_SHOW_AT) / (1.0 - SIGHT_SHOW_AT)
            radius = SIGHT_RADIUS * (1.55 - 0.55 * settle)
            outer = radius + SIGHT_RING_WIDTH * 0.5
            self._place(self.sight_ring, aim_x - outer, aim_y - outer,
                        aim_x + outer, aim_y + outer)
            self._place(self.sight_edge, aim_x - radius, aim_y - radius,
                        aim_x + radius, aim_y + radius)
            self._place(self.dot_halo, aim_x - 7, aim_y - 7, aim_x + 7, aim_y + 7)
            self._place(self.dot_item, aim_x - 3, aim_y - 3, aim_x + 3, aim_y + 3)
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
                self._place(item, aim_x + sx * near, aim_y + sy * near,
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
                self._place(item, 0, 0, 0, 0)
        else:
            for item, (x0, y0, x1, y1) in zip(self.cross_items, self.cross_parts):
                self._place(item, aim_x + x0, aim_y + y0, aim_x + x1, aim_y + y1)

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
            self._place(self.flash_item, *points)
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

        fraction = self.hp / self.max_hp
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

        self._place(self.hp_bar, 19, SCREEN_H + 59,
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
            self._place(item, x - 1.5, y - 1.5, x + 1.5, y + 1.5)
            canvas.itemconfigure(item, state="normal",
                                 fill="#ff5555" if monster.awake else "#a04040")
            index += 1

        for pickup in self.pickups:
            if pickup.taken or index >= len(self.minimap_dots):
                continue
            item = self.minimap_dots[index]
            x = ox + pickup.x * cell
            y = oy + pickup.y * cell
            self._place(item, x - 1.5, y - 1.5, x + 1.5, y + 1.5)
            canvas.itemconfigure(item, state="normal",
                                 fill="#55dd55" if pickup.kind == "h" else "#ddbb44")
            index += 1

        while index < len(self.minimap_dots):
            canvas.itemconfigure(self.minimap_dots[index], state="hidden")
            index += 1

        x = ox + self.px * cell
        y = oy + self.py * cell
        self._place(self.map_player, x - 2, y - 2, x + 2, y + 2)
        self._place(self.map_facing, x, y,
                    x + self.dir_x * cell * 1.9, y + self.dir_y * cell * 1.9)

    def _draw_minimap_walls(self):
        """Redrawn only when a level loads, never during play."""
        cell = self.map_cell
        ox, oy = self.map_x, self.map_y
        self._place(self.map_bg, ox - 3, oy - 3,
                    ox + self.map_w * cell + 3, oy + self.map_h * cell + 3)

        # Draw each unbroken run of identical squares as ONE bar rather than a
        # square per cell. A 24x20 map is mostly long straight walls, so this
        # turns five hundred shapes into a few dozen - and every shape on the
        # canvas is one more the drawing library has to walk past on a repaint,
        # whether or not it changed.
        index = 0
        for row in range(self.map_h):
            col = 0
            while col < self.map_w:
                char = self.grid[row][col]
                if char == ".":
                    col += 1
                    continue
                run_end = col + 1
                while run_end < self.map_w and self.grid[row][run_end] == char:
                    run_end += 1
                if index < len(self.minimap_wall_items):
                    item = self.minimap_wall_items[index]
                    x = ox + col * cell
                    y = oy + row * cell
                    self._place(item, x, y, x + (run_end - col) * cell, y + cell)
                    self.canvas.itemconfigure(
                        item, state="normal",
                        fill="#3fd46a" if char == "X" else "#5a5a6a")
                    index += 1
                col = run_end

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

    # -- the main menu -----------------------------------------------------

    def open_menu(self):
        self.state = STATE_MENU
        self._release_mouse()
        self._hide_overlay()
        self.canvas.configure(cursor="")
        self.draw_menu()
        for item in self.menu_items:
            self.canvas.itemconfigure(item, state="normal")
        self.canvas.tag_raise(self.menu_bg)
        for item in self.menu_items[1:]:
            self.canvas.tag_raise(item)

    def close_menu(self):
        for item in self.menu_items:
            self.canvas.itemconfigure(item, state="hidden")

    def _menu_value(self, entry):
        """The right hand side of a menu row, or None if it has no value."""
        if entry == "difficulty":
            return DIFFICULTIES[self.difficulty]["name"]
        if entry == "quality":
            return QUALITY_LEVELS[self.quality][0]
        if entry == "fullscreen":
            return "ON" if self.fullscreen else "OFF"
        if entry == "continue":
            return "OP %d" % (self.save["continue_level"] + 1)
        return None

    def _menu_label(self, entry):
        return {"continue": "CONTINUE", "new": "NEW MISSION",
                "difficulty": "DIFFICULTY", "quality": "DETAIL",
                "fullscreen": "FULLSCREEN", "quit": "QUIT"}[entry]

    def _menu_enabled(self, entry):
        # Nothing to continue from until you have actually cleared something.
        return entry != "continue" or self.save["continue_level"] > 0

    def draw_menu(self):
        canvas = self.canvas
        canvas.itemconfigure(
            self.menu_sub,
            text="%s      %d hostiles down      %d missions cleared"
                 % (DIFFICULTIES[self.difficulty]["name"],
                    self.save.get("kills_total", 0),
                    self.save["missions_cleared"]))

        for index, entry in enumerate(MENU_ENTRIES):
            marker, label, value = self.menu_rows[index]
            selected = index == self.menu_index
            enabled = self._menu_enabled(entry)

            if enabled:
                colour = "#ffcc33" if selected else "#d8d8e0"
            else:
                colour = "#4a4d55"
            canvas.itemconfigure(label, text=self._menu_label(entry),
                                 fill=colour)

            shown = self._menu_value(entry)
            canvas.itemconfigure(value, text=shown or "",
                                 fill="#7de07d" if enabled else "#3f4650")

            y = MENU_TOP + index * MENU_SPACING
            if selected:
                left = SCREEN_W / 2 - 150
                self._place(marker, left, y - 7, left + 12, y, left, y + 7)
            else:
                self._place(marker, 0, 0, 0, 0, 0, 0)

        canvas.itemconfigure(
            self.menu_hint,
            text="W/S or ARROWS to choose    ENTER to pick    A/D to change")
        canvas.itemconfigure(
            self.menu_best, text="best score  %d" % self.save["best_score"])

    def _menu_move(self, delta):
        for _ in range(len(MENU_ENTRIES)):
            self.menu_index = (self.menu_index + delta) % len(MENU_ENTRIES)
            if self._menu_enabled(MENU_ENTRIES[self.menu_index]):
                break
        self.draw_menu()

    def _menu_adjust(self, delta):
        """Left/right on a row that has a value cycles through its options."""
        entry = MENU_ENTRIES[self.menu_index]
        if entry == "difficulty":
            self.difficulty = (self.difficulty + delta) % len(DIFFICULTIES)
            self.save["difficulty"] = self.difficulty
        elif entry == "quality":
            self.set_quality((self.quality + delta) % len(QUALITY_LEVELS))
            self.save["quality"] = self.quality
        elif entry == "fullscreen":
            self.toggle_fullscreen()
        else:
            return
        self.store_save()
        self.draw_menu()

    def _menu_choose(self):
        entry = MENU_ENTRIES[self.menu_index]
        if not self._menu_enabled(entry):
            return
        if entry == "quit":
            self.quit()
        elif entry in ("difficulty", "quality", "fullscreen"):
            self._menu_adjust(1)
        elif entry == "new":
            self.total_score = 0
            self.start_mission(0)
        elif entry == "continue":
            self.total_score = self.save["continue_score"]
            self.start_mission(self.save["continue_level"])

    def start_mission(self, index):
        self.close_menu()
        self._hide_overlay()
        self.load_level(index)
        self.state = STATE_PLAYING
        self._capture_mouse()

    # -- settings ----------------------------------------------------------

    def toggle_fullscreen(self):
        """
        Borderless fullscreen.

        The view itself stays the size it is and sits in the middle of a black
        screen. Stretching it to fill a modern monitor would mean drawing four
        or five times as many wall stripes, and frame rate is the one thing
        this game has none of to spare - so it buys you the clean, undistracted
        screen without costing anything to run.
        """
        self.fullscreen = not self.fullscreen
        try:
            self.root.attributes("-fullscreen", self.fullscreen)
        except tk.TclError:
            self.fullscreen = False
            self.say("Fullscreen is not available here", 3.0)
            return

        if self.fullscreen:
            # Blow the picture up by the largest whole number that still fits.
            # Whole numbers keep every pixel an exact square block instead of
            # going soft, and because it is only a multiply on the way out to
            # the canvas it does not draw one extra shape - a bigger picture
            # is very nearly free.
            self.apply_scale(min(self.root.winfo_screenwidth() // SCREEN_W,
                                 self.root.winfo_screenheight()
                                 // (SCREEN_H + HUD_H)))
            self.canvas.pack_forget()
            self.canvas.place(relx=0.5, rely=0.5, anchor="center")
            self.root.configure(bg="#000000")
        else:
            self.apply_scale(1)
            self.canvas.place_forget()
            self.canvas.pack()
        self.save["fullscreen"] = self.fullscreen

    def store_save(self):
        """Write settings and progress out, quietly giving up if we cannot."""
        self.save["difficulty"] = self.difficulty
        self.save["quality"] = self.quality
        self.save["fullscreen"] = self.fullscreen
        if not write_save(self.save) and not self._save_warned:
            self._save_warned = True
            self.say("Cannot save here - settings will not be remembered", 4.0)

    def record_progress(self, cleared_index, score):
        """Remember how far you got, so CONTINUE has something to offer."""
        self.save["missions_cleared"] = self.save.get("missions_cleared", 0) + 1
        self.save["kills_total"] = self.save.get("kills_total", 0) + self.kills
        self.save["best_score"] = max(self.save["best_score"], score)
        nxt = min(len(LEVELS) - 1, cleared_index + 1)
        if nxt > self.save["continue_level"]:
            self.save["continue_level"] = nxt
            self.save["continue_score"] = score
        elif nxt == self.save["continue_level"]:
            self.save["continue_score"] = max(self.save["continue_score"], score)
        self.store_save()

    # -- the main loop -----------------------------------------------------

    def tick(self):
        """
        Called about 30 times a second. Everything happens here: read the
        clock, move the world forward, redraw, then book the next call.

        We use root.after() rather than a `while True:` loop because tkinter
        needs to get back to its own event loop to process keys and repaint
        the window. A busy loop would just freeze the whole program.

        The body is wrapped so that one bad frame cannot kill the game. If
        something does go wrong the error is reported once and the loop keeps
        running - a playable game with one glitchy frame beats a window that
        has silently stopped redrawing and looks like a crash.
        """
        if not self.running:
            return

        frame_start = time.perf_counter()
        try:
            self._frame(frame_start)
        except tk.TclError:
            return              # the window went away underneath us
        except Exception:       # noqa: BLE001 - keep playing whatever happens
            if not self._reported_error:
                self._reported_error = True
                traceback.print_exc()
                self.say("Something went wrong - see the IDLE window", 6.0)

        elapsed = time.perf_counter() - frame_start
        self._pace()

        # Always leave tkinter a few milliseconds of its own. If a frame
        # overruns the budget and we ask to be woken immediately, frames queue
        # up back to back and the window stops responding to the keyboard -
        # which looks exactly like a freeze.
        delay = max(MIN_FRAME_GAP_MS, FRAME_MS - int(elapsed * 1000))
        try:
            self.root.after(delay, self.tick)
        except tk.TclError:
            self.running = False

    def _frame(self, now):
        """One frame's worth of work: advance the world, then draw it."""
        dt = now - self.last_time
        self.last_time = now
        # If the computer stalls (or you drag the window), pretend it was a
        # normal frame. Otherwise one huge dt would teleport you through walls.
        if dt > 0.1:
            dt = 0.1

        if self.state == STATE_MENU:
            # The menu is a full stop: nothing moves, nothing is redrawn
            # behind it, and the frame costs almost nothing.
            return

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
        self.draw_stars()
        self.draw_walls(rays)
        self.draw_sprites()
        self.draw_blood()
        self.draw_weapon()
        self.draw_hud()
        self.draw_boss_bar()
        self.draw_minimap()

        hurting = "normal" if self.pain_timer > 0 else "hidden"
        if hurting != self._hud_cache.get("pain"):
            self.canvas.itemconfigure(self.pain_item, state=hurting)
            self._hud_cache["pain"] = hurting

        self.frame_times.append(dt)
        if len(self.frame_times) > 30:
            self.frame_times.pop(0)
        if len(self.frame_times) >= 5:
            average = sum(self.frame_times) / len(self.frame_times)
            self.fps = 1.0 / average if average > 0 else 0.0

    def _pace(self):
        """
        Keep the game playable on a machine that cannot keep up.

        This has to measure the REAL gap between frames, not how long our own
        Python took. Those are very different numbers: issuing the drawing
        commands is quick, and then the window system does the actual painting
        afterwards, on its own time. An earlier version of this timed only our
        own work, decided every frame was cheap, and so never stepped the
        quality down on the machines that needed it most.

        `self.fps` is worked out from the measured gap between frames, so it
        includes the painting, and that is what we judge on.
        """
        if len(self.frame_times) < 10:
            return                      # not enough history to judge yet

        if self.fps < QUALITY_DROP_FPS:
            self._detail_score += 1
        elif self.fps > QUALITY_RAISE_FPS:
            self._detail_score -= 1
        else:
            self._detail_score = 0
            return

        if self._detail_score >= DETAIL_PATIENCE:
            self._detail_score = 0
            self._step_quality(-1)
        elif self._detail_score <= -DETAIL_PATIENCE:
            self._detail_score = 0
            self._step_quality(+1)

    def _step_quality(self, direction):
        """
        Move one notch up or down the quality ladder.

        The ladder trades two things. Coarser wall textures cost the least to
        give up. Drawing fewer, wider stripes costs more in sharpness but saves
        far more work, because the real expense is not the arithmetic - it is
        how many separate shapes the drawing library has to keep track of and
        repaint. Halving the stripe count halves that.
        """
        level = self.quality + direction
        if level < 0 or level >= len(QUALITY_LEVELS):
            return
        self.set_quality(level, announce=True)

    def set_quality(self, level, announce=False):
        """Apply one of the preset quality levels."""
        level = max(0, min(len(QUALITY_LEVELS) - 1, level))
        self.quality = level
        name, step, tier = QUALITY_LEVELS[level]
        self.detail_tier = tier
        if step != self.column_step:
            self.column_step = step
            self._reset_column_pool()
        if announce:
            self.say("Detail: %s" % name, 2.0)


# ===========================================================================
#  SECTION 8 - START THE GAME
# ===========================================================================

def main():
    print(__doc__.split("HOW THE 3D EFFECT")[0])
    root = tk.Tk()
    Game(root)
    root.mainloop()


if __name__ == "__main__":
    main()
