package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.misc.LocateDesertTempleTask;
import adris.altoclef.tasks.misc.speedrun.LocateStrongholdTask;

public class LocateStructureCommand extends Command {

    public LocateStructureCommand() throws CommandException {
        super("locate_structure", "Locate a world generated structure.", new Arg(Structure.class, "structure"));
    }

    @Override
    protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
        Structure structure = parser.Get(Structure.class);
        switch (structure) {
            case STRONGHOLD:
                mod.runUserTask(new LocateStrongholdTask(1), nothing -> finish());
                break;
            case DESERT_TEMPLE:
                mod.runUserTask(new LocateDesertTempleTask(), nothing -> finish());
                break;
        }
    }

    public enum Structure {
        DESERT_TEMPLE,
        STRONGHOLD
    }
}