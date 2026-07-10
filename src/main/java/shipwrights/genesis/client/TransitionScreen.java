package shipwrights.genesis.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TransitionScreen extends Screen {
    public TransitionScreen() {
        super(Component.empty());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // A plain deep-space fill while the destination loads. We deliberately do NOT grab the previous
        // frame with glBlitFramebuffer any more — that raw blit off the (often multisampled) main
        // framebuffer hard-crashed some GPUs when crossing into space. A solid fill is safe everywhere
        // and reads as a brief blink into space rather than a vanilla loading screen.
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF01010A);
    }

    @Override
    public void onClose() {
        super.onClose();
        TransitionState.CURRENT = TransitionState.NONE;
    }
}
