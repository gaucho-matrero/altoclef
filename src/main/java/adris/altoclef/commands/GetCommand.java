package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.ui.MessagePriority;

import java.util.Arrays;

public class GetCommand extends Command {

    public GetCommand() throws CommandException {
        super("get", "Get an item/resource",
                new Arg(String.class, "name"),
                new Arg(Integer.class, "count", 1, 1));
    }

    @Override
    protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
        String resourceName = parser.Get(String.class);
        int count = parser.Get(Integer.class);

        if (TaskCatalogue.taskExists(resourceName)) {
            Task targetTask = TaskCatalogue.getItemTask(resourceName, count);
            mod.runUserTask(targetTask, nothing -> finish());
        } else {
            mod.log("\"" + resourceName + "\" is not a catalogued resource. Can't get it yet, sorry! If it's a generic block try using baritone.", MessagePriority.OPTIONAL);
            mod.log("Here's a list of everything we can get for you though:", MessagePriority.OPTIONAL);
            mod.log(Arrays.toString(TaskCatalogue.resourceNames().toArray()), MessagePriority.OPTIONAL);
            finish();
        }
    }
}