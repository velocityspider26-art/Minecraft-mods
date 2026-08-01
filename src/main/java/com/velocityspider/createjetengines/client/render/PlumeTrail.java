package com.velocityspider.createjetengines.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * A volumetric exhaust plume built from the nozzle's emission history.
 *
 * <p>The plume is not a cone bolted to the block. Once per tick the nozzle emits a parcel of gas,
 * recording <em>where in the world</em> it left and which way it was pointing. Each parcel then
 * drifts downstream along its own emission direction, spreads, cools and fades. The plume you see
 * is the surface through all the live parcels.
 *
 * <p>Two things fall out of that for free, and they are the whole point:
 * <ul>
 *   <li>Fly forward and the older parcels are left behind, so the plume stretches and lags rather
 *       than moving rigidly with the aircraft.</li>
 *   <li>Turn, roll or pitch and the plume <em>bends</em>, because each parcel keeps the direction
 *       the nozzle had when it was emitted.</li>
 * </ul>
 *
 * <p>It also reacts to the world: each parcel stores the clear distance measured ahead of the
 * nozzle at emission time, and its drift is clamped to that, so the plume splashes flat against
 * the ground or a wall instead of passing through it.
 */
public class PlumeTrail {

    /** How many ticks a gas parcel stays visible. */
    private static final int MAX_AGE = 12;
    private static final int MAX_SAMPLES = 20;
    /** Vertices per ring. Twelve gives enough resolution for the surface to look broken up. */
    private static final int RING = 12;

