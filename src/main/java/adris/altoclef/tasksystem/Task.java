package adris.altoclef.tasksystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

public abstract class Task {

    private String _debugState = "";

    private Task _sub = null;

    private boolean _first = true;

    private boolean _failed = false;

    private boolean _active = false;

    public void tick(AltoClef mod, TaskChain parentChain) {
        parentChain.addTaskToChain(this);
        if (_first) {
            Debug.logInternal("Task START: " + this.toString());
            _active = true;
            onStart(mod);
            _first = false;
            _failed = false;
        }
        if (_failed) return;

        Task newSub = onTick(mod);
        // We have a sub task
        if (newSub != null) {
            if (!newSub.isEqual(_sub)) {
                // Our sub task is new
                if (_sub != null) {
                    // Our previous sub must be interrupted.
                    _sub.stop(mod, newSub);
                }

                _sub = newSub;
            }

            // Run our child
            _sub.tick(mod, parentChain);
        } else {
            // We are null
            if (_sub != null) {
                // Our previous sub must be interrupted.
                _sub.stop(mod);
                _sub = null;
            }
        }
    }

    public void reset() {
        _first = true;
        _active = true;
        _failed = false;
    }

    protected void stop(AltoClef mod, Task interruptTask) {
        onStop(mod, interruptTask);
        Debug.logInternal("Task STOP: " + this.toString() + ", interrupted by " + interruptTask);

        if (_sub != null && !_sub.failed()) {
            _sub.stop(mod, interruptTask);
        }

        _first = true;
        _active = false;
    }

    protected boolean taskAssert(AltoClef mod, boolean condition, String message) {
        if (!condition && !_failed) {
            Debug.logError("Task assertion failed: " + message);
            stop(mod);
            _failed = true;
        }
        return condition;
    }

    public void stop(AltoClef mod) {
        stop(mod,null);
    }

    protected void setDebugState(String state) {
        if (!_debugState.equals(state)) {
            _debugState = state;
            Debug.logInternal(toString());
        } else {
            _debugState = state;
        }
    }

    // Virtual
    public boolean isFinished(AltoClef mod) {
        return false;
    }

    public boolean isActive() {return _active;}

    public boolean failed() {return _failed;}

    protected abstract void onStart(AltoClef mod);

    protected abstract Task onTick(AltoClef mod);

    // interruptTask = null if the task stopped cleanly
    protected abstract void onStop(AltoClef mod, Task interruptTask);

    protected abstract boolean isEqual(Task obj);

    protected abstract String toDebugString();

    @Override
    public String toString() {
        return "<" + toDebugString() + "> " + _debugState;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Task) {
            return isEqual((Task) obj);
        }
        return false;
    }
}
