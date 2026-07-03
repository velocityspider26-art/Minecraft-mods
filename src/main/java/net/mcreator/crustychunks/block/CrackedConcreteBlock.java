package net.mcreator.crustychunks.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.procedures.ConcreteDamage2Procedure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.util.DeferredSoundType;

public class CrackedConcreteBlock extends Block {
   public CrackedConcreteBlock() {
      super(
         Properties.of()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .mapColor(MapColor.CLAY)
            .sound(
               new DeferredSoundType(
                  1.0F,
                  1.0F,
                  () -> (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.basalt.break")),
                  () -> (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.stone.step")),
                  () -> (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.basalt.place")),
                  () -> (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.basalt.hit")),
                  () -> (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.stone.fall"))
               )
            )
            .strength(4.0F)
      );
   }

   public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 15;
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player) {
      return new ItemStack((ItemLike)CrustyChunksModBlocks.REENFORCED_CONCRETE.get());
   }

   public void wasExploded(Level world, BlockPos pos, Explosion e) {
      super.wasExploded(world, pos, e);
      ConcreteDamage2Procedure.execute(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
   }
}
