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
 * Client to server: "send me what these chunks of unit (ux, uz) look like", for players whose
 * game cannot run the world generator itself. A unit is {@link #UNIT}² chunks; {@code wanted}
 * has a bit per chunk ({@code dz * UNIT + dx}).
 */
public record DetailRequest(ResourceKey<Level> dimension, int ux, int uz, long wanted) implements CustomPacketPayload {
    /** Chunks per unit, along x and z. */
    public static final int UNIT = 8;

    public static final Type<DetailRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vantage.MODID, "detail_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DetailRequest> CODEC = CustomPacketPayload.codec(DetailRequest::write, DetailRequest::read);

    private static DetailRequest read(RegistryFriendlyByteBuf buf) {
        return new DetailRequest(buf.readResourceKey(Registries.DIMENSION), buf.readVarInt(), buf.readVarInt(), buf.readLong());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceKey(this.dimension);
        buf.writeVarInt(this.ux);
        buf.writeVarInt(this.uz);
        buf.writeLong(this.wanted);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
