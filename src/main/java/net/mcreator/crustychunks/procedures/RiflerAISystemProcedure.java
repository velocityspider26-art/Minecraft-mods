package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;

public class RiflerAISystemProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         RandomVoicelinesProcedure.execute(world, x, y, z, entity);
         AutoscoutingProcedure.execute(world, x, y, z, entity);
         if (entity.getPersistentData().getDouble("T") > 0.0) {
            entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") - 1.0);
         }

         if ((entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null) != null && (entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null).isAlive()) {
            entity.lookAt(
               Anchor.EYES,
               new Vec3(
                  (entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null).getX(),
                  (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getY() + 1.0,
                  (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getZ()
               )
            );
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1.0F) > 0.0F
               && Math.abs((entity instanceof Mob _mobEntxxxxxxx ? _mobEntxxxxxxx.getTarget() : null).getY() - y)
                     - (
                        Math.abs(
                              (double)entity.level()
                                    .clip(
                                       new ClipContext(
                                          entity.getEyePosition(1.0F),
                                          entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(512.0)),
                                          Block.COLLIDER,
                                          Fluid.ANY,
                                          entity
                                       )
                                    )
                                    .getBlockPos()
                                    .getY()
                                 - y
                           )
                           + 0.5
                     )
                  <= 0.0
               && Math.abs((entity instanceof Mob _mobEntxxxxxx ? _mobEntxxxxxx.getTarget() : null).getX() - x)
                     - (
                        Math.abs(
                              (double)entity.level()
                                    .clip(
                                       new ClipContext(
                                          entity.getEyePosition(1.0F),
                                          entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(512.0)),
                                          Block.COLLIDER,
                                          Fluid.ANY,
                                          entity
                                       )
                                    )
                                    .getBlockPos()
                                    .getX()
                                 - x
                           )
                           + 0.5
                     )
                  <= 0.0
               && Math.abs((entity instanceof Mob _mobEntxxxxx ? _mobEntxxxxx.getTarget() : null).getZ() - z)
                     - (
                        Math.abs(
                              (double)entity.level()
                                    .clip(
                                       new ClipContext(
                                          entity.getEyePosition(1.0F),
                                          entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(512.0)),
                                          Block.COLLIDER,
                                          Fluid.ANY,
                                          entity
                                       )
                                    )
                                    .getBlockPos()
                                    .getZ()
                                 - z
                           )
                           + 0.5
                     )
                  <= 0.0) {
               CrustyChunksMod.queueServerWork(
                  10,
                  () -> {
                     if (entity.getPersistentData().getDouble("T") <= 0.0 && (entity instanceof Mob _mobEntxxxxxxxx ? _mobEntxxxxxxxx.getTarget() : null) != null
                        )
                      {
                        entity.lookAt(
                           Anchor.EYES,
                           new Vec3(
                              (entity instanceof Mob _mobEntxxxxxxxxxxx ? _mobEntxxxxxxxxxxx.getTarget() : null).getX(),
                              (entity instanceof Mob _mobEntxxxxxxxxxx ? _mobEntxxxxxxxxxx.getTarget() : null).getY() + 1.0,
                              (entity instanceof Mob _mobEntxxxxxxxxx ? _mobEntxxxxxxxxx.getTarget() : null).getZ()
                           )
                        );
                        EliteRifleProcedure.execute(world, x, y, z, entity);
                     }
                  }
               );
            }

            if ((entity instanceof Mob _mobEntxxxxx ? _mobEntxxxxx.getTarget() : null)
                  .getType()
                  .is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))
               && entity instanceof Mob) {
               try {
                  ((Mob)entity).setTarget(null);
               } catch (Exception var14) {
                  var14.printStackTrace();
               }
            }
         }

         if (entity.getPersistentData().getDouble("Cycle") > 0.0) {
            entity.getPersistentData().putDouble("Cycle", entity.getPersistentData().getDouble("Cycle") - 1.0);
         } else if (entity.getDeltaMovement().x() != 0.0 && entity.getDeltaMovement().z() != 0.0) {
            entity.getPersistentData().putDouble("Cycle", 5.0);
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:strikerstep")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.3, 1.5)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:strikerstep")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.3, 1.5),
                     false
                  );
               }
            }
         }

         ClankerfloatProcedure.execute(world, x, y, z, entity);
      }
   }
}
