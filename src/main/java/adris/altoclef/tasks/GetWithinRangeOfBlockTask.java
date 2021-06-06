package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import net.minecraft.util.math.BlockPos;

public class GetWithinRangeOfBlockTask extends CustomBaritoneGoalTask {

    private final BlockPos _blockPos;
    private final int _range;

    public GetWithinRangeOfBlockTask(BlockPos blockPos, int range) {
        _blockPos = blockPos;
        _range = range;
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalNear(_blockPos, _range);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GetWithinRangeOfBlockTask) {
            GetWithinRangeOfBlockTask other = (GetWithinRangeOfBlockTask) obj;
            return other._blockPos.equals(_blockPos) && other._range == _range;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Getting within " + _range + " blocks of " + _blockPos.toShortString();
    }
}
