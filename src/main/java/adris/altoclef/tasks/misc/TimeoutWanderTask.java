package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.DistanceProgressChecker;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalRunAway;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.FenceBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

/**
 * Call this when the place you're currently at is bad for some reason and you just wanna get away.
 */

public class TimeoutWanderTask extends Task {

    private final float _distanceToWander;

    private Vec3d _origin;

    private DistanceProgressChecker _distanceProgressChecker = new DistanceProgressChecker(10, 0.1f);

    private boolean _executingPlanB = false;

    private boolean _forceExplore;

    private Task _unstuckTask = null;

    public TimeoutWanderTask(float distanceToWander) {
        _distanceToWander = distanceToWander;
        _forceExplore = false;
    }
    public TimeoutWanderTask() {
        this(Float.POSITIVE_INFINITY);
    }
    public TimeoutWanderTask(boolean forceExplore) {
        this();
        _forceExplore = forceExplore;
    }

    public void resetWander() {
        _executingPlanB = false;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _origin = mod.getPlayer().getPos();
        _distanceProgressChecker.reset();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (_unstuckTask != null && _unstuckTask.isActive() && !_unstuckTask.isFinished(mod) && stuckInFence(mod) != null) {
            setDebugState("Getting unstuck from fence. Yes this happens.");
            return _unstuckTask;
        }

        if (_executingPlanB) {
            setDebugState("Plan B: Random direction.");
            if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(getRandomDirectionGoal(mod));
            }
        } else {
            setDebugState("Exploring.");
            if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                mod.getClientBaritone().getExploreProcess().explore((int)_origin.getX(), (int)_origin.getZ());
            }
        }

        _distanceProgressChecker.setProgress(mod.getPlayer().getPos());
        if (_distanceProgressChecker.failed()) {
            // We failed at exploring.
            _distanceProgressChecker.reset();

            BlockPos fenceStuck = stuckInFence(mod);
            if (fenceStuck != null) {
                _unstuckTask = getFenceUnstuckTask(mod, fenceStuck);
                return _unstuckTask;
            }

            if (!_forceExplore) {
                Debug.logMessage("Failed exploring.");
                if (_executingPlanB) {
                    // Cancel current plan B
                    mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                }
                _executingPlanB = true;
            }
        }

        return null;
    }

    private Goal getRandomDirectionGoal(AltoClef mod) {
        double distance = Float.isInfinite(_distanceToWander)? _distanceToWander : _distanceToWander + Math.random() * 25;
        return new GoalRunAway(distance, mod.getPlayer().getBlockPos());
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getExploreProcess().onLostControl();
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if (_origin == null) return true;

        if (Float.isInfinite(_distanceToWander)) return false;

        if (mod.getPlayer() != null && mod.getPlayer().getPos() != null) {
            double sqDist = mod.getPlayer().getPos().squaredDistanceTo(_origin);
            return sqDist > _distanceToWander * _distanceToWander;
        } else {
            return false;
        }
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof TimeoutWanderTask) {
            TimeoutWanderTask other = (TimeoutWanderTask) obj;
            if (Float.isInfinite(other._distanceToWander) || Float.isInfinite(_distanceToWander)) {
                return Float.isInfinite(other._distanceToWander) == Float.isInfinite(_distanceToWander);
            }
            return Math.abs(other._distanceToWander - _distanceToWander) < 0.5f;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Wander for " + _distanceToWander + " blocks";
    }

    // This happens all the time in mineshafts.
    private BlockPos stuckInFence(AltoClef mod) {
        BlockPos p = mod.getPlayer().getBlockPos();
        if (isFence(mod, p)) return p;
        if (isFence(mod, p.up())) return p.up();
        BlockPos[] toCheck = generateSides(p);
        for (BlockPos check : toCheck) {
            if (isFence(mod, check)) {
                return check;
            }
        }
        BlockPos[] toCheckHigh = generateSides(p.up());
        Debug.logMessage("oof: " + p.toShortString());
        for (BlockPos check : toCheckHigh) {
            //Block temp = mod.getWorld().getBlockState(check).getBlock();
            //Debug.logMessage(check.toShortString() + " = " + temp.getTranslationKey() + " : " + temp.getClass());
            if (isFence(mod, check)) {
                return check;
            }
        }
        return null;
    }
    private boolean isFence(AltoClef mod, BlockPos pos) {
        return mod.getWorld().getBlockState(pos).getBlock() instanceof FenceBlock;
    }
    private Task getFenceUnstuckTask(AltoClef mod, BlockPos fencePos) {
        /*
        final BlockPos[] toCheck = generateSides(fencePos);
        List<BlockPos> viableSpots = new ArrayList<>();
        for (BlockPos check : toCheck) {
            if (mod.getWorld().getBlockState(check).getBlock() instanceof AirBlock
            && mod.getWorld().getBlockState(check.up()).getBlock() instanceof AirBlock) {
                viableSpots.add(check);
            }
        }
        BlockPos target;
        // If we find no free spot, force our way out anyway.
        if (viableSpots.size() == 0) {
            target = toCheck[(int) (toCheck.length * Math.random())];
        } else {
            target = viableSpots.get((int) (viableSpots.size() * Math.random()));
        }
        return new GetToBlockTask(target, false);
         */
        return new DestroyBlockTask(fencePos);
    }

    private static BlockPos[] generateSides(BlockPos pos) {
        return new BlockPos[] {
                pos.add(1, 0, 0),
                pos.add(-1, 0, 0),
                pos.add(0, 0, 1),
                pos.add(0, 0, -1),
        };
    }
}
