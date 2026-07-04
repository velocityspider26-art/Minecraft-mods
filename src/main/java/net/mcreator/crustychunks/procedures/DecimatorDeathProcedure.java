package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;

public class DecimatorDeathProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         MediumExplosionProcedure.execute(world, x, y, z);
         if (!entity.level().isClientSide()) {
            entity.discard();
         }

         CrustyChunksMod.queueServerWork(
            60,
            () -> {
               for (int index0 = 0; index0 < 3; index0++) {
                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModBlocks.STEEL_BLOCK.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_SHELL.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_SHELL.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModBlocks.AUTOCANNON_BARREL.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.ADVANCED_COMPONENT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.UNFABRICATED_TECH_COMPONENT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_GEAR.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_COMPONENT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.CAST_COMPONENT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.ELECTRIC_MOTOR.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }
               }

               if (world instanceof ServerLevel _level) {
                  ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.MG_RECEIVER.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _level.addFreshEntity(entityToSpawn);
               }

               if (world instanceof ServerLevel _level) {
                  ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModBlocks.STEEL_BLOCK.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _level.addFreshEntity(entityToSpawn);
               }
            }
         );
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("DecimatorDeathProcedure.execute", _wtSafe);
      }
   }
}
