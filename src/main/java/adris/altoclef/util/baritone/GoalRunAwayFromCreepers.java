package adris.altoclef.util.baritone;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.chains.MobDefenseChain;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.mob.CreeperEntity;

import java.util.List;

public class GoalRunAwayFromCreepers implements Goal {

    private final AltoClef _mod;
    private final double _distance;

    public GoalRunAwayFromCreepers(AltoClef mod, double distance) {
        _mod = mod;
        _distance = distance;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        List<CreeperEntity> creepers = getCreepers();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            for (CreeperEntity creepuh : creepers) {
                if (creepuh == null) continue;
                double sqFromMob = creepuh.squaredDistanceTo(x, y, z);
                if (sqFromMob < _distance * _distance) return false;
            }
        }
        return true;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        // The lower the cost, the better.
        double costSum = 0;
        List<CreeperEntity> creepers = getCreepers();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            for (CreeperEntity creepuh : creepers) {
                if (creepuh == null) continue;
                double cost = MobDefenseChain.getCreeperSafety(creepuh);
                costSum += cost;
            }
            return -1 * costSum;
        }
        //return -1 * BaritoneHelper.calculateGenericHeuristic(x, y, z, _badBoi.getPos().x, _badBoi.getPos().y, _badBoi.getPos().z);
    }

    private List<CreeperEntity> getCreepers() {
        return _mod.getEntityTracker().getTrackedEntities(CreeperEntity.class);
    }
}
