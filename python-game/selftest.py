"""
Self-test for trident.py.

You do not need this to play - it is here so you can prove the maths
and the level data are correct without having to squint at the screen.

Run it in IDLE with F5, or from a terminal:

    python selftest.py

It checks three things:

    1. LEVEL DATA  - every map is a proper rectangle, sealed by walls, has
                     exactly one player start, and nothing is walled off
                     somewhere you could never reach.
    2. RAYCASTING  - the rays measure the distances we expect, and flat walls
                     come out flat instead of curved (the "fisheye" bug).
    3. THE GAME    - the real game loop runs for a few hundred frames with
                     fake keyboard input and nothing crashes.

Test 3 needs a screen, so if you are running somewhere without one it is
skipped rather than failed.
"""

import json
import math
import os
import sys
import tempfile
import time
from collections import deque

import trident

# Tests must not touch a real save file, so point the game at a scratch one.
trident.SAVE_FILE = os.path.join(tempfile.gettempdir(), "trident_selftest.json")
try:
    os.remove(trident.SAVE_FILE)
except OSError:
    pass


PASSED = 0
FAILED = 0


def check(name, condition, detail=""):
    global PASSED, FAILED
    if condition:
        PASSED += 1
        print("  PASS  %s" % name)
    else:
        FAILED += 1
        print("  FAIL  %s %s" % (name, detail))


# ---------------------------------------------------------------------------
#  1. Level data
# ---------------------------------------------------------------------------

def reachable_cells(grid):
    """Flood fill outwards from the player start, returning every open cell."""
    height = len(grid)
    width = len(grid[0])

    start = None
    for row in range(height):
        for col in range(width):
            if grid[row][col] == "P":
                start = (col, row)
    if start is None:
        return set()

    seen = {start}
    queue = deque([start])
    while queue:
        col, row = queue.popleft()
        for next_col, next_row in ((col + 1, row), (col - 1, row),
                                   (col, row + 1), (col, row - 1)):
            if not (0 <= next_col < width and 0 <= next_row < height):
                continue
            if (next_col, next_row) in seen:
                continue
            if grid[next_row][next_col] in trident.WALL_COLOURS:
                continue
            seen.add((next_col, next_row))
            queue.append((next_col, next_row))
    return seen


def test_levels():
    print("\n1. LEVEL DATA")
    known = set(trident.WALL_COLOURS) | set(trident.MONSTERS) | set(trident.PICKUPS)
    known |= {".", "P", "X"}

    for number, level in enumerate(trident.LEVELS, start=1):
        grid = level["grid"]
        label = "op %d (%s)" % (number, level["name"])
        print("  --- %s ---" % label)

        widths = {len(row) for row in grid}
        check("%s: every row is the same width" % label, len(widths) == 1,
              "got widths %s" % sorted(widths))
        if len(widths) != 1:
            continue

        width = widths.pop()
        height = len(grid)

        bad = sorted({c for row in grid for c in row} - known)
        check("%s: no unknown characters" % label, not bad, "found %s" % bad)

        walled = (all(c in trident.WALL_COLOURS for c in grid[0])
                  and all(c in trident.WALL_COLOURS for c in grid[-1])
                  and all(row[0] in trident.WALL_COLOURS
                          and row[-1] in trident.WALL_COLOURS for row in grid))
        check("%s: sealed by an outer wall" % label, walled)

        starts = sum(row.count("P") for row in grid)
        check("%s: exactly one player start" % label, starts == 1,
              "found %d" % starts)

        exits = sum(row.count("X") for row in grid)
        check("%s: has at least one exit" % label, exits >= 1)

        # Everything interesting must be somewhere the player can walk to.
        open_cells = reachable_cells(grid)
        stranded = []
        for row in range(height):
            for col in range(width):
                char = grid[row][col]
                if char in known - set(trident.WALL_COLOURS) - {"."}:
                    if (col, row) not in open_cells:
                        stranded.append((char, col, row))
        check("%s: every hostile, pickup and exit is reachable" % label,
              not stranded, "stranded: %s" % stranded)

        monsters = sum(sum(row.count(k) for row in grid) for k in trident.MONSTERS)
        pickups = sum(sum(row.count(k) for row in grid) for k in trident.PICKUPS)
        check("%s: hostiles fit in the sprite pool (%d <= %d)"
              % (label, monsters, trident.MAX_SPRITE_SLOTS),
              monsters <= trident.MAX_SPRITE_SLOTS)
        check("%s: pickups fit in the sprite pool (%d <= %d)"
              % (label, pickups, trident.MAX_SPRITE_SLOTS),
              pickups <= trident.MAX_SPRITE_SLOTS)


# ---------------------------------------------------------------------------
#  2. Raycasting maths
# ---------------------------------------------------------------------------

