package adris.altoclef.tasks.chest;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.Item;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.math.BlockPos;

public class PickupFromChestTask extends AbstractDoInChestTask {

    private final ItemTarget[] _targets;
    private final TimerGame _actionTimer = new TimerGame(0);

    private final BlockPos _targetChest;

    public PickupFromChestTask(BlockPos targetChest, ItemTarget... targets) {
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
            for (ItemTarget target : _targets) {
                if (!mod.getInventoryTracker().targetMet(target)) {
                    for (Item mightMove : target.getMatches()) {
                        // Pick up all items that might fit our criteria.
                        if (data.hasItem(mightMove)) {
                            if (!ResourceTask.ensureInventoryFree(mod)) {
                                Debug.logWarning("FAILED TO FREE INVENTORY for chest pickup. This is bad.");
                            } else {
                                //int maxMove = target.targetCount - mod.getInventoryTracker().getItemCount(target);
                                Slot itemSlot = data.getItemSlotsWithItem(mightMove).get(0);
                                mod.getInventoryTracker().grabItem(itemSlot);
                            }
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected boolean isSubEqual(AbstractDoInChestTask obj) {
        if (obj instanceof PickupFromChestTask) {
            PickupFromChestTask task = (PickupFromChestTask) obj;
            return Util.arraysEqual(task._targets, _targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Picking up from chest: " + Util.arrayToString(_targets);
    }
}
