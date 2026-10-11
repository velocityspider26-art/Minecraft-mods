package com.velocityspider.crashphysics.network;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.client.CrashFxClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A tick's worth of crash effects for one player: splashes, wakes, sparks, dust, smoke and sounds. Clients without the
 * mod get plain particles and sounds from the server instead.
 */
public record CrashFxPayload(List<Fx> effects) implements CustomPacketPayload {

    public static final int SPLASH = 1;
    public static final int WAKE = 2;
    public static final int SPARKS = 3;
    public static final int DUST_RING = 4;
    public static final int SMOKE = 5;
    public static final int SOUND = 6;
    public static final int BUBBLES = 7;
    public static final int DEBRIS_DUST = 8;

    /**
     * One effect.
     *
     * @param kind      what it is
     * @param magnitude how big: log10 of the energy in kJ for impacts, the volume for sounds, the width for wakes
     * @param vx        a velocity [m/s] for wakes, sparks and splashes; the pitch for sounds; the radius of the vehicle for
     *                  dust rings; how long smoke lasts [ticks] (with its radius in vy)
     * @param data      a block state id for dust, a sound event id for sounds
     */
    public record Fx(int kind, double x, double y, double z, float magnitude, float vx, float vy, float vz, int data) {
    }

    public static final Type<CrashFxPayload> TYPE = new Type<>(CrashPhysics.id("crash_fx"));

    public static final StreamCodec<ByteBuf, CrashFxPayload> STREAM_CODEC = StreamCodec.of(CrashFxPayload::write, CrashFxPayload::read);

    private static void write(final ByteBuf buffer, final CrashFxPayload payload) {
        final List<Fx> effects = payload.effects();
        final int count = Math.min(effects.size(), 4096);
        buffer.writeShort(count);
        for (int i = 0; i < count; i++) {
            final Fx fx = effects.get(i);
            buffer.writeByte(fx.kind());
            buffer.writeDouble(fx.x());
            buffer.writeDouble(fx.y());
            buffer.writeDouble(fx.z());
            buffer.writeFloat(fx.magnitude());
            buffer.writeFloat(fx.vx());
            buffer.writeFloat(fx.vy());
            buffer.writeFloat(fx.vz());
            buffer.writeInt(fx.data());
        }
    }

    private static CrashFxPayload read(final ByteBuf buffer) {
        final int count = buffer.readUnsignedShort();
        final List<Fx> effects = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            effects.add(new Fx(buffer.readByte(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readInt()));
        }
        return new CrashFxPayload(effects);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CrashFxPayload payload, final IPayloadContext context) {
        // Only ever runs on the client, so the client class is only loaded there
        context.enqueueWork(() -> CrashFxClient.play(payload.effects()));
    }
}
