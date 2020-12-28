package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.DodgeProjectilesTask;
import adris.altoclef.tasks.RunAwayFromCreepersTask;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.CachedProjectile;
import adris.altoclef.util.ProjectileUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ConcurrentModificationException;
import java.util.List;

public class MobDefenseChain extends SingleTaskChain {

    private static final double CREEPER_KEEP_DISTANCE = 10;
    private static final double ARROW_KEEP_DISTANCE_HORIZONTAL = 4;
    private static final double ARROW_KEEP_DISTANCE_VERTICAL = 15;

    public MobDefenseChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    public float getPriority(AltoClef mod) {

        // Force field
        doForceField(mod);

        // Run away from creepers
        CreeperEntity blowingUp = getClosestFusingCreeper(mod);
        if (blowingUp != null) {
            //Debug.logMessage("RUNNING AWAY!");
            setTask(new RunAwayFromCreepersTask(CREEPER_KEEP_DISTANCE));
            return 50 + blowingUp.getClientFuseTime(1) * 50;
        }

        // Dodge projectiles
        if (isProjectileClose(mod)) {
            //Debug.logMessage("DODGING");
            setTask(new DodgeProjectilesTask(ARROW_KEEP_DISTANCE_HORIZONTAL, ARROW_KEEP_DISTANCE_VERTICAL));
            return 65;
        }

        return 0;
    }

    private void doForceField(AltoClef mod) {
        // Hit all hostiles close to us.
        List<Entity> entities = mod.getEntityTracker().getCloseEntities();
        try {
            for (Entity entity : entities) {
                if (entity instanceof HostileEntity) {
                    // TODO: Check if angerable
                    mod.getControllerExtras().attack(entity);
                }
            }
        } catch (Exception e) {
            Debug.logWarning("Weird exception caught and ignored while doing force field: " + e.getMessage());
        }
    }

    private CreeperEntity getClosestFusingCreeper(AltoClef mod) {
        double worstSafety = Float.POSITIVE_INFINITY;
        CreeperEntity target = null;
        try {
            List<CreeperEntity> creepers = mod.getEntityTracker().getTrackedMobs(CreeperEntity.class);
            for (CreeperEntity creeper : creepers) {

                if (creeper == null) continue;
                if (creeper.getClientFuseTime(1) < 0.001) continue;

                // We want to pick the closest creeper, but FIRST pick creepers about to blow
                // At max fuse, the cost goes to basically zero.
                double safety = getCreeperSafety(creeper);
                if (safety < worstSafety) {
                    target = creeper;
                }
            }
        } catch (ConcurrentModificationException | ArrayIndexOutOfBoundsException | NullPointerException e ) {
            // IDK why but these exceptions happen sometimes. It's extremely bizarre and I have no idea why.
            Debug.logWarning("Weird Exception caught and ignored while scanning for creepers: " + e.getMessage());
            return target;
        }
        return target;
    }

    private boolean isProjectileClose(AltoClef mod) {
        List<CachedProjectile> projectiles = mod.getEntityTracker().getProjectiles();

        try {
            for (CachedProjectile projectile : projectiles) {
                Vec3d expectedHit = ProjectileUtil.calculateArrowClosestApproach(projectile, mod.getPlayer());

                Vec3d delta = mod.getPlayer().getPos().subtract(expectedHit);

                //Debug.logMessage("EXPECTED HIT OFFSET: " + delta + " ( " + projectile.gravity + ")");

                double horizontalDistance = Math.sqrt(delta.x*delta.x + delta.z*delta.z);
                double verticalDistance = delta.y;

                if (horizontalDistance < ARROW_KEEP_DISTANCE_HORIZONTAL && verticalDistance < ARROW_KEEP_DISTANCE_VERTICAL) return true;
            }
        } catch (ConcurrentModificationException e) {
            Debug.logWarning("Weird exception caught and ignored while checking for nearby projectiles.");
        }
        return false;
    }

    public static double getCreeperSafety(CreeperEntity creeper) {
        double distance = creeper.squaredDistanceTo(MinecraftClient.getInstance().player);
        float fuse = creeper.getClientFuseTime(1);

        // Not fusing. We only get fusing crepers.
        if (fuse <= 0.001f) return 0;
        return distance * (1 - fuse*fuse);
    }

    @Override
    public boolean isActive() {
        // We're always checking for mobs
        return true;
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        // Task is done, so I guess we move on?
    }

    @Override
    public String getName() {
        return "Mob Defense";
    }
}
