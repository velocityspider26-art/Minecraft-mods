package net.mcreator.crustychunks.init;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.block.AIMineBlock;
import net.mcreator.crustychunks.block.ActiveRobotChuteBlock;
import net.mcreator.crustychunks.block.AdvancedAlloyBlockBlock;
import net.mcreator.crustychunks.block.AfterBurnerBlock;
import net.mcreator.crustychunks.block.AimerNodeBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelBlackBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelBlueBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelBrownBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelCyanBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelDarkGrayBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelGrayBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelGreenBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelLightBlueBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelLightGrayBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelLimeBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelMagentaBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelOrangeBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelPinkBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelPurpleBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelRedBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelWhiteBlock;
import net.mcreator.crustychunks.block.AluminumACBarrelYellowBlock;
import net.mcreator.crustychunks.block.AluminumBlockBlock;
import net.mcreator.crustychunks.block.AluminumPlatingBlackBlock;
import net.mcreator.crustychunks.block.AluminumPlatingBlock;
import net.mcreator.crustychunks.block.AluminumPlatingBlueBlock;
import net.mcreator.crustychunks.block.AluminumPlatingBrownBlock;
import net.mcreator.crustychunks.block.AluminumPlatingCyanBlock;
import net.mcreator.crustychunks.block.AluminumPlatingDarkGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingGreenBlock;
import net.mcreator.crustychunks.block.AluminumPlatingLightBlueBlock;
import net.mcreator.crustychunks.block.AluminumPlatingLightGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingLimeBlock;
import net.mcreator.crustychunks.block.AluminumPlatingMagentaBlock;
import net.mcreator.crustychunks.block.AluminumPlatingOrangeBlock;
import net.mcreator.crustychunks.block.AluminumPlatingPinkBlock;
import net.mcreator.crustychunks.block.AluminumPlatingPurpleBlock;
import net.mcreator.crustychunks.block.AluminumPlatingRedBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSidePanelBlueBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSidePanelBrownBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSidePanelCyanBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSidePanelOrangeBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSidePanelRedBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabBlackBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabBlueBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabBrownBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabCyanBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabDarkGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabGreenBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabLightBlueBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabLightGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabLimeBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabMagentaBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabOrangeBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabPinkBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabPurpleBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabRedBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabWhiteBlock;
import net.mcreator.crustychunks.block.AluminumPlatingSlabYellowBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsBlackBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsBlueBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsBrownBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsCyanBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsDarkGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsGreenBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsLightBlueBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsLightGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsLimeBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsMagentaBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsOrangeBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsPinkBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsPurpleBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsRedBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsWhiteBlock;
import net.mcreator.crustychunks.block.AluminumPlatingStairsYellowBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorBlackBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorBlueBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorBrownBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorCyanBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorDarkGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorGreenBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorLightBlueBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorLightGrayBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorLimeBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorMagentaBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorOrangeBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorPinkBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorPurpleBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorRedBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorWhiteBlock;
import net.mcreator.crustychunks.block.AluminumPlatingTrapdoorYellowBlock;
import net.mcreator.crustychunks.block.AluminumPlatingWhiteBlock;
import net.mcreator.crustychunks.block.AluminumPlatingYellowBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelDarkGrayBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelGreenBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelPlatingBlackBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelPlatingGrayBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelPlatingLightBlueBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelPlatingLightGrayBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelPlatingLimeBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelPlatingMagentaBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelPlatingPinkBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelPlatingPurpleBlock;
import net.mcreator.crustychunks.block.AluminumSidePanelYellowBlock;
import net.mcreator.crustychunks.block.AlumiumPlatingSidePanelWhiteBlock;
import net.mcreator.crustychunks.block.AncientLightBlock;
import net.mcreator.crustychunks.block.AncientWellBlock;
import net.mcreator.crustychunks.block.ArtilleryAutoloaderBlock;
import net.mcreator.crustychunks.block.ArtilleryBarrelBlock;
import net.mcreator.crustychunks.block.ArtilleryChargeLoaderBlock;
import net.mcreator.crustychunks.block.ArtillerybreechBlock;
import net.mcreator.crustychunks.block.AsphaltBlock;
import net.mcreator.crustychunks.block.AsphaltSlabBlock;
import net.mcreator.crustychunks.block.AssemblyCentrifugeBottomBlock;
import net.mcreator.crustychunks.block.AssemblyCentrifugeMiddleBlock;
import net.mcreator.crustychunks.block.AssemblyCentrifugeTopBlock;
import net.mcreator.crustychunks.block.AssemblyCircuitFabricatorBlock;
import net.mcreator.crustychunks.block.AssemblyCrusherBlock;
import net.mcreator.crustychunks.block.AssemblyDepotBlock;
import net.mcreator.crustychunks.block.AssemblyFurnaceBlock;
import net.mcreator.crustychunks.block.AssemblyMachineBlock;
import net.mcreator.crustychunks.block.AssemblyMechanicalFabricatorBlock;
import net.mcreator.crustychunks.block.AutocannonBarrelBlock;
import net.mcreator.crustychunks.block.AutocannonBlock;
import net.mcreator.crustychunks.block.AutocannonDrumBlock;
import net.mcreator.crustychunks.block.AutoloaderBlock;
import net.mcreator.crustychunks.block.BattleCannonBarrelBlock;
import net.mcreator.crustychunks.block.BattleCannonBreechBlock;
import net.mcreator.crustychunks.block.BattleCannonMantletBlock;
import net.mcreator.crustychunks.block.BauxiteBlock;
import net.mcreator.crustychunks.block.BauxiteDigesterBlock;
import net.mcreator.crustychunks.block.BerylliumBlockBlock;
import net.mcreator.crustychunks.block.BerylliumOreBlock;
import net.mcreator.crustychunks.block.BlackArmorBlock;
import net.mcreator.crustychunks.block.BlackArmorOpticBlock;
import net.mcreator.crustychunks.block.BlackArmorSlabBlock;
import net.mcreator.crustychunks.block.BlackArmorStairsBlock;
import net.mcreator.crustychunks.block.BlackArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.BlastFunnelBlock;
import net.mcreator.crustychunks.block.BlastFurnaceBlock;
import net.mcreator.crustychunks.block.BlastFurnaceBricksBlock;
import net.mcreator.crustychunks.block.BlueArmorBlock;
import net.mcreator.crustychunks.block.BlueArmorOpticBlock;
import net.mcreator.crustychunks.block.BlueArmorSlabBlock;
import net.mcreator.crustychunks.block.BlueArmorStairsBlock;
import net.mcreator.crustychunks.block.BlueArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.BrassBlockBlock;
import net.mcreator.crustychunks.block.BreederReactorCoreBlock;
import net.mcreator.crustychunks.block.BreederReactorInterfaceBlock;
import net.mcreator.crustychunks.block.BreederReactorPortBlock;
import net.mcreator.crustychunks.block.BrownArmorBlock;
import net.mcreator.crustychunks.block.BrownArmorOpticBlock;
import net.mcreator.crustychunks.block.BrownArmorSlabBlock;
import net.mcreator.crustychunks.block.BrownArmorStairsBlock;
import net.mcreator.crustychunks.block.BrownArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.BurntgrassBlock;
import net.mcreator.crustychunks.block.CharredBlockBlock;
import net.mcreator.crustychunks.block.ChlorineGasBlock;
import net.mcreator.crustychunks.block.ClusterOfBombsBlock;
import net.mcreator.crustychunks.block.CompressedAirBlock;
import net.mcreator.crustychunks.block.ConcreteWallBlock;
import net.mcreator.crustychunks.block.ControlRodBlock;
import net.mcreator.crustychunks.block.ConveyorBlock;
import net.mcreator.crustychunks.block.ConveyorSplitterBlock;
import net.mcreator.crustychunks.block.CountermeasureDispenserBlock;
import net.mcreator.crustychunks.block.CoveredFlameThrowerBarrelBlock;
import net.mcreator.crustychunks.block.CoveredMachineGunBarrelBlock;
import net.mcreator.crustychunks.block.CrackedConcreteBlock;
import net.mcreator.crustychunks.block.CrackedConcreteWallBlock;
import net.mcreator.crustychunks.block.CrudeOilBlock;
import net.mcreator.crustychunks.block.CyanArmorBlock;
import net.mcreator.crustychunks.block.CyanArmorOpticBlock;
import net.mcreator.crustychunks.block.CyanArmorSlabBlock;
import net.mcreator.crustychunks.block.CyanArmorStairsBlock;
import net.mcreator.crustychunks.block.CyanArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.DamagedConcreteBlock;
import net.mcreator.crustychunks.block.DamagedConcreteWallBlock;
import net.mcreator.crustychunks.block.DamagedfueltankBlock;
import net.mcreator.crustychunks.block.DeepslateLeadOreBlock;
import net.mcreator.crustychunks.block.DefenseCoreBlock;
import net.mcreator.crustychunks.block.DestroyedConcreteBlock;
import net.mcreator.crustychunks.block.DestroyedConcreteWallBlock;
import net.mcreator.crustychunks.block.DieselBlock;
import net.mcreator.crustychunks.block.DriveShaftBlock;
import net.mcreator.crustychunks.block.ERA1Block;
import net.mcreator.crustychunks.block.ERA2Block;
import net.mcreator.crustychunks.block.ERA3Block;
import net.mcreator.crustychunks.block.ERA4Block;
import net.mcreator.crustychunks.block.ElectricFireboxBlock;
import net.mcreator.crustychunks.block.EmptyFuelRodsBlock;
import net.mcreator.crustychunks.block.EmptyMissileHardpointBlock;
import net.mcreator.crustychunks.block.EnergyBatteryBlock;
import net.mcreator.crustychunks.block.EnergyDistributionNodeBlock;
import net.mcreator.crustychunks.block.EnergyNodeBlock;
import net.mcreator.crustychunks.block.EngineCyllinderBlock;
import net.mcreator.crustychunks.block.ExplosiveBarrelBlock;
import net.mcreator.crustychunks.block.ExtensionShaftBlock;
import net.mcreator.crustychunks.block.FireSpearMissileHardpointBlock;
import net.mcreator.crustychunks.block.FireboxBlock;
import net.mcreator.crustychunks.block.FissionBombBlock;
import net.mcreator.crustychunks.block.FlameThrowerBarrelBlock;
import net.mcreator.crustychunks.block.FlameThrowerBlock;
import net.mcreator.crustychunks.block.FoundryBlock;
import net.mcreator.crustychunks.block.FracturedConcreteBlock;
import net.mcreator.crustychunks.block.FracturedConcreteWallBlock;
import net.mcreator.crustychunks.block.FuelRods1Block;
import net.mcreator.crustychunks.block.FuelRods2Block;
import net.mcreator.crustychunks.block.FuelRods3Block;
import net.mcreator.crustychunks.block.FuelRods4Block;
import net.mcreator.crustychunks.block.FuelTankBlock;
import net.mcreator.crustychunks.block.FuelTankInputBlock;
import net.mcreator.crustychunks.block.FuelTankModuleBlock;
import net.mcreator.crustychunks.block.FusionBombBlock;
import net.mcreator.crustychunks.block.GasBombBlock;
import net.mcreator.crustychunks.block.GasDispenserBlock;
import net.mcreator.crustychunks.block.GeneratorBlock;
import net.mcreator.crustychunks.block.GiantCoilBlock;
import net.mcreator.crustychunks.block.GlassTrapdoorBlock;
import net.mcreator.crustychunks.block.GrayArmorBlock;
import net.mcreator.crustychunks.block.GrayArmorOpticBlock;
import net.mcreator.crustychunks.block.GrayArmorSlabBlock;
import net.mcreator.crustychunks.block.GrayArmorStairsBlock;
import net.mcreator.crustychunks.block.GrayArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.GreenArmorBlock;
import net.mcreator.crustychunks.block.GreenArmorOpticBlock;
import net.mcreator.crustychunks.block.GreenArmorSlabBlock;
import net.mcreator.crustychunks.block.GreenArmorStairsBlock;
import net.mcreator.crustychunks.block.GreenArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.HarddirtBlock;
import net.mcreator.crustychunks.block.HeavyMachineGunBlock;
import net.mcreator.crustychunks.block.HydrazineBlock;
import net.mcreator.crustychunks.block.ItemIncineratorBlock;
import net.mcreator.crustychunks.block.JetCompressorBlock;
import net.mcreator.crustychunks.block.JetExhaustBlock;
import net.mcreator.crustychunks.block.JetGearboxBlock;
import net.mcreator.crustychunks.block.JetTurbineBlock;
import net.mcreator.crustychunks.block.KeroseneBlock;
import net.mcreator.crustychunks.block.LandMineBlock;
import net.mcreator.crustychunks.block.LargeElectricMotorBlock;
import net.mcreator.crustychunks.block.LargeEngineSmokestackBlock;
import net.mcreator.crustychunks.block.LargeRocketPodBlock;
import net.mcreator.crustychunks.block.LargeRocketPodChamberBlock;
import net.mcreator.crustychunks.block.LeadBlockBlock;
import net.mcreator.crustychunks.block.LeadOreBlock;
import net.mcreator.crustychunks.block.LightAutocannonBlock;
import net.mcreator.crustychunks.block.LightBlueArmorBlock;
import net.mcreator.crustychunks.block.LightBlueArmorOpticBlock;
import net.mcreator.crustychunks.block.LightBlueArmorSlabBlock;
import net.mcreator.crustychunks.block.LightBlueArmorStairsBlock;
import net.mcreator.crustychunks.block.LightBlueArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.LightGrayArmorBlock;
import net.mcreator.crustychunks.block.LightGrayArmorOpticBlock;
import net.mcreator.crustychunks.block.LightGrayArmorSlabBlock;
import net.mcreator.crustychunks.block.LightGrayArmorStairsBlock;
import net.mcreator.crustychunks.block.LightGrayArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.LightMachineGunBlock;
import net.mcreator.crustychunks.block.LightWoodBlockBlock;
import net.mcreator.crustychunks.block.LightWoodFrameBlock;
import net.mcreator.crustychunks.block.LightWoodSidePanelBlock;
import net.mcreator.crustychunks.block.LightWoodSlabBlock;
import net.mcreator.crustychunks.block.LightWoodStairsBlock;
import net.mcreator.crustychunks.block.LightWoodTrapdoorBlock;
import net.mcreator.crustychunks.block.LimeArmorBlock;
import net.mcreator.crustychunks.block.LimeArmorOpticBlock;
import net.mcreator.crustychunks.block.LimeArmorSlabBlock;
import net.mcreator.crustychunks.block.LimeArmorStairsBlock;
import net.mcreator.crustychunks.block.LimeArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.LiquidHydrogenBlock;
import net.mcreator.crustychunks.block.LiquidOxygenBlock;
import net.mcreator.crustychunks.block.LithiumBlockBlock;
import net.mcreator.crustychunks.block.LithiumOreBlock;
import net.mcreator.crustychunks.block.LootBoxBlock;
import net.mcreator.crustychunks.block.MachineGunBarrelBlock;
import net.mcreator.crustychunks.block.MachineGunBlock;
import net.mcreator.crustychunks.block.MagentaArmorBlock;
import net.mcreator.crustychunks.block.MagentaArmorOpticBlock;
import net.mcreator.crustychunks.block.MagentaArmorSlabBlock;
import net.mcreator.crustychunks.block.MagentaArmorStairsBlock;
import net.mcreator.crustychunks.block.MagentaArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.ManualAimerBlock;
import net.mcreator.crustychunks.block.ManualCrankBlock;
import net.mcreator.crustychunks.block.MediumDieselEngineBlock;
import net.mcreator.crustychunks.block.MediumPetrolEngineBlock;
import net.mcreator.crustychunks.block.MineralGrinderBlock;
import net.mcreator.crustychunks.block.MiniGunBarrelBlock;
import net.mcreator.crustychunks.block.MinigunBlock;
import net.mcreator.crustychunks.block.MortarBlock;
import net.mcreator.crustychunks.block.NickelBlockBlock;
import net.mcreator.crustychunks.block.NickelOreBlock;
import net.mcreator.crustychunks.block.NiobiumBlockBlock;
import net.mcreator.crustychunks.block.NitrateBlockBlock;
import net.mcreator.crustychunks.block.NodeTriggerBlock;
import net.mcreator.crustychunks.block.NodeTriggerOnBlock;
import net.mcreator.crustychunks.block.OffsetERA1Block;
import net.mcreator.crustychunks.block.OffsetERA2Block;
import net.mcreator.crustychunks.block.OffsetERA3Block;
import net.mcreator.crustychunks.block.OffsetERA4Block;
import net.mcreator.crustychunks.block.OilBlock;
import net.mcreator.crustychunks.block.OilFireboxBlock;
import net.mcreator.crustychunks.block.OpenSummonationBlock;
import net.mcreator.crustychunks.block.OrangeArmorBlock;
import net.mcreator.crustychunks.block.OrangeArmorOpticBlock;
import net.mcreator.crustychunks.block.OrangeArmorSlabBlock;
import net.mcreator.crustychunks.block.OrangeArmorStairsBlock;
import net.mcreator.crustychunks.block.OrangeArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.OrdinanceClusterWarheadBlock;
import net.mcreator.crustychunks.block.OrdinanceControllerBlock;
import net.mcreator.crustychunks.block.OrdinanceCoreBlock;
import net.mcreator.crustychunks.block.OrdinanceFinsBlock;
import net.mcreator.crustychunks.block.OrdinanceFissionInitiatorHeadBlock;
import net.mcreator.crustychunks.block.OrdinanceHeavyWarheadBlock;
import net.mcreator.crustychunks.block.OrdinanceIRSeekerHeadBlock;
import net.mcreator.crustychunks.block.OrdinanceIncendiaryWarheadBlock;
import net.mcreator.crustychunks.block.OrdinanceInlineFissionWarheadBlock;
import net.mcreator.crustychunks.block.OrdinanceInlineFusionWarheadStage1Block;
import net.mcreator.crustychunks.block.OrdinanceInlineFusionWarheadStage2Block;
import net.mcreator.crustychunks.block.OrdinanceInlineWarheadBlock;
import net.mcreator.crustychunks.block.OrdinanceKineticHeadBlock;
import net.mcreator.crustychunks.block.OrdinanceRelocatorBlock;
import net.mcreator.crustychunks.block.OrdinanceSARHSeekerBlock;
import net.mcreator.crustychunks.block.OrdinanceThrusterBlock;
import net.mcreator.crustychunks.block.OvergrownReenforcedConcreteBlock;
import net.mcreator.crustychunks.block.PassengerSeatBlock;
import net.mcreator.crustychunks.block.PetroliumBlock;
import net.mcreator.crustychunks.block.PhosphateBlockBlock;
import net.mcreator.crustychunks.block.PinkArmorBlock;
import net.mcreator.crustychunks.block.PinkArmorOpticBlock;
import net.mcreator.crustychunks.block.PinkArmorSlabBlock;
import net.mcreator.crustychunks.block.PinkArmorStairsBlock;
import net.mcreator.crustychunks.block.PinkArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.PlutoniumBlockBlock;
import net.mcreator.crustychunks.block.PolishedBauxiteBlock;
import net.mcreator.crustychunks.block.PolishedTrinititeBlock;
import net.mcreator.crustychunks.block.PowerReactorInterfaceBlock;
import net.mcreator.crustychunks.block.PowerReactorPortBlock;
import net.mcreator.crustychunks.block.ProductionInputBlock;
import net.mcreator.crustychunks.block.ProductionOutputBlock;
import net.mcreator.crustychunks.block.PurpleArmorBlock;
import net.mcreator.crustychunks.block.PurpleArmorOpticBlock;
import net.mcreator.crustychunks.block.PurpleArmorSlabBlock;
import net.mcreator.crustychunks.block.PurpleArmorStairsBlock;
import net.mcreator.crustychunks.block.PurpleArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.PyrochloreBlockBlock;
import net.mcreator.crustychunks.block.PyrochloreOreBlock;
import net.mcreator.crustychunks.block.RACBarrelBlock;
import net.mcreator.crustychunks.block.RadarSpearMissileHardpointBlock;
import net.mcreator.crustychunks.block.RadioactiveAshBlock;
import net.mcreator.crustychunks.block.RadioactiveAshFullBlockBlock;
import net.mcreator.crustychunks.block.RawBerylliumBlockBlock;
import net.mcreator.crustychunks.block.RawLeadBlockBlock;
import net.mcreator.crustychunks.block.RawLithiumBlockBlock;
import net.mcreator.crustychunks.block.RawNickelBlockBlock;
import net.mcreator.crustychunks.block.RawUraniumBlockBlock;
import net.mcreator.crustychunks.block.RawZincBlockBlock;
import net.mcreator.crustychunks.block.RazorWireBlock;
import net.mcreator.crustychunks.block.ReactionChamberBlock;
import net.mcreator.crustychunks.block.ReactorCasingBlock;
import net.mcreator.crustychunks.block.RebarBlock;
import net.mcreator.crustychunks.block.RedArmorBlock;
import net.mcreator.crustychunks.block.RedArmorOpticBlock;
import net.mcreator.crustychunks.block.RedArmorSlabBlock;
import net.mcreator.crustychunks.block.RedArmorStairsBlock;
import net.mcreator.crustychunks.block.RedArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.RedirectorShaftBlock;
import net.mcreator.crustychunks.block.RedstoneTNTBlock;
import net.mcreator.crustychunks.block.ReenforcedConcreteBlock;
import net.mcreator.crustychunks.block.RefineryBlock;
import net.mcreator.crustychunks.block.RefineryTowerBlock;
import net.mcreator.crustychunks.block.ReinforcedGlassBlock;
import net.mcreator.crustychunks.block.ReinforcedGlassStairsBlock;
import net.mcreator.crustychunks.block.ReinforcedGlassTrapdoorBlock;
import net.mcreator.crustychunks.block.RobotChuteBlock;
import net.mcreator.crustychunks.block.RocketPodBlock;
import net.mcreator.crustychunks.block.RocketPodChamberBlock;
import net.mcreator.crustychunks.block.RotaryAutoCannonBlock;
import net.mcreator.crustychunks.block.RustyBlockBlock;
import net.mcreator.crustychunks.block.RustySlabBlock;
import net.mcreator.crustychunks.block.RustyStairsBlock;
import net.mcreator.crustychunks.block.RustyTrapdoorBlock;
import net.mcreator.crustychunks.block.SandBagsBlock;
import net.mcreator.crustychunks.block.ScorchDirtBlock;
import net.mcreator.crustychunks.block.SeekerSpearMissileHardpointBlock;
import net.mcreator.crustychunks.block.SheetMetalBlock;
import net.mcreator.crustychunks.block.SheetMetalPaneBlock;
import net.mcreator.crustychunks.block.SheetMetalSlabBlock;
import net.mcreator.crustychunks.block.SheetMetalStairsBlock;
import net.mcreator.crustychunks.block.SirenBlock;
import net.mcreator.crustychunks.block.SmallBombBlock;
import net.mcreator.crustychunks.block.SmallDieselEngineBlock;
import net.mcreator.crustychunks.block.SmallPetrolEngineBlock;
import net.mcreator.crustychunks.block.SmokeBombBlock;
import net.mcreator.crustychunks.block.SmokeLauncherBlock;
import net.mcreator.crustychunks.block.SolarGeneratorBlock;
import net.mcreator.crustychunks.block.SteelBlockBlock;
import net.mcreator.crustychunks.block.SteelDoorBlock;
import net.mcreator.crustychunks.block.SteelOpticBlock;
import net.mcreator.crustychunks.block.SteelPlatingBlock;
import net.mcreator.crustychunks.block.SteelPlatingSlabBlock;
import net.mcreator.crustychunks.block.SteelPlatingStairsBlock;
import net.mcreator.crustychunks.block.SteelTrapdoorBlock;
import net.mcreator.crustychunks.block.SteelTrussBlock;
import net.mcreator.crustychunks.block.StrikeSpearMissileHardpointBlock;
import net.mcreator.crustychunks.block.StructuralConcreteBlock;
import net.mcreator.crustychunks.block.SulfurBlockBlock;
import net.mcreator.crustychunks.block.SulfurOreBlock;
import net.mcreator.crustychunks.block.SulfuricAcidBlock;
import net.mcreator.crustychunks.block.SummonationBlock;
import net.mcreator.crustychunks.block.SummonatorActiveBlock;
import net.mcreator.crustychunks.block.SummonatorBlock;
import net.mcreator.crustychunks.block.SummonatorModuleBlock;
import net.mcreator.crustychunks.block.TarBlock;
import net.mcreator.crustychunks.block.ThermalFurnaceBlock;
import net.mcreator.crustychunks.block.ThickBattleCannonBarrelBlock;
import net.mcreator.crustychunks.block.TintedGlassStairsBlock;
import net.mcreator.crustychunks.block.TintedGlassTrapdoorBlock;
import net.mcreator.crustychunks.block.TorpedoThrusterBlock;
import net.mcreator.crustychunks.block.TrinititeBlock;
import net.mcreator.crustychunks.block.TrinititeGlassBlock;
import net.mcreator.crustychunks.block.TrinititeGlassStairsBlock;
import net.mcreator.crustychunks.block.TrinititeGlassTrapdoorBlock;
import net.mcreator.crustychunks.block.Type1BCMuzzleBrakeBlock;
import net.mcreator.crustychunks.block.Type2BCMuzzleBrakeBlock;
import net.mcreator.crustychunks.block.UraniumDepletedBlockBlock;
import net.mcreator.crustychunks.block.UraniumEnrichedBlockBlock;
import net.mcreator.crustychunks.block.UraniumNeutralBlockBlock;
import net.mcreator.crustychunks.block.UraniumOreBlock;
import net.mcreator.crustychunks.block.WhiteArmorBlock;
import net.mcreator.crustychunks.block.WhiteArmorOpticBlock;
import net.mcreator.crustychunks.block.WhiteArmorSlabBlock;
import net.mcreator.crustychunks.block.WhiteArmorStairsBlock;
import net.mcreator.crustychunks.block.WhiteArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.WireFenceBlock;
import net.mcreator.crustychunks.block.YellowArmorBlock;
import net.mcreator.crustychunks.block.YellowArmorOpticBlock;
import net.mcreator.crustychunks.block.YellowArmorSlabBlock;
import net.mcreator.crustychunks.block.YellowArmorStairsBlock;
import net.mcreator.crustychunks.block.YellowArmorTrapdoorBlock;
import net.mcreator.crustychunks.block.ZincBlockBlock;
import net.mcreator.crustychunks.block.ZincOreBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CrustyChunksModBlocks {
   public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK, "crusty_chunks");
   public static final DeferredHolder<Block, Block> REDSTONE_TNT = REGISTRY.register("redstone_tnt", () -> new RedstoneTNTBlock());
   public static final DeferredHolder<Block, Block> EXPLOSIVE_BARREL = REGISTRY.register("explosive_barrel", () -> new ExplosiveBarrelBlock());
   public static final DeferredHolder<Block, Block> BURNTGRASS = REGISTRY.register("burntgrass", () -> new BurntgrassBlock());
   public static final DeferredHolder<Block, Block> HARDDIRT = REGISTRY.register("harddirt", () -> new HarddirtBlock());
   public static final DeferredHolder<Block, Block> ZINC_ORE = REGISTRY.register("zinc_ore", () -> new ZincOreBlock());
   public static final DeferredHolder<Block, Block> ZINC_BLOCK = REGISTRY.register("zinc_block", () -> new ZincBlockBlock());
   public static final DeferredHolder<Block, Block> BRASS_BLOCK = REGISTRY.register("brass_block", () -> new BrassBlockBlock());
   public static final DeferredHolder<Block, Block> STEEL_BLOCK = REGISTRY.register("steel_block", () -> new SteelBlockBlock());
   public static final DeferredHolder<Block, Block> LEAD_ORE = REGISTRY.register("lead_ore", () -> new LeadOreBlock());
   public static final DeferredHolder<Block, Block> LEAD_BLOCK = REGISTRY.register("lead_block", () -> new LeadBlockBlock());
   public static final DeferredHolder<Block, Block> NICKEL_ORE = REGISTRY.register("nickel_ore", () -> new NickelOreBlock());
   public static final DeferredHolder<Block, Block> NICKEL_BLOCK = REGISTRY.register("nickel_block", () -> new NickelBlockBlock());
   public static final DeferredHolder<Block, Block> TAR = REGISTRY.register("tar", () -> new TarBlock());
   public static final DeferredHolder<Block, Block> STEEL_PLATING = REGISTRY.register("steel_plating", () -> new SteelPlatingBlock());
   public static final DeferredHolder<Block, Block> STEEL_PLATING_SLAB = REGISTRY.register("steel_plating_slab", () -> new SteelPlatingSlabBlock());
   public static final DeferredHolder<Block, Block> STEEL_PLATING_STAIRS = REGISTRY.register("steel_plating_stairs", () -> new SteelPlatingStairsBlock());
   public static final DeferredHolder<Block, Block> SMOKE_BOMB = REGISTRY.register("smoke_bomb", () -> new SmokeBombBlock());
   public static final DeferredHolder<Block, Block> STEEL_TRAPDOOR = REGISTRY.register("steel_trapdoor", () -> new SteelTrapdoorBlock());
   public static final DeferredHolder<Block, Block> STEEL_OPTIC = REGISTRY.register("steel_optic", () -> new SteelOpticBlock());
   public static final DeferredHolder<Block, Block> CHARRED_BLOCK = REGISTRY.register("charred_block", () -> new CharredBlockBlock());
   public static final DeferredHolder<Block, Block> REENFORCED_CONCRETE = REGISTRY.register("reenforced_concrete", () -> new ReenforcedConcreteBlock());
   public static final DeferredHolder<Block, Block> CRACKED_CONCRETE = REGISTRY.register("cracked_concrete", () -> new CrackedConcreteBlock());
   public static final DeferredHolder<Block, Block> FRACTURED_CONCRETE = REGISTRY.register("fractured_concrete", () -> new FracturedConcreteBlock());
   public static final DeferredHolder<Block, Block> DAMAGED_CONCRETE = REGISTRY.register("damaged_concrete", () -> new DamagedConcreteBlock());
   public static final DeferredHolder<Block, Block> DESTROYED_CONCRETE = REGISTRY.register("destroyed_concrete", () -> new DestroyedConcreteBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_BLOCK = REGISTRY.register("aluminum_block", () -> new AluminumBlockBlock());
   public static final DeferredHolder<Block, Block> MACHINE_GUN = REGISTRY.register("machine_gun", () -> new MachineGunBlock());
   public static final DeferredHolder<Block, Block> MACHINE_GUN_BARREL = REGISTRY.register("machine_gun_barrel", () -> new MachineGunBarrelBlock());
   public static final DeferredHolder<Block, Block> ARTILLERYBREECH = REGISTRY.register("artillerybreech", () -> new ArtillerybreechBlock());
   public static final DeferredHolder<Block, Block> ARTILLERY_BARREL = REGISTRY.register("artillery_barrel", () -> new ArtilleryBarrelBlock());
   public static final DeferredHolder<Block, Block> BATTLE_CANNON_BREECH = REGISTRY.register("battle_cannon_breech", () -> new BattleCannonBreechBlock());
   public static final DeferredHolder<Block, Block> BATTLE_CANNON_BARREL = REGISTRY.register("battle_cannon_barrel", () -> new BattleCannonBarrelBlock());
   public static final DeferredHolder<Block, Block> BLAST_FURNACE = REGISTRY.register("blast_furnace", () -> new BlastFurnaceBlock());
   public static final DeferredHolder<Block, Block> BLAST_FURNACE_BRICKS = REGISTRY.register("blast_furnace_bricks", () -> new BlastFurnaceBricksBlock());
   public static final DeferredHolder<Block, Block> BLAST_FUNNEL = REGISTRY.register("blast_funnel", () -> new BlastFunnelBlock());
   public static final DeferredHolder<Block, Block> FOUNDRY = REGISTRY.register("foundry", () -> new FoundryBlock());
   public static final DeferredHolder<Block, Block> CONVEYOR = REGISTRY.register("conveyor", () -> new ConveyorBlock());
   public static final DeferredHolder<Block, Block> FIREBOX = REGISTRY.register("firebox", () -> new FireboxBlock());
   public static final DeferredHolder<Block, Block> AUTOCANNON = REGISTRY.register("autocannon", () -> new AutocannonBlock());
   public static final DeferredHolder<Block, Block> AUTOCANNON_BARREL = REGISTRY.register("autocannon_barrel", () -> new AutocannonBarrelBlock());
   public static final DeferredHolder<Block, Block> AUTOCANNON_DRUM = REGISTRY.register("autocannon_drum", () -> new AutocannonDrumBlock());
   public static final DeferredHolder<Block, Block> RAZOR_WIRE = REGISTRY.register("razor_wire", () -> new RazorWireBlock());
   public static final DeferredHolder<Block, Block> WIRE_FENCE = REGISTRY.register("wire_fence", () -> new WireFenceBlock());
   public static final DeferredHolder<Block, Block> CONCRETE_WALL = REGISTRY.register("concrete_wall", () -> new ConcreteWallBlock());
   public static final DeferredHolder<Block, Block> SHEET_METAL = REGISTRY.register("sheet_metal", () -> new SheetMetalBlock());
   public static final DeferredHolder<Block, Block> SHEET_METAL_PANE = REGISTRY.register("sheet_metal_pane", () -> new SheetMetalPaneBlock());
   public static final DeferredHolder<Block, Block> AUTOLOADER = REGISTRY.register("autoloader", () -> new AutoloaderBlock());
   public static final DeferredHolder<Block, Block> FISSION_BOMB = REGISTRY.register("fission_bomb", () -> new FissionBombBlock());
   public static final DeferredHolder<Block, Block> TRINITITE = REGISTRY.register("trinitite", () -> new TrinititeBlock());
   public static final DeferredHolder<Block, Block> DEEPSLATE_LEAD_ORE = REGISTRY.register("deepslate_lead_ore", () -> new DeepslateLeadOreBlock());
   public static final DeferredHolder<Block, Block> RAW_LEAD_BLOCK = REGISTRY.register("raw_lead_block", () -> new RawLeadBlockBlock());
   public static final DeferredHolder<Block, Block> RAW_NICKEL_BLOCK = REGISTRY.register("raw_nickel_block", () -> new RawNickelBlockBlock());
   public static final DeferredHolder<Block, Block> RAW_ZINC_BLOCK = REGISTRY.register("raw_zinc_block", () -> new RawZincBlockBlock());
   public static final DeferredHolder<Block, Block> BAUXITE = REGISTRY.register("bauxite", () -> new BauxiteBlock());
   public static final DeferredHolder<Block, Block> MINERAL_GRINDER = REGISTRY.register("mineral_grinder", () -> new MineralGrinderBlock());
   public static final DeferredHolder<Block, Block> REBAR = REGISTRY.register("rebar", () -> new RebarBlock());
   public static final DeferredHolder<Block, Block> SMALL_BOMB = REGISTRY.register("small_bomb", () -> new SmallBombBlock());
   public static final DeferredHolder<Block, Block> CLUSTER_OF_BOMBS = REGISTRY.register("cluster_of_bombs", () -> new ClusterOfBombsBlock());
   public static final DeferredHolder<Block, Block> ARTILLERY_AUTOLOADER = REGISTRY.register("artillery_autoloader", () -> new ArtilleryAutoloaderBlock());
   public static final DeferredHolder<Block, Block> ARTILLERY_CHARGE_LOADER = REGISTRY.register("artillery_charge_loader", () -> new ArtilleryChargeLoaderBlock());
   public static final DeferredHolder<Block, Block> TORPEDO_THRUSTER = REGISTRY.register("torpedo_thruster", () -> new TorpedoThrusterBlock());
   public static final DeferredHolder<Block, Block> CRUDE_OIL = REGISTRY.register("crude_oil", () -> new CrudeOilBlock());
   public static final DeferredHolder<Block, Block> OIL = REGISTRY.register("oil", () -> new OilBlock());
   public static final DeferredHolder<Block, Block> DIESEL = REGISTRY.register("diesel", () -> new DieselBlock());
   public static final DeferredHolder<Block, Block> KEROSENE = REGISTRY.register("kerosene", () -> new KeroseneBlock());
   public static final DeferredHolder<Block, Block> PETROLIUM = REGISTRY.register("petrolium", () -> new PetroliumBlock());
   public static final DeferredHolder<Block, Block> RADIOACTIVE_ASH = REGISTRY.register("radioactive_ash", () -> new RadioactiveAshBlock());
   public static final DeferredHolder<Block, Block> RADIOACTIVE_ASH_FULL_BLOCK = REGISTRY.register(
      "radioactive_ash_full_block", () -> new RadioactiveAshFullBlockBlock()
   );
   public static final DeferredHolder<Block, Block> ASPHALT = REGISTRY.register("asphalt", () -> new AsphaltBlock());
   public static final DeferredHolder<Block, Block> ROCKET_POD = REGISTRY.register("rocket_pod", () -> new RocketPodBlock());
   public static final DeferredHolder<Block, Block> ROCKET_POD_CHAMBER = REGISTRY.register("rocket_pod_chamber", () -> new RocketPodChamberBlock());
   public static final DeferredHolder<Block, Block> JET_EXHAUST = REGISTRY.register("jet_exhaust", () -> new JetExhaustBlock());
   public static final DeferredHolder<Block, Block> JET_TURBINE = REGISTRY.register("jet_turbine", () -> new JetTurbineBlock());
   public static final DeferredHolder<Block, Block> JET_COMPRESSOR = REGISTRY.register("jet_compressor", () -> new JetCompressorBlock());
   public static final DeferredHolder<Block, Block> SHEET_METAL_SLAB = REGISTRY.register("sheet_metal_slab", () -> new SheetMetalSlabBlock());
   public static final DeferredHolder<Block, Block> SHEET_METAL_STAIRS = REGISTRY.register("sheet_metal_stairs", () -> new SheetMetalStairsBlock());
   public static final DeferredHolder<Block, Block> MORTAR = REGISTRY.register("mortar", () -> new MortarBlock());
   public static final DeferredHolder<Block, Block> AIMER_NODE = REGISTRY.register("aimer_node", () -> new AimerNodeBlock());
   public static final DeferredHolder<Block, Block> SAND_BAGS = REGISTRY.register("sand_bags", () -> new SandBagsBlock());
   public static final DeferredHolder<Block, Block> SIREN = REGISTRY.register("siren", () -> new SirenBlock());
   public static final DeferredHolder<Block, Block> GREEN_ARMOR = REGISTRY.register("green_armor", () -> new GreenArmorBlock());
   public static final DeferredHolder<Block, Block> GREEN_ARMOR_SLAB = REGISTRY.register("green_armor_slab", () -> new GreenArmorSlabBlock());
   public static final DeferredHolder<Block, Block> GREEN_ARMOR_STAIRS = REGISTRY.register("green_armor_stairs", () -> new GreenArmorStairsBlock());
   public static final DeferredHolder<Block, Block> GREEN_ARMOR_TRAPDOOR = REGISTRY.register("green_armor_trapdoor", () -> new GreenArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> GREEN_ARMOR_OPTIC = REGISTRY.register("green_armor_optic", () -> new GreenArmorOpticBlock());
   public static final DeferredHolder<Block, Block> YELLOW_ARMOR = REGISTRY.register("yellow_armor", () -> new YellowArmorBlock());
   public static final DeferredHolder<Block, Block> YELLOW_ARMOR_SLAB = REGISTRY.register("yellow_armor_slab", () -> new YellowArmorSlabBlock());
   public static final DeferredHolder<Block, Block> YELLOW_ARMOR_STAIRS = REGISTRY.register("yellow_armor_stairs", () -> new YellowArmorStairsBlock());
   public static final DeferredHolder<Block, Block> YELLOW_ARMOR_TRAPDOOR = REGISTRY.register("yellow_armor_trapdoor", () -> new YellowArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> YELLOW_ARMOR_OPTIC = REGISTRY.register("yellow_armor_optic", () -> new YellowArmorOpticBlock());
   public static final DeferredHolder<Block, Block> BROWN_ARMOR = REGISTRY.register("brown_armor", () -> new BrownArmorBlock());
   public static final DeferredHolder<Block, Block> BROWN_ARMOR_SLAB = REGISTRY.register("brown_armor_slab", () -> new BrownArmorSlabBlock());
   public static final DeferredHolder<Block, Block> BROWN_ARMOR_STAIRS = REGISTRY.register("brown_armor_stairs", () -> new BrownArmorStairsBlock());
   public static final DeferredHolder<Block, Block> BROWN_ARMOR_TRAPDOOR = REGISTRY.register("brown_armor_trapdoor", () -> new BrownArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> BROWN_ARMOR_OPTIC = REGISTRY.register("brown_armor_optic", () -> new BrownArmorOpticBlock());
   public static final DeferredHolder<Block, Block> WHITE_ARMOR = REGISTRY.register("white_armor", () -> new WhiteArmorBlock());
   public static final DeferredHolder<Block, Block> WHITE_ARMOR_SLAB = REGISTRY.register("white_armor_slab", () -> new WhiteArmorSlabBlock());
   public static final DeferredHolder<Block, Block> WHITE_ARMOR_STAIRS = REGISTRY.register("white_armor_stairs", () -> new WhiteArmorStairsBlock());
   public static final DeferredHolder<Block, Block> WHITE_ARMOR_TRAPDOOR = REGISTRY.register("white_armor_trapdoor", () -> new WhiteArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> WHITE_ARMOR_OPTIC = REGISTRY.register("white_armor_optic", () -> new WhiteArmorOpticBlock());
   public static final DeferredHolder<Block, Block> ROTARY_AUTO_CANNON = REGISTRY.register("rotary_auto_cannon", () -> new RotaryAutoCannonBlock());
   public static final DeferredHolder<Block, Block> RAC_BARREL = REGISTRY.register("rac_barrel", () -> new RACBarrelBlock());
   public static final DeferredHolder<Block, Block> LIGHT_AUTOCANNON = REGISTRY.register("light_autocannon", () -> new LightAutocannonBlock());
   public static final DeferredHolder<Block, Block> SMOKE_LAUNCHER = REGISTRY.register("smoke_launcher", () -> new SmokeLauncherBlock());
   public static final DeferredHolder<Block, Block> URANIUM_ORE = REGISTRY.register("uranium_ore", () -> new UraniumOreBlock());
   public static final DeferredHolder<Block, Block> RED_ARMOR = REGISTRY.register("red_armor", () -> new RedArmorBlock());
   public static final DeferredHolder<Block, Block> RED_ARMOR_SLAB = REGISTRY.register("red_armor_slab", () -> new RedArmorSlabBlock());
   public static final DeferredHolder<Block, Block> RED_ARMOR_STAIRS = REGISTRY.register("red_armor_stairs", () -> new RedArmorStairsBlock());
   public static final DeferredHolder<Block, Block> RED_ARMOR_TRAPDOOR = REGISTRY.register("red_armor_trapdoor", () -> new RedArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> RED_ARMOR_OPTIC = REGISTRY.register("red_armor_optic", () -> new RedArmorOpticBlock());
   public static final DeferredHolder<Block, Block> BLACK_ARMOR = REGISTRY.register("black_armor", () -> new BlackArmorBlock());
   public static final DeferredHolder<Block, Block> BLACK_ARMOR_SLAB = REGISTRY.register("black_armor_slab", () -> new BlackArmorSlabBlock());
   public static final DeferredHolder<Block, Block> BLACK_ARMOR_STAIRS = REGISTRY.register("black_armor_stairs", () -> new BlackArmorStairsBlock());
   public static final DeferredHolder<Block, Block> BLACK_ARMOR_TRAPDOOR = REGISTRY.register("black_armor_trapdoor", () -> new BlackArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> BLACK_ARMOR_OPTIC = REGISTRY.register("black_armor_optic", () -> new BlackArmorOpticBlock());
   public static final DeferredHolder<Block, Block> CYAN_ARMOR = REGISTRY.register("cyan_armor", () -> new CyanArmorBlock());
   public static final DeferredHolder<Block, Block> CYAN_ARMOR_SLAB = REGISTRY.register("cyan_armor_slab", () -> new CyanArmorSlabBlock());
   public static final DeferredHolder<Block, Block> CYAN_ARMOR_STAIRS = REGISTRY.register("cyan_armor_stairs", () -> new CyanArmorStairsBlock());
   public static final DeferredHolder<Block, Block> CYAN_ARMOR_TRAPDOOR = REGISTRY.register("cyan_armor_trapdoor", () -> new CyanArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> BLUE_ARMOR = REGISTRY.register("blue_armor", () -> new BlueArmorBlock());
   public static final DeferredHolder<Block, Block> BLUE_ARMOR_SLAB = REGISTRY.register("blue_armor_slab", () -> new BlueArmorSlabBlock());
   public static final DeferredHolder<Block, Block> BLUE_ARMOR_STAIRS = REGISTRY.register("blue_armor_stairs", () -> new BlueArmorStairsBlock());
   public static final DeferredHolder<Block, Block> BLUE_ARMOR_TRAPDOOR = REGISTRY.register("blue_armor_trapdoor", () -> new BlueArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> ORANGE_ARMOR = REGISTRY.register("orange_armor", () -> new OrangeArmorBlock());
   public static final DeferredHolder<Block, Block> ORANGE_ARMOR_SLAB = REGISTRY.register("orange_armor_slab", () -> new OrangeArmorSlabBlock());
   public static final DeferredHolder<Block, Block> ORANGE_ARMOR_STAIRS = REGISTRY.register("orange_armor_stairs", () -> new OrangeArmorStairsBlock());
   public static final DeferredHolder<Block, Block> ORANGE_ARMOR_TRAPDOOR = REGISTRY.register("orange_armor_trapdoor", () -> new OrangeArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> ORANGE_ARMOR_OPTIC = REGISTRY.register("orange_armor_optic", () -> new OrangeArmorOpticBlock());
   public static final DeferredHolder<Block, Block> GRAY_ARMOR = REGISTRY.register("gray_armor", () -> new GrayArmorBlock());
   public static final DeferredHolder<Block, Block> GRAY_ARMOR_SLAB = REGISTRY.register("gray_armor_slab", () -> new GrayArmorSlabBlock());
   public static final DeferredHolder<Block, Block> GRAY_ARMOR_STAIRS = REGISTRY.register("gray_armor_stairs", () -> new GrayArmorStairsBlock());
   public static final DeferredHolder<Block, Block> GRAY_ARMOR_TRAPDOOR = REGISTRY.register("gray_armor_trapdoor", () -> new GrayArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> GRAY_ARMOR_OPTIC = REGISTRY.register("gray_armor_optic", () -> new GrayArmorOpticBlock());
   public static final DeferredHolder<Block, Block> LIME_ARMOR = REGISTRY.register("lime_armor", () -> new LimeArmorBlock());
   public static final DeferredHolder<Block, Block> LIME_ARMOR_SLAB = REGISTRY.register("lime_armor_slab", () -> new LimeArmorSlabBlock());
   public static final DeferredHolder<Block, Block> LIME_ARMOR_STAIRS = REGISTRY.register("lime_armor_stairs", () -> new LimeArmorStairsBlock());
   public static final DeferredHolder<Block, Block> LIME_ARMOR_TRAPDOOR = REGISTRY.register("lime_armor_trapdoor", () -> new LimeArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> LIME_ARMOR_OPTIC = REGISTRY.register("lime_armor_optic", () -> new LimeArmorOpticBlock());
   public static final DeferredHolder<Block, Block> LIGHT_GRAY_ARMOR = REGISTRY.register("light_gray_armor", () -> new LightGrayArmorBlock());
   public static final DeferredHolder<Block, Block> LIGHT_GRAY_ARMOR_SLAB = REGISTRY.register("light_gray_armor_slab", () -> new LightGrayArmorSlabBlock());
   public static final DeferredHolder<Block, Block> LIGHT_GRAY_ARMOR_STAIRS = REGISTRY.register("light_gray_armor_stairs", () -> new LightGrayArmorStairsBlock());
   public static final DeferredHolder<Block, Block> LIGHT_GRAY_ARMOR_TRAPDOOR = REGISTRY.register("light_gray_armor_trapdoor", () -> new LightGrayArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> LIGHT_GRAY_ARMOR_OPTIC = REGISTRY.register("light_gray_armor_optic", () -> new LightGrayArmorOpticBlock());
   public static final DeferredHolder<Block, Block> LIGHT_BLUE_ARMOR = REGISTRY.register("light_blue_armor", () -> new LightBlueArmorBlock());
   public static final DeferredHolder<Block, Block> LIGHT_BLUE_ARMOR_SLAB = REGISTRY.register("light_blue_armor_slab", () -> new LightBlueArmorSlabBlock());
   public static final DeferredHolder<Block, Block> LIGHT_BLUE_ARMOR_STAIRS = REGISTRY.register("light_blue_armor_stairs", () -> new LightBlueArmorStairsBlock());
   public static final DeferredHolder<Block, Block> LIGHT_BLUE_ARMOR_TRAPDOOR = REGISTRY.register("light_blue_armor_trapdoor", () -> new LightBlueArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> LIGHT_BLUE_ARMOR_OPTIC = REGISTRY.register("light_blue_armor_optic", () -> new LightBlueArmorOpticBlock());
   public static final DeferredHolder<Block, Block> REFINERY = REGISTRY.register("refinery", () -> new RefineryBlock());
   public static final DeferredHolder<Block, Block> REFINERY_TOWER = REGISTRY.register("refinery_tower", () -> new RefineryTowerBlock());
   public static final DeferredHolder<Block, Block> SULFUR_ORE = REGISTRY.register("sulfur_ore", () -> new SulfurOreBlock());
   public static final DeferredHolder<Block, Block> GIANT_COIL = REGISTRY.register("giant_coil", () -> new GiantCoilBlock());
   public static final DeferredHolder<Block, Block> REACTION_CHAMBER = REGISTRY.register("reaction_chamber", () -> new ReactionChamberBlock());
   public static final DeferredHolder<Block, Block> BREEDER_REACTOR_PORT = REGISTRY.register("breeder_reactor_port", () -> new BreederReactorPortBlock());
   public static final DeferredHolder<Block, Block> OIL_FIREBOX = REGISTRY.register("oil_firebox", () -> new OilFireboxBlock());
   public static final DeferredHolder<Block, Block> SUMMONATOR = REGISTRY.register("summonator", () -> new SummonatorBlock());
   public static final DeferredHolder<Block, Block> SUMMONATOR_MODULE = REGISTRY.register("summonator_module", () -> new SummonatorModuleBlock());
   public static final DeferredHolder<Block, Block> STEEL_DOOR = REGISTRY.register("steel_door", () -> new SteelDoorBlock());
   public static final DeferredHolder<Block, Block> OVERGROWN_REENFORCED_CONCRETE = REGISTRY.register(
      "overgrown_reenforced_concrete", () -> new OvergrownReenforcedConcreteBlock()
   );
   public static final DeferredHolder<Block, Block> RUSTY_BLOCK = REGISTRY.register("rusty_block", () -> new RustyBlockBlock());
   public static final DeferredHolder<Block, Block> CRACKED_CONCRETE_WALL = REGISTRY.register("cracked_concrete_wall", () -> new CrackedConcreteWallBlock());
   public static final DeferredHolder<Block, Block> FRACTURED_CONCRETE_WALL = REGISTRY.register("fractured_concrete_wall", () -> new FracturedConcreteWallBlock());
   public static final DeferredHolder<Block, Block> DAMAGED_CONCRETE_WALL = REGISTRY.register("damaged_concrete_wall", () -> new DamagedConcreteWallBlock());
   public static final DeferredHolder<Block, Block> DESTROYED_CONCRETE_WALL = REGISTRY.register("destroyed_concrete_wall", () -> new DestroyedConcreteWallBlock());
   public static final DeferredHolder<Block, Block> RUSTY_SLAB = REGISTRY.register("rusty_slab", () -> new RustySlabBlock());
   public static final DeferredHolder<Block, Block> RUSTY_STAIRS = REGISTRY.register("rusty_stairs", () -> new RustyStairsBlock());
   public static final DeferredHolder<Block, Block> RUSTY_TRAPDOOR = REGISTRY.register("rusty_trapdoor", () -> new RustyTrapdoorBlock());
   public static final DeferredHolder<Block, Block> ROBOT_CHUTE = REGISTRY.register("robot_chute", () -> new RobotChuteBlock());
   public static final DeferredHolder<Block, Block> STRUCTURAL_CONCRETE = REGISTRY.register("structural_concrete", () -> new StructuralConcreteBlock());
   public static final DeferredHolder<Block, Block> STEEL_TRUSS = REGISTRY.register("steel_truss", () -> new SteelTrussBlock());
   public static final DeferredHolder<Block, Block> SUMMONATOR_ACTIVE = REGISTRY.register("summonator_active", () -> new SummonatorActiveBlock());
   public static final DeferredHolder<Block, Block> ACTIVE_ROBOT_CHUTE = REGISTRY.register("active_robot_chute", () -> new ActiveRobotChuteBlock());
   public static final DeferredHolder<Block, Block> BERYLLIUM_ORE = REGISTRY.register("beryllium_ore", () -> new BerylliumOreBlock());
   public static final DeferredHolder<Block, Block> RAW_BERYLLIUM_BLOCK = REGISTRY.register("raw_beryllium_block", () -> new RawBerylliumBlockBlock());
   public static final DeferredHolder<Block, Block> BREEDER_REACTOR_CORE = REGISTRY.register("breeder_reactor_core", () -> new BreederReactorCoreBlock());
   public static final DeferredHolder<Block, Block> BREEDER_REACTOR_INTERFACE = REGISTRY.register(
      "breeder_reactor_interface", () -> new BreederReactorInterfaceBlock()
   );
   public static final DeferredHolder<Block, Block> REACTOR_CASING = REGISTRY.register("reactor_casing", () -> new ReactorCasingBlock());
   public static final DeferredHolder<Block, Block> CONTROL_ROD = REGISTRY.register("control_rod", () -> new ControlRodBlock());
   public static final DeferredHolder<Block, Block> EMPTY_FUEL_RODS = REGISTRY.register("empty_fuel_rods", () -> new EmptyFuelRodsBlock());
   public static final DeferredHolder<Block, Block> FUEL_RODS_1 = REGISTRY.register("fuel_rods_1", () -> new FuelRods1Block());
   public static final DeferredHolder<Block, Block> FUEL_RODS_2 = REGISTRY.register("fuel_rods_2", () -> new FuelRods2Block());
   public static final DeferredHolder<Block, Block> FUEL_RODS_3 = REGISTRY.register("fuel_rods_3", () -> new FuelRods3Block());
   public static final DeferredHolder<Block, Block> FUEL_RODS_4 = REGISTRY.register("fuel_rods_4", () -> new FuelRods4Block());
   public static final DeferredHolder<Block, Block> SCORCH_DIRT = REGISTRY.register("scorch_dirt", () -> new ScorchDirtBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING = REGISTRY.register("aluminum_plating", () -> new AluminumPlatingBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB = REGISTRY.register("aluminum_plating_slab", () -> new AluminumPlatingSlabBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS = REGISTRY.register("aluminum_plating_stairs", () -> new AluminumPlatingStairsBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR = REGISTRY.register(
      "aluminum_plating_trapdoor", () -> new AluminumPlatingTrapdoorBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL = REGISTRY.register("aluminum_side_panel", () -> new AluminumSidePanelBlock());
   public static final DeferredHolder<Block, Block> GLASS_TRAPDOOR = REGISTRY.register("glass_trapdoor", () -> new GlassTrapdoorBlock());
   public static final DeferredHolder<Block, Block> REINFORCED_GLASS = REGISTRY.register("reinforced_glass", () -> new ReinforcedGlassBlock());
   public static final DeferredHolder<Block, Block> REINFORCED_GLASS_TRAPDOOR = REGISTRY.register(
      "reinforced_glass_trapdoor", () -> new ReinforcedGlassTrapdoorBlock()
   );
   public static final DeferredHolder<Block, Block> AFTER_BURNER = REGISTRY.register("after_burner", () -> new AfterBurnerBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL = REGISTRY.register("aluminum_ac_barrel", () -> new AluminumACBarrelBlock());
   public static final DeferredHolder<Block, Block> GAS_BOMB = REGISTRY.register("gas_bomb", () -> new GasBombBlock());
   public static final DeferredHolder<Block, Block> GAS_DISPENSER = REGISTRY.register("gas_dispenser", () -> new GasDispenserBlock());
   public static final DeferredHolder<Block, Block> NODE_TRIGGER = REGISTRY.register("node_trigger", () -> new NodeTriggerBlock());
   public static final DeferredHolder<Block, Block> NODE_TRIGGER_ON = REGISTRY.register("node_trigger_on", () -> new NodeTriggerOnBlock());
   public static final DeferredHolder<Block, Block> COUNTERMEASURE_DISPENSER = REGISTRY.register("countermeasure_dispenser", () -> new CountermeasureDispenserBlock());
   public static final DeferredHolder<Block, Block> ASPHALT_SLAB = REGISTRY.register("asphalt_slab", () -> new AsphaltSlabBlock());
   public static final DeferredHolder<Block, Block> ORDINANCE_CORE = REGISTRY.register("ordinance_core", () -> new OrdinanceCoreBlock());
   public static final DeferredHolder<Block, Block> ORDINANCE_FINS = REGISTRY.register("ordinance_fins", () -> new OrdinanceFinsBlock());
   public static final DeferredHolder<Block, Block> ORDINANCE_THRUSTER = REGISTRY.register("ordinance_thruster", () -> new OrdinanceThrusterBlock());
   public static final DeferredHolder<Block, Block> ORDINANCE_HEAVY_WARHEAD = REGISTRY.register("ordinance_heavy_warhead", () -> new OrdinanceHeavyWarheadBlock());
   public static final DeferredHolder<Block, Block> ORDINANCE_INCENDIARY_WARHEAD = REGISTRY.register(
      "ordinance_incendiary_warhead", () -> new OrdinanceIncendiaryWarheadBlock()
   );
   public static final DeferredHolder<Block, Block> ORDINANCE_INLINE_WARHEAD = REGISTRY.register("ordinance_inline_warhead", () -> new OrdinanceInlineWarheadBlock());
   public static final DeferredHolder<Block, Block> ORDINANCE_KINETIC_HEAD = REGISTRY.register("ordinance_kinetic_head", () -> new OrdinanceKineticHeadBlock());
   public static final DeferredHolder<Block, Block> ORDINANCE_IR_SEEKER_HEAD = REGISTRY.register("ordinance_ir_seeker_head", () -> new OrdinanceIRSeekerHeadBlock());
   public static final DeferredHolder<Block, Block> ORDINANCE_CLUSTER_WARHEAD = REGISTRY.register(
      "ordinance_cluster_warhead", () -> new OrdinanceClusterWarheadBlock()
   );
   public static final DeferredHolder<Block, Block> ORDINANCE_INLINE_FISSION_WARHEAD = REGISTRY.register(
      "ordinance_inline_fission_warhead", () -> new OrdinanceInlineFissionWarheadBlock()
   );
   public static final DeferredHolder<Block, Block> ORDINANCE_FISSION_INITIATOR_HEAD = REGISTRY.register(
      "ordinance_fission_initiator_head", () -> new OrdinanceFissionInitiatorHeadBlock()
   );
   public static final DeferredHolder<Block, Block> ANCIENT_LIGHT = REGISTRY.register("ancient_light", () -> new AncientLightBlock());
   public static final DeferredHolder<Block, Block> SUMMONATION = REGISTRY.register("summonation", () -> new SummonationBlock());
   public static final DeferredHolder<Block, Block> OPEN_SUMMONATION = REGISTRY.register("open_summonation", () -> new OpenSummonationBlock());
   public static final DeferredHolder<Block, Block> LOOT_BOX = REGISTRY.register("loot_box", () -> new LootBoxBlock());
   public static final DeferredHolder<Block, Block> DEFENSE_CORE = REGISTRY.register("defense_core", () -> new DefenseCoreBlock());
   public static final DeferredHolder<Block, Block> RAW_URANIUM_BLOCK = REGISTRY.register("raw_uranium_block", () -> new RawUraniumBlockBlock());
   public static final DeferredHolder<Block, Block> FUSION_BOMB = REGISTRY.register("fusion_bomb", () -> new FusionBombBlock());
   public static final DeferredHolder<Block, Block> BERYLLIUM_BLOCK = REGISTRY.register("beryllium_block", () -> new BerylliumBlockBlock());
   public static final DeferredHolder<Block, Block> POLISHED_TRINITITE = REGISTRY.register("polished_trinitite", () -> new PolishedTrinititeBlock());
   public static final DeferredHolder<Block, Block> SULFUR_BLOCK = REGISTRY.register("sulfur_block", () -> new SulfurBlockBlock());
   public static final DeferredHolder<Block, Block> ANCIENT_WELL = REGISTRY.register("ancient_well", () -> new AncientWellBlock());
   public static final DeferredHolder<Block, Block> PASSENGER_SEAT = REGISTRY.register("passenger_seat", () -> new PassengerSeatBlock());
   public static final DeferredHolder<Block, Block> JET_GEARBOX = REGISTRY.register("jet_gearbox", () -> new JetGearboxBlock());
   public static final DeferredHolder<Block, Block> GENERATOR = REGISTRY.register("generator", () -> new GeneratorBlock());
   public static final DeferredHolder<Block, Block> FUEL_TANK = REGISTRY.register("fuel_tank", () -> new FuelTankBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_DARK_GRAY = REGISTRY.register(
      "aluminum_plating_dark_gray", () -> new AluminumPlatingDarkGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_DARK_GRAY = REGISTRY.register(
      "aluminum_plating_slab_dark_gray", () -> new AluminumPlatingSlabDarkGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_DARK_GRAY = REGISTRY.register(
      "aluminum_plating_stairs_dark_gray", () -> new AluminumPlatingStairsDarkGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_DARK_GRAY = REGISTRY.register(
      "aluminum_plating_trapdoor_dark_gray", () -> new AluminumPlatingTrapdoorDarkGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_DARK_GRAY = REGISTRY.register(
      "aluminum_side_panel_dark_gray", () -> new AluminumSidePanelDarkGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_DARK_GRAY = REGISTRY.register(
      "aluminum_ac_barrel_dark_gray", () -> new AluminumACBarrelDarkGrayBlock()
   );
   public static final DeferredHolder<Block, Block> EXTENSION_SHAFT = REGISTRY.register("extension_shaft", () -> new ExtensionShaftBlock());
   public static final DeferredHolder<Block, Block> DRIVE_SHAFT = REGISTRY.register("drive_shaft", () -> new DriveShaftBlock());
   public static final DeferredHolder<Block, Block> ENGINE_CYLLINDER = REGISTRY.register("engine_cyllinder", () -> new EngineCyllinderBlock());
   public static final DeferredHolder<Block, Block> LARGE_ENGINE_SMOKESTACK = REGISTRY.register("large_engine_smokestack", () -> new LargeEngineSmokestackBlock());
   public static final DeferredHolder<Block, Block> TINTED_GLASS_TRAPDOOR = REGISTRY.register("tinted_glass_trapdoor", () -> new TintedGlassTrapdoorBlock());
   public static final DeferredHolder<Block, Block> CYAN_ARMOR_OPTIC = REGISTRY.register("cyan_armor_optic", () -> new CyanArmorOpticBlock());
   public static final DeferredHolder<Block, Block> BLUE_ARMOR_OPTIC = REGISTRY.register("blue_armor_optic", () -> new BlueArmorOpticBlock());
   public static final DeferredHolder<Block, Block> ENERGY_NODE = REGISTRY.register("energy_node", () -> new EnergyNodeBlock());
   public static final DeferredHolder<Block, Block> ENERGY_BATTERY = REGISTRY.register("energy_battery", () -> new EnergyBatteryBlock());
   public static final DeferredHolder<Block, Block> LARGE_ELECTRIC_MOTOR = REGISTRY.register("large_electric_motor", () -> new LargeElectricMotorBlock());
   public static final DeferredHolder<Block, Block> PINK_ARMOR = REGISTRY.register("pink_armor", () -> new PinkArmorBlock());
   public static final DeferredHolder<Block, Block> PINK_ARMOR_SLAB = REGISTRY.register("pink_armor_slab", () -> new PinkArmorSlabBlock());
   public static final DeferredHolder<Block, Block> PINK_ARMOR_STAIRS = REGISTRY.register("pink_armor_stairs", () -> new PinkArmorStairsBlock());
   public static final DeferredHolder<Block, Block> PINK_ARMOR_TRAPDOOR = REGISTRY.register("pink_armor_trapdoor", () -> new PinkArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> PINK_ARMOR_OPTIC = REGISTRY.register("pink_armor_optic", () -> new PinkArmorOpticBlock());
   public static final DeferredHolder<Block, Block> MAGENTA_ARMOR = REGISTRY.register("magenta_armor", () -> new MagentaArmorBlock());
   public static final DeferredHolder<Block, Block> MAGENTA_ARMOR_SLAB = REGISTRY.register("magenta_armor_slab", () -> new MagentaArmorSlabBlock());
   public static final DeferredHolder<Block, Block> MAGENTA_ARMOR_STAIRS = REGISTRY.register("magenta_armor_stairs", () -> new MagentaArmorStairsBlock());
   public static final DeferredHolder<Block, Block> MAGENTA_ARMOR_TRAPDOOR = REGISTRY.register("magenta_armor_trapdoor", () -> new MagentaArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> MAGENTA_ARMOR_OPTIC = REGISTRY.register("magenta_armor_optic", () -> new MagentaArmorOpticBlock());
   public static final DeferredHolder<Block, Block> PURPLE_ARMOR = REGISTRY.register("purple_armor", () -> new PurpleArmorBlock());
   public static final DeferredHolder<Block, Block> PURPLE_ARMOR_SLAB = REGISTRY.register("purple_armor_slab", () -> new PurpleArmorSlabBlock());
   public static final DeferredHolder<Block, Block> PURPLE_ARMOR_STAIRS = REGISTRY.register("purple_armor_stairs", () -> new PurpleArmorStairsBlock());
   public static final DeferredHolder<Block, Block> PURPLE_ARMOR_TRAPDOOR = REGISTRY.register("purple_armor_trapdoor", () -> new PurpleArmorTrapdoorBlock());
   public static final DeferredHolder<Block, Block> PURPLE_ARMOR_OPTIC = REGISTRY.register("purple_armor_optic", () -> new PurpleArmorOpticBlock());
   public static final DeferredHolder<Block, Block> ELECTRIC_FIREBOX = REGISTRY.register("electric_firebox", () -> new ElectricFireboxBlock());
   public static final DeferredHolder<Block, Block> THERMAL_FURNACE = REGISTRY.register("thermal_furnace", () -> new ThermalFurnaceBlock());
   public static final DeferredHolder<Block, Block> POWER_REACTOR_INTERFACE = REGISTRY.register("power_reactor_interface", () -> new PowerReactorInterfaceBlock());
   public static final DeferredHolder<Block, Block> POWER_REACTOR_PORT = REGISTRY.register("power_reactor_port", () -> new PowerReactorPortBlock());
   public static final DeferredHolder<Block, Block> LAND_MINE = REGISTRY.register("land_mine", () -> new LandMineBlock());
   public static final DeferredHolder<Block, Block> AI_MINE = REGISTRY.register("ai_mine", () -> new AIMineBlock());
   public static final DeferredHolder<Block, Block> ORDINANCE_INLINE_FUSION_WARHEAD_STAGE_1 = REGISTRY.register(
      "ordinance_inline_fusion_warhead_stage_1", () -> new OrdinanceInlineFusionWarheadStage1Block()
   );
   public static final DeferredHolder<Block, Block> ORDINANCE_INLINE_FUSION_WARHEAD_STAGE_2 = REGISTRY.register(
      "ordinance_inline_fusion_warhead_stage_2", () -> new OrdinanceInlineFusionWarheadStage2Block()
   );
   public static final DeferredHolder<Block, Block> TRINITITE_GLASS = REGISTRY.register("trinitite_glass", () -> new TrinititeGlassBlock());
   public static final DeferredHolder<Block, Block> TRINITITE_GLASS_TRAPDOOR = REGISTRY.register("trinitite_glass_trapdoor", () -> new TrinititeGlassTrapdoorBlock());
   public static final DeferredHolder<Block, Block> ENERGY_DISTRIBUTION_NODE = REGISTRY.register("energy_distribution_node", () -> new EnergyDistributionNodeBlock());
   public static final DeferredHolder<Block, Block> MANUAL_CRANK = REGISTRY.register("manual_crank", () -> new ManualCrankBlock());
   public static final DeferredHolder<Block, Block> LARGE_ROCKET_POD_CHAMBER = REGISTRY.register("large_rocket_pod_chamber", () -> new LargeRocketPodChamberBlock());
   public static final DeferredHolder<Block, Block> LARGE_ROCKET_POD = REGISTRY.register("large_rocket_pod", () -> new LargeRocketPodBlock());
   public static final DeferredHolder<Block, Block> EMPTY_MISSILE_HARDPOINT = REGISTRY.register("empty_missile_hardpoint", () -> new EmptyMissileHardpointBlock());
   public static final DeferredHolder<Block, Block> FIRE_SPEAR_MISSILE_HARDPOINT = REGISTRY.register(
      "fire_spear_missile_hardpoint", () -> new FireSpearMissileHardpointBlock()
   );
   public static final DeferredHolder<Block, Block> SEEKER_SPEAR_MISSILE_HARDPOINT = REGISTRY.register(
      "seeker_spear_missile_hardpoint", () -> new SeekerSpearMissileHardpointBlock()
   );
   public static final DeferredHolder<Block, Block> STRIKE_SPEAR_MISSILE_HARDPOINT = REGISTRY.register(
      "strike_spear_missile_hardpoint", () -> new StrikeSpearMissileHardpointBlock()
   );
   public static final DeferredHolder<Block, Block> LITHIUM_ORE = REGISTRY.register("lithium_ore", () -> new LithiumOreBlock());
   public static final DeferredHolder<Block, Block> POLISHED_BAUXITE = REGISTRY.register("polished_bauxite", () -> new PolishedBauxiteBlock());
   public static final DeferredHolder<Block, Block> RAW_LITHIUM_BLOCK = REGISTRY.register("raw_lithium_block", () -> new RawLithiumBlockBlock());
   public static final DeferredHolder<Block, Block> LITHIUM_BLOCK = REGISTRY.register("lithium_block", () -> new LithiumBlockBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_GREEN = REGISTRY.register("aluminum_plating_green", () -> new AluminumPlatingGreenBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_GREEN = REGISTRY.register(
      "aluminum_plating_slab_green", () -> new AluminumPlatingSlabGreenBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_GREEN = REGISTRY.register(
      "aluminum_plating_stairs_green", () -> new AluminumPlatingStairsGreenBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_GREEN = REGISTRY.register(
      "aluminum_plating_trapdoor_green", () -> new AluminumPlatingTrapdoorGreenBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_GREEN = REGISTRY.register("aluminum_side_panel_green", () -> new AluminumSidePanelGreenBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_GREEN = REGISTRY.register("aluminum_ac_barrel_green", () -> new AluminumACBarrelGreenBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_YELLOW = REGISTRY.register("aluminum_plating_yellow", () -> new AluminumPlatingYellowBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_YELLOW = REGISTRY.register(
      "aluminum_plating_slab_yellow", () -> new AluminumPlatingSlabYellowBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_YELLOW = REGISTRY.register(
      "aluminum_plating_stairs_yellow", () -> new AluminumPlatingStairsYellowBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_YELLOW = REGISTRY.register(
      "aluminum_plating_trapdoor_yellow", () -> new AluminumPlatingTrapdoorYellowBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_YELLOW = REGISTRY.register(
      "aluminum_side_panel_yellow", () -> new AluminumSidePanelYellowBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_YELLOW = REGISTRY.register("aluminum_ac_barrel_yellow", () -> new AluminumACBarrelYellowBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_BROWN = REGISTRY.register("aluminum_plating_brown", () -> new AluminumPlatingBrownBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_BROWN = REGISTRY.register(
      "aluminum_plating_slab_brown", () -> new AluminumPlatingSlabBrownBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_BROWN = REGISTRY.register(
      "aluminum_plating_stairs_brown", () -> new AluminumPlatingStairsBrownBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_BROWN = REGISTRY.register(
      "aluminum_plating_trapdoor_brown", () -> new AluminumPlatingTrapdoorBrownBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_BROWN = REGISTRY.register(
      "aluminum_side_panel_brown", () -> new AluminumPlatingSidePanelBrownBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_BROWN = REGISTRY.register("aluminum_ac_barrel_brown", () -> new AluminumACBarrelBrownBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_RED = REGISTRY.register("aluminum_plating_red", () -> new AluminumPlatingRedBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_RED = REGISTRY.register("aluminum_plating_slab_red", () -> new AluminumPlatingSlabRedBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_RED = REGISTRY.register(
      "aluminum_plating_stairs_red", () -> new AluminumPlatingStairsRedBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_RED = REGISTRY.register(
      "aluminum_plating_trapdoor_red", () -> new AluminumPlatingTrapdoorRedBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_RED = REGISTRY.register(
      "aluminum_side_panel_red", () -> new AluminumPlatingSidePanelRedBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_RED = REGISTRY.register("aluminum_ac_barrel_red", () -> new AluminumACBarrelRedBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_WHITE = REGISTRY.register("aluminum_plating_white", () -> new AluminumPlatingWhiteBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_WHITE = REGISTRY.register(
      "aluminum_plating_slab_white", () -> new AluminumPlatingSlabWhiteBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_WHITE = REGISTRY.register(
      "aluminum_plating_stairs_white", () -> new AluminumPlatingStairsWhiteBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_WHITE = REGISTRY.register(
      "aluminum_plating_trapdoor_white", () -> new AluminumPlatingTrapdoorWhiteBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_WHITE = REGISTRY.register(
      "aluminum_side_panel_white", () -> new AlumiumPlatingSidePanelWhiteBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_WHITE = REGISTRY.register("aluminum_ac_barrel_white", () -> new AluminumACBarrelWhiteBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_BLUE = REGISTRY.register("aluminum_plating_blue", () -> new AluminumPlatingBlueBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_BLUE = REGISTRY.register(
      "aluminum_plating_slab_blue", () -> new AluminumPlatingSlabBlueBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_BLUE = REGISTRY.register(
      "aluminum_plating_stairs_blue", () -> new AluminumPlatingStairsBlueBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_BLUE = REGISTRY.register(
      "aluminum_plating_trapdoor_blue", () -> new AluminumPlatingTrapdoorBlueBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_BLUE = REGISTRY.register(
      "aluminum_side_panel_blue", () -> new AluminumPlatingSidePanelBlueBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_BLUE = REGISTRY.register("aluminum_ac_barrel_blue", () -> new AluminumACBarrelBlueBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_ORANGE = REGISTRY.register("aluminum_plating_orange", () -> new AluminumPlatingOrangeBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_ORANGE = REGISTRY.register(
      "aluminum_plating_slab_orange", () -> new AluminumPlatingSlabOrangeBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_ORANGE = REGISTRY.register(
      "aluminum_plating_stairs_orange", () -> new AluminumPlatingStairsOrangeBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_ORANGE = REGISTRY.register(
      "aluminum_plating_trapdoor_orange", () -> new AluminumPlatingTrapdoorOrangeBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_ORANGE = REGISTRY.register(
      "aluminum_side_panel_orange", () -> new AluminumPlatingSidePanelOrangeBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_ORANGE = REGISTRY.register("aluminum_ac_barrel_orange", () -> new AluminumACBarrelOrangeBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_CYAN = REGISTRY.register("aluminum_plating_cyan", () -> new AluminumPlatingCyanBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_CYAN = REGISTRY.register(
      "aluminum_plating_slab_cyan", () -> new AluminumPlatingSlabCyanBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_CYAN = REGISTRY.register(
      "aluminum_plating_stairs_cyan", () -> new AluminumPlatingStairsCyanBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_CYAN = REGISTRY.register(
      "aluminum_plating_trapdoor_cyan", () -> new AluminumPlatingTrapdoorCyanBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_CYAN = REGISTRY.register(
      "aluminum_side_panel_cyan", () -> new AluminumPlatingSidePanelCyanBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_CYAN = REGISTRY.register("aluminum_ac_barrel_cyan", () -> new AluminumACBarrelCyanBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_BLACK = REGISTRY.register("aluminum_plating_black", () -> new AluminumPlatingBlackBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_BLACK = REGISTRY.register(
      "aluminum_plating_slab_black", () -> new AluminumPlatingSlabBlackBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_BLACK = REGISTRY.register(
      "aluminum_plating_stairs_black", () -> new AluminumPlatingStairsBlackBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_BLACK = REGISTRY.register(
      "aluminum_plating_trapdoor_black", () -> new AluminumPlatingTrapdoorBlackBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_BLACK = REGISTRY.register(
      "aluminum_side_panel_black", () -> new AluminumSidePanelPlatingBlackBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_BLACK = REGISTRY.register("aluminum_ac_barrel_black", () -> new AluminumACBarrelBlackBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_GRAY = REGISTRY.register("aluminum_plating_gray", () -> new AluminumPlatingGrayBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_GRAY = REGISTRY.register(
      "aluminum_plating_slab_gray", () -> new AluminumPlatingSlabGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_GRAY = REGISTRY.register(
      "aluminum_plating_stairs_gray", () -> new AluminumPlatingStairsGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_GRAY = REGISTRY.register(
      "aluminum_plating_trapdoor_gray", () -> new AluminumPlatingTrapdoorGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_GRAY = REGISTRY.register(
      "aluminum_side_panel_gray", () -> new AluminumSidePanelPlatingGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_GRAY = REGISTRY.register("aluminum_ac_barrel_gray", () -> new AluminumACBarrelGrayBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_LIGHT_GRAY = REGISTRY.register(
      "aluminum_plating_light_gray", () -> new AluminumPlatingLightGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_LIGHT_GRAY = REGISTRY.register(
      "aluminum_plating_slab_light_gray", () -> new AluminumPlatingSlabLightGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_LIGHT_GRAY = REGISTRY.register(
      "aluminum_plating_stairs_light_gray", () -> new AluminumPlatingStairsLightGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_LIGHT_GRAY = REGISTRY.register(
      "aluminum_plating_trapdoor_light_gray", () -> new AluminumPlatingTrapdoorLightGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_LIGHT_GRAY = REGISTRY.register(
      "aluminum_side_panel_light_gray", () -> new AluminumSidePanelPlatingLightGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_LIGHT_GRAY = REGISTRY.register(
      "aluminum_ac_barrel_light_gray", () -> new AluminumACBarrelLightGrayBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_LIME = REGISTRY.register("aluminum_plating_lime", () -> new AluminumPlatingLimeBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_LIME = REGISTRY.register(
      "aluminum_plating_slab_lime", () -> new AluminumPlatingSlabLimeBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_LIME = REGISTRY.register(
      "aluminum_plating_stairs_lime", () -> new AluminumPlatingStairsLimeBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_LIME = REGISTRY.register(
      "aluminum_plating_trapdoor_lime", () -> new AluminumPlatingTrapdoorLimeBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_LIME = REGISTRY.register(
      "aluminum_side_panel_lime", () -> new AluminumSidePanelPlatingLimeBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_LIME = REGISTRY.register("aluminum_ac_barrel_lime", () -> new AluminumACBarrelLimeBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_LIGHT_BLUE = REGISTRY.register(
      "aluminum_plating_light_blue", () -> new AluminumPlatingLightBlueBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_LIGHT_BLUE = REGISTRY.register(
      "aluminum_plating_slab_light_blue", () -> new AluminumPlatingSlabLightBlueBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_LIGHT_BLUE = REGISTRY.register(
      "aluminum_plating_stairs_light_blue", () -> new AluminumPlatingStairsLightBlueBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_LIGHT_BLUE = REGISTRY.register(
      "aluminum_plating_trapdoor_light_blue", () -> new AluminumPlatingTrapdoorLightBlueBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_LIGHT_BLUE = REGISTRY.register(
      "aluminum_side_panel_light_blue", () -> new AluminumSidePanelPlatingLightBlueBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_LIGHT_BLUE = REGISTRY.register(
      "aluminum_ac_barrel_light_blue", () -> new AluminumACBarrelLightBlueBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_PINK = REGISTRY.register("aluminum_plating_pink", () -> new AluminumPlatingPinkBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_PINK = REGISTRY.register(
      "aluminum_plating_slab_pink", () -> new AluminumPlatingSlabPinkBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_PINK = REGISTRY.register(
      "aluminum_plating_stairs_pink", () -> new AluminumPlatingStairsPinkBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_PINK = REGISTRY.register(
      "aluminum_plating_trapdoor_pink", () -> new AluminumPlatingTrapdoorPinkBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_PINK = REGISTRY.register(
      "aluminum_side_panel_pink", () -> new AluminumSidePanelPlatingPinkBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_PINK = REGISTRY.register("aluminum_ac_barrel_pink", () -> new AluminumACBarrelPinkBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_MAGENTA = REGISTRY.register("aluminum_plating_magenta", () -> new AluminumPlatingMagentaBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_MAGENTA = REGISTRY.register(
      "aluminum_plating_slab_magenta", () -> new AluminumPlatingSlabMagentaBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_MAGENTA = REGISTRY.register(
      "aluminum_plating_stairs_magenta", () -> new AluminumPlatingStairsMagentaBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_MAGENTA = REGISTRY.register(
      "aluminum_plating_trapdoor_magenta", () -> new AluminumPlatingTrapdoorMagentaBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_MAGENTA = REGISTRY.register(
      "aluminum_side_panel_magenta", () -> new AluminumSidePanelPlatingMagentaBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_MAGENTA = REGISTRY.register(
      "aluminum_ac_barrel_magenta", () -> new AluminumACBarrelMagentaBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_PURPLE = REGISTRY.register("aluminum_plating_purple", () -> new AluminumPlatingPurpleBlock());
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_SLAB_PURPLE = REGISTRY.register(
      "aluminum_plating_slab_purple", () -> new AluminumPlatingSlabPurpleBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_STAIRS_PURPLE = REGISTRY.register(
      "aluminum_plating_stairs_purple", () -> new AluminumPlatingStairsPurpleBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_PLATING_TRAPDOOR_PURPLE = REGISTRY.register(
      "aluminum_plating_trapdoor_purple", () -> new AluminumPlatingTrapdoorPurpleBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_SIDE_PANEL_PURPLE = REGISTRY.register(
      "aluminum_side_panel_purple", () -> new AluminumSidePanelPlatingPurpleBlock()
   );
   public static final DeferredHolder<Block, Block> ALUMINUM_AC_BARREL_PURPLE = REGISTRY.register("aluminum_ac_barrel_purple", () -> new AluminumACBarrelPurpleBlock());
   public static final DeferredHolder<Block, Block> MANUAL_AIMER = REGISTRY.register("manual_aimer", () -> new ManualAimerBlock());
   public static final DeferredHolder<Block, Block> SOLAR_GENERATOR = REGISTRY.register("solar_generator", () -> new SolarGeneratorBlock());
   public static final DeferredHolder<Block, Block> RADAR_SPEAR_MISSILE_HARDPOINT = REGISTRY.register(
      "radar_spear_missile_hardpoint", () -> new RadarSpearMissileHardpointBlock()
   );
   public static final DeferredHolder<Block, Block> ORDINANCE_CONTROLLER = REGISTRY.register("ordinance_controller", () -> new OrdinanceControllerBlock());
   public static final DeferredHolder<Block, Block> ORDINANCE_SARH_SEEKER = REGISTRY.register("ordinance_sarh_seeker", () -> new OrdinanceSARHSeekerBlock());
   public static final DeferredHolder<Block, Block> FUEL_TANK_MODULE = REGISTRY.register("fuel_tank_module", () -> new FuelTankModuleBlock());
   public static final DeferredHolder<Block, Block> DAMAGEDFUELTANK = REGISTRY.register("damagedfueltank", () -> new DamagedfueltankBlock());
   public static final DeferredHolder<Block, Block> FUEL_TANK_INPUT = REGISTRY.register("fuel_tank_input", () -> new FuelTankInputBlock());
   public static final DeferredHolder<Block, Block> HEAVY_MACHINE_GUN = REGISTRY.register("heavy_machine_gun", () -> new HeavyMachineGunBlock());
   public static final DeferredHolder<Block, Block> LIGHT_MACHINE_GUN = REGISTRY.register("light_machine_gun", () -> new LightMachineGunBlock());
   public static final DeferredHolder<Block, Block> COVERED_MACHINE_GUN_BARREL = REGISTRY.register(
      "covered_machine_gun_barrel", () -> new CoveredMachineGunBarrelBlock()
   );
   public static final DeferredHolder<Block, Block> REINFORCED_GLASS_STAIRS = REGISTRY.register("reinforced_glass_stairs", () -> new ReinforcedGlassStairsBlock());
   public static final DeferredHolder<Block, Block> TINTED_GLASS_STAIRS = REGISTRY.register("tinted_glass_stairs", () -> new TintedGlassStairsBlock());
   public static final DeferredHolder<Block, Block> TRINITITE_GLASS_STAIRS = REGISTRY.register("trinitite_glass_stairs", () -> new TrinititeGlassStairsBlock());
   public static final DeferredHolder<Block, Block> THICK_BATTLE_CANNON_BARREL = REGISTRY.register(
      "thick_battle_cannon_barrel", () -> new ThickBattleCannonBarrelBlock()
   );
   public static final DeferredHolder<Block, Block> MEDIUM_PETROL_ENGINE = REGISTRY.register("medium_petrol_engine", () -> new MediumPetrolEngineBlock());
   public static final DeferredHolder<Block, Block> SMALL_DIESEL_ENGINE = REGISTRY.register("small_diesel_engine", () -> new SmallDieselEngineBlock());
   public static final DeferredHolder<Block, Block> FLAME_THROWER = REGISTRY.register("flame_thrower", () -> new FlameThrowerBlock());
   public static final DeferredHolder<Block, Block> FLAME_THROWER_BARREL = REGISTRY.register("flame_thrower_barrel", () -> new FlameThrowerBarrelBlock());
   public static final DeferredHolder<Block, Block> COVERED_FLAME_THROWER_BARREL = REGISTRY.register(
      "covered_flame_thrower_barrel", () -> new CoveredFlameThrowerBarrelBlock()
   );
   public static final DeferredHolder<Block, Block> ERA_4 = REGISTRY.register("era_4", () -> new ERA4Block());
   public static final DeferredHolder<Block, Block> ERA_3 = REGISTRY.register("era_3", () -> new ERA3Block());
   public static final DeferredHolder<Block, Block> ERA_2 = REGISTRY.register("era_2", () -> new ERA2Block());
   public static final DeferredHolder<Block, Block> ERA_1 = REGISTRY.register("era_1", () -> new ERA1Block());
   public static final DeferredHolder<Block, Block> OFFSET_ERA_4 = REGISTRY.register("offset_era_4", () -> new OffsetERA4Block());
   public static final DeferredHolder<Block, Block> OFFSET_ERA_3 = REGISTRY.register("offset_era_3", () -> new OffsetERA3Block());
   public static final DeferredHolder<Block, Block> OFFSET_ERA_2 = REGISTRY.register("offset_era_2", () -> new OffsetERA2Block());
   public static final DeferredHolder<Block, Block> OFFSET_ERA_1 = REGISTRY.register("offset_era_1", () -> new OffsetERA1Block());
   public static final DeferredHolder<Block, Block> MEDIUM_DIESEL_ENGINE = REGISTRY.register("medium_diesel_engine", () -> new MediumDieselEngineBlock());
   public static final DeferredHolder<Block, Block> SMALL_PETROL_ENGINE = REGISTRY.register("small_petrol_engine", () -> new SmallPetrolEngineBlock());
   public static final DeferredHolder<Block, Block> PRODUCTION_INPUT = REGISTRY.register("production_input", () -> new ProductionInputBlock());
   public static final DeferredHolder<Block, Block> PRODUCTION_OUTPUT = REGISTRY.register("production_output", () -> new ProductionOutputBlock());
   public static final DeferredHolder<Block, Block> ASSEMBLY_DEPOT = REGISTRY.register("assembly_depot", () -> new AssemblyDepotBlock());
   public static final DeferredHolder<Block, Block> ASSEMBLY_MACHINE = REGISTRY.register("assembly_machine", () -> new AssemblyMachineBlock());
   public static final DeferredHolder<Block, Block> ASSEMBLY_CRUSHER = REGISTRY.register("assembly_crusher", () -> new AssemblyCrusherBlock());
   public static final DeferredHolder<Block, Block> ASSEMBLY_FURNACE = REGISTRY.register("assembly_furnace", () -> new AssemblyFurnaceBlock());
   public static final DeferredHolder<Block, Block> NITRATE_BLOCK = REGISTRY.register("nitrate_block", () -> new NitrateBlockBlock());
   public static final DeferredHolder<Block, Block> BAUXITE_DIGESTER = REGISTRY.register("bauxite_digester", () -> new BauxiteDigesterBlock());
   public static final DeferredHolder<Block, Block> ASSEMBLY_CENTRIFUGE_BOTTOM = REGISTRY.register(
      "assembly_centrifuge_bottom", () -> new AssemblyCentrifugeBottomBlock()
   );
   public static final DeferredHolder<Block, Block> ASSEMBLY_CENTRIFUGE_MIDDLE = REGISTRY.register(
      "assembly_centrifuge_middle", () -> new AssemblyCentrifugeMiddleBlock()
   );
   public static final DeferredHolder<Block, Block> ASSEMBLY_CENTRIFUGE_TOP = REGISTRY.register("assembly_centrifuge_top", () -> new AssemblyCentrifugeTopBlock());
   public static final DeferredHolder<Block, Block> CONVEYOR_SPLITTER = REGISTRY.register("conveyor_splitter", () -> new ConveyorSplitterBlock());
   public static final DeferredHolder<Block, Block> LIGHT_WOOD_FRAME = REGISTRY.register("light_wood_frame", () -> new LightWoodFrameBlock());
   public static final DeferredHolder<Block, Block> LIGHT_WOOD_BLOCK = REGISTRY.register("light_wood_block", () -> new LightWoodBlockBlock());
   public static final DeferredHolder<Block, Block> LIGHT_WOOD_SLAB = REGISTRY.register("light_wood_slab", () -> new LightWoodSlabBlock());
   public static final DeferredHolder<Block, Block> LIGHT_WOOD_STAIRS = REGISTRY.register("light_wood_stairs", () -> new LightWoodStairsBlock());
   public static final DeferredHolder<Block, Block> LIGHT_WOOD_SIDE_PANEL = REGISTRY.register("light_wood_side_panel", () -> new LightWoodSidePanelBlock());
   public static final DeferredHolder<Block, Block> LIGHT_WOOD_TRAPDOOR = REGISTRY.register("light_wood_trapdoor", () -> new LightWoodTrapdoorBlock());
   public static final DeferredHolder<Block, Block> ASSEMBLY_CIRCUIT_FABRICATOR = REGISTRY.register(
      "assembly_circuit_fabricator", () -> new AssemblyCircuitFabricatorBlock()
   );
   public static final DeferredHolder<Block, Block> ASSEMBLY_MECHANICAL_FABRICATOR = REGISTRY.register(
      "assembly_mechanical_fabricator", () -> new AssemblyMechanicalFabricatorBlock()
   );
   public static final DeferredHolder<Block, Block> PYROCHLORE_ORE = REGISTRY.register("pyrochlore_ore", () -> new PyrochloreOreBlock());
   public static final DeferredHolder<Block, Block> NIOBIUM_BLOCK = REGISTRY.register("niobium_block", () -> new NiobiumBlockBlock());
   public static final DeferredHolder<Block, Block> PYROCHLORE_BLOCK = REGISTRY.register("pyrochlore_block", () -> new PyrochloreBlockBlock());
   public static final DeferredHolder<Block, Block> URANIUM_NEUTRAL_BLOCK = REGISTRY.register("uranium_neutral_block", () -> new UraniumNeutralBlockBlock());
   public static final DeferredHolder<Block, Block> URANIUM_ENRICHED_BLOCK = REGISTRY.register("uranium_enriched_block", () -> new UraniumEnrichedBlockBlock());
   public static final DeferredHolder<Block, Block> URANIUM_DEPLETED_BLOCK = REGISTRY.register("uranium_depleted_block", () -> new UraniumDepletedBlockBlock());
   public static final DeferredHolder<Block, Block> PLUTONIUM_BLOCK = REGISTRY.register("plutonium_block", () -> new PlutoniumBlockBlock());
   public static final DeferredHolder<Block, Block> ADVANCED_ALLOY_BLOCK = REGISTRY.register("advanced_alloy_block", () -> new AdvancedAlloyBlockBlock());
   public static final DeferredHolder<Block, Block> PHOSPHATE_BLOCK = REGISTRY.register("phosphate_block", () -> new PhosphateBlockBlock());
   public static final DeferredHolder<Block, Block> SULFURIC_ACID = REGISTRY.register("sulfuric_acid", () -> new SulfuricAcidBlock());
   public static final DeferredHolder<Block, Block> COMPRESSED_AIR = REGISTRY.register("compressed_air", () -> new CompressedAirBlock());
   public static final DeferredHolder<Block, Block> LIQUID_OXYGEN = REGISTRY.register("liquid_oxygen", () -> new LiquidOxygenBlock());
   public static final DeferredHolder<Block, Block> LIQUID_HYDROGEN = REGISTRY.register("liquid_hydrogen", () -> new LiquidHydrogenBlock());
   public static final DeferredHolder<Block, Block> CHLORINE_GAS = REGISTRY.register("chlorine_gas", () -> new ChlorineGasBlock());
   public static final DeferredHolder<Block, Block> HYDRAZINE = REGISTRY.register("hydrazine", () -> new HydrazineBlock());
   public static final DeferredHolder<Block, Block> MINIGUN = REGISTRY.register("minigun", () -> new MinigunBlock());
   public static final DeferredHolder<Block, Block> MINI_GUN_BARREL = REGISTRY.register("mini_gun_barrel", () -> new MiniGunBarrelBlock());
   public static final DeferredHolder<Block, Block> ORDINANCE_RELOCATOR = REGISTRY.register("ordinance_relocator", () -> new OrdinanceRelocatorBlock());
   public static final DeferredHolder<Block, Block> ITEM_INCINERATOR = REGISTRY.register("item_incinerator", () -> new ItemIncineratorBlock());
   public static final DeferredHolder<Block, Block> TYPE_1_BC_MUZZLE_BRAKE = REGISTRY.register("type_1_bc_muzzle_brake", () -> new Type1BCMuzzleBrakeBlock());
   public static final DeferredHolder<Block, Block> TYPE_2_BC_MUZZLE_BRAKE = REGISTRY.register("type_2_bc_muzzle_brake", () -> new Type2BCMuzzleBrakeBlock());
   public static final DeferredHolder<Block, Block> BATTLE_CANNON_MANTLET = REGISTRY.register("battle_cannon_mantlet", () -> new BattleCannonMantletBlock());
   public static final DeferredHolder<Block, Block> REDIRECTOR_SHAFT = REGISTRY.register("redirector_shaft", () -> new RedirectorShaftBlock());
}
