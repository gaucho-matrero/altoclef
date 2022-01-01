package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;

import java.util.Optional;

public class EnsureFreeInventorySlotTask extends Task {
    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (cursorStack.isEmpty() || !ItemHelper.canThrowAwayStack(mod, cursorStack)) {
            Optional<Slot> toThrow = StorageHelper.getGarbageSlot(mod);
            if (toThrow.isPresent()) {
                setDebugState("Finding garbage");
                return new ClickSlotTask(toThrow.get());
            } else {
                setDebugState("NO THROWAWAYABLE!");
                return null;
            }
        } else {
            setDebugState("Throwing cursor");
            return new ThrowCursorTask();
        }
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof EnsureFreeInventorySlotTask;
    }

    @Override
    protected String toDebugString() {
        return "Ensuring inventory is free";
    }
}
