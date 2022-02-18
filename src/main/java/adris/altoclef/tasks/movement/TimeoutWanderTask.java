package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalRunAway;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Call this when the place you're currently at is bad for some reason and you just wanna get away.
 */
public class TimeoutWanderTask extends Task implements ITaskRequiresGrounded {

    private static final HashSet<Block> ANNOYING_STUCK_BLOCKS;
    static {
        ANNOYING_STUCK_BLOCKS = new HashSet<>();
        ANNOYING_STUCK_BLOCKS.addAll(Arrays.stream(ItemHelper.WOOD_FENCE).map(Block::getBlockFromItem).toList());
        //ANNOYING_STUCK_BLOCKS.addAll(Arrays.stream(ItemHelper.WOOD_DOOR).map(Block::getBlockFromItem).toList());
        ANNOYING_STUCK_BLOCKS.addAll(Arrays.stream(ItemHelper.FLOWER).map(Block::getBlockFromItem).toList());
        ANNOYING_STUCK_BLOCKS.addAll(Arrays.asList(Blocks.VINE,
                Blocks.NETHER_SPROUTS,
                Blocks.CAVE_VINES,
                Blocks.CAVE_VINES_PLANT,
                Blocks.TWISTING_VINES,
                Blocks.TWISTING_VINES_PLANT,
                Blocks.WEEPING_VINES_PLANT,
                Blocks.LADDER,
                Blocks.BIG_DRIPLEAF,
                Blocks.BIG_DRIPLEAF_STEM,
                Blocks.SMALL_DRIPLEAF,
                Blocks.TALL_GRASS,
                Blocks.SNOW,
                Blocks.GRASS
        ));
    }

    private final float _distanceToWander;
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private final boolean _increaseRange;
    //private DistanceProgressChecker _distanceProgressChecker = new DistanceProgressChecker(10, 0.1f);

    private Vec3d _origin;
    private boolean _executingPlanB = false;
    private boolean _forceExplore;
    private Task _unstuckTask = null;
    private int _failCounter;
    private double _wanderDistanceExtension;

    public TimeoutWanderTask(float distanceToWander, boolean increaseRange) {
        _distanceToWander = distanceToWander;
        _increaseRange = increaseRange;
        _forceExplore = false;
    }

    public TimeoutWanderTask(float distanceToWander) {
        this(distanceToWander, false);
    }

    public TimeoutWanderTask() {
        this(Float.POSITIVE_INFINITY, false);
    }

    public TimeoutWanderTask(boolean forceExplore) {
        this();
        _forceExplore = forceExplore;
    }

    private static BlockPos[] generateSides(BlockPos pos) {
        return new BlockPos[]{
                pos.add(1, 0, 0),
                pos.add(-1, 0, 0),
                pos.add(0, 0, 1),
                pos.add(0, 0, -1),
        };
    }

    public void resetWander() {
        _executingPlanB = false;
        _wanderDistanceExtension = 0;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _origin = mod.getPlayer().getPos();
        _progressChecker.reset();
        _failCounter = 0;
        StorageHelper.closeScreen();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (_unstuckTask != null && _unstuckTask.isActive() && !_unstuckTask.isFinished(mod) && stuckInBlock(mod) != null) {
            setDebugState("Getting unstuck from block.");
            // Stop other tasks, we are JUST shimmying
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
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
                mod.getClientBaritone().getExploreProcess().explore((int) _origin.getX(), (int) _origin.getZ());
            }
        }

        //_distanceProgressChecker.setProgress(mod.getPlayer().getPos());
        //if (_distanceProgressChecker.failed()) {
        if (!_progressChecker.check(mod)) {
            // We failed at exploring.
            //_distanceProgressChecker.reset();
            _progressChecker.reset();

            BlockPos blockStuck = stuckInBlock(mod);
            if (blockStuck != null) {
                _failCounter++;
                Debug.logMessage("Failed exploring, found stuck block nearby.");
                _unstuckTask = getFenceUnstuckTask(mod, blockStuck);
                return _unstuckTask;
            }

            if (!_forceExplore) {
                _failCounter++;
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
        double distance = Float.isInfinite(_distanceToWander) ? _distanceToWander : _distanceToWander + _wanderDistanceExtension;
        return new GoalRunAway(distance, mod.getPlayer().getBlockPos());
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getExploreProcess().onLostControl();
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        if (isFinished(mod)) {
            if (_increaseRange) {
                _wanderDistanceExtension += _distanceToWander;
                Debug.logMessage("Increased wander range");
            }
        }
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        // Why the heck did I add this in?
        //if (_origin == null) return true;

        if (Float.isInfinite(_distanceToWander)) return false;

        // If we fail 10 times or more, we may as well try the previous task again.
        if (_failCounter > 10) {
            return true;
        }

        if (mod.getPlayer() != null && mod.getPlayer().getPos() != null) {
            double sqDist = mod.getPlayer().getPos().squaredDistanceTo(_origin);
            double toWander = _distanceToWander + _wanderDistanceExtension;
            return sqDist > toWander * toWander;
        } else {
            return false;
        }
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof TimeoutWanderTask task) {
            if (Float.isInfinite(task._distanceToWander) || Float.isInfinite(_distanceToWander)) {
                return Float.isInfinite(task._distanceToWander) == Float.isInfinite(_distanceToWander);
            }
            return Math.abs(task._distanceToWander - _distanceToWander) < 0.5f;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Wander for " + (_distanceToWander + _wanderDistanceExtension) + " blocks";
    }

    // This happens all the time in mineshafts and swamps/jungles
    private BlockPos stuckInBlock(AltoClef mod) {
        BlockPos p = mod.getPlayer().getBlockPos();
        if (isAnnoying(mod, p)) return p;
        if (isAnnoying(mod, p.up())) return p.up();
        BlockPos[] toCheck = generateSides(p);
        for (BlockPos check : toCheck) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }
        BlockPos[] toCheckHigh = generateSides(p.up());
        for (BlockPos check : toCheckHigh) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }
        return null;
    }

    private boolean isAnnoying(AltoClef mod, BlockPos pos) {
        return ANNOYING_STUCK_BLOCKS.contains(mod.getWorld().getBlockState(pos).getBlock());
        //mod.getWorld().getBlockState(pos).getBlock() instanceof FenceBlock;
    }

    private Task getFenceUnstuckTask(AltoClef mod, BlockPos fencePos) {
        return new SafeRandomShimmyTask();
    }
}
