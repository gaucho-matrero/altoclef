package adris.altoclef.util.baritone;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.util.math.ChunkPos;

public class GoalChunk implements Goal {

    private final ChunkPos _pos;

    public GoalChunk(ChunkPos pos) {
        _pos = pos;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return _pos.getStartX() <= x && x <= _pos.getEndX() &&
                _pos.getStartZ() <= z && z <= _pos.getEndZ();
    }

    @Override
    public double heuristic(int x, int y, int z) {
        double cx = (_pos.getStartX() + _pos.getEndX()) / 2.0, cz = (_pos.getStartZ() + _pos.getEndZ()) / 2.0;
        return GoalXZ.calculate(cx - x, cz - z);
    }
}
