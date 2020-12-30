package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;

// TODO: Make this collect more than just coal. It should smartly pick alternative sources if coal is too far away or if we simply cannot get a wooden pick.
public class CollectFuelTask extends Task {

    private final double _targetFuel;

    public CollectFuelTask(double targetFuel) {
        _targetFuel = targetFuel;
    }

    @Override
    protected void onStart(AltoClef mod) {
        // Nothing
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // Just collect coal for now.
        return TaskCatalogue.getItemTask("coal", (int)Math.ceil(_targetFuel / 8));
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Nothing
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof CollectFuelTask) {
            CollectFuelTask other = (CollectFuelTask) obj;
            return Math.abs(other._targetFuel - _targetFuel) < 0.01;
        }
        return false;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getInventoryTracker().getTotalFuel() >= _targetFuel;
    }

    @Override
    protected String toDebugString() {
        return "Collect Fuel: x" + _targetFuel;
    }
}
