package shipwrights.genesis.compat.aeronautics;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Locates Create Aeronautics constructs.
 *
 * <p>Compatibility replacement for {@code VSGameUtilsKt.getShipManagingPos},
 * {@code getShipObjectManagingPos}, {@code getLoadedShips()} and friends.</p>
 */
public final class AeronauticsContraptionLookup {

    private AeronauticsContraptionLookup() {}

    /**
     * @return the construct that a given (plot-space) block position belongs to, or {@code null}
     *         if the position is in the ordinary world. Equivalent to
     *         {@code VSGameUtilsKt.getShipManagingPos}.
     */
    @Nullable
    public static AeronauticsConstruct getConstructManaging(Level level, BlockPos pos) {
        if (!AeronauticsCompat.isLoaded()) return null;
        SubLevel sub = Sable.HELPER.getContaining(level, pos);
        return sub == null ? null : new AeronauticsConstruct(sub);
    }

    @Nullable
    public static AeronauticsConstruct getConstructManaging(Level level, Vector3dc pos) {
        if (!AeronauticsCompat.isLoaded()) return null;
        SubLevel sub = Sable.HELPER.getContaining(level, pos);
        return sub == null ? null : new AeronauticsConstruct(sub);
    }

    @Nullable
    public static AeronauticsConstruct getConstructManaging(Level level, Vec3 pos) {
        if (!AeronauticsCompat.isLoaded()) return null;
        SubLevel sub = Sable.HELPER.getContaining(level, pos.x, pos.z);
        return sub == null ? null : new AeronauticsConstruct(sub);
    }

    /** @return the construct a block entity is part of, or {@code null}. */
    @Nullable
    public static AeronauticsConstruct getConstructForBlockEntity(BlockEntity be) {
        if (!AeronauticsCompat.isLoaded()) return null;
        SubLevel sub = Sable.HELPER.getContaining(be);
        return sub == null ? null : new AeronauticsConstruct(sub);
    }

    /** @return the construct an entity is currently standing on / riding, or {@code null}. */
    @Nullable
    public static AeronauticsConstruct getConstructForEntity(Entity entity) {
        if (!AeronauticsCompat.isLoaded()) return null;
        SubLevel sub = Sable.HELPER.getTrackingOrVehicleSubLevel(entity);
        return sub == null ? null : new AeronauticsConstruct(sub);
    }

    /** @return true when the position lies inside any construct's plot region. */
    public static boolean isConstructBlock(Level level, Vec3 pos) {
        return getConstructManaging(level, pos) != null;
    }

    /** @return every loaded construct in this level. Equivalent to {@code shipObjectWorld.getLoadedShips()}. */
    public static List<AeronauticsConstruct> getAllConstructs(Level level) {
        List<AeronauticsConstruct> result = new ArrayList<>();
        if (!AeronauticsCompat.isLoaded()) return result;
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return result;
        for (SubLevel sub : container.getAllSubLevels()) {
            result.add(new AeronauticsConstruct(sub));
        }
        return result;
    }

    /**
     * @return every loaded construct in this level, largest first (by world-bounds volume) and
     *         then by id for deterministic ordering. Equivalent to Genesis' old {@code Util.getSortedShips}.
     */
    public static List<AeronauticsConstruct> getSortedConstructs(Level level) {
        List<AeronauticsConstruct> ships = getAllConstructs(level);
        ships.sort(Comparator
                .comparingDouble((AeronauticsConstruct c) -> {
                    var b = c.worldBounds();
                    return (b.maxX() - b.minX()) * (b.maxY() - b.minY()) * (b.maxZ() - b.minZ());
                })
                .reversed()
                .thenComparing(c -> c.id()));
        return ships;
    }

    /** @return the construct with the given id in this level, or {@code null}. */
    @Nullable
    public static AeronauticsConstruct getConstructById(Level level, UUID id) {
        if (!AeronauticsCompat.isLoaded()) return null;
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return null;
        SubLevel sub = container.getSubLevel(id);
        return sub == null ? null : new AeronauticsConstruct(sub);
    }

    /** @return all constructs whose world bounds intersect the given (inflated) box. */
    public static List<AeronauticsConstruct> getConstructsIntersecting(Level level, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        List<AeronauticsConstruct> result = new ArrayList<>();
        if (!AeronauticsCompat.isLoaded()) return result;
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return result;
        BoundingBox3d box = new BoundingBox3d(minX, minY, minZ, maxX, maxY, maxZ);
        for (SubLevel sub : container.queryIntersecting(box)) {
            result.add(new AeronauticsConstruct(sub));
        }
        return result;
    }
}
