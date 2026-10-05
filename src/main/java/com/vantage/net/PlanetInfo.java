package com.vantage.net;

import com.vantage.Vantage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Server to client, on joining and on every dimension change: whether the server answers
 * {@link TerrainRequest}s for this dimension, the planet radius if it is a Vantage planet (0
 * otherwise) so the distant terrain curves like the world really does, and the sea level and
 * base rock (block state id) of its generator.
 */
public record PlanetInfo(ResourceKey<Level> dimension, int radius, boolean servesTerrain, int seaLevel, int stone)
        implements CustomPacketPayload {
    public static final Type<PlanetInfo> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vantage.MODID, "planet_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlanetInfo> CODEC = CustomPacketPayload.codec(PlanetInfo::write, PlanetInfo::read);

    private static PlanetInfo read(RegistryFriendlyByteBuf buf) {
        return new PlanetInfo(buf.readResourceKey(Registries.DIMENSION), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceKey(this.dimension);
        buf.writeVarInt(this.radius);
        buf.writeBoolean(this.servesTerrain);
        buf.writeVarInt(this.seaLevel);
        buf.writeVarInt(this.stone);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
