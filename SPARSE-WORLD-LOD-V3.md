# Sparse world-to-cube LOD V3

Target: Minecraft 1.21.1, NeoForge 21.1.234, Java 21.

This is the first implementation of the architecture where normal Minecraft
chunks exist only where gameplay needs them and everything farther away is a
lightweight 3D surface prediction. It replaces the previous rotating whole-area
scan.

## Implemented data hierarchy

The renderer now chooses the best available representation in this order:

1. Normal Minecraft chunks inside the configured player view/simulation area.
2. Exact 16x16 surface tiles captured from real loaded/generated chunks.
3. Generator-predicted 16x16 tiles with one sample per 4 blocks.
4. Generator-predicted tiles with one sample per 16 blocks.
5. Generator-predicted tiles with one sample per 64 blocks.
6. The existing six-face global planetary atlas as the final fallback.

All sparse tiles use a face ID plus world-space origin, so the same packet and
cache feed both the overworld ascent renderer and the orbital cube renderer.

## Undiscovered terrain

`SparsePlanetLodService` asks the overworld's real `ChunkGenerator` for surface
columns. It does not call `getChunk`, add tickets, or create normal chunks.
These predictions therefore contain terrain height, top material colour, biome
water/grass/foliage colour and ocean level without mobs, block entities, caves
or simulation.

One small procedural tile is scheduled per overworld player per tick. The
scheduler cycles through three rings instead of scanning the world:

- 4-block cells: local high-detail prediction;
- 16-block cells: regional prediction;
- 64-block cells: broad prediction.

The work runs on a dedicated minimum-priority thread. It aborts and stops
scheduling during an `ArrivalGate` crossing so it cannot compete with the
seamless dimension handoff.

## Discovery and live changes

A server `ChunkEvent.Load` queues each real loaded/generated overworld chunk for
an exact capture. The capture is budgeted to four chunks per tick and only uses
`getChunkNow`, so it never force-loads a chunk.

`LevelChunkSparseLodMixin` also marks a chunk dirty after a successful
`setBlockState`. Multiple changes to the same chunk are deduplicated. This
covers building, breaking, many machine changes, fluid updates and explosions
without continuously rescanning the player area.

Exact tiles are broadcast from the server to every connected player, including
players already in space. New clients receive the current sparse cache at three
tiles per tick rather than one giant login packet.

## Distant Horizons

The unmodified DH 3.2.0-b JAR remains bundled through NeoForge Jar-in-Jar.
Genesis does not rename or duplicate DH classes. DH exact patches remain the
highest-detail client cache where available; the new server sparse hierarchy
fills the large visual gap between those exact patches and the old blurry global
face.

## Gemini's six-dimension proposal

The explicit face topology and 90-degree border-frame rotation are correct
long-term ideas. This build does not create six new Earth dimensions. Doing that
now would change world storage, lighting, Sable sublevels, chunk tickets and
movement at the same time, while risking the seamless crossing system that is
already working.

The new tile key includes the cube face explicitly, so the sparse database is
ready to back six face dimensions later. Full walking across cube seams and the
camera/gravity rotation remain a separate topology pass.

## Current limits

- The current Genesis cube-net still uses the existing 8192-block face regions;
  this build does not yet remap the complete 60-million-block world border.
- Server sparse tiles are held for the current server session. Generator tiles
  are cheap to recreate, while DH keeps its own persistent client terrain cache.
  A server-side region-file store for exact multiplayer tiles is the next
  persistence step.
- Structures in an undiscovered area cannot be predicted unless the structure
  generator is sampled too. They appear once the real chunk loads and its exact
  tile is captured.
- Physical walking/flying across face borders is not implemented here.
- The full Minecraft client could not be launched in this environment; runtime
  visuals, server packet timing and mod compatibility require an in-game test.

## Installation

Remove older Genesis and standalone Distant Horizons JARs. Install only:

`genesis-1.21.1-0.8.0-port-sparse-world-lod3-bundled.jar`

The uploaded DH JAR is embedded byte-for-byte.

## Expected log lines

```text
[SPARSE-LOD] active: real chunks near players, generator-predicted 3D tiles outside them, global cube fallback beyond the sparse cache
[DH-CUBE] world-to-planet LOD visible at y=...; ... sparse tiles
[VIEWAREA] reused ... sections crossing into ...
```

No new procedural work should begin while `ArrivalGate.crossingInProgress()` is
true.
