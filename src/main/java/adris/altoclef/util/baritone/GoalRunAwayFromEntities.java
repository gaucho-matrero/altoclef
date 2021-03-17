package adris.altoclef.util.baritone;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.chains.MobDefenseChain;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;

import java.util.List;

public abstract class GoalRunAwayFromEntities implements Goal {

    private final AltoClef _mod;
    private final double _distance;

    public GoalRunAwayFromEntities(AltoClef mod, double distance) {
        _mod = mod;
        _distance = distance;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        List<Entity> entities = getEntities(_mod);
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            for (Entity entity : entities) {
                if (entity == null || !entity.isAlive()) continue;
                double sqFromMob = entity.squaredDistanceTo(x, y, z);
                if (sqFromMob < _distance * _distance) return false;
            }
        }
        return true;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        // The lower the cost, the better.
        double costSum = 0;
        List<Entity> creepers = getEntities(_mod);
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            for (Entity entity : creepers) {
                if (entity == null || !entity.isAlive()) continue;
                double cost = getCostOfEntity(entity, x, y, z);
                costSum += cost;
            }
            return -1 * costSum;
        }
        //return -1 * BaritoneHelper.calculateGenericHeuristic(x, y, z, _badBoi.getPos().x, _badBoi.getPos().y, _badBoi.getPos().z);
    }

    protected abstract List<Entity> getEntities(AltoClef mod);

    // Virtual
    protected double getCostOfEntity(Entity entity, int x, int y, int z) {
        return entity.squaredDistanceTo(x, y, z);
    }
}
