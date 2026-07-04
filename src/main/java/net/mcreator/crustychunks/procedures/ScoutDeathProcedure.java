package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

public class ScoutDeathProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                  SoundSource.NEUTRAL,
                  3.0F,
                  1.0F
               );
            } else {
               _level.playLocalSound(
                  x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")), SoundSource.NEUTRAL, 3.0F, 1.0F, false
               );
            }
         }

         world.levelEvent(2001, BlockPos.containing(x, y + 1.0, z), Block.getId(((Block)CrustyChunksModBlocks.GREEN_ARMOR.get()).defaultBlockState()));

         for (int index0 = 0; index0 < Mth.nextInt(RandomSource.create(), 0, 3); index0++) {
            if (world instanceof ServerLevel _levelx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.ELECTRIC_MOTOR.get()));
               entityToSpawn.setPickUpDelay(10);
               _levelx.addFreshEntity(entityToSpawn);
            }

            if (world instanceof ServerLevel _levelx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.ALUMINUM_PLATE.get()));
               entityToSpawn.setPickUpDelay(10);
               _levelx.addFreshEntity(entityToSpawn);
            }
         }

         for (int index1 = 0; index1 < Mth.nextInt(RandomSource.create(), 0, 1); index1++) {
            if (world instanceof ServerLevel _levelx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_COMPONENT.get()));
               entityToSpawn.setPickUpDelay(10);
               _levelx.addFreshEntity(entityToSpawn);
            }
         }

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x, y + 1.0, z, 5, 0.0, 0.0, 0.0, 0.5);
         }

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SPARKS.get(), x, y + 1.0, z, 25, 0.0, 0.0, 0.0, 0.5);
         }

         if (!entity.level().isClientSide()) {
            entity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ScoutDeathProcedure.execute", _wtSafe);
      }
   }
}
