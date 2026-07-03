package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.text.DecimalFormat;
import net.minecraft.world.item.ItemStack;

public class AmmoCountPaintProcedure {
   public static String execute(ItemStack itemstack) {
      String Type = "";
      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("")) {
         Type = "§8NONE";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("none")) {
         Type = "§8NONE";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("remove")) {
         Type = "§8Remover";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("black")) {
         Type = "§0Black";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("white")) {
         Type = "§fWhite";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("light_gray")) {
         Type = "§7Light Gray";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("gray")) {
         Type = "§8Gray";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("red")) {
         Type = "§4Red";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("lime")) {
         Type = "§aLime";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("brown")) {
         Type = "§6Brown";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("orange")) {
         Type = "§6Orange";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("yellow")) {
         Type = "§eYellow";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("blue")) {
         Type = "§9Blue";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("green")) {
         Type = "§2Green";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("cyan")) {
         Type = "§3Cyan";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("light_blue")) {
         Type = "§bLight Blue";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("pink")) {
         Type = "§cPink";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("magenta")) {
         Type = "§dMagenta";
      }

      if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Color").equals("Purple")) {
         Type = "§5Purple";
      }

      return "§8Fluid §6" + new DecimalFormat("####").format(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Fluid")) + "§8/§6100 Type: " + Type;
   }
}
