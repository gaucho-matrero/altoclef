package adris.altoclef.util.baritone;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class GoalFollowEntity implements Goal {

    private final Entity _entity;
    private final double _closeEnoughDistance;

    public GoalFollowEntity(Entity entity, double closeEnoughDistance) {
        _entity = entity;
        _closeEnoughDistance = closeEnoughDistance;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        return _entity.getBlockPos().equals(p) || p.isWithinDistance(_entity.getPos(), _closeEnoughDistance);
    }

    @Override
    public double heuristic(int x, int y, int z) {
        //synchronized (BaritoneHelper.MINECRAFT_LOCK) {
        double xDiff = x - _entity.getPos().getX();
        int yDiff = y - _entity.getBlockPos().getY();
        double zDiff = z - _entity.getPos().getZ();
        return GoalBlock.calculate(xDiff, yDiff, zDiff);
        //}
    }
}
