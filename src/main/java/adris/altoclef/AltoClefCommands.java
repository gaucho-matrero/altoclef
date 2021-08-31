package adris.altoclef;

import adris.altoclef.commands.*;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.commandsystem.CommandExecutor;
import adris.altoclef.commandsystem.CommandList;

/// This structure was copied from a C# project. Fuck java. All my homies hate java.
public class AltoClefCommands extends CommandList {

    public AltoClefCommands(CommandExecutor executor) throws CommandException {
        super(executor,
                // List commands here
                new HelpCommand(),
                new GetCommand(),
                new FollowCommand(),
                new GiveCommand(),
                new EquipCommand(),
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
                new SetGammaCommand()
                //new TestMoveInventoryCommand(),
                //    new TestSwapInventoryCommand()
        );
    }

}
