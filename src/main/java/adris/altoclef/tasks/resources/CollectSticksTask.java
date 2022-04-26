package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CollectSticksTask extends ResourceTask {

    private final int _targetCount;

    private static final Item[] _itemsToPickup;
    static {
        List<Item> itemsToPickup = new ArrayList<>(Arrays.asList(ItemHelper.LOG));
        itemsToPickup.addAll(Arrays.asList(ItemHelper.PLANKS));
        _itemsToPickup = itemsToPickup.toArray(new Item[itemsToPickup.size()]);
    }

    public CollectSticksTask(int targetCount) {
        super(Items.STICK, targetCount);
        _targetCount = targetCount;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBlockTracker().trackBlock(Blocks.DEAD_BUSH);
        mod.getBlockTracker().trackBlock(ItemHelper.itemsToBlocks(ItemHelper.LOG));
        mod.getBlockTracker().trackBlock(ItemHelper.itemsToBlocks(ItemHelper.PLANKS));
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // Calculate how much sticks can be made from planks and craft
        int potentialStickCountFromPlanks = (mod.getItemStorage().getItemCount(ItemHelper.PLANKS)/2)*4 + mod.getItemStorage().getItemCount(Items.STICK);
        if (potentialStickCountFromPlanks >= _targetCount) {
            for (Item plank : ItemHelper.PLANKS) {
                int count = mod.getItemStorage().getItemCount(plank);
                if (count > 0) {
                    setDebugState("Crafting sticks from planks");
                    return new CraftInInventoryTask(new RecipeTarget(Items.STICK, (count/2)*4, generateStickRecipe(ItemHelper.PLANKS)));
                }
            }
        }
        // Do the same for logs to craft planks
        int potentialStickCountFromLogsAndPlanks = potentialStickCountFromPlanks + mod.getItemStorage().getItemCount(ItemHelper.LOG)*8;
        if (potentialStickCountFromLogsAndPlanks >= _targetCount) {
            return TaskCatalogue.getItemTask(new ItemTarget(ItemHelper.PLANKS));
        }
        // Check the ground for loot
        if (mod.getEntityTracker().itemDropped(_itemsToPickup)) {
            return new PickupDroppedItemTask(new ItemTarget(_itemsToPickup), true);
        }
        // Mine wood or dead bushes
        ArrayList<Block> blocksToMine = new ArrayList<>(Arrays.asList(ItemHelper.itemsToBlocks(ItemHelper.LOG)));
        blocksToMine.add(Blocks.DEAD_BUSH);
        blocksToMine.addAll(Arrays.asList(ItemHelper.itemsToBlocks(ItemHelper.PLANKS)));
        Optional<BlockPos> block = mod.getBlockTracker().getNearestTracking(blocksToMine.toArray(new Block[blocksToMine.size()])); // very janky but i suck at java
        if (block.isPresent()) {
            BlockPos blockPos = block.get();
            return new DestroyBlockTask(blockPos);
        }
        return new TimeoutWanderTask();
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.DEAD_BUSH);
        mod.getBlockTracker().stopTracking(ItemHelper.itemsToBlocks(ItemHelper.LOG));
        mod.getBlockTracker().stopTracking(ItemHelper.itemsToBlocks(ItemHelper.PLANKS));
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectSticksTask && this._targetCount == ((CollectSticksTask) other)._targetCount;
    }

    @Override
    protected String toDebugStringName() {
        return "Crafting " + _targetCount + " sticks";
    }

    private static CraftingRecipe generateStickRecipe(Item[] planks) {
        return CraftingRecipe.newShapedRecipe(
                "stick",
                new Item[][]{
                        planks, null,
                        planks, null
                },
                4
        );
    }
    private static CraftingRecipe generatePlankRecipe(Item[] logs) {
        return CraftingRecipe.newShapedRecipe(
                "planks",
                new Item[][]{
                        logs, null,
                        null, null
                },
                4
        );
    }
}
