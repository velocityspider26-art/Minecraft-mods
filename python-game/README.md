# TRIDENT

A modern special-forces first-person shooter written in plain Python, using
nothing but the standard library. You play a Navy SEAL operator clearing a
facility room by room, aiming with the mouse. It runs in IDLE on a school
computer with no installing, no `pip`, and no downloads.

```
python-game/
├── trident.py    the game            (this is the thing you run)
├── selftest.py   optional checks     (proves the maths and maps are correct)
└── README.md     this write-up
```

---

## Running it

1. Open `trident.py` in IDLE — **File → Open…**
2. Press **F5** (or **Run → Run Module**)
3. **Click inside the game window.** That is what hands the mouse over to the
   game. Press **Esc** at any time to get your cursor back.

`tkinter`, the library that draws the window, ships with Python and is what
IDLE itself is built on — so if you can open IDLE, you can run this.

### Controls

| Input | Action |
| --- | --- |
| **Mouse** | aim — click the window to capture it |
| **Left mouse** or `Space` | fire |
| `W` `S` or `↑` `↓` | move forward / backward |
| `A` `D` | strafe (step sideways without turning) |
| `←` `→` or `Q` `E` | turn without the mouse |
| `Shift` | sprint |
| `M` | mouse aiming on / off |
| `Tab` | show / hide the tac-map |
| `R` | restart the mission |
| `N` | next mission (once you have cleared one) |
| `Esc` | release the mouse — press again to quit |

### The mission

Neutralise every hostile in the compound, then reach the green extraction
beacon. It stays sealed until the last hostile is down — the status bar tells
you how many are left. Grab medkits and rifle magazines on the way.

Two operations, two kinds of hostile:

| | Health | Behaviour |
| --- | --- | --- |
| **Tango** | 46 | Rushes you. Dangerous inside 5 metres, poor beyond it. |
| **Marksman** | 62 | Backs off to keep you at rifle range and shoots accurately. |

Your weapon is an M4-style carbine: fully automatic at about 570 rounds per
minute, 24 damage a round, and accurate enough that misses are your fault.
Magazines hold 30. Hold the trigger down and 90 rounds last nine seconds, so
short bursts are worth learning.

### A note on the setting

The hostiles are a made-up armed group — masked figures in plain tactical
gear. There is deliberately nothing in the art or the text that ties them to
any real country, group or people, and no real unit, agency or place is named.

---

## How the 3D works

**There is no 3D in this game.** The world is a flat grid of text characters:

```
"#......#.........#.....#",
"#..P...#....h....#..t..#",
"#......#.........#.....#",
```

`#` is a wall, `.` is floor, `P` is where you start, `t` is a hostile. The 3D
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

### 4. Mouse aiming, without a pointer lock

A real game asks the operating system to **lock** the mouse pointer — hide it,
pin it in place, and report raw movement. Tkinter cannot do that. If we simply
tracked the pointer's position, you would turn a little way, hit the edge of
the window, and stop.

So the game fakes a pointer lock:

1. Every time the mouse moves, measure how far it got **from the centre of the
   window**.
2. Turn by that much.
3. **Teleport the pointer back to the centre**, using
   `event_generate("<Motion>", warp=True, …)`.

Because it is always snapped back, the pointer never reaches an edge and you
can keep turning forever. The cursor is hidden with `cursor="none"` so you
never see it twitching about.

There is one catch, and it is the kind of bug that is very confusing if you
have not met it before: **warping the pointer generates another Motion
event.** If you treat that echo as real input, it looks like the player yanked
the mouse back the other way, and the view fights itself and never turns. The
fix is a one-line flag — `_warping` — set just before the warp and checked at
the top of the handler so the echo can be thrown away.

Looking up and down is a cheat. A grid raycaster genuinely has no concept of
"up", so instead of rotating the camera the game just **slides the whole
horizon line** up or down the screen. Doom did exactly the same thing. Two
details make it hold together:

- Everything that draws must use the *same* horizon — the ceiling and floor
  bands, the wall stripes and the sprites all read `self.horizon`, which is
  computed once per frame. The ceiling and floor bands are also drawn taller
  than the screen so sliding them never drags a bare gap into view.
- Rounds still travel **level**, along the horizon, because the world is flat.
  So the crosshair rides the horizon rather than sitting at a fixed point on
  the screen. That way it always covers what you will actually hit, and the
  carbine moves with it.

### 5. Hostiles: billboards and a depth buffer

Hostiles and pickups are **billboards** — flat cut-outs that always turn to
face you. Each is projected onto the screen by inverting the camera matrix
(`project()`), which gives a screen x-position and a depth.

But how do you stop a hostile showing through a wall it is standing behind?
While casting rays, the game saves every column's wall distance into a
**depth buffer** (`self.zbuffer`). Before drawing a sprite it compares:

```python
if self.zbuffer[column] < depth:
    ...this sprite is behind a wall, skip it
```

