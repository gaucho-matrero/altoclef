package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.ui.MessagePriority;
import adris.altoclef.util.ItemTarget;

import java.util.HashMap;
import java.util.function.Consumer;

public class GetCommand extends Command {

    public GetCommand() throws CommandException {
        super("get", "Get an item/resource",
                new Arg(String.class, "name"),
                new Arg(Integer.class, "count", 1, 1));
    }

    // We may ignore our "one item" rule in place of an item array.
    @Override
    public void run(AltoClef mod, String line, Consumer onFinish) throws CommandException {
        try {
            super.run(mod, line, onFinish);
        } catch (CommandException e) {
            line = line.substring(getName().length() + 1).trim();
            // Might be an array of items...
            if (line.startsWith("[") && line.endsWith("]")) {
                line = line.substring(1, line.length() - 1);
                String[] parts = line.split(",");
                HashMap<String, Integer> items = new HashMap<>();
                for (String part : parts) {
                    part = part.trim();
                    String[] itemQuantityPair = part.split(" ");
                    if (itemQuantityPair.length > 2 || itemQuantityPair.length <= 0) {
                        // Must be either "item count" or "item"
                        throw new CommandException("Resource array element must be either \"item count\" or \"item\", but \"" + part + "\"" + " has " + itemQuantityPair.length + " parts.");
                    }
                    String item = itemQuantityPair[0];
                    int count = 1;
                    if (itemQuantityPair.length > 1) {
                        try {
                            count = Integer.parseInt(itemQuantityPair[1]);
                        } catch (Exception iex) {
                            throw new CommandException("Failed to parse count for array element \"" + part + "\".");
                        }
                    }
                    if (TaskCatalogue.taskExists(item)) {
                        items.put(item, items.getOrDefault(item, 0) + count);
                    } else {
                        OnResourceDoesNotExist(mod, item);
                        finish();
                        return;
                    }
                }
                if (items.size() != 0) {
                    GetItems(mod, items.entrySet().stream().map(entry -> new ItemTarget(entry.getKey(), entry.getValue())).toArray(ItemTarget[]::new));
                } else {
                    mod.log("No items specified.");
                    finish();
                }
            } else {
                throw e;
            }
        }
    }

    private static void OnResourceDoesNotExist(AltoClef mod, String resource) {
        mod.log("\"" + resource + "\" is not a catalogued resource. Can't get it yet, sorry! If it's a generic block try using baritone.", MessagePriority.OPTIONAL);
        mod.log("Use @list to get a list of available resources.", MessagePriority.OPTIONAL);
    }

    private void GetItems(AltoClef mod, ItemTarget... items) {
        Task targetTask;
        if (items.length == 1) {
            targetTask = TaskCatalogue.getItemTask(items[0]);
        } else {
            targetTask = TaskCatalogue.getSquashedItemTask(items);
        }
        if (targetTask != null) {
            mod.runUserTask(targetTask, this::finish);
        } else {
            finish();
        }
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String resourceName = parser.get(String.class);
        int count = parser.get(Integer.class);
        if (TaskCatalogue.taskExists(resourceName)) {
            GetItems(mod, new ItemTarget(resourceName, count));
        } else {
            OnResourceDoesNotExist(mod, resourceName);
            finish();
        }
    }
}