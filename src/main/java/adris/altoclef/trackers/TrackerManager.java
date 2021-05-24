package adris.altoclef.trackers;


import adris.altoclef.AltoClef;

import java.util.ArrayList;


public class TrackerManager {
    private final ArrayList<Tracker> trackers = new ArrayList<>();
    private final AltoClef mod;
    private boolean wasInGame;
    
    public TrackerManager(AltoClef mod) {
        this.mod = mod;
    }
    
    public void tick() {
        boolean inGame = mod.inGame();
        if (!inGame && wasInGame) {
            // Reset when we leave our world
            for (Tracker tracker : trackers) {
                tracker.reset();
            }
            // This is a bit of a spaghetti dependency but it's ok for now.
            mod.getChunkTracker().reset(mod);
        }
        wasInGame = inGame;
        
        for (Tracker tracker : trackers) {
            tracker.setDirty();
        }
    }
    
    public void addTracker(Tracker tracker) {
        tracker.mod = mod;
        trackers.add(tracker);
    }
}
