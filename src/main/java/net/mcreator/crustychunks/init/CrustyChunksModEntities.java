package net.mcreator.crustychunks.init;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.AIStealthBulletEntity;
import net.mcreator.crustychunks.entity.APMediumBulletEntity;
import net.mcreator.crustychunks.entity.AimerBeamEntity;
import net.mcreator.crustychunks.entity.ArtilleryFireProjectileEntity;
import net.mcreator.crustychunks.entity.ArtillerySolidProjectileEntity;
import net.mcreator.crustychunks.entity.ArtilleryWarningMarkerEntity;
import net.mcreator.crustychunks.entity.AssassinEntity;
import net.mcreator.crustychunks.entity.AssassinpodEntity;
import net.mcreator.crustychunks.entity.BirdshotParticleEntity;
import net.mcreator.crustychunks.entity.BlockBusterProjectileEntity;
import net.mcreator.crustychunks.entity.BreacherEntity;
import net.mcreator.crustychunks.entity.BreechingProjectileEntity;
import net.mcreator.crustychunks.entity.BulletfireProjectileEntity;
import net.mcreator.crustychunks.entity.BunkerBusterProjectileEntity;
import net.mcreator.crustychunks.entity.CIWSEntity;
import net.mcreator.crustychunks.entity.CannonMuzzleFlashProducerEntity;
import net.mcreator.crustychunks.entity.ChaffEntity;
import net.mcreator.crustychunks.entity.ClusterBombProjectileEntity;
import net.mcreator.crustychunks.entity.ClusterRocketEntity;
import net.mcreator.crustychunks.entity.CommanderEntity;
import net.mcreator.crustychunks.entity.CommanderPodEntity;
import net.mcreator.crustychunks.entity.DebrisEntity;
import net.mcreator.crustychunks.entity.DecimatorEntity;
import net.mcreator.crustychunks.entity.DrillProjectileEntity;
import net.mcreator.crustychunks.entity.EmberParticleProjectileEntity;
import net.mcreator.crustychunks.entity.EradicatorEntity;
import net.mcreator.crustychunks.entity.EradicatorTurretEntity;
import net.mcreator.crustychunks.entity.ExtraLargeBulletFireEntity;
import net.mcreator.crustychunks.entity.FireArtilleryProjectileEntity;
import net.mcreator.crustychunks.entity.FireBombProjectileEntity;
import net.mcreator.crustychunks.entity.FireClientEffectEntity;
import net.mcreator.crustychunks.entity.FireSpearRocketProjectileEntity;
import net.mcreator.crustychunks.entity.FlameThrowerEmberEntity;
import net.mcreator.crustychunks.entity.FlamerEntity;
import net.mcreator.crustychunks.entity.FlareProjectileEntity;
import net.mcreator.crustychunks.entity.FusionHeatWaveEntity;
import net.mcreator.crustychunks.entity.GasArtilleryProjectileEntity;
import net.mcreator.crustychunks.entity.GasBombProjectileEntity;
import net.mcreator.crustychunks.entity.GenericLargeBulletGreenEntity;
import net.mcreator.crustychunks.entity.GenericlargeBulletEntity;
import net.mcreator.crustychunks.entity.GiantShockExplosionBypassEntity;
import net.mcreator.crustychunks.entity.GlareEffectEntity;
import net.mcreator.crustychunks.entity.GrenadeProjectileEntity;
import net.mcreator.crustychunks.entity.HEATEntity;
import net.mcreator.crustychunks.entity.HVParticleProjectileEntity;
import net.mcreator.crustychunks.entity.HugeAIBulletEntity;
import net.mcreator.crustychunks.entity.HugeBulletFireEntity;
import net.mcreator.crustychunks.entity.HugeFragmentEntity;
import net.mcreator.crustychunks.entity.HugeHEBulletFireEntity;
import net.mcreator.crustychunks.entity.HunterEntity;
import net.mcreator.crustychunks.entity.IRMissileEntity;
import net.mcreator.crustychunks.entity.ImpactGrenadeProjectileEntity;
import net.mcreator.crustychunks.entity.IncendiaryBottleProjectileEntity;
import net.mcreator.crustychunks.entity.IncendiaryGrenadeProjectileEntity;
import net.mcreator.crustychunks.entity.IncindiaryRocketProjectileEntity;
import net.mcreator.crustychunks.entity.JetExhaustProjectileEntity;
import net.mcreator.crustychunks.entity.LargeAPBulletEntity;
import net.mcreator.crustychunks.entity.LargeAPFireEntity;
import net.mcreator.crustychunks.entity.LargeBombProjectileEntity;
import net.mcreator.crustychunks.entity.LargeBulletFireProjectileEntity;
import net.mcreator.crustychunks.entity.LargeFlakProjectileEntity;
import net.mcreator.crustychunks.entity.LargeHEATFireEntity;
import net.mcreator.crustychunks.entity.LargeRadarMissileProjectileEntity;
import net.mcreator.crustychunks.entity.LargeRocketEntity;
import net.mcreator.crustychunks.entity.LargeSmokeFireEntity;
import net.mcreator.crustychunks.entity.LargeSolidProjectileEntity;
import net.mcreator.crustychunks.entity.LargeStealthBulletEntity;
import net.mcreator.crustychunks.entity.LargeTorpedoEntity;
import net.mcreator.crustychunks.entity.MediumAIBulletEntity;
import net.mcreator.crustychunks.entity.MediumBombProjectileEntity;
import net.mcreator.crustychunks.entity.MortarProjectileEntity;
import net.mcreator.crustychunks.entity.MortarerEntity;
import net.mcreator.crustychunks.entity.MuzzleFlashProducerEntity;
import net.mcreator.crustychunks.entity.NuclearBombProjectileEntity;
import net.mcreator.crustychunks.entity.NuclearThermalRadEntity;
import net.mcreator.crustychunks.entity.OrdinanceFusionBombProjectileEntity;
import net.mcreator.crustychunks.entity.ParticleProjectileEntity;
import net.mcreator.crustychunks.entity.PhosphorusParticleEntity;
import net.mcreator.crustychunks.entity.PrototypeEradicatorEntity;
import net.mcreator.crustychunks.entity.RadarSpearMissileProjectileEntity;
import net.mcreator.crustychunks.entity.RadioactiveCloudDetectorEntity;
import net.mcreator.crustychunks.entity.RaidscoutEntity;
import net.mcreator.crustychunks.entity.ReaperEntity;
import net.mcreator.crustychunks.entity.RiflerEntity;
import net.mcreator.crustychunks.entity.RiflerPodEntity;
import net.mcreator.crustychunks.entity.RocketEntity;
import net.mcreator.crustychunks.entity.ScoutEntity;
import net.mcreator.crustychunks.entity.SeatEntityEntity;
import net.mcreator.crustychunks.entity.SeekerSpearMissileProjectileEntity;
import net.mcreator.crustychunks.entity.ShockClientsideBypassEntity;
import net.mcreator.crustychunks.entity.SmallAIBulletEntity;
import net.mcreator.crustychunks.entity.SmallAPCannonFireEntity;
import net.mcreator.crustychunks.entity.SmallBombProjectileEntity;
import net.mcreator.crustychunks.entity.SmallBulletAltEntity;
import net.mcreator.crustychunks.entity.SmallBulletHPEntity;
import net.mcreator.crustychunks.entity.SmallBulletStealthEntity;
import net.mcreator.crustychunks.entity.SmallFlakShellProjectileEntity;
import net.mcreator.crustychunks.entity.SmallMuzzleFlashProducerEntity;
import net.mcreator.crustychunks.entity.SmallShellFireEntity;
import net.mcreator.crustychunks.entity.SmallbulletfireProjectileEntity;
import net.mcreator.crustychunks.entity.SmokeClientBypassEntity;
import net.mcreator.crustychunks.entity.SmokeGrenadeProjectileEntity;
import net.mcreator.crustychunks.entity.SmokeImpactGrenadeEntity;
import net.mcreator.crustychunks.entity.SmokeLauncherProjectileEntity;
import net.mcreator.crustychunks.entity.SmokeMortarProjectileEntity;
import net.mcreator.crustychunks.entity.SmokeStackSmokeEntity;
import net.mcreator.crustychunks.entity.SpaceFusionThermalRadEntityEntity;
import net.mcreator.crustychunks.entity.SpaceThermalRadEntityEntity;
import net.mcreator.crustychunks.entity.SplashEffectClientBypassEntity;
import net.mcreator.crustychunks.entity.StealthMediumBulletEntity;
import net.mcreator.crustychunks.entity.StrikeSpearProjectileEntity;
import net.mcreator.crustychunks.entity.StrikerEntity;
import net.mcreator.crustychunks.entity.SuperLargeBombProjectileEntity;
import net.mcreator.crustychunks.entity.TankFireProjectileEntity;
import net.mcreator.crustychunks.entity.ThermalProjectileEntity;
import net.mcreator.crustychunks.entity.TinyClientEffectEntity;
import net.mcreator.crustychunks.entity.TinyprojectileEntity;
import net.mcreator.crustychunks.entity.TorpedoEntity;
import net.mcreator.crustychunks.entity.ToxicCloudDetectorEntity;
import net.mcreator.crustychunks.entity.VehicleFlareProjectileEntity;
import net.mcreator.crustychunks.entity.WorkerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(
   bus = Bus.MOD
)
public class CrustyChunksModEntities {
   public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, "crusty_chunks");
   public static final DeferredHolder<EntityType<?>, EntityType<ParticleProjectileEntity>> PARTICLE_PROJECTILE = register(
      "particle_projectile",
      Builder.<ParticleProjectileEntity>of(ParticleProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.2F, 0.2F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<EmberParticleProjectileEntity>> EMBER_PARTICLE_PROJECTILE = register(
      "ember_particle_projectile",
      Builder.<EmberParticleProjectileEntity>of(EmberParticleProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<HVParticleProjectileEntity>> HV_PARTICLE_PROJECTILE = register(
      "hv_particle_projectile",
      Builder.<HVParticleProjectileEntity>of(HVParticleProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.2F, 0.2F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmallbulletfireProjectileEntity>> SMALLBULLETFIRE_PROJECTILE = register(
      "smallbulletfire_projectile",
      Builder.<SmallbulletfireProjectileEntity>of(SmallbulletfireProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<BulletfireProjectileEntity>> BULLETFIRE_PROJECTILE = register(
      "bulletfire_projectile",
      Builder.<BulletfireProjectileEntity>of(BulletfireProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeBulletFireProjectileEntity>> LARGE_BULLET_FIRE_PROJECTILE = register(
      "large_bullet_fire_projectile",
      Builder.<LargeBulletFireProjectileEntity>of(LargeBulletFireProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TankFireProjectileEntity>> TANK_FIRE_PROJECTILE = register(
      "tank_fire_projectile",
      Builder.<TankFireProjectileEntity>of(TankFireProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ArtilleryFireProjectileEntity>> ARTILLERY_FIRE_PROJECTILE = register(
      "artillery_fire_projectile",
      Builder.<ArtilleryFireProjectileEntity>of(ArtilleryFireProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RocketEntity>> ROCKET = register(
      "rocket",
      Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.3F, 0.3F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<HEATEntity>> HEAT = register(
      "heat",
      Builder.<HEATEntity>of(HEATEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeHEATFireEntity>> LARGE_HEAT_FIRE = register(
      "large_heat_fire",
      Builder.<LargeHEATFireEntity>of(LargeHEATFireEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeAPFireEntity>> LARGE_AP_FIRE = register(
      "large_ap_fire",
      Builder.<LargeAPFireEntity>of(LargeAPFireEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GrenadeProjectileEntity>> GRENADE_PROJECTILE = register(
      "grenade_projectile",
      Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TinyprojectileEntity>> TINYPROJECTILE = register(
      "tinyprojectile",
      Builder.<TinyprojectileEntity>of(TinyprojectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmokeGrenadeProjectileEntity>> SMOKE_GRENADE_PROJECTILE = register(
      "smoke_grenade_projectile",
      Builder.<SmokeGrenadeProjectileEntity>of(SmokeGrenadeProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<HugeBulletFireEntity>> HUGE_BULLET_FIRE = register(
      "huge_bullet_fire",
      Builder.<HugeBulletFireEntity>of(HugeBulletFireEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ShockClientsideBypassEntity>> SHOCK_CLIENTSIDE_BYPASS = register(
      "shock_clientside_bypass",
      Builder.<ShockClientsideBypassEntity>of(ShockClientsideBypassEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.0F, 0.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeBombProjectileEntity>> LARGE_BOMB_PROJECTILE = register(
      "large_bomb_projectile",
      Builder.<LargeBombProjectileEntity>of(LargeBombProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SuperLargeBombProjectileEntity>> SUPER_LARGE_BOMB_PROJECTILE = register(
      "super_large_bomb_projectile",
      Builder.<SuperLargeBombProjectileEntity>of(SuperLargeBombProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<NuclearBombProjectileEntity>> NUCLEAR_BOMB_PROJECTILE = register(
      "nuclear_bomb_projectile",
      Builder.<NuclearBombProjectileEntity>of(NuclearBombProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<FireBombProjectileEntity>> FIRE_BOMB_PROJECTILE = register(
      "fire_bomb_projectile",
      Builder.<FireBombProjectileEntity>of(FireBombProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MediumBombProjectileEntity>> MEDIUM_BOMB_PROJECTILE = register(
      "medium_bomb_projectile",
      Builder.<MediumBombProjectileEntity>of(MediumBombProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmokeClientBypassEntity>> SMOKE_CLIENT_BYPASS = register(
      "smoke_client_bypass",
      Builder.<SmokeClientBypassEntity>of(SmokeClientBypassEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.0F, 0.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeRocketEntity>> LARGE_ROCKET = register(
      "large_rocket",
      Builder.<LargeRocketEntity>of(LargeRocketEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeFlakProjectileEntity>> LARGE_FLAK_PROJECTILE = register(
      "large_flak_projectile",
      Builder.<LargeFlakProjectileEntity>of(LargeFlakProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmallBombProjectileEntity>> SMALL_BOMB_PROJECTILE = register(
      "small_bomb_projectile",
      Builder.<SmallBombProjectileEntity>of(SmallBombProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<FlameThrowerEmberEntity>> FLAME_THROWER_EMBER = register(
      "flame_thrower_ember",
      Builder.<FlameThrowerEmberEntity>of(FlameThrowerEmberEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TorpedoEntity>> TORPEDO = register(
      "torpedo",
      Builder.<TorpedoEntity>of(TorpedoEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SplashEffectClientBypassEntity>> SPLASH_EFFECT_CLIENT_BYPASS = register(
      "splash_effect_client_bypass",
      Builder.<SplashEffectClientBypassEntity>of(SplashEffectClientBypassEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<IncindiaryRocketProjectileEntity>> INCINDIARY_ROCKET_PROJECTILE = register(
      "incindiary_rocket_projectile",
      Builder.<IncindiaryRocketProjectileEntity>of(IncindiaryRocketProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ArtillerySolidProjectileEntity>> ARTILLERY_SOLID_PROJECTILE = register(
      "artillery_solid_projectile",
      Builder.<ArtillerySolidProjectileEntity>of(ArtillerySolidProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<IncendiaryGrenadeProjectileEntity>> INCENDIARY_GRENADE_PROJECTILE = register(
      "incendiary_grenade_projectile",
      Builder.<IncendiaryGrenadeProjectileEntity>of(IncendiaryGrenadeProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<IncendiaryBottleProjectileEntity>> INCENDIARY_BOTTLE_PROJECTILE = register(
      "incendiary_bottle_projectile",
      Builder.<IncendiaryBottleProjectileEntity>of(IncendiaryBottleProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MortarProjectileEntity>> MORTAR_PROJECTILE = register(
      "mortar_projectile",
      Builder.<MortarProjectileEntity>of(MortarProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmallBulletAltEntity>> SMALL_BULLET_ALT = register(
      "small_bullet_alt",
      Builder.<SmallBulletAltEntity>of(SmallBulletAltEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<DebrisEntity>> DEBRIS = register(
      "debris",
      Builder.<DebrisEntity>of(DebrisEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<PhosphorusParticleEntity>> PHOSPHORUS_PARTICLE = register(
      "phosphorus_particle",
      Builder.<PhosphorusParticleEntity>of(PhosphorusParticleEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GenericlargeBulletEntity>> GENERICLARGE_BULLET = register(
      "genericlarge_bullet",
      Builder.<GenericlargeBulletEntity>of(GenericlargeBulletEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<FireClientEffectEntity>> FIRE_CLIENT_EFFECT = register(
      "fire_client_effect",
      Builder.<FireClientEffectEntity>of(FireClientEffectEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.0F, 0.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmallShellFireEntity>> SMALL_SHELL_FIRE = register(
      "small_shell_fire",
      Builder.<SmallShellFireEntity>of(SmallShellFireEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TinyClientEffectEntity>> TINY_CLIENT_EFFECT = register(
      "tiny_client_effect",
      Builder.<TinyClientEffectEntity>of(TinyClientEffectEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.0F, 0.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<BreechingProjectileEntity>> BREECHING_PROJECTILE = register(
      "breeching_projectile",
      Builder.<BreechingProjectileEntity>of(BreechingProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<HugeFragmentEntity>> HUGE_FRAGMENT = register(
      "huge_fragment",
      Builder.<HugeFragmentEntity>of(HugeFragmentEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.3F, 0.3F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GenericLargeBulletGreenEntity>> GENERIC_LARGE_BULLET_GREEN = register(
      "generic_large_bullet_green",
      Builder.<GenericLargeBulletGreenEntity>of(GenericLargeBulletGreenEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmokeMortarProjectileEntity>> SMOKE_MORTAR_PROJECTILE = register(
      "smoke_mortar_projectile",
      Builder.<SmokeMortarProjectileEntity>of(SmokeMortarProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmokeLauncherProjectileEntity>> SMOKE_LAUNCHER_PROJECTILE = register(
      "smoke_launcher_projectile",
      Builder.<SmokeLauncherProjectileEntity>of(SmokeLauncherProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<DecimatorEntity>> DECIMATOR = register(
      "decimator",
      Builder.<DecimatorEntity>of(DecimatorEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(128)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(2.5F, 2.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<StrikerEntity>> STRIKER = register(
      "striker",
      Builder.<StrikerEntity>of(StrikerEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(32)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(0.5F, 1.7F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmallAIBulletEntity>> SMALL_AI_BULLET = register(
      "small_ai_bullet",
      Builder.<SmallAIBulletEntity>of(SmallAIBulletEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MediumAIBulletEntity>> MEDIUM_AI_BULLET = register(
      "medium_ai_bullet",
      Builder.<MediumAIBulletEntity>of(MediumAIBulletEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<FlamerEntity>> FLAMER = register(
      "flamer",
      Builder.<FlamerEntity>of(FlamerEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(128)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(2.5F, 2.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<HugeAIBulletEntity>> HUGE_AI_BULLET = register(
      "huge_ai_bullet",
      Builder.<HugeAIBulletEntity>of(HugeAIBulletEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.2F, 0.2F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<HunterEntity>> HUNTER = register(
      "hunter",
      Builder.<HunterEntity>of(HunterEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .sized(2.75F, 1.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ClusterRocketEntity>> CLUSTER_ROCKET = register(
      "cluster_rocket",
      Builder.<ClusterRocketEntity>of(ClusterRocketEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<DrillProjectileEntity>> DRILL_PROJECTILE = register(
      "drill_projectile",
      Builder.<DrillProjectileEntity>of(DrillProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeSolidProjectileEntity>> LARGE_SOLID_PROJECTILE = register(
      "large_solid_projectile",
      Builder.<LargeSolidProjectileEntity>of(LargeSolidProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmallBulletHPEntity>> SMALL_BULLET_HP = register(
      "small_bullet_hp",
      Builder.<SmallBulletHPEntity>of(SmallBulletHPEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmallBulletStealthEntity>> SMALL_BULLET_STEALTH = register(
      "small_bullet_stealth",
      Builder.<SmallBulletStealthEntity>of(SmallBulletStealthEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<APMediumBulletEntity>> AP_MEDIUM_BULLET = register(
      "ap_medium_bullet",
      Builder.<APMediumBulletEntity>of(APMediumBulletEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<StealthMediumBulletEntity>> STEALTH_MEDIUM_BULLET = register(
      "stealth_medium_bullet",
      Builder.<StealthMediumBulletEntity>of(StealthMediumBulletEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeAPBulletEntity>> LARGE_AP_BULLET = register(
      "large_ap_bullet",
      Builder.<LargeAPBulletEntity>of(LargeAPBulletEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeStealthBulletEntity>> LARGE_STEALTH_BULLET = register(
      "large_stealth_bullet",
      Builder.<LargeStealthBulletEntity>of(LargeStealthBulletEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<BirdshotParticleEntity>> BIRDSHOT_PARTICLE = register(
      "birdshot_particle",
      Builder.<BirdshotParticleEntity>of(BirdshotParticleEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.05F, 0.05F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RiflerEntity>> RIFLER = register(
      "rifler",
      Builder.<RiflerEntity>of(RiflerEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(256)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(0.5F, 1.7F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<BunkerBusterProjectileEntity>> BUNKER_BUSTER_PROJECTILE = register(
      "bunker_buster_projectile",
      Builder.<BunkerBusterProjectileEntity>of(BunkerBusterProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GiantShockExplosionBypassEntity>> GIANT_SHOCK_EXPLOSION_BYPASS = register(
      "giant_shock_explosion_bypass",
      Builder.<GiantShockExplosionBypassEntity>of(GiantShockExplosionBypassEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<BlockBusterProjectileEntity>> BLOCK_BUSTER_PROJECTILE = register(
      "block_buster_projectile",
      Builder.<BlockBusterProjectileEntity>of(BlockBusterProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<FlareProjectileEntity>> FLARE_PROJECTILE = register(
      "flare_projectile",
      Builder.<FlareProjectileEntity>of(FlareProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<AimerBeamEntity>> AIMER_BEAM = register(
      "aimer_beam",
      Builder.<AimerBeamEntity>of(AimerBeamEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<JetExhaustProjectileEntity>> JET_EXHAUST_PROJECTILE = register(
      "jet_exhaust_projectile",
      Builder.<JetExhaustProjectileEntity>of(JetExhaustProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<NuclearThermalRadEntity>> NUCLEAR_THERMAL_RAD = register(
      "nuclear_thermal_rad",
      Builder.<NuclearThermalRadEntity>of(NuclearThermalRadEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ToxicCloudDetectorEntity>> TOXIC_CLOUD_DETECTOR = register(
      "toxic_cloud_detector",
      Builder.<ToxicCloudDetectorEntity>of(ToxicCloudDetectorEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GasBombProjectileEntity>> GAS_BOMB_PROJECTILE = register(
      "gas_bomb_projectile",
      Builder.<GasBombProjectileEntity>of(GasBombProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GasArtilleryProjectileEntity>> GAS_ARTILLERY_PROJECTILE = register(
      "gas_artillery_projectile",
      Builder.<GasArtilleryProjectileEntity>of(GasArtilleryProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<IRMissileEntity>> IR_MISSILE = register(
      "ir_missile",
      Builder.<IRMissileEntity>of(IRMissileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<VehicleFlareProjectileEntity>> VEHICLE_FLARE_PROJECTILE = register(
      "vehicle_flare_projectile",
      Builder.<VehicleFlareProjectileEntity>of(VehicleFlareProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MortarerEntity>> MORTARER = register(
      "mortarer",
      Builder.<MortarerEntity>of(MortarerEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(3.0F, 2.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<CommanderEntity>> COMMANDER = register(
      "commander",
      Builder.<CommanderEntity>of(CommanderEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(0.6F, 1.8F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ArtilleryWarningMarkerEntity>> ARTILLERY_WARNING_MARKER = register(
      "artillery_warning_marker",
      Builder.<ArtilleryWarningMarkerEntity>of(ArtilleryWarningMarkerEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<CIWSEntity>> CIWS = register(
      "ciws",
      Builder.<CIWSEntity>of(CIWSEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(3.0F, 2.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ClusterBombProjectileEntity>> CLUSTER_BOMB_PROJECTILE = register(
      "cluster_bomb_projectile",
      Builder.<ClusterBombProjectileEntity>of(ClusterBombProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<AssassinEntity>> ASSASSIN = register(
      "assassin",
      Builder.<AssassinEntity>of(AssassinEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(0.5F, 1.7F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<AIStealthBulletEntity>> AI_STEALTH_BULLET = register(
      "ai_stealth_bullet",
      Builder.<AIStealthBulletEntity>of(AIStealthBulletEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<AssassinpodEntity>> ASSASSINPOD = register(
      "assassinpod",
      Builder.<AssassinpodEntity>of(AssassinpodEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<EradicatorEntity>> ERADICATOR = register(
      "eradicator",
      Builder.<EradicatorEntity>of(EradicatorEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(128)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(3.75F, 3.75F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<FusionHeatWaveEntity>> FUSION_HEAT_WAVE = register(
      "fusion_heat_wave",
      Builder.<FusionHeatWaveEntity>of(FusionHeatWaveEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ScoutEntity>> SCOUT = register(
      "scout",
      Builder.<ScoutEntity>of(ScoutEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(32)
         .setUpdateInterval(3)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RaidscoutEntity>> RAIDSCOUT = register(
      "raidscout",
      Builder.<RaidscoutEntity>of(RaidscoutEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(256)
         .setUpdateInterval(3)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<WorkerEntity>> WORKER = register(
      "worker",
      Builder.<WorkerEntity>of(WorkerEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(2.25F, 1.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SeatEntityEntity>> SEAT_ENTITY = register(
      "seat_entity",
      Builder.<SeatEntityEntity>of(SeatEntityEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(0.0F, 0.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<CommanderPodEntity>> COMMANDER_POD = register(
      "commander_pod",
      Builder.<CommanderPodEntity>of(CommanderPodEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RiflerPodEntity>> RIFLER_POD = register(
      "rifler_pod",
      Builder.<RiflerPodEntity>of(RiflerPodEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ExtraLargeBulletFireEntity>> EXTRA_LARGE_BULLET_FIRE = register(
      "extra_large_bullet_fire",
      Builder.<ExtraLargeBulletFireEntity>of(ExtraLargeBulletFireEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmokeStackSmokeEntity>> SMOKE_STACK_SMOKE = register(
      "smoke_stack_smoke",
      Builder.<SmokeStackSmokeEntity>of(SmokeStackSmokeEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ReaperEntity>> REAPER = register(
      "reaper",
      Builder.<ReaperEntity>of(ReaperEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(128)
         .setUpdateInterval(3)
         .sized(7.0F, 1.8F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<OrdinanceFusionBombProjectileEntity>> ORDINANCE_FUSION_BOMB_PROJECTILE = register(
      "ordinance_fusion_bomb_projectile",
      Builder.<OrdinanceFusionBombProjectileEntity>of(OrdinanceFusionBombProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<BreacherEntity>> BREACHER = register(
      "breacher",
      Builder.<BreacherEntity>of(BreacherEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(0.8F, 0.8F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<FireArtilleryProjectileEntity>> FIRE_ARTILLERY_PROJECTILE = register(
      "fire_artillery_projectile",
      Builder.<FireArtilleryProjectileEntity>of(FireArtilleryProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<FireSpearRocketProjectileEntity>> FIRE_SPEAR_ROCKET_PROJECTILE = register(
      "fire_spear_rocket_projectile",
      Builder.<FireSpearRocketProjectileEntity>of(FireSpearRocketProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.3F, 0.3F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SeekerSpearMissileProjectileEntity>> SEEKER_SPEAR_MISSILE_PROJECTILE = register(
      "seeker_spear_missile_projectile",
      Builder.<SeekerSpearMissileProjectileEntity>of(SeekerSpearMissileProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.3F, 0.3F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<StrikeSpearProjectileEntity>> STRIKE_SPEAR_PROJECTILE = register(
      "strike_spear_projectile",
      Builder.<StrikeSpearProjectileEntity>of(StrikeSpearProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.3F, 0.3F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmallFlakShellProjectileEntity>> SMALL_FLAK_SHELL_PROJECTILE = register(
      "small_flak_shell_projectile",
      Builder.<SmallFlakShellProjectileEntity>of(SmallFlakShellProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MuzzleFlashProducerEntity>> MUZZLE_FLASH_PRODUCER = register(
      "muzzle_flash_producer",
      Builder.<MuzzleFlashProducerEntity>of(MuzzleFlashProducerEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.0F, 0.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RadarSpearMissileProjectileEntity>> RADAR_SPEAR_MISSILE_PROJECTILE = register(
      "radar_spear_missile_projectile",
      Builder.<RadarSpearMissileProjectileEntity>of(RadarSpearMissileProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.3F, 0.3F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ChaffEntity>> CHAFF = register(
      "chaff",
      Builder.<ChaffEntity>of(ChaffEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeRadarMissileProjectileEntity>> LARGE_RADAR_MISSILE_PROJECTILE = register(
      "large_radar_missile_projectile",
      Builder.<LargeRadarMissileProjectileEntity>of(LargeRadarMissileProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SpaceThermalRadEntityEntity>> SPACE_THERMAL_RAD_ENTITY = register(
      "space_thermal_rad_entity",
      Builder.<SpaceThermalRadEntityEntity>of(SpaceThermalRadEntityEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ThermalProjectileEntity>> THERMAL_PROJECTILE = register(
      "thermal_projectile",
      Builder.<ThermalProjectileEntity>of(ThermalProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SpaceFusionThermalRadEntityEntity>> SPACE_FUSION_THERMAL_RAD_ENTITY = register(
      "space_fusion_thermal_rad_entity",
      Builder.<SpaceFusionThermalRadEntityEntity>of(SpaceFusionThermalRadEntityEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<EradicatorTurretEntity>> ERADICATOR_TURRET = register(
      "eradicator_turret",
      Builder.<EradicatorTurretEntity>of(EradicatorTurretEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<HugeHEBulletFireEntity>> HUGE_HE_BULLET_FIRE = register(
      "huge_he_bullet_fire",
      Builder.<HugeHEBulletFireEntity>of(HugeHEBulletFireEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<PrototypeEradicatorEntity>> PROTOTYPE_ERADICATOR = register(
      "prototype_eradicator",
      Builder.<PrototypeEradicatorEntity>of(PrototypeEradicatorEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(128)
         .setUpdateInterval(3)
         .fireImmune()
         .sized(2.75F, 2.25F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmallMuzzleFlashProducerEntity>> SMALL_MUZZLE_FLASH_PRODUCER = register(
      "small_muzzle_flash_producer",
      Builder.<SmallMuzzleFlashProducerEntity>of(SmallMuzzleFlashProducerEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.0F, 0.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RadioactiveCloudDetectorEntity>> RADIOACTIVE_CLOUD_DETECTOR = register(
      "radioactive_cloud_detector",
      Builder.<RadioactiveCloudDetectorEntity>of(RadioactiveCloudDetectorEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ImpactGrenadeProjectileEntity>> IMPACT_GRENADE_PROJECTILE = register(
      "impact_grenade_projectile",
      Builder.<ImpactGrenadeProjectileEntity>of(ImpactGrenadeProjectileEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmokeImpactGrenadeEntity>> SMOKE_IMPACT_GRENADE = register(
      "smoke_impact_grenade",
      Builder.<SmokeImpactGrenadeEntity>of(SmokeImpactGrenadeEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeSmokeFireEntity>> LARGE_SMOKE_FIRE = register(
      "large_smoke_fire",
      Builder.<LargeSmokeFireEntity>of(LargeSmokeFireEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LargeTorpedoEntity>> LARGE_TORPEDO = register(
      "large_torpedo",
      Builder.<LargeTorpedoEntity>of(LargeTorpedoEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.5F, 0.5F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<CannonMuzzleFlashProducerEntity>> CANNON_MUZZLE_FLASH_PRODUCER = register(
      "cannon_muzzle_flash_producer",
      Builder.<CannonMuzzleFlashProducerEntity>of(CannonMuzzleFlashProducerEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.0F, 0.0F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SmallAPCannonFireEntity>> SMALL_AP_CANNON_FIRE = register(
      "small_ap_cannon_fire",
      Builder.<SmallAPCannonFireEntity>of(SmallAPCannonFireEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.1F, 0.1F)
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GlareEffectEntity>> GLARE_EFFECT = register(
      "glare_effect",
      Builder.<GlareEffectEntity>of(GlareEffectEntity::new, MobCategory.MISC)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(1)
         .sized(0.0F, 0.0F)
   );

   private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryname, Builder<T> entityTypeBuilder) {
      return REGISTRY.register(registryname, () -> entityTypeBuilder.build(registryname));
   }

   @SubscribeEvent
   public static void registerSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
      {
         DecimatorEntity.init(event);
         StrikerEntity.init(event);
         FlamerEntity.init(event);
         HunterEntity.init(event);
         RiflerEntity.init(event);
         MortarerEntity.init(event);
         CommanderEntity.init(event);
         CIWSEntity.init(event);
         AssassinEntity.init(event);
         EradicatorEntity.init(event);
         ScoutEntity.init(event);
         RaidscoutEntity.init(event);
         WorkerEntity.init(event);
         SeatEntityEntity.init(event);
         ReaperEntity.init(event);
         BreacherEntity.init(event);
         PrototypeEradicatorEntity.init(event);
      }
   }

   @SubscribeEvent
   public static void registerAttributes(EntityAttributeCreationEvent event) {
      event.put((EntityType)DECIMATOR.get(), DecimatorEntity.createAttributes().build());
      event.put((EntityType)STRIKER.get(), StrikerEntity.createAttributes().build());
      event.put((EntityType)FLAMER.get(), FlamerEntity.createAttributes().build());
      event.put((EntityType)HUNTER.get(), HunterEntity.createAttributes().build());
      event.put((EntityType)RIFLER.get(), RiflerEntity.createAttributes().build());
      event.put((EntityType)MORTARER.get(), MortarerEntity.createAttributes().build());
      event.put((EntityType)COMMANDER.get(), CommanderEntity.createAttributes().build());
      event.put((EntityType)CIWS.get(), CIWSEntity.createAttributes().build());
      event.put((EntityType)ASSASSIN.get(), AssassinEntity.createAttributes().build());
      event.put((EntityType)ERADICATOR.get(), EradicatorEntity.createAttributes().build());
      event.put((EntityType)SCOUT.get(), ScoutEntity.createAttributes().build());
      event.put((EntityType)RAIDSCOUT.get(), RaidscoutEntity.createAttributes().build());
      event.put((EntityType)WORKER.get(), WorkerEntity.createAttributes().build());
      event.put((EntityType)SEAT_ENTITY.get(), SeatEntityEntity.createAttributes().build());
      event.put((EntityType)REAPER.get(), ReaperEntity.createAttributes().build());
      event.put((EntityType)BREACHER.get(), BreacherEntity.createAttributes().build());
      event.put((EntityType)PROTOTYPE_ERADICATOR.get(), PrototypeEradicatorEntity.createAttributes().build());
   }
}
