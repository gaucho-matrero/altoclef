package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.utils.input.Input;
import net.minecraft.util.math.BlockPos;


public class GetToBlockTask extends Task implements ITaskRequiresGrounded {
    
    private static final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(10, true);
    private final BlockPos _position;
    private final boolean _rightClickOnArrival;
    private final boolean _preferStairs;
    private final MovementProgressChecker _moveChecker = new MovementProgressChecker(10, 1, 5, 0.1);
    private boolean _running;
    
    public GetToBlockTask(BlockPos position, boolean rightClickOnArrival, boolean preferStairs) {
        _position = position;
        _rightClickOnArrival = rightClickOnArrival;
        _preferStairs = preferStairs;
    }
    
    public GetToBlockTask(BlockPos position, boolean rightClickOnArrival) {
        this(position, rightClickOnArrival, false);
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        if (_rightClickOnArrival) {
            return _running && !mod.getCustomBaritone().getInteractWithBlockPositionProcess().failed() &&
                   !mod.getCustomBaritone().getInteractWithBlockPositionProcess().isActive();
        } else {
            return _position.isWithinDistance(mod.getPlayer().getPos(), 1);
        }
    }
    
    @Override
    protected void onStart(AltoClef mod) {
        if (_preferStairs) {
            mod.getConfigState().push();
            mod.getConfigState().setPreferredStairs(true);
        }
        
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
        if (_preferStairs) {
            mod.getConfigState().pop();
        }
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
    protected String toDebugString() {
        return "Getting to block " + _position;
    }
    
    private boolean procActive(AltoClef mod) {
        if (_rightClickOnArrival) {
            return mod.getCustomBaritone().getInteractWithBlockPositionProcess().isActive() &&
                   !mod.getCustomBaritone().getInteractWithBlockPositionProcess().failed();
        } else {
            return mod.getClientBaritone().getCustomGoalProcess().isActive();
        }
    }
    
    private void startProc(AltoClef mod) {
        if (_rightClickOnArrival) {
            mod.getCustomBaritone().getInteractWithBlockPositionProcess().getToBlock(_position, Input.CLICK_RIGHT);
        } else {
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalTwoBlocks(_position));
        }
    }
    
    private void stopProc(AltoClef mod) {
        if (!mod.inGame()) return;
        if (_rightClickOnArrival) {
            mod.getCustomBaritone().getInteractWithBlockPositionProcess().onLostControl();
        } else {
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        }
    }
}
