package adris.altoclef.tasksystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.Input;
import adris.altoclef.util.csharpisbetter.Stopwatch;
import org.lwjgl.glfw.GLFW;

// A task chain that runs a user defined task at the same priority.
// This basically replaces our old Task Runner.
public class UserTaskChain extends TaskChain {

    private Task _mainTask = null;

    private final boolean _stopOnFinish = true;

    private final Stopwatch _taskStopwatch = new Stopwatch();

    public UserTaskChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTick(AltoClef mod) {
        if (!isActive()) return;

        if (_mainTask.isActive() && Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) && Input.isKeyPressed(GLFW.GLFW_KEY_K)) {
            Debug.logMessage("(stop shortcut sent)");
            stop(mod);
            onFinish(mod);
            return;
        }

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
        _taskStopwatch.begin();
        _mainTask = task;
    }

    private void onFinish(AltoClef mod) {
        mod.getTaskRunner().disable();
        Debug.logMessage("User task FINISHED. Took %.2f seconds.", _taskStopwatch.time());
        _mainTask = null;
    }
}
