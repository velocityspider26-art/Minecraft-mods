package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.mcreator.crustychunks.utils.WariumCaps;
import java.util.concurrent.atomic.AtomicInteger;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class GeneratorTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      try {
      double Kinetic = 0.0;
      Kinetic = TestShaftProcedure.execute(world, x, y, z, blockstate, (new Object() {
         public Direction getDirection(BlockState _bs) {
            if (_bs.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty _dp) {
               return (Direction)_bs.getValue(_dp);
            } else {
               if (_bs.getBlock().getStateDefinition().getProperty("axis") instanceof EnumProperty _ep && _ep.getPossibleValues().toArray()[0] instanceof Axis) {
                  return Direction.fromAxisAndDirection((Axis)_bs.getValue(_ep), AxisDirection.POSITIVE);
               }

               return Direction.NORTH;
            }
         }
      }).getDirection(blockstate));
      if (0.0 < Kinetic) {
         if (world instanceof ServerLevel _level) {
            _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SPARKS.get(), x + 0.5, y + 0.5, z + 0.5, 2, 0.3, 0.3, 0.3, 1.0);
         }

         if ((new Object() {
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
         }).getEnergyStored(world, BlockPos.containing(x, y, z)) < (new Object() {
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
         }).getMaxEnergyStored(world, BlockPos.containing(x, y, z))) {
            BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            int _amount = (int)Math.max(0L, Math.round(0.95 * (Kinetic / 1.0)));
            if (_ent != null) {
               {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) capability.receiveEnergy(_amount, false);
}
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("GeneratorTickProcedure.execute", _wtSafe);
      }
   }
}
