package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

public class KillEntityTask extends AbstractKillEntityTask {

    private final Entity _target;

    public KillEntityTask(Entity entity) {
        _target = entity;
    }

    @Override
    protected Entity getEntityTarget(AltoClef mod) {
        return _target;
    }

    @Override
    protected boolean isSubEqual(AbstractDoToEntityTask obj) {
        if (obj instanceof KillEntityTask) {
            return ((KillEntityTask) obj)._target.equals(_target);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Killing " + _target.getEntityName();
    }
}
