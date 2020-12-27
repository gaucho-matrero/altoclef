package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.Input;
import adris.altoclef.util.csharpisbetter.Stopwatch;
import org.lwjgl.glfw.GLFW;

// A task chain that runs a user defined task at the same priority.
// This basically replaces our old Task Runner.
public class UserTaskChain extends SingleTaskChain {

    private final Stopwatch _taskStopwatch = new Stopwatch();

    public UserTaskChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTick(AltoClef mod) {
        // Stop shortcut
        if (_mainTask != null && _mainTask.isActive() && Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) && Input.isKeyPressed(GLFW.GLFW_KEY_K)) {
            Debug.logMessage("(stop shortcut sent)");
            stop(mod);
            onTaskFinish(mod);
            return;
        }
        super.onTick(mod);
    }

    @Override
    public float getPriority(AltoClef mod) {
        return 50;
    }

    @Override
    public String getName() {
        return "User Tasks";
    }

    public void runTask(AltoClef mod, Task task) {
        Debug.logMessage("User Task Set: " + task.toString());
        mod.getTaskRunner().enable();
        _taskStopwatch.begin();
        setTask(task);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        mod.getTaskRunner().disable();
        Debug.logMessage("User task FINISHED. Took %.2f seconds.", _taskStopwatch.time());
        _mainTask = null;
    }
}
