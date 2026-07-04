package shipwrights.genesis.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;

public record StopVoidEngineStartSoundPacket() implements CustomPacketPayload {
    public static final StopVoidEngineStartSoundPacket INSTANCE = new StopVoidEngineStartSoundPacket();

    public static final Type<StopVoidEngineStartSoundPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "stop_void_engine_start_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StopVoidEngineStartSoundPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
