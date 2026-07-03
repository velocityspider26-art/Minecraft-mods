package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

public class AimerUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      if (entity != null) {
         double Multiplier = 0.0;
         double Pitch = 0.0;
         double Yaw = 0.0;
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.AIMER.get()) {
            if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:cannon")))
               || world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.AIMER_NODE.get()) {
               { final var _fvcc1 = x; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("POSX", _fvcc1)); }
               { final var _fvcc1 = y; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("POSY", _fvcc1)); }
               { final var _fvcc1 = z; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("POSZ", _fvcc1)); }
            }
         } else {
            AimerProcedureProcedure.execute(world, x, y, z, entity, itemstack);
         }
      }
   }
}
