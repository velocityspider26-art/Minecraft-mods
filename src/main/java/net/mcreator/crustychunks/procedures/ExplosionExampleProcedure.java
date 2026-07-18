package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.network.ClientExplosionPacket;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.network.PacketDistributor;

public class ExplosionExampleProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, double power) {
      WariumExplosionServerProcedure.execute(world, x, y, z, power);
      if (!world.isClientSide()) {
         CrustyChunksMod.sendToNear(world, x, y, z, Math.max(512.0, power * 30.0), new ClientExplosionPacket(x, y, z, power, WariumExplosionClientProcedure.BlastType.CONVENTIONAL));
         net.mcreator.crustychunks.compat.WariumNukeEffects.onServerExplosion(world, x, y, z, power, false);
      }
   }
}
