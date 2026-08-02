package shipwrights.genesis.hyperspace;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class HyperdriveScreen extends AbstractContainerScreen<HyperdriveMenu> {
    private final List<Button> destinationButtons = new ArrayList<>();

    public HyperdriveScreen(HyperdriveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 218;
        imageHeight = 142;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();

        destinationButtons.clear();
        HyperspaceDestination[] destinations = HyperspaceDestination.values();
        for (int i = 0; i < destinations.length; i++) {
            HyperspaceDestination destination = destinations[i];
            Button button = addRenderableWidget(Button.builder(Component.literal(destination.displayName()), b -> {
                if (minecraft != null && minecraft.gameMode != null && minecraft.player != null) {
                    // The server confirms the jump with a HyperspaceStatePacket; the overlay
                    // only starts then, so a rejected jump shows nothing.
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, destination.buttonId());
                    minecraft.player.closeContainer();
                }
            }).bounds(leftPos + 22, topPos + 44 + i * 26, 76, 24).build());
            destinationButtons.add(button);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (Button button : destinationButtons) {
            button.active = !menu.isJumping();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        int x1 = leftPos + imageWidth;
        int y1 = topPos + imageHeight;

        graphics.fill(x0, y0, x1, y1, 0xe60a1020);
        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, 0xff101a2a);
        graphics.fill(x0 + 6, y0 + 6, x1 - 6, y0 + 28, 0xff17283d);
        graphics.fill(x0 + 110, y0 + 44, x1 - 18, y1 - 24, 0xff07101d);

        float progress = menu.progress();
        int barLeft = x0 + 22;
        int barTop = y0 + 102;
        int barWidth = imageWidth - 44;
        int filled = (int) (barWidth * progress);

        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 10, 0xff03070d);
        graphics.fill(barLeft, barTop, barLeft + filled, barTop + 10, 0xff21c8ff);
        graphics.fill(barLeft, barTop, barLeft + Math.max(0, filled - 10), barTop + 3, 0xffd8fbff);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 11, 0xffd8f6ff, false);
        graphics.drawString(font, Component.literal("Destination"), 22, 33, 0xff8fb8d0, false);

        Component status = menu.isJumping()
                ? Component.literal("JUMPING TO " + menu.destinationName().toUpperCase())
                : Component.literal("READY");
        graphics.drawString(font, status, 114, 50, menu.isJumping() ? 0xff65e7ff : 0xff8cffb4, false);

        int secondsLeft = Math.max(0, (menu.jumpDuration() - menu.jumpTicks() + 19) / 20);
        Component detail = menu.isJumping()
                ? Component.literal("Transit " + secondsLeft + "s")
                : Component.literal("Select destination");
        graphics.drawString(font, detail, 114, 68, 0xffd4dde8, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
