package adris.altoclef.trackers;

import adris.altoclef.AltoClef;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Sometimes we want very specific things, I don't want to pollute other trackers to add these.
 */
public class MiscBlockTracker {

    private final AltoClef _mod;

    private final Map<Dimension, BlockPos> _lastNetherPortal = new HashMap<>();

    public MiscBlockTracker(AltoClef mod) {
        _mod = mod;
    }

    public void tick() {
        if (AltoClef.inGame()) {
            for (BlockPos check : WorldUtil.scanRegion(_mod, _mod.getPlayer().getBlockPos().add(-1, -1, -1), _mod.getPlayer().getBlockPos().add(1, 1, 1))) {
                Block currentBlock = _mod.getWorld().getBlockState(check).getBlock();
                if (currentBlock == Blocks.NETHER_PORTAL) {
                    for (int y = check.getY() - 1; y >= 0; --y) {
                        if (_mod.getWorld().getBlockState(check).getBlock() == Blocks.NETHER_PORTAL) {
                            check = new BlockPos(check.getX(), y, check.getZ());
                        } else {
                            break;
                        }
                    }
                    _lastNetherPortal.put(_mod.getCurrentDimension(), check);
                    break;
                }
            }
        }
    }

    public void reset() {
        _lastNetherPortal.clear();
    }

    public BlockPos getLastNetherPortal(Dimension dimension) {
        if (_lastNetherPortal.containsKey(dimension)) {
            BlockPos portalPos = _lastNetherPortal.get(dimension);
            // Check whether our nether portal pos is invalid.
            if (_mod.getChunkTracker().isChunkLoaded(portalPos)) {
                if (!_mod.getBlockTracker().blockIsValid(portalPos, Blocks.NETHER_PORTAL)) {
                    _lastNetherPortal.remove(dimension);
                    return null;
                }
            }
            return portalPos;
        }
        return null;
    }
}
