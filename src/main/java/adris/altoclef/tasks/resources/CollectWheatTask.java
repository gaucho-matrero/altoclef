package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;

public class CollectWheatTask extends ResourceTask {

    private final int _count;

    // To prevent infinite chunk-unload-reload loop bug
    private final HashSet<BlockPos> _wasFullyGrown = new HashSet<>();

    public CollectWheatTask(int targetCount) {
        super(Items.WHEAT, targetCount);
        _count = targetCount;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.HAY_BLOCK);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // We may have enough hay blocks to meet our needs.
        int potentialCount = mod.getInventoryTracker().getItemCount(Items.WHEAT) + 9*mod.getInventoryTracker().getItemCount(Items.HAY_BLOCK);
        if (potentialCount >= _count) {
            setDebugState("Crafting wheat");
            return new CraftInInventoryTask(new ItemTarget(Items.WHEAT, _count), CraftingRecipe.newShapedRecipe("wheat", new ItemTarget[]{new ItemTarget(Items.HAY_BLOCK, 1), null, null, null}, 9));
        }
        if (mod.getBlockTracker().anyFound(Blocks.HAY_BLOCK) || mod.getEntityTracker().itemDropped(Items.HAY_BLOCK)) {
            return new MineAndCollectTask(Items.HAY_BLOCK, 999, new Block[]{Blocks.HAY_BLOCK}, MiningRequirement.HAND);
        }
        // Collect wheat
        return new DoToClosestBlockTask(
                () -> mod.getPlayer().getPos(),
                DestroyBlockTask::new,
                pos -> mod.getBlockTracker().getNearestTracking(pos,
                        isInvalid -> !isWheatMature(mod, isInvalid),
                        Blocks.WHEAT),
                Blocks.WHEAT
        );
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.HAY_BLOCK);
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        return obj instanceof CollectWheatTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " wheat.";
    }

    private boolean isWheatMature(AltoClef mod, BlockPos blockPos) {
        // Chunk needs to be loaded for wheat maturity to be checked.
        if (!mod.getChunkTracker().isChunkLoaded(blockPos) || mod.getBlockTracker().unreachable(blockPos)) {
            return _wasFullyGrown.contains(blockPos);
        }
        // Prune if we're not mature/fully grown wheat.
        BlockState s = mod.getWorld().getBlockState(blockPos);
        CropBlock crop = (CropBlock) s.getBlock();
        boolean mature = crop.isMature(s);
        if (_wasFullyGrown.contains(blockPos)) {
            if (!mature) _wasFullyGrown.remove(blockPos);
        } else {
            if (mature) _wasFullyGrown.add(blockPos);
        }
        return mature;
    }
}
