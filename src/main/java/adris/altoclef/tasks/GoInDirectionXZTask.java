package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalDirectionXZ;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalAxis;
import baritone.api.pathing.goals.GoalStrictDirection;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.util.math.Vec3d;

public class GoInDirectionXZTask extends CustomBaritoneGoalTask {

    private final Vec3d _origin;
    private final Vec3d _delta;
    private final double _sidePenalty;

    public GoInDirectionXZTask(Vec3d origin, Vec3d delta, double sidePenalty) {
        _origin = origin;
        _delta = delta;
        _sidePenalty = sidePenalty;
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalDirectionXZ(_origin, _delta, _sidePenalty);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GoInDirectionXZTask) {
            GoInDirectionXZTask task = (GoInDirectionXZTask) obj;
            return (closeEnough(task._origin, _origin) && closeEnough(task._delta, _delta));
        }
        return false;
    }

    private static boolean closeEnough(Vec3d a, Vec3d b) {
        return a.squaredDistanceTo(b) < 0.001;
    }

    @Override
    protected String toDebugString() {
        return "Going in direction: <" + _origin.x + "," + _origin.z + "> direction: <" + _delta.x + "," + _delta.z + ">";
    }
}
