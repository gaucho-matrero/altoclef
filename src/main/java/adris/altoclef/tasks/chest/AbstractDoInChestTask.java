package adris.altoclef.tasks.chest;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.math.BlockPos;


public abstract class AbstractDoInChestTask extends Task {
    private final BlockPos targetChest;

    public AbstractDoInChestTask(BlockPos targetChest) {
        this.targetChest = targetChest;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getPlayer().closeHandledScreen();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (targetChest.isWithinDistance(mod.getPlayer().getPos(), 8) &&
            mod.getPlayer().currentScreenHandler instanceof GenericContainerScreenHandler) {
            return doToOpenChestTask(mod, (GenericContainerScreenHandler) mod.getPlayer().currentScreenHandler);
        }
        return new GetToBlockTask(targetChest, true);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getPlayer().closeHandledScreen();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof AbstractDoInChestTask) {
            AbstractDoInChestTask task = (AbstractDoInChestTask) obj;
            if (!task.targetChest.equals(targetChest)) return false;
            return isSubEqual(task);
        }
        return false;
    }

    protected abstract Task doToOpenChestTask(AltoClef mod, GenericContainerScreenHandler handler);

    protected abstract boolean isSubEqual(AbstractDoInChestTask obj);

}
