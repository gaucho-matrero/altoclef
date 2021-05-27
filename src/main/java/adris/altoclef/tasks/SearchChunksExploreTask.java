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
    private final Object searcherMutex = new Object();
    private final Set<ChunkPos> alreadyExplored = new HashSet<>();
    private ChunkSearchTask searcher;
    private AltoClef mod;
    private final ActionListener<WorldChunk> chunkLoadEvent = new ActionListener<WorldChunk>() {
        @Override
        public void invoke(WorldChunk value) {
            onChunkLoad(value);
        }
    };

    // Virtual
    protected ChunkPos getBestChunkOverride(AltoClef mod, List<ChunkPos> chunks) {
        return null;
    }

    @Override
    protected void onStart(AltoClef mod) {
        this.mod = mod;
        mod.getOnChunkLoad().addListener(chunkLoadEvent);

        resetSearch(mod);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        synchronized (searcherMutex) {
            if (searcher == null) {
                setDebugState("Exploring/Searching for valid chunk");
                // Explore
                return getWanderTask(mod);
            }

            if (searcher.isActive() && searcher.isFinished(mod)) {
                Debug.logWarning("Target object search failed.");
                alreadyExplored.addAll(searcher.getSearchedChunks());
                searcher = null;
            } else if (searcher.finished()) {
                setDebugState("Searching for target object...");
                Debug.logMessage("Search finished.");
                alreadyExplored.addAll(searcher.getSearchedChunks());
                searcher = null;
            }
            //Debug.logMessage("wtf: " + (_searcher == null? "(null)" :_searcher.finished()));
            setDebugState("Searching within chunks...");
            return searcher;
        }
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getOnChunkLoad().removeListener(chunkLoadEvent);
    }

    // When we find that desert, start our search there.
    private void onChunkLoad(WorldChunk chunk) {
        if (searcher != null) return;
        if (!this.isActive()) return;
        if (isChunkWithinSearchSpace(mod, chunk.getPos())) {
            synchronized (searcherMutex) {
                if (!alreadyExplored.contains(chunk.getPos())) {
                    Debug.logMessage("New searcher: " + chunk.getPos());
                    searcher = new SearchSubTask(chunk.getPos(), this);
                }
            }
        }
    }

    protected Task getWanderTask(AltoClef mod) {
        return new TimeoutWanderTask(true);
    }

    protected abstract boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos);

    public boolean failedSearch() {
        return searcher == null;
    }

    public void resetSearch(AltoClef mod) {
        //Debug.logMessage("Search reset");
        searcher = null;
        alreadyExplored.clear();
        // We want to search the currently loaded chunks too!!!
        for (ChunkPos start : mod.getChunkTracker().getLoadedChunks()) {
            onChunkLoad(mod.getWorld().getChunk(start.x, start.z));
        }
    }

    public static class SearchSubTask extends ChunkSearchTask {
        private final SearchChunksExploreTask searchChunksExploreTask;

        SearchSubTask(ChunkPos start, SearchChunksExploreTask searchChunksExploreTask) {
            super(start);
            this.searchChunksExploreTask = searchChunksExploreTask;
        }

        @Override
        public ChunkPos getBestChunk(AltoClef mod, List<ChunkPos> chunks) {
            ChunkPos override = searchChunksExploreTask.getBestChunkOverride(mod, chunks);
            if (override != null) return override;
            return super.getBestChunk(mod, chunks);
        }

        @Override
        protected boolean isChunkPartOfSearchSpace(AltoClef mod, ChunkPos pos) {
            return searchChunksExploreTask.isChunkWithinSearchSpace(mod, pos);
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
