package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.FastTravelTask;
import adris.altoclef.tasks.movement.FastTravelTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;

//Fast travels if you are 1000 blocks away or more.
public class TravelCommand extends Command {
    private static final int EMPTY = Integer.MAX_VALUE;

    public TravelCommand() throws CommandException {
        super("travel", "travels to <x,y,z>. Will utilize nether portal if more than 1000 blocks away",
                new Arg(Integer.class, "x", EMPTY, 1, false),
                new Arg(Integer.class, "y", EMPTY, 2, false),
                new Arg(Integer.class, "z", EMPTY, 1, false),
                new Arg(Dimension.class, "dimension", null, 3, false));
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
            //Check if we should fast travel
            /*
                IF player pos - target pos >= |1000|
                    THEN
                        Launch fast travel command
                            {
                                IF player has less than 64 cobblestone
                                    THEN
                                        get cobblestone x20
                               END IF

                               IF player doesn't have enough obsidian
                                    THEN
                                        get obsidian x10
                              END IF

                              IF player is within 15 blocks of target nether portal exit
                                    THEN
                                        match nether portal target y and player y
                              ELSE
                                    GOTO nether portal target
                              END IF

                              IF player pos == nether portal target
                                    THEN
                                            build portal
                                            exit portal
                              END IF

                              IF player pos != target pos
                                THEN
                                    goto target pos (no fast travel)
                              ELSE
                                    STOP
                              END IF
                            }
                ELSE
                    goto target pos (no fast travel)
             */
              boolean shouldFastTravel = false;
            if(shouldFastTravel){
                mod.runUserTask(new FastTravelTask(x,y,z));
            }else {
                mod.runUserTask(new DefaultGoToDimensionTask(dimension), this::finish);
            }
        } else if (y != EMPTY) {
            BlockPos target = new BlockPos(x, y, z);
            boolean shouldFastTravel = false; // temp
            if(shouldFastTravel){
                mod.runUserTask(new FastTravelTask(x,y,z));
            }else {
                mod.runUserTask(new GetToBlockTask(target, dimension), this::finish);            }
        } else {
            boolean shouldFastTravel = false; // temp
            if (shouldFastTravel) {
                mod.runUserTask(new FastTravelTask(x, y, z));
            } else {
                mod.runUserTask(new GetToXZTask(x, z, dimension), this::finish);
            }
        }
    }
}
