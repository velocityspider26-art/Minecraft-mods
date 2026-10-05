# Vantage — design notes

Vantage is a level-of-detail (LOD) terrain renderer for Minecraft 1.21.1 on
NeoForge. It draws simplified terrain far beyond the vanilla render distance so
you can lower the vanilla distance (the expensive part) and still see for
kilometres.

It is an original implementation. Voxy (MCRcortex) was studied for ideas only;
Voxy's source is "All rights reserved", so no code, shaders or assets were
copied from it.

## What was learned from Voxy, and what Vantage does differently

| Topic | Voxy | Vantage |
|---|---|---|
| Loader / version | Fabric only, no 1.21.1 | NeoForge 1.21.1 |
| Dependencies | Requires Sodium | None (vanilla renderer hooks via NeoForge events + 1 mixin) |
| Data source | Chunks you have loaded; manual import command | Loaded chunks **plus** automatic background import of every already-generated chunk in singleplayer saves, **plus** unexplored land generated on the fly from the world generator |
| Max detail levels | 5 (voxel up to 16 blocks) | 11 (voxel up to 1024 blocks), so hundreds of kilometres stay cheap |
| Flying high | Fog hides the ground | Haze thins with altitude, view distance grows with height, optional planet curvature with a real horizon |
| Hidden geometry | GPU occlusion culling | Removed at the source: buried blocks and unlit caves are never stored or meshed, so the GPU never sees them |
| Storage | LMDB/RocksDB + ZSTD natives | Plain Java: sector region files + LZ4 (already shipped with Minecraft), uniform sections stored in the index with zero bytes |
| LOD selection | GPU traversal with screen-space size | CPU planner on a worker thread, screen-space size per voxel, frustum culling per frame on the render thread |

## Data model

* **Voxel** — one `int`: 20-bit visual id, 4-bit block light, 4-bit sky light, and an
  *unknown* flag for space nothing has been written to yet (it renders as sky-lit air).
  A *visual id* (vid) names a `(BlockState, biome)` pair; the biome is only kept
  for blocks whose colour depends on it (grass, leaves, water...).
* **LOD section** — 32×32×32 voxels. At level `L` a voxel covers `2^L` blocks,
  so a section covers `32·2^L` blocks. Levels 0..10.
* **Y axis** — stored relative to the dimension's minimum build height, so all
  section Y coordinates are non-negative and a whole 384-block world fits in a
  single section from level 4 up.
* Levels 0..4 are produced directly from each 16³ chunk section (16 = 2⁴).
  Levels 5..10 are built from 2×2×2 voxel groups of the level below.
* Sections track how many voxel columns are still unknown and whether they hold only air; the
  region index stores both as flags, so the planner can tell "nothing to draw", "partly
  explored" and "never seen" apart without loading anything.

### What is stored ("visible-only" data)

During voxelization (per chunk column):

1. Decorations (flowers, grass tufts, torches, rails...) become air, or water
   if waterlogged.
2. In dimensions with sky light, air with sky light 0 and block light 0 (unlit
   caves) becomes solid filler.
3. Opaque voxels whose six neighbours are all opaque become filler too.

None of this can be seen from beyond the vanilla render distance, and it makes
underground sections uniform, which the store keeps in the index for free.

### Mipping (2×2×2 → 1)

Non-air wins over air. Among non-air children the top layer wins, then opaque
beats translucent, then the most common visual id. Air keeps the maximum
light of its children so faces next to it stay lit; water keeps the minimum, so
sea floors seen through it stay as dark as deep water should be. Eight unknown
children stay unknown, and a mip never overwrites known (e.g. generated) data
with unknown.

## Distant generation (singleplayer)

Where nothing has been explored, terrain is made straight from the integrated
server's world generator, without generating chunks:

* **Height** — the noise router's final density function, re-wired once per
  worker: holder and marker indirections removed, and functions the generator
  marks as two-dimensional (`flat_cache`, `cache_2d`) cached per column. A
  top-down search starting just above the neighbouring column's surface, then a
  bisection to the voxel size, finds the surface in 4–10 density evaluations
  (25–40 µs per column, 60–100× faster than `getBaseHeight`), matching vanilla's
  heightmap within 3 blocks for 94% of columns.
* **Look** — per biome: ground blocks from vanilla and NeoForge common biome
  tags; tree cover read from the biome's own vegetation features (leaves block,
  trunk height, expected trees per chunk from the placement modifiers), so
  modded biomes work; snow and sea ice from the biome's temperature at that
  height.
* **Voxels** — one sample per voxel column at the level the planner asks for,
  turned into voxels with the same "what is seen from above" rule as mipping.
