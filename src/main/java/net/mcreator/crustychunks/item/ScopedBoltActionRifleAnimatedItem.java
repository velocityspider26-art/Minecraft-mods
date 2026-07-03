package net.mcreator.crustychunks.item;

import net.minecraft.client.model.HumanoidModel;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.Consumer;
import net.mcreator.crustychunks.item.renderer.ScopedBoltActionRifleAnimatedItemRenderer;
import net.mcreator.crustychunks.procedures.AmmoCount8Procedure;
import net.mcreator.crustychunks.procedures.BoltActionRifleEntitySwingsItemProcedure;
import net.mcreator.crustychunks.procedures.BoltFireScriptProcedure;
import net.mcreator.crustychunks.procedures.GunCooldownOverrideProcedure;
import net.mcreator.crustychunks.procedures.RiflehandtickProcedure;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController.State;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ScopedBoltActionRifleAnimatedItem extends Item implements GeoItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   public String animationprocedure = "empty";
   String prevAnim = "empty";

   public ScopedBoltActionRifleAnimatedItem() {
      super(new Properties().stacksTo(1).rarity(Rarity.COMMON));
   }

   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return false;
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private ScopedBoltActionRifleAnimatedItemRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null)
               this.renderer = new ScopedBoltActionRifleAnimatedItemRenderer();
            return this.renderer;
         }
      });
   }

   public static final EnumProxy<HumanoidModel.ArmPose> ARM_POSE = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) (model, entity, arm) -> {
               if (arm != HumanoidArm.LEFT) {
                  model.rightArm.xRot = -1.5F + model.head.xRot;
                  model.rightArm.yRot = 0.0F + model.head.yRot;
                  model.leftArm.xRot = -1.5F + model.head.xRot;
                  model.leftArm.yRot = 0.75F + model.head.yRot;
               }
   });

   @Override
   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      super.initializeClient(consumer);
      consumer.accept(new IClientItemExtensions() {
         public ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
               return !itemStack.isEmpty() && entityLiving.getUsedItemHand() == hand ? (ArmPose) ARM_POSE.getValue() : ArmPose.EMPTY;
            }

         public boolean applyForgeHandTransform(
               PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess
            ) {
               int i = arm == HumanoidArm.RIGHT ? 1 : -1;
               poseStack.translate((float)i * 0.56F, -0.52F, -0.72F);
               if (player.getUseItem() == itemInHand) {
                  poseStack.translate(0.05, 0.05, 0.05);
               }

               return true;
            }
      });
   }

   private PlayState idlePredicate(AnimationState event) {
      if (this.animationprocedure.equals("empty")) {
         event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
         return PlayState.CONTINUE;
      } else {
         return PlayState.STOP;
      }
   }

   private PlayState procedurePredicate(AnimationState event) {
      if (!this.animationprocedure.equals("empty") && event.getController().getAnimationState() == State.STOPPED
         || !this.animationprocedure.equals(this.prevAnim) && !this.animationprocedure.equals("empty")) {
         if (!this.animationprocedure.equals(this.prevAnim)) {
            event.getController().forceAnimationReset();
         }

         event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));
         if (event.getController().getAnimationState() == State.STOPPED) {
            this.animationprocedure = "empty";
            event.getController().forceAnimationReset();
         }
      } else if (this.animationprocedure.equals("empty")) {
         this.prevAnim = "empty";
         return PlayState.STOP;
      }

      this.prevAnim = this.animationprocedure;
      return PlayState.CONTINUE;
   }

   public void registerControllers(ControllerRegistrar data) {
      AnimationController procedureController = new AnimationController(this, "procedureController", 0, this::procedurePredicate);
      data.add(new AnimationController[]{procedureController});
      AnimationController idleController = new AnimationController(this, "idleController", 0, this::idlePredicate);
      data.add(new AnimationController[]{idleController});
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public void appendHoverText(ItemStack itemstack, net.minecraft.world.item.Item.TooltipContext level, List<Component> list, TooltipFlag flag) {
      super.appendHoverText(itemstack, level, list, flag);
      Entity entity = itemstack.getEntityRepresentation();
      String hoverText = AmmoCount8Procedure.execute(itemstack);
      if (hoverText != null) {
         for (String line : hoverText.split("\n")) {
            list.add(Component.literal(line));
         }
      }
   }

   public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
      InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
      ItemStack itemstack = (ItemStack)ar.getObject();
      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();
      BoltFireScriptProcedure.execute(world, x, y, z, entity, itemstack);
      return ar;
   }

   public boolean onEntitySwing(ItemStack itemstack, LivingEntity entity) {
      boolean retval = super.onEntitySwing(itemstack, entity);
      BoltActionRifleEntitySwingsItemProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity, itemstack);
      return retval;
   }

   public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
      super.inventoryTick(itemstack, world, entity, slot, selected);
      if (selected) {
         RiflehandtickProcedure.execute(entity);
      }

      GunCooldownOverrideProcedure.execute(itemstack);
   }
}
