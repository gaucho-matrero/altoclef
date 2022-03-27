package adris.altoclef.tasksystem;

import adris.altoclef.AltoClef;

/**
 * Some tasks (mainly tasks that open containers) will break if they don't require
 * that the 2x2 crafting grid is empty. Tasks which implement this interface
 * require implementers to maintain an empty crafting grid.
 *
 * This interface let's a task declare that it MUST have certain slots clear before it can execute.
 */
public interface ITaskLimitsSlots {

    /**
     * @return whether the implementer should clear the cursor slot prior to onTick()
     * @param candidate the task which is considering emptying the cursor
     */
     boolean shouldEmptyCursorSlot(AltoClef mod, Task candidate);

    /**
     * @return whether the implementer should clear the crafting grid prior to onTick()
     * @param candidate the task which is considering emptying the crafting grid
     */
     boolean shouldEmptyCraftingGrid(AltoClef mod, Task candidate);

}
