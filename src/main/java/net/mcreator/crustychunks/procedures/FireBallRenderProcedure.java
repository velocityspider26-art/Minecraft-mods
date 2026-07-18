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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;

@EventBusSubscriber({Dist.CLIENT})
public class FireBallRenderProcedure {
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
               FireBallRenderProcedure.mode = mode;
               FireBallRenderProcedure.format = format;
               bufferBuilder = Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_COLOR);
               return true;
            }

            if (format == DefaultVertexFormat.POSITION_TEX_COLOR) {
               FireBallRenderProcedure.mode = mode;
               FireBallRenderProcedure.format = format;
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

   private static void release() {
      targetStage = 0;
   }

   private static VertexBuffer shape() {
      return vertexBuffer;
   }

   private static boolean target(int targetStage) {
      if (targetStage == currentStage) {
         FireBallRenderProcedure.targetStage = targetStage;
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
      if (minecraft.level == null)
         return;
      poseStack = event.getPoseStack();
      projectionMatrix = event.getProjectionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      execute(null, minecraft.level);
      RenderSystem.disableBlend();
   }

   public static void execute(LevelAccessor world) {
      execute(null, world);
   }

   public static void execute(@Nullable Event event, LevelAccessor world) {
      try {
      Minecraft mc = Minecraft.getInstance();
      Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
      synchronized (WariumExplosionClientProcedure.ACTIVE_NUKES) {
         for (WariumExplosionClientProcedure.NuclearBlast nuke : WariumExplosionClientProcedure.ACTIVE_NUKES) {
            double T = nuke.time;
            // Glare (450 - T/2) and sky flash (255 - T) are both fully faded by T=900:
            // skip the buffer build/upload entirely instead of drawing invisible quads
            // every frame for the rest of the nuke's multi-minute lifetime.
            if (T >= 910.0) {
               continue;
            }
            float yaw = (float)(Math.atan2(cam.x - nuke.x, cam.z - nuke.z) * (180.0 / Math.PI) * -1.0 + 180.0);
            float pitch = (float)(
               Math.atan2(cam.y - nuke.y, Math.sqrt(Math.pow(cam.x - nuke.x, 2.0) + Math.pow(cam.z - nuke.z, 2.0)))
                     * (180.0 / Math.PI)
                     * 1.0
                  - 90.0
            );
            double scale = Math.max(255.0 - T, 0.0);
            double glareScale = nuke.power >= 60.0 ? Math.max(450.0 - T / 2.0, 0.0) : Math.max(250.0 - T / 2.0, 0.0);
            String tex = nuke.power >= 60.0 ? "purpleglare" : "yellowglare";
            if (begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR, false)) {
               add(0.5, 0.0, 0.5, 0.0F, 0.0F, -1);
               add(0.5, 0.0, -0.5, 0.0F, 1.0F, -1);
               add(-0.5, 0.0, -0.5, 1.0F, 1.0F, -1);
               add(-0.5, 0.0, 0.5, 1.0F, 0.0F, -1);
               end();
            }

            if (target(2)) {
               RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/" + tex + ".png"));
               renderShape(shape(), nuke.x, nuke.y + T / 10.0, nuke.z, yaw, pitch, 0.0F, (float)glareScale, (float)glareScale, (float)glareScale, -1);
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

            if (target(1)) {
               RenderSystem.disableDepthTest();
               RenderSystem.depthMask(false);
               RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
               int skyCol = (int)Math.max(Math.min(200.0, scale), 0.0) << 24 | (int)Math.min(255.0, scale) << 16 | (int)Math.min(255.0, scale) << 8 | 0xFF;
               renderShape(shape(), cam.x, cam.y, cam.z, 0.0F, 0.0F, 0.0F, -2.0F, -1.0F, -1.0F, skyCol);
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(true);
               RenderSystem.defaultBlendFunc();
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
               int worldCol = nuke.power >= 60.0
                  ? (int)Math.max(scale / 3.0, 0.0) << 24 | 0xFF0000 | (int)scale << 8 | (int)scale
                  : (int)Math.max(scale / 3.0, 0.0) << 24 | 0xFF0000 | (int)scale << 8 | (int)scale;
               renderShape(shape(), cam.x, cam.y, cam.z, 0.0F, 0.0F, 0.0F, -1.0F, -1.0F, -1.0F, worldCol);
               RenderSystem.defaultBlendFunc();
               release();
            }

            clear();
         }
      }

      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FireBallRenderProcedure.execute", _wtSafe);
      }
   }
}
