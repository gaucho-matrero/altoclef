package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.Util;
import net.minecraft.item.Item;

import java.util.Collections;
import java.util.List;

public abstract class ResourceTask extends Task {

    protected final List<ItemTarget> _itemTargets;

    public ResourceTask(List<ItemTarget> itemTargets) {
        _itemTargets = itemTargets;
    }
    public ResourceTask(ItemTarget target) {
        this(Collections.singletonList(target));
    }
    public ResourceTask(Item item, int targetCount) {
        this(new ItemTarget(item, targetCount));
    }


    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getInventoryTracker().targetReached(Util.toArray(ItemTarget.class, _itemTargets));
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getConfigState().push();
        mod.getConfigState().removeThrowawayItems(Util.toArray(ItemTarget.class, _itemTargets));
        onResourceStart(mod);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (!shouldAvoidPickingUp(mod)) {
            // Check if items are on the floor. If so, pick em up.
            if (mod.getEntityTracker().itemDropped(Util.toArray(ItemTarget.class, _itemTargets))) {
                setDebugState("Going to dropped items...");
                return new PickupDroppedItemTask(_itemTargets);
            }
        }

        return onResourceTick(mod);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getConfigState().pop();
        onResourceStop(mod, interruptTask);
    }

    @Override
    protected boolean isEqual(Task other) {
        // Same target items
        if (other instanceof ResourceTask) {
            ResourceTask t = (ResourceTask) other;
            if (!isEqualResource(t)) return false;
            if (t._itemTargets.size() != _itemTargets.size()) return false;
            for (int i = 0; i < _itemTargets.size(); ++i) {
                if (!_itemTargets.get(i).equals(t._itemTargets.get(i))) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        StringBuilder result = new StringBuilder();
        result.append(toDebugStringName()).append(": [");
        int c = 0;
        for (ItemTarget target : _itemTargets) {
            result.append(target.toString());
            if (++c != _itemTargets.size()) {
                result.append(", ");
            }
        }
        result.append("]");
        return result.toString();
    }

    protected abstract boolean shouldAvoidPickingUp(AltoClef mod);

    protected abstract void onResourceStart(AltoClef mod);

    protected abstract Task onResourceTick(AltoClef mod);

    protected abstract void onResourceStop(AltoClef mod, Task interruptTask);

    protected abstract boolean isEqualResource(ResourceTask other);

    protected abstract String toDebugStringName();
}
