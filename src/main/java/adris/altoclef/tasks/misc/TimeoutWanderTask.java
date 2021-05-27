package adris.altoclef.tasks.misc;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalRunAway;
import net.minecraft.block.FenceBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


/**
 * Call this when the place you're currently at is bad for some reason and you just wanna get away.
 */

public class TimeoutWanderTask extends Task implements ITaskRequiresGrounded {
    private final float distanceToWander;
    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private final boolean increaseRange;
    //private DistanceProgressChecker _distanceProgressChecker = new DistanceProgressChecker(10, 0.1f);
    private Vec3d origin;
    private boolean executingPlanB;
    private boolean forceExplore;
    private Task unstuckTask;
    private int failCounter;
    private double wanderDistanceExtension;

    public TimeoutWanderTask(float distanceToWander, boolean increaseRange) {
        this.distanceToWander = distanceToWander;
        this.increaseRange = increaseRange;
        forceExplore = false;
    }

    public TimeoutWanderTask(float distanceToWander) {
        this(distanceToWander, false);
    }

    public TimeoutWanderTask() {
        this(Float.POSITIVE_INFINITY, false);
    }

    public TimeoutWanderTask(boolean forceExplore) {
        this();
        this.forceExplore = forceExplore;
    }

    private static BlockPos[] generateSides(BlockPos pos) {
        return new BlockPos[]{
                pos.add(1, 0, 0), pos.add(-1, 0, 0), pos.add(0, 0, 1), pos.add(0, 0, -1),
                };
    }

    public void resetWander() {
        executingPlanB = false;
        wanderDistanceExtension = 0;
    }

    private Goal getRandomDirectionGoal(AltoClef mod) {
        double distance = Float.isInfinite(distanceToWander) ? distanceToWander : distanceToWander + wanderDistanceExtension;
        return new GoalRunAway(distance, mod.getPlayer().getBlockPos());
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if (origin == null) return true;

        if (Float.isInfinite(distanceToWander)) return false;

        // If we fail 10 times or more, we may as well try the previous task again.
        if (failCounter > 10) {
            return true;
        }

        if (mod.getPlayer() != null && mod.getPlayer().getPos() != null) {
            double sqDist = mod.getPlayer().getPos().squaredDistanceTo(origin);
            double toWander = distanceToWander + wanderDistanceExtension;
            return sqDist > toWander * toWander;
        } else {
            return false;
        }
    }

    @Override
    protected void onStart(AltoClef mod) {
        origin = mod.getPlayer().getPos();
        progressChecker.reset();
        failCounter = 0;
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (unstuckTask != null && unstuckTask.isActive() && !unstuckTask.isFinished(mod) && stuckInFence(mod) != null) {
            setDebugState("Getting unstuck from fence. Yes this happens.");
            return unstuckTask;
        }

        if (executingPlanB) {
            setDebugState("Plan B: Random direction.");
            if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(getRandomDirectionGoal(mod));
            }
        } else {
            setDebugState("Exploring.");
            if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                mod.getClientBaritone().getExploreProcess().explore((int) origin.getX(), (int) origin.getZ());
            }
        }

        //_distanceProgressChecker.setProgress(mod.getPlayer().getPos());
        //if (_distanceProgressChecker.failed()) {
        if (!progressChecker.check(mod)) {
            // We failed at exploring.
            //_distanceProgressChecker.reset();
            progressChecker.reset();

            BlockPos fenceStuck = stuckInFence(mod);
            if (fenceStuck != null) {
                failCounter++;
                Debug.logMessage("Failed exploring, found fence nearby.");
                unstuckTask = getFenceUnstuckTask(mod, fenceStuck);
                return unstuckTask;
            }

            if (!forceExplore) {
                failCounter++;
                Debug.logMessage("Failed exploring.");
                if (executingPlanB) {
                    // Cancel current plan B
                    mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                }
                executingPlanB = true;
            }
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getExploreProcess().onLostControl();
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        if (isFinished(mod)) {
            if (increaseRange) {
                wanderDistanceExtension += distanceToWander;
                Debug.logMessage("Increased wander range");
            }
        }
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof TimeoutWanderTask) {
            TimeoutWanderTask other = (TimeoutWanderTask) obj;
            if (Float.isInfinite(other.distanceToWander) || Float.isInfinite(distanceToWander)) {
                return Float.isInfinite(other.distanceToWander) == Float.isInfinite(distanceToWander);
            }
            return Math.abs(other.distanceToWander - distanceToWander) < 0.5f;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Wander for " + (distanceToWander + wanderDistanceExtension) + " blocks";
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
        //Debug.logMessage("oof: " + p.toShortString());
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
        return new DestroyBlockTask(fencePos);
    }
}
