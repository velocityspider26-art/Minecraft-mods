package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

public class LMGHandTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      try {
      if (entity != null) {
         RiflehandtickProcedure.execute(entity);
         if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ShotQue") > 0.0) {
            if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ShotQue") == 1.0 || itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ShotQue") == 3.0) {
               LMGFireScriptProcedure.execute(world, x, y, z, entity, itemstack);
            }

            { final var _fvcc1 = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ShotQue") - 1.0; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("ShotQue", _fvcc1)); }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("LMGHandTickProcedure.execute", _wtSafe);
      }
   }
}
