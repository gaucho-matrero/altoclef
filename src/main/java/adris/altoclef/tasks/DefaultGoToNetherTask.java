package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;

/**
 * Some generic tasks require us to go to the nether.
 *
 * The user should be able to specify how this should be done in settings
 * (ex, craft a new portal from scratch or check particular portal areas first or highway or whatever)
 *
 */
public class DefaultGoToNetherTask extends Task {
    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        setDebugState("NOT IMPLEMENTED YET!");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof DefaultGoToNetherTask;
    }

    @Override
    protected String toDebugString() {
        return "Going to nether (default)";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return true; // TODO: When implemented, remove this obviously.
    }
}
