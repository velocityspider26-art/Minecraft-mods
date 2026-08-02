package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.client.VeilBloomBridge;

/**
 * Draws particle-built magnetic prominences on the physical cube sun.
 *
 * Tuned against the reference footage: the flares there read as small, soft,
 * pale-cream puffs that only become visible where they bulge past the sun's
 * silhouette into the dark sky — not as large saturated arcs. So the loops are
 * compressed toward the surface ({@link #SPAN_SCALE}/{@link #HEIGHT_SCALE}),
 * the particles are round rather than streaked ({@link #PARTICLE_SCALE}), and
 * the palette is pale yellow-white instead of orange-red.
 */
final class SolarFlareParticleEmitter {
    // Compact puffs need far fewer samples than the old long arcs did, which
    // buys the budget for ~3x as many flares across the star. Each particle is
    // now a soft fan rather than one quad, so the count is trimmed again to
    // keep the vertex cost in the same ballpark.
    private static final int PARTICLES_PER_STRAND = 18;
    private static final int STRANDS = 3;
    /** Fan segments per soft particle — enough to read as round when overlapped. */
    private static final int PARTICLE_SEGMENTS = 7;

    /** Compresses each prominence loop into a compact puff hugging the surface. */
    private static final double SPAN_SCALE = 0.46;
    /** Peak reach of an erupting flare — enough to clear the silhouette and be seen. */
    private static final double HEIGHT_SCALE = 0.52;
    /** Fatter, rounder particles so a strand reads as soft plasma, not a streak. */
    private static final double PARTICLE_SCALE = 2.15;

    /**
     * Flares per cube face. Activity covers the whole star so that, from any
     * viewing angle, several sit near the silhouette where they actually read.
     */
    private static final int FLARES_PER_FACE = 9;
    /** Jittered grid resolution used to spread flares over each face. */
    private static final int FLARE_GRID = 3;

    private static final FlareSpec[] FLARES = generateFlares();

    /**
     * Deterministically scatters prominences over all six faces. Sizes follow a
     * skewed distribution — mostly modest puffs with a few standouts — which is
     * what the reference shows: a busy rim where one or two flares dominate at
     * any moment rather than a uniform ring.
     */
    private static FlareSpec[] generateFlares() {
        java.util.Random random = new java.util.Random(0x50AC_F1A3L);
        java.util.List<FlareSpec> specs = new java.util.ArrayList<>(6 * FLARES_PER_FACE);

        for (int face = 0; face < 6; face++) {
            for (int index = 0; index < FLARES_PER_FACE; index++) {
                // Jittered grid keeps them spread instead of clumping.
                int cell = index % (FLARE_GRID * FLARE_GRID);
                double gridU = (cell % FLARE_GRID + 0.5) / FLARE_GRID;
                double gridV = (cell / FLARE_GRID + 0.5) / FLARE_GRID;
                double jitter = 0.9 / FLARE_GRID;
                double u = (gridU * 2.0 - 1.0) * 0.82 + (random.nextDouble() - 0.5) * jitter;
                double v = (gridV * 2.0 - 1.0) * 0.82 + (random.nextDouble() - 0.5) * jitter;
                // Push samples toward the face edges. A pale flare over the
                // middle of a lit face is invisible — flares only read where
                // they bulge past the silhouette, and the faces' edges are what
                // form that rim from any viewing angle.
                u = Math.signum(u) * Math.pow(Math.abs(u), 0.55);
                v = Math.signum(v) * Math.pow(Math.abs(v), 0.55);

                // Skewed size: cube the roll so large flares stay rare.
                double roll = random.nextDouble();
                double scale = roll * roll * roll;
                double span = 0.26 + scale * 0.62;
                double height = 0.15 + scale * 0.40;
                double width = 0.026 + scale * 0.032;
                double intensity = 0.58 + scale * 0.42;

                double angle = random.nextDouble() * Math.PI * 2.0;
                double phase = random.nextDouble();
                double period = 5.2 + random.nextDouble() * 6.0;

                specs.add(new FlareSpec(face, Mth.clamp(u, -0.88, 0.88), Mth.clamp(v, -0.88, 0.88),
                        angle, span, height, width, phase, period, intensity));
            }
        }
        return specs.toArray(new FlareSpec[0]);
    }

    private record FaceBasis(Vector3d normal, Vector3d tangent, Vector3d bitangent) {
    }

    private record FlareSpec(int face, double u, double v, double angle, double span,
                             double height, double width, double phase, double period,
                             double intensity) {
    }

    private enum Pass {
        MAIN,
        BLOOM
    }

    private SolarFlareParticleEmitter() {
    }

