package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractItemWithBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.progresscheck.DistanceProgressChecker;
import adris.altoclef.util.progresscheck.IProgressChecker;
import adris.altoclef.util.progresscheck.LinearProgressChecker;
import baritone.api.utils.RayTraceUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;

public class CollectBucketLiquidTask extends ResourceTask {

    private int _count;

    private Item _target;
    private Block _toCollect;

    private String _liquidName;

    private IProgressChecker<Double> _checker = new LinearProgressChecker(3, 0.1);

    private Task _wanderTask = new TimeoutWanderTask(6.5f);

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

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onResourceStart(AltoClef mod) {
        // Track fluids
        mod.getConfigState().push();
        mod.getConfigState().setRayTracingFluidHandling(RaycastContext.FluidHandling.SOURCE_ONLY);
        mod.getConfigState().setSearchAnywhereFlag(true); // If we don't set this, lava will never be found.
        mod.getBlockTracker().trackBlock(_toCollect);

        // Avoid breaking / placing blocks at our liquid
        mod.getConfigState().avoidBlockBreaking((pos) -> MinecraftClient.getInstance().world.getBlockState(pos).getBlock() == _toCollect);
        mod.getConfigState().avoidBlockPlacing((pos) -> MinecraftClient.getInstance().world.getBlockState(pos).getBlock() == _toCollect);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            setDebugState("Failed to receive: Wandering.");
            resetChecker();
            return _wanderTask;
        }

        // Get buckets if we need em
        int bucketsNeeded = _count - mod.getInventoryTracker().getItemCount(Items.BUCKET) - mod.getInventoryTracker().getItemCount(_target);
        if (bucketsNeeded > 0) {
            setDebugState("Getting bucket...");
            resetChecker();
            return TaskCatalogue.getItemTask("bucket", bucketsNeeded);
        }

        // Find nearest water and right click it
        BlockPos nearestLiquid = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), _toCollect, (blockPos -> {
            assert MinecraftClient.getInstance().world != null;
            BlockState s = MinecraftClient.getInstance().world.getBlockState(blockPos);
            int level =s.getFluidState().getLevel();
            //Debug.logMessage("TEST LEVEL: " + level);
            // Only accept FULL SOURCE BLOCKS
            return level != 8;
        }));
        if (nearestLiquid != null) {
            double distToLiquid = mod.getPlayer().squaredDistanceTo(nearestLiquid.getX(), nearestLiquid.getY(), nearestLiquid.getZ());
            // We want to MINIMIZE this distance to liquid.
            _checker.setProgress(-1 * distToLiquid);
            if (_checker.failed()) {
                Debug.logMessage("Failed to get to liquid, wandering for a bit before trying again...");
                return _wanderTask;
            }
            setDebugState("Interacting...");
            //Debug.logMessage("TEST: " + RayTraceUtils.fluidHandling);
            return new InteractItemWithBlockTask(new ItemTarget(Items.BUCKET, 1), nearestLiquid);
        }

        // Oof, no liquid found.
        setDebugState("Searching for liquid by wandering around aimlessly");

        return new TimeoutWanderTask(Float.POSITIVE_INFINITY);
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

    private void resetChecker() {
        _checker.setProgress(Double.NEGATIVE_INFINITY);
        _checker.reset();
    }
}
