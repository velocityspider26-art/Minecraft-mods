package net.mcreator.crustychunks.item;

import java.util.List;
import net.mcreator.crustychunks.procedures.AimerConstantUpdatingProcedure;
import net.mcreator.crustychunks.procedures.AimerEntitySwingsItemProcedure;
import net.mcreator.crustychunks.procedures.AimerUpdateProcedure;
import net.mcreator.crustychunks.procedures.DirectionLogProcedure;
import net.mcreator.crustychunks.procedures.DirectionUpdateProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class AimerItem extends Item {
   public AimerItem() {
      super(new Properties().stacksTo(1).rarity(Rarity.COMMON));
   }

   public void appendHoverText(ItemStack itemstack, net.minecraft.world.item.Item.TooltipContext level, List<Component> list, TooltipFlag flag) {
      super.appendHoverText(itemstack, level, list, flag);
      Entity entity = itemstack.getEntityRepresentation();
      String hoverText = DirectionLogProcedure.execute(itemstack);
      if (hoverText != null) {
         for (String line : hoverText.split("\n")) {
            list.add(Component.literal(line));
         }
      }
   }

   public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
      InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
      DirectionUpdateProcedure.execute(world, entity, (ItemStack)ar.getObject());
      // Server-authoritative laser designation: mark a target point/entity for laser-guided munitions.
      if (!world.isClientSide() && entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
         try {
            net.mcreator.crustychunks.compat.AeronauticsLaserDesignationHandler.designate(serverPlayer);
         } catch (Throwable t) {
            net.mcreator.crustychunks.compat.WariumSafety.report("AimerItem.designate", t);
         }
      }
      return ar;
   }

   public InteractionResult useOn(UseOnContext context) {
      super.useOn(context);
      AimerUpdateProcedure.execute(
         context.getLevel(),
         (double)context.getClickedPos().getX(),
         (double)context.getClickedPos().getY(),
         (double)context.getClickedPos().getZ(),
         context.getPlayer(),
         context.getItemInHand()
      );
      return InteractionResult.SUCCESS;
   }

   public boolean onEntitySwing(ItemStack itemstack, LivingEntity entity) {
      boolean retval = super.onEntitySwing(itemstack, entity);
      AimerEntitySwingsItemProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity, itemstack);
      return retval;
   }

   public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
      super.inventoryTick(itemstack, world, entity, slot, selected);
      if (selected) {
         AimerConstantUpdatingProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity, itemstack);
      }
   }
}
