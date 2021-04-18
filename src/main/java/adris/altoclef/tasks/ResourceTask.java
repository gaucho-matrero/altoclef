package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.chest.PickupFromChestTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.Slot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public abstract class ResourceTask extends Task {

    protected final ItemTarget[] _itemTargets;

    private final PickupDroppedItemTask _pickupTask;
    private BlockPos _currentChest;

    public ResourceTask(ItemTarget[] itemTargets) {
        _itemTargets = itemTargets;
        _pickupTask = new PickupDroppedItemTask(_itemTargets, true);
    }
    public ResourceTask(ItemTarget target) {
        this(new ItemTarget[] {target});
    }
    public ResourceTask(Item item, int targetCount) {
        this(new ItemTarget(item, targetCount));
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        //Debug.logInternal("FOOF: " + Arrays.toString(Util.toArray(ItemTarget.class, _itemTargets)));
        return mod.getInventoryTracker().targetMet(_itemTargets);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getConfigState().push();
        mod.getConfigState().addProtectedItems(ItemTarget.getMatches(_itemTargets));//removeThrowawayItems(_itemTargets);
        onResourceStart(mod);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (!shouldAvoidPickingUp(mod)) {
            // Check if items are on the floor. If so, pick em up.
            if (mod.getEntityTracker().itemDropped(_itemTargets)) {

                // If we're picking up a pickaxe (we can't go far underground or mine much)
                if (PickupDroppedItemTask.isIsGettingPickaxeFirst(mod)) {
                    if (_pickupTask.isCollectingPickaxeForThis()) {
                        // Our pickup task is the one collecting the pickaxe, keep it going.
                        return _pickupTask;
                    }
                    // Only get items that are CLOSE to us.
                    ItemEntity closest = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), _itemTargets);
                    if (!closest.isInRange(mod.getPlayer(), 10)) {
                        return onResourceTick(mod);
                    }
                }

                double range = mod.getModSettings().getResourcePickupRange();
                ItemEntity closest = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), _itemTargets);
                if (range < 0 || closest.isInRange(mod.getPlayer(), range) || (_pickupTask.isActive() && !_pickupTask.isFinished(mod)) ) {
                    return _pickupTask;
                }
            }
        }

        // Check for chests and grab resources from them.
        if (_currentChest != null) {
            ContainerTracker.ChestData data = mod.getContainerTracker().getChestMap().getCachedChestData(_currentChest);
            if (data == null) {
                _currentChest = null;
            } else {
                if (!data.hasItem(_itemTargets)) {
                    _currentChest = null;
                } else {
                    // We have a current chest, grab from it.
                    return new PickupFromChestTask(_currentChest, _itemTargets);
                }
            }
        }
        List<BlockPos> chestsWithItem = mod.getContainerTracker().getChestMap().getBlocksWithItem(_itemTargets);
        Debug.logInternal("CHESTS: " + Util.arrayToString(Util.toArray(BlockPos.class, chestsWithItem)));
        if (!chestsWithItem.isEmpty()) {
            BlockPos closest = Util.minItem(chestsWithItem, (left, right) -> (int) (right.getSquaredDistance(mod.getPlayer().getPos(), false) - left.getSquaredDistance(mod.getPlayer().getPos(), false)));
            if (closest.isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceChestLocateRange())) {
                _currentChest = closest;
                return new PickupFromChestTask(_currentChest, _itemTargets);
            }
        }

        return onResourceTick(mod);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getConfigState().pop();
        onResourceStop(mod, interruptTask);
    }

    @Override
    protected boolean isEqual(Task other) {
        // Same target items
        if (other instanceof ResourceTask) {
            ResourceTask t = (ResourceTask) other;
            if (!isEqualResource(t)) return false;
            return Util.arraysEqual(t._itemTargets, _itemTargets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        StringBuilder result = new StringBuilder();
        result.append(toDebugStringName()).append(": [");
        int c = 0;
        for (ItemTarget target : _itemTargets) {
            result.append(target.toString());
            if (++c != _itemTargets.length) {
                result.append(", ");
            }
        }
        result.append("]");
        return result.toString();
    }

    // Returns: Whether this failed.
    public static boolean ensureInventoryFree(AltoClef mod) {
        if (mod.getInventoryTracker().isInventoryFull()) {
            // Throw away!
            Slot toThrow = mod.getInventoryTracker().getGarbageSlot();
            if (toThrow != null) {
                // Equip then throw
                //Debug.logMessage("Throwing away from inventory slot " + toThrow.getInventorySlot());
                mod.getInventoryTracker().throwSlot(toThrow);
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    protected abstract boolean shouldAvoidPickingUp(AltoClef mod);

    protected abstract void onResourceStart(AltoClef mod);

    protected abstract Task onResourceTick(AltoClef mod);

    protected abstract void onResourceStop(AltoClef mod, Task interruptTask);

    protected abstract boolean isEqualResource(ResourceTask obj);

    protected abstract String toDebugStringName();
}
