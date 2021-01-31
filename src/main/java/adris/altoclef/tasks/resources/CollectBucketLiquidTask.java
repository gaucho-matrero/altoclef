package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractItemWithBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;

public class CollectBucketLiquidTask extends ResourceTask {

    private int _count;

    private Item _target;
    private Block _toCollect;

    private String _liquidName;

    public CollectBucketLiquidTask(String liquidName, Item filledBucket, int targetCount, Block toCollect) {
        super(filledBucket, targetCount);
        _liquidName = liquidName;
        _target = filledBucket;
        _count = targetCount;
        _toCollect = toCollect;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        // Track fluids
        mod.getConfigState().push();
        mod.getConfigState().setRayTracingFluidHandling(RaycastContext.FluidHandling.SOURCE_ONLY);
        mod.getConfigState().setMineProcSearchAnywhereFlag(true); // If we don't set this, lava will never be found.
        mod.getBlockTracker().trackBlock(_toCollect);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        // Get buckets if we need em
        int bucketsNeeded = _count - mod.getInventoryTracker().getItemCount(Items.BUCKET) - mod.getInventoryTracker().getItemCount(_target);
        if (bucketsNeeded > 0) {
            return TaskCatalogue.getItemTask("bucket", bucketsNeeded);
        }

        // Find nearest water and right click it
        BlockPos nearestLiquid = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), _toCollect);
        if (nearestLiquid != null) {
            return new InteractItemWithBlockTask(new ItemTarget(Items.BUCKET, 1), nearestLiquid);
        }

        setDebugState("Failed to find liquid. TODO: Search!!");

        return null;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(_toCollect);
        mod.getConfigState().pop();
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        if (obj instanceof CollectBucketLiquidTask) {
            CollectBucketLiquidTask task = (CollectBucketLiquidTask) obj;
            if (task._count != _count) return false;
            return task._toCollect == _toCollect;
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect " + _count + " " + _liquidName + " buckets";
    }

    public static class CollectWaterBucketTask extends CollectBucketLiquidTask {
        public CollectWaterBucketTask(int targetCount) {
            super("water", Items.WATER_BUCKET, targetCount, Blocks.WATER);
        }
    }
    public static class CollectLavaBucketTask extends CollectBucketLiquidTask {
        public CollectLavaBucketTask(int targetCount) {
            super("lava", Items.LAVA_BUCKET, targetCount, Blocks.LAVA);
        }
    }
}
