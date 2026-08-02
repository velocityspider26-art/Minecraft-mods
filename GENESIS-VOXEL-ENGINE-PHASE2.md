# Genesis clean-room voxel LOD engine — Phase 2

Target: Minecraft 1.21.1, NeoForge 21.1.233/234, Java 21.

## Why this is clean-room rather than copied Voxy source

The Voxy repository is useful as an architectural reference, but its current
license says that redistribution is prohibited. This branch therefore contains
no Voxy source, package names, binaries, shaders, or copied implementation.
It implements the same broad class of solution independently for Genesis:
sparse 3D voxel bricks, occupancy-aware mip levels, persistent region storage,
greedy meshing, client streaming, and cube-face rendering.

## What is implemented

### Server-authoritative 3D terrain ingestion

`PlanetVoxelChunkIngestor` converts only already-loaded overworld chunks into
exact 16 x 16 x 16 LOD0 bricks. It stores real block-state IDs, biome tint,
sky/block light, fluids, translucency, foliage, emissive state, and a
structure-vs-natural priority flag. It does not force-load or generate another
chunk.

`PlanetVoxelService` listens for loaded and modified chunks, deduplicates work,
and processes one chunk per server tick. Every changed leaf updates only its
logarithmic chain of parent mip bricks.

### Structure-preserving mip reduction

`PlanetVoxelReducer` reduces each 2 x 2 x 2 child group while retaining:

- occupancy coverage;
- structure/player-built blocks before natural terrain;
- emissive blocks;
- opaque roofs/walls;
- foliage and liquids;
- representative block material and lighting.

This avoids converting a village or base into a single averaged ground colour.

### Persistent storage

`PlanetVoxelRegionStore` is an append-only compressed region database with:

- fixed offset/length indices;
- per-record Deflate compression;
- CRC validation;
- atomic index updates;
- record deletion and compaction support.

World storage location:

`<save>/genesis/planet_voxels/`

### Networking and client memory

`PlanetVoxelBrickPacket` streams upserts/removals using palette data plus RLE
of palette indices and occupancy. `PlanetVoxelClientCache` uses a bounded 1 GiB
per-planet LRU cache. The server store uses a bounded 2 GiB memory cache and
persists evicted data.

### Actual 3D renderer

`PlanetVoxelRenderer` replaces the old exact-detail height/colour shell whenever
voxel bricks are available.

- LOD0 is real 3D Minecraft block volume.
- Higher LODs remain volumetric instead of becoming a 2D texture.
- Structure blocks retain one-block texture repetition at LOD0.
- Natural terrain uses limited merging for lower vertex cost.
- Greedy-meshed roofs, walls, cliffs, trees, towers and buildings use the real
  Minecraft block atlas.
- Solid and translucent material passes use private persistent native builders.
- CPU meshing runs on two minimum-priority daemon threads.
- Neighbouring bricks are consulted to remove internal border faces.
- Ascent and orbit consume the same brick stream and cube-face coordinates.
- Camera-relative uniform distance compression keeps long-range geometry inside
  the vanilla far plane without changing its apparent angular size.

The renderer selects an LOD ring by surface distance. Near the orbital survey
crosshair it requests exact LOD0 bricks; farther terrain uses the parent mip
chain. Remote cube faces use coarse 3D mips rather than exact blocks.

### RAM

The project remains configured for:

- runClient/runServer: `-Xms2G -Xmx10G -XX:+UseZGC`
- Gradle: `-Xms1G -Xmx10G -XX:+UseZGC`

## Important current limitations

This is an engineering phase, not a claimed final visual match.

1. The old Distant Horizons/fallback classes are still present. They remain the
   undiscovered-terrain fallback until the voxel database has real bricks. The
   new 3D renderer takes over exact detail when voxel data exists.
2. The server currently streams each changed brick/mip to all players. A later
   pass should add per-player interest manifests and packet batching for large
   servers.
3. Existing stored bricks are not replayed to a newly connected player from
   disk yet. Loaded/dirty chunks are streamed normally during the session.
4. Procedural 3D bricks for never-generated terrain are not implemented. Those
   areas still use the legacy coarse fallback.
5. GPU vertex buffers are not persistent per brick yet. CPU meshes are cached,
   but selected meshes are copied into persistent render builders each frame.
   A later GPU allocator/indirect-draw pass is still required for Voxy-class
   scale.
6. A full Minecraft client/GPU test was not possible in this environment.

## Validation performed

- Java 21 core compile passed.
- Java 21 Minecraft/NeoForge adapter compile passed against the project merged
  NeoForge 21.1.233/Minecraft 1.21.1 classes.
- Java 21 client renderer compile passed against the same target and the
  preceding Genesis compiled classes.
- `PlanetVoxelEngineSelfTest` passed:
  - material packing;
  - palette compression;
  - randomized structure-preserving reductions;
  - persistent region read/write/CRC;
  - batch mip creation and removal;
  - dirty queue deduplication;
  - LRU eviction;
  - greedy meshing of a 16 x 16 slab into six quads.

No temporary compile stubs are inside the project or source ZIPs.
