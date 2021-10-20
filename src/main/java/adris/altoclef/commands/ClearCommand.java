package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.construction.ClearRegionTask;
import baritone.api.BaritoneAPI;
import baritone.api.selection.ISelection;

public class ClearCommand extends Command {

    public ClearCommand() throws CommandException{
        super("clear", "Clears a specified region", new Arg(String.class, "extra", null, 0));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        ISelection selection = BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().getLastSelection();
        mod.runUserTask(new ClearRegionTask(selection.pos1(), selection.pos2()));
    }
}
