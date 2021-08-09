package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.slots.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ClickSlotTask extends Task {

    private final Slot _slot;
    private final int _mouseButton;
    private final SlotActionType type;

    public ClickSlotTask(Slot slot, int mouseButton, SlotActionType type) {
        _slot = slot;
        _mouseButton = mouseButton;
        this.type = type;
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
