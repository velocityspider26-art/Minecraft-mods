# Genesis planet voxel engine — testing

## Build commands

```bash
./gradlew compileJava     # or gradlew.bat on Windows
./gradlew test
./gradlew build
```

## Results

| Command | Result |
| --- | --- |
| `./gradlew compileJava` | **PASS** (warnings only: pre-existing `EventBusSubscriber.bus()` and `FluidType.initializeClient` deprecations) |
| `./gradlew test` | **PASS** — 212 tests, 0 failures, 0 errors |
| `./gradlew build` | **PASS** — `build/libs/genesis-1.21.1-0.8.0-port-true-world-lod7.jar` (3.99 MB), copied to `dist/` |
| `./gradlew runClient` | **NOT RUN** — see “Runtime status” below |

Environment: Ubuntu container, OpenJDK 21.0.10, NeoForge 21.1.233, Gradle 8.14,
ModDevGradle 2.0.141.

### Per-class results

```
PlanetVoxelLodSelectorTest             4    CubeSurfaceProjectionTest          10
CoordinateTransformationTest           6    FoldMatrixTest                      6
FaceShadowTest                         6    PlanetVoxelAuthorityTest            2
PlanetShadingTest                     24    PlanetVoxelInterestManagerTest      3
ShadowComputationBinarySearchTest      6    PlanetVoxelPredictionSamplerTest    2
ShadowGeometryVisualizationTest        3    PlanetVoxelRegionStoreTest          3
ShadowProjectionTest                   8    CubeFaceFrameTest                   2
ShadowRenderingIntegrationTest         6    CubeNetSurfaceTransformTest         7
OBBTest                               24    PlanetToSpaceTeleporterTest         4
OcclusionTest                         21    SpaceLevel celestialRaycast        13
PolygonClippingTest                   43    PlanetVoxelEngineSelfTest           9
                                                                        TOTAL 212
```

`PlanetVoxelEngineSelfTest` was a `main()` harness and therefore never ran under
`gradlew test`. It is ordinary JUnit now, which is what moves rows 8–10, 14, 17
and 18 of the table below from "written" to "enforced by the build".

## Coverage against the required test list

| # | Requirement | Where | Status |
| --- | --- | --- | --- |
| 1 | Cube coordinate round-trip | `CubeSurfaceProjectionTest.faceLocalRoundTripsThroughCubeSpaceOnEveryFace`, `worldToFaceAndBackIsExactIncludingNegativeCoordinates` (4000 random samples), `CubeNetSurfaceTransformTest` | ✅ |
| 2 | Face edge continuity | `FoldMatrixTest.adjacentFacesMeetAlongTheirSharedEdgeWhenFullyFolded` | ✅ |
| 3 | Face corner continuity | `CubeSurfaceProjectionTest.faceCornersAndEdgesStayOnACubeFace` | ✅ |
| 4 | Negative coordinate mapping | `worldToFaceAndBackIsExactIncludingNegativeCoordinates` (all six faces, negative X/Z regions of the net) | ✅ |
| 5 | World-border clamping | `columnsOutsideTheNetAreReportedRatherThanSilentlyRemapped`, `CubeNetSurfaceTransformTest` | ✅ |
| 6 | LOD brick key conversion | `PlanetVoxelLodSelectorTest`, `PlanetVoxelAuthorityTest` | ✅ |
| 7 | Parent/child brick relationships | `PlanetVoxelLodSelectorTest.brickTracksOnlyOccupiedOctants`, `completeOccupiedChildrenReplaceTheirParent` | ✅ |
| 8 | Voxel mip reduction | `PlanetVoxelEngineSelfTest` (randomised reductions) | ✅ |
| 9 | Structure preservation | `PlanetVoxelEngineSelfTest`, `PlanetVoxelAuthorityTest` | ✅ |
| 10 | Palette serialization | `PlanetVoxelEngineSelfTest` | ✅ |
| 11 | Brick compression/decompression | `PlanetVoxelRegionStoreTest` | ✅ |
| 12 | CRC failure handling | `PlanetVoxelRegionStoreTest` | ✅ |
| 13 | Region storage read/write | `PlanetVoxelRegionStoreTest` | ✅ |
| 14 | Dirty chunk propagation | `PlanetVoxelEngineSelfTest` (dedup queue) | ✅ |
| 15 | Revision handling | `PlanetVoxelInterestManagerTest` | ✅ |
| 16 | Network packet size validation | `PlanetVoxelInterestManagerTest` (bounded plans, clamped request fields) | ✅ |
| 17 | Greedy meshing | `PlanetVoxelEngineSelfTest` (16×16 slab → six quads) | ✅ |
| 18 | Mesh face culling | `PlanetVoxelEngineSelfTest` | ✅ |
| 19 | LOD hysteresis | `PlanetVoxelLodSelectorTest.missingOccupiedChildKeepsCoarseBrickVisible`, `coverageRootsAreNeverDiscardedByDetailBudget` | ✅ |
| 20 | Approach coordinate continuity | `CubeSurfaceProjectionTest.rayFromOutsideHitsTheFaceItIsAimedAt`, `offsetRayResolvesTheSurfaceCoordinateItIsAimedAt`, `FoldMatrixTest.foldIsContinuousInTheFoldParameter`, `PlanetToSpaceTeleporterTest` | ✅ |

