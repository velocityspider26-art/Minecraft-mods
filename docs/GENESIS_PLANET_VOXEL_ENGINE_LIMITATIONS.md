# Genesis planet voxel engine — known limitations

Written to be read before the first in-game test, so that expected behaviour is
not mistaken for a bug and vice versa.

---

## 1. No in-game verification was possible

This work was done in a headless Linux container with no GPU and no display.
`./gradlew runClient` cannot run here. **No claim in any of these documents about
how the planet looks on screen has been observed.** What is verified is that the
project compiles against real NeoForge 21.1.233 / Minecraft 1.21.1 classes, that
`./gradlew build` produces a jar, and that 212 automated tests pass.

The specific things that can only be confirmed with a GPU:

- that the block atlas binds correctly for `entityCutoutNoCull` /
  `entityTranslucent` when driven through `VertexBuffer.drawWithShader` per
  brick rather than through `MultiBufferSource`;
- that the fog override reads correctly in-scene;
- frame rate under a real brick count;
- that the crossing is *visually* seamless, as opposed to logically gated.

---

## 2. Predicted terrain has no features

Predicted bricks come from `ChunkGenerator.getBaseColumn`, which is the noise
terrain **before** features, carvers and structures run. So for land the player
has never visited, the orbital view shows correct mountains, valleys, oceans,
coastlines and biome surface materials — but no trees, no villages, no ores, no
caves.

This is a deliberate trade: forcing real chunk generation for the whole planet is
explicitly out of scope, and predicted bricks are replaced by exact ones the
moment a chunk generates, via the authority ordering. It does mean that **a
village only appears from orbit once its chunks have really been generated at
least once.** Flying over an area, or increasing the world-preparation preset,
is what makes it appear.

---

## 3. The first coarse cube takes tens of seconds

The complete coarse cube is a few hundred bricks generated at a bounded
background rate (2–8 per tick depending on the preparation preset). Until it
finishes, the far side of the planet is missing rather than coarse. Nothing
draws a placeholder in its place — by design, since a placeholder is what this
whole change removed.

---

## 4. World preparation is a config preset, not a world-creation screen

FAST / BALANCED / HIGH / EXTREME exist and control the coverage LOD, the spawn
detail radius and the background budget, but they live in the common config file
rather than in the world-creation UI. Adding a page to that screen means a
`CreateWorldScreen` mixin and a level-stem-level setting, which is a separate
piece of work with its own compatibility surface. There is also no progress bar
during world creation, because preparation is deliberately backgrounded rather
than blocking.

---

## 5. The fold factor is global, not per-vertex

The design brief asks for a fold that varies with horizontal distance from the
camera. This implementation uses a single per-frame scalar instead, for a
specific reason: a fold factor that differs between neighbouring pieces of
geometry tears them apart at their shared edge, and the brief also forbids cracks
between flat and cube-projected regions. Those two requirements are in direct
tension, and a cracked planet is far more visible than a different fold schedule.

The camera-centred flat zone is obtained a different way, and obtained exactly:
`foldMatrix(face, currentFace, t)` reduces to `flatMatrix(face)` identically when
`face == currentFace`, so terrain on the player's own face is bit-for-bit where
vanilla puts it at every fold value, while the five remote faces wrap around it.
`FoldMatrixTest.theCameraSOwnFaceNeverMovesAtAnyFoldValue` pins this.

A genuinely per-vertex distance-based fold is achievable with a custom core
shader doing the mix in GLSL against a static buffer. That is the correct next
step if the global schedule proves visually unsatisfying; it was not attempted
here because a custom shader cannot be validated without a GPU.

---

## 6. Non-cube block shapes are approximated

Voxels store a block-state id and are meshed as full cubes with the correct
atlas sprite, tint and light. Stairs, slabs, fences, walls and leaves therefore
render with correct **material and silhouette to voxel resolution**, but not with
baked block models. At LOD 0 a slab occupies a full voxel.

Preserving baked geometry for important block types at LOD 0 is listed in the
brief as "where practical"; it is not implemented. It would require running
`BakedModel` quads through the mesher, which changes the mesh format and the
memory budget substantially.

`FLAG_STRUCTURE` blocks do keep one-block texture repetition at LOD 0, so a
village roof reads as shingles rather than one stretched texture.

---

## 7. GPU allocation is one buffer pair per brick

Each brick owns a solid and a translucent `VertexBuffer`. This is simple,
correct and safe to invalidate, but it is not an arena allocator with indirect
draws. Expect a few thousand draw calls at maximum detail. The default 384 MiB
GPU budget and the brick budgets (1200–6144 depending on quality and survey
mode) are set with that in mind, but they have not been tuned against measured
frame times.

---

## 8. Removed packets are a version break

`SparsePlanetLodTilePacket` and `PlanetLodVolumeTilePacket` are no longer
registered. A client and server on different sides of this change will fail
channel negotiation. That is the correct failure mode, but it does mean servers
and clients must be updated together.

---

## 9. Shader pack interaction is unknown

Planet draws override `RenderSystem` fog uniforms and restore them afterwards.
Iris and shader packs that replace the entity render types may ignore the
override, in which case distant planet terrain will be fog-coloured. There is an
existing `IrisCompat` class in the project but it has not been extended to cover
this renderer.

---

## 10. Sable integration was not modified

The brief asks for a `SablePlanetCoordinateBridge`. The project already has
`DimensionalSableBridge`, `CraftKeepAlive`, `SubLevelVisibilitySweep` and the
sub-level mixins, and the orbital renderer takes its camera from the celestial
render coordinates in parent/world space — not ship-local space — via
`CelestialRenderCoordinates.getObserverCelestialRotation`. Since that path was
already correct and the reported problem was elsewhere, nothing in the Sable
integration was changed, and no new bridge class was added to avoid creating a
second competing coordinate path. Craft continuity across the crossing is
therefore unchanged from before this work — neither improved nor regressed.

---

## 11. Performance figures are absent

No frame times, no allocation profiles, no measured brick counts. Those require
a running client. The budgets in the config are starting points chosen from the
structure of the workload, not from measurement, and should be tuned against
`spark` (already a runtime dependency) once the client runs.
