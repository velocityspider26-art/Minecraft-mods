# Distant Horizons cube-world LOD pass

Build: `genesis-1.21.1-0.8.0-port-dh-cube-lod1.jar`
Target: Minecraft 1.21.1, NeoForge 21.1.234, Java 21, Distant Horizons 3.2.0-b.

## What changed

This pass removes the previous ascent renderer's loaded-chunk snapshot cache as
the primary planet source. Genesis now reads Distant Horizons' persistent
terrain repository through its bundled public API.

The same six-face `PlanetSurfaceData` is consumed by:

1. the overworld high-altitude renderer while the craft is still on Earth; and
2. `PlanetRenderer` after the seamless crossing into `genesis:great_unknown`.

There is no static Earth texture or detached Earth object rendered underneath
the player in the overworld.

## Visual pipeline

- Below Y=256: vanilla chunks and ordinary DH rendering.
- Y=256 upward: Genesis begins extending real DH terrain beyond the vanilla
  three-dimensional far plane.
- Around Y=512-2048: camera-relative distance compression ramps in. This moves
  the entire distant terrain field uniformly toward the camera while retaining
  its exact angular size and direction. It prevents the ground from being
  clipped without making Earth look smaller.
- Y=15000-16000: the five remote cube-net regions fold around the unchanged face
  beneath the player.
- Y=16000+: the visible terrain is a complete cube Earth made from the six
  overworld regions.
- After the dimension crossing: the orbital cube continues using the same DH
  height and colour arrays.

## Distant Horizons integration

Genesis registers two DH API events:

- `DhApiBeforeRenderEvent`: DH's normal flat terrain pass is cancelled only when
  the current Genesis face has enough DH data and the craft is above
  `WorldLodFullHeight`.
- `DhApiChunkModifiedEvent`: terrain edits trigger a debounced refresh of the
  current face instead of rescanning on every block update.

Terrain repository operations run on the minimum-priority daemon thread
`Genesis-DH-Cube-LOD`. No DH repository call is made on the client/render tick.

Sampling order:

1. current face at 16x16 (256 reads), then publish immediately;
2. current face at 32x32, then publish the refinement;
3. the other five faces at 16x16 each.

The 16/32 grids are expanded into Genesis' existing 64x64 face arrays. This is
a planet-scale LOD, not a full-block mesh.

## Required installation

Put both files in the mods folder:

- `genesis-1.21.1-0.8.0-port-dh-cube-lod1.jar`
- `DistantHorizons-3.2.0-b-1.21.1-fabric-neoforge.jar`

Remove every older Genesis JAR. The Genesis metadata now declares Distant
Horizons 3.2.0-b or newer as a required client dependency.

## Expected logs

```text
[DH-CUBE] Distant Horizons terrain and render hooks installed
[DH-CUBE] attached to ...overworld...
[DH-CUBE] sampling UP from DH cached world columns
[DH-CUBE] UP face published; the overworld and orbital Earth now use the same DH terrain
[DH-CUBE] world-to-planet LOD visible at y=...
[DH-CUBE] the overworld itself is now the complete six-face Earth
```

The face name may differ if the player is in another cube-net region.

## Config

Client config:

```toml
WorldLodStartHeight = 256
WorldLodFullHeight = 1536
WorldCubeCompressionFullHeight = 2048
WorldCubeFoldStartHeight = 15000
CurrentPlanetRevealHeight = 16000
WorldLodRadius = 4096
WorldLodQuality = 2
```

`WorldLodQuality=1` uses fewer remote quads. `3` uses the full 64x64 mesh on
all six faces and is intended for stronger GPUs.

## Verification performed

- All seven replaced/generated classes compiled as Java 21 bytecode.
- The DH-facing classes compiled directly against the uploaded DH 3.2.0-b API.
- Common Minecraft/NeoForge call descriptors were compared with the previous
  working Genesis bytecode.
- The packaged JAR passed ZIP integrity testing.
- Randomized geometry checks confirmed that the face beneath the player is
  unchanged by folding (maximum numeric error below 5e-13 blocks).
- Uniform distance compression preserved every tested camera ray direction.

The complete client was not launched in this environment. In-game rendering and
DH database availability must still be verified from the log lines above.

## Current boundary

This pass implements the requested visual/world-data continuity and retains the
existing six-face landing transform. Walking directly across a cube-face seam
without a topology transfer is not implemented here. The six world regions are
still the existing 3x2 cube net; adding physical seam traversal for players and
Sable sublevels is a separate server-side movement pass and should not be mixed
into a renderer hotfix without runtime testing.
