package adris.altoclef.util.progresscheck;


import net.minecraft.util.math.Vec3d;


public class DistanceProgressChecker implements IProgressChecker<Vec3d> {
    
    private final IProgressChecker<? super Double> distanceChecker;
    private final boolean reduceDistance;
    private Vec3d start;
    private Vec3d prevPos; // TODO: 2021-05-22 what is this for?
    
    public DistanceProgressChecker(IProgressChecker<? super Double> distanceChecker, boolean reduceDistance) {
        this.distanceChecker = distanceChecker;
        this.reduceDistance = reduceDistance;
        if (reduceDistance) {
            this.distanceChecker.setProgress(Double.NEGATIVE_INFINITY);
        }
        reset();
    }
    
    public DistanceProgressChecker(double timeout, double minDistanceToMake, boolean reduceDistance) {
        this(new LinearProgressChecker(timeout, minDistanceToMake), reduceDistance);
    }
    
    public DistanceProgressChecker(double timeout, double minDistanceToMake) {
        this(timeout, minDistanceToMake, false);
    }
    
    @Override
    public void setProgress(Vec3d position) {
        if (start == null) {
            start = position;
            return;
        }
        double delta = position.distanceTo(start);
        // If we want to reduce distance, penalize distance.
        if (reduceDistance) delta *= -1;
        prevPos = position;
        distanceChecker.setProgress(delta);
    }
    
    @Override
    public boolean failed() {
        return distanceChecker.failed();
    }
    
    @Override
    public void reset() {
        start = null;//_prevPos;
        distanceChecker.setProgress(0.0);
        distanceChecker.reset();
    }
}
