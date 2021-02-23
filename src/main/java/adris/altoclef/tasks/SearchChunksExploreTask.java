package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.ActionListener;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public abstract class SearchChunksExploreTask extends Task {

    private ChunkSearchTask _searcher;

    private final Object _searcherMutex = new Object();

    private AltoClef _mod;

    private final Set<ChunkPos> _alreadyExplored = new HashSet<>();

    private ActionListener<WorldChunk> chunkLoadEvent = new ActionListener<WorldChunk>() {
        @Override
        public void invoke(WorldChunk value) {
            onChunkLoad(value);
        }
    };

    @Override
    protected void onStart(AltoClef mod) {
        _mod = mod;
        mod.getOnChunkLoad().addListener(chunkLoadEvent);

        // We want to search the currently loaded chunks too!!!
        for (ChunkPos start : mod.getChunkTracker().getLoadedChunks()) {
            onChunkLoad(mod.getWorld().getChunk(start.x, start.z));
        }
    }

    @Override
    protected Task onTick(AltoClef mod) {
        synchronized (_searcherMutex) {
            if (_searcher == null) {
                setDebugState("Exploring/Searching for valid chunk");
                // Explore
                return getWanderTask(mod);
            }

            if (_searcher.isActive() && _searcher.isFinished(mod)) {
                Debug.logWarning("Target object search failed.");
                _alreadyExplored.addAll(_searcher.getSearchedChunks());
                _searcher = null;
            }
            setDebugState("Searching for target object...");
            return _searcher;
        }
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getOnChunkLoad().removeListener(chunkLoadEvent);
    }

    // When we find that desert, start our search there.
    private void onChunkLoad(WorldChunk chunk) {
        if (_searcher != null) return;
        if (!this.isActive()) return;
        if (isChunkWithinSearchSpace(_mod, chunk.getPos())) {
            synchronized (_searcherMutex) {
                if (!_alreadyExplored.contains(chunk.getPos())) {
                    _searcher = new SearchSubTask(chunk.getPos());
                }
            }
        }
    }

    protected Task getWanderTask(AltoClef mod) {
        return new TimeoutWanderTask(true);
    }

    protected abstract boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos);

    class SearchSubTask extends ChunkSearchTask {

        public SearchSubTask(ChunkPos start) {
            super(start);
        }

        @Override
        protected boolean isChunkPartOfSearchSpace(AltoClef mod, ChunkPos pos) {

            return isChunkWithinSearchSpace(mod, pos);
        }

        @Override
        protected boolean isChunkSearchEqual(ChunkSearchTask other) {
            return other instanceof SearchSubTask;
        }

        @Override
        protected String toDebugString() {
            return "Searching chunks...";
        }
    }

}
