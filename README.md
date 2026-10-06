# Vantage

Distant-terrain (LOD) rendering for **Minecraft 1.21.1 / NeoForge**. Vantage draws simplified
terrain far past your normal render distance, so you can lower the vanilla render distance (the
expensive part) and still see for kilometres — or, flying high, for hundreds of kilometres: the
ground stays visible under you all the way up, fading into haze at the horizon like real life.

* The land around you, out to a kilometre, is real Minecraft terrain even where nobody has been:
  Vantage runs Minecraft's own world generator in the background, so you see the actual trees,
  plants, rocks, rivers and snow that will be there, block for block. Further out a faster
  approximation takes over, all the way to the horizon.
* Works in singleplayer and on any server. Also install it on the server (or, for LAN and
  Essential games, on the host's game) and players get distant terrain they have never explored,
  the real terrain near them included.
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
so explored land shows up immediately, and fills in land nobody has explored from the world
generator, so the ground never just ends:

* **Near you** (`detailDistance`, 1 km) with Minecraft's real world generator, run in throwaway
  chunks on background threads: terrain, surface, trees, plants, rocks, lakes and snow come out
  exactly as the game will make them (only buildings and caves are left out). This is what
  Distant Horizons does too; Vantage makes only the band of each chunk around the surface, so
  it is several times faster.
* **Further out** from a quick sample of the generator's terrain shape and biomes, cheap enough
  to reach hundreds of kilometres.

Explored chunks always replace generated land, and your save is never touched. In multiplayer
Vantage builds LODs from chunks as you load them, remembers them between sessions, and, if the
server has Vantage too, gets the land nobody has explored from the server.

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

Players then get the same as in singleplayer: the real terrain near them (explored chunks as they
are, from the server's memory or save, and unexplored ones made with the world generator on the
server, sent as a compact "skin" of what can be seen, a few kilobytes a chunk), and the quick
approximation further out. Terrain the host's game makes for itself is shared with friends nearby
instead of being made twice. The server does this on low-priority background threads; it never
adds chunks to the world or touches the save. Server settings live in
`config/vantage-common.toml`:

| Setting | Default | What it does |
|---|---|---|
| `serveDistantTerrain` | true | Answer players' requests for unexplored terrain far away |
| `serverThreads` | 0 (auto) | Threads for those requests (auto: a quarter of the cores, 1–4) |
| `maxRequestsPerPlayer` | 24 | Requests each player may have waiting at once |
| `serveDetailedTerrain` | true | Send players the real terrain near them |
| `detailedTerrainDistance` | 1024 | How far from each player, in blocks |
| `detailedTerrainThreads` | 0 (auto) | Threads making real terrain (auto: one per eight cores, at least one) |
| `detailedTerrainCacheMiB` | 64 | Memory for terrain made for players, so the next one gets it at once |

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
| `detailedGeneration` | true | Near you, use Minecraft's real world generator: real trees, plants, rocks and snow |
| `detailDistance` | 1024 | How far (blocks) the real generator is used; further out the fast approximation |
| `detailThreads` | 0 (auto) | Threads for the real generator (auto: a quarter of the cores, 1–4) |
| `altitudeViewFactor` | 8.0 | High up, LODs reach at least this many times your height above sea level |
| `hazeDistance` | 24000 | Blocks of sea-level air that hide about two thirds of the view |
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
seen from a distance is kept. Unexplored land near you is made by Minecraft's own generation
steps in throwaway chunks, unexplored land further out by sampling the generator's terrain
shape directly.

## Development

* `./gradlew test` — unit tests (mesher, mipper, storage, allocator, LOD world, distant
  generation, the planet generator, network messages)
* `./gradlew runClient` — run the game
* `./gradlew runClient -PvantageAutotest` — scripted run that loads the world `run/saves/world`,
  waits for LODs to build, saves comparison screenshots (Vantage on/off) to `run/screenshots`, logs
  frame timings and quits. `-PvantageWorld=<name>` picks another saved world,
  `-PvantageServer=localhost` joins a server instead (the player needs operator rights there),
  and `-PvantageJvmArgs=-Dvantage.autotest.ascent=true` climbs to 15 km taking screenshots
  (`-Dvantage.autotest.at=x,z` starts somewhere else)
* `./gradlew runServer` with `-Dvantage.debug.sandboxBench=true` (add `-Dvantage.debug.sandboxVerify=4`)
  times the real-terrain generator on its own, and compares what it makes with chunks the server
  generates for real

Vantage is an original implementation. It was designed after studying how
[Voxy](https://modrinth.com/mod/voxy) and [Distant Horizons](https://modrinth.com/mod/distanthorizons)
approach the problem, but contains no code from either.
