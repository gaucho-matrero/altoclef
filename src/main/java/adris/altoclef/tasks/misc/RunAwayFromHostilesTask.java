package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CustomBaritoneGoalTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalRunAwayFromEntities;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SkeletonEntity;

import java.util.List;
import java.util.stream.Collectors;

public class RunAwayFromHostilesTask extends CustomBaritoneGoalTask {

    private final double _distanceToRun;

    public RunAwayFromHostilesTask(double distance) {
        _distanceToRun = distance;
    }


    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalRunAwayFromHostiles(mod, _distanceToRun);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof RunAwayFromHostilesTask) {
            RunAwayFromHostilesTask other = (RunAwayFromHostilesTask) obj;
            return Math.abs(other._distanceToRun - _distanceToRun) < 1;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "NIGERUNDAYOO, SUMOOKEYY!";
    }

    private class GoalRunAwayFromHostiles extends GoalRunAwayFromEntities {

        public GoalRunAwayFromHostiles(AltoClef mod, double distance) {
            super(mod, distance);
        }

        @Override
        protected List<Entity> getEntities(AltoClef mod) {
            List<Entity> result = mod.getEntityTracker().getHostiles().stream()
                    .filter(hostile -> !(hostile instanceof SkeletonEntity))
            .collect(Collectors.toList());
            return result;
        }
    }
}
