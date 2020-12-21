package adris.altoclef.tasksystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

// A task chain that runs a user defined task at the same priority.
// This basically replaces our old Task Runner.
public class UserTaskChain extends TaskChain {

    private Task _mainTask = null;

    private boolean _stopOnFinish = true;

    public UserTaskChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTick(AltoClef mod) {
        if (!isActive()) return;

        if ((_stopOnFinish && _mainTask.isFinished(mod)) || _mainTask.failed()) {
            onFinish(mod);
        } else {
            _mainTask.tick(mod, this);
        }
    }

    public void onStop(AltoClef mod) {
        if (isActive()) {
            _mainTask.stop(mod);
        }
    }


    @Override
    public float getPriority() {
        return 50;
    }

    @Override
    public boolean isActive() {
        return _mainTask != null;
    }

    @Override
    public String getName() {
        return "User Tasks";
    }

    public void runTask(AltoClef mod, Task task) {
        Debug.logMessage("User Task Set: " + task.toString());
        mod.getTaskRunner().enable();
        _mainTask = task;
    }

    private void onFinish(AltoClef mod) {
        mod.getTaskRunner().disable();
        Debug.logMessage("User task FINISHED");
        _mainTask = null;
    }
}
