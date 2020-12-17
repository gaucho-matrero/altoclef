package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.resources.CollectPlanksTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.TaskCatalogue;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import sun.security.util.ArrayUtil;

import java.util.Arrays;

/// This structure was copied from a C# project. Fuck java. All my homies hate java.
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class AltoClefCommands extends CommandList {

    private static void TEMP_TEST_FUNCTION(AltoClef mod) {
        //mod.runUserTask();
        /*
        TaskCatalogue.getItemTask("log", 5);
        TaskCatalogue.getItemTask("planks", 4);
        TaskCatalogue.getItemTask("log", 5);
         */
        Debug.logMessage("Running test...");

        /*
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(
                new Item[][]{
                        ItemTarget.PLANKS, null,
                        ItemTarget.PLANKS, null
                        //new Item[] {Items.OAK_PLANKS}, null,
                        //new Item[] {Items.OAK_PLANKS}, null
                }
        );

        if (mod.getInventoryTracker().craftInstant(recipe)) {
            Debug.logMessage("Craft Success!");
        } else {
            Debug.logWarning("Craft failed.");
        }
         */

        mod.runUserTask(new CollectPlanksTask(20));

    }

    public AltoClefCommands(CommandExecutor executor) throws CommandException {
        super(executor,
            // List commands here
            new HelpCommand(),
            new GetCommand(),
            new StopCommand(),
            new TestCommand()
            //new TestMoveInventoryCommand(),
            //    new TestSwapInventoryCommand()
        );
    }

    static class HelpCommand extends Command {

        public HelpCommand() {
            super("help", "Lists all commands");
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) {
            Debug.logMessage("########## HELP: ##########");
            int padSize = 10;
            for(Command c : mod.getCommandExecutor().AllCommands()) {
                StringBuilder line = new StringBuilder();
                //line.append("");
                line.append(c.getName());
                int toAdd = padSize - c.getName().length();
                for (int i = 0; i < toAdd; ++i) {
                    line.append(" ");
                }
                line.append(" ");
                line.append(c.getDescription());
                Debug.logMessage(line.toString());
            }
            Debug.logMessage("###########################");
        }
    }

    static class StopCommand extends Command {

        public StopCommand() {
            super("stop", "Stop task runner (stops all automation)");
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) {
            mod.getTaskRunner().disable();
        }
    }

    static class GetCommand extends Command {

        public GetCommand() throws CommandException {
            super("get", "Get an item/resource",
                    new Arg(String.class, "name"),
                    new Arg(Integer.class, "count", 1, 1));
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            String resourceName = parser.Get(String.class);
            int count = parser.Get(Integer.class);

            Task targetTask = TaskCatalogue.getItemTask(resourceName, count);
            if (targetTask == null) {
                Debug.logWarning("\"" + resourceName + "\" is not a catalogued resource. Can't get it yet, sorry! If it's a generic block try using baritone.");
                Debug.logWarning("Here's a list of everything we can get for you though:");
                Debug.logWarning(Arrays.toString(TaskCatalogue.resourceNames().toArray()));
            } else {
                mod.runUserTask(targetTask);
            }
        }
    }

    static class TestCommand extends Command {

        public TestCommand() {
            super("test", "Generic command for testing");
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) {
            TEMP_TEST_FUNCTION(mod);
        }
    }

    static class TestMoveInventoryCommand extends Command {

        public TestMoveInventoryCommand() throws Exception {
            super("testmoveinv", "Test command to move items around in inventory",
                    new Arg(Integer.class, "from"),
                    new Arg(Integer.class, "to"),
                    new Arg(Integer.class, "amount", 1, 2)
            );
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            int from = parser.Get(Integer.class);
            int to = parser.Get(Integer.class);
            int amount = parser.Get(Integer.class);

            int moved = mod.getInventoryTracker().moveItems(new PlayerSlot(from), new PlayerSlot(to), amount);
            Debug.logMessage("Successfully moved " + moved + " items.");
        }
    }
    static class TestSwapInventoryCommand extends Command {

        public TestSwapInventoryCommand() throws CommandException {
            super("testswapinv", "Test command to swap two slots in the inventory",
                    new Arg(Integer.class, "slot1"),
                    new Arg(Integer.class, "slot2")
            );
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            int slot1 = parser.Get(Integer.class);
            int slot2 = parser.Get(Integer.class);

            mod.getInventoryTracker().swapItems(new PlayerSlot(slot1), new PlayerSlot(slot2));
            Debug.logMessage("Successfully swapped.");
        }
    }
}
