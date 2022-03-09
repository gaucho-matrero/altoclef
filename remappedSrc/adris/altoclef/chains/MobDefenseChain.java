package adris.altoclef.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.entity.KillEntitiesTask;
import adris.altoclef.tasks.movement.CustomBaritoneGoalTask;
import adris.altoclef.tasks.movement.DodgeProjectilesTask;
import adris.altoclef.tasks.movement.RunAwayFromCreepersTask;
import adris.altoclef.tasks.movement.RunAwayFromHostilesTask;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.control.KillAura;
import adris.altoclef.util.helpers.BaritoneHelper;
import adris.altoclef.util.baritone.CachedProjectile;
import adris.altoclef.util.time.TimerGame;
import adris.altoclef.util.helpers.EntityHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.ProjectileHelper;
import baritone.Baritone;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

@SuppressWarnings("rawtypes")
public class MobDefenseChain extends SingleTaskChain {

    private static final double CREEPER_KEEP_DISTANCE = 10;
    private static final double ARROW_KEEP_DISTANCE_HORIZONTAL = 2;//4;
    private static final double ARROW_KEEP_DISTANCE_VERTICAL = 10;//15;

    private static final double DANGER_KEEP_DISTANCE = 15 * 2;

    private static final double SAFE_KEEP_DISTANCE = 8;
    // Kind of a silly solution
    public static Class[] HOSTILE_ANNOYING_CLASSES = new Class[]{SkeletonEntity.class, ZombieEntity.class, SpiderEntity.class, CaveSpiderEntity.class, WitchEntity.class, PiglinEntity.class, PiglinBruteEntity.class, HoglinEntity.class, ZoglinEntity.class, BlazeEntity.class, WitherSkeletonEntity.class, PillagerEntity.class, DrownedEntity.class};
    private final KillAura _killAura = new KillAura();
    private final HashMap<Entity, TimerGame> _closeAnnoyingEntities = new HashMap<>();
    private Entity _targetEntity;
    private boolean _doingFunkyStuff = false;
    private boolean _wasPuttingOutFire = false;
    private CustomBaritoneGoalTask _runAwayTask;

    private float _cachedLastPriority;

    public MobDefenseChain(TaskRunner runner) {
        super(runner);
    }

    public static double getCreeperSafety(Vec3d pos, CreeperEntity creeper) {
        double distance = creeper.squaredDistanceTo(pos);
        float fuse = creeper.getClientFuseTime(1);

        // Not fusing.
        if (fuse <= 0.001f) return distance;
        return distance * 0.2; // less is WORSE
    }

    @Override
    public float getPriority(AltoClef mod) {
        _cachedLastPriority = getPriorityInner(mod);
        return _cachedLastPriority;
    }
    private float getPriorityInner(AltoClef mod) {
        if (!AltoClef.inGame()) {
            return Float.NEGATIVE_INFINITY;
        }

        if (!mod.getModSettings().isMobDefense()) {
            return Float.NEGATIVE_INFINITY;
        }

        // Apply avoidance if we're vulnerable, avoiding mobs if at all possible.
        // mod.getClientBaritoneSettings().avoidance.value = isVulnurable(mod);
        // Doing you a favor by disabling avoidance


        // Pause if we're not loaded into a world.
        if (!AltoClef.inGame()) return Float.NEGATIVE_INFINITY;

        // Put out fire if we're standing on one like an idiot
        BlockPos fireBlock = isInsideFireAndOnFire(mod);
        if (fireBlock != null) {
            putOutFire(mod, fireBlock);
        } else if (_wasPuttingOutFire) {
            // Stop putting stuff out if we no longer need to put out a fire.
            mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, false);
            _wasPuttingOutFire = false;
        }

        if (prioritizeEating(mod)) {
            return Float.NEGATIVE_INFINITY;
        }

        // Force field
        doForceField(mod);


        // Tell baritone to avoid mobs if we're vulnurable.
        // Costly.
        //mod.getClientBaritoneSettings().avoidance.value = isVulnurable(mod);

        // Run away if a weird mob is close by.
        Optional<Entity> universallyDangerous = getUniversallyDangerousMob(mod);
        if (universallyDangerous.isPresent()) {
            _runAwayTask = new RunAwayFromHostilesTask(DANGER_KEEP_DISTANCE);
            setTask(_runAwayTask);
            return 70;
        }

