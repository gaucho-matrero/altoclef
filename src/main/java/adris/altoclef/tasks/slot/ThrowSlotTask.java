package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.slots.Slot;

public class ThrowSlotTask extends Task {

    private final Slot _slot;

    public ThrowSlotTask(Slot slot) {
        _slot = slot;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        return false;
    }

    @Override
    protected String toDebugString() {
        return null;
    }
}
