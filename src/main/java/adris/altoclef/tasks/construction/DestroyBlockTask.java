package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.PlaceBlockSchematic;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class DestroyBlockTask extends Task {

    private final BlockPos _pos;

    private boolean _failedFirstTry;

    public DestroyBlockTask(BlockPos pos) {
        _pos = pos;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritone().getBuilderProcess().build("destroy block", new PlaceBlockSchematic(Blocks.AIR), _pos);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!_failedFirstTry && !mod.getClientBaritone().getBuilderProcess().isActive() || mod.getClientBaritone().getBuilderProcess().isPaused()) {
            Debug.logMessage("Failed initial destruction, trying another way.");
            _failedFirstTry = true;
        }

        if (_failedFirstTry) {
            if (mod.getClientBaritone().getBuilderProcess().isActive()) {
                mod.getClientBaritone().getBuilderProcess().onLostControl();
            }
            setDebugState("Going to destroy block to destroy the block");
            // This will destroy the target block.
            return new GetToBlockTask(_pos, false);
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getBuilderProcess().onLostControl();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof DestroyBlockTask) {
            DestroyBlockTask task = (DestroyBlockTask) obj;
            return task._pos.equals(_pos);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Destroy block at " + _pos.toShortString();
    }
}
