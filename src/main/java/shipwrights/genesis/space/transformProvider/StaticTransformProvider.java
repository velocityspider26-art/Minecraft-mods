package shipwrights.genesis.space.transformProvider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import shipwrights.genesis.space.Celestial;

public class StaticTransformProvider implements CelestialTransformProvider {

    public static final ResourceLocation TYPE = ResourceLocation.parse("genesis:static");

    private final double x;
    private final double y;
    private final double z;
    private final double xRot;
    private final double yRot;
    private final double zRot;
    /** Ticks for one full spin about the local Y axis; 0 = no spin (a fixed star still turns slowly). */
    private final double spinPeriod;
    private final Quaterniondc rotation;


    /**
     * Creates a static transform provider with position and rotation.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param xRot rotation around the x-axis in radians
     * @param yRot rotation around the y-axis in radians
     * @param zRot rotation around the z-axis in radians
     */
    public StaticTransformProvider(double x, double y, double z, double xRot, double yRot, double zRot, double spinPeriod) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = xRot;
        this.yRot = yRot;
        this.zRot = zRot;
        this.spinPeriod = spinPeriod;
        this.rotation = new Quaterniond().rotationXYZ(xRot, yRot, zRot);
    }

    public StaticTransformProvider(double x, double y, double z, double xRot, double yRot, double zRot) {
        this(x, y, z, xRot, yRot, zRot, 0.0);
    }

    /**
     * Creates a static transform provider with position only (no rotation).
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public StaticTransformProvider(double x, double y, double z) {
        this(x, y, z, 0, 0, 0, 0.0);
    }

    @Override
    public Quaterniondc getRotation(long ticks, float subticks, Registry<Celestial> registry) {
        if (spinPeriod <= 0.0) return rotation;
        // Slow, steady spin about the local Y axis, tied to game time.
        double angle = 2.0 * Math.PI * (ticks + subticks) / spinPeriod;
        return new Quaterniond(rotation).rotateY(angle);
    }

    @Override
    public Vector3d getPosition(long ticks, float subticks, Registry<Celestial> registry) {
        return new Vector3d(x, y, z);
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    // Codec for serialization/deserialization
    public static final MapCodec<StaticTransformProvider> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("x").forGetter(p -> p.x),
                    Codec.DOUBLE.fieldOf("y").forGetter(p -> p.y),
                    Codec.DOUBLE.fieldOf("z").forGetter(p -> p.z),
                    Codec.DOUBLE.optionalFieldOf("xRot", 0.0).forGetter(p -> p.xRot),
                    Codec.DOUBLE.optionalFieldOf("yRot", 0.0).forGetter(p -> p.yRot),
                    Codec.DOUBLE.optionalFieldOf("zRot", 0.0).forGetter(p -> p.zRot),
                    Codec.DOUBLE.optionalFieldOf("spinPeriod", 0.0).forGetter(p -> p.spinPeriod)
            ).apply(instance, StaticTransformProvider::new)
    );

    // Example registration method (call this during mod initialization)
    public static void register() {
        CelestialTransformProvider.register(TYPE, CODEC);
    }
}
