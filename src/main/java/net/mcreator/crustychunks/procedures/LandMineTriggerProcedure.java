package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class LandMineTriggerProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if (entity instanceof LivingEntity) {
            CrustyChunksMod.queueServerWork(7, () -> {
               if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.LAND_MINE.get()) {
                  world.destroyBlock(BlockPos.containing(x, y, z), false);
                  ExplosionExampleProcedure.execute(world, x + 0.5, y + 0.25, z + 0.5, 4.0);
               }
            });
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("LandMineTriggerProcedure.execute", _wtSafe);
      }
   }
}
