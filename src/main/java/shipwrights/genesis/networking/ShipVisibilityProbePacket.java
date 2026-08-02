package shipwrights.genesis.networking;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ordered behind Sable's natural destination packets. When this reaches the
 * client, it can report exactly which arriving craft are still absent instead
 * of forcing every craft to be sent a second time.
 */
public record ShipVisibilityProbePacket(UUID syncId, List<UUID> shipIds)
        implements CustomPacketPayload {
    private static final int MAX_SHIPS = 64;

    public static final Type<ShipVisibilityProbePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "ship_visibility_probe"));
    public static final StreamCodec<ByteBuf, ShipVisibilityProbePacket> STREAM_CODEC =
            StreamCodec.of(ShipVisibilityProbePacket::write, ShipVisibilityProbePacket::read);

    public ShipVisibilityProbePacket {
        shipIds = List.copyOf(shipIds);
        if (shipIds.size() > MAX_SHIPS) {
            throw new IllegalArgumentException("Too many ships in visibility probe: " + shipIds.size());
        }
    }

    private static void write(ByteBuf buffer, ShipVisibilityProbePacket packet) {
        writeUuid(buffer, packet.syncId());
        ByteBufCodecs.VAR_INT.encode(buffer, packet.shipIds().size());
        for (UUID shipId : packet.shipIds()) {
            writeUuid(buffer, shipId);
        }
    }

    private static ShipVisibilityProbePacket read(ByteBuf buffer) {
        UUID syncId = readUuid(buffer);
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        if (count < 0 || count > MAX_SHIPS) {
            throw new IllegalArgumentException("Implausible visibility probe size " + count);
        }
        List<UUID> shipIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            shipIds.add(readUuid(buffer));
        }
        return new ShipVisibilityProbePacket(syncId, shipIds);
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

    public static void handle(ShipVisibilityProbePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            ClientSubLevelContainer container = minecraft.level == null
                    ? null : SubLevelContainer.getContainer(minecraft.level);
            List<UUID> missing = new ArrayList<>();
            for (UUID shipId : packet.shipIds()) {
                if (container == null || container.getSubLevel(shipId) == null) {
                    missing.add(shipId);
                }
            }
            if (minecraft.getConnection() != null) {
                PacketDistributor.sendToServer(
                        new ShipVisibilityStatusPacket(packet.syncId(), missing));
            }
        });
    }
}
