package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber({Dist.CLIENT})
public class ProjectilesTracersProcedure {
   private static RenderLevelStageEvent provider = null;
   private static Map<EntityType, Entity> data = new HashMap<>();

   public static void renderBackground(String texts, double x, double y, double z, float yaw, float pitch, float roll, float scale, int color) {
      Minecraft minecraft = Minecraft.getInstance();
      Font font = minecraft.font;
      BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
      Vec3 pos = provider.getCamera().getPosition();
      PoseStack poseStack = provider.getPoseStack();
      poseStack.pushPose();
      poseStack.translate(x - pos.x(), y - pos.y(), z - pos.z());
      poseStack.mulPose(Axis.YN.rotationDegrees(yaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
      poseStack.mulPose(Axis.ZN.rotationDegrees(roll));
      poseStack.scale(scale, -scale, 1.0F);
      poseStack.translate((float)(font.width(texts) - 1) * -0.5F, (float)(9 - 1) * -0.5F, 0.0F);
      Matrix4f matrix4f = poseStack.last().pose();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      font.drawInBatch(texts, 0.0F, 0.0F, 0, false, matrix4f, bufferSource, DisplayMode.SEE_THROUGH, color, 15728880);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      poseStack.popPose();
   }

   public static void renderBlock(BlockState blockState, double x, double y, double z, float yaw, float pitch, float roll, float scale, boolean glowing) {
      BlockPos blockPos = BlockPos.containing(x, y, z);
      Vec3 pos = provider.getCamera().getPosition();
      int packedLight = glowing ? 15728880 : LevelRenderer.getLightColor(Minecraft.getInstance().level, blockPos);
      PoseStack poseStack = provider.getPoseStack();
      poseStack.pushPose();
      poseStack.translate(x - pos.x(), y - pos.y(), z - pos.z());
      poseStack.mulPose(Axis.YN.rotationDegrees(yaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
      poseStack.mulPose(Axis.ZN.rotationDegrees(roll));
      poseStack.scale(scale, scale, scale);
      poseStack.translate(-0.5F, -0.5F, -0.5F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      renderBlockModel(blockState, blockPos, poseStack, packedLight);
      renderBlockEntity(blockState, blockPos, poseStack, packedLight);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      poseStack.popPose();
   }

   private static void renderBlockEntity(BlockState blockState, BlockPos blockPos, PoseStack poseStack, int packedLight) {
      if (blockState.getBlock() instanceof EntityBlock entityBlock) {
         Minecraft minecraft = Minecraft.getInstance();
         ClientLevel level = minecraft.level;
         BlockEntity blockEntity = entityBlock.newBlockEntity(blockPos, blockState);
         if (blockEntity != null) {
            BlockEntityRenderer blockEntityRenderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
            if (blockEntityRenderer != null) {
               BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
               blockEntity.setLevel(level);
               blockEntityRenderer.render(blockEntity, 0.0F, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
            }
         }
      }
   }

   private static void renderBlockModel(BlockState blockState, BlockPos blockPos, PoseStack poseStack, int packedLight) {
      if (blockState.getRenderShape() == RenderShape.MODEL) {
         Minecraft minecraft = Minecraft.getInstance();
         ClientLevel level = minecraft.level;
         BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
         BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
         ModelBlockRenderer renderer = dispatcher.getModelRenderer();
         BakedModel bakedModel = dispatcher.getBlockModel(blockState);
         ModelData modelData = bakedModel.getModelData(level, blockPos, blockState, ModelData.builder().build());
         Pose pose = poseStack.last();
         int color = minecraft.getBlockColors().getColor(blockState, level, blockPos);
         float red = (float)(color >> 16 & 0xFF) / 255.0F;
         float green = (float)(color >> 8 & 0xFF) / 255.0F;
         float blue = (float)(color & 0xFF) / 255.0F;

         for (RenderType renderType : bakedModel.getRenderTypes(blockState, RandomSource.create(42L), modelData)) {
            renderer.renderModel(
               pose,
               bufferSource.getBuffer(Sheets.translucentCullBlockSheet()),
               blockState,
               bakedModel,
               red,
               green,
               blue,
               packedLight,
               OverlayTexture.NO_OVERLAY,
               modelData,
               renderType
            );
         }
      }
   }

   public static void renderEntity(EntityType type, double x, double y, double z, float yaw, float pitch, float roll, float scale, boolean glowing) {
      if (type != null) {
         ClientLevel level = Minecraft.getInstance().level;
         Entity entity;
         if (data.containsKey(type)) {
            entity = data.get(type);
            if (entity.level() != level) {
               entity = type.create(level);
               data.put(type, entity);
            }
         } else {
            entity = type.create(level);
            data.put(type, entity);
         }

         renderEntity(entity, 0.0F, x, y, z, yaw, pitch, roll, scale, glowing ? 15728880 : LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z)));
      }
   }

   public static void renderEntity(Entity entity, double x, double y, double z, float yaw, float pitch, float roll, float scale, boolean glowing) {
      float partialTick = provider.getPartialTick().getGameTimeDeltaPartialTick(true);
      int packedLight = glowing ? 15728880 : Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(entity, partialTick);
      renderEntity(entity, partialTick, x, y, z, yaw, pitch, roll, scale, packedLight);
   }

   private static void renderEntity(
      Entity entity, float partialTick, double x, double y, double z, float yaw, float pitch, float roll, float scale, int packedLight
   ) {
      if (entity != null) {
         Minecraft minecraft = Minecraft.getInstance();
         BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
         EntityRenderer renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
         Vec3 pos = provider.getCamera().getPosition();
         float offset = entity.getBbHeight() / 2.0F * scale;
         PoseStack poseStack = provider.getPoseStack();
         poseStack.pushPose();
         poseStack.translate(x - pos.x(), y + (double)offset - pos.y(), z - pos.z());
         poseStack.mulPose(Axis.YN.rotationDegrees(yaw));
         poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
         poseStack.mulPose(Axis.ZN.rotationDegrees(roll));
         poseStack.translate(0.0F, -offset, 0.0F);
         poseStack.scale(scale, scale, scale);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         renderer.render(entity, entity.getViewYRot(partialTick), partialTick, poseStack, bufferSource, packedLight);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         poseStack.popPose();
      }
   }

   public static void renderItem(
      ItemStack itemStack, double x, double y, double z, float yaw, float pitch, float roll, float scale, boolean flipping, boolean glowing
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
      ItemRenderer renderer = minecraft.getItemRenderer();
      Vec3 pos = provider.getCamera().getPosition();
      int packedLight = glowing ? 15728880 : LevelRenderer.getLightColor(minecraft.level, BlockPos.containing(x, y, z));
      PoseStack poseStack = provider.getPoseStack();
      poseStack.pushPose();
      poseStack.translate(x - pos.x(), y - pos.y(), z - pos.z());
      poseStack.mulPose(Axis.YN.rotationDegrees(yaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
      poseStack.mulPose(Axis.ZN.rotationDegrees(roll));
      poseStack.scale(scale, scale, scale);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      renderer.renderStatic(
         null, itemStack, ItemDisplayContext.FIXED, flipping, poseStack, bufferSource, minecraft.level, packedLight, OverlayTexture.NO_OVERLAY, 0
      );
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      poseStack.popPose();
   }

   public static void renderLine(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
      Vec3 pos = provider.getCamera().getPosition();
      Vector3f normal = new Vec3(x2 - x1, y2 - y1, z2 - z1).normalize().toVector3f();
      Matrix4f matrix4f = provider.getPoseStack().last().pose();
      VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
      vertexConsumer.addVertex(matrix4f, (float)(x1 - pos.x()), (float)(y1 - pos.y()), (float)(z1 - pos.z()))
         .setColor(color)
         .setNormal(normal.x(), normal.y(), normal.z())
         ;
      vertexConsumer.addVertex(matrix4f, (float)(x2 - pos.x()), (float)(y2 - pos.y()), (float)(z2 - pos.z()))
         .setColor(color)
         .setNormal(normal.x(), normal.y(), normal.z())
         ;
   }

   public static void renderTexts(String texts, double x, double y, double z, float yaw, float pitch, float roll, float scale, int color, boolean glowing) {
      Minecraft minecraft = Minecraft.getInstance();
      Font font = minecraft.font;
      BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
      Vec3 pos = provider.getCamera().getPosition();
      int packedLight = glowing ? 15728880 : LevelRenderer.getLightColor(minecraft.level, BlockPos.containing(x, y, z));
      PoseStack poseStack = provider.getPoseStack();
      poseStack.pushPose();
      poseStack.translate(x - pos.x(), y - pos.y(), z - pos.z());
      poseStack.mulPose(Axis.YN.rotationDegrees(yaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
      poseStack.mulPose(Axis.ZN.rotationDegrees(roll));
      poseStack.scale(scale, -scale, 1.0F);
      poseStack.translate((float)(font.width(texts) - 1) * -0.5F, (float)(9 - 1) * -0.5F, 0.0F);
      Matrix4f matrix4f = poseStack.last().pose();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      font.drawInBatch(texts, 0.0F, 0.0F, color, false, matrix4f, bufferSource, DisplayMode.NORMAL, 0, packedLight);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      poseStack.popPose();
   }

   @SubscribeEvent
   public static void renderModels(RenderLevelStageEvent event) {
      provider = event;
      if (provider.getStage() == Stage.AFTER_ENTITIES) {
         ClientLevel level = Minecraft.getInstance().level;
         Entity entity = provider.getCamera().getEntity();
         Vec3 pos = entity.getPosition(provider.getPartialTick().getGameTimeDeltaPartialTick(true));
         execute(provider, level, (double)provider.getPartialTick().getGameTimeDeltaPartialTick(true));
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
      }
   }

   public static void execute(LevelAccessor world, double partialTick) {
      execute(null, world, partialTick);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, double partialTick) {
      double xvector = 0.0;
      double yvector = 0.0;
      double zvector = 0.0;
      double pmagnitude = 0.0;
      double vmagnitude = 0.0;
      double scale = 0.0;
      double watersurfaceheight = 0.0;
      double mvmultiplier = 0.0;
      mvmultiplier = ProjectilelibsProcedure.execute();
      if (world instanceof ClientLevel) {
         for (Entity entityiterator : ((ClientLevel)world).entitiesForRendering()) {
            if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:redtracersmall")))
               || entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:greentracersmall")))
               || entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:yellowtracersmall")))
               || entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:sparktracer")))) {
               entityiterator.getPersistentData().putDouble("VX", entityiterator.getDeltaMovement().x());
               entityiterator.getPersistentData().putDouble("VY", entityiterator.getDeltaMovement().y());
               entityiterator.getPersistentData().putDouble("VZ", entityiterator.getDeltaMovement().z());
               vmagnitude = (entityiterator instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) * 1.5 / mvmultiplier;
               xvector = entityiterator.getViewVector((float)partialTick).x() * -1.0 * vmagnitude;
               yvector = entityiterator.getViewVector((float)partialTick).y() * -1.0 * vmagnitude;
               zvector = entityiterator.getViewVector((float)partialTick).z() * vmagnitude;
               if (4.0 >= entityiterator.getPersistentData().getDouble("Tracking")) {
                  entityiterator.getPersistentData().putDouble("Tracking", entityiterator.getPersistentData().getDouble("Tracking") + 1.0);
               } else if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:redtracersmall")))) {
                  RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
                  renderLine(
                     entityiterator.getPosition((float)partialTick).x(),
                     entityiterator.getPosition((float)partialTick).y(),
                     entityiterator.getPosition((float)partialTick).z(),
                     entityiterator.getPosition((float)partialTick).x() - xvector * 2.0,
                     entityiterator.getPosition((float)partialTick).y() - yvector * 2.0,
                     entityiterator.getPosition((float)partialTick).z() - zvector * 2.0,
                     -46261
                  );
                  renderLine(
                     entityiterator.getPosition((float)partialTick).x(),
                     entityiterator.getPosition((float)partialTick).y(),
                     entityiterator.getPosition((float)partialTick).z(),
                     entityiterator.getPosition((float)partialTick).x() - xvector * 1.5,
                     entityiterator.getPosition((float)partialTick).y() - yvector * 1.5,
                     entityiterator.getPosition((float)partialTick).z() - zvector * 1.5,
                     -20636
                  );
                  renderLine(
                     entityiterator.getPosition((float)partialTick).x(),
                     entityiterator.getPosition((float)partialTick).y(),
                     entityiterator.getPosition((float)partialTick).z(),
                     entityiterator.getPosition((float)partialTick).x() - xvector * 0.5,
                     entityiterator.getPosition((float)partialTick).y() - yvector * 0.5,
                     entityiterator.getPosition((float)partialTick).z() - zvector * 0.5,
                     -1
                  );
               } else if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:greentracersmall")))) {
                  RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
                  renderLine(
                     entityiterator.getPosition((float)partialTick).x(),
                     entityiterator.getPosition((float)partialTick).y(),
                     entityiterator.getPosition((float)partialTick).z(),
                     entityiterator.getPosition((float)partialTick).x() - xvector * 2.0,
                     entityiterator.getPosition((float)partialTick).y() - yvector * 2.0,
                     entityiterator.getPosition((float)partialTick).z() - zvector * 2.0,
                     -11796661
                  );
                  renderLine(
                     entityiterator.getPosition((float)partialTick).x(),
                     entityiterator.getPosition((float)partialTick).y(),
                     entityiterator.getPosition((float)partialTick).z(),
                     entityiterator.getPosition((float)partialTick).x() - xvector * 1.5,
                     entityiterator.getPosition((float)partialTick).y() - yvector * 1.5,
                     entityiterator.getPosition((float)partialTick).z() - zvector * 1.5,
                     -5242961
                  );
                  renderLine(
                     entityiterator.getPosition((float)partialTick).x(),
                     entityiterator.getPosition((float)partialTick).y(),
                     entityiterator.getPosition((float)partialTick).z(),
                     entityiterator.getPosition((float)partialTick).x() - xvector * 0.5,
                     entityiterator.getPosition((float)partialTick).y() - yvector * 0.5,
                     entityiterator.getPosition((float)partialTick).z() - zvector * 0.5,
                     -1
                  );
               } else if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:yellowtracersmall")))) {
                  RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
                  renderLine(
                     entityiterator.getPosition((float)partialTick).x(),
                     entityiterator.getPosition((float)partialTick).y(),
                     entityiterator.getPosition((float)partialTick).z(),
                     entityiterator.getPosition((float)partialTick).x() - xvector * 2.0,
                     entityiterator.getPosition((float)partialTick).y() - yvector * 2.0,
                     entityiterator.getPosition((float)partialTick).z() - zvector * 2.0,
                     -20686
                  );
                  renderLine(
                     entityiterator.getPosition((float)partialTick).x(),
                     entityiterator.getPosition((float)partialTick).y(),
                     entityiterator.getPosition((float)partialTick).z(),
                     entityiterator.getPosition((float)partialTick).x() - xvector * 1.5,
                     entityiterator.getPosition((float)partialTick).y() - yvector * 1.5,
                     entityiterator.getPosition((float)partialTick).z() - zvector * 1.5,
                     -156
                  );
                  renderLine(
                     entityiterator.getPosition((float)partialTick).x(),
                     entityiterator.getPosition((float)partialTick).y(),
                     entityiterator.getPosition((float)partialTick).z(),
                     entityiterator.getPosition((float)partialTick).x() - xvector * 0.5,
                     entityiterator.getPosition((float)partialTick).y() - yvector * 0.5,
                     entityiterator.getPosition((float)partialTick).z() - zvector * 0.5,
                     -1
                  );
               }

               if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:sparktracer")))) {
                  RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
                  renderLine(
                     entityiterator.getPosition((float)partialTick).x(),
                     entityiterator.getPosition((float)partialTick).y(),
                     entityiterator.getPosition((float)partialTick).z(),
                     entityiterator.getPosition((float)partialTick).x() - xvector * 0.5,
                     entityiterator.getPosition((float)partialTick).y() - yvector * 0.5,
                     entityiterator.getPosition((float)partialTick).z() - zvector * 0.5,
                     -1
                  );
               }
            }

            if (world.getBlockState(BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ())).getBlock() instanceof LiquidBlock
               && !entityiterator.getPersistentData().getBoolean("splashed")
               && (
                  entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:artillery")))
                     || entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))
               )) {
               entityiterator.getPersistentData().putBoolean("splashed", true);

               for (int index0 = 0; index0 < 7; index0++) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.SMALL_SPLASH_PUFF.get(),
                     entityiterator.getX(),
                     entityiterator.getY() + Mth.nextDouble(RandomSource.create(), 0.9, 2.0),
                     entityiterator.getZ(),
                     0.0 + Mth.nextDouble(RandomSource.create(), -0.25, 0.25),
                     Mth.nextDouble(RandomSource.create(), -0.1, 0.5),
                     0.0 + Mth.nextDouble(RandomSource.create(), -0.25, 0.25)
                  );
               }

               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.player.splash.high_speed")),
                        SoundSource.BLOCKS,
                        5.0F,
                        2.0F
                     );
                  } else {
                     _level.playLocalSound(
                        entityiterator.getX(),
                        entityiterator.getY(),
                        entityiterator.getZ(),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.player.splash.high_speed")),
                        SoundSource.BLOCKS,
                        5.0F,
                        2.0F,
                        false
                     );
                  }
               }
            }
         }
      }
   }
}
