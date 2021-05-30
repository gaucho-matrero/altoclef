package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.LookUtil;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class GiveItemToPlayerTask extends Task {
    private final String playerName;
    private final ItemTarget[] targets;
    private final CataloguedResourceTask resourceTask;
    private final List<ItemTarget> throwTarget = new ArrayList<>();
    private boolean droppingItems;

    public GiveItemToPlayerTask(String player, ItemTarget... targets) {
        playerName = player;
        this.targets = targets;

        // Some targets may not exist, so ignore the resources for them!
        List<ItemTarget> result = new ArrayList<>();
        for (ItemTarget target : targets) {
            if (target.isCatalogueItem()) result.add(target);
        }
        resourceTask = TaskCatalogue.getSquashedItemTask(Util.toArray(ItemTarget.class, result));
    }

    @Override
    protected void onStart(AltoClef mod) {
        droppingItems = false;
        throwTarget.clear();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        Vec3d targetPos = mod.getEntityTracker().getPlayerMostRecentPosition(playerName);

        if (droppingItems) {
            // THROW ITEMS
            setDebugState("Throwing items");
            LookUtil.lookAt(mod, targetPos);
            for (ItemTarget target : throwTarget) {
                if (target.targetCount > 0) {
                    Optional<Integer> has = mod.getInventoryTracker().getInventorySlotsWithItem(target.getMatches()).stream().findFirst();
                    if (has.isPresent()) {
                        Debug.logMessage("THROWING: " + has.get());
                        ItemStack stack = mod.getInventoryTracker().throwSlot(Slot.getFromInventory(has.get()));
                        //mod.getInventoryTracker().equipItem(target);
                        //mod.getControllerExtras().dropCurrentStack(true);
                        target.targetCount -= stack.getCount();
                        return null;
                    }
                }
            }
            mod.log("Finished giving items.");
            stop(mod);
            return null;
        }

        if (!mod.getInventoryTracker().targetMet(targets)) {
            setDebugState("Collecting resources...");
            return resourceTask;
        }

        if (targetPos == null) {
            mod.logWarning("Failed to get to player \"" + playerName + "\" because we have no idea where they are.");
            stop(mod);
            return null;
        }

        if (targetPos.isInRange(mod.getPlayer().getPos(), 1.5)) {
            if (!mod.getEntityTracker().isPlayerLoaded(playerName)) {
                mod.logWarning("Failed to get to player \"" + playerName +
                               "\". We moved to where we last saw them but now have no idea where they are.");
                stop(mod);
                return null;
            }
            droppingItems = true;
            throwTarget.addAll(Arrays.asList(targets));
        }

        setDebugState("Going to player...");
        return new FollowPlayerTask(playerName);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GiveItemToPlayerTask) {
            GiveItemToPlayerTask task = (GiveItemToPlayerTask) obj;
            if (!task.playerName.equals(playerName)) return false;
            return Util.arraysEqual(task.targets, targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Giving items to " + playerName;
    }
}
