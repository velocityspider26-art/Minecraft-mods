package shipwrights.genesis.client.lod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.OrbitalSurveyController;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.space.planet.CubeSurfaceProjection;
import shipwrights.genesis.space.planet.FlatToCubeFoldController;
import shipwrights.genesis.space.planet.PlanetRenderDiagnostics;
import shipwrights.genesis.space.voxel.PlanetVoxelAuthority;
import shipwrights.genesis.space.voxel.PlanetVoxelBrick;
import shipwrights.genesis.space.voxel.PlanetVoxelBrickKey;
import shipwrights.genesis.space.voxel.PlanetVoxelGreedyMesher;
import shipwrights.genesis.space.voxel.PlanetVoxelMaterial;
import shipwrights.genesis.space.voxel.PlanetVoxelLodSelector;
import shipwrights.genesis.teleportation.CubeFaceFrame;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Earth renderer. There is no other one.
 *
 * <p>Every pixel of the planet — from a hillside beyond the render distance
 * during take-off to the complete six-face cube seen from orbit — is drawn from
 * the same sparse voxel pyramid, which holds the actual block states of the
 * actual Minecraft world. No painted texture, no height-field shell and no
 * separate planet model participates. That is the whole point: the ground the
 * player launched from <em>is</em> the Earth they see from space.</p>
 *
 * <h2>One buffer, two frames</h2>
 * <p>A brick's geometry is meshed once, in face-local coordinates, and uploaded
 * to an immutable {@link VertexBuffer}. Where that geometry appears is decided
 * entirely by a 4x4 matrix:</p>
 * <ul>
 *   <li>during ascent, {@link CubeSurfaceProjection#foldMatrix} — the exact
 *       affine equivalent of a per-vertex {@code mix(flat, cube, fold)};</li>
 *   <li>in orbit, {@link CubeSurfaceProjection#orbitMatrix}.</li>
 * </ul>
 * <p>So the fold costs one matrix per face per frame instead of a CPU pass over
 * every vertex, the buffers survive the whole flight, and the geometry crossing
 * the dimension boundary is bit-for-bit the same geometry.</p>
 *
 * <h2>Detail</h2>
 * <p>{@link PlanetVoxelLodSelector} refines by projected screen size across the
 * entire viewport, not by distance from the crosshair, and only descends into a
 * brick when every occupied child octant is resident. Missing fine data
 * therefore shows coarser <em>3D</em> terrain — never a hole and never a flat
 * coloured tile.</p>
 */
public final class PlanetVoxelRenderer {
    private static final ResourceLocation EARTH_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final RenderType SOLID_TYPE =
            RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
    private static final RenderType TRANSLUCENT_TYPE =
            RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

    /** Lifts LOD geometry a fraction of a block clear of the vanilla surface it overlaps. */
    private static final double OUTWARD_NUDGE = 0.72;
    private static final int MAX_PENDING_GPU_UPLOADS = 2048;
    private static final int MAX_RESIDENT_SNAPSHOT = 100_000;

    private static final Object GPU_LOCK = new Object();
    private static final LinkedHashMap<GpuKey, GpuEntry> GPU_CACHE =
            new LinkedHashMap<>(1024, 0.75f, true);
    private static final LinkedHashMap<GpuKey, PendingGpuUpload> GPU_UPLOADS =
            new LinkedHashMap<>();
    private static final List<GpuEntry> GPU_DISPOSALS = new ArrayList<>();
    private static long gpuBytes;

    private static long residentGeneration = Long.MIN_VALUE;
    private static List<PlanetVoxelBrick> residentBricks = List.of();

    private static final SelectionCache ASCENT = new SelectionCache();
    private static final SelectionCache ORBIT = new SelectionCache();

    private static volatile boolean disabled;
    private static volatile boolean loggedOverworld;
    private static volatile boolean loggedOrbit;

    private PlanetVoxelRenderer() {
    }

    public static boolean hasVoxelData() {
        return PlanetVoxelClientCache.hasAny(EARTH_ID);
    }

    private static boolean enabled() {
        return !disabled && GenesisClientConfig.isPlanetVoxelRendererEnabled();
    }

    /**
     * Whether the voxel pyramid is carrying the planet on its own.
     *
     * <p>Kept as a diagnostic, and consulted by the celestial renderer so it
     * never draws a second Earth. It is deliberately <em>not</em> a gate on
     * rendering: the voxel path is the only path, and gating it on a coverage
     * ratio is exactly how a blurry painted cube used to stay on screen
     * forever.</p>
     */
    public static boolean hasCompleteCoarseCoverage(CubeNetSurfaceTransform transform) {
        return enabled() && hasVoxelData();
    }

    // ------------------------------------------------------------------
    // Ascent
    // ------------------------------------------------------------------

    /**
     * Draws the world beyond vanilla's chunks while climbing out, folding the
     * five remote faces around the one the player is standing on.
     *
     * @param view          the frame's camera-relative model-view matrix
     * @param camera        render camera position in planet-dimension world space
     * @param fold          0 = flat world, 1 = complete cube
     * @param vanillaRadius vanilla render distance in blocks
     */
    public static void renderOverworld(Matrix4f view, Vec3 camera, Vector3f cameraLook,
                                       CubeSurfaceProjection projection,
                                       CubeNetSurfaceTransform.Face currentFace,
                                       float fold, int vanillaRadius) {
        if (!enabled() || !hasVoxelData()) return;
        Quality quality = Quality.current();
        Projection screen = screenProjection();

        Vector3d cameraWorld = new Vector3d(camera.x, camera.y, camera.z);
        Vector3d look = new Vector3d(cameraLook.x, cameraLook.y, cameraLook.z);

        // Anything the vanilla chunk renderer already draws is skipped outright
        // rather than cross-faded. Overlapping the two would z-fight on exactly
        // the surfaces the player is closest to.
        double nearCull = Math.max(48.0, vanillaRadius * 0.92);

        PlanetVoxelLodSelector.OrbitView selectorView = new PlanetVoxelLodSelector.OrbitView(
                cameraWorld, look, projection.faceHalfSpan(), 1.0, projection.seaLevel(),
                screen.viewportHeight(), screen.aspectRatio(), screen.verticalFovRadians(),
                quality.targetVoxelPixels(), PlanetVoxelBrickKey.MAX_LOD,
                quality.maximumBricks(), false);
        AscentPlacement placement = new AscentPlacement(projection, currentFace, fold);

        List<PlanetVoxelLodSelector.Selection> selected = dropVanillaCovered(
                ASCENT.select(selectorView, placement, screen, quality, fold), nearCull);
        if (selected.isEmpty()) return;

        requestInterest(projection, currentFace, camera, cameraLook, screen, quality);

        double centerDistance = cameraWorld.distance(
                projection.transform().faceCenterX(currentFace),
                projection.seaLevel(),
                projection.transform().faceCenterZ(currentFace));
        double compression = FlatToCubeFoldController.distanceCompression(
                centerDistance, camera.y, vanillaRadius);

        int drawn = draw(selected, key -> {
            Matrix4d model = new Matrix4d()
                    .translation(-compression * cameraWorld.x,
                            -compression * cameraWorld.y,
                            -compression * cameraWorld.z)
                    .scale(compression)
                    .mul(projection.foldMatrix(key.face(), currentFace, fold));
            return applyBrickOrigin(model, projection, key);
        }, view, camera.y);

        if (drawn > 0 && !loggedOverworld) {
            loggedOverworld = true;
            GenesisMod.LOGGER.info("[PLANET-VOXEL] the world's own 3D LOD chunks are extending past "
                    + "the vanilla render distance; {} bricks drawn from a {} MiB client cache",
                    drawn, PlanetVoxelClientCache.usedBytes(EARTH_ID) / (1024L * 1024L));
        }
    }

    // ------------------------------------------------------------------
    // Orbit
    // ------------------------------------------------------------------

    /** Draws the complete cube Earth from space, out of the same brick pyramid. */
    public static void renderOrbit(Matrix4f view, CubeSurfaceProjection projection,
                                   Vector3d planetPosition, Quaterniondc rotation,
                                   double renderedHalfExtent, double worldToRendered,
                                   Vector3d localCamera, Vector3d localLook,
                                   CubeNetSurfaceTransform.Face focusFace) {
        if (!enabled()) return;
        Quality quality = Quality.current();
        Projection screen = screenProjection();

        // Interest is requested in unscaled cube-space units so the server's
        // selector and the client's agree on what "one voxel" subtends.
        PlanetVoxelInterestClient.update(new Vector3d(localCamera).div(worldToRendered), localLook,
                screen.viewportHeight(), screen.aspectRatio(), screen.verticalFovRadians(),
                quality.targetVoxelPixels(), quality.maximumBricks());
        if (!hasVoxelData()) return;

        PlanetVoxelLodSelector.OrbitView selectorView = new PlanetVoxelLodSelector.OrbitView(
                localCamera, localLook, projection.faceHalfSpan(), worldToRendered,
                projection.seaLevel(), screen.viewportHeight(), screen.aspectRatio(),
                screen.verticalFovRadians(), quality.targetVoxelPixels(),
                PlanetVoxelBrickKey.MAX_LOD, quality.maximumBricks(), true);
        List<PlanetVoxelLodSelector.Selection> selected = ORBIT.select(selectorView,
                PlanetVoxelLodSelector.cubePlacement(selectorView), screen, quality, 1.0f);
        if (selected.isEmpty()) return;

        double renderScale = renderedHalfExtent / Math.max(1.0, projection.faceHalfSpan());
        Quaterniond localRotation = new Quaterniond(rotation);
        int drawn = draw(selected, key -> applyBrickOrigin(
                projection.orbitMatrix(key.face(), planetPosition, localRotation, renderScale),
                projection, key), view, Double.MAX_VALUE);

        if (drawn > 0 && !loggedOrbit) {
            loggedOrbit = true;
            GenesisMod.LOGGER.info("[PLANET-VOXEL] the cube Earth in orbit is the world's own voxel "
                    + "terrain; {} bricks around {}", drawn, focusFace);
        }
    }

    /**
     * Drops bricks the vanilla chunk renderer is already drawing.
     *
     * <p>Cross-fading the two representations of the same hillside makes them
     * z-fight at exactly the distance the player is looking hardest. Handing the
     * near field to vanilla outright is both cleaner and cheaper — and the
     * geometry is identical either way, because the bricks hold that hillside's
     * real block states.</p>
     */
    private static List<PlanetVoxelLodSelector.Selection> dropVanillaCovered(
            List<PlanetVoxelLodSelector.Selection> selected, double nearCull) {
        List<PlanetVoxelLodSelector.Selection> kept = new ArrayList<>(selected.size());
        for (PlanetVoxelLodSelector.Selection selection : selected) {
            double radius = selection.brick().key().brickSpan() * 0.88;
            if (selection.distance() + radius < nearCull) continue;
            kept.add(selection);
        }
        return kept;
    }

    /**
     * Shifts a placement matrix onto a specific brick.
     *
     * <p>Vertices are stored relative to their brick, so the brick's own origin
     * in face-local space is folded into the matrix. Composing in double keeps
     * the +/-8192 block face offsets exact right up until the final
     * camera-relative float, where they have already cancelled.</p>
     */
    private static Matrix4f applyBrickOrigin(Matrix4d placement, CubeSurfaceProjection projection,
                                             PlanetVoxelBrickKey key) {
        Vector3d origin = brickOrigin(projection, key);
        return new Matrix4f().set(placement.translate(origin.x, origin.y, origin.z));
    }

    /** A brick's own corner, expressed in the local vector space the matrices consume. */
    private static Vector3d brickOrigin(CubeSurfaceProjection projection, PlanetVoxelBrickKey key) {
        // minY is already a world Y, so this is localVector() with the brick's
        // corner substituted for a terrain sample.
        return projection.localVector(key.minU(), key.minY(), key.minV(), OUTWARD_NUDGE);
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    @FunctionalInterface
    private interface ModelMatrix {
        Matrix4f of(PlanetVoxelBrickKey key);
    }

    private static int draw(List<PlanetVoxelLodSelector.Selection> selected,
                            ModelMatrix models, Matrix4f view, double cameraY) {
        for (PlanetVoxelLodSelector.Selection selection : selected) {
            PlanetVoxelGreedyMesher.Mesh mesh = PlanetVoxelMeshCache.getOrSchedule(
                    EARTH_ID, selection.brick());
            if (mesh != null) queueGpuUpload(selection.brick(), mesh);
        }
        drainGpuDisposals();
        drainGpuUploads();

        List<GpuDraw> draws = new ArrayList<>(selected.size());
        int[] lodCounts = new int[PlanetVoxelBrickKey.MAX_LOD + 1];
        int exact = 0;
        int predicted = 0;
        int fallback = 0;
        synchronized (GPU_LOCK) {
            for (PlanetVoxelLodSelector.Selection selection : selected) {
                PlanetVoxelBrickKey key = selection.brick().key();
                GpuEntry entry = GPU_CACHE.get(new GpuKey(EARTH_ID, key));
                if (entry == null || entry.revision != selection.brick().revision()) {
                    fallback++;
                    continue;
                }
                draws.add(new GpuDraw(entry, key, selection.distance(), models.of(key)));
                lodCounts[key.lod()]++;
                if (entry.authority == PlanetVoxelAuthority.GENERATED_EXACT
                        || entry.authority == PlanetVoxelAuthority.PLAYER_MODIFIED) {
                    exact++;
                } else {
                    predicted++;
                }
            }
        }
        publishDiagnostics(draws.size(), lodCounts, exact, predicted, fallback);
        if (draws.isEmpty()) return 0;

        draws.sort(Comparator.comparingDouble(GpuDraw::distance));
        try {
            drawLayer(SOLID_TYPE, view, draws, false);
            drawLayer(TRANSLUCENT_TYPE, view, draws, true);
            if (PlanetRenderDiagnostics.debugMode() == PlanetRenderDiagnostics.DebugMode.BRICK_BOUNDS
                    || PlanetRenderDiagnostics.debugMode()
                    == PlanetRenderDiagnostics.DebugMode.SEAMS) {
                drawDebugBounds(view, draws);
            }
        } catch (Throwable error) {
            // A renderer failure must be loud. Silently degrading to nothing is
            // how a black planet ends up looking like a content bug.
            disabled = true;
            GenesisMod.LOGGER.error("[PLANET-VOXEL] planet renderer disabled after a draw failure; "
                    + "the orbital Earth will not be drawn until the world is reloaded", error);
            return 0;
        }
        return draws.size();
    }

    private static void drawLayer(RenderType type, Matrix4f view, List<GpuDraw> draws,
                                  boolean translucent) {
        type.setupRenderState();
        // Vanilla fog is tied to the render distance. Terrain eight kilometres
        // away is the entire point of this renderer, so it draws with fog pushed
        // beyond any distance it can reach and restores the world's fog after.
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(1.0E7f);
        RenderSystem.setShaderFogEnd(1.0E7f + 1.0f);
        try {
            ShaderInstance shader = RenderSystem.getShader();
            if (shader == null) return;
            Matrix4f projection = RenderSystem.getProjectionMatrix();
            if (translucent) {
                for (int index = draws.size() - 1; index >= 0; index--) {
                    drawOne(draws.get(index), view, projection, shader, true);
                }
            } else {
                for (GpuDraw draw : draws) {
                    drawOne(draw, view, projection, shader, false);
                }
            }
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
            VertexBuffer.unbind();
            type.clearRenderState();
        }
    }

    private static void drawOne(GpuDraw draw, Matrix4f view, Matrix4f projection,
                                ShaderInstance shader, boolean translucent) {
        VertexBuffer buffer = translucent ? draw.entry.translucent : draw.entry.solid;
        if (buffer == null) return;
        applyDebugTint(draw);
        Matrix4f modelView = new Matrix4f(view).mul(draw.model);
        buffer.bind();
        buffer.drawWithShader(modelView, projection, shader);
    }

    private static void applyDebugTint(GpuDraw draw) {
        switch (PlanetRenderDiagnostics.debugMode()) {
            case LOD -> {
                float hue = (draw.key.lod() % 8) / 8.0f;
                int rgb = Mth.hsvToRgb(hue, 0.85f, 1.0f);
                RenderSystem.setShaderColor(((rgb >> 16) & 0xFF) / 255.0f,
                        ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f, 1.0f);
            }
            case AUTHORITY -> {
                switch (draw.entry.authority) {
                    case PLAYER_MODIFIED -> RenderSystem.setShaderColor(1.0f, 0.35f, 0.35f, 1.0f);
                    case GENERATED_EXACT -> RenderSystem.setShaderColor(0.35f, 1.0f, 0.45f, 1.0f);
                    case STRUCTURE_PREDICTED -> RenderSystem.setShaderColor(1.0f, 0.9f, 0.35f, 1.0f);
                    case TERRAIN_PREDICTED -> RenderSystem.setShaderColor(0.45f, 0.6f, 1.0f, 1.0f);
                    default -> RenderSystem.setShaderColor(0.4f, 0.4f, 0.4f, 1.0f);
                }
            }
            case MISSING -> {
                boolean coarse = draw.key.lod() > 0;
                RenderSystem.setShaderColor(coarse ? 1.0f : 0.4f, coarse ? 0.4f : 1.0f, 0.4f, 1.0f);
            }
            case FOLD -> RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            default -> RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    /** Wireframe brick bounds / face seams. Debug only, off unless explicitly configured. */
    private static void drawDebugBounds(Matrix4f view, List<GpuDraw> draws) {
        boolean seamsOnly = PlanetRenderDiagnostics.debugMode()
                == PlanetRenderDiagnostics.DebugMode.SEAMS;
        RenderType lines = RenderType.lines();
        com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(lines.mode(), lines.format());
        for (GpuDraw draw : draws) {
            if (seamsOnly && !touchesFaceEdge(draw.key)) continue;
            float span = draw.key.brickSpan();
            float hue = (draw.key.lod() % 8) / 8.0f;
            int rgb = Mth.hsvToRgb(hue, 0.9f, 1.0f);
            addBox(builder, new Matrix4f(view).mul(draw.model), span,
                    ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                    (rgb & 0xFF) / 255.0f);
        }
        MeshData mesh = builder.build();
        if (mesh != null) lines.draw(mesh);
    }

    private static boolean touchesFaceEdge(PlanetVoxelBrickKey key) {
        return key.brickU() == 0 || key.brickV() == 0
                || key.brickU() == -1 || key.brickV() == -1;
    }

    private static void addBox(BufferBuilder builder, Matrix4f matrix, float span,
                               float r, float g, float b) {
        float[][] corners = {
                {0, 0, 0}, {span, 0, 0}, {span, 0, span}, {0, 0, span},
                {0, span, 0}, {span, span, 0}, {span, span, span}, {0, span, span}
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (int[] edge : edges) {
            float[] a = corners[edge[0]];
            float[] c = corners[edge[1]];
            float nx = c[0] - a[0];
            float ny = c[1] - a[1];
            float nz = c[2] - a[2];
            float length = Math.max(1.0E-4f, (float) Math.sqrt(nx * nx + ny * ny + nz * nz));
            builder.addVertex(matrix, a[0], a[1], a[2]).setColor(r, g, b, 1.0f)
                    .setNormal(nx / length, ny / length, nz / length);
            builder.addVertex(matrix, c[0], c[1], c[2]).setColor(r, g, b, 1.0f)
                    .setNormal(nx / length, ny / length, nz / length);
        }
    }

    private static void publishDiagnostics(int visible, int[] lodCounts,
                                           int exact, int predicted, int fallback) {
        int pending;
        long bytes;
        synchronized (GPU_LOCK) {
            pending = GPU_UPLOADS.size();
            bytes = gpuBytes;
        }
        PlanetRenderDiagnostics.setFrame(visible, residentBricks.size(), pending,
                PlanetVoxelMeshCache.queuedJobs(),
                PlanetVoxelClientCache.usedBytes(EARTH_ID), bytes,
                predicted, exact, fallback, lodCounts);
    }

    // ------------------------------------------------------------------
    // Resident data and selection caching
    // ------------------------------------------------------------------

    private static List<PlanetVoxelBrick> resident() {
        long generation = PlanetVoxelClientCache.generation();
        if (generation != residentGeneration) {
            residentBricks = PlanetVoxelClientCache.snapshot(EARTH_ID, key -> true,
                    MAX_RESIDENT_SNAPSHOT);
            residentGeneration = generation;
            ASCENT.invalidate();
            ORBIT.invalidate();
        }
        return residentBricks;
    }

    /**
     * Re-runs the LOD selection only when the view has actually moved.
     *
     * <p>Selection walks the whole pyramid; doing it every frame while parked in
     * orbit is pure waste, and doing it never makes detail lag behind the
     * camera. Two frames of staleness is invisible and cheap.</p>
     */
    private static final class SelectionCache {
        private List<PlanetVoxelLodSelector.Selection> selection = List.of();
        private Set<PlanetVoxelBrickKey> previousKeys = Set.of();
        private final Vector3d lastCamera = new Vector3d(Double.NaN);
        private final Vector3d lastLook = new Vector3d(Double.NaN);
        private double lastFov = Double.NaN;
        private double lastFold = Double.NaN;
        private int lastViewportHeight = -1;
        private int lastMaximumBricks = -1;
        private int age = Integer.MAX_VALUE;

        void invalidate() {
            age = Integer.MAX_VALUE;
        }

        List<PlanetVoxelLodSelector.Selection> select(PlanetVoxelLodSelector.OrbitView view,
                                                      PlanetVoxelLodSelector.BrickPlacement placement,
                                                      Projection screen, Quality quality,
                                                      double fold) {
            List<PlanetVoxelBrick> bricks = resident();
            if (bricks.isEmpty()) return List.of();
            boolean moved = selection.isEmpty()
                    || lastCamera.distanceSquared(view.camera()) > 0.25
                    || lastLook.dot(view.look()) < 0.9995
                    || Math.abs(lastFov - screen.verticalFovRadians()) > 0.001
                    || Math.abs(lastFold - fold) > 0.002
                    || lastViewportHeight != screen.viewportHeight()
                    || lastMaximumBricks != quality.maximumBricks();
            if (age != Integer.MAX_VALUE && (!moved || age < 2)) {
                age++;
                return selection;
            }
            selection = PlanetVoxelLodSelector.select(bricks, view, previousKeys, placement);
            previousKeys = selection.stream()
                    .map(entry -> entry.brick().key())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            lastCamera.set(view.camera());
            lastLook.set(view.look());
            lastFov = screen.verticalFovRadians();
            lastFold = fold;
            lastViewportHeight = screen.viewportHeight();
            lastMaximumBricks = quality.maximumBricks();
            age = 0;
            return selection;
        }

        void reset() {
            selection = List.of();
            previousKeys = Set.of();
            lastCamera.set(Double.NaN);
            lastLook.set(Double.NaN);
            lastFov = Double.NaN;
            lastFold = Double.NaN;
            lastViewportHeight = -1;
            lastMaximumBricks = -1;
            age = Integer.MAX_VALUE;
        }
    }

    /** Ascent placement: bricks sit at their folded world positions. */
    private record AscentPlacement(CubeSurfaceProjection projection,
                                   CubeNetSurfaceTransform.Face currentFace,
                                   float fold)
            implements PlanetVoxelLodSelector.BrickPlacement {

        @Override
        public Vector3d center(PlanetVoxelBrickKey key) {
            double span = key.brickSpan();
            Vector3d local = projection.localVector(key.minU() + span * 0.5,
                    key.minY() + span * 0.5, key.minV() + span * 0.5, OUTWARD_NUDGE);
            return projection.foldMatrix(key.face(), currentFace, fold)
                    .transformPosition(local);
        }

        @Override
        public double radius(PlanetVoxelBrickKey key) {
            return key.brickSpan() * 0.88;
        }

        @Override
        public double voxelSize(PlanetVoxelBrickKey key) {
            return key.cellSize();
        }

        @Override
        public Vector3dc outwardNormal(PlanetVoxelBrickKey key) {
            // A partly folded world has no consistent "far side" to cull, and
            // the camera is inside the shell rather than outside it.
            return fold >= 0.999f ? CubeFaceFrame.axes(key.face()).up() : null;
        }
    }

    // ------------------------------------------------------------------
    // GPU cache
    // ------------------------------------------------------------------

    static void invalidateGpu(ResourceLocation planet, PlanetVoxelBrickKey key) {
        synchronized (GPU_LOCK) {
            GpuKey gpuKey = new GpuKey(planet, key);
            GpuEntry removed = GPU_CACHE.remove(gpuKey);
            if (removed != null) {
                gpuBytes -= removed.estimatedBytes;
                GPU_DISPOSALS.add(removed);
            }
            GPU_UPLOADS.remove(gpuKey);
        }
    }

    private static void queueGpuUpload(PlanetVoxelBrick brick, PlanetVoxelGreedyMesher.Mesh mesh) {
        GpuKey key = new GpuKey(EARTH_ID, brick.key());
        synchronized (GPU_LOCK) {
            GpuEntry resident = GPU_CACHE.get(key);
            if (resident != null && resident.revision == brick.revision()) return;
            PendingGpuUpload pending = GPU_UPLOADS.get(key);
            if (pending != null && pending.brick.revision() >= brick.revision()) return;
            GPU_UPLOADS.put(key, new PendingGpuUpload(brick, mesh));
            while (GPU_UPLOADS.size() > MAX_PENDING_GPU_UPLOADS) {
                GpuKey eldest = GPU_UPLOADS.keySet().iterator().next();
                GPU_UPLOADS.remove(eldest);
            }
        }
    }

    private static void drainGpuUploads() {
        RenderSystem.assertOnRenderThread();
        int budget = GenesisClientConfig.getPlanetGpuUploadsPerFrame();
        for (int uploaded = 0; uploaded < budget; uploaded++) {
            GpuKey key;
            PendingGpuUpload pending;
            synchronized (GPU_LOCK) {
                if (GPU_UPLOADS.isEmpty()) break;
                Map.Entry<GpuKey, PendingGpuUpload> first = GPU_UPLOADS.entrySet().iterator().next();
                key = first.getKey();
                pending = first.getValue();
                GPU_UPLOADS.remove(key);
            }
            PlanetVoxelBrick current = PlanetVoxelClientCache.get(key.planet, key.key);
            if (current == null || current.revision() != pending.brick.revision()) continue;
            GpuEntry entry;
            try {
                entry = uploadGpuMesh(pending);
            } catch (Throwable error) {
                GenesisMod.LOGGER.warn("[PLANET-VOXEL] failed to upload GPU mesh {}", key.key, error);
                continue;
            }
            PlanetRenderDiagnostics.onGpuUpload();
            synchronized (GPU_LOCK) {
                GpuEntry previous = GPU_CACHE.put(key, entry);
                if (previous != null) {
                    gpuBytes -= previous.estimatedBytes;
                    GPU_DISPOSALS.add(previous);
                }
                gpuBytes += entry.estimatedBytes;
                trimGpuCache();
            }
        }
    }

    private static GpuEntry uploadGpuMesh(PendingGpuUpload pending) {
        int vertexBytes = DefaultVertexFormat.NEW_ENTITY.getVertexSize();
        int estimated = Math.max(4096, pending.mesh.quadCount() * 4 * vertexBytes);
        ByteBufferBuilder solidMemory = new ByteBufferBuilder(estimated);
        ByteBufferBuilder translucentMemory = new ByteBufferBuilder(Math.max(4096, estimated / 4));
        try {
            BufferBuilder solid = new BufferBuilder(
                    solidMemory, SOLID_TYPE.mode(), SOLID_TYPE.format());
            BufferBuilder translucent = new BufferBuilder(
                    translucentMemory, TRANSLUCENT_TYPE.mode(), TRANSLUCENT_TYPE.format());
            emit(solid, translucent, pending.brick.key(), pending.mesh);
            VertexBuffer solidBuffer = uploadBuffer(solid);
            VertexBuffer translucentBuffer = uploadBuffer(translucent);
            long bytes = (long) pending.mesh.quadCount() * 4L * vertexBytes;
            return new GpuEntry(pending.brick.revision(), solidBuffer, translucentBuffer, bytes,
                    dominantAuthority(pending.brick));
        } finally {
            solidMemory.close();
            translucentMemory.close();
        }
    }

    private static PlanetVoxelAuthority dominantAuthority(PlanetVoxelBrick brick) {
        PlanetVoxelAuthority best = PlanetVoxelAuthority.EMPTY;
        for (int y = 0; y < PlanetVoxelBrick.EDGE; y += 4) {
            for (int z = 0; z < PlanetVoxelBrick.EDGE; z += 4) {
                for (int x = 0; x < PlanetVoxelBrick.EDGE; x += 4) {
                    PlanetVoxelAuthority authority = brick.authority(x, y, z);
                    if (authority.ordinal() > best.ordinal()) best = authority;
                }
            }
        }
        return best;
    }

    private static VertexBuffer uploadBuffer(BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh == null) return null;
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            buffer.bind();
            buffer.upload(mesh);
            return buffer;
        } catch (RuntimeException | Error error) {
            buffer.close();
            mesh.close();
            throw error;
        } finally {
            VertexBuffer.unbind();
        }
    }

    private static void trimGpuCache() {
        long budget = GenesisClientConfig.getPlanetGpuCacheBytes();
        var iterator = GPU_CACHE.entrySet().iterator();
        while (gpuBytes > budget && iterator.hasNext()) {
            GpuEntry removed = iterator.next().getValue();
            iterator.remove();
            gpuBytes -= removed.estimatedBytes;
            GPU_DISPOSALS.add(removed);
        }
    }

    private static void drainGpuDisposals() {
        RenderSystem.assertOnRenderThread();
        List<GpuEntry> disposals;
        synchronized (GPU_LOCK) {
            if (GPU_DISPOSALS.isEmpty()) return;
            disposals = new ArrayList<>(GPU_DISPOSALS);
            GPU_DISPOSALS.clear();
        }
        for (GpuEntry entry : disposals) entry.close();
    }

    public static void reset() {
        disabled = false;
        loggedOverworld = false;
        loggedOrbit = false;
        residentGeneration = Long.MIN_VALUE;
        residentBricks = List.of();
        ASCENT.reset();
        ORBIT.reset();
        PlanetVoxelInterestClient.reset();
        PlanetVoxelMeshCache.clear();
        PlanetRenderDiagnostics.reset();
        synchronized (GPU_LOCK) {
            GPU_DISPOSALS.addAll(GPU_CACHE.values());
            GPU_CACHE.clear();
            GPU_UPLOADS.clear();
            gpuBytes = 0L;
        }
        if (RenderSystem.isOnRenderThread()) {
            drainGpuDisposals();
        } else {
            RenderSystem.recordRenderCall(PlanetVoxelRenderer::drainGpuDisposals);
        }
    }

    // ------------------------------------------------------------------
    // Geometry emission (brick-local, uploaded once)
    // ------------------------------------------------------------------

    private static void emit(VertexConsumer solid, VertexConsumer translucent,
                             PlanetVoxelBrickKey key, PlanetVoxelGreedyMesher.Mesh mesh) {
        for (PlanetVoxelGreedyMesher.Quad quad : mesh.solid()) {
            emitQuad(solid, key, quad);
        }
        for (PlanetVoxelGreedyMesher.Quad quad : mesh.translucent()) {
            emitQuad(translucent, key, quad);
        }
    }

    private static void emitQuad(VertexConsumer buffer, PlanetVoxelBrickKey key,
                                 PlanetVoxelGreedyMesher.Quad quad) {
        int flags = PlanetVoxelMaterial.flags(quad.material());
        // Structures keep one block per texture repeat so a village roof still
        // reads as shingles; open natural terrain may stride to halve the
        // vertex count without any visible change at that distance.
        int step = key.lod() == 0 && (flags & PlanetVoxelMaterial.FLAG_STRUCTURE) != 0 ? 1
                : key.lod() == 0 ? 2 : 1;
        for (int b = quad.b0(); b < quad.b1(); b += step) {
            int b1 = Math.min(quad.b1(), b + step);
            for (int a = quad.a0(); a < quad.a1(); a += step) {
                int a1 = Math.min(quad.a1(), a + step);
                FacePoints local = facePoints(key, quad.direction(), quad.plane(), a, b, a1, b1);
                Vector3f normal = normalOf(quad.direction());
                writeQuad(buffer, local, quad.material(), textureDirection(quad.direction()),
                        quad.coverage(), normal);
            }
        }
    }

    private static FacePoints facePoints(PlanetVoxelBrickKey key,
                                         PlanetVoxelGreedyMesher.FaceDirection direction,
                                         int plane, int a0, int b0, int a1, int b1) {
        // Brick-local block offsets only: the brick's own origin lives in the
        // model matrix so one buffer works for the flat, folded and orbital
        // placements alike.
        double cell = key.cellSize();
        double fixed = plane * cell;
        double aa0 = a0 * cell, aa1 = a1 * cell;
        double bb0 = b0 * cell, bb1 = b1 * cell;
        return switch (direction) {
            case UP -> new FacePoints(
                    new LocalPoint(aa0, fixed, bb1), new LocalPoint(aa1, fixed, bb1),
                    new LocalPoint(aa1, fixed, bb0), new LocalPoint(aa0, fixed, bb0));
            case DOWN -> new FacePoints(
                    new LocalPoint(aa0, fixed, bb0), new LocalPoint(aa1, fixed, bb0),
                    new LocalPoint(aa1, fixed, bb1), new LocalPoint(aa0, fixed, bb1));
            case NORTH -> new FacePoints(
                    new LocalPoint(aa1, bb0, fixed), new LocalPoint(aa0, bb0, fixed),
                    new LocalPoint(aa0, bb1, fixed), new LocalPoint(aa1, bb1, fixed));
            case SOUTH -> new FacePoints(
                    new LocalPoint(aa0, bb0, fixed), new LocalPoint(aa1, bb0, fixed),
                    new LocalPoint(aa1, bb1, fixed), new LocalPoint(aa0, bb1, fixed));
            case WEST -> new FacePoints(
                    new LocalPoint(fixed, bb0, aa0), new LocalPoint(fixed, bb0, aa1),
                    new LocalPoint(fixed, bb1, aa1), new LocalPoint(fixed, bb1, aa0));
            case EAST -> new FacePoints(
                    new LocalPoint(fixed, bb0, aa1), new LocalPoint(fixed, bb0, aa0),
                    new LocalPoint(fixed, bb1, aa0), new LocalPoint(fixed, bb1, aa1));
        };
    }

    private static void writeQuad(VertexConsumer buffer, FacePoints points, long material,
                                  Direction direction, int coverage, Vector3f normal) {
        int stateId = PlanetVoxelMaterial.blockStateId(material);
        PlanetVolumeTextureCache.Sprite resolved = PlanetVolumeTextureCache.sprite(stateId, direction);
        TextureAtlasSprite sprite = resolved.texture();
        int flags = PlanetVoxelMaterial.flags(material);
        float shade = switch (direction) {
            case UP -> 1.0f;
            case DOWN -> 0.58f;
            case NORTH -> 0.74f;
            case SOUTH -> 0.90f;
            default -> 0.82f;
        };
        if ((flags & PlanetVoxelMaterial.FLAG_EMISSIVE) != 0) shade = 1.0f;
        int tint = resolved.tinted() ? PlanetVoxelMaterial.rgb(material) : 0xFFFFFF;
        int r = Mth.clamp((int) (((tint >> 16) & 0xFF) * shade), 0, 255);
        int g = Mth.clamp((int) (((tint >> 8) & 0xFF) * shade), 0, 255);
        int b = Mth.clamp((int) ((tint & 0xFF) * shade), 0, 255);
        // Coverage is the fraction of a reduced voxel that was really solid; it
        // darkens rather than fades, so a half-full mip cell reads as thinner
        // geometry instead of ghosting.
        float coverageFactor = 0.55f + 0.45f * (coverage / 255.0f);
        r = Mth.clamp((int) (r * coverageFactor), 0, 255);
        g = Mth.clamp((int) (g * coverageFactor), 0, 255);
        b = Mth.clamp((int) (b * coverageFactor), 0, 255);
        int light = (flags & PlanetVoxelMaterial.FLAG_EMISSIVE) != 0
                ? LightTexture.FULL_BRIGHT
                : LightTexture.pack(PlanetVoxelMaterial.blockLight(material),
                Math.max(10, PlanetVoxelMaterial.skyLight(material)));
        writeVertex(buffer, points.p0(), sprite.getU0(), sprite.getV1(), r, g, b, light, normal);
        writeVertex(buffer, points.p1(), sprite.getU1(), sprite.getV1(), r, g, b, light, normal);
        writeVertex(buffer, points.p2(), sprite.getU1(), sprite.getV0(), r, g, b, light, normal);
        writeVertex(buffer, points.p3(), sprite.getU0(), sprite.getV0(), r, g, b, light, normal);
    }

    private static void writeVertex(VertexConsumer buffer, LocalPoint point,
                                    float u, float v, int r, int g, int b,
                                    int light, Vector3f normal) {
        buffer.addVertex((float) point.x(), (float) point.y(), (float) point.z())
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(normal.x, normal.y, normal.z);
    }

    private static Vector3f normalOf(PlanetVoxelGreedyMesher.FaceDirection direction) {
        return switch (direction) {
            case UP -> new Vector3f(0, 1, 0);
            case DOWN -> new Vector3f(0, -1, 0);
            case NORTH -> new Vector3f(0, 0, -1);
            case SOUTH -> new Vector3f(0, 0, 1);
            case WEST -> new Vector3f(-1, 0, 0);
            case EAST -> new Vector3f(1, 0, 0);
        };
    }

    private static Direction textureDirection(PlanetVoxelGreedyMesher.FaceDirection direction) {
        return switch (direction) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
        };
    }

    // ------------------------------------------------------------------
    // View helpers
    // ------------------------------------------------------------------

    private static void requestInterest(CubeSurfaceProjection projection,
                                        CubeNetSurfaceTransform.Face currentFace,
                                        Vec3 camera, Vector3f cameraLook,
                                        Projection screen, Quality quality) {
        Vector3d localCamera = projection.faceLocalToCubeAtHeight(currentFace,
                camera.x - projection.transform().faceCenterX(currentFace),
                camera.z - projection.transform().faceCenterZ(currentFace),
                camera.y);
        Vector3d localLook = new Vector3d(cameraLook.x, cameraLook.y, cameraLook.z)
                .rotate(projection.faceOrientation(currentFace));
        PlanetVoxelInterestClient.update(localCamera, localLook,
                screen.viewportHeight(), screen.aspectRatio(), screen.verticalFovRadians(),
                quality.targetVoxelPixels(), quality.maximumBricks());
    }

    private static Projection screenProjection() {
        Minecraft minecraft = Minecraft.getInstance();
        int width = Math.max(1, minecraft.getWindow().getWidth());
        int height = Math.max(1, minecraft.getWindow().getHeight());
        double projectionScale = Math.abs(RenderSystem.getProjectionMatrix().m11());
        double verticalFov = projectionScale > 1.0E-6
                ? 2.0 * Math.atan(1.0 / projectionScale)
                : Math.toRadians(70.0);
        return new Projection(height, (double) width / height, verticalFov);
    }

    /**
     * Screen-space refinement targets.
     *
     * <p>{@code targetVoxelPixels} is the pixel size a voxel is allowed to reach
     * before its brick is refined. Roughly one voxel per pixel at high quality
     * means a 16-block brick refines once it covers about 16 px — the same rule
     * across the whole viewport, not just where the player is aiming.</p>
     */
    private record Quality(int maximumBricks, double targetVoxelPixels) {
        static Quality current() {
            boolean survey = OrbitalSurveyController.isActive();
            int level = GenesisClientConfig.getWorldLodQuality();
            int bricks = survey ? 6144 : level >= 3 ? 4096 : level == 2 ? 2300 : 1200;
            double pixels = survey ? 0.72 : level >= 3 ? 1.10 : level == 2 ? 1.75 : 2.75;
            return new Quality(bricks, pixels);
        }
    }

    private record Projection(int viewportHeight, double aspectRatio, double verticalFovRadians) {
    }

    private record GpuKey(ResourceLocation planet, PlanetVoxelBrickKey key) {
    }

    private record PendingGpuUpload(PlanetVoxelBrick brick, PlanetVoxelGreedyMesher.Mesh mesh) {
    }

    private record GpuDraw(GpuEntry entry, PlanetVoxelBrickKey key,
                           double distance, Matrix4f model) {
    }

    private record LocalPoint(double x, double y, double z) {
    }

    private record FacePoints(LocalPoint p0, LocalPoint p1, LocalPoint p2, LocalPoint p3) {
    }

    private static final class GpuEntry {
        private final long revision;
        private final VertexBuffer solid;
        private final VertexBuffer translucent;
        private final long estimatedBytes;
        private final PlanetVoxelAuthority authority;
        private boolean closed;

        private GpuEntry(long revision, VertexBuffer solid, VertexBuffer translucent,
                         long estimatedBytes, PlanetVoxelAuthority authority) {
            this.revision = revision;
            this.solid = solid;
            this.translucent = translucent;
            this.estimatedBytes = estimatedBytes;
            this.authority = authority;
        }

        private void close() {
            if (closed) return;
            closed = true;
            if (solid != null) solid.close();
            if (translucent != null) translucent.close();
        }
    }
}
