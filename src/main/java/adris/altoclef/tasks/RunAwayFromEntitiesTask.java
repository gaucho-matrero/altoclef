package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.util.baritone.GoalRunAwayFromEntities;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.Entity;

import java.util.List;
import java.util.function.Supplier;


public abstract class RunAwayFromEntitiesTask extends CustomBaritoneGoalTask {
    private final Supplier<List<Entity>> runAwaySupplier;
    private final double distanceToRun;
    private final boolean xz;
    // See GoalrunAwayFromEntities penalty value
    private final double penalty;

    public RunAwayFromEntitiesTask(Supplier<List<Entity>> toRunAwayFrom, double distanceToRun, boolean xz, double penalty) {
        runAwaySupplier = toRunAwayFrom;
        this.distanceToRun = distanceToRun;
        this.xz = xz;
        this.penalty = penalty;
    }

    public RunAwayFromEntitiesTask(Supplier<List<Entity>> toRunAwayFrom, double distanceToRun, double penalty) {
        this(toRunAwayFrom, distanceToRun, false, penalty);
    }


    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalRunAwayStuff(mod, distanceToRun, xz);
    }


    private class GoalRunAwayStuff extends GoalRunAwayFromEntities {

        public GoalRunAwayStuff(AltoClef mod, double distance, boolean xz) {
            super(mod, distance, xz, penalty);
        }

        @Override
        protected List<net.minecraft.entity.Entity> getEntities(AltoClef mod) {
            return runAwaySupplier.get();
        }
    }
}
