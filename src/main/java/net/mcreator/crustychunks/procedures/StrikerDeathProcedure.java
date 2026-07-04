package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.AssassinEntity;
import net.mcreator.crustychunks.entity.BreacherEntity;
import net.mcreator.crustychunks.entity.CommanderEntity;
import net.mcreator.crustychunks.entity.StrikerEntity;
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

public class StrikerDeathProcedure {
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

         world.levelEvent(2001, BlockPos.containing(x, y, z), Block.getId(((Block)CrustyChunksModBlocks.STEEL_BLOCK.get()).defaultBlockState()));
         world.levelEvent(2001, BlockPos.containing(x, y + 1.0, z), Block.getId(((Block)CrustyChunksModBlocks.GREEN_ARMOR.get()).defaultBlockState()));
         if (entity instanceof StrikerEntity) {
            for (int index0 = 0; index0 < Mth.nextInt(RandomSource.create(), 0, 12); index0++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.SMALLBULLET.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index1 = 0; index1 < Mth.nextInt(RandomSource.create(), 0, 1); index1++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_INGOT.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index2 = 0; index2 < Mth.nextInt(RandomSource.create(), 0, 2); index2++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_COMPONENT.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            if (1 == Mth.nextInt(RandomSource.create(), 1, 20) && world instanceof ServerLevel _levelx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_BORED_BARREL.get()));
               entityToSpawn.setPickUpDelay(10);
               _levelx.addFreshEntity(entityToSpawn);
            }
         } else if (entity instanceof AssassinEntity) {
            for (int index3 = 0; index3 < Mth.nextInt(RandomSource.create(), 0, 12); index3++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_STEALTH_BULLET.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index4 = 0; index4 < Mth.nextInt(RandomSource.create(), 0, 1); index4++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_INGOT.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index5 = 0; index5 < Mth.nextInt(RandomSource.create(), 0, 2); index5++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_COMPONENT.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            if (1 == Mth.nextInt(RandomSource.create(), 1, 20) && world instanceof ServerLevel _levelx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_BORED_BARREL.get()));
               entityToSpawn.setPickUpDelay(10);
               _levelx.addFreshEntity(entityToSpawn);
            }

            if (1 == Mth.nextInt(RandomSource.create(), 1, 20) && world instanceof ServerLevel _levelx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.WEAPON_SUPRESSOR.get()));
               entityToSpawn.setPickUpDelay(10);
               _levelx.addFreshEntity(entityToSpawn);
            }
         } else if (entity instanceof CommanderEntity) {
            for (int index6 = 0; index6 < Mth.nextInt(RandomSource.create(), 0, 12); index6++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.BULLET.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index7 = 0; index7 < Mth.nextInt(RandomSource.create(), 0, 1); index7++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_INGOT.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index8 = 0; index8 < Mth.nextInt(RandomSource.create(), 0, 1); index8++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.ADVANCED_COMPONENT.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index9 = 0; index9 < Mth.nextInt(RandomSource.create(), 1, 3); index9++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_COMPONENT.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            if (1 == Mth.nextInt(RandomSource.create(), 1, 4) && world instanceof ServerLevel _levelx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.MEDIUM_BORED_BARREL.get()));
               entityToSpawn.setPickUpDelay(10);
               _levelx.addFreshEntity(entityToSpawn);
            }
         } else if (entity instanceof BreacherEntity) {
            for (int index10 = 0; index10 < Mth.nextInt(RandomSource.create(), 0, 1); index10++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.BRASS_FITTING.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index11 = 0; index11 < Mth.nextInt(RandomSource.create(), 2, 3); index11++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.VOLATILE_DUST.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index12 = 0; index12 < Mth.nextInt(RandomSource.create(), 1, 1); index12++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.ADVANCED_COMPONENT.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index13 = 0; index13 < Mth.nextInt(RandomSource.create(), 2, 3); index13++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_INGOT.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }

            for (int index14 = 0; index14 < Mth.nextInt(RandomSource.create(), 3, 4); index14++) {
               if (world instanceof ServerLevel _levelx) {
                  ItemEntity entityToSpawn = new ItemEntity(_levelx, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_COMPONENT.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _levelx.addFreshEntity(entityToSpawn);
               }
            }
         }

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x, y + 1.0, z, 5, 0.0, 0.0, 0.0, 0.5);
         }

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SPARKS.get(), x, y + 1.0, z, 5, 0.0, 0.0, 0.0, 0.5);
         }

         if (!entity.level().isClientSide()) {
            entity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("StrikerDeathProcedure.execute", _wtSafe);
      }
   }
}
