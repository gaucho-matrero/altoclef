package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class CarveThenCollectTask extends ResourceTask {

    private final ItemTarget _target;
    private final Block[] _targetBlocks;
    private final ItemTarget _toCarve;
    private final Block[] _toCarveBlocks;
    private final ItemTarget _carveWith;

    public CarveThenCollectTask(ItemTarget target, Block[] targetBlocks, ItemTarget toCarve, Block[] toCarveBlocks, ItemTarget carveWith) {
        super(target);
        _target = target;
        _targetBlocks = targetBlocks;
        _toCarve = toCarve;
        _toCarveBlocks = toCarveBlocks;
        _carveWith = carveWith;
    }

    public CarveThenCollectTask(Item target, int targetCount, Block targetBlock, Item toCarve, Block toCarveBlock, Item carveWith) {
        this(new ItemTarget(target, targetCount), new Block[]{targetBlock}, new ItemTarget(toCarve, targetCount), new Block[]{toCarveBlock}, new ItemTarget(carveWith, 1));
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(_targetBlocks);
        mod.getBlockTracker().trackBlock(_toCarveBlocks);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // If target block spotted, break it!
        // If toCarve block spotted, carve it!
        // neededCarve = (neededTarget - currentTarget)
        // If neededCarve > currentCarveItems:
        //      collect carve items!
        // ELSE:
        //      Place carved items down

        // If our target block is placed, break it!
        if (mod.getBlockTracker().anyFound(_targetBlocks)) {
            setDebugState("Breaking carved/target block");
            return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(), DestroyBlockTask::new, pos -> mod.getBlockTracker().getNearestTracking(pos, _targetBlocks), _targetBlocks);
        }
        // Collect our "carve with" item (can be shears, axe, whatever)
        if (!mod.getInventoryTracker().targetMet(_carveWith)) {
            setDebugState("Collect our carve tool");
            return TaskCatalogue.getItemTask(_carveWith);
        }
        // If our carve block is spotted, carve it.
        if (mod.getBlockTracker().anyFound(_toCarveBlocks)) {
            setDebugState("Carving block");
            return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(), blockPos -> new InteractWithBlockTask(_carveWith, blockPos, false), pos -> mod.getBlockTracker().getNearestTracking(pos, _toCarveBlocks), _toCarveBlocks);
        }
        // Collect carve blocks if we don't have enough, or place them down if we do.
        int neededCarveItems = _target.getTargetCount() - mod.getInventoryTracker().getItemCount(_target);
        int currentCarveItems = mod.getInventoryTracker().getItemCount(_toCarve);
        if (neededCarveItems > currentCarveItems) {
            setDebugState("Collecting more blocks to carve");
            return TaskCatalogue.getItemTask(_toCarve);
        } else {
            setDebugState("Placing blocks to carve down");
            return new PlaceBlockNearbyTask(_toCarveBlocks);
        }
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(_targetBlocks);
        mod.getBlockTracker().stopTracking(_toCarveBlocks);
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        if (obj instanceof CarveThenCollectTask) {
            CarveThenCollectTask task = (CarveThenCollectTask) obj;
            return (task._target.equals(_target) && task._toCarve.equals(_toCarve) && Util.arraysEqual(task._targetBlocks, _targetBlocks) && Util.arraysEqual(task._toCarveBlocks, _toCarveBlocks));
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Getting after carving: " + _target;
    }
}
