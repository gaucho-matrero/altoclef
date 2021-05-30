package adris.altoclef.tasks.resources;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractItemWithBlockTask;
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
    private final ItemTarget cropToCollect;
    private final Item[] cropSeed;
    private final Predicate<? super BlockPos> ignoreBreak;
    private final Block[] cropBlock;
    private final Set<BlockPos> emptyCropland = new HashSet<>();
    private final Task collectSeedTask;
    // To prevent infinite chunk-unload-reload loop bug
    private final HashSet<BlockPos> wasFullyGrown = new HashSet<>();

    public CollectCropTask(ItemTarget cropToCollect, Block[] cropBlock, Item[] cropSeed, Predicate<? super BlockPos> ignoreBreak) {
        super(cropToCollect);
        this.cropToCollect = cropToCollect;
        this.cropSeed = cropSeed;
        this.ignoreBreak = ignoreBreak;
        this.cropBlock = cropBlock;
        collectSeedTask = new PickupDroppedItemTask(new ItemTarget(cropSeed, 1), true);
    }

    public CollectCropTask(ItemTarget cropToCollect, Block[] cropBlock, Item... cropSeed) {
        this(cropToCollect, cropBlock, cropSeed, ignore -> false);
    }

    public CollectCropTask(ItemTarget cropToCollect, Block cropBlock, Item... cropSeed) {
        this(cropToCollect, new Block[]{ cropBlock }, cropSeed);
    }

    public CollectCropTask(Item cropItem, int count, Block cropBlock, Item... cropSeed) {
        this(new ItemTarget(cropItem, count), cropBlock, cropSeed);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        // Don't stop while we're replanting crops.
        if (shouldReplantNow(mod)) {
            return false;
        }
        return super.isFinished(mod);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(cropBlock);
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
        if (hasEmptyCrops(mod) && mod.getModSettings().shouldReplantCrops() && !mod.getInventoryTracker().hasItem(cropSeed)) {
            if (collectSeedTask.isActive() && !collectSeedTask.isFinished(mod)) {
                setDebugState("Picking up dropped seeds");
                return collectSeedTask;
            }
            if (mod.getEntityTracker().itemDropped(cropSeed)) {
                Entity closest = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), cropSeed);
                if (closest != null && closest.isInRange(mod.getPlayer(), 7)) {
                    // Trigger the collection of seeds.
                    return collectSeedTask;
                }
            }
        }

        // Replant if we need to!
        if (shouldReplantNow(mod)) {
            setDebugState("Replanting...");
            // We guarantee that empty cropland list has valid empty blocks. We can purge at this stage.
            emptyCropland.removeIf(blockPos -> !isEmptyCrop(mod, blockPos));
            return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(),
                                            blockPos -> new InteractItemWithBlockTask(new ItemTarget(cropSeed, 1), Direction.UP,
                                                                                      blockPos.down(), true),
                                            pos -> Util.minItem(emptyCropland, (block) -> block.getSquaredDistance(pos, false)),
                                            Blocks.FARMLAND); // Blocks.FARMLAND is useless to be put here
        }

        Predicate<BlockPos> invalidCrop = ignoreBlock -> {
            if (ignoreBreak.test(ignoreBlock)) return true;
            // Breaking immature crops will only yield one output! This is a bad move.
            if (mod.getModSettings().shouldReplantCrops() && !isMature(mod, ignoreBlock)) return true;
            // Wheat must be mature always.
            return mod.getWorld().getBlockState(ignoreBlock).getBlock() == Blocks.WHEAT && !isMature(mod, ignoreBlock);
        };

        // Dimension
        if (isInWrongDimension(mod) && !mod.getBlockTracker().anyFound(invalidCrop, cropBlock)) {
            return getToCorrectDimensionTask(mod);
        }

        // Break crop blocks.
        setDebugState("Breaking crops.");
        return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(), blockPos -> {
            emptyCropland.add(blockPos);
            return new DestroyBlockTask(blockPos);
        }, pos -> mod.getBlockTracker().getNearestTracking(pos, invalidCrop, cropBlock));
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(cropBlock);
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        if (obj instanceof CollectCropTask) {
            CollectCropTask task = (CollectCropTask) obj;
            return Util.arraysEqual(task.cropSeed, cropSeed) && Util.arraysEqual(task.cropBlock, cropBlock) &&
                   task.cropToCollect.equals(cropToCollect);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting crops: " + cropToCollect;
    }

    private boolean shouldReplantNow(AltoClef mod) {
        return mod.getModSettings().shouldReplantCrops() && hasEmptyCrops(mod) && mod.getInventoryTracker().hasItem(cropSeed);
    }

    private boolean hasEmptyCrops(AltoClef mod) {
        for (BlockPos pos : emptyCropland) {
            if (isEmptyCrop(mod, pos)) return true;
        }
        return false;
    }

    private boolean isEmptyCrop(AltoClef mod, BlockPos pos) {
        return WorldUtil.isAir(mod, pos);
    }

    private boolean isMature(AltoClef mod, BlockPos blockPos) {
        // Chunk needs to be loaded for wheat maturity to be checked.
        if (!mod.getChunkTracker().isChunkLoaded(blockPos) || mod.getBlockTracker().unreachable(blockPos)) {
            return wasFullyGrown.contains(blockPos);
        }
        // Prune if we're not mature/fully grown wheat.
        BlockState s = mod.getWorld().getBlockState(blockPos);
        if (s.getBlock() instanceof CropBlock) {
            CropBlock crop = (CropBlock) s.getBlock();
            boolean mature = crop.isMature(s);
            if (wasFullyGrown.contains(blockPos)) {
                if (!mature) wasFullyGrown.remove(blockPos);
            } else {
                if (mature) wasFullyGrown.add(blockPos);
            }
            return mature;
        }
        // Not a crop block.
        return false;
    }
}
