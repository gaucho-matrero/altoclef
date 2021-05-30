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
    protected final MovementProgressChecker _progress = new MovementProgressChecker(5, 0.1, 5, 0.001, 2);
    private final double _maintainDistance;
    private final double _combatGuardLowerRange;
    private final double _combatGuardLowerFieldRadius;
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(10);

    public AbstractDoToEntityTask(double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        _maintainDistance = maintainDistance;
        _combatGuardLowerRange = combatGuardLowerRange;
        _combatGuardLowerFieldRadius = combatGuardLowerFieldRadius;
    }

    public AbstractDoToEntityTask(double maintainDistance) {
        this(maintainDistance, 0, Double.POSITIVE_INFINITY);
    }

    @Override
    protected void onStart(AltoClef mod) {
        _wanderTask.resetWander();
        _progress.reset();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            _progress.reset();
            setDebugState("Failed to get to target, wandering for a bit.");
            return _wanderTask;
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

        if (sqDist < _combatGuardLowerRange * _combatGuardLowerRange) {
            mod.getMobDefenseChain().setForceFieldRange(_combatGuardLowerFieldRadius);
        } else {
            mod.getMobDefenseChain().resetForceField();
        }

        boolean tooClose = sqDist < _maintainDistance * _maintainDistance;
        // Step away if we're too close
        if (tooClose) {
            //setDebugState("Maintaining distance");
            if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalRunAway(_maintainDistance, entity.getBlockPos()));
            }
        }

        if (entity.squaredDistanceTo(mod.getPlayer()) < playerReach * playerReach && result != null && result.getType() == HitResult.Type.ENTITY) {
            _progress.reset();
            return onEntityInteract(mod, entity);
        } else if (!tooClose) {
            setDebugState("Approaching target");

            if (!_progress.check(mod)) {
                Debug.logMessage("Failed to get to target, wandering.");
                return _wanderTask;
            }

            // Move to target

            return new GetToEntityTask(entity, _maintainDistance);
        }

        return null;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof AbstractDoToEntityTask) {
            AbstractDoToEntityTask task = (AbstractDoToEntityTask) obj;
            if (!doubleCheck(task._maintainDistance, _maintainDistance)) return false;
            if (!doubleCheck(task._combatGuardLowerFieldRadius, _combatGuardLowerFieldRadius)) return false;
            if (!doubleCheck(task._combatGuardLowerRange, _combatGuardLowerRange)) return false;
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

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getMobDefenseChain().setTargetEntity(null);
        mod.getMobDefenseChain().resetForceField();
    }

    protected abstract Entity getEntityTarget(AltoClef mod);

}
