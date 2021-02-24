package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.AbstractDoToClosestObjectTask;
import adris.altoclef.util.progresscheck.IProgressChecker;
import adris.altoclef.util.progresscheck.LinearProgressChecker;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.baritone.GoalGetToPosition;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Util;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalTwoBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class PickupDroppedItemTask extends AbstractDoToClosestObjectTask<ItemEntity> {

    private final List<ItemTarget> _itemTargets;

    public PickupDroppedItemTask(List<ItemTarget> itemTargets) {
        _itemTargets = itemTargets;
    }

    public PickupDroppedItemTask(Item item, int targetCount) {
        this(Collections.singletonList(new ItemTarget(item, targetCount)));
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
        result.append("Pickup Dropped Items: [");
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

    @Override
    protected Vec3d getPos(AltoClef mod, ItemEntity obj) {
        return obj.getPos();
    }

    @Override
    protected ItemEntity getClosestTo(AltoClef mod, Vec3d pos) {
        if (!mod.getEntityTracker().itemDropped(Util.toArray(ItemTarget.class, _itemTargets))) return null;
        return mod.getEntityTracker().getClosestItemDrop(pos, Util.toArray(ItemTarget.class, _itemTargets));
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
