package adris.altoclef.tasks.misc.anarchysurvive;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public enum HighwayAxis {
    POSITIVE_X(1, 0), NEGATIVE_X(-1, 0), POSITIVE_Z(0, 1), NEGATIVE_Z(0, -1);

    private final Vec3d _direction;
    HighwayAxis(float dx, float dz) {
        _direction = new Vec3d(dx, 0, dz);
    }

    public Vec3d getDirection() {
        return _direction;
    }

    public double getDistanceFrom(Vec3i pos) {
        return getDistanceFrom(pos.getX(), pos.getZ());
    }
    public double getDistanceFrom(Vec3d pos) {
        return getDistanceFrom(pos.x, pos.z);
    }

    public double getDistanceFrom(double x, double z) {
        switch (this) {
            case NEGATIVE_X:
            case POSITIVE_X:
                return Math.abs(z);
            case NEGATIVE_Z:
            case POSITIVE_Z:
                return Math.abs(x);
        }
        return 0;
    }
}
