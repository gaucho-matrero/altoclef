package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.AbstractDoToEntityTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class CollectWoolTask extends ResourceTask {

    private final int _count;

    public CollectWoolTask(int count) {
        super(new ItemTarget(ItemTarget.WOOL, count));
        _count = count;
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

        // If we have shears, right click nearest sheep
        // Otherwise, kill + loot wool.

        if (mod.getInventoryTracker().hasItem(Items.SHEARS)) {
            // Shear sheep.
            return new ShearSheepTask();
        }

        // Only option left is to Kill la Kill.
        return new KillAndLootTask(SheepEntity.class, entity -> {
            if (entity instanceof SheepEntity) {
                return ((SheepEntity)entity).isSheared();
            }
            return false;
        }, new ItemTarget(ItemTarget.WOOL, _count));
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        return obj instanceof CollectWoolTask && ((CollectWoolTask) obj)._count == _count;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect " + _count + " wool.";
    }

    static class ShearSheepTask extends AbstractDoToEntityTask {

        public ShearSheepTask() {
            super(0, -1, -1);
        }

        @Override
        protected boolean isSubEqual(AbstractDoToEntityTask other) {
            return other instanceof ShearSheepTask;
        }

        @Override
        protected Task onEntityInteract(AltoClef mod, Entity entity) {
            if (!mod.getInventoryTracker().hasItem(Items.SHEARS)) {
                Debug.logWarning("Failed to shear sheep because you have no shears.");
                return null;
            }
            if (mod.getInventoryTracker().equipItem(Items.SHEARS)) {
                mod.getController().interactEntity(mod.getPlayer(), entity, Hand.MAIN_HAND);
            } else {
                Debug.logWarning("Failed to equip shears for some reason.");
            }


            return null;
        }

        @Override
        protected Entity getEntityTarget(AltoClef mod) {
            Entity found = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(),
                    (entity) -> {
                        if (entity instanceof SheepEntity) {
                            SheepEntity sheep = (SheepEntity) entity;
                            return !sheep.isShearable() || sheep.isSheared();
                        }

                        return true;
                    }, SheepEntity.class
            );
            return found;
        }

        @Override
        protected String toDebugString() {
            return "Shearing Sheep";
        }
    }
}
