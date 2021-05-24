package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.ActionListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Use to walk through and search interconnected structures or biomes.
 * <p>
 * Example use cases: - Search a dark forest for a woodland mansion and avoid going to different biomes - Search a nether fortress for blaze
 * spawners - Search a stronghold for the portal
 */
public abstract class ChunkSearchTask extends Task {
    private final BlockPos startPoint;
    private final Object searchMutex = new Object();
    // We're either searched or will be searched later.
    private final Set<ChunkPos> consideredAlready = new HashSet<>();
    // We definitely were searched before.
    private final Set<ChunkPos> searchedAlready = new HashSet<>();
    private final ArrayList<ChunkPos> searchLater = new ArrayList<>();
    private final ArrayList<ChunkPos> justLoaded = new ArrayList<>();
    private final ActionListener<WorldChunk> chunkLoadEvent = new ActionListener<WorldChunk>() {
        @Override
        public void invoke(WorldChunk value) {
            onChunkLoad(value);
        }
    };
    private boolean _first = true;
    private boolean _finished;
    
    protected ChunkSearchTask(BlockPos startPoint) {
        this.startPoint = startPoint;
    }
    
    protected ChunkSearchTask(ChunkPos chunkPos) {
        this(chunkPos.getStartPos().add(1, 1, 1));
    }
    
    public Set<ChunkPos> getSearchedChunks() {
        return searchedAlready;
    }
    
    public boolean finished() {
        return _finished;
    }
    
    // Virtual
    protected ChunkPos getBestChunk(AltoClef mod, List<ChunkPos> chunks) {
        double lowestScore = Double.POSITIVE_INFINITY;
        ChunkPos bestChunk = null;
        for (ChunkPos toSearch : chunks) {
            double cx = (toSearch.getStartX() + toSearch.getEndX() + 1) / 2.0, cz = (toSearch.getStartZ() + toSearch.getEndZ() + 1) / 2.0;
            double px = mod.getPlayer().getX(), pz = mod.getPlayer().getZ();
            double distanceSq = (cx - px) * (cx - px) + (cz - pz) * (cz - pz);
            double distanceToCenterSq = new Vec3d(startPoint.getX() - cx, 0, startPoint.getZ() - cz).lengthSquared();
            double score = distanceSq + distanceToCenterSq * 0.8;
            if (score < lowestScore) {
                lowestScore = score;
                bestChunk = toSearch;
            }
        }
        return bestChunk;
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        return searchLater.isEmpty();
    }
    
    @Override
    protected void onStart(AltoClef mod) {
        /*
        _consideredAlready.clear();
        _searchLater.clear();
        _searchedAlready.clear();
         */
        //Debug.logMessage("(deleteme) start. Finished: " + _finished);
        if (_first) {
            _finished = false;
            _first = false;
            ChunkPos startPos = mod.getWorld().getChunk(startPoint).getPos();
            synchronized (searchMutex) {
                searchChunkOrQueueSearch(mod, startPos);
            }
        }
        
        mod.getOnChunkLoad().addListener(chunkLoadEvent);
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        
        // WTF This is a horrible idea.
        // Backup in case if chunk search fails?
        //onChunkLoad((WorldChunk) mod.getWorld().getChunk(mod.getPlayer().getBlockPos()));
        
        synchronized (searchMutex) {
            // Search all items from _justLoaded that we ought to search.
            for (ChunkPos justLoaded : justLoaded) {
                if (searchLater.contains(justLoaded)) {
                    // Search this one. If we succeed, we no longer need to search.
                    if (trySearchChunk(mod, justLoaded)) {
                        searchLater.remove(justLoaded);
                    }
                }
            }
            justLoaded.clear();
        }
        
        // Now that we have an updated map, go to the nearest
        ChunkPos closest = getBestChunk(mod, searchLater);
        
        if (closest == null) {
            _finished = true;
            Debug.logWarning("Failed to find any chunks to go to. If we finish, that means we scanned all possible chunks.");
            //Debug.logMessage("wtf??????: " + _finished);
            return null;
        }
        
        return new GetToChunkTask(closest);
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getOnChunkLoad().removeListener(chunkLoadEvent);
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof ChunkSearchTask) {
            ChunkSearchTask task = (ChunkSearchTask) obj;
            if (!task.startPoint.equals(startPoint)) return false;
            return isChunkSearchEqual(task);
        }
        return false;
    }
    
    private void searchChunkOrQueueSearch(AltoClef mod, ChunkPos pos) {
        // Don't search/consider this chunk again.
        if (consideredAlready.contains(pos)) {
            return;
        }
        consideredAlready.add(pos);
        
        if (!trySearchChunk(mod, pos)) {
            // We'll check it later if we haven't searched it.
            if (!searchedAlready.contains(pos)) {
                searchLater.add(pos);
            }
        }
    }
    
    /**
     * Try to search the chunk.
     *
     * @param pos chunk to search
     *
     * @return true if we're DONE searching this chunk false if we need to SEARCH IT IN PERSON
     */
    private boolean trySearchChunk(AltoClef mod, ChunkPos pos) {
        // Do NOT search later.
        if (searchedAlready.contains(pos)) {
            return true;
        }
        if (mod.getChunkTracker().isChunkLoaded(pos)) {
            searchedAlready.add(pos);
            if (isChunkPartOfSearchSpace(mod, pos)) {
                // This chunk may lead to more, so either search or enqueue its neighbors.
                searchChunkOrQueueSearch(mod, new ChunkPos(pos.x + 1, pos.z));
                searchChunkOrQueueSearch(mod, new ChunkPos(pos.x - 1, pos.z));
                searchChunkOrQueueSearch(mod, new ChunkPos(pos.x, pos.z + 1));
                searchChunkOrQueueSearch(mod, new ChunkPos(pos.x, pos.z - 1));
            }
            return true;
        }
        return false;
    }
    
    private void onChunkLoad(WorldChunk chunk) {
        if (chunk == null) return;
        synchronized (searchMutex) {
            if (!searchedAlready.contains(chunk.getPos())) {
                justLoaded.add(chunk.getPos());
            }
        }
    }
    
    protected abstract boolean isChunkPartOfSearchSpace(AltoClef mod, ChunkPos pos);
    
    protected abstract boolean isChunkSearchEqual(ChunkSearchTask other);
}