    /** The additive layer, drawn inline with the star so planets paint over it. */
    static void renderAttachedMain(Matrix4f matrix, Vector3dc cameraLocal, double halfSize,
                                   float timeSeconds, float opacity) {
        if (opacity < 0.08f || halfSize <= 0.0) {
            return;
        }
        renderPass(matrix, cameraLocal, halfSize, timeSeconds, opacity, Pass.MAIN);
    }

    /**
     * The bloom layer, run only after every planet has drawn its depth, so the
     * glow is hidden behind a planet in front of the star instead of bleeding
     * through it.
     */
    static void renderAttachedBloom(Matrix4f matrix, Vector3dc cameraLocal, double halfSize,
                                    float timeSeconds, float opacity) {
        if (opacity < 0.08f || halfSize <= 0.0) {
            return;
        }
        renderPass(matrix, cameraLocal, halfSize, timeSeconds, opacity, Pass.BLOOM);
    }

    private static void renderPass(Matrix4f matrix, Vector3dc cameraLocal, double halfSize,
                                   float timeSeconds, float opacity, Pass pass) {
        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        emitProminences(builder, matrix, cameraLocal, halfSize, timeSeconds, opacity, pass);
        draw(builder, pass == Pass.BLOOM);
    }

    private static void emitProminences(BufferBuilder builder, Matrix4f matrix,
                                        Vector3dc cameraLocal, double halfSize,
                                        float timeSeconds, float opacity, Pass pass) {
        for (int flareIndex = 0; flareIndex < FLARES.length; flareIndex++) {
            FlareSpec flare = FLARES[flareIndex];
            FaceBasis face = faceBasis(flare.face());
            Vector3d base = new Vector3d(face.normal()).mul(halfSize)
                    .fma(flare.u() * halfSize, face.tangent())
                    .fma(flare.v() * halfSize, face.bitangent());

            Vector3d toCamera = new Vector3d(cameraLocal).sub(base);
            double facing = face.normal().dot(toCamera) / Math.max(toCamera.length(), 1.0E-6);
            if (facing < -0.14) {
                continue;
            }

            double cos = Math.cos(flare.angle());
            double sin = Math.sin(flare.angle());
            Vector3d along = new Vector3d(face.tangent()).mul(cos)
                    .fma(sin, face.bitangent()).normalize();
            Vector3d across = new Vector3d(face.bitangent()).mul(cos)
                    .fma(-sin, face.tangent()).normalize();

            double phase = flare.phase() * Math.PI * 2.0;

            // Real eruption cycle: each flare climbs out of the surface, peaks,
            // and sinks back. Sharp rise / slower decay (the 0.65 power skews
            // the curve) reads as an actual eruption instead of a static arc.
            // Phases are staggered per flare, so at any moment some are
            // erupting while others are quiet.
            double cyclePosition = timeSeconds / flare.period() + flare.phase();
            double sawtooth = cyclePosition - Math.floor(cyclePosition);
            double eruption = Math.pow(Math.sin(Math.PI * sawtooth), 0.65);

            double pulse = 0.30 + 0.70 * eruption;
            double span = halfSize * flare.span() * SPAN_SCALE * (0.55 + 0.55 * eruption);
            double height = halfSize * flare.height() * HEIGHT_SCALE * (0.18 + 1.05 * eruption);
            double width = halfSize * flare.width() * (0.7 + 0.5 * eruption);

            for (int strand = 0; strand < STRANDS; strand++) {
                double strandUnit = (strand - (STRANDS - 1) * 0.5)
                        / ((STRANDS - 1) * 0.5);
                double strandOffset = strandUnit * width;
                double strandStrength = 1.0 - Math.abs(strandUnit) * 0.18;

                for (int particle = 0; particle < PARTICLES_PER_STRAND; particle++) {
                    double s = particle / (double) (PARTICLES_PER_STRAND - 1);
                    double previousS = Math.max(0.0, s - 1.0 / (PARTICLES_PER_STRAND - 1));
                    double nextS = Math.min(1.0, s + 1.0 / (PARTICLES_PER_STRAND - 1));

                    Vector3d point = flarePoint(base, face.normal(), along, across,
                            span, height, strandOffset, s, timeSeconds, phase)
                            .fma(halfSize * 0.006, face.normal());
                    Vector3d previous = flarePoint(base, face.normal(), along, across,
                            span, height, strandOffset, previousS, timeSeconds, phase)
                            .fma(halfSize * 0.006, face.normal());
                    Vector3d next = flarePoint(base, face.normal(), along, across,
                            span, height, strandOffset, nextS, timeSeconds, phase)
                            .fma(halfSize * 0.006, face.normal());

                    Vector3d tangent = new Vector3d(next).sub(previous);
                    double spacing = 0.5 * (point.distance(previous) + point.distance(next));
                    double arch = Math.sin(Math.PI * s);
                    double heat = Mth.clamp(0.24 + Math.pow(Math.max(arch, 0.0), 0.48) * 0.76,
                            0.0, 1.0);
                    double flow = 0.88 + 0.12 * Math.sin(
                            s * Math.PI * 10.0 - timeSeconds * 3.4 + phase + strand * 0.41);
                    double footpoint = 1.0 + 0.45 * Math.pow(1.0 - Math.max(arch, 0.0), 3.0);
                    double alpha = opacity * pulse * strandStrength * flow * flare.intensity();
                    double particleWidth = halfSize * (0.0068 + 0.0042 * arch)
                            * footpoint * flare.intensity() * PARTICLE_SCALE;
                    // Round, not streaked: keep length within a hair of width so
                    // overlapping particles blur into a soft puff.
                    double particleLength = Math.max(particleWidth, spacing * 0.74);

                    emitParticle(builder, matrix, cameraLocal, point, tangent,
                            particleLength, particleWidth, heat, alpha, pass);
                }
            }
        }
    }

