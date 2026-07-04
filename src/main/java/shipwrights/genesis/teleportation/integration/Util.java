package shipwrights.genesis.teleportation.integration;

import net.minecraft.server.level.ServerLevel;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsContraptionLookup;

import java.util.List;

public class Util {
    /** Constructs in the level ordered largest-first (see {@link AeronauticsContraptionLookup#getSortedConstructs}). */
    public static List<AeronauticsConstruct> getSortedConstructs(ServerLevel level) {
        return AeronauticsContraptionLookup.getSortedConstructs(level);
    }
}
