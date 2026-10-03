package dev.velocityspider.caw.compat;

import dev.velocityspider.caw.CreateAerialWarfare;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runtime Sable bridge with no hard Sable dependency.
 *
 * CAW mirrors Sable's own BlockEntitySubLevelPropellerActor path:
 * ServerSubLevel -> PROPULSION queued-force group -> applyAndRecordPointForce.
 * The direct RigidBodyHandle path remains as a fallback.
 */
public final class SablePhysicsBridge {
    private static final String SERVER_SUB_LEVEL = "dev.ryanhcode.sable.sublevel.ServerSubLevel";
    private static final String FORCE_GROUP = "dev.ryanhcode.sable.api.physics.force.ForceGroup";
    private static final String FORCE_GROUPS = "dev.ryanhcode.sable.api.physics.force.ForceGroups";
    private static final String QUEUED_FORCE_GROUP = "dev.ryanhcode.sable.api.physics.force.QueuedForceGroup";
    private static final String RIGID_BODY_HANDLE = "dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle";
    private static final String REGISTRY_OBJECT = "foundry.veil.platform.registry.RegistryObject";

    private static boolean lookupAttempted;
    private static boolean queuedForceAvailable;
    private static boolean handleFallbackAvailable;

    private static Class<?> serverSubLevelClass;
    private static Object propulsionForceGroup;
    private static Method getOrCreateQueuedForceGroup;
    private static Method applyAndRecordPointForce;
    private static Method rigidBodyOf;
    private static Method applyImpulseAtPoint;

    private static boolean loggedWrongLevel;
    private static boolean loggedSuccessfulThrust;

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

        if (serverSubLevelClass == null || !serverSubLevelClass.isInstance(level)) {
            if (!loggedWrongLevel) {
                loggedWrongLevel = true;
                CreateAerialWarfare.LOGGER.warn(
                        "CAW jet is ticking outside a Sable ServerSubLevel; physics thrust cannot be applied here"
                );
            }
            return false;
        }

        Vector3d localPoint = new Vector3d(
                nozzlePos.getX() + 0.5,
                nozzlePos.getY() + 0.5,
                nozzlePos.getZ() + 0.5
        );
        Vector3d localImpulse = new Vector3d(
                localThrustDirection.getStepX(),
                localThrustDirection.getStepY(),
                localThrustDirection.getStepZ()
        ).mul(impulseNewtonSeconds);

        if (queuedForceAvailable) {
            try {
                Object queue = getOrCreateQueuedForceGroup.invoke(level, propulsionForceGroup);
                if (queue != null) {
                    applyAndRecordPointForce.invoke(queue, localPoint, localImpulse);
                    logFirstSuccess("queued propulsion force");
                    return true;
                }
            } catch (ReflectiveOperationException ex) {
                queuedForceAvailable = false;
                CreateAerialWarfare.LOGGER.error(
                        "CAW Sable queued propulsion bridge failed; trying rigid-body fallback",
                        ex
                );
            }
        }

        if (handleFallbackAvailable) {
            try {
                Object handle = rigidBodyOf.invoke(null, level);
                if (handle != null) {
                    Vec3 point = new Vec3(localPoint.x, localPoint.y, localPoint.z);
                    Vec3 impulse = new Vec3(localImpulse.x, localImpulse.y, localImpulse.z);
                    applyImpulseAtPoint.invoke(handle, point, impulse);
                    logFirstSuccess("RigidBodyHandle fallback");
                    return true;
                }
            } catch (ReflectiveOperationException ex) {
                handleFallbackAvailable = false;
                CreateAerialWarfare.LOGGER.error("CAW Sable rigid-body fallback failed", ex);
            }
        }

        return false;
    }

    private static void logFirstSuccess(String path) {
        if (!loggedSuccessfulThrust) {
            loggedSuccessfulThrust = true;
            CreateAerialWarfare.LOGGER.info("CAW jet thrust is reaching Sable through {}", path);
        }
    }

    private static synchronized void ensureLookup() {
        if (lookupAttempted) {
            return;
        }
        lookupAttempted = true;

        try {
            serverSubLevelClass = Class.forName(SERVER_SUB_LEVEL);
        } catch (ClassNotFoundException ex) {
            CreateAerialWarfare.LOGGER.warn("CAW could not find Sable ServerSubLevel; physics integration disabled");
            return;
        }

        try {
            Class<?> forceGroupClass = Class.forName(FORCE_GROUP);
            Class<?> forceGroupsClass = Class.forName(FORCE_GROUPS);
            Class<?> queuedForceGroupClass = Class.forName(QUEUED_FORCE_GROUP);
            Class<?> registryObjectClass = Class.forName(REGISTRY_OBJECT);

            Field propulsionField = forceGroupsClass.getField("PROPULSION");
            Object registryObject = propulsionField.get(null);
            Method registryGet = registryObjectClass.getMethod("get");
            propulsionForceGroup = registryGet.invoke(registryObject);

            getOrCreateQueuedForceGroup = serverSubLevelClass.getMethod(
                    "getOrCreateQueuedForceGroup",
                    forceGroupClass
            );
            applyAndRecordPointForce = queuedForceGroupClass.getMethod(
                    "applyAndRecordPointForce",
                    Vector3dc.class,
                    Vector3dc.class
            );

            queuedForceAvailable = propulsionForceGroup != null;
            if (queuedForceAvailable) {
                CreateAerialWarfare.LOGGER.info("CAW detected Sable queued propulsion API");
            }
        } catch (ReflectiveOperationException ex) {
            queuedForceAvailable = false;
            CreateAerialWarfare.LOGGER.warn(
                    "CAW could not bind Sable queued propulsion API; will try fallback",
                    ex
            );
        }

        try {
            Class<?> rigidBodyHandleClass = Class.forName(RIGID_BODY_HANDLE);
            rigidBodyOf = rigidBodyHandleClass.getMethod("of", serverSubLevelClass);
            applyImpulseAtPoint = rigidBodyHandleClass.getMethod(
                    "applyImpulseAtPoint",
                    Vec3.class,
                    Vec3.class
            );
            handleFallbackAvailable = true;
        } catch (ReflectiveOperationException ex) {
            handleFallbackAvailable = false;
            CreateAerialWarfare.LOGGER.warn("CAW could not bind Sable RigidBodyHandle fallback", ex);
        }
    }
}
