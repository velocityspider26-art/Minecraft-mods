package shipwrights.genesis.compat.aeronautics;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves whole Create Aeronautics constructs, in place and across dimensions.
 *
 * <p>Compatibility replacement for {@code shipWorld.teleportShip(ship, ShipTeleportData)}.</p>
 *
 * <ul>
 *   <li><b>Same dimension</b> &mdash; the construct's rigid body is teleported to the new pose and
 *       given the requested velocity. This is the fast, exact path used for the majority of
 *       Genesis' movement.</li>
 *   <li><b>Cross dimension</b> &mdash; Sable has no primitive for moving an assembled sub-level
 *       between levels, so this performs a genuine disassemble &rarr; transfer &rarr; reassemble:
 *       the construct's blocks (and their block-entity data) are snapshotted from its plot, stamped
 *       into the destination level around the target position, re-assembled into a fresh construct
 *       with {@link SubLevelAssemblyHelper#assembleBlocks}, and the source construct is removed.</li>
 * </ul>
 */
public final class AeronauticsTeleportHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("genesis/aeronautics-teleport");

    private AeronauticsTeleportHelper() {}

    /** Snapshot of a single construct block relative to a chosen anchor. */
    private record BlockSnapshot(BlockPos offset, BlockState state, @Nullable CompoundTag beTag) {}

    /**
     * Teleport a construct to a new pose within the same level and give it a new velocity.
     *
     * @return true if the construct was repositioned.
     */
    public static boolean teleportWithinLevel(AeronauticsConstruct construct, Vector3dc newPos, Quaterniondc newRotation,
                                              Vector3dc velocity, Vector3dc omega) {
        RigidBodyHandle body = AeronauticsPhysicsAdapter.getRigidBody(construct);
        if (body == null || !body.isValid()) return false;

        body.teleport(newPos, newRotation);

        // Replace the current velocity with the requested one.
        Vector3d dv = new Vector3d(velocity).sub(body.getLinearVelocity());
        Vector3d dw = new Vector3d(omega).sub(body.getAngularVelocity());
        body.addLinearAndAngularVelocity(dv, dw);
        return true;
    }

    /**
     * Teleport a construct to another level (dimension) at the given pose/velocity.
     *
     * @return the new construct in the target level, or {@code null} if the transfer could not be
     *         performed (in which case the source construct is left untouched).
     */
    @Nullable
    public static AeronauticsConstruct teleportToLevel(AeronauticsConstruct construct, ServerLevel targetLevel,
                                                       Vector3dc newPos, Quaterniondc newRotation,
                                                       Vector3dc velocity, Vector3dc omega) {
        SubLevel sub = construct.subLevel();
        if (!(sub instanceof ServerSubLevel serverSub)) return null;

        ServerLevel sourceLevel = serverSub.getLevel();
        if (sourceLevel == targetLevel) {
            return teleportWithinLevel(construct, newPos, newRotation, velocity, omega) ? construct : null;
        }

        try {
            List<BlockSnapshot> snapshot = snapshotConstruct(serverSub);
            if (snapshot.isEmpty()) {
                LOGGER.warn("Refusing to cross-dimension teleport empty construct {}", construct.id());
                return null;
            }

            BlockPos anchor = BlockPos.containing(newPos.x(), newPos.y(), newPos.z());

            // Stamp the blocks into the destination world.
            List<BlockPos> placed = new ArrayList<>(snapshot.size());
            BoundingBox3i bounds = new BoundingBox3i(anchor.getX(), anchor.getY(), anchor.getZ(), anchor.getX(), anchor.getY(), anchor.getZ());
            for (BlockSnapshot bs : snapshot) {
                BlockPos worldPos = anchor.offset(bs.offset());
                targetLevel.setBlock(worldPos, bs.state(), 2);
                if (bs.beTag() != null) {
                    BlockEntity be = targetLevel.getBlockEntity(worldPos);
                    if (be != null) {
                        be.loadWithComponents(bs.beTag(), targetLevel.registryAccess());
                    }
                }
                placed.add(worldPos);
                bounds = bounds.expandTo(worldPos.getX(), worldPos.getY(), worldPos.getZ(), bounds);
            }

            // Re-assemble into a fresh moving construct in the destination level.
            ServerSubLevel newSub = SubLevelAssemblyHelper.assembleBlocks(targetLevel, anchor, placed, bounds);
            AeronauticsConstruct result = new AeronauticsConstruct(newSub);

            // Apply orientation and velocity.
            teleportWithinLevel(result, newPos, newRotation, velocity, omega);

            // Remove the old construct now that the new one exists.
            ServerSubLevelContainer container = SubLevelContainer.getContainer(sourceLevel);
            if (container != null) {
                container.removeSubLevel(serverSub, SubLevelRemovalReason.REMOVED);
            }
            return result;
        } catch (Exception e) {
            LOGGER.error("Cross-dimension construct teleport failed for {}; leaving source construct intact", construct.id(), e);
            return null;
        }
    }

    /** Read every non-air block (and block-entity data) of the construct, relative to its plot center. */
    private static List<BlockSnapshot> snapshotConstruct(ServerSubLevel sub) {
        List<BlockSnapshot> out = new ArrayList<>();
        LevelPlot plot = sub.getPlot();
        if (plot == null) return out;

        ServerLevel plotLevel = sub.getLevel();
        BlockPos center = plot.getCenterBlock();

        for (PlotChunkHolder holder : plot.getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            int minX = chunk.getPos().getMinBlockX();
            int minZ = chunk.getPos().getMinBlockZ();
            int minY = plotLevel.getMinBuildHeight();
            int maxY = plotLevel.getMaxBuildHeight();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minY; y < maxY; y++) {
                        BlockPos pos = new BlockPos(minX + x, y, minZ + z);
                        BlockState state = chunk.getBlockState(pos);
                        if (state.isAir()) continue;
                        BlockPos offset = pos.subtract(center);
                        CompoundTag beTag = null;
                        BlockEntity be = chunk.getBlockEntity(pos);
                        if (be != null) {
                            beTag = be.saveWithFullMetadata(plotLevel.registryAccess());
                        }
                        out.add(new BlockSnapshot(offset, state, beTag));
                    }
                }
            }
        }
        return out;
    }
}
