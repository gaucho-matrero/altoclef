package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.util.ItemTarget;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// This structure was copied from a C# project. Fuck java. All my homies hate java.
public class AltoClefCommands extends CommandList {

    private static void TEMP_TEST_FUNCTION(AltoClef mod) {
        //mod.runUserTask();
        Debug.logMessage("Running test...");
        mod.runUserTask(new MineAndCollectTask(
                Arrays.asList(new ItemTarget(Items.DIRT, 5),
                        new ItemTarget(Items.OAK_LOG, 5))
        ));
    }

    public AltoClefCommands(CommandExecutor executor) throws Exception {
        super(executor,
            // List commands here
            new HelpCommand(),
            new StopCommand(),
            new TestCommand()
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
}
