package adris.altoclef.util.baritone;


import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import net.minecraft.util.math.Vec3d;


public class GoalDirectionXZ implements Goal {
    // TODO: 2021-05-22 why public...
    public final double originX;
    //public final double y;
    public final double originZ;
    public final double dirX;
    public final double dirZ;
    
    private final double sidePenalty;
    
    public GoalDirectionXZ(Vec3d origin, Vec3d offset, double sidePenalty) {
        this.originX = origin.getX();
        //this.y = origin.getY();
        this.originZ = origin.getZ();
        offset = offset.multiply(1, 0, 1);
        offset = offset.normalize();
        this.dirX = offset.x;
        this.dirZ = offset.z;
        if (this.dirX == 0 && this.dirZ == 0) {
            throw new IllegalArgumentException(String.valueOf(offset));
        }
        this.sidePenalty = sidePenalty;
    }
    
    public GoalDirectionXZ(Vec3d origin, Vec3d offset) {
        this(origin, offset, 1000);
    }
    
    private static String maybeCensor(double value) {
        return Baritone.settings().censorCoordinates.value ? "<censored>" : Double.toString(value);
    }
    
    public boolean isInGoal(int x, int y, int z) {
        return false;
    }
    
    public double heuristic(int x, int y, int z) {
        double dx = (x - this.originX), dz = (z - this.originZ);
        double correctDistance = dx * this.dirX + dz * this.dirZ;
        double px = dirX * correctDistance, pz = dirZ * correctDistance;
        double perpendicularDistance = ((dx - px) * (dx - px)) + ((dz - pz) * (dz - pz));

        /*
        double distanceFromStartInDesiredDirection = (x - this.x) * this.dx + (z - this.z) * this.dz;
        double distanceFromStartInIncorrectDirection = Math.abs((x - this.x) * this.dz) + Math.abs((z - this.z) * this.dx);
        double heuristic = (double)(-distanceFromStartInDesiredDirection) * (Double) BaritoneAPI.getSettings().costHeuristic.value;
        heuristic += (double)(distanceFromStartInIncorrectDirection * _sidePenalty);
         */
        
        return -correctDistance * BaritoneAPI.getSettings().costHeuristic.value + perpendicularDistance * sidePenalty;
    }
    
    public String toString() {
        return String.format("GoalDirection{x=%s, z=%s, dx=%s, dz=%s}", maybeCensor(this.originX), maybeCensor(this.originZ),
                             maybeCensor(this.dirX), maybeCensor(this.dirZ));
    }
}
