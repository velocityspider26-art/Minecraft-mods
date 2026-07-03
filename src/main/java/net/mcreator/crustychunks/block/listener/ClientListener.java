package net.mcreator.crustychunks.block.listener;

import net.mcreator.crustychunks.block.renderer.MiniGunBarrelTileRenderer;
import net.mcreator.crustychunks.block.renderer.RACBarrelTileRenderer;
import net.mcreator.crustychunks.init.CrustyChunksModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "crusty_chunks", bus = Bus.MOD, value = {Dist.CLIENT})
public class ClientListener {
   @SubscribeEvent
   public static void registerRenderers(RegisterRenderers event) {
      event.registerBlockEntityRenderer((BlockEntityType)CrustyChunksModBlockEntities.RAC_BARREL.get(), context -> new RACBarrelTileRenderer());
      event.registerBlockEntityRenderer((BlockEntityType)CrustyChunksModBlockEntities.MINI_GUN_BARREL.get(), context -> new MiniGunBarrelTileRenderer());
   }
}
