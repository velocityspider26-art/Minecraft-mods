# Genesis True World LOD 7 — source handoff

Target: Minecraft 1.21.1, NeoForge 21.1.233/234, Java 21.

## Why the previous result still looked flat

LOD 5 contained real vertical block-volume data for generated/Distant Horizons chunks, but the viewer was still placed in Genesis' tiny 1:16 visual planet frame. At the normal crossing altitude, a house was physically smaller than a screen pixel. The coarse face atlas therefore dominated the image and made exact geometry look like a Microsoft Paint texture.

The reference video does not get its look from a sharper flat texture. It shows a low-orbit camera above real LOD terrain geometry taken from the Minecraft world. This pass changes the render frame so the same principle can work on a cube.

## Main change: simulation scale is no longer visual scale

Genesis simulation and teleportation remain unchanged at 1 space block per 16 planet blocks.

Earth rendering now uses:

- one Minecraft surface block = one rendered LOD block;
- a 4096-block half-face for the existing 8192-block cube-net regions;
- compressed empty altitude above the terrain;
- the same visual frame before and after the seamless dimension crossing.

This preserves the real shape and size of mountains, roofs, forests, towers and player builds instead of shrinking exact chunk geometry into a few pixels.

The altitude mapping is continuous:

- 0–1024 blocks: unchanged;
- 1024–32768 blocks: logarithmically compressed into low orbit;
- above 32768: continues linearly at 1:16.

At an actual camera altitude near Y=21000, Earth is displayed at a low-orbit visual altitude of roughly 2183 blocks. The cube half-face is 4096 blocks, giving an angular half-size of about 33.1 degrees instead of about 9.3 degrees in the previous frame.

## Actual 3D chunk LOD

Generated or DH-known chunks continue to use `PlanetLodVolumeTile`, not the global face color atlas.

The exact layer preserves:

- multiple vertical block/material segments per X/Z column;
- exposed roofs and top surfaces;
- exposed side walls and cliffs;
- tree canopies and trunks where represented by the cached column data;
- towers and separated building layers;
- block-atlas textures with biome tinting;
- translucent water/material pass;
- player builds after their real chunks have been captured or updated.

Changes in this pass:

- exact orbit/ascent detail budget increased to 1024 chunks normally and 1536 in survey mode;
- block-volume cache increased to 16384 tiles;
- scan depth increased from 96 to 192 blocks;
- maximum vertical material runs increased from 12 to 24 per column;
- DH sweep radius increased to 48 chunks;
- all exposed walls use one-block texture repetition instead of four-block stretched textures;
- two background mesh workers and larger persistent native render buffers reduce transition stalls.

The coarse six-face atlas remains only as a fallback where neither the server nor DH has real chunk-volume data. It is not a replacement for known terrain.

## Critical relief correction

The old planet relief formula divided by the 1:16 scale twice. The corrected formula is:

```java
halfSize * PLANET_BLOCKS_PER_SPACE_BLOCK / physicalHalfExtent
```

This produces 0.0625 rendered units per world block in the old physical 1:16 frame and exactly 1.0 rendered unit per world block in the new Earth visual frame.

## RAM

`runClient` and `runServer` now use:

```text
-Xms2G
-Xmx10G
-XX:+UseZGC
```

The Gradle daemon uses:

```text
-Xms1G
-Xmx10G
-XX:+UseZGC
```

These settings affect the ModDevGradle development runs. A separate launcher installation has its own Java-memory setting.

## Files changed

- `build.gradle`
- `gradle.properties`
- `src/main/java/shipwrights/genesis/client/lod/EarthLodVisualFrame.java` — new
- `src/main/java/shipwrights/genesis/client/lod/WorldLodEarthRenderer.java`
- `src/main/java/shipwrights/genesis/client/lod/OrbitalDhLivePatchRenderer.java`
- `src/main/java/shipwrights/genesis/client/lod/PlanetVolumeLodRenderer.java`
- `src/main/java/shipwrights/genesis/client/lod/PlanetVolumeMeshCache.java`
- `src/main/java/shipwrights/genesis/client/lod/PlanetLodVolumeClientCache.java`
- `src/main/java/shipwrights/genesis/client/lod/DhLiveChunkPatchCache.java`
- `src/main/java/shipwrights/genesis/space/renderer/PlanetRenderer.java`
- `src/main/java/shipwrights/genesis/space/surface/PlanetLodVolumeTile.java`
- `src/main/java/shipwrights/genesis/space/surface/SparsePlanetLodService.java`

## Test procedure

1. Replace the project files with this source.
2. Start through the project's `runClient` configuration.
3. Enter a world where Distant Horizons has already cached terrain around a village or player base.
4. Confirm normal chunks transition into textured block-volume terrain during ascent.
5. Continue through the seamless crossing and confirm the same terrain scale and position remain visible in space.
6. Test a mountain, forest, village and tall player build.
7. Leave the world and re-enter to check render-buffer/cache cleanup.
8. Search the log for `[VOLUME-LOD]`, `[DH-CUBE]`, `[WATCHDOG]`, and exceptions.

## Verification status

The ten changed Java compilation units compile with Java 21 against the bundled NeoForge/Minecraft classes, Distant Horizons API, Registrate and the preceding Genesis classes. Temporary compile-only dependency stubs were kept outside the project and are not included in either source ZIP.

A full graphical Minecraft launch was not available in the packaging environment. The source is type-checked, but the claim that it visually matches the reference requires the real in-game test.
