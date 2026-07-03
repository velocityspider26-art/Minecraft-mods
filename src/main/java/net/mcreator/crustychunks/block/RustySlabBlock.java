package net.mcreator.crustychunks.block;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class RustySlabBlock extends SlabBlock {
   public RustySlabBlock() {
      super(Properties.of().instrument(NoteBlockInstrument.BASEDRUM).mapColor(MapColor.WOOD).sound(SoundType.NETHERITE_BLOCK).strength(13.0F, 5.0F));
   }
}
