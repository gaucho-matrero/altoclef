package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalYLevel;

public class GetToYTask extends CustomBaritoneGoalTask {

    private final int _yLevel;

    public GetToYTask(int ylevel) {
        _yLevel = ylevel;
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalYLevel(_yLevel);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GetToYTask) {
            return ((GetToYTask) obj)._yLevel == _yLevel;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Going to y=" + _yLevel;
    }
}
