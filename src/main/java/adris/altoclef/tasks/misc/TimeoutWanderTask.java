package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.DistanceProgressChecker;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalRunAway;
import net.minecraft.util.math.Vec3d;

/**
 * Call this when the place you're currently at is bad for some reason and you just wanna get away.
 */

public class TimeoutWanderTask extends Task {

    private final float _distanceToWander;

    private Vec3d _origin;

    private DistanceProgressChecker _distanceProgressChecker = new DistanceProgressChecker(10, 0.1f);

    private boolean _executingPlanB = false;

    private boolean _forceExplore;

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

        if (!_forceExplore) {
            _distanceProgressChecker.setProgress(mod.getPlayer().getPos());
            if (_distanceProgressChecker.failed()) {
                _distanceProgressChecker.reset();
                // We failed at exploring.
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
    }

    @Override
    public boolean isFinished(AltoClef mod) {

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
}
