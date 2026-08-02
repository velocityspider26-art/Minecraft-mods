package shipwrights.genesis.mixin;

import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelChunk.RebindableTickingBlockEntityWrapper.class)
public interface RebindableTickingBlockEntityWrapperAccessor {
    @Invoker("rebind")
    void genesis$rebind(TickingBlockEntity ticker);
}
