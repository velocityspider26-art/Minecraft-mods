package shipwrights.genesis.content.blockentity;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.physics.CelestialGravityWells;
import shipwrights.genesis.teleportation.SpaceTravelManager;

/**
 * Holds the craft it is mounted on in a stable circular orbit.
 *
 * <p>Gravity already supplies the centripetal force, so the stabilizer does not
 * fight it — all it does is trim the craft's velocity toward the speed that
 * balances that pull ({@code sqrt(a·r)}), which is what an orbit actually is.
 * Because the correction is a gentle per-tick nudge rather than a hard set, the
 * craft still responds to thrusters and collisions; let go and it settles back
 * into orbit instead of being frozen onto a rail.</p>
 */
public class OrbitalStabilizerBlockEntity extends BlockEntity {
    /** Fraction of the velocity error corrected per tick. */
    private static final double TRIM_RATE = 0.08;
    /** Below this the craft is treated as already on station. */
    private static final double DEADBAND = 0.05;
    /** Only re-evaluate every few ticks; orbits change slowly. */
    private static final int TRIM_INTERVAL_TICKS = 4;

    private static final String ENABLED_TAG = "Enabled";

    private boolean enabled = true;
    private int tickCounter;

    public OrbitalStabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(GenesisBlockEntities.ORBITAL_STABILIZER.get(), pos, state);
    }

    public void tickServer() {
        if (!enabled || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // Orbits only exist in the space dimension; on a planet this is inert.
        if (!GenesisMod.isSpaceDimension(serverLevel)) {
            setActive(false);
            return;
        }
        if (++tickCounter < TRIM_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        ServerSubLevel ship = SpaceTravelManager.findShipContaining(serverLevel, worldPosition);
        if (ship == null) {
            setActive(false);
            return; // not mounted on a craft — nothing to stabilize
        }

        RigidBodyHandle handle = RigidBodyHandle.of(ship);
        if (handle == null || !handle.isValid()) {
            setActive(false);
            return;
        }

        Vector3dc shipPosition = ship.logicalPose().position();
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());
        Vector3d target = CelestialGravityWells.circularOrbitVelocity(serverLevel, shipPosition, velocity);
        if (target == null) {
            setActive(false);
            return; // out in deep space with nothing to orbit
        }

        Vector3d correction = target.sub(velocity, new Vector3d());
        correction.y = 0.0; // orbits are held in the celestial plane
        if (correction.length() < DEADBAND) {
            setActive(true);
            return;
        }

        handle.addLinearAndAngularVelocity(
                correction.mul(TRIM_RATE * TRIM_INTERVAL_TICKS), new Vector3d());
        setActive(true);
    }

    /** Drives the block's lit state so you can see whether it has a lock. */
    private void setActive(boolean active) {
        if (level == null || !getBlockState().hasProperty(BlockStateProperties.POWERED)) {
            return;
        }
        if (getBlockState().getValue(BlockStateProperties.POWERED) != active) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BlockStateProperties.POWERED, active));
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(ENABLED_TAG, enabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        enabled = !tag.contains(ENABLED_TAG) || tag.getBoolean(ENABLED_TAG);
    }
}
