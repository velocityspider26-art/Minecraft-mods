package net.mcreator.crustychunks.network;

import com.google.gson.JsonArray;
import java.util.function.Supplier;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CrustyChunksModVariables {
   public static JsonArray recipesloaded = new JsonArray();

   public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CrustyChunksMod.MODID);
   public static final Supplier<AttachmentType<PlayerVariables>> PLAYER_VARIABLES = ATTACHMENT_TYPES.register("player_variables", () -> AttachmentType.serializable(() -> new PlayerVariables()).build());

   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
      CrustyChunksMod.addNetworkMessage(SavedDataSyncMessage.TYPE, SavedDataSyncMessage.STREAM_CODEC, SavedDataSyncMessage::handleData);
      CrustyChunksMod.addNetworkMessage(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.STREAM_CODEC, PlayerVariablesSyncMessage::handleData);
   }

   @EventBusSubscriber
   public static class EventBusVariableHandlers {
      @SubscribeEvent
      public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
         if (event.getEntity() instanceof ServerPlayer player)
            player.getData(PLAYER_VARIABLES).syncPlayerVariables(event.getEntity());
      }

      @SubscribeEvent
      public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
         if (event.getEntity() instanceof ServerPlayer player)
            player.getData(PLAYER_VARIABLES).syncPlayerVariables(event.getEntity());
      }

      @SubscribeEvent
      public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
         if (event.getEntity() instanceof ServerPlayer player)
            player.getData(PLAYER_VARIABLES).syncPlayerVariables(event.getEntity());
      }

      @SubscribeEvent
      public static void clonePlayer(PlayerEvent.Clone event) {
         PlayerVariables original = event.getOriginal().getData(PLAYER_VARIABLES);
         PlayerVariables clone = new PlayerVariables();
         clone.eastereggcooldown = original.eastereggcooldown;
         if (!event.isWasDeath()) {
            clone.Inflash = original.Inflash;
            clone.UpKey = original.UpKey;
            clone.DownKey = original.DownKey;
            clone.LeftKey = original.LeftKey;
            clone.RightKey = original.RightKey;
            clone.clickrelease = original.clickrelease;
            clone.AimDownSights = original.AimDownSights;
         }
         event.getEntity().setData(PLAYER_VARIABLES, clone);
      }

      @SubscribeEvent
      public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
         if (event.getEntity() instanceof ServerPlayer player) {
            SavedData mapdata = MapVariables.get(event.getEntity().level());
            SavedData worlddata = WorldVariables.get(event.getEntity().level());
            if (mapdata != null)
               PacketDistributor.sendToPlayer(player, new SavedDataSyncMessage(0, mapdata));
            if (worlddata != null)
               PacketDistributor.sendToPlayer(player, new SavedDataSyncMessage(1, worlddata));
         }
      }

      @SubscribeEvent
      public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
         if (event.getEntity() instanceof ServerPlayer player) {
            SavedData worlddata = WorldVariables.get(event.getEntity().level());
            if (worlddata != null)
               PacketDistributor.sendToPlayer(player, new SavedDataSyncMessage(1, worlddata));
         }
      }
   }

   public static class WorldVariables extends SavedData {
      public static final String DATA_NAME = "crusty_chunks_worldvars";

      public static WorldVariables load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
         WorldVariables data = new WorldVariables();
         data.read(tag, lookupProvider);
         return data;
      }

      public void read(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
      }

      @Override
      public CompoundTag save(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
         return nbt;
      }

      public void syncData(LevelAccessor world) {
         this.setDirty();
         if (world instanceof ServerLevel level)
            PacketDistributor.sendToPlayersInDimension(level, new SavedDataSyncMessage(1, this));
      }

      static WorldVariables clientSide = new WorldVariables();

      public static WorldVariables get(LevelAccessor world) {
         if (world instanceof ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldVariables::new, WorldVariables::load), DATA_NAME);
         } else {
            return clientSide;
         }
      }
   }

   public static class MapVariables extends SavedData {
      public static final String DATA_NAME = "crusty_chunks_mapvars";
      public double ApocalypseStrikers = 20.0;
      public double ApocalypseRiflers = 20.0;
      public double ApocalypseCommanders = 8.0;
      public double ApocalypseHunters = 5.0;
      public double ApocalypseDecimators = 4.0;
      public double ApocalypseEradicators = 1.0;
      public double ApocalypseArtillery = 4.0;
      public double Production = 0.0;
      public double ApocalypseWorkers = 5.0;
      public double motivation = 0.0;
      public double ApocalypseMultiplier = 1.0;
      public double ApocalypseReapers = 4.0;
      public double ApocalypseBreachers = 6.0;

      public static MapVariables load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
         MapVariables data = new MapVariables();
         data.read(tag, lookupProvider);
         return data;
      }

      public void read(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
         this.ApocalypseStrikers = nbt.getDouble("ApocalypseStrikers");
         this.ApocalypseRiflers = nbt.getDouble("ApocalypseRiflers");
         this.ApocalypseCommanders = nbt.getDouble("ApocalypseCommanders");
         this.ApocalypseHunters = nbt.getDouble("ApocalypseHunters");
         this.ApocalypseDecimators = nbt.getDouble("ApocalypseDecimators");
         this.ApocalypseEradicators = nbt.getDouble("ApocalypseEradicators");
         this.ApocalypseArtillery = nbt.getDouble("ApocalypseArtillery");
         this.Production = nbt.getDouble("Production");
         this.ApocalypseWorkers = nbt.getDouble("ApocalypseWorkers");
         this.motivation = nbt.getDouble("motivation");
         this.ApocalypseMultiplier = nbt.getDouble("ApocalypseMultiplier");
         this.ApocalypseReapers = nbt.getDouble("ApocalypseReapers");
         this.ApocalypseBreachers = nbt.getDouble("ApocalypseBreachers");
      }

      @Override
      public CompoundTag save(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
         nbt.putDouble("ApocalypseStrikers", this.ApocalypseStrikers);
         nbt.putDouble("ApocalypseRiflers", this.ApocalypseRiflers);
         nbt.putDouble("ApocalypseCommanders", this.ApocalypseCommanders);
         nbt.putDouble("ApocalypseHunters", this.ApocalypseHunters);
         nbt.putDouble("ApocalypseDecimators", this.ApocalypseDecimators);
         nbt.putDouble("ApocalypseEradicators", this.ApocalypseEradicators);
         nbt.putDouble("ApocalypseArtillery", this.ApocalypseArtillery);
         nbt.putDouble("Production", this.Production);
         nbt.putDouble("ApocalypseWorkers", this.ApocalypseWorkers);
         nbt.putDouble("motivation", this.motivation);
         nbt.putDouble("ApocalypseMultiplier", this.ApocalypseMultiplier);
         nbt.putDouble("ApocalypseReapers", this.ApocalypseReapers);
         nbt.putDouble("ApocalypseBreachers", this.ApocalypseBreachers);
         return nbt;
      }

      public void syncData(LevelAccessor world) {
         this.setDirty();
         if (world instanceof Level && !world.isClientSide())
            PacketDistributor.sendToAllPlayers(new SavedDataSyncMessage(0, this));
      }

      static MapVariables clientSide = new MapVariables();

      public static MapVariables get(LevelAccessor world) {
         if (world instanceof ServerLevelAccessor serverLevelAcc) {
            return serverLevelAcc.getLevel().getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(new SavedData.Factory<>(MapVariables::new, MapVariables::load), DATA_NAME);
         } else {
            return clientSide;
         }
      }
   }

   public record SavedDataSyncMessage(int dataType, SavedData data) implements CustomPacketPayload {
      public static final Type<SavedDataSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrustyChunksMod.MODID, "saved_data_sync"));
      public static final StreamCodec<RegistryFriendlyByteBuf, SavedDataSyncMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, SavedDataSyncMessage message) -> {
         buffer.writeInt(message.dataType);
         if (message.data != null)
            buffer.writeNbt(message.data.save(new CompoundTag(), buffer.registryAccess()));
      }, (RegistryFriendlyByteBuf buffer) -> {
         int dataType = buffer.readInt();
         CompoundTag nbt = buffer.readNbt();
         SavedData data = null;
         if (nbt != null) {
            data = dataType == 0 ? new MapVariables() : new WorldVariables();
            if (data instanceof MapVariables mapVariables)
               mapVariables.read(nbt, buffer.registryAccess());
            else if (data instanceof WorldVariables worldVariables)
               worldVariables.read(nbt, buffer.registryAccess());
         }
         return new SavedDataSyncMessage(dataType, data);
      });

      @Override
      public Type<SavedDataSyncMessage> type() {
         return TYPE;
      }

      public static void handleData(final SavedDataSyncMessage message, final IPayloadContext context) {
         if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
            context.enqueueWork(() -> {
               if (message.dataType == 0)
                  MapVariables.clientSide.read(message.data.save(new CompoundTag(), context.player().registryAccess()), context.player().registryAccess());
               else
                  WorldVariables.clientSide.read(message.data.save(new CompoundTag(), context.player().registryAccess()), context.player().registryAccess());
            }).exceptionally(e -> {
               context.connection().disconnect(Component.literal(e.getMessage()));
               return null;
            });
         }
      }
   }

   public static class PlayerVariables implements INBTSerializable<CompoundTag> {
      public boolean Inflash = false;
      public boolean UpKey = false;
      public boolean DownKey = false;
      public boolean LeftKey = false;
      public boolean RightKey = false;
      public boolean clickrelease = true;
      public double eastereggcooldown = 0.0;
      public boolean AimDownSights = false;

      @Override
      public CompoundTag serializeNBT(HolderLookup.Provider lookupProvider) {
         CompoundTag nbt = new CompoundTag();
         nbt.putBoolean("Inflash", this.Inflash);
         nbt.putBoolean("UpKey", this.UpKey);
         nbt.putBoolean("DownKey", this.DownKey);
         nbt.putBoolean("LeftKey", this.LeftKey);
         nbt.putBoolean("RightKey", this.RightKey);
         nbt.putBoolean("clickrelease", this.clickrelease);
         nbt.putDouble("eastereggcooldown", this.eastereggcooldown);
         nbt.putBoolean("AimDownSights", this.AimDownSights);
         return nbt;
      }

      @Override
      public void deserializeNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
         this.Inflash = nbt.getBoolean("Inflash");
         this.UpKey = nbt.getBoolean("UpKey");
         this.DownKey = nbt.getBoolean("DownKey");
         this.LeftKey = nbt.getBoolean("LeftKey");
         this.RightKey = nbt.getBoolean("RightKey");
         this.clickrelease = nbt.getBoolean("clickrelease");
         this.eastereggcooldown = nbt.getDouble("eastereggcooldown");
         this.AimDownSights = nbt.getBoolean("AimDownSights");
      }

      public void syncPlayerVariables(Entity entity) {
         if (entity instanceof ServerPlayer serverPlayer)
            PacketDistributor.sendToPlayer(serverPlayer, new PlayerVariablesSyncMessage(this));
      }
   }

   public record PlayerVariablesSyncMessage(PlayerVariables data) implements CustomPacketPayload {
      public static final Type<PlayerVariablesSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrustyChunksMod.MODID, "player_variables_sync"));
      public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVariablesSyncMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, PlayerVariablesSyncMessage message) -> buffer.writeNbt(message.data().serializeNBT(buffer.registryAccess())), (RegistryFriendlyByteBuf buffer) -> {
         PlayerVariablesSyncMessage message = new PlayerVariablesSyncMessage(new PlayerVariables());
         message.data.deserializeNBT(buffer.registryAccess(), buffer.readNbt());
         return message;
      });

      @Override
      public Type<PlayerVariablesSyncMessage> type() {
         return TYPE;
      }

      public static void handleData(final PlayerVariablesSyncMessage message, final IPayloadContext context) {
         if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
            context.enqueueWork(() -> context.player().getData(PLAYER_VARIABLES).deserializeNBT(context.player().registryAccess(), message.data.serializeNBT(context.player().registryAccess()))).exceptionally(e -> {
               context.connection().disconnect(Component.literal(e.getMessage()));
               return null;
            });
         }
      }
   }
}
