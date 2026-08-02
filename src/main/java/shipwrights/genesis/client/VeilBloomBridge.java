package shipwrights.genesis.client;

import shipwrights.genesis.GenesisMod;

/**
 * Runtime bridge to Veil's bloom target. Sable bundles Veil at runtime but
 * does not expose its classes on Genesis's compile classpath.
 */
public final class VeilBloomBridge {
    private static final Object BLOOM_SHARD;
    private static final java.lang.reflect.Method SETUP;
    private static final java.lang.reflect.Method CLEAR;

    static {
        Object shard = null;
        java.lang.reflect.Method setup = null;
        java.lang.reflect.Method clear = null;
        try {
            Class<?> renderSystem = Class.forName("foundry.veil.api.client.render.VeilRenderSystem");
            shard = renderSystem.getField("BLOOM_SHARD").get(null);
            setup = shard.getClass().getMethod("setupRenderState");
            clear = shard.getClass().getMethod("clearRenderState");
        } catch (ReflectiveOperationException | LinkageError error) {
            GenesisMod.LOGGER.warn("Veil bloom is unavailable; emissive effects will use additive rendering only", error);
        }
        BLOOM_SHARD = shard;
        SETUP = setup;
        CLEAR = clear;
    }

    private VeilBloomBridge() {
    }

    /**
     * Whether a bloom pass can render at all. Callers should check this BEFORE
     * building bloom geometry — otherwise the mesh is assembled and thrown away.
     */
    public static boolean isAvailable() {
        return BLOOM_SHARD != null && SETUP != null;
    }

    public static boolean begin() {
        if (BLOOM_SHARD == null || SETUP == null) {
            return false;
        }
        try {
            SETUP.invoke(BLOOM_SHARD);
            return true;
        } catch (ReflectiveOperationException error) {
            return false;
        }
    }

    public static void end() {
        if (BLOOM_SHARD == null || CLEAR == null) {
            return;
        }
        try {
            CLEAR.invoke(BLOOM_SHARD);
        } catch (ReflectiveOperationException ignored) {
            // The caller restores the normal render state after every pass.
        }
    }
}
