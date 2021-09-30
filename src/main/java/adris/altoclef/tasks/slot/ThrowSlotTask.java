package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ThrowSlotTask extends Task {

    private final Slot _slot;

    private boolean _clickFirstFlag;

    public ThrowSlotTask(Slot slot) {
        _slot = slot;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _clickFirstFlag = true;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_clickFirstFlag) {
            _clickFirstFlag = false;
            return new ClickSlotTask(_slot);
        }
        return new ClickSlotTask(new PlayerSlot(-999), 0, SlotActionType.PICKUP);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof ThrowSlotTask task) {
            return task._slot.equals(_slot);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Throwing " + _slot;
    }
}
