package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CustomBaritoneGoalTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.chains.MobDefenseChain;
import adris.altoclef.util.baritone.GoalRunAwayFromEntities;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;

import java.util.ArrayList;
import java.util.List;

public class RunAwayFromCreepersTask extends CustomBaritoneGoalTask {

    private final double _distanceToRun;

    public RunAwayFromCreepersTask(double distance) {
        _distanceToRun = distance;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof RunAwayFromCreepersTask) {
            RunAwayFromCreepersTask task = (RunAwayFromCreepersTask) obj;
            //if (task._mob.getPos().squaredDistanceTo(_mob.getPos()) > 0.5) return false;
            if (Math.abs(task._distanceToRun - _distanceToRun) > 1) return false;
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Run " + _distanceToRun + " blocks away from creepers";
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalRunAwayFromCreepers(mod, _distanceToRun);
    }

    private class GoalRunAwayFromCreepers extends GoalRunAwayFromEntities {

        public GoalRunAwayFromCreepers(AltoClef mod, double distance) {
            super(mod, distance);
        }

        @Override
        protected List<Entity> getEntities(AltoClef mod) {
            List<Entity> result = new ArrayList<>(mod.getEntityTracker().getTrackedEntities(CreeperEntity.class));
            return result;
        }

        @Override
        protected double getCostOfEntity(Entity entity, int x, int y, int z) {
            if (entity instanceof CreeperEntity) {
                return MobDefenseChain.getCreeperSafety((CreeperEntity) entity);
            }
            return super.getCostOfEntity(entity, x, y, z);
        }
    }
}
