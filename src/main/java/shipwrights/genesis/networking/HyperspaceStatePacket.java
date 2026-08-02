package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.hyperspace.HyperspaceTunnelRenderer;

public record HyperspaceStatePacket(int phase, int ticks, float axisX, float axisY, float axisZ)
        implements CustomPacketPayload {
    public static final int PHASE_CANCEL = 0;
    public static final int PHASE_PRE_JUMP = 1;
    public static final int PHASE_POST_TUNNEL = 2;
    public static final int PHASE_ENTER_FLASH = 3;
    /** Plain dimension transitions: a gentle white fade with NO warp streaks. */
    public static final int PHASE_SOFT_FADE = 4;
    /**
     * Retired. Once told the client that the upcoming "Downloading terrain"
     * screen was ours to reskin; the transition screen is now claimed per
     * dimension through NeoForge's own registry (see
     * {@code TransitionScreenRegistration}), which knows which crossing is
     * ours without being told and cannot be raced by a packet arriving after
     * the screen has already opened. Kept as a no-op so an older client on a
     * newer server does not fall through to {@code default} and cancel a
     * transit it is in the middle of.
     */
    public static final int PHASE_LOADING_HINT = 5;

    public static final Type<HyperspaceStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "hyperspace_state"));

    public static final StreamCodec<ByteBuf, HyperspaceStatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, HyperspaceStatePacket::phase,
                    ByteBufCodecs.VAR_INT, HyperspaceStatePacket::ticks,
                    ByteBufCodecs.FLOAT, HyperspaceStatePacket::axisX,
                    ByteBufCodecs.FLOAT, HyperspaceStatePacket::axisY,
                    ByteBufCodecs.FLOAT, HyperspaceStatePacket::axisZ,
                    HyperspaceStatePacket::new
            );

    public static HyperspaceStatePacket preJump(int ticks) {
        return withoutAxis(PHASE_PRE_JUMP, ticks);
    }

    /** Gentle fade for ordinary dimension transitions — no warp streaks. */
    public static HyperspaceStatePacket softFade(int ticks) {
        return withoutAxis(PHASE_SOFT_FADE, ticks);
    }

    public static HyperspaceStatePacket loadingHint() {
        return withoutAxis(PHASE_LOADING_HINT, 0);
    }

    public static HyperspaceStatePacket postTunnel(int ticks) {
        return withoutAxis(PHASE_POST_TUNNEL, ticks);
    }

    public static HyperspaceStatePacket enterFlash(int ticks) {
        return withoutAxis(PHASE_ENTER_FLASH, ticks);
    }

    public static HyperspaceStatePacket enterFlash(int ticks, Vec3 tunnelAxis) {
        Vec3 axis = tunnelAxis == null ? Vec3.ZERO : tunnelAxis.normalize();
        return new HyperspaceStatePacket(PHASE_ENTER_FLASH, ticks,
                (float) axis.x, (float) axis.y, (float) axis.z);
    }

    public static HyperspaceStatePacket cancel(int ticks) {
        return withoutAxis(PHASE_CANCEL, ticks);
    }

    private static HyperspaceStatePacket withoutAxis(int phase, int ticks) {
        return new HyperspaceStatePacket(phase, ticks, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HyperspaceStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            switch (packet.phase()) {
                case PHASE_PRE_JUMP -> HyperspaceTunnelRenderer.startPreJump(packet.ticks());
                case PHASE_ENTER_FLASH -> {
                    boolean hasAxis = packet.axisX() * packet.axisX()
                            + packet.axisY() * packet.axisY()
                            + packet.axisZ() * packet.axisZ() > 1.0E-5f;
                    if (hasAxis) {
                        HyperspaceTunnelRenderer.setTunnelAxis(packet.axisX(), packet.axisY(), packet.axisZ());
                    }
                    HyperspaceTunnelRenderer.startFlash(packet.ticks(), !hasAxis, true);
                }
                case PHASE_POST_TUNNEL -> HyperspaceTunnelRenderer.startFlash(packet.ticks(), false, true);
                case PHASE_SOFT_FADE -> HyperspaceTunnelRenderer.startFlash(packet.ticks(), false, false);
                case PHASE_LOADING_HINT -> {
                    // Nothing to do; the screen is claimed by dimension now.
                }
                default -> HyperspaceTunnelRenderer.cancelTransit(packet.ticks());
            }
        });
    }
}
