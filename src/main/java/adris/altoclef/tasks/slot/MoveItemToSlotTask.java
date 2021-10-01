package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StlHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MoveItemToSlotTask extends Task {

    private final ItemTarget _toMove;
    private final Slot _destination;

    public MoveItemToSlotTask(ItemTarget toMove, Slot destination) {
        _toMove = toMove;
        _destination = destination;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getSlotHandler().canDoSlotAction()) {
            // Rough plan
            // - If empty slot or wrong item
            //      Find best matching item (smallest count over target, or largest count if none over)
            //      Click on it (one turn)
            // - If held slot has < items than target count
            //      Left click on destination slot (one turn)
            // - If held slot has > items than target count
            //      Right click on destination slot (one turn)
            ItemStack currentHeld = mod.getInventoryTracker().getItemStackInCursorSlot();
            ItemStack atTarget = mod.getInventoryTracker().getItemStackInSlot(_destination);

            // Items that CAN be moved to that slot.
            Item[] validItems = Arrays.stream(_toMove.getMatches()).filter(item -> mod.getInventoryTracker().getItemCount(item) >= _toMove.getTargetCount()).collect(Collectors.toList()).toArray(Item[]::new);

            if (currentHeld.isEmpty() || !Arrays.asList(validItems).contains(currentHeld.getItem())) {
                // Wrong item held, replace with best match.
                Slot bestPickup = getBestSlotToPickUp(mod, validItems);
                if (bestPickup == null) {
                    Debug.logError("Called MoveItemToSlotTask when item/not enough item is available! valid items: " + StlHelper.toString(validItems, Item::getTranslationKey));
                    return null;
                }
                return new ClickSlotTask(bestPickup);
            }

            int currentlyPlaced = Arrays.asList(validItems).contains(atTarget.getItem()) ? atTarget.getCount() : 0;
            if (currentHeld.getCount() + currentlyPlaced  < _toMove.getTargetCount()) {
                // Just place all of 'em
                return new ClickSlotTask(_destination);
            } else {
                // Place one at a time.
                return new ClickSlotTask(_destination, 1);
            }
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    public boolean isFinished(AltoClef mod) {
        ItemStack atDestination = mod.getInventoryTracker().getItemStackInSlot(_destination);
        return (_toMove.matches(atDestination.getItem()) && atDestination.getCount() >= _toMove.getTargetCount());
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof MoveItemToSlotTask) {
            MoveItemToSlotTask task = (MoveItemToSlotTask) obj;
            return task._toMove.equals(_toMove) && task._destination.equals(_destination);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Moving " + _toMove + " to " + _destination;
    }

    private Slot    getBestSlotToPickUp(AltoClef mod, Item[] validItems) {
        Slot bestMatch = null;
        for (Slot slot : mod.getInventoryTracker().getInventorySlotsWithItem(validItems)) {
            if (Slot.isCursor(slot)) continue;
            if (bestMatch == null) {
                bestMatch = slot;
                continue;
            }
            int countBest = mod.getInventoryTracker().getItemStackInSlot(bestMatch).getCount();
            int countCheck = mod.getInventoryTracker().getItemStackInSlot(slot).getCount();
            if (   (countBest < _toMove.getTargetCount() && countCheck > countBest)
                    || (countBest >= _toMove.getTargetCount() && countCheck >= _toMove.getTargetCount() && countCheck > countBest) ) {
                // If we don't have enough, go for largest
                // If we have too much, go for smallest over the limit.
                bestMatch = slot;
            }
        }
        return bestMatch;
    }
}
