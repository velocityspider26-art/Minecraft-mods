package shipwrights.genesis.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;

public record WormholeTravelSoundPacket(BlockPos enginePos) implements CustomPacketPayload {
    public static final Type<WormholeTravelSoundPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "wormhole_travel_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WormholeTravelSoundPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, WormholeTravelSoundPacket::enginePos,
                    WormholeTravelSoundPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
