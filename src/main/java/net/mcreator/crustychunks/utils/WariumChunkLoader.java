package net.mcreator.crustychunks.utils;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class WariumChunkLoader {
	public static final TicketController TICKET_CONTROLLER = new TicketController(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "chunk_loader"));

	private WariumChunkLoader() {
	}

	@SubscribeEvent
	public static void registerTicketControllers(RegisterTicketControllersEvent event) {
		event.register(TICKET_CONTROLLER);
	}
}
