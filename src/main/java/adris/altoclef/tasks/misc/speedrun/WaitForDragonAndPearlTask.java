package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.GetToYTask;
import adris.altoclef.tasks.movement.RunAwayFromPositionTask;
import adris.altoclef.tasks.resources.GetBuildingMaterialsTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class WaitForDragonAndPearlTask extends Task implements IDragonWaiter {

    // How far to travel away from the portal, in XZ
    private static final double XZ_RADIUS = 30;
    // How high to pillar
    private static final int HEIGHT = 38;

    private static final int CLOSE_ENOUGH_DISTANCE = 15;

    private Task _heightPillarTask;
    private Task _throwPearlTask;
    private final Task _buildingMaterialsTask = new GetBuildingMaterialsTask(HEIGHT + 10);

    private BlockPos _targetToPearl;
    private boolean _dragonIsPerching;

    @Override
    public void setExitPortalTop(BlockPos top) {
        BlockPos actualTarget = top.down();
        if (!actualTarget.equals(_targetToPearl)) {
            _targetToPearl = actualTarget;
            _throwPearlTask = new ThrowEnderPearlSimpleProjectileTask(actualTarget);
        }
    }

    @Override
    public void setPerchState(boolean perching) {
        _dragonIsPerching = perching;
    }

    @Override
    protected void onStart(AltoClef mod) {
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_throwPearlTask != null && _throwPearlTask.isActive() && !_throwPearlTask.isFinished(mod)) {
            setDebugState("Throwing pearl!");
            return _throwPearlTask;
        }

        if (!mod.getItemStorage().hasItem(Items.ENDER_PEARL)) {
            setDebugState("First get ender pearls.");
            return TaskCatalogue.getItemTask(Items.ENDER_PEARL, 1);
        }

        if (StorageHelper.getBuildingMaterialCount(mod) < 5 || (_buildingMaterialsTask != null && _buildingMaterialsTask.isActive() && !_buildingMaterialsTask.isFinished(mod))) {
            setDebugState("Collecting building materials...");
            return _buildingMaterialsTask;
        }

        // Our trigger to throw is that the dragon starts perching. We can be an arbitrary distance and we'll still do it lol
        if (_dragonIsPerching) {
            Debug.logMessage("THROWING PEARL!!");
            return _throwPearlTask;
        }

        int minHeight = _targetToPearl.getY() + HEIGHT - 3;
        if (mod.getPlayer().getBlockPos().getY() < minHeight) {
            if (_heightPillarTask != null && _heightPillarTask.isActive() && !_heightPillarTask.isFinished(mod)) {
                setDebugState("Pillaring up!");
                return _heightPillarTask;
            }
        } else {
            setDebugState("We're high enough.");
            return null;
        }

        if (WorldHelper.inRangeXZ(mod.getPlayer(), _targetToPearl, XZ_RADIUS)) {
            setDebugState("Moving away from center...");
            return new RunAwayFromPositionTask(XZ_RADIUS, _targetToPearl);
        }

        // We're far enough, pillar up!
        _heightPillarTask = new GetToYTask(minHeight);
        return _heightPillarTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof WaitForDragonAndPearlTask;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return !_dragonIsPerching
                && ((_throwPearlTask == null || !_throwPearlTask.isActive() || _throwPearlTask.isFinished(mod))
                || WorldHelper.inRangeXZ(mod.getPlayer(), _targetToPearl, CLOSE_ENOUGH_DISTANCE));
    }

    @Override
    protected String toDebugString() {
        return "Waiting for Dragon Perch + Pearling";
    }
}