        _doingFunkyStuff = false;
        // Run away from creepers
        CreeperEntity blowingUp = getClosestFusingCreeper(mod);
        if (blowingUp != null) {
            _doingFunkyStuff = true;
            //Debug.logMessage("RUNNING AWAY!");
            _runAwayTask = new RunAwayFromCreepersTask(CREEPER_KEEP_DISTANCE);
            setTask(_runAwayTask);
            return 50 + blowingUp.getClientFuseTime(1) * 50;
        }

        // Dodge projectiles
        if (!mod.getFoodChain().isTryingToEat() && mod.getModSettings().isDodgeProjectiles() && isProjectileClose(mod)) {
            _doingFunkyStuff = true;
            //Debug.logMessage("DODGING");
            _runAwayTask = null;
            setTask(new DodgeProjectilesTask(ARROW_KEEP_DISTANCE_HORIZONTAL, ARROW_KEEP_DISTANCE_VERTICAL));
            return 65;
        }

        // Dodge all mobs cause we boutta die son
        if (isInDanger(mod)) {
            _doingFunkyStuff = true;
            if (_targetEntity == null) {
                _runAwayTask = new RunAwayFromHostilesTask(DANGER_KEEP_DISTANCE);
                setTask(_runAwayTask);
                return 70;
            }
        }

        if (mod.getModSettings().shouldDealWithAnnoyingHostiles()) {
            // Deal with hostiles because they are annoying.
            List<Entity> hostiles;
            // TODO: I don't think this lock is necessary at all.
            synchronized (BaritoneHelper.MINECRAFT_LOCK) {
                hostiles = mod.getEntityTracker().getHostiles();//mod.getEntityTracker().getTrackedEntities(SkeletonEntity.class;
            }

            ToolItem bestSword = null;
            Item[] SWORDS = new Item[]{Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.STONE_SWORD, Items.WOODEN_SWORD};
            for (Item item : SWORDS) {
                if (mod.getItemStorage().hasItem(item)) {
                    bestSword = (ToolItem) item;
                    break;
                }
            }

            List<Entity> toDealWith = new ArrayList<>();

            // TODO: I don't think this lock is necessary at all.
            synchronized (BaritoneHelper.MINECRAFT_LOCK) {
                for (Entity hostile : hostiles) {
                    int annoyingRange = (hostile instanceof SkeletonEntity || hostile instanceof WitchEntity) ? 18 : 5;
                    boolean isClose = hostile.isInRange(mod.getPlayer(), annoyingRange);

                    if (isClose) {
                        isClose = LookHelper.seesPlayer(hostile, mod.getPlayer(), annoyingRange);
                    }

                    // Give each hostile a timer, if they're close for too long deal with them.
                    if (isClose) {
                        if (!_closeAnnoyingEntities.containsKey(hostile)) {
                            _closeAnnoyingEntities.put(hostile, new TimerGame(mod.getModSettings().getKillHostileWhenCloseForSeconds()));
                            _closeAnnoyingEntities.get(hostile).reset();
                        }
                        if (_closeAnnoyingEntities.get(hostile).elapsed()) {
                            toDealWith.add(hostile);
                        }
                    } else {
                        _closeAnnoyingEntities.remove(hostile);
                    }
                }

                // Clear dead/non existing hostiles
                List<Entity> toRemove = new ArrayList<>();
                for (Entity check : _closeAnnoyingEntities.keySet()) {
                    if (!check.isAlive()) {
                        toRemove.add(check);
                    }
                }
                for (Entity remove : toRemove) _closeAnnoyingEntities.remove(remove);

                int numberOfProblematicEntities = toDealWith.size();

                if (numberOfProblematicEntities > 0) {

                    // Depending on our weapons/armor, we may chose to straight up kill hostiles if we're not dodging their arrows.

                    // wood 0 : 1 skeleton
                    // stone 1 : 1 skeleton
                    // iron 2 : 2 hostiles
                    // diamond 3 : 3 hostiles
                    // netherite 4 : 4 hostiles

                    // Armor: (do the math I'm not boutta calculate this)
                    // leather: ?1 skeleton
                    // iron: ?2 hostiles
                    // diamond: ?3 hostiles

                    // 7 is full set of leather
                    // 15 is full set of iron.
                    // 20 is full set of diamond.
                    // Diamond+netherite have bonus "toughness" parameter (we can simply add them I think, for now.)
                    // full diamond has 8 bonus toughness
                    // full netherite has 12 bonus toughness
                    int armor = mod.getPlayer().getArmor();
                    float damage = bestSword == null ? 0 : (1 + bestSword.getMaterial().getAttackDamage());

                    int canDealWith = (int) Math.ceil((armor * 3.6 / 20.0) + (damage * 0.8));

                    canDealWith += 1;
                    if (canDealWith > numberOfProblematicEntities) {
                        // We can deal with it.
                        _runAwayTask = null;
                        setTask(new KillEntitiesTask(
                                toDealWith::contains,
                                // Oof
                                HOSTILE_ANNOYING_CLASSES));
                        return 65;
                    } else {
                        // We can't deal with it
                        _runAwayTask = new RunAwayFromHostilesTask(30, true);
                        setTask(_runAwayTask);
                        return 80;
                    }
                }
            }
        }


