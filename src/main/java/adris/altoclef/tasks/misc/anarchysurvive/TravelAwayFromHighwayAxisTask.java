package adris.altoclef.tasks.misc.anarchysurvive;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CustomBaritoneGoalTask;
import adris.altoclef.tasksystem.Task;
import baritone.api.pathing.goals.Goal;

public class TravelAwayFromHighwayAxisTask extends CustomBaritoneGoalTask {

    private final HighwayAxis _axis;

    public TravelAwayFromHighwayAxisTask(HighwayAxis axis) {
        _axis = axis;
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new TravelAwayFromHighwayAxisGoal();
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof TravelAwayFromHighwayAxisTask && ((TravelAwayFromHighwayAxisTask)obj)._axis == _axis;
    }

    @Override
    protected String toDebugString() {
        return "Traveling away from " + _axis;
    }

    private class TravelAwayFromHighwayAxisGoal implements Goal {

        @Override
        public boolean isInGoal(int x, int y, int z) {
            return false;
        }

        @Override
        public double heuristic(int x, int y, int z) {
            return -1 * _axis.getDistanceFrom(x, z);
        }
    }
}
