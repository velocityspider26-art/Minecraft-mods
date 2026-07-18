package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "crusty_chunks",
   value = {Dist.CLIENT}
)
public class WariumExplosionClientProcedure {
   public static final List<WariumExplosionClientProcedure.NuclearBlast> ACTIVE_NUKES = new ArrayList<>();

   public static void execute(LevelAccessor world, final double x, final double y, final double z, double power, WariumExplosionClientProcedure.BlastType type) {
      RandomSource rand = RandomSource.create();
      if (type == WariumExplosionClientProcedure.BlastType.NUCLEAR) {
         final Minecraft mc = Minecraft.getInstance();
         double dist = mc.gameRenderer.getMainCamera().getPosition().distanceTo(new Vec3(x, y, z));
         long delayMs = (long)(dist / 40.0 * 1000.0);
         if (delayMs <= 0L) {
            ClientSoundUtils.playSoundAtDistance(x, y, z, "crusty_chunks:explosioncolossalfar", 400.0F, 0.95F);
            ClientSoundUtils.playSoundAtDistance(x, y, z, "crusty_chunks:explosioncolossal", 80.0F, 0.95F);
         } else {
            new Timer().schedule(new TimerTask() {
               @Override
               public void run() {
                  mc.execute(() -> {
                     ClientSoundUtils.playSoundAtDistance(x, y, z, "crusty_chunks:explosioncolossalfar", 400.0F, 1.15F);
                     ClientSoundUtils.playSoundAtDistance(x, y, z, "crusty_chunks:explosioncolossal", 60.0F, 1.15F);
                  });
               }
            }, delayMs);
         }

         synchronized (ACTIVE_NUKES) {
            ACTIVE_NUKES.add(new WariumExplosionClientProcedure.NuclearBlast(world, x, y, z, power));
         }
      } else {
         double pitchDivider = power / 10.0;
         WariumExplosionClientProcedure.ExplosionTier tier = WariumExplosionClientProcedure.ExplosionTier.get(power);
         if (power > 7.0 && net.mcreator.crustychunks.compat.WariumNukeEffects.shockwaveEnabled()) {
            spawnParticle(world, CrustyChunksModParticleTypes.SHOCK_RING, x, y, z, 0.0, 0.0, 0.0);
         }

         float rumblePitch = (float)Mth.nextDouble(rand, (double)tier.rumbleMin, (double)tier.rumbleMax)
            - (tier == WariumExplosionClientProcedure.ExplosionTier.COLOSSAL ? 0.0F : (float)pitchDivider);
         float farPitch = (float)Mth.nextDouble(rand, (double)tier.farMinP, (double)tier.farMaxP)
            - (tier == WariumExplosionClientProcedure.ExplosionTier.COLOSSAL ? 0.0F : (float)(pitchDivider / 2.0));
         float nearPitch = (float)Mth.nextDouble(rand, (double)tier.nearMinP, (double)tier.nearMaxP)
            - (tier == WariumExplosionClientProcedure.ExplosionTier.COLOSSAL ? 0.0F : (float)(pitchDivider / 3.0));
         ClientSoundUtils.playSoundAtDistance(x, y, z, "crusty_chunks:farblast", power >= 30.0 ? 800.0F : 200.0F, rumblePitch);
         ClientSoundUtils.playSoundAtDistance(x, y, z, tier.farSound, tier.farVol, farPitch);
         ClientSoundUtils.playSoundAtDistance(x, y, z, tier.nearSound, tier.nearVol, nearPitch);
         int maxParticles = net.mcreator.crustychunks.compat.WariumNukeEffects.clientParticleBudget((int)Math.min(200.0, power * 15.0));
         Supplier<SimpleParticleType> fireType = power <= 5.0
            ? CrustyChunksModParticleTypes.SPARKS
            : (
               power <= 15.0
                  ? CrustyChunksModParticleTypes.FIRE_EXPLOSION
                  : (power <= 35.0 ? CrustyChunksModParticleTypes.FIRE_EXPLOSION_SIZE_2 : CrustyChunksModParticleTypes.FIRE_EXPLOSION_SIZE_3)
            );
         Supplier<SimpleParticleType> smokeType = power <= 2.0
            ? CrustyChunksModParticleTypes.DARK_SMOKE
            : (
               power <= 10.0
                  ? CrustyChunksModParticleTypes.LARGE_SMOKE
                  : (power <= 30.0 ? CrustyChunksModParticleTypes.LARGE_SMOKE_SIZE_2 : CrustyChunksModParticleTypes.LARGE_SMOKE_SIZE_3)
            );
         int loops = power <= 3.0 ? maxParticles * 2 : maxParticles;

         for (int i = 0; i < loops; i++) {
            double fSpeed = power <= 3.0 ? 0.2 : 0.025;
            spawnRandomParticle(world, fireType, x, y, z, fSpeed * power, rand);
            spawnRandomParticle(world, smokeType, x, y, z, 0.05 * power, rand);
         }
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post e) {
      if (true) {
         synchronized (ACTIVE_NUKES) {
            Iterator<WariumExplosionClientProcedure.NuclearBlast> it = ACTIVE_NUKES.iterator();

            while (it.hasNext()) {
               WariumExplosionClientProcedure.NuclearBlast nuke = it.next();
               NuclearEffectProcedure.execute(nuke.world, nuke.x, nuke.y, nuke.z, nuke.power, nuke.time);
               nuke.time++;
               if (nuke.time > 410.0 * nuke.power) {
                  it.remove();
               }
            }
         }
      }
   }

   private static void spawnParticle(
      LevelAccessor world, Supplier<SimpleParticleType> typeSupplier, double x, double y, double z, double vx, double vy, double vz
   ) {
      world.addParticle((ParticleOptions)typeSupplier.get(), x, y, z, vx, vy, vz);
   }

   private static void spawnRandomParticle(
      LevelAccessor world, Supplier<SimpleParticleType> typeSupplier, double x, double y, double z, double speed, RandomSource rand
   ) {
      double vx = Mth.nextDouble(rand, -1.0, 1.0) * speed;
      double vy = Mth.nextDouble(rand, -1.0, 1.0) * speed;
      double vz = Mth.nextDouble(rand, -1.0, 1.0) * speed;
      spawnParticle(world, typeSupplier, x, y, z, vx, vy, vz);
   }

   public static enum BlastType {
      CONVENTIONAL,
      FIRE,
      NUCLEAR;
   }

   private static enum ExplosionTier {
      TINY(0.9, 1.15F, 1.2F, "crusty_chunks:tinyexplosionfar", 80.0F, 0.95F, 1.05F, "crusty_chunks:explosionsmall", 15.0F, 1.1F, 1.15F),
      SMALL(5.0, 1.15F, 1.2F, "crusty_chunks:explosionsmallfar", 80.0F, 1.0F, 1.05F, "crusty_chunks:explosionsmall", 15.0F, 1.0F, 1.05F),
      MEDIUM(10.0, 1.15F, 1.2F, "crusty_chunks:explosionlargefar", 80.0F, 1.15F, 1.2F, "crusty_chunks:explosionlarge", 20.0F, 1.15F, 1.2F),
      LARGE(20.0, 1.15F, 1.2F, "crusty_chunks:explosionlargefar", 80.0F, 1.15F, 1.2F, "crusty_chunks:explosionlarge", 20.0F, 1.15F, 1.2F),
      HUGE(30.0, 1.15F, 1.2F, "crusty_chunks:explosionlargefar", 80.0F, 1.15F, 1.2F, "crusty_chunks:explosionlarge", 20.0F, 1.15F, 1.2F),
      COLOSSAL(Double.MAX_VALUE, 0.95F, 1.0F, "crusty_chunks:explosioncolossalfar", 400.0F, 1.15F, 1.2F, "crusty_chunks:explosioncolossal", 60.0F, 1.15F, 1.2F);

      final double maxPower;
      final float rumbleMin;
      final float rumbleMax;
      final String farSound;
      final String nearSound;
      final float farVol;
      final float nearVol;
      final float farMinP;
      final float farMaxP;
      final float nearMinP;
      final float nearMaxP;

      private ExplosionTier(
         double maxPower, float rMin, float rMax, String far, float fV, float fMinP, float fMaxP, String near, float nV, float nMinP, float nMaxP
      ) {
         this.maxPower = maxPower;
         this.rumbleMin = rMin;
         this.rumbleMax = rMax;
         this.farSound = far;
         this.farVol = fV;
         this.farMinP = fMinP;
         this.farMaxP = fMaxP;
         this.nearSound = near;
         this.nearVol = nV;
         this.nearMinP = nMinP;
         this.nearMaxP = nMaxP;
      }

      public static WariumExplosionClientProcedure.ExplosionTier get(double power) {
         for (WariumExplosionClientProcedure.ExplosionTier tier : values()) {
            if (power <= tier.maxPower) {
               return tier;
            }
         }

         return COLOSSAL;
      }
   }

   public static class NuclearBlast {
      public LevelAccessor world;
      public double x;
      public double y;
      public double z;
      public double power;
      public double time;

      public NuclearBlast(LevelAccessor w, double x, double y, double z, double p) {
         this.world = w;
         this.x = x;
         this.y = y;
         this.z = z;
         this.power = p;
         this.time = 0.0;
      }
   }
}
