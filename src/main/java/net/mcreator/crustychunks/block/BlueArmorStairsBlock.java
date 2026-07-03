package net.mcreator.crustychunks.block;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;

public class BlueArmorStairsBlock extends StairBlock {
   public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, 6);

   public BlueArmorStairsBlock() {
      super(Blocks.AIR.defaultBlockState(), Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.NETHERITE_BLOCK).strength(15.0F));
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(DAMAGE, 0));
   }

   public float getExplosionResistance() {
      return 15.0F;
   }

   public boolean isRandomlyTicking(BlockState state) {
      return false;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{DAMAGE});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)super.getStateForPlacement(context).setValue(DAMAGE, 0);
   }
}
