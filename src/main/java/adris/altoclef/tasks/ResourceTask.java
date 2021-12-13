package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.chest.PickupFromChestTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.util.*;
import adris.altoclef.util.helpers.StlHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The parent for all "collect an item" tasks.
 *
 * If the target item is on the ground or in a chest, will grab from those sources first.
 */
public abstract class ResourceTask extends Task {

    protected final ItemTarget[] _itemTargets;

    private final PickupDroppedItemTask _pickupTask;
    private BlockPos _currentChest;

    // Extra resource parameters
    private Block[] _mineIfPresent = null;
    private boolean _forceDimension = false;
    private Dimension _targetDimension;

    private BlockPos _mineLastClosest = null;

    public ResourceTask(ItemTarget[] itemTargets) {
        _itemTargets = itemTargets;
        _pickupTask = new PickupDroppedItemTask(_itemTargets, true);
    }

    public ResourceTask(ItemTarget target) {
        this(new ItemTarget[]{target});
    }

    public ResourceTask(Item item, int targetCount) {
        this(new ItemTarget(item, targetCount));
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        //Debug.logInternal("FOOF: " + Arrays.toString(Util.toArray(ItemTarget.class, _itemTargets)));
        return mod.getInventoryTracker().targetsMet(_itemTargets);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(ItemTarget.getMatches(_itemTargets));//removeThrowawayItems(_itemTargets);
        if (_mineIfPresent != null) {
            mod.getBlockTracker().trackBlock(_mineIfPresent);
        }
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
                if (range < 0 || closest.isInRange(mod.getPlayer(), range) || (_pickupTask.isActive() && !_pickupTask.isFinished(mod))) {
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
                } else if (!Blacklist.isBlacklisted(_currentChest)) {
                    // We have a current chest, grab from it.
                    return new PickupFromChestTask(_currentChest, _itemTargets);
                }
            }
        }
        List<BlockPos> chestsWithItem = mod.getContainerTracker().getChestMap().getBlocksWithItem(_itemTargets);
        if (!chestsWithItem.isEmpty()) {
            BlockPos closest = chestsWithItem.stream().min(StlHelper.compareValues(block -> block.getSquaredDistance(mod.getPlayer().getPos(), false))).get();
            if (closest.isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceChestLocateRange())) {
                if (!Blacklist.isBlacklisted(_currentChest)) {
                    _currentChest = closest;
                    return new PickupFromChestTask(_currentChest, _itemTargets);
                }
            }
        }

        /* TODO
        Is it considered in the ResourceTask that the bot could walk away and therefore unload a
        tracked mining candidate while going for a side task like getting the right mining tool?
        Because if not, it would stop doing both and starts exploration instead.
        This could be a cause for a potential soft lock since it would be possible to switch between
        those two states by retracking the same target block when the exploration task goes for the
        same direction the mining candidate was already found earlier.

        Taco: It should have some kind of force condition and mine that block as long as it's valid
        (which will only return false if the block is in the chunk and is not the same block as it was before)
        */
        if (_mineIfPresent != null) {
            ArrayList<Block> satisfiedReqs = new ArrayList<>(Arrays.asList(_mineIfPresent));
            satisfiedReqs.removeIf(block -> !mod.getInventoryTracker().miningRequirementMet(MiningRequirement.getMinimumRequirementForBlock(block)));
            if (!satisfiedReqs.isEmpty()) {
                if (mod.getBlockTracker().anyFound(satisfiedReqs.toArray(Block[]::new))) {
                    BlockPos closest = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), _mineIfPresent);
                    //TODO: could this make the bot get stuck between two resources?
                    if (Utils.isSet(closest)) {
                        _mineLastClosest = closest;
                    }

                    if (Utils.isSet(_mineLastClosest)) {
                        final boolean isInChunk = mod.getChunkTracker().isChunkLoaded(_mineLastClosest);
                        final boolean isMined = !mod.getBlockTracker().blockIsValid(_mineLastClosest, _mineIfPresent);

                        if (isInChunk && isMined || !Blacklist.isBlacklisted(_mineLastClosest)) {
                            //TODO so if we set it null here, shouldn't we ensure the next mining target to stick with executing MineAndCollectTask?
                            //Answer no because if that would be the case then this would count for isBlacklisted too
                            _mineLastClosest = null;
                        }

                        if (Utils.isSet(_mineLastClosest)) {
                            return new MineAndCollectTask(_itemTargets, _mineIfPresent, MiningRequirement.HAND);
                        }
                    }
                }
            }
        }
        /*
        // We may just mine if a block is found.
        if (_mineIfPresent != null) {
            ArrayList<Block> satisfiedReqs = new ArrayList<>(Arrays.asList(_mineIfPresent));
            satisfiedReqs.removeIf(block -> !mod.getInventoryTracker().miningRequirementMet(MiningRequirement.getMinimumRequirementForBlock(block)));
            if (!satisfiedReqs.isEmpty()) {
                if (mod.getBlockTracker().anyFound(satisfiedReqs.toArray(Block[]::new))) {
                    BlockPos closest = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), _mineIfPresent);
                    if (closest != null && closest.isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceMineRange())) {
                        _mineLastClosest = closest;
                    }
                    if (_mineLastClosest != null && !Blacklist.isBlacklisted(_mineLastClosest)) {
                        if (_mineLastClosest.isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceMineRange() * 1.5 + 20)) {
                            return new MineAndCollectTask(_itemTargets, _mineIfPresent, MiningRequirement.HAND);
                        }
                    }
                }
            }
        }*/

        return onResourceTick(mod);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
        if (_mineIfPresent != null) {
            mod.getBlockTracker().stopTracking(_mineIfPresent);
        }
        onResourceStop(mod, interruptTask);
    }

    @Override
    protected boolean isEqual(Task other) {
        // Same target items
        if (other instanceof ResourceTask t) {
            if (!isEqualResource(t)) return false;
            return Arrays.equals(t._itemTargets, _itemTargets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        StringBuilder result = new StringBuilder();
        result.append(toDebugStringName()).append(": [");
        int c = 0;
        for (ItemTarget target : _itemTargets) {
            result.append(target != null? target.toString() : "(null)");
            if (++c != _itemTargets.length) {
                result.append(", ");
            }
        }
        result.append("]");
        return result.toString();
    }

    protected boolean isInWrongDimension(AltoClef mod) {
        if (_forceDimension) {
            return mod.getCurrentDimension() != _targetDimension;
        }
        return false;
    }

    protected Task getToCorrectDimensionTask(AltoClef mod) {
        return new DefaultGoToDimensionTask(_targetDimension);
    }

    public ResourceTask mineIfPresent(Block[] toMine) {
        _mineIfPresent = toMine;
        return this;
    }

    public ResourceTask forceDimension(Dimension dimension) {
        _forceDimension = true;
        _targetDimension = dimension;
        return this;
    }

    protected abstract boolean shouldAvoidPickingUp(AltoClef mod);

    protected abstract void onResourceStart(AltoClef mod);

    protected abstract Task onResourceTick(AltoClef mod);

    protected abstract void onResourceStop(AltoClef mod, Task interruptTask);

    protected abstract boolean isEqualResource(ResourceTask other);

    protected abstract String toDebugStringName();

    public ItemTarget[] getItemTargets() {
        return _itemTargets;
    }
}
