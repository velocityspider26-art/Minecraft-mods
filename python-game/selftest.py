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

import math
import sys
import time
from collections import deque

import trident


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
    game.angle = math.radians(90)
    game._update_camera_vectors()
    rays = game.cast_rays()
    distance, _side, char = rays[middle]
    check("centre ray measures 16.5 squares", abs(distance - 16.5) < 0.001,
          "got %.4f" % distance)
    check("centre ray hits a stone wall", char == "#", "got %r" % char)

    # -- looking east from the same spot --
    # Column 7 of row 2 is a wall, so from x = 3.5 that is 3.5 squares away.
    game.angle = 0.0
    game._update_camera_vectors()
    rays = game.cast_rays()
    distance, _side, _char = rays[middle]
    check("looking east measures 3.5 squares", abs(distance - 3.5) < 0.001,
          "got %.4f" % distance)

    # -- the fisheye test --
    # Stand 1.5 squares from a long flat wall and look straight at it. Every
    # single column must report the same distance; if the perspective maths is
    # wrong the edges of the screen report further away and the wall bulges.
    game.px, game.py = 3.5, 17.5
    game.angle = math.radians(90)
    game._update_camera_vectors()
    rays = game.cast_rays()
    distances = [r[0] for r in rays]
    spread = max(distances) - min(distances)
    check("a flat wall reads flat across all %d columns" % trident.NUM_COLUMNS,
          spread < 0.001, "spread was %.5f" % spread)
    check("that flat wall is 1.5 squares away",
          abs(distances[middle] - 1.5) < 0.001, "got %.4f" % distances[middle])

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
    check("looking up is clamped", game.pitch <= trident.MAX_PITCH,
          "pitch ran to %.1f" % game.pitch)
    for _ in range(80):
        game._warping = False
        game._on_mouse_move(FakeMove(centre_x, centre_y + 200))
    check("looking down is clamped", game.pitch >= -trident.MAX_PITCH,
          "pitch ran to %.1f" % game.pitch)

    # The whole view has to move together, or the horizon tears away from the
    # floor and you get a bare strip along the bottom of the screen.
    game.pitch = trident.MAX_PITCH
    game.bob_offset = 0.0
    game.tick()
    check("the horizon follows the pitch",
          abs(game.horizon - (trident.SCREEN_H / 2 + trident.MAX_PITCH)) < 1.0,
          "horizon at %.1f" % game.horizon)
    lowest = max(y1 for _item, _y0, y1 in game.bg_items)
    check("the floor still reaches the bottom at full pitch",
          lowest + game.pitch >= trident.SCREEN_H,
          "floor ends at %.1f" % (lowest + game.pitch))

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
