package adris.altoclef.util.progresscheck;

import net.minecraft.util.math.Vec3d;

public class DistanceProgressChecker implements IProgressChecker<Vec3d> {

    private Vec3d _start;
    private Vec3d _prevPos;

    private IProgressChecker<Double> _distanceChecker;

    public DistanceProgressChecker(double timeout, double minDistanceToMake) {
        _distanceChecker = new LinearProgressChecker(timeout, minDistanceToMake);
        reset();
    }

    @Override
    public void setProgress(Vec3d progress) {
        double delta = progress.distanceTo(_start) - _prevPos.distanceTo(_start);
        _prevPos = progress;
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
