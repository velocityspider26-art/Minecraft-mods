package shipwrights.genesis.client.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;

/** Model layer locations for Genesis entities. */
public final class GenesisModelLayers {
    public static final ModelLayerLocation SPACE_CRITTER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "space_critter"), "main");

    private GenesisModelLayers() {
    }
}
