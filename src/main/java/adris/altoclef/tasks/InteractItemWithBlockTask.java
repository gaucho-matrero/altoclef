package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.baritone.InteractWithBlockPositionProcess;
import adris.altoclef.util.csharpisbetter.Action;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.utils.input.Input;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;


public class InteractItemWithBlockTask extends Task {
    private static final int MAX_REACH = 7;
    public final Action timedOut = new Action();
    private final ItemTarget toUse;
    private final Direction direction;
    private final BlockPos target;
    private final boolean walkInto;
    private final Vec3i interactOffset;
    private final Input interactInput;
    private final boolean shouldShiftClick;
    private final MovementProgressChecker moveChecker = new MovementProgressChecker(4, 0.1, 4, 0.01);
    private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(5);
    private boolean trying;
    private int prevReach = -1;


    public InteractItemWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto,
                                     Vec3i interactOffset, boolean shouldShiftClick) {
        this.toUse = toUse;
        this.direction = direction;
        this.target = target;
        this.interactInput = interactInput;
        this.walkInto = walkInto;
        this.interactOffset = interactOffset;
        this.shouldShiftClick = shouldShiftClick;
    }

    public InteractItemWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto,
                                     boolean shouldShiftClick) {
        this(toUse, direction, target, interactInput, walkInto, Vec3i.ZERO, shouldShiftClick);
    }

    public InteractItemWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, boolean walkInto) {
        this(toUse, direction, target, Input.CLICK_RIGHT, walkInto, true);
    }

    public InteractItemWithBlockTask(ItemTarget toUse, BlockPos target, boolean walkInto, Vec3i interactOffset) {
        // null means any side is OK
        this(toUse, null, target, Input.CLICK_RIGHT, walkInto, interactOffset, true);
    }

    public InteractItemWithBlockTask(ItemTarget toUse, BlockPos target, boolean walkInto) {
        this(toUse, target, walkInto, Vec3i.ZERO);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return trying && !proc(mod).isActive();
    }

    @Override
    protected void onStart(AltoClef mod) {
        trying = false;
        moveChecker.reset();
        wanderTask.resetWander();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (toUse != null && !mod.getInventoryTracker().targetMet(toUse)) {
            moveChecker.reset();
            return TaskCatalogue.getItemTask(toUse);
        }

        // Wander and check
        if (wanderTask.isActive() && !wanderTask.isFinished(mod)) {
            moveChecker.reset();
            return wanderTask;
        }
        if (!moveChecker.check(mod)) {
            Debug.logMessage("Failed, wandering.");
            timedOut.invoke();
            return wanderTask;
        }

        if (proc(mod).isActive()) {
            if (prevReach < MAX_REACH && proc(mod).reachCounter != prevReach) {
                prevReach = proc(mod).reachCounter;
                moveChecker.reset();
            }
        } else {
            trying = true;
            proc(mod).getToBlock(target, direction, interactInput, true, walkInto, interactOffset, shouldShiftClick);
            if (toUse != null) {
                proc(mod).setInteractEquipItem(toUse);
            }
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        proc(mod).onLostControl();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof InteractItemWithBlockTask) {
            InteractItemWithBlockTask task = (InteractItemWithBlockTask) obj;
            if ((task.direction == null) != (direction == null)) return false;
            if (task.direction != null && task.direction != direction) return false;
            if ((task.toUse == null) != (toUse == null)) return false;
            if (task.toUse != null && !task.toUse.equals(toUse)) return false;
            if (!task.target.equals(target)) return false;
            if (task.interactInput != interactInput) return false;
            return task.walkInto == walkInto;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Interact using " + toUse + " at " + target + " dir " + direction;
    }

    private InteractWithBlockPositionProcess proc(AltoClef mod) {
        return mod.getCustomBaritone().getInteractWithBlockPositionProcess();
    }

}
