package adris.altoclef.util.baritone;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import net.minecraft.util.math.Vec3d;

public class GoalDirectionXZ implements Goal {
    public final double originx;
    //public final double y;
    public final double originz;
    public final double dirx;
    public final double dirz;

    private final double _sidePenalty;

    public GoalDirectionXZ(Vec3d origin, Vec3d offset, double sidePenalty) {
        this.originx = origin.getX();
        //this.y = origin.getY();
        this.originz = origin.getZ();
        offset = offset.multiply(1, 0, 1);
        offset = offset.normalize();
        this.dirx = offset.x;
        this.dirz = offset.z;
        if (this.dirx == 0 && this.dirz == 0) {
            throw new IllegalArgumentException(offset + "");
        }
        this._sidePenalty = sidePenalty;
    }
    public GoalDirectionXZ(Vec3d origin, Vec3d offset) {
        this(origin, offset, 1000);
    }

    public boolean isInGoal(int x, int y, int z) {
        return false;
    }

    public double heuristic(int x, int y, int z) {
        double  dx = (x - this.originx),
                dz = (z - this.originz);
        double correctDistance = dx * this.dirx + dz * this.dirz;
        double  px = dirx * correctDistance,
                pz = dirz * correctDistance;
        double perpendicularDistance = ((dx - px) * (dx - px)) + ((dz - pz) * (dz - pz));

        /*
        double distanceFromStartInDesiredDirection = (x - this.x) * this.dx + (z - this.z) * this.dz;
        double distanceFromStartInIncorrectDirection = Math.abs((x - this.x) * this.dz) + Math.abs((z - this.z) * this.dx);
        double heuristic = (double)(-distanceFromStartInDesiredDirection) * (Double) BaritoneAPI.getSettings().costHeuristic.value;
        heuristic += (double)(distanceFromStartInIncorrectDirection * _sidePenalty);
         */

        return -correctDistance * BaritoneAPI.getSettings().costHeuristic.value
                + perpendicularDistance * _sidePenalty;
    }

    public String toString() {
        return String.format("GoalDirection{x=%s, z=%s, dx=%s, dz=%s}", maybeCensor(this.originx), maybeCensor(this.originz), maybeCensor(this.dirx), maybeCensor(this.dirz));
    }
    private static String maybeCensor(double value) {
        return Baritone.settings().censorCoordinates.value? "<censored>" : Double.toString(value);
    }
}
