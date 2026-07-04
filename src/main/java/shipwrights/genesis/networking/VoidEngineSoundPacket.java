package shipwrights.genesis.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;

public record VoidEngineSoundPacket(BlockPos enginePos) implements CustomPacketPayload {
    public static final Type<VoidEngineSoundPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "void_engine_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoidEngineSoundPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, VoidEngineSoundPacket::enginePos,
                    VoidEngineSoundPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
