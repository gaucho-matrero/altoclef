package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.misc.speedrun.BeatMinecraftTask;

public class GamerCommand extends Command {
    public GamerCommand() {
        super("gamer", "Beats the game");
    }

    @Override
    protected void Call(AltoClef mod, ArgParser parser) {
        mod.runUserTask(new BeatMinecraftTask(), nothing -> finish());
    }
}
