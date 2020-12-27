package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalRunAwayFromCreepers;
import net.minecraft.entity.mob.MobEntity;

public class RunAwayFromCreepersTask extends Task {

    private final double _distanceToRun;

    public RunAwayFromCreepersTask(double distance) {
        _distanceToRun = distance;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalRunAwayFromCreepers(mod, _distanceToRun));
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof RunAwayFromCreepersTask) {
            RunAwayFromCreepersTask task = (RunAwayFromCreepersTask) obj;
            //if (task._mob.getPos().squaredDistanceTo(_mob.getPos()) > 0.5) return false;
            if (Math.abs(task._distanceToRun - _distanceToRun) > 1) return false;
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Run " + _distanceToRun + " blocks away from creepers";
    }
}
