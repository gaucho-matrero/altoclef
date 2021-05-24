package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.util.math.BlockPos;


public class GetToXZTask extends CustomBaritoneGoalTask {
    private final int x;
    private final int z;
    
    public GetToXZTask(int x, int z) {
        this.x = x;
        this.z = z;
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GetToXZTask) {
            GetToXZTask task = (GetToXZTask) obj;
            return task.x == x && task.z == z;
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Getting to (" + x + "," + z + ")";
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        BlockPos cur = mod.getPlayer().getBlockPos();
        return (cur.getX() == x && cur.getZ() == z);
    }
    
    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalXZ(x, z);
    }
}
