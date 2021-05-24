package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.Util;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalRunAway;
import net.minecraft.util.math.BlockPos;


public class RunAwayFromPositionTask extends CustomBaritoneGoalTask {
    private final BlockPos[] dangerBlocks;
    private final double distance;
    
    public RunAwayFromPositionTask(double distance, BlockPos... toRunAwayFrom) {
        this.distance = distance;
        dangerBlocks = toRunAwayFrom;
    }
    
    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalRunAway(distance, dangerBlocks);
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof RunAwayFromPositionTask) {
            return Util.arraysEqual(((RunAwayFromPositionTask) obj).dangerBlocks, dangerBlocks);
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Running away from " + Util.arrayToString(dangerBlocks);
    }
}
