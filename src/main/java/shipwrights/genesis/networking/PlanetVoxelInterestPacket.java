package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.voxel.PlanetVoxelService;

/** Bounded client camera request for server-authoritative viewport voxel streaming. */
public record PlanetVoxelInterestPacket(
        ResourceLocation planet,
        double cameraX,
        double cameraY,
        double cameraZ,
        float lookX,
        float lookY,
        float lookZ,
        int viewportHeight,
        float aspectRatio,
        float verticalFovRadians,
        float targetVoxelPixels,
        int maximumBricks) implements CustomPacketPayload {

    private static final double MAX_CAMERA_COORDINATE = 100_000_000.0;
    public static final Type<PlanetVoxelInterestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet_voxel_interest"));
    public static final StreamCodec<ByteBuf, PlanetVoxelInterestPacket> STREAM_CODEC =
            StreamCodec.of(PlanetVoxelInterestPacket::write, PlanetVoxelInterestPacket::read);

    public PlanetVoxelInterestPacket {
        if (planet == null || !finite(cameraX) || !finite(cameraY) || !finite(cameraZ)
                || Math.abs(cameraX) > MAX_CAMERA_COORDINATE
                || Math.abs(cameraY) > MAX_CAMERA_COORDINATE
                || Math.abs(cameraZ) > MAX_CAMERA_COORDINATE) {
            throw new IllegalArgumentException("invalid planet voxel camera");
        }
        double lookLengthSquared = lookX * lookX + lookY * lookY + lookZ * lookZ;
        if (!Float.isFinite(lookX) || !Float.isFinite(lookY) || !Float.isFinite(lookZ)
                || lookLengthSquared < 0.25 || lookLengthSquared > 4.0) {
            throw new IllegalArgumentException("invalid planet voxel look vector");
        }
        if (viewportHeight < 180 || viewportHeight > 16_384
                || !Float.isFinite(aspectRatio) || aspectRatio < 0.2f || aspectRatio > 8.0f
                || !Float.isFinite(verticalFovRadians)
                || verticalFovRadians < Math.toRadians(20.0)
                || verticalFovRadians > Math.toRadians(150.0)
                || !Float.isFinite(targetVoxelPixels)
                || targetVoxelPixels < 0.25f || targetVoxelPixels > 16.0f
                || maximumBricks < 128 || maximumBricks > 8192) {
            throw new IllegalArgumentException("invalid planet voxel viewport request");
        }
    }

    private static void write(ByteBuf raw, PlanetVoxelInterestPacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(raw);
        ResourceLocation.STREAM_CODEC.encode(buffer, packet.planet);
        raw.writeDouble(packet.cameraX);
        raw.writeDouble(packet.cameraY);
        raw.writeDouble(packet.cameraZ);
        raw.writeFloat(packet.lookX);
        raw.writeFloat(packet.lookY);
        raw.writeFloat(packet.lookZ);
        raw.writeInt(packet.viewportHeight);
        raw.writeFloat(packet.aspectRatio);
        raw.writeFloat(packet.verticalFovRadians);
        raw.writeFloat(packet.targetVoxelPixels);
        raw.writeInt(packet.maximumBricks);
    }

    private static PlanetVoxelInterestPacket read(ByteBuf raw) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(raw);
        return new PlanetVoxelInterestPacket(
                ResourceLocation.STREAM_CODEC.decode(buffer),
                raw.readDouble(), raw.readDouble(), raw.readDouble(),
                raw.readFloat(), raw.readFloat(), raw.readFloat(),
                raw.readInt(), raw.readFloat(), raw.readFloat(), raw.readFloat(), raw.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlanetVoxelInterestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PlanetVoxelService.updateInterest(player, packet);
            }
        });
    }

    private static boolean finite(double value) {
        return Double.isFinite(value);
    }
}