    private static Vector3d flarePoint(Vector3d base, Vector3d normal, Vector3d along,
                                       Vector3d across, double span, double height,
                                       double strandOffset, double s, double time,
                                       double phase) {
        double arch = Math.pow(Math.max(0.0, Math.sin(Math.PI * s)), 0.82);
        double breathing = 1.0 + 0.055 * Math.sin(time * 1.55 + phase + s * Math.PI);
        double strandSpread = strandOffset * Math.pow(arch, 0.58);
        double plasmaShear = height * 0.035 * arch
                * Math.sin(s * Math.PI * 2.0 - time * 1.35 + phase);
        double lean = height * 0.075 * (s - 0.5) * arch
                * Math.sin(time * 0.73 + phase);

        return new Vector3d(base)
                .fma((s - 0.5) * span + lean, along)
                .fma(height * arch * breathing, normal)
                .fma(strandSpread + plasmaShear, across);
    }

    private static void emitParticle(BufferBuilder builder, Matrix4f matrix,
                                     Vector3dc cameraLocal, Vector3d point,
                                     Vector3d tangent, double halfLength,
                                     double halfWidth, double heat, double alpha,
                                     Pass pass) {
        Vector3d view = new Vector3d(cameraLocal).sub(point);
        if (view.lengthSquared() < 1.0E-8 || tangent.lengthSquared() < 1.0E-8) {
            return;
        }
        view.normalize();

        Vector3d projectedTangent = new Vector3d(tangent)
                .fma(-tangent.dot(view), view);
        if (projectedTangent.lengthSquared() < 1.0E-8) {
            Vector3d fallback = Math.abs(view.y) > 0.92
                    ? new Vector3d(1.0, 0.0, 0.0)
                    : new Vector3d(0.0, 1.0, 0.0);
            projectedTangent.set(view).cross(fallback);
        }
        projectedTangent.normalize();
        Vector3d across = new Vector3d(view).cross(projectedTangent).normalize();

        // Pale yellow-white, matching the reference: the flares there are cream
        // puffs, never saturated orange-red.
        if (pass == Pass.BLOOM) {
            int[] glow = rgba(255, (int) (226 + 24 * heat), (int) (150 + 70 * heat),
                    64.0 * alpha);
            emitSoftParticle(builder, matrix, point, projectedTangent, across,
                    halfLength * 2.1, halfWidth * 2.6, glow);
            return;
        }

        // Soft round sprites: bright centre fading to fully transparent at the
        // rim. A wide faint halo plus a tighter bright core gives the fuzzy
        // plasma blob the reference shows, with no hard quad edges.
        int[] halo = rgba(255, (int) (210 + 30 * heat), (int) (112 + 74 * heat),
                86.0 * alpha);
        int[] core = rgba(255, (int) (240 + 15 * heat), (int) (196 + 59 * heat),
                190.0 * alpha);
        emitSoftParticle(builder, matrix, point, projectedTangent, across,
                halfLength * 1.9, halfWidth * 2.2, halo);
        emitSoftParticle(builder, matrix, point, projectedTangent, across,
                halfLength * 1.0, halfWidth * 1.0, core);
    }

    /**
     * A soft round sprite: a fan of triangles from a bright centre out to a
     * fully transparent rim, so the particle fades smoothly in every direction
     * instead of ending at a hard quad edge. This is what makes the flares read
     * as fuzzy plasma rather than stacked rectangles.
     */
    /**
     * Unit-circle offsets for a fan, computed once instead of calling sin/cos
     * per segment per particle every frame.
     */
    private static final double[] SEGMENT_COS = new double[PARTICLE_SEGMENTS + 1];
    private static final double[] SEGMENT_SIN = new double[PARTICLE_SEGMENTS + 1];

