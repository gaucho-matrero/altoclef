package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class StoreInAnyContainerTask extends Task {

    private final String _uniqueId;
    private final Function<AltoClef, Optional<ItemTarget>> _getNextItemTargetToDump;
    private final Predicate<BlockPos> _acceptableContainer;

    private final HashSet<BlockPos> _dungeonChests = new HashSet<>();
    private final HashSet<BlockPos> _nonDungeonChests = new HashSet<>();

    private final MovementProgressChecker _progressChecker = new MovementProgressChecker(2);
    private BlockPos _currentChestTry = null;

    private static final Block[] TO_SCAN = Stream.concat(Arrays.stream(new Block[]{Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL}), Arrays.stream(ItemHelper.itemsToBlocks(ItemHelper.SHULKER_BOXES))).toArray(Block[]::new);

    public StoreInAnyContainerTask(String uniqueId, Function<AltoClef, Optional<ItemTarget>> getNextItemTargetToDump, Predicate<BlockPos> acceptableContainer) {
        _uniqueId = uniqueId;
        _getNextItemTargetToDump = getNextItemTargetToDump;
        _acceptableContainer = acceptableContainer;
    }
    public StoreInAnyContainerTask(String uniqueId, Function<AltoClef, Optional<ItemTarget>> getNextItemTargetToDump) {
        this(uniqueId, getNextItemTargetToDump, p -> true);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(TO_SCAN);
        _dungeonChests.clear();
        _nonDungeonChests.clear();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        Predicate<BlockPos> validContainer = containerPos -> {

            // If it's a chest and the block above can't be broken, we can't open this one.
            boolean isChest = WorldHelper.isChest(mod, containerPos);
            if (isChest && WorldHelper.isSolid(mod, containerPos.up()) && !WorldHelper.canBreak(mod, containerPos.up())) return false;

            if (!_acceptableContainer.test(containerPos))
                return false;

            Optional<ContainerCache> data = mod.getItemStorage().getContainerAtPosition(containerPos);

            if (data.isPresent() && data.get().isFull()) return false;

            if (isChest && mod.getModSettings().shouldAvoidSearchingForDungeonChests()) {
                boolean cachedDungeon = _dungeonChests.contains(containerPos) && !_nonDungeonChests.contains(containerPos);
                if (cachedDungeon) {
                    return false;
                }
                // Spawner
                int range = 6;
                for (int dx = -range; dx <= range; ++dx) {
                    for (int dz = -range; dz <= range; ++dz) {
                        BlockPos offset = containerPos.add(dx, 0, dz);
                        if (mod.getWorld().getBlockState(offset).getBlock() == Blocks.SPAWNER) {
                            _dungeonChests.add(containerPos);
                            return false;
                        }
                    }
                }
                _nonDungeonChests.add(containerPos);
            }
            return true;
        };

        if (mod.getBlockTracker().anyFound(validContainer, TO_SCAN)) {

            setDebugState("Going to container and depositing items");

            if (!_progressChecker.check(mod) && _currentChestTry != null) {
                Debug.logMessage("Failed to open container. Suggesting it may be unreachable.");
                mod.getBlockTracker().requestBlockUnreachable(_currentChestTry, 2);
                _currentChestTry = null;
                _progressChecker.reset();
            }

            return new DoToClosestBlockTask(
                    blockPos -> {
                        if (_currentChestTry != blockPos) {
                            _progressChecker.reset();
                        }
                        _currentChestTry = blockPos;
                        return new DumpInContainerTask(blockPos, _uniqueId, _getNextItemTargetToDump, altoClef -> null);
                    },
                    validContainer,
                    TO_SCAN);
        }

        _progressChecker.reset();
        // Craft + place chest nearby
        for (Block couldPlace : TO_SCAN) {
            if (mod.getItemStorage().hasItem(couldPlace.asItem())) {
                setDebugState("Placing container nearby");
                return new PlaceBlockNearbyTask(canPlace -> {
                    // For chests, above must be air OR breakable.
                    if (WorldHelper.isChest(couldPlace)) {
                        return WorldHelper.isAir(mod, canPlace.up()) || WorldHelper.canBreak(mod, canPlace.up());
                    }
                    return true;
                }, couldPlace);
            }
        }
        setDebugState("Obtaining a chest item (by default)");
        return TaskCatalogue.getItemTask(Items.CHEST, 1);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _getNextItemTargetToDump.apply(mod).isEmpty();
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(TO_SCAN);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof StoreInAnyContainerTask task) {
            return Objects.equals(task._uniqueId, _uniqueId);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Storing in any container: " + _uniqueId;
    }
}