The code goes a bit further and walks outwards from the sprite's centre column
to find how much of it is unobstructed, so a hostile can be *half* hidden
around a corner rather than popping in and out all at once.

### 6. Making it fast enough in Python

Python is not a fast language, and tkinter's canvas is not a fast renderer.
Two decisions do most of the heavy lifting:

**Create every shape once, then only move it.** Deleting and re-creating 320
rectangles every frame would be hopeless. Instead the game builds its shapes at
start-up — 320 wall stripes, a pool of sprite parts, the carbine, the HUD —
and during play only ever calls `coords()` to move them.

**Pre-compute every colour.** Tkinter wants colours as strings like
`"#8a4a34"`. Building those each frame means string formatting 320 times over.
Instead `make_shade_table()` builds all 24 brightness steps of every wall
colour once, at import time, and the render loop just indexes into a list.
There is also a small cache so a stripe that has not changed colour does not
get recoloured at all.

Measured by `selftest.py` in this environment: **under 5 ms of work per frame**
at 320 rays, against a 33 ms budget for 30 fps.

---

## Reading the code

`trident.py` is one file in seven labelled sections:

| Section | What is in it |
| --- | --- |
| 1 — Settings | every tunable number, all in one place |
| 2 — Colours | the pre-computed shade tables |
| 3 — Sprites | hostiles and pickups, described as lists of coloured boxes |
| 4 — Levels | the maps, as plain text |
| 5 — Objects | the `Monster` and `Pickup` classes |
| 6 — The Game | the `Game` class: input, physics, AI, rendering |
| 7 — Start | `main()` |

The interesting methods, in the order a frame happens:

```
tick()                 once every ~33 ms, driven by root.after()
├── update_player()    read the keys, move, slide along walls, pick things up
├── update_monsters()  wake up, close in or hold position, attack
├── cast_rays()        ← the raycaster, fills the depth buffer
├── draw_walls()       320 stripes
├── draw_sprites()     billboards, sorted far-to-near
├── draw_weapon()      the carbine, its bob, recoil and muzzle flash
├── draw_hud()         health, rounds, hostiles, score
└── draw_minimap()
```

Mouse input does not run from `tick()` — it arrives as tkinter events
(`_on_mouse_move`, `_on_mouse_down`) and updates the camera immediately, which
is why aiming feels sharper than the 30 fps redraw rate would suggest.

---

## Making your own level

Copy one of the entries in `LEVELS` and redraw the grid. The characters are:

| Char | Meaning | | Char | Meaning |
| --- | --- | --- | --- | --- |
| `#` | bare concrete | | `.` | empty floor |
| `O` | olive drab wall | | `P` | operator start (exactly one) |
| `S` | steel bulkhead | | `t` | a Tango |
| `R` | rusted plate | | `m` | a Marksman |
| `Y` | yellow hazard panel | | `h` | a medkit |
| `X` | extraction point | | `a` | a rifle magazine |

Two rules: every row must be the same length, and there must be a solid wall
all the way around the outside — otherwise a ray can shoot off the edge of the
world. Run `selftest.py` afterwards and it will check both of those for you,
plus confirm that nothing has ended up walled off where you cannot reach it.

## Tuning it

Everything worth changing is at the top of the file in **Section 1**.

- **Mouse too fast or too slow?** `MOUSE_SENSITIVITY`. Set `MOUSE_INVERT_Y`
  to `True` if you prefer inverted look, and `MAX_PITCH` controls how far up
  and down you can look.
- **Game runs slowly?** Lower `NUM_COLUMNS` from `320` to `160` or `128`. This
  is by far the biggest lever — the picture gets chunkier, nothing else
  changes. There is a live frame-rate readout in the corner of the status bar.
- **Too hard?** Raise `PLAYER_MAX_HP` or `START_AMMO`, or lower the hostiles'
  `ranged_damage` in Section 3.
- **Too dark?** Raise `FOG_FAR`.
- **Want a wider view?** Raise `FOV_DEGREES`.

## Running the checks

```
python selftest.py
```

or just open it in IDLE and press F5. It runs 61 checks covering the level
data, the raycasting maths, mouse aiming and pitch clamping, and a few hundred
frames of the real game loop driven by fake input. Everything should say
`PASS`.

## Known limitations

These are deliberate — fixing them properly is what a real engine does.

- **Walls are flat colours, not textures.** Sampling a texture per column is
  the natural next step, but it is roughly an order of magnitude more work per
  frame and pure Python would struggle.
- **The floor and ceiling are flat bands.** Real floor-casting means one
  calculation per *pixel* rather than per column.
- **Looking up and down is faked** by shearing the picture, as described
  above — the world itself is still perfectly flat, so rounds always travel
  level and there are no stairs, lifts or balconies.
- **No sound.** The standard library has `winsound`, but only on Windows.
- **Mouse capture leans on tkinter's pointer warping,** which is solid on
  Windows and Linux but can be fussy on some setups. If aiming ever feels
  wrong, press `M` to switch it off and turn with the arrow keys instead.
