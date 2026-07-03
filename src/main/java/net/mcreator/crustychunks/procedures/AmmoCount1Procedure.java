package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.text.DecimalFormat;
import net.minecraft.world.item.ItemStack;

public class AmmoCount1Procedure {
   public static String execute(ItemStack itemstack) {
      double Loaded = 0.0;
      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Loaded")) {
         Loaded = 1.0;
      } else {
         Loaded = 0.0;
      }

      return "§8Ammo: §6" + new DecimalFormat("####").format(Loaded) + "§8/§61";
   }
}
