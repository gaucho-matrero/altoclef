
package adris.altoclef.trackers;

import adris.altoclef.Debug;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.csharpisbetter.Action;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.pathing.movement.CalculationContext;
import baritone.process.MineProcess;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 *
 * Blocks tracks the way I want it, When I want it.
 *
 */
public class BlockTracker extends Tracker {

    private final PosCache _cache = new PosCache(100, 64*1.5);

    private final Timer _timer = new Timer(5.0);

    private Block _currentlyTracking = null;

    public BlockTracker(TrackerManager manager) {
        super(manager);
    }

    private Action<Pair<BlockPos, BlockState>> _onBlockPlace = new Action<>();
    private Action<BlockPos> _onBlockRemove = new Action<>();

    @Override
    protected void updateState() {
        if (shouldUpdate()) {
            update();
        }
    }

    public void trackBlock(Block block) {
        _currentlyTracking = block;
    }

    public void stopTracking(Block block) {
        if (block == null || block.is(_currentlyTracking)) {
            _currentlyTracking = null;
        }
    }

    private boolean isTrackingBlock() {
        return _currentlyTracking != null;
    }

    public BlockPos getNearestTracking(Vec3d pos) {
        if (!isTrackingBlock()) {
            Debug.logWarning("BlockTracker: Not tracking anything right now...");
            return null;
        }
        // Make sure we've scanned the first time if we need to.
        updateState();
        return _cache.getNearest(_currentlyTracking, pos);
    }

    private boolean shouldUpdate() {
        return isTrackingBlock() && _timer.elapsed();
    }
    private void update() {
        _timer.reset();
        // Perform a baritone scan.
        rescanWorld();
    }

    private void rescanWorld() {
        Debug.logMessage("Rescanning world for " + _currentlyTracking.getTranslationKey() + "... Hopefully not dummy slow.");
        CalculationContext ctx = new CalculationContext(_mod.getClientBaritone());
        List<BlockPos> knownBlocks = _cache.getKnownLocations(_currentlyTracking);
        BlockOptionalMetaLookup boml = new BlockOptionalMetaLookup(new BlockOptionalMeta(_currentlyTracking));
        List<BlockPos> found = MineProcess.searchWorld(ctx, boml, 64, knownBlocks, Collections.emptyList(), Collections.emptyList());
        _cache.addBlocks(_currentlyTracking, found);
    }

    public void onBlockPlace(BlockPos pos, BlockState state) {
        Debug.logMessage("BLOCK PLACED:::::: " + state.getBlock().getTranslationKey());
        _cache.addBlock(state.getBlock(), pos);
        _onBlockPlace.invoke(new Pair<>(pos, state));
    }
    public void onBlockRemove(BlockPos pos, BlockState oldState) {
        _cache.removeBlock(oldState.getBlock(), pos);
        _onBlockRemove.invoke(pos);
    }

    public Action<Pair<BlockPos, BlockState>> getOnBlockPlace() {
        return _onBlockPlace;
    }
    public Action<BlockPos> getOnBlockRemove() {
        return _onBlockRemove;
    }

    static class PosCache {
        private HashMap<Block, List<BlockPos>> _cachedBlocks = new HashMap<>();

        // Once we have too many blocks, start cutting them off. First only the ones that are far enough.
        private double _cutoffRadius;
        private int _cutoffSize;

        public PosCache(int cutoffSize, double cutoffRadius) {
            _cutoffSize = cutoffSize;
            _cutoffRadius = cutoffRadius;
        }

        public boolean anyFound(Block block) {
            return _cachedBlocks.containsKey(block);
        }

        public List<BlockPos> getKnownLocations(Block block) {
            if (!anyFound(block)) {
                return Collections.emptyList();
            }
            return _cachedBlocks.get(block);
        }

        public void removeBlock(Block block, BlockPos pos) {
            if (anyFound(block)) {
                _cachedBlocks.get(block).remove(pos);
            }
        }

        public void addBlock(Block block, BlockPos pos) {
            if (!anyFound(block)) {
                _cachedBlocks.put(block, new ArrayList<>());
            }
            _cachedBlocks.get(block).add(pos);
        }

        public void addBlocks(Block block, List<BlockPos> positions) {
            if (!anyFound(block)) {
                _cachedBlocks.put(block, new ArrayList<>());
            }
            _cachedBlocks.get(block).addAll(positions);
        }

        // Gets nearest block. For now does linear search. In the future might optimize this a bit
        public BlockPos getNearest(Block block, Vec3d position) {
            if (!anyFound(block)) {
                Debug.logInternal("(failed cataloguecheck for " + block.getTranslationKey() + ")");
                return null;
            }
            BlockPos closest = null;
            double minScore = Double.POSITIVE_INFINITY;

            List<BlockPos> blockList = getKnownLocations(block);

            int toPurge = blockList.size() - _cutoffSize;

            boolean closestPurged = false;

            ListIterator<BlockPos> it = blockList.listIterator();
            while (it.hasNext()) {
                BlockPos pos = it.next();

                // If our current block isn't valid, fix it up. This cleans while we're iterating.
                if (blockIsInvalid(block, pos)) {
                    Debug.logInternal("BlockTracker Removed " + block.getTranslationKey() + " at " + pos);
                    it.remove();
                    continue;
                }

                double score = BaritoneHelper.calculateGenericHeuristic(position, Util.toVec3d(pos));

                boolean currentlyClosest = false;
                boolean purged = false;

                if (score < minScore) {
                    minScore = score;
                    closest = pos;
                    currentlyClosest = true;
                }

                if (toPurge > 0) {
                    double sqDist = position.squaredDistanceTo(Util.toVec3d(pos));
                    if (sqDist > _cutoffRadius*_cutoffRadius) {
                        // cut this one off.
                        it.remove();
                        toPurge --;
                        purged = true;
                    }
                }

                if (currentlyClosest) {
                    closestPurged = purged;
                }
            }

            while (toPurge > 0) {
                blockList.remove(blockList.size() - 1);
                toPurge --;
            }

            // Special case: Our closest was purged. Add us back.
            if (closestPurged) {
                Debug.logInternal("Rare edge case: Closest block was purged cause it was real far away, it will now be added back.");
                blockList.add(closest);
            }

            return closest;
        }

        // Checks whether it would be WRONG to say "at pos the block is block"
        // Returns true if wrong, false if correct OR undetermined/unsure.
        private static boolean blockIsInvalid(Block block, BlockPos pos) {
            // I'm bored
            ClientWorld zaWarudo = MinecraftClient.getInstance().world;
            // No world, therefore we don't assume block is invalid.
            if (zaWarudo == null) {
                Debug.logInternal("(failed worldcheck)");
                return false;
            }
            try {
                if (zaWarudo.isAir(pos) && !block.is(Blocks.AIR)) {
                    // This tracked block is air when it doesn't think it should.
                    Debug.logInternal("(failed aircheck)");
                    return true;
                }
                // It might be OK to remove this. Will have to test.
                //noinspection deprecation
                if (!zaWarudo.isChunkLoaded(pos)) {
                    Debug.logInternal("(failed chunkcheck)");
                    return false;
                }
                BlockState state = zaWarudo.getBlockState(pos);
                return !state.getBlock().is(block);
            } catch (NullPointerException e) {
                // Probably out of chunk. This means we can't judge its state.
                return false;
            }
        }
    }
}
