package adris.altoclef.trackers;


import adris.altoclef.AltoClef;


public abstract class Tracker {
    protected AltoClef mod;
    // Needs to update
    private boolean dirty = true;

    protected Tracker(TrackerManager manager) {
        manager.addTracker(this);
    }

    public void setDirty() {
        dirty = true;
    }

    protected void ensureUpdated() {
        if (dirty) {
            updateState();
            dirty = false;
        }
    }

    protected abstract void updateState();

    protected abstract void reset();
}
