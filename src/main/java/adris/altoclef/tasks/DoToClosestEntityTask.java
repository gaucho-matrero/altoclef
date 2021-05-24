package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


@SuppressWarnings("ALL")
public class DoToClosestEntityTask extends AbstractDoToClosestObjectTask<Entity> {
    private final Class[] targetEntities;
    private final Supplier<Vec3d> getOriginPos;
    private final Function<Entity, Task> getTargetTask;
    private final Predicate<Entity> ignorePredicate;
    
    public DoToClosestEntityTask(Supplier<Vec3d> getOriginSupplier, Function<Entity, Task> getTargetTask, Predicate<Entity> ignorePredicate,
                                 Class... entities) {
        getOriginPos = getOriginSupplier;
        this.getTargetTask = getTargetTask;
        this.ignorePredicate = ignorePredicate;
        targetEntities = entities;
    }
    
    public DoToClosestEntityTask(Supplier<Vec3d> getOriginSupplier, Function<Entity, Task> getTargetTask, Class... entities) {
        this(getOriginSupplier, getTargetTask, entity -> false, entities);
    }
    
    @Override
    protected Vec3d getPos(AltoClef mod, Entity obj) {
        return obj.getPos();
    }
    
    @Override
    protected Entity getClosestTo(AltoClef mod, Vec3d pos) {
        if (!mod.getEntityTracker().entityFound(targetEntities)) return null;
        return mod.getEntityTracker().getClosestEntity(pos, ignorePredicate, targetEntities);
    }
    
    @Override
    protected Vec3d getOriginPos(AltoClef mod) {
        return getOriginPos.get();
    }
    
    @Override
    protected Task getGoalTask(Entity obj) {
        return getTargetTask.apply(obj);
    }
    
    @Override
    protected boolean isValid(AltoClef mod, Entity obj) {
        return obj.isAlive() && mod.getEntityTracker().isEntityReachable(obj);
    }
    
    @Override
    protected void onStart(AltoClef mod) {
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof DoToClosestEntityTask) {
            DoToClosestEntityTask task = (DoToClosestEntityTask) obj;
            return Util.arraysEqual(task.targetEntities, targetEntities);
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Doing something to closest entity...";
    }
}