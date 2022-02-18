package adris.altoclef.trackers;

import adris.altoclef.AltoClef;

public abstract class Tracker {

    protected AltoClef _mod;
    // Needs to update
    private boolean _dirty = true;

    public Tracker(TrackerManager manager) {
        manager.addTracker(this);
    }

    public void setDirty() {
        _dirty = true;
    }

    protected void ensureUpdated() {
        if (_dirty) {
            updateState();
            _dirty = false;
        }
    }

    protected abstract void updateState();

    protected abstract void reset();
}