* **Scheduling** — the planner requests sections it wants to draw that are
  absent or incomplete; it keeps drawing the parent until the children exist,
  so coverage appears coarse-first. Generation only ever fills unknown voxels,
  and chunk data always overwrites it. While the save's first import pass runs,
  places the save already has are left to the import.

## Pipeline

```
client chunk load / block change ──► IngestScheduler (main thread, budgeted copy)
                                          │ ChunkSnapshot
                                          ▼
                               worker: voxelize + mip ──► LodWorld (section cache)
singleplayer region files ──► importer ┘       │ dirty sections
                                                ├──► RegionStore (disk, LZ4)
                                                ▼
planner (worker, camera position) ──► mesh requests ──► worker: greedy mesher
                                                              │ quads
                                                              ▼
render thread: upload (budgeted) → frustum cull → 1 multi-draw-indirect per pass
```

## Rendering

* Quads are 8 bytes, pulled from a single shader storage buffer by
  `gl_VertexID` (no vertex attributes per quad, one shared index buffer).
* Each section's quads are split into 6 face directions × {opaque,
  translucent}; directions facing away from the camera are never drawn.
* Per-section data (camera-relative origin, level) comes through one instanced
  attribute indexed by `baseInstance`.
* LODs draw right after the sky, into the main colour buffer with Vantage's
  own depth buffer (reverse-Z float depth when `glClipControl` exists).
  A fullscreen pass then writes LOD depth into the vanilla depth buffer
  (converted to vanilla's projection) so clouds and translucent geometry sort
  correctly against far terrain.
* A small per-chunk coverage texture tells the LOD shader where vanilla is
  drawing. It has two bits per chunk column: *loaded* (vanilla will draw it)
  hides LOD water there, and *built* (vanilla has built the column's surface)
  hides all LOD geometry. Columns vanilla has loaded but not built yet — the
  outer ring, whose neighbours are missing, or areas that just came into view —
  stay covered by solid LODs, so LODs also fill vanilla's loading holes.
* LOD depth written into vanilla's buffer is pushed 0.3% further away, so
  wherever both draw the same surface vanilla wins and nothing z-fights.
* Vanilla terrain fog is pushed out to the LOD distance. LODs apply
  atmospheric haze: air density falls off exponentially with height, and the
  haze along each view ray is integrated exactly, so looking down from high up
  stays clear while the horizon fades. A short fade hides the end of the data.
* The LOD distance grows with the camera's height. With a planet radius set,
  vertices are lowered by `d²/2R` (beyond vanilla's area), culling bounds move
  with them, and fragments the planet would hide (the view ray dipping below the
  bent sea level) are discarded, which gives a real horizon.
* Vanilla clips everything past its far plane (4× its render distance). The
  coverage mask only discards LOD fragments vanilla actually draws, so flying
  above that height never opens a hole under the player.
* Other mods can set the planet radius, haze and haze colour per dimension
  through `com.vantage.api.VantageApi`.
* Requires OpenGL 4.3 (any Windows/Linux GPU from the last ~10 years). macOS
  (OpenGL 4.1) is not supported.

## Testing

* Unit tests cover the mesher (checked face-by-face against a brute-force
  reference on random voxel fields), mipping, storage, the allocator, the LOD
  world update path, and chunk decoding with real Minecraft block containers.
* `./gradlew runClient -PvantageAutotest` drives the real game headlessly
  (it was developed against Mesa's llvmpipe software OpenGL): it waits for LODs
  to build, takes LOD-on/off screenshots from several angles, flies across the
  world to stress streaming, and logs timings. `-Dvantage.debug.levelColors=true`
  tints LODs by detail level. Extra stages: `-Dvantage.autotest.compareRd=N`
  (vanilla at render distance N, for cost comparisons),
  `-Dvantage.autotest.dimensionHop=true` (Nether and back) and
  `-Dvantage.autotest.toggle=true` (switched off at join, back on mid-game).
  Pass them with `-PvantageJvmArgs="..."`.
* Checked this way: Fancy and Fabulous graphics, Sodium 0.8.13 alongside
  Vantage, dimension changes, and switching Vantage on mid-game.

Pitfall found this way: `PalettedContainer.getAll` yields each *distinct* state
once, not every voxel; chunk sections must be read voxel by voxel.

## Threads and budgets

* Main thread: copies chunk data (time-budgeted per tick), never voxelizes.
* Render thread: uploads at most a configurable number of megabytes per frame,
  frustum-culls the current plan and issues two multi-draw calls.
* Workers (low priority): voxelize, mip, mesh, compress, import.
* One planner thread recomputes the LOD selection when the camera moves.
