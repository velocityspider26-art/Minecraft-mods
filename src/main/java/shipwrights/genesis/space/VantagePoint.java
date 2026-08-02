package shipwrights.genesis.space;


import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.teleportation.CubeFaceFrame;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;
import shipwrights.genesis.teleportation.SpaceTravelManager;

/// Represents where the observer is in space, for purposes such as rendering celestials.
public interface VantagePoint {

    Vector3dc getPosition();
    Quaterniondc getRotation();

    /// if null, the observer has no access to space. Example: the observer is in The Nether
    static @Nullable VantagePoint get(Level level, Vector3dc posInLevel, long ticks, float partialTick) {
        if (GenesisMod.isSpaceDimension(level)) {
            return new VantagePoint.InSpace(SpaceLevel.toCelestialSpace(level, posInLevel));
        } else {
            Celestial celestial = GenesisMod.getCelestialForLevel(level);
            if (celestial != null) {
                Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
                CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
                        celestial.getActualSize(), SpaceTravelManager.entryPadding(celestial.getActualSize()));
                CubeNetSurfaceTransform.Face face = transform.faceContaining(posInLevel.x(), posInLevel.z());
                if (face == null) {
                    face = CubeNetSurfaceTransform.Face.UP;
                }
                Quaterniond rotation = CubeFaceFrame.surfaceToCelestial(face);

                return new VantagePoint.OnCelestial(celestial, rotation, ticks, partialTick, registry);
            } else {
                return null;
            }
        }
    }


    record InSpace (Vector3dc position) implements VantagePoint {
        public InSpace() {
            this(new Vector3d());
        }

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
            Registry<Celestial> registry
    ) implements VantagePoint {


        @Override
        public Vector3dc getPosition() {
            return celestial.getPosition(ticks, partialTick, registry);
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