        // By default if we aren't "immediately" in danger but were running away, keep running away until we're good.
        if (_runAwayTask != null && !_runAwayTask.isFinished(mod)) {
            setTask(_runAwayTask);
            return _cachedLastPriority;
        }

        return 0;
    }

    private BlockPos isInsideFireAndOnFire(AltoClef mod) {
        boolean onFire = mod.getPlayer().isOnFire();
        if (!onFire) return null;
        BlockPos p = mod.getPlayer().getBlockPos();
        BlockPos[] toCheck = new BlockPos[]{
                p,
                p.add(1, 0, 0),
                p.add(1, 0, 1),
                p.add(1, 0, -1),
                p.add(0, 0, 1),
                p.add(0, 0, -1),
                p.add(-1, 0, 1),
                p.add(-1, 0, -1)
        };
        for (BlockPos check : toCheck) {
            Block b = mod.getWorld().getBlockState(check).getBlock();
            if (b instanceof AbstractFireBlock) {
                return check;
            }
        }
        return null;
    }

    private void putOutFire(AltoClef mod, BlockPos pos) {
        Baritone b = mod.getClientBaritone();
        IPlayerContext ctx = b.getPlayerContext();
        Optional<Rotation> reachable = RotationUtils.reachableOffset(ctx.player(), pos, new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), ctx.playerController().getBlockReachDistance(), false);
        if (reachable.isPresent()) {
            b.getLookBehavior().updateTarget(reachable.get(), true);
            if (ctx.isLookingAt(pos)) {
                _wasPuttingOutFire = true;
                b.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
            }
        }
    }

    private boolean prioritizeEating(AltoClef mod) {
        return mod.getFoodChain().needsToEatCritical(mod);
    }

    private void doForceField(AltoClef mod) {

        _killAura.tickStart(mod);

        // Hit all hostiles close to us.
        List<Entity> entities = mod.getEntityTracker().getCloseEntities();
        try {
            for (Entity entity : entities) {
                boolean shouldForce = false;
                if (mod.getBehaviour().shouldExcludeFromForcefield(entity)) continue;
                if (entity instanceof Monster) {
                    if (EntityHelper.isGenerallyHostileToPlayer(mod, entity)) {
                        if (LookHelper.seesPlayer(entity, mod.getPlayer(), 10)) {
                            shouldForce = true;
                        }
                    }
                } else if (entity instanceof FireballEntity) {
                    // Ghast ball
                    shouldForce = true;
                } else if (entity instanceof PlayerEntity player && mod.getBehaviour().shouldForceFieldPlayers()) {
                    if (!player.equals(mod.getPlayer())) {
                        String name = player.getName().getString();
                        if (!mod.getButler().isUserAuthorized(name)) {
                            shouldForce = true;
                        }
                    }
                }
                if (shouldForce) {
                    applyForceField(mod, entity);
                }
            }
        } catch (Exception e) {
            Debug.logWarning("Weird exception caught and ignored while doing force field.");
            e.printStackTrace();
        }

        _killAura.tickEnd(mod);
    }

    private void applyForceField(AltoClef mod, Entity entity) {
        if (_targetEntity != null && _targetEntity.equals(entity)) return;
        _killAura.applyAura(mod, entity);
    }

    private CreeperEntity getClosestFusingCreeper(AltoClef mod) {
        double worstSafety = Float.POSITIVE_INFINITY;
        CreeperEntity target = null;
        try {
            List<CreeperEntity> creepers = mod.getEntityTracker().getTrackedEntities(CreeperEntity.class);
            for (CreeperEntity creeper : creepers) {

                if (creeper == null) continue;
                if (creeper.getClientFuseTime(1) < 0.001) continue;

                // We want to pick the closest creeper, but FIRST pick creepers about to blow
                // At max fuse, the cost goes to basically zero.
                double safety = getCreeperSafety(mod.getPlayer().getPos(), creeper);
                if (safety < worstSafety) {
                    target = creeper;
                }
            }
        } catch (ConcurrentModificationException | ArrayIndexOutOfBoundsException | NullPointerException e) {
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
                    // Ignore ghast balls
                    continue;
                }
                if (projectile.projectileType == DragonFireballEntity.class) {
                    // Ignore dragon fireballs
                    continue;
                }

                Vec3d expectedHit = ProjectileHelper.calculateArrowClosestApproach(projectile, mod.getPlayer());

                Vec3d delta = mod.getPlayer().getPos().subtract(expectedHit);

                //Debug.logMessage("EXPECTED HIT OFFSET: " + delta + " ( " + projectile.gravity + ")");

                double horizontalDistanceSq = delta.x * delta.x + delta.z * delta.z;
                double verticalDistance = Math.abs(delta.y);

                if (horizontalDistanceSq < ARROW_KEEP_DISTANCE_HORIZONTAL*ARROW_KEEP_DISTANCE_HORIZONTAL && verticalDistance < ARROW_KEEP_DISTANCE_VERTICAL)
                    return true;
            }
        } catch (ConcurrentModificationException e) {
            Debug.logWarning("Weird exception caught and ignored while checking for nearby projectiles.");
        }
        return false;
    }

    private Optional<Entity> getUniversallyDangerousMob(AltoClef mod) {
        // Wither skeletons are dangerous because of the wither effect. Oof kinda obvious.
        // If we merely force field them, we will run into them and get the wither effect which will kill us.
        if (mod.getEntityTracker().entityFound(WitherSkeletonEntity.class)) {
            Optional<Entity> entity = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), WitherSkeletonEntity.class);
            if (entity.isPresent()) {
                double range = SAFE_KEEP_DISTANCE - 2;
                if (entity.get().squaredDistanceTo(mod.getPlayer()) < range * range && EntityHelper.isAngryAtPlayer(mod, entity.get())) {
                    return entity;
                }
            }
        }
        // Hoglins are dangerous because we can't push them with the force field.
        // If we merely force field them and stand still our health will slowly be chipped away until we die
        if (mod.getEntityTracker().entityFound(HoglinEntity.class, ZoglinEntity.class)) {
            if (mod.getPlayer().getHealth() < 10) {
                Optional<Entity> entity = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), HoglinEntity.class, ZoglinEntity.class);
                if (entity.isPresent()) {
                    double range = SAFE_KEEP_DISTANCE - 1;
                    if (entity.get().squaredDistanceTo(mod.getPlayer()) < range * range && EntityHelper.isAngryAtPlayer(mod, entity.get())) {
                        return entity;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean isInDanger(AltoClef mod) {

        if (isVulnurable(mod)) {
            // If hostile mobs are nearby...
            try {
                ClientPlayerEntity player = mod.getPlayer();
                List<Entity> hostiles = mod.getEntityTracker().getHostiles();
                for (Entity entity : hostiles) {
                    // Ignore skeletons
                    if (entity instanceof SkeletonEntity) continue;
                    if (entity.isInRange(player, SAFE_KEEP_DISTANCE) && !mod.getBehaviour().shouldExcludeFromForcefield(entity) && EntityHelper.isAngryAtPlayer(mod, entity)) {
                        return true;
                    }
                }
            } catch (Exception e) {
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
        return armor < 5 && health < 18;
    }

    public void setTargetEntity(Entity entity) {
        _targetEntity = entity;
    }
    public void resetTargetEntity() {
        _targetEntity = null;
    }

    public void setForceFieldRange(double range) {
        _killAura.setRange(range);
    }

    public void resetForceField() {
        _killAura.setRange(Double.POSITIVE_INFINITY);
    }

    public boolean isDoingAcrobatics() {
        return _doingFunkyStuff;
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