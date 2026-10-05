# Vantage

Distant-terrain (LOD) rendering for **Minecraft 1.21.1 / NeoForge**. Vantage draws simplified
terrain far past your normal render distance, so you can lower the vanilla render distance (the
expensive part) and still see for kilometres.

* Client-only. Works in singleplayer and on any server; nothing is needed server-side.
* No other mods required (no Sodium dependency).
* Needs an OpenGL 4.3 GPU (any Windows/Linux GPU from about 2012 on). macOS is not supported.

## Using it

1. Build with `./gradlew build` and put `build/libs/vantage-<version>.jar` in your `mods` folder.
2. Lower **Video Settings → Render Distance** to 8–12 chunks.
3. Set the LOD distance in **Mods → Vantage → Config** (`renderDistance`, default 256 chunks = 4 km).

In singleplayer, Vantage reads every chunk your world has already generated in the background,
so explored land shows up immediately. In multiplayer it builds LODs from chunks as you load
them, and remembers them between sessions.

Press F3 to see Vantage's statistics on the right side of the debug screen.

### Settings (`config/vantage-client.toml`)

| Setting | Default | What it does |
|---|---|---|
| `enabled` | true | Turn LODs on/off |
| `renderDistance` | 256 | LOD distance in chunks (16–4096) |
| `pixelsPerVoxel` | 3.0 | Detail. Lower = sharper but more triangles |
| `caveCulling` | true | Drop unlit caves and buried blocks (cannot be seen from far away; saves a lot) |
| `importExistingChunks` | true | Singleplayer background import of the save |
| `workerThreads` | 0 (auto) | Background threads |
| `uploadBudgetKiB` | 4096 | Max geometry uploaded to the GPU per frame (lower = smoother on weak GPUs) |
| `gpuMemoryMiB` | 768 | Max GPU memory for LOD geometry |
| `ramCacheMiB` | 256 | Max RAM for decoded LOD sections |
| `fogStart` | 0.8 | Where distance fog starts (fraction of the LOD distance) |

LOD data lives in `.minecraft/vantage/<world>/<dimension>/`. Deleting that folder is always safe.

## How it works

See [DESIGN.md](DESIGN.md). In short: chunks are converted to compact voxels and averaged into 7
detail levels (1 to 64 blocks per voxel), stored compressed on disk, meshed into merged quads on
background threads, and drawn with one GPU multi-draw call per pass. Only what can actually be
seen from a distance is kept.

## Development

* `./gradlew test` — unit tests (mesher, mipper, storage, allocator, LOD world)
* `./gradlew runClient` — run the game
* `./gradlew runClient -PvantageAutotest` — scripted run that loads the world `run/saves/world`,
  waits for LODs to build, saves comparison screenshots (Vantage on/off) to `run/screenshots`, logs
  frame timings and quits

Vantage is an original implementation. It was designed after studying how
[Voxy](https://modrinth.com/mod/voxy) approaches the problem, but contains no Voxy code
(Voxy's source is all rights reserved).
