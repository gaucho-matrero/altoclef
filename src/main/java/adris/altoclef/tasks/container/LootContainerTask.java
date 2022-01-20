package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.slot.ClickSlotTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasks.speedrun.BeatMinecraft2Task;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;


public class LootContainerTask extends Task {
    private final BlockPos chest;
    private final Item target;
    private Task _pickupTask;

    public LootContainerTask(BlockPos chestPos, Item item) {
        chest = chestPos;
        target = item;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(target);
        _pickupTask = new PickupFromContainerTask(chest, new ItemTarget(target, 1));
    }

    @Override
    protected Task onTick(AltoClef mod) {
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (!cursor.isEmpty()) {
            Optional<Slot> toFit = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
            if (toFit.isPresent()) {
                setDebugState("Putting cursor in inventory");
                return new ClickSlotTask(toFit.get());
            } else {
                setDebugState("Ensuring space");
                return new EnsureFreeInventorySlotTask();
            }
        }
        setDebugState("Looting a container for 1 " + target.toString());
        return _pickupTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task task) {
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task task) {
        return task instanceof LootContainerTask;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        Debug.logMessage("Isfinished: " + _pickupTask.isFinished(mod));
        Debug.logMessage("Isactive: " + _pickupTask.isActive());
        Debug.logMessage("Isempty: " + StorageHelper.getItemStackInCursorSlot().isEmpty());
        return _pickupTask.isFinished(mod) && !_pickupTask.isActive() && StorageHelper.getItemStackInCursorSlot().isEmpty();
    }

    @Override
    protected String toDebugString() {
        return "Looting a container";
    }
}
