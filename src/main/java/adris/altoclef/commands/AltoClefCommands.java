package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.PlayerInventorySlot;
import net.minecraft.item.Items;

import java.util.Arrays;

/// This structure was copied from a C# project. Fuck java. All my homies hate java.
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class AltoClefCommands extends CommandList {

    private static void TEMP_TEST_FUNCTION(AltoClef mod) {
        //mod.runUserTask();
        Debug.logMessage("Running test...");
        mod.runUserTask(new MineAndCollectTask(
                Arrays.asList(new ItemTarget(Items.DIRT, 5),
                        new ItemTarget(Items.OAK_LOG, 5))
        ));
    }

    public AltoClefCommands(CommandExecutor executor) {
        super(executor,
            // List commands here
            new HelpCommand(),
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
        protected void Call(AltoClef mod, ArgParser parser) throws Exception {
            int from = parser.Get(Integer.class);
            int to = parser.Get(Integer.class);
            int amount = parser.Get(Integer.class);

            int moved = mod.getInventoryTracker().moveItems(new PlayerInventorySlot(from), new PlayerInventorySlot(to), amount);
            Debug.logMessage("Successfully moved " + moved + " items.");
        }
    }
    static class TestSwapInventoryCommand extends Command {

        public TestSwapInventoryCommand() throws Exception {
            super("testswapinv", "Test command to swap two slots in the inventory",
                    new Arg(Integer.class, "slot1"),
                    new Arg(Integer.class, "slot2")
            );
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws Exception {
            int slot1 = parser.Get(Integer.class);
            int slot2 = parser.Get(Integer.class);

            mod.getInventoryTracker().swapItems(new PlayerInventorySlot(slot1), new PlayerInventorySlot(slot2));
            Debug.logMessage("Successfully swapped.");
        }
    }
}
