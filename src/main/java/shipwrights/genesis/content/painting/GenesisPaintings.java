package shipwrights.genesis.content.painting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import shipwrights.genesis.GenesisMod;

public class GenesisPaintings {
    public static final DeferredRegister<PaintingVariant> PAINTING_VARIANTS =
            DeferredRegister.create(Registries.PAINTING_VARIANT, GenesisMod.MOD_ID);

    // In 1.21.1 PaintingVariant carries the asset id pointing at
    // assets/<namespace>/textures/painting/<path>.png
    public static final DeferredHolder<PaintingVariant, PaintingVariant> SPACE_0 = PAINTING_VARIANTS.register("space_0",
            () -> new PaintingVariant(4, 4, ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "space_0")));
}
