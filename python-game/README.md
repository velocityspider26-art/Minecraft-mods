# TRIDENT

A modern special-forces first-person shooter written in plain Python, using
nothing but the standard library. You play a Navy SEAL operator clearing a
facility room by room, aiming with the mouse. It runs in IDLE on a school
computer with no installing, no `pip`, and no downloads.

```
python-game/
├── trident.py           the game     (this is the thing you run)
├── selftest.py          optional checks
├── README.md            this write-up
└── trident_save.json    created on first run - settings and progress
```

---

## Running it

1. Open `trident.py` in IDLE — **File → Open…**
2. Press **F5** (or **Run → Run Module**)
3. The **main menu** appears. Pick a mission and the game takes the mouse.
   Press **Esc** at any time to get your cursor back.

`tkinter`, the library that draws the window, ships with Python and is what
IDLE itself is built on — so if you can open IDLE, you can run this.

### Controls

| Input | Action |
| --- | --- |
| **Mouse** | aim — click the window to capture it |
| **Left mouse** or `Space` | fire |
| **Right mouse** | aim down the sight — red dot, zoom, steadier rifle |
| `W` `S` or `↑` `↓` | move forward / backward |
| `A` `D` | strafe (step sideways without turning) |
| `Q` `E` | lean left / right to peek round a corner |
| `←` `→` | turn without the mouse |
| `Shift` | sprint |
| `M` | mouse aiming on / off — or back to the menu after a mission |
| `Tab` | show / hide the tac-map |
| `R` | restart the mission |
| `N` | next mission (once you have cleared one) |
| `Esc` | release the mouse — press again to quit |

### The menu

The game opens on a menu rather than dropping you straight in:

| Entry | |
| --- | --- |
| **CONTINUE** | picks up at the furthest mission you have reached |
| **NEW MISSION** | starts again from the first |
| **DIFFICULTY** | RECRUIT / OPERATOR / VETERAN |
| **DETAIL** | LOW / MEDIUM / HIGH — see *Frame rate* below |
| **FULLSCREEN** | borderless, magnified to fit your screen |
| **QUIT** | |

`W`/`S` or the arrows move, `A`/`D` change a setting, `Enter` picks, and you
can click a row directly. Everything you change is written to
`trident_save.json` next to the game, along with your best score and how far
you have got, so it is all still there next time.

### Difficulty

Each setting is a set of multipliers on the normal game, so one line says
exactly how much easier or harder it is:

| | Your health | Your ammo | Their damage | Their health | Score |
| --- | --- | --- | --- | --- | --- |
| **RECRUIT** | ×1.35 | ×1.35 | ×0.6 | ×0.8 | ×0.75 |
| **OPERATOR** | — | — | — | — | — |
| **VETERAN** | ×0.7 | ×0.75 | ×1.5 | ×1.3 | ×1.5 |

### Frame rate

If the game runs slowly, **it should sort itself out** — it measures its own
frame rate and steps the detail down until it keeps up, then back up if there
is room. You can also set it yourself in the menu.

What the levels change:

| | Stripe width | Wall texture |
| --- | --- | --- |
| **LOW** | 4 rays per stripe | coarse |
| **MEDIUM** | 2 rays per stripe | reduced |
| **HIGH** | 1 ray per stripe | full |

MEDIUM is the default and is very hard to tell from HIGH while you are moving.
LOW still has all the shading, perspective and wall seams — it just draws about
**eighty** wall shapes a frame instead of two thousand.

### The mission

Neutralise every hostile in the compound, then reach the green extraction
beacon. It stays sealed until the last hostile is down — the status bar tells
you how many are left. Grab medkits and rifle magazines on the way.

**Eleven operations**: ten on Earth ending with a compound raid against a
high-value target, and then one somewhere else entirely.

| | Health | Behaviour |
| --- | --- | --- |
| **Tango** | 46 | Rushes you. Dangerous inside 5 metres, poor beyond it. |
| **Marksman** | 62 | Backs off to keep you at rifle range and shoots accurately. |
| **BLACKJACK** | 900 | The final target. Slow, enormous, hits like a truck. |

