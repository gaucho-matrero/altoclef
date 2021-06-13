package adris.altoclef.tasks.chest;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.ChestSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.Item;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class StoreInChestTask extends AbstractDoInChestTask {

    private final ItemTarget[] _targets;
    private final TimerGame _actionTimer = new TimerGame(0);

    private final BlockPos _targetChest;

    public StoreInChestTask(BlockPos targetChest, ItemTarget... targets) {
        super(targetChest);
        _targets = targets;
        _targetChest = targetChest;
    }

    @Override
    protected Task doToOpenChestTask(AltoClef mod, GenericContainerScreenHandler handler) {
        _actionTimer.setInterval(mod.getModSettings().getContainerItemMoveDelay());
        if (_actionTimer.elapsed()) {
            _actionTimer.reset();

            ContainerTracker.ChestData data = mod.getContainerTracker().getChestMap().getCachedChestData(_targetChest);
            if (data == null) {
                Debug.logWarning("Failed to find valid chest at " + _targetChest + ", hopefully this is handled up the chain!!!");
                return null;
            }
            if (data.isFull()) {
                Debug.logWarning("Chest is full at " + _targetChest + ", can't store here. Hopefully this is handled up the chain!!!");
                return null;
            }
            if (data.isBig() != (handler.getRows() == 6)) {
                Debug.logWarning("Chest was tracked as invalid size. Will wait for recache.");
                return null;
            }
            for (ItemTarget target : _targets) {
                int has = 0;
                for (Item match : target.getMatches()) {
                    has += data.getItemCount(match);
                }
                if (has < target.getTargetCount()) {
                    // We need to store items!
                    // Get empty spot in chest
                    int start = 0;
                    int end = data.isBig() ? 53 : 26;
                    int emptySlot = -1;
                    for (int slot = start; slot <= end; ++slot) {
                        if (!handler.getSlot(slot).hasStack() || handler.getSlot(slot).getStack().isEmpty()) {
                            emptySlot = slot;
                            break;
                        }
                    }
                    if (emptySlot == -1) {
                        Debug.logWarning("No empty slot found for chest that should be empty. Hopefully will be re-cached properly.");
                        return null;
                    }
                    // Move at most (target.targetCount - has) of any one item to empty slot
                    int maxToMove = target.getTargetCount() - has;
                    List<Integer> availableSlots = mod.getInventoryTracker().getInventorySlotsWithItem(target.getMatches());
                    if (availableSlots.size() != 0) {
                        Slot slotFrom = Slot.getFromInventory(availableSlots.get(0));
                        int countInSlot = mod.getInventoryTracker().getItemStackInSlot(slotFrom).getCount();
                        if (countInSlot < maxToMove) {
                            maxToMove = countInSlot;
                        }
                        Slot slotTo = new ChestSlot(emptySlot, data.isBig());
                        mod.getInventoryTracker().moveItems(slotFrom, slotTo, maxToMove);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected boolean isSubEqual(AbstractDoInChestTask obj) {
        if (obj instanceof StoreInChestTask) {
            StoreInChestTask task = (StoreInChestTask) obj;
            return Util.arraysEqual(task._targets, _targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Storing in chest: " + Util.arrayToString(_targets);
    }
}
