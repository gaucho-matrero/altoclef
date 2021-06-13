package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.Util;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalRunAway;
import net.minecraft.util.math.BlockPos;

public class RunAwayFromPositionTask extends CustomBaritoneGoalTask {

    private final BlockPos[] _dangerBlocks;

    private final double _distance;

    public RunAwayFromPositionTask(double distance, BlockPos... toRunAwayFrom) {
        _distance = distance;
        _dangerBlocks = toRunAwayFrom;
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalRunAway(_distance, _dangerBlocks);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof RunAwayFromPositionTask) {
            return Util.arraysEqual(((RunAwayFromPositionTask) obj)._dangerBlocks, _dangerBlocks);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Running away from " + Util.arrayToString(_dangerBlocks);
    }
}
