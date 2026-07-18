package net.mcreator.crustychunks.init;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.block.display.MiniGunBarrelDisplayItem;
import net.mcreator.crustychunks.block.display.RACBarrelDisplayItem;
import net.mcreator.crustychunks.item.APFSDSProjectileItem;
import net.mcreator.crustychunks.item.APLargeBulletItem;
import net.mcreator.crustychunks.item.APShellItem;
import net.mcreator.crustychunks.item.AdvancedAlloyComponentItem;
import net.mcreator.crustychunks.item.AdvancedAlloyIngotItem;
import net.mcreator.crustychunks.item.AdvancedAlloyMixtureItem;
import net.mcreator.crustychunks.item.AdvancedAutomaticRifleReceiverItem;
import net.mcreator.crustychunks.item.AdvancedComponentItem;
import net.mcreator.crustychunks.item.AdvancedPistolReceiverItem;
import net.mcreator.crustychunks.item.AimerItem;
import net.mcreator.crustychunks.item.AluminateDustItem;
import net.mcreator.crustychunks.item.AluminumDustItem;
import net.mcreator.crustychunks.item.AluminumIngotItem;
import net.mcreator.crustychunks.item.AluminumPlateItem;
import net.mcreator.crustychunks.item.AluminumTinyDustItem;
import net.mcreator.crustychunks.item.ArmorPeelerAnimatedItem;
import net.mcreator.crustychunks.item.ArmorPeelerRocketItem;
import net.mcreator.crustychunks.item.ArmorPeelerUnloadedItem;
import net.mcreator.crustychunks.item.ArtilleryShellItem;
import net.mcreator.crustychunks.item.ArtillerySolidShellItem;
import net.mcreator.crustychunks.item.AutoPistolItem;
import net.mcreator.crustychunks.item.AutomaticRifleItem;
import net.mcreator.crustychunks.item.AutomaticRifleReceiverItem;
import net.mcreator.crustychunks.item.BasicReceiverItem;
import net.mcreator.crustychunks.item.BattleRifleItem;
import net.mcreator.crustychunks.item.BauxiteDustItem;
import net.mcreator.crustychunks.item.BentComponentItem;
import net.mcreator.crustychunks.item.BerylliumDustItem;
import net.mcreator.crustychunks.item.BerylliumIngotItem;
import net.mcreator.crustychunks.item.BirdShotItem;
import net.mcreator.crustychunks.item.BlastArmorItem;
import net.mcreator.crustychunks.item.BlastClayItem;
import net.mcreator.crustychunks.item.BlastFurnaceBrickItem;
import net.mcreator.crustychunks.item.BodyArmorItem;
import net.mcreator.crustychunks.item.BoltActionReceiverItem;
import net.mcreator.crustychunks.item.BoltActionRifleAnimatedItem;
import net.mcreator.crustychunks.item.BoredComponentItem;
import net.mcreator.crustychunks.item.BrassDustItem;
import net.mcreator.crustychunks.item.BrassFittingItem;
import net.mcreator.crustychunks.item.BrassIngotItem;
import net.mcreator.crustychunks.item.BrassPlateItem;
import net.mcreator.crustychunks.item.BreakActionShotgunAnimatedItem;
import net.mcreator.crustychunks.item.BreechRifleItem;
import net.mcreator.crustychunks.item.BulletItem;
import net.mcreator.crustychunks.item.BulletResistantHelmet2Item;
import net.mcreator.crustychunks.item.BulletResistantHelmet3Item;
import net.mcreator.crustychunks.item.BulletResistantHelmet4Item;
import net.mcreator.crustychunks.item.BulletResistantHelmetItem;
import net.mcreator.crustychunks.item.BurstRifleItem;
import net.mcreator.crustychunks.item.CableItem;
import net.mcreator.crustychunks.item.CanisterProjectileItem;
import net.mcreator.crustychunks.item.CanisterShellItem;
import net.mcreator.crustychunks.item.CastComponentItem;
import net.mcreator.crustychunks.item.ChaffChargeItem;
import net.mcreator.crustychunks.item.ChiselItem;
import net.mcreator.crustychunks.item.ChlorineDustItem;
import net.mcreator.crustychunks.item.CombustionCylinderItem;
import net.mcreator.crustychunks.item.ComponentFoundryTemplateItem;
import net.mcreator.crustychunks.item.CompressedAdvancedMixtureItem;
import net.mcreator.crustychunks.item.CopperCoilItem;
import net.mcreator.crustychunks.item.CopperDustItem;
import net.mcreator.crustychunks.item.CopperPlateItem;
import net.mcreator.crustychunks.item.CopperWireItem;
import net.mcreator.crustychunks.item.CrudeOilItem;
import net.mcreator.crustychunks.item.CutComponentItem;
import net.mcreator.crustychunks.item.CuttersItem;
import net.mcreator.crustychunks.item.CylinderFoundryTemplateItem;
import net.mcreator.crustychunks.item.DieselItem;
import net.mcreator.crustychunks.item.ERATileItem;
import net.mcreator.crustychunks.item.ElectricMotorItem;
import net.mcreator.crustychunks.item.EmberParticleItem;
import net.mcreator.crustychunks.item.EnergyMeterItem;
import net.mcreator.crustychunks.item.EngineComponentItem;
import net.mcreator.crustychunks.item.EnrichedLithiumIngotItem;
import net.mcreator.crustychunks.item.EnrichedLithiumNuggetItem;
import net.mcreator.crustychunks.item.EradicationItem;
import net.mcreator.crustychunks.item.ExtraLargeBulletItem;
import net.mcreator.crustychunks.item.ExtraLargeCasingItem;
import net.mcreator.crustychunks.item.ExtraLargeProjectileItem;
import net.mcreator.crustychunks.item.ExtraLargeProjectileTemplateItem;
import net.mcreator.crustychunks.item.FilteredAluminateDustItem;
import net.mcreator.crustychunks.item.FilteredPyrochloreDustItem;
import net.mcreator.crustychunks.item.FireAgentItem;
import net.mcreator.crustychunks.item.FireArtilleryShellItem;
import net.mcreator.crustychunks.item.FireSpearRocketItem;
import net.mcreator.crustychunks.item.FiringMechanismItem;
import net.mcreator.crustychunks.item.FiringPinItem;
import net.mcreator.crustychunks.item.FissionCoreItem;
import net.mcreator.crustychunks.item.FlakProjectileItem;
import net.mcreator.crustychunks.item.FlakShellItem;
import net.mcreator.crustychunks.item.FlameThrowerAnimatedItem;
import net.mcreator.crustychunks.item.FlameThrowerTankItem;
import net.mcreator.crustychunks.item.FlareChargeItem;
import net.mcreator.crustychunks.item.FlarePistolItem;
import net.mcreator.crustychunks.item.FoundryTemplateItem;
import net.mcreator.crustychunks.item.FuelHoseItem;
import net.mcreator.crustychunks.item.FuelRodItem;
import net.mcreator.crustychunks.item.FusionCoreItem;
import net.mcreator.crustychunks.item.GasArtilleryShellItem;
import net.mcreator.crustychunks.item.GasCanisterItem;
import net.mcreator.crustychunks.item.GasMaskHelmetItem;
import net.mcreator.crustychunks.item.GasMaskItem;
import net.mcreator.crustychunks.item.GeigerCounterItem;
import net.mcreator.crustychunks.item.GoldDustItem;
import net.mcreator.crustychunks.item.GrenadeItem;
import net.mcreator.crustychunks.item.GrenadeLauncherItem;
import net.mcreator.crustychunks.item.GrenadeShellItem;
import net.mcreator.crustychunks.item.HEATProjectileItem;
import net.mcreator.crustychunks.item.HEProjectileItem;
import net.mcreator.crustychunks.item.HammerItem;
import net.mcreator.crustychunks.item.HandDrillItem;
import net.mcreator.crustychunks.item.HeatShellItem;
import net.mcreator.crustychunks.item.HollowedExtraLargeProjectileItem;
import net.mcreator.crustychunks.item.HollowedHugeProjectileItem;
import net.mcreator.crustychunks.item.HollowedLargeProjectileItem;
import net.mcreator.crustychunks.item.HugeBarrelFoundryTemplateItem;
import net.mcreator.crustychunks.item.HugeBoredBarrelItem;
import net.mcreator.crustychunks.item.HugeBulletItem;
import net.mcreator.crustychunks.item.HugeCannonFoundryTemplateItem;
import net.mcreator.crustychunks.item.HugeCasingItem;
import net.mcreator.crustychunks.item.HugeHEBulletItem;
import net.mcreator.crustychunks.item.HugeProjectileFoundryTemplateItem;
import net.mcreator.crustychunks.item.HugeProjectileItem;
import net.mcreator.crustychunks.item.HugeUnboredBarrelItem;
import net.mcreator.crustychunks.item.HugeUnboredCannonBarrelItem;
import net.mcreator.crustychunks.item.HydrazineItem;
import net.mcreator.crustychunks.item.IRComponentItem;
import net.mcreator.crustychunks.item.ImpactFuzeItem;
import net.mcreator.crustychunks.item.ImplosionLensItem;
import net.mcreator.crustychunks.item.ImplosionModuleItem;
import net.mcreator.crustychunks.item.IncendiaryBottleItem;
import net.mcreator.crustychunks.item.IncendiaryGrenadeItem;
import net.mcreator.crustychunks.item.InvisibleitemItem;
import net.mcreator.crustychunks.item.IronDustItem;
import net.mcreator.crustychunks.item.IronTubeItem;
import net.mcreator.crustychunks.item.IrongearItem;
import net.mcreator.crustychunks.item.KeroseneItem;
import net.mcreator.crustychunks.item.LMGAnimatedItem;
import net.mcreator.crustychunks.item.LMGMagazineItem;
import net.mcreator.crustychunks.item.LargeBarrelTemplateItem;
import net.mcreator.crustychunks.item.LargeBoredBarrelItem;
import net.mcreator.crustychunks.item.LargeBulletItem;
import net.mcreator.crustychunks.item.LargeCannonFoundryTemplateItem;
import net.mcreator.crustychunks.item.LargeCasingItem;
import net.mcreator.crustychunks.item.LargeFoundryTemplateItem;
import net.mcreator.crustychunks.item.LargeMagazineItem;
import net.mcreator.crustychunks.item.LargeProjectileFoundryTemplateItem;
import net.mcreator.crustychunks.item.LargeProjectileItem;
import net.mcreator.crustychunks.item.LargeShellItem;
import net.mcreator.crustychunks.item.LargeUnboredBarrelItem;
import net.mcreator.crustychunks.item.LargeUnboredCannonBarrelItem;
import net.mcreator.crustychunks.item.LargeVolatilePileItem;
import net.mcreator.crustychunks.item.LeadDustItem;
import net.mcreator.crustychunks.item.LeadIngotItem;
import net.mcreator.crustychunks.item.LeadNuggetItem;
import net.mcreator.crustychunks.item.LeverRifleItem;
import net.mcreator.crustychunks.item.LithiumDeuterideItem;
import net.mcreator.crustychunks.item.LithiumDustItem;
import net.mcreator.crustychunks.item.LithiumIngotItem;
import net.mcreator.crustychunks.item.LithiumNuggetItem;
import net.mcreator.crustychunks.item.MGReceiverItem;
import net.mcreator.crustychunks.item.MachineCarbineItem;
import net.mcreator.crustychunks.item.MachineGunBoxItem;
import net.mcreator.crustychunks.item.MechanicalBoreItem;
import net.mcreator.crustychunks.item.MechanicalExtruderItem;
import net.mcreator.crustychunks.item.MechanicalPressItem;
import net.mcreator.crustychunks.item.MechanicalShearItem;
import net.mcreator.crustychunks.item.MediumAPBulletItem;
import net.mcreator.crustychunks.item.MediumBarrelTemplateItem;
import net.mcreator.crustychunks.item.MediumBoredBarrelItem;
import net.mcreator.crustychunks.item.MediumCannonFoundryTemplateItem;
import net.mcreator.crustychunks.item.MediumCasingItem;
import net.mcreator.crustychunks.item.MediumMagazineItem;
import net.mcreator.crustychunks.item.MediumProjectileFoundryTemplateItem;
import net.mcreator.crustychunks.item.MediumProjectileItem;
import net.mcreator.crustychunks.item.MediumStealthBulletItem;
import net.mcreator.crustychunks.item.MediumUnboredBarrelItem;
import net.mcreator.crustychunks.item.MediumUnboredCannonBarrelItem;
import net.mcreator.crustychunks.item.MortarShellItem;
import net.mcreator.crustychunks.item.MusketBallItem;
import net.mcreator.crustychunks.item.MusketItem;
import net.mcreator.crustychunks.item.NVDHelmetItem;
import net.mcreator.crustychunks.item.NeutronReflectorItem;
import net.mcreator.crustychunks.item.NickelDustItem;
import net.mcreator.crustychunks.item.NickelIngotItem;
import net.mcreator.crustychunks.item.NiobiumDustItem;
import net.mcreator.crustychunks.item.NiobiumIngotItem;
import net.mcreator.crustychunks.item.NiobiumTinyDustItem;
import net.mcreator.crustychunks.item.NitrateItem;
import net.mcreator.crustychunks.item.OilItem;
import net.mcreator.crustychunks.item.PaintToolItem;
import net.mcreator.crustychunks.item.Particle2Item;
import net.mcreator.crustychunks.item.ParticleItem;
import net.mcreator.crustychunks.item.PetroliumItem;
import net.mcreator.crustychunks.item.PhosphorusDustItem;
import net.mcreator.crustychunks.item.PistolReceiverItem;
import net.mcreator.crustychunks.item.PlutoniumCoreItem;
import net.mcreator.crustychunks.item.PlutoniumIngotItem;
import net.mcreator.crustychunks.item.PlutoniumNuggetItem;
import net.mcreator.crustychunks.item.PowderChargeItem;
import net.mcreator.crustychunks.item.PowerCellItem;
import net.mcreator.crustychunks.item.PrecisionComponentItem;
import net.mcreator.crustychunks.item.PropellentItem;
import net.mcreator.crustychunks.item.PumpActionShotgunAnimatedItem;
import net.mcreator.crustychunks.item.PyrochloreDustItem;
import net.mcreator.crustychunks.item.PyrochloreItem;
import net.mcreator.crustychunks.item.RadarComponentItem;
import net.mcreator.crustychunks.item.RadarSpearMissileItem;
import net.mcreator.crustychunks.item.RawBerylliumItem;
import net.mcreator.crustychunks.item.RawLeadItem;
import net.mcreator.crustychunks.item.RawLithiumItem;
import net.mcreator.crustychunks.item.RawNickelItem;
import net.mcreator.crustychunks.item.RawUraniumItem;
import net.mcreator.crustychunks.item.RawZincItem;
import net.mcreator.crustychunks.item.ReactioncomponentItem;
import net.mcreator.crustychunks.item.RevolverAnimatedItem;
import net.mcreator.crustychunks.item.RevolverReceiverItem;
import net.mcreator.crustychunks.item.RifleStockItem;
import net.mcreator.crustychunks.item.SMGAnimatedItem;
import net.mcreator.crustychunks.item.SMGMagazineItem;
import net.mcreator.crustychunks.item.SMGReceiverItem;
import net.mcreator.crustychunks.item.ScopedBoltActionRifleAnimatedItem;
import net.mcreator.crustychunks.item.ScopedBreechRifleItem;
import net.mcreator.crustychunks.item.SeekerSpearRocketItem;
import net.mcreator.crustychunks.item.SemiAutomaticPistolAnimatedItem;
import net.mcreator.crustychunks.item.SemiAutomaticRifleAnimatedItem;
import net.mcreator.crustychunks.item.ShaleOilItem;
import net.mcreator.crustychunks.item.ShapedChargeFuzeItem;
import net.mcreator.crustychunks.item.ShieldingComponentItem;
import net.mcreator.crustychunks.item.ShotgunCasingItem;
import net.mcreator.crustychunks.item.ShotgunShellItem;
import net.mcreator.crustychunks.item.SingleShotRifleItem;
import net.mcreator.crustychunks.item.SlugShellItem;
import net.mcreator.crustychunks.item.SmallAPShellItem;
import net.mcreator.crustychunks.item.SmallBarrelTemplateItem;
import net.mcreator.crustychunks.item.SmallBoredBarrelItem;
import net.mcreator.crustychunks.item.SmallCannonFoundryTemplateItem;
import net.mcreator.crustychunks.item.SmallCasingItem;
import net.mcreator.crustychunks.item.SmallEngineItem;
import net.mcreator.crustychunks.item.SmallFlakProjectileItem;
import net.mcreator.crustychunks.item.SmallFlakShellItem;
import net.mcreator.crustychunks.item.SmallHEProjectileItem;
import net.mcreator.crustychunks.item.SmallHollowPointBulletItem;
import net.mcreator.crustychunks.item.SmallProjectileFoundryTemplateItem;
import net.mcreator.crustychunks.item.SmallProjectileItem;
import net.mcreator.crustychunks.item.SmallShellItem;
import net.mcreator.crustychunks.item.SmallStealthBulletItem;
import net.mcreator.crustychunks.item.SmallUnboredBarrelItem;
import net.mcreator.crustychunks.item.SmallUnboredCannonBarrelItem;
import net.mcreator.crustychunks.item.SmallbulletItem;
import net.mcreator.crustychunks.item.SmallmagazineItem;
import net.mcreator.crustychunks.item.SmokeAgentItem;
import net.mcreator.crustychunks.item.SmokeGrenadeItem;
import net.mcreator.crustychunks.item.SmokeGrenadeShellItem;
import net.mcreator.crustychunks.item.SmokeMortarShellItem;
import net.mcreator.crustychunks.item.SmokeProjectileItem;
import net.mcreator.crustychunks.item.SmokeShellItem;
import net.mcreator.crustychunks.item.SolidRocketFuelPackItem;
import net.mcreator.crustychunks.item.SolidShellItem;
import net.mcreator.crustychunks.item.StealthLargeBulletItem;
import net.mcreator.crustychunks.item.StealthPistolItem;
import net.mcreator.crustychunks.item.SteelComponentItem;
import net.mcreator.crustychunks.item.SteelCrushingWheelItem;
import net.mcreator.crustychunks.item.SteelCylinderItem;
import net.mcreator.crustychunks.item.SteelGearItem;
import net.mcreator.crustychunks.item.SteelIngotItem;
import net.mcreator.crustychunks.item.SteelSpringItem;
import net.mcreator.crustychunks.item.SteelTubeItem;
import net.mcreator.crustychunks.item.SteelWireItem;
import net.mcreator.crustychunks.item.SteelplateItem;
import net.mcreator.crustychunks.item.StrikeSpearMissileItem;
import net.mcreator.crustychunks.item.SulfurItem;
import net.mcreator.crustychunks.item.SulfuricAcidItem;
import net.mcreator.crustychunks.item.TechComponentItem;
import net.mcreator.crustychunks.item.ThermalShellItem;
import net.mcreator.crustychunks.item.ThermoNuclearFuelItem;
import net.mcreator.crustychunks.item.ThermometerItem;
import net.mcreator.crustychunks.item.TimedFuzeItem;
import net.mcreator.crustychunks.item.TinyLithiumDeuterideItem;
import net.mcreator.crustychunks.item.TinyprojectileItemItem;
import net.mcreator.crustychunks.item.ToxicAgentItem;
import net.mcreator.crustychunks.item.TransparentItemItem;
import net.mcreator.crustychunks.item.TrinititeShardItem;
import net.mcreator.crustychunks.item.TurbineRotorItem;
import net.mcreator.crustychunks.item.UnfabricatedTechComponentItem;
import net.mcreator.crustychunks.item.UraniumDepletedDustItem;
import net.mcreator.crustychunks.item.UraniumDepletedIngotItem;
import net.mcreator.crustychunks.item.UraniumDepletedTinyDustItem;
import net.mcreator.crustychunks.item.UraniumEnrichedDustItem;
import net.mcreator.crustychunks.item.UraniumEnrichedIngotItem;
import net.mcreator.crustychunks.item.UraniumEnrichedTinyDustItem;
import net.mcreator.crustychunks.item.UraniumNeuralIngotItem;
import net.mcreator.crustychunks.item.UraniumNeutralDustItem;
import net.mcreator.crustychunks.item.UraniumNeutraltinyDustItem;
import net.mcreator.crustychunks.item.VolatileDustItem;
import net.mcreator.crustychunks.item.WeaponBoltItem;
import net.mcreator.crustychunks.item.WeaponSupressorItem;
import net.mcreator.crustychunks.item.WelderItem;
import net.mcreator.crustychunks.item.WoodComponentItem;
import net.mcreator.crustychunks.item.ZincDustItem;
import net.mcreator.crustychunks.item.ZincIngotItem;
import net.mcreator.crustychunks.procedures.MagazineLevelProcedure;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class CrustyChunksModItems {
   public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(BuiltInRegistries.ITEM, "crusty_chunks");
   public static final DeferredHolder<Item, Item> EXPLOSIVE_BARREL = block(CrustyChunksModBlocks.EXPLOSIVE_BARREL);
   public static final DeferredHolder<Item, Item> BURNTGRASS = block(CrustyChunksModBlocks.BURNTGRASS);
   public static final DeferredHolder<Item, Item> HARDDIRT = block(CrustyChunksModBlocks.HARDDIRT);
   public static final DeferredHolder<Item, Item> ZINC_INGOT = REGISTRY.register("zinc_ingot", () -> new ZincIngotItem());
   public static final DeferredHolder<Item, Item> ZINC_ORE = block(CrustyChunksModBlocks.ZINC_ORE);
   public static final DeferredHolder<Item, Item> ZINC_BLOCK = block(CrustyChunksModBlocks.ZINC_BLOCK);
   public static final DeferredHolder<Item, Item> BRASS_INGOT = REGISTRY.register("brass_ingot", () -> new BrassIngotItem());
   public static final DeferredHolder<Item, Item> BRASS_BLOCK = block(CrustyChunksModBlocks.BRASS_BLOCK);
   public static final DeferredHolder<Item, Item> STEEL_INGOT = REGISTRY.register("steel_ingot", () -> new SteelIngotItem());
   public static final DeferredHolder<Item, Item> STEEL_BLOCK = block(CrustyChunksModBlocks.STEEL_BLOCK);
   public static final DeferredHolder<Item, Item> SMALLBULLET = REGISTRY.register("smallbullet", () -> new SmallbulletItem());
   public static final DeferredHolder<Item, Item> BULLET = REGISTRY.register("bullet", () -> new BulletItem());
   public static final DeferredHolder<Item, Item> LARGE_BULLET = REGISTRY.register("large_bullet", () -> new LargeBulletItem());
   public static final DeferredHolder<Item, Item> HUGE_BULLET = REGISTRY.register("huge_bullet", () -> new HugeBulletItem());
   public static final DeferredHolder<Item, Item> SMALL_SHELL = REGISTRY.register("small_shell", () -> new SmallShellItem());
   public static final DeferredHolder<Item, Item> LARGE_SHELL = REGISTRY.register("large_shell", () -> new LargeShellItem());
   public static final DeferredHolder<Item, Item> POWDER_CHARGE = REGISTRY.register("powder_charge", () -> new PowderChargeItem());
   public static final DeferredHolder<Item, Item> ARTILLERY_SHELL = REGISTRY.register("artillery_shell", () -> new ArtilleryShellItem());
   public static final DeferredHolder<Item, Item> LEAD_INGOT = REGISTRY.register("lead_ingot", () -> new LeadIngotItem());
   public static final DeferredHolder<Item, Item> LEAD_ORE = block(CrustyChunksModBlocks.LEAD_ORE);
   public static final DeferredHolder<Item, Item> LEAD_BLOCK = block(CrustyChunksModBlocks.LEAD_BLOCK);
   public static final DeferredHolder<Item, Item> STEELPLATE = REGISTRY.register("steelplate", () -> new SteelplateItem());
   public static final DeferredHolder<Item, Item> STEEL_COMPONENT = REGISTRY.register("steel_component", () -> new SteelComponentItem());
   public static final DeferredHolder<Item, Item> CAST_COMPONENT = REGISTRY.register("cast_component", () -> new CastComponentItem());
   public static final DeferredHolder<Item, Item> STEEL_CYLINDER = REGISTRY.register("steel_cylinder", () -> new SteelCylinderItem());
   public static final DeferredHolder<Item, Item> STEEL_SPRING = REGISTRY.register("steel_spring", () -> new SteelSpringItem());
   public static final DeferredHolder<Item, Item> STEEL_TUBE = REGISTRY.register("steel_tube", () -> new SteelTubeItem());
   public static final DeferredHolder<Item, Item> STEEL_WIRE = REGISTRY.register("steel_wire", () -> new SteelWireItem());
   public static final DeferredHolder<Item, Item> CUT_COMPONENT = REGISTRY.register("cut_component", () -> new CutComponentItem());
   public static final DeferredHolder<Item, Item> BORED_COMPONENT = REGISTRY.register("bored_component", () -> new BoredComponentItem());
   public static final DeferredHolder<Item, Item> BENT_COMPONENT = REGISTRY.register("bent_component", () -> new BentComponentItem());
   public static final DeferredHolder<Item, Item> STEEL_GEAR = REGISTRY.register("steel_gear", () -> new SteelGearItem());
   public static final DeferredHolder<Item, Item> WOOD_COMPONENT = REGISTRY.register("wood_component", () -> new WoodComponentItem());
   public static final DeferredHolder<Item, Item> SMALL_CASING = REGISTRY.register("small_casing", () -> new SmallCasingItem());
   public static final DeferredHolder<Item, Item> MEDIUM_CASING = REGISTRY.register("medium_casing", () -> new MediumCasingItem());
   public static final DeferredHolder<Item, Item> LARGE_CASING = REGISTRY.register("large_casing", () -> new LargeCasingItem());
   public static final DeferredHolder<Item, Item> HUGE_CASING = REGISTRY.register("huge_casing", () -> new HugeCasingItem());
   public static final DeferredHolder<Item, Item> SMALLMAGAZINE = REGISTRY.register("smallmagazine", () -> new SmallmagazineItem());
   public static final DeferredHolder<Item, Item> MEDIUM_MAGAZINE = REGISTRY.register("medium_magazine", () -> new MediumMagazineItem());
   public static final DeferredHolder<Item, Item> COPPER_COIL = REGISTRY.register("copper_coil", () -> new CopperCoilItem());
   public static final DeferredHolder<Item, Item> COPPER_WIRE = REGISTRY.register("copper_wire", () -> new CopperWireItem());
   public static final DeferredHolder<Item, Item> NICKEL_INGOT = REGISTRY.register("nickel_ingot", () -> new NickelIngotItem());
   public static final DeferredHolder<Item, Item> NICKEL_ORE = block(CrustyChunksModBlocks.NICKEL_ORE);
   public static final DeferredHolder<Item, Item> NICKEL_BLOCK = block(CrustyChunksModBlocks.NICKEL_BLOCK);
   public static final DeferredHolder<Item, Item> TAR = block(CrustyChunksModBlocks.TAR);
   public static final DeferredHolder<Item, Item> STEEL_PLATING_STAIRS = block(CrustyChunksModBlocks.STEEL_PLATING_STAIRS);
   public static final DeferredHolder<Item, Item> SMOKE_BOMB = block(CrustyChunksModBlocks.SMOKE_BOMB);
   public static final DeferredHolder<Item, Item> STEEL_TRAPDOOR = block(CrustyChunksModBlocks.STEEL_TRAPDOOR);
   public static final DeferredHolder<Item, Item> STEEL_OPTIC = block(CrustyChunksModBlocks.STEEL_OPTIC);
   public static final DeferredHolder<Item, Item> CHARRED_BLOCK = block(CrustyChunksModBlocks.CHARRED_BLOCK);
   public static final DeferredHolder<Item, Item> REENFORCED_CONCRETE = block(CrustyChunksModBlocks.REENFORCED_CONCRETE);
   public static final DeferredHolder<Item, Item> CRACKED_CONCRETE = block(CrustyChunksModBlocks.CRACKED_CONCRETE);
   public static final DeferredHolder<Item, Item> FRACTURED_CONCRETE = block(CrustyChunksModBlocks.FRACTURED_CONCRETE);
   public static final DeferredHolder<Item, Item> DAMAGED_CONCRETE = block(CrustyChunksModBlocks.DAMAGED_CONCRETE);
   public static final DeferredHolder<Item, Item> DESTROYED_CONCRETE = block(CrustyChunksModBlocks.DESTROYED_CONCRETE);
   public static final DeferredHolder<Item, Item> ALUMINUM_INGOT = REGISTRY.register("aluminum_ingot", () -> new AluminumIngotItem());
   public static final DeferredHolder<Item, Item> ALUMINUM_BLOCK = block(CrustyChunksModBlocks.ALUMINUM_BLOCK);
   public static final DeferredHolder<Item, Item> SMG_MAGAZINE = REGISTRY.register("smg_magazine", () -> new SMGMagazineItem());
   public static final DeferredHolder<Item, Item> MACHINE_GUN = block(CrustyChunksModBlocks.MACHINE_GUN);
   public static final DeferredHolder<Item, Item> MACHINE_GUN_BARREL = block(CrustyChunksModBlocks.MACHINE_GUN_BARREL);
   public static final DeferredHolder<Item, Item> AIMER = REGISTRY.register("aimer", () -> new AimerItem());
   public static final DeferredHolder<Item, Item> ARTILLERYBREECH = block(CrustyChunksModBlocks.ARTILLERYBREECH);
   public static final DeferredHolder<Item, Item> ARTILLERY_BARREL = block(CrustyChunksModBlocks.ARTILLERY_BARREL);
   public static final DeferredHolder<Item, Item> MACHINE_GUN_BOX = REGISTRY.register("machine_gun_box", () -> new MachineGunBoxItem());
   public static final DeferredHolder<Item, Item> BATTLE_CANNON_BARREL = block(CrustyChunksModBlocks.BATTLE_CANNON_BARREL);
   public static final DeferredHolder<Item, Item> PARTICLE = REGISTRY.register("particle", () -> new ParticleItem());
   public static final DeferredHolder<Item, Item> EMBER_PARTICLE = REGISTRY.register("ember_particle", () -> new EmberParticleItem());
   public static final DeferredHolder<Item, Item> ARMOR_PEELER_UNLOADED = REGISTRY.register("armor_peeler_unloaded", () -> new ArmorPeelerUnloadedItem());
   public static final DeferredHolder<Item, Item> ARMOR_PEELER_ROCKET = REGISTRY.register("armor_peeler_rocket", () -> new ArmorPeelerRocketItem());
   public static final DeferredHolder<Item, Item> HEAT_SHELL = REGISTRY.register("heat_shell", () -> new HeatShellItem());
   public static final DeferredHolder<Item, Item> AP_SHELL = REGISTRY.register("ap_shell", () -> new APShellItem());
   public static final DeferredHolder<Item, Item> GRENADE = REGISTRY.register("grenade", () -> new GrenadeItem());
   public static final DeferredHolder<Item, Item> TINYPROJECTILE_ITEM = REGISTRY.register("tinyprojectile_item", () -> new TinyprojectileItemItem());
   public static final DeferredHolder<Item, Item> SHOTGUN_SHELL = REGISTRY.register("shotgun_shell", () -> new ShotgunShellItem());
   public static final DeferredHolder<Item, Item> SHOTGUN_CASING = REGISTRY.register("shotgun_casing", () -> new ShotgunCasingItem());
   public static final DeferredHolder<Item, Item> BLAST_FURNACE_BRICKS = block(CrustyChunksModBlocks.BLAST_FURNACE_BRICKS);
   public static final DeferredHolder<Item, Item> BLAST_FURNACE_BRICK = REGISTRY.register("blast_furnace_brick", () -> new BlastFurnaceBrickItem());
   public static final DeferredHolder<Item, Item> BLAST_FUNNEL = block(CrustyChunksModBlocks.BLAST_FUNNEL);
   public static final DeferredHolder<Item, Item> FOUNDRY = block(CrustyChunksModBlocks.FOUNDRY);
   public static final DeferredHolder<Item, Item> CONVEYOR = block(CrustyChunksModBlocks.CONVEYOR);
   public static final DeferredHolder<Item, Item> SMALL_PROJECTILE = REGISTRY.register("small_projectile", () -> new SmallProjectileItem());
   public static final DeferredHolder<Item, Item> FIREBOX = block(CrustyChunksModBlocks.FIREBOX);
   public static final DeferredHolder<Item, Item> LEAD_NUGGET = REGISTRY.register("lead_nugget", () -> new LeadNuggetItem());
   public static final DeferredHolder<Item, Item> CHISEL = REGISTRY.register("chisel", () -> new ChiselItem());
   public static final DeferredHolder<Item, Item> BRASS_PLATE = REGISTRY.register("brass_plate", () -> new BrassPlateItem());
   public static final DeferredHolder<Item, Item> ELECTRIC_MOTOR = REGISTRY.register("electric_motor", () -> new ElectricMotorItem());
   public static final DeferredHolder<Item, Item> HAMMER = REGISTRY.register("hammer", () -> new HammerItem());
   public static final DeferredHolder<Item, Item> CUTTERS = REGISTRY.register("cutters", () -> new CuttersItem());
   public static final DeferredHolder<Item, Item> COPPER_PLATE = REGISTRY.register("copper_plate", () -> new CopperPlateItem());
   public static final DeferredHolder<Item, Item> SMOKE_GRENADE = REGISTRY.register("smoke_grenade", () -> new SmokeGrenadeItem());
   public static final DeferredHolder<Item, Item> AUTOCANNON = block(CrustyChunksModBlocks.AUTOCANNON);
   public static final DeferredHolder<Item, Item> AUTOCANNON_BARREL = block(CrustyChunksModBlocks.AUTOCANNON_BARREL);
   public static final DeferredHolder<Item, Item> AUTOCANNON_DRUM = block(CrustyChunksModBlocks.AUTOCANNON_DRUM);
   public static final DeferredHolder<Item, Item> RAZOR_WIRE = block(CrustyChunksModBlocks.RAZOR_WIRE);
   public static final DeferredHolder<Item, Item> WIRE_FENCE = block(CrustyChunksModBlocks.WIRE_FENCE);
   public static final DeferredHolder<Item, Item> CONCRETE_WALL = block(CrustyChunksModBlocks.CONCRETE_WALL);
   public static final DeferredHolder<Item, Item> SHEET_METAL = block(CrustyChunksModBlocks.SHEET_METAL);
   public static final DeferredHolder<Item, Item> SHEET_METAL_PANE = block(CrustyChunksModBlocks.SHEET_METAL_PANE);
   public static final DeferredHolder<Item, Item> AUTOLOADER = block(CrustyChunksModBlocks.AUTOLOADER);
   public static final DeferredHolder<Item, Item> MUSKET_BALL = REGISTRY.register("musket_ball", () -> new MusketBallItem());
   public static final DeferredHolder<Item, Item> PARTICLE_2 = REGISTRY.register("particle_2", () -> new Particle2Item());
   public static final DeferredHolder<Item, Item> FISSION_BOMB = block(CrustyChunksModBlocks.FISSION_BOMB);
   public static final DeferredHolder<Item, Item> TRINITITE = block(CrustyChunksModBlocks.TRINITITE);
   public static final DeferredHolder<Item, Item> DEEPSLATE_LEAD_ORE = block(CrustyChunksModBlocks.DEEPSLATE_LEAD_ORE);
   public static final DeferredHolder<Item, Item> RAW_LEAD = REGISTRY.register("raw_lead", () -> new RawLeadItem());
   public static final DeferredHolder<Item, Item> RAW_LEAD_BLOCK = block(CrustyChunksModBlocks.RAW_LEAD_BLOCK);
   public static final DeferredHolder<Item, Item> LEAD_DUST = REGISTRY.register("lead_dust", () -> new LeadDustItem());
   public static final DeferredHolder<Item, Item> BRASS_DUST = REGISTRY.register("brass_dust", () -> new BrassDustItem());
   public static final DeferredHolder<Item, Item> RAW_NICKEL = REGISTRY.register("raw_nickel", () -> new RawNickelItem());
   public static final DeferredHolder<Item, Item> RAW_NICKEL_BLOCK = block(CrustyChunksModBlocks.RAW_NICKEL_BLOCK);
   public static final DeferredHolder<Item, Item> NICKEL_DUST = REGISTRY.register("nickel_dust", () -> new NickelDustItem());
   public static final DeferredHolder<Item, Item> RAW_ZINC_BLOCK = block(CrustyChunksModBlocks.RAW_ZINC_BLOCK);
   public static final DeferredHolder<Item, Item> RAW_ZINC = REGISTRY.register("raw_zinc", () -> new RawZincItem());
   public static final DeferredHolder<Item, Item> BAUXITE = block(CrustyChunksModBlocks.BAUXITE);
   public static final DeferredHolder<Item, Item> BAUXITE_DUST = REGISTRY.register("bauxite_dust", () -> new BauxiteDustItem());
   public static final DeferredHolder<Item, Item> ALUMINUM_DUST = REGISTRY.register("aluminum_dust", () -> new AluminumDustItem());
   public static final DeferredHolder<Item, Item> IRON_DUST = REGISTRY.register("iron_dust", () -> new IronDustItem());
   public static final DeferredHolder<Item, Item> ZINC_DUST = REGISTRY.register("zinc_dust", () -> new ZincDustItem());
   public static final DeferredHolder<Item, Item> RAW_URANIUM = REGISTRY.register("raw_uranium", () -> new RawUraniumItem());
   public static final DeferredHolder<Item, Item> URANIUM_NEURAL_INGOT = REGISTRY.register("uranium_neural_ingot", () -> new UraniumNeuralIngotItem());
   public static final DeferredHolder<Item, Item> URANIUM_NEUTRAL_DUST = REGISTRY.register("uranium_neutral_dust", () -> new UraniumNeutralDustItem());
   public static final DeferredHolder<Item, Item> URANIUM_NEUTRALTINY_DUST = REGISTRY.register("uranium_neutraltiny_dust", () -> new UraniumNeutraltinyDustItem());
   public static final DeferredHolder<Item, Item> URANIUM_ENRICHED_INGOT = REGISTRY.register("uranium_enriched_ingot", () -> new UraniumEnrichedIngotItem());
   public static final DeferredHolder<Item, Item> URANIUM_ENRICHED_DUST = REGISTRY.register("uranium_enriched_dust", () -> new UraniumEnrichedDustItem());
   public static final DeferredHolder<Item, Item> URANIUM_ENRICHED_TINY_DUST = REGISTRY.register(
      "uranium_enriched_tiny_dust", () -> new UraniumEnrichedTinyDustItem()
   );
   public static final DeferredHolder<Item, Item> URANIUM_DEPLETED_INGOT = REGISTRY.register("uranium_depleted_ingot", () -> new UraniumDepletedIngotItem());
   public static final DeferredHolder<Item, Item> URANIUM_DEPLETED_DUST = REGISTRY.register("uranium_depleted_dust", () -> new UraniumDepletedDustItem());
   public static final DeferredHolder<Item, Item> URANIUM_DEPLETED_TINY_DUST = REGISTRY.register(
      "uranium_depleted_tiny_dust", () -> new UraniumDepletedTinyDustItem()
   );
   public static final DeferredHolder<Item, Item> MINERAL_GRINDER = block(CrustyChunksModBlocks.MINERAL_GRINDER);
   public static final DeferredHolder<Item, Item> GOLD_DUST = REGISTRY.register("gold_dust", () -> new GoldDustItem());
   public static final DeferredHolder<Item, Item> COPPER_DUST = REGISTRY.register("copper_dust", () -> new CopperDustItem());
   public static final DeferredHolder<Item, Item> THERMOMETER = REGISTRY.register("thermometer", () -> new ThermometerItem());
   public static final DeferredHolder<Item, Item> FLAK_SHELL = REGISTRY.register("flak_shell", () -> new FlakShellItem());
   public static final DeferredHolder<Item, Item> REBAR = block(CrustyChunksModBlocks.REBAR);
   public static final DeferredHolder<Item, Item> SMALL_BOMB = block(CrustyChunksModBlocks.SMALL_BOMB);
   public static final DeferredHolder<Item, Item> CLUSTER_OF_BOMBS = block(CrustyChunksModBlocks.CLUSTER_OF_BOMBS);
   public static final DeferredHolder<Item, Item> FLAME_THROWER_TANK_CHESTPLATE = REGISTRY.register(
      "flame_thrower_tank_chestplate", () -> new FlameThrowerTankItem.Chestplate()
   );
   public static final DeferredHolder<Item, Item> BULLET_RESISTANT_HELMET_HELMET = REGISTRY.register(
      "bullet_resistant_helmet_helmet", () -> new BulletResistantHelmetItem.Helmet()
   );
   public static final DeferredHolder<Item, Item> SEMI_AUTOMATIC_RIFLE_ANIMATED = REGISTRY.register(
      "semi_automatic_rifle_animated", () -> new SemiAutomaticRifleAnimatedItem()
   );
   public static final DeferredHolder<Item, Item> SEMI_AUTOMATIC_PISTOL_ANIMATED = REGISTRY.register(
      "semi_automatic_pistol_animated", () -> new SemiAutomaticPistolAnimatedItem()
   );
   public static final DeferredHolder<Item, Item> BOLT_ACTION_RIFLE_ANIMATED = REGISTRY.register(
      "bolt_action_rifle_animated", () -> new BoltActionRifleAnimatedItem()
   );
   public static final DeferredHolder<Item, Item> SCOPED_BOLT_ACTION_RIFLE_ANIMATED = REGISTRY.register(
      "scoped_bolt_action_rifle_animated", () -> new ScopedBoltActionRifleAnimatedItem()
   );
   public static final DeferredHolder<Item, Item> PUMP_ACTION_SHOTGUN_ANIMATED = REGISTRY.register(
      "pump_action_shotgun_animated", () -> new PumpActionShotgunAnimatedItem()
   );
   public static final DeferredHolder<Item, Item> ARMOR_PEELER_ANIMATED = REGISTRY.register("armor_peeler_animated", () -> new ArmorPeelerAnimatedItem());
   public static final DeferredHolder<Item, Item> SMG_ANIMATED = REGISTRY.register("smg_animated", () -> new SMGAnimatedItem());
   public static final DeferredHolder<Item, Item> ARTILLERY_AUTOLOADER = block(CrustyChunksModBlocks.ARTILLERY_AUTOLOADER);
   public static final DeferredHolder<Item, Item> ARTILLERY_CHARGE_LOADER = block(CrustyChunksModBlocks.ARTILLERY_CHARGE_LOADER);
   public static final DeferredHolder<Item, Item> TORPEDO_THRUSTER = block(CrustyChunksModBlocks.TORPEDO_THRUSTER);
   public static final DeferredHolder<Item, Item> FLAME_THROWER_ANIMATED = REGISTRY.register("flame_thrower_animated", () -> new FlameThrowerAnimatedItem());
   public static final DeferredHolder<Item, Item> REVOLVER_ANIMATED = REGISTRY.register("revolver_animated", () -> new RevolverAnimatedItem());
   public static final DeferredHolder<Item, Item> SINGLE_SHOT_RIFLE = REGISTRY.register("single_shot_rifle", () -> new SingleShotRifleItem());
   public static final DeferredHolder<Item, Item> ARTILLERY_SOLID_SHELL = REGISTRY.register("artillery_solid_shell", () -> new ArtillerySolidShellItem());
   public static final DeferredHolder<Item, Item> CRUDE_OIL_BUCKET = REGISTRY.register("crude_oil_bucket", () -> new CrudeOilItem());
   public static final DeferredHolder<Item, Item> OIL_BUCKET = REGISTRY.register("oil_bucket", () -> new OilItem());
   public static final DeferredHolder<Item, Item> DIESEL_BUCKET = REGISTRY.register("diesel_bucket", () -> new DieselItem());
   public static final DeferredHolder<Item, Item> KEROSENE_BUCKET = REGISTRY.register("kerosene_bucket", () -> new KeroseneItem());
   public static final DeferredHolder<Item, Item> PETROLIUM_BUCKET = REGISTRY.register("petrolium_bucket", () -> new PetroliumItem());
   public static final DeferredHolder<Item, Item> MEDIUM_PROJECTILE = REGISTRY.register("medium_projectile", () -> new MediumProjectileItem());
   public static final DeferredHolder<Item, Item> LARGE_PROJECTILE = REGISTRY.register("large_projectile", () -> new LargeProjectileItem());
   public static final DeferredHolder<Item, Item> EXTRA_LARGE_PROJECTILE = REGISTRY.register("extra_large_projectile", () -> new ExtraLargeProjectileItem());
   public static final DeferredHolder<Item, Item> HUGE_PROJECTILE = REGISTRY.register("huge_projectile", () -> new HugeProjectileItem());
   public static final DeferredHolder<Item, Item> HOLLOWED_EXTRA_LARGE_PROJECTILE = REGISTRY.register(
      "hollowed_extra_large_projectile", () -> new HollowedExtraLargeProjectileItem()
   );
   public static final DeferredHolder<Item, Item> HOLLOWED_HUGE_PROJECTILE = REGISTRY.register("hollowed_huge_projectile", () -> new HollowedHugeProjectileItem());
   public static final DeferredHolder<Item, Item> FOUNDRY_TEMPLATE = REGISTRY.register("foundry_template", () -> new FoundryTemplateItem());
   public static final DeferredHolder<Item, Item> COMPONENT_FOUNDRY_TEMPLATE = REGISTRY.register(
      "component_foundry_template", () -> new ComponentFoundryTemplateItem()
   );
   public static final DeferredHolder<Item, Item> CYLINDER_FOUNDRY_TEMPLATE = REGISTRY.register("cylinder_foundry_template", () -> new CylinderFoundryTemplateItem());
   public static final DeferredHolder<Item, Item> SMALL_PROJECTILE_FOUNDRY_TEMPLATE = REGISTRY.register(
      "small_projectile_foundry_template", () -> new SmallProjectileFoundryTemplateItem()
   );
   public static final DeferredHolder<Item, Item> MEDIUM_PROJECTILE_FOUNDRY_TEMPLATE = REGISTRY.register(
      "medium_projectile_foundry_template", () -> new MediumProjectileFoundryTemplateItem()
   );
   public static final DeferredHolder<Item, Item> LARGE_PROJECTILE_FOUNDRY_TEMPLATE = REGISTRY.register(
      "large_projectile_foundry_template", () -> new LargeProjectileFoundryTemplateItem()
   );
   public static final DeferredHolder<Item, Item> EXTRA_LARGE_PROJECTILE_TEMPLATE = REGISTRY.register(
      "extra_large_projectile_template", () -> new ExtraLargeProjectileTemplateItem()
   );
   public static final DeferredHolder<Item, Item> HUGE_PROJECTILE_FOUNDRY_TEMPLATE = REGISTRY.register(
      "huge_projectile_foundry_template", () -> new HugeProjectileFoundryTemplateItem()
   );
   public static final DeferredHolder<Item, Item> SMALL_UNBORED_BARREL = REGISTRY.register("small_unbored_barrel", () -> new SmallUnboredBarrelItem());
   public static final DeferredHolder<Item, Item> MEDIUM_UNBORED_BARREL = REGISTRY.register("medium_unbored_barrel", () -> new MediumUnboredBarrelItem());
   public static final DeferredHolder<Item, Item> LARGE_UNBORED_BARREL = REGISTRY.register("large_unbored_barrel", () -> new LargeUnboredBarrelItem());
   public static final DeferredHolder<Item, Item> HUGE_UNBORED_BARREL = REGISTRY.register("huge_unbored_barrel", () -> new HugeUnboredBarrelItem());
   public static final DeferredHolder<Item, Item> SMALL_UNBORED_CANNON_BARREL = REGISTRY.register(
      "small_unbored_cannon_barrel", () -> new SmallUnboredCannonBarrelItem()
   );
   public static final DeferredHolder<Item, Item> MEDIUM_UNBORED_CANNON_BARREL = REGISTRY.register(
      "medium_unbored_cannon_barrel", () -> new MediumUnboredCannonBarrelItem()
   );
   public static final DeferredHolder<Item, Item> LARGE_UNBORED_CANNON_BARREL = REGISTRY.register(
      "large_unbored_cannon_barrel", () -> new LargeUnboredCannonBarrelItem()
   );
   public static final DeferredHolder<Item, Item> HUGE_UNBORED_CANNON_BARREL = REGISTRY.register(
      "huge_unbored_cannon_barrel", () -> new HugeUnboredCannonBarrelItem()
   );
   public static final DeferredHolder<Item, Item> LARGE_FOUNDRY_TEMPLATE = REGISTRY.register("large_foundry_template", () -> new LargeFoundryTemplateItem());
   public static final DeferredHolder<Item, Item> SMALL_BARREL_TEMPLATE = REGISTRY.register("small_barrel_template", () -> new SmallBarrelTemplateItem());
   public static final DeferredHolder<Item, Item> MEDIUM_BARREL_TEMPLATE = REGISTRY.register("medium_barrel_template", () -> new MediumBarrelTemplateItem());
   public static final DeferredHolder<Item, Item> LARGE_BARREL_TEMPLATE = REGISTRY.register("large_barrel_template", () -> new LargeBarrelTemplateItem());
   public static final DeferredHolder<Item, Item> HUGE_BARREL_FOUNDRY_TEMPLATE = REGISTRY.register(
      "huge_barrel_foundry_template", () -> new HugeBarrelFoundryTemplateItem()
   );
   public static final DeferredHolder<Item, Item> SMALL_CANNON_FOUNDRY_TEMPLATE = REGISTRY.register(
      "small_cannon_foundry_template", () -> new SmallCannonFoundryTemplateItem()
   );
   public static final DeferredHolder<Item, Item> MEDIUM_CANNON_FOUNDRY_TEMPLATE = REGISTRY.register(
      "medium_cannon_foundry_template", () -> new MediumCannonFoundryTemplateItem()
   );
   public static final DeferredHolder<Item, Item> LARGE_CANNON_FOUNDRY_TEMPLATE = REGISTRY.register(
      "large_cannon_foundry_template", () -> new LargeCannonFoundryTemplateItem()
   );
   public static final DeferredHolder<Item, Item> HUGE_CANNON_FOUNDRY_TEMPLATE = REGISTRY.register(
      "huge_cannon_foundry_template", () -> new HugeCannonFoundryTemplateItem()
   );
   public static final DeferredHolder<Item, Item> SMALL_BORED_BARREL = REGISTRY.register("small_bored_barrel", () -> new SmallBoredBarrelItem());
   public static final DeferredHolder<Item, Item> MEDIUM_BORED_BARREL = REGISTRY.register("medium_bored_barrel", () -> new MediumBoredBarrelItem());
   public static final DeferredHolder<Item, Item> LARGE_BORED_BARREL = REGISTRY.register("large_bored_barrel", () -> new LargeBoredBarrelItem());
   public static final DeferredHolder<Item, Item> HUGE_BORED_BARREL = REGISTRY.register("huge_bored_barrel", () -> new HugeBoredBarrelItem());
   public static final DeferredHolder<Item, Item> INCENDIARY_GRENADE = REGISTRY.register("incendiary_grenade", () -> new IncendiaryGrenadeItem());
   public static final DeferredHolder<Item, Item> INVISIBLEITEM = REGISTRY.register("invisibleitem", () -> new InvisibleitemItem());
   public static final DeferredHolder<Item, Item> RADIOACTIVE_ASH = block(CrustyChunksModBlocks.RADIOACTIVE_ASH);
   public static final DeferredHolder<Item, Item> GEIGER_COUNTER = REGISTRY.register("geiger_counter", () -> new GeigerCounterItem());
   public static final DeferredHolder<Item, Item> RADIOACTIVE_ASH_FULL_BLOCK = block(CrustyChunksModBlocks.RADIOACTIVE_ASH_FULL_BLOCK);
   public static final DeferredHolder<Item, Item> ASPHALT = block(CrustyChunksModBlocks.ASPHALT);
   public static final DeferredHolder<Item, Item> ROCKET_POD = block(CrustyChunksModBlocks.ROCKET_POD);
   public static final DeferredHolder<Item, Item> ROCKET_POD_CHAMBER = block(CrustyChunksModBlocks.ROCKET_POD_CHAMBER);
   public static final DeferredHolder<Item, Item> JET_EXHAUST = block(CrustyChunksModBlocks.JET_EXHAUST);
   public static final DeferredHolder<Item, Item> JET_TURBINE = block(CrustyChunksModBlocks.JET_TURBINE);
   public static final DeferredHolder<Item, Item> JET_COMPRESSOR = block(CrustyChunksModBlocks.JET_COMPRESSOR);
   public static final DeferredHolder<Item, Item> SHEET_METAL_SLAB = block(CrustyChunksModBlocks.SHEET_METAL_SLAB);
   public static final DeferredHolder<Item, Item> SHEET_METAL_STAIRS = block(CrustyChunksModBlocks.SHEET_METAL_STAIRS);
   public static final DeferredHolder<Item, Item> VOLATILE_DUST = REGISTRY.register("volatile_dust", () -> new VolatileDustItem());
   public static final DeferredHolder<Item, Item> PROPELLENT = REGISTRY.register("propellent", () -> new PropellentItem());
   public static final DeferredHolder<Item, Item> LARGE_VOLATILE_PILE = REGISTRY.register("large_volatile_pile", () -> new LargeVolatilePileItem());
   public static final DeferredHolder<Item, Item> IMPACT_FUZE = REGISTRY.register("impact_fuze", () -> new ImpactFuzeItem());
   public static final DeferredHolder<Item, Item> TIMED_FUZE = REGISTRY.register("timed_fuze", () -> new TimedFuzeItem());
   public static final DeferredHolder<Item, Item> LMG_ANIMATED = REGISTRY.register("lmg_animated", () -> new LMGAnimatedItem());
   public static final DeferredHolder<Item, Item> LMG_MAGAZINE = REGISTRY.register("lmg_magazine", () -> new LMGMagazineItem());
   public static final DeferredHolder<Item, Item> BURST_RIFLE = REGISTRY.register("burst_rifle", () -> new BurstRifleItem());
   public static final DeferredHolder<Item, Item> INCENDIARY_BOTTLE = REGISTRY.register("incendiary_bottle", () -> new IncendiaryBottleItem());
   public static final DeferredHolder<Item, Item> MORTAR = block(CrustyChunksModBlocks.MORTAR);
   public static final DeferredHolder<Item, Item> MORTAR_SHELL = REGISTRY.register("mortar_shell", () -> new MortarShellItem());
   public static final DeferredHolder<Item, Item> AIMER_NODE = block(CrustyChunksModBlocks.AIMER_NODE);
   public static final DeferredHolder<Item, Item> SAND_BAGS = block(CrustyChunksModBlocks.SAND_BAGS);
   public static final DeferredHolder<Item, Item> BODY_ARMOR_CHESTPLATE = REGISTRY.register("body_armor_chestplate", () -> new BodyArmorItem.Chestplate());
   public static final DeferredHolder<Item, Item> AUTO_PISTOL = REGISTRY.register("auto_pistol", () -> new AutoPistolItem());
   public static final DeferredHolder<Item, Item> SIREN = block(CrustyChunksModBlocks.SIREN);
   public static final DeferredHolder<Item, Item> FIRING_PIN = REGISTRY.register("firing_pin", () -> new FiringPinItem());
   public static final DeferredHolder<Item, Item> WEAPON_BOLT = REGISTRY.register("weapon_bolt", () -> new WeaponBoltItem());
   public static final DeferredHolder<Item, Item> FIRING_MECHANISM = REGISTRY.register("firing_mechanism", () -> new FiringMechanismItem());
   public static final DeferredHolder<Item, Item> PISTOL_RECEIVER = REGISTRY.register("pistol_receiver", () -> new PistolReceiverItem());
   public static final DeferredHolder<Item, Item> REVOLVER_RECEIVER = REGISTRY.register("revolver_receiver", () -> new RevolverReceiverItem());
   public static final DeferredHolder<Item, Item> ADVANCED_PISTOL_RECEIVER = REGISTRY.register("advanced_pistol_receiver", () -> new AdvancedPistolReceiverItem());
   public static final DeferredHolder<Item, Item> BASIC_RECEIVER = REGISTRY.register("basic_receiver", () -> new BasicReceiverItem());
   public static final DeferredHolder<Item, Item> BOLT_ACTION_RECEIVER = REGISTRY.register("bolt_action_receiver", () -> new BoltActionReceiverItem());
   public static final DeferredHolder<Item, Item> AUTOMATIC_RIFLE_RECEIVER = REGISTRY.register("automatic_rifle_receiver", () -> new AutomaticRifleReceiverItem());
   public static final DeferredHolder<Item, Item> ADVANCED_AUTOMATIC_RIFLE_RECEIVER = REGISTRY.register(
      "advanced_automatic_rifle_receiver", () -> new AdvancedAutomaticRifleReceiverItem()
   );
   public static final DeferredHolder<Item, Item> SMG_RECEIVER = REGISTRY.register("smg_receiver", () -> new SMGReceiverItem());
   public static final DeferredHolder<Item, Item> MG_RECEIVER = REGISTRY.register("mg_receiver", () -> new MGReceiverItem());
   public static final DeferredHolder<Item, Item> RIFLE_STOCK = REGISTRY.register("rifle_stock", () -> new RifleStockItem());
   public static final DeferredHolder<Item, Item> BREAK_ACTION_SHOTGUN_ANIMATED = REGISTRY.register(
      "break_action_shotgun_animated", () -> new BreakActionShotgunAnimatedItem()
   );
   public static final DeferredHolder<Item, Item> GREEN_ARMOR = block(CrustyChunksModBlocks.GREEN_ARMOR);
   public static final DeferredHolder<Item, Item> GREEN_ARMOR_SLAB = block(CrustyChunksModBlocks.GREEN_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> GREEN_ARMOR_STAIRS = block(CrustyChunksModBlocks.GREEN_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> GREEN_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.GREEN_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> GREEN_ARMOR_OPTIC = block(CrustyChunksModBlocks.GREEN_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> YELLOW_ARMOR = block(CrustyChunksModBlocks.YELLOW_ARMOR);
   public static final DeferredHolder<Item, Item> YELLOW_ARMOR_SLAB = block(CrustyChunksModBlocks.YELLOW_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> YELLOW_ARMOR_STAIRS = block(CrustyChunksModBlocks.YELLOW_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> YELLOW_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.YELLOW_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> YELLOW_ARMOR_OPTIC = block(CrustyChunksModBlocks.YELLOW_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> BROWN_ARMOR = block(CrustyChunksModBlocks.BROWN_ARMOR);
   public static final DeferredHolder<Item, Item> BROWN_ARMOR_SLAB = block(CrustyChunksModBlocks.BROWN_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> BROWN_ARMOR_STAIRS = block(CrustyChunksModBlocks.BROWN_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> BROWN_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.BROWN_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> BROWN_ARMOR_OPTIC = block(CrustyChunksModBlocks.BROWN_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> WHITE_ARMOR = block(CrustyChunksModBlocks.WHITE_ARMOR);
   public static final DeferredHolder<Item, Item> WHITE_ARMOR_SLAB = block(CrustyChunksModBlocks.WHITE_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> WHITE_ARMOR_STAIRS = block(CrustyChunksModBlocks.WHITE_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> WHITE_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.WHITE_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> WHITE_ARMOR_OPTIC = block(CrustyChunksModBlocks.WHITE_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> LARGE_MAGAZINE = REGISTRY.register("large_magazine", () -> new LargeMagazineItem());
   public static final DeferredHolder<Item, Item> BATTLE_RIFLE = REGISTRY.register("battle_rifle", () -> new BattleRifleItem());
   public static final DeferredHolder<Item, Item> IRONGEAR = REGISTRY.register("irongear", () -> new IrongearItem());
   public static final DeferredHolder<Item, Item> ROTARY_AUTO_CANNON = block(CrustyChunksModBlocks.ROTARY_AUTO_CANNON);
   public static final DeferredHolder<Item, Item> RAC_BARREL = REGISTRY.register(
      CrustyChunksModBlocks.RAC_BARREL.getId().getPath(), () -> new RACBarrelDisplayItem((Block)CrustyChunksModBlocks.RAC_BARREL.get(), new Properties())
   );
   public static final DeferredHolder<Item, Item> LIGHT_AUTOCANNON = block(CrustyChunksModBlocks.LIGHT_AUTOCANNON);
   public static final DeferredHolder<Item, Item> MACHINE_CARBINE = REGISTRY.register("machine_carbine", () -> new MachineCarbineItem());
   public static final DeferredHolder<Item, Item> SMOKE_MORTAR_SHELL = REGISTRY.register("smoke_mortar_shell", () -> new SmokeMortarShellItem());
   public static final DeferredHolder<Item, Item> SMOKE_LAUNCHER = block(CrustyChunksModBlocks.SMOKE_LAUNCHER);
   public static final DeferredHolder<Item, Item> HE_PROJECTILE = REGISTRY.register("he_projectile", () -> new HEProjectileItem());
   public static final DeferredHolder<Item, Item> HEAT_PROJECTILE = REGISTRY.register("heat_projectile", () -> new HEATProjectileItem());
   public static final DeferredHolder<Item, Item> APFSDS_PROJECTILE = REGISTRY.register("apfsds_projectile", () -> new APFSDSProjectileItem());
   public static final DeferredHolder<Item, Item> SHAPED_CHARGE_FUZE = REGISTRY.register("shaped_charge_fuze", () -> new ShapedChargeFuzeItem());
   public static final DeferredHolder<Item, Item> FLAK_PROJECTILE = REGISTRY.register("flak_projectile", () -> new FlakProjectileItem());
   public static final DeferredHolder<Item, Item> HOLLOWED_LARGE_PROJECTILE = REGISTRY.register("hollowed_large_projectile", () -> new HollowedLargeProjectileItem());
   public static final DeferredHolder<Item, Item> SMALL_HE_PROJECTILE = REGISTRY.register("small_he_projectile", () -> new SmallHEProjectileItem());
   public static final DeferredHolder<Item, Item> SMOKE_AGENT = REGISTRY.register("smoke_agent", () -> new SmokeAgentItem());
   public static final DeferredHolder<Item, Item> FIRE_AGENT = REGISTRY.register("fire_agent", () -> new FireAgentItem());
   public static final DeferredHolder<Item, Item> IMPLOSION_MODULE = REGISTRY.register("implosion_module", () -> new ImplosionModuleItem());
   public static final DeferredHolder<Item, Item> URANIUM_ORE = block(CrustyChunksModBlocks.URANIUM_ORE);
   public static final DeferredHolder<Item, Item> RED_ARMOR = block(CrustyChunksModBlocks.RED_ARMOR);
   public static final DeferredHolder<Item, Item> RED_ARMOR_SLAB = block(CrustyChunksModBlocks.RED_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> RED_ARMOR_STAIRS = block(CrustyChunksModBlocks.RED_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> RED_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.RED_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> RED_ARMOR_OPTIC = block(CrustyChunksModBlocks.RED_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> BLACK_ARMOR = block(CrustyChunksModBlocks.BLACK_ARMOR);
   public static final DeferredHolder<Item, Item> BLACK_ARMOR_SLAB = block(CrustyChunksModBlocks.BLACK_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> BLACK_ARMOR_STAIRS = block(CrustyChunksModBlocks.BLACK_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> BLACK_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.BLACK_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> BLACK_ARMOR_OPTIC = block(CrustyChunksModBlocks.BLACK_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> CYAN_ARMOR = block(CrustyChunksModBlocks.CYAN_ARMOR);
   public static final DeferredHolder<Item, Item> CYAN_ARMOR_SLAB = block(CrustyChunksModBlocks.CYAN_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> CYAN_ARMOR_STAIRS = block(CrustyChunksModBlocks.CYAN_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> CYAN_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.CYAN_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> BLUE_ARMOR = block(CrustyChunksModBlocks.BLUE_ARMOR);
   public static final DeferredHolder<Item, Item> BLUE_ARMOR_SLAB = block(CrustyChunksModBlocks.BLUE_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> BLUE_ARMOR_STAIRS = block(CrustyChunksModBlocks.BLUE_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> BLUE_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.BLUE_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> ORANGE_ARMOR = block(CrustyChunksModBlocks.ORANGE_ARMOR);
   public static final DeferredHolder<Item, Item> ORANGE_ARMOR_SLAB = block(CrustyChunksModBlocks.ORANGE_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> ORANGE_ARMOR_STAIRS = block(CrustyChunksModBlocks.ORANGE_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> ORANGE_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.ORANGE_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> ORANGE_ARMOR_OPTIC = block(CrustyChunksModBlocks.ORANGE_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> GRAY_ARMOR = block(CrustyChunksModBlocks.GRAY_ARMOR);
   public static final DeferredHolder<Item, Item> GRAY_ARMOR_SLAB = block(CrustyChunksModBlocks.GRAY_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> GRAY_ARMOR_STAIRS = block(CrustyChunksModBlocks.GRAY_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> GRAY_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.GRAY_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> GRAY_ARMOR_OPTIC = block(CrustyChunksModBlocks.GRAY_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> LIME_ARMOR = block(CrustyChunksModBlocks.LIME_ARMOR);
   public static final DeferredHolder<Item, Item> LIME_ARMOR_SLAB = block(CrustyChunksModBlocks.LIME_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> LIME_ARMOR_STAIRS = block(CrustyChunksModBlocks.LIME_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> LIME_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.LIME_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> LIME_ARMOR_OPTIC = block(CrustyChunksModBlocks.LIME_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> LIGHT_GRAY_ARMOR = block(CrustyChunksModBlocks.LIGHT_GRAY_ARMOR);
   public static final DeferredHolder<Item, Item> LIGHT_GRAY_ARMOR_SLAB = block(CrustyChunksModBlocks.LIGHT_GRAY_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> LIGHT_GRAY_ARMOR_STAIRS = block(CrustyChunksModBlocks.LIGHT_GRAY_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> LIGHT_GRAY_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.LIGHT_GRAY_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> LIGHT_GRAY_ARMOR_OPTIC = block(CrustyChunksModBlocks.LIGHT_GRAY_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> LIGHT_BLUE_ARMOR = block(CrustyChunksModBlocks.LIGHT_BLUE_ARMOR);
   public static final DeferredHolder<Item, Item> LIGHT_BLUE_ARMOR_SLAB = block(CrustyChunksModBlocks.LIGHT_BLUE_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> LIGHT_BLUE_ARMOR_STAIRS = block(CrustyChunksModBlocks.LIGHT_BLUE_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> LIGHT_BLUE_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.LIGHT_BLUE_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> LIGHT_BLUE_ARMOR_OPTIC = block(CrustyChunksModBlocks.LIGHT_BLUE_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> PLUTONIUM_INGOT = REGISTRY.register("plutonium_ingot", () -> new PlutoniumIngotItem());
   public static final DeferredHolder<Item, Item> REFINERY = block(CrustyChunksModBlocks.REFINERY);
   public static final DeferredHolder<Item, Item> REFINERY_TOWER = block(CrustyChunksModBlocks.REFINERY_TOWER);
   public static final DeferredHolder<Item, Item> ADVANCED_COMPONENT = REGISTRY.register("advanced_component", () -> new AdvancedComponentItem());
   public static final DeferredHolder<Item, Item> SHALE_OIL = REGISTRY.register("shale_oil", () -> new ShaleOilItem());
   public static final DeferredHolder<Item, Item> BLAST_CLAY = REGISTRY.register("blast_clay", () -> new BlastClayItem());
   public static final DeferredHolder<Item, Item> SULFUR_ORE = block(CrustyChunksModBlocks.SULFUR_ORE);
   public static final DeferredHolder<Item, Item> SULFUR = REGISTRY.register("sulfur", () -> new SulfurItem());
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATE = REGISTRY.register("aluminum_plate", () -> new AluminumPlateItem());
   public static final DeferredHolder<Item, Item> PLUTONIUM_NUGGET = REGISTRY.register("plutonium_nugget", () -> new PlutoniumNuggetItem());
   public static final DeferredHolder<Item, Item> PLUTONIUM_CORE = REGISTRY.register("plutonium_core", () -> new PlutoniumCoreItem());
   public static final DeferredHolder<Item, Item> FISSION_CORE = REGISTRY.register("fission_core", () -> new FissionCoreItem());
   public static final DeferredHolder<Item, Item> SHIELDING_COMPONENT = REGISTRY.register("shielding_component", () -> new ShieldingComponentItem());
   public static final DeferredHolder<Item, Item> IMPLOSION_LENS = REGISTRY.register("implosion_lens", () -> new ImplosionLensItem());
   public static final DeferredHolder<Item, Item> GIANT_COIL = block(CrustyChunksModBlocks.GIANT_COIL);
   public static final DeferredHolder<Item, Item> REACTION_CHAMBER = block(CrustyChunksModBlocks.REACTION_CHAMBER);
   public static final DeferredHolder<Item, Item> BREEDER_REACTOR_PORT = block(CrustyChunksModBlocks.BREEDER_REACTOR_PORT);
   public static final DeferredHolder<Item, Item> REACTIONCOMPONENT = REGISTRY.register("reactioncomponent", () -> new ReactioncomponentItem());
   public static final DeferredHolder<Item, Item> ALUMINUM_TINY_DUST = REGISTRY.register("aluminum_tiny_dust", () -> new AluminumTinyDustItem());
   public static final DeferredHolder<Item, Item> OIL_FIREBOX = block(CrustyChunksModBlocks.OIL_FIREBOX);
   public static final DeferredHolder<Item, Item> DECIMATOR_SPAWN_EGG = REGISTRY.register(
      "decimator_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.DECIMATOR, -12757452, -13553094, new Properties())
   );
   public static final DeferredHolder<Item, Item> STRIKER_SPAWN_EGG = REGISTRY.register(
      "striker_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.STRIKER, -11764658, -4698803, new Properties())
   );
   public static final DeferredHolder<Item, Item> FLAMER_SPAWN_EGG = REGISTRY.register(
      "flamer_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.FLAMER, -11127756, -13553094, new Properties())
   );
   public static final DeferredHolder<Item, Item> HUNTER_SPAWN_EGG = REGISTRY.register(
      "hunter_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.HUNTER, -10057884, -2039584, new Properties())
   );
   public static final DeferredHolder<Item, Item> SUMMONATOR = block(CrustyChunksModBlocks.SUMMONATOR);
   public static final DeferredHolder<Item, Item> SUMMONATOR_MODULE = block(CrustyChunksModBlocks.SUMMONATOR_MODULE);
   public static final DeferredHolder<Item, Item> STEEL_DOOR = doubleBlock(CrustyChunksModBlocks.STEEL_DOOR);
   public static final DeferredHolder<Item, Item> OVERGROWN_REENFORCED_CONCRETE = block(CrustyChunksModBlocks.OVERGROWN_REENFORCED_CONCRETE);
   public static final DeferredHolder<Item, Item> RUSTY_BLOCK = block(CrustyChunksModBlocks.RUSTY_BLOCK);
   public static final DeferredHolder<Item, Item> CRACKED_CONCRETE_WALL = block(CrustyChunksModBlocks.CRACKED_CONCRETE_WALL);
   public static final DeferredHolder<Item, Item> FRACTURED_CONCRETE_WALL = block(CrustyChunksModBlocks.FRACTURED_CONCRETE_WALL);
   public static final DeferredHolder<Item, Item> DAMAGED_CONCRETE_WALL = block(CrustyChunksModBlocks.DAMAGED_CONCRETE_WALL);
   public static final DeferredHolder<Item, Item> DESTROYED_CONCRETE_WALL = block(CrustyChunksModBlocks.DESTROYED_CONCRETE_WALL);
   public static final DeferredHolder<Item, Item> RUSTY_SLAB = block(CrustyChunksModBlocks.RUSTY_SLAB);
   public static final DeferredHolder<Item, Item> RUSTY_STAIRS = block(CrustyChunksModBlocks.RUSTY_STAIRS);
   public static final DeferredHolder<Item, Item> RUSTY_TRAPDOOR = block(CrustyChunksModBlocks.RUSTY_TRAPDOOR);
   public static final DeferredHolder<Item, Item> ROBOT_CHUTE = block(CrustyChunksModBlocks.ROBOT_CHUTE);
   public static final DeferredHolder<Item, Item> STRUCTURAL_CONCRETE = block(CrustyChunksModBlocks.STRUCTURAL_CONCRETE);
   public static final DeferredHolder<Item, Item> STEEL_TRUSS = block(CrustyChunksModBlocks.STEEL_TRUSS);
   public static final DeferredHolder<Item, Item> SUMMONATOR_ACTIVE = block(CrustyChunksModBlocks.SUMMONATOR_ACTIVE);
   public static final DeferredHolder<Item, Item> ACTIVE_ROBOT_CHUTE = block(CrustyChunksModBlocks.ACTIVE_ROBOT_CHUTE);
   public static final DeferredHolder<Item, Item> HAND_DRILL = REGISTRY.register("hand_drill", () -> new HandDrillItem());
   public static final DeferredHolder<Item, Item> BRASS_FITTING = REGISTRY.register("brass_fitting", () -> new BrassFittingItem());
   public static final DeferredHolder<Item, Item> COMBUSTION_CYLINDER = REGISTRY.register("combustion_cylinder", () -> new CombustionCylinderItem());
   public static final DeferredHolder<Item, Item> ENGINE_COMPONENT = REGISTRY.register("engine_component", () -> new EngineComponentItem());
   public static final DeferredHolder<Item, Item> SMALL_ENGINE = REGISTRY.register("small_engine", () -> new SmallEngineItem());
   public static final DeferredHolder<Item, Item> BERYLLIUM_ORE = block(CrustyChunksModBlocks.BERYLLIUM_ORE);
   public static final DeferredHolder<Item, Item> RAW_BERYLLIUM = REGISTRY.register("raw_beryllium", () -> new RawBerylliumItem());
   public static final DeferredHolder<Item, Item> BERYLLIUM_DUST = REGISTRY.register("beryllium_dust", () -> new BerylliumDustItem());
   public static final DeferredHolder<Item, Item> BERYLLIUM_INGOT = REGISTRY.register("beryllium_ingot", () -> new BerylliumIngotItem());
   public static final DeferredHolder<Item, Item> RAW_BERYLLIUM_BLOCK = block(CrustyChunksModBlocks.RAW_BERYLLIUM_BLOCK);
   public static final DeferredHolder<Item, Item> NEUTRON_REFLECTOR = REGISTRY.register("neutron_reflector", () -> new NeutronReflectorItem());
   public static final DeferredHolder<Item, Item> BREEDER_REACTOR_CORE = block(CrustyChunksModBlocks.BREEDER_REACTOR_CORE);
   public static final DeferredHolder<Item, Item> BREEDER_REACTOR_INTERFACE = block(CrustyChunksModBlocks.BREEDER_REACTOR_INTERFACE);
   public static final DeferredHolder<Item, Item> REACTOR_CASING = block(CrustyChunksModBlocks.REACTOR_CASING);
   public static final DeferredHolder<Item, Item> CONTROL_ROD = block(CrustyChunksModBlocks.CONTROL_ROD);
   public static final DeferredHolder<Item, Item> EMPTY_FUEL_RODS = block(CrustyChunksModBlocks.EMPTY_FUEL_RODS);
   public static final DeferredHolder<Item, Item> FUEL_RODS_1 = block(CrustyChunksModBlocks.FUEL_RODS_1);
   public static final DeferredHolder<Item, Item> FUEL_RODS_2 = block(CrustyChunksModBlocks.FUEL_RODS_2);
   public static final DeferredHolder<Item, Item> FUEL_RODS_3 = block(CrustyChunksModBlocks.FUEL_RODS_3);
   public static final DeferredHolder<Item, Item> FUEL_RODS_4 = block(CrustyChunksModBlocks.FUEL_RODS_4);
   public static final DeferredHolder<Item, Item> FUEL_ROD = REGISTRY.register("fuel_rod", () -> new FuelRodItem());
   public static final DeferredHolder<Item, Item> SOLID_SHELL = REGISTRY.register("solid_shell", () -> new SolidShellItem());
   public static final DeferredHolder<Item, Item> NVD_HELMET_HELMET = REGISTRY.register("nvd_helmet_helmet", () -> new NVDHelmetItem.Helmet());
   public static final DeferredHolder<Item, Item> TECH_COMPONENT = REGISTRY.register("tech_component", () -> new TechComponentItem());
   public static final DeferredHolder<Item, Item> SMALL_HOLLOW_POINT_BULLET = REGISTRY.register("small_hollow_point_bullet", () -> new SmallHollowPointBulletItem());
   public static final DeferredHolder<Item, Item> SMALL_STEALTH_BULLET = REGISTRY.register("small_stealth_bullet", () -> new SmallStealthBulletItem());
   public static final DeferredHolder<Item, Item> MEDIUM_AP_BULLET = REGISTRY.register("medium_ap_bullet", () -> new MediumAPBulletItem());
   public static final DeferredHolder<Item, Item> MEDIUM_STEALTH_BULLET = REGISTRY.register("medium_stealth_bullet", () -> new MediumStealthBulletItem());
   public static final DeferredHolder<Item, Item> AP_LARGE_BULLET = REGISTRY.register("ap_large_bullet", () -> new APLargeBulletItem());
   public static final DeferredHolder<Item, Item> STEALTH_LARGE_BULLET = REGISTRY.register("stealth_large_bullet", () -> new StealthLargeBulletItem());
   public static final DeferredHolder<Item, Item> TRANSPARENT_ITEM = REGISTRY.register("transparent_item", () -> new TransparentItemItem());
   public static final DeferredHolder<Item, Item> BIRD_SHOT = REGISTRY.register("bird_shot", () -> new BirdShotItem());
   public static final DeferredHolder<Item, Item> SLUG_SHELL = REGISTRY.register("slug_shell", () -> new SlugShellItem());
   public static final DeferredHolder<Item, Item> RIFLER_SPAWN_EGG = REGISTRY.register(
      "rifler_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.RIFLER, -11764658, -4698803, new Properties())
   );
   public static final DeferredHolder<Item, Item> FLARE_PISTOL = REGISTRY.register("flare_pistol", () -> new FlarePistolItem());
   public static final DeferredHolder<Item, Item> FLARE_CHARGE = REGISTRY.register("flare_charge", () -> new FlareChargeItem());
   public static final DeferredHolder<Item, Item> SCORCH_DIRT = block(CrustyChunksModBlocks.SCORCH_DIRT);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING = block(CrustyChunksModBlocks.ALUMINUM_PLATING);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL);
   public static final DeferredHolder<Item, Item> GLASS_TRAPDOOR = block(CrustyChunksModBlocks.GLASS_TRAPDOOR);
   public static final DeferredHolder<Item, Item> REINFORCED_GLASS = block(CrustyChunksModBlocks.REINFORCED_GLASS);
   public static final DeferredHolder<Item, Item> REINFORCED_GLASS_TRAPDOOR = block(CrustyChunksModBlocks.REINFORCED_GLASS_TRAPDOOR);
   public static final DeferredHolder<Item, Item> AFTER_BURNER = block(CrustyChunksModBlocks.AFTER_BURNER);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL);
   public static final DeferredHolder<Item, Item> GAS_BOMB = block(CrustyChunksModBlocks.GAS_BOMB);
   public static final DeferredHolder<Item, Item> TOXIC_AGENT = REGISTRY.register("toxic_agent", () -> new ToxicAgentItem());
   public static final DeferredHolder<Item, Item> GAS_ARTILLERY_SHELL = REGISTRY.register("gas_artillery_shell", () -> new GasArtilleryShellItem());
   public static final DeferredHolder<Item, Item> GAS_DISPENSER = block(CrustyChunksModBlocks.GAS_DISPENSER);
   public static final DeferredHolder<Item, Item> NODE_TRIGGER = block(CrustyChunksModBlocks.NODE_TRIGGER);
   public static final DeferredHolder<Item, Item> NODE_TRIGGER_ON = block(CrustyChunksModBlocks.NODE_TRIGGER_ON);
   public static final DeferredHolder<Item, Item> COUNTERMEASURE_DISPENSER = block(CrustyChunksModBlocks.COUNTERMEASURE_DISPENSER);
   public static final DeferredHolder<Item, Item> MORTARER_SPAWN_EGG = REGISTRY.register(
      "mortarer_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.MORTARER, -13035239, -14801636, new Properties())
   );
   public static final DeferredHolder<Item, Item> COMMANDER_SPAWN_EGG = REGISTRY.register(
      "commander_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.COMMANDER, -14601442, -65536, new Properties())
   );
   public static final DeferredHolder<Item, Item> CIWS_SPAWN_EGG = REGISTRY.register(
      "ciws_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.CIWS, -13678800, -1, new Properties())
   );
   public static final DeferredHolder<Item, Item> STEALTH_PISTOL = REGISTRY.register("stealth_pistol", () -> new StealthPistolItem());
   public static final DeferredHolder<Item, Item> WEAPON_SUPRESSOR = REGISTRY.register("weapon_supressor", () -> new WeaponSupressorItem());
   public static final DeferredHolder<Item, Item> ASPHALT_SLAB = block(CrustyChunksModBlocks.ASPHALT_SLAB);
   public static final DeferredHolder<Item, Item> ORDINANCE_CORE = block(CrustyChunksModBlocks.ORDINANCE_CORE);
   public static final DeferredHolder<Item, Item> ORDINANCE_FINS = block(CrustyChunksModBlocks.ORDINANCE_FINS);
   public static final DeferredHolder<Item, Item> ORDINANCE_THRUSTER = block(CrustyChunksModBlocks.ORDINANCE_THRUSTER);
   public static final DeferredHolder<Item, Item> ORDINANCE_HEAVY_WARHEAD = block(CrustyChunksModBlocks.ORDINANCE_HEAVY_WARHEAD);
   public static final DeferredHolder<Item, Item> ORDINANCE_INCENDIARY_WARHEAD = block(CrustyChunksModBlocks.ORDINANCE_INCENDIARY_WARHEAD);
   public static final DeferredHolder<Item, Item> ORDINANCE_INLINE_WARHEAD = block(CrustyChunksModBlocks.ORDINANCE_INLINE_WARHEAD);
   public static final DeferredHolder<Item, Item> ORDINANCE_KINETIC_HEAD = block(CrustyChunksModBlocks.ORDINANCE_KINETIC_HEAD);
   public static final DeferredHolder<Item, Item> ORDINANCE_IR_SEEKER_HEAD = block(CrustyChunksModBlocks.ORDINANCE_IR_SEEKER_HEAD);
   public static final DeferredHolder<Item, Item> ORDINANCE_CLUSTER_WARHEAD = block(CrustyChunksModBlocks.ORDINANCE_CLUSTER_WARHEAD);
   public static final DeferredHolder<Item, Item> ORDINANCE_INLINE_FISSION_WARHEAD = block(CrustyChunksModBlocks.ORDINANCE_INLINE_FISSION_WARHEAD);
   public static final DeferredHolder<Item, Item> ORDINANCE_FISSION_INITIATOR_HEAD = block(CrustyChunksModBlocks.ORDINANCE_FISSION_INITIATOR_HEAD);
   public static final DeferredHolder<Item, Item> SOLID_ROCKET_FUEL_PACK = REGISTRY.register("solid_rocket_fuel_pack", () -> new SolidRocketFuelPackItem());
   public static final DeferredHolder<Item, Item> ANCIENT_LIGHT = block(CrustyChunksModBlocks.ANCIENT_LIGHT);
   public static final DeferredHolder<Item, Item> SUMMONATION = block(CrustyChunksModBlocks.SUMMONATION);
   public static final DeferredHolder<Item, Item> OPEN_SUMMONATION = block(CrustyChunksModBlocks.OPEN_SUMMONATION);
   public static final DeferredHolder<Item, Item> LOOT_BOX = block(CrustyChunksModBlocks.LOOT_BOX);
   public static final DeferredHolder<Item, Item> GAS_MASK_HELMET = REGISTRY.register("gas_mask_helmet", () -> new GasMaskItem.Helmet());
   public static final DeferredHolder<Item, Item> DEFENSE_CORE = block(CrustyChunksModBlocks.DEFENSE_CORE);
   public static final DeferredHolder<Item, Item> ASSASSIN_SPAWN_EGG = REGISTRY.register(
      "assassin_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.ASSASSIN, -16777216, -4698803, new Properties())
   );
   public static final DeferredHolder<Item, Item> BLAST_ARMOR_HELMET = REGISTRY.register("blast_armor_helmet", () -> new BlastArmorItem.Helmet());
   public static final DeferredHolder<Item, Item> BLAST_ARMOR_CHESTPLATE = REGISTRY.register("blast_armor_chestplate", () -> new BlastArmorItem.Chestplate());
   public static final DeferredHolder<Item, Item> BLAST_ARMOR_LEGGINGS = REGISTRY.register("blast_armor_leggings", () -> new BlastArmorItem.Leggings());
   public static final DeferredHolder<Item, Item> BLAST_ARMOR_BOOTS = REGISTRY.register("blast_armor_boots", () -> new BlastArmorItem.Boots());
   public static final DeferredHolder<Item, Item> RAW_URANIUM_BLOCK = block(CrustyChunksModBlocks.RAW_URANIUM_BLOCK);
   public static final DeferredHolder<Item, Item> ERADICATOR_SPAWN_EGG = REGISTRY.register(
      "eradicator_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.ERADICATOR, -14604513, -13553094, new Properties())
   );
   public static final DeferredHolder<Item, Item> FUSION_BOMB = block(CrustyChunksModBlocks.FUSION_BOMB);
   public static final DeferredHolder<Item, Item> SCOUT_SPAWN_EGG = REGISTRY.register(
      "scout_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.SCOUT, -8343168, -4935761, new Properties())
   );
   public static final DeferredHolder<Item, Item> RAIDSCOUT_SPAWN_EGG = REGISTRY.register(
      "raidscout_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.RAIDSCOUT, -8343168, -4935761, new Properties())
   );
   public static final DeferredHolder<Item, Item> BERYLLIUM_BLOCK = block(CrustyChunksModBlocks.BERYLLIUM_BLOCK);
   public static final DeferredHolder<Item, Item> POLISHED_TRINITITE = block(CrustyChunksModBlocks.POLISHED_TRINITITE);
   public static final DeferredHolder<Item, Item> SULFUR_BLOCK = block(CrustyChunksModBlocks.SULFUR_BLOCK);
   public static final DeferredHolder<Item, Item> WORKER_SPAWN_EGG = REGISTRY.register(
      "worker_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.WORKER, -13150152, -7716543, new Properties())
   );
   public static final DeferredHolder<Item, Item> ANCIENT_WELL = block(CrustyChunksModBlocks.ANCIENT_WELL);
   public static final DeferredHolder<Item, Item> PASSENGER_SEAT = block(CrustyChunksModBlocks.PASSENGER_SEAT);
   public static final DeferredHolder<Item, Item> BREECH_RIFLE = REGISTRY.register("breech_rifle", () -> new BreechRifleItem());
   public static final DeferredHolder<Item, Item> EXTRA_LARGE_BULLET = REGISTRY.register("extra_large_bullet", () -> new ExtraLargeBulletItem());
   public static final DeferredHolder<Item, Item> SCOPED_BREECH_RIFLE = REGISTRY.register("scoped_breech_rifle", () -> new ScopedBreechRifleItem());
   public static final DeferredHolder<Item, Item> JET_GEARBOX = block(CrustyChunksModBlocks.JET_GEARBOX);
   public static final DeferredHolder<Item, Item> GENERATOR = block(CrustyChunksModBlocks.GENERATOR);
   public static final DeferredHolder<Item, Item> GAS_MASK_HELMET_HELMET = REGISTRY.register("gas_mask_helmet_helmet", () -> new GasMaskHelmetItem.Helmet());
   public static final DeferredHolder<Item, Item> ERADICATION = REGISTRY.register("eradication", () -> new EradicationItem());
   public static final DeferredHolder<Item, Item> BULLET_RESISTANT_HELMET_2_HELMET = REGISTRY.register(
      "bullet_resistant_helmet_2_helmet", () -> new BulletResistantHelmet2Item.Helmet()
   );
   public static final DeferredHolder<Item, Item> BULLET_RESISTANT_HELMET_3_HELMET = REGISTRY.register(
      "bullet_resistant_helmet_3_helmet", () -> new BulletResistantHelmet3Item.Helmet()
   );
   public static final DeferredHolder<Item, Item> BULLET_RESISTANT_HELMET_4_HELMET = REGISTRY.register(
      "bullet_resistant_helmet_4_helmet", () -> new BulletResistantHelmet4Item.Helmet()
   );
   public static final DeferredHolder<Item, Item> FUEL_TANK = block(CrustyChunksModBlocks.FUEL_TANK);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_DARK_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_DARK_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_DARK_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_DARK_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_DARK_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_DARK_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_DARK_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_DARK_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_DARK_GRAY = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_DARK_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_DARK_GRAY = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_DARK_GRAY);
   public static final DeferredHolder<Item, Item> EXTENSION_SHAFT = block(CrustyChunksModBlocks.EXTENSION_SHAFT);
   public static final DeferredHolder<Item, Item> DRIVE_SHAFT = block(CrustyChunksModBlocks.DRIVE_SHAFT);
   public static final DeferredHolder<Item, Item> ENGINE_CYLLINDER = block(CrustyChunksModBlocks.ENGINE_CYLLINDER);
   public static final DeferredHolder<Item, Item> LARGE_ENGINE_SMOKESTACK = block(CrustyChunksModBlocks.LARGE_ENGINE_SMOKESTACK);
   public static final DeferredHolder<Item, Item> TINTED_GLASS_TRAPDOOR = block(CrustyChunksModBlocks.TINTED_GLASS_TRAPDOOR);
   public static final DeferredHolder<Item, Item> CYAN_ARMOR_OPTIC = block(CrustyChunksModBlocks.CYAN_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> BLUE_ARMOR_OPTIC = block(CrustyChunksModBlocks.BLUE_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> ENERGY_NODE = block(CrustyChunksModBlocks.ENERGY_NODE);
   public static final DeferredHolder<Item, Item> ENERGY_BATTERY = block(CrustyChunksModBlocks.ENERGY_BATTERY);
   public static final DeferredHolder<Item, Item> LARGE_ELECTRIC_MOTOR = block(CrustyChunksModBlocks.LARGE_ELECTRIC_MOTOR);
   public static final DeferredHolder<Item, Item> CABLE = REGISTRY.register("cable", () -> new CableItem());
   public static final DeferredHolder<Item, Item> ENERGY_METER = REGISTRY.register("energy_meter", () -> new EnergyMeterItem());
   public static final DeferredHolder<Item, Item> PINK_ARMOR = block(CrustyChunksModBlocks.PINK_ARMOR);
   public static final DeferredHolder<Item, Item> PINK_ARMOR_SLAB = block(CrustyChunksModBlocks.PINK_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> PINK_ARMOR_STAIRS = block(CrustyChunksModBlocks.PINK_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> PINK_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.PINK_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> PINK_ARMOR_OPTIC = block(CrustyChunksModBlocks.PINK_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> MAGENTA_ARMOR = block(CrustyChunksModBlocks.MAGENTA_ARMOR);
   public static final DeferredHolder<Item, Item> MAGENTA_ARMOR_SLAB = block(CrustyChunksModBlocks.MAGENTA_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> MAGENTA_ARMOR_STAIRS = block(CrustyChunksModBlocks.MAGENTA_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> MAGENTA_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.MAGENTA_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> MAGENTA_ARMOR_OPTIC = block(CrustyChunksModBlocks.MAGENTA_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> PURPLE_ARMOR = block(CrustyChunksModBlocks.PURPLE_ARMOR);
   public static final DeferredHolder<Item, Item> PURPLE_ARMOR_SLAB = block(CrustyChunksModBlocks.PURPLE_ARMOR_SLAB);
   public static final DeferredHolder<Item, Item> PURPLE_ARMOR_STAIRS = block(CrustyChunksModBlocks.PURPLE_ARMOR_STAIRS);
   public static final DeferredHolder<Item, Item> PURPLE_ARMOR_TRAPDOOR = block(CrustyChunksModBlocks.PURPLE_ARMOR_TRAPDOOR);
   public static final DeferredHolder<Item, Item> PURPLE_ARMOR_OPTIC = block(CrustyChunksModBlocks.PURPLE_ARMOR_OPTIC);
   public static final DeferredHolder<Item, Item> EXTRA_LARGE_CASING = REGISTRY.register("extra_large_casing", () -> new ExtraLargeCasingItem());
   public static final DeferredHolder<Item, Item> TURBINE_ROTOR = REGISTRY.register("turbine_rotor", () -> new TurbineRotorItem());
   public static final DeferredHolder<Item, Item> ELECTRIC_FIREBOX = block(CrustyChunksModBlocks.ELECTRIC_FIREBOX);
   public static final DeferredHolder<Item, Item> THERMAL_FURNACE = block(CrustyChunksModBlocks.THERMAL_FURNACE);
   public static final DeferredHolder<Item, Item> POWER_CELL = REGISTRY.register("power_cell", () -> new PowerCellItem());
   public static final DeferredHolder<Item, Item> POWER_REACTOR_INTERFACE = block(CrustyChunksModBlocks.POWER_REACTOR_INTERFACE);
   public static final DeferredHolder<Item, Item> POWER_REACTOR_PORT = block(CrustyChunksModBlocks.POWER_REACTOR_PORT);
   public static final DeferredHolder<Item, Item> LEVER_RIFLE = REGISTRY.register("lever_rifle", () -> new LeverRifleItem());
   public static final DeferredHolder<Item, Item> LAND_MINE = block(CrustyChunksModBlocks.LAND_MINE);
   public static final DeferredHolder<Item, Item> AI_MINE = block(CrustyChunksModBlocks.AI_MINE);
   public static final DeferredHolder<Item, Item> REAPER_SPAWN_EGG = REGISTRY.register(
      "reaper_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.REAPER, -16052206, -5684927, new Properties())
   );
   public static final DeferredHolder<Item, Item> FUSION_CORE = REGISTRY.register("fusion_core", () -> new FusionCoreItem());
   public static final DeferredHolder<Item, Item> ORDINANCE_INLINE_FUSION_WARHEAD_STAGE_1 = block(CrustyChunksModBlocks.ORDINANCE_INLINE_FUSION_WARHEAD_STAGE_1);
   public static final DeferredHolder<Item, Item> ORDINANCE_INLINE_FUSION_WARHEAD_STAGE_2 = block(CrustyChunksModBlocks.ORDINANCE_INLINE_FUSION_WARHEAD_STAGE_2);
   public static final DeferredHolder<Item, Item> BREACHER_SPAWN_EGG = REGISTRY.register(
      "breacher_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.BREACHER, -12509152, -14399962, new Properties())
   );
   public static final DeferredHolder<Item, Item> TRINITITE_SHARD = REGISTRY.register("trinitite_shard", () -> new TrinititeShardItem());
   public static final DeferredHolder<Item, Item> TRINITITE_GLASS = block(CrustyChunksModBlocks.TRINITITE_GLASS);
   public static final DeferredHolder<Item, Item> TRINITITE_GLASS_TRAPDOOR = block(CrustyChunksModBlocks.TRINITITE_GLASS_TRAPDOOR);
   public static final DeferredHolder<Item, Item> ENERGY_DISTRIBUTION_NODE = block(CrustyChunksModBlocks.ENERGY_DISTRIBUTION_NODE);
   public static final DeferredHolder<Item, Item> FIRE_ARTILLERY_SHELL = REGISTRY.register("fire_artillery_shell", () -> new FireArtilleryShellItem());
   public static final DeferredHolder<Item, Item> PAINT_TOOL = REGISTRY.register("paint_tool", () -> new PaintToolItem());
   public static final DeferredHolder<Item, Item> AUTOMATIC_RIFLE = REGISTRY.register("automatic_rifle", () -> new AutomaticRifleItem());
   public static final DeferredHolder<Item, Item> MANUAL_CRANK = block(CrustyChunksModBlocks.MANUAL_CRANK);
   public static final DeferredHolder<Item, Item> LARGE_ROCKET_POD_CHAMBER = block(CrustyChunksModBlocks.LARGE_ROCKET_POD_CHAMBER);
   public static final DeferredHolder<Item, Item> LARGE_ROCKET_POD = block(CrustyChunksModBlocks.LARGE_ROCKET_POD);
   public static final DeferredHolder<Item, Item> FIRE_SPEAR_ROCKET = REGISTRY.register("fire_spear_rocket", () -> new FireSpearRocketItem());
   public static final DeferredHolder<Item, Item> SEEKER_SPEAR_ROCKET = REGISTRY.register("seeker_spear_rocket", () -> new SeekerSpearRocketItem());
   public static final DeferredHolder<Item, Item> EMPTY_MISSILE_HARDPOINT = block(CrustyChunksModBlocks.EMPTY_MISSILE_HARDPOINT);
   public static final DeferredHolder<Item, Item> FIRE_SPEAR_MISSILE_HARDPOINT = block(CrustyChunksModBlocks.FIRE_SPEAR_MISSILE_HARDPOINT);
   public static final DeferredHolder<Item, Item> SEEKER_SPEAR_MISSILE_HARDPOINT = block(CrustyChunksModBlocks.SEEKER_SPEAR_MISSILE_HARDPOINT);
   public static final DeferredHolder<Item, Item> STRIKE_SPEAR_MISSILE = REGISTRY.register("strike_spear_missile", () -> new StrikeSpearMissileItem());
   public static final DeferredHolder<Item, Item> STRIKE_SPEAR_MISSILE_HARDPOINT = block(CrustyChunksModBlocks.STRIKE_SPEAR_MISSILE_HARDPOINT);
   public static final DeferredHolder<Item, Item> LITHIUM_ORE = block(CrustyChunksModBlocks.LITHIUM_ORE);
   public static final DeferredHolder<Item, Item> RAW_LITHIUM = REGISTRY.register("raw_lithium", () -> new RawLithiumItem());
   public static final DeferredHolder<Item, Item> LITHIUM_INGOT = REGISTRY.register("lithium_ingot", () -> new LithiumIngotItem());
   public static final DeferredHolder<Item, Item> LITHIUM_NUGGET = REGISTRY.register("lithium_nugget", () -> new LithiumNuggetItem());
   public static final DeferredHolder<Item, Item> LITHIUM_DUST = REGISTRY.register("lithium_dust", () -> new LithiumDustItem());
   public static final DeferredHolder<Item, Item> ENRICHED_LITHIUM_INGOT = REGISTRY.register("enriched_lithium_ingot", () -> new EnrichedLithiumIngotItem());
   public static final DeferredHolder<Item, Item> ENRICHED_LITHIUM_NUGGET = REGISTRY.register("enriched_lithium_nugget", () -> new EnrichedLithiumNuggetItem());
   public static final DeferredHolder<Item, Item> TINY_LITHIUM_DEUTERIDE = REGISTRY.register("tiny_lithium_deuteride", () -> new TinyLithiumDeuterideItem());
   public static final DeferredHolder<Item, Item> LITHIUM_DEUTERIDE = REGISTRY.register("lithium_deuteride", () -> new LithiumDeuterideItem());
   public static final DeferredHolder<Item, Item> THERMO_NUCLEAR_FUEL = REGISTRY.register("thermo_nuclear_fuel", () -> new ThermoNuclearFuelItem());
   public static final DeferredHolder<Item, Item> POLISHED_BAUXITE = block(CrustyChunksModBlocks.POLISHED_BAUXITE);
   public static final DeferredHolder<Item, Item> RAW_LITHIUM_BLOCK = block(CrustyChunksModBlocks.RAW_LITHIUM_BLOCK);
   public static final DeferredHolder<Item, Item> LITHIUM_BLOCK = block(CrustyChunksModBlocks.LITHIUM_BLOCK);
   public static final DeferredHolder<Item, Item> SMALL_FLAK_SHELL = REGISTRY.register("small_flak_shell", () -> new SmallFlakShellItem());
   public static final DeferredHolder<Item, Item> SMALL_FLAK_PROJECTILE = REGISTRY.register("small_flak_projectile", () -> new SmallFlakProjectileItem());
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_GREEN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_GREEN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_GREEN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_GREEN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_GREEN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_GREEN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_GREEN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_GREEN);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_GREEN = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_GREEN);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_GREEN = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_GREEN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_YELLOW = block(CrustyChunksModBlocks.ALUMINUM_PLATING_YELLOW);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_YELLOW = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_YELLOW);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_YELLOW = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_YELLOW);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_YELLOW = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_YELLOW);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_YELLOW = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_YELLOW);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_YELLOW = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_YELLOW);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_BROWN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_BROWN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_BROWN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_BROWN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_BROWN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_BROWN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_BROWN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_BROWN);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_BROWN = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_BROWN);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_BROWN = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_BROWN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_RED = block(CrustyChunksModBlocks.ALUMINUM_PLATING_RED);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_RED = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_RED);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_RED = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_RED);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_RED = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_RED);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_RED = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_RED);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_RED = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_RED);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_WHITE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_WHITE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_WHITE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_WHITE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_WHITE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_WHITE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_WHITE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_WHITE);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_WHITE = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_WHITE);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_WHITE = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_WHITE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_BLUE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_BLUE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_BLUE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_BLUE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_BLUE = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_BLUE = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_ORANGE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_ORANGE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_ORANGE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_ORANGE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_ORANGE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_ORANGE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_ORANGE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_ORANGE);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_ORANGE = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_ORANGE);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_ORANGE = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_ORANGE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_CYAN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_CYAN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_CYAN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_CYAN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_CYAN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_CYAN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_CYAN = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_CYAN);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_CYAN = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_CYAN);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_CYAN = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_CYAN);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_BLACK = block(CrustyChunksModBlocks.ALUMINUM_PLATING_BLACK);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_BLACK = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_BLACK);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_BLACK = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_BLACK);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_BLACK = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_BLACK);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_BLACK = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_BLACK);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_BLACK = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_BLACK);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_GRAY = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_GRAY = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_LIGHT_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_LIGHT_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_LIGHT_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_LIGHT_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_LIGHT_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_LIGHT_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_LIGHT_GRAY = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_LIGHT_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_LIGHT_GRAY = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_LIGHT_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_LIGHT_GRAY = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_LIGHT_GRAY);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_LIME = block(CrustyChunksModBlocks.ALUMINUM_PLATING_LIME);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_LIME = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_LIME);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_LIME = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_LIME);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_LIME = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_LIME);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_LIME = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_LIME);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_LIME = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_LIME);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_LIGHT_BLUE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_LIGHT_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_LIGHT_BLUE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_LIGHT_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_LIGHT_BLUE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_LIGHT_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_LIGHT_BLUE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_LIGHT_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_LIGHT_BLUE = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_LIGHT_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_LIGHT_BLUE = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_LIGHT_BLUE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_PINK = block(CrustyChunksModBlocks.ALUMINUM_PLATING_PINK);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_PINK = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_PINK);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_PINK = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_PINK);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_PINK = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_PINK);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_PINK = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_PINK);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_PINK = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_PINK);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_MAGENTA = block(CrustyChunksModBlocks.ALUMINUM_PLATING_MAGENTA);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_MAGENTA = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_MAGENTA);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_MAGENTA = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_MAGENTA);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_MAGENTA = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_MAGENTA);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_MAGENTA = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_MAGENTA);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_MAGENTA = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_MAGENTA);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_PURPLE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_PURPLE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_SLAB_PURPLE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_SLAB_PURPLE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_STAIRS_PURPLE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_STAIRS_PURPLE);
   public static final DeferredHolder<Item, Item> ALUMINUM_PLATING_TRAPDOOR_PURPLE = block(CrustyChunksModBlocks.ALUMINUM_PLATING_TRAPDOOR_PURPLE);
   public static final DeferredHolder<Item, Item> ALUMINUM_SIDE_PANEL_PURPLE = block(CrustyChunksModBlocks.ALUMINUM_SIDE_PANEL_PURPLE);
   public static final DeferredHolder<Item, Item> ALUMINUM_AC_BARREL_PURPLE = block(CrustyChunksModBlocks.ALUMINUM_AC_BARREL_PURPLE);
   public static final DeferredHolder<Item, Item> MANUAL_AIMER = block(CrustyChunksModBlocks.MANUAL_AIMER);
   public static final DeferredHolder<Item, Item> SOLAR_GENERATOR = block(CrustyChunksModBlocks.SOLAR_GENERATOR);
   public static final DeferredHolder<Item, Item> RADAR_SPEAR_MISSILE_HARDPOINT = block(CrustyChunksModBlocks.RADAR_SPEAR_MISSILE_HARDPOINT);
   public static final DeferredHolder<Item, Item> RADAR_SPEAR_MISSILE = REGISTRY.register("radar_spear_missile", () -> new RadarSpearMissileItem());
   public static final DeferredHolder<Item, Item> RADAR_COMPONENT = REGISTRY.register("radar_component", () -> new RadarComponentItem());
   public static final DeferredHolder<Item, Item> IR_COMPONENT = REGISTRY.register("ir_component", () -> new IRComponentItem());
   public static final DeferredHolder<Item, Item> CHAFF_CHARGE = REGISTRY.register("chaff_charge", () -> new ChaffChargeItem());
   public static final DeferredHolder<Item, Item> ORDINANCE_CONTROLLER = block(CrustyChunksModBlocks.ORDINANCE_CONTROLLER);
   public static final DeferredHolder<Item, Item> ORDINANCE_SARH_SEEKER = block(CrustyChunksModBlocks.ORDINANCE_SARH_SEEKER);
   public static final DeferredHolder<Item, Item> FUEL_TANK_MODULE = block(CrustyChunksModBlocks.FUEL_TANK_MODULE);
   public static final DeferredHolder<Item, Item> FUEL_HOSE = REGISTRY.register("fuel_hose", () -> new FuelHoseItem());
   public static final DeferredHolder<Item, Item> DAMAGEDFUELTANK = block(CrustyChunksModBlocks.DAMAGEDFUELTANK);
   public static final DeferredHolder<Item, Item> FUEL_TANK_INPUT = block(CrustyChunksModBlocks.FUEL_TANK_INPUT);
   public static final DeferredHolder<Item, Item> HEAVY_MACHINE_GUN = block(CrustyChunksModBlocks.HEAVY_MACHINE_GUN);
   public static final DeferredHolder<Item, Item> LIGHT_MACHINE_GUN = block(CrustyChunksModBlocks.LIGHT_MACHINE_GUN);
   public static final DeferredHolder<Item, Item> COVERED_MACHINE_GUN_BARREL = block(CrustyChunksModBlocks.COVERED_MACHINE_GUN_BARREL);
   public static final DeferredHolder<Item, Item> REINFORCED_GLASS_STAIRS = block(CrustyChunksModBlocks.REINFORCED_GLASS_STAIRS);
   public static final DeferredHolder<Item, Item> TINTED_GLASS_STAIRS = block(CrustyChunksModBlocks.TINTED_GLASS_STAIRS);
   public static final DeferredHolder<Item, Item> TRINITITE_GLASS_STAIRS = block(CrustyChunksModBlocks.TRINITITE_GLASS_STAIRS);
   public static final DeferredHolder<Item, Item> THICK_BATTLE_CANNON_BARREL = block(CrustyChunksModBlocks.THICK_BATTLE_CANNON_BARREL);
   public static final DeferredHolder<Item, Item> GRENADE_LAUNCHER = REGISTRY.register("grenade_launcher", () -> new GrenadeLauncherItem());
   public static final DeferredHolder<Item, Item> MEDIUM_PETROL_ENGINE = block(CrustyChunksModBlocks.MEDIUM_PETROL_ENGINE);
   public static final DeferredHolder<Item, Item> SMALL_DIESEL_ENGINE = block(CrustyChunksModBlocks.SMALL_DIESEL_ENGINE);
   public static final DeferredHolder<Item, Item> THERMAL_SHELL = REGISTRY.register("thermal_shell", () -> new ThermalShellItem());
   public static final DeferredHolder<Item, Item> FLAME_THROWER = block(CrustyChunksModBlocks.FLAME_THROWER);
   public static final DeferredHolder<Item, Item> FLAME_THROWER_BARREL = block(CrustyChunksModBlocks.FLAME_THROWER_BARREL);
   public static final DeferredHolder<Item, Item> COVERED_FLAME_THROWER_BARREL = block(CrustyChunksModBlocks.COVERED_FLAME_THROWER_BARREL);
   public static final DeferredHolder<Item, Item> ERA_4 = block(CrustyChunksModBlocks.ERA_4);
   public static final DeferredHolder<Item, Item> ERA_3 = block(CrustyChunksModBlocks.ERA_3);
   public static final DeferredHolder<Item, Item> ERA_2 = block(CrustyChunksModBlocks.ERA_2);
   public static final DeferredHolder<Item, Item> ERA_1 = block(CrustyChunksModBlocks.ERA_1);
   public static final DeferredHolder<Item, Item> HUGE_HE_BULLET = REGISTRY.register("huge_he_bullet", () -> new HugeHEBulletItem());
   public static final DeferredHolder<Item, Item> ERA_TILE = REGISTRY.register("era_tile", () -> new ERATileItem());
   public static final DeferredHolder<Item, Item> OFFSET_ERA_4 = block(CrustyChunksModBlocks.OFFSET_ERA_4);
   public static final DeferredHolder<Item, Item> OFFSET_ERA_3 = block(CrustyChunksModBlocks.OFFSET_ERA_3);
   public static final DeferredHolder<Item, Item> OFFSET_ERA_2 = block(CrustyChunksModBlocks.OFFSET_ERA_2);
   public static final DeferredHolder<Item, Item> OFFSET_ERA_1 = block(CrustyChunksModBlocks.OFFSET_ERA_1);
   public static final DeferredHolder<Item, Item> MEDIUM_DIESEL_ENGINE = block(CrustyChunksModBlocks.MEDIUM_DIESEL_ENGINE);
   public static final DeferredHolder<Item, Item> SMALL_PETROL_ENGINE = block(CrustyChunksModBlocks.SMALL_PETROL_ENGINE);
   public static final DeferredHolder<Item, Item> PRODUCTION_INPUT = block(CrustyChunksModBlocks.PRODUCTION_INPUT);
   public static final DeferredHolder<Item, Item> PRODUCTION_OUTPUT = block(CrustyChunksModBlocks.PRODUCTION_OUTPUT);
   public static final DeferredHolder<Item, Item> ASSEMBLY_DEPOT = block(CrustyChunksModBlocks.ASSEMBLY_DEPOT);
   public static final DeferredHolder<Item, Item> ASSEMBLY_MACHINE = block(CrustyChunksModBlocks.ASSEMBLY_MACHINE);
   public static final DeferredHolder<Item, Item> ASSEMBLY_CRUSHER = block(CrustyChunksModBlocks.ASSEMBLY_CRUSHER);
   public static final DeferredHolder<Item, Item> ASSEMBLY_FURNACE = block(CrustyChunksModBlocks.ASSEMBLY_FURNACE);
   public static final DeferredHolder<Item, Item> MECHANICAL_BORE = REGISTRY.register("mechanical_bore", () -> new MechanicalBoreItem());
   public static final DeferredHolder<Item, Item> MECHANICAL_PRESS = REGISTRY.register("mechanical_press", () -> new MechanicalPressItem());
   public static final DeferredHolder<Item, Item> MECHANICAL_EXTRUDER = REGISTRY.register("mechanical_extruder", () -> new MechanicalExtruderItem());
   public static final DeferredHolder<Item, Item> MECHANICAL_SHEAR = REGISTRY.register("mechanical_shear", () -> new MechanicalShearItem());
   public static final DeferredHolder<Item, Item> STEEL_CRUSHING_WHEEL = REGISTRY.register("steel_crushing_wheel", () -> new SteelCrushingWheelItem());
   public static final DeferredHolder<Item, Item> NITRATE = REGISTRY.register("nitrate", () -> new NitrateItem());
   public static final DeferredHolder<Item, Item> NITRATE_BLOCK = block(CrustyChunksModBlocks.NITRATE_BLOCK);
   public static final DeferredHolder<Item, Item> PROTOTYPE_ERADICATOR_SPAWN_EGG = REGISTRY.register(
      "prototype_eradicator_spawn_egg", () -> new DeferredSpawnEggItem(CrustyChunksModEntities.PROTOTYPE_ERADICATOR, -15522036, -14608106, new Properties())
   );
   public static final DeferredHolder<Item, Item> BAUXITE_DIGESTER = block(CrustyChunksModBlocks.BAUXITE_DIGESTER);
   public static final DeferredHolder<Item, Item> ALUMINATE_DUST = REGISTRY.register("aluminate_dust", () -> new AluminateDustItem());
   public static final DeferredHolder<Item, Item> FILTERED_ALUMINATE_DUST = REGISTRY.register("filtered_aluminate_dust", () -> new FilteredAluminateDustItem());
   public static final DeferredHolder<Item, Item> ASSEMBLY_CENTRIFUGE_BOTTOM = block(CrustyChunksModBlocks.ASSEMBLY_CENTRIFUGE_BOTTOM);
   public static final DeferredHolder<Item, Item> ASSEMBLY_CENTRIFUGE_MIDDLE = block(CrustyChunksModBlocks.ASSEMBLY_CENTRIFUGE_MIDDLE);
   public static final DeferredHolder<Item, Item> ASSEMBLY_CENTRIFUGE_TOP = block(CrustyChunksModBlocks.ASSEMBLY_CENTRIFUGE_TOP);
   public static final DeferredHolder<Item, Item> CONVEYOR_SPLITTER = block(CrustyChunksModBlocks.CONVEYOR_SPLITTER);
   public static final DeferredHolder<Item, Item> LIGHT_WOOD_FRAME = block(CrustyChunksModBlocks.LIGHT_WOOD_FRAME);
   public static final DeferredHolder<Item, Item> LIGHT_WOOD_BLOCK = block(CrustyChunksModBlocks.LIGHT_WOOD_BLOCK);
   public static final DeferredHolder<Item, Item> LIGHT_WOOD_SLAB = block(CrustyChunksModBlocks.LIGHT_WOOD_SLAB);
   public static final DeferredHolder<Item, Item> LIGHT_WOOD_STAIRS = block(CrustyChunksModBlocks.LIGHT_WOOD_STAIRS);
   public static final DeferredHolder<Item, Item> LIGHT_WOOD_SIDE_PANEL = block(CrustyChunksModBlocks.LIGHT_WOOD_SIDE_PANEL);
   public static final DeferredHolder<Item, Item> LIGHT_WOOD_TRAPDOOR = block(CrustyChunksModBlocks.LIGHT_WOOD_TRAPDOOR);
   public static final DeferredHolder<Item, Item> PRECISION_COMPONENT = REGISTRY.register("precision_component", () -> new PrecisionComponentItem());
   public static final DeferredHolder<Item, Item> UNFABRICATED_TECH_COMPONENT = REGISTRY.register(
      "unfabricated_tech_component", () -> new UnfabricatedTechComponentItem()
   );
   public static final DeferredHolder<Item, Item> ASSEMBLY_CIRCUIT_FABRICATOR = block(CrustyChunksModBlocks.ASSEMBLY_CIRCUIT_FABRICATOR);
   public static final DeferredHolder<Item, Item> ASSEMBLY_MECHANICAL_FABRICATOR = block(CrustyChunksModBlocks.ASSEMBLY_MECHANICAL_FABRICATOR);
   public static final DeferredHolder<Item, Item> PYROCHLORE_ORE = block(CrustyChunksModBlocks.PYROCHLORE_ORE);
   public static final DeferredHolder<Item, Item> PYROCHLORE = REGISTRY.register("pyrochlore", () -> new PyrochloreItem());
   public static final DeferredHolder<Item, Item> PYROCHLORE_DUST = REGISTRY.register("pyrochlore_dust", () -> new PyrochloreDustItem());
   public static final DeferredHolder<Item, Item> FILTERED_PYROCHLORE_DUST = REGISTRY.register("filtered_pyrochlore_dust", () -> new FilteredPyrochloreDustItem());
   public static final DeferredHolder<Item, Item> NIOBIUM_DUST = REGISTRY.register("niobium_dust", () -> new NiobiumDustItem());
   public static final DeferredHolder<Item, Item> NIOBIUM_TINY_DUST = REGISTRY.register("niobium_tiny_dust", () -> new NiobiumTinyDustItem());
   public static final DeferredHolder<Item, Item> NIOBIUM_INGOT = REGISTRY.register("niobium_ingot", () -> new NiobiumIngotItem());
   public static final DeferredHolder<Item, Item> NIOBIUM_BLOCK = block(CrustyChunksModBlocks.NIOBIUM_BLOCK);
   public static final DeferredHolder<Item, Item> PYROCHLORE_BLOCK = block(CrustyChunksModBlocks.PYROCHLORE_BLOCK);
   public static final DeferredHolder<Item, Item> ADVANCED_ALLOY_MIXTURE = REGISTRY.register("advanced_alloy_mixture", () -> new AdvancedAlloyMixtureItem());
   public static final DeferredHolder<Item, Item> COMPRESSED_ADVANCED_MIXTURE = REGISTRY.register(
      "compressed_advanced_mixture", () -> new CompressedAdvancedMixtureItem()
   );
   public static final DeferredHolder<Item, Item> ADVANCED_ALLOY_INGOT = REGISTRY.register("advanced_alloy_ingot", () -> new AdvancedAlloyIngotItem());
   public static final DeferredHolder<Item, Item> ADVANCED_ALLOY_COMPONENT = REGISTRY.register("advanced_alloy_component", () -> new AdvancedAlloyComponentItem());
   public static final DeferredHolder<Item, Item> URANIUM_NEUTRAL_BLOCK = block(CrustyChunksModBlocks.URANIUM_NEUTRAL_BLOCK);
   public static final DeferredHolder<Item, Item> URANIUM_ENRICHED_BLOCK = block(CrustyChunksModBlocks.URANIUM_ENRICHED_BLOCK);
   public static final DeferredHolder<Item, Item> URANIUM_DEPLETED_BLOCK = block(CrustyChunksModBlocks.URANIUM_DEPLETED_BLOCK);
   public static final DeferredHolder<Item, Item> PLUTONIUM_BLOCK = block(CrustyChunksModBlocks.PLUTONIUM_BLOCK);
   public static final DeferredHolder<Item, Item> GRENADE_SHELL = REGISTRY.register("grenade_shell", () -> new GrenadeShellItem());
   public static final DeferredHolder<Item, Item> SMOKE_GRENADE_SHELL = REGISTRY.register("smoke_grenade_shell", () -> new SmokeGrenadeShellItem());
   public static final DeferredHolder<Item, Item> SMOKE_SHELL = REGISTRY.register("smoke_shell", () -> new SmokeShellItem());
   public static final DeferredHolder<Item, Item> SMOKE_PROJECTILE = REGISTRY.register("smoke_projectile", () -> new SmokeProjectileItem());
   public static final DeferredHolder<Item, Item> PHOSPHORUS_DUST = REGISTRY.register("phosphorus_dust", () -> new PhosphorusDustItem());
   public static final DeferredHolder<Item, Item> CHLORINE_DUST = REGISTRY.register("chlorine_dust", () -> new ChlorineDustItem());
   public static final DeferredHolder<Item, Item> ADVANCED_ALLOY_BLOCK = block(CrustyChunksModBlocks.ADVANCED_ALLOY_BLOCK);
   public static final DeferredHolder<Item, Item> PHOSPHATE_BLOCK = block(CrustyChunksModBlocks.PHOSPHATE_BLOCK);
   public static final DeferredHolder<Item, Item> SULFURIC_ACID_BUCKET = REGISTRY.register("sulfuric_acid_bucket", () -> new SulfuricAcidItem());
   public static final DeferredHolder<Item, Item> HYDRAZINE_BUCKET = REGISTRY.register("hydrazine_bucket", () -> new HydrazineItem());
   public static final DeferredHolder<Item, Item> GAS_CANISTER = REGISTRY.register("gas_canister", () -> new GasCanisterItem());
   public static final DeferredHolder<Item, Item> MINIGUN = block(CrustyChunksModBlocks.MINIGUN);
   public static final DeferredHolder<Item, Item> MINI_GUN_BARREL = REGISTRY.register(
      CrustyChunksModBlocks.MINI_GUN_BARREL.getId().getPath(),
      () -> new MiniGunBarrelDisplayItem((Block)CrustyChunksModBlocks.MINI_GUN_BARREL.get(), new Properties())
   );
   public static final DeferredHolder<Item, Item> ORDINANCE_RELOCATOR = block(CrustyChunksModBlocks.ORDINANCE_RELOCATOR);
   public static final DeferredHolder<Item, Item> ITEM_INCINERATOR = block(CrustyChunksModBlocks.ITEM_INCINERATOR);
   public static final DeferredHolder<Item, Item> SMALL_AP_SHELL = REGISTRY.register("small_ap_shell", () -> new SmallAPShellItem());
   public static final DeferredHolder<Item, Item> TYPE_1_BC_MUZZLE_BRAKE = block(CrustyChunksModBlocks.TYPE_1_BC_MUZZLE_BRAKE);
   public static final DeferredHolder<Item, Item> TYPE_2_BC_MUZZLE_BRAKE = block(CrustyChunksModBlocks.TYPE_2_BC_MUZZLE_BRAKE);
   public static final DeferredHolder<Item, Item> BATTLE_CANNON_MANTLET = block(CrustyChunksModBlocks.BATTLE_CANNON_MANTLET);
   public static final DeferredHolder<Item, Item> WELDER = REGISTRY.register("welder", () -> new WelderItem());
   public static final DeferredHolder<Item, Item> REDIRECTOR_SHAFT = block(CrustyChunksModBlocks.REDIRECTOR_SHAFT);
   public static final DeferredHolder<Item, Item> IMMUNITY_SAND = block(CrustyChunksModBlocks.IMMUNITY_SAND);
   public static final DeferredHolder<Item, Item> IMMUNITY_RED_SAND = block(CrustyChunksModBlocks.IMMUNITY_RED_SAND);
   public static final DeferredHolder<Item, Item> MEDIUM_BOMB = block(CrustyChunksModBlocks.MEDIUM_BOMB);
   public static final DeferredHolder<Item, Item> LIGHT_TURBINE_ENGINE = block(CrustyChunksModBlocks.LIGHT_TURBINE_ENGINE);
   public static final DeferredHolder<Item, Item> CANISTER_PROJECTILE = REGISTRY.register("canister_projectile", () -> new CanisterProjectileItem());
   public static final DeferredHolder<Item, Item> CANISTER_SHELL = REGISTRY.register("canister_shell", () -> new CanisterShellItem());
   public static final DeferredHolder<Item, Item> MUSKET = REGISTRY.register("musket", () -> new MusketItem());
   public static final DeferredHolder<Item, Item> IRON_TUBE = REGISTRY.register("iron_tube", () -> new IronTubeItem());
   public static final DeferredHolder<Item, Item> MINING_CHARGE = block(CrustyChunksModBlocks.MINING_CHARGE);

   private static DeferredHolder<Item, Item> block(DeferredHolder<Block, Block> block) {
      return REGISTRY.register(block.getId().getPath(), () -> new BlockItem((Block)block.get(), new Properties()));
   }

   private static DeferredHolder<Item, Item> doubleBlock(DeferredHolder<Block, Block> block) {
      return REGISTRY.register(block.getId().getPath(), () -> new DoubleHighBlockItem((Block)block.get(), new Properties()));
   }

   @SubscribeEvent
   public static void clientLoad(FMLClientSetupEvent event) {
      event.enqueueWork(
         () -> {
            ItemProperties.register(
               (Item)SMALLMAGAZINE.get(),
               ResourceLocation.parse("crusty_chunks:smallmagazine_ammunitionload"),
               (itemStackToRender, clientWorld, entity, itemEntityId) -> (float)MagazineLevelProcedure.execute(itemStackToRender)
            );
            ItemProperties.register(
               (Item)MEDIUM_MAGAZINE.get(),
               ResourceLocation.parse("crusty_chunks:medium_magazine_ammo"),
               (itemStackToRender, clientWorld, entity, itemEntityId) -> (float)MagazineLevelProcedure.execute(itemStackToRender)
            );
            ItemProperties.register(
               (Item)SMG_MAGAZINE.get(),
               ResourceLocation.parse("crusty_chunks:smg_magazine_ammo"),
               (itemStackToRender, clientWorld, entity, itemEntityId) -> (float)MagazineLevelProcedure.execute(itemStackToRender)
            );
            ItemProperties.register(
               (Item)MACHINE_GUN_BOX.get(),
               ResourceLocation.parse("crusty_chunks:machine_gun_box_ammo"),
               (itemStackToRender, clientWorld, entity, itemEntityId) -> (float)MagazineLevelProcedure.execute(itemStackToRender)
            );
            ItemProperties.register(
               (Item)LMG_MAGAZINE.get(),
               ResourceLocation.parse("crusty_chunks:lmg_magazine_ammo"),
               (itemStackToRender, clientWorld, entity, itemEntityId) -> (float)MagazineLevelProcedure.execute(itemStackToRender)
            );
            ItemProperties.register(
               (Item)LARGE_MAGAZINE.get(),
               ResourceLocation.parse("crusty_chunks:large_magazine_ammo"),
               (itemStackToRender, clientWorld, entity, itemEntityId) -> (float)MagazineLevelProcedure.execute(itemStackToRender)
            );
         }
      );
   }
}
