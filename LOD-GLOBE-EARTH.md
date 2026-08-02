# LOD globe Earth pass

Built on top of `cubenet-worldtexture1`.

## What changed

Earth (`minecraft:overworld`) is now rendered as a sphere built from coarse LOD
columns sampled from the actual overworld generator. It is not a curvature
post-process and it is not a cube wearing a screenshot.

For every one of the six cube-net world regions, the server:

1. queries the real chunk generator with `ChunkGenerator#getBaseColumn`, without
   loading a chunk;
2. finds the visible top block in that generated column;
3. stores the block-derived map colour and the real surface Y;
4. sends the six LOD grids to the client once;
5. wraps the grids over a spherified-cube mesh and displaces the radius around
   the generator sea level.

The same cube-net coordinates used for travel are used for the globe. The
`up` region remains centred on world `(0, 0)` and all existing travel mappings
remain unchanged.

## Current LOD tier

Each face uses a `64 x 64` global grid, for 24,576 generated columns across the
whole globe. Rendering chooses a 16, 32, or 64 subdivision mesh according to
apparent planet size. This is the global orbital LOD tier.

This pass represents generated terrain and block materials. Player-built
structures are not yet overlaid, and there is not yet a streamed high-detail
local tile hierarchy. Those can be added later without changing the cube-net
coordinates or the sphere mapping.

## Overworld reveal altitude

While the player is still in the overworld:

- below `Y=16000`: Earth render alpha is exactly `0`;
- `Y=16000..17000`: Earth fades in with smoothstep;
- at and above `Y=17000`: Earth is fully visible;
- the dimension boundary remains controlled separately by
  `atmosphereExitHeight` (currently 20000).

The first-visible altitude is client-configurable:

```toml
CurrentPlanetRevealHeight = 16000
```

## Live terrain setting

The Earth sampler is enabled by default:

```toml
planetSurfaceSampling = true
```

Only `minecraft:overworld` is sampled in this pass. The Moon and Mercury retain
their painted fallback because their generators are known to be too slow for a
global sample. Sampling uses one dedicated minimum-priority daemon thread,
pauses during crossings, never uses Minecraft's world-generation executor, and
abandons Earth after 90 seconds instead of blocking travel. Set the option to
`false` to compare against the painted fallback during diagnosis.

## Expected log lines

```text
[SURFACE] building spherical LOD for minecraft:overworld from six 8192-block world regions
[SURFACE] built spherical LOD for minecraft:overworld from generated block columns; relief y=...,... sea=...
[SURFACE] minecraft:overworld globe is now driven by six real world regions
```

## Test sequence

1. Remove every older Genesis JAR and install only the new JAR.
2. Ascend in the overworld: the globe must be absent below Y=16000, start
   appearing at Y=16000, and be fully visible at Y=17000.
3. Wait for the three `[SURFACE]` log lines above. Sampling is enabled by default.
4. Enter space and orbit Earth. Coastlines, terrain colours, and relief should
   rotate as one spherical body with no cube corners.
5. Check the transition diagnostics:

```bash
grep -E "\[WATCHDOG\]|\[VIEWAREA\]|\[GATE\]|\[SURFACE\]" run/logs/latest.log
```

A clean test has the expected `[VIEWAREA]` and `[SURFACE]` lines and no new
`[WATCHDOG]` dump during a crossing.
