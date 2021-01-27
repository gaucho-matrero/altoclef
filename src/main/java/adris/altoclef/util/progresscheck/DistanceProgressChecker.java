package adris.altoclef.util.progresscheck;

import net.minecraft.util.math.Vec3d;

public class DistanceProgressChecker implements IProgressChecker<Vec3d> {

    private Vec3d _start;
    private Vec3d _prevPos;

    private final IProgressChecker<Double> _distanceChecker;

    public DistanceProgressChecker(IProgressChecker<Double> distanceChecker) {
        _distanceChecker = distanceChecker;
        reset();
    }
    public DistanceProgressChecker(double timeout, double minDistanceToMake) {
        this(new LinearProgressChecker(timeout, minDistanceToMake));
    }

    @Override
    public void setProgress(Vec3d position) {
        if (_start == null) {
            _start = position;
            return;
        }
        double delta = position.distanceTo(_start);
        _prevPos = position;
        _distanceChecker.setProgress(delta);
    }

    @Override
    public boolean failed() {
        return _distanceChecker.failed();
    }

    @Override
    public void reset() {
        _start = null;//_prevPos;
        _distanceChecker.reset();
    }
}
