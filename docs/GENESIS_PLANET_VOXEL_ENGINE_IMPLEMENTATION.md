# Genesis planet voxel engine — implementation

What actually changed in the repository, and why each change was necessary.

---

## 1. The defect that produced the reported symptom

`PlanetVoxelRenderer.renderOrbit` was never called from anywhere. A repository
search for its call sites returned the declaration and nothing else.

Consequently, from space, `space/renderer/PlanetRenderer.invoke` always fell
through to `renderPlanetAt`, which draws a cube model textured with
`PlanetSurfaceTextures` — a `DynamicTexture` world map. However good the voxel
engine underneath was, the player could only ever see the painted model.

During ascent the situation was subtler but had the same effect.
`WorldLodEarthRenderer` drew three flat layers and skipped them only when
`PlanetVoxelRenderer.hasCompleteCoarseCoverage` returned true. That method
called `PlanetVoxelCoverage.analyze`, which picked
`lod = min(preferredLod, highestResidentLod)` and then counted only bricks at
exactly that LOD. Once the server's pyramid propagated parents up to LOD 9,
`highestResidentLod` became 9, `preferredLod` stayed 7, and the analysis
measured coverage at LOD 7 — a level the client typically held no bricks for.
Coverage read 0 %, the gate never opened, and the flat colour tiles stayed on
screen permanently.

---

## 2. One Earth renderer

`PlanetVoxelRenderer` is now the only thing that draws Earth, in both
dimensions. The flat paths are deleted rather than disabled.

**Ascent** — `WorldLodEarthRenderer` shrank from 688 lines of three interleaved
flat-quad passes to a ~100-line entry point that resolves the face, evaluates
the fold schedule and calls `PlanetVoxelRenderer.renderOverworld`.

**Orbit** — `PlanetRenderer.renderVoxelEarth` is new. It expresses the camera in
the planet's own rotating cube frame, resolves the survey target with an exact
`cubeRayIntersection`, and calls `renderOrbit`. `renderPlanetAt` still exists
and still serves genuinely painted bodies; Earth never reaches it.

---

## 3. The fold is a matrix, not a CPU pass

Both placements of a face are affine in the same local vector
`L = (u, halfSpan + (y − seaLevel) + nudge, v)`:

```
flat(face)              = translate(faceCenterX, seaLevel − halfSpan, faceCenterZ)
cube(face, currentFace) = translate(currentCenterX, seaLevel − halfSpan, currentCenterZ)
                          · (basis(currentFace)ᵀ · basis(face))
```

So `mix(flatPosition, cubePosition, t)` — the per-vertex morph the old code ran
every frame — is exactly `lerp(flatMatrix, cubeMatrix, t) · L`. Interpolating
two 4×4 matrices reproduces interpolating every transformed vertex.

Three things follow:

1. **Brick geometry never changes.** It is meshed once in brick-local block
   offsets and uploaded to an immutable `VertexBuffer`. Where it appears is one
   matrix. Ascent, fold and orbit all reuse the same buffer, so the geometry
   crossing the dimension boundary is literally the same geometry.
2. **The fold cannot crack.** The fold factor is a single per-frame scalar, so
   neighbouring bricks are transformed by the same matrix and their shared
   vertices stay welded. A per-brick or per-vertex-distance fold would tear at
   every brick boundary, and a cracked planet is far more visible than a
   different fold schedule.
3. **The camera's own face is a fixed point.** For `face == currentFace` the
   cube matrix reduces to the flat matrix identically. The ground under the
   player is bit-for-bit where vanilla puts it at every fold value. The
   camera-centred flat zone the design calls for is therefore exact by
   construction, not tuned — and the remote faces are what wrap around it.

`FoldMatrixTest` verifies the equivalence over all 36 face pairs × 5 fold
values × 40 random samples to 1e-7, verifies the fixed point, verifies seam
continuity along the UP/SOUTH edge, and verifies continuity in the fold
parameter across 1000 steps.

