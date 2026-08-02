package shipwrights.genesis.space.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.type.CelestialType;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(Dist.CLIENT)
public class CelestialRenderDispatcher {

    private static Boolean oculusLoaded = null;

    private static boolean isOculusLoaded() {
        if (oculusLoaded == null) oculusLoaded = ModList.get().isLoaded("oculus");
        return oculusLoaded;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;

        if (level == null) {
            return;
        }

        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);

        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        Vec3 cameraPos = event.getCamera().getPosition();

        VantagePoint vantagePoint = VantagePoint.get(level, new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z), ticks, partialTick);

        if (vantagePoint != null) {
            // In 1.21.1 the AFTER_SKY RenderLevelStageEvent is dispatched with a null pose
            // stack, so event.getPoseStack() is a fresh IDENTITY stack -- the camera/view
            // rotation is supplied separately via event.getModelViewMatrix(). Bake that view
            // rotation into the pose stack here so all celestial renderers are transformed by
            // the camera orientation. Without this the bodies render in a screen-fixed frame
            // and appear glued to the camera (the 1.20.1 -> 1.21.1 regression).
            event.getPoseStack().mulPose(event.getModelViewMatrix());

            Vector3dc cameraForRenderOrder = CelestialRenderCoordinates.getObserverCelestialPosition(vantagePoint, cameraPos);
            final Registry<Celestial> reg = registry;
            List<Celestial> celestials = registry.stream()
                .sorted(Comparator.comparingDouble(a -> -a.getPosition(ticks, partialTick, reg).distanceSquared(cameraForRenderOrder)))
                .toList();

            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

            for (Celestial celestial : celestials) {
                CelestialType type = celestial.type();
                CelestialRenderer renderer = type.getRenderer();

                renderer.setup(event, vantagePoint);
                renderer.invoke(event, celestial, vantagePoint);
                renderer.teardown(event, vantagePoint);

                // Celestial shaders rely on per-object uniforms. Flush any
                // leftover batched geometry before the next celestial mutates
                // shared shader state.
                bufferSource.endBatch();
            }
        }
    }
}
