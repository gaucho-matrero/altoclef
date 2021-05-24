package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.utils.input.Input;
import net.minecraft.util.math.BlockPos;


public class GetToBlockTask extends Task implements ITaskRequiresGrounded {
    private static final TimeoutWanderTask wanderTask = new TimeoutWanderTask(10, true);
    private final BlockPos position;
    private final boolean rightClickOnArrival;
    private final boolean preferStairs;
    private final MovementProgressChecker moveChecker = new MovementProgressChecker(10, 1, 5, 0.1);
    private boolean running;
    
    public GetToBlockTask(BlockPos position, boolean rightClickOnArrival, boolean preferStairs) {
        this.position = position;
        this.rightClickOnArrival = rightClickOnArrival;
        this.preferStairs = preferStairs;
    }
    
    public GetToBlockTask(BlockPos position, boolean rightClickOnArrival) {
        this(position, rightClickOnArrival, false);
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        if (rightClickOnArrival) {
            return running && !mod.getCustomBaritone().getInteractWithBlockPositionProcess().failed() &&
                   !mod.getCustomBaritone().getInteractWithBlockPositionProcess().isActive();
        } else {
            return position.isWithinDistance(mod.getPlayer().getPos(), 1);
        }
    }
    
    @Override
    protected void onStart(AltoClef mod) {
        if (preferStairs) {
            mod.getConfigState().push();
            mod.getConfigState().setPreferredStairs(true);
        }
        
        startProc(mod);
        moveChecker.reset();
        wanderTask.resetWander();
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        
        // Wander
        if (wanderTask.isActive() && !wanderTask.isFinished(mod)) {
            setDebugState("Wandering...");
            moveChecker.reset();
            return wanderTask;
        }
        
        if (!procActive(mod)) {
            Debug.logWarning("Restarting interact with block...");
            startProc(mod);
            running = true;
        }
        // Check for failure
        boolean failed = false;
        if (!moveChecker.check(mod)) {
            return wanderTask;
        }
        // Baritone task
        setDebugState("Going to block.");
        return null;
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        running = false;
        stopProc(mod);
        if (preferStairs) {
            mod.getConfigState().pop();
        }
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GetToBlockTask) {
            GetToBlockTask other = (GetToBlockTask) obj;
            if (other.position == null) return true;
            return other.position.equals(position) && other.rightClickOnArrival == rightClickOnArrival;
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Getting to block " + position;
    }
    
    private boolean procActive(AltoClef mod) {
        if (rightClickOnArrival) {
            return mod.getCustomBaritone().getInteractWithBlockPositionProcess().isActive() &&
                   !mod.getCustomBaritone().getInteractWithBlockPositionProcess().failed();
        } else {
            return mod.getClientBaritone().getCustomGoalProcess().isActive();
        }
    }
    
    private void startProc(AltoClef mod) {
        if (rightClickOnArrival) {
            mod.getCustomBaritone().getInteractWithBlockPositionProcess().getToBlock(position, Input.CLICK_RIGHT);
        } else {
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalTwoBlocks(position));
        }
    }
    
    private void stopProc(AltoClef mod) {
        if (!mod.inGame()) return;
        if (rightClickOnArrival) {
            mod.getCustomBaritone().getInteractWithBlockPositionProcess().onLostControl();
        } else {
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        }
    }
}
