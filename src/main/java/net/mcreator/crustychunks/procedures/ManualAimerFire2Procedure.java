package net.mcreator.crustychunks.procedures;

import javax.annotation.Nullable;
import net.mcreator.crustychunks.entity.SeatEntityEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class ManualAimerFire2Procedure {
   @SubscribeEvent
   public static void onLeftClickBlock(LeftClickBlock event) {
      execute(event, event.getLevel(), event.getEntity());
   }

   public static void execute(LevelAccessor world, Entity entity) {
      try {
      execute(null, world, entity);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ManualAimerFire2Procedure.execute", _wtSafe);
      }
   }

   private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
      try {
      if (entity != null) {
         if (entity.getRootVehicle() instanceof SeatEntityEntity
            && world.getBlockState(BlockPos.containing(entity.getRootVehicle().getX(), entity.getRootVehicle().getY(), entity.getRootVehicle().getZ())).getBlock()
               == CrustyChunksModBlocks.MANUAL_AIMER.get()) {
            int _value = 5;
            BlockPos _pos = BlockPos.containing(entity.getRootVehicle().getX(), entity.getRootVehicle().getY(), entity.getRootVehicle().getZ());
            BlockState _bs = world.getBlockState(_pos);
            if (_bs.getBlock().getStateDefinition().getProperty("firing") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value)) {
               world.setBlock(_pos, (BlockState)_bs.setValue(_integerProp, _value), 3);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ManualAimerFire2Procedure.execute", _wtSafe);
      }
   }
}
