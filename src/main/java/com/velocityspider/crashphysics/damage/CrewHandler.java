package com.velocityspider.crashphysics.damage;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.physics.CrewLoads;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

/**
 * What a crash does to the people and animals riding a vehicle.
 * <p>
 * Everyone aboard feels the vehicle's deceleration at their own position (so the tail of a spinning wreck is worse
 * than the middle). Seated riders tolerate far more than anyone standing loose, and loose riders keep flying when the
 * vehicle stops under them.
 */
public final class CrewHandler {

    public static final ResourceKey<DamageType> G_FORCE = ResourceKey.create(Registries.DAMAGE_TYPE, CrashPhysics.id("g_force"));

    private CrewHandler() {
    }

    /**
     * @param peakG               worst deceleration of the vehicle this tick [g]
     * @param linearAcceleration  proper acceleration of the centre of mass [m/s²], world space
     * @param angularAcceleration [rad/s²], world space
     * @param angularVelocity     [rad/s], world space
     * @param tickStartVelocity   velocity of the vehicle when the tick started [m/s]
     * @param currentVelocity     velocity of the vehicle now [m/s]
     */
    public static void apply(final ServerLevel level, final ServerSubLevel vehicle, final Pose3dc pose, final double peakG,
                             final Vector3dc linearAcceleration, final Vector3dc angularAcceleration, final Vector3dc angularVelocity,
                             final Vector3dc tickStartVelocity, final Vector3dc currentVelocity) {
        final double seatedTolerance = CrashConfig.SEATED_TOLERANCE_G.getAsDouble();
        final double standingTolerance = CrashConfig.STANDING_TOLERANCE_G.getAsDouble();
        final double damageScale = CrashConfig.CREW_DAMAGE_SCALE.getAsDouble();
        final boolean throwLoose = CrashConfig.THROW_LOOSE_CREW.getAsBoolean();

        final Vector3d deltaV = new Vector3d(currentVelocity).sub(tickStartVelocity);
        if (peakG < Math.min(seatedTolerance, standingTolerance) * 0.5 && deltaV.lengthSquared() < 9.0) {
            return;
        }

        final AABB area = vehicle.boundingBox().toMojang().inflate(3.0);
        if (!(area.getXsize() < 1024.0 && area.getYsize() < 1024.0 && area.getZsize() < 1024.0)) {
            // A corrupt or absurd bounding box would make the entity query scan millions of chunks
            return;
        }
        final List<LivingEntity> riders = level.getEntitiesOfClass(LivingEntity.class, area, entity -> {
            final SubLevel riding = Sable.HELPER.getTrackingOrVehicleSubLevel(entity);
            return riding == vehicle && entity.isAlive() && !entity.isSpectator();
        });
        if (riders.isEmpty()) {
            return;
        }

        final DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(G_FORCE));
        final Vector3dc com = pose.position();
        final Vector3d r = new Vector3d();
        final Vector3d a = new Vector3d();
        final Vector3d tmp = new Vector3d();

        for (final LivingEntity rider : riders) {
            final Vec3 position = rider.position();
            r.set(position.x - com.x(), position.y - com.y(), position.z - com.z());

            // a = a_com + α × r + ω × (ω × r)
            a.set(linearAcceleration);
            a.add(angularAcceleration.cross(r, tmp));
            angularVelocity.cross(r, tmp);
            a.add(angularVelocity.cross(tmp, tmp));

            final double g = CrewLoads.toG(a.x, a.y, a.z);
            final boolean seated = rider.getVehicle() != null;
            final double damage = CrewLoads.damage(g, seated ? seatedTolerance : standingTolerance, damageScale);

            if (damage >= 0.5) {
                rider.hurt(source, (float) damage);
            }

            // Someone standing loose doesn't stop with the vehicle
            if (!seated && throwLoose && deltaV.lengthSquared() > 9.0) {
                final double scale = 0.8 / 20.0;
                Vec3 kick = new Vec3(-deltaV.x * scale, -deltaV.y * scale, -deltaV.z * scale);
                final double maxKick = 2.5;
                if (kick.length() > maxKick) {
                    kick = kick.normalize().scale(maxKick);
                }
                rider.setDeltaMovement(rider.getDeltaMovement().add(kick));
                rider.hurtMarked = true;
            }
        }
    }
}
