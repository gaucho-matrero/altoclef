package adris.altoclef.tasks.chest;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.math.BlockPos;

public abstract class AbstractDoInChestTask extends Task {

    private final BlockPos _targetChest;

    public AbstractDoInChestTask(BlockPos targetChest) {
        _targetChest = targetChest;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getPlayer().closeHandledScreen();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_targetChest.isWithinDistance(mod.getPlayer().getPos(), 8) && mod.getPlayer().currentScreenHandler instanceof GenericContainerScreenHandler) {
            return doToOpenChestTask(mod, (GenericContainerScreenHandler) mod.getPlayer().currentScreenHandler);
        }
        return new InteractWithBlockTask(_targetChest);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getPlayer().closeHandledScreen();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof AbstractDoInChestTask) {
            AbstractDoInChestTask task = (AbstractDoInChestTask) obj;
            if (!task._targetChest.equals(_targetChest)) return false;
            return isSubEqual(task);
        }
        return false;
    }

    protected abstract Task doToOpenChestTask(AltoClef mod, GenericContainerScreenHandler handler);

    protected abstract boolean isSubEqual(AbstractDoInChestTask obj);

}
