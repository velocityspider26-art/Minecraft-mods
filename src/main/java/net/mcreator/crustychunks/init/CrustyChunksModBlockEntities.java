package net.mcreator.crustychunks.init;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.block.entity.ActiveRobotChuteBlockEntity;
import net.mcreator.crustychunks.block.entity.AimerNodeBlockEntity;
import net.mcreator.crustychunks.block.entity.ArtilleryAutoloaderBlockEntity;
import net.mcreator.crustychunks.block.entity.ArtilleryChargeLoaderBlockEntity;
import net.mcreator.crustychunks.block.entity.ArtillerybreechBlockEntity;
import net.mcreator.crustychunks.block.entity.AssemblyCentrifugeMiddleBlockEntity;
import net.mcreator.crustychunks.block.entity.AssemblyCircuitFabricatorBlockEntity;
import net.mcreator.crustychunks.block.entity.AssemblyCrusherBlockEntity;
import net.mcreator.crustychunks.block.entity.AssemblyDepotBlockEntity;
import net.mcreator.crustychunks.block.entity.AssemblyFurnaceBlockEntity;
import net.mcreator.crustychunks.block.entity.AssemblyMachineBlockEntity;
import net.mcreator.crustychunks.block.entity.AssemblyMechanicalFabricatorBlockEntity;
import net.mcreator.crustychunks.block.entity.AutocannonBlockEntity;
import net.mcreator.crustychunks.block.entity.AutocannonDrumBlockEntity;
import net.mcreator.crustychunks.block.entity.AutoloaderBlockEntity;
import net.mcreator.crustychunks.block.entity.BattleCannonBreechBlockEntity;
import net.mcreator.crustychunks.block.entity.BauxiteDigesterBlockEntity;
import net.mcreator.crustychunks.block.entity.BlastFurnaceBlockEntity;
import net.mcreator.crustychunks.block.entity.BreederReactorInterfaceBlockEntity;
import net.mcreator.crustychunks.block.entity.BreederReactorPortBlockEntity;
import net.mcreator.crustychunks.block.entity.ClusterOfBombsBlockEntity;
import net.mcreator.crustychunks.block.entity.ConveyorBlockEntity;
import net.mcreator.crustychunks.block.entity.ConveyorSplitterBlockEntity;
import net.mcreator.crustychunks.block.entity.CountermeasureDispenserBlockEntity;
import net.mcreator.crustychunks.block.entity.DamagedfueltankBlockEntity;
import net.mcreator.crustychunks.block.entity.DefenseCoreBlockEntity;
import net.mcreator.crustychunks.block.entity.DriveShaftBlockEntity;
import net.mcreator.crustychunks.block.entity.ElectricFireboxBlockEntity;
import net.mcreator.crustychunks.block.entity.EmptyMissileHardpointBlockEntity;
import net.mcreator.crustychunks.block.entity.EnergyBatteryBlockEntity;
import net.mcreator.crustychunks.block.entity.EnergyDistributionNodeBlockEntity;
import net.mcreator.crustychunks.block.entity.EnergyNodeBlockEntity;
import net.mcreator.crustychunks.block.entity.EngineCyllinderBlockEntity;
import net.mcreator.crustychunks.block.entity.ExtensionShaftBlockEntity;
import net.mcreator.crustychunks.block.entity.FireSpearMissileHardpointBlockEntity;
import net.mcreator.crustychunks.block.entity.FireboxBlockEntity;
import net.mcreator.crustychunks.block.entity.FissionBombBlockEntity;
import net.mcreator.crustychunks.block.entity.FlameThrowerBlockEntity;
import net.mcreator.crustychunks.block.entity.FoundryBlockEntity;
import net.mcreator.crustychunks.block.entity.FuelTankBlockEntity;
import net.mcreator.crustychunks.block.entity.FuelTankInputBlockEntity;
import net.mcreator.crustychunks.block.entity.FuelTankModuleBlockEntity;
import net.mcreator.crustychunks.block.entity.FusionBombBlockEntity;
import net.mcreator.crustychunks.block.entity.GasBombBlockEntity;
import net.mcreator.crustychunks.block.entity.GasDispenserBlockEntity;
import net.mcreator.crustychunks.block.entity.GeneratorBlockEntity;
import net.mcreator.crustychunks.block.entity.HeavyMachineGunBlockEntity;
import net.mcreator.crustychunks.block.entity.ItemIncineratorBlockEntity;
import net.mcreator.crustychunks.block.entity.JetCompressorBlockEntity;
import net.mcreator.crustychunks.block.entity.JetExhaustBlockEntity;
import net.mcreator.crustychunks.block.entity.JetGearboxBlockEntity;
import net.mcreator.crustychunks.block.entity.JetTurbineBlockEntity;
import net.mcreator.crustychunks.block.entity.LargeElectricMotorBlockEntity;
import net.mcreator.crustychunks.block.entity.LargeRocketPodBlockEntity;
import net.mcreator.crustychunks.block.entity.LightAutocannonBlockEntity;
import net.mcreator.crustychunks.block.entity.LightMachineGunBlockEntity;
import net.mcreator.crustychunks.block.entity.MachineGunBlockEntity;
import net.mcreator.crustychunks.block.entity.ManualAimerBlockEntity;
import net.mcreator.crustychunks.block.entity.ManualCrankBlockEntity;
import net.mcreator.crustychunks.block.entity.MediumDieselEngineBlockEntity;
import net.mcreator.crustychunks.block.entity.MediumPetrolEngineBlockEntity;
import net.mcreator.crustychunks.block.entity.MineralGrinderBlockEntity;
import net.mcreator.crustychunks.block.entity.MiniGunBarrelTileEntity;
import net.mcreator.crustychunks.block.entity.MinigunBlockEntity;
import net.mcreator.crustychunks.block.entity.MortarBlockEntity;
import net.mcreator.crustychunks.block.entity.NodeTriggerBlockEntity;
import net.mcreator.crustychunks.block.entity.NodeTriggerOnBlockEntity;
import net.mcreator.crustychunks.block.entity.OilFireboxBlockEntity;
import net.mcreator.crustychunks.block.entity.OpenSummonationBlockEntity;
import net.mcreator.crustychunks.block.entity.OrdinanceControllerBlockEntity;
import net.mcreator.crustychunks.block.entity.OrdinanceCoreBlockEntity;
import net.mcreator.crustychunks.block.entity.OrdinanceInlineFissionWarheadBlockEntity;
import net.mcreator.crustychunks.block.entity.OrdinanceInlineFusionWarheadStage1BlockEntity;
import net.mcreator.crustychunks.block.entity.PowerReactorInterfaceBlockEntity;
import net.mcreator.crustychunks.block.entity.PowerReactorPortBlockEntity;
import net.mcreator.crustychunks.block.entity.ProductionInputBlockEntity;
import net.mcreator.crustychunks.block.entity.ProductionOutputBlockEntity;
import net.mcreator.crustychunks.block.entity.RACBarrelTileEntity;
import net.mcreator.crustychunks.block.entity.RadarSpearMissileHardpointBlockEntity;
import net.mcreator.crustychunks.block.entity.ReactionChamberBlockEntity;
import net.mcreator.crustychunks.block.entity.RedirectorShaftBlockEntity;
import net.mcreator.crustychunks.block.entity.RedstoneTNTBlockEntity;
import net.mcreator.crustychunks.block.entity.RefineryBlockEntity;
import net.mcreator.crustychunks.block.entity.RefineryTowerBlockEntity;
import net.mcreator.crustychunks.block.entity.RobotChuteBlockEntity;
import net.mcreator.crustychunks.block.entity.RocketPodBlockEntity;
import net.mcreator.crustychunks.block.entity.RotaryAutoCannonBlockEntity;
import net.mcreator.crustychunks.block.entity.SeekerSpearMissileHardpointBlockEntity;
import net.mcreator.crustychunks.block.entity.SirenBlockEntity;
import net.mcreator.crustychunks.block.entity.SmallBombBlockEntity;
import net.mcreator.crustychunks.block.entity.SmallDieselEngineBlockEntity;
import net.mcreator.crustychunks.block.entity.SmallPetrolEngineBlockEntity;
import net.mcreator.crustychunks.block.entity.SmokeLauncherBlockEntity;
import net.mcreator.crustychunks.block.entity.SolarGeneratorBlockEntity;
import net.mcreator.crustychunks.block.entity.StrikeSpearMissileHardpointBlockEntity;
import net.mcreator.crustychunks.block.entity.SummonationBlockEntity;
import net.mcreator.crustychunks.block.entity.SummonatorActiveBlockEntity;
import net.mcreator.crustychunks.block.entity.SummonatorBlockEntity;
import net.mcreator.crustychunks.block.entity.ThermalFurnaceBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CrustyChunksModBlockEntities {
   public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "crusty_chunks");
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> REDSTONE_TNT = register(
      "redstone_tnt", CrustyChunksModBlocks.REDSTONE_TNT, RedstoneTNTBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> MACHINE_GUN = register("machine_gun", CrustyChunksModBlocks.MACHINE_GUN, MachineGunBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ARTILLERYBREECH = register(
      "artillerybreech", CrustyChunksModBlocks.ARTILLERYBREECH, ArtillerybreechBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> BATTLE_CANNON_BREECH = register(
      "battle_cannon_breech", CrustyChunksModBlocks.BATTLE_CANNON_BREECH, BattleCannonBreechBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> BLAST_FURNACE = register(
      "blast_furnace", CrustyChunksModBlocks.BLAST_FURNACE, BlastFurnaceBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> FOUNDRY = register("foundry", CrustyChunksModBlocks.FOUNDRY, FoundryBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> CONVEYOR = register("conveyor", CrustyChunksModBlocks.CONVEYOR, ConveyorBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> FIREBOX = register("firebox", CrustyChunksModBlocks.FIREBOX, FireboxBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> AUTOCANNON = register("autocannon", CrustyChunksModBlocks.AUTOCANNON, AutocannonBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> AUTOCANNON_DRUM = register(
      "autocannon_drum", CrustyChunksModBlocks.AUTOCANNON_DRUM, AutocannonDrumBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> AUTOLOADER = register("autoloader", CrustyChunksModBlocks.AUTOLOADER, AutoloaderBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> FISSION_BOMB = register(
      "fission_bomb", CrustyChunksModBlocks.FISSION_BOMB, FissionBombBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> MINERAL_GRINDER = register(
      "mineral_grinder", CrustyChunksModBlocks.MINERAL_GRINDER, MineralGrinderBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> SMALL_BOMB = register("small_bomb", CrustyChunksModBlocks.SMALL_BOMB, SmallBombBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> CLUSTER_OF_BOMBS = register(
      "cluster_of_bombs", CrustyChunksModBlocks.CLUSTER_OF_BOMBS, ClusterOfBombsBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ARTILLERY_AUTOLOADER = register(
      "artillery_autoloader", CrustyChunksModBlocks.ARTILLERY_AUTOLOADER, ArtilleryAutoloaderBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ARTILLERY_CHARGE_LOADER = register(
      "artillery_charge_loader", CrustyChunksModBlocks.ARTILLERY_CHARGE_LOADER, ArtilleryChargeLoaderBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ROCKET_POD = register("rocket_pod", CrustyChunksModBlocks.ROCKET_POD, RocketPodBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> JET_EXHAUST = register("jet_exhaust", CrustyChunksModBlocks.JET_EXHAUST, JetExhaustBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> JET_TURBINE = register("jet_turbine", CrustyChunksModBlocks.JET_TURBINE, JetTurbineBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> JET_COMPRESSOR = register(
      "jet_compressor", CrustyChunksModBlocks.JET_COMPRESSOR, JetCompressorBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> MORTAR = register("mortar", CrustyChunksModBlocks.MORTAR, MortarBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> AIMER_NODE = register("aimer_node", CrustyChunksModBlocks.AIMER_NODE, AimerNodeBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> SIREN = register("siren", CrustyChunksModBlocks.SIREN, SirenBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ROTARY_AUTO_CANNON = register(
      "rotary_auto_cannon", CrustyChunksModBlocks.ROTARY_AUTO_CANNON, RotaryAutoCannonBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RACBarrelTileEntity>> RAC_BARREL = REGISTRY.register(
      "rac_barrel", () -> Builder.of(RACBarrelTileEntity::new, new Block[]{(Block)CrustyChunksModBlocks.RAC_BARREL.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> LIGHT_AUTOCANNON = register(
      "light_autocannon", CrustyChunksModBlocks.LIGHT_AUTOCANNON, LightAutocannonBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> SMOKE_LAUNCHER = register(
      "smoke_launcher", CrustyChunksModBlocks.SMOKE_LAUNCHER, SmokeLauncherBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> REFINERY = register("refinery", CrustyChunksModBlocks.REFINERY, RefineryBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> REFINERY_TOWER = register(
      "refinery_tower", CrustyChunksModBlocks.REFINERY_TOWER, RefineryTowerBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> REACTION_CHAMBER = register(
      "reaction_chamber", CrustyChunksModBlocks.REACTION_CHAMBER, ReactionChamberBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> BREEDER_REACTOR_PORT = register(
      "breeder_reactor_port", CrustyChunksModBlocks.BREEDER_REACTOR_PORT, BreederReactorPortBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> OIL_FIREBOX = register("oil_firebox", CrustyChunksModBlocks.OIL_FIREBOX, OilFireboxBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> SUMMONATOR = register("summonator", CrustyChunksModBlocks.SUMMONATOR, SummonatorBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ROBOT_CHUTE = register("robot_chute", CrustyChunksModBlocks.ROBOT_CHUTE, RobotChuteBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> SUMMONATOR_ACTIVE = register(
      "summonator_active", CrustyChunksModBlocks.SUMMONATOR_ACTIVE, SummonatorActiveBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ACTIVE_ROBOT_CHUTE = register(
      "active_robot_chute", CrustyChunksModBlocks.ACTIVE_ROBOT_CHUTE, ActiveRobotChuteBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> BREEDER_REACTOR_INTERFACE = register(
      "breeder_reactor_interface", CrustyChunksModBlocks.BREEDER_REACTOR_INTERFACE, BreederReactorInterfaceBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> GAS_BOMB = register("gas_bomb", CrustyChunksModBlocks.GAS_BOMB, GasBombBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> GAS_DISPENSER = register(
      "gas_dispenser", CrustyChunksModBlocks.GAS_DISPENSER, GasDispenserBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> NODE_TRIGGER = register(
      "node_trigger", CrustyChunksModBlocks.NODE_TRIGGER, NodeTriggerBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> NODE_TRIGGER_ON = register(
      "node_trigger_on", CrustyChunksModBlocks.NODE_TRIGGER_ON, NodeTriggerOnBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> COUNTERMEASURE_DISPENSER = register(
      "countermeasure_dispenser", CrustyChunksModBlocks.COUNTERMEASURE_DISPENSER, CountermeasureDispenserBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ORDINANCE_CORE = register(
      "ordinance_core", CrustyChunksModBlocks.ORDINANCE_CORE, OrdinanceCoreBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ORDINANCE_INLINE_FISSION_WARHEAD = register(
      "ordinance_inline_fission_warhead", CrustyChunksModBlocks.ORDINANCE_INLINE_FISSION_WARHEAD, OrdinanceInlineFissionWarheadBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> SUMMONATION = register("summonation", CrustyChunksModBlocks.SUMMONATION, SummonationBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> OPEN_SUMMONATION = register(
      "open_summonation", CrustyChunksModBlocks.OPEN_SUMMONATION, OpenSummonationBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> DEFENSE_CORE = register(
      "defense_core", CrustyChunksModBlocks.DEFENSE_CORE, DefenseCoreBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> FUSION_BOMB = register("fusion_bomb", CrustyChunksModBlocks.FUSION_BOMB, FusionBombBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> JET_GEARBOX = register("jet_gearbox", CrustyChunksModBlocks.JET_GEARBOX, JetGearboxBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> GENERATOR = register("generator", CrustyChunksModBlocks.GENERATOR, GeneratorBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> FUEL_TANK = register("fuel_tank", CrustyChunksModBlocks.FUEL_TANK, FuelTankBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> EXTENSION_SHAFT = register(
      "extension_shaft", CrustyChunksModBlocks.EXTENSION_SHAFT, ExtensionShaftBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> DRIVE_SHAFT = register("drive_shaft", CrustyChunksModBlocks.DRIVE_SHAFT, DriveShaftBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ENGINE_CYLLINDER = register(
      "engine_cyllinder", CrustyChunksModBlocks.ENGINE_CYLLINDER, EngineCyllinderBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ENERGY_NODE = register("energy_node", CrustyChunksModBlocks.ENERGY_NODE, EnergyNodeBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ENERGY_BATTERY = register(
      "energy_battery", CrustyChunksModBlocks.ENERGY_BATTERY, EnergyBatteryBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> LARGE_ELECTRIC_MOTOR = register(
      "large_electric_motor", CrustyChunksModBlocks.LARGE_ELECTRIC_MOTOR, LargeElectricMotorBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ELECTRIC_FIREBOX = register(
      "electric_firebox", CrustyChunksModBlocks.ELECTRIC_FIREBOX, ElectricFireboxBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> THERMAL_FURNACE = register(
      "thermal_furnace", CrustyChunksModBlocks.THERMAL_FURNACE, ThermalFurnaceBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> POWER_REACTOR_INTERFACE = register(
      "power_reactor_interface", CrustyChunksModBlocks.POWER_REACTOR_INTERFACE, PowerReactorInterfaceBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> POWER_REACTOR_PORT = register(
      "power_reactor_port", CrustyChunksModBlocks.POWER_REACTOR_PORT, PowerReactorPortBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ORDINANCE_INLINE_FUSION_WARHEAD_STAGE_1 = register(
      "ordinance_inline_fusion_warhead_stage_1",
      CrustyChunksModBlocks.ORDINANCE_INLINE_FUSION_WARHEAD_STAGE_1,
      OrdinanceInlineFusionWarheadStage1BlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ENERGY_DISTRIBUTION_NODE = register(
      "energy_distribution_node", CrustyChunksModBlocks.ENERGY_DISTRIBUTION_NODE, EnergyDistributionNodeBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> MANUAL_CRANK = register(
      "manual_crank", CrustyChunksModBlocks.MANUAL_CRANK, ManualCrankBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> LARGE_ROCKET_POD = register(
      "large_rocket_pod", CrustyChunksModBlocks.LARGE_ROCKET_POD, LargeRocketPodBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> EMPTY_MISSILE_HARDPOINT = register(
      "empty_missile_hardpoint", CrustyChunksModBlocks.EMPTY_MISSILE_HARDPOINT, EmptyMissileHardpointBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> FIRE_SPEAR_MISSILE_HARDPOINT = register(
      "fire_spear_missile_hardpoint", CrustyChunksModBlocks.FIRE_SPEAR_MISSILE_HARDPOINT, FireSpearMissileHardpointBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> SEEKER_SPEAR_MISSILE_HARDPOINT = register(
      "seeker_spear_missile_hardpoint", CrustyChunksModBlocks.SEEKER_SPEAR_MISSILE_HARDPOINT, SeekerSpearMissileHardpointBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> STRIKE_SPEAR_MISSILE_HARDPOINT = register(
      "strike_spear_missile_hardpoint", CrustyChunksModBlocks.STRIKE_SPEAR_MISSILE_HARDPOINT, StrikeSpearMissileHardpointBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> MANUAL_AIMER = register(
      "manual_aimer", CrustyChunksModBlocks.MANUAL_AIMER, ManualAimerBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> SOLAR_GENERATOR = register(
      "solar_generator", CrustyChunksModBlocks.SOLAR_GENERATOR, SolarGeneratorBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> RADAR_SPEAR_MISSILE_HARDPOINT = register(
      "radar_spear_missile_hardpoint", CrustyChunksModBlocks.RADAR_SPEAR_MISSILE_HARDPOINT, RadarSpearMissileHardpointBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ORDINANCE_CONTROLLER = register(
      "ordinance_controller", CrustyChunksModBlocks.ORDINANCE_CONTROLLER, OrdinanceControllerBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> FUEL_TANK_MODULE = register(
      "fuel_tank_module", CrustyChunksModBlocks.FUEL_TANK_MODULE, FuelTankModuleBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> DAMAGEDFUELTANK = register(
      "damagedfueltank", CrustyChunksModBlocks.DAMAGEDFUELTANK, DamagedfueltankBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> FUEL_TANK_INPUT = register(
      "fuel_tank_input", CrustyChunksModBlocks.FUEL_TANK_INPUT, FuelTankInputBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> HEAVY_MACHINE_GUN = register(
      "heavy_machine_gun", CrustyChunksModBlocks.HEAVY_MACHINE_GUN, HeavyMachineGunBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> LIGHT_MACHINE_GUN = register(
      "light_machine_gun", CrustyChunksModBlocks.LIGHT_MACHINE_GUN, LightMachineGunBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> MEDIUM_PETROL_ENGINE = register(
      "medium_petrol_engine", CrustyChunksModBlocks.MEDIUM_PETROL_ENGINE, MediumPetrolEngineBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> SMALL_DIESEL_ENGINE = register(
      "small_diesel_engine", CrustyChunksModBlocks.SMALL_DIESEL_ENGINE, SmallDieselEngineBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> FLAME_THROWER = register(
      "flame_thrower", CrustyChunksModBlocks.FLAME_THROWER, FlameThrowerBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> MEDIUM_DIESEL_ENGINE = register(
      "medium_diesel_engine", CrustyChunksModBlocks.MEDIUM_DIESEL_ENGINE, MediumDieselEngineBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> SMALL_PETROL_ENGINE = register(
      "small_petrol_engine", CrustyChunksModBlocks.SMALL_PETROL_ENGINE, SmallPetrolEngineBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> PRODUCTION_INPUT = register(
      "production_input", CrustyChunksModBlocks.PRODUCTION_INPUT, ProductionInputBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> PRODUCTION_OUTPUT = register(
      "production_output", CrustyChunksModBlocks.PRODUCTION_OUTPUT, ProductionOutputBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ASSEMBLY_DEPOT = register(
      "assembly_depot", CrustyChunksModBlocks.ASSEMBLY_DEPOT, AssemblyDepotBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ASSEMBLY_MACHINE = register(
      "assembly_machine", CrustyChunksModBlocks.ASSEMBLY_MACHINE, AssemblyMachineBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ASSEMBLY_CRUSHER = register(
      "assembly_crusher", CrustyChunksModBlocks.ASSEMBLY_CRUSHER, AssemblyCrusherBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ASSEMBLY_FURNACE = register(
      "assembly_furnace", CrustyChunksModBlocks.ASSEMBLY_FURNACE, AssemblyFurnaceBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> BAUXITE_DIGESTER = register(
      "bauxite_digester", CrustyChunksModBlocks.BAUXITE_DIGESTER, BauxiteDigesterBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ASSEMBLY_CENTRIFUGE_MIDDLE = register(
      "assembly_centrifuge_middle", CrustyChunksModBlocks.ASSEMBLY_CENTRIFUGE_MIDDLE, AssemblyCentrifugeMiddleBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> CONVEYOR_SPLITTER = register(
      "conveyor_splitter", CrustyChunksModBlocks.CONVEYOR_SPLITTER, ConveyorSplitterBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ASSEMBLY_CIRCUIT_FABRICATOR = register(
      "assembly_circuit_fabricator", CrustyChunksModBlocks.ASSEMBLY_CIRCUIT_FABRICATOR, AssemblyCircuitFabricatorBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ASSEMBLY_MECHANICAL_FABRICATOR = register(
      "assembly_mechanical_fabricator", CrustyChunksModBlocks.ASSEMBLY_MECHANICAL_FABRICATOR, AssemblyMechanicalFabricatorBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> MINIGUN = register("minigun", CrustyChunksModBlocks.MINIGUN, MinigunBlockEntity::new);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MiniGunBarrelTileEntity>> MINI_GUN_BARREL = REGISTRY.register(
      "mini_gun_barrel", () -> Builder.of(MiniGunBarrelTileEntity::new, new Block[]{(Block)CrustyChunksModBlocks.MINI_GUN_BARREL.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ITEM_INCINERATOR = register(
      "item_incinerator", CrustyChunksModBlocks.ITEM_INCINERATOR, ItemIncineratorBlockEntity::new
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> REDIRECTOR_SHAFT = register(
      "redirector_shaft", CrustyChunksModBlocks.REDIRECTOR_SHAFT, RedirectorShaftBlockEntity::new
   );

   private static DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> register(String registryname, DeferredHolder<Block, Block> block, BlockEntitySupplier<?> supplier) {
      return REGISTRY.register(registryname, () -> Builder.of(supplier, new Block[]{(Block)block.get()}).build(null));
   }

   @SubscribeEvent
   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, REDSTONE_TNT.get(), (blockEntity, side) -> ((RedstoneTNTBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACHINE_GUN.get(), (blockEntity, side) -> ((MachineGunBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ARTILLERYBREECH.get(), (blockEntity, side) -> ((ArtillerybreechBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BATTLE_CANNON_BREECH.get(), (blockEntity, side) -> ((BattleCannonBreechBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BLAST_FURNACE.get(), (blockEntity, side) -> ((BlastFurnaceBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FOUNDRY.get(), (blockEntity, side) -> ((FoundryBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONVEYOR.get(), (blockEntity, side) -> ((ConveyorBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FIREBOX.get(), (blockEntity, side) -> ((FireboxBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AUTOCANNON.get(), (blockEntity, side) -> ((AutocannonBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AUTOCANNON_DRUM.get(), (blockEntity, side) -> ((AutocannonDrumBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AUTOLOADER.get(), (blockEntity, side) -> ((AutoloaderBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FISSION_BOMB.get(), (blockEntity, side) -> ((FissionBombBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MINERAL_GRINDER.get(), (blockEntity, side) -> ((MineralGrinderBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SMALL_BOMB.get(), (blockEntity, side) -> ((SmallBombBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CLUSTER_OF_BOMBS.get(), (blockEntity, side) -> ((ClusterOfBombsBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ARTILLERY_AUTOLOADER.get(), (blockEntity, side) -> ((ArtilleryAutoloaderBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ARTILLERY_CHARGE_LOADER.get(), (blockEntity, side) -> ((ArtilleryChargeLoaderBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ROCKET_POD.get(), (blockEntity, side) -> ((RocketPodBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, JET_EXHAUST.get(), (blockEntity, side) -> ((JetExhaustBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, JET_TURBINE.get(), (blockEntity, side) -> ((JetTurbineBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JET_TURBINE.get(), (blockEntity, side) -> ((JetTurbineBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, JET_COMPRESSOR.get(), (blockEntity, side) -> ((JetCompressorBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MORTAR.get(), (blockEntity, side) -> ((MortarBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AIMER_NODE.get(), (blockEntity, side) -> ((AimerNodeBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SIREN.get(), (blockEntity, side) -> ((SirenBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ROTARY_AUTO_CANNON.get(), (blockEntity, side) -> ((RotaryAutoCannonBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LIGHT_AUTOCANNON.get(), (blockEntity, side) -> ((LightAutocannonBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SMOKE_LAUNCHER.get(), (blockEntity, side) -> ((SmokeLauncherBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, REFINERY.get(), (blockEntity, side) -> ((RefineryBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, REFINERY_TOWER.get(), (blockEntity, side) -> ((RefineryTowerBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, REFINERY_TOWER.get(), (blockEntity, side) -> ((RefineryTowerBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, REACTION_CHAMBER.get(), (blockEntity, side) -> ((ReactionChamberBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BREEDER_REACTOR_PORT.get(), (blockEntity, side) -> ((BreederReactorPortBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, OIL_FIREBOX.get(), (blockEntity, side) -> ((OilFireboxBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, OIL_FIREBOX.get(), (blockEntity, side) -> ((OilFireboxBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SUMMONATOR.get(), (blockEntity, side) -> ((SummonatorBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ROBOT_CHUTE.get(), (blockEntity, side) -> ((RobotChuteBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SUMMONATOR_ACTIVE.get(), (blockEntity, side) -> ((SummonatorActiveBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ACTIVE_ROBOT_CHUTE.get(), (blockEntity, side) -> ((ActiveRobotChuteBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BREEDER_REACTOR_INTERFACE.get(), (blockEntity, side) -> ((BreederReactorInterfaceBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GAS_BOMB.get(), (blockEntity, side) -> ((GasBombBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GAS_DISPENSER.get(), (blockEntity, side) -> ((GasDispenserBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, NODE_TRIGGER.get(), (blockEntity, side) -> ((NodeTriggerBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, NODE_TRIGGER_ON.get(), (blockEntity, side) -> ((NodeTriggerOnBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, COUNTERMEASURE_DISPENSER.get(), (blockEntity, side) -> ((CountermeasureDispenserBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ORDINANCE_CORE.get(), (blockEntity, side) -> ((OrdinanceCoreBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ORDINANCE_INLINE_FISSION_WARHEAD.get(), (blockEntity, side) -> ((OrdinanceInlineFissionWarheadBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SUMMONATION.get(), (blockEntity, side) -> ((SummonationBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, OPEN_SUMMONATION.get(), (blockEntity, side) -> ((OpenSummonationBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DEFENSE_CORE.get(), (blockEntity, side) -> ((DefenseCoreBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FUSION_BOMB.get(), (blockEntity, side) -> ((FusionBombBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, JET_GEARBOX.get(), (blockEntity, side) -> ((JetGearboxBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GENERATOR.get(), (blockEntity, side) -> ((GeneratorBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, GENERATOR.get(), (blockEntity, side) -> ((GeneratorBlockEntity) blockEntity).getEnergyStorage());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FUEL_TANK.get(), (blockEntity, side) -> ((FuelTankBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FUEL_TANK.get(), (blockEntity, side) -> ((FuelTankBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, EXTENSION_SHAFT.get(), (blockEntity, side) -> ((ExtensionShaftBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DRIVE_SHAFT.get(), (blockEntity, side) -> ((DriveShaftBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENGINE_CYLLINDER.get(), (blockEntity, side) -> ((EngineCyllinderBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENGINE_CYLLINDER.get(), (blockEntity, side) -> ((EngineCyllinderBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENERGY_NODE.get(), (blockEntity, side) -> ((EnergyNodeBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENERGY_NODE.get(), (blockEntity, side) -> ((EnergyNodeBlockEntity) blockEntity).getEnergyStorage());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENERGY_BATTERY.get(), (blockEntity, side) -> ((EnergyBatteryBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENERGY_BATTERY.get(), (blockEntity, side) -> ((EnergyBatteryBlockEntity) blockEntity).getEnergyStorage());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LARGE_ELECTRIC_MOTOR.get(), (blockEntity, side) -> ((LargeElectricMotorBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LARGE_ELECTRIC_MOTOR.get(), (blockEntity, side) -> ((LargeElectricMotorBlockEntity) blockEntity).getEnergyStorage());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ELECTRIC_FIREBOX.get(), (blockEntity, side) -> ((ElectricFireboxBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ELECTRIC_FIREBOX.get(), (blockEntity, side) -> ((ElectricFireboxBlockEntity) blockEntity).getEnergyStorage());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, THERMAL_FURNACE.get(), (blockEntity, side) -> ((ThermalFurnaceBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, POWER_REACTOR_INTERFACE.get(), (blockEntity, side) -> ((PowerReactorInterfaceBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, POWER_REACTOR_PORT.get(), (blockEntity, side) -> ((PowerReactorPortBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, POWER_REACTOR_PORT.get(), (blockEntity, side) -> ((PowerReactorPortBlockEntity) blockEntity).getEnergyStorage());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ORDINANCE_INLINE_FUSION_WARHEAD_STAGE_1.get(), (blockEntity, side) -> ((OrdinanceInlineFusionWarheadStage1BlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENERGY_DISTRIBUTION_NODE.get(), (blockEntity, side) -> ((EnergyDistributionNodeBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENERGY_DISTRIBUTION_NODE.get(), (blockEntity, side) -> ((EnergyDistributionNodeBlockEntity) blockEntity).getEnergyStorage());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MANUAL_CRANK.get(), (blockEntity, side) -> ((ManualCrankBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LARGE_ROCKET_POD.get(), (blockEntity, side) -> ((LargeRocketPodBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, EMPTY_MISSILE_HARDPOINT.get(), (blockEntity, side) -> ((EmptyMissileHardpointBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FIRE_SPEAR_MISSILE_HARDPOINT.get(), (blockEntity, side) -> ((FireSpearMissileHardpointBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SEEKER_SPEAR_MISSILE_HARDPOINT.get(), (blockEntity, side) -> ((SeekerSpearMissileHardpointBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STRIKE_SPEAR_MISSILE_HARDPOINT.get(), (blockEntity, side) -> ((StrikeSpearMissileHardpointBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MANUAL_AIMER.get(), (blockEntity, side) -> ((ManualAimerBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SOLAR_GENERATOR.get(), (blockEntity, side) -> ((SolarGeneratorBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RADAR_SPEAR_MISSILE_HARDPOINT.get(), (blockEntity, side) -> ((RadarSpearMissileHardpointBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ORDINANCE_CONTROLLER.get(), (blockEntity, side) -> ((OrdinanceControllerBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FUEL_TANK_MODULE.get(), (blockEntity, side) -> ((FuelTankModuleBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FUEL_TANK_MODULE.get(), (blockEntity, side) -> ((FuelTankModuleBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DAMAGEDFUELTANK.get(), (blockEntity, side) -> ((DamagedfueltankBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DAMAGEDFUELTANK.get(), (blockEntity, side) -> ((DamagedfueltankBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FUEL_TANK_INPUT.get(), (blockEntity, side) -> ((FuelTankInputBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FUEL_TANK_INPUT.get(), (blockEntity, side) -> ((FuelTankInputBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, HEAVY_MACHINE_GUN.get(), (blockEntity, side) -> ((HeavyMachineGunBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LIGHT_MACHINE_GUN.get(), (blockEntity, side) -> ((LightMachineGunBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MEDIUM_PETROL_ENGINE.get(), (blockEntity, side) -> ((MediumPetrolEngineBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, MEDIUM_PETROL_ENGINE.get(), (blockEntity, side) -> ((MediumPetrolEngineBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SMALL_DIESEL_ENGINE.get(), (blockEntity, side) -> ((SmallDieselEngineBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SMALL_DIESEL_ENGINE.get(), (blockEntity, side) -> ((SmallDieselEngineBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FLAME_THROWER.get(), (blockEntity, side) -> ((FlameThrowerBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLAME_THROWER.get(), (blockEntity, side) -> ((FlameThrowerBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MEDIUM_DIESEL_ENGINE.get(), (blockEntity, side) -> ((MediumDieselEngineBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, MEDIUM_DIESEL_ENGINE.get(), (blockEntity, side) -> ((MediumDieselEngineBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SMALL_PETROL_ENGINE.get(), (blockEntity, side) -> ((SmallPetrolEngineBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SMALL_PETROL_ENGINE.get(), (blockEntity, side) -> ((SmallPetrolEngineBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PRODUCTION_INPUT.get(), (blockEntity, side) -> ((ProductionInputBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PRODUCTION_OUTPUT.get(), (blockEntity, side) -> ((ProductionOutputBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ASSEMBLY_DEPOT.get(), (blockEntity, side) -> ((AssemblyDepotBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ASSEMBLY_MACHINE.get(), (blockEntity, side) -> ((AssemblyMachineBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ASSEMBLY_CRUSHER.get(), (blockEntity, side) -> ((AssemblyCrusherBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ASSEMBLY_FURNACE.get(), (blockEntity, side) -> ((AssemblyFurnaceBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BAUXITE_DIGESTER.get(), (blockEntity, side) -> ((BauxiteDigesterBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ASSEMBLY_CENTRIFUGE_MIDDLE.get(), (blockEntity, side) -> ((AssemblyCentrifugeMiddleBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONVEYOR_SPLITTER.get(), (blockEntity, side) -> ((ConveyorSplitterBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ASSEMBLY_CIRCUIT_FABRICATOR.get(), (blockEntity, side) -> ((AssemblyCircuitFabricatorBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ASSEMBLY_MECHANICAL_FABRICATOR.get(), (blockEntity, side) -> ((AssemblyMechanicalFabricatorBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MINIGUN.get(), (blockEntity, side) -> ((MinigunBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ITEM_INCINERATOR.get(), (blockEntity, side) -> ((ItemIncineratorBlockEntity) blockEntity).getItemHandler());
      event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ITEM_INCINERATOR.get(), (blockEntity, side) -> ((ItemIncineratorBlockEntity) blockEntity).getFluidTank());
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, REDIRECTOR_SHAFT.get(), (blockEntity, side) -> ((RedirectorShaftBlockEntity) blockEntity).getItemHandler());
   }
}
