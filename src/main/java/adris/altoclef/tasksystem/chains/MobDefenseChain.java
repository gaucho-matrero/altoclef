package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.DodgeProjectilesTask;
import adris.altoclef.tasks.misc.RunAwayFromCreepersTask;
import adris.altoclef.tasks.misc.RunAwayFromHostilesTask;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.trackers.EntityTracker;
import adris.altoclef.util.CachedProjectile;
import adris.altoclef.util.ProjectileUtil;
import adris.altoclef.util.slots.PlayerInventorySlot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.util.math.Vec3d;

import java.util.ConcurrentModificationException;
import java.util.List;

public class MobDefenseChain extends SingleTaskChain {

    private static final double CREEPER_KEEP_DISTANCE = 10;
    private static final double ARROW_KEEP_DISTANCE_HORIZONTAL = 2;//4;
    private static final double ARROW_KEEP_DISTANCE_VERTICAL = 10;//15;

    private static final double DANGER_KEEP_DISTANCE = 15;

    private static final double SAFE_KEEP_DISTANCE = 8;

    private Entity _targetEntity;
    private double _forceFieldRange = Double.POSITIVE_INFINITY;

    private boolean _doingFunkyStuff = false;

    public MobDefenseChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    public float getPriority(AltoClef mod) {

        if (!mod.getModSettings().isMobDefense()) {
            return Float.NEGATIVE_INFINITY;
        }

        // Pause if we're not loaded into a world.
        if (!mod.inGame()) return Float.NEGATIVE_INFINITY;

        if (prioritizeEating(mod)) {
            return Float.NEGATIVE_INFINITY;
        }

        // Force field
        doForceField(mod);

        // Tell baritone to avoid mobs if we're vulnurable.
        mod.getClientBaritoneSettings().avoidance.value = isVulnurable(mod);

        // Run away if a weird mob is close by.
        Entity universallyDangerous = getUniversallyDangerousMob(mod);
        if (universallyDangerous != null) {
            setTask(new RunAwayFromHostilesTask(SAFE_KEEP_DISTANCE));
            return 70;
        }

        _doingFunkyStuff = false;
        // Run away from creepers
        CreeperEntity blowingUp = getClosestFusingCreeper(mod);
        if (blowingUp != null) {
            _doingFunkyStuff = true;
            //Debug.logMessage("RUNNING AWAY!");
            setTask(new RunAwayFromCreepersTask(CREEPER_KEEP_DISTANCE));
            return 50 + blowingUp.getClientFuseTime(1) * 50;
        }

        // Dodge projectiles
        if (mod.getModSettings().isDodgeProjectiles() && isProjectileClose(mod)) {
            _doingFunkyStuff = true;
            //Debug.logMessage("DODGING");
            setTask(new DodgeProjectilesTask(ARROW_KEEP_DISTANCE_HORIZONTAL, ARROW_KEEP_DISTANCE_VERTICAL));
            return 65;
        }

        // Dodge all mobs cause we boutta die son
        if (isInDanger(mod)) {
            _doingFunkyStuff = true;
            if (_targetEntity == null) {
                setTask(new RunAwayFromHostilesTask(DANGER_KEEP_DISTANCE));
                return 70;
            }
        }

        return 0;
    }

    private boolean prioritizeEating(AltoClef mod) {
        return mod.getFoodChain().needsToEatCritical(mod);
    }

    private void doForceField(AltoClef mod) {
        // Hit all hostiles close to us.
        List<Entity> entities = mod.getEntityTracker().getCloseEntities();
        try {
            for (Entity entity : entities) {
                if (entity instanceof Monster) {
                    if (EntityTracker.isAngryAtPlayer((Monster)entity)) {
                        applyForceField(mod, entity);
                    }
                } else if (entity instanceof FireballEntity) {
                    // Ghast ball
                    applyForceField(mod, entity);
                }
            }
        } catch (Exception e) {
            Debug.logWarning("Weird exception caught and ignored while doing force field: " + e.getMessage());
        }
    }

    private void applyForceField(AltoClef mod, Entity entity) {
        if (_targetEntity != null && _targetEntity.equals(entity)) return;
        if (Double.isInfinite(_forceFieldRange) || entity.squaredDistanceTo(mod.getPlayer()) < _forceFieldRange*_forceFieldRange) {
            // Equip non-tool
            deequipTool(mod);
            mod.getControllerExtras().attack(entity);
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

                boolean isGhastBall = projectile.projectileType == FireballEntity.class;
                if (isGhastBall) {
                    // ignore if it's too far away.
                    if (!projectile.position.isInRange(mod.getPlayer().getPos(), 40)) {
                        continue;
                    }
                }

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

    private Entity getUniversallyDangerousMob(AltoClef mod) {
        if (mod.getEntityTracker().mobFound(WitherSkeletonEntity.class)) {
            Entity entity = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), WitherSkeletonEntity.class);
            if (entity.squaredDistanceTo(mod.getPlayer()) < 6*6) {
                return entity;
            }
        }
        return null;
    }

    private boolean isInDanger(AltoClef mod) {

        if (isVulnurable(mod)) {
            // If hostile mobs are nearby...
            try {
                ClientPlayerEntity player = mod.getPlayer();
                List<HostileEntity> hostiles = mod.getEntityTracker().getHostiles();
                for(HostileEntity entity : hostiles) {
                    // Ignore skeletons
                    if (entity instanceof SkeletonEntity) continue;
                    if (entity.isInRange(player, DANGER_KEEP_DISTANCE)) {
                        return true;
                    }
                }
            }catch (Exception e) {
                Debug.logWarning("Weird multithread exception. Will fix later.");
            }
        }

        return false;
    }

    private boolean isVulnurable(AltoClef mod) {
        int armor = mod.getPlayer().getArmor();
        float health = mod.getPlayer().getHealth();
        if (armor <= 15 && health < 3) return true;
        if (armor < 10 && health < 10) return true;
        if (armor < 5 && health < 18) return true;
        return false;
    }

    public static double getCreeperSafety(CreeperEntity creeper) {
        double distance = creeper.squaredDistanceTo(MinecraftClient.getInstance().player);
        float fuse = creeper.getClientFuseTime(1);

        // Not fusing. We only get fusing crepers.
        if (fuse <= 0.001f) return 0;
        return distance * (1 - fuse*fuse);
    }

    public void setTargetEntity(Entity entity) {
        _targetEntity = entity;
    }

    public void setForceFieldRange(double range) {
        _forceFieldRange = range;
    }
    public void resetForceField() {
        _forceFieldRange = Double.POSITIVE_INFINITY;
    }

    public boolean isDoingAcrobatics() {
        return _doingFunkyStuff;
    }

    private void deequipTool(AltoClef mod) {
        boolean toolEquipped = false;
        Item equip = mod.getInventoryTracker().getItemStackInSlot(PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND)).getItem();
        if (equip instanceof ToolItem) {
            // Pick non tool item or air
            mod.getInventoryTracker().equipItem(Items.AIR);
        }
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
