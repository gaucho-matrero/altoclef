package adris.altoclef.trackers;

import adris.altoclef.AltoClef;

import java.util.ArrayList;

public class TrackerManager {

    private ArrayList<Tracker> _trackers = new ArrayList<>();

    private AltoClef _mod;

    private boolean _wasInGame = false;

    public TrackerManager(AltoClef mod) {
        _mod = mod;
    }

    public void tick() {
        boolean inGame = _mod.inGame();
        if (!inGame && _wasInGame) {
            // Reset when we leave our world
            for (Tracker tracker : _trackers) {
                tracker.reset();
            }
            // This is a bit of a spaghetti dependency but it's ok for now.
            _mod.getChunkTracker().reset(_mod);
        }
        _wasInGame = inGame;

        for(Tracker tracker : _trackers) {
            tracker.setDirty();
        }
    }

    public void addTracker(Tracker tracker) {
        tracker._mod =_mod;
        _trackers.add(tracker);
    }
}
