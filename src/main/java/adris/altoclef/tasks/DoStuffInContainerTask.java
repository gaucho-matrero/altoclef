package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * TO TEST:
 * - Do stuff in container works ALL THE WAY up until opening the container
 * - Crafting table placed down, goes to it
 * - Crafting table far away, goes to it
 * - Crafting table non existant, makes one
 * - Crafting table SUPER far away, makes one
 * TO DO NEXT:
 * - Craft recipe in the table just like with CraftInInventoryTask
 * - Test crafting a wooden pickaxe
 * - Test crafting a stone pickaxe
 * - Test crafting 2 stone pickaxes. Make sure we __delay__ the crafting table stuff until we get all resources.
 */

@SuppressWarnings("ConstantConditions")
public abstract class DoStuffInContainerTask extends Task {

    private final String _containerCatalogueName;
    private final Block[] _containerBlocks;

    private final PlaceBlockNearbyTask _placeTask;
    // If we decided on placing, force place for at least 10 seconds
    private final TimerGame _placeForceTimer = new TimerGame(10);
    // If we just placed something, stop placing and try going to the nearest container.
    private final TimerGame _justPlacedTimer = new TimerGame(3);
    private BlockPos _cachedContainerPosition = null;
    private Task _openTableTask;

    public DoStuffInContainerTask(Block[] containerBlocks, String containerCatalogueName) {
        _containerBlocks = containerBlocks;
        _containerCatalogueName = containerCatalogueName;

        _placeTask = new PlaceBlockNearbyTask(_containerBlocks);
    }

    public DoStuffInContainerTask(Block containerBlock, String containerCatalogueName) {
        this(new Block[]{containerBlock}, containerCatalogueName);
    }

    @Override
    protected void onStart(AltoClef mod) {
        if (_openTableTask == null) {
            _openTableTask = new DoToClosestBlockTask(mod, () -> mod.getPlayer().getPos(), InteractWithBlockTask::new, _containerBlocks);
        }

        mod.getBlockTracker().trackBlock(_containerBlocks);

        // Protect container since we might place it.
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(Util.blocksToItems(_containerBlocks));
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // If we're placing, keep on placing.
        if (mod.getInventoryTracker().hasItem(Util.blocksToItems(_containerBlocks)) && _placeTask.isActive() && !_placeTask.isFinished(mod)) {
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
            costToWalk = BaritoneHelper.calculateGenericHeuristic(currentPos, Util.toVec3d(nearest));
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
            if (!mod.getInventoryTracker().hasItem(_containerCatalogueName)) {
                //Debug.logInternal("GRABBING " + _containerCatalogueName);
                //Debug.logInternal("Cause " + costToWalk + " > " + getCostToMakeNew(mod));
                //Debug.logInternal("(from " + currentPos + " to " + Util.toVec3d(nearest));
                setDebugState("Getting container item");
                return TaskCatalogue.getItemTask(_containerCatalogueName, 1);
            }

            setDebugState("Placing container... Oof.");

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
    protected boolean isEqual(Task obj) {
        if (obj instanceof DoStuffInContainerTask) {
            DoStuffInContainerTask other = (DoStuffInContainerTask) obj;
            if (!Util.arraysEqual(other._containerBlocks, _containerBlocks)) return false;
            if (!other._containerCatalogueName.equals(_containerCatalogueName)) return false;
            return isSubTaskEqual(other);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Doing stuff in " + _containerCatalogueName + " container";
    }

    protected abstract boolean isSubTaskEqual(DoStuffInContainerTask obj);

    protected abstract boolean isContainerOpen(AltoClef mod);

    protected abstract Task containerSubTask(AltoClef mod);

    protected abstract double getCostToMakeNew(AltoClef mod);
}
