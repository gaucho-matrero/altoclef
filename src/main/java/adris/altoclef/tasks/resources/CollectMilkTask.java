package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.AbstractDoToEntityTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class CollectMilkTask extends ResourceTask {

    private final int _count;

    public CollectMilkTask(int targetCount) {
        super(Items.MILK_BUCKET, targetCount);
        _count = targetCount;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // Make sure we have a bucket.
        if (!mod.getInventoryTracker().hasItem(Items.BUCKET)) {
            return TaskCatalogue.getItemTask("bucket", 1);
        }
        // Dimension
        if (!mod.getEntityTracker().entityFound(CowEntity.class) && isInWrongDimension(mod)) {
            return getToCorrectDimensionTask(mod);
        }
        return new MilkCowTask();
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        return obj instanceof CollectMilkTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " milk buckets.";
    }

    static class MilkCowTask extends AbstractDoToEntityTask {

        public MilkCowTask() {
            super(0, -1, -1);
        }

        @Override
        protected boolean isSubEqual(AbstractDoToEntityTask other) {
            return other instanceof MilkCowTask;
        }

        @Override
        protected Task onEntityInteract(AltoClef mod, Entity entity) {
            if (!mod.getInventoryTracker().hasItem(Items.BUCKET)) {
                Debug.logWarning("Failed to milk cow because you have no bucket.");
                return null;
            }
            if (mod.getInventoryTracker().equipItem(Items.BUCKET)) {
                mod.getController().interactEntity(mod.getPlayer(), entity, Hand.MAIN_HAND);
            } else {
                Debug.logWarning("Failed to equip bucket for some reason.");
            }


            return null;
        }

        @Override
        protected Entity getEntityTarget(AltoClef mod) {
            Entity found = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), CowEntity.class);
            return found;
        }

        @Override
        protected String toDebugString() {
            return "Milking Cow";
        }
    }
}
