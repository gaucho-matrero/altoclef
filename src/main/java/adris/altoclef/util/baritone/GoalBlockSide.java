package adris.altoclef.util.baritone;


import baritone.api.pathing.goals.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;


public class GoalBlockSide implements Goal {

    private final BlockPos block;
    private final Direction direction;
    private final double buffer;

    public GoalBlockSide(BlockPos block, Direction direction, double bufferDistance) {
        this.block = block;
        this.direction = direction;
        this.buffer = bufferDistance;
    }

    public GoalBlockSide(BlockPos block, Direction direction) {
        this(block, direction, 1);
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        // We are on the right side
        return getDistanceInRightDirection(x, y, z) > 0;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        // How far are we away
        return Math.min(getDistanceInRightDirection(x, y, z), 0);
    }

    private double getDistanceInRightDirection(int x, int y, int z) {
        Vec3d delta = new Vec3d(x, y, z).subtract(block.getX(), block.getY(), block.getZ());
        Vec3i dir = direction.getVector();
        double dot = new Vec3d(dir.getX(), dir.getY(), dir.getZ()).dotProduct(delta);
        // WE ASSUME THAT dir IS NORMALIZED
        double distCorrect = dot;
        return distCorrect - this.buffer;
    }
}
