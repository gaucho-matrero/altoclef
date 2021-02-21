package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.*;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.utils.input.Input;
import net.minecraft.util.math.BlockPos;

public class GetToBlockTask extends Task {

    private BlockPos _position;
    private boolean _rightClickOnArrival;

    private boolean _running;

    private final MovementProgressChecker _moveChecker = new MovementProgressChecker(10, 1, 5, 0.1);

    private static TimeoutWanderTask _wanderTask = new TimeoutWanderTask(10);

    public GetToBlockTask(BlockPos position, boolean rightClickOnArrival) {
        if (position == null) Debug.logError("Shouldn't be null!");
        _position = position;
        _rightClickOnArrival = rightClickOnArrival;
    }

    @Override
    protected void onStart(AltoClef mod) {
        startProc(mod);
        _moveChecker.reset();
        _wanderTask.resetWander();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // Wander
        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            setDebugState("Wandering...");
            _moveChecker.reset();
            return _wanderTask;
        }

        if (!procActive(mod)) {
            Debug.logWarning("Restarting interact with block...");
            startProc(mod);
            _running = true;
        }
        // Check for failure
        boolean failed = false;
        if (!_moveChecker.check(mod)) {
            return _wanderTask;
        }
        // Baritone task
        setDebugState("Going to block.");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        _running = false;
        stopProc(mod);
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
        return _running && !mod.getCustomBaritone().getInteractWithBlockPositionProcess().failed() && !mod.getCustomBaritone().getInteractWithBlockPositionProcess().isActive();
    }

    @Override
    protected String toDebugString() {
        return "Getting to block " + _position;
    }

    private boolean procActive(AltoClef mod) {
        if (_rightClickOnArrival) {
            return mod.getCustomBaritone().getInteractWithBlockPositionProcess().isActive() && !mod.getCustomBaritone().getInteractWithBlockPositionProcess().failed();
        } else {
            return mod.getClientBaritone().getCustomGoalProcess().isActive();
        }
    }
    private void startProc(AltoClef mod) {
        if (_rightClickOnArrival) {
            mod.getCustomBaritone().getInteractWithBlockPositionProcess().getToBlock(_position, _rightClickOnArrival? Input.CLICK_RIGHT : null);
        } else {
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalTwoBlocks(_position));
        }
    }
    private void stopProc(AltoClef mod) {
        if (_rightClickOnArrival) {
            mod.getCustomBaritone().getInteractWithBlockPositionProcess().onLostControl();
        } else {
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        }
    }
}
