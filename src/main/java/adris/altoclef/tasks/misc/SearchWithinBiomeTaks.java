package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.SearchChunksExploreTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;

public class SearchWithinBiomeTaks extends SearchChunksExploreTask {

    private final Biome.Category _toSearch;

    public SearchWithinBiomeTaks(Biome.Category toSearch) {
        _toSearch = toSearch;
    }

    @Override
    protected boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos) {
        Biome b = mod.getWorld().getBiome(pos.getStartPos().add(1, 1, 1));
        return b.getCategory() == _toSearch;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof SearchWithinBiomeTaks) {
            return ((SearchWithinBiomeTaks) obj)._toSearch == _toSearch;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Searching for+within biome: " + _toSearch;
    }
}
