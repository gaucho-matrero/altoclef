package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.misc.SearchWithinBiomeTaks;
import adris.altoclef.tasksystem.Task;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CocoaBlock;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.HashSet;
import java.util.function.Predicate;

public class CollectCocoaBeansTask extends ResourceTask {
    private final int _count;
    private final HashSet<BlockPos> _wasFullyGrown = new HashSet<>();

    public CollectCocoaBeansTask(int targetCount) {
        super(Items.COCOA_BEANS, targetCount);
        _count = targetCount;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.COCOA);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        Predicate<BlockPos> invalidCocoaCheck = (blockPos) -> {
            if (!mod.getChunkTracker().isChunkLoaded(blockPos)) {
                return !_wasFullyGrown.contains(blockPos);
            }

            BlockState s = mod.getWorld().getBlockState(blockPos);
            boolean mature = s.get(CocoaBlock.AGE) == 2;
            if (_wasFullyGrown.contains(blockPos)) {
                if (!mature) _wasFullyGrown.remove(blockPos);
            } else {
                if (mature) _wasFullyGrown.add(blockPos);
            }
            return !mature;
        };

        // Break mature cocoa blocks
        if (mod.getBlockTracker().anyFound(invalidCocoaCheck, Blocks.COCOA)) {
            setDebugState("Breaking cocoa blocks");
            return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(), DestroyBlockTask::new, pos -> mod.getBlockTracker().getNearestTracking(pos, invalidCocoaCheck), Blocks.COCOA);
        }

        // Dimension
        if (isInWrongDimension(mod)) {
            return getToCorrectDimensionTask(mod);
        }

        // Search for jungles
        setDebugState("Exploring around jungles");
        return new SearchWithinBiomeTaks(Biome.Category.JUNGLE);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.COCOA);
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        return obj instanceof CollectCocoaBeansTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " cocoa beans.";
    }
}
