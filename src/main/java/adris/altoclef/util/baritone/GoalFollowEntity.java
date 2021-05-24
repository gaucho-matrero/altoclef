package adris.altoclef.util.baritone;


import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;


public class GoalFollowEntity implements Goal {
    
    private final Entity entity;
    private final double closeEnoughDistance;
    
    public GoalFollowEntity(Entity entity, double closeEnoughDistance) {
        this.entity = entity;
        this.closeEnoughDistance = closeEnoughDistance;
    }
    
    @Override
    public boolean isInGoal(int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        return entity.getBlockPos().equals(p) || p.isWithinDistance(entity.getPos(), closeEnoughDistance);
    }
    
    @Override
    public double heuristic(int x, int y, int z) {
        //synchronized (BaritoneHelper.MINECRAFT_LOCK) {
        double xDiff = x - entity.getPos().getX();
        int yDiff = y - entity.getBlockPos().getY();
        double zDiff = z - entity.getPos().getZ();
        return GoalBlock.calculate(xDiff, yDiff, zDiff);
        //}
    }
}
