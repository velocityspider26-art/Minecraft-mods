package net.mcreator.crustychunks.procedures;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.neoforged.fml.ModList;

public class WariumNuclearExplosionTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, double power, double time) {
      if (!ModList.get().isLoaded("explosionoverhaul") && world instanceof Level level && !level.isClientSide()) {
         double xRadius = 1.0 + time * 2.0;
         long particleAmount = Math.round(Math.min(xRadius / 6.0 + 1.0, 45.0));
         if (time % 2.0 != 0.0) {
            return;
         }

         double angleStep = (Math.PI * 2) / (double)particleAmount;
         double randomRotation = Math.random() * Math.PI * 2.0;

         for (int loop = 0; (long)loop < particleAmount; loop++) {
            double angle = angleStep * (double)loop + randomRotation;
            double targetX = x + 0.5 + Math.cos(angle) * xRadius;
            double targetZ = z + 0.5 + Math.sin(angle) * xRadius;
            int chunkX = (int)targetX >> 4;
            int chunkZ = (int)targetZ >> 4;
            GlobalChunkLoaderProcedure.registerChunk((ServerLevel)level, chunkX * 16, chunkZ * 16);
            int groundY = world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)targetX, (int)targetZ);
            if (y + 10.0 > (double)groundY) {
               level.explode(null, targetX, (double)(groundY + 11), targetZ, 8.0F, ExplosionInteraction.BLOCK);
            }

            if (time >= 5.0 && y + 5.0 > (double)groundY) {
               float blastPower = (float)(10.0 / Math.ceil(xRadius / 100.0 + 0.01) + 6.0);
               level.explode(null, targetX, Math.max((double)(groundY + 22), y + 7.0), targetZ, blastPower, ExplosionInteraction.BLOCK);
            }
         }

         return;
      }
   }
}
