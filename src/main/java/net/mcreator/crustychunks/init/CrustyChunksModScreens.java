package net.mcreator.crustychunks.init;

import net.mcreator.crustychunks.client.gui.ABGuiScreen;
import net.mcreator.crustychunks.client.gui.BlastFurnaceGUIScreen;
import net.mcreator.crustychunks.client.gui.BreederReactorControlGUIScreen;
import net.mcreator.crustychunks.client.gui.ConveyorGUIScreen;
import net.mcreator.crustychunks.client.gui.ConveyorSplitterGUIScreen;
import net.mcreator.crustychunks.client.gui.CrusherGUIScreen;
import net.mcreator.crustychunks.client.gui.EnergyDisplayScreen;
import net.mcreator.crustychunks.client.gui.FireboxGUIScreen;
import net.mcreator.crustychunks.client.gui.FissionBombGUIScreen;
import net.mcreator.crustychunks.client.gui.FissionWarheadGUIScreen;
import net.mcreator.crustychunks.client.gui.FoundryGUIScreen;
import net.mcreator.crustychunks.client.gui.FusionBombGUIScreen;
import net.mcreator.crustychunks.client.gui.MachineputGUI4xScreen;
import net.mcreator.crustychunks.client.gui.MineralGrinderGUIScreen;
import net.mcreator.crustychunks.client.gui.ProgressGUIScreen;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class CrustyChunksModScreens {
   @SubscribeEvent
   public static void clientLoad(RegisterMenuScreensEvent event) {
      event.register((MenuType)CrustyChunksModMenus.BLAST_FURNACE_GUI.get(), BlastFurnaceGUIScreen::new);
      event.register((MenuType)CrustyChunksModMenus.CONVEYOR_GUI.get(), ConveyorGUIScreen::new);
      event.register((MenuType)CrustyChunksModMenus.FOUNDRY_GUI.get(), FoundryGUIScreen::new);
      event.register((MenuType)CrustyChunksModMenus.FIREBOX_GUI.get(), FireboxGUIScreen::new);
      event.register((MenuType)CrustyChunksModMenus.MINERAL_GRINDER_GUI.get(), MineralGrinderGUIScreen::new);
      event.register((MenuType)CrustyChunksModMenus.FISSION_BOMB_GUI.get(), FissionBombGUIScreen::new);
      event.register((MenuType)CrustyChunksModMenus.BREEDER_REACTOR_CONTROL_GUI.get(), BreederReactorControlGUIScreen::new);
      event.register((MenuType)CrustyChunksModMenus.FISSION_WARHEAD_GUI.get(), FissionWarheadGUIScreen::new);
      event.register((MenuType)CrustyChunksModMenus.ENERGY_DISPLAY.get(), EnergyDisplayScreen::new);
      event.register((MenuType)CrustyChunksModMenus.AB_GUI.get(), ABGuiScreen::new);
      event.register((MenuType)CrustyChunksModMenus.FUSION_BOMB_GUI.get(), FusionBombGUIScreen::new);
      event.register((MenuType)CrustyChunksModMenus.MACHINEPUT_GUI_4X.get(), MachineputGUI4xScreen::new);
      event.register((MenuType)CrustyChunksModMenus.CRUSHER_GUI.get(), CrusherGUIScreen::new);
      event.register((MenuType)CrustyChunksModMenus.CONVEYOR_SPLITTER_GUI.get(), ConveyorSplitterGUIScreen::new);
      event.register((MenuType)CrustyChunksModMenus.PROGRESS_GUI.get(), ProgressGUIScreen::new);
   }
}
