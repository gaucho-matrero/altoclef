package adris.altoclef.trackers;

import adris.altoclef.AltoClef;

public abstract class Tracker {

    protected AltoClef _mod;

    public Tracker(TrackerManager manager) {
        manager.addTracker(this);
    }

    // Needs to update
    private boolean _dirty = true;

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
}
