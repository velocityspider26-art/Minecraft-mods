package shipwrights.genesis.compat.aeronautics;

import net.neoforged.fml.ModList;

/**
 * Entry point / feature-detection facade for the Create Aeronautics integration layer.
 *
 * <p>Genesis was originally built on Valkyrien Skies. This package is the clean compatibility
 * layer that re-implements all of the "moving vehicle / physics" behaviour on top of
 * <a href="https://modrinth.com/mod/create-aeronautics">Create Aeronautics</a> and its underlying
 * <a href="https://modrinth.com/mod/sable">Sable</a> sub-level physics engine.</p>
 *
 * <p>The rest of the mod only talks to the {@code Aeronautics*} helpers here; nothing outside this
 * package references a Sable or Create type directly, so the platform can be swapped again in the
 * future by re-implementing this one package.</p>
 */
public final class AeronauticsCompat {

    public static final String SABLE_ID = "sable";
    public static final String CREATE_ID = "create";
    public static final String AERONAUTICS_ID = "aeronautics";

    private static Boolean loaded = null;

    private AeronauticsCompat() {}

    /** @return true when the Create Aeronautics platform (Sable) is present. */
    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded(SABLE_ID);
        }
        return loaded;
    }

    /** @return true when the full Create Aeronautics content mod is present. */
    public static boolean isAeronauticsLoaded() {
        return ModList.get().isLoaded(AERONAUTICS_ID);
    }

    public static boolean isCreateLoaded() {
        return ModList.get().isLoaded(CREATE_ID);
    }
}
