package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.ClearLiquidTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class CollectObsidianTask extends ResourceTask {

    private final TimerGame _placeWaterTimeout = new TimerGame(6);
    private final MovementProgressChecker _lavaTimeout = new MovementProgressChecker();
    private final Set<BlockPos> _lavaBlacklist = new HashSet<>();
    private final int _count;
    private Task _forceCompleteTask = null;
    private BlockPos _lavaWaitCurrentPos;

    public CollectObsidianTask(int count) {
        super(Items.OBSIDIAN, count);
        _count = count;
    }

    private static BlockPos getLavaStructurePos(BlockPos lavaPos) {
        return lavaPos.add(1, 1, 0);
    }

    private static BlockPos getLavaWaterPos(BlockPos lavaPos) {
        return lavaPos.up();
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBehaviour().push();

        mod.getBehaviour().setRayTracingFluidHandling(RaycastContext.FluidHandling.SOURCE_ONLY);
        mod.getBehaviour().setSearchAnywhereFlag(true); // If we don't set this, lava will never be found.

        mod.getBlockTracker().trackBlock(Blocks.OBSIDIAN);
        mod.getBlockTracker().trackBlock(Blocks.WATER);
        mod.getBlockTracker().trackBlock(Blocks.LAVA);

        // Avoid placing on the lava block we're trying to mine.
        mod.getBehaviour().avoidBlockPlacing(pos -> {
            if (_lavaWaitCurrentPos != null) {
                return pos.equals(_lavaWaitCurrentPos) || pos.equals(getLavaWaterPos(_lavaWaitCurrentPos));
            }
            return false;
        });
        mod.getBehaviour().avoidBlockBreaking(pos -> {
            if (_lavaWaitCurrentPos != null) {
                return pos.equals(getLavaStructurePos(_lavaWaitCurrentPos));
            }
            return false;
        });
    }

    @Override
    protected adris.altoclef.tasksystem.Task onResourceTick(AltoClef mod) {

        // Clear the current waiting lava pos if it's no longer lava.
        if (_lavaWaitCurrentPos != null && mod.getChunkTracker().isChunkLoaded(_lavaWaitCurrentPos) && mod.getWorld().getBlockState(_lavaWaitCurrentPos).getBlock() != Blocks.LAVA) {
            _lavaWaitCurrentPos = null;
        }

        // Get a diamond pickaxe FIRST
        if (!mod.getInventoryTracker().miningRequirementMet(MiningRequirement.DIAMOND)) {
            setDebugState("Getting diamond pickaxe first");
            return new SatisfyMiningRequirementTask(MiningRequirement.DIAMOND);
        }

        if (_forceCompleteTask != null && _forceCompleteTask.isActive() && !_forceCompleteTask.isFinished(mod)) {
            return _forceCompleteTask;
        }

        if (mod.getBlockTracker().anyFound(Blocks.OBSIDIAN) || mod.getEntityTracker().itemDropped(Items.OBSIDIAN)) {
            // Clear nearby water
            BlockPos nearestObby = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), Blocks.OBSIDIAN);
            if (nearestObby != null) {
                BlockPos nearestWater = mod.getBlockTracker().getNearestTracking(WorldUtil.toVec3d(nearestObby), blockPos -> !WorldUtil.isSourceBlock(mod, blockPos, true), Blocks.WATER);

                if (nearestWater != null && nearestWater.getSquaredDistance(nearestObby) < 10 * 10) {
                    _forceCompleteTask = new ClearLiquidTask(nearestWater);
                    setDebugState("Clearing water nearby obsidian");
                    return _forceCompleteTask;
                }
            }

            setDebugState("Mining/Collecting obsidian");
            return new MineAndCollectTask(new ItemTarget(Items.OBSIDIAN, _count), new Block[]{Blocks.OBSIDIAN}, MiningRequirement.DIAMOND);
        }

        Function<Vec3d, BlockPos> getNearestLava = ppos -> mod.getBlockTracker().getNearestTracking(ppos,
                blockPos -> {
                    if (_lavaBlacklist.contains(blockPos)) return true;
                    if (!WorldUtil.isSourceBlock(mod, blockPos, true)) return true;

                    BlockPos placeOnPos = getLavaStructurePos(blockPos);

                    BlockState placeOnState = mod.getWorld().getBlockState(placeOnPos);
                    // We can't place a structure on lava.
                    return placeOnState.getBlock() instanceof FluidBlock;
                    //if (!mod.getWorld().getBlockState(placeOnPos).getFluidState().isEmpty()) return true;
                },
                Blocks.LAVA);

        // No stuff detected, try finding lava and placing water near it.
        BlockPos nearestLava = getNearestLava.apply(mod.getPlayer().getPos());

        //Debug.logInternal("NEAREST LAVA: " + (nearestLava != null? nearestLava.toShortString() : "(null)") + " # lava: " + mod.getBlockTracker().getKnownLocations(Blocks.LAVA).size());

        if (nearestLava != null) {

            //noinspection PointlessNullCheck
            if (_lavaWaitCurrentPos == null || !nearestLava.equals(_lavaWaitCurrentPos)) {
                // We found a new lava to pursue.
                _lavaWaitCurrentPos = nearestLava;
                _lavaTimeout.reset();
            }

            // Collect water first
            if (!mod.getInventoryTracker().hasItem(Items.WATER_BUCKET)) {
                _lavaTimeout.reset();
                _forceCompleteTask = TaskCatalogue.getItemTask("water_bucket", 1);
                setDebugState("Getting water bucket");
                return _forceCompleteTask;
            }

            if (!_lavaTimeout.check(mod)) {
                Debug.logMessage("Failed to obsidian-ify lava, blacklisting.");
                _lavaBlacklist.add(_lavaWaitCurrentPos);
                //_lavaWaitCurrentPos = null;
                return null;
            }

            // Make sure we have a spot to place the water.
            BlockPos waterTargetPos = getLavaWaterPos(nearestLava);
            BlockPos placeOnPos = getLavaStructurePos(nearestLava);
            if (!WorldUtil.isSolid(mod, placeOnPos)) {
                setDebugState("Making structure to place water on");
                return new PlaceStructureBlockTask(placeOnPos);
            }

            setDebugState("Placing water near lava");

            if (!mod.getWorld().getBlockState(waterTargetPos).isAir()) {
                return new DestroyBlockTask(waterTargetPos);
            }

            //_placeWaterTimeout.reset();
            return new InteractWithBlockTask(TaskCatalogue.getItemTarget("water_bucket", 1), Direction.WEST, placeOnPos, true);
        } else {
            _lavaTimeout.reset();
        }

        setDebugState("Wandering, no obsidian/lava found.");
        return new TimeoutWanderTask(true);
    }

    @Override
    protected void onResourceStop(AltoClef mod, adris.altoclef.tasksystem.Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.LAVA);
        mod.getBlockTracker().stopTracking(Blocks.WATER);
        mod.getBlockTracker().stopTracking(Blocks.OBSIDIAN);
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        if (obj instanceof CollectObsidianTask) {
            CollectObsidianTask task = (CollectObsidianTask) obj;
            return task._count == _count;
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect " + _count + " blocks of obsidian";
    }
}
