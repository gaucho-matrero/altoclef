package adris.altoclef.util.progresscheck;

import adris.altoclef.Debug;
import net.minecraft.util.math.Vec3d;

public class DistanceProgressChecker implements IProgressChecker<Vec3d> {

    private Vec3d _start;
    private Vec3d _prevPos;

    private IProgressChecker<Double> _distanceChecker;

    public DistanceProgressChecker(double timeout, IProgressChecker<Double> distanceChecker) {
        _distanceChecker = distanceChecker;
        reset();
    }
    public DistanceProgressChecker(double timeout, double minDistanceToMake) {
        this(timeout, new LinearProgressChecker(timeout, minDistanceToMake));
    }

    @Override
    public void setProgress(Vec3d position) {
        double delta = position.distanceTo(_start);
        Debug.logMessage("DELTA: " + delta);
        _prevPos = position;
        _distanceChecker.setProgress(delta);
    }

    @Override
    public boolean failed() {
        return _distanceChecker.failed();
    }

    public void reset(Vec3d startPos) {
        _prevPos = startPos;
        reset();
    }

    @Override
    public void reset() {
        _start = _prevPos;
        _distanceChecker.reset();
    }
}
