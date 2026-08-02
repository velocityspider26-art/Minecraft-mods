# Genesis planet voxel engine — engineering plan

Target: Minecraft 1.21.1, NeoForge 21.1.233/234, Java 21, Create 6.0.10,
Create Aeronautics 1.3.0, Sable 2.0.3, Flywheel 1.0.6.

The rule this plan exists to enforce:

> **The Earth is not a model with a world texture. The Earth is the actual
> world's 3D LOD chunks reprojected onto six cube faces.**

---

## 1. What was already in the repository

The project was further along than its symptoms suggested. A complete
server-authoritative voxel pipeline existed and worked:

| Area | Class | State |
| --- | --- | --- |
| Cube coordinates | `teleportation/CubeNetSurfaceTransform` | Sound. 3x2 cube net, invertible, `Face` enum doubles as atlas order. |
| Face orientation | `teleportation/CubeFaceFrame` | Sound. Right/up/south basis per face. |
| Brick key | `space/voxel/PlanetVoxelBrickKey` | Sound. 16³ bricks, `lod`, parent/child arithmetic. |
| Voxel format | `space/voxel/PlanetVoxelMaterial`, `PlanetVoxelBrick` | Sound. Packed block-state id, tint, sky/block light, flags, coverage, per-cell authority. |
| Mip reduction | `space/voxel/PlanetVoxelReducer` | Sound. Weighted structure-preserving reduction. |
| Persistence | `space/voxel/PlanetVoxelRegionStore` | Sound. Deflate region files, CRC, atomic index. |
| Ingestion | `space/voxel/PlanetVoxelChunkIngestor`, `PlanetVoxelService` | Sound. Exact block capture, dirty queue, bounded workers. |
| Prediction | `space/voxel/PlanetVoxelPredictionManager`, `PlanetVoxelPredictionSampler` | Sound. Deterministic 3D bricks from the real chunk generator's noise router. |
| Streaming | `space/voxel/PlanetVoxelInterestManager`, `networking/PlanetVoxelBrickPacket` | Sound. Per-player viewport interest, revision tracking, bandwidth budget. |
| LOD selection | `space/voxel/PlanetVoxelLodSelector` | Sound. Screen-space, hole-free, hysteresis. |
| Meshing | `space/voxel/PlanetVoxelGreedyMesher`, `client/lod/PlanetVoxelMeshCache` | Sound. Async, neighbour-aware, block atlas. |

### The three defects that produced the reported symptom

1. **`PlanetVoxelRenderer.renderOrbit` had no call site.** The entire orbital
   voxel path was dead code. `space/renderer/PlanetRenderer` drew Earth with
   `renderPlanetAt`, a textured cube built from `PlanetSurfaceTextures` — a
   `DynamicTexture` world map on a cube model. That is the separate Earth model
   with a world texture, and it was the only thing ever drawn from space.

2. **The ascent renderer's flat paths were the default, not the fallback.**
   `WorldLodEarthRenderer` drew three untextured `RenderType.debugQuads()`
   layers — a 64×64-per-face colour/height atlas, a sparse height/colour tile
   hierarchy and DH chunk patches — and only skipped them when
   `PlanetVoxelRenderer.hasCompleteCoarseCoverage` returned true. That gate
   asked for ≥98.5 % occupancy at a LOD chosen as `min(preferredLod,
   highestResidentLod)`; once high-LOD parents propagated it selected a LOD the
   client held no bricks for, measured 0 % coverage, and never retired the flat
   layer. The painted cube was therefore permanent in practice.

3. **Ascent and orbit used different LOD rules.** `renderOverworld` used
   distance rings from the camera and rebuilt geometry into transient buffers
   every frame; `renderOrbit` used screen-space selection and persistent GPU
   buffers. Only one of them was the design.

---

## 2. Classes replaced, retained and removed

### Removed

The flat Earth is deleted, not disabled — there is no configuration that brings
it back, because a permanent blurry cube underneath 3D patches is exactly the
failure mode being designed out.

- `client/lod/DhCubeLodBridge`, `DhLiveChunkPatchCache`, `OrbitalDhLivePatchRenderer`
- `client/lod/PlanetVolumeLodRenderer`, `PlanetVolumeMeshCache`, `PlanetLodVolumeClientCache`
- `client/lod/SparsePlanetLodClientCache`, `WorldLodTileCache`, `PlanetVoxelCoverage`
- `space/surface/SparsePlanetLodService`, `SparsePlanetLodTile`, `PlanetLodVolumeTile`
- `networking/SparsePlanetLodTilePacket`, `PlanetLodVolumeTilePacket`

