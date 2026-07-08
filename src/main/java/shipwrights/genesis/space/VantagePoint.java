package shipwrights.genesis.space;


import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;

/// Represents where the observer is in space, for purposes such as rendering celestials.
public interface VantagePoint {

    Vector3dc getPosition();
    Quaterniondc getRotation();

    /// Where the observer actually is for rendering, accounting for how high they've climbed above
    /// the planet's surface. On a planet this rises off the surface along the local "up" as altitude
    /// increases, so the whole celestial field (the planet below, the Moon ahead) shifts and grows as
    /// you fly up — seamless Earth-to-space with no dimension change. Defaults to {@link #getPosition()}.
    default Vector3dc getObserverPosition() {
        return getPosition();
    }

    /// if null, the observer has no access to space. Example: the observer is in The Nether
    static @Nullable VantagePoint get(Level level, Vector3dc posInLevel, long ticks, float partialTick) {
        if (GenesisMod.isSpaceDimension(level)) {
            return new VantagePoint.InSpace(new Vector3d(posInLevel));
        } else {
            Celestial celestial = GenesisMod.getCelestialForLevel(level);
            if (celestial != null) {
                Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
                /// for now, always put the player on the "north" (-z) side of the planet
                Quaterniond rotation = new Quaterniond()
                        .rotateTo(new Vector3d(0, 1, 0), new Vector3d(0, 0, -1));

                // How far above the surface the observer has climbed (blocks). Drives the seamless
                // Earth-to-space ascent: the planet falls away below and the Moon draws nearer.
                double altitude = Math.max(0.0, posInLevel.y());

                return new VantagePoint.OnCelestial(celestial, rotation, ticks, partialTick, registry, altitude);
            } else {
                return null;
            }
        }
    }


    /// In the space dimension the observer's position IS the space-level position; celestial
    /// coordinates live in the same space, so rendering and fog must use the real camera position.
    record InSpace (Vector3dc position) implements VantagePoint {
        @Override
        public Vector3dc getPosition() {
            return position;
        }

        @Override
        public Quaterniondc getRotation() {
            return new Quaterniond();
        }
    }

    record OnCelestial (
            Celestial celestial,
            /// should be treated like as the camera's latitude/longitude on the celestial, but as a quaternion for easier usage
            Quaterniondc cameraRotationFromNorthPole,
            long ticks,
            float partialTick,
            Registry<Celestial> registry,
            /// how far (blocks) the observer has climbed above the planet surface
            double altitude
    ) implements VantagePoint {


        @Override
        public Vector3dc getPosition() {
            return celestial.getPosition(ticks, partialTick, registry);
        }

        /// The observer rises off the surface along local "up" as they climb, so the planet shrinks
        /// below them and nearer bodies (the Moon) grow as they approach.
        @Override
        public Vector3dc getObserverPosition() {
            Vector3dc center = getPosition();
            double radius = celestial.getActualSize() * 0.5;
            Vector3d up = new Vector3d(0, 1, 0);
            getCelestialRotation().transform(up);
            return up.mul(radius + altitude).add(center);
        }

        @Override
        public Quaterniondc getRotation() {
            // worldRotation = celestialRotation * localCameraRotation
            return getCelestialRotation().mul(cameraRotationFromNorthPole, new Quaterniond());
        }

        public Quaterniondc getCelestialRotation() {
            return celestial.getRotation(ticks, partialTick, registry);
        }
    }
}