BLACKJACK sits in a sealed inner chamber whose only way in faces *away* from
where you come ashore, so the compound has to be cleared before you can breach
it. A health bar across the top of the screen tracks it once it has seen you.

Your weapon is an M4-style carbine: fully automatic at about 570 rounds per
minute, 24 damage a round, and accurate enough that misses are your fault.
Magazines hold 30. Hold the trigger down and 90 rounds last nine seconds, so
short bursts are worth learning.

### Operation 11

The last mission is on the moon, and it is not just a repaint. Everything that
makes it feel different comes out of one `env` block on the level itself, which
any mission can use:

- **One sixth of a g.** You drift up to walking speed instead of reaching it
  instantly, and you glide for a moment after you let go. Blood thrown up by a
  hit hangs in the air roughly two and a half times as long before it settles.
- **A sky full of stars.** Stars are infinitely far away, so they have no
  position at all — only a bearing and a height. Turning slides them across the
  sky, looking up and down carries them with the horizon, and walking does not
  move them a pixel, which is exactly right.
- **White habitat panelling and grey regolith** instead of concrete and night.

### A note on the setting

The hostiles are a made-up armed group — masked figures in plain tactical
gear. There is deliberately nothing in the art or the text that ties them to
any real country, group or people, and no real unit, agency or place is named.

---

## How it all works — the simple version

This is the whole game explained without any jargon. The section after it says
the same things again properly, with the real names for everything.

### 1. The world is just typing

There is no 3D world anywhere in this game. The map is a picture made out of
letters, like something you would draw on graph paper:

```
"#......#.........#.....#",
"#..P...#....h....#..t..#",
```

`#` is a wall, `.` is empty floor, `P` is you, `t` is a bad guy. That is
genuinely all the game knows about the level.

### 2. Torch beams

To draw the view, the game chops the screen into **320 thin vertical strips**.
For each strip it fires an invisible torch beam straight out into that letter
map and asks: *how far before you hit a wall?*

### 3. Close things look big

Hold your thumb up close to your eye and it looks huge. Move it away and it
looks tiny. Nothing about your thumb changed — only the distance. The game uses
that exact rule:

```
how tall to draw the wall  =  400  ÷  how far away it is
```

A wall 1 step away is drawn 400 pixels tall and fills the screen. A wall 10
steps away is drawn 40 pixels tall. Draw all 320 strips side by side, tall ones
near and short ones far, and your brain glues them into a corridor. It is a
flip-book trick, not real 3D.

### 4. Why the walls have lines on them

If every strip were one flat colour, the walls would look like wallpaper. So
each strip is chopped into a few chunks, each a slightly different brightness —
like painted stripes across a fence. Because near strips are tall and far ones
are short, the stripes automatically bunch up in the distance, and *that* is
what makes the corridor look like it goes somewhere.

### 5. The bad guys are cardboard cutouts

Every hostile is a flat cardboard cutout that always spins to face you, like a
standee in a cinema foyer. You never see their back, because there isn't one.

### 6. How a cutout knows to hide behind a wall

While firing all those torch beams, the game writes down a list:

> *"At strip 57, the wall is 4 steps away."*

Then before drawing a cutout it checks that list. If the cutout is 6 steps away
and the wall at that strip is only 4, the cutout is behind the wall — so don't
draw it. That list has a proper name: a **depth buffer**.

### 7. The mouse treadmill

Here is the problem with mouse aiming: your mouse would slide to the edge of
the window and stop, and so would your turning.

So the game cheats. Every time you move the mouse it:

1. measures how far the pointer got from the middle of the window,
2. turns you by that much,
3. **secretly teleports the pointer back to the middle.**

It is a treadmill. You keep moving, you never reach the end. The pointer is
also made invisible so you don't see it snapping about.

