package adris.altoclef.util.progresscheck;


import adris.altoclef.util.csharpisbetter.Timer;


/**
 * Simple progress checker that requires we always make progress.
 */
public class LinearProgressChecker implements IProgressChecker<Double> {

    private final double minProgress;
    private final Timer timer;

    private double lastProgress;
    private double currentProgress;

    private boolean first;

    private boolean failed;

    public LinearProgressChecker(double timeout, double minProgress) {
        this.minProgress = minProgress;
        timer = new Timer(timeout);
        reset();
    }

    @Override
    public void setProgress(Double progress) {
        currentProgress = progress;
        if (first) {
            lastProgress = progress;
            first = false;
        }
        if (timer.elapsed()) {
            double improvement = progress - lastProgress;
            if (improvement < minProgress) {
                failed = true;
            }
            first = false;
            timer.reset();
            lastProgress = progress;
        }
    }

    @Override
    public boolean failed() {
        return failed;
    }

    public void reset() {
        //first = true;
        failed = false;
        timer.reset();
        first = true;
    }
}
