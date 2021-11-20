package adris.altoclef.tasks.chest;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.util.Blacklist;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.TaskDataPackage;
import adris.altoclef.util.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Collectors;

public class FillTargetChestTask extends Task {
    private final ItemTarget itemTargets;
    private final TaskDataPackage taskDataPackage;
    private final TaskDataPackage storeInChestTaskDataPackage;
    private final BlockPos chestPos;
    private boolean sourced;
    //This should be final
    private StoreInTargetChestTask storeInTargetChestTask;
    private final int minChunkSlots = 3;
    private boolean paused;

    public FillTargetChestTask(ItemTarget targets) {
        itemTargets = targets;
        taskDataPackage = new TaskDataPackage();
        storeInChestTaskDataPackage = new TaskDataPackage();
        chestPos = getLookingBlock();

        if (Utils.isNull(chestPos)) {
            Debug.logMessage("No chest at target found!");
            taskDataPackage.setFinished(true);
            return;
        }

        Debug.logMessage("Target chest found!");

        Blacklist.blacklist(chestPos);
        sourced = false;
        this.storeInChestTaskDataPackage.setFinished(false);
        this.storeInTargetChestTask = new StoreInTargetChestTask(chestPos, storeInChestTaskDataPackage, itemTargets);
        this.paused = false;
    }

    private BlockPos getLookingBlock() {
        HitResult r = MinecraftClient.getInstance().crosshairTarget;
        if (r != null && r.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult)r;
            final BlockPos blockPos = blockHitResult.getBlockPos();
            if (MinecraftClient.getInstance().world.getBlockState(blockPos).getBlock() instanceof ChestBlock) {
                return blockPos;
            }
        }
        return null;
    }

    @Override
    protected void onStart(AltoClef mod) {
        this.paused = false;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (this.paused) {
            return null;
        }

        if (storeInChestTaskDataPackage.isFinished()) {
            final Map<Item, Integer> complementaryMap =
                    storeInChestTaskDataPackage.getExtra(TaskDataPackage.ExtraData.PROGRESS_DATA, HashMap.class);

            Item[] items = complementaryMap.keySet().toArray(new Item[complementaryMap.size()]);
            List<ItemTarget> trashTargets = Arrays.stream(mod.getModSettings().getThrowawayItems(mod)).map(e -> new ItemTarget(e)).collect(Collectors.toList());
            Arrays.stream(items).forEach(e -> trashTargets.removeIf(a -> a.matches(e)));
            int trashCounter = 0;
            final InventoryTracker inventory = mod.getInventoryTracker();

            //TODO: {Meloweh} 1 slot has always be to be empty for some reason...
            if (!inventory.hasItem(itemTargets) && inventory.isInventoryFull()) {
                Debug.logMessage("Ducktape: This state is not supported yet. Terminating...");
                taskDataPackage.setFinished(true);
                return null;
            }

            for (final ItemTarget target : trashTargets) {
                final Item item = target.getMatches()[0];

                trashCounter += mod.getInventoryTracker().getInventorySlotsWithItem(item).size();

                if (trashCounter <= this.minChunkSlots && mod.getInventoryTracker().getInventorySlotsWithItem(Items.AIR).size() < 2) {
                    storeInChestTaskDataPackage.setFinished(false);
                    return storeInTargetChestTask;
                }
            }

            for (final Item item : complementaryMap.keySet()) {
                final int hasCount = mod.getInventoryTracker().getItemCount(item);
                final int targetCount = complementaryMap.get(item);
                if (hasCount < targetCount) {
                    return TaskCatalogue.getItemTask(new ItemTarget(item, targetCount - hasCount));
                }
            }
        }

        /*
        if (storeInChestTaskDataPackage.getFeedback().equals(TaskDataPackage.Feedback.CHEST_LOST)
                && !(MinecraftClient.getInstance().world.getBlockState(chestPos).getBlock() instanceof ChestBlock)) {
            return new PlaceBlockTask(chestPos, Blocks.CHEST);
        }*/

        storeInChestTaskDataPackage.setFinished(false);
        return storeInTargetChestTask;
    }

    //use static Store system to pass parameter and have multilevel finished tags.
    @Override
    public boolean isFinished(AltoClef mod) {
        return storeInChestTaskDataPackage.getFeedback().equals(TaskDataPackage.Feedback.CONTAINER_FULL)
                && storeInChestTaskDataPackage.isFinished() || taskDataPackage.isFinished();
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        //taskDataPackage.setFinished(true);
        this.paused = true;
    }

    @Override
    protected boolean isEqual(Task other) {
        return false;
    }

    @Override
    protected String toDebugString() {
        return "FillTargetChest...";
    }
}
