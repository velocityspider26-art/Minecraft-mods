package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.text.DecimalFormat;
import net.minecraft.world.item.ItemStack;

public class AmmoCount10Procedure {
   public static String execute(ItemStack itemstack) {
      return "§8Ammo: §6" + new DecimalFormat("####").format(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) + "§8/§610";
   }
}
