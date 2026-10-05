# Vantage

Distant-terrain (LOD) rendering for **Minecraft 1.21.1 / NeoForge**. Vantage draws simplified
terrain far past your normal render distance, so you can lower the vanilla render distance (the
expensive part) and still see for kilometres — or, flying high, for hundreds of kilometres: the
ground stays visible under you all the way up, fading into haze at the horizon like real life.

* Works in singleplayer and on any server. Also install it on the server (or, for LAN and
  Essential games, on the host's game) and players get distant terrain they have never explored.
  Optional on both sides: players without Vantage can still join, and Vantage players can join
  any server.
* Comes with the **Vantage Planet** world type: Earth-like continents, mountain ranges and climate
  belts on a round planet (see below).
* No other mods required (no Sodium dependency).
* Needs an OpenGL 4.3 GPU (any Windows/Linux GPU from about 2012 on). macOS is not supported.

## Using it

1. Build with `./gradlew build` and put `build/libs/vantage-<version>.jar` in your `mods` folder.
2. Lower **Video Settings → Render Distance** to 8–12 chunks.
3. Set the LOD distance in **Mods → Vantage → Config** (`renderDistance`, default 256 chunks = 4 km).

In singleplayer, Vantage reads every chunk your world has already generated in the background,
so explored land shows up immediately, and fills in land nobody has explored straight from the
world generator (terrain shape, biomes, forests, oceans, snow; no buildings), so the ground never
just ends. Explored chunks always replace generated land, and your save is never touched. In
multiplayer it builds LODs from chunks as you load them, remembers them between sessions, and,
if the server has Vantage too, asks the server for the land nobody has explored.

### The Vantage Planet world type

Pick **World Type: Vantage Planet** when creating a world (or set
`level-type=vantage:planet` in `server.properties`). It keeps everything Minecraft's terrain
has (caves, aquifers, ore veins, structures, every biome), but lays the world out like a planet:

* **Continents and oceans** about 6 km across, instead of patchy land everywhere. About half
  the surface is ocean.
* **Mountain ranges** that run for kilometres as long, winding chains, instead of scattered
  peaks.
* **Climate belts by latitude**, like Earth: jungle around the equator, deserts, badlands and
  savanna in the subtropics, forests and plains in the middle latitudes, taiga, then snow and
  ice spikes at the poles. You spawn at 40° north. North is towards -z: walk about 26 km north
  and you reach the pole.
* **It wraps around**: walk 188 km east (`2·π·radius`) and you are back where you started.
* Distant terrain is drawn curved with the planet's real radius (30 km).

The planet can be tuned with a datapack world preset (all fields optional):

```json
"generator": {
  "type": "vantage:planet",
  "biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:overworld" },
  "planet": {
    "radius": 30000,
    "ocean_fraction": 0.55,
    "continent_scale": 6000,
    "mountain_scale": 3000,
    "spawn_latitude": 40,
    "temperature_offset": 0,
    "humidity_offset": 0,
    "wrap": true
  }
}
```

### Multiplayer, LAN and Essential

To give everyone distant terrain in multiplayer, the game that runs the world needs Vantage too:

* **Dedicated server**: put the same jar in the server's `mods` folder.
* **Open to LAN / Essential**: the host's game is the server, so it already has Vantage. Friends
  who join need Vantage installed to see LODs.

The server samples terrain for players on a few low-priority background threads; it never
generates chunks or touches the save. Server settings live in `config/vantage-common.toml`:

| Setting | Default | What it does |
|---|---|---|
| `serveDistantTerrain` | true | Answer players' requests for unexplored terrain |
| `serverThreads` | 0 (auto) | Threads for those requests (auto: a quarter of the cores, 1–4) |
| `maxRequestsPerPlayer` | 24 | Requests each player may have waiting at once |

### Flying high (space mods)

* The air thins with height: looking down from high up you see the ground clearly, while the
  horizon fades into haze. `hazeDistance` and `atmosphereHeight` tune this.
* The LOD distance grows with your height (`altitudeViewFactor`), up to about 500 km. Coarser
  detail levels (voxels up to 1024 blocks) keep this cheap.
* `planetRadius` bends the terrain like a planet, with a real horizon that sinks as you climb.
* Above the atmosphere (from about 3.6 km up by default) the sky fades to black and the stars
  come out, even by day, while the planet below keeps its glowing haze (`spaceSky`).
* Vanilla stops drawing its own chunks once they are more than your render distance below you
  (or past its far clipping plane); Vantage draws that ground instead, so no hole opens under you.

Press F3 to see Vantage's statistics on the right side of the debug screen.

### Compatibility

* **Sodium**: works alongside it (tested with Sodium 0.8.13).
* **Graphics modes**: Fast, Fancy and Fabulous all work.
* **Shader packs (Iris/Oculus)**: not supported.
* **The Nether**: Vantage stays off there. Its fog hides everything past about 100 blocks anyway.
* On a server without Vantage, LODs exist only where you have loaded chunks (your game cannot
  run the world generator without the server's seed); where explored land ends, the terrain ends
  in a cut-away edge.

### Settings (`config/vantage-client.toml`)

| Setting | Default | What it does |
|---|---|---|
| `enabled` | true | Turn LODs on/off |
| `renderDistance` | 256 | LOD distance on the ground, in chunks (16–32768) |
| `distantGeneration` | true | Fill unexplored land from the world generator (singleplayer, or servers with Vantage) |
| `altitudeViewFactor` | 8.0 | High up, LODs reach at least this many times your height above sea level |
| `hazeDistance` | 12000 | Blocks of sea-level air that hide about two thirds of the view |
| `atmosphereHeight` | 1200 | Air gets ~2.7× thinner every this many blocks up |
| `spaceSky` | true | High above the atmosphere the sky turns black with stars; turn off if another mod draws the sky there |
| `planetRadius` | 0 | Bend terrain like a planet of this radius (blocks); 0 = flat. Vantage Planet worlds use their own radius |
| `pixelsPerVoxel` | 3.0 | Detail. Lower = sharper but more triangles |
| `caveCulling` | true | Drop unlit caves and buried blocks (cannot be seen from far away; saves a lot) |
| `importExistingChunks` | true | Singleplayer background import of the save |
| `workerThreads` | 0 (auto) | Background threads |
| `uploadBudgetKiB` | 4096 | Max geometry uploaded to the GPU per frame (lower = smoother on weak GPUs) |
| `gpuMemoryMiB` | 768 | Max GPU memory for LOD geometry |
| `ramCacheMiB` | 256 | Max RAM for decoded LOD sections |
| `fogStart` | 0.8 | Where terrain starts fading out towards the end of the LOD distance |

LOD data lives in `.minecraft/vantage/<world>/<dimension>/`. Deleting that folder is always safe.

### For mod authors (space mods)

Planets can look different: set the radius, haze and haze colour per dimension from your client
setup (only when Vantage is installed):

```java
if (ModList.get().isLoaded("vantage")) {
    // Earth: a big planet with a blue atmosphere.
    VantageApi.setPlanet(Level.OVERWORLD, new VantageApi.Planet(200_000, 12_000, 1_200, -1));
    // A moon: small, no air, so the horizon stays sharp.
    VantageApi.setPlanet(MY_MOON, new VantageApi.Planet(20_000, 0, 1, -1));
}
```

These replace the matching config values in that dimension. Distant generation works in any
dimension that uses a noise-based (or flat) world generator.

## How it works

See [DESIGN.md](DESIGN.md). In short: chunks are converted to compact voxels and averaged into 11
detail levels (1 to 1024 blocks per voxel), stored compressed on disk, meshed into merged quads on
background threads, and drawn with one GPU multi-draw call per pass. Only what can actually be
seen from a distance is kept.

## Development

* `./gradlew test` — unit tests (mesher, mipper, storage, allocator, LOD world, distant
  generation, the planet generator, network messages)
* `./gradlew runClient` — run the game
* `./gradlew runClient -PvantageAutotest` — scripted run that loads the world `run/saves/world`,
  waits for LODs to build, saves comparison screenshots (Vantage on/off) to `run/screenshots`, logs
  frame timings and quits. `-PvantageWorld=<name>` picks another saved world,
  `-PvantageServer=localhost` joins a server instead (the player needs operator rights there),
  and `-PvantageJvmArgs=-Dvantage.autotest.ascent=true` climbs to 15 km taking screenshots

Vantage is an original implementation. It was designed after studying how
[Voxy](https://modrinth.com/mod/voxy) approaches the problem, but contains no Voxy code
(Voxy's source is all rights reserved).
