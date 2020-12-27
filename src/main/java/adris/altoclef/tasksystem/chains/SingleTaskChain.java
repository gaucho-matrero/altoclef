package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.csharpisbetter.Stopwatch;

public abstract class SingleTaskChain extends TaskChain {

    protected Task _mainTask = null;
    private final Stopwatch _taskStopwatch = new Stopwatch();

    public SingleTaskChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTick(AltoClef mod) {
        if (!isActive()) return;

        if ((_mainTask.isFinished(mod)) || _mainTask.failed()) {
            onTaskFinish(mod);
        } else {
            _mainTask.tick(mod, this);
        }
    }

    protected void onStop(AltoClef mod) {
        if (isActive() && _mainTask != null) {
            _mainTask.stop(mod);
        }
    }

    public void setTask(Task task) {
        _mainTask = task;
    }


    @Override
    public boolean isActive() {
        return _mainTask != null;
    }

    protected abstract void onTaskFinish(AltoClef mod);
}
