package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.chest.PickupFromChestTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public abstract class ResourceTask extends Task {
    protected final ItemTarget[] itemTargets;
    private final PickupDroppedItemTask pickupTask;
    private BlockPos currentChest;
    // Extra resource parameters
    private Block[] mineIfPresent;
    private boolean forceDimension;
    private Dimension targetDimension;
    private BlockPos mineLastClosest;
    
    public ResourceTask(ItemTarget[] itemTargets) {
        this.itemTargets = itemTargets;
        pickupTask = new PickupDroppedItemTask(this.itemTargets, true);
    }
    
    public ResourceTask(ItemTarget target) {
        this(new ItemTarget[]{ target });
    }
    
    public ResourceTask(Item item, int targetCount) {
        this(new ItemTarget(item, targetCount));
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
    
    @Override
    public boolean isFinished(AltoClef mod) {
        //Debug.logInternal("FOOF: " + Arrays.toString(Util.toArray(ItemTarget.class, _itemTargets)));
        return mod.getInventoryTracker().targetMet(itemTargets);
    }
    
    @Override
    protected void onStart(AltoClef mod) {
        mod.getConfigState().push();
        mod.getConfigState().addProtectedItems(ItemTarget.getMatches(itemTargets));//removeThrowawayItems(_itemTargets);
        if (mineIfPresent != null) {
            mod.getBlockTracker().trackBlock(mineIfPresent);
        }
        onResourceStart(mod);
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        
        if (!shouldAvoidPickingUp(mod)) {
            // Check if items are on the floor. If so, pick em up.
            if (mod.getEntityTracker().itemDropped(itemTargets)) {
                
                // If we're picking up a pickaxe (we can't go far underground or mine much)
                if (PickupDroppedItemTask.isIsGettingPickaxeFirst(mod)) {
                    if (pickupTask.isCollectingPickaxeForThis()) {
                        // Our pickup task is the one collecting the pickaxe, keep it going.
                        return pickupTask;
                    }
                    // Only get items that are CLOSE to us.
                    ItemEntity closest = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemTargets);
                    if (!closest.isInRange(mod.getPlayer(), 10)) {
                        return onResourceTick(mod);
                    }
                }
                
                double range = mod.getModSettings().getResourcePickupRange();
                ItemEntity closest = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemTargets);
                if (range < 0 || closest.isInRange(mod.getPlayer(), range) || (pickupTask.isActive() && !pickupTask.isFinished(mod))) {
                    return pickupTask;
                }
            }
        }
        
        // Check for chests and grab resources from them.
        if (currentChest != null) {
            ContainerTracker.ChestData data = mod.getContainerTracker().getChestMap().getCachedChestData(currentChest);
            if (data == null) {
                currentChest = null;
            } else {
                if (!data.hasItem(itemTargets)) {
                    currentChest = null;
                } else {
                    // We have a current chest, grab from it.
                    return new PickupFromChestTask(currentChest, itemTargets);
                }
            }
        }
        List<BlockPos> chestsWithItem = mod.getContainerTracker().getChestMap().getBlocksWithItem(itemTargets);
        if (!chestsWithItem.isEmpty()) {
            BlockPos closest = Util.minItem(chestsWithItem, (left, right) -> (int) (right.getSquaredDistance(mod.getPlayer().getPos(),
                                                                                                             false) -
                                                                                    left.getSquaredDistance(mod.getPlayer().getPos(),
                                                                                                            false)));
            if (closest.isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceChestLocateRange())) {
                currentChest = closest;
                return new PickupFromChestTask(currentChest, itemTargets);
            }
        }
        
        // We may just mine if a block is found.
        if (mineIfPresent != null) {
            ArrayList<Block> satisfiedReqs = new ArrayList<>(Arrays.asList(mineIfPresent));
            satisfiedReqs.removeIf(
                    block -> !mod.getInventoryTracker().miningRequirementMet(MiningRequirement.getMinimumRequirementForBlock(block)));
            if (!satisfiedReqs.isEmpty()) {
                if (mod.getBlockTracker().anyFound(Util.toArray(Block.class, satisfiedReqs))) {
                    BlockPos closest = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), mineIfPresent);
                    if (closest.isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceMineRange())) {
                        mineLastClosest = closest;
                    }
                    if (mineLastClosest != null) {
                        if (mineLastClosest.isWithinDistance(mod.getPlayer().getPos(),
                                                             mod.getModSettings().getResourceMineRange() * 1.5 + 20)) {
                            return new MineAndCollectTask(itemTargets, mineIfPresent, MiningRequirement.HAND);
                        }
                    }
                }
            }
        }
        
        return onResourceTick(mod);
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getConfigState().pop();
        if (mineIfPresent != null) {
            mod.getBlockTracker().stopTracking(mineIfPresent);
        }
        onResourceStop(mod, interruptTask);
    }
    
    @Override
    protected boolean isEqual(Task other) {
        // Same target items
        if (other instanceof ResourceTask) {
            ResourceTask t = (ResourceTask) other;
            if (!isEqualResource(t)) return false;
            return Util.arraysEqual(t.itemTargets, itemTargets);
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        StringBuilder result = new StringBuilder();
        result.append(toDebugStringName()).append(": [");
        int c = 0;
        for (ItemTarget target : itemTargets) {
            result.append(target.toString());
            if (++c != itemTargets.length) {
                result.append(", ");
            }
        }
        result.append("]");
        return result.toString();
    }
    
    protected boolean isInWrongDimension(AltoClef mod) {
        if (forceDimension) {
            return mod.getCurrentDimension() != targetDimension;
        }
        return false;
    }
    
    protected Task getToCorrectDimensionTask(AltoClef mod) {
        return new DefaultGoToDimensionTask(targetDimension);
    }
    
    public ResourceTask mineIfPresent(Block[] toMine) {
        mineIfPresent = toMine;
        return this;
    }
    
    public ResourceTask forceDimension(Dimension dimension) {
        forceDimension = true;
        targetDimension = dimension;
        return this;
    }
    
    protected abstract boolean shouldAvoidPickingUp(AltoClef mod);
    
    protected abstract void onResourceStart(AltoClef mod);
    
    protected abstract Task onResourceTick(AltoClef mod);
    
    protected abstract void onResourceStop(AltoClef mod, Task interruptTask);
    
    protected abstract boolean isEqualResource(ResourceTask obj);
    
    protected abstract String toDebugStringName();
}
