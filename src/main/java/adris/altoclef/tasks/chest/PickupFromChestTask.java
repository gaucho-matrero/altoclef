package adris.altoclef.tasks.chest;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.Item;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.math.BlockPos;


public class PickupFromChestTask extends AbstractDoInChestTask {
    private final ItemTarget[] targets;
    private final Timer actionTimer = new Timer(0);
    private final BlockPos targetChest;
    
    public PickupFromChestTask(BlockPos targetChest, ItemTarget... targets) {
        super(targetChest);
        this.targets = targets;
        this.targetChest = targetChest;
    }
    
    @Override
    protected Task doToOpenChestTask(AltoClef mod, GenericContainerScreenHandler handler) {
        actionTimer.setInterval(mod.getModSettings().getContainerItemMoveDelay());
        if (actionTimer.elapsed()) {
            actionTimer.reset();
            
            ContainerTracker.ChestData data = mod.getContainerTracker().getChestMap().getCachedChestData(targetChest);
            if (data == null) {
                Debug.logWarning("Failed to find valid chest at " + targetChest + ", hopefully this is handled up the chain!!!");
                return null;
            }
            for (ItemTarget target : targets) {
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
            return Util.arraysEqual(task.targets, targets);
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Picking up from chest: " + Util.arrayToString(targets);
    }
}
