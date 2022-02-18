package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;

/**
 * Out of all the commands, this one probably demonstrates
 * why we need a better arg parsing system. Please.
 */
public class GotoCommand extends Command {
    private static final int EMPTY = Integer.MAX_VALUE;

    public GotoCommand() throws CommandException {
        // x z
        // x y z
        // x y z dimension
        // (dimension)
        // (x z dimension)
        super("goto", "Tell bot to travel to a set of coordinates.",
                new Arg(Integer.class, "x", EMPTY, 1, false),
                new Arg(Integer.class, "y", EMPTY, 2, false),
                new Arg(Integer.class, "z", EMPTY, 1, false),
                new Arg(Dimension.class, "dimension", null, 3, false)
        );
    }

    private static Dimension getDimensionJank(ArgParser parser, int expectedIndex) throws CommandException {
        // Massive duct tape, if only one arg parse it as a dimension manually.
        if (parser.getArgUnits().length == expectedIndex + 1) {
            ArgParser jank = new ArgParser(new Arg(Dimension.class, "dimension"));
            jank.loadArgs(parser.getArgUnits()[expectedIndex], false);
            return jank.get(Dimension.class);
        }
        return null;
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        int x = parser.get(Integer.class);
        int y = parser.get(Integer.class);
        // Turbo jank duct tape below, accounting for possibility of (x z dimension)
        int z = EMPTY;
        Dimension dimension = null;
        try {
            z = parser.get(Integer.class);
        } catch (CommandException e) {
            // z might just be the dimension.
            if (parser.getArgUnits().length == 3) {
                dimension = getDimensionJank(parser, 2);
                if (dimension != null) {
                    // it WORKED! Our argument order is now messed up.
                    z = y;
                    y = EMPTY;
                } else {
                    // We failed, z is not the dimension.
                    throw e;
                }
            } else {
                // We failed, too many arguments.
                throw e;
            }
        }
        if (dimension == null) {
            dimension = parser.get(Dimension.class);
        }
        if(x == EMPTY && y == EMPTY && z == EMPTY) {
            // Require dimension as the only argument
            if (dimension == null) {
                dimension = getDimensionJank(parser, 0);
                if (dimension == null) {
                    finish();
                    return;
                }
            }
            mod.runUserTask(new DefaultGoToDimensionTask(dimension), this::finish);
        } else if (y != EMPTY) {
            BlockPos target = new BlockPos(x, y, z);
            mod.runUserTask(new GetToBlockTask(target, dimension), this::finish);
        } else {
            mod.runUserTask(new GetToXZTask(x, z, dimension), this::finish);
        }
    }
}
