package net.mcreator.crustychunks.procedures;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;

@EventBusSubscriber({Dist.CLIENT})
public class NVDRenderProcedure {
   private static BufferBuilder bufferBuilder = null;
   private static VertexBuffer vertexBuffer = null;
   private static Mode mode = null;
   private static VertexFormat format = null;
   private static PoseStack poseStack = null;
   private static Matrix4f projectionMatrix = null;
   private static boolean worldCoordinate = true;
   private static Vec3 offset = Vec3.ZERO;
   private static int currentStage = 0;
   private static int targetStage = 0;

   private static void add(double x, double y, double z, int color) {
      add(x, y, z, 0.0F, 0.0F, color);
   }

   private static void add(double x, double y, double z, float u, float v, int color) {
      if (bufferBuilder != null) {
         if (format == DefaultVertexFormat.POSITION_COLOR) {
            bufferBuilder.addVertex((float) x, (float) y, (float) z).setColor(color);
         } else if (format == DefaultVertexFormat.POSITION_TEX_COLOR) {
            bufferBuilder.addVertex((float) x, (float) y, (float) z).setUv(u, v).setColor(color);
         }
      }
   }

   private static boolean begin(Mode mode, VertexFormat format, boolean update) {
      if (bufferBuilder == null) {
         if (update) {
            clear();
         }

         if (vertexBuffer == null) {
            if (format == DefaultVertexFormat.POSITION_COLOR) {
               NVDRenderProcedure.mode = mode;
               NVDRenderProcedure.format = format;
               bufferBuilder = Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_COLOR);
               return true;
            }

            if (format == DefaultVertexFormat.POSITION_TEX_COLOR) {
               NVDRenderProcedure.mode = mode;
               NVDRenderProcedure.format = format;
               bufferBuilder = Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_TEX_COLOR);
               return true;
            }
         }
      }

      return false;
   }

   private static void clear() {
      if (vertexBuffer != null) {
         vertexBuffer.close();
         vertexBuffer = null;
      }
   }

   private static void end() {
      if (bufferBuilder != null) {
         if (vertexBuffer != null) {
            vertexBuffer.close();
         }

         vertexBuffer = new VertexBuffer(Usage.STATIC);
         vertexBuffer.bind();
         com.mojang.blaze3d.vertex.MeshData _mesh = bufferBuilder.build();
         if (_mesh != null)
            vertexBuffer.upload(_mesh);
         bufferBuilder = null;
         VertexBuffer.unbind();
      }
   }

   private static void offset(double x, double y, double z) {
      offset = new Vec3(x, y, z);
   }

   private static void release() {
      targetStage = 0;
   }

   private static VertexBuffer shape() {
      return vertexBuffer;
   }

   private static void system(boolean worldCoordinate) {
      NVDRenderProcedure.worldCoordinate = worldCoordinate;
   }

   private static boolean target(int targetStage) {
      if (targetStage == currentStage) {
         NVDRenderProcedure.targetStage = targetStage;
         return true;
      } else {
         return false;
      }
   }

   private static void renderShape(
      VertexBuffer vertexBuffer, double x, double y, double z, float yaw, float pitch, float roll, float xScale, float yScale, float zScale, int color
   ) {
      if (currentStage != 0 && currentStage == targetStage) {
         if (poseStack != null && projectionMatrix != null) {
            if (vertexBuffer != null) {
               float i;
               float j;
               float k;
               if (worldCoordinate) {
                  Vec3 pos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                  i = (float)(x - pos.x());
                  j = (float)(y - pos.y());
                  k = (float)(z - pos.z());
               } else {
                  i = (float)x;
                  j = (float)y;
                  k = (float)z;
               }

               poseStack.pushPose();
               poseStack.translate(i, j, k);
               poseStack.mulPose(Axis.YN.rotationDegrees(yaw));
               poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
               poseStack.mulPose(Axis.ZN.rotationDegrees(roll));
               poseStack.scale(xScale, yScale, zScale);
               poseStack.translate(offset.x(), offset.y(), offset.z());
               RenderSystem.setShaderColor(
                  (float)(color >> 16 & 0xFF) / 255.0F, (float)(color >> 8 & 0xFF) / 255.0F, (float)(color & 0xFF) / 255.0F, (float)(color >>> 24) / 255.0F
               );
               vertexBuffer.bind();
               vertexBuffer.drawWithShader(
                  poseStack.last().pose(), projectionMatrix, vertexBuffer.getFormat().hasUV(0) ? GameRenderer.getPositionTexColorShader() : GameRenderer.getPositionColorShader()
               );
               VertexBuffer.unbind();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               poseStack.popPose();
            }
         }
      }
   }

   @SubscribeEvent
   public static void renderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_SKY) {
         currentStage = 1;
         RenderSystem.depthMask(false);
         renderShapes(event);
         RenderSystem.enableCull();
         RenderSystem.depthMask(true);
         currentStage = 0;
      } else if (event.getStage() == Stage.AFTER_PARTICLES) {
         currentStage = 2;
         RenderSystem.depthMask(true);
         renderShapes(event);
         RenderSystem.enableCull();
         RenderSystem.depthMask(true);
         currentStage = 0;
      }
   }

   private static void renderShapes(RenderLevelStageEvent event) {
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      Entity entity = minecraft.gameRenderer.getMainCamera().getEntity();
      if (level != null && entity != null) {
         poseStack = event.getPoseStack();
         projectionMatrix = event.getProjectionMatrix();
         Vec3 pos = entity.getPosition(event.getPartialTick().getGameTimeDeltaPartialTick(true));
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         execute(event, entity);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
      }
   }

   public static void execute(Entity entity) {
      execute(null, entity);
   }

   private static void execute(@Nullable Event event, Entity entity) {
      if (entity != null) {
         double scale = 0.0;
         if ((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY).getItem()
            == CrustyChunksModItems.NVD_HELMET_HELMET.get()) {
            if (begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR, false)) {
               add(0.5, -0.5, -0.5, -1);
               add(0.5, -0.5, 0.5, -1);
               add(-0.5, -0.5, 0.5, -1);
               add(-0.5, -0.5, -0.5, -1);
               add(0.5, 0.5, 0.5, -1);
               add(0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, 0.5, -1);
               add(0.5, 0.5, -0.5, -1);
               add(0.5, -0.5, -0.5, -1);
               add(-0.5, -0.5, -0.5, -1);
               add(-0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, 0.5, -1);
               add(-0.5, -0.5, 0.5, -1);
               add(0.5, -0.5, 0.5, -1);
               add(0.5, 0.5, 0.5, -1);
               add(0.5, 0.5, 0.5, -1);
               add(0.5, -0.5, 0.5, -1);
               add(0.5, -0.5, -0.5, -1);
               add(0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, -0.5, -1);
               add(-0.5, -0.5, -0.5, -1);
               add(-0.5, -0.5, 0.5, -1);
               add(-0.5, 0.5, 0.5, -1);
               end();
            }

            if (target(2)) {
               RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
               renderShape(
                  shape(),
                  Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x(),
                  Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y(),
                  Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().z(),
                  0.0F,
                  0.0F,
                  0.0F,
                  -0.4F,
                  -0.4F,
                  -0.4F,
                  1694498815
               );
               release();
            }

            clear();
            if (begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR, false)) {
               add(0.5, -0.5, -0.5, -1);
               add(0.5, -0.5, 0.5, -1);
               add(-0.5, -0.5, 0.5, -1);
               add(-0.5, -0.5, -0.5, -1);
               add(0.5, 0.5, 0.5, -1);
               add(0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, 0.5, -1);
               add(0.5, 0.5, -0.5, -1);
               add(0.5, -0.5, -0.5, -1);
               add(-0.5, -0.5, -0.5, -1);
               add(-0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, 0.5, -1);
               add(-0.5, -0.5, 0.5, -1);
               add(0.5, -0.5, 0.5, -1);
               add(0.5, 0.5, 0.5, -1);
               add(0.5, 0.5, 0.5, -1);
               add(0.5, -0.5, 0.5, -1);
               add(0.5, -0.5, -0.5, -1);
               add(0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, -0.5, -1);
               add(-0.5, -0.5, -0.5, -1);
               add(-0.5, -0.5, 0.5, -1);
               add(-0.5, 0.5, 0.5, -1);
               end();
            }

            if (target(2)) {
               RenderSystem.blendFuncSeparate(SourceFactor.DST_COLOR, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
               renderShape(
                  shape(),
                  Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x(),
                  Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y(),
                  Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().z(),
                  0.0F,
                  0.0F,
                  0.0F,
                  -0.4F,
                  -0.4F,
                  -0.4F,
                  -16711936
               );
               release();
            }

            clear();
            if (begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR, false)) {
               add(0.5, -0.5, -0.5, -1);
               add(0.5, -0.5, 0.5, -1);
               add(-0.5, -0.5, 0.5, -1);
               add(-0.5, -0.5, -0.5, -1);
               add(0.5, 0.5, 0.5, -1);
               add(0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, 0.5, -1);
               add(0.5, 0.5, -0.5, -1);
               add(0.5, -0.5, -0.5, -1);
               add(-0.5, -0.5, -0.5, -1);
               add(-0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, 0.5, -1);
               add(-0.5, -0.5, 0.5, -1);
               add(0.5, -0.5, 0.5, -1);
               add(0.5, 0.5, 0.5, -1);
               add(0.5, 0.5, 0.5, -1);
               add(0.5, -0.5, 0.5, -1);
               add(0.5, -0.5, -0.5, -1);
               add(0.5, 0.5, -0.5, -1);
               add(-0.5, 0.5, -0.5, -1);
               add(-0.5, -0.5, -0.5, -1);
               add(-0.5, -0.5, 0.5, -1);
               add(-0.5, 0.5, 0.5, -1);
               end();
            }

            if (target(2)) {
               RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
               renderShape(
                  shape(),
                  Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x(),
                  Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y(),
                  Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().z(),
                  0.0F,
                  0.0F,
                  0.0F,
                  -0.4F,
                  -0.4F,
                  -0.4F,
                  436207615
               );
               release();
            }

            clear();
         }
      }
   }
}
