package net.mcreator.crustychunks.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.network.MineralGrinderGUIButtonMessage;
import net.mcreator.crustychunks.world.inventory.MineralGrinderGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class MineralGrinderGUIScreen extends AbstractContainerScreen<MineralGrinderGUIMenu> {
   private static final HashMap<String, Object> guistate = MineralGrinderGUIMenu.guistate;
   private final Level world;
   private final int x;
   private final int y;
   private final int z;
   private final Player entity;
   Button button_process;
   private static final ResourceLocation texture = ResourceLocation.parse("crusty_chunks:textures/screens/mineral_grinder_gui.png");

   public MineralGrinderGUIScreen(MineralGrinderGUIMenu container, Inventory inventory, Component text) {
      super(container, inventory, text);
      this.world = container.world;
      this.x = container.x;
      this.y = container.y;
      this.z = container.z;
      this.entity = container.entity;
      this.imageWidth = 172;
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
         ResourceLocation.parse("crusty_chunks:textures/screens/graydust.png"), this.leftPos + 77, this.topPos + 53, 0.0F, 0.0F, 16, 16, 16, 16
      );
      guiGraphics.blit(
         ResourceLocation.parse("crusty_chunks:textures/screens/graydust.png"), this.leftPos + 104, this.topPos + 53, 0.0F, 0.0F, 16, 16, 16, 16
      );
      guiGraphics.blit(
         ResourceLocation.parse("crusty_chunks:textures/screens/rawgray.png"), this.leftPos + 77, this.topPos + 8, 0.0F, 0.0F, 16, 16, 16, 16
      );
      guiGraphics.blit(
         ResourceLocation.parse("crusty_chunks:textures/screens/graygear.png"), this.leftPos + 41, this.topPos + 26, 0.0F, 0.0F, 16, 16, 16, 16
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
      this.button_process = Button.builder(Component.translatable("gui.crusty_chunks.mineral_grinder_gui.button_process"), e -> {
         PacketDistributor.sendToServer(new MineralGrinderGUIButtonMessage(0, this.x, this.y, this.z));
         MineralGrinderGUIButtonMessage.handleButtonAction(this.entity, 0, this.x, this.y, this.z);
      }).bounds(this.leftPos + 102, this.topPos + 7, 61, 20).build();
      guistate.put("button:button_process", this.button_process);
      this.addRenderableWidget(this.button_process);
   }
}
