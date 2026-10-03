package dev.velocityspider.caw.compat;

import dev.velocityspider.caw.CreateAerialWarfare;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * Runtime-only Sable bridge so CAW can compile without bundling Sable as a hard dependency.
 * If Sable is installed, thrust is applied at the actual nozzle point on the sub-level rigid body.
 */
public final class SablePhysicsBridge {
    private static final String SERVER_SUB_LEVEL = "dev.ryanhcode.sable.sublevel.ServerSubLevel";
    private static final String RIGID_BODY_HANDLE = "dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle";

    private static boolean lookupAttempted;
    private static boolean available;

    private static Class<?> serverSubLevelClass;
    private static Method rigidBodyOf;
    private static Method applyImpulseAtPoint;

    private SablePhysicsBridge() {}

    public static boolean applyThrustImpulse(
            Level level,
            BlockPos nozzlePos,
            Direction localThrustDirection,
            double impulseNewtonSeconds
    ) {
        if (impulseNewtonSeconds <= 0.0) {
            return false;
        }

        ensureLookup();

        if (!available || !serverSubLevelClass.isInstance(level)) {
            return false;
        }

        try {
            Object handle = rigidBodyOf.invoke(null, level);
            if (handle == null) {
                return false;
            }

            Vec3 localPoint = Vec3.atCenterOf(nozzlePos);
            Vec3 localDirection = Vec3.atLowerCornerOf(localThrustDirection.getNormal());
            Vec3 localImpulse = localDirection.scale(impulseNewtonSeconds);

            applyImpulseAtPoint.invoke(handle, localPoint, localImpulse);
            return true;
        } catch (ReflectiveOperationException ex) {
            CreateAerialWarfare.LOGGER.warn("CAW lost Sable thrust integration at runtime", ex);
            available = false;
            return false;
        }
    }

    private static synchronized void ensureLookup() {
        if (lookupAttempted) {
            return;
        }
        lookupAttempted = true;

        try {
            serverSubLevelClass = Class.forName(SERVER_SUB_LEVEL);
            Class<?> rigidBodyHandleClass = Class.forName(RIGID_BODY_HANDLE);

            rigidBodyOf = rigidBodyHandleClass.getMethod("of", serverSubLevelClass);
            applyImpulseAtPoint = rigidBodyHandleClass.getMethod(
                    "applyImpulseAtPoint",
                    Vec3.class,
                    Vec3.class
            );

            available = true;
            CreateAerialWarfare.LOGGER.info("CAW detected compatible Sable physics; jet thrust bridge enabled");
        } catch (ReflectiveOperationException ex) {
            available = false;
            CreateAerialWarfare.LOGGER.info("CAW did not detect compatible Sable physics; thrust bridge dormant");
        }
    }
}
