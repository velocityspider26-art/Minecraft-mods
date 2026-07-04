package net.mcreator.crustychunks.procedures;

import java.util.Map.Entry;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class SummonatorActivateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world instanceof ServerLevel _level) {
         _level.sendParticles(ParticleTypes.SONIC_BOOM, x, y, z, 15, 3.0, 3.0, 3.0, 1.0);
      }

      int horizontalRadiusSquare = 49;
      int verticalRadiusSquare = 49;
      int yIterationsSquare = verticalRadiusSquare;

      for (int i = -verticalRadiusSquare; i <= yIterationsSquare; i++) {
         for (int xi = -horizontalRadiusSquare; xi <= horizontalRadiusSquare; xi++) {
            for (int zi = -horizontalRadiusSquare; zi <= horizontalRadiusSquare; zi++) {
               if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock() == CrustyChunksModBlocks.ROBOT_CHUTE.get()) {
                  BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                  BlockState _bs = ((Block)CrustyChunksModBlocks.ACTIVE_ROBOT_CHUTE.get()).defaultBlockState();
                  BlockState _bso = world.getBlockState(_bp);
                  java.util.Iterator<Property<?>> _be = _bso.getProperties().iterator();

                  while (_be.hasNext()) {

                     Property<?> entry_prop = _be.next();

                     Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                     if (_property != null && _bs.getValue(_property) != null) {
                        try {
                           _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                        } catch (Exception var20) {
                        }
                     }
                  }

                  BlockEntity _bex = world.getBlockEntity(_bp);
                  CompoundTag _bnbt = null;
                  if (_bex != null) {
                     _bnbt = _bex.saveWithFullMetadata(world.registryAccess());
                     _bex.setRemoved();
                  }

                  world.setBlock(_bp, _bs, 3);
                  if (_bnbt != null) {
                     BlockEntity var39 = world.getBlockEntity(_bp);
                     if (var39 != null) {
                        try {
                           var39.loadWithComponents(_bnbt, world.registryAccess());
                        } catch (Exception var27) {
                        }
                     }
                  }
               }

               if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock() == CrustyChunksModBlocks.GAS_DISPENSER.get()) {
                  BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                  BlockState _bs = ((Block)CrustyChunksModBlocks.GAS_DISPENSER.get()).defaultBlockState();
                  BlockState _bso = world.getBlockState(_bp);
                  java.util.Iterator<Property<?>> var40 = _bso.getProperties().iterator();

                  while (var40.hasNext()) {

                     Property<?> entry_prop = var40.next();

                     Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                     if (_property != null && _bs.getValue(_property) != null) {
                        try {
                           _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                        } catch (Exception var26) {
                        }
                     }
                  }

                  BlockEntity _bex = world.getBlockEntity(_bp);
                  CompoundTag _bnbtx = null;
                  if (_bex != null) {
                     _bnbtx = _bex.saveWithFullMetadata(world.registryAccess());
                     _bex.setRemoved();
                  }

                  world.setBlock(_bp, _bs, 3);
                  if (_bnbtx != null) {
                     BlockEntity var42 = world.getBlockEntity(_bp);
                     if (var42 != null) {
                        try {
                           var42.loadWithComponents(_bnbtx, world.registryAccess());
                        } catch (Exception var25) {
                        }
                     }
                  }
               }

               if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock() == CrustyChunksModBlocks.SUMMONATOR.get()) {
                  BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                  BlockState _bs = ((Block)CrustyChunksModBlocks.SUMMONATOR_ACTIVE.get()).defaultBlockState();
                  BlockState _bso = world.getBlockState(_bp);
                  java.util.Iterator<Property<?>> var43 = _bso.getProperties().iterator();

                  while (var43.hasNext()) {

                     Property<?> entry_prop = var43.next();

                     Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                     if (_property != null && _bs.getValue(_property) != null) {
                        try {
                           _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                        } catch (Exception var24) {
                        }
                     }
                  }

                  BlockEntity _bexx = world.getBlockEntity(_bp);
                  CompoundTag _bnbtxx = null;
                  if (_bexx != null) {
                     _bnbtxx = _bexx.saveWithFullMetadata(world.registryAccess());
                     _bexx.setRemoved();
                  }

                  world.setBlock(_bp, _bs, 3);
                  if (_bnbtxx != null) {
                     BlockEntity var45 = world.getBlockEntity(_bp);
                     if (var45 != null) {
                        try {
                           var45.loadWithComponents(_bnbtxx, world.registryAccess());
                        } catch (Exception var23) {
                        }
                     }
                  }
               }

               if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock() == CrustyChunksModBlocks.DEFENSE_CORE.get()) {
                  BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                  BlockState _bs = ((Block)CrustyChunksModBlocks.DEFENSE_CORE.get()).defaultBlockState();
                  BlockState _bso = world.getBlockState(_bp);
                  java.util.Iterator<Property<?>> var46 = _bso.getProperties().iterator();

                  while (var46.hasNext()) {

                     Property<?> entry_prop = var46.next();

                     Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                     if (_property != null && _bs.getValue(_property) != null) {
                        try {
                           _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                        } catch (Exception var22) {
                        }
                     }
                  }

                  BlockEntity _bexxx = world.getBlockEntity(_bp);
                  CompoundTag _bnbtxxx = null;
                  if (_bexxx != null) {
                     _bnbtxxx = _bexxx.saveWithFullMetadata(world.registryAccess());
                     _bexxx.setRemoved();
                  }

                  world.setBlock(_bp, _bs, 3);
                  if (_bnbtxxx != null) {
                     BlockEntity var48 = world.getBlockEntity(_bp);
                     if (var48 != null) {
                        try {
                           var48.loadWithComponents(_bnbtxxx, world.registryAccess());
                        } catch (Exception var21) {
                        }
                     }
                  }
               }
            }
         }
      }

      CrustyChunksModVariables.MapVariables.get(world).Production++;
      CrustyChunksModVariables.MapVariables.get(world).syncData(world);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SummonatorActivateProcedure.execute", _wtSafe);
      }
   }
}
