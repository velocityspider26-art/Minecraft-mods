package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.DecimatorEntity;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

public class DecimatorMeleeProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         int horizontalRadiusSquare = 3;
         int verticalRadiusSquare = 2;
         int yIterationsSquare = verticalRadiusSquare;

         for (int i = -verticalRadiusSquare; i <= yIterationsSquare; i++) {
            for (int xi = -horizontalRadiusSquare; xi <= horizontalRadiusSquare; xi++) {
               for (int zi = -horizontalRadiusSquare; zi <= horizontalRadiusSquare; zi++) {
                  world.levelEvent(
                     2001,
                     BlockPos.containing(x + (double)xi, y + (double)i + 3.0, z + (double)zi),
                     Block.getId(world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 3.0, z + (double)zi)))
                  );
                  if ((
                        !(
                              world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 3.0, z + (double)zi))
                                    .getDestroySpeed(world, BlockPos.containing(x + (double)xi, y + (double)i + 3.0, z + (double)zi))
                                 < 5.0F
                           )
                           || !(
                              world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 3.0, z + (double)zi))
                                    .getDestroySpeed(world, BlockPos.containing(x + (double)xi, y + (double)i + 3.0, z + (double)zi))
                                 >= 0.0F
                           )
                     )
                     && !entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:chippable")))) {
                     HeavyCrackProcedureProcedure.execute(world, x + (double)xi, y + (double)i + 3.0, z + (double)zi);
                  } else {
                     world.destroyBlock(BlockPos.containing(x + (double)xi, y + (double)i + 3.0, z + (double)zi), false);
                  }
               }
            }
         }

         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
                  SoundSource.NEUTRAL,
                  5.0F,
                  0.4F
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
                  SoundSource.NEUTRAL,
                  5.0F,
                  0.4F,
                  false
               );
            }
         }

         entity.getPersistentData().putDouble("T", 60.0);
         if (entity instanceof DecimatorEntity) {
            ((DecimatorEntity)entity).setAnimation("Anger");
         }

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.DUST.get(), x, y + 1.5, z, 15, 2.0, 0.0, 2.0, 1.0);
         }

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.WHITE_DUST.get(), x, y + 1.5, z, 15, 2.0, 0.0, 2.0, 1.0);
         }
      }
   }
}
