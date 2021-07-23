package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.TimerGame;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.Function;
import java.util.function.Predicate;

public class EnterNetherPortalTask extends Task {

    private final Task _getPortalTask;
    private final Dimension _targetDimension;

    private final TimerGame _portalTimeout = new TimerGame(10);
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(2);

    private final Predicate<BlockPos> _badPortal;

    private boolean _leftPortal;

    public EnterNetherPortalTask(Task getPortalTask, Dimension targetDimension, Predicate<BlockPos> badPortal) {
        if (targetDimension == Dimension.END)
            throw new IllegalArgumentException("Can't build a nether portal to the end.");
        _getPortalTask = getPortalTask;
        _targetDimension = targetDimension;
        _badPortal = badPortal;
    }

    public EnterNetherPortalTask(Dimension targetDimension, Predicate<BlockPos> badPortal) {
        this(null, targetDimension, badPortal);
    }
    public EnterNetherPortalTask(Task getPortalTask, Dimension targetDimension) {
        this(getPortalTask, targetDimension, blockPos -> false);
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

        Function<Vec3d, BlockPos> getClosestPortal = pos -> mod.getBlockTracker().getNearestTracking(pos,
                block -> {
                    // REQUIRE that there be solid ground beneath us, not more portal.
                    if (!mod.getChunkTracker().isChunkLoaded(block)) {
                        return false;
                    }
                    BlockPos below = block.down();
                    boolean canStand = WorldUtil.isSolid(mod, below) && !mod.getBlockTracker().blockIsValid(below, Blocks.NETHER_PORTAL);
                    return !canStand || _badPortal.test(block);
                },
                Blocks.NETHER_PORTAL);

        if (getClosestPortal.apply(mod.getPlayer().getPos()) != null) {
            setDebugState("Going to found portal");
            return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(), (blockpos) -> new GetToBlockTask(blockpos, false), getClosestPortal, Blocks.NETHER_PORTAL);
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
        return mod.getCurrentDimension() == _targetDimension;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof EnterNetherPortalTask) {
            EnterNetherPortalTask task = (EnterNetherPortalTask) obj;
            //noinspection ConstantConditions
            return (((task._getPortalTask == null) == (_getPortalTask == null) || task._getPortalTask.equals(_getPortalTask)) && task._targetDimension.equals(_targetDimension));
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Entering nether portal";
    }
}
