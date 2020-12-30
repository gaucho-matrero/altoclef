package adris.altoclef.util.baritone;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.chains.MobDefenseChain;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.mob.HostileEntity;

import java.util.List;

// This goal makes us act like a lil baby bitch
// To use when we're dangerously low on health with little/no armor.
public class GoalRunAwayFromHostiles implements Goal {

    private final AltoClef _mod;
    private final double _distance;

    public GoalRunAwayFromHostiles(AltoClef mod, double distance) {
        _mod = mod;
        _distance = distance;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        List<HostileEntity> hostiles = getHostiles();
        for (HostileEntity hostile : hostiles) {
            if (hostile == null) continue;
            double sqFromMob = hostile.squaredDistanceTo(x, y, z);
            if (sqFromMob < _distance*_distance) return false;
        }
        return true;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        // The lower the cost, the better.
        double distSum = 0;
        List<HostileEntity> hostiles = getHostiles();
        for (HostileEntity hostile : hostiles) {
            if (hostile == null) continue;
            double dist = hostile.squaredDistanceTo(x, y, z);
            distSum += dist;
        }
        return -1 * distSum;
        //return -1 * BaritoneHelper.calculateGenericHeuristic(x, y, z, _badBoi.getPos().x, _badBoi.getPos().y, _badBoi.getPos().z);
    }

    private List<HostileEntity> getHostiles() {
        return _mod.getEntityTracker().getHostiles();
    }
}
