package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.PickupDroppedItemTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class CollectCropTask extends ResourceTask {

    private final ItemTarget _cropToCollect;
    private final Item[] _cropSeed;
    private final Predicate<BlockPos> _ignoreBreak;
    private final Block[] _cropBlock;

    private final Set<BlockPos> _emptyCropland = new HashSet<>();

    private final Task _collectSeedTask;

    // To prevent infinite chunk-unload-reload loop bug
    private final HashSet<BlockPos> _wasFullyGrown = new HashSet<>();

    public CollectCropTask(ItemTarget cropToCollect, Block[] cropBlock, Item[] cropSeed, Predicate<BlockPos> ignoreBreak) {
        super(cropToCollect);
        _cropToCollect = cropToCollect;
        _cropSeed = cropSeed;
        _ignoreBreak = ignoreBreak;
        _cropBlock = cropBlock;
        _collectSeedTask = new PickupDroppedItemTask(new ItemTarget(cropSeed, 1), true);
    }

    public CollectCropTask(ItemTarget cropToCollect, Block[] cropBlock, Item... cropSeed) {
        this(cropToCollect, cropBlock, cropSeed, ignore -> false);
    }

    public CollectCropTask(ItemTarget cropToCollect, Block cropBlock, Item... cropSeed) {
        this(cropToCollect, new Block[]{cropBlock}, cropSeed);
    }

    public CollectCropTask(Item cropItem, int count, Block cropBlock, Item... cropSeed) {
        this(new ItemTarget(cropItem, count), cropBlock, cropSeed);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(_cropBlock);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        /*
         *   Filter the empty crop list to remove non-empty areas
         *   If _currentCropBreaking != null && _currentCropBreaking is EMPTY:
         *      Add _currentCropBreaking to empty cropland
         * - If empty cropland list not empty && mod.getSettings().shouldReplaceCrops() && we have crop seed in inventory:
         *      do to closest block task: interact with seed
         *   Do to closest block task:
         *      set _currentCropBreaking = block
         *      break the block
         */

        // Collect seeds if we need to.
        if (hasEmptyCrops(mod) && mod.getModSettings().shouldReplantCrops() && !mod.getInventoryTracker().hasItem(_cropSeed)) {
            if (_collectSeedTask.isActive() && !_collectSeedTask.isFinished(mod)) {
                setDebugState("Picking up dropped seeds");
                return _collectSeedTask;
            }
            if (mod.getEntityTracker().itemDropped(_cropSeed)) {
                Entity closest = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), _cropSeed);
                if (closest != null && closest.isInRange(mod.getPlayer(), 7)) {
                    // Trigger the collection of seeds.
                    return _collectSeedTask;
                }
            }
        }

        // Replant if we need to!
        if (shouldReplantNow(mod)) {
            setDebugState("Replanting...");
            // We guarantee that empty cropland list has valid empty blocks. We can purge at this stage.
            _emptyCropland.removeIf(blockPos -> !isEmptyCrop(mod, blockPos));
            assert !_emptyCropland.isEmpty();
            return new DoToClosestBlockTask(
                    () -> mod.getPlayer().getPos(),
                    blockPos -> new InteractWithBlockTask(new ItemTarget(_cropSeed, 1), Direction.UP, blockPos.down(), true),
                    pos -> Util.minItem(_emptyCropland, (block) -> block.getSquaredDistance(pos, false)), Blocks.FARMLAND); // Blocks.FARMLAND is useless to be put here
        }

        Predicate<BlockPos> invalidCrop = ignoreBlock -> {
            if (_ignoreBreak.test(ignoreBlock)) return true;
            // Breaking immature crops will only yield one output! This is a bad move.
            if (mod.getModSettings().shouldReplantCrops() && !isMature(mod, ignoreBlock)) return true;
            // Wheat must be mature always.
            return mod.getWorld().getBlockState(ignoreBlock).getBlock() == Blocks.WHEAT && !isMature(mod, ignoreBlock);
        };

        // Dimension
        if (isInWrongDimension(mod) && !mod.getBlockTracker().anyFound(invalidCrop, _cropBlock)) {
            return getToCorrectDimensionTask(mod);
        }

        // Break crop blocks.
        setDebugState("Breaking crops.");
        return new DoToClosestBlockTask(
                () -> mod.getPlayer().getPos(),
                blockPos -> {
                    _emptyCropland.add(blockPos);
                    return new DestroyBlockTask(blockPos);
                }, pos -> mod.getBlockTracker().getNearestTracking(pos, invalidCrop, _cropBlock)
        );
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(_cropBlock);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        // Don't stop while we're replanting crops.
        if (shouldReplantNow(mod)) {
            return false;
        }
        return super.isFinished(mod);
    }

    private boolean shouldReplantNow(AltoClef mod) {
        return mod.getModSettings().shouldReplantCrops() && hasEmptyCrops(mod) && mod.getInventoryTracker().hasItem(_cropSeed);
    }

    private boolean hasEmptyCrops(AltoClef mod) {
        for (BlockPos pos : _emptyCropland) {
            if (isEmptyCrop(mod, pos)) return true;
        }
        return false;
    }

    private boolean isEmptyCrop(AltoClef mod, BlockPos pos) {
        return WorldUtil.isAir(mod, pos);
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        if (obj instanceof CollectCropTask) {
            CollectCropTask task = (CollectCropTask) obj;
            return Util.arraysEqual(task._cropSeed, _cropSeed) && Util.arraysEqual(task._cropBlock, _cropBlock) && task._cropToCollect.equals(_cropToCollect);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting crops: " + _cropToCollect;
    }


    private boolean isMature(AltoClef mod, BlockPos blockPos) {
        // Chunk needs to be loaded for wheat maturity to be checked.
        if (!mod.getChunkTracker().isChunkLoaded(blockPos) || mod.getBlockTracker().unreachable(blockPos)) {
            return _wasFullyGrown.contains(blockPos);
        }
        // Prune if we're not mature/fully grown wheat.
        BlockState s = mod.getWorld().getBlockState(blockPos);
        if (s.getBlock() instanceof CropBlock) {
            CropBlock crop = (CropBlock) s.getBlock();
            boolean mature = crop.isMature(s);
            if (_wasFullyGrown.contains(blockPos)) {
                if (!mature) _wasFullyGrown.remove(blockPos);
            } else {
                if (mature) _wasFullyGrown.add(blockPos);
            }
            return mature;
        }
        // Not a crop block.
        return false;
    }
}
