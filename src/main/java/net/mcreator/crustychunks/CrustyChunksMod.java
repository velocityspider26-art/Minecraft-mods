package net.mcreator.crustychunks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.mcreator.crustychunks.init.CrustyChunksModBlockEntities;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModFluidTypes;
import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.init.CrustyChunksModMenus;
import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.mcreator.crustychunks.init.CrustyChunksModSounds;
import net.mcreator.crustychunks.init.CrustyChunksModTabs;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Tuple;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("crusty_chunks")
public class CrustyChunksMod {
   public static final Logger LOGGER = LogManager.getLogger(CrustyChunksMod.class);
   public static final String MODID = "crusty_chunks";

   public CrustyChunksMod(IEventBus modEventBus, ModContainer container) {
      container.registerConfig(ModConfig.Type.COMMON, WariumConfig.SPEC);
      NeoForge.EVENT_BUS.register(this);
      modEventBus.addListener(this::registerNetworking);
      CrustyChunksModSounds.REGISTRY.register(modEventBus);
      CrustyChunksModBlocks.REGISTRY.register(modEventBus);
      CrustyChunksModBlockEntities.REGISTRY.register(modEventBus);
      CrustyChunksModItems.REGISTRY.register(modEventBus);
      CrustyChunksModEntities.REGISTRY.register(modEventBus);
      CrustyChunksModTabs.REGISTRY.register(modEventBus);
      CrustyChunksModMobEffects.REGISTRY.register(modEventBus);
      CrustyChunksModParticleTypes.REGISTRY.register(modEventBus);
      CrustyChunksModMenus.REGISTRY.register(modEventBus);
      CrustyChunksModFluids.REGISTRY.register(modEventBus);
      CrustyChunksModFluidTypes.REGISTRY.register(modEventBus);
      CrustyChunksModVariables.ATTACHMENT_TYPES.register(modEventBus);
   }

   private static boolean networkingRegistered = false;
   private static final Map<CustomPacketPayload.Type<?>, NetworkMessage<?>> MESSAGES = new HashMap<>();

   private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
   }

   public static <T extends CustomPacketPayload> void addNetworkMessage(CustomPacketPayload.Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
      if (networkingRegistered)
         throw new IllegalStateException("Cannot register new network messages after networking has been registered");
      MESSAGES.put(id, new NetworkMessage<>(reader, handler));
   }

   /** 1.3.0: broadcast a payload to every connected player (client VFX/sound cues). */
   public static void sendToAll(CustomPacketPayload message) {
      if (net.neoforged.fml.util.thread.SidedThreadGroups.SERVER == Thread.currentThread().getThreadGroup())
         net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(message);
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private void registerNetworking(final RegisterPayloadHandlersEvent event) {
      final PayloadRegistrar registrar = event.registrar(MODID);
      MESSAGES.forEach((id, networkMessage) -> registrar.playBidirectional(id, ((NetworkMessage) networkMessage).reader(), ((NetworkMessage) networkMessage).handler()));
      networkingRegistered = true;
   }

   private static final Collection<Tuple<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

   public static void queueServerWork(int tick, Runnable action) {
      if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
         workQueue.add(new Tuple<>(action, tick));
   }

   @SubscribeEvent
   public void tick(ServerTickEvent.Post event) {
      List<Tuple<Runnable, Integer>> actions = new ArrayList<>();
      workQueue.forEach(work -> {
         work.setB(work.getB() - 1);
         if (work.getB() == 0)
            actions.add(work);
      });
      actions.forEach(e -> e.getA().run());
      workQueue.removeAll(actions);
   }
}
