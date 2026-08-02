# Genesis High-Detail Volume LOD 5

## Purpose

This pass replaces the exact-terrain layer's previous **one height + one colour per X/Z column** format with a structure-preserving vertical-volume format.

The coarse six-face height/colour terrain still exists as the extreme-distance fallback. Generated and DH-cached chunks now override that fallback with textured 3D geometry built from real Minecraft block states.

## Data format

Each exact 16×16 chunk tile contains up to twelve visible vertical material segments per column:

- lower and upper Y;
- Minecraft block-state ID;
- biome/material colour;
- sky and block light;
- liquid, translucent, foliage, emissive, natural and structure flags.

The server scans only the upper 96 blocks of an already-loaded chunk. It never force-loads a chunk for this system. Long solid terrain runs are capped to their upper shell while separated roofs, trees, towers and walls remain separate segments.

Distant Horizons cached columns use the same volume-tile format when DH exposes vertical data for them.

## Meshing

A minimum-priority worker converts each tile revision into logical quads once:

- top surfaces for visible material segments;
- exposed wall intervals after subtracting neighbouring column volumes;
- merged side strips where material, height and lighting match.

Render frames transform and submit the cached quads instead of rebuilding structure silhouettes every frame.

## Texturing

The exact volume renderer uses Minecraft's actual block texture atlas:

- roof and terrain tops use the block model's upward sprite;
- walls use the matching directional sprite;
- biome-tinted blocks retain sampled grass, foliage and water colouring;
- emissive blocks use full-bright lighting;
- translucent/liquid geometry uses a separate translucent pass.

The old colour-only mesh remains underneath solely as a fallback for areas where no exact volume tile exists.

## Performance limits

- Maximum client exact-volume cache: 8,192 chunks per planet.
- Maximum server replay cache: 8,192 chunks.
- One dirty/generated chunk volume capture per server tick.
- Three normal sparse tiles processed per tick.
- One volume tile replayed per joining player per tick.
- Maximum 4,096 segments per 16×16 tile.
- Maximum 12 vertical runs per column.
- Maximum 256 exact volume chunks rendered during ascent at quality 3.
- Maximum 384 exact volume chunks rendered in orbital survey mode.
- Logical meshing runs on one daemon thread at minimum priority.

## Installation

1. Remove every older Genesis JAR.
2. Remove a separate Distant Horizons JAR because this build bundles the supplied DH 3.2.0-b JAR.
3. Install `genesis-1.21.1-0.8.0-port-high-detail-volume-lod5-bundled.jar`.
4. Enter the overworld and allow visited chunks to be captured.
5. Fly upward and test both normal view and orbital survey view.

## Expected logs

```text
[VOLUME-LOD] textured 3D block-volume terrain active during ascent; ... exact chunks rendered
[VOLUME-LOD] textured 3D block-volume terrain active in orbit; ... exact chunks rendered on ...
```

The existing logs should also remain present:

```text
[VIEWAREA] reused ... sections crossing into ...
[TRANSITION] client room handoff ... completed in ... ms
[DH-CUBE] world-to-planet LOD visible at y=...
```

## What to inspect in-game

- Village roofs should retain separate footprints instead of becoming one averaged colour patch.
- House and tower walls should have visible height when viewed obliquely.
- Trees should retain canopy/trunk volume where the source data contains it.
- Grass, water, foliage, roofs and walls should use recognisable Minecraft textures.
- Exact chunks should sit over the coarse cube without replacing the whole face with a sharp square.
- Crossing to space and leaving the world should not produce a payload-registration crash.

## Known limits

- Runtime visuals were not available for verification in this build environment.
- Undiscovered areas still use procedural/coarse LOD until real or DH terrain data exists.
- At full-planet scale a small house is physically below a screen pixel. Recognising an individual house from orbit requires optical survey zoom; near-space views naturally preserve more detail.
- The server exact-volume cache is currently session memory. DH retains its own persistent terrain cache and can repopulate known chunks, but a dedicated server-side volume region-file format remains future work.
- The renderer represents visible block volumes, not entities, interiors hidden by roofs, or complete caves.
- Cross-chunk boundary walls are conservative and may remain until neighbouring-tile edge stitching is added.
