package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;

public class LargeTorpedoFlightProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         immediatesourceentity.getPersistentData().putDouble("Time", immediatesourceentity.getPersistentData().getDouble("Time") + 1.0);
         if (immediatesourceentity.isUnderWater()) {
            immediatesourceentity.setNoGravity(true);
            if (immediatesourceentity.getPersistentData().getDouble("Time") >= 20.0) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.SPLASH_PUFF.get(),
                  x + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  y + Mth.nextDouble(RandomSource.create(), 0.1, 0.3),
                  z + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
               );
               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.swim")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.9)
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.swim")),
                        SoundSource.NEUTRAL,
                        3.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.9),
                        false
                     );
                  }
               }

               if (world instanceof ServerLevel _levelx) {
                  _levelx.sendParticles(ParticleTypes.SPLASH, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
               }

               if (world.getBlockState(BlockPos.containing(x, y + 1.5, z)).getBlock() instanceof LiquidBlock) {
                  immediatesourceentity.setDeltaMovement(new Vec3(immediatesourceentity.getLookAngle().x * -1.0, 0.4, immediatesourceentity.getLookAngle().z));
               } else {
                  immediatesourceentity.setDeltaMovement(new Vec3(immediatesourceentity.getLookAngle().x * -1.0, -0.2, immediatesourceentity.getLookAngle().z));
               }
            }
         } else {
            immediatesourceentity.setNoGravity(false);
         }

         if (immediatesourceentity.getPersistentData().getDouble("Time") >= 400.0) {
            LargeTorpedoHitProcedure.execute(world, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }

         if (world instanceof ServerLevel _world) {
            net.mcreator.crustychunks.utils.WariumChunkLoader.TICKET_CONTROLLER.forceChunk(_world,
               new BlockPos(
                  world.getChunk(new BlockPos((int)x, (int)y, (int)z)).getPos().x,
                  0,
                  world.getChunk(new BlockPos((int)x, (int)y, (int)z)).getPos().z
               ),
               world.getChunk(new BlockPos((int)x, (int)y, (int)z)).getPos().x,
               world.getChunk(new BlockPos((int)x, (int)y, (int)z)).getPos().z,
               true,
               true
            );
         }

         if (world instanceof ServerLevel _world) {
            net.mcreator.crustychunks.utils.WariumChunkLoader.TICKET_CONTROLLER.forceChunk(_world,
               new BlockPos(
                  world.getChunk(new BlockPos((int)(x + 16.0), (int)y, (int)z)).getPos().x,
                  0,
                  world.getChunk(new BlockPos((int)(x + 16.0), (int)y, (int)z)).getPos().z
               ),
               world.getChunk(new BlockPos((int)(x + 16.0), (int)y, (int)z)).getPos().x,
               world.getChunk(new BlockPos((int)(x + 16.0), (int)y, (int)z)).getPos().z,
               true,
               true
            );
         }

         if (world instanceof ServerLevel _world) {
            net.mcreator.crustychunks.utils.WariumChunkLoader.TICKET_CONTROLLER.forceChunk(_world,
               new BlockPos(
                  world.getChunk(new BlockPos((int)x, (int)y, (int)(z + 16.0))).getPos().x,
                  0,
                  world.getChunk(new BlockPos((int)x, (int)y, (int)(z + 16.0))).getPos().z
               ),
               world.getChunk(new BlockPos((int)x, (int)y, (int)(z + 16.0))).getPos().x,
               world.getChunk(new BlockPos((int)x, (int)y, (int)(z + 16.0))).getPos().z,
               true,
               true
            );
         }

         if (world instanceof ServerLevel _world) {
            net.mcreator.crustychunks.utils.WariumChunkLoader.TICKET_CONTROLLER.forceChunk(_world,
               new BlockPos(
                  world.getChunk(new BlockPos((int)(x - 16.0), (int)y, (int)z)).getPos().x,
                  0,
                  world.getChunk(new BlockPos((int)(x - 16.0), (int)y, (int)z)).getPos().z
               ),
               world.getChunk(new BlockPos((int)(x - 16.0), (int)y, (int)z)).getPos().x,
               world.getChunk(new BlockPos((int)(x - 16.0), (int)y, (int)z)).getPos().z,
               true,
               true
            );
         }

         if (world instanceof ServerLevel _world) {
            net.mcreator.crustychunks.utils.WariumChunkLoader.TICKET_CONTROLLER.forceChunk(_world,
               new BlockPos(
                  world.getChunk(new BlockPos((int)x, (int)y, (int)(z - 16.0))).getPos().x,
                  0,
                  world.getChunk(new BlockPos((int)x, (int)y, (int)(z - 16.0))).getPos().z
               ),
               world.getChunk(new BlockPos((int)x, (int)y, (int)(z - 16.0))).getPos().x,
               world.getChunk(new BlockPos((int)x, (int)y, (int)(z - 16.0))).getPos().z,
               true,
               true
            );
         }

         CrustyChunksMod.queueServerWork(
            40,
            () -> {
               if (world instanceof ServerLevel _worldxxxxx) {
                  net.mcreator.crustychunks.utils.WariumChunkLoader.TICKET_CONTROLLER.forceChunk(_worldxxxxx,
                     new BlockPos(
                        world.getChunk(new BlockPos((int)x, (int)y, (int)z)).getPos().x,
                        0,
                        world.getChunk(new BlockPos((int)x, (int)y, (int)z)).getPos().z
                     ),
                     world.getChunk(new BlockPos((int)x, (int)y, (int)z)).getPos().x,
                     world.getChunk(new BlockPos((int)x, (int)y, (int)z)).getPos().z,
                     false,
                     true
                  );
               }

               if (world instanceof ServerLevel _worldxxxx) {
                  net.mcreator.crustychunks.utils.WariumChunkLoader.TICKET_CONTROLLER.forceChunk(_worldxxxx,
                     new BlockPos(
                        world.getChunk(new BlockPos((int)(x + 16.0), (int)y, (int)z)).getPos().x,
                        0,
                        world.getChunk(new BlockPos((int)(x + 16.0), (int)y, (int)z)).getPos().z
                     ),
                     world.getChunk(new BlockPos((int)(x + 16.0), (int)y, (int)z)).getPos().x,
                     world.getChunk(new BlockPos((int)(x + 16.0), (int)y, (int)z)).getPos().z,
                     false,
                     true
                  );
               }

               if (world instanceof ServerLevel _worldxxx) {
                  net.mcreator.crustychunks.utils.WariumChunkLoader.TICKET_CONTROLLER.forceChunk(_worldxxx,
                     new BlockPos(
                        world.getChunk(new BlockPos((int)x, (int)y, (int)(z + 16.0))).getPos().x,
                        0,
                        world.getChunk(new BlockPos((int)x, (int)y, (int)(z + 16.0))).getPos().z
                     ),
                     world.getChunk(new BlockPos((int)x, (int)y, (int)(z + 16.0))).getPos().x,
                     world.getChunk(new BlockPos((int)x, (int)y, (int)(z + 16.0))).getPos().z,
                     false,
                     true
                  );
               }

               if (world instanceof ServerLevel _worldxx) {
                  net.mcreator.crustychunks.utils.WariumChunkLoader.TICKET_CONTROLLER.forceChunk(_worldxx,
                     new BlockPos(
                        world.getChunk(new BlockPos((int)(x - 16.0), (int)y, (int)z)).getPos().x,
                        0,
                        world.getChunk(new BlockPos((int)(x - 16.0), (int)y, (int)z)).getPos().z
                     ),
                     world.getChunk(new BlockPos((int)(x - 16.0), (int)y, (int)z)).getPos().x,
                     world.getChunk(new BlockPos((int)(x - 16.0), (int)y, (int)z)).getPos().z,
                     false,
                     true
                  );
               }

               if (world instanceof ServerLevel _worldx) {
                  net.mcreator.crustychunks.utils.WariumChunkLoader.TICKET_CONTROLLER.forceChunk(_worldx,
                     new BlockPos(
                        world.getChunk(new BlockPos((int)x, (int)y, (int)(z - 16.0))).getPos().x,
                        0,
                        world.getChunk(new BlockPos((int)x, (int)y, (int)(z - 16.0))).getPos().z
                     ),
                     world.getChunk(new BlockPos((int)x, (int)y, (int)(z - 16.0))).getPos().x,
                     world.getChunk(new BlockPos((int)x, (int)y, (int)(z - 16.0))).getPos().z,
                     false,
                     true
                  );
               }
            }
         );
      }
   }
}
