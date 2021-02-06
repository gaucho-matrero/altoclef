package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalFollowEntity;
import net.minecraft.entity.Entity;

public class GetToEntityTask extends Task {

    private final Entity _entity;

    public GetToEntityTask(Entity entity) {
        _entity = entity;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalFollowEntity(_entity));
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
