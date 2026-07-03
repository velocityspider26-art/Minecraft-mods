package net.mcreator.crustychunks.procedures;

import java.util.Map.Entry;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class FuelTankDamagedProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.FUEL_TANK.get()
         || world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.FUEL_TANK_MODULE.get()
         || world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.FUEL_TANK_INPUT.get()) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockState _bs = ((Block)CrustyChunksModBlocks.DAMAGEDFUELTANK.get()).defaultBlockState();
         BlockState _bso = world.getBlockState(_bp);
         java.util.Iterator<Property<?>> _be = _bso.getProperties().iterator();

         while (_be.hasNext()) {

            Property<?> entry_prop = _be.next();

            Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
            if (_property != null && _bs.getValue(_property) != null) {
               try {
                  _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
               } catch (Exception var15) {
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
            BlockEntity var18 = world.getBlockEntity(_bp);
            if (var18 != null) {
               try {
                  var18.loadWithComponents(_bnbt, world.registryAccess());
               } catch (Exception var14) {
               }
            }
         }

         if (world instanceof ServerLevel _level) {
            _level.sendParticles(ParticleTypes.FLAME, x + 0.5, y + 0.5, z + 0.5, 15, 0.25, 0.25, 0.25, 0.1);
         }
      }
   }
}
