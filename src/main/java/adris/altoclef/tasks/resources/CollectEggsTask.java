package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.DoToClosestEntityTask;
import adris.altoclef.tasks.GetToEntityTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.Items;

public class CollectEggsTask extends ResourceTask {

    private final int _count;

    public CollectEggsTask(int targetCount) {
        super(Items.EGG, targetCount);
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
        // Just wait around chickens.
        setDebugState("Waiting around chickens. Yes.");
        return new DoToClosestEntityTask(() -> mod.getPlayer().getPos(), chicken -> new GetToEntityTask(chicken, 5), ChickenEntity.class);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        return obj instanceof CollectEggsTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " eggs.";
    }
}
