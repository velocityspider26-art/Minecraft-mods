package com.example.examplemod.content.thruster;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Bridges our thrusters to Create Sable's rigid-body physics <b>by reflection</b>, so the mod compiles
 * and runs whether or not Sable is installed.
 *
 * <p>It mirrors what Propulsion does with hard Sable types:
 * <ol>
 *   <li>{@code Sable.HELPER.getContaining(level, pos)} to find the {@code SubLevel} a thruster sits on,</li>
 *   <li>if it's a {@code ServerSubLevel}, {@code getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get())},</li>
 *   <li>then {@code applyAndRecordPointForce(pointLocal, impulseLocal)} — Sable turns the off-centre
 *       application point into torque automatically.</li>
 * </ol>
 *
 * <p>Unlike Propulsion (which applies force from Sable's sub-tick physics callback via a Sable
 * interface we can't implement without the classpath), we apply the impulse once per game tick with
 * {@code impulse = force * dt}. That is a clean adaptation that needs no compile-time Sable. All calls
 * are guarded; any failure disables the adapter so a dedicated server without Sable never crashes.</p>
 *
 * <p>Because it is reflective and cannot be exercised here without a running Sable ship, the exact
 * field/method names are the ones Propulsion uses on 1.21.1 and should be verified in-engine.</p>
 */
public final class SableThrustAdapter {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean initialised;
    private static boolean available;

    private static Object sableHelper;                 // Sable.HELPER instance
    private static Method mGetContaining;              // helper.getContaining(Level, BlockPos) -> SubLevel
    private static Class<?> serverSubLevelClass;       // ServerSubLevel
    private static Method mGetOrCreateForceGroup;      // ServerSubLevel.getOrCreateQueuedForceGroup(Object) -> QueuedForceGroup
    private static Method mApplyPointForce;            // QueuedForceGroup.applyAndRecordPointForce(Vector3d, Vector3d)
    private static Object propulsionForceGroup;        // resolved ForceGroups.PROPULSION value

    private SableThrustAdapter() {
    }

    /** Whether Sable was detected and the reflective bindings resolved. */
    public static boolean isAvailable() {
        ensureInit();
        return available;
    }

    private static synchronized void ensureInit() {
        if (initialised) {
            return;
        }
        initialised = true;
        try {
            Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
            Field helperField = sableClass.getField("HELPER");
            sableHelper = helperField.get(null);

            mGetContaining = findMethod(sableHelper.getClass(), "getContaining", Level.class, BlockPos.class);

            serverSubLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.ServerSubLevel");
            mGetOrCreateForceGroup = findMethodByName(serverSubLevelClass, "getOrCreateQueuedForceGroup", 1);

            Class<?> forceGroupsClass = Class.forName("dev.ryanhcode.sable.api.physics.force.ForceGroups");
            propulsionForceGroup = resolveForceGroup(forceGroupsClass, "PROPULSION");

            // applyAndRecordPointForce(Vector3d, Vector3d) lives on the queued force group; resolve lazily
            // from the first instance we get, but pre-find on the declared return type if possible.
            available = mGetContaining != null && mGetOrCreateForceGroup != null && propulsionForceGroup != null;
            if (available) {
                LOGGER.info("Sable detected: thrusters will apply physics force via reflection.");
            }
        } catch (Throwable t) {
            available = false;
            LOGGER.info("Sable not present ({}). Thrusters will render/consume fuel but apply no physics force.",
                    t.getClass().getSimpleName());
        }
    }

    /**
     * Applies a thrust impulse to the Sable body a thruster is mounted on, at the thruster's position
     * so torque is produced for off-centre mounts. No-ops if Sable is absent or the block is not on a
     * physics object.
     *
     * @param level     the (sub)level the block entity lives in
     * @param pos       the block position (sub-level local)
     * @param pointX/Y/Z local application point (block-space, incl. nozzle offset)
     * @param forceX/Y/Z thrust direction * magnitude
     * @param dt        time step (seconds) to convert force to impulse
     * @return true if a force was applied
     */
    public static boolean applyThrust(Level level, BlockPos pos,
                                      double pointX, double pointY, double pointZ,
                                      double forceX, double forceY, double forceZ, double dt) {
        ensureInit();
        if (!available) {
            return false;
        }
        try {
            Object subLevel = mGetContaining.invoke(sableHelper, level, pos);
            if (subLevel == null || !serverSubLevelClass.isInstance(subLevel)) {
                return false; // not on a server-side physics object
            }
            Object forceGroup = mGetOrCreateForceGroup.invoke(subLevel, propulsionForceGroup);
            if (forceGroup == null) {
                return false;
            }
            if (mApplyPointForce == null) {
                mApplyPointForce = findMethodByName(forceGroup.getClass(), "applyAndRecordPointForce", 2);
                if (mApplyPointForce == null) {
                    available = false;
                    return false;
                }
            }
            Vector3d point = new Vector3d(pointX, pointY, pointZ);
            Vector3d impulse = new Vector3d(forceX * dt, forceY * dt, forceZ * dt);
            mApplyPointForce.invoke(forceGroup, point, impulse);
            return true;
        } catch (Throwable t) {
            // Disable on unexpected shape mismatch so we don't spam or crash.
            available = false;
            LOGGER.warn("Disabling Sable thrust adapter after error: {}", t.toString());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object resolveForceGroup(Class<?> forceGroupsClass, String name) throws Exception {
        Field field = forceGroupsClass.getField(name);
        Object ro = field.get(null);
        if (ro instanceof Supplier<?> supplier) {
            return supplier.get();
        }
        try {
            Method get = ro.getClass().getMethod("get");
            return get.invoke(ro);
        } catch (NoSuchMethodException e) {
            return ro; // already the value
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... params) {
        try {
            Method m = owner.getMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            return findMethodByName(owner, name, params.length);
        }
    }

    private static Method findMethodByName(Class<?> owner, String name, int paramCount) {
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            for (Method m : c.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }
}