---

## 4. One LOD rule for the whole flight

`PlanetVoxelLodSelector` gained a `BrickPlacement` hook: where a brick lands on
screen is now supplied by the caller instead of hard-coded to cube space.

- Orbit passes `cubePlacement` — the previous behaviour, unchanged.
- Ascent passes `AscentPlacement`, which puts bricks at their folded world
  positions.
- The server interest manager keeps using `selectOrbit`, which delegates to the
  same code.

Ascent therefore gained, in one change, everything orbit already had: refinement
by projected screen size across the entire viewport rather than distance from
the crosshair, refine/coarsen hysteresis, and hole-free descent that only
subdivides a brick when every occupied child octant is resident. A missing fine
brick shows coarser *3D* terrain — never a hole, never a flat tile.

Uniform camera-relative distance compression is deliberately ignored by the LOD
maths: it scales distance and radius equally, so projected pixel size is
invariant under it. That is the same property that makes the compression
invisible to the player.

---

## 5. Near-field handling

Bricks whose bounding sphere lies entirely inside vanilla's render radius are
dropped from the ascent selection. The old code cross-faded them with an alpha
band, which meant two representations of the same hillside z-fighting at exactly
the distance the player is looking hardest. Handing the near field to vanilla
outright is cleaner, cheaper, and produces identical geometry either way,
because the bricks hold that hillside's real block states.

Planet draws also override the `RenderSystem` fog uniforms and restore them
afterwards. Vanilla fog is tied to render distance, and terrain eight kilometres
away is the entire point of this renderer.

---

## 6. Distant Horizons removed as a dependency

- `build.gradle`: the `compileOnly` / `localRuntime` / `jarJar` entries for the
  DH jar are gone, replaced by an optional `fileTree(dir: 'libs')` sweep for
  development runtimes.
- `neoforge.mods.toml`: the dependency is now `optional`.
- `DhCubeLodBridge` and `DhLiveChunkPatchCache` were the only classes importing
  `com.seibel.*`; both are deleted.

This also fixed a build reproducibility problem: the DH jar lives in `libs/`,
which is gitignored, so a clean checkout could not compile at all.

---

## 7. Handoff readiness

`ArrivalGate.ready` previously answered one question: have the destination
chunks reached FULL? `ArrivalGate.readiness` now returns a
`SeamlessPlanetHandoffController.Readiness` reporting each condition by name:

- the arrival column resolves to a real cube face,
- the coordinates are finite,
- destination chunks are at FULL,
- **the landing column's voxel bricks are captured in the pyramid.**

The last one is new and it is what makes the swap invisible. Crossing before the
pyramid holds the destination terrain means the voxel Earth is replaced by
vanilla chunks that do not yet match it — a one-frame flicker at exactly the
moment the player is watching for it.

The existing 100-tick deadline still applies, so an unsatisfiable condition
delays the crossing by at most five seconds and then logs precisely what it was
waiting on instead of hanging.

---

## 8. World preparation presets

`GenesisCommonConfig.PreparationQuality` — FAST, BALANCED, HIGH, EXTREME —
drives three things: the LOD at which the complete cube is guaranteed to exist,
the fine-detail radius pre-generated around spawn, and the per-tick background
prediction budget. A complete cube at the coarsest level is always generated,
because a planet with holes in it is not a planet. EXTREME additionally honours
`PlanetPreparationRadius`.

`PlanetVoxelPredictionManager.queueSpawnDetail` pre-generates finer bricks
around spawn so the first take-off already has block-scale terrain under it.

---

## 9. Diagnostics

`PlanetRenderDiagnostics` collects face, surface coordinates, simulation and
visual altitude, fold factor, visible/resident brick counts, the LOD histogram,
exact/predicted/fallback authority split, mesh and GPU-upload queue depths,
client and GPU cache usage, streamed bricks and bytes, transition state and the
handoff blocker list.

