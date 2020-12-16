package adris.altoclef.tasksystem;

import adris.altoclef.AltoClef;

public abstract class TaskChain {

    public TaskChain(TaskRunner runner) {
        runner.addTaskChain(this);
    }

    public void tick(AltoClef mod) {
        onTick(mod);
    }

    public abstract void stop(AltoClef mod);

    protected abstract void onTick(AltoClef mod);

    public abstract float getPriority();

    public abstract boolean isActive();

}
