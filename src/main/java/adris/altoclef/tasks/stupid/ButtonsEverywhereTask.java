package adris.altoclef.tasks.stupid;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import baritone.api.schematic.FillSchematic;
import baritone.api.utils.BlockOptionalMeta;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class ButtonsEverywhereTask extends Task {
    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritone().getBuilderProcess().onLostControl();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!mod.getInventoryTracker().hasItem(Items.DIAMOND_PICKAXE)) {
            setDebugState("Getting axe");
            return TaskCatalogue.getItemTask("diamond_pickaxe", 1);
        }
        if (!mod.getInventoryTracker().hasItem(Items.DIAMOND_SHOVEL)) {
            setDebugState("Getting shovel");
            return TaskCatalogue.getItemTask("diamond_shovel", 1);
        }
        /*
        if (!mod.getInventoryTracker().hasItem(ItemTarget.WOOD_BUTTON)) {
            setDebugState("Getting button");
            return TaskCatalogue.getItemTask("wooden_button", 1);
        }
         */
        // Place button nearby
        setDebugState("Placing button");

        if (!mod.getClientBaritone().getBuilderProcess().isActive()) {
            mod.getClientBaritone().getBuilderProcess().build("fuckemup", new FillSchematic(64, 64, 64, new BlockOptionalMeta(Blocks.AIR)), new BlockPos(100, 64, 100));
        }

        return null;
        //return new PlaceEverywhereTask(ItemTarget.itemsToBlocks(ItemTarget.WOOD_BUTTON));//PlaceBlockNearbyTask(ItemTarget.itemsToBlocks(ItemTarget.WOOD_BUTTON));
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getBuilderProcess().onLostControl();
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof ButtonsEverywhereTask;
    }

    @Override
    protected String toDebugString() {
        return null;
    }
}
