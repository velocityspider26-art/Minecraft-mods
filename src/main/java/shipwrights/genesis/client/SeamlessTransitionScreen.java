package shipwrights.genesis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import shipwrights.genesis.GenesisMod;

import java.util.function.BooleanSupplier;

/**
 * The screen shown while crossing between Genesis dimensions — which, for a
 * crossing that goes as intended, is nothing at all.
 *
 * <p>Vanilla puts up "Downloading terrain" the instant a player changes
 * dimension and takes it down once the section they are standing in has been
 * received and compiled. The screen is not the delay; it is the curtain over
 * it. With the arrival chunks already generated before the teleport fires
 * (see {@code ArrivalGate}), that curtain has nothing left to hide, so this
 * draws nothing and lets the world show through — the crossing reads as
 * flying from one place into another rather than as a load.</p>
 *
 * <p>Two earlier attempts at this went wrong in ways worth not repeating.
 * Cancelling {@code ScreenEvent.Opening} does not close the current screen, it
 * only blocks the new one, so the previous screen stayed up forever at 100%.
 * And simply deleting the screen leaves nothing to say a slow crossing is
 * still working. So this keeps vanilla's own screen and its own dismissal
 * logic — it cannot strand anyone, because the thing that closes it is
 * unchanged — and only stops it drawing. If a crossing does run long, the
 * themed screen fades up rather than leaving the player staring into a
 * half-built world with no explanation.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public class SeamlessTransitionScreen extends ReceivingLevelScreen {
    /**
     * How long a crossing may take before the player is told anything. Below
     * this the screen is invisible; a fast crossing is meant to feel like no
     * transition happened at all, and a flash of text would undo that on its
     * own.
     */
    private static final long QUIET_MS = 450L;
    /** How long the themed screen takes to fade up once it does appear. */
    private static final long FADE_MS = 350L;
    /**
     * Let the destination render a few real frames before asking the server to
     * verify Sable visibility. Screen removal happens before the first world
     * frame, while the render thread is still draining dimension packets.
     */
    private static final int DESTINATION_FRAMES_BEFORE_READY = 3;
    private static int destinationFramesUntilReady = -1;

    /** Shown only if a crossing runs long enough to need explaining. */
    private static final String[] MESSAGES = {
            "Recalibrating artificial gravity... please remain seated",
            "Convincing the chunks that space is a real place",
            "Folding space. Do not iron.",
            "Asking the moon to please hold still",
            "Reticulating star splines",
            "Loading the void (it's mostly nothing, one sec)",
            "Hyperspace lawyers are reviewing your flight plan",
            "Untangling the space-time extension cords",
            "Politely asking physics to look the other way",
            "Downloading more universe...",
            "Warming up the vacuum",
            "Counting stars... lost count, starting over",
            "Your ship is in another dimension. Literally. Fetching it.",
    };

    /** Rotates through the list without immediate repeats. */
    private static int lastMessageIndex = -1;

    private final long openedAt = System.currentTimeMillis();
    private final String message = nextMessage();
    private boolean notifiedServer;

    private static String nextMessage() {
        RandomSource random = RandomSource.create();
        int index = random.nextInt(MESSAGES.length);
        if (index == lastMessageIndex) {
            index = (index + 1 + random.nextInt(MESSAGES.length - 1)) % MESSAGES.length;
        }
        lastMessageIndex = index;
        return MESSAGES[index];
    }

    public SeamlessTransitionScreen(BooleanSupplier levelReceived, Reason reason) {
        super(levelReceived, reason);
    }

    @Override
    public void removed() {
        super.removed();
        Minecraft minecraft = Minecraft.getInstance();
        if (!notifiedServer && minecraft.level != null
                && minecraft.player != null && minecraft.getConnection() != null) {
            notifiedServer = true;
            destinationFramesUntilReady = DESTINATION_FRAMES_BEFORE_READY;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || destinationFramesUntilReady < 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.getConnection() == null) {
            destinationFramesUntilReady = -1;
            return;
        }
        if (--destinationFramesUntilReady > 0) {
            return;
        }

        // The old build sent genesis:transition_ready here even though the
        // payload was not registered client-to-server, crashing the render
        // thread after an otherwise successful crossing. The acknowledgement
        // is optional; the existing server-side follow/retrack fallback remains
        // authoritative, so completing the countdown must be a no-op.
        destinationFramesUntilReady = -1;
    }


    /** Clears the delayed acknowledgement when disconnecting or changing saves. */
    public static void resetTransitionNotification() {
        destinationFramesUntilReady = -1;
    }

    /**
     * Nothing. The vanilla implementation draws a panorama, a blur and a menu
     * background, all of which would hide the world we are trying to show.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float visibility = visibility();
        if (visibility <= 0.0f) {
            return; // the good case: the world, uninterrupted
        }

        int alpha = (int) (visibility * 255.0f) << 24;
        graphics.fill(0, 0, width, height, alpha | 0x02030A);

        long time = System.currentTimeMillis() - openedAt;
        RandomSource stars = RandomSource.create(9021L);
        for (int i = 0; i < 140; i++) {
            int x = stars.nextInt(Math.max(1, width));
            int y = stars.nextInt(Math.max(1, height));
            float phase = stars.nextFloat() * (float) Math.PI * 2.0f;
            float twinkle = 0.55f + 0.45f * Mth.sin(time * 0.002f + phase);
            int brightness = (int) (150 * twinkle * visibility) + (int) (60 * visibility);
            int color = alpha | (brightness << 16) | (brightness << 8) | Math.min(255, brightness + 25);
            graphics.fill(x, y, x + 1, y + 1, color);
        }

        int centerY = height / 2;
        graphics.drawCenteredString(font, Component.literal("Traversing the void"),
                width / 2, centerY - 20, alpha | 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(message),
                width / 2, centerY + 2, alpha | 0xAAB7DD);
    }

    /** 0 while the crossing is still quick enough to hide, ramping to 1 if it drags. */
    private float visibility() {
        long elapsed = System.currentTimeMillis() - openedAt - QUIET_MS;
        if (elapsed <= 0L) {
            return 0.0f;
        }
        return Mth.clamp(elapsed / (float) FADE_MS, 0.0f, 1.0f);
    }
}
