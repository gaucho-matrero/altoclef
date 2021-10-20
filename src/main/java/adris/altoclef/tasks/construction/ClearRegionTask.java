package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;


public class ClearRegionTask extends Task implements ITaskRequiresGrounded {

    private final BlockPos from;
    private final BlockPos to;

    // TODO: Progress checkers in the event of a failure.
    // Progress checker 1 for movement
    // Progress checker 2 for if block breaking isn't happening
    // Make it an "and", as in both MUST fail for a failure to count.

    public ClearRegionTask(BlockPos from, BlockPos to) {
        this.from = from;
        this.to = to;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!mod.getClientBaritone().getBuilderProcess().isActive()) {
            mod.getClientBaritone().getBuilderProcess().clearArea(from, to);
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getBuilderProcess().onLostControl();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        int x = from.getX() - to.getX();
        int y = from.getY() - to.getY();
        int z = from.getZ() - to.getZ();
        for (int xx = 0; xx < Math.abs(x); ++xx) {
            for (int yy = 0; yy < Math.abs(y); ++yy) {
                for (int zz = 0; zz < Math.abs(z); ++zz) {
                    BlockPos toCheck = new BlockPos(from).add(xx * -Integer.signum(x), yy * -Integer.signum(y), zz * -Integer.signum(z));
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
    protected boolean isEqual(Task other) {
        if (other instanceof ClearRegionTask) {
            ClearRegionTask task = (ClearRegionTask) other;
            return (task.from.equals(from) && task.to.equals(to));
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Clear region from " + from.toShortString() + " to " + to.toShortString();
    }
}
