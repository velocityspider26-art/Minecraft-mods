package net.mcreator.crustychunks.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import net.mcreator.crustychunks.world.inventory.BreederReactorControlGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class BreederReactorControlGUIScreen extends AbstractContainerScreen<BreederReactorControlGUIMenu> {
   private static final HashMap<String, Object> guistate = BreederReactorControlGUIMenu.guistate;
   private final Level world;
   private final int x;
   private final int y;
   private final int z;
   private final Player entity;
   private static final ResourceLocation texture = ResourceLocation.parse("crusty_chunks:textures/screens/breeder_reactor_control_gui.png");

   public BreederReactorControlGUIScreen(BreederReactorControlGUIMenu container, Inventory inventory, Component text) {
      super(container, inventory, text);
      this.world = container.world;
      this.x = container.x;
      this.y = container.y;
      this.z = container.z;
      this.entity = container.entity;
      this.imageWidth = 172;
      this.imageHeight = 142;
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
      this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
      super.render(guiGraphics, mouseX, mouseY, partialTicks);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      guiGraphics.blit(texture, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
      guiGraphics.blit(
         ResourceLocation.parse("crusty_chunks:textures/screens/graynuclear.png"), this.leftPos + 69, this.topPos + 17, 0.0F, 0.0F, 32, 32, 32, 32
      );
      guiGraphics.blit(
         ResourceLocation.parse("crusty_chunks:textures/screens/grayuranium.png"), this.leftPos + 150, this.topPos + 27, 0.0F, 0.0F, 16, 16, 16, 16
      );
      guiGraphics.blit(
         ResourceLocation.parse("crusty_chunks:textures/screens/graydust.png"), this.leftPos + 6, this.topPos + 27, 0.0F, 0.0F, 16, 16, 16, 16
      );
      RenderSystem.disableBlend();
   }

   public boolean keyPressed(int key, int b, int c) {
      if (key == 256) {
         this.minecraft.player.closeContainer();
         return true;
      } else {
         return super.keyPressed(key, b, c);
      }
   }

   protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
   }

   public void init() {
      super.init();
   }
}
