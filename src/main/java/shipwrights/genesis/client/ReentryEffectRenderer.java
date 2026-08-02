package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cassini-style atmospheric reentry plasma for Sable ships descending fast
 * through a planet's upper atmosphere. No particles.
 *
 * The plasma is skinned onto the ship's ACTUAL exposed block faces
 * ({@link #renderHullSkin}) so it takes the hull's shape like a compression
 * shirt: windward faces (into the airflow) blaze white-hot and seed long
 * magenta-blue streamers that flow behind the craft. Ships too large to
 * skin per frame fall back to a smooth ellipsoidal envelope built from the
 * bounding box. Colours follow the white-yellow → orange → magenta →
 * violet-blue gradient. A second, deliberately softer copy is written to
 * Veil's bloom target so the hull stays readable inside the glow.
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public final class ReentryEffectRenderer {
    /** Reentry glow exists between these altitudes (full by REENTRY_FULL_Y). */
    private static final double REENTRY_MIN_Y = 260.0;
    private static final double LOW_ALTITUDE_FADE_END_Y = 340.0;
    /**
     * Gameplay-scaled atmospheric-entry thresholds. One block is treated as a
     * metre, so the sheath ignites at about 16 m/s and is fully developed near
     * 33 m/s. Separate on/off values prevent threshold chatter.
     */
    private static final double ENTRY_SPEED_ON = 0.80;
    private static final double ENTRY_SPEED_OFF = 0.62;
    private static final double ENTRY_SPEED_FULL = 1.65;
    private static final double ENTRY_DESCENT_ON = 0.20;
    private static final double ENTRY_DESCENT_OFF = 0.12;
    private static final double ENTRY_DESCENT_FULL = 0.85;
    private static final double VELOCITY_SMOOTHING = 0.13;
    private static final double REVERSAL_SMOOTHING = 0.045;
    private static final double INTENSITY_RISE = 0.14;
    private static final double INTENSITY_FALL = 0.08;
    private static final double TICK_SECONDS = 0.05;

    /** Only faces whose outward normal aligns with the airflow past this get the burn. */
    private static final double LEADING_FACE_THRESHOLD = 0.18;
    /** Above this exposed-face count we fall back to the smooth envelope. */
    private static final int MAX_HULL_FACES = 2600;
    /** Tiny offset that prevents z-fighting without visibly detaching plasma. */
    private static final double FACE_EPSILON = 0.005;
    private static final double TEXTURE_PIXEL = 1.0 / 16.0;
    /** Small outer expansion keeps the merged sheath just beyond the hull silhouette. */
    private static final double FACE_PATCH_OVERHANG = 1.0 / 128.0;
    /** How often (ms) a ship's cached hull silhouette is rebuilt. */
    private static final long HULL_CACHE_MS = 1500;
    /** Shared vertex count retained by the large-ship envelope fallback. */
    private static final int WAKE_RING_SEGMENTS = 32;
    /** Longitudinal subdivisions in each broad boundary ribbon. */
    private static final int WAKE_RIBBON_STEPS = 10;
    /** Exact block edge used to join adjacent windward faces into one outline. */
    private static final double WAKE_HALF_SIZE = 0.5;
    private static final double EDGE_KEY_SCALE = 4096.0;
    private static final double SHIELD_TRACE_DISTANCE = 32.0;
    private static final double SHIELD_TRACE_STEP = 0.25;

    /** One exposed hull face in Sable plot coordinates plus its outward normal. */
    private record HullFace(Vector3d localCenter, Vector3d localNormal,
                            long blockKey, Direction direction) {
    }

    private record HullCache(List<HullFace> faces, long builtAt) {
    }

    private record FaceAxes(Vector3d axisA, Vector3d axisB) {
    }

    private record FacePlane(Direction direction, int plane) {
    }

    private record FaceCell(int a, int b) implements Comparable<FaceCell> {
        @Override
        public int compareTo(FaceCell other) {
            int bResult = Integer.compare(b, other.b);
            return bResult != 0 ? bResult : Integer.compare(a, other.a);
        }
    }

    private record SurfacePatch(Direction direction, int plane, int minA, int minB,
                                int sizeA, int sizeB) {
    }

    private record ActiveReentry(SubLevel ship, Vector3d position, Quaterniond orientation,
                                 Vector3d rotationPoint, Vector3d scale, Vector3d velocity,
                                 Vector3d acceleration, Vector3d angularVelocity,
                                 float intensity) {
    }

    private record FlowState(long tick, Vector3d rawVelocity, Vector3d velocity,
                             Vector3d acceleration, Vector3d angularVelocity,
                             float intensity, boolean reentryActive) {
    }

    private record Point2(double u, double v) {
    }

    private record QuantizedPoint(long x, long y, long z) implements Comparable<QuantizedPoint> {
        @Override
        public int compareTo(QuantizedPoint other) {
            int xResult = Long.compare(x, other.x);
            if (xResult != 0) {
                return xResult;
            }
            int yResult = Long.compare(y, other.y);
            return yResult != 0 ? yResult : Long.compare(z, other.z);
        }
    }

    private record EdgeKey(QuantizedPoint first, QuantizedPoint second) {
    }

    private record WakeEdge(Vector3d first, Vector3d second, long seed) {
    }

    private record WakeFrame(Vector3d frontCenter, Vector3d axisU, Vector3d axisV,
                             List<Vector3d> perimeter, double radius) {
    }

    private enum PlasmaPass {
        MAIN,
        BLOOM
    }

    private static final Map<UUID, HullCache> HULL_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, FlowState> FLOW_STATES = new ConcurrentHashMap<>();

    private ReentryEffectRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null
                || GenesisMod.isSpaceDimension(level)
                || GenesisMod.isSubSpaceDimension(level)
                || GenesisMod.getCelestialForLevel(level) == null) {
            return; // reentry only happens in planet dimensions
        }

        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 cameraPos = event.getCamera().getPosition();
        Matrix4f matrix = event.getPoseStack().last().pose();
        float time = (level.getGameTime() + partialTick) * 0.05f;
        long gameTick = level.getGameTime();

        List<ActiveReentry> active = new ArrayList<>();

        for (SubLevel ship : container.getAllSubLevels()) {
            Vector3dc now = ship.logicalPose().position();
            Vector3dc last = ship.lastPose().position();
            FlowState flow = flowState(ship, gameTick, now, last);
            float intensity = flow.intensity();
            if (intensity <= 0.02f) {
                continue;
            }

            var renderPose = ship instanceof ClientSubLevel clientShip
                    ? clientShip.renderPose(partialTick)
                    : ship.logicalPose();
            Vector3d position = new Vector3d(renderPose.position())
                    .sub(cameraPos.x, cameraPos.y, cameraPos.z);
            active.add(new ActiveReentry(ship, position,
                    new Quaterniond(renderPose.orientation()),
                    new Vector3d(renderPose.rotationPoint()),
                    new Vector3d(renderPose.scale()),
                    new Vector3d(flow.velocity()),
                    new Vector3d(flow.acceleration()), new Vector3d(flow.angularVelocity()), intensity));
        }

        if (active.isEmpty()) {
            return;
        }

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder main = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (ActiveReentry reentry : active) {
            renderShipPlasma(main, matrix, reentry.ship(), reentry.position(), reentry.orientation(),
                    reentry.rotationPoint(), reentry.scale(), reentry.velocity(),
                    reentry.acceleration(), reentry.angularVelocity(), reentry.intensity(), time, PlasmaPass.MAIN);
        }
        drawTranslucent(main);

        BufferBuilder bloom = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (ActiveReentry reentry : active) {
            renderShipPlasma(bloom, matrix, reentry.ship(), reentry.position(), reentry.orientation(),
                    reentry.rotationPoint(), reentry.scale(), reentry.velocity(),
                    reentry.acceleration(), reentry.angularVelocity(), reentry.intensity(), time, PlasmaPass.BLOOM);
        }
        drawBloom(bloom);
    }

    private static boolean reentryActive(double y, double speed, double descentSpeed, boolean wasActive) {
        double ceiling = GenesisCommonConfig.getAtmosphereEntryHeight() + 200.0;
        if (y < REENTRY_MIN_Y || y > ceiling) {
            return false;
        }

        double speedThreshold = wasActive ? ENTRY_SPEED_OFF : ENTRY_SPEED_ON;
        double descentThreshold = wasActive ? ENTRY_DESCENT_OFF : ENTRY_DESCENT_ON;
        return speed >= speedThreshold && descentSpeed >= descentThreshold;
    }

    private static float targetIntensity(double y, double speed, double descentSpeed, boolean active) {
        double ceiling = GenesisCommonConfig.getAtmosphereEntryHeight() + 200.0;
        if (!active || y < REENTRY_MIN_Y || y > ceiling) {
            return 0.0f;
        }

        double atmosphericDepth = Mth.clamp((ceiling - y) / (ceiling - REENTRY_MIN_Y), 0.0, 1.0);
        // Exponential density gives a visible but restrained onset at the top
        // of the atmosphere, then rapidly builds the continuous Cassini sheath.
        double density = 1.0 - Math.exp(-18.0 * atmosphericDepth);
        double speedFactor = smoothstep(ENTRY_SPEED_OFF, ENTRY_SPEED_FULL, speed);
        double descentFactor = smoothstep(ENTRY_DESCENT_OFF, ENTRY_DESCENT_FULL, descentSpeed);
        double heatFlux = Math.pow(density, 0.32)
                * Math.pow(speedFactor, 1.7)
                * (0.46 + 0.54 * descentFactor);
        double lowAltitudeFade = smoothstep(REENTRY_MIN_Y, LOW_ALTITUDE_FADE_END_Y, y);
        return (float) Mth.clamp(heatFlux * lowAltitudeFade * 1.9, 0.0, 1.0);
    }

    /**
     * Samples Sable's rigid-body velocity in world space and smooths it across
     * ticks. The wake follows this inertial state, never the craft's local up
     * axis. Acceleration bends the historical trail; angular velocity only
     * contributes near-body shear and decays downstream.
     */
    private static FlowState flowState(SubLevel ship, long tick, Vector3dc now, Vector3dc last) {
        UUID id = ship.getUniqueId();
        FlowState previous = FLOW_STATES.get(id);
        if (previous != null && previous.tick() == tick) {
            return previous;
        }

        Vector3d rawVelocity = new Vector3d(now).sub(last);
        Vector3d rawAngularVelocity = angularVelocity(
                ship.lastPose().orientation(), ship.logicalPose().orientation());

        if (previous == null || tick - previous.tick() > 5) {
            double initialSpeed = rawVelocity.length();
            double initialDescent = Math.max(0.0, -rawVelocity.y);
            boolean active = reentryActive(now.y(), initialSpeed, initialDescent, false);
            FlowState initial = new FlowState(tick, new Vector3d(rawVelocity), new Vector3d(rawVelocity),
                    new Vector3d(), new Vector3d(rawAngularVelocity), 0.0f, active);
            FLOW_STATES.put(id, initial);
            return initial;
        }

        double elapsedTicks = Math.max(1.0, tick - previous.tick());
        Vector3d rawAcceleration = new Vector3d(rawVelocity).sub(previous.rawVelocity()).div(elapsedTicks);
        double directionDot = normalizedDot(previous.velocity(), rawVelocity);
        double velocityBlend = directionDot < 0.0 ? REVERSAL_SMOOTHING : VELOCITY_SMOOTHING;
        Vector3d velocity = new Vector3d(previous.velocity()).lerp(rawVelocity, velocityBlend);
        Vector3d acceleration = new Vector3d(previous.acceleration()).lerp(rawAcceleration, 0.08);
        Vector3d angularVelocity = new Vector3d(previous.angularVelocity()).lerp(rawAngularVelocity, 0.16);
        clampLength(acceleration, 0.06);
        clampLength(angularVelocity, 4.0);

        double speed = velocity.length();
        double descentSpeed = Math.max(0.0, -velocity.y);
        boolean active = reentryActive(now.y(), speed, descentSpeed, previous.reentryActive());
        float target = targetIntensity(now.y(), speed, descentSpeed, active);
        double intensityBlend = target > previous.intensity() ? INTENSITY_RISE : INTENSITY_FALL;
        float intensity = (float) Mth.lerp(intensityBlend, previous.intensity(), target);
        if (!active && intensity < 0.005f) {
            intensity = 0.0f;
        }

        FlowState state = new FlowState(tick, new Vector3d(rawVelocity), velocity, acceleration,
                angularVelocity, intensity, active);
        FLOW_STATES.put(id, state);
        if (FLOW_STATES.size() > 64) {
            FLOW_STATES.entrySet().removeIf(entry -> tick - entry.getValue().tick() > 200);
        }
        return state;
    }

    // --- Hull-conforming plasma skin (the "compression shirt") ---

    /**
     * Skins glowing plasma onto the ship's actual exposed block faces so the
     * sheath takes the hull's shape. Windward faces (pointing into the airflow)
     * blaze white-hot and seed the cooling wake. Returns false (envelope
     * fallback) if the ship's
     * blocks can't be read or it has too many faces to skin per frame.
     */
    private static boolean renderHullSkin(BufferBuilder builder, Matrix4f matrix, SubLevel ship,
                                          Vector3d position, Quaterniondc orientation,
                                          Vector3dc rotationPoint, Vector3dc scale,
                                          Vector3d velocity, Vector3d acceleration,
                                          Vector3d angularVelocity, float intensity, float time,
                                          PlasmaPass pass) {
        List<HullFace> faces = hullFaces(ship);
        if (faces == null || faces.isEmpty()) {
            return false;
        }

        Vector3d direction = new Vector3d(velocity);
        if (direction.lengthSquared() < 1.0E-8) {
            return false;
        }
        direction.normalize();
        // Airflow direction in the ship's LOCAL frame (so we can classify faces).
        Vector3d localFlow = new Quaterniond(orientation).conjugate().transform(new Vector3d(direction));
        List<Vector3d> silhouetteSamples = new ArrayList<>();
        double leadingProjection = Double.NEGATIVE_INFINITY;
        double trailingProjection = Double.POSITIVE_INFINITY;

        // Mark each block that owns a directly windward face. Its exposed side
        // faces receive a weaker wrap layer so the compression plasma engulfs
        // the whole leading block instead of ending at its front-face edges.
        Set<Long> hullBlocks = new HashSet<>();
        for (HullFace face : faces) {
            hullBlocks.add(face.blockKey());
        }
        Map<Long, Boolean> upstreamExposure = new HashMap<>();
        Set<Long> windwardBlocks = new HashSet<>();
        for (HullFace face : faces) {
            Vector3d worldCenter = transformHullPosition(
                    face.localCenter(), rotationPoint, scale, orientation, position);
            double projection = worldCenter.dot(direction);
            leadingProjection = Math.max(leadingProjection, projection);
            trailingProjection = Math.min(trailingProjection, projection);
            if (face.localNormal().dot(localFlow) >= LEADING_FACE_THRESHOLD
                    && upstreamExposure.computeIfAbsent(face.blockKey(),
                    key -> isUpstreamExposed(key, hullBlocks, localFlow))) {
                windwardBlocks.add(face.blockKey());
            }
        }

        List<HullFace> heatedFaces = new ArrayList<>();
        for (HullFace face : faces) {
            double windward = face.localNormal().dot(localFlow); // +1 facing flow, -1 trailing
            // The bow face is white-hot; exposed faces on the same leading
            // block carry the weaker boundary layer around its sides.
            boolean directlyWindward = windward >= LEADING_FACE_THRESHOLD
                    && windwardBlocks.contains(face.blockKey());
            boolean wrappedSide = windwardBlocks.contains(face.blockKey());
            if (!directlyWindward && !wrappedSide) {
                continue;
            }
            heatedFaces.add(face);

            if (directlyWindward) {
                Vector3d worldCenter = transformHullPosition(
                        face.localCenter(), rotationPoint, scale, orientation, position);
                appendFaceCorners(silhouetteSamples, worldCenter, orientation, scale, face.localNormal());
            }
        }

        renderSurfacePatches(builder, matrix, heatedFaces, position, orientation,
                rotationPoint, scale, localFlow, intensity, time, pass);

        double hullDepth = Math.max(1.0, leadingProjection - trailingProjection);
        renderConnectedWake(builder, matrix, ship, direction, velocity, acceleration, angularVelocity,
                intensity, time, silhouetteSamples, leadingProjection, hullDepth, pass);
        return true;
    }

    private static Vector3d transformHullPosition(Vector3dc localPosition, Vector3dc rotationPoint,
                                                   Vector3dc scale, Quaterniondc orientation,
                                                   Vector3d renderPosition) {
        Vector3d transformed = new Vector3d(localPosition).sub(rotationPoint).mul(scale);
        orientation.transform(transformed, transformed);
        return transformed.add(renderPosition);
    }

    /**
     * Traces into the oncoming flow through the actual ship blocks. A block is
     * heated only when no other hull block sits upstream of its centre, so a
     * heat shield casts a real aerodynamic shadow over seats and equipment.
     */
    private static boolean isUpstreamExposed(long blockKey, Set<Long> hullBlocks, Vector3d localFlow) {
        BlockPos block = BlockPos.of(blockKey);
        double centerX = block.getX() + 0.5;
        double centerY = block.getY() + 0.5;
        double centerZ = block.getZ() + 0.5;

        for (double distance = 0.55; distance <= SHIELD_TRACE_DISTANCE; distance += SHIELD_TRACE_STEP) {
            int x = Mth.floor(centerX + localFlow.x * distance);
            int y = Mth.floor(centerY + localFlow.y * distance);
            int z = Mth.floor(centerZ + localFlow.z * distance);
            long sampleKey = BlockPos.asLong(x, y, z);
            if (sampleKey != blockKey && hullBlocks.contains(sampleKey)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Greedy-mesh adjacent hot block faces into continuous coplanar patches.
     * One patch is blended once, so block-edge shading and overlapping alpha
     * can no longer produce bright or dark seams inside the plasma sheath.
     */
    private static void renderSurfacePatches(BufferBuilder builder, Matrix4f matrix,
                                             List<HullFace> heatedFaces, Vector3d position,
                                             Quaterniondc orientation, Vector3dc rotationPoint,
                                             Vector3dc scale, Vector3d localFlow, float intensity,
                                             float time, PlasmaPass pass) {
        if (heatedFaces.isEmpty()) {
            return;
        }

        double coverage = smoothstep(0.02, 0.58, intensity);
        double pulse = 0.99 + 0.01 * Math.sin(time * 2.8);
        for (SurfacePatch patch : surfacePatches(heatedFaces)) {
            Vector3d localNormal = new Vector3d(
                    patch.direction().getStepX(),
                    patch.direction().getStepY(),
                    patch.direction().getStepZ());
            FaceAxes axes = faceAxes(localNormal);
            Vector3d localCenter = new Vector3d(localNormal).mul(patch.plane())
                    .fma(patch.minA() + patch.sizeA() * 0.5, axes.axisA())
                    .fma(patch.minB() + patch.sizeB() * 0.5, axes.axisB());
            Vector3d worldCenter = transformHullPosition(
                    localCenter, rotationPoint, scale, orientation, position);
            Vector3d worldNormal = orientation.transform(new Vector3d(localNormal), new Vector3d()).normalize();

            double windward = localNormal.dot(localFlow);
            double heat = windward >= LEADING_FACE_THRESHOLD
                    ? Mth.clamp((windward - LEADING_FACE_THRESHOLD)
                    / (1.0 - LEADING_FACE_THRESHOLD), 0.0, 1.0)
                    : 0.32 + 0.22 * Mth.clamp(windward + 1.0, 0.0, 1.0);
            float alpha = (float) Mth.clamp(
                    coverage * (0.985 + 0.015 * heat) * pulse, 0.0, 1.0);
            int[] color = gradientAt(0.02 + (1.0 - heat) * 0.18);

            emitSurfacePatch(builder, matrix, worldCenter, worldNormal, orientation, scale,
                    localNormal,
                    patch.sizeA() * 0.5 + FACE_PATCH_OVERHANG,
                    patch.sizeB() * 0.5 + FACE_PATCH_OVERHANG,
                    color, alpha, pass);
        }
    }

    private static List<SurfacePatch> surfacePatches(List<HullFace> faces) {
        Map<FacePlane, Set<FaceCell>> planes = new LinkedHashMap<>();
        for (HullFace face : faces) {
            FaceAxes axes = faceAxes(face.localNormal());
            int plane = (int) Math.round(face.localCenter().dot(face.localNormal()));
            int a = Mth.floor(face.localCenter().dot(axes.axisA()));
            int b = Mth.floor(face.localCenter().dot(axes.axisB()));
            planes.computeIfAbsent(new FacePlane(face.direction(), plane), key -> new HashSet<>())
                    .add(new FaceCell(a, b));
        }

        List<SurfacePatch> patches = new ArrayList<>();
        for (Map.Entry<FacePlane, Set<FaceCell>> entry : planes.entrySet()) {
            java.util.TreeSet<FaceCell> remaining = new java.util.TreeSet<>(entry.getValue());
            while (!remaining.isEmpty()) {
                FaceCell start = remaining.first();
                int width = 1;
                while (remaining.contains(new FaceCell(start.a() + width, start.b()))) {
                    width++;
                }

                int height = 1;
                boolean completeRow = true;
                while (completeRow) {
                    for (int offset = 0; offset < width; offset++) {
                        if (!remaining.contains(new FaceCell(start.a() + offset, start.b() + height))) {
                            completeRow = false;
                            break;
                        }
                    }
                    if (completeRow) {
                        height++;
                    }
                }

                for (int bOffset = 0; bOffset < height; bOffset++) {
                    for (int aOffset = 0; aOffset < width; aOffset++) {
                        remaining.remove(new FaceCell(start.a() + aOffset, start.b() + bOffset));
                    }
                }
                patches.add(new SurfacePatch(entry.getKey().direction(), entry.getKey().plane(),
                        start.a(), start.b(), width, height));
            }
        }
        return patches;
    }

    private static FaceAxes faceAxes(Vector3d localNormal) {
        Vector3d axisA = Math.abs(localNormal.y) > 0.5
                ? new Vector3d(1, 0, 0)
                : new Vector3d(0, 1, 0);
        Vector3d axisB = new Vector3d(localNormal).cross(axisA).normalize();
        axisA = new Vector3d(axisB).cross(localNormal).normalize();
        return new FaceAxes(axisA, axisB);
    }

    private static void appendFaceCorners(List<Vector3d> samples, Vector3d center,
                                          Quaterniondc orientation, Vector3dc scale,
                                          Vector3d localNormal) {
        FaceAxes localAxes = faceAxes(localNormal);
        Vector3d axisA = orientation.transform(localAxes.axisA().mul(scale), new Vector3d());
        Vector3d axisB = orientation.transform(localAxes.axisB().mul(scale), new Vector3d());
        samples.add(new Vector3d(center).fma(-WAKE_HALF_SIZE, axisA).fma(-WAKE_HALF_SIZE, axisB));
        samples.add(new Vector3d(center).fma(WAKE_HALF_SIZE, axisA).fma(-WAKE_HALF_SIZE, axisB));
        samples.add(new Vector3d(center).fma(WAKE_HALF_SIZE, axisA).fma(WAKE_HALF_SIZE, axisB));
        samples.add(new Vector3d(center).fma(-WAKE_HALF_SIZE, axisA).fma(WAKE_HALF_SIZE, axisB));
    }

    /**
     * Builds ribbons only from the actual boundary edges of the windward block
     * faces. The previous implementation extruded one convex hull around the
     * entire craft; concave ships therefore gained enormous diagonal faces,
     * which appeared as the pale triangles in side and rear views. Cancelling
     * shared block edges preserves the real non-convex silhouette and leaves no
     * distant cap or closed ring that can turn into a screen-sized wedge.
     */
    private static void renderConnectedWake(BufferBuilder builder, Matrix4f matrix, SubLevel ship,
                                            Vector3d direction, Vector3d velocity,
                                            Vector3d acceleration, Vector3d angularVelocity,
                                            float intensity, float time, List<Vector3d> samples,
                                            double leadingProjection, double hullDepth,
                                            PlasmaPass pass) {
        if (samples.size() < 3) {
            return;
        }

        long shipSeed = ship.getUniqueId().getMostSignificantBits()
                ^ ship.getUniqueId().getLeastSignificantBits();
        List<WakeEdge> edges = boundaryEdges(samples, shipSeed);
        if (edges.isEmpty()) {
            return;
        }

        double speed = Math.max(velocity.length(), 0.08);
        double wakeLength = hullDepth + Mth.clamp(
                (speed - ENTRY_SPEED_OFF) * 3.0 + intensity * 5.0, 3.2, 8.5);
        Vector3d accelerationPerpendicular = new Vector3d(acceleration)
                .fma(-acceleration.dot(direction), direction);
        double rotationEnergy = Mth.clamp(angularVelocity.length() * 0.035, 0.0, 0.18);

        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            WakeEdge edge = edges.get(edgeIndex);
            Vector3d tangent = new Vector3d(edge.second()).sub(edge.first());
            if (tangent.lengthSquared() < 1.0E-8) {
                continue;
            }
            tangent.normalize();
            // Adjacent boundary ribbons overlap by one texture pixel at their
            // corners. Their roots therefore read as one unbroken sheath.
            Vector3d rootFirst = new Vector3d(edge.first())
                    .fma(-TEXTURE_PIXEL, tangent)
                    .fma(-TEXTURE_PIXEL, direction);
            Vector3d rootSecond = new Vector3d(edge.second())
                    .fma(TEXTURE_PIXEL, tangent)
                    .fma(-TEXTURE_PIXEL, direction);
            Vector3d[] first = new Vector3d[WAKE_RIBBON_STEPS + 1];
            Vector3d[] second = new Vector3d[WAKE_RIBBON_STEPS + 1];
            for (int step = 0; step <= WAKE_RIBBON_STEPS; step++) {
                double progress = step / (double) WAKE_RIBBON_STEPS;
                double distance = wakeLength * progress;
                Vector3d offset = wakeOffset(direction, accelerationPerpendicular, speed,
                        distance, wakeLength);
                first[step] = new Vector3d(rootFirst).add(offset);
                second[step] = new Vector3d(rootSecond).add(offset);
            }

            for (int step = 0; step < WAKE_RIBBON_STEPS; step++) {
                double nearProgress = step / (double) WAKE_RIBBON_STEPS;
                double farProgress = (step + 1.0) / WAKE_RIBBON_STEPS;
                int[] nearColor = ribbonColor(nearProgress, edgeIndex, edge.seed(), intensity,
                        rotationEnergy, time, pass);
                int[] farColor = ribbonColor(farProgress, edgeIndex, edge.seed(), intensity,
                        rotationEnergy, time, pass);
                quad(builder, matrix, first[step], second[step], second[step + 1], first[step + 1],
                        nearColor, farColor);

                // A broad hot band inside each translucent sheet recreates the
                // layered Cassini streaks without falling back to thin lines.
                Vector3d coreNearFirst = new Vector3d(first[step]).lerp(second[step], 0.18);
                Vector3d coreNearSecond = new Vector3d(first[step]).lerp(second[step], 0.82);
                Vector3d coreFarFirst = new Vector3d(first[step + 1]).lerp(second[step + 1], 0.18);
                Vector3d coreFarSecond = new Vector3d(first[step + 1]).lerp(second[step + 1], 0.82);
                int[] coreNear = ribbonCoreColor(nearProgress, intensity, time, pass);
                int[] coreFar = ribbonCoreColor(farProgress, intensity, time, pass);
                quad(builder, matrix, coreNearFirst, coreNearSecond, coreFarSecond, coreFarFirst,
                        coreNear, coreFar);
            }
        }
    }

    private static List<WakeEdge> boundaryEdges(List<Vector3d> samples, long shipSeed) {
        Map<EdgeKey, WakeEdge> boundary = new LinkedHashMap<>();
        int faceCount = samples.size() / 4;
        for (int face = 0; face < faceCount; face++) {
            int offset = face * 4;
            for (int side = 0; side < 4; side++) {
                Vector3d first = samples.get(offset + side);
                Vector3d second = samples.get(offset + (side + 1) % 4);
                EdgeKey key = edgeKey(first, second);
                if (boundary.remove(key) == null) {
                    long edgeSeed = mix64(shipSeed ^ ((long) face << 32) ^ side);
                    boundary.put(key, new WakeEdge(new Vector3d(first), new Vector3d(second), edgeSeed));
                }
            }
        }
        return new ArrayList<>(boundary.values());
    }

    private static EdgeKey edgeKey(Vector3d first, Vector3d second) {
        QuantizedPoint a = quantize(first);
        QuantizedPoint b = quantize(second);
        return a.compareTo(b) <= 0 ? new EdgeKey(a, b) : new EdgeKey(b, a);
    }

    private static QuantizedPoint quantize(Vector3d point) {
        return new QuantizedPoint(
                Math.round(point.x * EDGE_KEY_SCALE),
                Math.round(point.y * EDGE_KEY_SCALE),
                Math.round(point.z * EDGE_KEY_SCALE));
    }

    private static Vector3d wakeOffset(Vector3d direction, Vector3d accelerationPerpendicular,
                                       double speed, double distance, double wakeLength) {
        Vector3d offset = new Vector3d(direction).mul(-distance);
        if (distance <= 0.0) {
            return offset;
        }

        double ageTicks = Math.min(18.0, distance / speed);
        Vector3d curvature = new Vector3d(accelerationPerpendicular).mul(0.16 * ageTicks * ageTicks);
        clampLength(curvature, Math.max(0.30, wakeLength * 0.12));
        return offset.add(curvature);
    }

    private static int[] ribbonColor(double progress, int edgeIndex, long seed, float intensity,
                                     double rotationEnergy, float time, PlasmaPass pass) {
        int[] rgb = gradientAt(0.12 + progress * 0.88);
        double seedPhase = (seed & 0xffffL) * 0.00031;
        double broad = 0.5 + 0.5 * Math.sin(edgeIndex * 1.37 + seedPhase);
        double downstreamVariation = smoothstep(0.12, 0.55, progress);
        double connectedBand = Mth.lerp(downstreamVariation, 1.0, 0.90 + 0.10 * broad);
        double phaseSpread = downstreamVariation * (edgeIndex * 0.31 + seedPhase * 0.18);
        double pulseAmount = 0.08 + rotationEnergy * 0.22;
        double pulse = 1.0 - pulseAmount
                + pulseAmount * (0.5 + 0.5 * Math.sin(
                time * 6.0 - progress * 13.0 + phaseSpread));
        double fade = Math.pow(Math.max(0.0, 1.0 - progress), 0.56);
        double alpha = (pass == PlasmaPass.BLOOM ? 28.0 : 142.0)
                * intensity * fade * connectedBand * pulse;

        double whiteMix = broad * Math.pow(1.0 - progress, 0.7) * 0.18;
        rgb[0] = (int) Mth.lerp(whiteMix, rgb[0], 255);
        rgb[1] = (int) Mth.lerp(whiteMix, rgb[1], 236);
        rgb[2] = (int) Mth.lerp(whiteMix, rgb[2], 230);
        if (pass == PlasmaPass.BLOOM) {
            rgb[0] = (rgb[0] + 255) / 2;
            rgb[1] = (rgb[1] + 225) / 2;
            rgb[2] = (rgb[2] + 235) / 2;
        }
        return rgba(rgb[0], rgb[1], rgb[2], alpha);
    }

    private static int[] ribbonCoreColor(double progress, float intensity, float time, PlasmaPass pass) {
        int[] rgb = gradientAt(0.05 + progress * 0.72);
        double pulse = 0.90 + 0.10 * Math.sin(time * 7.2 - progress * 16.0);
        double fade = Math.pow(Math.max(0.0, 1.0 - progress), 0.78);
        double alpha = (pass == PlasmaPass.BLOOM ? 18.0 : 82.0) * intensity * fade * pulse;
        if (pass == PlasmaPass.BLOOM) {
            rgb[0] = (rgb[0] + 255) / 2;
            rgb[1] = (rgb[1] + 240) / 2;
            rgb[2] = (rgb[2] + 235) / 2;
        }
        return rgba(rgb[0], rgb[1], rgb[2], alpha);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static WakeFrame buildWakeFrame(List<Vector3d> samples, Vector3d direction,
                                            Quaterniondc orientation, double leadingProjection) {
        Vector3d average = new Vector3d();
        for (Vector3d sample : samples) {
            average.add(sample);
        }
        average.mul(1.0 / samples.size());

        Vector3d axisU = orientation.transform(new Vector3d(1, 0, 0), new Vector3d());
        axisU.fma(-axisU.dot(direction), direction);
        if (axisU.lengthSquared() < 1.0E-5) {
            axisU = orientation.transform(new Vector3d(0, 0, 1), new Vector3d());
            axisU.fma(-axisU.dot(direction), direction);
        }
        if (axisU.lengthSquared() < 1.0E-5) {
            axisU = Math.abs(direction.y) > 0.9
                    ? new Vector3d(1, 0, 0)
                    : new Vector3d(0, 1, 0);
            axisU.fma(-axisU.dot(direction), direction);
        }
        axisU.normalize();
        Vector3d axisV = new Vector3d(direction).cross(axisU).normalize();

        List<Point2> projected = new ArrayList<>(samples.size());
        for (Vector3d sample : samples) {
            Vector3d relative = new Vector3d(sample).sub(average);
            projected.add(new Point2(relative.dot(axisU), relative.dot(axisV)));
        }
        List<Point2> hull = convexHull(projected);
        if (hull.size() < 3) {
            return null;
        }

        Vector3d frontCenter = new Vector3d(average)
                .fma(leadingProjection - average.dot(direction) + FACE_EPSILON, direction);
        List<Point2> resampled = resamplePerimeter(hull, WAKE_RING_SEGMENTS);
        List<Vector3d> perimeter = new ArrayList<>(WAKE_RING_SEGMENTS);
        double radius = 0.0;
        for (Point2 point : resampled) {
            Vector3d worldPoint = new Vector3d(frontCenter)
                    .fma(point.u(), axisU).fma(point.v(), axisV);
            perimeter.add(worldPoint);
            radius += Math.sqrt(point.u() * point.u() + point.v() * point.v());
        }
        radius = Math.max(0.75, radius / WAKE_RING_SEGMENTS);
        return new WakeFrame(frontCenter, axisU, axisV, perimeter, radius);
    }

    private static List<Point2> convexHull(List<Point2> input) {
        List<Point2> points = new ArrayList<>(input);
        points.sort((a, b) -> {
            int u = Double.compare(a.u(), b.u());
            return u != 0 ? u : Double.compare(a.v(), b.v());
        });

        List<Point2> unique = new ArrayList<>(points.size());
        for (Point2 point : points) {
            if (unique.isEmpty()) {
                unique.add(point);
                continue;
            }
            Point2 previous = unique.getLast();
            if (Math.abs(point.u() - previous.u()) > 1.0E-4
                    || Math.abs(point.v() - previous.v()) > 1.0E-4) {
                unique.add(point);
            }
        }
        if (unique.size() < 3) {
            return unique;
        }

        List<Point2> lower = new ArrayList<>();
        for (Point2 point : unique) {
            while (lower.size() >= 2
                    && cross(lower.get(lower.size() - 2), lower.getLast(), point) <= 0.0) {
                lower.removeLast();
            }
            lower.add(point);
        }
        List<Point2> upper = new ArrayList<>();
        for (int i = unique.size() - 1; i >= 0; i--) {
            Point2 point = unique.get(i);
            while (upper.size() >= 2
                    && cross(upper.get(upper.size() - 2), upper.getLast(), point) <= 0.0) {
                upper.removeLast();
            }
            upper.add(point);
        }
        lower.removeLast();
        upper.removeLast();
        lower.addAll(upper);
        return lower;
    }

    private static List<Point2> resamplePerimeter(List<Point2> hull, int count) {
        double[] lengths = new double[hull.size()];
        double totalLength = 0.0;
        for (int i = 0; i < hull.size(); i++) {
            Point2 a = hull.get(i);
            Point2 b = hull.get((i + 1) % hull.size());
            lengths[i] = Math.hypot(b.u() - a.u(), b.v() - a.v());
            totalLength += lengths[i];
        }

        List<Point2> result = new ArrayList<>(count);
        int edge = 0;
        double edgeStart = 0.0;
        for (int i = 0; i < count; i++) {
            double target = totalLength * i / count;
            while (edge < lengths.length - 1 && target > edgeStart + lengths[edge]) {
                edgeStart += lengths[edge++];
            }
            Point2 a = hull.get(edge);
            Point2 b = hull.get((edge + 1) % hull.size());
            double fraction = lengths[edge] < 1.0E-8 ? 0.0 : (target - edgeStart) / lengths[edge];
            result.add(new Point2(
                    Mth.lerp(fraction, a.u(), b.u()),
                    Mth.lerp(fraction, a.v(), b.v())
            ));
        }
        return result;
    }

    private static double cross(Point2 a, Point2 b, Point2 c) {
        return (b.u() - a.u()) * (c.v() - a.v())
                - (b.v() - a.v()) * (c.u() - a.u());
    }

    /** A single white-hot exterior sheet. This avoids the old buried quad
     * appearing as an inner box from rear angles. */
    private static void emitSurfacePatch(BufferBuilder builder, Matrix4f matrix,
                                         Vector3d worldCenter, Vector3d worldNormal,
                                         Quaterniondc orientation, Vector3dc scale,
                                         Vector3d localNormal, double halfA, double halfB,
                                         int[] col, float alpha, PlasmaPass pass) {
        FaceAxes localAxes = faceAxes(localNormal);
        Vector3d axisA = orientation.transform(localAxes.axisA().mul(scale), new Vector3d());
        Vector3d axisB = orientation.transform(localAxes.axisB().mul(scale), new Vector3d());

        Vector3d plasmaCenter = new Vector3d(worldCenter).fma(FACE_EPSILON, worldNormal);
        boolean bloom = pass == PlasmaPass.BLOOM;

        int r = 255;
        int g = (col[1] + 255) / 2;
        int b = (col[2] + 238) / 2;
        int[] plasmaColor = rgba(r, g, b, (bloom ? 112.0 : 255.0) * alpha);

        Vector3d p00 = new Vector3d(plasmaCenter)
                .fma(-halfA, axisA).fma(-halfB, axisB);
        Vector3d p10 = new Vector3d(plasmaCenter)
                .fma(halfA, axisA).fma(-halfB, axisB);
        Vector3d p11 = new Vector3d(plasmaCenter)
                .fma(halfA, axisA).fma(halfB, axisB);
        Vector3d p01 = new Vector3d(plasmaCenter)
                .fma(-halfA, axisA).fma(halfB, axisB);
        quad(builder, matrix, p00, p10, p11, p01, plasmaColor, plasmaColor);
    }


    /** Cached list of the ship's exposed hull faces in local space. */
    private static List<HullFace> hullFaces(SubLevel ship) {
        UUID id = ship.getUniqueId();
        long now = System.currentTimeMillis();
        HullCache cached = HULL_CACHE.get(id);
        if (cached != null && now - cached.builtAt() < HULL_CACHE_MS) {
            return cached.faces();
        }

        List<HullFace> faces = buildHullFaces(ship);
        HULL_CACHE.put(id, new HullCache(faces == null ? List.of() : faces, now));
        // Keep the cache from growing unbounded as ships come and go.
        if (HULL_CACHE.size() > 64) {
            HULL_CACHE.entrySet().removeIf(e -> now - e.getValue().builtAt() > 10_000);
        }
        return faces;
    }

    /** Scans the ship's plot blocks and records every exposed (air-adjacent) face. */
    private static List<HullFace> buildHullFaces(SubLevel ship) {
        try {
            LevelPlot plot = ship.getPlot();
            if (plot == null) {
                return null;
            }
            List<HullFace> faces = new ArrayList<>();

            for (PlotChunkHolder holder : plot.getLoadedChunks()) {
                LevelChunk chunk = holder.getChunk();
                if (chunk == null) {
                    continue;
                }
                int baseX = chunk.getPos().getMinBlockX();
                int baseZ = chunk.getPos().getMinBlockZ();
                LevelChunkSection[] sections = chunk.getSections();
                for (int si = 0; si < sections.length; si++) {
                    LevelChunkSection section = sections[si];
                    if (section == null || section.hasOnlyAir()) {
                        continue;
                    }
                    int baseY = chunk.getMinBuildHeight() + si * 16;
                    for (int ly = 0; ly < 16; ly++) {
                        for (int lz = 0; lz < 16; lz++) {
                            for (int lx = 0; lx < 16; lx++) {
                                if (section.getBlockState(lx, ly, lz).isAir()) {
                                    continue;
                                }
                                int wx = baseX + lx, wy = baseY + ly, wz = baseZ + lz;
                                for (Direction dir : Direction.values()) {
                                    if (!chunk.getBlockState(new BlockPos(
                                            wx + dir.getStepX(), wy + dir.getStepY(), wz + dir.getStepZ())).isAir()) {
                                        continue; // face is internal
                                    }
                                    faces.add(new HullFace(
                                            new Vector3d(wx + 0.5, wy + 0.5, wz + 0.5)
                                                    .fma(0.5, new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ())),
                                            new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ()),
                                            BlockPos.asLong(wx, wy, wz), dir));
                                    if (faces.size() > MAX_HULL_FACES) {
                                        return null; // too big — use the envelope
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return faces;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Interpolated gradient colour (RGB only) at position t in [0,1]. */
    private static int[] gradientAt(double t) {
        double scaled = Mth.clamp(t, 0.0, 1.0) * (GRADIENT.length - 1);
        int i = (int) Math.floor(scaled);
        int j = Math.min(i + 1, GRADIENT.length - 1);
        double f = scaled - i;
        return new int[]{
                (int) (GRADIENT[i][0] + (GRADIENT[j][0] - GRADIENT[i][0]) * f),
                (int) (GRADIENT[i][1] + (GRADIENT[j][1] - GRADIENT[i][1]) * f),
                (int) (GRADIENT[i][2] + (GRADIENT[j][2] - GRADIENT[i][2]) * f),
        };
    }

    private static int[] rgba(int r, int g, int b, double alpha) {
        return new int[]{r, g, b, (int) Mth.clamp(alpha, 0.0, 255.0)};
    }

    private static Vector3d angularVelocity(Quaterniondc previous, Quaterniondc current) {
        Quaterniond delta = new Quaterniond(current)
                .mul(new Quaterniond(previous).conjugate())
                .normalize();
        double sign = delta.w() < 0.0 ? -1.0 : 1.0;
        double w = Mth.clamp(delta.w() * sign, -1.0, 1.0);
        double halfSine = Math.sqrt(Math.max(0.0, 1.0 - w * w));
        if (halfSine < 1.0E-6) {
            return new Vector3d();
        }
        double angle = 2.0 * Math.acos(w);
        return new Vector3d(delta.x() * sign, delta.y() * sign, delta.z() * sign)
                .mul(angle / (halfSine * TICK_SECONDS));
    }

    private static void clampLength(Vector3d vector, double maximum) {
        double lengthSquared = vector.lengthSquared();
        if (lengthSquared > maximum * maximum) {
            vector.normalize(maximum);
        }
    }

    private static double normalizedDot(Vector3d first, Vector3d second) {
        double denominator = Math.sqrt(first.lengthSquared() * second.lengthSquared());
        if (denominator < 1.0E-8) {
            return 1.0;
        }
        return Mth.clamp(first.dot(second) / denominator, -1.0, 1.0);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** Ring resolution of the plasma envelope (around the travel axis). */
    private static final int RING_SEGMENTS = 28;
    /** Longitudinal resolution (front cap → trailing wake). */
    private static final int LENGTH_STEPS = 16;

    /**
     * Reference-matched heat ramp: white-hot pressure layer, yellow-orange
     * plasma, red streamlines, and a restrained magenta-violet tail.
     */
    private static final int[][] GRADIENT = {
            {255, 253, 238},
            {255, 226, 148},
            {255, 166, 105},
            {255, 119, 155},
            {226, 76, 186},
            {126, 52, 158},
    };

    private static void renderShipPlasma(BufferBuilder builder, Matrix4f matrix, SubLevel ship,
                                         Vector3d position, Quaterniondc orientation,
                                         Vector3dc rotationPoint, Vector3dc scale,
                                         Vector3d velocity, Vector3d acceleration,
                                         Vector3d angularVelocity, float intensity, float time,
                                         PlasmaPass pass) {
        Vector3d direction = new Vector3d(velocity);
        if (direction.lengthSquared() < 1.0E-8) {
            return;
        }
        direction.normalize();
        // Preferred: a plasma SKIN over the ship's actual exposed block faces,
        // so the glow takes the shape of the hull like a compression shirt.
        // Falls back to the smooth envelope for ships too big to skin per frame.
        if (renderHullSkin(builder, matrix, ship, position, orientation, rotationPoint, scale,
                velocity, acceleration,
                angularVelocity, intensity, time, pass)) {
            return;
        }

        // Fallback: smooth ellipsoidal envelope from the bounding box.
        var box = ship.boundingBox();
        double sizeX = Math.max(2.0, box.maxX() - box.minX());
        double sizeY = Math.max(2.0, box.maxY() - box.minY());
        double sizeZ = Math.max(2.0, box.maxZ() - box.minZ());
        double crossRadius = Math.max(Math.max(sizeX, sizeZ), sizeY) * 0.5;

        // Orthonormal frame around the travel direction.
        Vector3d up = Math.abs(direction.y) > 0.92 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(direction).cross(up).normalize();
        Vector3d binormal = new Vector3d(right).cross(direction).normalize();

        long seed = ship.getUniqueId().getLeastSignificantBits();
        double sheathLength = crossRadius * (1.05 + 0.65 * intensity);
        double capLead = crossRadius * 0.08;

        // The envelope is ONE continuous ring-mesh: for each longitudinal step t
        // (0 at the cap ahead of the hull, 1 at the wake tail) we build a ring of
        // vertices, and connect consecutive rings with quads. Because adjacent
        // quads share vertices, the surface is seamless — no worm gaps.
        for (int s = 0; s < LENGTH_STEPS; s++) {
            double t0 = s / (double) LENGTH_STEPS;
            double t1 = (s + 1) / (double) LENGTH_STEPS;
            for (int r = 0; r < RING_SEGMENTS; r++) {
                double a0 = (Math.PI * 2.0 * r) / RING_SEGMENTS;
                double a1 = (Math.PI * 2.0 * (r + 1)) / RING_SEGMENTS;

                Vector3d v00 = envelopePoint(position, direction, right, binormal,
                        crossRadius, sheathLength, capLead, t0, a0);
                Vector3d v01 = envelopePoint(position, direction, right, binormal,
                        crossRadius, sheathLength, capLead, t0, a1);
                Vector3d v11 = envelopePoint(position, direction, right, binormal,
                        crossRadius, sheathLength, capLead, t1, a1);
                Vector3d v10 = envelopePoint(position, direction, right, binormal,
                        crossRadius, sheathLength, capLead, t1, a0);

                int[] c0 = envelopeColor(t0, a0, time, seed, intensity, pass);
                int[] c1 = envelopeColor(t1, a1, time, seed, intensity, pass);
                quad(builder, matrix, v00, v01, v11, v10, c0, c1);
            }
        }

        // Bright bow-shock cap on the windward face, filled from the apex out.
        bowShockCap(builder, matrix, position, direction, right, binormal, crossRadius, capLead,
                intensity, time, seed, pass);
    }

    /**
     * A point on the plasma envelope surface. {@code t} runs 0 (cap, ahead of
     * the hull) → 1 (wake tail); {@code angle} goes around the axis. The radius
     * bulges at the shoulder and tapers to the tail, with a little noise on the
     * rim so the border varies instead of being a clean tube.
     */
    private static Vector3d envelopePoint(Vector3d position, Vector3d direction, Vector3d right, Vector3d binormal,
                                          double crossRadius, double sheathLength, double capLead,
                                          double t, double angle) {
        // Keep the cross-section disciplined. Perspective creates the fan;
        // geometric noise only makes the effect look inflated and wavy.
        double profile = 1.03 - 0.36 * smoothstep(0.42, 1.0, t);
        double radius = crossRadius * profile;

        double along = capLead - t * sheathLength;
        return new Vector3d(position)
                .fma(along, direction)
                .fma(Math.cos(angle) * radius, right)
                .fma(Math.sin(angle) * radius, binormal);
    }

    /** Interpolated gradient colour + broad connected ray bands. */
    private static int[] envelopeColor(double t, double angle, float time, long seed,
                                       float intensity, PlasmaPass pass) {
        double scaled = t * (GRADIENT.length - 1);
        int i = (int) Math.floor(scaled);
        int j = Math.min(i + 1, GRADIENT.length - 1);
        double f = scaled - i;
        int r = (int) (GRADIENT[i][0] + (GRADIENT[j][0] - GRADIENT[i][0]) * f);
        int g = (int) (GRADIENT[i][1] + (GRADIENT[j][1] - GRADIENT[i][1]) * f);
        int b = (int) (GRADIENT[i][2] + (GRADIENT[j][2] - GRADIENT[i][2]) * f);
        double seedPhase = (seed & 0xffffL) * 0.00031;
        double broad = 0.5 + 0.5 * Math.sin(angle * 5.0 + seedPhase);
        double rays = 0.28 + 0.72 * smoothstep(0.18, 0.84, broad);
        double pulse = 0.90 + 0.10 * Math.sin(time * 3.0 - t * 12.0 + angle * 1.4);
        double alphaProfile = Math.pow(Math.max(0.0, 1.0 - t), 0.48);
        int alpha = (int) ((pass == PlasmaPass.BLOOM ? 14 : 78)
                * intensity * alphaProfile * rays * pulse);
        if (pass == PlasmaPass.BLOOM) {
            r = (r + 255) / 2;
            g = (g + 235) / 2;
            b = (b + 230) / 2;
        }
        return new int[]{r, g, b, alpha};
    }

    /** The compressed-air shock dome just ahead of the hull's leading face. */
    private static void bowShockCap(BufferBuilder builder, Matrix4f matrix, Vector3d center, Vector3d direction,
                                    Vector3d right, Vector3d binormal, double crossRadius, double capLead,
                                    float intensity, float time, long seed, PlasmaPass pass) {
        Vector3d apex = new Vector3d(center).fma(capLead * 1.35, direction);
        int[] core = rgba(255, 252, 238, (pass == PlasmaPass.BLOOM ? 64.0 : 175.0) * intensity);
        int[] rim = {255, pass == PlasmaPass.BLOOM ? 195 : 150, pass == PlasmaPass.BLOOM ? 155 : 70, 0};

        for (int i = 0; i < RING_SEGMENTS; i++) {
            double a0 = (Math.PI * 2.0 * i) / RING_SEGMENTS;
            double a1 = (Math.PI * 2.0 * (i + 1)) / RING_SEGMENTS;
            double r0 = crossRadius * 1.02;
            double r1 = crossRadius * 1.02;

            Vector3d e0 = new Vector3d(apex).fma(Math.cos(a0) * r0, right).fma(Math.sin(a0) * r0, binormal);
            Vector3d e1 = new Vector3d(apex).fma(Math.cos(a1) * r1, right).fma(Math.sin(a1) * r1, binormal);
            quad(builder, matrix, apex, apex, e1, e0, core, rim);
        }
    }

    private static void quad(BufferBuilder builder, Matrix4f matrix,
                             Vector3d a, Vector3d b, Vector3d c, Vector3d d,
                             int[] colorNear, int[] colorFar) {
        builder.addVertex(matrix, (float) a.x, (float) a.y, (float) a.z)
                .setColor(colorNear[0], colorNear[1], colorNear[2], colorNear[3]);
        builder.addVertex(matrix, (float) b.x, (float) b.y, (float) b.z)
                .setColor(colorNear[0], colorNear[1], colorNear[2], colorNear[3]);
        builder.addVertex(matrix, (float) c.x, (float) c.y, (float) c.z)
                .setColor(colorFar[0], colorFar[1], colorFar[2], colorFar[3]);
        builder.addVertex(matrix, (float) d.x, (float) d.y, (float) d.z)
                .setColor(colorFar[0], colorFar[1], colorFar[2], colorFar[3]);
    }

    private static void drawTranslucent(BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh == null) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        // Depth-test the plasma so it is hidden by hull blocks it sits behind
        // instead of being painted over the whole craft.
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        BufferUploader.drawWithShader(mesh);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Writes only the soft energy layer to Veil's HDR bloom target. */
    private static void drawBloom(BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh == null) {
            return;
        }

        if (!VeilBloomBridge.begin()) {
            mesh.close();
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        // Depth-test the plasma so it is hidden by hull blocks it sits behind
        // instead of being painted over the whole craft.
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        try {
            BufferUploader.drawWithShader(mesh);
        } finally {
            VeilBloomBridge.end();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

}
