package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.TaskCatalogue;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 *
 * TO TEST:
 *  - Do stuff in container works ALL THE WAY up until opening the container
 *      - Crafting table placed down, goes to it
 *      - Crafting table far away, goes to it
 *      - Crafting table non existant, makes one
 *      - Crafting table SUPER far away, makes one
 * TO DO NEXT:
 *  - Craft recipe in the table just like with CraftInInventoryTask
 *  - Test crafting a wooden pickaxe
 *  - Test crafting a stone pickaxe
 *  - Test crafting 2 stone pickaxes. Make sure we __delay__ the crafting table stuff until we get all resources.
 */

public abstract class DoStuffInContainerTask extends Task {

    private String _containerCatalogueName;
    private Block _containerBlock;

    private PlaceBlockNearbyTask _placeTask;

    public DoStuffInContainerTask(Block containerBlock, String containerCatalogueName) {
        _containerBlock = containerBlock;
        _containerCatalogueName = containerCatalogueName;

        _placeTask = new PlaceBlockNearbyTask(_containerBlock);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(_containerBlock);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (isContainerOpen(mod)) {
            return containerSubTask(mod);
        }

        // infinity if such a container does not exist.
        double costToWalk = Double.POSITIVE_INFINITY;

        // TODO: hmmmmm... this shouldn't be necessary.
        mod.getBlockTracker().trackBlock(_containerBlock);
        Vec3d currentPos = mod.getPlayer().getPos();
        BlockPos nearest = mod.getBlockTracker().getNearestTracking(currentPos);
        if (nearest != null) {
            costToWalk = BaritoneHelper.calculateGenericHeuristic(currentPos, Util.toVec3d(nearest));
        } else {
            //Debug.logWarning("FAILED TO FIND CONTAINER!");
        }

        if (costToWalk > getCostToMakeNew(mod)) {

            // Get if we don't have...
            if (!mod.getInventoryTracker().hasItem(_containerCatalogueName)) {
                return TaskCatalogue.getItemTask(_containerCatalogueName, 1);
            }

            // Now place!
            return _placeTask;
        }

        // Walk to it and open it
        return new GetToBlockTask(nearest, true);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(_containerBlock);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof DoStuffInContainerTask) {
            DoStuffInContainerTask other = (DoStuffInContainerTask) obj;
            //Debug.logInternal("A");
            if (!other._containerBlock.is(_containerBlock)) return false;
            //Debug.logInternal("B");
            if (!other._containerCatalogueName.equals(_containerCatalogueName)) return false;
            //Debug.logInternal("C");
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
