package shipwrights.genesis.tests;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsContraptionLookup;
import shipwrights.genesis.compat.aeronautics.AeronauticsTeleportHelper;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Utility for creating and positioning Create Aeronautics constructs in GameTests. Ported from the
 * Valkyrien Skies {@code ShipAssembler} helper onto Sable's {@link SubLevelAssemblyHelper}.
 */
public class TestShipHelper {

    private static final int MAX_ASSEMBLY_BLOCKS = 256;

    /** Places a 3×3 stone platform at {@code relPos}, flood-fills it, and assembles a construct. */
    public static AeronauticsConstruct assembleConstruct(GameTestHelper helper, BlockPos relPos) {
        ServerLevel level = helper.getLevel();
        BlockPos absPos = helper.absolutePos(relPos);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(absPos.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
            }
        }

        List<BlockPos> blocks = collectBlocks(level, absPos);
        GenesisMod.LOGGER.info("[TestShipHelper] assembleConstruct: {} blocks at {}", blocks.size(), absPos);

        BoundingBox3i bounds = new BoundingBox3i(absPos.getX(), absPos.getY(), absPos.getZ(), absPos.getX(), absPos.getY(), absPos.getZ());
        for (BlockPos p : blocks) bounds = bounds.expandTo(p.getX(), p.getY(), p.getZ(), bounds);

        var subLevel = SubLevelAssemblyHelper.assembleBlocks(level, absPos, blocks, bounds);
        return AeronauticsContraptionLookup.getConstructById(level, subLevel.getUniqueId());
    }

    /** Moves a construct above the atmosphere-exit height so PlanetToSpaceTeleporter picks it up. */
    public static void moveConstructAboveAtmosphere(ServerLevel level, AeronauticsConstruct construct) {
        Vector3dc pos = construct.positionInWorld();
        double targetY = GenesisCommonConfig.getAtmosphereExitHeight() + 20.0;
        AeronauticsTeleportHelper.teleportWithinLevel(construct,
                new Vector3d(pos.x(), targetY, pos.z()), new Quaterniond(), new Vector3d(), new Vector3d());
    }

    /** Moves a construct into space just inside a celestial's collision radius. */
    public static void moveConstructNearPlanet(ServerLevel spaceLevel, AeronauticsConstruct construct, ResourceLocation celestialId) {
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(spaceLevel);
        Celestial celestial = registry.get(celestialId);
        if (celestial == null) {
            GenesisMod.LOGGER.warn("[TestShipHelper] Celestial '{}' not found; cannot position construct", celestialId);
            return;
        }
        long ticks = GenesisMod.getTicks(spaceLevel);
        Vector3dc celestialPos = celestial.getPosition(ticks, registry);
        double targetDist = celestial.getActualSize() * 0.5;
        AeronauticsTeleportHelper.teleportWithinLevel(construct,
                new Vector3d(celestialPos.x(), celestialPos.y() + targetDist, celestialPos.z()),
                new Quaterniond(), new Vector3d(), new Vector3d());
    }

    private static List<BlockPos> collectBlocks(ServerLevel level, BlockPos origin) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        if (!level.getBlockState(origin).isAir()) {
            visited.add(origin);
            queue.add(origin);
        }
        while (!queue.isEmpty() && result.size() < MAX_ASSEMBLY_BLOCKS) {
            BlockPos pos = queue.poll();
            result.add(pos);
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (visited.add(neighbor) && !level.getBlockState(neighbor).isAir()) {
                    queue.add(neighbor);
                }
            }
        }
        return result;
    }
}
