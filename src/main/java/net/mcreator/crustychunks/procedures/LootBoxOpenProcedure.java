package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

public class LootBoxOpenProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double random = 0.0;
      random = (double)Mth.nextInt(RandomSource.create(), 1, 17);
      if (1.0 == random) {
         for (int index0 = 0; index0 < Mth.nextInt(RandomSource.create(), 6, 9); index0++) {
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.SMALLBULLET.get()));
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }
         }

         for (int index1 = 0; index1 < Mth.nextInt(RandomSource.create(), 5, 6); index1++) {
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.BULLET.get()));
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }
         }

         for (int index2 = 0; index2 < Mth.nextInt(RandomSource.create(), 2, 3); index2++) {
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.LARGE_BULLET.get()));
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }
         }
      } else if (2.0 == random) {
         for (int index3 = 0; index3 < Mth.nextInt(RandomSource.create(), 1, 2); index3++) {
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.GRENADE.get()));
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }
         }
      } else if (3.0 == random) {
         if (world instanceof ServerLevel _level) {
            ItemEntity entityToSpawn = new ItemEntity(
               _level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.ARMOR_PEELER_ROCKET.get())
            );
            entityToSpawn.setPickUpDelay(10);
            _level.addFreshEntity(entityToSpawn);
         }
      } else if (4.0 == random) {
         for (int index4 = 0; index4 < Mth.nextInt(RandomSource.create(), 1, 5); index4++) {
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.ALUMINUM_PLATE.get()));
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }
         }

         for (int index5 = 0; index5 < Mth.nextInt(RandomSource.create(), 1, 5); index5++) {
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_INGOT.get()));
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }
         }

         for (int index6 = 0; index6 < Mth.nextInt(RandomSource.create(), 1, 5); index6++) {
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_COMPONENT.get()));
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }
         }
      } else if (5.0 == random) {
         for (int index7 = 0; index7 < Mth.nextInt(RandomSource.create(), 1, 3); index7++) {
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(
                  _level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.ADVANCED_COMPONENT.get())
               );
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }

            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.ELECTRIC_MOTOR.get()));
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }
         }
      } else if (6.0 == random) {
         if (world instanceof ServerLevel _level) {
            ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.SMG_RECEIVER.get()));
            entityToSpawn.setPickUpDelay(10);
            _level.addFreshEntity(entityToSpawn);
         }

         if (world instanceof ServerLevel _level) {
            ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_BORED_BARREL.get()));
            entityToSpawn.setPickUpDelay(10);
            _level.addFreshEntity(entityToSpawn);
         }

         if (world instanceof ServerLevel _level) {
            ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_COMPONENT.get()));
            entityToSpawn.setPickUpDelay(10);
            _level.addFreshEntity(entityToSpawn);
         }
      } else if (7.0 == random) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.OIL.get()).defaultBlockState(), 3);
      } else if (8.0 == random) {
         for (int index8 = 0; index8 < Mth.nextInt(RandomSource.create(), 3, 5); index8++) {
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.HUGE_BULLET.get()));
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }
         }
      } else if (9.0 == random) {
         for (int index9 = 0; index9 < Mth.nextInt(RandomSource.create(), 6, 9); index9++) {
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.BRASS_PLATE.get()));
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }
         }

         for (int index10 = 0; index10 < Mth.nextInt(RandomSource.create(), 6, 9); index10++) {
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 0.5, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.COPPER_PLATE.get()));
               entityToSpawn.setPickUpDelay(10);
               _level.addFreshEntity(entityToSpawn);
            }
         }
      } else if (10.0 <= random && 15.0 >= random) {
         if (world instanceof ServerLevel _level) {
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.STRIKER.get())
               .spawn(_level, BlockPos.containing(x + 0.5, y + 0.5, z + 0.5), MobSpawnType.MOB_SUMMONED);
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      } else if (16.0 == random) {
         if (world instanceof ServerLevel _levelx) {
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.BREACHER.get())
               .spawn(_levelx, BlockPos.containing(x + 0.5, y + 0.5, z + 0.5), MobSpawnType.MOB_SUMMONED);
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      } else if (17.0 == random) {
         for (int index11 = 0; index11 < Mth.nextInt(RandomSource.create(), 6, 9); index11++) {
            if (world instanceof ServerLevel _levelxx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelxx, x + 0.5, y + 0.5, z + 0.5, new ItemStack(Items.IRON_INGOT));
               entityToSpawn.setPickUpDelay(10);
               _levelxx.addFreshEntity(entityToSpawn);
            }
         }

         for (int index12 = 0; index12 < Mth.nextInt(RandomSource.create(), 2, 3); index12++) {
            if (world instanceof ServerLevel _levelxx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelxx, x + 0.5, y + 0.5, z + 0.5, new ItemStack(Items.DIAMOND));
               entityToSpawn.setPickUpDelay(10);
               _levelxx.addFreshEntity(entityToSpawn);
            }
         }

         for (int index13 = 0; index13 < Mth.nextInt(RandomSource.create(), 2, 3); index13++) {
            if (world instanceof ServerLevel _levelxx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelxx, x + 0.5, y + 0.5, z + 0.5, new ItemStack(Items.GOLD_INGOT));
               entityToSpawn.setPickUpDelay(10);
               _levelxx.addFreshEntity(entityToSpawn);
            }
         }

         for (int index14 = 0; index14 < Mth.nextInt(RandomSource.create(), 2, 3); index14++) {
            if (world instanceof ServerLevel _levelxx) {
               ItemEntity entityToSpawn = new ItemEntity(_levelxx, x + 0.5, y + 0.5, z + 0.5, new ItemStack(Items.EMERALD));
               entityToSpawn.setPickUpDelay(10);
               _levelxx.addFreshEntity(entityToSpawn);
            }
         }
      }

      if (world instanceof ServerLevel _levelxx) {
         _levelxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x + 0.5, y + 0.5, z + 0.5, 5, 1.0, 1.0, 1.0, 0.1);
      }

      if (world instanceof Level _levelxx) {
         if (!_levelxx.isClientSide()) {
            _levelxx.playSound(
               null,
               BlockPos.containing(x + 0.5, y + 0.5, z + 0.5),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
               SoundSource.NEUTRAL,
               2.0F,
               1.0F
            );
         } else {
            _levelxx.playLocalSound(
               x + 0.5,
               y + 0.5,
               z + 0.5,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
               SoundSource.NEUTRAL,
               2.0F,
               1.0F,
               false
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("LootBoxOpenProcedure.execute", _wtSafe);
      }
   }
}
