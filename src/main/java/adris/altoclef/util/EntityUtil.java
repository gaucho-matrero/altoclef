package adris.altoclef.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class EntityUtil {

    public static EntityHitResult raycast(Entity from, Entity to, double reachDistance) {
        Vec3d fromPos = from.getCameraPosVec(1f),
                toPos = to.getCameraPosVec(1f);
        Vec3d direction = (toPos.subtract(fromPos).normalize().multiply(reachDistance));
        Box box = to.getBoundingBox();
        return ProjectileUtil.raycast(from, fromPos, fromPos.add(direction), box, entity -> entity.equals(to), 0);
    }
}
