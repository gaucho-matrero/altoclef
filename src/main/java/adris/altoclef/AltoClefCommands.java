package adris.altoclef;

import adris.altoclef.commands.*;
import adris.altoclef.commandsystem.CommandException;

/// This structure was copied from a C# project. Fuck java. All my homies hate java.
public class AltoClefCommands {

    public AltoClefCommands() throws CommandException {
        // List commands here
        AltoClef.getCommandExecutor().RegisterNewCommand(
                new HelpCommand(),
                new GetCommand(),
                new FollowCommand(),
                new GiveCommand(),
                new GotoCommand(),
                new CoordsCommand(),
                new StatusCommand(),
                new InventoryCommand(),
                new LocateStructureCommand(),
                new StopCommand(),
                new TestCommand(),
                new FoodCommand(),
                new ReloadSettingsCommand(),
                new GamerCommand(),
                new PunkCommand(),
                new SetGammaCommand(),
                new ListCommand()
                //new TestMoveInventoryCommand(),
                //    new TestSwapInventoryCommand()
        );
    }
}
