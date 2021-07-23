package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import net.minecraft.util.math.BlockPos;

public class GetToBlockTask extends CustomBaritoneGoalTask implements ITaskRequiresGrounded {

    private final BlockPos _position;
    private final boolean _preferStairs;
    private final Dimension _dimension;

    public GetToBlockTask(BlockPos position, boolean preferStairs) {
        this(position, preferStairs, null);
    }
    public GetToBlockTask(BlockPos position, Dimension dimension) {
        this(position, false, dimension);
    }

    public GetToBlockTask(BlockPos position, boolean preferStairs, Dimension dimension) {
        _dimension = dimension;
        _position = position;
        _preferStairs = preferStairs;
    }

    public GetToBlockTask(BlockPos position) {
        this(position, false);
    }
    @Override
    protected Task onTick(AltoClef mod) {
        if (_dimension != null && mod.getCurrentDimension() != _dimension) {
            return new DefaultGoToDimensionTask(_dimension);
        }
        return super.onTick(mod);
    }

    @Override
    protected void onStart(AltoClef mod) {
        if (_preferStairs) {
            mod.getBehaviour().push();
            mod.getBehaviour().setPreferredStairs(true);
        }
        super.onStart(mod);
    }


    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        if (_preferStairs) {
            mod.getBehaviour().pop();
        }
        super.onStop(mod, interruptTask);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GetToBlockTask) {
            GetToBlockTask other = (GetToBlockTask) obj;
            return other._position.equals(_position) && other._preferStairs == _preferStairs && other._dimension == _dimension;
        }
        return false;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return super.isFinished(mod) && (_dimension == null || _dimension == mod.getCurrentDimension());
    }

    @Override
    protected String toDebugString() {
        return "Getting to block " + _position + (_dimension != null ? " in dimension " + _dimension : "");
    }


    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalBlock(_position);
    }

    @Override
    protected void onWander(AltoClef mod) {
        mod.getBlockTracker().unreachable(_position);
        super.onWander(mod);
    }
}
