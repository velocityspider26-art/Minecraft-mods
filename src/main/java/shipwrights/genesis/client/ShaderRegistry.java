package shipwrights.genesis.client;

/**
 * Placeholder for Genesis' former Lodestone-driven GLSL shader registry.
 *
 * <p>The bespoke sun / planet / atmosphere / black-hole / wormhole shaders were built on the
 * Minecraft 1.20 + Lodestone 1.20 shader API, which was rewritten for the 1.21 core-shader
 * pipeline. The celestial bodies are now drawn through the vanilla pipeline
 * ({@link shipwrights.genesis.space.renderer.SimpleBillboardCelestialRenderer}), so this class only
 * retains the small hooks other client code still calls. Re-introducing the procedural shaders on
 * 1.21 core shaders is tracked as follow-up work in the porting report.</p>
 */
public final class ShaderRegistry {

    private ShaderRegistry() {}

    /** Previously evicted cached per-texture planet render types; now a no-op. */
    public static void clearTexturedPlanetRenderTypes() {
        // no cached render types in the vanilla-pipeline renderer
    }
}
