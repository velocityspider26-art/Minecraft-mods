package com.velocityspider.crashphysics.sable;

import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.util.BlockScan;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The shapes of a dimension's vehicles: gathered the first time they are needed, then kept up to date from Sable's
 * block change events, so even huge vehicles are never scanned again.
 */
final class VehicleShapes {

    private final ServerLevel level;
    private final Reference2ObjectOpenHashMap<ServerSubLevel, VehicleShape> shapes = new Reference2ObjectOpenHashMap<>();
    private final BlockPos.MutableBlockPos changed = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();

    VehicleShapes(final ServerLevel level) {
        this.level = level;
    }

    /**
     * @return the vehicle's shape, or null if it has no blocks or is too big to track
     */
    @Nullable
    VehicleShape get(final ServerSubLevel vehicle) {
        final VehicleShape cached = this.shapes.get(vehicle);
        if (cached != null) {
            return cached.solid.isEmpty() ? null : cached;
        }

        final VehicleShape shape = new VehicleShape();
        this.shapes.put(vehicle, shape);

        final BoundingBox3ic bounds = vehicle.getPlot().getBoundingBox();
        if (bounds == null) {
            return null;
        }
        final long volume = (long) (bounds.maxX() - bounds.minX() + 1) * (bounds.maxY() - bounds.minY() + 1) * (bounds.maxZ() - bounds.minZ() + 1);
        final long maxVolume = Math.max(200_000L, CrashConfig.MAX_STRUCTURAL_BLOCKS.getAsInt() * 8L);
        if (volume <= 0 || volume > maxVolume) {
            return null;
        }

        BlockScan.solidBlocks(this.level, bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ(),
                (x, y, z, blockState) -> shape.add(x, y, z, blockState.isCollisionShapeFullBlock(this.level, this.scan.set(x, y, z))));
        return shape.solid.isEmpty() ? null : shape;
    }

    /**
     * Keeps the shape of a vehicle up to date when one of its blocks changes: broken off in a crash, built, mined.
     */
    void blockChanged(final int x, final int y, final int z, final BlockState oldState, final BlockState newState) {
        if (this.shapes.isEmpty()) {
            return;
        }
        final SubLevel subLevel = Sable.HELPER.getContaining(this.level, x >> 4, z >> 4);
        if (!(subLevel instanceof final ServerSubLevel vehicle)) {
            return;
        }
        final VehicleShape shape = this.shapes.get(vehicle);
        if (shape == null) {
            return;
        }
        this.changed.set(x, y, z);
        final boolean is = !newState.isAir() && !newState.getCollisionShape(this.level, this.changed).isEmpty();
        if (is) {
            shape.add(x, y, z, newState.isCollisionShapeFullBlock(this.level, this.changed));
        } else {
            shape.remove(this.changed.asLong());
        }
    }

    void forgetRemoved() {
        if (!this.shapes.isEmpty()) {
            this.shapes.keySet().removeIf(ServerSubLevel::isRemoved);
        }
    }
}
