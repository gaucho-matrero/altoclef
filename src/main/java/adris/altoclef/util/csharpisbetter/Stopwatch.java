package adris.altoclef.util.csharpisbetter;


public class Stopwatch {
    private boolean running;
    private double startTime;

    public void begin() {
        startTime = Timer.currentTime();
        running = true;
    }

    public double time() {
        if (!running) return 0;
        return Timer.currentTime() - startTime;
    }
}
