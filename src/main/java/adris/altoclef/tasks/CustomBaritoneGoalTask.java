package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.Goal;


public abstract class CustomBaritoneGoalTask extends Task implements ITaskRequiresGrounded {
    
    private final boolean _wander;
    private final Task _wanderTask = new TimeoutWanderTask(10);
    protected Goal _cachedGoal = null;
    protected MovementProgressChecker _checker = new MovementProgressChecker();
    
    public CustomBaritoneGoalTask(boolean wander) {
        _wander = wander;
    }
    
    public CustomBaritoneGoalTask() {
        this(true);
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        return _cachedGoal != null && _cachedGoal.isInGoal(mod.getPlayer().getBlockPos());
    }
    
    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        _checker.reset();
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        
        if (_wander) {
            if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
                setDebugState("Wandering...");
                _checker.reset();
                return _wanderTask;
            }
            if (!_checker.check(mod)) {
                Debug.logMessage("Failed to make progress on goal, wandering.");
                return _wanderTask;
            }
        }
        
        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
            _cachedGoal = newGoal(mod);
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(_cachedGoal);
        }
        setDebugState("Completing goal.");
        return null;
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }
    
    protected abstract Goal newGoal(AltoClef mod);
}