*(One funny catch: teleporting the mouse makes the computer think you moved
the mouse. If the game believed that, it would think you yanked the mouse back
the other way and you would never turn at all. So the game leaves itself a
sticky note saying "that next one was me" and ignores it.)*

### 8. Looking up and down is a fib

The game genuinely has no idea what "up" means — the world is flat letters.
So when you look up, it does not tilt anything. It just **slides the whole
picture down the screen**, like sliding a photo behind a window frame. Your
brain reads that as looking up. Doom did the same thing in 1993.

This is also why you can't look *straight* up: slide too far and the picture
starts to look wrong.

### 9. Right-click to zoom

Looking down the sight is like squinting through a cardboard tube. You see
**less** of the room, but what you do see is **bigger**. The game does this by
making its pretend camera lens narrower. Everything else — slower mouse,
slower walk, steadier rifle — follows from that one change.

### 10. What you see when you aim

Looking down a sight is not "the gun, but squashed". It is a **tube you look
through**. So the game draws a big dark ring with clear space in the middle, a
red dot floating in that space, and the rest of the rifle poking up from below
the ring. The barrel folds away out of sight, because when you look down a
rifle the barrel is pointing away from you — you would not see it side-on.

### 11. Leaning is just moving your eyeballs

Press `Q` or `E` and your **feet stay exactly where they are** while your
**head slides sideways**. That is the entire trick, and it is why you can peek
past a corner without stepping into the open. If your head would end up inside
a wall, the game shortens the lean until it fits.

### 12. Blood

Each speck of blood is a tiny dot that remembers three things: where it is, how
high off the floor it is, and how fast it is travelling. Thirty times a second
each dot moves a little and falls a little. When it hits the floor, it is
deleted. Bodies leave a dark pool that spreads out over about a second.

### 13. The gun is a staircase

The gun has to sit at an angle, but the only shape this game can draw is a
rectangle that is perfectly straight up and down. You cannot tilt a rectangle.

So the rifle is drawn the way you would draw a diagonal line on graph paper:
lots of little blocks, each one nudged slightly to the side of the one below.
Close up you would see the steps. At this size your eye just sees a slope.

The gun also has **weight**. Swing the view fast and it trails behind for a
moment before catching up, and it drifts very gently even when you stand still,
as if you were breathing.

### 14. Why it doesn't run like treacle

Python is slow, and drawing is slow, so the game follows two rules:

- **Make every shape once, then just move it.** All 2,000-odd rectangles are
  created when the game starts and are never thrown away. Moving a rectangle
  is cheap; making a new one is not.
- **Mix every colour before you start.** The drawing library wants colours
  written out as text like `"#8a4a34"`. Writing those out mid-game, hundreds of
  times a frame, would be slow — so every shade of every colour is worked out
  once at the beginning and looked up from a list afterwards.

### 15. The moon

The last mission reads a small block of settings off the level and everything
else follows from it. Gravity is one number: turn it down and blood floats for
longer. How fast you get moving is another: turn *that* down and you slide
around like you are on ice, which is more or less what low gravity feels like.

The stars are the neat bit. A star is so far away that walking towards it does
nothing at all — so the game does not give them a position, only a *direction*.
That is all it needs to work out where on screen to put them.

### 16. Fullscreen

The game always draws the same picture, 640 across and 400 down. Fullscreen
just multiplies every number on the way to the screen — by 2, or 3 if your
monitor is big enough. It is nearly free, because it draws exactly the same
number of shapes; they are simply bigger. Whole numbers only, so every pixel
becomes a neat square block instead of going blurry.

### 17. It looks after itself

Nobody knows how fast the computer running this will be, so the game times its
own frames. If they start taking too long it quietly draws the walls with less
detail; when there is room again it puts the detail back. You lose some of the
lines on the walls and nothing else.

It is also hard to kill. If something goes wrong on one frame it prints the
problem and keeps going, rather than freezing. And if you click away to another
window it lets go of whatever keys you were holding, so you do not come back to
find yourself running into a wall.

