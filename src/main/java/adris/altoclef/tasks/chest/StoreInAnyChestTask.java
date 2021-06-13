package adris.altoclef.tasks.chest;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.function.Predicate;

public class StoreInAnyChestTask extends Task {

    private final HashSet<BlockPos> _dungeonChests = new HashSet<>();
    private final HashSet<BlockPos> _nonDungeonChests = new HashSet<>();

    private final ItemTarget[] _targets;

    private final MovementProgressChecker _progressChecker = new MovementProgressChecker(2);
    private BlockPos _currentChestTry = null;

    public StoreInAnyChestTask(ItemTarget... targets) {
        _targets = targets;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.CHEST);
        _dungeonChests.clear();
        _nonDungeonChests.clear();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        Predicate<BlockPos> invalidChest = chest -> {

            // If block above can't be broken, we can't open this one.
            if (!WorldUtil.canBreak(mod, chest.up())) return true;

            ContainerTracker.ChestData data = mod.getContainerTracker().getChestMap().getCachedChestData(chest);
            if (data != null && data.isFull()) return true;

            if (mod.getModSettings().shouldAvoidSearchingForDungeonChests()) {
                if (_dungeonChests.contains(chest)) return true;
                if (_nonDungeonChests.contains(chest)) return false;
                // Spawner
                int range = 6;
                for (int dx = -range; dx <= range; ++dx) {
                    for (int dz = -range; dz <= range; ++dz) {
                        BlockPos offset = chest.add(dx, 0, dz);
                        if (mod.getWorld().getBlockState(offset).getBlock() == Blocks.SPAWNER) {
                            _dungeonChests.add(chest);
                            return true;
                        }
                    }
                }
                _nonDungeonChests.add(chest);
            }
            return false;
        };

        if (mod.getBlockTracker().anyFound(invalidChest, Blocks.CHEST)) {

            setDebugState("Going to chest and depositing items");

            if (!_progressChecker.check(mod) && _currentChestTry != null) {
                Debug.logMessage("Failed to open chest. Suggesting it may be unreachable.");
                mod.getBlockTracker().requestBlockUnreachable(_currentChestTry, 2);
                _currentChestTry = null;
                _progressChecker.reset();
            }

            return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(),
                    blockPos -> {
                        if (_currentChestTry != blockPos) {
                            _progressChecker.reset();
                        }
                        _currentChestTry = blockPos;
                        // If block above is solid, break it.
                        if (WorldUtil.isSolid(mod, blockPos.up())) {
                            return new DestroyBlockTask(blockPos.up());
                        }
                        return new StoreInChestTask(blockPos, _targets);
                    },
                    pos -> mod.getBlockTracker().getNearestTracking(pos, invalidChest, Blocks.CHEST),
                    Blocks.CHEST);
        }

        _progressChecker.reset();
        // Craft + place chest nearby
        setDebugState("Placing chest nearby");
        if (mod.getInventoryTracker().hasItem(Items.CHEST)) {
            return new PlaceBlockNearbyTask(cantPlace -> {
                // Above must be air OR breakable.
                if (WorldUtil.isAir(mod, cantPlace.up())) return false;
                return !WorldUtil.canBreak(mod, cantPlace.up());
            }, Blocks.CHEST);
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
            return Util.arraysEqual(task._targets, _targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Storing in any chest: " + Util.arrayToString(_targets);
    }
}
