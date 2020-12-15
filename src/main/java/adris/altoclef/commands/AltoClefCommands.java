package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

/// This structure was copied from a C# project. Fuck java. All my homies hate java.
public class AltoClefCommands extends CommandList {

    public AltoClefCommands(CommandExecutor executor) throws Exception {
        super(executor,
            new TestCommand()
            // TODO: List commands here
        );
    }

    static class TestCommand extends Command {

        public TestCommand() throws Exception {
            super("test", "A test command",
                new Arg(Float.class, "number", 4f, 0),
                new Arg<String>(String.class, "name", "billy", 1)
            );
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws Exception {
            float number = parser.Get(Float.class);
            String name = parser.Get(String.class);
            Debug.logMessage("RAN WITH ARGS " + number + " " + name );
        }
    }
}