### 18. The heartbeat

About 30 times a second the game does the same three things: **read the
keyboard and mouse → move everything a tiny bit → redraw the screen.** Then it
asks to be woken up again in 33 milliseconds.

It politely asks to be woken rather than sitting in a `while True:` loop,
because the window needs a moment to itself to notice your keypresses and
actually paint. A busy loop would freeze the whole thing solid.

---

## How the 3D works — the proper version

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

### 5. Texturing walls without a texture

Flat-coloured walls are what make a raycaster look cheap. The proper fix is to
sample a bitmap per screen pixel — and that is exactly what pure Python cannot
afford. At 320 columns and 400 pixels tall that is over a hundred thousand
lookups a frame, which is an order of magnitude past the budget.

So the walls are textured a cheaper way, in two halves:

**Across the wall, detail is free.** The raycaster already knows *where on the
wall face* each ray landed — that is the `wall_x` it returns, running 0 to 1
across each square. Look that up in a small table of brightness offsets and
nudge the shade, and you get vertical seams, mortar joints and panel edges for
the price of one list index. No extra drawing at all.

**Down the wall, detail costs rectangles.** Each column is split into a handful
of horizontal bands with their own brightness — courses of block, the lip of a
steel panel, a dark skirt at the bottom. The bands are measured from 0 at the
top of the wall to 1 at the bottom, so they scale with the stripe and stay
glued to the wall however far away it is.

That is the whole trick, and the perspective does the rest: because every
column computes its own bands, a long wall's courses converge towards the
vanishing point on their own.

Two things keep it affordable. Walls shorter than 30 pixels on screen collapse
to a single flat stripe, since bands a pixel tall are just noise — and most
columns in view at any moment are distant ones. And the existing "has this
changed colour?" cache still applies per band.

### 6. Drawing a weapon that cannot be rotated

A rifle held in first person runs diagonally — butt down by your shoulder,
muzzle up and away towards the centre of the screen. A tkinter canvas
rectangle cannot be rotated, so the weapon is built as a **staircase**: each
length of it is a short stack of blocks that each step sideways, laid along a
straight line from the muzzle position to the butt position. At this size the
steps read as a slope rather than as stairs.

Everything hangs off that one line. `gun_axis(y)` says where the centre of the
weapon is at any height, and `gun_run()` builds one length of it — with two
extra arguments that do most of the work: `sideways` shifts a run off the
centre-line, which is how the highlight down one edge and the shadow down the
other are drawn, and `drift` leans a run away from the axis as it descends,
which is how the magazine and the pistol grip hang off at their own angles.

Giving every material a light and a dark version and painting a thin highlight
along one edge is what stops a heap of rectangles reading as a heap of
rectangles. It is the cheapest possible way to fake a rounded, solid object.

The weapon also has inertia. However far the view swung this frame — from the
mouse and the arrow keys alike — is banked up and pushes the weapon the
opposite way, then it springs back to centre. A slow sine drift stands in for
breathing. One consequence needed guarding: looking a long way up slides the
weapon down with the horizon, and sway can shove it back up, and between them
they could park the barrel right over the reticle. So the hip-fire pose is
clamped to always leave clear air above the muzzle, and `selftest.py` sweeps
every combination of pitch, sway and breathing to prove it.

### 7. Aiming down the sight

