package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalYLevel;


public class GetToYTask extends CustomBaritoneGoalTask {
    private final int yLevel;

    public GetToYTask(int ylevel) {
        yLevel = ylevel;
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalYLevel(yLevel);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GetToYTask) {
            return ((GetToYTask) obj).yLevel == yLevel;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Going to y=" + yLevel;
    }
}
