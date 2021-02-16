package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.AbstractDoToClosestObjectTask;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.function.Function;
import java.util.function.Supplier;

public class DoToClosestEntityTask extends AbstractDoToClosestObjectTask<Entity> {

    private final Class[] _targetEntities;

    private final Supplier<Vec3d> _getOriginPos;

    private final Function<Entity, Task> _getTargetTask;

    public DoToClosestEntityTask(Supplier<Vec3d> getOriginSupplier, Function<Entity, Task> getTargetTask, Class ...entities) {
        _getOriginPos = getOriginSupplier;
        _getTargetTask = getTargetTask;
        _targetEntities = entities;
    }

    @Override
    protected Vec3d getPos(AltoClef mod, Entity obj) {
        return obj.getPos();
    }

    @Override
    protected Entity getClosestTo(AltoClef mod, Vec3d pos) {
        if (!mod.getEntityTracker().mobFound(_targetEntities)) return null;
        return mod.getEntityTracker().getClosestEntity(pos, _targetEntities);
    }

    @Override
    protected Vec3d getOriginPos(AltoClef mod) {
        return _getOriginPos.get();
    }

    @Override
    protected Task getGoalTask(Entity obj) {
        return _getTargetTask.apply(obj);
    }

    @Override
    protected boolean isValid(AltoClef mod, Entity obj) {
        return obj.isAlive();
    }

    @Override
    protected void onStart(AltoClef mod) { }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) { }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof DoToClosestEntityTask) {
            DoToClosestEntityTask task = (DoToClosestEntityTask) obj;
            if (task._targetEntities.length != _targetEntities.length) return false;
            for (int i = 0; i < _targetEntities.length; ++i) {
                if (!task._targetEntities[i].equals(_targetEntities[i])) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Doing something to closest entity...";
    }
}