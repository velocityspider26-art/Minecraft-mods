package net.mcreator.crustychunks.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import net.mcreator.crustychunks.world.inventory.BlastFurnaceGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class BlastFurnaceGUIScreen extends AbstractContainerScreen<BlastFurnaceGUIMenu> {
   private static final HashMap<String, Object> guistate = BlastFurnaceGUIMenu.guistate;
   private final Level world;
   private final int x;
   private final int y;
   private final int z;
   private final Player entity;
   private static final ResourceLocation texture = ResourceLocation.parse("crusty_chunks:textures/screens/blast_furnace_gui.png");

   public BlastFurnaceGUIScreen(BlastFurnaceGUIMenu container, Inventory inventory, Component text) {
      super(container, inventory, text);
      this.world = container.world;
      this.x = container.x;
      this.y = container.y;
      this.z = container.z;
      this.entity = container.entity;
      this.imageWidth = 176;
      this.imageHeight = 166;
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
         ResourceLocation.parse("crusty_chunks:textures/screens/graydust.png"), this.leftPos + 7, this.topPos + 35, 0.0F, 0.0F, 16, 16, 16, 16
      );
      guiGraphics.blit(
         ResourceLocation.parse("crusty_chunks:textures/screens/graydust.png"), this.leftPos + 7, this.topPos + 62, 0.0F, 0.0F, 16, 16, 16, 16
      );
      guiGraphics.blit(
         ResourceLocation.parse("crusty_chunks:textures/screens/grayingot.png"), this.leftPos + 151, this.topPos + 35, 0.0F, 0.0F, 16, 16, 16, 16
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
      guiGraphics.drawString(this.font, Component.translatable("gui.crusty_chunks.blast_furnace_gui.label_blastfurnace"), 105, 7, -12829636, false);
   }

   public void init() {
      super.init();
   }
}
