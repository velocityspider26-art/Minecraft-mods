package shipwrights.genesis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisClientConfig;

@EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID)
public class SpaceShaderEventHandler {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Update shader state at the start of each frame
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level != null) {
            boolean shouldBeActive = GenesisMod.isSpaceDimension(level) && GenesisClientConfig.enableSpaceLighting();
            SpaceInvertPostProcessor.INSTANCE.setActive(shouldBeActive);
        }
    }
}
