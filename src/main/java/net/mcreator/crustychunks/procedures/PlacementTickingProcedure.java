package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.CIWSEntity;
import net.mcreator.crustychunks.entity.MortarerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class PlacementTickingProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (entity instanceof CIWSEntity) {
            CrustyChunksMod.queueServerWork(1, () -> CIWSAIProcedure.execute(world, x, y, z, entity));
         } else if (entity instanceof MortarerEntity) {
            CrustyChunksMod.queueServerWork(1, () -> MortarerAIProcedure.execute(world, x, y, z, entity));
         }
      }
   }
}
