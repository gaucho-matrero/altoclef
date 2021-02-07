package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.SearchChunksExploreTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.ChunkPos;

public class SearchNetherFortressTask extends SearchChunksExploreTask {

    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getCurrentDimension() != Dimension.NETHER) {
            Debug.logWarning("You're not in the nether, don't search for blaze spawner...");
            return null;
        }
        return super.onTick(mod);
    }

    @Override
    protected boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos) {
        // Search nether fortresses
        return mod.getChunkTracker().scanChunk(pos, (block) -> mod.getWorld().getBlockState(block).getBlock() == Blocks.NETHER_BRICKS);
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof SearchNetherFortressTask;
    }

    @Override
    protected String toDebugString() {
        return "Searching for nether fortress...";
    }
}