def test_raycasting(game):
    print("\n2. RAYCASTING")
    middle = trident.NUM_COLUMNS // 2

    # -- looking straight down the left corridor of map 1 --
    # The player stands at (3.5, 2.5). Row 19 is the bottom wall, so the wall
    # dead ahead should measure 19 - 2.5 = 16.5 squares away.
    game.load_level(0)
    game.px, game.py = 3.5, 2.5
    game._update_camera_position()
    game.angle = math.radians(90)
    game._update_camera_vectors()
    rays = game.cast_rays()
    distance, _side, char, _wall_x = rays[middle]
    check("centre ray measures 16.5 squares", abs(distance - 16.5) < 0.001,
          "got %.4f" % distance)
    check("centre ray hits a stone wall", char == "#", "got %r" % char)

    # -- looking east from the same spot --
    # Column 7 of row 2 is a wall, so from x = 3.5 that is 3.5 squares away.
    game.angle = 0.0
    game._update_camera_vectors()
    rays = game.cast_rays()
    distance, _side, _char, _wall_x = rays[middle]
    check("looking east measures 3.5 squares", abs(distance - 3.5) < 0.001,
          "got %.4f" % distance)

    # -- the fisheye test --
    # Stand 1.5 squares from a long flat wall and look straight at it. Every
    # single column must report the same distance; if the perspective maths is
    # wrong the edges of the screen report further away and the wall bulges.
    game.px, game.py = 3.5, 17.5
    game._update_camera_position()
    game.angle = math.radians(90)
    game._update_camera_vectors()
    rays = game.cast_rays()
    distances = [r[0] for r in rays]
    spread = max(distances) - min(distances)
    check("a flat wall reads flat across all %d columns" % trident.NUM_COLUMNS,
          spread < 0.001, "spread was %.5f" % spread)
    check("that flat wall is 1.5 squares away",
          abs(distances[middle] - 1.5) < 0.001, "got %.4f" % distances[middle])

    # -- the texture coordinate across the wall face --
    # Walk along a flat wall and the hit position should sweep smoothly from
    # 0 to 1 and wrap, once per wall square. If this is wrong, seams crawl
    # about instead of staying painted on the wall.
    game.px, game.py = 3.5, 17.5
    game._update_camera_position()
    game.angle = math.radians(90)
    game._update_camera_vectors()
    rays = game.cast_rays()
    check("every ray reports a wall position in 0..1",
          all(0.0 <= r[3] < 1.0 for r in rays),
          "out of range: %s" % [r[3] for r in rays if not 0.0 <= r[3] < 1.0][:3])

    # Standing at x = 3.5 and looking at the wall dead ahead, the centre ray
    # lands halfway across square 3, so the coordinate should be 0.5.
    check("the centre ray lands mid-square", abs(rays[middle][3] - 0.5) < 0.01,
          "got %.4f" % rays[middle][3])

    # Sliding sideways by a quarter of a square should slide the hit by the
    # same amount, because the wall is flat and square-on.
    game.px = 3.75
    game._update_camera_position()
    rays = game.cast_rays()
    check("sliding sideways slides the wall position with it",
          abs(rays[middle][3] - 0.75) < 0.01, "got %.4f" % rays[middle][3])
    game.px = 3.5
    game._update_camera_position()

    # -- the depth buffer is filled in --
    check("depth buffer has one entry per column",
          len(game.zbuffer) == trident.NUM_COLUMNS)
    check("depth buffer matches the rays",
          all(abs(a - b[0]) < 1e-9 for a, b in zip(game.zbuffer, rays)))

    # -- walls really do block you --
    check("cannot stand inside a wall",
          not game.can_stand(0.5, 0.5, trident.PLAYER_RADIUS))
    check("can stand in an open corridor",
          game.can_stand(3.5, 5.5, trident.PLAYER_RADIUS))
    check("line of sight is blocked by a wall",
          not game.has_line_of_sight(3.5, 2.5, 12.5, 2.5))
    check("line of sight is clear down an open corridor",
          game.has_line_of_sight(3.5, 2.5, 3.5, 8.5))

    # -- projecting a point in front of you lands near the middle --
    screen_x, depth = game.project(3.5, 19.0)
    check("a point straight ahead projects to the screen centre",
          abs(screen_x - trident.SCREEN_W / 2) < 1.0, "got x=%.2f" % screen_x)
    check("a point straight ahead has positive depth", depth > 0)
    _screen_x, depth = game.project(3.5, 10.0)
    check("a point behind you is reported as behind", depth <= 0)


# ---------------------------------------------------------------------------
#  3. Running the real game loop
# ---------------------------------------------------------------------------

