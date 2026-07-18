package net.mcreator.crustychunks.init;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.item.ArmorPeelerAnimatedItem;
import net.mcreator.crustychunks.item.AutoPistolItem;
import net.mcreator.crustychunks.item.AutomaticRifleItem;
import net.mcreator.crustychunks.item.BattleRifleItem;
import net.mcreator.crustychunks.item.BoltActionRifleAnimatedItem;
import net.mcreator.crustychunks.item.BreakActionShotgunAnimatedItem;
import net.mcreator.crustychunks.item.BreechRifleItem;
import net.mcreator.crustychunks.item.BurstRifleItem;
import net.mcreator.crustychunks.item.EradicationItem;
import net.mcreator.crustychunks.item.FlameThrowerAnimatedItem;
import net.mcreator.crustychunks.item.FlarePistolItem;
import net.mcreator.crustychunks.item.GrenadeLauncherItem;
import net.mcreator.crustychunks.item.HandDrillItem;
import net.mcreator.crustychunks.item.LMGAnimatedItem;
import net.mcreator.crustychunks.item.LeverRifleItem;
import net.mcreator.crustychunks.item.MachineCarbineItem;
import net.mcreator.crustychunks.item.MusketItem;
import net.mcreator.crustychunks.item.PumpActionShotgunAnimatedItem;
import net.mcreator.crustychunks.item.RevolverAnimatedItem;
import net.mcreator.crustychunks.item.SMGAnimatedItem;
import net.mcreator.crustychunks.item.ScopedBoltActionRifleAnimatedItem;
import net.mcreator.crustychunks.item.ScopedBreechRifleItem;
import net.mcreator.crustychunks.item.SemiAutomaticPistolAnimatedItem;
import net.mcreator.crustychunks.item.SemiAutomaticRifleAnimatedItem;
import net.mcreator.crustychunks.item.SingleShotRifleItem;
import net.mcreator.crustychunks.item.StealthPistolItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import software.bernie.geckolib.animatable.GeoItem;

@EventBusSubscriber
public class ItemAnimationFactory {
   @SubscribeEvent
   public static void animatedItems(PlayerTickEvent.Post event) {
      String animation = "";
      ItemStack mainhandItem = event.getEntity().getMainHandItem();
      ItemStack offhandItem = event.getEntity().getOffhandItem();
      if (false && (mainhandItem.getItem() instanceof GeoItem || offhandItem.getItem() instanceof GeoItem)) {
         if (mainhandItem.getItem() instanceof SemiAutomaticRifleAnimatedItem animatable) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((SemiAutomaticRifleAnimatedItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof SemiAutomaticRifleAnimatedItem animatablex) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((SemiAutomaticRifleAnimatedItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof SemiAutomaticPistolAnimatedItem animatablexx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((SemiAutomaticPistolAnimatedItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof SemiAutomaticPistolAnimatedItem animatablexxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((SemiAutomaticPistolAnimatedItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof BoltActionRifleAnimatedItem animatablexxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((BoltActionRifleAnimatedItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof BoltActionRifleAnimatedItem animatablexxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((BoltActionRifleAnimatedItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof ScopedBoltActionRifleAnimatedItem animatablexxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((ScopedBoltActionRifleAnimatedItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof ScopedBoltActionRifleAnimatedItem animatablexxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((ScopedBoltActionRifleAnimatedItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof PumpActionShotgunAnimatedItem animatablexxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((PumpActionShotgunAnimatedItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof PumpActionShotgunAnimatedItem animatablexxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((PumpActionShotgunAnimatedItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof ArmorPeelerAnimatedItem animatablexxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((ArmorPeelerAnimatedItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof ArmorPeelerAnimatedItem animatablexxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((ArmorPeelerAnimatedItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof SMGAnimatedItem animatablexxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((SMGAnimatedItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof SMGAnimatedItem animatablexxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((SMGAnimatedItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof FlameThrowerAnimatedItem animatablexxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((FlameThrowerAnimatedItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof FlameThrowerAnimatedItem animatablexxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((FlameThrowerAnimatedItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof RevolverAnimatedItem animatablexxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((RevolverAnimatedItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof RevolverAnimatedItem animatablexxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((RevolverAnimatedItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof SingleShotRifleItem animatablexxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((SingleShotRifleItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof SingleShotRifleItem animatablexxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((SingleShotRifleItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof LMGAnimatedItem animatablexxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((LMGAnimatedItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof LMGAnimatedItem animatablexxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((LMGAnimatedItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof BurstRifleItem animatablexxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((BurstRifleItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof BurstRifleItem animatablexxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((BurstRifleItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof AutoPistolItem animatablexxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((AutoPistolItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof AutoPistolItem animatablexxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((AutoPistolItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof BreakActionShotgunAnimatedItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((BreakActionShotgunAnimatedItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof BreakActionShotgunAnimatedItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((BreakActionShotgunAnimatedItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof BattleRifleItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((BattleRifleItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof BattleRifleItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((BattleRifleItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof MachineCarbineItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((MachineCarbineItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof MachineCarbineItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((MachineCarbineItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof HandDrillItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((HandDrillItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof HandDrillItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((HandDrillItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof FlarePistolItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((FlarePistolItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof FlarePistolItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((FlarePistolItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof StealthPistolItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((StealthPistolItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof StealthPistolItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((StealthPistolItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof BreechRifleItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((BreechRifleItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof BreechRifleItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((BreechRifleItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof ScopedBreechRifleItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((ScopedBreechRifleItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof ScopedBreechRifleItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((ScopedBreechRifleItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof EradicationItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((EradicationItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof EradicationItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((EradicationItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof LeverRifleItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((LeverRifleItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof LeverRifleItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((LeverRifleItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof AutomaticRifleItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((AutomaticRifleItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof AutomaticRifleItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((AutomaticRifleItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof GrenadeLauncherItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((GrenadeLauncherItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof GrenadeLauncherItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((GrenadeLauncherItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (mainhandItem.getItem() instanceof MusketItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = mainhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getMainHandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((MusketItem)event.getEntity().getMainHandItem().getItem()).animationprocedure = animation;
               }
            }
         }

         if (offhandItem.getItem() instanceof MusketItem animatablexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx) {
            animation = offhandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("geckoAnim");
            if (!animation.isEmpty()) {
               CustomData.update(DataComponents.CUSTOM_DATA, event.getEntity().getOffhandItem(), _tagupd -> _tagupd.putString("geckoAnim", ""));
               if (event.getEntity().level().isClientSide()) {
                  ((MusketItem)event.getEntity().getOffhandItem().getItem()).animationprocedure = animation;
               }
            }
         }
      }
   }
}
