package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.network.ClientExplosionPacket;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.network.PacketDistributor;

public class NuclearExplosionExampleProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, double rawPower) {
      final double power = rawPower * net.mcreator.crustychunks.compat.WariumNukeEffects.blastRadiusMultiplier();
      WariumNuclearExplosionShapesProcedure.execute(world, x, y, z, power);
      if (!world.isClientSide()) {
         CrustyChunksMod.sendToNear(world, x, y, z, 4096.0, new ClientExplosionPacket(x, y, z, power, WariumExplosionClientProcedure.BlastType.NUCLEAR));
         net.mcreator.crustychunks.compat.WariumNukeEffects.onServerExplosion(world, x, y, z, power, true);
      }

      if (SpaceLogicProcedure.execute(world, x, y, z)) {
         SpaceNuclearExplosionProcedure.execute(world, x, y, z, power);
      } else {
         int totalTicks = (int)(2.5 * power);

         for (int i = 0; i < totalTicks; i++) {
            int time = i;
            CrustyChunksMod.queueServerWork(time, () -> WariumNuclearExplosionTickProcedure.execute(world, x, y, z, power, (double)time));
         }
      }
   }
}
