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

## Distant generation

Where nothing has been explored, terrain is made straight from the world
generator, without generating chunks. In singleplayer (and for the host of a LAN
or Essential game) the client runs it against the integrated server's
generator; other players ask the server (see *Multiplayer* below):

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

## Real terrain nearby

Within `detailDistance` (1 km) unexplored land is made the way the game would
make it, so it has the real trees, plants, rocks and snow. Distant Horizons
does this by running Minecraft's world generation steps on throwaway proto
chunks in a stand-in `WorldGenRegion`; Vantage takes the same approach
(`WorldGenSandbox`, its own code) and makes it cheaper:

* **Batches** of 10×10 chunks, of which the inner 8×8 are kept: features (tree
  crowns) spill into neighbours, so only chunks whose eight neighbours were
  decorated too are complete.
* **Only the band around the surface.** One survey `NoiseChunk` over the whole
  batch gives Minecraft's quick surface estimate for every 4×4 column group;
  each chunk's terrain shape is then computed only from 8 blocks under its
  lowest estimate to 24 above its highest (about 55 blocks instead of 384),
  by handing Minecraft a noise state sized to that band. The estimate leaves
  out the finer shape (mountain jaggedness above all), so a chunk whose terrain
  reaches the band's top, or has a column with nothing solid in it, is made
  again at full height (about 1% of chunks). Under the band counts as rock.
* **Nothing that cannot be seen**: cave entrances, noodle caves and the deep
  cave systems are taken out of the density function (keeping the terrain
  identical), as are ore veins, carvers, structures and the underground
  feature steps. Real ores, geodes, dungeons and the like are skipped; dirt,
  gravel, sand, clay and stone-kind blobs are kept since they show on slopes.
* **Surface rules** check the biome of every stone block above about the
  surface estimate, which is slow where mountains rise above it, yet change
  only the top blocks (at most 8 down in vanilla, more for desert and beach
  sandstone and badlands bands). Deeper stone is swapped for a look-alike the
  rules skip, and swapped back before features run.
* **Features** are placed exactly as `applyBiomeDecoration` seeds them
  (decoration seed per chunk, feature seed per index and step), so trees stand
  where the real world will have them; writes are limited to the 3×3 chunks
  around the one being decorated, as in vanilla.
* **Light**: sky light straight down (water and leaves dim it); enough for the
  LOD's cave culling and shading.

Measured on one thread with nothing else running: about 13 ms per kept chunk
(about 2.5× faster than running every step for the full height). Compared
block for block with the chunks the server generates for real
(`-Dvantage.debug.sandboxVerify`), 92% of columns have the same top block at
the same height; the rest are cave mouths and ravines that are left out, and
overlapping tree crowns (which of two overlapping trees wins depends on the
order chunks are decorated in, which varies in vanilla too). Generated chunks
are marked in `chunks.bin` so they are never made twice, and never overwrite a
chunk that is real.

## Multiplayer

The client cannot run the world generator on a server: it has neither the seed
nor the server's datapacks. So a server with Vantage answers for it:

* On joining and on every dimension change the server sends `planet_info`: does
  it answer terrain requests, the planet radius (Vantage Planet worlds), sea
  level and base rock. Payloads are registered as optional, so players without
  Vantage can join a Vantage server and the other way round.
* The client's generator then sends `terrain_request`s: LOD level, section
  column, and a 1024-bit mask of the voxel columns still unknown. The server
  samples them with the same column sampler as singleplayer and answers with
  `terrain_columns`: one height (short) and palette index (byte) per column,
  plus per palette biome how it looks from afar (top, under, seabed and leaves
  block states, tree cover, tree height), since biome features are not synced
  to clients. About 3 KB per section column.
* The server works on a small daemon thread pool at low priority (default a
  quarter of the cores, 1–4), allows each player 24 waiting requests (more get
  `busy`), and refuses requests for other dimensions or more than 600 km away.
  The client keeps at most 16 requests in flight and gives up on any answer
  older than 15 s. Answers are turned into voxels on the client's own workers.
