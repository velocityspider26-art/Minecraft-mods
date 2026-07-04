package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.mcreator.crustychunks.utils.WariumCaps;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EnergyMeterTestProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(Component.literal((new Object() {
               public int getEnergyStored(LevelAccessor level, BlockPos pos) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
                  }

                  return _retval.get();
               }
            }).getEnergyStored(world, BlockPos.containing(x, y, z)) + "/" + (new Object() {
               public int getMaxEnergyStored(LevelAccessor level, BlockPos pos) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getMaxEnergyStored());
}
                  }

                  return _retval.get();
               }
            }).getMaxEnergyStored(world, BlockPos.containing(x, y, z)) + " FE"), true);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("EnergyMeterTestProcedure.execute", _wtSafe);
      }
   }
}
