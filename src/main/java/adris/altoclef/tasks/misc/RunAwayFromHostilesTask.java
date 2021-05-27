package adris.altoclef.tasks.misc;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CustomBaritoneGoalTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.baritone.GoalRunAwayFromEntities;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SkeletonEntity;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class RunAwayFromHostilesTask extends CustomBaritoneGoalTask {
    private final double distanceToRun;
    private final boolean excludeSkeletons;

    public RunAwayFromHostilesTask(double distance, boolean excludeSkeletons) {
        distanceToRun = distance;
        this.excludeSkeletons = !excludeSkeletons;
    }

    public RunAwayFromHostilesTask(double distance) {
        this(distance, false);
    }


    @Override
    protected Goal newGoal(AltoClef mod) {
        // We want to run away NOW
        mod.getClientBaritone().getPathingBehavior().forceCancel();
        return new GoalRunAwayFromHostiles(mod, distanceToRun);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof RunAwayFromHostilesTask) {
            RunAwayFromHostilesTask other = (RunAwayFromHostilesTask) obj;
            return Math.abs(other.distanceToRun - distanceToRun) < 1;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "NIGERUNDAYOO, SUMOOKEYY!";
    }

    public class GoalRunAwayFromHostiles extends GoalRunAwayFromEntities {

        public GoalRunAwayFromHostiles(AltoClef mod, double distance) {
            super(mod, distance, false, 0.8);
        }

        @Override
        protected List<Entity> getEntities(AltoClef mod) {
            List<Entity> result;
            Stream<HostileEntity> stream = mod.getEntityTracker().getHostiles().stream();
            synchronized (BaritoneHelper.MINECRAFT_LOCK) {
                if (excludeSkeletons) {
                    stream = stream.filter(hostile -> !(hostile instanceof SkeletonEntity));
                }
                return stream.collect(Collectors.toList());
            }
        }
    }
}
