package shipwrights.genesis.content.painting;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import shipwrights.genesis.GenesisMod;

public class GenesisPaintings {
    public static final DeferredRegister<PaintingVariant> PAINTING_VARIANTS =
            DeferredRegister.create(Registries.PAINTING_VARIANT, GenesisMod.MOD_ID);

    public static final DeferredHolder<PaintingVariant, PaintingVariant> SPACE_0 = PAINTING_VARIANTS.register("space_0",
            () -> new PaintingVariant(64, 64, ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "space_0")));
}
