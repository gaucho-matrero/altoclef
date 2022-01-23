package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.slot.ClickSlotTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerType;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;

import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;


public class LootContainerTask extends Task {
    private Task _pickupTask;
    private boolean _wasProtected = true;

    public final BlockPos chest;
    public final Item target;

    public LootContainerTask(BlockPos chestPos, Item item) {
        chest = chestPos;
        target = item;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        if (!mod.getBehaviour().isProtected(target)) {
            mod.getBehaviour().addProtectedItems(target);
            _wasProtected = false;
        }
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if(!ContainerType.screenHandlerMatches(ContainerType.CHEST)) {
            setDebugState("Interact with container");
            return new InteractWithBlockTask(chest);
        }
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
        Optional<Slot> optimal = getAMatchingSlot(mod);
        if (optimal.isEmpty()) {
            return null;
        }
        setDebugState("Looting a container for all of their " + target.toString());
        return new ClickSlotTask(optimal.get());
    }

    @Override
    protected void onStop(AltoClef mod, Task task) {
        if (!_wasProtected) {
            mod.getBehaviour().removeProtectedItems(target);
        }
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof LootContainerTask && target == ((LootContainerTask) other).target;
    }

    private Optional<Slot> getAMatchingSlot(AltoClef mod) {
        List<Slot> slots = mod.getItemStorage().getSlotsWithItemContainer(target);
        if (slots.isEmpty()) return Optional.empty();
        else return Optional.ofNullable(slots.get(0));
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return getAMatchingSlot(mod).isEmpty();
    }

    @Override
    protected String toDebugString() {
        return "Looting a container";
    }
}
