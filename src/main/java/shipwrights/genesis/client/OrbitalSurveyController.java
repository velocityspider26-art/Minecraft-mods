package shipwrights.genesis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.lod.DhLiveChunkPatchCache;
import shipwrights.genesis.client.lod.PlanetLodVolumeClientCache;
import shipwrights.genesis.client.lod.SparsePlanetLodClientCache;
import shipwrights.genesis.space.surface.SparsePlanetLodService;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

/**
 * Optical orbital survey view used by the high-detail cube LOD.
 *
 * <p>A Minecraft house is physically far below one screen pixel when the full
 * 8192-block face is visible. The correct solution is optical magnification,
 * not enlarging structures or lying about planet scale. This controller narrows
 * the real camera FOV, keeps the crosshair mapped to an exact cube-face world
 * coordinate, and tells the renderer which small region deserves block-scale
 * terrain.</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID)
public final class OrbitalSurveyController {
    private static final int[] ZOOM_LEVELS = {4, 8, 16, 32, 64, 128};

    public record Focus(CubeNetSurfaceTransform.Face face, double worldX, double worldZ,
                        boolean crosshairHit, int bestCellSize) {
    }

    private static volatile boolean active;
    private static volatile int zoomIndex = 2;
    private static volatile Focus focus;

    private OrbitalSurveyController() {
    }

    public static boolean isActive() {
        return active;
    }

    public static int zoom() {
        return ZOOM_LEVELS[Mth.clamp(zoomIndex, 0, ZOOM_LEVELS.length - 1)];
    }

    public static Focus focus() {
        return focus;
    }

    public static void toggle() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !GenesisMod.isSpaceDimension(minecraft.level)) {
            active = false;
            return;
        }
        active = !active;
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal(
                    active ? "Orbital survey enabled — scroll to zoom"
                            : "Orbital survey disabled"), true);
        }
    }

    public static void updateTarget(CubeNetSurfaceTransform.Face face,
                                    double worldX, double worldZ,
                                    boolean crosshairHit) {
        int best = SparsePlanetLodClientCache.bestCellSizeAt(
                SparsePlanetLodService.EARTH_ID, face, worldX, worldZ);
        if (PlanetLodVolumeClientCache.hasTileAt(SparsePlanetLodService.EARTH_ID, face, worldX, worldZ)
                || DhLiveChunkPatchCache.hasPatchAt(face, worldX, worldZ)) {
            best = 0;
        }
        focus = new Focus(face, worldX, worldZ, crosshairHit, best);
    }

    /** Radius, in planet blocks, that receives exact/high-detail rendering. */
    public static double detailRadius() {
        if (!active) {
            return 384.0;
        }
        return switch (zoom()) {
            case 4 -> 1024.0;
            case 8 -> 768.0;
            case 16 -> 512.0;
            case 32 -> 384.0;
            case 64 -> 256.0;
            default -> 160.0;
        };
    }

    public static void reset() {
        active = false;
        focus = null;
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!active || Minecraft.getInstance().screen != null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !GenesisMod.isSpaceDimension(minecraft.level)) {
            reset();
            return;
        }
        double delta = event.getScrollDeltaY();
        if (delta == 0.0) {
            return;
        }
        zoomIndex = Mth.clamp(zoomIndex + (delta > 0.0 ? 1 : -1),
                0, ZOOM_LEVELS.length - 1);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !GenesisMod.isSpaceDimension(minecraft.level)) {
            reset();
            return;
        }
        event.setFOV(Math.max(0.35, event.getFOV() / zoom()));
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !GenesisMod.isSpaceDimension(minecraft.level)) {
            reset();
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int cx = width / 2;
        int cy = height / 2;
        int cross = 7;
        graphics.fill(cx - cross, cy, cx - 2, cy + 1, 0xE0FFFFFF);
        graphics.fill(cx + 3, cy, cx + cross + 1, cy + 1, 0xE0FFFFFF);
        graphics.fill(cx, cy - cross, cx + 1, cy - 2, 0xE0FFFFFF);
        graphics.fill(cx, cy + 3, cx + 1, cy + cross + 1, 0xE0FFFFFF);

        Focus current = focus;
        int boxWidth = 250;
        int left = cx - boxWidth / 2;
        graphics.fill(left, 8, left + boxWidth, current == null ? 34 : 52, 0xA8050910);
        graphics.drawCenteredString(minecraft.font,
                "ORBITAL SURVEY  ×" + zoom(), cx, 13, 0xFF9DEBFF);
        if (current == null) {
            graphics.drawCenteredString(minecraft.font,
                    "Aim at Earth", cx, 27, 0xFFB8C0CC);
            return;
        }

        graphics.drawCenteredString(minecraft.font,
                current.face().name() + "  X " + Mth.floor(current.worldX())
                        + "  Z " + Mth.floor(current.worldZ()),
                cx, 27, current.crosshairHit() ? 0xFFFFFFFF : 0xFFB8C0CC);
        graphics.drawCenteredString(minecraft.font,
                qualityLabel(current.bestCellSize()), cx, 40,
                current.bestCellSize() <= 1 ? 0xFF7CFF9C : 0xFFFFCE73);
    }

    private static String qualityLabel(int cellSize) {
        if (cellSize == Integer.MAX_VALUE) return "NO CACHED TERRAIN — global fallback";
        if (cellSize <= 0) return "TEXTURED 3D BLOCK VOLUME — roofs, walls and structures";
        if (cellSize <= 1) return "EXACT 1-BLOCK SURFACE — structure targeting ready";
        if (cellSize <= 4) return "HIGH DETAIL — 4 blocks per sample";
        if (cellSize <= 16) return "REGIONAL DETAIL — 16 blocks per sample";
        if (cellSize <= 64) return "COARSE DETAIL — 64 blocks per sample";
        return "GLOBAL PREDICTION — " + cellSize + " blocks per sample";
    }
}
