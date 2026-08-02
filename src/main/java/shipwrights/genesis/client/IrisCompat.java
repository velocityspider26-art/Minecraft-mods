package shipwrights.genesis.client;

import shipwrights.genesis.GenesisMod;

import java.lang.reflect.Method;

/**
 * Tells us whether a shader pack has taken over the render pipeline.
 *
 * <p>Iris (and its Forge-family port, Oculus) does not extend the vanilla
 * pipeline — it replaces it. There is no supported way to hand a shader pack
 * our own core shaders and have them survive, so "make our shaders work under
 * Iris" is not a thing that can be built. What can be built is knowing when a
 * pack is active, so our passes stand down instead of fighting it.</p>
 *
 * <p>The rule the rest of the mod follows: effects that write to the shared
 * framebuffer or drive their own post-processing chain check
 * {@link #isShaderPackActive()} first and skip when it returns true. The shader
 * pack then draws the scene its own way, and the player gets their pack looking
 * correct rather than our effects looking correct on top of a broken scene.
 * Geometry we render normally (planets, the star) is unaffected and still shows
 * up — it is only the pipeline-level passes that have to yield.</p>
 *
 * <p>Detection is reflective and re-checked per call, because a player can
 * enable, switch, or disable a pack at any point without restarting.</p>
 */
public final class IrisCompat {
    /** Iris 1.6+ and current Oculus both publish this. */
    private static final String[] API_CLASSES = {
            "net.irisshaders.iris.api.v0.IrisApi",
            "net.coderbot.iris.api.v0.IrisApi",
    };

    private static final Object API;
    private static final Method IN_USE;

    static {
        Object api = null;
        Method inUse = null;
        for (String className : API_CLASSES) {
            try {
                Class<?> irisApi = Class.forName(className);
                api = irisApi.getMethod("getInstance").invoke(null);
                inUse = irisApi.getMethod("isShaderPackInUse");
                GenesisMod.LOGGER.info("Shader pack support active via {}", className);
                break;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Try the next candidate; absence is the normal case.
            }
        }
        if (api == null) {
            GenesisMod.LOGGER.info("No shader pack loader present; Genesis will use its own render passes");
        }
        API = api;
        IN_USE = inUse;
    }

    private IrisCompat() {
    }

    /** Whether a shader loader is installed at all, pack active or not. */
    public static boolean isPresent() {
        return API != null && IN_USE != null;
    }

    /**
     * Whether a shader pack is driving the pipeline right now.
     *
     * <p>False when no loader is installed, and false when one is installed but
     * the player is on internal shaders — in both cases our passes are safe to
     * run. Any failure is reported as false for the same reason: losing an
     * effect is better than losing the frame.</p>
     */
    public static boolean isShaderPackActive() {
        if (!isPresent()) {
            return false;
        }
        try {
            return (Boolean) IN_USE.invoke(API);
        } catch (ReflectiveOperationException | ClassCastException error) {
            return false;
        }
    }
}
