package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.DefaultGoToDimensionTask;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.GetToXZTask;
import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
            jank.LoadArgs(parser.getArgUnits()[expectedIndex], false);
            return jank.Get(Dimension.class);
        }
        return null;
    }

    @Override
    protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
        int x = parser.Get(Integer.class);
        int y = parser.Get(Integer.class);
        // Turbo jank duct tape below, accounting for possibility of (x z dimension)
        int z = EMPTY;
        Dimension dimension = null;
        try {
            z = parser.Get(Integer.class);
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
            dimension = parser.Get(Dimension.class);
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
            mod.runUserTask(new DefaultGoToDimensionTask(dimension), nothing -> finish());
        } else if (y != EMPTY) {
            BlockPos target = new BlockPos(x, y, z);
            mod.runUserTask(new GetToBlockTask(target, dimension), nothing1 -> finish());
        } else {
            mod.runUserTask(new GetToXZTask(x, z, dimension), nothing -> finish());
        }
    }
}
