package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

@Deprecated
public class ClearRegionTask extends Task implements ITaskRequiresGrounded {

    private final BlockPos _from;
    private final BlockPos _to;

    // TODO: Progress checkers in the event of a failure.
    // Progress checker 1 for movement
    // Progress checker 2 for if block breaking isn't happening
    // Make it an "and", as in both MUST fail for a failure to count.

    public ClearRegionTask(BlockPos from, BlockPos to) {
        _from = from;
        _to = to;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!mod.getClientBaritone().getBuilderProcess().isActive()) {
            mod.getClientBaritone().getBuilderProcess().clearArea(_from, _to);
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getBuilderProcess().onLostControl();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        for (int xx = _from.getX(); xx < _to.getX(); ++xx) {
            for (int zz = _from.getZ(); zz < _to.getZ(); ++zz) {
                for (int yy = _from.getY(); yy < _to.getY(); ++yy) {
                    BlockPos toCheck = new BlockPos(xx, yy, zz);
                    assert MinecraftClient.getInstance().world != null;
                    if (!MinecraftClient.getInstance().world.isAir(toCheck)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof ClearRegionTask) {
            ClearRegionTask task = (ClearRegionTask) obj;
            return (task._from.equals(_from) && task._to.equals(_to));
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Clear region from " + _from.toShortString() + " to " + _to.toShortString();
    }
}
