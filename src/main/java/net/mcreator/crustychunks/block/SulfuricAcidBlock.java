package net.mcreator.crustychunks.block;

import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.mcreator.crustychunks.procedures.SulfuricAcidTouchProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class SulfuricAcidBlock extends LiquidBlock {
   public SulfuricAcidBlock() {
      super(CrustyChunksModFluids.SULFURIC_ACID.get(),
         Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .strength(100.0F)
            .noCollission()
            .noLootTable()
            .liquid()
            .pushReaction(PushReaction.DESTROY)
            .sound(SoundType.EMPTY)
            .replaceable()
      );
   }

   public void entityInside(BlockState blockstate, Level world, BlockPos pos, Entity entity) {
      super.entityInside(blockstate, world, pos, entity);
      SulfuricAcidTouchProcedure.execute(world, entity);
   }
}
