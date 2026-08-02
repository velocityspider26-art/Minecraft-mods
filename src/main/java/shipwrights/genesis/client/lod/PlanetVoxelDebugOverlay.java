package shipwrights.genesis.client.lod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.space.planet.PlanetRenderDiagnostics;

import java.util.List;

/**
 * Debug HUD for the planet voxel engine.
 *
 * <p>Disabled by default and drawn only when {@code DebugOverlay} is switched on
 * in the client config. It exists because "the planet looks wrong" is not a
 * report anyone can act on, while "LOD 6 bricks, 400 queued mesh jobs, handoff
 * waiting on landing-column voxel bricks" is.</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID)
public final class PlanetVoxelDebugOverlay {
    private static final int LINE_HEIGHT = 10;
    private static final int MARGIN = 4;
    private static final int BACKGROUND = 0xA0000000;
    private static final int TEXT = 0xFFE0E8FF;

    private PlanetVoxelDebugOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!GenesisClientConfig.isPlanetDebugOverlayEnabled()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui) return;

        List<String> lines = PlanetRenderDiagnostics.overlayLines();
        if (lines.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = 0;
        for (String line : lines) width = Math.max(width, font.width(line));

        int x = MARGIN;
        int y = MARGIN + 40;
        graphics.fill(x - 2, y - 2, x + width + 4, y + lines.size() * LINE_HEIGHT + 1, BACKGROUND);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), x, y + index * LINE_HEIGHT, TEXT, false);
        }
    }
}
