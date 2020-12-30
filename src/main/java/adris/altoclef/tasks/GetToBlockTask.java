package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.BlockPos;

public class GetToBlockTask extends Task {

    private BlockPos _position;
    private boolean _rightClickOnArrival;

    private boolean _running;

    public GetToBlockTask(BlockPos position, boolean rightClickOnArrival) {
        if (position == null) Debug.logError("Shouldn't be null!");
        _position = position;
        _rightClickOnArrival = rightClickOnArrival;
    }

    @Override
    protected void onStart(AltoClef mod) {
        Debug.logMessage("GOING TO BLOCK");
        mod.getCustomBaritone().getInteractWithBlockPositionProcess().getToBlock(_position, _rightClickOnArrival);
        _running = true;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Baritone task
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        _running = false;
        mod.getCustomBaritone().getInteractWithBlockPositionProcess().onLostControl();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GetToBlockTask) {
            GetToBlockTask other = (GetToBlockTask) obj;
            if (other._position == null) return true;
            return other._position.equals(_position) && other._rightClickOnArrival == _rightClickOnArrival;
        }
        return false;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _running && !mod.getCustomBaritone().getInteractWithBlockPositionProcess().isActive();
    }

    @Override
    protected String toDebugString() {
        return "Getting to block " + _position;
    }
}
