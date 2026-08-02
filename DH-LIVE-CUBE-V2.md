# DH live cube Earth — v2

This pass fixes the specific problem visible in the test screenshot: the orbital
Earth was a cube, but most of it was still a 64×64-per-face terrain summary. On
an 8192×8192 face, one summary cell represents 128×128 world blocks, so builds,
cliffs and destroyed terrain were crushed into a dark colour patch.

## Rendering architecture

Earth remains a strict six-sided cube. No spherical projection is used.

Genesis now renders two layers from the same cube-net coordinates:

1. **Global DH base** — a bounded coarse surface for the complete six-face
   planet, so unknown territory does not become an empty black face.
2. **Exact live chunk patches** — DH's complete 16×16 terrain columns for every
   observed or modified chunk, rendered as voxel-like top surfaces plus vertical
   skirts. These preserve buildings, excavations, coastlines and cliffs at
   block-column resolution.

The exact patches use the same transform in both places:

```text
Overworld ascent -> folded cube face -> seamless dimension crossing -> orbital cube
```

There is no texture/model swap at the crossing.

## Live world changes

The client does not rescan the whole world every frame. It uses two bounded
sources of work:

- `DhApiChunkModifiedEvent` queues the exact chunk changed by placement,
  breaking, explosions, terrain updates or incoming multiplayer LOD updates.
- A slow 24-chunk-radius rotating sweep repairs missed notifications and fills
  nearby DH-cached terrain without force-loading Minecraft chunks.

One minimum-priority daemon processes at most 48 queued chunks per pass. The
cache keeps the most recently used 8192 chunks and persists beside DH's own
world database as `genesis-cube-live-v2.bin`.

A multiplayer build can appear from space after the client or DH server has
actually received and cached that chunk. Genesis does not invent or force-load
terrain the server never sent.

## One-JAR installation

The release JAR contains the original, unmodified Distant Horizons 3.2.0-b JAR
as a NeoForge Jar-in-Jar dependency. DH remains its own nested mod with its
original mod ID, metadata, mixins and LGPL notices. Genesis uses its public
terrain repository and events instead of copying/renaming DH classes.

Remove any standalone Distant Horizons JAR before installing this build, because
the nested copy already supplies it.

## Visual changes

- Earth remains cubic in orbit.
- Default Genesis world-LOD quality is now `3`.
- The exact current-face patches use one terrain cell per Minecraft block when
  the cube is close enough.
- Night-side ambient light was raised from `0.10` to `0.28`, preventing real
  terrain from becoming an unreadable black slab.
- Recently observed exact patches are drawn first and back-facing cube faces are
  culled in orbit.

## Expected log lines

```text
[DH-CUBE] Distant Horizons terrain and render hooks installed
[DH-LIVE] attached live 16x16 chunk overlay to ...
[DH-LIVE] restored ... exact terrain chunks from DH-side cache
[DH-LIVE] ... exact chunks cached; latest x,z on FACE
[DH-CUBE] world-to-planet LOD visible at y=... (... DH base columns / ... exact chunks)
```

## Verification performed

- Java 21 direct compilation passed for every replaced/new class.
- 120,000 randomized cube-face coordinate and round-trip checks passed.
- Packaged class hashes match the directly validated bytecode.
- Nested Distant Horizons JAR is byte-for-byte identical to the supplied file.
- ZIP/JAR integrity and Jar-in-Jar metadata JSON checks passed.

A full Minecraft launch was not available in this sandbox, so GPU appearance,
NeoForge nested-mod discovery and multiplayer update timing still require the
real in-game test.
