package adris.altoclef.util;

// Simple timer
public class Timer {

    private double _prevTime = 0;
    private double _interval;

    public Timer(double interval) {
        _interval = interval;
    }

    public double getDuration() {
        return currentTime() - _prevTime;
    }

    public boolean elapsed() {
        return getDuration() > _interval;
    }

    public void reset() {
        _prevTime = currentTime();
    }

    private double currentTime() {
        return (double) System.currentTimeMillis() / 1000.0;
    }
}
