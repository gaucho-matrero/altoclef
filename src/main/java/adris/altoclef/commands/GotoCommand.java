package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.GetToXZTask;
import net.minecraft.util.math.BlockPos;

public class GotoCommand extends Command {
    private static final int EMPTY = -1;
    public GotoCommand() throws CommandException {
        super("goto", "Tell bot to travel to a set of coordinates.", new Arg(Integer.class, "x"), new Arg(Integer.class, "y", EMPTY, 2), new Arg(Integer.class, "z"));
    }

    @Override
    protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
        int x = parser.Get(Integer.class),
                y = parser.Get(Integer.class),
                z = parser.Get(Integer.class);
        if (y != EMPTY) {
            mod.runUserTask(new GetToBlockTask(new BlockPos(x, y, z), false), nothing -> finish());
        } else {
            mod.runUserTask(new GetToXZTask(x, z), nothing -> finish());
        }
    }
}
