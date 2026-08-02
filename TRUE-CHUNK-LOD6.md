# Genesis True Chunk LOD 6 — source hotfix

Target: Minecraft 1.21.1 / NeoForge 21.1.233 / Java 21

## What the supplied crash log proved

The first textured volume quad crashed before the high-detail layer could draw:

```
java.lang.IllegalStateException: Not building!
  at BufferBuilder.addVertex
  at PlanetVolumeLodRenderer.addTexturedVertex
```

`PlanetVolumeLodRenderer` requested a solid consumer and then a translucent
consumer from Minecraft's shared `MultiBufferSource`. The second request ended
the first transient builder, so the first solid vertex was written into a
builder that was no longer building.

## Fixes in this source

- `PlanetVolumeLodRenderer` now owns private native `ByteBufferBuilder` /
  `BufferBuilder` instances for solid and translucent block-atlas geometry.
- The finished `MeshData` is drawn through the matching `RenderType`; it no
  longer writes to or ends Minecraft's shared world-render batches.
- A render failure degrades to the coarse LOD for that level instead of
  crashing the client.
- The Quaterniondc/Vector3f compile mismatch from Hotfix 2 remains fixed by
  rotating a `Vector3d` first and converting the result to `Vector3f`.
- `WorldLodEarthRenderer` and `OrbitalDhLivePatchRenderer` stop drawing the old
  one-height/one-colour exact patch wherever a textured block-volume chunk is
  available. This removes the flat coloured square underneath real roofs,
  walls, trees, cliffs and builds.

## Important visual fact

The previous run never displayed the new textured block-volume layer; it
crashed on its first vertex. The visible PS1-like terrain was the older
height/colour fallback. This source makes the actual multi-layer chunk shell
reach the GPU for the first time.

Undiscovered terrain still needs a coarse generated fallback. Generated,
visited and modified chunks use the real block-volume representation.

## Validation performed

- Direct Java 21 compilation of all three replaced renderer classes: PASS
- Bytecode uses private `ByteBufferBuilder`/`BufferBuilder`: PASS
- Bytecode calls `RenderType.draw(MeshData)`: PASS
- No `MultiBufferSource` use remains in `PlanetVolumeLodRenderer`: PASS
- Flat exact DH/sparse patches are skipped when a volume tile exists: PASS

Full in-game rendering still requires testing in the user's modpack.
