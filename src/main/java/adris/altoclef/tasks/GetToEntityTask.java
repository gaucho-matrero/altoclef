package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalFollowEntity;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.entity.Entity;

public class GetToEntityTask extends Task implements ITaskRequiresGrounded {

    private final Entity _entity;

    private final MovementProgressChecker _progress = new MovementProgressChecker(5, 0.1, 5, 0.001, 2);
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(20);

    public GetToEntityTask(Entity entity) {
        _entity = entity;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        _wanderTask.resetWander();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            _progress.reset();
            setDebugState("Failed to get to target, wandering for a bit.");
            return _wanderTask;
        }

        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalFollowEntity(_entity));
        }

        if (!_progress.check(mod)) {
            return _wanderTask;
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GetToEntityTask) {
            GetToEntityTask task = (GetToEntityTask) obj;
            return task._entity.equals(_entity);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Approach entity " + _entity.getDisplayName().asString();
    }
}
