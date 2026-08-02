package shipwrights.genesis.hyperspace;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import shipwrights.genesis.GenesisMod;

/**
 * 2D overlay for the hyperspace jump transitions, mirroring the classic
 * cockpit sequence: stars stretch into streaks while the drive spools, then
 * the view surges to white BEFORE the dimension swap and fades back out after
 * it, hiding the swap hitch. The cruise itself has no overlay — the hyperspace
 * dimension's sky ({@link shipwrights.genesis.client.WormholeDimensionEffects})
 * is the tunnel.
 *
 * All timing advances in {@link #onClientTick} (20/s); the render pass only
 * draws the current state, so overlay durations match server ticks regardless
 * of frame rate.
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public final class HyperspaceTunnelRenderer {
    private static final ResourceLocation STREAKS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "textures/gui/hyperspace_streaks.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int TEXTURE_HALF = TEXTURE_SIZE / 2;

    private static final int FLASH_RISE_TICKS = 10;
    /** Failsafe: drop a held flash fast if the dimension swap never happens. */
    private static final int FLASH_HOLD_TIMEOUT_TICKS = 60;

    // Spool-up streaks
    private static int preJumpTicks;
    private static int preJumpDuration;

    // White flash (rise -> hold through the dimension swap -> settle -> fade)
    private static final int FLASH_IDLE = 0;
    private static final int FLASH_RISING = 1;
    private static final int FLASH_HOLDING = 2;
    private static final int FLASH_SETTLING = 3;
    private static final int FLASH_FADING = 4;
    private static int flashPhase = FLASH_IDLE;
    private static int flashTicks;
    private static int flashFadeDuration;
    private static int flashHoldTicks;
    private static int flashSettleTicks;
    /** Whether this flash is a hyperdrive jump (warp streaks) or a plain transition (soft fade). */
    private static boolean flashWithStreaks = true;
    private static ResourceKey<Level> flashStartDimension;
    /**
     * Once the world has rendered after a swap, the fade can reveal it safely.
     * Short timeout: void dimensions may never report a compiled section.
     */
    private static final int FLASH_SETTLE_TIMEOUT_TICKS = 40;

    private static int cancelFadeTicks;
    private static float streakAmount;

    /** World-space axis the hyperspace tunnel flows along; set when a jump fires. */
    private static Vector3f tunnelAxis = new Vector3f(0.0f, 0.0f, 1.0f);

    private HyperspaceTunnelRenderer() {
    }

    public static void startPreJump(int durationTicks) {
        preJumpDuration = Math.max(1, durationTicks);
        preJumpTicks = preJumpDuration;
        flashPhase = FLASH_IDLE;
        cancelFadeTicks = 0;
        streakAmount = Math.max(streakAmount, 0.18f);
    }

    /**
     * Surge to white, hold until the dimension changes AND the destination has
     * actually rendered, then fade out. {@code withStreaks} selects the full
     * hyperdrive warp treatment vs. a plain soft fade for ordinary transitions.
     */
    public static void startFlash(int fadeTicks, boolean captureAxis, boolean withStreaks) {
        flashPhase = FLASH_RISING;
        flashTicks = 0;
        flashHoldTicks = 0;
        flashSettleTicks = 0;
        flashFadeDuration = Math.max(10, fadeTicks);
        flashWithStreaks = withStreaks;
        preJumpTicks = 0;
        preJumpDuration = 0;
        cancelFadeTicks = 0;

        Minecraft minecraft = Minecraft.getInstance();
        flashStartDimension = minecraft.level != null ? minecraft.level.dimension() : null;
        if (captureAxis) {
            updateTunnelAxisFromLook();
        }
    }

    public static void cancelTransit(int fadeTicks) {
        preJumpTicks = 0;
        preJumpDuration = 0;
        flashPhase = FLASH_IDLE;
        cancelFadeTicks = Math.max(10, fadeTicks);
        streakAmount = Math.max(streakAmount, 0.28f);
    }

    public static boolean isTransitActive() {
        return preJumpTicks > 0 || flashPhase != FLASH_IDLE || cancelFadeTicks > 0 || streakAmount > 0.025f;
    }

    /** The direction hyperspace streams along, used by the subspace sky renderer. */
    public static Vector3f tunnelAxis() {
        return tunnelAxis;
    }

    /** Uses the server's actual destination bearing instead of camera yaw. */
    public static void setTunnelAxis(float x, float y, float z) {
        Vector3f axis = new Vector3f(x, y, z);
        axis.y = 0.0f;
        if (axis.lengthSquared() < 1.0E-5f) {
            return;
        }
        tunnelAxis = axis.normalize();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        if (preJumpTicks > 0) {
            preJumpTicks--;
        }
        if (cancelFadeTicks > 0) {
            cancelFadeTicks--;
        }

        switch (flashPhase) {
            case FLASH_RISING -> {
                flashTicks++;
                if (flashTicks >= FLASH_RISE_TICKS) {
                    flashPhase = FLASH_HOLDING;
                    flashHoldTicks = 0;
                }
            }
            case FLASH_HOLDING -> {
                flashHoldTicks++;
                boolean dimensionChanged = flashStartDimension == null
                        || minecraft.level.dimension() != flashStartDimension;
                if (dimensionChanged || flashHoldTicks >= FLASH_HOLD_TIMEOUT_TICKS) {
                    flashPhase = FLASH_SETTLING;
                    flashSettleTicks = 0;
                }
            }
            case FLASH_SETTLING -> {
                // Stay white until the destination is actually on screen — the
                // load hitch and chunk meshing happen behind the flash, so the
                // fade reveals a finished world instead of a frozen frame.
                flashSettleTicks++;
                boolean rendered = minecraft.player != null
                        && minecraft.levelRenderer.isSectionCompiled(minecraft.player.blockPosition());
                if (rendered || flashSettleTicks >= FLASH_SETTLE_TIMEOUT_TICKS) {
                    flashPhase = FLASH_FADING;
                    flashTicks = 0;
                }
            }
            case FLASH_FADING -> {
                flashTicks++;
                if (flashTicks >= flashFadeDuration) {
                    flashPhase = FLASH_IDLE;
                }
            }
            default -> {
            }
        }
    }

    private static void updateTunnelAxisFromLook() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Vec3 look = player.getLookAngle();
        // Flatten to horizontal so the tunnel never points into the ground.
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(0.0, 0.0, 1.0);
        }
        flat = flat.normalize();
        tunnelAxis = new Vector3f((float) flat.x, 0.0f, (float) flat.z);
    }

    public static void renderOverlay(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null) {
            return;
        }

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);

        if (flashPhase != FLASH_IDLE) {
            float flash = flashAmount(partialTick);
            if (flashWithStreaks) {
                // Streaks keep flowing beneath the flash the whole way through
                // the transition, so the effect never "ends before the teleport"
                // — it only fades once the flash does, after arrival.
                float streaks = Math.max(streakAmount, 0.5f + flash * 0.5f);
                streakAmount = streaks;
                renderStreakField(graphics, minecraft, partialTick, streaks * (1.0f - flash * 0.35f), 1.0f, flash);
            }
            renderFlash(graphics, minecraft, flash);
            return;
        }

        if (preJumpTicks > 0) {
            float progress = 1.0f - (preJumpTicks - partialTick) / (float) Math.max(1, preJumpDuration);
            float spool = smoothstep(Mth.clamp(progress / 0.68f, 0.0f, 1.0f));
            float surge = smoothstep(Mth.clamp((progress - 0.82f) / 0.18f, 0.0f, 1.0f));
            float amount = Mth.clamp(0.12f + spool * 0.58f + surge * 0.3f, 0.0f, 0.95f);
            streakAmount += (amount - streakAmount) * 0.22f;
            renderPreJump(graphics, minecraft, partialTick, streakAmount, progress, surge);
            return;
        }

        if (cancelFadeTicks > 0) {
            float amount = smoothstep(cancelFadeTicks / 35.0f) * streakAmount;
            renderAbortFade(graphics, minecraft, amount);
            streakAmount *= 0.94f;
            return;
        }

        streakAmount *= 0.9f;
    }

    private static float flashAmount(float partialTick) {
        return switch (flashPhase) {
            case FLASH_RISING -> smoothstep((flashTicks + partialTick) / FLASH_RISE_TICKS);
            case FLASH_HOLDING, FLASH_SETTLING -> 1.0f;
            case FLASH_FADING -> smoothstep(1.0f - (flashTicks + partialTick) / Math.max(1, flashFadeDuration));
            default -> 0.0f;
        };
    }

    /**
     * The star-streak spool-up: radial streaks build speed and brightness while
     * the drive charges.
     */
    private static void renderPreJump(GuiGraphics graphics, Minecraft minecraft, float partialTick, float amount, float progress, float surge) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        renderStreakField(graphics, minecraft, partialTick, amount, 0.35f + progress * 0.65f, 0.0f);

        // The last stretch before the flash starts pulling toward white.
        if (surge > 0.02f) {
            graphics.fill(0, 0, width, height, argb((int) (160.0f * surge * amount), 214, 228, 255));
        }
    }

    /**
     * The warp streak field itself: layered blue-white radial streaks rushing
     * outward from a drifting center, additively blended so overlapping layers
     * glow. {@code speed} scales how hard the tunnel is rushing; {@code whiteMix}
     * pulls the streak color toward white as the flash takes over.
     */
    private static void renderStreakField(GuiGraphics graphics, Minecraft minecraft, float partialTick,
                                          float amount, float speed, float whiteMix) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (width <= 0 || height <= 0 || amount <= 0.01f) {
            return;
        }

        float time = (minecraft.level.getGameTime() + partialTick) * 0.05f;
        float minSide = Math.min(width, height);
        float cover = (float) Math.hypot(width, height) * 1.28f;
        float cx = width * 0.5f + Mth.sin(time * 2.4f) * minSide * 0.012f;
        float cy = height * 0.5f + Mth.cos(time * 2.1f) * minSide * 0.012f;

        // Deep space-blue base so the streaks read against any scene.
        graphics.fill(0, 0, width, height, argb((int) (150.0f * amount), 1, 6, 24));

        // Streak color: electric blue-white, pulled toward pure white with the flash.
        float r = Mth.lerp(whiteMix, 0.62f, 1.0f);
        float g = Mth.lerp(whiteMix, 0.80f, 1.0f);
        float b = 1.0f;

        // Three layers rushing outward at staggered phases = tunnel depth.
        float rush = 1.9f + speed * 6.5f;
        float fastA = wrapped(time * rush);
        float fastB = wrapped(fastA + 0.37f);
        float fastC = wrapped(fastA + 0.71f);
        float rotation = time * (0.4f + speed * 0.55f);

        drawStreakLayer(graphics, cx, cy, cover * (0.68f + fastA * 0.95f),
                rotation, amount * (0.85f - fastA * 0.35f), r, g, b);
        drawStreakLayer(graphics, cx, cy, cover * (0.82f + fastB * 0.9f),
                -rotation * 0.6f, amount * (0.6f - fastB * 0.25f), r, g, b);
        drawStreakLayer(graphics, cx, cy, cover * (0.95f + fastC * 0.85f),
                rotation * 0.3f, amount * (0.45f - fastC * 0.2f), r, g, b);
    }

    /** Full-screen white-blue flash used to mask the dimension swap. */
    private static void renderFlash(GuiGraphics graphics, Minecraft minecraft, float amount) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (width <= 0 || height <= 0 || amount <= 0.01f) {
            return;
        }

        graphics.fill(0, 0, width, height, argb((int) (255.0f * Math.min(1.0f, amount * 1.1f)), 224, 234, 255));
    }

    private static void renderAbortFade(GuiGraphics graphics, Minecraft minecraft, float amount) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (width <= 0 || height <= 0 || amount <= 0.01f) {
            return;
        }
        graphics.fill(0, 0, width, height, argb((int) (68.0f * amount), 0, 9, 24));
    }

    /** Draws one streak texture layer, additively blended so layers glow where they overlap. */
    private static void drawStreakLayer(GuiGraphics graphics, float cx, float cy,
                                        float size, float rotation, float alpha,
                                        float red, float green, float blue) {
        float clampedAlpha = Mth.clamp(alpha, 0.0f, 1.0f);
        if (size <= 1.0f || clampedAlpha <= 0.01f) {
            return;
        }

        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0f);
        graphics.pose().mulPose(new Quaternionf().rotationZ(rotation));
        float scale = size / TEXTURE_SIZE;
        graphics.pose().scale(scale, scale, 1.0f);

        RenderSystem.enableBlend();
        // Additive: source alpha in, everything adds to what's behind it.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(red, green, blue, clampedAlpha);
        graphics.blit(STREAKS_TEXTURE, -TEXTURE_HALF, -TEXTURE_HALF, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.flush();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    private static float wrapped(float value) {
        return value - Mth.floor(value);
    }

    private static float smoothstep(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }

    private static int argb(int a, int r, int g, int b) {
        return (Mth.clamp(a, 0, 255) << 24)
                | (Mth.clamp(r, 0, 255) << 16)
                | (Mth.clamp(g, 0, 255) << 8)
                | Mth.clamp(b, 0, 255);
    }
}
