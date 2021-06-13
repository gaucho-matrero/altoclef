package adris.altoclef.trackers;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// Keeps track of currently loaded chunks. That's it.
public class SimpleChunkTracker {

    private final AltoClef _mod;
    private final Set<ChunkPos> _loaded = new HashSet<>();

    public SimpleChunkTracker(AltoClef mod) {
        _mod = mod;
    }

    public void onLoad(ChunkPos pos) {
        //Debug.logInternal("LOADED: " + pos);
        _loaded.add(pos);
    }

    public void onUnload(ChunkPos pos) {
        //Debug.logInternal("unloaded: " + pos);
        _loaded.remove(pos);
    }

    public boolean isChunkLoaded(ChunkPos pos) {
        return !(_mod.getWorld().getChunk(pos.x, pos.z) instanceof EmptyChunk);
    }

    public boolean isChunkLoaded(BlockPos pos) {
        return isChunkLoaded(new ChunkPos(pos));
    }

    public List<ChunkPos> getLoadedChunks() {
        List<ChunkPos> result = new ArrayList<>(_loaded);
        // Only show LOADED chunks.
        result = result.stream()
                .filter(this::isChunkLoaded)
                .distinct()
                .collect(Collectors.toList());
        return result;
    }

    public boolean scanChunk(ChunkPos chunk, Predicate<BlockPos> onBlock) {
        if (!isChunkLoaded(chunk)) return false;
        //Debug.logInternal("SCANNED CHUNK " + chunk.toString());
        for (int xx = chunk.getStartX(); xx <= chunk.getEndX(); ++xx) {
            for (int yy = 0; yy <= 255; ++yy) {
                for (int zz = chunk.getStartZ(); zz <= chunk.getEndZ(); ++zz) {
                    if (onBlock.test(new BlockPos(xx, yy, zz))) return true;
                }
            }
        }
        return false;
    }

    public void reset(AltoClef mod) {
        Debug.logInternal("CHUNKS RESET");
        _loaded.clear();
    }

    public void scanChunk(ChunkPos chunk, Consumer<BlockPos> onBlock) {
        scanChunk(chunk, (block) -> false);
    }
}
