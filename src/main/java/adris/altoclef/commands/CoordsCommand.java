package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;

public class CoordsCommand extends Command {
    public CoordsCommand() {
        super("coords", "Get bot's current coordinates");
    }

    @Override
    protected void Call(AltoClef mod, ArgParser parser) {
        mod.log("CURRENT COORDINATES: " + mod.getPlayer().getBlockPos().toShortString() + " (Current dimension: " + mod.getCurrentDimension() + ")");
        finish();
    }
}
