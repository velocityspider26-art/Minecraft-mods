package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;

public class DroneLaunchProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if (entity.getPersistentData().getDouble("I") <= 4.0) {
            if (world instanceof ServerLevel _level) {
               Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.SCOUT.get())
                  .spawn(_level, BlockPos.containing(x, y + 3.0, z), MobSpawnType.MOB_SUMMONED);
               if (entityToSpawn != null) {
                  entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
               }
            }

            entity.getPersistentData().putDouble("I", entity.getPersistentData().getDouble("I") + 1.0);
            entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") + 80.0);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("DroneLaunchProcedure.execute", _wtSafe);
      }
   }
}
