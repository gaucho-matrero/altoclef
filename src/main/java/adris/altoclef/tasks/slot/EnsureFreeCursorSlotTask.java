package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

public class EnsureFreeCursorSlotTask extends Task {

    @Override
    protected void onStart(AltoClef mod) {
         // YEET
    }

    @Override
    protected Task onTick(AltoClef mod) {


        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();


        if (!cursor.isEmpty()) {
        Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
            if (moveTo.isPresent()) {
                setDebugState("Moving cursor stack back");
                return new ClickSlotTask(moveTo.get());
        }
        if (ItemHelper.canThrowAwayStack(mod, cursor)) {
                setDebugState("Incompatible cursor stack, throwing");
                return new ThrowCursorTask();
            } else {
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
                if (garbage.isPresent()) {
                    // Pick up garbage so we throw it out next frame
                    setDebugState("Picking up garbage");
                    return new ClickSlotTask(garbage.get());
                } else {
                    setDebugState("STUCK! Everything's protected.");
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof EnsureFreeCursorSlotTask;
    }


    // And filling this in will make it look ok in the task tree
    @Override
    protected String toDebugString() {
        return "Breaking the cursor slot";
    }
}
