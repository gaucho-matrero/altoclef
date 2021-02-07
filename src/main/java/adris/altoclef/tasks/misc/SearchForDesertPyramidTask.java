package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.ChunkSearchTask;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

// TODO: Standardize as "search chunk explore task" or something like that.
public class SearchForDesertPyramidTask extends Task {

    private SearchDesertTask _searcher;

    private final Object _searcherMutex = new Object();

    private AltoClef _mod;

    private final Set<ChunkPos> _alreadyExplored = new HashSet<>();

    private Task _finalMovementTask;

    @Override
    protected void onStart(AltoClef mod) {
        _mod = mod;
        mod.getOnChunkLoad().addListener(this::onChunkLoad);

        // We want to search the currently loaded chunks too!!!
        for (ChunkPos start : mod.getChunkTracker().getLoadedChunks()) {
            onChunkLoad(mod.getWorld().getChunk(start.x, start.z));
        }

        // Track desert pyramid blocks
        mod.getBlockTracker().trackBlock(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        synchronized (_searcherMutex) {
            if (_searcher == null) {
                setDebugState("Finding desert");
                // Explore
                return new TimeoutWanderTask();
            }

            BlockPos desertTemplePos = desertTemplePosOrNull(mod);
            if (desertTemplePos != null) {
                setDebugState("Going to found desert temple");
                _finalMovementTask = new GetToBlockTask(desertTemplePos, false);
                return _finalMovementTask;
            }

            if (_searcher.isActive() && _searcher.isFinished(mod)) {
                Debug.logWarning("Temple search failed.");
                _alreadyExplored.addAll(_searcher.getSearchedChunks());
                _searcher = null;
            }
            setDebugState("Searching for temple...");
            return _searcher;
        }
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getOnChunkLoad().removeListener(this::onChunkLoad);
        mod.getBlockTracker().stopTracking(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof SearchForDesertPyramidTask;
    }

    @Override
    protected String toDebugString() {
        return "Searchin' for temples";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _finalMovementTask != null && _finalMovementTask.isFinished(mod);
    }

    private BlockPos desertTemplePosOrNull(AltoClef mod) {
        for (BlockPos pos : mod.getBlockTracker().getKnownLocations(Blocks.STONE_PRESSURE_PLATE)) {
            if (b(mod, pos.down()) == Blocks.CUT_SANDSTONE &&
                b(mod, pos.down().down()) == Blocks.TNT) {
                // 14 blocks up is where the teracotta is.
                return pos.add(0, 14, 0);
            }
        }
        return null;
    }
    private Block b(AltoClef mod, BlockPos pos) {
        return mod.getWorld().getBlockState(pos).getBlock();
    }

    // When we find that desert, start our search there.
    private void onChunkLoad(WorldChunk chunk) {
        if (_searcher != null) return;
        Biome b = _mod.getWorld().getBiome(chunk.getPos().getStartPos());
        if (SearchDesertTask.isDesert(_mod, chunk.getPos())) {
            synchronized (_searcherMutex) {
                Debug.logMessage("FOUND!!!!!");
                if (!_alreadyExplored.contains(chunk.getPos())) {
                    Debug.logMessage("(FOUND confirmation) !!!!!");
                    _searcher = new SearchDesertTask(chunk.getPos());
                }
            }
        }
    }

    private static class SearchDesertTask extends ChunkSearchTask {

        public SearchDesertTask(ChunkPos start) {
            super(start);
        }

        @Override
        protected boolean isChunkPartOfSearchSpace(AltoClef mod, ChunkPos pos) {

            //WorldChunk c = mod.getWorld().getChunk(pos.x, pos.z);

            Biome b = mod.getWorld().getBiome(pos.getStartPos());

            return isDesert(mod, pos);
        }

        @Override
        protected boolean isChunkSearchEqual(ChunkSearchTask other) {
            return other instanceof SearchDesertTask;
        }

        @Override
        protected String toDebugString() {
            return "Searching for temple...";
        }

        private static boolean isDesert(AltoClef mod, ChunkPos pos) {
            Biome b = mod.getWorld().getBiome(pos.getStartPos().add(1, 1, 1));
            return b.getCategory() == Biome.Category.DESERT;
        }
    }

}
