package com.velocityspider.createjetengines.registry;

import com.velocityspider.createjetengines.CreateJetEngines;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JetSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, CreateJetEngines.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ENGINE_START = register("engine_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENGINE_IDLE = register("engine_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENGINE_POWER = register("engine_power");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENGINE_STOP = register("engine_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> AFTERBURNER_IGNITE = register("afterburner_ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> AFTERBURNER_LOOP = register("afterburner_loop");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(CreateJetEngines.MODID, name)));
    }

    private JetSounds() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