    /**
     * The burning core is drawn as a short stack of straight frusta, six-sided and hard-stepped.
     * Low ring count and abrupt diameter changes are what make it read as rigid burning gas
     * instead of a flexible tube.
     */
    private static final int CORE_RING = 6;
    /**
     * Station radii down the core, as multiples of the nozzle radius: a taper to a point with a
     * mild mach-diamond ripple on top. Sampled at discrete stations and rendered flat-shaded, so
     * the silhouette stays hard and faceted. An earlier version rippled nearly 2:1 between
     * stations, which broke the flame into a string of separate beads.
     */
    private static final float[] CORE_PROFILE = {
            1.000F, 1.086F, 1.039F, 0.901F, 0.781F, 0.745F, 0.758F, 0.745F, 0.666F, 0.543F, 0.419F, 0.292F, 0.060F
    };
    /** How ragged the cold trail gets downstream. The burning core is not perturbed at all. */
    private static final float TURBULENCE = 0.30F;

    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);

    private static final class Parcel {
        final Vec3 origin;
        final Vec3 dir;
        /** Stable perpendicular, parallel-transported from the previous parcel to avoid twisting. */
        final Vec3 side;
        final Vec3 other;
        final float throttle;
        final boolean lit;
        final double clearance;
        /** Drives this parcel's turbulence and flicker so the plume never repeats. */
        final int seed;
        int age;

        Parcel(Vec3 origin, Vec3 dir, Vec3 side, Vec3 other,
               float throttle, boolean lit, double clearance, int seed) {
            this.seed = seed;
            this.origin = origin;
            this.dir = dir;
            this.side = side;
            this.other = other;
            this.throttle = throttle;
            this.lit = lit;
            this.clearance = clearance;
        }

        /** Drift speed in blocks per tick; reheat blows much harder than dry thrust. */
        double driftSpeed() {
            return (lit ? 0.34D : 0.15D) + 0.30D * throttle;
        }

        Vec3 positionAt(float partial) {
            double t = age + partial;
            double travel = Math.min(clearance, driftSpeed() * t);
            // Hot gas floats as it slows. Scaled by (1 - throttle) so a hard plume stays straight
            // and only a lazy idle plume visibly curls upward.
            double rise = 0.0075D * t * t * (1.0D - 0.75D * throttle);
            return origin.add(dir.scale(travel)).add(WORLD_UP.scale(rise));
        }

        float radiusAt(float partial) {
            double u = Math.min(1.0D, (age + partial) / (double) MAX_AGE);
            double r0 = 0.26D + 0.10D * throttle;
            // The trail is spent gas, lit or not: it just diffuses and keeps spreading. The
            // burning structure lives in buildCore, not here.
            return (float) (r0 * (1.0D + (lit ? 2.4D : 1.9D) * u));
        }
    }

    private final Deque<Parcel> parcels = new ArrayDeque<>();
    private Vec3 lastSide = null;
    private int idleTicks;
    private int nextSeed = 1;

    /** True once the trail has fully faded and can be dropped. */
    public boolean isDead() {
        return parcels.isEmpty() && idleTicks > MAX_AGE;
    }

    public boolean isEmpty() {
        return parcels.isEmpty();
    }

    /** Ages every parcel one tick and retires the expired ones. Call once per client tick. */
    public void tick() {
        Iterator<Parcel> it = parcels.iterator();
        while (it.hasNext()) {
            Parcel p = it.next();
            if (++p.age > MAX_AGE) {
                it.remove();
            }
        }
        idleTicks++;
    }

    /**
     * Emits one parcel at the nozzle.
     *
     * @param origin    world position of the nozzle mouth right now
     * @param dir       world-space exhaust direction (unit)
     * @param clearance clear distance ahead of the nozzle before terrain is hit
     */
    public void emit(Vec3 origin, Vec3 dir, float throttle, boolean lit, double clearance) {
        idleTicks = 0;

        // Parallel transport: re-orthogonalise the previous frame against the new direction rather
        // than rebuilding it from world up. Rebuilding every tick makes the tube spin about its own
        // axis whenever the aircraft rolls.
        Vec3 reference = lastSide;
        if (reference == null || Math.abs(reference.dot(dir)) > 0.999D) {
            reference = Math.abs(dir.y) > 0.9D ? new Vec3(1, 0, 0) : WORLD_UP;
        }
        Vec3 side = reference.subtract(dir.scale(reference.dot(dir)));
        if (side.lengthSqr() < 1.0E-6D) {
            side = Math.abs(dir.y) > 0.9D ? new Vec3(1, 0, 0) : WORLD_UP;
            side = side.subtract(dir.scale(side.dot(dir)));
        }
        side = side.normalize();
        Vec3 other = dir.cross(side).normalize();
        lastSide = side;

        parcels.addFirst(new Parcel(origin, dir, side, other, throttle, lit, clearance, nextSeed++));
        while (parcels.size() > MAX_SAMPLES) {
            parcels.removeLast();
        }
    }

    /**
     * Writes the plume surface into {@code consumer}, in world coordinates.
     *
     * <p>Drawn as concentric shells: a tight bright core and a wider dim halo. Because the render
     * type is additive, the overlap accumulates into a hot centre with a soft edge, which is much
     * closer to a real plume than a single opaque tube.
     */
    public void build(VertexConsumer consumer, PoseStack pose, float partialTick) {
        if (parcels.isEmpty()) {
            return;
        }
        Matrix4f matrix = pose.last().pose();
        Parcel head = parcels.peekFirst();

        // The burning flame is rigid and anchored to the nozzle. Building it from the emission
        // history made it bend and flex along the flight path like a hose — physically true of the
        // gas, but a real reheat flame is a stiff cone locked to the engine axis. Only the cold
        // trail behind it follows the aircraft's path.
        if (head != null && head.lit) {
            buildCore(consumer, matrix, head, partialTick);
        }

        if (parcels.size() >= 2) {
            Parcel[] arr = parcels.toArray(new Parcel[0]);
            // Cold trail: faint haze only. This is the part that bends and lags.
            buildShell(consumer, matrix, arr, partialTick, 1.0F, head != null && head.lit ? 0.14F : 0.40F);
        }
    }

    /**
     * The rigid burning core: a straight stack of six-sided frusta along the current nozzle axis,
     * with hard steps between stations and flat per-segment colour.
     */
    private void buildCore(VertexConsumer consumer, Matrix4f matrix, Parcel head, float partialTick) {
        int segments = CORE_PROFILE.length - 1;
        double r0 = 0.26D + 0.10D * head.throttle;
        double length = Math.min(head.clearance, 1.9D + 3.2D * head.throttle);
        if (length <= 0.05D) {
            return;
        }

        for (int i = 0; i < segments; i++) {
            double za = length * (i / (double) segments);
            double zb = length * ((i + 1) / (double) segments);
            double ra = r0 * CORE_PROFILE[i];
            double rb = r0 * CORE_PROFILE[i + 1];

            Vec3 pa = head.origin.add(head.dir.scale(za));
            Vec3 pb = head.origin.add(head.dir.scale(zb));

            // Flat colour per segment. A smooth gradient reads as a soft continuous fluid;
            // banding it makes the flame look like it has discrete structure.
            int col = coreColour(i, segments, head, partialTick);

            for (int k = 0; k < CORE_RING; k++) {
                double t0 = (k / (double) CORE_RING) * Math.PI * 2.0D;
                double t1 = ((k + 1) / (double) CORE_RING) * Math.PI * 2.0D;

                Vec3 a0 = plainRing(head, pa, ra, t0);
                Vec3 a1 = plainRing(head, pa, ra, t1);
                Vec3 b1 = plainRing(head, pb, rb, t1);
                Vec3 b0 = plainRing(head, pb, rb, t0);

                vertex(consumer, matrix, a0, col);
                vertex(consumer, matrix, a1, col);
                vertex(consumer, matrix, b1, col);
                vertex(consumer, matrix, b0, col);
            }
        }
    }

    private static Vec3 plainRing(Parcel p, Vec3 centre, double radius, double theta) {
        return centre
                .add(p.side.scale(Math.cos(theta) * radius))
                .add(p.other.scale(Math.sin(theta) * radius));
    }

    /** Banded colour down the core: white-blue at the nozzle, stepping to orange at the tip. */
    private static int coreColour(int segment, int segments, Parcel head, float partialTick) {
        float t = segment / (float) (segments - 1);
        float r = 0.62F + 0.38F * Math.min(1.0F, t * 1.7F);
        float g = 0.80F - 0.40F * t;
        float b = Math.max(0.0F, 1.0F - 1.9F * t);
        // Bright near the nozzle, dropping off hard so the flame has a definite end.
        float a = (float) Math.pow(1.0F - t, 1.6D) * (0.70F + 0.30F * head.throttle);
        // Slow global flicker only; per-vertex noise here would soften the hard edges.
        a *= 0.88F + 0.12F * hash(head.seed, 3);

        int ai = (int) (Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
        return (ai << 24) | ((int) (Math.min(1.0F, r) * 255.0F) << 16)
                | ((int) (Math.min(1.0F, g) * 255.0F) << 8) | (int) (Math.min(1.0F, b) * 255.0F);
    }

    private void buildShell(VertexConsumer consumer, Matrix4f matrix, Parcel[] arr,
                            float partialTick, float radiusScale, float alphaScale) {
        for (int i = 0; i < arr.length - 1; i++) {
            Parcel a = arr[i];
            Parcel b = arr[i + 1];

            Vec3 pa = a.positionAt(partialTick);
            Vec3 pb = b.positionAt(partialTick);
            float ra = a.radiusAt(partialTick) * radiusScale;
            float rb = b.radiusAt(partialTick) * radiusScale;

            int ca = colour(a, partialTick, alphaScale);
            int cb = colour(b, partialTick, alphaScale);
            if ((ca >>> 24) == 0 && (cb >>> 24) == 0) {
                continue;
            }

            for (int k = 0; k < RING; k++) {
                double t0 = (k / (double) RING) * Math.PI * 2.0D;
                double t1 = ((k + 1) / (double) RING) * Math.PI * 2.0D;

                Vec3 a0 = ringPoint(a, pa, ra, t0, k, partialTick);
                Vec3 a1 = ringPoint(a, pa, ra, t1, k + 1, partialTick);
                Vec3 b1 = ringPoint(b, pb, rb, t1, k + 1, partialTick);
                Vec3 b0 = ringPoint(b, pb, rb, t0, k, partialTick);

                vertex(consumer, matrix, a0, ca);
                vertex(consumer, matrix, a1, ca);
                vertex(consumer, matrix, b1, cb);
                vertex(consumer, matrix, b0, cb);
            }
        }
    }

    private static Vec3 ringPoint(Parcel p, Vec3 centre, float radius, double theta, int k,
                                  float partial) {
        // Perturb each ring vertex so the cross-section is not a perfect circle. The amount grows
        // with age: the jet leaves the nozzle clean and breaks up as it entrains air, which is what
        // turns a smooth tube into something that reads as burning gas.
        float u = Math.min(1.0F, (p.age + partial) / (float) MAX_AGE);
        float amount = TURBULENCE * u * u;
        float n = (hash(p.seed, k) - 0.5F) * 2.0F;
        float r = radius * (1.0F + amount * n);
        return centre
                .add(p.side.scale(Math.cos(theta) * r))
                .add(p.other.scale(Math.sin(theta) * r));
    }

    /** Cheap deterministic hash in 0..1, so the same parcel always jitters the same way. */
    private static float hash(int seed, int k) {
        int h = seed * 374761393 + k * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0F;
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 v, int argb) {
        consumer.addVertex(matrix, (float) v.x, (float) v.y, (float) v.z).setColor(argb);
    }

    /**
     * Colour by parcel age: a blue-white core cooling through orange, or a faint grey haze when the
     * afterburner is not lit. Alpha is what shapes the plume's fade, and it is squared so the tail
     * dies away smoothly instead of ending in a hard edge.
     */
    private static int colour(Parcel p, float partialTick, float alphaScale) {
        float t = Math.min(1.0F, (p.age + partialTick) / (float) MAX_AGE);
        // Squaring the fade killed the plume before it ever cooled to orange; a gentler
        // curve keeps the warm section actually visible.
        float fade = (float) Math.pow(1.0F - t, 1.4D);

        float r;
        float g;
        float b;
        float a;
        if (p.lit) {
            // Cooling exhaust behind the flame: warm grey, never bright. Anything luminous here
            // competes with the rigid core and drags the silhouette back toward looking fluid.
            r = 1.0F;
            g = 0.80F - 0.18F * t;
            b = 0.62F - 0.30F * t;
            a = fade * 0.16F * (0.4F + 0.6F * p.throttle);
            a *= 0.80F + 0.20F * hash(p.seed, 7);
        } else {
            // Dry thrust is nearly invisible: just enough hot haze to catch the light.
            r = 1.0F;
            g = 0.86F;
            b = 0.72F;
            a = fade * 0.10F * (0.4F + 0.6F * p.throttle);
        }
        a *= alphaScale;

        int ai = (int) (Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
        int ri = (int) (Math.min(1.0F, r) * 255.0F);
        int gi = (int) (Math.min(1.0F, g) * 255.0F);
        int bi = (int) (Math.min(1.0F, b) * 255.0F);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }
}
