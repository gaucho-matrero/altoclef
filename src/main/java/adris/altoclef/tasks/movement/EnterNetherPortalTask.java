package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.time.TimerGame;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.function.Predicate;

public class EnterNetherPortalTask extends Task {

    private final Task _getPortalTask;
    private final Dimension _targetDimension;

    private final TimerGame _portalTimeout = new TimerGame(10);
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(2);

    private final Predicate<BlockPos> _goodPortal;

    private boolean _leftPortal;

    public EnterNetherPortalTask(Task getPortalTask, Dimension targetDimension, Predicate<BlockPos> goodPortal) {
        if (targetDimension == Dimension.END)
            throw new IllegalArgumentException("Can't build a nether portal to the end.");
        _getPortalTask = getPortalTask;
        _targetDimension = targetDimension;
        _goodPortal = goodPortal;
    }

    public EnterNetherPortalTask(Dimension targetDimension, Predicate<BlockPos> goodPortal) {
        this(null, targetDimension, goodPortal);
    }
    public EnterNetherPortalTask(Task getPortalTask, Dimension targetDimension) {
        this(getPortalTask, targetDimension, blockPos -> true);
    }
    public EnterNetherPortalTask(Dimension targetDimension) {
        this(null, targetDimension);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.NETHER_PORTAL);
        _leftPortal = false;
        _portalTimeout.reset();

        _wanderTask.resetWander();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            setDebugState("Exiting portal for a bit.");
            _portalTimeout.reset();
            _leftPortal = true;
            return _wanderTask;
        }

        if (mod.getWorld().getBlockState(mod.getPlayer().getBlockPos()).getBlock() == Blocks.NETHER_PORTAL) {

            if (_portalTimeout.elapsed() && !_leftPortal) {
                return _wanderTask;
            }
            setDebugState("Waiting inside portal");
            return null;
        } else {
            _portalTimeout.reset();
        }

        Predicate<BlockPos> standablePortal = blockPos -> {
            // REQUIRE that there be solid ground beneath us, not more portal.
            if (!mod.getChunkTracker().isChunkLoaded(blockPos)) {
                // Eh just assume it's good for now
                return true;
            }
            BlockPos below = blockPos.down();
            boolean canStand = WorldHelper.isSolid(mod, below) && !mod.getBlockTracker().blockIsValid(below, Blocks.NETHER_PORTAL);
            return canStand && _goodPortal.test(blockPos);
        };

        if (mod.getBlockTracker().anyFound(standablePortal, Blocks.NETHER_PORTAL)) {
            setDebugState("Going to found portal");
            return new DoToClosestBlockTask(blockPos -> new GetToBlockTask(blockPos, false), standablePortal, Blocks.NETHER_PORTAL);
        }
        setDebugState("Getting our portal");
        return _getPortalTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.NETHER_PORTAL);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return WorldHelper.getCurrentDimension() == _targetDimension;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof EnterNetherPortalTask task) {
            return (Objects.equals(task._getPortalTask, _getPortalTask) && Objects.equals(task._targetDimension, _targetDimension));
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Entering nether portal";
    }
}
