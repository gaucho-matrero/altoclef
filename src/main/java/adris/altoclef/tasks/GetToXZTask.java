package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.util.math.BlockPos;

public class GetToXZTask extends CustomBaritoneGoalTask {

    private final int _x, _z;

    public GetToXZTask(int x, int z) {
        _x = x;
        _z = z;
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalXZ(_x, _z);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GetToXZTask) {
            GetToXZTask task = (GetToXZTask) obj;
            return task._x == _x && task._z == _z;
        }
        return false;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        BlockPos cur = mod.getPlayer().getBlockPos();
        return (cur.getX() == _x && cur.getZ() == _z);
    }

    @Override
    protected String toDebugString() {
        return "Getting to (" + _x + "," + _z + ")";
    }
}
