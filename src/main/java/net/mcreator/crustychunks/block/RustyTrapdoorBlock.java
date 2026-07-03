package net.mcreator.crustychunks.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class RustyTrapdoorBlock extends TrapDoorBlock {
   public RustyTrapdoorBlock() {
      super(BlockSetType.STONE, Properties.of().instrument(NoteBlockInstrument.BASEDRUM).mapColor(MapColor.WOOD).sound(SoundType.NETHERITE_BLOCK).strength(13.0F, 5.0F));
   }
}
