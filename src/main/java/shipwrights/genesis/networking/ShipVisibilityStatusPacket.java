package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.teleportation.SpaceTravelManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Reports only the destination craft that the client genuinely did not receive. */
public record ShipVisibilityStatusPacket(UUID syncId, List<UUID> missingShipIds)
        implements CustomPacketPayload {
    private static final int MAX_SHIPS = 64;

    public static final Type<ShipVisibilityStatusPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "ship_visibility_status"));
    public static final StreamCodec<ByteBuf, ShipVisibilityStatusPacket> STREAM_CODEC =
            StreamCodec.of(ShipVisibilityStatusPacket::write, ShipVisibilityStatusPacket::read);

    public ShipVisibilityStatusPacket {
        missingShipIds = List.copyOf(missingShipIds);
        if (missingShipIds.size() > MAX_SHIPS) {
            throw new IllegalArgumentException("Too many missing ships: " + missingShipIds.size());
        }
    }

    private static void write(ByteBuf buffer, ShipVisibilityStatusPacket packet) {
        writeUuid(buffer, packet.syncId());
        ByteBufCodecs.VAR_INT.encode(buffer, packet.missingShipIds().size());
        for (UUID shipId : packet.missingShipIds()) {
            writeUuid(buffer, shipId);
        }
    }

    private static ShipVisibilityStatusPacket read(ByteBuf buffer) {
        UUID syncId = readUuid(buffer);
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        if (count < 0 || count > MAX_SHIPS) {
            throw new IllegalArgumentException("Implausible missing-ship count " + count);
        }
        List<UUID> missingShipIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            missingShipIds.add(readUuid(buffer));
        }
        return new ShipVisibilityStatusPacket(syncId, missingShipIds);
    }

    private static void writeUuid(ByteBuf buffer, UUID id) {
        buffer.writeLong(id.getMostSignificantBits());
        buffer.writeLong(id.getLeastSignificantBits());
    }

    private static UUID readUuid(ByteBuf buffer) {
        return new UUID(buffer.readLong(), buffer.readLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShipVisibilityStatusPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpaceTravelManager.onClientVisibilityStatus(
                        player, packet.syncId(), packet.missingShipIds());
            }
        });
    }
}
