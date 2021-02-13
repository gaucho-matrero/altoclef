package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractItemWithBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.progresscheck.DistanceProgressChecker;
import adris.altoclef.util.progresscheck.IProgressChecker;
import adris.altoclef.util.progresscheck.LinearProgressChecker;
import baritone.api.utils.RayTraceUtils;
import baritone.api.utils.Rotation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;

import java.util.HashSet;
import java.util.Optional;

public class CollectBucketLiquidTask extends ResourceTask {

    private int _count;

    private Item _target;
    private Block _toCollect;

    private String _liquidName;

    //private IProgressChecker<Double> _checker = new LinearProgressChecker(5, 0.1);

    private Task _wanderTask = new TimeoutWanderTask(6.5f);

    private final HashSet<BlockPos> _blacklist = new HashSet<>();

    private final Timer _reachTimer = new Timer(2);

    private BlockPos _targetLiquid;

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

        _blacklist.clear();
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            setDebugState("Failed to receive: Wandering.");
            _reachTimer.reset();
            return _wanderTask;
        }

        // Get buckets if we need em
        int bucketsNeeded = _count - mod.getInventoryTracker().getItemCount(Items.BUCKET) - mod.getInventoryTracker().getItemCount(_target);
        if (bucketsNeeded > 0) {
            setDebugState("Getting bucket...");
            _reachTimer.reset();
            return TaskCatalogue.getItemTask("bucket", bucketsNeeded);
        }

        // Find nearest water and right click it
        BlockPos nearestLiquid = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), _toCollect, (blockPos -> {
            if (_blacklist.contains(blockPos)) return true;
            assert MinecraftClient.getInstance().world != null;
            BlockState s = MinecraftClient.getInstance().world.getBlockState(blockPos);
            if (s.getBlock() instanceof FluidBlock) {
                float height = s.getFluidState().getHeight();
                // Only accept still fluids.
                if (!s.getFluidState().isStill()) return true;
                int level = s.getFluidState().getLevel();
                //Debug.logMessage("TEST LEVEL: " + level + ", " + height);
                // Only accept FULL SOURCE BLOCKS
                return level != 8;
            }
            return true;
        }));
        _targetLiquid = nearestLiquid;
        if (nearestLiquid != null) {
            // We want to MINIMIZE this distance to liquid.
            setDebugState("Interacting...");
            //Debug.logMessage("TEST: " + RayTraceUtils.fluidHandling);

            // If we're able to reach the block but we fail...
            if (mod.getCustomBaritone().getInteractWithBlockPositionProcess().isActive()) {
                Optional<Rotation> reach = mod.getCustomBaritone().getInteractWithBlockPositionProcess().getReach();
                if (reach.isPresent()) {
                    if (_reachTimer.elapsed()) {
                        _reachTimer.reset();
                        Debug.logMessage("Failed to collect liquid at " + nearestLiquid + ", probably an invalid source block. blacklisting and trying another one.");
                        _blacklist.add(nearestLiquid);
                        // Try again.
                        return null;
                    }
                } else {
                    _reachTimer.reset();
                }
            }

            InteractItemWithBlockTask task = new InteractItemWithBlockTask(new ItemTarget(Items.BUCKET, 1), nearestLiquid);
            //noinspection unchecked
            task.TimedOut.addListener((empty) -> {
                Debug.logMessage("Blacklisted " + nearestLiquid);
                _blacklist.add(nearestLiquid);
            });
            return task;
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

}
