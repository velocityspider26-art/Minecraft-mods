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
import net.mcreator.crustychunks.entity.MuzzleFlashProducerEntity;
import net.mcreator.crustychunks.entity.SmallMuzzleFlashProducerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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
public class RenderProjectileFlashProcedure {
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
               RenderProjectileFlashProcedure.mode = mode;
               RenderProjectileFlashProcedure.format = format;
               bufferBuilder = Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_COLOR);
               return true;
            }

            if (format == DefaultVertexFormat.POSITION_TEX_COLOR) {
               RenderProjectileFlashProcedure.mode = mode;
               RenderProjectileFlashProcedure.format = format;
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
      RenderProjectileFlashProcedure.worldCoordinate = worldCoordinate;
   }

   private static boolean target(int targetStage) {
      if (targetStage == currentStage) {
         RenderProjectileFlashProcedure.targetStage = targetStage;
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
         execute(event, level);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
      }
   }

   public static void execute(LevelAccessor world) {
      try {
      execute(null, world);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RenderProjectileFlashProcedure.execute", _wtSafe);
      }
   }

   private static void execute(@Nullable Event event, LevelAccessor world) {
      try {
      double scale = 0.0;
      double yvector = 0.0;
      double vmagnitude = 0.0;
      double xvector = 0.0;
      double zvector = 0.0;
      double targetz = 0.0;
      double targetx = 0.0;
      double Yaw = 0.0;
      double pitch = 0.0;
      if (world instanceof ClientLevel) {
         for (Entity entityiterator : ((ClientLevel)world).entitiesForRendering()) {
            Yaw = Math.atan2(
                     Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x() - entityiterator.getX(),
                     Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().z() - entityiterator.getZ()
                  )
                  * (180.0 / Math.PI)
                  * -1.0
               + 180.0;
            pitch = Math.atan2(
                     Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y() - entityiterator.getY(),
                     Math.sqrt(
                        Math.pow(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x() - entityiterator.getX(), 2.0)
                           + Math.pow(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().z() - entityiterator.getZ(), 2.0)
                     )
                  )
                  * (180.0 / Math.PI)
                  * 1.0
               - 90.0;
            if (entityiterator instanceof MuzzleFlashProducerEntity) {
               if (entityiterator.getPersistentData().getDouble("Type") == 0.0) {
                  entityiterator.getPersistentData().putDouble("Type", (double)Mth.nextInt(RandomSource.create(), 1, 4));
               }

               scale = Mth.nextDouble(RandomSource.create(), 3.95, 4.05);
               if (begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR, false)) {
                  add(0.5, 0.0, 0.5, 0.0F, 0.0F, -1);
                  add(0.5, 0.0, -0.5, 0.0F, 1.0F, -1);
                  add(-0.5, 0.0, -0.5, 1.0F, 1.0F, -1);
                  add(-0.5, 0.0, 0.5, 1.0F, 0.0F, -1);
                  end();
               }

               if (target(2)) {
                  RenderSystem.depthMask(false);
                  RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
                  if (entityiterator.getPersistentData().getDouble("Type") == 1.0) {
                     RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/muzzleflash.png"));
                  } else if (entityiterator.getPersistentData().getDouble("Type") == 2.0) {
                     RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/muzzleflash2.png"));
                  } else if (entityiterator.getPersistentData().getDouble("Type") == 3.0) {
                     RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/muzzleflash3.png"));
                  } else {
                     RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/muzzleflash4.png"));
                  }

                  renderShape(
                     shape(),
                     entityiterator.getX(),
                     entityiterator.getY(),
                     entityiterator.getZ(),
                     (float)Yaw,
                     (float)pitch,
                     0.0F,
                     (float)scale,
                     (float)scale,
                     (float)scale,
                     -65536 | Mth.nextInt(RandomSource.create(), 245, 255) << 8 | Mth.nextInt(RandomSource.create(), 245, 255)
                  );
                  release();
               }

               clear();
            } else if (entityiterator instanceof SmallMuzzleFlashProducerEntity) {
               if (entityiterator.getPersistentData().getDouble("Type") == 0.0) {
                  entityiterator.getPersistentData().putDouble("Type", (double)Mth.nextInt(RandomSource.create(), 1, 4));
               }

               scale = Mth.nextDouble(RandomSource.create(), 2.4, 2.6);
               if (begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR, false)) {
                  add(0.5, 0.0, 0.5, 0.0F, 0.0F, -1);
                  add(0.5, 0.0, -0.5, 0.0F, 1.0F, -1);
                  add(-0.5, 0.0, -0.5, 1.0F, 1.0F, -1);
                  add(-0.5, 0.0, 0.5, 1.0F, 0.0F, -1);
                  end();
               }

               if (target(2)) {
                  RenderSystem.depthMask(false);
                  RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
                  if (entityiterator.getPersistentData().getDouble("Type") == 1.0) {
                     RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/muzzleflash.png"));
                  } else if (entityiterator.getPersistentData().getDouble("Type") == 2.0) {
                     RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/muzzleflash2.png"));
                  } else if (entityiterator.getPersistentData().getDouble("Type") == 3.0) {
                     RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/muzzleflash3.png"));
                  } else {
                     RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/muzzleflash4.png"));
                  }

                  renderShape(
                     shape(),
                     entityiterator.getX(),
                     entityiterator.getY(),
                     entityiterator.getZ(),
                     (float)Yaw,
                     (float)pitch,
                     0.0F,
                     (float)scale,
                     (float)scale,
                     (float)scale,
                     -65536 | Mth.nextInt(RandomSource.create(), 230, 245) << 8 | Mth.nextInt(RandomSource.create(), 230, 245)
                  );
                  release();
               }

               clear();
            } else if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:smallflash")))) {
               scale = Mth.nextDouble(RandomSource.create(), 15.0, 17.0);
               if (begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR, false)) {
                  add(0.5, 0.0, 0.5, 0.0F, 0.0F, -1);
                  add(0.5, 0.0, -0.5, 0.0F, 1.0F, -1);
                  add(-0.5, 0.0, -0.5, 1.0F, 1.0F, -1);
                  add(-0.5, 0.0, 0.5, 1.0F, 0.0F, -1);
                  end();
               }

               if (target(2)) {
                  RenderSystem.depthMask(false);
                  RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
                  RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/smallflash.png"));
                  renderShape(
                     shape(),
                     entityiterator.getX(),
                     entityiterator.getY(),
                     entityiterator.getZ(),
                     (float)Yaw,
                     (float)pitch,
                     0.0F,
                     (float)scale,
                     (float)scale,
                     (float)scale,
                     -65536 | Mth.nextInt(RandomSource.create(), 245, 255) << 8 | Mth.nextInt(RandomSource.create(), 245, 255)
                  );
                  release();
               }
            } else if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:smallrocketglare")))
               && entityiterator.getPersistentData().getDouble("Time") <= entityiterator.getPersistentData().getDouble("MaxTime")) {
               scale = Mth.nextDouble(RandomSource.create(), 6.0, 7.0);
               if (begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR, false)) {
                  add(0.5, 0.0, 0.5, 0.0F, 0.0F, -1);
                  add(0.5, 0.0, -0.5, 0.0F, 1.0F, -1);
                  add(-0.5, 0.0, -0.5, 1.0F, 1.0F, -1);
                  add(-0.5, 0.0, 0.5, 1.0F, 0.0F, -1);
                  end();
               }

               if (target(2)) {
                  RenderSystem.depthMask(false);
                  RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
                  RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/smallflash.png"));
                  renderShape(
                     shape(),
                     entityiterator.getX(),
                     entityiterator.getY(),
                     entityiterator.getZ(),
                     (float)Yaw,
                     (float)pitch,
                     0.0F,
                     (float)scale,
                     (float)scale,
                     (float)scale,
                     -65536 | Mth.nextInt(RandomSource.create(), 245, 255) << 8 | Mth.nextInt(RandomSource.create(), 245, 255)
                  );
                  release();
               }
            } else if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:largerocketglare")))
               && entityiterator.getPersistentData().getDouble("Time") <= entityiterator.getPersistentData().getDouble("MaxTime")) {
               scale = Mth.nextDouble(RandomSource.create(), 10.0, 11.0);
               if (begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR, false)) {
                  add(0.5, 0.0, 0.5, 0.0F, 0.0F, -1);
                  add(0.5, 0.0, -0.5, 0.0F, 1.0F, -1);
                  add(-0.5, 0.0, -0.5, 1.0F, 1.0F, -1);
                  add(-0.5, 0.0, 0.5, 1.0F, 0.0F, -1);
                  end();
               }

               if (target(2)) {
                  RenderSystem.depthMask(false);
                  RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
                  RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/smallflash.png"));
                  renderShape(
                     shape(),
                     entityiterator.getX(),
                     entityiterator.getY(),
                     entityiterator.getZ(),
                     (float)Yaw,
                     (float)pitch,
                     0.0F,
                     (float)scale,
                     (float)scale,
                     (float)scale,
                     -65536 | Mth.nextInt(RandomSource.create(), 245, 255) << 8 | Mth.nextInt(RandomSource.create(), 245, 255)
                  );
                  release();
               }
            } else if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:flare")))) {
               scale = Mth.nextDouble(RandomSource.create(), 7.0, 8.0);
               if (begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR, false)) {
                  add(0.5, 0.0, 0.5, 0.0F, 0.0F, -1);
                  add(0.5, 0.0, -0.5, 0.0F, 1.0F, -1);
                  add(-0.5, 0.0, -0.5, 1.0F, 1.0F, -1);
                  add(-0.5, 0.0, 0.5, 1.0F, 0.0F, -1);
                  end();
               }

               if (target(2)) {
                  RenderSystem.depthMask(false);
                  RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
                  RenderSystem.setShaderTexture(0, ResourceLocation.parse("crusty_chunks:textures/flareglare.png"));
                  renderShape(
                     shape(),
                     entityiterator.getX(),
                     entityiterator.getY(),
                     entityiterator.getZ(),
                     (float)Yaw,
                     (float)pitch,
                     0.0F,
                     (float)scale,
                     (float)scale,
                     (float)scale,
                     -65536 | Mth.nextInt(RandomSource.create(), 245, 255) << 8 | Mth.nextInt(RandomSource.create(), 245, 255)
                  );
                  release();
               }
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RenderProjectileFlashProcedure.execute", _wtSafe);
      }
   }
}
