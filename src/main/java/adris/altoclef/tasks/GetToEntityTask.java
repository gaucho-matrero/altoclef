package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalFollowEntity;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.entity.Entity;


public class GetToEntityTask extends Task implements ITaskRequiresGrounded {
    private final Entity entity;
    private final double closeEnoughDistance;
    private final MovementProgressChecker progress = new MovementProgressChecker(5, 0.1, 5, 0.001, 2);
    private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(10);
    
    public GetToEntityTask(Entity entity, double closeEnoughDistance) {
        this.entity = entity;
        this.closeEnoughDistance = closeEnoughDistance;
    }
    
    public GetToEntityTask(Entity entity) {
        this(entity, 1);
    }
    
    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        wanderTask.resetWander();
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        
        if (wanderTask.isActive() && !wanderTask.isFinished(mod)) {
            progress.reset();
            setDebugState("Failed to get to target, wandering for a bit.");
            return wanderTask;
        }
        
        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalFollowEntity(entity, closeEnoughDistance));
        }
        
        if (mod.getPlayer().isInRange(entity, closeEnoughDistance)) {
            progress.reset();
        }
        
        if (!progress.check(mod)) {
            return wanderTask;
        }
        
        setDebugState("Going to entity");
        return null;
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GetToEntityTask) {
            GetToEntityTask task = (GetToEntityTask) obj;
            return task.entity.equals(entity) && Math.abs(task.closeEnoughDistance - closeEnoughDistance) < 0.1;
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Approach entity " + entity.getDisplayName().asString();
    }
}
