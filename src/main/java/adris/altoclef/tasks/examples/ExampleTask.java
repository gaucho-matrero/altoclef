package adris.altoclef.tasks.examples;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class ExampleTask extends Task {

    private final int _numberOfStonePickaxesToGrab;
    private final BlockPos _whereToPlaceCobblestone;

    public ExampleTask(int numberOfStonePickaxesToGrab, BlockPos whereToPlaceCobblestone) {
        _numberOfStonePickaxesToGrab = numberOfStonePickaxesToGrab;
        _whereToPlaceCobblestone = whereToPlaceCobblestone;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(Items.COBBLESTONE);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        /*
         * Grab X stone pickaxes
         * Make sure we have a block
         * Then, place the block.
         */

        if (mod.getInventoryTracker().getItemCount(Items.STONE_PICKAXE) < _numberOfStonePickaxesToGrab) {
            return TaskCatalogue.getItemTask("stone_pickaxe", _numberOfStonePickaxesToGrab);
        }

        if (!mod.getInventoryTracker().hasItem(Items.COBBLESTONE)) {
            return TaskCatalogue.getItemTask("cobblestone", 1);
        }

        if (mod.getChunkTracker().isChunkLoaded(_whereToPlaceCobblestone)) {
            if (mod.getWorld().getBlockState(_whereToPlaceCobblestone).getBlock() != Blocks.COBBLESTONE) {
                return new PlaceBlockTask(_whereToPlaceCobblestone, Blocks.COBBLESTONE); ///new PlaceStructureBlockTask(_whereToPlaceCobblestone);
            }
            return null;
        } else {
            return new GetToBlockTask(_whereToPlaceCobblestone);
        }
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getInventoryTracker().getItemCount(Items.STONE_PICKAXE) >= _numberOfStonePickaxesToGrab &&
                mod.getWorld().getBlockState(_whereToPlaceCobblestone).getBlock() == Blocks.COBBLESTONE;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof ExampleTask) {
            ExampleTask task = (ExampleTask) obj;
            return task._numberOfStonePickaxesToGrab == _numberOfStonePickaxesToGrab
                    && task._whereToPlaceCobblestone.equals(_whereToPlaceCobblestone);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Boofin";
    }
}
