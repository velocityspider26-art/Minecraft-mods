package net.mcreator.crustychunks.init;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CrustyChunksModParticleTypes {
   public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, "crusty_chunks");
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LARGE_SMOKE = REGISTRY.register("large_smoke", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIREBALL = REGISTRY.register("fireball", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SAND = REGISTRY.register("sand", () -> new SimpleParticleType(false));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST = REGISTRY.register("dust", () -> new SimpleParticleType(false));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WHITE_DUST = REGISTRY.register("white_dust", () -> new SimpleParticleType(false));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMOKE_SCREEN = REGISTRY.register("smoke_screen", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PUFF = REGISTRY.register("puff", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TRACER = REGISTRY.register("tracer", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CAMP_SMOKE = REGISTRY.register("camp_smoke", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMALL_TRACER = REGISTRY.register("small_tracer", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARKS = REGISTRY.register("sparks", () -> new SimpleParticleType(false));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMALL_PUFF = REGISTRY.register("small_puff", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HUGE_SPARKS = REGISTRY.register("huge_sparks", () -> new SimpleParticleType(false));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCK_WAVE = REGISTRY.register("shock_wave", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_SHOCK_PARTICLE = REGISTRY.register(
      "nuclear_shock_particle", () -> new SimpleParticleType(true)
   );
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HUGE_FIREBALL = REGISTRY.register("huge_fireball", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HUGE_SMOKE = REGISTRY.register("huge_smoke", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GROUND_HUGE_SMOKE = REGISTRY.register("ground_huge_smoke", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MEDIUM_TRACER = REGISTRY.register("medium_tracer", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPLASH_PUFF = REGISTRY.register("splash_puff", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMOKE = REGISTRY.register("smoke", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BULLET_TRAIL = REGISTRY.register("bullet_trail", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LARGE_BULLET_TRAIL = REGISTRY.register("large_bullet_trail", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AERIAL_SPARKS = REGISTRY.register("aerial_sparks", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GREEN_TRACER = REGISTRY.register("green_tracer", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WHITE_TRACER = REGISTRY.register("white_tracer", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMALL_GREEN_TRACER = REGISTRY.register("small_green_tracer", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RISING_FLAME = REGISTRY.register("rising_flame", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUN_SMOKE = REGISTRY.register("gun_smoke", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST_WAVE = REGISTRY.register("dust_wave", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLARE = REGISTRY.register("flare", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> JET_FLAME = REGISTRY.register("jet_flame", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DEATH_RAY = REGISTRY.register("death_ray", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HUGE_STATIC_FIREBALL = REGISTRY.register("huge_static_fireball", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GAS_CLOUD = REGISTRY.register("gas_cloud", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROCKET_SMOKE = REGISTRY.register("rocket_smoke", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FUSION_FIREBALL = REGISTRY.register("fusion_fireball", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FUSION_STATIC_FIREBALL = REGISTRY.register(
      "fusion_static_fireball", () -> new SimpleParticleType(true)
   );
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FUSION_SMOKE = REGISTRY.register("fusion_smoke", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPACE_FIREBALL = REGISTRY.register("space_fireball", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLAME_PARTICLE = REGISTRY.register("flame_particle", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROCKET_FLAME = REGISTRY.register("rocket_flame", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMALL_SPLASH_PUFF = REGISTRY.register("small_splash_puff", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RADIOACTIVE_CLOUD = REGISTRY.register("radioactive_cloud", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHOSPHORUS_TRAIL = REGISTRY.register("phosphorus_trail", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCK_RING = REGISTRY.register("shock_ring", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LARGE_SMOKE_SIZE_2 = REGISTRY.register("large_smoke_size_2", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LARGE_SMOKE_SIZE_3 = REGISTRY.register("large_smoke_size_3", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE_EXPLOSION = REGISTRY.register("fire_explosion", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE_EXPLOSION_SIZE_2 = REGISTRY.register("fire_explosion_size_2", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE_EXPLOSION_SIZE_3 = REGISTRY.register("fire_explosion_size_3", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DARK_SMOKE = REGISTRY.register("dark_smoke", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LINGERING_CLOUD = REGISTRY.register("lingering_cloud", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_SHOCK_RING = REGISTRY.register("nuclear_shock_ring", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLACK_POWDER_SMOKE = REGISTRY.register("black_powder_smoke", () -> new SimpleParticleType(true));
}
