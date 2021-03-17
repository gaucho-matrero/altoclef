package adris.altoclef.trackers;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.MiningRequirement;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;

/**
 * Sometimes we will try to access something and fail TOO many times.
 *
 * This lets us know that a block is unreachable, and will ignore it from the search intelligently.
 */
public class WorldLocateBlacklist {

    private final HashMap<BlockPos, BlacklistEntry> _entries = new HashMap<>();

    public void blackListBlock(AltoClef mod, BlockPos pos, int numberOfFailuresAllowed) {
        if (!_entries.containsKey(pos)) {
            BlacklistEntry entry = new BlacklistEntry();
            entry.numberOfFailuresAllowed = numberOfFailuresAllowed;
            entry.numberOfFailures = 0;
            entry.bestDistanceSq = Double.POSITIVE_INFINITY;
            entry.bestTool = MiningRequirement.HAND;
            _entries.put(pos, entry);
        }
        BlacklistEntry entry = _entries.get(pos);
        double newDistance = pos.getSquaredDistance(mod.getPlayer().getPos(), false);
        MiningRequirement newTool = mod.getInventoryTracker().getCurrentMiningRequirement();
        if (newTool.ordinal() > entry.bestTool.ordinal() || newDistance < entry.bestDistanceSq) {
            if (newTool.ordinal() > entry.bestTool.ordinal()) entry.bestTool = newTool;
            if (newDistance < entry.bestDistanceSq) entry.bestDistanceSq = newDistance;
            entry.numberOfFailures = 0;
            Debug.logMessage("    TEMP: (failure RESET): " + pos.toShortString());
        }
        entry.numberOfFailures ++;
        entry.numberOfFailuresAllowed = numberOfFailuresAllowed;
        Debug.logMessage("TEMP: " + pos.toShortString() +" FAIL: " + entry.numberOfFailures + " / " + entry.numberOfFailuresAllowed);
    }

    public boolean unreachable(BlockPos pos) {
        if (_entries.containsKey(pos)) {
            BlacklistEntry entry = _entries.get(pos);
            return entry.numberOfFailures > entry.numberOfFailuresAllowed;
        }
        return false;
    }

    public void clear() {
        _entries.clear();
    }

    // Key: BlockPos
    private static class BlacklistEntry {
        public int numberOfFailuresAllowed;
        public int numberOfFailures;
        public double bestDistanceSq;
        public MiningRequirement bestTool;
    }
}
