package adris.altoclef.util.baritone;

import baritone.api.pathing.goals.Goal;

import java.util.Arrays;

public class GoalAnd implements Goal {
    private final Goal[] goals;

    public GoalAnd(Goal... goals) {
        this.goals = goals;
    }

    public boolean isInGoal(int x, int y, int z) {
        Goal[] var4 = this.goals;
        int var5 = var4.length;

        for (Goal goal : var4) {
            if (!goal.isInGoal(x, y, z)) {
                return false;
            }
        }

        return true;
    }

    public double heuristic(int x, int y, int z) {
        double sum = 0;
        if (this.goals != null) {
            for (Goal goal : this.goals) {
                sum += goal.heuristic(x, y, z);
            }
        }
        return sum;
        /*double min = 1.7976931348623157E308D;
        Goal[] var6 = this.goals;
        int var7 = var6.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            Goal g = var6[var8];
            min = Math.min(min, g.heuristic(x, y, z));
        }

        return min;
         */
    }

    public String toString() {
        return "GoalAnd" + Arrays.toString(this.goals);
    }

    public Goal[] goals() {
        return this.goals;
    }
}
