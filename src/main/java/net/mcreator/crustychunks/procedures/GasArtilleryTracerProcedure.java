package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class GasArtilleryTracerProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         world.addParticle(
            (SimpleParticleType)CrustyChunksModParticleTypes.GREEN_TRACER.get(),
            x,
            y,
            z,
            immediatesourceentity.getLookAngle().x
               * -1.0
               * (immediatesourceentity instanceof Projectile _projEntxx ? _projEntxx.getDeltaMovement().length() : 0.0),
            immediatesourceentity.getLookAngle().y * -1.0 * (immediatesourceentity instanceof Projectile _projEntx ? _projEntx.getDeltaMovement().length() : 0.0),
            immediatesourceentity.getLookAngle().z * (immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0)
         );
         world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.CAMP_SMOKE.get(), x, y, z, 0.0, 0.0, 0.0);
         if (OrdinanceTriggerProcedure.execute(world, immediatesourceentity)) {
            GasBombHitsBlockProcedure.execute(world, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }

         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         if (immediatesourceentity.getPersistentData().getDouble("T") >= 20.0 && world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.launch")),
                  SoundSource.NEUTRAL,
                  4.0F,
                  (float)(3.0 - immediatesourceentity.getLookAngle().y * 3.0)
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.launch")),
                  SoundSource.NEUTRAL,
                  4.0F,
                  (float)(3.0 - immediatesourceentity.getLookAngle().y * 3.0),
                  false
               );
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("GasArtilleryTracerProcedure.execute", _wtSafe);
      }
   }
}
