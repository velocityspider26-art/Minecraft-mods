package shipwrights.genesis.content.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import shipwrights.genesis.GenesisMod;

public class GenesisParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, GenesisMod.MOD_ID);


    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ZAP_BUBBLE_PARTICLES =
            PARTICLE_TYPES.register("zap_bubble_particles", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VERDITE_PARTICLES =
            PARTICLE_TYPES.register("verdite_particles", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus){
        PARTICLE_TYPES.register(eventBus);
    }
}
