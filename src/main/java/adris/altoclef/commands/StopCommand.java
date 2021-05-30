package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;

public class StopCommand extends Command {

    public StopCommand() {
        super("stop", "Stop task runner (stops all automation)");
    }

    @Override
    protected void Call(AltoClef mod, ArgParser parser) {
        mod.getTaskRunner().disable();
        finish();
    }
}
