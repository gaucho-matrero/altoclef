package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;


/**
 * Some generic tasks require us to go to the nether.
 * <p>
 * The user should be able to specify how this should be done in settings (ex, craft a new portal from scratch or check particular portal
 * areas first or highway or whatever)
 */
public class DefaultGoToDimensionTask extends Task {
    private final Dimension target;
    
    public DefaultGoToDimensionTask(Dimension target) {
        this.target = target;
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getCurrentDimension() == target;
    }
    
    @Override
    protected void onStart(AltoClef mod) {
    
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        setDebugState("NOT IMPLEMENTED YET!");
        return null;
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof DefaultGoToDimensionTask) {
            DefaultGoToDimensionTask task = (DefaultGoToDimensionTask) obj;
            return task.target == target;
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Going to dimension: " + target + " (default version)";
    }
}
