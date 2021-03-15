package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.KillEntitiesTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;

import java.util.Arrays;

public class KillAndLooktTask extends ResourceTask {

    private final Class _toKill;

    public KillAndLooktTask(Class toKill, ItemTarget ...itemTargets) {
        super(itemTargets.clone());
        _toKill = toKill;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (!mod.getEntityTracker().entityFound(_toKill)) {
            setDebugState("Searching for mob...");
            return new TimeoutWanderTask(999999);
        }
        // We found the mob!
        return new KillEntitiesTask(_toKill);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        if (obj instanceof KillAndLooktTask) {
            KillAndLooktTask task = (KillAndLooktTask) obj;
            return task._toKill.equals(_toKill);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect items from " + _toKill.toGenericString();
    }
}
