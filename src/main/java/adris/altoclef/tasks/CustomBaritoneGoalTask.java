package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.Goal;


public abstract class CustomBaritoneGoalTask extends Task implements ITaskRequiresGrounded {
    private final boolean wander;
    private final Task wanderTask = new TimeoutWanderTask(10);
    protected Goal cachedGoal;
    protected MovementProgressChecker checker = new MovementProgressChecker();

    public CustomBaritoneGoalTask(boolean wander) {
        this.wander = wander;
    }

    public CustomBaritoneGoalTask() {
        this(true);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return cachedGoal != null && cachedGoal.isInGoal(mod.getPlayer().getBlockPos());
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        checker.reset();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (wander) {
            if (wanderTask.isActive() && !wanderTask.isFinished(mod)) {
                setDebugState("Wandering...");
                checker.reset();
                return wanderTask;
            }
            if (!checker.check(mod)) {
                Debug.logMessage("Failed to make progress on goal, wandering.");
                return wanderTask;
            }
        }

        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
            cachedGoal = newGoal(mod);
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(cachedGoal);
        }
        setDebugState("Completing goal.");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    protected abstract Goal newGoal(AltoClef mod);
}
