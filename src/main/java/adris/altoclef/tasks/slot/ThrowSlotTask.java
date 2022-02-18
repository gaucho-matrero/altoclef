package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;

public class ThrowSlotTask extends Task {

    private final Slot _slot;

    private boolean _clickFirstFlag;

    public ThrowSlotTask(Slot slot) {
        _slot = slot;
    }
    public ThrowSlotTask() {
        this(null);
    }

    @Override
    protected void onStart(AltoClef mod) {
        // Click if we're NOT throwing the cursor slot.
        _clickFirstFlag = _slot != null && !Slot.isCursor(_slot);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_slot != null && (_clickFirstFlag || mod.getInventoryTracker().getItemStackInCursorSlot().isEmpty())) {
            _clickFirstFlag = false;
            return new ClickSlotTask(_slot);
        }
        return new ClickSlotTask(new PlayerSlot(-999));
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof ThrowSlotTask task) {
            if (task._slot == null) {
                return _slot == null;
            }
            return task._slot.equals(_slot);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Throwing " + _slot;
    }
}
