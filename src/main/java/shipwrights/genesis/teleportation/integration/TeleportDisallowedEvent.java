package shipwrights.genesis.teleportation.integration;

import net.neoforged.bus.api.Event;
import shipwrights.genesis.space.Celestial;


/// Fired when a ship is denied entry to a planet due to addon or datapack functionality
public final class TeleportDisallowedEvent extends Event {
    private final Object subLevel;
    private final Celestial celestial;

    public TeleportDisallowedEvent(Object subLevel, Celestial celestial) {
        this.subLevel = subLevel;
        this.celestial = celestial;
    }

    /// Sable sub-level that tried to teleport to a Celestial
    public Object subLevel() {
        return subLevel;
    }

    /// Celestial that the ship tried to teleport to
    public Celestial celestial() {
        return celestial;
    }
}
