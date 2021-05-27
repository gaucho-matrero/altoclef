package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.LookUtil;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.GoalRunAway;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;


public abstract class AbstractDoToEntityTask extends Task implements ITaskRequiresGrounded {
    protected final MovementProgressChecker progress = new MovementProgressChecker(5, 0.1, 5, 0.001, 2);
    private final double maintainDistance;
    private final double combatGuardLowerRange;
    private final double combatGuardLowerFieldRadius;
    private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(10);

    public AbstractDoToEntityTask(double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        this.maintainDistance = maintainDistance;
        this.combatGuardLowerRange = combatGuardLowerRange;
        this.combatGuardLowerFieldRadius = combatGuardLowerFieldRadius;
    }

    public AbstractDoToEntityTask(double maintainDistance) {
        this(maintainDistance, 0, Double.POSITIVE_INFINITY);
    }

    @Override
    protected void onStart(AltoClef mod) {
        wanderTask.resetWander();
        progress.reset();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (wanderTask.isActive() && !wanderTask.isFinished(mod)) {
            progress.reset();
            setDebugState("Failed to get to target, wandering for a bit.");
            return wanderTask;
        }

        Entity entity = getEntityTarget(mod);

        mod.getMobDefenseChain().setTargetEntity(entity);

        // Oof
        if (entity == null) {
            mod.getMobDefenseChain().resetForceField();
            return null;
        }

        double playerReach = mod.getClientBaritone().getPlayerContext().playerController().getBlockReachDistance();

        // TODO: This is basically useless.
        EntityHitResult result = LookUtil.raycast(mod.getPlayer(), entity, playerReach);

        double sqDist = entity.squaredDistanceTo(mod.getPlayer());

        if (sqDist < combatGuardLowerRange * combatGuardLowerRange) {
            mod.getMobDefenseChain().setForceFieldRange(combatGuardLowerFieldRadius);
        } else {
            mod.getMobDefenseChain().resetForceField();
        }

        boolean tooClose = sqDist < maintainDistance * maintainDistance;
        // Step away if we're too close
        if (tooClose) {
            //setDebugState("Maintaining distance");
            if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalRunAway(maintainDistance, entity.getBlockPos()));
            }
        }

        if (entity.squaredDistanceTo(mod.getPlayer()) < playerReach * playerReach && result != null &&
            result.getType() == HitResult.Type.ENTITY) {
            progress.reset();
            return onEntityInteract(mod, entity);
        } else if (!tooClose) {
            setDebugState("Approaching target");

            if (!progress.check(mod)) {
                Debug.logMessage("Failed to get to target, wandering.");
                return wanderTask;
            }

            // Move to target

            return new GetToEntityTask(entity, maintainDistance);
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getMobDefenseChain().setTargetEntity(null);
        mod.getMobDefenseChain().resetForceField();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof AbstractDoToEntityTask) {
            AbstractDoToEntityTask task = (AbstractDoToEntityTask) obj;
            if (!doubleCheck(task.maintainDistance, maintainDistance)) return false;
            if (!doubleCheck(task.combatGuardLowerFieldRadius, combatGuardLowerFieldRadius)) return false;
            if (!doubleCheck(task.combatGuardLowerRange, combatGuardLowerRange)) return false;
            return isSubEqual(task);
        }
        return false;
    }

    private boolean doubleCheck(double a, double b) {
        if (Double.isInfinite(a) == Double.isInfinite(b)) return true;
        return Math.abs(a - b) < 0.1;
    }

    protected abstract boolean isSubEqual(AbstractDoToEntityTask other);

    protected abstract Task onEntityInteract(AltoClef mod, Entity entity);

    protected abstract Entity getEntityTarget(AltoClef mod);

}
