package net.mcreator.crustychunks.item;

import java.util.List;
import net.mcreator.crustychunks.procedures.MortarOnBlockRightClickedProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class GrenadeShellItem extends Item {
   public GrenadeShellItem() {
      super(new Properties().stacksTo(4).rarity(Rarity.COMMON));
   }

   public void appendHoverText(ItemStack itemstack, net.minecraft.world.item.Item.TooltipContext level, List<Component> list, TooltipFlag flag) {
      super.appendHoverText(itemstack, level, list, flag);
      list.add(Component.translatable("item.crusty_chunks.grenade_shell.description_0"));
   }

   public InteractionResult useOn(UseOnContext context) {
      super.useOn(context);
      MortarOnBlockRightClickedProcedure.execute(
         context.getLevel(),
         (double)context.getClickedPos().getX(),
         (double)context.getClickedPos().getY(),
         (double)context.getClickedPos().getZ(),
         context.getLevel().getBlockState(context.getClickedPos()),
         context.getPlayer()
      );
      return InteractionResult.SUCCESS;
   }
}
