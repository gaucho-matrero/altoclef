package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.ActionListener;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SearchChunksExploreTask extends Task {

    private final Object _searcherMutex = new Object();
    private final Set<ChunkPos> _alreadyExplored = new HashSet<>();
    private ChunkSearchTask _searcher;
    private AltoClef _mod;
    private final ActionListener<WorldChunk> chunkLoadEvent = new ActionListener<WorldChunk>(this::onChunkLoad);

    // Virtual
    protected ChunkPos getBestChunkOverride(AltoClef mod, List<ChunkPos> chunks) {
        return null;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _mod = mod;
        mod.getOnChunkLoad().addListener(chunkLoadEvent);

        resetSearch(mod);
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
            } else if (_searcher.finished()) {
                setDebugState("Searching for target object...");
                Debug.logMessage("Search finished.");
                _alreadyExplored.addAll(_searcher.getSearchedChunks());
                _searcher = null;
            }
            //Debug.logMessage("wtf: " + (_searcher == null? "(null)" :_searcher.finished()));
            setDebugState("Searching within chunks...");
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
                    Debug.logMessage("New searcher: " + chunk.getPos());
                    _searcher = new SearchSubTask(chunk.getPos());
                }
            }
        }
    }

    protected Task getWanderTask(AltoClef mod) {
        return new TimeoutWanderTask(true);
    }

    protected abstract boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos);

    public boolean failedSearch() {
        return _searcher == null;
    }

    public void resetSearch(AltoClef mod) {
        //Debug.logMessage("Search reset");
        _searcher = null;
        _alreadyExplored.clear();
        // We want to search the currently loaded chunks too!!!
        for (ChunkPos start : mod.getChunkTracker().getLoadedChunks()) {
            onChunkLoad(mod.getWorld().getChunk(start.x, start.z));
        }
    }

    class SearchSubTask extends ChunkSearchTask {

        public SearchSubTask(ChunkPos start) {
            super(start);
        }

        @Override
        protected boolean isChunkPartOfSearchSpace(AltoClef mod, ChunkPos pos) {
            return isChunkWithinSearchSpace(mod, pos);
        }

        @Override
        public ChunkPos getBestChunk(AltoClef mod, List<ChunkPos> chunks) {
            ChunkPos override = getBestChunkOverride(mod, chunks);
            if (override != null) return override;
            return super.getBestChunk(mod, chunks);
        }

        @Override
        protected boolean isChunkSearchEqual(ChunkSearchTask other) {
            // Since we're keeping track of "_searcher", we expect the subchild routine to ALWAYS be consistent!
            return other == this;//return other instanceof SearchSubTask;
        }

        @Override
        protected String toDebugString() {
            return "Searching chunks...";
        }
    }

}
