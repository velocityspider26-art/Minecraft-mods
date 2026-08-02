# VS Genesis — cube-net world/texture pass

Built 2026-07-31 on top of `seamless-roomfix1`.

## What this build changes

This build establishes one invertible coordinate transform between a body's
rotating local space frame and six regions in its planet dimension.

The region layout is the same 3x2 layout used by the planet texture atlas:

```text
north | west | south
east  | down | up
```

The **up** face remains centred at world `(0, 0)`. Existing terrain and builds in
the old reachable square therefore stay at the same coordinates.

For Earth (`actualSize = 512`, one face = 8192 planet blocks):

| Face | Centre X/Z | Approximate region bounds |
|---|---:|---:|
| north | `-16384, -8192` | X `-20480..-12288`, Z `-12288..-4096` |
| west | `-8192, -8192` | X `-12288..-4096`, Z `-12288..-4096` |
| south | `0, -8192` | X `-4096..4096`, Z `-12288..-4096` |
| east | `-16384, 0` | X `-20480..-12288`, Z `-4096..4096` |
| down | `-8192, 0` | X `-12288..-4096`, Z `-4096..4096` |
| up | `0, 0` | X `-4096..4096`, Z `-4096..4096` |

Shared boundary lines are assigned deterministically to one neighbouring face.
They are mathematically zero-width and do not affect ordinary flight.

### Travel

- Space entry now chooses the face from the dominant local-space axis.
- All three local coordinates participate; no face collapses to a line or plane.
- Launch decodes the face and in-face coordinates directly from planet-world X/Z.
- `ENTRY_DIRECTIONS` is removed from source. Relogging or changing ships no
  longer destroys the information needed for a correct exit.
- Approach pre-generation, actual entry, launch pre-generation and actual launch
  all use the same transform.
- The actual entry detector now uses the same padded cube as the documented
  coordinate transform. Previously it tested the physical cube while mapping
  and pre-generation used the padded cube.

### World becomes the orbital texture

When `planetSurfaceSampling` is enabled:

- The sampler reads all six cube-net regions instead of only the origin square.
- The network packet carries six 64x64 colour grids and six 64x64 height grids.
- Each region is copied into its matching texture-atlas face.
- The orbital planet is subdivided and displaced along each face normal using
  the sampled world height at that exact region coordinate.
- Relief uses the same `1 space block = 16 planet blocks` scale as travel.
- Mesh LOD is 16, 32 or 64 subdivisions per face based on apparent size.
- Bodies without a successful sample keep their existing painted texture and
  ordinary cube mesh.

`planetSurfaceSampling` remains **off by default** because the project brief
records previous freezes and very slow Mercury/Moon generators. This build does
not silently re-enable it.

## Verification completed here

- Java 21 pure transform compile: passed.
- 60,000 randomized face -> world -> face/local round trips: passed.
- All six mesh face windings point outward: passed.
- Modified surface data, sampler and texture classes compiled against the
  bundled NeoForge 21.1.234 / Minecraft 1.21.1 merged JAR.
- Modified entry and launch teleporters compiled against the same merged JAR,
  using compile-only API stubs matching the descriptors already present in the
  project's last successful classes.
- Modified planet renderer compiled to Java 21 bytecode.
- Full Gradle build/client launch could not run in this sandbox because the
  wrapper requires downloading Gradle 8.14 and network access is unavailable.

## Test procedure

1. Back up the world and copy `run/logs` somewhere safe.
2. Remove the previous Genesis JAR. Install only the new cube-net JAR.
3. Confirm the seamless Earth <-> space transition still works first, with
   surface sampling left off.
4. Approach Earth separately from +Y, -Y, +X, -X, +Z and -Z.
5. After each landing, note the `[CUBENET] entry through ...` line and launch
   again without moving far. The launch line should name the same face.
6. Confirm old builds around `(0,0)` still belong to the up region and have not
   moved.
7. Only after travel is stable, set `planetSurfaceSampling=true` in the Genesis
   common config and restart the world.
8. Wait for both:

```text
[SURFACE] sampled ... from its own terrain
[SURFACE] ... is now drawn from six real cube-net regions
```

9. Inspect the planet from orbit. Six faces should no longer repeat the same
   origin terrain, and close silhouettes should show sampled height relief.

Useful log filter:

```bash
grep -E "\[CUBENET\]|\[SURFACE\]|\[WATCHDOG\]|\[VIEWAREA\]|\[GATE\]" run/logs/latest.log
```

## Deliberately not included in this pass

- Walking/flying across a cube-net region edge does not wrap to the adjacent
  face yet. The regions are coordinate-contiguous, but seam traversal needs its
  own teleport/orientation rule.
- Client gravity-frame camera rolling is not included yet.
- Velocity conversion and atmospheric drag are not included yet.
- Player-built structures are not streamed into the orbital texture; the
  sampler represents generator terrain.

These are isolated follow-up passes. None is required for the cube-net
coordinate round trip or six-face world texture to function.
