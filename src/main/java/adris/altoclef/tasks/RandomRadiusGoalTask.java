package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.movement.CustomBaritoneGoalTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasksystem.Task;
import baritone.api.pathing.goals.Goal;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public class RandomRadiusGoalTask extends Task {
    final BlockPos end;
    final Random rand;
    final Task goal;

    public RandomRadiusGoalTask(final BlockPos start, final int r) {
        this.rand = new Random();
        final double phi = rand.nextInt(360) + rand.nextDouble();
        final double radius = rand.nextInt(r) + rand.nextDouble();
        final int x = (int) Math.round(radius * Math.sin(phi));
        final int z = (int) Math.round(radius * Math.cos(phi));

        this.end = new BlockPos(start.getX() + x, start.getY(), start.getZ() + z);
        this.goal = new GetToXZTask(this.end.getX(), this.end.getZ());
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        return goal;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        goal.stop(mod);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof RandomRadiusGoalTask task) {
            return task.end.getX() == end.getX() && task.end.getY() == end.getY() && task.end.getZ() == end.getZ();
        }
        return false;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return goal.isFinished(mod);
    }

    @Override
    protected String toDebugString() {
        return "RandomRadiusGoalTask";
    }
}
