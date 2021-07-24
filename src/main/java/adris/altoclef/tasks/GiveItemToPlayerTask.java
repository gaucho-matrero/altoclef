package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.squashed.CataloguedResourceTask;
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

    private final String _playerName;
    private final ItemTarget[] _targets;

    private final CataloguedResourceTask _resourceTask;
    private final List<ItemTarget> _throwTarget = new ArrayList<>();
    private boolean _droppingItems;

    public GiveItemToPlayerTask(String player, ItemTarget... targets) {
        _playerName = player;
        _targets = targets;

        // Some targets may not exist, so ignore the resources for them!
        List<ItemTarget> result = new ArrayList<>();
        for (ItemTarget target : targets) {
            if (target.isCatalogueItem()) result.add(target);
        }
        _resourceTask = TaskCatalogue.getSquashedItemTask(Util.toArray(ItemTarget.class, result));
    }

    @Override
    protected void onStart(AltoClef mod) {
        _droppingItems = false;
        _throwTarget.clear();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        Vec3d targetPos = mod.getEntityTracker().getPlayerMostRecentPosition(_playerName);

        if (_droppingItems) {
            // THROW ITEMS
            setDebugState("Throwing items");
            LookUtil.lookAt(mod, targetPos);
            for (int i = 0; i < _throwTarget.size(); ++i) {
                ItemTarget target = _throwTarget.get(i);
                if (target.getTargetCount() > 0) {
                    Optional<Integer> has = mod.getInventoryTracker().getInventorySlotsWithItem(target.getMatches()).stream().findFirst();
                    if (has.isPresent()) {
                        Debug.logMessage("THROWING: " + has.get());
                        ItemStack stack = mod.getInventoryTracker().throwSlot(Slot.getFromInventory(has.get()));
                        // Update target
                        target = new ItemTarget(target, target.getTargetCount() - stack.getCount());
                        _throwTarget.set(i, target);
                        return null;
                    }
                }
            }
            mod.log("Finished giving items.");
            stop(mod);
            return null;
        }

        if (!mod.getInventoryTracker().targetMet(_targets)) {
            setDebugState("Collecting resources...");
            return _resourceTask;
        }

        if (targetPos == null) {
            mod.logWarning("Failed to get to player \"" + _playerName + "\" because we have no idea where they are.");
            stop(mod);
            return null;
        }

        if (targetPos.isInRange(mod.getPlayer().getPos(), 1.5)) {
            if (!mod.getEntityTracker().isPlayerLoaded(_playerName)) {
                mod.logWarning("Failed to get to player \"" + _playerName + "\". We moved to where we last saw them but now have no idea where they are.");
                stop(mod);
                return null;
            }
            _droppingItems = true;
            _throwTarget.addAll(Arrays.asList(_targets));
        }

        setDebugState("Going to player...");
        return new FollowPlayerTask(_playerName);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GiveItemToPlayerTask) {
            GiveItemToPlayerTask task = (GiveItemToPlayerTask) obj;
            if (!task._playerName.equals(_playerName)) return false;
            return Util.arraysEqual(task._targets, _targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Giving items to " + _playerName;
    }
}
