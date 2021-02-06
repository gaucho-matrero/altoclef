package adris.altoclef.util.progresscheck;

import adris.altoclef.AltoClef;
import net.minecraft.util.math.Vec3d;

public class MovementProgressChecker {

    private final IProgressChecker<Vec3d> _distanceChecker;
    private final IProgressChecker<Double> _mineChecker;

    public MovementProgressChecker(double distanceTimeout, double minDistance, double mineTimeout, double minMineProgress, int moveRetries) {
        _distanceChecker = new ProgressCheckerRetry<>(new DistanceProgressChecker(distanceTimeout, minDistance), moveRetries);
        _mineChecker = new LinearProgressChecker(mineTimeout, minMineProgress);
    }
    public MovementProgressChecker(double distanceTimeout, double minDistance, double mineTimeout, double minMineProgress) {
        this(distanceTimeout, minDistance, mineTimeout, minMineProgress, 1);
    }

    public boolean check(AltoClef mod) {
        if (mod.getController().isBreakingBlock()) {
            _distanceChecker.reset();
            _mineChecker.setProgress(mod.getControllerExtras().getBreakingBlockProgress());
            if (_mineChecker.failed()) return false;
        } else {
            _mineChecker.reset();
            _distanceChecker.setProgress(mod.getPlayer().getPos());
            if (_distanceChecker.failed()) return false;
        }
        return true;
    }

    public void reset() {
        _distanceChecker.reset();
        _mineChecker.reset();
    }

}
