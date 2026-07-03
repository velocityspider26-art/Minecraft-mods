package net.mcreator.crustychunks.init;

import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CrustyChunksModGameRules {
   public static GameRules.Key<GameRules.IntegerValue> ENRICHMENT_TIME;
   public static GameRules.Key<GameRules.BooleanValue> ALLOW_IMPACT_FUZE;
   public static GameRules.Key<GameRules.BooleanValue> STRATEGIC_WEAPONS;
   public static GameRules.Key<GameRules.BooleanValue> APOCALYPSE_MODE;
   public static GameRules.Key<GameRules.IntegerValue> BULLET_DAMAGE_MULTIPLIER;
   public static GameRules.Key<GameRules.BooleanValue> WARIUM_APOCALYPSE_DYNAMIC_PRODUCTION;

   @SubscribeEvent
   public static void registerGameRules(FMLCommonSetupEvent event) {
      ENRICHMENT_TIME = GameRules.register("enrichmentTime", GameRules.Category.MISC, GameRules.IntegerValue.create(800));
      ALLOW_IMPACT_FUZE = GameRules.register("allowImpactFuze", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
      STRATEGIC_WEAPONS = GameRules.register("strategicWeapons", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
      APOCALYPSE_MODE = GameRules.register("apocalypseMode", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
      BULLET_DAMAGE_MULTIPLIER = GameRules.register("bulletDamageMultiplier", GameRules.Category.PLAYER, GameRules.IntegerValue.create(1));
      WARIUM_APOCALYPSE_DYNAMIC_PRODUCTION = GameRules.register("wariumApocalypseDynamicProduction", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
   }
}