Additional tests written for this change beyond the required list:

- `FoldMatrixTest.foldMatrixMatchesThePerVertexMorphEverywhere` — 36 face pairs
  × 5 fold values × 40 random samples, agreement to 1e-7. This is the test that
  licenses replacing the per-vertex CPU morph with a per-face matrix.
- `FoldMatrixTest.theCameraSOwnFaceNeverMovesAtAnyFoldValue` — proves the flat
  zone around the craft is exact rather than tuned.
- `FoldMatrixTest.orbitMatrixScalesAndPlacesTheCubeWithoutDistortingIt` — the
  orbital placement is a rigid scale, so terrain is never sheared.
- `CubeSurfaceProjectionTest.velocityTransformsAreInverses` — the surface/space
  frame change preserves speed, which is what keeps craft motion continuous
  across a crossing.
- `CubeSurfaceProjectionTest.gravityUpRotatesFromSpaceUpToTheFaceNormal` — the
  approach gravity frame is a unit vector at every alignment value.

## Runtime status

**No Minecraft client was launched.** This environment is a headless Linux
container with no GPU and no display, so `./gradlew runClient` cannot run.
Nothing in this document claims in-game visual behaviour was observed.

What *is* verified: the project compiles against real NeoForge 21.1.233 and
Minecraft 1.21.1 classes (not stubs), the full `build` task succeeds and
produces a loadable jar, and all headless logic — the coordinate system, the
fold mathematics, LOD selection, mip reduction, region storage and packet
bounds — passes automated tests.

## The exact test to run next

```bash
./gradlew runClient
```

Then, in order:

1. **Create a world.** Watch the log for
   `[PLANET-VOXEL] clean-room 3D voxel pyramid active` and
   `[PLANET-VOXEL] deterministic 3D terrain prediction active`.
2. **Turn the overlay on.** `config/genesis-client.toml` →
   `[PlanetVoxelEngine] DebugOverlay = true`. Restart or `/reload`.
3. **Stand at spawn and note the coordinates.** Find a village if there is one
   nearby; that is the landmark for step 7.
4. **Climb.** Past y≈256 LOD terrain should extend beyond the vanilla render
   distance as textured 3D blocks — mountains with real height, tree canopies,
   building walls. Log line: `[PLANET-VOXEL] the world's own 3D LOD chunks are
   extending past the vanilla render distance`.
5. **Keep climbing past y≈4096.** The five remote cube-net regions should begin
   folding around you. **The ground directly below must not move at all** — that
   is the fixed-point property; if it shifts, the fold matrix is wrong.
6. **Reach orbit.** Log line: `[PLANET-VOXEL] the cube Earth in orbit is the
   world's own voxel terrain`. Check that no second Earth appears at any
   altitude and that the overlay's `authority` line shows non-zero `exact` or
   `predicted` counts (never all `fallback`).
7. **Fly around and back.** Aim at the village from step 3, use the survey zoom,
   and descend. Coordinates on arrival must match step 3. Watch for a teleport
   snap, a velocity reset or terrain disappearing at the crossing; the gate log
   `[GATE] ... ready ...` should appear before it happens, not
   `[GATE] ... not ready after 100 ticks`.
8. **Break some blocks, climb again.** The orbital terrain should update.

If step 5 or 7 misbehaves, set `DebugRenderMode = "FOLD"` or `"AUTHORITY"` and
re-run — those two modes distinguish a projection error from a streaming one.
