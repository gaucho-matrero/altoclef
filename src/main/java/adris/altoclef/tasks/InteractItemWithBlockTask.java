package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.baritone.InteractWithBlockPositionProcess;
import adris.altoclef.util.csharpisbetter.Action;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.utils.input.Input;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class InteractItemWithBlockTask extends Task {

    private static final int MAX_REACH = 7;

    private final ItemTarget _toUse;

    private final Direction _direction;
    private final BlockPos _target;

    private Input _interactInput;

    private boolean _trying;

    private final MovementProgressChecker _moveChecker = new MovementProgressChecker(4, 0.1, 4, 0.01);
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(5);

    private int _prevReach = -1;

    public final Action TimedOut = new Action();

    public InteractItemWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, Input interactInput) {
        _toUse = toUse;
        _direction = direction;
        _target = target;
        _interactInput = interactInput;
    }
    public InteractItemWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target) {
        this(toUse, direction, target, Input.CLICK_RIGHT);
    }
    public InteractItemWithBlockTask(ItemTarget toUse, BlockPos target) {
        // null means any side is OK
        this(toUse, null, target);
    }

    @Override
    protected void onStart(AltoClef mod) {
        _trying = false;
        _moveChecker.reset();
        _wanderTask.resetWander();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (_toUse != null && !mod.getInventoryTracker().targetMet(_toUse)) {
            _moveChecker.reset();
            return TaskCatalogue.getItemTask(_toUse);
        }

        // Wander and check
        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            _moveChecker.reset();
            return _wanderTask;
        }
        if (!_moveChecker.check(mod)) {
            Debug.logMessage("Failed, wandering.");
            TimedOut.invoke();
            return _wanderTask;
        }

        if (!proc(mod).isActive()) {
            Debug.logMessage("Interact with block process restarting");
            _trying = true;
            proc(mod).getToBlock(_target, _direction, _interactInput, true, false);
            if (_toUse != null) {
                proc(mod).setInteractEquipItem(_toUse);
            }
        } else {
            if (_prevReach < MAX_REACH && proc(mod).reachCounter != _prevReach) {
                _prevReach = proc(mod).reachCounter;
                _moveChecker.reset();
            }
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        proc(mod).onLostControl();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _trying && !proc(mod).isActive();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof InteractItemWithBlockTask) {
            InteractItemWithBlockTask task = (InteractItemWithBlockTask) obj;
            if ((task._direction == null) != (_direction == null)) return false;
            if (task._direction != null && !task._direction.equals(_direction)) return false;
            if (!task._toUse.equals(_toUse)) return false;
            if (!task._target.equals(_target)) return false;
            if (!task._interactInput.equals(_interactInput)) return false;
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Interact using " + _toUse + " at " + _target + " dir " + _direction;
    }

    private InteractWithBlockPositionProcess proc(AltoClef mod) {
        return mod.getCustomBaritone().getInteractWithBlockPositionProcess();
    }

}
