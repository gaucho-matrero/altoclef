package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

/**
 * Places obsidian at a position using buckets and a cast.
 */
public class PlaceObsidianBucketTask extends Task {

    public static final Vec3i[] CAST_FRAME = new Vec3i[]{
            new Vec3i(0, -1, 0),
            new Vec3i(0, 0, -1),
            new Vec3i(0, 0, 1),
            new Vec3i(-1, 0, 0),
            new Vec3i(1, 0, 0),
            new Vec3i(1, 1, 0)
    };
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private final BlockPos _pos;

    private BlockPos _currentCastTarget;
    private BlockPos _currentDestroyTarget;

    public PlaceObsidianBucketTask(BlockPos pos) {
        _pos = pos;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        // Don't break cast
        mod.getBehaviour().avoidBlockBreaking(block -> {
            for (Vec3i castPosRelativeToLava : PlaceObsidianBucketTask.CAST_FRAME) {
                BlockPos castPos = _pos.add(castPosRelativeToLava);
                if (block.equals(castPos)) {
                    return true;
                }
            }
            return false;
        });
        // Don't place blocks inside our cast water/lava
        mod.getBehaviour().avoidBlockPlacing(block -> {
            BlockPos waterTarget = _pos.up();
            return block.equals(_pos) || block.equals(waterTarget);
        });

        _progressChecker.reset();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            _progressChecker.reset();
        }

        // Clear leftover water
        if (mod.getBlockTracker().blockIsValid(_pos, Blocks.OBSIDIAN) && mod.getBlockTracker().blockIsValid(_pos.up(), Blocks.WATER)) {
            return new ClearLiquidTask(_pos.up());
        }
        // Make sure we have water, juuust in case we have another creeper appear run end here
        if (!mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
            _progressChecker.reset();
            return TaskCatalogue.getItemTask(Items.WATER_BUCKET, 1);
        }
        if (!mod.getItemStorage().hasItem(Items.LAVA_BUCKET)) {
            // The only excuse is that we have lava at our position.
            if (!mod.getBlockTracker().blockIsValid(_pos, Blocks.LAVA)) {
                _progressChecker.reset();
                return TaskCatalogue.getItemTask(Items.LAVA_BUCKET, 1);
            }
        }
        if (!_progressChecker.check(mod)) {
            mod.getClientBaritone().getPathingBehavior().cancelEverything();
            mod.getClientBaritone().getPathingBehavior().forceCancel();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getBlockTracker().requestBlockUnreachable(_pos);
            _progressChecker.reset();
            return new TimeoutWanderTask(5);
        }

        if (_currentCastTarget != null) {
            if (WorldHelper.isSolid(mod, _currentCastTarget)) {
                _currentCastTarget = null;
            } else {
                return new PlaceStructureBlockTask(_currentCastTarget);
            }
        }
        if (_currentDestroyTarget != null) {
            if (!WorldHelper.isSolid(mod, _currentDestroyTarget)) {
                _currentDestroyTarget = null;
            } else {
                return new DestroyBlockTask(_currentDestroyTarget);
            }
        }

        // Build the cast frame
        if (_currentCastTarget != null && WorldHelper.isSolid(mod, _currentCastTarget)) {
            // Current cast frame already built.
            _currentCastTarget = null;
        }
        for (Vec3i castPosRelative : CAST_FRAME) {
            BlockPos castPos = _pos.add(castPosRelative);
            if (!WorldHelper.isSolid(mod, castPos)) {
                _currentCastTarget = castPos;
                return null;
            }
        }

        // Cast frame built. Now, place lava.
        if (mod.getWorld().getBlockState(_pos).getBlock() != Blocks.LAVA) {
            // Don't place lava at our position!
            // Would lead to an embarrassing death.
            BlockPos targetPos = _pos.add(-1, 1, 0);
            if (!mod.getPlayer().getBlockPos().equals(targetPos) && mod.getItemStorage().hasItem(Items.LAVA_BUCKET)) {
                setDebugState("Positioning player before lava");
                return new GetToBlockTask(targetPos, false);
            }
            if (WorldHelper.isSolid(mod, _pos)) {
                setDebugState("Clearing space around lava");
                _currentDestroyTarget = _pos;
                return null;
                //return new DestroyBlockTask(framePos);
            }
            // Clear the upper two as well, to make placing more reliable.
            if (WorldHelper.isSolid(mod, _pos.up())) {
                setDebugState("Clearing space around lava");
                _currentDestroyTarget = _pos.up();
                return null;
            }
            if (WorldHelper.isSolid(mod, _pos.up(2))) {
                setDebugState("Clearing space around lava");
                _currentDestroyTarget = _pos.up(2);
                return null;
            }
            setDebugState("Placing lava for cast");
            return new InteractWithBlockTask(new ItemTarget(Items.LAVA_BUCKET, 1), Direction.WEST, _pos.add(1, 0, 0), false);
        }
        // Lava placed, Now, place water.
        BlockPos waterCheck = _pos.up();
        if (mod.getWorld().getBlockState(waterCheck).getBlock() != Blocks.WATER) {
            setDebugState("Placing water for cast");
            // Get to position to avoid weird stuck scenario
            BlockPos targetPos = _pos.add(-1, 1, 0);
            if (!mod.getPlayer().getBlockPos().equals(targetPos) && mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
                setDebugState("Positioning player before water");
                return new GetToBlockTask(targetPos, false);
            }
            if (WorldHelper.isSolid(mod, waterCheck)) {
                _currentDestroyTarget = waterCheck;
                return null;
                //return new DestroyBlockTask(waterCheck);

            }
            if (WorldHelper.isSolid(mod, waterCheck.up())) {
                _currentDestroyTarget = waterCheck.up();
                return null;
                //return new DestroyBlockTask(waterCheck.up());
            }
            return new InteractWithBlockTask(new ItemTarget(Items.WATER_BUCKET, 1), Direction.WEST, _pos.add(1, 1, 0), true);
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getBlockTracker().blockIsValid(_pos, Blocks.OBSIDIAN) && !mod.getBlockTracker().blockIsValid(_pos.up(), Blocks.WATER);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof PlaceObsidianBucketTask task) {
            return task._pos.equals(_pos);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Placing obsidian at " + _pos + " with a cast";
    }

    public BlockPos getPos() {
        return _pos;
    }
}
