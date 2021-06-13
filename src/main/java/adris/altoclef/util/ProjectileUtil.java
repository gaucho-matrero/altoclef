package adris.altoclef.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;

public class ProjectileUtil {

    public static final double GRAVITY_ACCEL = 0.05000000074505806D;

    public static boolean hasGravity(ProjectileEntity entity) {
        if (entity instanceof ExplosiveProjectileEntity) return false;
        return !entity.hasNoGravity();
    }

    // If we shoot on a 2d plane, what is the 2d point on that trajectory closest to our player pos?
    private static Vec3d getClosestPointOnFlatLine(double shootX, double shootZ, double velX, double velZ, double playerX, double playerZ) {
        double deltaX = playerX - shootX,
                deltaZ = playerZ - shootZ;
        // Did da math I am smurt boi who knows basic calculus
        double t = ((velX * deltaX) + (velZ * deltaZ)) / (velX * velX + velZ * velZ);

        double hitX = shootX + velX * t,
                hitZ = shootZ + velZ * t;

        return new Vec3d(hitX, 0, hitZ);
    }

    public static double getFlatDistanceSqr(double shootX, double shootZ, double velX, double velZ, double playerX, double playerZ) {
        return getClosestPointOnFlatLine(shootX, shootZ, velX, velZ, playerX, playerZ).squaredDistanceTo(playerX, 0, playerZ);
    }

    private static double getArrowHitHeight(double gravity, double horizontalVel, double verticalVel, double initialHeight, double distanceTraveled) {
        double time = distanceTraveled / horizontalVel;
        return initialHeight - (verticalVel * time) - 0.5 * (gravity * time * time);
    }

    /**
     * Calculates where we think an arrow will "hit" us, or at least where it will be at its closest.
     * Does so by figuring out the closest X-Z coordinate of the arrow's trajectory and then using the height
     * of the arrow when it reaches that point as the result's Y value.
     */
    public static Vec3d calculateArrowClosestApproach(Vec3d shootOrigin, Vec3d shootVelocity, double yGravity, Vec3d playerOrigin) {
        Vec3d flatEncounter = getClosestPointOnFlatLine(shootOrigin.x, shootOrigin.z, shootVelocity.x, shootVelocity.z, playerOrigin.x, playerOrigin.z);
        double encounterDistanceTraveled = (flatEncounter.subtract(shootOrigin.x, flatEncounter.y, shootOrigin.z)).length();

        double horizontalVel = Math.sqrt(shootVelocity.x * shootVelocity.x + shootVelocity.z * shootVelocity.z);
        double verticalVel = shootVelocity.y;
        double initialHeight = shootOrigin.y;

        double hitHeight = getArrowHitHeight(yGravity, horizontalVel, verticalVel, initialHeight, encounterDistanceTraveled);

        return new Vec3d(flatEncounter.x, hitHeight, flatEncounter.z);
    }

    public static Vec3d calculateArrowClosestApproach(CachedProjectile projectile, Vec3d pos) {
        return calculateArrowClosestApproach(projectile.position, projectile.velocity, projectile.gravity, pos);
    }

    public static Vec3d calculateArrowClosestApproach(CachedProjectile projectile, ClientPlayerEntity player) {
        return calculateArrowClosestApproach(projectile, player.getPos());
    }

    // Unable to figure out how to extract multiple roots, this is too complicated for engineering major like me.
    @SuppressWarnings("UnnecessaryLocalVariable")
    @Deprecated
    private static double getNearestTimeOfShotProjectile(Vec3d shootOrigin, Vec3d shootVelocity, double yGravity, Vec3d playerOrigin) {
        // Formatted for equations and ease of writing. This is why I'm not minoring in math.
        Vec3d D = playerOrigin.subtract(shootOrigin);
        Vec3d V = shootVelocity;
        double g = yGravity;

        // Cubic terms
        double a = (g * g) / 2.0;
        double b = -(3.0 * g * V.y) / 2.0;
        double c = V.lengthSquared() + (g * V.y);
        double d = -1 * V.dotProduct(D);


        // Now that we have our cubic equation for SQUARED distance, find the "zero" points.
        // This will only happen when we reach either a local minimum or a local maximum.

        // Solution stuff, thanks https://math.vanderbilt.edu/schectex/courses/cubic/
        double p = -b / (3.0 * a);
        double q = (p * p * p) + (((b * c) - (3.0 * a * d)) / (6.0 * a * a));
        double r = c / (3.0 * a);

        double rootInner = (q * q) + Math.pow(r - p * p, 3);

        // Theoretically there could exist imaginary roots that will cancel themselves out, but that ought to be rare.
        // We will get an imaginary root somewhere so ignore it.
        if (rootInner < 0) return -1;

        rootInner = Math.sqrt(rootInner);


        double outerPreCubeLeft = q + rootInner;
        double outerPreCubeRight = q - rootInner;

        // THERE SHOULD BE UP TO 3!!! This will only find one and that can be completely wrong.
        return Math.pow(outerPreCubeLeft, 1.0 / 3.0) + Math.pow(outerPreCubeRight, 1.0 / 3.0) + p;
    }

}
