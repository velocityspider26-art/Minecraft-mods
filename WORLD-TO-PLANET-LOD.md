# World-to-planet LOD cube — test build 1

This pass replaces the detached current-Earth visual with a renderer owned by
the overworld itself. The normal world, live cached chunk snapshots, the global
six-face surface sample, and the orbital Earth all use the same cube-net
coordinates and the same terrain source.

## Render progression

- **Below Y=256:** vanilla chunk rendering only.
- **Y=256 upward:** the overworld LOD begins fading in beyond the area vanilla
  can still cover.
- **As vertical distance defeats vanilla's far plane:** the LOD also fills the
  centre beneath the craft, preventing the blank-hole problem.
- **By Y=1536:** the flat current-face LOD is fully active.
- **Y=16000–20000:** the other five faces smoothly fold into view.
- **Y=20000–21000:** the complete cube remains stable through the final near-space buffer.
- **At the default Y=21000 room crossing:** the orbital cube uses the same size,
  face layout, terrain heights, colours, sea-level reference, and apparent scale.

This is not a second Earth model placed below the player. While the player is
in the overworld, `PlanetRenderer` refuses to draw a celestial Earth. The
world-level LOD renderer owns the Earth visual.

## Terrain sources

1. **Vanilla chunks** — untouched close to the camera.
2. **Live LOD tiles** — 64x64-block snapshots generated only from client chunks
   that already exist. Each tile stores a 9x9 height/material grid. This can
   retain the top silhouette and colour of local terrain edits and structures
   after the vanilla render sections unload.
3. **Global six-face sample** — guarantees a planet-scale surface and prevents
   an empty void where no live tile has ever been cached.

No chunk is force-loaded by the client LOD cache. Sampling is staged at two to
four tiles per client tick and the cache is bounded to 4096 tiles.

## Cube geometry and coordinates

Earth remains physically consistent with the established 1:16 transform:

- orbital cube: 512 space blocks wide;
- each surface face: 8192 planet blocks wide;
- the `up` face remains centred on world `(0, 0)`;
- all six regions keep the existing cube-net landing transform.

`CubeFaceFrame` is now the shared orientation authority for travel, overworld
LOD, orbital rendering, and surface observer coordinates. Sea level is the
face plane on both sides, eliminating the previous vertical offset. Launch
distance is converted through the same 1:16 scale instead of using the old fixed
400-space-block standoff, so the cube does not jump larger at the handoff.

## Sable/Create Aeronautics handling

The LOD renderer uses `RenderLevelStageEvent.getCamera().getPosition()`, which
is the parent-level camera position used by the rest of Genesis rendering. It
never reads a ship block position or mixes Sable-local coordinates with
parent-world terrain coordinates.

## Client configuration

```toml
WorldLodStartHeight = 256
WorldLodFullHeight = 1536
WorldLodRadius = 4096
WorldLodQuality = 2
CurrentPlanetRevealHeight = 16000
```

Quality values:

- `1`: coarse folded faces;
- `2`: balanced default;
- `3`: full global sample on every face.

## Required in-game verification

1. Remove every older Genesis JAR and install only the new JAR.
2. Launch in an existing overworld near recognizable terrain.
3. Fly vertically through Y=256, 512, 1000, 4000, 16000, 19000, 20000, and 21000.
4. Confirm there is no empty hole directly beneath the craft when vanilla
   chunks fall outside the vertical far plane.
5. Confirm the flat terrain gradually becomes a finite cube only above Y=16000.
6. Cross into space at the default Y=21000 boundary and confirm Earth does not change size or face orientation.
7. Re-enter through at least two different faces and confirm they map to
   different cube-net regions.
8. Grep the log for:

```text
[WORLD-LOD]
[SURFACE]
[VIEWAREA]
[WATCHDOG]
[CUBENET]
```

A successful activation includes:

```text
[WORLD-LOD] live overworld LOD active at y=...; current face=...; cube reveal begins at y=16000
[CUBENET] launch from ... radius=1565
```

## Current limits

- Live edited terrain is an in-memory cache, not a persistent disk LOD database.
- The orbital cube uses the global six-face terrain sample. Local player-built
  details are represented in the high-altitude overworld cache where available,
  but are not yet merged into the orbital atlas after the dimension crossing.
- Geometry is CPU-cached as height/material grids and emitted through a bounded
  render pass; this pass does not yet maintain one persistent GPU vertex buffer
  per tile.
- Cube-edge walking/wrapping remains a separate feature.

The modified classes were directly compiled as Java 21 bytecode against the
project's bundled NeoForge 21.1.234 / Minecraft 1.21.1 merged classes. The full
client still requires an actual game launch for runtime verification.
