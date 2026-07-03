package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.text.DecimalFormat;
import net.minecraft.world.item.ItemStack;

public class AmmoCountMGProcedure {
   public static String execute(ItemStack itemstack) {
      String Type = "";
      double capacity = 0.0;
      capacity = 200.0;
      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize") == 0.0) {
         Type = "Large ";
      } else if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize") == 1.0) {
         Type = "XL ";
      } else if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize") == -1.0) {
         Type = "Medium";
         capacity = 400.0;
      }

      return "§8Ammo: §4"
         + Type
         + " §6"
         + new DecimalFormat("####").format(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo"))
         + "§8/§6"
         + new DecimalFormat("####").format(capacity);
   }
}
