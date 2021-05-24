package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalDirectionXZ;
import baritone.api.pathing.goals.Goal;
import net.minecraft.util.math.Vec3d;


public class GoInDirectionXZTask extends CustomBaritoneGoalTask {
    private final Vec3d origin;
    private final Vec3d delta;
    private final double sidePenalty;
    
    public GoInDirectionXZTask(Vec3d origin, Vec3d delta, double sidePenalty) {
        this.origin = origin;
        this.delta = delta;
        this.sidePenalty = sidePenalty;
    }
    
    private static boolean closeEnough(Vec3d a, Vec3d b) {
        return a.squaredDistanceTo(b) < 0.001;
    }
    
    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalDirectionXZ(origin, delta, sidePenalty);
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GoInDirectionXZTask) {
            GoInDirectionXZTask task = (GoInDirectionXZTask) obj;
            return (closeEnough(task.origin, origin) && closeEnough(task.delta, delta));
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Going in direction: <" + origin.x + "," + origin.z + "> direction: <" + delta.x + "," + delta.z + ">";
    }
}
