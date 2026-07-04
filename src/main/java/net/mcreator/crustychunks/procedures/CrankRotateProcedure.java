package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class CrankRotateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      try {
      if (blockstate.getBlock().getStateDefinition().getProperty("positioned") instanceof BooleanProperty _getbp1 && (Boolean)blockstate.getValue(_getbp1)) {
         return;
      }

      BlockPos _pos = BlockPos.containing(x, y, z);
      BlockState _bs = world.getBlockState(_pos);
      if (_bs.getBlock().getStateDefinition().getProperty("positioned") instanceof BooleanProperty _booleanProp) {
         world.setBlock(_pos, (BlockState)_bs.setValue(_booleanProp, true), 3);
      }

      CrustyChunksMod.queueServerWork(
         1,
         () -> {
            Direction _dir = (new Object() {
                  public Direction getDirection(BlockPos pos) {
                     BlockState _bs = world.getBlockState(pos);
                     Property<?> property = _bs.getBlock().getStateDefinition().getProperty("facing");
                     if (property != null) {
                        Comparable var5 = _bs.getValue(property);
                        if (var5 instanceof Direction) {
                           return (Direction)var5;
                        }
                     }

                     if (_bs.hasProperty(BlockStateProperties.AXIS)) {
                        return Direction.fromAxisAndDirection((Axis)_bs.getValue(BlockStateProperties.AXIS), AxisDirection.POSITIVE);
                     } else {
                        return _bs.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)
                           ? Direction.fromAxisAndDirection((Axis)_bs.getValue(BlockStateProperties.HORIZONTAL_AXIS), AxisDirection.POSITIVE)
                           : Direction.NORTH;
                     }
                  }
               })
               .getDirection(BlockPos.containing(x, y, z))
               .getClockWise(Axis.Y)
               .getClockWise(Axis.Y);
            BlockPos _posx = BlockPos.containing(x, y, z);
            BlockState _bsx = world.getBlockState(_posx);
            if (_bsx.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty _dp && _dp.getPossibleValues().contains(_dir)) {
               world.setBlock(_posx, (BlockState)_bsx.setValue(_dp, _dir), 3);
               return;
            }

            if (_bsx.getBlock().getStateDefinition().getProperty("axis") instanceof EnumProperty _ap && _ap.getPossibleValues().contains(_dir.getAxis())) {
               world.setBlock(_posx, (BlockState)_bsx.setValue(_ap, _dir.getAxis()), 3);
            }
         }
      );
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("CrankRotateProcedure.execute", _wtSafe);
      }
   }
}
