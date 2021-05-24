package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


/**
 * TO TEST: - Do stuff in container works ALL THE WAY up until opening the container - Crafting table placed down, goes to it - Crafting
 * table far away, goes to it - Crafting table non existant, makes one - Crafting table SUPER far away, makes one TO DO NEXT: - Craft recipe
 * in the table just like with CraftInInventoryTask - Test crafting a wooden pickaxe - Test crafting a stone pickaxe - Test crafting 2 stone
 * pickaxes. Make sure we __delay__ the crafting table stuff until we get all resources.
 */

@SuppressWarnings("ConstantConditions")
public abstract class DoStuffInContainerTask extends Task {
    private final String containerCatalogueName;
    private final Block containerBlock;
    private final PlaceBlockNearbyTask placeTask;
    // If we decided on placing, force place for at least 10 seconds
    private final Timer placeForceTimer = new Timer(10);
    // If we just placed something, stop placing and try going to the nearest container.
    private final Timer justPlacedTimer = new Timer(3);
    private BlockPos cachedContainerPosition;
    private Task openTableTask;
    
    public DoStuffInContainerTask(Block containerBlock, String containerCatalogueName) {
        this.containerBlock = containerBlock;
        this.containerCatalogueName = containerCatalogueName;
        
        placeTask = new PlaceBlockNearbyTask(this.containerBlock);
    }
    
    @Override
    protected void onStart(AltoClef mod) {
        if (openTableTask == null) {
            openTableTask = new DoToClosestBlockTask(mod, () -> mod.getPlayer().getPos(), blockpos -> new GetToBlockTask(blockpos, true),
                                                     containerBlock);
        }
        
        mod.getBlockTracker().trackBlock(containerBlock);
        
        // Protect container since we might place it.
        mod.getConfigState().push();
        mod.getConfigState().addProtectedItems(containerBlock.asItem());
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        
        // If we're placing, keep on placing.
        if (mod.getInventoryTracker().hasItem(containerBlock.asItem()) && placeTask.isActive() && !placeTask.isFinished(mod)) {
            setDebugState("Placing container");
            return placeTask;
        }
        
        if (isContainerOpen(mod)) {
            return containerSubTask(mod);
        }
        
        // infinity if such a container does not exist.
        double costToWalk = Double.POSITIVE_INFINITY;
        
        BlockPos nearest;
        
        Vec3d currentPos = mod.getPlayer().getPos();
        BlockPos override = overrideContainerPosition(mod);
        
        if (override != null && mod.getBlockTracker().blockIsValid(override, containerBlock)) {
            // We have an override so go there instead.
            nearest = override;
        } else {
            // Track nearest container
            nearest = mod.getBlockTracker().getNearestTracking(currentPos, containerBlock);
        }
        if (nearest == null) {
            // If all else fails, try using our placed task
            nearest = placeTask.getPlaced();
            if (nearest != null && !mod.getBlockTracker().blockIsValid(nearest, containerBlock)) {
                nearest = null;
            }
        }
        if (nearest != null) {
            costToWalk = BaritoneHelper.calculateGenericHeuristic(currentPos, Util.toVec3d(nearest));
        }
        
        // Make a new container if going to the container is a pretty bad cost.
        // Also keep on making the container if we're stuck in some
        if (costToWalk > getCostToMakeNew(mod)) {
            placeForceTimer.reset();
        }
        if (nearest == null || (!placeForceTimer.elapsed() && justPlacedTimer.elapsed())) {
            // It's cheaper to make a new one, or our only option.
            
            // We're no longer going to our previous container.
            cachedContainerPosition = null;
            
            // Get if we don't have...
            if (!mod.getInventoryTracker().hasItem(containerCatalogueName)) {
                //Debug.logInternal("GRABBING " + _containerCatalogueName);
                //Debug.logInternal("Cause " + costToWalk + " > " + getCostToMakeNew(mod));
                //Debug.logInternal("(from " + currentPos + " to " + Util.toVec3d(nearest));
                setDebugState("Getting container item");
                return TaskCatalogue.getItemTask(containerCatalogueName, 1);
            }
            
            setDebugState("Placing container... Oof.");
            
            justPlacedTimer.reset();
            // Now place!
            return placeTask;
        }
        
        cachedContainerPosition = nearest;
        
        // Walk to it and open it
        setDebugState("Walking to container... " + nearest);
        
        // Wait for food
        if (mod.getFoodTaskChain().isTryingToEat()) {
            return null;
        }
        
        if (nearest != null) {
            return openTableTask;
        }
        return null;
        //return new GetToBlockTask(nearest, true);
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(containerBlock);
        mod.getConfigState().pop();
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof DoStuffInContainerTask) {
            DoStuffInContainerTask other = (DoStuffInContainerTask) obj;
            if (!other.containerBlock.is(containerBlock)) return false;
            if (!other.containerCatalogueName.equals(containerCatalogueName)) return false;
            return isSubTaskEqual(other);
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Doing stuff in " + containerCatalogueName + " container";
    }
    
    // Virtual
    protected BlockPos overrideContainerPosition(AltoClef mod) {
        return null;
    }
    
    protected BlockPos getTargetContainerPosition() {
        return cachedContainerPosition;
    }
    
    protected abstract boolean isSubTaskEqual(DoStuffInContainerTask obj);
    
    protected abstract boolean isContainerOpen(AltoClef mod);
    
    protected abstract Task containerSubTask(AltoClef mod);
    
    protected abstract double getCostToMakeNew(AltoClef mod);
}
