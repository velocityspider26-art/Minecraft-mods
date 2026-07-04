package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SulfuricAcidTouchProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      try {
      if (entity != null) {
         entity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.ON_FIRE)), 1.0F);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SulfuricAcidTouchProcedure.execute", _wtSafe);
      }
   }
}
