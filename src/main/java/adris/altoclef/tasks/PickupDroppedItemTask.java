package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import baritone.process.FollowProcess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class PickupDroppedItemTask extends Task {

    private final List<ItemTarget> _itemTargets;

    private AltoClef _mod;

    private final TargetPredicate _targetPredicate = new TargetPredicate();

    public PickupDroppedItemTask(List<ItemTarget> itemTargets) {
        _itemTargets = itemTargets;
    }

    public PickupDroppedItemTask(Item item, int targetCount) {
        this(Collections.singletonList(new ItemTarget(item, targetCount)));
    }

    @Override
    protected void onStart(AltoClef mod) {
        _mod = mod;

        // Config
        mod.getConfigState().push();
        mod.getConfigState().setFollowDistance(0);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        ItemEntity closest = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), _itemTargets);

        taskAssert(mod, closest != null, "Failed to find any items to pick up. Should have checked this condition earlier");

        //noinspection ConstantConditions
        mod.getClientBaritone().getCustomGoalProcess().setGoal(new GoalBlock(closest.getBlockPos()));

        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
            mod.getClientBaritone().getCustomGoalProcess().path();
        }

        //mod.getClientBaritone().getFollowProcess().follow(_targetPredicate);

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getConfigState().pop();
        // Stop baritone IF the other task isn't an item task.
        if (!(interruptTask instanceof PickupDroppedItemTask)) {
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            //_mod.getClientBaritone().getFollowProcess().cancel();
        }
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

    class TargetPredicate implements Predicate<Entity> {

        @Override
        public boolean test(Entity entity) {
            if (entity instanceof ItemEntity) {
                ItemEntity iEntity = (ItemEntity) entity;
                for (ItemTarget target : _itemTargets) {
                    // If we already have this item, ignore it
                    if (_mod.getInventoryTracker().targetReached(target)) continue;
                    // Match for item
                    if (target.item.equals(iEntity.getStack().getItem())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }


}
