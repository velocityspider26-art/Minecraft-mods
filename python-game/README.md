# PyDOOM

A Doom-style first-person shooter written in plain Python, using nothing but
the standard library. It runs in IDLE on a school computer with no installing,
no `pip`, and no downloads.

```
python-game/
├── pydoom.py     the game            (this is the thing you run)
├── selftest.py   optional checks     (proves the maths and maps are correct)
└── README.md     this write-up
```

---

## Running it

1. Open `pydoom.py` in IDLE — **File → Open…**
2. Press **F5** (or **Run → Run Module**)
3. A window appears. **Click on it once** so it takes keyboard focus.

`tkinter`, the library that draws the window, ships with Python and is what
IDLE itself is built on — so if you can open IDLE, you can run this.

### Controls

| Key | Action |
| --- | --- |
| `W` `S` or `↑` `↓` | walk forward / backward |
| `A` `D` | strafe (step sideways without turning) |
| `←` `→` or `Q` `E` | turn |
| `Shift` | run |
| `Space` or `Ctrl` | fire the shotgun |
| `Tab` | show / hide the minimap |
| `R` | restart the level |
| `N` | next level (once you have cleared one) |
| `Esc` | quit |

### The goal

Kill every monster on the level, then walk onto the glowing yellow exit pad.
The pad stays sealed until the last monster is down — the status bar tells you
how many are left. Grab medikits and shell boxes on the way.

There are two maps, two kinds of monster (an **Imp** that charges you, and a
**Guard** that hangs back and shoots), and a score that carries between levels.

---

## How the 3D works

**There is no 3D in this game.** The world is a flat grid of text characters:

```
"#......#.........#.....#",
"#..P...#....h....#..e..#",
"#......#.........#.....#",
```

`#` is a wall, `.` is floor, `P` is where you start, `e` is a monster. The 3D
you see on screen is an illusion built by an old trick called **raycasting**,
the same one Wolfenstein 3D used in 1992.

### 1. One ray per stripe of the screen

The view is 640 pixels wide. Instead of drawing 640 separate things, the game
slices it into **320 vertical stripes** and fires one imaginary ray out into
the map for each stripe:

```
        the map, seen from above
        ┌───────────────────────┐
        │ #######################│
        │ #                     #│         what you see
        │ #    ＼   |   ／       #│      ┌─────────────┐
        │ #     ＼  |  ／        #│      │▓▓         ▓▓│
        │ #      ＼ | ／         #│      │▓▓▓  ▒▒▒  ▓▓▓│
        │ #       ＼|／          #│      │▓▓▓▓ ▒▒▒ ▓▓▓▓│
        │ #         P            #│      └─────────────┘
        └───────────────────────┘       near = tall, far = short
```

Whichever wall a ray hits, we measure how far it travelled. Then:

```python
height = SCREEN_H / distance
```

A wall one square away fills the screen. A wall ten squares away is a tenth as
tall. Draw all 320 stripes side by side and your brain assembles a corridor.

### 2. Finding the wall quickly: the DDA algorithm

The naive way to find what a ray hits is to creep along it in tiny steps,
checking the grid each time. That is slow and it can miss thin walls.

Instead `cast_rays()` uses **DDA** (Digital Differential Analysis). Because the
world is a grid, a ray can only enter a new square by crossing a vertical grid
line or a horizontal one. So the algorithm repeatedly asks *"which grid line do
I reach first?"* and jumps straight to it:

```python
while True:
    if side_x < side_y:          # the next vertical line is closer
        side_x += delta_x        # jump to it
        map_col += step_col
    else:                        # the next horizontal line is closer
        side_y += delta_y
        map_row += step_row
    if grid[map_row][map_col] is a wall:
        break
```

The cost depends on how many *squares* the ray crosses, not on how far it
travels, and it lands exactly on the wall face — no overshoot, no gaps.

### 3. Avoiding the fisheye bug

The obvious thing is to use the ray's actual length as the distance. Do that
and flat walls bulge outwards like a fisheye lens, because rays towards the
edge of the screen are travelling diagonally and so are genuinely longer.

The fix is to measure the **perpendicular** distance — how far the wall is
*along the direction you are facing*, not along the ray. The DDA loop produces
that number for free, which is why the code uses `side_x` / `side_y` directly
rather than calling `math.hypot`.

The camera is stored as two vectors, which is what makes this work out neatly:

- `dir` — the unit vector you are facing
- `plane` — perpendicular to `dir`, and its **length sets the field of view**

Sweeping a value from `-1` to `+1` along `plane` and adding it to `dir` gives
every ray direction across the screen, already correctly spaced.

`selftest.py` checks this: it stands the player 1.5 squares from a flat wall
and asserts all 320 columns report the same distance to within 0.001.

### 4. Monsters: billboards and a depth buffer

