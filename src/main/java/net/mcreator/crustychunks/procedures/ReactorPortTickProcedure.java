package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class ReactorPortTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if ((new Object() {
         public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
         }
      }).getValue(world, BlockPos.containing(x, y - 1.0, z), "Greenlight") && 0 == (new Object() {
         public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
            AtomicInteger _retval = new AtomicInteger(0);
            BlockEntity _ent = world.getBlockEntity(pos);
            if (_ent != null) {
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
            }

            return _retval.get();
         }
      }).getAmount(world, BlockPos.containing(x, y - 1.0, z), 0) && (new Object() {
         public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
            AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
            BlockEntity _ent = world.getBlockEntity(pos);
            if (_ent != null) {
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
            }

            return _retval.get();
         }
      }).getItemStack(world, BlockPos.containing(x, y - 1.0, z), 0).getItem() == ItemStack.EMPTY.getItem()) {
         BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y - 1.0, z));
         if (_ent != null) {
            int _slotid = 0;
            ItemStack _setstack = (new Object() {
                  public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
                     AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
                     BlockEntity _ent = world.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
                     }

                     return _retval.get();
                  }
               })
               .getItemStack(world, BlockPos.containing(x, y, z), 0)
               .copy();
            _setstack.setCount(1);
            {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
               if (capability instanceof IItemHandlerModifiable) {
                  ((IItemHandlerModifiable)capability).setStackInSlot(0, _setstack);
               }
            }
}
         }

         _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
         if (_ent != null) {
            int _slotid = 0;
            int _amount = 1;
            {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
               if (capability instanceof IItemHandlerModifiable) {
                  ItemStack _stk = capability.getStackInSlot(0).copy();
                  _stk.shrink(1);
                  ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
               }
            }
}
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ReactorPortTickProcedure.execute", _wtSafe);
      }
   }
}
