package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Optional;


/**
 * https://www.notion.so/Closest-threshold-ing-system-utility-c3816b880402494ba9209c9f9b62b8bf
 * <p>
 * Use this whenever you want to travel to a target position that may change.
 */
public abstract class AbstractDoToClosestObjectTask<T> extends Task {
    private final HashMap<T, Double> heuristicMap = new HashMap<>();
    private T currentlyPursuing;
    private boolean wasWandering;
    private Task goalTask;

    protected abstract Vec3d getPos(AltoClef mod, T obj);

    protected abstract T getClosestTo(AltoClef mod, Vec3d pos);

    protected abstract Vec3d getOriginPos(AltoClef mod);

    protected abstract Task getGoalTask(T obj);

    protected abstract boolean isValid(AltoClef mod, T obj);

    // Virtual
    protected Task getWanderTask(AltoClef mod) {
        return new TimeoutWanderTask(true);
    }

    public void resetSearch() {
        currentlyPursuing = null;
        heuristicMap.clear();
        goalTask = null;
    }

    public boolean wasWandering() {
        return wasWandering;
    }

    private double getCurrentCalculatedHeuristic(AltoClef mod) {
        Optional<Double> ticksRemainingOp = mod.getClientBaritone().getPathingBehavior().ticksRemainingInSegment();
        return ticksRemainingOp.orElse(Double.POSITIVE_INFINITY);
    }

    private boolean isMovingToClosestPos(AltoClef mod) {
        return goalTask != null;// && _goalTask.isActive() && !_goalTask.isFinished(mod);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        wasWandering = false;

        // Reset our pursuit if our pursuing object no longer is pursuable.
        if (currentlyPursuing != null && !isValid(mod, currentlyPursuing)) {
            // This is probably a good idea, no?
            heuristicMap.remove(currentlyPursuing);
            currentlyPursuing = null;
        }

        // Get closest object
        T newClosest = getClosestTo(mod, getOriginPos(mod));

        // Receive closest object and position
        if (newClosest != null && !newClosest.equals(currentlyPursuing)) {
            // Different closest object
            if (currentlyPursuing == null) {
                // We don't have a closest object
                currentlyPursuing = newClosest;
            } else {
                if (isMovingToClosestPos(mod)) {
                    setDebugState("Moving towards closest...");
                    double currentHeuristic = getCurrentCalculatedHeuristic(mod);
                    heuristicMap.put(currentlyPursuing, currentHeuristic);
                    if (heuristicMap.containsKey(newClosest)) {
                        //Debug.logInternal("OVERKILL: " + _heuristicMap.get(newClosest) + " ?< " + currentHeuristic);
                        // Our new object has a past potential heuristic calculated, if it's better try it out.
                        if (heuristicMap.get(newClosest) < currentHeuristic) {
                            setDebugState("Found closer!");
                            // The currently closest previously calculated heuristic is better, move towards it!
                            currentlyPursuing = newClosest;
                        }
                    } else {
                        setDebugState("Trying out NEW pursuit");
                        // Our new object does not have a heuristic, TRY IT OUT!
                        currentlyPursuing = newClosest;
                    }
                } else {
                    setDebugState("Waiting for move task to kick in...");
                    // We should keep moving towards our object until we get some new info.
                }
            }
        }

        if (currentlyPursuing != null) {
            goalTask = getGoalTask(currentlyPursuing);
            return goalTask;
        } else {
            goalTask = null;
        }

        //noinspection ConstantConditions
        if (newClosest == null && currentlyPursuing == null) {
            setDebugState("Waiting for calculations I think (wandering)");
            wasWandering = true;
            return getWanderTask(mod);
        }

        setDebugState("Waiting for calculations I think (NOT wandering)");
        return null;
    }

    // Interface DRAFT:
    /*
     * MAKE THIS AN ABSTRACT TASK
     *
     * T can be
     *      - BlockPos
     *      - Entity
     *      - Any object we might want to travel to
     *
     * Abstract functions
     *  - get position(T) -> position
     *  - get closest(T) -> (position, T)
     *
     *
     * Private fields
     * - best heurisitc
     * - best object
     * - (best position)?
     *
     * Methods
     * - Reset Search
     * - Get closest object
     *      - Runs closest position function
     *      - If different object:
     *          If previous object was null, accept and get to.
     *          If we're not running our GOTO task/process, ignore and keep going to previous object.
     *          If we ARE running our GOTO task/process, GET CURRENT CALCULATED HEURISTIC and MAP.
     *          If the NEW object has a MAPPING to a calculated heuristic:
     *              If PREVIOUS object has BETTER heuristic, accept previous object.
     *          If the NEW object does NOT have a mapping to a calculated heuristic, ACCEPT IT!
     *      - If same object, keep running the GOTO Task (if no goto task, create one)
     * - Create GOTO task/process given T
     */
}
