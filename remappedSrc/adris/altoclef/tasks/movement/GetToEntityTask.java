package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalFollowEntity;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.entity.Entity;

public class GetToEntityTask extends Task implements ITaskRequiresGrounded {

    private final Entity _entity;

    private final double _closeEnoughDistance;

    private final MovementProgressChecker _progress = new MovementProgressChecker(5, 0.1, 5, 0.001, 2);
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(10);

    public GetToEntityTask(Entity entity, double closeEnoughDistance) {
        _entity = entity;
        _closeEnoughDistance = closeEnoughDistance;
    }

    public GetToEntityTask(Entity entity) {
        this(entity, 1);
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
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalFollowEntity(_entity, _closeEnoughDistance));
        }

        if (mod.getPlayer().isInRange(_entity, _closeEnoughDistance)) {
            _progress.reset();
        }

        if (!_progress.check(mod)) {
            return _wanderTask;
        }

        setDebugState("Going to entity");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof GetToEntityTask task) {
            return task._entity.equals(_entity) && Math.abs(task._closeEnoughDistance - _closeEnoughDistance) < 0.1;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Approach entity " + _entity.getType().getTranslationKey();
    }
}
