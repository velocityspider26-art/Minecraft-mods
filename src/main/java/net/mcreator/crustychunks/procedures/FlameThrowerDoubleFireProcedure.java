package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;

public class FlameThrowerDoubleFireProcedure {
   public static void execute(ItemStack itemstack) {
      try {
      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ShotQue") == 0.0) {
         CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("ShotQue", 4.0));
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FlameThrowerDoubleFireProcedure.execute", _wtSafe);
      }
   }
}
