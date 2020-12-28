package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalRunAwayFromCreepers;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.mob.MobEntity;

public abstract class CustomBaritoneGoalTask extends Task {

    protected Goal _cachedGoal = null;

    public CustomBaritoneGoalTask() {
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
            _cachedGoal = newGoal(mod);
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(_cachedGoal);
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    protected abstract Goal newGoal(AltoClef mod);
}
