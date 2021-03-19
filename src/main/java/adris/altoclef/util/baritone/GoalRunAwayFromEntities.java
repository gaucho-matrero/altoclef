package adris.altoclef.util.baritone;

import adris.altoclef.AltoClef;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.pathing.goals.GoalYLevel;
import net.minecraft.entity.Entity;

import java.util.List;

public abstract class GoalRunAwayFromEntities implements Goal {

    private final AltoClef _mod;
    private final double _distance;
    private final boolean _xzOnly;

    public GoalRunAwayFromEntities(AltoClef mod, double distance, boolean xzOnly) {
        _mod = mod;
        _distance = distance;
        _xzOnly = xzOnly;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        List<Entity> entities = getEntities(_mod);
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            for (Entity entity : entities) {
                if (entity == null || !entity.isAlive()) continue;
                double sqDistance;
                if (_xzOnly) {
                    sqDistance = entity.getPos().subtract(x, y, z).multiply(1, 0, 1).lengthSquared();
                } else {
                    sqDistance = entity.squaredDistanceTo(x, y, z);
                }
                if (sqDistance < _distance * _distance) return false;
            }
        }
        return true;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        // The lower the cost, the better.
        double costSum = 0;
        List<Entity> entities = getEntities(_mod);
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            int max = 5; // If we have 100 players, this will never calculate.
            int counter = 0;
            for (Entity entity : entities) {
                counter++;
                if (entity == null || !entity.isAlive()) continue;
                double cost = getCostOfEntity(entity, x, y, z);
                costSum += cost;
                if (counter >= max) break;
            }
            costSum /= counter;
            return -1 * costSum * 0.1;
        }
        //return -1 * BaritoneHelper.calculateGenericHeuristic(x, y, z, _badBoi.getPos().x, _badBoi.getPos().y, _badBoi.getPos().z);
    }

    protected abstract List<Entity> getEntities(AltoClef mod);

    // Virtual
    protected double getCostOfEntity(Entity entity, int x, int y, int z) {
        double heuristic = 0;
        heuristic += GoalYLevel.calculate(entity.getBlockPos().getY(), y);
        heuristic += GoalXZ.calculate(entity.getBlockPos().getX() - x, entity.getBlockPos().getZ() - z);
        return heuristic; //entity.squaredDistanceTo(x, y, z);
    }
}
