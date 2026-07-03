package net.mcreator.crustychunks.init;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.Category;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.GameRules.Key;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;

@EventBusSubscriber(
   bus = Bus.MOD
)
public class CrustyChunksModGameRules {
   public static final Key<IntegerValue> ENRICHMENT_TIME = GameRules.register("enrichmentTime", Category.MISC, IntegerValue.create(800));
   public static final Key<BooleanValue> ALLOW_IMPACT_FUZE = GameRules.register("allowImpactFuze", Category.PLAYER, BooleanValue.create(true));
   public static final Key<BooleanValue> STRATEGIC_WEAPONS = GameRules.register("strategicWeapons", Category.MISC, BooleanValue.create(true));
   public static final Key<BooleanValue> APOCALYPSE_MODE = GameRules.register("apocalypseMode", Category.PLAYER, BooleanValue.create(false));
   public static final Key<IntegerValue> BULLET_DAMAGE_MULTIPLIER = GameRules.register("bulletDamageMultiplier", Category.PLAYER, IntegerValue.create(1));
   public static final Key<BooleanValue> WARIUM_APOCALYPSE_DYNAMIC_PRODUCTION = GameRules.register(
      "wariumApocalypseDynamicProduction", Category.MOBS, BooleanValue.create(true)
   );
}
