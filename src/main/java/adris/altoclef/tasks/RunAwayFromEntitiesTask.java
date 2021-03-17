package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.chains.MobDefenseChain;
import adris.altoclef.util.baritone.GoalRunAwayFromEntities;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class RunAwayFromEntitiesTask extends CustomBaritoneGoalTask {

    private final Supplier<List<Entity>> _runAwaySupplier;

    private final double _distanceToRun;

    public RunAwayFromEntitiesTask(Supplier<List<Entity>> toRunAwayFrom, double distanceToRun) {
        _runAwaySupplier = toRunAwayFrom;
        _distanceToRun = distanceToRun;
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalRunAwayStuff(mod, _distanceToRun);
    }


    private class GoalRunAwayStuff extends GoalRunAwayFromEntities {

        public GoalRunAwayStuff(AltoClef mod, double distance) {
            super(mod, distance);
        }

        @Override
        protected List<net.minecraft.entity.Entity> getEntities(AltoClef mod) {
            return _runAwaySupplier.get();
        }
    }
}
