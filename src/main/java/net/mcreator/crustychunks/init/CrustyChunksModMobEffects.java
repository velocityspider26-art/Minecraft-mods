package net.mcreator.crustychunks.init;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.potion.ContaminatedMobEffect;
import net.mcreator.crustychunks.potion.FlammableMobEffect;
import net.mcreator.crustychunks.potion.ImpendingDoomMobEffect;
import net.mcreator.crustychunks.potion.RadiationMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;

@net.neoforged.fml.common.EventBusSubscriber
public class CrustyChunksModMobEffects {
   public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, "crusty_chunks");
   public static final DeferredHolder<MobEffect, MobEffect> FLAMMABLE = REGISTRY.register("flammable", () -> new FlammableMobEffect());
   public static final DeferredHolder<MobEffect, MobEffect> RADIATION = REGISTRY.register("radiation", () -> new RadiationMobEffect());
   public static final DeferredHolder<MobEffect, MobEffect> IMPENDING_DOOM = REGISTRY.register("impending_doom", () -> new ImpendingDoomMobEffect());
   public static final DeferredHolder<MobEffect, MobEffect> CONTAMINATED = REGISTRY.register("contaminated", () -> new ContaminatedMobEffect());

   @net.neoforged.bus.api.SubscribeEvent
   public static void onEffectRemoved(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove event) {
      net.minecraft.world.effect.MobEffectInstance effectInstance = event.getEffectInstance();
      if (effectInstance != null)
         expireEffects(event.getEntity(), effectInstance);
   }

   @net.neoforged.bus.api.SubscribeEvent
   public static void onEffectExpired(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired event) {
      net.minecraft.world.effect.MobEffectInstance effectInstance = event.getEffectInstance();
      if (effectInstance != null)
         expireEffects(event.getEntity(), effectInstance);
   }

   private static void expireEffects(net.minecraft.world.entity.Entity entity, net.minecraft.world.effect.MobEffectInstance effectInstance) {
      if (effectInstance.getEffect().is(IMPENDING_DOOM)) {
         net.mcreator.crustychunks.procedures.AISiegeTriggerProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity);
      }
   }
}
