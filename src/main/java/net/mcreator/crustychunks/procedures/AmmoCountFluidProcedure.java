package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.text.DecimalFormat;
import net.minecraft.world.item.ItemStack;

public class AmmoCountFluidProcedure {
   public static String execute(ItemStack itemstack) {
      return "§8Fluid §6" + new DecimalFormat("####").format(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Fluid")) + "§8/§61000";
   }
}
