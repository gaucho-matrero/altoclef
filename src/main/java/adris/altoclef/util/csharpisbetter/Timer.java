package adris.altoclef.util.csharpisbetter;

// Simple timer
public class Timer {
    
    private double _prevTime = 0;
    private double _interval;
    
    public Timer(double interval) {
        _interval = interval;
    }
    
    public static double currentTime() {
        return (double) System.currentTimeMillis() / 1000.0;
    }
    
    public double getDuration() {
        return currentTime() - _prevTime;
    }
    
    public void setInterval(double interval) {_interval = interval;}
    
    public boolean elapsed() {
        return getDuration() > _interval;
    }
    
    public void reset() {
        _prevTime = currentTime();
    }
    
    public void forceElapse() {
        _prevTime = 0;
    }
}
