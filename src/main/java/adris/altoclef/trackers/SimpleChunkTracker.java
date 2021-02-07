package adris.altoclef.trackers;

import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Keeps track of currently loaded chunks. That's it.
public class SimpleChunkTracker {

    private Set<ChunkPos> _loaded = new HashSet<>();

    public void onLoad(ChunkPos pos) {
        _loaded.add(pos);
    }
    public void onUnload(ChunkPos pos) {
        _loaded.remove(pos);
    }

    public boolean isChunkLoaded(ChunkPos pos) {
        return _loaded.contains(pos);
    }
    public List<ChunkPos> getLoadedChunks() {
        return new ArrayList<>(_loaded);
    }
}
