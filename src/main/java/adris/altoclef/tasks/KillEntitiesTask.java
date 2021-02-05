package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

public class KillEntitiesTask extends AbstractKillEntityTask {

    private Class _class;

    public KillEntitiesTask(Class toFind) {
        _class = toFind;
    }

    @Override
    protected LivingEntity getEntityTarget(AltoClef mod) {
        Entity closest = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), _class);

        if (closest instanceof LivingEntity) return (LivingEntity) closest;
        return null;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof KillEntitiesTask) {
            KillEntitiesTask task = (KillEntitiesTask) obj;
            return task._class.equals(_class);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Kill entities of type " + _class.toGenericString();
    }
}