Distant Horizons goes with them: no `compileOnly`, no `jarJar`, and the
`neoforge.mods.toml` entry becomes `optional`. Nothing in the planet renderer
reads DH's terrain repository any more.

### Retained

Everything in the table in §1, plus `PlanetSurfaceTextures` and
`PlanetSurfaceSampler` — those still serve genuinely painted bodies (the moon,
Mercury). Only Earth is forbidden from being a texture, because only Earth has a
real Minecraft world behind it.

### Added

| Class | Responsibility |
| --- | --- |
| `space/planet/CubeSurfaceProjection` | The single canonical mapping. World↔face, face↔cube, ray intersection, velocity frames, and the flat/cube/fold/orbit render matrices. |
| `space/planet/FlatToCubeFoldController` | The fold schedule and the camera-relative distance compression. |
| `space/planet/SeamlessPlanetHandoffController` | Handoff readiness, reported condition by condition. |
| `space/planet/PlanetRenderDiagnostics` | Live counters and debug-mode selection. |
| `client/lod/PlanetVoxelDebugOverlay` | The debug HUD, off by default. |

---

## 3. Rendering entry points

```
RenderLevelStageEvent.AFTER_SOLID_BLOCKS
  └── WorldLodEarthRenderer.onRenderLevel          (planet dimension, ascent)
        └── PlanetVoxelRenderer.renderOverworld

CelestialRenderDispatcher
  └── PlanetRenderer.invoke                        (space dimension)
        ├── renderVoxelEarth → PlanetVoxelRenderer.renderOrbit   [Earth]
        └── renderPlanetAt                                       [painted bodies]
```

There is exactly one Earth draw path in each dimension, and they consume the
same brick pyramid and the same GPU buffers.

---

## 4. Data flow

```
ServerLevel chunk load / setBlockState (LevelChunkVoxelDirtyMixin)
        │
        ▼
PlanetVoxelService  ── dirty queue, 1 chunk/tick, bounded workers
        │
        ├── PlanetVoxelChunkIngestor ──► exact LOD0 bricks
        └── PlanetVoxelPredictionManager ──► predicted bricks from the
        │        chunk generator's own noise router (never forces chunk gen)
        ▼
PlanetVoxelPyramid ── structure-preserving mip chain, parents only along the
        │              logarithmic path of what changed
        ├──► PlanetVoxelStore   (2 GiB bounded memory)
        └──► PlanetVoxelRegionStore (<save>/genesis/planet_voxels/)
        │
        ▼
PlanetVoxelInterestManager ── per-player viewport selection + baseline roots,
        │                      revision-tracked, per-tick byte budget
        ▼  PlanetVoxelBrickPacket
PlanetVoxelClientCache (LRU, configurable, default 1 GiB)
        │
        ▼
PlanetVoxelLodSelector ── screen-space, hysteresis, hole-free
        │
        ▼
PlanetVoxelMeshCache ── async greedy meshing, neighbour-aware
        │
        ▼
PlanetVoxelRenderer ── render-thread GPU upload, persistent VertexBuffers,
                       per-brick affine matrix (fold or orbit)
```

Authority order, enforced per voxel cell:
`PLAYER_MODIFIED > GENERATED_EXACT > STRUCTURE_PREDICTED > TERRAIN_PREDICTED > EMPTY`.

---

## 5. Coordinate mapping

`CubeSurfaceProjection` is the only place cube arithmetic is written. Everything
is expressed in terms of one local vector:

```
L = (u, faceHalfSpan + (worldY - seaLevel) + nudge, v)
```

- flat placement: `flatMatrix(face) · L`
- cube placement: `cubeMatrix(face, currentFace) · L`
- morph:          `foldMatrix(face, currentFace, t) = lerp(flat, cube, t)`
- orbit:          `orbitMatrix(face, position, rotation, scale) · L`

**Both placements are affine in the same `L`.** That is the key structural fact
of this design: a per-vertex `mix(flatPosition, cubePosition, fold)` is exactly
reproduced by interpolating the two matrices, so the fold costs one matrix per
face per frame instead of a CPU pass over every vertex — and the geometry can
live in an immutable GPU buffer for the whole flight. `FoldMatrixTest` pins the
equivalence rather than trusting the derivation.

