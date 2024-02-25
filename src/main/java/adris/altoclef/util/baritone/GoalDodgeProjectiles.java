package adris.altoclef.util.baritone;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.helpers.BaritoneHelper;
import adris.altoclef.util.helpers.ProjectileHelper;
import baritone.api.pathing.goals.Goal;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class GoalDodgeProjectiles implements Goal {

    private static final double Y_SCALE = 0.3f;

    private final AltoClef _mod;

    private final double _distanceHorizontal;
    private final double _distanceVertical;

    private final List<CachedProjectile> _cachedProjectiles = new ArrayList<>();

    public GoalDodgeProjectiles(AltoClef mod, double distanceHorizontal, double distanceVertical) {
        _mod = mod;
        _distanceHorizontal = distanceHorizontal;
        _distanceVertical = distanceVertical;
    }

    private static boolean isInvalidProjectile(CachedProjectile projectile) {
        //noinspection RedundantIfStatement
        if (projectile == null) return true;
        //if (projectile.getVelocity().lengthSquared() < 0.1) return false;
        return false;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        List<CachedProjectile> projectiles = getProjectiles();
        Vec3d p = new Vec3d(x, y, z);
        //Debug.logMessage("SIZE: " + projectiles.size());
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            if (!projectiles.isEmpty()) {
                for (CachedProjectile projectile : projectiles) {
                    if (isInvalidProjectile(projectile)) continue;
                    try {
                        if (projectile.needsToRecache()) {
                            projectile.setCacheHit(ProjectileHelper.calculateArrowClosestApproach(projectile, p));
                        }
                        Vec3d hit = projectile.getCachedHit();
                        //Debug.logMessage("Hit Delta: " + p.subtract(hit));

                        if (isHitCloseEnough(hit, p)) return false;
                    } catch (Exception e) {
                        Debug.logWarning("Weird exception caught while checking for goal: " + e.getMessage());
                        /// ????? No clue why a nullptrexception happens here.
                    }
                    //double sqFromMob = creepuh.squaredDistanceTo(x, y, z);
                    //if (sqFromMob < _distance*_distance) return false;
                }
            }
        }
        //Debug.logMessage("COMFY: " + p.subtract(MinecraftClient.getInstance().player.getPos()));
        return true;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        Vec3d p = new Vec3d(x, y, z);
        // The HIGHER the cost, the better (total distance from arrows)
        double costFactor = 0;

        List<CachedProjectile> projectiles = getProjectiles();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            if (!projectiles.isEmpty()) {
                for (CachedProjectile projectile : projectiles) {
                    if (isInvalidProjectile(projectile)) continue;

                    if (projectile.needsToRecache()) {
                        projectile.setCacheHit(ProjectileHelper.calculateArrowClosestApproach(projectile, p));
                    }
                    Vec3d hit = projectile.getCachedHit();

                    double arrowPenalty = ProjectileHelper.getFlatDistanceSqr(projectile.position.x, projectile.position.z, projectile.velocity.x, projectile.velocity.z, p.x, p.z);
                    //double arrowCost = hit.squaredDistanceTo(p); //Math.pow(p.x - hit.x, 2) + Math.pow(p.z - hit.z, 2);

                    if (isHitCloseEnough(hit, p)) {
                        costFactor += arrowPenalty;
                    }
                }
            }
        }
        return -1 * costFactor;
    }

    private boolean isHitCloseEnough(Vec3d hit, Vec3d to) {
        Vec3d delta = to.subtract(hit);
        double horizontalSquared = delta.x * delta.x + delta.z * delta.z;
        double vertical = Math.abs(delta.y);
        return horizontalSquared < _distanceHorizontal * _distanceHorizontal && vertical < _distanceVertical;
    }

    private List<CachedProjectile> getProjectiles() {
        return _mod.getEntityTracker().getProjectiles();
    }
}
