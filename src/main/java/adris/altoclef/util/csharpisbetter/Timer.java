package adris.altoclef.util.csharpisbetter;


// Simple timer
public class Timer {

    private double prevTime;
    private double interval;

    public Timer(double interval) {
        this.interval = interval;
    }

    public static double currentTime() {
        return System.currentTimeMillis() / 1000.0d;
    }

    public double getDuration() {
        return currentTime() - prevTime;
    }

    public void setInterval(double interval) {
        this.interval = interval;
    }

    public boolean elapsed() {
        return getDuration() > interval;
    }

    public void reset() {
        prevTime = currentTime();
    }

    public void forceElapse() {
        prevTime = 0;
    }
}
