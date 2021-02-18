package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CustomBaritoneGoalTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalDodgeProjectiles;
import baritone.api.pathing.goals.Goal;

public class DodgeProjectilesTask extends CustomBaritoneGoalTask {

    private final double _distanceHorizontal;
    private final double _distanceVertical;

    public DodgeProjectilesTask(double distanceHorizontal, double distanceVertical) {
        _distanceHorizontal = distanceHorizontal;
        _distanceVertical = distanceVertical;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_cachedGoal != null) {
            GoalDodgeProjectiles goal = (GoalDodgeProjectiles) _cachedGoal;
            goal.setProjectileList(mod.getEntityTracker().getProjectiles());
        }
        return super.onTick(mod);
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof DodgeProjectilesTask) {
            DodgeProjectilesTask task = (DodgeProjectilesTask) obj;
            //if (task._mob.getPos().squaredDistanceTo(_mob.getPos()) > 0.5) return false;
            if (Math.abs(task._distanceHorizontal - _distanceHorizontal) > 1) return false;
            if (Math.abs(task._distanceVertical - _distanceVertical) > 1) return false;
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Dodge arrows at " + _distanceHorizontal + " blocks away";
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalDodgeProjectiles(_distanceHorizontal, _distanceVertical);
    }
}
