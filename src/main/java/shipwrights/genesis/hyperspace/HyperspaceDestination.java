package shipwrights.genesis.hyperspace;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.teleportation.SpaceTravelManager;

import java.util.Locale;

/**
 * Hyperdrive destinations are celestials. A jump drops out of hyperspace in the
 * SPACE dimension on a standoff ring just outside the destination's atmosphere
 * entry zone — from there you see the body filling the sky and fly (or fall)
 * the last stretch in, so arrival always reads as approach, never a cut.
 *
 * The destination's position in hyperspace is its celestial-space position
 * compressed by the subspace coordinate scale, so transit is real flight:
 * farther bodies genuinely take longer to reach.
 */
public enum HyperspaceDestination {
    MOON(0, "moon", "Moon",
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "moon")),
    /**
     * Staging point for future Dyson sphere/cube construction. The sun's
     * gravity well plus station anchoring make parked ships hold position.
     */
    SUN(1, "sun", "Sun",
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "sun")),
    /** Innermost planet, a scorched rocky world just outside the sun's glare. */
    MERCURY(2, "mercury", "Mercury",
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "mercury"));

    private final int buttonId;
    private final String id;
    private final String displayName;
    private final ResourceLocation celestialId;

    HyperspaceDestination(int buttonId, String id, String displayName, ResourceLocation celestialId) {
        this.buttonId = buttonId;
        this.id = id;
        this.displayName = displayName;
        this.celestialId = celestialId;
    }

    public int buttonId() {
        return buttonId;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Every hyperdrive jump exits into the space dimension. */
    public ResourceKey<Level> dimension() {
        return ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM);
    }

    @Nullable
    public Celestial celestial(ServerLevel anyLevel) {
        return GenesisMod.getCelestialRegistry(anyLevel).get(celestialId);
    }

    /** Where this destination sits inside the hyperspace dimension, or null if unknown. */
    @Nullable
    public Vec3 subspaceTarget(ServerLevel subspace) {
        Celestial celestial = celestial(subspace);
        if (celestial == null) {
            return null;
        }
        Vector3dc position = celestial.getPosition(GenesisMod.getTicks(subspace),
                GenesisMod.getCelestialRegistry(subspace));
        double compression = subspace.dimensionType().coordinateScale();
        return new Vec3(position.x() / compression, 0.0, position.z() / compression);
    }

    /**
     * Arrival point in the space dimension: on a ring outside the celestial's
     * atmosphere-entry zone, in the direction the traveler came from.
     */
    public Vec3 resolveArrival(ServerLevel spaceLevel, double exactX, double exactZ) {
        Celestial celestial = celestial(spaceLevel);

        // Anchor the arrival on the destination celestial's actual position in
        // space. The direction toward the approach point is NORMALIZED and the
        // distance is a fixed standoff, so the arrival is ALWAYS within
        // `standoff` of the celestial — a garbage approach coordinate (e.g. a
        // Sable shadow-region plot value) can never leak into the arrival.
        Vector3d center;
        double half;
        boolean visitable;
        if (celestial != null) {
            Vector3dc c = celestial.getPosition(GenesisMod.getTicks(spaceLevel),
                    GenesisMod.getCelestialRegistry(spaceLevel));
            center = new Vector3d(c.x(), c.y(), c.z());
            half = celestial.getActualSize() / 2.0;
            visitable = celestial.type().isVisitable();
        } else {
            // Should not happen, but never fall back to the raw approach coords.
            center = new Vector3d(0.0, 0.0, 0.0);
            half = 200.0;
            visitable = false;
        }

        Vector3d outward = new Vector3d(exactX - center.x(), 0.0, exactZ - center.z());
        if (!(outward.length() > 1.0E-6) || !Double.isFinite(outward.length())) {
            outward.set(1.0, 0.0, 0.0);
        } else {
            outward.normalize();
        }

        // Stars are not landable and pull hard: drop out well beyond the normal
        // ring so arriving at the sun is a safe parking orbit, not a plunge.
        double standoff = SpaceTravelManager.arrivalRingRadius(half) * (visitable ? 1.0 : 1.6);
        return new Vec3(center.x() + outward.x * standoff, 96.0, center.z() + outward.z * standoff);
    }

    public static HyperspaceDestination byButtonId(int buttonId) {
        for (HyperspaceDestination destination : values()) {
            if (destination.buttonId == buttonId) {
                return destination;
            }
        }
        return null;
    }

    public static HyperspaceDestination byId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        String normalized = id.toLowerCase(Locale.ROOT);
        for (HyperspaceDestination destination : values()) {
            if (destination.id.equals(normalized)) {
                return destination;
            }
        }
        return null;
    }
}
