package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.movement.GetToXZWithElytraTask;

public class GotoWithElytraCommand extends Command {
    public GotoWithElytraCommand() throws CommandException {
        super("elytra", "Tell bot to travel to a set of coordinates using Elytra", new Arg(Integer.class, "x"), new Arg(Integer.class, "z"));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        int x = parser.get(Integer.class);
        int z = parser.get(Integer.class);
        mod.runUserTask(new GetToXZWithElytraTask(x,z), this::finish);
    }
}