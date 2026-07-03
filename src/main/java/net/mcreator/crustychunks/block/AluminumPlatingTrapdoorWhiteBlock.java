package net.mcreator.crustychunks.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public class AluminumPlatingTrapdoorWhiteBlock extends TrapDoorBlock {
   public AluminumPlatingTrapdoorWhiteBlock() {
      super(BlockSetType.STONE, Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.NETHERITE_BLOCK).strength(4.0F));
   }
}
