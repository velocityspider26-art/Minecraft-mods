package com.velocityspider.createjetengines.registry;

import com.velocityspider.createjetengines.CreateJetEngines;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom exhaust particles.
 *
 * <p>Vanilla particles are the wrong shape for a jet: {@code WHITE_ASH} drifts like snow,
 * {@code CLOUD} is a fat steam puff, and {@code FLAME} rises and stalls almost immediately. These
 * four are shaped for high-velocity exhaust instead — no gravity, no collision, short lifetimes,
 * and velocity carried from the aircraft.
 */
public final class JetParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CreateJetEngines.MODID);

    /** Barely-visible hot air, for the shimmer over a dry nozzle. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EXHAUST_HAZE =
            PARTICLES.register("exhaust_haze", () -> new SimpleParticleType(false));

    /** Startup and rich-throttle soot. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EXHAUST_SOOT =
            PARTICLES.register("exhaust_soot", () -> new SimpleParticleType(false));

    /** Reheat flame; tinted blue at the core and orange further downstream. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> JET_FLAME =
            PARTICLES.register("jet_flame", () -> new SimpleParticleType(false));

    /** The bright periodic rings in a supersonic plume. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCK_DIAMOND =
            PARTICLES.register("shock_diamond", () -> new SimpleParticleType(false));

    private JetParticles() {
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
