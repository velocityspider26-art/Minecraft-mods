package shipwrights.genesis.client.lod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Resolves real Minecraft block-atlas sprites for volume LOD faces. */
public final class PlanetVolumeTextureCache {
    public record Sprite(TextureAtlasSprite texture, boolean tinted) {
    }

    private static final Map<Integer, EnumMap<Direction, Sprite>> CACHE = new HashMap<>();

    private PlanetVolumeTextureCache() {
    }

    public static Sprite sprite(int stateId, Direction direction) {
        EnumMap<Direction, Sprite> states = CACHE.computeIfAbsent(stateId,
                PlanetVolumeTextureCache::buildStateSprites);
        return states.get(direction);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static EnumMap<Direction, Sprite> buildStateSprites(int stateId) {
        EnumMap<Direction, Sprite> result = new EnumMap<>(Direction.class);
        BlockState state = Block.stateById(stateId);
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        TextureAtlasSprite fallback = model.getParticleIcon();
        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(state, direction, RandomSource.create(31L + direction.ordinal()));
            if (quads.isEmpty()) {
                quads = model.getQuads(state, null, RandomSource.create(73L + direction.ordinal()));
            }
            BakedQuad selected = null;
            for (BakedQuad quad : quads) {
                if (quad.getDirection() == direction) {
                    selected = quad;
                    break;
                }
                if (selected == null) selected = quad;
            }
            result.put(direction, selected == null
                    ? new Sprite(fallback, false)
                    : new Sprite(selected.getSprite(), selected.isTinted()));
        }
        return result;
    }
}
