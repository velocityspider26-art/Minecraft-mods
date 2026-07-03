package net.mcreator.crustychunks.item;

import java.util.List;
import net.mcreator.crustychunks.procedures.AmmoCountPaintProcedure;
import net.mcreator.crustychunks.procedures.PaintToolFireProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class PaintToolItem extends Item {
   public PaintToolItem() {
      super(new Properties().stacksTo(1).rarity(Rarity.COMMON));
   }

   public void appendHoverText(ItemStack itemstack, net.minecraft.world.item.Item.TooltipContext level, List<Component> list, TooltipFlag flag) {
      super.appendHoverText(itemstack, level, list, flag);
      Entity entity = itemstack.getEntityRepresentation();
      String hoverText = AmmoCountPaintProcedure.execute(itemstack);
      if (hoverText != null) {
         for (String line : hoverText.split("\n")) {
            list.add(Component.literal(line));
         }
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      super.useOn(context);
      PaintToolFireProcedure.execute(
         context.getLevel(),
         (double)context.getClickedPos().getX(),
         (double)context.getClickedPos().getY(),
         (double)context.getClickedPos().getZ(),
         context.getItemInHand()
      );
      return InteractionResult.SUCCESS;
   }
}
