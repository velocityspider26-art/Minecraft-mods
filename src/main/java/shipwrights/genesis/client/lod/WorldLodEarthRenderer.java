package shipwrights.genesis.client.lod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.planet.CubeSurfaceProjection;
import shipwrights.genesis.space.planet.FlatToCubeFoldController;
import shipwrights.genesis.space.planet.PlanetRenderDiagnostics;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;
import shipwrights.genesis.teleportation.SpaceTravelManager;

/**
 * Turns the overworld itself into the cube Earth as the player climbs out.
 *
 * <p>Nothing here is a second world. The geometry drawn beyond vanilla's chunks
 * is the same voxel pyramid that holds the real block states of the real save,
 * placed at the real cube-net world coordinates. As altitude increases the five
 * remote cube-net regions fold around the region under the player — which never
 * moves — so a complete six-sided planet assembles out of the ground the player
 * took off from. No planet model is ever introduced, at any altitude.</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID)
public final class WorldLodEarthRenderer {
    private static boolean loggedActive;
    private static boolean loggedFolded;

    private WorldLodEarthRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !level.dimension().equals(Level.OVERWORLD)) {
            loggedActive = false;
            loggedFolded = false;
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        if (camera.y < GenesisClientConfig.getWorldLodStartHeight()) {
            loggedActive = false;
            loggedFolded = false;
            return;
        }

        Celestial earth = GenesisMod.getCelestialForLevel(level);
        if (earth == null) return;

        CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
                earth.getActualSize(), SpaceTravelManager.entryPadding(earth.getActualSize()));
        CubeSurfaceProjection projection = new CubeSurfaceProjection(transform, level.getSeaLevel());

        CubeNetSurfaceTransform.Face currentFace = transform.faceContaining(camera.x, camera.z);
        if (currentFace == null) currentFace = CubeNetSurfaceTransform.Face.UP;

        int vanillaRadius = Math.max(64, minecraft.options.getEffectiveRenderDistance() * 16);
        FlatToCubeFoldController.FoldState foldState = FlatToCubeFoldController.evaluate(
                camera.y, projection.seaLevel(), vanillaRadius);

        // Genesis simulates at one space block per sixteen planet blocks. The
        // renderer does not: one Minecraft block stays one rendered block, and
        // only the empty altitude above the terrain is compressed. That is the
        // difference between a low-orbit view of real chunks and a thumbnail of
        // a planet in which a house is smaller than a pixel.
        Vec3 renderCamera = EarthLodVisualFrame.overworldRenderCamera(
                camera, projection.seaLevel(), Math.max(foldState.lodAlpha(), foldState.fold()));

        CubeSurfaceProjection.PlanetSurfacePosition here =
                projection.worldToFace(camera.x, camera.y, camera.z);
        PlanetRenderDiagnostics.setLocation(currentFace, here.surfaceX(), here.surfaceZ(),
                camera.y - projection.seaLevel(), renderCamera.y - projection.seaLevel());
        PlanetRenderDiagnostics.setFold(foldState.fold());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        PlanetVoxelRenderer.renderOverworld(event.getModelViewMatrix(), renderCamera,
                event.getCamera().getLookVector(), projection, currentFace,
                foldState.fold(), vanillaRadius);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        if (!loggedActive && PlanetVoxelRenderer.hasVoxelData()) {
            loggedActive = true;
            GenesisMod.LOGGER.info("[PLANET-VOXEL] world-to-cube LOD active at y={}, face={}",
                    Mth.floor(camera.y), currentFace);
        }
        if (foldState.cubeComplete() && !loggedFolded) {
            loggedFolded = true;
            GenesisMod.LOGGER.info("[PLANET-VOXEL] the overworld itself is now the complete "
                    + "six-face Earth; no standalone ground model exists");
        }
    }
}
