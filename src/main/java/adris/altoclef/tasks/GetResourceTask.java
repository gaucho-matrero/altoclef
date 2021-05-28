package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.Task;

public class GetResourceTask extends Task
{

    private final int _resourceCount;
    private final String _resourceName;

    private Task _collectFoodTask;

    

    public GetResourceTask(String resourceName, int count)
    {
        _resourceName = resourceName;
        _resourceCount = count;
    }

    @Override protected void onStart(AltoClef mod)
    {

    }

    @Override protected Task onTick(AltoClef mod)
    {

        if (_collectFoodTask != null && _collectFoodTask.isActive() && !_collectFoodTask.isFinished(mod))
        {
            setDebugState("Collecting food");
            return _collectFoodTask;
        }

        if (mod.getPlayer().getHungerManager().getFoodLevel() < 3)
        {
            if (_collectFoodTask == null)
                _collectFoodTask = new CollectFoodTask(20);
            Debug.logMessage("Almost starving... getting food");
            setDebugState("Collecting food");

            return _collectFoodTask;

        }

        // checking how many items we already have, if they're enough we'll stop

        if (TaskCatalogue.getItemMatches(_resourceName) != null)
        {

            int resourceInInventory = mod.getInventoryTracker().getItemCount(TaskCatalogue.getItemMatches(_resourceName));

            if (_resourceCount > resourceInInventory)
                return TaskCatalogue.getItemTask(_resourceName, _resourceCount - resourceInInventory);

        }

        Debug.logMessage("Done!");
        stop(mod);

        return null;
    }

    @Override protected void onStop(AltoClef mod, Task interruptTask)
    {

    }

    @Override protected boolean isEqual(Task obj)
    {
        if (obj instanceof GetResourceTask)
        {
            GetResourceTask task = (GetResourceTask) obj;
            return task._resourceCount == _resourceCount && task._resourceName.equals(_resourceName);
        }
        return false;
    }

    @Override protected String toDebugString()
    {
        return "Getting x" + _resourceCount + " of " + _resourceName;
    }
}
