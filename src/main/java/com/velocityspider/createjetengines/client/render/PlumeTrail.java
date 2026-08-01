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
    /** Vertices per ring. Eight matches the octagonal engine geometry and is cheap. */
    private static final int RING = 8;

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
        int age;

        Parcel(Vec3 origin, Vec3 dir, Vec3 side, Vec3 other,
               float throttle, boolean lit, double clearance) {
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
            if (lit) {
                // A reheat core is a spindle: it bulges just past the nozzle as the flow expands,
                // then necks back down to a point. Letting it widen monotonically made it read as
                // a fat cone rather than a flame.
                return (float) (r0 * (1.0D + 0.9D * u) * Math.pow(1.0D - u, 0.45D));
            }
            // Dry exhaust has nothing burning in it; it just diffuses and keeps spreading.
            return (float) (r0 * (1.0D + 1.9D * u));
        }
    }

    private final Deque<Parcel> parcels = new ArrayDeque<>();
    private Vec3 lastSide = null;
    private int idleTicks;

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

        parcels.addFirst(new Parcel(origin, dir, side, other, throttle, lit, clearance));
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
        if (parcels.size() < 2) {
            return;
        }
        Matrix4f matrix = pose.last().pose();
        Parcel[] arr = parcels.toArray(new Parcel[0]);

        // inner core, then outer halo
        buildShell(consumer, matrix, arr, partialTick, 0.55F, 1.0F);
        buildShell(consumer, matrix, arr, partialTick, 1.0F, 0.40F);
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

                Vec3 a0 = ringPoint(a, pa, ra, t0);
                Vec3 a1 = ringPoint(a, pa, ra, t1);
                Vec3 b1 = ringPoint(b, pb, rb, t1);
                Vec3 b0 = ringPoint(b, pb, rb, t0);

                vertex(consumer, matrix, a0, ca);
                vertex(consumer, matrix, a1, ca);
                vertex(consumer, matrix, b1, cb);
                vertex(consumer, matrix, b0, cb);
            }
        }
    }

    private static Vec3 ringPoint(Parcel p, Vec3 centre, float radius, double theta) {
        return centre
                .add(p.side.scale(Math.cos(theta) * radius))
                .add(p.other.scale(Math.sin(theta) * radius));
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
            r = 0.55F + 0.45F * Math.min(1.0F, t * 1.8F);
            g = 0.74F - 0.34F * t;
            // Blue has to collapse early and hard. Falling off as t^2 leaves the middle of the
            // plume sitting at roughly equal red and blue, which reads as mauve, not flame.
            b = Math.max(0.0F, 1.0F - 1.7F * t);
            a = fade * (0.55F + 0.45F * p.throttle);
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
