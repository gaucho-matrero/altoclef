package adris.altoclef.util.baritone;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

// The baritone goal block thing but with double precision

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.pathing.goals.GoalYLevel;
import baritone.api.utils.SettingsUtil;
import baritone.api.utils.interfaces.IGoalRenderPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


public class GoalGetToPosition implements Goal, IGoalRenderPos {
    public final double x;
    public final double y;
    public final double z;
    // TODO: 2021-05-22 use vector instead

    public GoalGetToPosition(Vec3d pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public GoalGetToPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static double calculate(double xDiff, int yDiff, double zDiff) {
        double heuristic = 0.0D;
        heuristic += GoalYLevel.calculate(yDiff, 0);
        heuristic += GoalXZ.calculate(xDiff, zDiff);
        return heuristic;
    }

    public boolean isInGoal(int x, int y, int z) {
        double thresh = 1.1f;
        return Math.abs(x - this.x) < thresh && Math.abs(y - this.y) < thresh && Math.abs(z - this.z) < thresh;
    }

    public double heuristic(int x, int y, int z) {
        double xDiff = x - this.x;
        double yDiff = y - this.y;
        double zDiff = z - this.z;
        return calculate(xDiff, (int) yDiff, zDiff);
    }

    public String toString() {
        return String.format("GoalBlock{x=%s,y=%s,z=%s}", SettingsUtil.maybeCensor((int) this.x), SettingsUtil.maybeCensor((int) this.y),
                             SettingsUtil.maybeCensor((int) this.z));
    }

    public BlockPos getGoalPos() {
        return new BlockPos(this.x, this.y, this.z);
    }
}
