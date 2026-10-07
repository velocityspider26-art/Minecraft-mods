package com.velocityspider.crashphysics;

import com.mojang.logging.LogUtils;
import com.velocityspider.crashphysics.config.CrashClientConfig;
import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.material.MaterialDataLoader;
import com.velocityspider.crashphysics.network.CameraShakePayload;
import com.velocityspider.crashphysics.sable.CrashTickHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(CrashPhysics.MODID)
public class CrashPhysics {
    public static final String MODID = "crashphysics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrashPhysics(final IEventBus modEventBus, final ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, CrashConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CrashClientConfig.SPEC);

        modEventBus.addListener(CrashPhysics::registerPayloads);

        NeoForge.EVENT_BUS.addListener(CrashPhysics::addReloadListeners);
        CrashTickHandler.register(NeoForge.EVENT_BUS);
    }

    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        // Optional, so vanilla clients and clients without the mod can still join; they just don't get camera shake
        event.registrar("1").optional().playToClient(CameraShakePayload.TYPE, CameraShakePayload.STREAM_CODEC, CameraShakePayload::handle);
    }

    private static void addReloadListeners(final AddReloadListenerEvent event) {
        event.addListener(new MaterialDataLoader());
    }
}
