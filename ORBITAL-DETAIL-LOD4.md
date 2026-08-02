# Orbital detail LOD 4

Target: Minecraft 1.21.1, NeoForge 21.1.233/234, Java 21.
Base: `sparse-world-lod3`.

## What the supplied log proved

The Earth/space handoff itself completed: the view area reused 15,000 sections
and the client changed rooms in 34 ms. The fatal crash happened afterward when
`SeamlessTransitionScreen` sent `genesis:transition_ready`, a payload that had
not been registered for client-to-server transmission.

The freeze watchdog separately caught both terrain renderers building large
numbers of immediate-mode quads on the render thread. The log had 889 sparse
tiles and more than 360 exact DH chunks available at once, and the old renderer
attempted to draw thousands of tiles/patches every frame.

## Fixes

### Transition and logout

- Removed the optional `transition_ready` send from the render callback. The
  existing server follow/retrack fallback remains authoritative.
- Clears delayed transition and orbital-survey state before a world disconnect.
- DH exact-patch persistence now snapshots immediately, then writes and closes
  its DH cache on the existing minimum-priority worker. World exit no longer
  performs the multi-megabyte save synchronously on the render thread.

### Complete six-face cube

- The server stages 96 generator-predicted tiles: four by four tiles for each of
  the six faces, one sample per 128 blocks.
- A cancelled global tile is retried. Coverage is only declared complete after
  all 96 tiles actually exist, not merely while jobs are queued.
- Finer 64/16/4-block predictions and exact one-block tiles remain sparse and
  player-driven.

### Render-thread budget

- All visible faces retain their cheap 128-block fallback.
- Finer terrain is rendered only on the camera-facing face and only near the
  relevant point.
- Overworld ascent is capped at 384 sparse tiles and 96 non-duplicated DH exact
  patches instead of thousands.
- Orbit is capped at 224 normal tiles or 512 survey tiles.
- A DH exact patch is skipped whenever the server already supplied the same
  authoritative exact chunk.
- Exact geometry uses top quads and vertical skirts, preserving building roofs,
  cliffs, towers and other height discontinuities as 3D voxel-like relief.

## Orbital survey

A normal whole-planet view cannot physically show a house clearly. At the
current 1:16 scale, a 20-block house is only 1.25 space blocks wide. From the
launch orbit it is far below one pixel at ordinary FOV.

Press **V** in space to enable Orbital Survey, then use the mouse wheel to zoom:

`4x, 8x, 16x, 32x, 64x, 128x`

The crosshair ray is transformed into Earth's rotating cube frame and displays:

- cube face;
- exact surface X/Z;
- best terrain resolution currently cached at that point.

At 128x, a 20-block structure at the default launch distance should occupy
roughly 190 pixels on a 1920-pixel-wide display when centered. This is optical
magnification; the structure and planet are not enlarged or rescaled.

The same face/X/Z target can be passed directly to a future orbital-bombardment
weapon. `EXACT 1-BLOCK SURFACE` means the target area is represented by an exact
generated/DH chunk tile. Procedural regions remain suitable for terrain aiming
but cannot contain player structures that the server has never generated.

## Installation

Remove every older Genesis JAR and any separate copy of the same bundled
Distant Horizons JAR. Install only:

`genesis-1.21.1-0.8.0-port-orbital-detail-lod4-bundled.jar`

## Test sequence

1. Enter the world and wait for sparse/global terrain to begin filling.
2. Fly upward through Y=10,000 and confirm there is no watchdog stall from
   `WorldLodEarthRenderer`.
3. Cross into space. There must be no `transition_ready may not be sent` fatal.
4. Press V, aim at the Earth and scroll to 128x.
5. Aim at a generated base. The HUD should show its face/X/Z and ideally
   `EXACT 1-BLOCK SURFACE` after the chunk data has been cached.
6. Leave the world. The title screen should return immediately while the cache
   saves asynchronously.

Useful grep:

```bash
grep -E "\[WATCHDOG\]|transition_ready|\[SPARSE-LOD\]|\[DH-CUBE\]|\[TRANSITION\]" run/logs/latest.log
```

## Limits of this pass

- No full Minecraft client was available in the build environment, so GPU
  appearance, FPS and actual multiplayer cache delivery need in-game testing.
- Exact tiles are a surface-column representation. They show the highest block
  and vertical height changes, not every hidden interior block, cave or entity.
- A friend’s base can only become exact after its chunks have been generated
  and the server/DH data has reached the observing client.
- Cube-edge walking/gravity rotation is separate work and is not changed here.
