package adris.altoclef.tasks.misc.anarchysurvive;

import adris.altoclef.AltoClef;
import net.minecraft.util.math.Vec3d;

public interface AnarchyUtil {
    static HighwayAxis getClosestAxis(Vec3d pos) {
        double dx = pos.x;
        double dz = pos.z;

        if (Math.abs(dx) < Math.abs(dz)) {
            // Closer to Z axis
            return (dz < 0)? HighwayAxis.NEGATIVE_Z : HighwayAxis.POSITIVE_Z;
        }
        // Closer to X axis
        return (dx < 0)? HighwayAxis.NEGATIVE_X : HighwayAxis.POSITIVE_X;
    }
    static HighwayAxis getClosestAxis(AltoClef mod) {
        return getClosestAxis(mod.getPlayer().getPos());
    }
}
