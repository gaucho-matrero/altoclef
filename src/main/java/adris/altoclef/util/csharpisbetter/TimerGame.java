package adris.altoclef.util.csharpisbetter;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

// Simple timer
public class TimerGame extends BaseTimer {

    public TimerGame(double intervalSeconds) {
        super(intervalSeconds);
    }

    @Override
    protected double currentTime() {
        if (!AltoClef.inGame()) {
            Debug.logError("Running game timer while not in game.");
            return 0;
        }
        // Use ticks for timing. 20TPS is normal, if we go slower that's fine.
        // Adding a "mod" argument here would be hell across the board. Not happening.
        return (double) AltoClef.getTicks() / 20.0;
    }
}