    static {
        for (int segment = 0; segment <= PARTICLE_SEGMENTS; segment++) {
            double angle = (Math.PI * 2.0 * segment) / PARTICLE_SEGMENTS;
            SEGMENT_COS[segment] = Math.cos(angle);
            SEGMENT_SIN[segment] = Math.sin(angle);
        }
    }

    /**
     * Scratch vectors reused across every particle. The emitter runs only on the
     * render thread, and these are consumed immediately by {@code vertex}, so a
     * shared pair is safe — and it removes tens of thousands of short-lived
     * allocations per frame that were otherwise pure GC churn.
     */
    private static final Vector3d SCRATCH_EDGE_0 = new Vector3d();
    private static final Vector3d SCRATCH_EDGE_1 = new Vector3d();
    private static final int[] SCRATCH_RIM = new int[4];

    private static void emitSoftParticle(BufferBuilder builder, Matrix4f matrix,
                                         Vector3d center, Vector3d along,
                                         Vector3d across, double halfLength,
                                         double halfWidth, int[] color) {
        SCRATCH_RIM[0] = color[0];
        SCRATCH_RIM[1] = color[1];
        SCRATCH_RIM[2] = color[2];
        SCRATCH_RIM[3] = 0;

        for (int segment = 0; segment < PARTICLE_SEGMENTS; segment++) {
            Vector3d edge0 = SCRATCH_EDGE_0.set(center)
                    .fma(SEGMENT_COS[segment] * halfLength, along)
                    .fma(SEGMENT_SIN[segment] * halfWidth, across);
            Vector3d edge1 = SCRATCH_EDGE_1.set(center)
                    .fma(SEGMENT_COS[segment + 1] * halfLength, along)
                    .fma(SEGMENT_SIN[segment + 1] * halfWidth, across);
            // Degenerate quad = triangle (centre, edge0, edge1).
            vertex(builder, matrix, center, color);
            vertex(builder, matrix, center, color);
            vertex(builder, matrix, edge0, SCRATCH_RIM);
            vertex(builder, matrix, edge1, SCRATCH_RIM);
        }
    }

    private static void emitOrientedParticle(BufferBuilder builder, Matrix4f matrix,
                                             Vector3d center, Vector3d along,
                                             Vector3d across, double halfLength,
                                             double halfWidth, int[] color) {
        Vector3d p00 = new Vector3d(center).fma(-halfLength, along).fma(-halfWidth, across);
        Vector3d p10 = new Vector3d(center).fma(halfLength, along).fma(-halfWidth, across);
        Vector3d p11 = new Vector3d(center).fma(halfLength, along).fma(halfWidth, across);
        Vector3d p01 = new Vector3d(center).fma(-halfLength, along).fma(halfWidth, across);
        vertex(builder, matrix, p00, color);
        vertex(builder, matrix, p10, color);
        vertex(builder, matrix, p11, color);
        vertex(builder, matrix, p01, color);
    }

    private static void draw(BufferBuilder builder, boolean bloom) {
        MeshData mesh = builder.build();
        if (mesh == null) {
            return;
        }

        // Draw into the MAIN framebuffer, not Veil's bloom target. The bloom
        // target has no shared depth buffer, so anything drawn there composites
        // over the finished frame no matter what is in front of it — which is
        // exactly why the sun bled through blocks and terrain. In the main
        // framebuffer the depth test actually occludes the glow.
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            BufferUploader.drawWithShader(mesh);
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static FaceBasis faceBasis(int face) {
        return switch (face) {
            case 0 -> new FaceBasis(new Vector3d(1, 0, 0),
                    new Vector3d(0, 1, 0), new Vector3d(0, 0, 1));
            case 1 -> new FaceBasis(new Vector3d(-1, 0, 0),
                    new Vector3d(0, 1, 0), new Vector3d(0, 0, -1));
            case 2 -> new FaceBasis(new Vector3d(0, 1, 0),
                    new Vector3d(1, 0, 0), new Vector3d(0, 0, -1));
            case 3 -> new FaceBasis(new Vector3d(0, -1, 0),
                    new Vector3d(1, 0, 0), new Vector3d(0, 0, 1));
            case 4 -> new FaceBasis(new Vector3d(0, 0, 1),
                    new Vector3d(1, 0, 0), new Vector3d(0, 1, 0));
            default -> new FaceBasis(new Vector3d(0, 0, -1),
                    new Vector3d(-1, 0, 0), new Vector3d(0, 1, 0));
        };
    }

    private static void vertex(BufferBuilder builder, Matrix4f matrix,
                               Vector3d point, int[] color) {
        builder.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setColor(color[0], color[1], color[2], color[3]);
    }

    private static int[] rgba(int red, int green, int blue, double alpha) {
        return new int[]{red, green, blue, (int) Mth.clamp(alpha, 0.0, 255.0)};
    }
}
