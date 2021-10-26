package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.Input;

/**
 * Will move around randomly while holding shift
 * Used to escape weird situations where baritone doesn't work.
 */
public class SafeRandomShimmyTask extends Task {

    private final TimerGame _lookTimer;

    public SafeRandomShimmyTask(float randomLookInterval) {
        _lookTimer  = new TimerGame(randomLookInterval);
    }
    public SafeRandomShimmyTask() {
        this(10);
    }

    @Override
    protected void onStart(AltoClef mod) {
        _lookTimer.reset();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (_lookTimer.elapsed()) {
            _lookTimer.reset();
            LookHelper.randomOrientation(mod);
        }

        mod.getInputControls().hold(Input.SNEAK);
        mod.getInputControls().hold(Input.MOVE_FORWARD);
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getInputControls().release(Input.SNEAK);
        mod.getInputControls().release(Input.MOVE_FORWARD);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof SafeRandomShimmyTask;
    }

    @Override
    protected String toDebugString() {
        return "Shimmying";
    }
}
