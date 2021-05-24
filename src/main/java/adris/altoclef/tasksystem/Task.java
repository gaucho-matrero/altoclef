package adris.altoclef.tasksystem;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;

import java.util.function.Predicate;


public abstract class Task {
    
    private String debugState = "";
    private Task sub;
    private boolean first = true;
    private boolean stopped;
    private boolean active;
    
    public void tick(AltoClef mod, TaskChain parentChain) {
        parentChain.addTaskToChain(this);
        if (first) {
            Debug.logInternal("Task START: " + this);
            active = true;
            onStart(mod);
            first = false;
            stopped = false;
        }
        if (stopped) return;
        
        Task newSub = onTick(mod);
        // We have a sub task
        if (newSub != null) {
            if (!newSub.isEqual(sub)) {
                if (canBeInterrupted(mod, sub, newSub)) {
                    // Our sub task is new
                    if (sub != null) {
                        // Our previous sub must be interrupted.
                        sub.stop(mod, newSub);
                    }
                    
                    sub = newSub;
                }
            }
            
            // Run our child
            sub.tick(mod, parentChain);
        } else {
            // We are null
            if (sub != null && canBeInterrupted(mod, sub, null)) {
                // Our previous sub must be interrupted.
                sub.stop(mod);
                sub = null;
            }
        }
    }
    
    public void reset() {
        first = true;
        active = false;
        stopped = false;
    }
    
    protected void stop(AltoClef mod, Task interruptTask) {
        if (!active) return;
        
        onStop(mod, interruptTask);
        Debug.logInternal("Task STOP: " + this + ", interrupted by " + interruptTask);
        
        if (sub != null && !sub.stopped()) {
            sub.stop(mod, interruptTask);
        }
        
        first = true;
        active = false;
        stopped = true;
    }
    
    protected boolean taskAssert(AltoClef mod, boolean condition, String message) {
        if (!condition && !stopped) {
            Debug.logError("Task assertion failed: " + message);
            stop(mod);
            stopped = true;
        }
        return condition;
    }
    
    public void stop(AltoClef mod) {
        stop(mod, null);
    }
    
    protected void setDebugState(String state) {
        if (debugState.equals(state)) {
            debugState = state;
        } else {
            debugState = state;
            Debug.logInternal(toString());
        }
    }
    
    // Virtual
    public boolean isFinished(AltoClef mod) {
        return false;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public boolean stopped() {
        return stopped;
    }
    
    protected abstract void onStart(AltoClef mod);
    
    protected abstract Task onTick(AltoClef mod);
    
    // interruptTask = null if the task stopped cleanly
    protected abstract void onStop(AltoClef mod, Task interruptTask);
    
    protected abstract boolean isEqual(Task obj);
    
    protected abstract String toDebugString();
    
    public boolean thisOrChildSatisfies(Predicate<? super Task> pred) {
        Task t = this;
        while (t != null) {
            if (pred.test(t)) return true;
            t = t.sub;
        }
        return false;
    }
    
    public boolean thisOrChildAreTimedOut() {
        return thisOrChildSatisfies(TimeoutWanderTask.class::isInstance);
    }
    
    /**
     * Sometimes a task just can NOT be bothered to be interrupted right now. For instance, if we're in mid air and MUST complete the
     * parkour movement.
     */
    private boolean canBeInterrupted(AltoClef mod, Task subTask, Task toInterruptWith) {
        if (subTask == null) return true;
        if (subTask.thisOrChildSatisfies(task -> task instanceof ITaskRequiresGrounded)) {
            // This task (or any of its children) REQUIRES we be grounded or in water or something.
            if (toInterruptWith instanceof ITaskOverridesGrounded) return true;
            return (mod.getPlayer().isOnGround() || mod.getPlayer().isSwimming() || mod.getPlayer().isTouchingWater() ||
                    mod.getPlayer().isClimbing());
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = debugState != null ? debugState.hashCode() : 0;
        result = 31 * result + (sub != null ? sub.hashCode() : 0);
        result = 31 * result + (first ? 1 : 0);
        result = 31 * result + (stopped ? 1 : 0);
        result = 31 * result + (active ? 1 : 0);
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Task) {
            return isEqual((Task) obj);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "<" + toDebugString() + "> " + debugState;
    }
}
