package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.InteractItemWithBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.ProjectileUtil;
import adris.altoclef.util.csharpisbetter.ActionListener;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;

import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

public class CollectBucketLiquidTask extends ResourceTask {

    private int _count;

    private Item _target;
    private Block _toCollect;

    private String _liquidName;

    //private IProgressChecker<Double> _checker = new LinearProgressChecker(5, 0.1);

    private TimeoutWanderTask _wanderTask = new TimeoutWanderTask(6.5f);

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

        //_blacklist.clear();

        _wanderTask.resetWander();
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

        Function<Vec3d, BlockPos> getNearestLiquid = ppos -> mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), (blockPos -> {
            if (_blacklist.contains(blockPos)) return true;
            assert MinecraftClient.getInstance().world != null;

            // Block above must not have liquid
            BlockState above = mod.getWorld().getBlockState(blockPos.up());
            if (above.getBlock() instanceof FluidBlock) return true;

            BlockState s = mod.getWorld().getBlockState(blockPos);
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
        }), _toCollect);

        // Find nearest water and right click it
        BlockPos nearestLiquid = getNearestLiquid.apply(mod.getPlayer().getPos());
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

            return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(), (BlockPos blockpos) -> {
                //Vec3d center = new Vec3d(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 0.5);
                //BlockHitResult hit = mod.getWorld().raycast(new RaycastContext(mod.getPlayer().getCameraPosVec(1.0F), center, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.SOURCE_ONLY, mod.getPlayer()));
                //if (hit.getBlockPos().equals(blockpos)) {
                InteractItemWithBlockTask task = new InteractItemWithBlockTask(new ItemTarget(Items.BUCKET, 1), blockpos, false);
                //noinspection unchecked
                task.TimedOut.addListener(
                        new ActionListener() {
                            @Override
                            public void invoke(Object value) {
                                Debug.logInternal("CURRENT BLACKLIST: " + Util.arrayToString(_blacklist.toArray()));
                                Debug.logMessage("Blacklisted " + blockpos);
                                _blacklist.add(nearestLiquid);

                            }
                        });
                return task;
                //} else {
                /*
                    if (!mod.getWorld().getBlockState(blockpos.up()).isAir()) {
                        // If above is solid and we're stuck, break the top off.
                        return new DestroyBlockTask(blockpos.up());
                    } else {
                        // Try to get close.
                        for (int dx = -1; dx <= 1; ++dx) {
                            for (int dz = -1; dz <= 1; ++dz) {
                                boolean good = true;
                                BlockPos currentTry = blockpos.add(dx, 1, dz);
                                // Check if lava is around us
                                Vec3i[] checkDeltas = new Vec3i[] {
                                        new Vec3i(-1, 0, 0),
                                        new Vec3i(1, 0, 0),
                                        new Vec3i(0, 0, -1),
                                        new Vec3i(0, 0, 1),

                                        new Vec3i(-1, 1, 0),
                                        new Vec3i(1, 1, 0),
                                        new Vec3i(0, 1, -1),
                                        new Vec3i(0, 1, 1)
                                };
                                for (Vec3i deltaCheck : checkDeltas) {
                                    BlockPos check = currentTry.add(deltaCheck);
                                    if (mod.getWorld().getBlockState(check).getBlock() == Blocks.LAVA) {
                                        good = false;
                                        break;
                                    }
                                }
                                if (good) {
                                    return new GetToBlockTask(currentTry, false);
                                }
                            }
                        }
                        // We're kinda screwed I think...
                        return null;
                    }
                }

                 */
            }, getNearestLiquid, _toCollect);
            //return task;
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
