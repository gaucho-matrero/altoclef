package adris.altoclef.util.csharpisbetter;

public class Stopwatch {

    private double _startTime = 0;
    boolean _running = false;

    public void begin() {
        _startTime = Timer.currentTime();
        _running = true;
    }

    public double time() {
        if (!_running) return 0;
        return Timer.currentTime() - _startTime;
    }
}
