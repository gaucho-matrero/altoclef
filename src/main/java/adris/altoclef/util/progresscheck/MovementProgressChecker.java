package adris.altoclef.util.progresscheck;


import adris.altoclef.AltoClef;
import adris.altoclef.util.WorldUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


public class MovementProgressChecker {
    
    private final IProgressChecker<Vec3d> distanceChecker;
    private final IProgressChecker<Double> mineChecker;
    
    private BlockPos lastBreakingBlock;
    
    public MovementProgressChecker(double distanceTimeout, double minDistance, double mineTimeout, double minMineProgress, int attempts) {
        distanceChecker = new ProgressCheckerRetry<>(new DistanceProgressChecker(distanceTimeout, minDistance), attempts);
        mineChecker = new LinearProgressChecker(mineTimeout, minMineProgress);
    }
    
    public MovementProgressChecker(double distanceTimeout, double minDistance, double mineTimeout, double minMineProgress) {
        this(distanceTimeout, minDistance, mineTimeout, minMineProgress, 1);
    }
    
    public MovementProgressChecker(int attempts) {
        this(4, 0.1, 0.5, 0.001, attempts);
    }
    
    public MovementProgressChecker() {
        this(1);
    }
    
    public boolean check(AltoClef mod) {
        
        // Allow pause on eat
        if (mod.getFoodTaskChain().isTryingToEat()) {
            distanceChecker.reset();
            mineChecker.reset();
        }
        
        if (mod.getControllerExtras().isBreakingBlock()) {
            BlockPos breakBlock = mod.getControllerExtras().getBreakingBlockPos();
            // If we broke a block, we made progress.
            // We must also delay reseting the distance checker UNTIL we break a block.
            // Because otherwise we risk not failing if we keep retrtying to mine and don't succeed.
            if (lastBreakingBlock != null && WorldUtil.isAir(mod, lastBreakingBlock)) {
                distanceChecker.reset();
                mineChecker.reset();
            }
            lastBreakingBlock = breakBlock;
            mineChecker.setProgress(mod.getControllerExtras().getBreakingBlockProgress());
            return !mineChecker.failed();
        } else {
            mineChecker.reset();
            distanceChecker.setProgress(mod.getPlayer().getPos());
            return !distanceChecker.failed();
        }
    }
    
    public void reset() {
        distanceChecker.reset();
        mineChecker.reset();
    }
    
}