* Real terrain nearby: `planet_info` also says how far the server sends it.
  The client sends a `detail_request` per unit of 8×8 chunks (a 64-bit mask of
  the chunks it has nothing real or generated for), nearest first, at most 3
  at a time. The server answers each chunk from the best source it has: the
  loaded chunk (read off-thread, as Minecraft's light engine does), the save
  (status `features` or later, through the chunk map's IO worker), or else the
  sandbox above, whose results it keeps (64 MiB) for the next player and
  makes once even when two players ask at the same time. The answer,
  `detail_chunks`, carries chunk *skins*: per column the blocks from its top
  down to where nothing more can be seen (two solid blocks below the lowest
  neighbouring column's first solid block), as runs, with palettes of block
  state and biome ids and the surface biome per 4×4 columns. About 1-3 KB a
  chunk; the client fills stone below, air above, and computes sky light.
  On a game opened to LAN or Essential, what the host's own game makes is
  offered to the same cache, and the host's game uses what was made for a
  friend, so each chunk is made once.

## Vantage Planet

A world type (`vantage:planet`): `NoiseBasedChunkGenerator` with noise settings
built in code from the planet's parameters. The overworld recipe is kept as is
(terrain splines, base 3D noise, caves, aquifers, ore veins, surface rules,
the multi-noise biome table), but the five climate inputs — continentalness,
erosion, weirdness, temperature, humidity — come from one density function
type, `vantage:planet_climate`, instead of vanilla's noises. Everything that
reads climate (terrain height, biomes, structures, Vantage's distant terrain)
follows it.

* **Wrapping** — climate noise is sampled on a cylinder of the planet's
  radius (x becomes an angle), so the world repeats every `2·π·r` blocks
  east-west with no seam. Latitude is linear in z; past a pole it comes back
  down the other side.
* **Continents** — fractal noise with domain warp, shifted so the requested
  share of the surface ends up under water (calibrated against generated
  heights: low coasts, rivers and lakes count too). Below continentalness
  -1.05 vanilla's terrain rises again (mushroom islands); planet oceans level
  off towards -1.0 instead, with rare islands added back where a sparse noise
  peaks far out at sea.
* **Mountain ranges** — erosion drops to its mountain values along the zero
  lines of a second warped noise, giving long winding chains.
* **Climate belts** — in Minecraft's biome table "hot" means desert, so the
  temperature profile peaks in the subtropics (~20°), not at the equator; the
  equator is warm and, with humidity following the wind belts (wet tropics,
  dry subtropics, wet mid-latitudes, dry poles), mostly jungle.
* Climate values are cached per thread and column, and the five functions are
  wrapped in `flat_cache`, so one column costs one evaluation of the model.
* Noises are ordinary `worldgen/noise` entries (`vantage:planet_*`), seeded
  per world by `RandomState` like vanilla's.

Tests build the generator from vanilla's registries plus the mod's noise files
and check ocean share, mountain heights, biome belts, wrapping, and that the
distant-terrain sampler matches the real heightmap.

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
* Vanilla clips everything past its far plane (4× its render distance), and
  its section graph never reaches chunk sections more than its render distance
  (in blocks) above or below the camera's section: flying a few hundred blocks
  up, vanilla draws none of its own chunks. The planner and the coverage mask
  only leave to vanilla what it actually draws (inside the far plane and
  within that vertical band, decided per fragment in the shader), so no hole
  opens under the player at any height.
* Space sky: from three atmosphere scale heights above sea level the sky and
  fog colours fade to black and stars come out (mixins on the sky colour and
  star brightness, plus the fog colour event). The haze on LOD terrain keeps
  the undarkened fog colour, so seen from orbit the planet keeps a bright
  limb.
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
* A server started with `-Dvantage.debug.sandboxBench=true` times the
  real-terrain generator on batches spread over the world;
  `-Dvantage.debug.sandboxVerify=N` also has the server generate the first N
  batches for real and compares every column's top block.
* Chunk skins are tested for keeping every block that can be seen from
  outside the ground (`ChunkSkinTest`).

Pitfall found this way: `PalettedContainer.getAll` yields each *distinct* state
once, not every voxel; chunk sections must be read voxel by voxel.

## Threads and budgets

* Main thread: copies chunk data (time-budgeted per tick), never voxelizes.
* Render thread: uploads at most a configurable number of megabytes per frame,
  frustum-culls the current plan and issues two multi-draw calls.
* Workers (low priority): voxelize, mip, mesh, compress, import.
* One planner thread recomputes the LOD selection when the camera moves.
