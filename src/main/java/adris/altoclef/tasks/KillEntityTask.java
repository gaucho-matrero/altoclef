package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import net.minecraft.entity.Entity;

public class KillEntityTask extends AbstractKillEntityTask {

    private final Entity _target;

    public KillEntityTask(Entity entity) {
        _target = entity;
    }

    public KillEntityTask(Entity entity, double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        super(maintainDistance, combatGuardLowerRange, combatGuardLowerFieldRadius);
        _target = entity;
    }

    @Override
    protected Entity getEntityTarget(AltoClef mod) {
        return _target;
    }

    @Override
    protected boolean isSubEqual(AbstractDoToEntityTask other) {
        if (other instanceof KillEntityTask task) {
            return task._target.equals(_target);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Killing " + _target.getEntityName();
    }
}
