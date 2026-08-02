package shipwrights.genesis.mixin;

import net.minecraft.client.renderer.ViewArea;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets a standing render grid be pointed at a different world.
 *
 * <p>{@code ViewArea.level} is final because vanilla never re-points a grid —
 * it throws the whole thing away and builds another. Reusing one across a
 * crossing between worlds of the same shape needs exactly this one field to
 * move, and nothing else about the class to change.</p>
 */
@Mixin(ViewArea.class)
public interface ViewAreaAccessor {
    @Mutable
    @Accessor("level")
    void genesis$setLevel(Level level);

    @Accessor("sectionGridSizeX")
    int genesis$getSectionGridSizeX();

    @Accessor("sectionGridSizeY")
    int genesis$getSectionGridSizeY();

    @Accessor("level")
    Level genesis$getLevel();
}
