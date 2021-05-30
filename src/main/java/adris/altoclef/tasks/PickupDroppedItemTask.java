package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasks.resources.SatisfyMiningRequirementTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;


public class PickupDroppedItemTask extends AbstractDoToClosestObjectTask<ItemEntity> implements ITaskRequiresGrounded {
    private static final Task getPickaxeFirstTask = new SatisfyMiningRequirementTask(MiningRequirement.STONE);
    // Not clean practice, but it helps keep things self contained I think.
    private static boolean isGettingPickaxeFirstFlag;
    private final ItemTarget[] itemTargets;
    private final Set<ItemEntity> blacklist = new HashSet<>();
    private final MovementProgressChecker progressChecker = new MovementProgressChecker(3);
    private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(20);
    private final boolean freeInventoryIfFull;
    private boolean collectingPickaxeForThisResource;
    private ItemEntity currentDrop;
    private boolean fullCheckFailed;

    public PickupDroppedItemTask(ItemTarget[] itemTargets, boolean freeInventoryIfFull) {
        this.itemTargets = itemTargets;
        this.freeInventoryIfFull = freeInventoryIfFull;
    }

    public PickupDroppedItemTask(ItemTarget target, boolean freeInventoryIfFull) {
        this(new ItemTarget[]{ target }, freeInventoryIfFull);
    }

    public PickupDroppedItemTask(Item item, int targetCount, boolean freeInventoryIfFull) {
        this(new ItemTarget(item, targetCount), freeInventoryIfFull);
    }

    public static boolean isIsGettingPickaxeFirst(AltoClef mod) {
        return isGettingPickaxeFirstFlag && mod.getModSettings().shouldCollectPickaxeFirst();
    }

    public boolean isCollectingPickaxeForThis() {
        return collectingPickaxeForThisResource;
    }

    @Override
    protected void onStart(AltoClef mod) {
        fullCheckFailed = false;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        // Same target items
        if (other instanceof PickupDroppedItemTask) {
            PickupDroppedItemTask t = (PickupDroppedItemTask) other;
            return Util.arraysEqual(t.itemTargets, itemTargets) && t.freeInventoryIfFull == freeInventoryIfFull;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        StringBuilder result = new StringBuilder();
        result.append("Pickup Dropped Items: [");
        int c = 0;
        for (ItemTarget target : itemTargets) {
            result.append(target.toString());
            if (++c != itemTargets.length) {
                result.append(", ");
            }
        }
        result.append("]");
        return result.toString();
    }

    @Override
    protected Vec3d getPos(AltoClef mod, ItemEntity obj) {
        return obj.getPos();
    }

    @Override
    protected ItemEntity getClosestTo(AltoClef mod, Vec3d pos) {
        if (!mod.getEntityTracker().itemDropped(itemTargets)) return null;
        return mod.getEntityTracker().getClosestItemDrop(pos, itemTargets);
    }

    @Override
    protected Vec3d getOriginPos(AltoClef mod) {
        return mod.getPlayer().getPos();
    }

    @Override
    protected Task getGoalTask(ItemEntity obj) {
        if (!obj.equals(currentDrop) && isGettingPickaxeFirstFlag && collectingPickaxeForThisResource) {
            Debug.logMessage("New goal, no longer collecting a pickaxe.");
            collectingPickaxeForThisResource = false;
            isGettingPickaxeFirstFlag = false;
        }
        currentDrop = obj;
        return new GetToEntityTask(obj);
    }

    @Override
    protected boolean isValid(AltoClef mod, ItemEntity obj) {
        return obj.isAlive() && !blacklist.contains(obj);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (wanderTask.isActive() && !wanderTask.isFinished(mod)) {
            setDebugState("Wandering after blacklisting item...");
            progressChecker.reset();
            return wanderTask;
        }

        // If we're getting a pickaxe for THIS resource...
        if (isIsGettingPickaxeFirst(mod) && collectingPickaxeForThisResource && !mod.getInventoryTracker().miningRequirementMet(
                MiningRequirement.STONE)) {
            progressChecker.reset();
            setDebugState("Collecting pickaxe first");
            return getPickaxeFirstTask;
        } else {
            if (mod.getInventoryTracker().miningRequirementMet(MiningRequirement.STONE)) {
                isGettingPickaxeFirstFlag = false;
            }
            collectingPickaxeForThisResource = false;
        }

        if (!progressChecker.check(mod)) {
            progressChecker.reset();
            if (currentDrop != null) {
                // We might want to get a pickaxe first.
                if (!isGettingPickaxeFirstFlag && mod.getModSettings().shouldCollectPickaxeFirst() &&
                    !mod.getInventoryTracker().miningRequirementMet(MiningRequirement.STONE)) {
                    Debug.logMessage("Failed to pick up drop, will try to collect a stone pickaxe first and try again!");
                    collectingPickaxeForThisResource = true;
                    isGettingPickaxeFirstFlag = true;
                    return getPickaxeFirstTask;
                }
                Debug.logMessage(Util.arrayToString(Util.toArray(ItemEntity.class, blacklist), element -> element == null
                                                                                                          ? "(null)"
                                                                                                          : element.getStack()
                                                                                                                   .getItem()
                                                                                                                   .getTranslationKey()));
                Debug.logMessage("Failed to pick up drop, suggesting it's unreachable.");
                blacklist.add(currentDrop);
                mod.getEntityTracker().requestEntityUnreachable(currentDrop);
                return wanderTask;
            }
        }
        if (freeInventoryIfFull) {
            boolean weGood = ResourceTask.ensureInventoryFree(mod);

            if (weGood) {
                fullCheckFailed = false;
            } else {
                if (!fullCheckFailed) {
                    Debug.logWarning("Failed to free up inventory as no throwaway-able slot was found. Awaiting user input.");
                }
                fullCheckFailed = true;
            }
        }

        return super.onTick(mod);
    }

}