Monsters and pickups are **billboards** — flat cut-outs that always turn to
face you. Each is projected onto the screen by inverting the camera matrix
(`project()`), which gives a screen x-position and a depth.

But how do you stop a monster showing through a wall it is standing behind?
While casting rays, the game saves every column's wall distance into a
**depth buffer** (`self.zbuffer`). Before drawing a sprite it compares:

```python
if self.zbuffer[column] < depth:
    ...this sprite is behind a wall, skip it
```

The code goes a bit further and walks outwards from the sprite's centre column
to find how much of it is unobstructed, so a monster can be *half* hidden
around a corner rather than popping in and out all at once.

### 5. Making it fast enough in Python

Python is not a fast language, and tkinter's canvas is not a fast renderer.
Two decisions do most of the heavy lifting:

**Create every shape once, then only move it.** Deleting and re-creating 320
rectangles every frame would be hopeless. Instead the game builds its shapes at
start-up — 320 wall stripes, a pool of sprite parts, the gun, the HUD — and
during play only ever calls `coords()` to move them.

**Pre-compute every colour.** Tkinter wants colours as strings like
`"#8a4a34"`. Building those each frame means string formatting 320 times over.
Instead `make_shade_table()` builds all 24 brightness steps of every wall
colour once, at import time, and the render loop just indexes into a list.
There is also a small cache so a stripe that has not changed colour does not
get recoloured at all.

Measured by `selftest.py` in this environment: **3.5 ms of work per frame** at
320 rays, against a 33 ms budget for 30 fps — about ten times more headroom
than needed.

---

## Reading the code

`pydoom.py` is one file in seven labelled sections:

| Section | What is in it |
| --- | --- |
| 1 — Settings | every tunable number, all in one place |
| 2 — Colours | the pre-computed shade tables |
| 3 — Sprites | monsters and pickups, described as lists of coloured boxes |
| 4 — Levels | the maps, as plain text |
| 5 — Objects | the `Monster` and `Pickup` classes |
| 6 — The Game | the `Game` class: input, physics, AI, rendering |
| 7 — Start | `main()` |

The interesting methods, in the order a frame happens:

```
tick()                 once every ~33 ms, driven by root.after()
├── update_player()    read the keys, move, slide along walls, pick things up
├── update_monsters()  wake up, chase or hold position, attack
├── cast_rays()        ← the raycaster, fills the depth buffer
├── draw_walls()       320 stripes
├── draw_sprites()     billboards, sorted far-to-near
├── draw_weapon()      the shotgun, its bob and its muzzle flash
├── draw_hud()         health, ammo, kills, score
└── draw_minimap()
```

---

## Making your own level

Copy one of the entries in `LEVELS` and redraw the grid. The characters are:

| Char | Meaning | | Char | Meaning |
| --- | --- | --- | --- | --- |
| `#` | grey stone wall | | `.` | empty floor |
| `B` | red brick wall | | `P` | player start (exactly one) |
| `M` | metal wall | | `e` | an Imp |
| `G` | dark red wall | | `s` | a Guard |
| `T` | yellow hazard wall | | `h` | a medikit |
| `X` | the exit pad | | `a` | a box of shells |

Two rules: every row must be the same length, and there must be a solid wall
all the way around the outside — otherwise a ray can shoot off the edge of the
world. Run `selftest.py` afterwards and it will check both of those for you,
plus confirm that nothing has ended up walled off where you cannot reach it.

## Tuning it

Everything worth changing is at the top of the file in **Section 1**.

- **Game runs slowly?** Lower `NUM_COLUMNS` from `320` to `160` or `128`. This
  is by far the biggest lever — the picture gets chunkier, nothing else
  changes. There is a live frame-rate readout in the corner of the status bar.
- **Too hard?** Raise `PLAYER_MAX_HP` or `START_AMMO`, or lower `SHOT_COOLDOWN`.
- **Too dark?** Raise `FOG_FAR`.
- **Want a wider view?** Raise `FOV_DEGREES` (Doom itself used about 90).

## Running the checks

```
python selftest.py
```

or just open it in IDLE and press F5. It runs 48 checks covering the level
data, the raycasting maths, and a few hundred frames of the real game loop
driven by fake keyboard input. Everything should say `PASS`.

## Known limitations

These are deliberate — fixing them properly is what a real engine does.

- **Walls are flat colours, not textures.** Sampling a texture per column is
  the natural next step, but it is roughly an order of magnitude more work per
  frame and pure Python would struggle.
- **The floor and ceiling are flat bands.** Real floor-casting means one
  calculation per *pixel* rather than per column.
- **Everything is at one height.** No stairs, no lifts, no looking up or down —
  a grid raycaster has no way to express them.
- **No sound.** The standard library has `winsound`, but only on Windows.
