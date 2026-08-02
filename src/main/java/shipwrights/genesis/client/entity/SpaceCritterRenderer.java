package shipwrights.genesis.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.entity.GenesisEntities;
import shipwrights.genesis.content.entity.SpaceCritterEntity;

/** Renders the crawler, choosing its texture from which world it belongs to. */
public class SpaceCritterRenderer extends MobRenderer<SpaceCritterEntity, SpaceCritterModel> {
    private static final ResourceLocation MOON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "textures/entity/moon_lurker.png");
    private static final ResourceLocation CINDER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "textures/entity/cinder_crawler.png");

    public SpaceCritterRenderer(EntityRendererProvider.Context context) {
        super(context, new SpaceCritterModel(context.bakeLayer(GenesisModelLayers.SPACE_CRITTER)), 0.4f);
    }

    @Override
    public ResourceLocation getTextureLocation(SpaceCritterEntity entity) {
        return entity.getType() == GenesisEntities.CINDER_CRAWLER.get() ? CINDER_TEXTURE : MOON_TEXTURE;
    }
}
