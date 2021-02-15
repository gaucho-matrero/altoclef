package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.Input;
import adris.altoclef.util.csharpisbetter.Action;
import adris.altoclef.util.csharpisbetter.Stopwatch;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

// A task chain that runs a user defined task at the same priority.
// This basically replaces our old Task Runner.
public class UserTaskChain extends SingleTaskChain {

    private final Stopwatch _taskStopwatch = new Stopwatch();

    public final Action<String> onTaskFinish = new Action<>();

    private Consumer _currentOnFinish = null;

    public UserTaskChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTick(AltoClef mod) {

        // Pause if we're not loaded into a world.
        if (!mod.inGame()) return;

        // Stop shortcut
        if (_mainTask != null && _mainTask.isActive() && Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) && Input.isKeyPressed(GLFW.GLFW_KEY_K)) {
            Debug.logMessage("(stop shortcut sent)");
            cancel(mod);
            return;
        }
        super.onTick(mod);
    }

    public void cancel(AltoClef mod) {
        if (_mainTask != null && _mainTask.isActive()) {
            stop(mod);
            onTaskFinish(mod);
        }
    }

    @Override
    public float getPriority(AltoClef mod) {
        return 50;
    }

    @Override
    public String getName() {
        return "User Tasks";
    }

    public void runTask(AltoClef mod, Task task, Consumer onFinish) {
        _currentOnFinish = onFinish;
        Debug.logMessage("User Task Set: " + task.toString());
        mod.getTaskRunner().enable();
        _taskStopwatch.begin();
        setTask(task);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        mod.getTaskRunner().disable();
        Debug.logMessage("User task FINISHED. Took %.2f seconds.", _taskStopwatch.time());
        if (_currentOnFinish != null) {
            //noinspection unchecked
            _currentOnFinish.accept(null);
        }
        _currentOnFinish = null;
        onTaskFinish.invoke(String.format("Took %.2f seconds", _taskStopwatch.time()));
        _mainTask = null;
    }
}
