package adris.altoclef.tasksystem.chains;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.csharpisbetter.Stopwatch;


public abstract class SingleTaskChain extends TaskChain {
    private final Stopwatch taskStopwatch = new Stopwatch();
    protected Task mainTask;
    private boolean interrupted;

    protected SingleTaskChain(TaskRunner runner) {
        super(runner);
    }

    protected void onStop(AltoClef mod) {
        if (isActive() && mainTask != null) {
            mainTask.stop(mod);
            mainTask = null;
        }
    }

    @Override
    public void onInterrupt(AltoClef mod, TaskChain other) {
        Debug.logInternal("Chain Interrupted: " + this + " by " + other.toString());
        // Stop our task. When we're started up again, let our task know we need to run.
        interrupted = true;
        if (mainTask != null && mainTask.isActive()) {
            mainTask.stop(mod);
        }
    }

    @Override
    protected void onTick(AltoClef mod) {
        if (!isActive()) return;

        if (interrupted) {
            interrupted = false;
            if (mainTask != null) {
                mainTask.reset();
            }
        }

        if (mainTask != null) {
            if ((mainTask.isFinished(mod)) || mainTask.stopped()) {
                onTaskFinish(mod);
            } else {
                mainTask.tick(mod, this);
            }
        }
    }

    @Override
    public boolean isActive() {
        return mainTask != null;
    }

    public void setTask(Task task) {
        if (mainTask == null || !mainTask.equals(task)) {
            mainTask = task;
            if (task != null) task.reset();
        }
    }

    protected abstract void onTaskFinish(AltoClef mod);

    public Task getCurrentTask() {
        return mainTask;
    }
}
