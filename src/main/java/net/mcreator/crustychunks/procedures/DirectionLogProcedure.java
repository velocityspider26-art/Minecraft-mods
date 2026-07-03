package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.text.DecimalFormat;
import net.minecraft.world.item.ItemStack;

public class DirectionLogProcedure {
   public static String execute(ItemStack itemstack) {
      String WeaponAttatched = "";
      String ATGMMode = "";
      String Mode = "";
      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("POSX") == 0.0 && itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("POSY") == 0.0 && itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("POSZ") == 0.0) {
         WeaponAttatched = " ";
      } else {
         WeaponAttatched = "§3Weapon Attached";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Mode")) {
         Mode = "§9Mouse-Aim Mode";
      } else {
         Mode = " ";
      }

      return "§8Pitch: §6"
         + new DecimalFormat("####").format(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Pitch"))
         + "§8Yaw:§6"
         + new DecimalFormat("####").format(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Yaw"))
         + WeaponAttatched
         + Mode
         + "§7 Shift-Click on an Aimer Node while the item is in your off hand for linking.";
   }
}
