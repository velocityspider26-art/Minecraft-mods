package shipwrights.genesis.teleportation.integration;

import net.neoforged.bus.api.Event;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.space.Celestial;

/// Fired when a construct is denied entry to a planet due to addon or datapack functionality
public final class TeleportDisallowedEvent extends Event {
    private final AeronauticsConstruct construct;
    private final Celestial celestial;

    public TeleportDisallowedEvent(AeronauticsConstruct construct, Celestial celestial) {
        this.construct = construct;
        this.celestial = celestial;
    }

    /// construct that tried to teleport to a Celestial
    public AeronauticsConstruct construct() {
        return construct;
    }

    /// Celestial that the construct tried to teleport to
    public Celestial celestial() {
        return celestial;
    }
}
