package adris.altoclef.tasks.chest;


import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.function.Predicate;


public class StoreInAnyChestTask extends Task {
    private final HashSet<BlockPos> dungeonChests = new HashSet<>();
    private final HashSet<BlockPos> nonDungeonChests = new HashSet<>();
    private final ItemTarget[] targets;
    
    public StoreInAnyChestTask(ItemTarget... targets) {
        this.targets = targets;
    }
    
    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.CHEST);
        dungeonChests.clear();
        nonDungeonChests.clear();
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        Predicate<BlockPos> invalidChest = chest -> {
            ContainerTracker.ChestData data = mod.getContainerTracker().getChestMap().getCachedChestData(chest);
            if (data != null && data.isFull()) return true;
            
            if (mod.getModSettings().shouldAvoidSearchingForDungeonChests()) {
                if (dungeonChests.contains(chest)) return true;
                if (nonDungeonChests.contains(chest)) return false;
                // Spawner
                int range = 6;
                for (int dx = -range; dx <= range; ++dx) {
                    for (int dz = -range; dz <= range; ++dz) {
                        BlockPos offset = chest.add(dx, 0, dz);
                        if (mod.getWorld().getBlockState(offset).getBlock() == Blocks.SPAWNER) {
                            dungeonChests.add(chest);
                            return true;
                        }
                    }
                }
                nonDungeonChests.add(chest);
            }
            return false;
        };
        
        if (mod.getBlockTracker().anyFound(invalidChest, Blocks.CHEST)) {
            setDebugState("Going to chest and depositing items");
            return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(), blockPos -> new StoreInChestTask(blockPos, targets),
                                            pos -> mod.getBlockTracker().getNearestTracking(pos, invalidChest, Blocks.CHEST), Blocks.CHEST);
        }
        
        // Craft + place chest nearby
        setDebugState("Placing chest nearby");
        if (mod.getInventoryTracker().hasItem(Items.CHEST)) {
            return new PlaceBlockNearbyTask(Blocks.CHEST);
        }
        return TaskCatalogue.getItemTask("chest", 1);
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.CHEST);
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof StoreInAnyChestTask) {
            StoreInAnyChestTask task = (StoreInAnyChestTask) obj;
            return Util.arraysEqual(task.targets, targets);
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Storing in any chest: " + Util.arrayToString(targets);
    }
}
