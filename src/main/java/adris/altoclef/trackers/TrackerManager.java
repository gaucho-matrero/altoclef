package adris.altoclef.trackers;

import adris.altoclef.AltoClef;

import java.util.ArrayList;

public class TrackerManager {

    private ArrayList<Tracker> _trackers = new ArrayList<>();

    private AltoClef _mod;

    public TrackerManager(AltoClef mod) {
        _mod = mod;
    }

    public void tick() {
        for(Tracker tracker : _trackers) {
            tracker.setDirty();
        }
    }

    public void addTracker(Tracker tracker) {
        tracker._mod =_mod;
        _trackers.add(tracker);
    }
}
