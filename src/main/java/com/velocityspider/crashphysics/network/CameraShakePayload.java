package com.velocityspider.crashphysics.network;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.client.CameraShake;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Tells a client to shake the camera because a crash happened nearby.
 *
 * @param strength shake strength at the player's position, 0 to about 1.5
 */
public record CameraShakePayload(float strength) implements CustomPacketPayload {

    public static final Type<CameraShakePayload> TYPE = new Type<>(CrashPhysics.id("camera_shake"));

    public static final StreamCodec<ByteBuf, CameraShakePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, CameraShakePayload::strength, CameraShakePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CameraShakePayload payload, final IPayloadContext context) {
        // Only ever runs on the client, so the client class is only loaded there
        context.enqueueWork(() -> CameraShake.add(payload.strength()));
    }
}