**The camera's own face is a fixed point of the fold.** For `face == currentFace`
the cube matrix reduces to the flat matrix identically, so the ground under the
player is bit-for-bit where vanilla puts it at every fold value. The
camera-centred flat zone is therefore exact by construction rather than tuned,
and the five remote faces are what wrap around it.

---

## 6. Threading model

| Thread | Work | Never does |
| --- | --- | --- |
| Server main | dirty queue drain, interest tick, packet emission | blocking disk IO, meshing |
| `Genesis-PlanetVoxel` worker (1) | pyramid mutation, region store IO | touching the level |
| `Genesis-PlanetPrediction-*` (2) | deterministic terrain sampling | touching the level or the pyramid |
| `Genesis-PlanetVoxelInterest` (1) | per-player LOD selection | mutating player state |
| `Genesis-PlanetVoxelMesher-*` (configurable 1–4) | greedy meshing | any GL, `NativeImage`, `RenderSystem` |
| Render thread | GPU upload, draw, buffer disposal | disk IO, world generation, meshing |

Generator references are captured on the server thread before any worker starts.
GPU resources are created, uploaded, closed and invalidated only on the render
thread; `GpuEntry.close` is idempotent so a double close cannot occur.

---

## 7. Networking model

Server-authoritative. Clients send `PlanetVoxelInterestPacket` (camera, look,
viewport, FOV, target voxel pixels, brick budget), throttled to at most one per
250 ms with a 3 s keep-alive. The server replies with `PlanetVoxelBrickPacket`
upserts and removals: palette plus RLE of palette indices, coverage and
authority. Budgets are 8 packets / 256 KiB per player per tick and
48 packets / 1 MiB globally. Revisions are tracked per player so nothing is
re-sent, and coarse bricks stay resident until their finer replacements arrive.

---

## 8. Persistence model

`<save>/genesis/planet_voxels/`, region files holding many bricks, each record
carrying key, LOD, revision, authority, palette, Deflate-compressed payload,
CRC and format version. Index updates are atomic and a corrupt record is
rejected individually rather than invalidating the database.

---

## 9. Expected risks

1. **Predicted vs. generated mismatch.** Predictions use
   `ChunkGenerator.getBaseColumn`, which is the noise terrain *before* features,
   carvers and structures. Predicted bricks therefore show correct mountains,
   oceans and biome materials but no trees or villages until the chunk is really
   generated and ingested. Authority ordering makes the replacement automatic.
2. **Streaming latency on first entry.** The complete coarse cube is a few
   hundred bricks and takes tens of seconds of background work; before that
   completes the planet is visibly coarse. It is 3D and correct, just blocky.
3. **Fog and shader packs.** Planet geometry overrides `RenderSystem` fog
   uniforms for its own draws. Iris/shader packs replacing the entity render
   types may ignore that override.
4. **GPU buffer count.** One `VertexBuffer` pair per brick is simple and safe
   but not an arena allocator; several thousand small draws is the practical
   ceiling.

---

## 10. Performance strategy

- Screen-space LOD across the whole viewport with refine/coarsen hysteresis
  (1.18 / 0.82) so detail does not oscillate.
- Selection re-run only when the view actually moves, with a two-frame floor.
- Frustum culling and, once fully folded, cube backface rejection.
- Bricks entirely inside vanilla's render radius are dropped outright rather
  than cross-faded, which removes both the z-fighting and the work.
- Persistent GPU buffers; per-frame cost is a matrix multiply and a draw call.
- Bounded caches everywhere: client voxel (default 1 GiB), client GPU (default
  384 MiB), server voxel (2 GiB), all LRU.
- `-Xms2G -Xmx10G -XX:+UseZGC` for `runClient`/`runServer`; Gradle capped at
  10 GB.

---

## 11. Testable milestones

1. Cube round trips on all six faces, edges, corners, negative coordinates. ✔
2. Fold matrix equals the per-vertex morph; camera face is a fixed point. ✔
3. Seam continuity between adjacent faces when fully folded. ✔
4. Ray intersection resolves the aimed-at face and surface coordinate. ✔
5. Mip reduction preserves structures; palette and region round trips. ✔ (pre-existing)
6. Hole-free refinement: a missing child keeps the coarse 3D brick. ✔ (pre-existing)
7. In-game: ascend, verify no model appears, descend to the same village. ✖ (requires a GPU)