`PlanetVoxelDebugOverlay` renders it. Debug render modes — `LOD`, `AUTHORITY`,
`BRICK_BOUNDS`, `SEAMS`, `FOLD`, `MISSING` — tint draws or emit wireframe brick
bounds. Everything is off by default.

---

## 10. Files changed

**New**

```
src/main/java/shipwrights/genesis/space/planet/CubeSurfaceProjection.java
src/main/java/shipwrights/genesis/space/planet/FlatToCubeFoldController.java
src/main/java/shipwrights/genesis/space/planet/SeamlessPlanetHandoffController.java
src/main/java/shipwrights/genesis/space/planet/PlanetRenderDiagnostics.java
src/main/java/shipwrights/genesis/client/lod/PlanetVoxelDebugOverlay.java
src/test/java/shipwrights/genesis/space/planet/CubeSurfaceProjectionTest.java
src/test/java/shipwrights/genesis/space/planet/FoldMatrixTest.java
docs/GENESIS_PLANET_VOXEL_ENGINE_*.md
```

**Modified**

```
build.gradle
src/main/resources/genesis.mixins.json
src/main/templates/META-INF/neoforge.mods.toml
src/main/java/shipwrights/genesis/client/lod/PlanetVoxelRenderer.java     (rewritten)
src/main/java/shipwrights/genesis/client/lod/WorldLodEarthRenderer.java   (rewritten)
src/main/java/shipwrights/genesis/client/lod/PlanetVoxelMeshCache.java
src/main/java/shipwrights/genesis/client/lod/PlanetVoxelClientCache.java
src/main/java/shipwrights/genesis/client/OrbitalSurveyController.java
src/main/java/shipwrights/genesis/client/PlanetSurfaceLifecycle.java
src/main/java/shipwrights/genesis/config/GenesisClientConfig.java
src/main/java/shipwrights/genesis/config/GenesisCommonConfig.java
src/main/java/shipwrights/genesis/networking/GenesisNetworking.java
src/main/java/shipwrights/genesis/networking/PlanetVoxelBrickPacket.java
src/main/java/shipwrights/genesis/space/renderer/PlanetRenderer.java
src/main/java/shipwrights/genesis/space/voxel/PlanetVoxelLodSelector.java
src/main/java/shipwrights/genesis/space/voxel/PlanetVoxelPredictionManager.java
src/main/java/shipwrights/genesis/space/voxel/PlanetVoxelService.java
src/main/java/shipwrights/genesis/teleportation/ArrivalGate.java
src/test/java/shipwrights/genesis/client/lod/PlanetVoxelLodSelectorTest.java
```

**Renamed**

```
mixin/LevelChunkSparseLodMixin.java -> mixin/LevelChunkVoxelDirtyMixin.java
```

**Deleted** — 15 files, listed in the plan document §2.

Net: 41 files, +2458 / −5690 lines.

---

## 11. Migration notes

- **Existing saves.** `<save>/genesis/planet_voxels/` is unchanged in format and
  is read back as-is. Data written by the deleted sparse-LOD service was never
  stored in the world save (it was streamed per session), so nothing is orphaned.
- **Removed packets.** `SparsePlanetLodTilePacket` and
  `PlanetLodVolumeTilePacket` are no longer registered. A client and server must
  run matching versions; a mixed pair fails at channel negotiation rather than
  misbehaving.
- **Distant Horizons.** Players with DH installed keep it; it simply no longer
  feeds Genesis. Players without it now see the full cube Earth, which was not
  previously true.
- **`libs/`.** No jar is required there any more. Any jar dropped in is added to
  the development runtime classpath only.
- **Config.** New keys appear under `PlanetVoxelEngine` (client) and
  `PlanetPreparationQuality` / `PlanetPreparationRadius` (common). Existing keys
  are untouched except that `WorldCubeFoldFullHeight` now ends the fold instead
  of `CurrentPlanetRevealHeight`.
