package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CustomBaritoneGoalTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalRunAwayFromHostiles;
import baritone.api.pathing.goals.Goal;

public class RunAwayFromHostilesTask extends CustomBaritoneGoalTask {

    private final double _distanceToRun;

    public RunAwayFromHostilesTask(double distance) {
        _distanceToRun = distance;
    }


    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalRunAwayFromHostiles(mod, _distanceToRun);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof RunAwayFromHostilesTask) {
            RunAwayFromHostilesTask other = (RunAwayFromHostilesTask) obj;
            return Math.abs(other._distanceToRun - _distanceToRun) < 1;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "NIGERUNDAYOO, SUMOOKEYY!";
    }
}