def test_game_loop(root, game):
    print("\n3. GAME LOOP")

    game.load_level(0)
    game.state = trident.STATE_PLAYING
    game._hide_overlay()

    frames = 0
    errors = []
    start = time.perf_counter()

    # Fake somebody playing: walk forward, turn, and shoot.
    script = [
        ({"w"}, 0.8),
        ({"w", "right"}, 0.6),
        ({"space"}, 0.6),
        ({"a"}, 0.4),
        ({"s", "left"}, 0.4),
        ({"w", "shift_l", "space"}, 0.8),
    ]

    original_tick = game.tick

    def counting_tick():
        nonlocal frames
        try:
            original_tick()
            frames += 1
        except Exception as exc:            # noqa: BLE001 - we want them all
            errors.append(exc)

    game.tick = counting_tick

    for keys, seconds in script:
        game.keys = set(keys)
        until = time.perf_counter() + seconds
        while time.perf_counter() < until:
            root.update()
    game.keys = set()

    elapsed = time.perf_counter() - start

    check("no exceptions during %d frames" % frames, not errors,
          "first error: %r" % (errors[0] if errors else None))
    check("the loop actually ran", frames > 30, "only %d frames" % frames)
    check("operator stayed inside the map",
          0 < game.px < game.map_w and 0 < game.py < game.map_h,
          "player at (%.2f, %.2f)" % (game.px, game.py))
    check("operator never ended up inside a wall",
          not game.is_wall(game.px, game.py))
    check("firing used up ammo", game.ammo < trident.START_AMMO,
          "ammo is still %d" % game.ammo)

    print("\n  ran %d frames in %.1fs  (%.1f frames/sec)"
          % (frames, elapsed, frames / elapsed))

    # How much work one frame actually costs, with the frame-rate cap taken
    # out of the picture. If this is comfortably under 33ms the game will hold
    # a steady 30 fps on this machine.
    # The camera is turned a little every round on purpose. Standing still
    # would let the "has this column changed colour?" cache skip nearly all
    # the work and give a flattering answer.
    game.load_level(0)
    rounds = 60
    begin = time.perf_counter()
    for step in range(rounds):
        game.angle = step * 0.05
        game._update_camera_vectors()
        rays = game.cast_rays()
        game.draw_background()
        game.draw_walls(rays)
        game.draw_sprites()
        game.draw_weapon()
        game.draw_hud()
        game.draw_minimap()
        root.update()               # make tkinter actually paint it
    per_frame = (time.perf_counter() - begin) / rounds
    print("  work per frame: %.1f ms with %d rays  -> good for about %d fps"
          % (per_frame * 1000, trident.NUM_COLUMNS, int(1 / per_frame)))
    check("a frame is drawn well inside the 33ms budget", per_frame < 0.033,
          "took %.1f ms - try lowering NUM_COLUMNS in trident.py" % (per_frame * 1000))

    # -- combat actually works --
    game.load_level(0)
    victim = game.monsters[0]
    before = game.kills
    game.damage_monster(victim, 999)
    check("a hostile dies when it runs out of health", not victim.alive)
    check("killing a hostile counts", game.kills == before + 1)

    # -- the exit stays shut until the level is clear --
    game.load_level(0)
    exit_col, exit_row = game.exits[0]
    game.px, game.py = exit_col + 0.5, exit_row + 0.5
    game._update_camera_position()
    game.check_exit()
    check("extraction is sealed while hostiles are alive",
          game.state != trident.STATE_CLEARED)
    for monster in game.monsters:
        game.damage_monster(monster, 9999)
    game.check_exit()
    check("extraction opens once every hostile is down",
          game.state in (trident.STATE_CLEARED, trident.STATE_WON))

    # -- points are banked exactly once, not counted twice on the status bar --
    game.load_level(0)
    game.total_score = 0
    for monster in game.monsters:
        game.damage_monster(monster, 9999)
    expected = game.level_score
    exit_col, exit_row = game.exits[0]
    game.px, game.py = exit_col + 0.5, exit_row + 0.5
    game._update_camera_position()
    game.check_exit()
    shown = game.total_score + game.level_score
    check("clearing a level banks the score exactly once", shown == expected,
          "earned %d but the status bar would show %d" % (expected, shown))

    # -- damage and death --
    game.load_level(0)
    game.state = trident.STATE_PLAYING
    game.hurt_player(30)
    check("taking damage lowers health", game.hp == trident.PLAYER_MAX_HP - 30)
    game.hurt_player(9999)
    check("health never goes below zero", game.hp == 0)
    check("running out of health ends the game", game.state == trident.STATE_DEAD)

    # -- pickups --
    game.load_level(0)
    game.hp = 10
    medkit = next(p for p in game.pickups if p.kind == "h")
    game.px, game.py = medkit.x, medkit.y
    game._update_camera_position()
    game.check_pickups()
    check("walking over a medkit heals you", game.hp == 35, "hp is %d" % game.hp)
    check("the medkit is gone afterwards", medkit.taken)

    # -- mouse aiming --
    game.load_level(0)
    game.state = trident.STATE_PLAYING
    game.mouse_captured = True

    class FakeMove:
        def __init__(self, x, y):
            self.x, self.y = x, y

    centre_x, centre_y = trident.SCREEN_W // 2, trident.SCREEN_H // 2
    before = game.angle
    game._warping = False
    game._on_mouse_move(FakeMove(centre_x + 100, centre_y))
    turned = (game.angle - before + math.pi) % (2 * math.pi) - math.pi
    check("moving the mouse right turns you right", turned > 0,
          "turned %.4f rad" % turned)
    check("the turn matches the sensitivity setting",
          abs(turned - 100 * trident.MOUSE_SENSITIVITY) < 1e-6,
          "expected %.4f, got %.4f" % (100 * trident.MOUSE_SENSITIVITY, turned))

    before = game.angle
    game._warping = True                # pretend this is the warp echo
    game._on_mouse_move(FakeMove(centre_x + 100, centre_y))
    check("the pointer-warp echo is ignored", game.angle == before)

    game.pitch = 0.0
    game._warping = False
    game._on_mouse_move(FakeMove(centre_x, centre_y - 60))
    check("moving the mouse up looks up", game.pitch > 0,
          "pitch %.1f" % game.pitch)
    for _ in range(40):
        game._warping = False
        game._on_mouse_move(FakeMove(centre_x, centre_y - 200))
    check("looking up is clamped", game.pitch <= trident.MAX_PITCH_UP,
          "pitch ran to %.1f" % game.pitch)
    for _ in range(80):
        game._warping = False
        game._on_mouse_move(FakeMove(centre_x, centre_y + 200))
    check("looking down is clamped", game.pitch >= -trident.MAX_PITCH_DOWN,
          "pitch ran to %.1f" % game.pitch)

    # At full upward pitch the carbine slides down with the horizon. The
    # reticle must still sit clear above it, or you cannot see what you shoot.
    game.pitch = trident.MAX_PITCH_UP
    game.bob_offset = 0.0
    game.recoil = 0.0
    game.ads = 0.0
    game.sway_x = game.sway_y = 0.0
    game.tick()
    gun_top = min(game.canvas.coords(i)[1] for i in game.gun_items)
    check("the reticle stays clear of the weapon at full upward pitch",
          game.horizon < gun_top,
          "reticle %.0f vs weapon top %.0f" % (game.horizon, gun_top))

    # ...and it must still be clear with the sway shoved as far the wrong way
    # as it can possibly go, which is what would otherwise park the barrel
    # right on top of the crosshair.
    worst = float("-inf")
    for pitch in (0.0, trident.MAX_PITCH_UP * 0.5, trident.MAX_PITCH_UP):
        for sway in (-trident.SWAY_MAX, 0.0, trident.SWAY_MAX):
            for breath in range(0, 7):
                game.pitch = pitch
                game.breath = breath * 0.5
                game.sway_y = sway
                game.tick()
                top = min(game.canvas.coords(i)[1] for i in game.gun_items)
                worst = max(worst, game.horizon - top)
    check("the reticle stays clear at every pitch and sway combination",
          worst < 0, "closest approach was %.1f px (negative means clear)" % worst)
    game.pitch = 0.0
    game.sway_y = 0.0

    # The whole view has to move together, or the horizon tears away from the
    # floor and you get a bare strip along the bottom of the screen.
    game.pitch = trident.MAX_PITCH_UP
    game.bob_offset = 0.0
    game.tick()
    check("the horizon follows the pitch",
          abs(game.horizon - (trident.SCREEN_H / 2 + trident.MAX_PITCH_UP)) < 1.0,
          "horizon at %.1f" % game.horizon)
    lowest = max(y1 for _item, _y0, y1 in game.bg_items)
    check("the floor still reaches the bottom at full pitch",
          lowest + game.pitch >= trident.SCREEN_H,
          "floor ends at %.1f" % (lowest + game.pitch))

    # -- leaning --
    game.load_level(0)
    game.state = trident.STATE_PLAYING
    game.px, game.py = 3.5, 5.5      # open corridor, walls to the left/right
    game.angle = math.radians(90)    # facing down the corridor
    game.lean = 0.0
    game._update_camera_vectors()
    game._update_camera_position()
    check("no lean means the eye sits on the body",
          game.cam_x == game.px and game.cam_y == game.py)

    game.lean = 1.0
    game._update_camera_position()
    shifted = math.hypot(game.cam_x - game.px, game.cam_y - game.py)
    check("leaning moves the eye off the body", shifted > 0.1,
          "moved %.3f" % shifted)
    check("leaning does not move the body",
          (game.px, game.py) == (3.5, 5.5))
    check("the eye never ends up inside a wall",
          not game.is_wall(game.cam_x, game.cam_y))

    # Leaning right should move the eye to the player's right. Facing south
    # (+y), right is -x, so the eye should slide towards a smaller x.
    check("leaning right peeks to the right", game.cam_x < game.px,
          "cam_x %.3f vs px %.3f" % (game.cam_x, game.px))

    game.lean = -1.0
    game._update_camera_position()
    check("leaning left peeks the other way", game.cam_x > game.px)

    # Jammed against a wall, the lean has to give way rather than push your
    # head through it.
    game.px, game.py = 1.3, 5.5      # hard up against the west wall
    game._update_camera_position()
    # Facing south (+y), the player's right-hand side points at -x, which is
    # where that wall is. So a full right lean is a lean straight into it.
    game.lean = 1.0
    game._update_camera_position()
    check("leaning into a wall is cut short, not clipped through",
          not game.is_wall(game.cam_x, game.cam_y)
          and abs(game.lean_applied) < 1.0,
          "lean_applied %.2f at (%.2f, %.2f)"
          % (game.lean_applied, game.cam_x, game.cam_y))
    game.lean = 0.0
    game._update_camera_position()

    # -- aiming down the sight --
    game.load_level(0)
    game.state = trident.STATE_PLAYING
    game.ads = 0.0
    game._update_camera_vectors()
    hip_plane = game.plane_len
    game.ads = 1.0
    game._update_camera_vectors()
    check("the sight narrows the field of view", game.plane_len < hip_plane,
          "hip %.3f -> ads %.3f" % (hip_plane, game.plane_len))
    check("the zoom matches ADS_FOV_DEGREES",
          abs(game.plane_len
              - math.tan(math.radians(trident.ADS_FOV_DEGREES) / 2)) < 1e-9)

    # Holding the right button should bring the sight up, and letting go
    # should put it back down.
    game.ads = 0.0
    game.mouse_captured = True
    game._on_aim_down(None)
    check("right mouse starts the sight coming up", game.aiming)
    for _ in range(30):
        game.update_player(0.016)
    check("the sight settles fully up", game.ads > 0.95, "ads %.2f" % game.ads)
    game._on_aim_up(None)
    for _ in range(30):
        game.update_player(0.016)
    check("letting go lowers it again", game.ads < 0.05, "ads %.2f" % game.ads)
    game._update_camera_vectors()

    # -- the sight picture --
    game.load_level(0)
    game.state = trident.STATE_PLAYING
    game.sway_x = game.sway_y = 0.0

    game.ads = 0.0
    game.tick()
    check("no sight tube while hip firing",
          game.canvas.itemcget(game.sight_ring, "state") == "hidden")

    game.ads = 1.0
    game._update_camera_vectors()
    game.tick()
    check("the sight tube appears when fully sighted in",
          game.canvas.itemcget(game.sight_ring, "state") == "normal")

    # The tube and the dot both have to sit on the aim point, or you would be
    # aiming with something that is not pointing where the rounds go.
    ring = game.canvas.coords(game.sight_ring)
    check("the sight tube is centred on the aim point",
          abs((ring[0] + ring[2]) / 2 - trident.SCREEN_W / 2) < 1.0
          and abs((ring[1] + ring[3]) / 2 - game.horizon) < 1.0,
          "tube centre (%.1f, %.1f)" % ((ring[0] + ring[2]) / 2,
                                        (ring[1] + ring[3]) / 2))
    dot = game.canvas.coords(game.dot_item)
    check("the red dot sits on the aim point",
          abs((dot[0] + dot[2]) / 2 - trident.SCREEN_W / 2) < 1.0
          and abs((dot[1] + dot[3]) / 2 - game.horizon) < 1.0)

    # Everything forward of the optic must fold away, or the barrel sits
    # across the glass you are trying to look through.
    forward = [item for item, part in zip(game.gun_items, game.gun_parts)
               if part[1] < trident.GUN_FORWARD_OF_OPTIC]
    check("there are parts in front of the optic to fold away", forward)
    spans = [game.canvas.coords(i) for i in forward]
    biggest = max((c[3] - c[1]) for c in spans)
    check("the barrel folds away behind the sight", biggest < 2.0,
          "tallest forward part is still %.1f px" % biggest)

    game.ads = 0.0
    game._update_camera_vectors()
    game.tick()
    check("the tube goes away again when you lower the weapon",
          game.canvas.itemcget(game.sight_ring, "state") == "hidden")

    # -- hit marker --
    game.load_level(0)
    game.state = trident.STATE_PLAYING
    game.hitmark_timer = 0.0
    game.tick()
    check("no hit marker before you hit anything",
          game.canvas.itemcget(game.hit_items[0], "state") == "hidden")

    # Stand right on top of a hostile and fire - every round should connect.
    target = game.monsters[0]
    game.px, game.py = target.x, target.y - 1.4
    game._update_camera_position()
    game.angle = math.atan2(target.y - game.py, target.x - game.px)
    game._update_camera_vectors()
    game.cast_rays()
    game.shot_timer = 0.0
    game.shoot()
    check("a round that connects raises the hit marker",
          game.hitmark_timer > 0)
    game.tick()
    check("the hit marker is drawn on the reticle",
          game.canvas.itemcget(game.hit_items[0], "state") == "normal")

    # Wait it out by the clock, not by counting frames. The marker fades on
    # elapsed time, and how many frames that takes depends entirely on how
    # fast the machine is.
    deadline = time.perf_counter() + trident.HITMARK_TIME + 0.15
    while time.perf_counter() < deadline:
        game.tick()
    check("the hit marker fades away on its own",
          game.canvas.itemcget(game.hit_items[0], "state") == "hidden")

    # -- low ammo warning --
    game.ammo = trident.LOW_AMMO + 5
    game.tick()
    plenty = game.canvas.itemcget(game.ammo_text, "fill")
    game.ammo = trident.LOW_AMMO - 1
    game.tick()
    check("the round counter warns you when you are nearly dry",
          game.canvas.itemcget(game.ammo_text, "fill") != plenty)

    # -- blood --
    game.load_level(0)
    game.blood = []
    target = game.monsters[0]
    game.damage_monster(target, 1)
    check("hitting a hostile throws blood", len(game.blood) > 0,
          "%d specks" % len(game.blood))
    check("blood starts off the floor",
          all(speck[2] > 0 for speck in game.blood))

    # It must fall, land, and clear itself up rather than piling up forever.
    for _ in range(400):
        game.update_blood(0.016)
    check("blood settles and is cleared away", not game.blood,
          "%d specks left" % len(game.blood))

    game.blood = []
    for _ in range(60):
        game.spray_blood(3.5, 5.5, 40)
    check("the particle pool has a hard ceiling",
          len(game.blood) <= trident.MAX_BLOOD, "%d specks" % len(game.blood))
    check("the pool never outgrows its canvas items",
          len(game.blood) <= len(game.blood_items))

    # -- releasing the mouse --
    game.mouse_captured = True
    game._release_mouse()
    check("Esc-style release hands the pointer back", not game.mouse_captured)
    before = game.angle
    game._warping = False
    game._on_mouse_move(FakeMove(centre_x + 100, centre_y))
    check("mouse movement is ignored once released", game.angle == before)

    # -- clicking fires --
    game.load_level(0)
    game.state = trident.STATE_PLAYING
    game.mouse_captured = True
    game.firing = False
    game._on_mouse_down(FakeMove(0, 0))
    check("clicking while captured pulls the trigger", game.firing)
    game._on_mouse_up(FakeMove(0, 0))
    check("letting go stops firing", not game.firing)

    ammo_before = game.ammo
    game.shot_timer = 0.0
    game.firing = True
    game.update_player(0.016)
    check("holding the mouse button spends ammo", game.ammo < ammo_before)
    game.firing = False

    # -- wall banding --
    check("the band pool fits the busiest wall texture",
          all(len(b) <= trident.MAX_BANDS for b in trident.WALL_BANDS.values()))
    check("every wall type has a texture and a seam table",
          all(c in trident.WALL_BANDS and c in trident.WALL_SEAMS
              for c in trident.WALL_COLOURS),
          "missing: %s" % [c for c in trident.WALL_COLOURS
                           if c not in trident.WALL_BANDS])
    for char, bands in trident.WALL_BANDS.items():
        gapless = all(abs(bands[i][1] - bands[i + 1][0]) < 1e-9
                      for i in range(len(bands) - 1))
        check("wall %r bands cover the whole height with no gaps" % char,
              gapless and bands[0][0] == 0.0 and bands[-1][1] == 1.0)
    check("seam tables have one entry per sample step",
          all(len(t) == trident.SEAM_STEPS for t in trident.WALL_SEAMS.values()))

    # A band offset must never push the shade past the ends of the table.
    worst = max(abs(step) for bands in trident.WALL_BANDS.values()
                for _a, _b, step in bands)
    worst += max(abs(v) for t in trident.WALL_SEAMS.values() for v in t)
    check("shade table is deep enough for the texture offsets (%d < %d)"
          % (worst, trident.SHADE_LEVELS), worst < trident.SHADE_LEVELS)

    # -- staying alive when things go wrong --
    game.load_level(0)
    game.state = trident.STATE_PLAYING

    # Losing focus must drop every held key. Without this you alt-tab away
    # mid-sprint and come back jammed against a wall.
    game.keys = {"w", "shift_l"}
    game.firing = True
    game.aiming = True
    game.mouse_captured = True
    game._on_focus_lost()
    check("losing focus lets go of every held key", not game.keys)
    check("losing focus stops you firing", not game.firing and not game.aiming)
    check("losing focus hands the mouse back", not game.mouse_captured)

    # One bad frame must not kill the loop for good. A game that has silently
    # stopped redrawing looks exactly like a crash.
    game._reported_error = False
    broken = game.draw_hud
    game.draw_hud = lambda: 1 / 0
    game.tick()
    game.draw_hud = broken
    check("a broken frame does not stop the game", game.running)
    check("the error is reported rather than swallowed", game._reported_error)
    game.tick()
    check("the next frame carries on normally", game.running)

    # Quitting is idempotent - the window close button and Esc both land here.
    check("quit is available to the window close button", callable(game.quit))

    # -- automatic quality --
    # It has to judge on the REAL frame rate. An earlier version timed only
    # our own Python work, decided every frame was cheap, and so never
    # stepped down on the machines that needed it most.
    def pretend_fps(value, frames):
        game.fps = value
        game.frame_times = [1.0 / value] * 30
        for _ in range(frames):
            game._pace()

    game.set_quality(2)                      # start at the top
    pretend_fps(9.0, trident.DETAIL_PATIENCE * 3)
    check("quality drops when the frame rate is bad", game.quality < 2,
          "quality stuck at %d" % game.quality)
    check("dropping quality really does widen the stripes",
          game.column_step > 1, "stripe width %d" % game.column_step)

    pretend_fps(60.0, trident.DETAIL_PATIENCE * 3)
    check("quality comes back when the frame rate is fine", game.quality == 2,
          "quality stuck at %d" % game.quality)

    # A frame rate between the two thresholds must not make it wobble.
    game.set_quality(1)
    steady = (trident.QUALITY_DROP_FPS + trident.QUALITY_RAISE_FPS) / 2
    pretend_fps(steady, trident.DETAIL_PATIENCE * 4)
    check("a steady frame rate leaves the quality alone", game.quality == 1,
          "quality drifted to %d" % game.quality)

    check("quality never runs off either end of the ladder", True)
    game.set_quality(0)
    pretend_fps(5.0, trident.DETAIL_PATIENCE * 3)
    check("it stops at the bottom of the ladder", game.quality == 0)
    game.set_quality(len(trident.QUALITY_LEVELS) - 1)
    pretend_fps(120.0, trident.DETAIL_PATIENCE * 3)
    check("it stops at the top of the ladder",
          game.quality == len(trident.QUALITY_LEVELS) - 1)

    # Every level has to be drawable, because the machine picks which one gets
    # used and we never get to try it first. Each should draw fewer shapes
    # than the one above it - that is the whole point of the ladder.
    drawn = []
    game.load_level(0)
    game.state = trident.STATE_PLAYING
    for level in range(len(trident.QUALITY_LEVELS)):
        game.set_quality(level)
        game.tick()
        drawn.append(sum(game.column_used))
    check("every quality level renders without error", True)
    check("lower quality really does draw fewer shapes (%s)" % drawn,
          drawn[0] < drawn[-1])
    game.set_quality(trident.DEFAULT_QUALITY)

    check("coarser tiers really do use fewer bands",
          all(len(t[0]) >= len(t[-1]) for t in trident.WALL_BAND_TIERS.values()))
    for char, tiers in trident.WALL_BAND_TIERS.items():
        covers = all(abs(t[0][0]) < 1e-9 and abs(t[-1][1] - 1.0) < 1e-9
                     for t in tiers)
        gapless = all(abs(t[i][1] - t[i + 1][0]) < 1e-9
                      for t in tiers for i in range(len(t) - 1))
        check("wall %r covers the full height at every tier" % char,
              covers and gapless)

    # -- difficulty --
    for level, rules in enumerate(trident.DIFFICULTIES):
        game.difficulty = level
        game.load_level(0)
        hostile = game.monsters[0]
        check("%s sets up without error" % rules["name"], game.max_hp > 0)
        if level > 0:
            check("%s is harder than %s"
                  % (rules["name"], trident.DIFFICULTIES[level - 1]["name"]),
                  rules["enemy_damage"] >= trident.DIFFICULTIES[level - 1]["enemy_damage"]
                  and rules["health"] <= trident.DIFFICULTIES[level - 1]["health"])
        check("%s hostiles have at least 1 health" % rules["name"],
              hostile.hp >= 1)
        check("%s hostiles do at least 1 damage" % rules["name"],
              hostile.melee_damage >= 1 and hostile.ranged_damage >= 1)

    game.difficulty = 0
    game.load_level(0)
    easy_hp, easy_enemy = game.max_hp, game.monsters[0].hp
    game.difficulty = len(trident.DIFFICULTIES) - 1
    game.load_level(0)
    check("the easiest setting gives you more health than the hardest",
          easy_hp > game.max_hp, "%d vs %d" % (easy_hp, game.max_hp))
    check("the easiest setting gives hostiles less health",
          easy_enemy < game.monsters[0].hp)
    check("a medkit cannot heal past the difficulty's health cap",
          True)
    game.hp = game.max_hp
    medkit = next(p for p in game.pickups if p.kind == "h")
    game.px, game.py = medkit.x, medkit.y
    game._update_camera_position()
    game.check_pickups()
    check("a full-health operator leaves the medkit alone", not medkit.taken)
    game.difficulty = trident.DEFAULT_DIFFICULTY

    # -- the save file --
    check("a missing save file falls back to the defaults",
          trident.load_save()["difficulty"] == trident.DEFAULT_DIFFICULTY)

    # Nonsense in the file must not stop the game starting. A save is just a
    # text file - somebody will open it.
    for junk in ("", "not json at all", "[1, 2, 3]", "null",
                 '{"difficulty": "hard"}', '{"quality": 999}',
                 '{"continue_level": -5}', '{"best_score": "loads"}'):
        with open(trident.SAVE_FILE, "w") as handle:
            handle.write(junk)
        data = trident.load_save()
        ok = (0 <= data["difficulty"] < len(trident.DIFFICULTIES)
              and 0 <= data["quality"] < len(trident.QUALITY_LEVELS)
              and 0 <= data["continue_level"] < len(trident.LEVELS)
              and isinstance(data["best_score"], int))
        check("save file survives junk: %r" % junk[:22], ok, "got %s" % data)

    # A real round trip.
    trident.write_save({"difficulty": 2, "quality": 0, "fullscreen": True,
                        "best_score": 4321, "missions_cleared": 3,
                        "continue_level": 1, "continue_score": 900})
    data = trident.load_save()
    check("settings survive a save and reload",
          data["difficulty"] == 2 and data["quality"] == 0
          and data["fullscreen"] is True and data["best_score"] == 4321
          and data["continue_level"] == 1)

    # Progress is recorded when a mission is cleared.
    game.save = dict(trident.DEFAULT_SAVE)
    game.load_level(0)
    game.kills = 5
    game.record_progress(0, 1500)
    check("clearing a mission unlocks the next one",
          game.save["continue_level"] == 1)
    check("clearing a mission records the score", game.save["best_score"] == 1500)
    check("clearing a mission counts towards the total",
          game.save["missions_cleared"] == 1)
    game.record_progress(0, 200)
    check("a worse run does not overwrite your best score",
          game.save["best_score"] == 1500)

    # An unwritable save must not crash anything.
    saved_path = trident.SAVE_FILE
    trident.SAVE_FILE = os.path.join(saved_path, "nope", "cannot", "write.json")
    check("an unwritable save file is handled quietly",
          trident.write_save({"a": 1}) is False)
    game.store_save()
    check("the game keeps running when it cannot save", game.running)
    trident.SAVE_FILE = saved_path

    # -- the main menu --
    game.open_menu()
    check("the menu opens", game.state == trident.STATE_MENU)
    check("opening the menu hands the mouse back", not game.mouse_captured)

    # The menu is a full stop - nothing should move behind it.
    where = (game.px, game.py)
    game.keys = {"w"}
    for _ in range(20):
        game.tick()
    game.keys = set()
    check("nothing moves while the menu is open", (game.px, game.py) == where)

    # Navigation must skip over anything that cannot be chosen.
    game.save["continue_level"] = 0          # nothing to continue yet
    game.menu_index = 0
    game._menu_move(1)
    check("the menu skips entries you cannot pick",
          trident.MENU_ENTRIES[game.menu_index] != "continue")

    game.save["continue_level"] = 1          # now there is
    check("continue turns on once you have cleared something",
          game._menu_enabled("continue"))

    for index, entry in enumerate(trident.MENU_ENTRIES):
        game.menu_index = index
        game.draw_menu()
    check("every menu row draws", True)

    # Changing a value from the menu sticks and is written out.
    game.open_menu()
    game.menu_index = trident.MENU_ENTRIES.index("difficulty")
    before = game.difficulty
    game._menu_adjust(1)
    check("the menu changes the difficulty", game.difficulty != before)
    check("the change is written to the save file",
          trident.load_save()["difficulty"] == game.difficulty)

    game.menu_index = trident.MENU_ENTRIES.index("quality")
    before = game.quality
    game._menu_adjust(1)
    check("the menu changes the detail level", game.quality != before)

    # Starting a mission from the menu actually starts it.
    game.open_menu()
    game.menu_index = trident.MENU_ENTRIES.index("new")
    game._menu_choose()
    check("NEW MISSION starts the game", game.state == trident.STATE_PLAYING)
    check("NEW MISSION starts from the first map", game.level_index == 0)

    game.save["continue_level"] = 1
    game.save["continue_score"] = 700
    game.open_menu()
    game.menu_index = trident.MENU_ENTRIES.index("continue")
    game._menu_choose()
    check("CONTINUE picks up where you left off",
          game.level_index == 1 and game.total_score == 700)

    # -- fullscreen --
    game.open_menu()
    was = game.fullscreen
    game.toggle_fullscreen()
    check("fullscreen toggles", game.fullscreen != was)
    game.tick()
    check("the game still draws in fullscreen", game.running)
    game.toggle_fullscreen()
    check("fullscreen toggles back", game.fullscreen == was)
    game.tick()
    check("the game still draws after leaving fullscreen", game.running)
    game.close_menu()
    game.state = trident.STATE_PLAYING

    # -- the boss --
    boss_level = next(i for i, lvl in enumerate(trident.LEVELS)
                      if any("b" in row for row in lvl["grid"]))
    check("the last mission is the boss mission",
          boss_level == len(trident.LEVELS) - 1)

    game.difficulty = trident.DEFAULT_DIFFICULTY
    game.load_level(boss_level)
    game.state = trident.STATE_PLAYING
    boss = next(m for m in game.monsters if m.info.get("boss"))
    check("the boss has far more health than a normal hostile",
          boss.max_hp > 5 * trident.MONSTERS["t"]["max_hp"])
    check("the boss is bigger than a normal hostile",
          boss.info["height_scale"] > trident.MONSTERS["t"]["height_scale"])
    check("the boss shares the sprite pool layout",
          len(trident.BLACKJACK_BODY) == len(trident.SPRITE_SHAPES)
          and all(part[0] == shape for part, shape
                  in zip(trident.BLACKJACK_BODY, trident.SPRITE_SHAPES)))

    # The bar only belongs on screen once it has noticed you. Drive the draw
    # directly - a full tick would let the boss wake itself up again, since it
    # can see out of its chamber from the moment the mission starts.
    boss.awake = False
    game.draw_boss_bar()
    check("no boss bar before it has seen you",
          game.canvas.itemcget(game.boss_back, "state") == "hidden")
    boss.awake = True
    game.tick()
    check("the boss bar appears once it is awake",
          game.canvas.itemcget(game.boss_back, "state") == "normal")

    full = game.canvas.coords(game.boss_bar)[2]
    boss.hp = boss.max_hp // 2
    game.tick()
    half = game.canvas.coords(game.boss_bar)[2]
    check("the boss bar tracks its health", half < full,
          "%.0f vs %.0f" % (half, full))

    game.damage_monster(boss, 10 ** 6)
    game.tick()
    check("killing the boss takes the bar away",
          game.canvas.itemcget(game.boss_back, "state") == "hidden")
    check("the boss is worth a lot of points", boss.info["score"] >= 1000)

    # It has to be killable with the ammunition on the level, or the last
    # mission is unwinnable.
    game.load_level(boss_level)
    boss = next(m for m in game.monsters if m.info.get("boss"))
    on_level = game.ammo + sum(p.info["ammo"] for p in game.pickups)
    needed = sum(m.hp for m in game.monsters) / trident.SHOT_DAMAGE
    check("the last mission carries enough ammunition to finish it (%d rounds "
          "available, ~%d needed with every shot on target)"
          % (on_level, needed), on_level > needed * 1.5)

    game.load_level(0)

    # -- display scaling --
    check("the game starts unscaled", game.scale == 1)
    base_w = int(game.canvas["width"])
    game.apply_scale(2)
    check("scaling resizes the canvas", int(game.canvas["width"]) == base_w * 2)

    game.state = trident.STATE_PLAYING
    game.close_menu()
    game.tick()
    # Everything drawn must scale together. A stripe left behind at 1x would
    # be an obvious tear down the middle of the picture.
    lit = [game.canvas.coords(i) for i in game.column_items[120]
           if game.canvas.coords(i)[0] > 0]
    check("wall stripes scale with the view",
          lit and abs(lit[0][0] - 120 * game.column_w * 2) < 4,
          "stripe at %s, expected ~%.0f"
          % (lit[0][:1] if lit else "nothing", 120 * game.column_w * 2))
    reticle_2x = game.canvas.coords(game.cross_items[0])[0]
    weapon_2x = max(game.canvas.coords(i)[3] for i in game.gun_items)
    game.apply_scale(1)
    game.tick()
    reticle_1x = game.canvas.coords(game.cross_items[0])[0]
    weapon_1x = max(game.canvas.coords(i)[3] for i in game.gun_items)
    game.apply_scale(2)
    game.tick()
    check("the reticle scales with the view",
          abs(reticle_2x - reticle_1x * 2) < 2,
          "%.0f at 2x vs %.0f at 1x" % (reticle_2x, reticle_1x))
    check("the weapon scales with the view",
          abs(weapon_2x - weapon_1x * 2) < 2,
          "%.0f at 2x vs %.0f at 1x" % (weapon_2x, weapon_1x))

    # Mouse movement is in screen pixels and has to be converted back.
    game.mouse_captured = True
    game._warping = False
    before = game.angle
    game._on_mouse_move(FakeMove(trident.SCREEN_W * 2 // 2 + 200,
                                 trident.SCREEN_H * 2 // 2))
    turned_2x = (game.angle - before + math.pi) % (2 * math.pi) - math.pi
    game.apply_scale(1)
    game._warping = False
    before = game.angle
    game._on_mouse_move(FakeMove(trident.SCREEN_W // 2 + 100,
                                 trident.SCREEN_H // 2))
    turned_1x = (game.angle - before + math.pi) % (2 * math.pi) - math.pi
    check("the mouse feels the same whatever the scale",
          abs(turned_2x - turned_1x) < 1e-6,
          "%.5f at 2x vs %.5f at 1x" % (turned_2x, turned_1x))
    game.mouse_captured = False
    game.tick()
    check("the game still draws back at 1x", game.running)

    # A rescale must not strand whatever was on screen at the time. The
    # visibility bookkeeping is not a position cache, and clearing it left
    # sprites and blood from the moment of the switch stuck there for good.
    game.load_level(boss_level)
    game.state = trident.STATE_PLAYING
    boss = next(m for m in game.monsters if m.info.get("boss"))
    game.px, game.py = boss.x, boss.y - 4.0
    game._update_camera_position()
    game.angle = math.atan2(boss.y - game.py, boss.x - game.px)
    game._update_camera_vectors()
    boss.awake = True
    game.spray_blood(boss.x, boss.y, 20)
    game.tick()
    on_before = len(game._on_screen)
    check("the boss is on screen to begin with", on_before > 0)

    game.apply_scale(2)
    game.tick()
    game.load_level(0)              # a mission with no boss in it
    game.state = trident.STATE_PLAYING
    game.tick()
    check("the boss bar goes away when the mission changes",
          game.canvas.itemcget(game.boss_back, "state") == "hidden")

    stranded = [item for item in game._on_screen
                if game.canvas.coords(item)[0] > trident.PARKED + 100
                and item not in game.pool_items]
    visible_blood = [i for i in game.blood_items
                     if game.canvas.itemcget(i, "state") == "normal"]
    check("no blood is stranded on screen after a rescale",
          len(visible_blood) <= len(game.blood),
          "%d specks drawn, %d alive" % (len(visible_blood), len(game.blood)))
    game.apply_scale(1)

    # -- ten missions --
    check("there are ten missions", len(trident.LEVELS) == 10)
    check("every mission has a different name",
          len({lvl["name"] for lvl in trident.LEVELS}) == len(trident.LEVELS))

    # -- every level loads --
    for index in range(len(trident.LEVELS)):
        game.load_level(index)
        game.cast_rays()
        check("op %d loads and renders" % (index + 1),
              game.total_monsters > 0 and len(game.exits) > 0)


# ---------------------------------------------------------------------------

def main():
    print(__doc__.strip().splitlines()[0])
    print("=" * 62)

    test_levels()

    try:
        import tkinter as tk
        root = tk.Tk()
    except Exception as exc:            # noqa: BLE001
        print("\n2-3. SKIPPED - no display available (%s)" % exc)
    else:
        game = trident.Game(root)
        try:
            test_raycasting(game)
            test_game_loop(root, game)
        finally:
            root.destroy()

    print("=" * 62)
    print("%d passed, %d failed" % (PASSED, FAILED))
    return 1 if FAILED else 0


if __name__ == "__main__":
    sys.exit(main())