Holding the right mouse button raises the sight, and one number drives all of
it. `self.ads` slides between 0 and 1 rather than snapping, and everything
reads off it: the field of view narrows (which *is* the zoom — the camera
plane's length is the FOV, so shortening it magnifies), the mouse slows down,
your walk slows down, the rifle's scatter tightens, and the weapon sway damps
right down.

The first attempt at this just squeezed the side-on weapon sprite and slid its
little optic block onto the aim point. It looked wrong, and the reason is worth
understanding: **looking down a sight is not a squashed side view of a rifle.**
It is a view *through a tube*. So the sight is now drawn as what you would
actually see:

- **The tube** is a hollow oval — tkinter will draw an oval outline of any
  thickness you like, which is exactly the shape needed — with a thin lighter
  ring just inside it for the edge of the glass. It starts oversized and
  settles to its proper size, which reads as the optic coming up to your eye.
- **Everything in front of the optic** — handguard, gas block, front sight,
  muzzle — folds away towards the optic as the sight comes up. Behind a red dot
  you are looking straight *down* the weapon, so the barrel is pointing away
  from you and must recede rather than sit there side-on.
- **The weapon body drops below the tube.** Once the tube exists it *is* the
  sight, so the sprite's own optic is redundant, and leaving the receiver
  sitting in the middle of the glass looks like you are aiming through your own
  rifle. Only the top of the body shows under the housing, which is what you
  really see.
- **The red dot** floats in the clear middle with a dim halo behind it, and the
  crosshair disappears — from then on the dot is what you aim with.

The muzzle flash is scaled down while sighted in, or it would blot out the
whole sight picture every time you pulled the trigger.

### 8. Leaning

`Q` and `E` tip you out to the side. The important part is that leaning moves
**where you look from without moving where you stand** — the whole point is to
peek past a corner without stepping into the open.

That means the eye and the body are no longer the same point. The body stays at
`px, py`; the eye is `cam_x, cam_y`, offset sideways along the camera plane,
and it is the eye that the rays and the sprite projection use.

Leaning must not put your head through a wall. Rather than refusing the lean
outright, the code tries the full distance, and if that spot is solid it keeps
shortening the reach until it finds one that is not. So leaning into a wall
just quietly stops part-way instead of clipping through.

### 9. Feedback: hit markers and warnings

Two small things that cost almost nothing and make the game feel finished.

A round that actually connects flicks a **hit marker** onto the reticle for
about an eighth of a second — four short diagonals that spring outwards. They
are `create_line` items rather than rectangles, because a line can be drawn at
any angle and a rectangle cannot.

The rounds counter turns **red** below ten, so you notice you are nearly dry
before the rifle goes quiet on you.

### 10. Blood

Specks live in the **world**, not on the screen: each has a map position, a
height above the floor, and a velocity, and falls under gravity until it lands
or hits a wall. Screen-space specks would have been cheaper, but they slide
about when you turn. World-space ones stay where they were thrown, and they
project through exactly the same maths as a sprite — including the depth
buffer check, so blood behind a wall stays hidden.

Bodies stay where they fall and a pool spreads underneath them over about a
second. The particle pool is capped, so a long firefight cannot slowly fill
memory with old specks.

### 11. Hostiles: billboards and a depth buffer

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

### 12. Staying up on a computer you have never seen

The game has to survive a machine nobody tested it on, so a few things are
defensive on purpose.

**One bad frame must not end the game.** The frame body is wrapped, and if
anything throws, the error is printed once and the loop carries on. A window
that has silently stopped redrawing looks exactly like a crash; a game with one
glitchy frame does not.

**Closing the window has to stop the loop.** `WM_DELETE_WINDOW` is bound, so
the X button and `Esc` both go through the same clean shutdown. Without it the
next scheduled frame wakes up and tries to draw into a window that is gone.

**Losing focus drops every held key.** Keys held when you alt-tab never send a
release, so without this you come back to find yourself still sprinting into a
wall.

**Pointer warping might not be allowed.** It is the one thing here that depends
on the operating system agreeing to something, so it is wrapped: if it fails,
mouse aiming turns itself off and says so rather than leaving you with a view
that will not move.

**The frame loop always leaves tkinter some time.** If a frame overruns and we
ask to be woken immediately, frames queue back-to-back and the window stops
answering the keyboard — which, again, looks like a freeze. There is a minimum
gap between frames for that reason.

### 13. Automatic quality, and the bug that stopped it working

The first version of this timed how long our own Python took each frame,
decided everything was comfortable, and never stepped the quality down — on a
machine that was managing ten frames a second.

The mistake is worth knowing about, because it is easy to make. **Issuing
drawing commands and actually drawing are different things.** All those calls
do is tell the canvas where its shapes now are; the window system does the real
painting afterwards, on its own time. So our own stopwatch said "3 ms, plenty
of room" while the frame really took a hundred. It now judges on the measured
gap between frames, which includes everything.

The other half of the fix was giving it something worth turning down. Wall
texture detail alone was not enough, because the real cost is not the
arithmetic — it is **how many separate shapes the drawing library has to track
and repaint**, and there were over three thousand of them. So the quality
ladder now also widens the stripes: one stripe per two rays, or per four, which
cuts the shape count straight down with it. Rays are all still cast either way,
since they are cheap and the sprites need the full depth buffer to clip
against.

Measured at the same viewpoint: **2,240 wall shapes a frame at the old
settings, 209 at MEDIUM, 80 at LOW.** The minimap went from 511 shapes to a few
dozen at the same time, by drawing each run of identical squares as one bar
instead of a square per cell.

### 14. How it decides

There is no way to know in advance how fast the computer running this will be,
so rather than guessing, the game watches how long its frames actually take.

If they start overrunning the budget it steps the walls down to a coarser
version of their texture; when there is room again it steps them back up. Wall
banding is by far the cheapest thing to give up — you lose the horizontal
courses but keep the shading, the seams and all of the perspective, so it still
reads as a corridor.

The two thresholds are deliberately far apart and a change has to be earned
over twenty frames, or the quality would visibly flicker every time one frame
ran slightly long.

### 15. Fullscreen without paying for it

The first attempt at fullscreen just centred the 640x400 view on a black
screen, which is not fullscreen — it is a small game with a big border.

Making it genuinely bigger sounds expensive, but it is not, because **the cost
here is the number of shapes, not the number of pixels.** Scaling the picture
up draws exactly as many rectangles as before; they are simply larger. So the
game still thinks entirely in its fixed 640x400 view — all the maths, the
collision, the sprite work — and every coordinate is multiplied by a whole
number on its way out to the canvas, in `_place`. Nothing upstream knows.

Whole numbers only, deliberately: at 2x every pixel becomes an exact 2x2 block
and the art stays crisp, where a fractional stretch would go soft. Fullscreen
picks the largest multiple that fits your screen.

Two things needed care. Fonts and line widths do not scale with coordinates,
so their originals are recorded at start-up and multiplied separately. And the
mouse reports screen pixels while the game thinks in view units, so pointer
movement is divided back down — otherwise aiming would get twice as twitchy in
fullscreen. There is a test for exactly that.

### 16. Making it fast enough in Python

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

Drawing the walls turned out to be **86%** of the whole frame, so that is where
the work went. Profiling showed it making 3,234 separate calls into the drawing
library per frame, at about 1.6 microseconds each. Three changes between them
roughly halved the frame:

- **Talk to Tcl directly.** `canvas.coords(...)` is convenient but adds a layer
  of Python argument handling to every call. Going straight to `tk.call` is
  about 30% cheaper per call, which matters a lot when there are thousands.
- **Use whole numbers.** Integers marshal faster than floats, *and* a wall that
  has shifted a fraction of a pixel now rounds to the same place it was — which
  feeds the next trick.
- **Do not touch what has not moved.** Every band remembers where it was put
  last frame. If it has not changed, the call is skipped entirely. Standing
  still costs almost nothing.

Plus the detail tiers from section 13, which cut the number of rectangles a
typical frame draws in the first place.

Measured by `selftest.py` in this environment: **12.6 ms → 6.9 ms of work per
frame** at 320 rays, against a 33 ms budget for 30 fps. That figure is the
worst case — the benchmark spins the camera every frame, which defeats the
change-detection completely. Walking normally it is well under half that.

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
├── update_blood()     move the specks, drop the ones that have landed
├── cast_rays()        ← the raycaster, fills the depth buffer
├── draw_walls()       320 stripes
├── draw_sprites()     billboards, sorted far-to-near
├── draw_blood()       specks in flight, and pools under the bodies
├── draw_weapon()      the carbine: bob, recoil, lean, sight and muzzle flash
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
| `W` | white habitat panel | | `b` | BLACKJACK (the boss) |
| `V` | viewport glass | | | |

Two rules: every row must be the same length, and there must be a solid wall
all the way around the outside — otherwise a ray can shoot off the edge of the
world. Run `selftest.py` afterwards and it will check both of those for you,
plus confirm that nothing has ended up walled off where you cannot reach it.

## Tuning it

Everything worth changing is at the top of the file in **Section 1**.

- **Mouse too fast or too slow?** `MOUSE_SENSITIVITY` (halved recently — raise
  it back towards `0.003` if you want it quicker). Set `MOUSE_INVERT_Y`
  to `True` if you prefer inverted look. `MAX_PITCH_UP` and `MAX_PITCH_DOWN`
  control how far you can look up and down.
- **Sight not to your taste?** `ADS_FOV_DEGREES` sets the zoom,
  `ADS_SENSITIVITY` how much the mouse slows while zoomed.
- **Lean too far or too little?** `LEAN_DISTANCE`.
- **Weapon swings too much?** `SWAY_TURN`, or `SWAY_MAX` for the limit.
  `SWAY_BREATH` is the idle drift. Zero them all to lock the weapon still.
- **Hit markers or the low-ammo warning in the way?** `HITMARK_TIME` and
  `LOW_AMMO`. `SIGHT_RADIUS` and `SIGHT_RING_WIDTH` size the optic tube.
- **Too much blood?** `BLOOD_ON_HIT`, `BLOOD_ON_DEATH`, or `MAX_BLOOD` for
  the overall cap. Set them all to 0 to turn it off.
- **Game runs slowly?** It should handle this itself — the walls step down to
  a coarser texture automatically when frames overrun, and step back up when
  they do not. If you want to force the issue, lower `NUM_COLUMNS` from `320`
  to `160` or `128`; that is the biggest lever there is. There is a live
  frame-rate readout in the corner of the status bar. `DETAIL_DROP_AT`,
  `DETAIL_RAISE_AT` and `DETAIL_PATIENCE` tune how eagerly it adapts.
- **Too hard?** Raise `PLAYER_MAX_HP` or `START_AMMO`, or lower the hostiles'
  `ranged_damage` in Section 3.
- **Too dark?** Raise `FOG_FAR`.
- **Want a wider view?** Raise `FOV_DEGREES`.

## Running the checks

```
python selftest.py
```

or just open it in IDLE and press F5. It runs 302 checks covering the level
data, the raycasting maths, the wall texture tables, mouse aiming, pitch
clamping, leaning being cut short at a wall, the sight picture, hit markers, blood
physics, the reticle staying clear of the weapon at every pitch and sway, the game
surviving a broken frame, focus loss letting go of held keys, automatic quality
stepping down and back up, the difficulty multipliers, the save file surviving
being empty or edited into nonsense, every menu row, fullscreen scaling keeping the mouse
feeling the same, the boss and its health bar, the last mission carrying
enough ammunition to actually finish it, and the moon's gravity and starfield
actually behaving differently from Earth's, and a few hundred
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
  level and there are no stairs, lifts or balconies. It is also why the
  vertical range is limited: a modern shooter lets you look almost straight
  up, but a shear that steep distorts badly, and it slides the weapon clean
  off the bottom of the screen. Real free-look needs a different renderer.
- **Walls are textured by banding, not by sampling a bitmap** (section 5).
  Close up you can see the bands are flat blocks of colour rather than real
  pixels.
- **No sound.** The standard library has `winsound`, but only on Windows.
- **Mouse capture leans on tkinter's pointer warping,** which is solid on
  Windows and Linux but can be fussy on some setups. If aiming ever feels
  wrong, press `M` to switch it off and turn with the arrow keys instead.
