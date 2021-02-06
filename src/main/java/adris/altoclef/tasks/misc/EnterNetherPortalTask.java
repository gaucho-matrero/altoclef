package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class EnterNetherPortalTask extends Task {

    private final Task _getPortalTask;
    private final Dimension _targetDimension;

    public EnterNetherPortalTask(Task getPortalTask, Dimension targetDimension) {
        if (targetDimension == Dimension.END) throw new IllegalArgumentException("Can't build a nether portal to the end.");
        _getPortalTask = getPortalTask;
        _targetDimension = targetDimension;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.NETHER_PORTAL);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (mod.getWorld().getBlockState(mod.getPlayer().getBlockPos()).getBlock() == Blocks.NETHER_PORTAL) {
            setDebugState("Waiting inside portal");
            return null;
        }

        BlockPos portal = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), Blocks.NETHER_PORTAL);
        if (portal != null) {
            setDebugState("Going to found portal");
            return new GetToBlockTask(portal, false);
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
            return (task._getPortalTask.equals(_getPortalTask) && task._targetDimension.equals(_targetDimension));
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Entering nether portal";
    }
}
