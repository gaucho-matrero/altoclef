package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.LivingEntity;

public class KillEntityTask extends AbstractKillEntityTask {

    private final LivingEntity _target;

    public KillEntityTask(LivingEntity entity) {
        _target = entity;
    }

    @Override
    protected LivingEntity getEntityTarget(AltoClef mod) {
        return _target;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof KillEntityTask) {
            return ((KillEntityTask) obj)._target.equals(_target);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Killing " + _target.getDisplayName().asString();
    }
}
