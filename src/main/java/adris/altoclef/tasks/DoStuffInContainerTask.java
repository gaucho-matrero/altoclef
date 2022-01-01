package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasks.slot.ClickSlotTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.BaritoneHelper;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Optional;


@SuppressWarnings("ConstantConditions")
/*
 * Interacts with a container, obtaining and placing one if none were found nearby.
 */
public abstract class DoStuffInContainerTask extends Task {

    private final ItemTarget _containerTarget;
    private final Block[] _containerBlocks;

    private final PlaceBlockNearbyTask _placeTask;
    // If we decided on placing, force place for at least 10 seconds
    private final TimerGame _placeForceTimer = new TimerGame(10);
    // If we just placed something, stop placing and try going to the nearest container.
    private final TimerGame _justPlacedTimer = new TimerGame(3);
    private BlockPos _cachedContainerPosition = null;
    private Task _openTableTask;

    public DoStuffInContainerTask(Block[] containerBlocks, ItemTarget containerTarget) {
        _containerBlocks = containerBlocks;
        _containerTarget = containerTarget;

        _placeTask = new PlaceBlockNearbyTask(_containerBlocks);
    }

    public DoStuffInContainerTask(Block containerBlock, ItemTarget containerTarget) {
        this(new Block[]{containerBlock}, containerTarget);
    }

    @Override
    protected void onStart(AltoClef mod) {
        if (_openTableTask == null) {
            _openTableTask = new DoToClosestBlockTask(InteractWithBlockTask::new, _containerBlocks);
        }

        mod.getBlockTracker().trackBlock(_containerBlocks);

        // Protect container since we might place it.
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(ItemHelper.blocksToItems(_containerBlocks));
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // If we're placing, keep on placing.
        if (mod.getItemStorage().hasItem(ItemHelper.blocksToItems(_containerBlocks)) && _placeTask.isActive() && !_placeTask.isFinished(mod)) {
            setDebugState("Placing container");
            return _placeTask;
        }

        if (isContainerOpen(mod)) {
            return containerSubTask(mod);
        }

        // infinity if such a container does not exist.
        double costToWalk = Double.POSITIVE_INFINITY;

        BlockPos nearest;

        Vec3d currentPos = mod.getPlayer().getPos();
        BlockPos override = overrideContainerPosition(mod);

        if (override != null && mod.getBlockTracker().blockIsValid(override, _containerBlocks)) {
            // We have an override so go there instead.
            nearest = override;
        } else {
            // Track nearest container
            nearest = mod.getBlockTracker().getNearestTracking(currentPos, _containerBlocks);
        }
        if (nearest == null) {
            // If all else fails, try using our placed task
            nearest = _placeTask.getPlaced();
            if (nearest != null && !mod.getBlockTracker().blockIsValid(nearest, _containerBlocks)) {
                nearest = null;
            }
        }
        if (nearest != null) {
            costToWalk = BaritoneHelper.calculateGenericHeuristic(currentPos, WorldHelper.toVec3d(nearest));
        }

        // Make a new container if going to the container is a pretty bad cost.
        // Also keep on making the container if we're stuck in some
        if (costToWalk > getCostToMakeNew(mod)) {
            _placeForceTimer.reset();
        }
        if (nearest == null || (!_placeForceTimer.elapsed() && _justPlacedTimer.elapsed())) {
            // It's cheaper to make a new one, or our only option.

            // We're no longer going to our previous container.
            _cachedContainerPosition = null;

            // Get if we don't have...
            if (!mod.getItemStorage().hasItem(_containerTarget)) {
                setDebugState("Getting container item");
                return TaskCatalogue.getItemTask(_containerTarget);
            }

            setDebugState("Placing container...");

            _justPlacedTimer.reset();
            // Now place!
            return _placeTask;
        }

        _cachedContainerPosition = nearest;

        // Walk to it and open it
        setDebugState("Walking to container... " + nearest);

        // Wait for food
        if (mod.getFoodChain().isTryingToEat()) {
            return null;
        }

        if (nearest != null) {
            if (!StorageHelper.getItemStackInCursorSlot().isEmpty()) {
                setDebugState("Clearing cursor slot (otherwise this causes BIG problems)");
                Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(StorageHelper.getItemStackInCursorSlot(), false);
                if (toMoveTo.isEmpty()) {
                    return new EnsureFreeInventorySlotTask();
                } else {
                    return new ClickSlotTask(toMoveTo.get());
                }
            }
            return _openTableTask;
        }
        return null;
        //return new GetToBlockTask(nearest, true);
    }

    // Virtual
    protected BlockPos overrideContainerPosition(AltoClef mod) {
        return null;
    }

    protected BlockPos getTargetContainerPosition() {
        return _cachedContainerPosition;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(_containerBlocks);
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DoStuffInContainerTask task) {
            if (!Arrays.equals(task._containerBlocks, _containerBlocks)) return false;
            if (!task._containerTarget.equals(_containerTarget)) return false;
            return isSubTaskEqual(task);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Doing stuff in " + _containerTarget + " container";
    }

    protected abstract boolean isSubTaskEqual(DoStuffInContainerTask other);

    protected abstract boolean isContainerOpen(AltoClef mod);

    protected abstract Task containerSubTask(AltoClef mod);

    protected abstract double getCostToMakeNew(AltoClef mod);
}
