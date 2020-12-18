package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Call this when the place you're currently at is bad for some reason and you just wanna get away.
 */

public class TimeoutWanderTask extends Task {

    private float _distanceToWander;

    private Vec3d _origin;

    public TimeoutWanderTask(float distanceToWander) {
        _distanceToWander = distanceToWander;
    }

    @Override
    protected void onStart(AltoClef mod) {
        BlockPos origin = mod.getPlayer().getBlockPos();
        _origin = mod.getPlayer().getPos();
        mod.getClientBaritone().getExploreProcess().explore(origin.getX(), origin.getZ());
    }

    @Override
    protected Task onTick(AltoClef mod) {
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getExploreProcess().onLostControl();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        double sqDist = mod.getPlayer().getPos().squaredDistanceTo(_origin);
        return sqDist > _distanceToWander;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof TimeoutWanderTask) {
            TimeoutWanderTask other = (TimeoutWanderTask) obj;
            return Math.abs(other._distanceToWander - _distanceToWander) < 0.5f;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Wander for " + _distanceToWander + " blocks";
    }
}
