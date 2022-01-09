package adris.altoclef.trackers;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class BehaviorTracker {
    final Map<BlockPos, Integer> vectors = new HashMap<>();

    private boolean equalsPos(final BlockPos pos1, final BlockPos pos2) {
        return pos1.getX() == pos2.getX() && pos1.getY() == pos2.getY() && pos1.getZ() == pos2.getZ();
    }

    private boolean notMoving(final BlockPos pos) {
        return equalsPos(pos, new BlockPos(0, 0, 0));
    }

    private boolean contains(final BlockPos pos) {
        return vectors.keySet().stream().anyMatch(e -> equalsPos(e, pos));
    }

    private boolean scan(final BlockPos pos) {
        if (contains(pos)) {

        }
        return false;
    }

    public boolean addAndScan(final BlockPos pos) {
        if (notMoving(pos)) return false;

        return false;
    }
}
