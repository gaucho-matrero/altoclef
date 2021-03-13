package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.List;

public class PickupDroppedItemTask extends AbstractDoToClosestObjectTask<ItemEntity> {

    private final ItemTarget[] _itemTargets;

    public PickupDroppedItemTask(ItemTarget[] itemTargets) {
        _itemTargets = itemTargets;
    }
    public PickupDroppedItemTask(ItemTarget target) {
        this(new ItemTarget[] {target});
    }

    public PickupDroppedItemTask(Item item, int targetCount) {
        this(new ItemTarget(item, targetCount));
    }

    @Override
    protected void onStart(AltoClef mod) {
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }


    @Override
    protected boolean isEqual(Task other) {
        // Same target items
        if (other instanceof PickupDroppedItemTask) {
            PickupDroppedItemTask t = (PickupDroppedItemTask) other;
            return Util.arraysEqual(t._itemTargets, _itemTargets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        StringBuilder result = new StringBuilder();
        result.append("Pickup Dropped Items: [");
        int c = 0;
        for (ItemTarget target : _itemTargets) {
            result.append(target.toString());
            if (++c != _itemTargets.length) {
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
        if (!mod.getEntityTracker().itemDropped(_itemTargets)) return null;
        return mod.getEntityTracker().getClosestItemDrop(pos, _itemTargets);
    }

    @Override
    protected Vec3d getOriginPos(AltoClef mod) {
        return mod.getPlayer().getPos();
    }

    @Override
    protected Task getGoalTask(ItemEntity obj) {
        return new GetToEntityTask(obj);
    }

    @Override
    protected boolean isValid(AltoClef mod, ItemEntity obj) {
        return obj.isAlive();
    }

}
