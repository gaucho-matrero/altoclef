package adris.altoclef.tasks.misc.anarchysurvive;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.GoInDirectionXZTask;
import adris.altoclef.tasks.misc.EnterNetherPortalTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public class TravelAlongHighwayAxis extends Task {

    private final HighwayAxis _axis;
    private final Predicate<BlockPos> _invalidNetherPortal;

    public TravelAlongHighwayAxis(HighwayAxis axis, Predicate<BlockPos> invalidNetherPortal) {
        _axis = axis;
        _invalidNetherPortal = invalidNetherPortal;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.NETHER_PORTAL);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // If we're in the overworld and we found a portal...
        if (mod.getCurrentDimension() == Dimension.OVERWORLD) {

            if (mod.getBlockTracker().anyFound(_invalidNetherPortal, Blocks.NETHER_PORTAL)) {
                return new EnterNetherPortalTask(Dimension.NETHER, _invalidNetherPortal);
            }
        }
        return new GoInDirectionXZTask(Vec3d.ZERO, _axis.getDirection(), 1.8);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.NETHER_PORTAL);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof TravelAlongHighwayAxis) {
            TravelAlongHighwayAxis task = (TravelAlongHighwayAxis) obj;
            return task._axis == _axis;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Travelling along " + _axis;
    }
}
