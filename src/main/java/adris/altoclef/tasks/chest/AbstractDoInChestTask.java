package adris.altoclef.tasks.chest;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.SchematicBuildTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Utils;
import baritone.api.schematic.FillSchematic;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.math.BlockPos;

public abstract class AbstractDoInChestTask extends Task {
    private final BlockPos _targetChest;
    private boolean lostChest;
    private SchematicBuildTask builder;

    public AbstractDoInChestTask(BlockPos targetChest) {
        _targetChest = targetChest;
        this.lostChest = false;
    }

    protected final boolean isChestLost() {
        return this.lostChest;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getControllerExtras().closeScreen();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_targetChest.isWithinDistance(mod.getPlayer().getPos(), 8)) {
            if (!(MinecraftClient.getInstance().world.getBlockState(_targetChest).getBlock() instanceof ChestBlock)) {
                this.lostChest = true;

                /*
                if (Utils.isNull(this.builder)) {
                    this.builder = new SchematicBuildTask("place chest", new FillSchematic(1, 1, 1, Blocks.CHEST.getDefaultState()), _targetChest);
                }

                return this.builder; //new SchematicBuildTask("place chest", new FillSchematic(1, 1, 1, Blocks.CHEST.getDefaultState()), _targetChest);
                */

                if (mod.getInventoryTracker().hasItem(Items.CHEST)) {
                    setDebugState("Placing chest");
                    return new PlaceBlockTask(_targetChest, Blocks.CHEST);
                } else {
                    setDebugState("Collecting materials for chest");
                    return TaskCatalogue.getItemTask(Items.CHEST, 1);
                }
            }

            if (mod.getPlayer().currentScreenHandler instanceof GenericContainerScreenHandler) {
                return doToOpenChestTask(mod, (GenericContainerScreenHandler) mod.getPlayer().currentScreenHandler);
            }
        }
        return new InteractWithBlockTask(_targetChest);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getControllerExtras().closeScreen();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof AbstractDoInChestTask task) {
            if (!task._targetChest.equals(_targetChest)) return false;
            return isSubEqual(task);
        }
        return false;
    }

    protected abstract Task doToOpenChestTask(AltoClef mod, GenericContainerScreenHandler handler);

    protected abstract boolean isSubEqual(AbstractDoInChestTask other);

}
