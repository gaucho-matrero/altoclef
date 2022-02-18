package adris.altoclef.tasksystem;

import adris.altoclef.AltoClef;

/***
 * Some tasks have periods of "Downtime" where the task is essentially waiting.
 * This interface lets somebody know when this task is waiting, as well as
 * when it should stop waiting.
 *
 * For example: smelting in a furnace. While we wait for a furnace to finish smelting,
 * there is nothing that we can really do but wait.
 *
 * Use case: UserTask. If we have a "downtime" task, set our priority way lower so other tasks
 * can happen.
 */
public interface ITaskWithDowntime {
    boolean isInDowntime(AltoClef mod);
}
