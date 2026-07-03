package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber({Dist.CLIENT})
public class ProjectileWizzProcedure {
   @SubscribeEvent
   public static void updateWorldTick(ClientTickEvent.Post event) {
      if (false) {
         Minecraft minecraft = Minecraft.getInstance();
         ClientLevel level = minecraft.level;
         Entity entity = minecraft.gameRenderer.getMainCamera().getEntity();
         if (level != null && entity != null) {
            Vec3 pos = entity.getPosition(minecraft.getTimer().getGameTimeDeltaPartialTick(true));
            execute(event, level, pos.x(), pos.y(), pos.z());
         }
      }
   }

   public static void execute(LevelAccessor world, double x, double y, double z) {
      execute(null, world, x, y, z);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z) {
      double locationy = 0.0;
      double distancewithvector = 0.0;
      double locationz = 0.0;
      double distance = 0.0;
      double locationx = 0.0;
      Vec3 _center = new Vec3(x, y, z);

      for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(15.0), e -> true)
         .stream()
         .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
         .toList()) {
         if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))
            && !entityiterator.getPersistentData().getBoolean("Passed")) {
            distance = Math.sqrt(
               Math.pow(x - entityiterator.getX(), 2.0) + Math.pow(y - entityiterator.getY(), 2.0) + Math.pow(z - entityiterator.getZ(), 2.0)
            );
            distancewithvector = Math.sqrt(
               Math.pow(x - (entityiterator.getX() + entityiterator.getDeltaMovement().x()), 2.0)
                  + Math.pow(y - (entityiterator.getY() + entityiterator.getDeltaMovement().y()), 2.0)
                  + Math.pow(z - (entityiterator.getZ() + entityiterator.getDeltaMovement().z()), 2.0)
            );
            if (distancewithvector < distance) {
               entityiterator.getPersistentData().putBoolean("Passed", true);
               if (world instanceof Level) {
                  Level _level = (Level)world;
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bulletcrack")),
                        SoundSource.BLOCKS,
                        3.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05)
                     );
                  } else {
                     _level.playLocalSound(
                        entityiterator.getX(),
                        entityiterator.getY(),
                        entityiterator.getZ(),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:bulletcrack")),
                        SoundSource.BLOCKS,
                        3.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05),
                        false
                     );
                  }
               }
            }
         }
      }

      Vec3 _center1 = new Vec3(x, y, z);

      for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_center1, _center1).inflate(100.0), e -> true)
         .stream()
         .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center1)))
         .toList()) {
         if (entityiteratorx.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:artillery")))
            && !entityiteratorx.getPersistentData().getBoolean("Passed")) {
            distance = Math.sqrt(
               Math.pow(x - entityiteratorx.getX(), 2.0) + Math.pow(y - entityiteratorx.getY(), 2.0) + Math.pow(z - entityiteratorx.getZ(), 2.0)
            );
            distancewithvector = Math.sqrt(
               Math.pow(x - (entityiteratorx.getX() + entityiteratorx.getDeltaMovement().x()), 2.0)
                  + Math.pow(y - (entityiteratorx.getY() + entityiteratorx.getDeltaMovement().y()), 2.0)
                  + Math.pow(z - (entityiteratorx.getZ() + entityiteratorx.getDeltaMovement().z()), 2.0)
            );
            if (distancewithvector < distance) {
               entityiteratorx.getPersistentData().putBoolean("Passed", true);
               if (world instanceof Level) {
                  Level _level = (Level)world;
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(entityiteratorx.getX(), entityiteratorx.getY(), entityiteratorx.getZ()),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:artyfall")),
                        SoundSource.BLOCKS,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05)
                     );
                  } else {
                     _level.playLocalSound(
                        entityiteratorx.getX(),
                        entityiteratorx.getY(),
                        entityiteratorx.getZ(),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:artyfall")),
                        SoundSource.BLOCKS,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05),
                        false
                     );
                  }
               }
            }
         }
      }
   }
}
