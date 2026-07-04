package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SolidEntityHitProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      try {
      if (entity != null) {
         entity.hurt(
            new DamageSource(
               world.registryAccess()
                  .registryOrThrow(Registries.DAMAGE_TYPE)
                  .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("crusty_chunks:armor_bypass_damage")))
            ),
            150.0F
         );
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SolidEntityHitProcedure.execute", _wtSafe);
      }
   }
}
