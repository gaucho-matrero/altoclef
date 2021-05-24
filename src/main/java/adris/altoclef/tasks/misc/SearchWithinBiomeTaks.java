package adris.altoclef.tasks.misc;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.SearchChunksExploreTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Category;


public class SearchWithinBiomeTaks extends SearchChunksExploreTask {
    private final Category toSearch;
    
    public SearchWithinBiomeTaks(Category toSearch) {
        this.toSearch = toSearch;
    }
    
    @Override
    protected boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos) {
        Biome b = mod.getWorld().getBiome(pos.getStartPos().add(1, 1, 1));
        return b.getCategory() == toSearch;
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof SearchWithinBiomeTaks) {
            return ((SearchWithinBiomeTaks) obj).toSearch == toSearch;
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Searching for+within biome: " + toSearch;
    }
}
