package shipwrights.genesis.tests.commands;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class GameTestCommands {

    public static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        AssembleShipCommand.register(event);
    }
}
