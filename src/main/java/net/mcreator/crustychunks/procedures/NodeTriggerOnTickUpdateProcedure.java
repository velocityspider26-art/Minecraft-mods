package net.mcreator.crustychunks.procedures;

import java.util.Map.Entry;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class NodeTriggerOnTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double Power = 0.0;
      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.NODE_TRIGGER.get()) {
         if (0.0 < (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Trigger")) {
            Power = (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Trigger");
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockState _bs = ((Block)CrustyChunksModBlocks.NODE_TRIGGER_ON.get()).defaultBlockState();
            BlockState _bso = world.getBlockState(_bp);
            java.util.Iterator<Property<?>> _level = _bso.getProperties().iterator();

            while (_level.hasNext()) {

               Property<?> entry_prop = _level.next();

               Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
               if (_property != null && _bs.getValue(_property) != null) {
                  try {
                     _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                  } catch (Exception var17) {
                  }
               }
            }

            world.setBlock(_bp, _bs, 3);
            if (!world.isClientSide()) {
               _bp = BlockPos.containing(x, y, z);
               BlockEntity _blockEntity = world.getBlockEntity(_bp);
               _bso = world.getBlockState(_bp);
               if (_blockEntity != null) {
                  _blockEntity.getPersistentData().putDouble("Trigger", Power);
               }

               if (world instanceof Level _levelx) {
                  _levelx.sendBlockUpdated(_bp, _bso, _bso, 3);
               }
            }
         }
      } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.NODE_TRIGGER_ON.get()) {
         if (0.0 < (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Trigger")) {
            if (!world.isClientSide()) {
               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityx = world.getBlockEntity(_bp);
               BlockState _bs = world.getBlockState(_bp);
               if (_blockEntityx != null) {
                  _blockEntityx.getPersistentData().putDouble("Trigger", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Trigger") - 1.0);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bp, _bs, _bs, 3);
               }
            }
         } else {
            BlockPos _bpx = BlockPos.containing(x, y, z);
            BlockState _bsx = ((Block)CrustyChunksModBlocks.NODE_TRIGGER.get()).defaultBlockState();
            BlockState _bso = world.getBlockState(_bpx);
            java.util.Iterator<Property<?>> var30 = _bso.getProperties().iterator();

            while (var30.hasNext()) {

               Property<?> entry_prop = var30.next();

               Property _property = _bsx.getBlock().getStateDefinition().getProperty(entry_prop.getName());
               if (_property != null && _bsx.getValue(_property) != null) {
                  try {
                     _bsx = (BlockState)_bsx.setValue(_property, _bso.getValue(entry_prop));
                  } catch (Exception var16) {
                  }
               }
            }

            world.setBlock(_bpx, _bsx, 3);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("NodeTriggerOnTickUpdateProcedure.execute", _wtSafe);
      }
   }
}
