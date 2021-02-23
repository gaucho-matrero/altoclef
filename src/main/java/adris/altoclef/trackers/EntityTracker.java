package adris.altoclef.trackers;

import adris.altoclef.Debug;
import adris.altoclef.util.CachedProjectile;
import adris.altoclef.util.ProjectileUtil;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.ItemTarget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("rawtypes")
public class EntityTracker extends Tracker {

    private final HashMap<Item, List<ItemEntity>> _itemDropLocations = new HashMap<>();

    private final List<Vec3d> _blacklist = new ArrayList<>();

    private final HashMap<Class, List<Entity>> _entityMap = new HashMap<>();

    private final List<Entity> _closeEntities = new ArrayList<>();
    private final List<HostileEntity> _hostiles = new ArrayList<>();

    private final List<CachedProjectile> _projectiles = new ArrayList<>();

    private final HashMap<String, PlayerEntity> _playerMap = new HashMap<>();
    private final HashMap<String, Vec3d> _playerLastCoordinates = new HashMap<>();

    public EntityTracker(TrackerManager manager) {
        super(manager);
    }

    public ItemEntity getClosestItemDrop(Vec3d position, Item ...items) {
        ensureUpdated();
        ItemTarget[] tempTargetList = new ItemTarget[items.length];
        for (int i = 0; i < items.length; ++i) {
            tempTargetList[i] = new ItemTarget(items[i], 9999999);
        }
        return getClosestItemDrop(position, tempTargetList);
        //return getClosestItemDrop(position, ItemTarget.getItemArray(_mod, targets));
    }

    public ItemEntity getClosestItemDrop(Vec3d position, ItemTarget ...targets) {
        ensureUpdated();
        if (targets.length == 0) {
            Debug.logError("You asked for the drop position of zero items... Most likely a typo.");
            return null;
        }
        if (!itemDropped(targets)) {
            Debug.logError("You forgot to check for whether item (example): " + targets[0].getMatches()[0].getTranslationKey() + " was dropped before finding its drop location.");
            return null;
        }

        ItemEntity closestEntity = null;
        float minCost = Float.POSITIVE_INFINITY;
        for (ItemTarget target : targets) {
            for (Item item : target.getMatches()) {
                if (!itemDropped(item)) continue;
                for (ItemEntity entity : _itemDropLocations.get(item)) {
                    if (isBlackListed(entity)) continue;
                    if (!entity.getStack().getItem().equals(item)) continue;

                    float cost = (float) BaritoneHelper.calculateGenericHeuristic(position, entity.getPos());
                    if (cost < minCost) {
                        minCost = cost;
                        closestEntity = entity;
                    }
                }
            }
        }
        return closestEntity;
    }

    public Entity getClosestEntity(Vec3d position, Class ...entityTypes) {
        return this.getClosestEntity(position, (entity) -> false, entityTypes);
    }

    public Entity getClosestEntity(Vec3d position, Predicate<Entity> ignore, Class ...entityTypes) {
        Entity closestEntity = null;
        double minCost = Float.POSITIVE_INFINITY;
        for (Class toFind : entityTypes) {
            if (_entityMap.containsKey(toFind)) {
                for (Entity entity : _entityMap.get(toFind)) {
                    if (ignore.test(entity)) continue;
                    double cost = entity.squaredDistanceTo(position);
                    if (cost < minCost) {
                        minCost = cost;
                        closestEntity = entity;
                    }
                }
            }
        }
        return closestEntity;
    }

    private boolean isBlackListed(Entity entity) {
        if (entity == null) return false;
        return isBlackListed(entity.getPos());
    }
    private boolean isBlackListed(Vec3d pos) {
        for (Vec3d item : _blacklist) {
            double distSq = pos.squaredDistanceTo(item);
            if (distSq < 1) {
                return true;
            }
        }
        return false;
    }

    public void blacklist(Vec3d position) {
        _blacklist.add(position);
    }

    public void clearBlacklist() {
        _blacklist.clear();
    }

    public boolean itemDropped(Item ...items) {
        ensureUpdated();
        for(Item item : items) {
            if (_itemDropLocations.containsKey(item)) {
                // Find a non-blacklisted item
                for (ItemEntity entity : _itemDropLocations.get(item)) {
                    if (!isBlackListed(entity)) return true;
                }
            }
        }
        return false;
    }

    public boolean itemDropped(ItemTarget ...targets) {
        ensureUpdated();
        for (ItemTarget target : targets) {
            if (itemDropped(target.getMatches())) return true;
        }
        return false;
    }

    public boolean entityFound(Class ...types) {
        ensureUpdated();
        for (Class type : types) {
            if (_entityMap.containsKey(type)) return true;
        }
        return false;
    }

    public <T extends Entity> List<T> getTrackedEntities(Class<T> type) {
        ensureUpdated();
        if (!entityFound(type)) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        return (List<T>) _entityMap.get(type);
    }

    public List<Entity> getCloseEntities() {
        ensureUpdated();
        return _closeEntities;
    }

    public List<CachedProjectile> getProjectiles() {
        ensureUpdated();
        return _projectiles;
    }

    public List<HostileEntity> getHostiles() {
        ensureUpdated();
        return _hostiles;
    }

    public boolean isPlayerLoaded(String name) {
        ensureUpdated();
        return _playerMap.containsKey(name);
    }
    public Vec3d getPlayerMostRecentPosition(String name) {
        ensureUpdated();
        if (_playerLastCoordinates.containsKey(name)) {
            return _playerLastCoordinates.get(name);
        }
        return null;
    }
    public PlayerEntity getPlayerEntity(String name) {
        if (isPlayerLoaded(name)) {
            return _playerMap.get(name);
        }
        return null;
    }

    @Override
    protected synchronized void updateState() {
        _itemDropLocations.clear();
        _entityMap.clear();
        _closeEntities.clear();
        _projectiles.clear();
        _hostiles.clear();
        _playerMap.clear();
        if (MinecraftClient.getInstance().world == null) return;

        // Loop through all entities and track 'em
        for(Entity entity : MinecraftClient.getInstance().world.getEntities()) {

            Class type = entity.getClass();
            if (!_entityMap.containsKey(type)) {
                _entityMap.put(type, new ArrayList<>());
            }
            _entityMap.get(type).add(entity);

            if (_mod.getControllerExtras().inRange(entity)) {
                _closeEntities.add(entity);
            }

            if (entity instanceof ItemEntity) {
                ItemEntity ientity = (ItemEntity) entity;
                Item droppedItem = ientity.getStack().getItem();

                if (!_itemDropLocations.containsKey(droppedItem)) {
                    _itemDropLocations.put(droppedItem, new ArrayList<>());
                }
                _itemDropLocations.get(droppedItem).add(ientity);
            } else if (entity instanceof MobEntity) {
                //MobEntity mob = (MobEntity) entity;


                if (entity instanceof HostileEntity) {

                    // Only run away if the hostile can see us.
                    HostileEntity hostile = (HostileEntity) entity;

                    if (hostile.canSee(_mod.getPlayer())) {
                        // Check if the mob is facing us or is close enough
                        boolean closeEnough = hostile.isInRange(_mod.getPlayer(), 4);

                        if (!closeEnough) {
                            if (hostile.getLookControl().isActive()) {
                                Vec3d lookTarget = new Vec3d(
                                        hostile.getLookControl().getLookX(),
                                        hostile.getLookControl().getLookY(),
                                        hostile.getLookControl().getLookZ()
                                );
                                //Debug.logInternal("LOOK: " + lookTarget + " : " + lookTarget.subtract(_mod.getPlayer().getPos()));
                                if (lookTarget.isInRange(_mod.getPlayer().getPos(), 4f)) {
                                    closeEnough = true;
                                }
                            }
                        }

                        //Debug.logInternal("TARGET: " + hostile.is);
                        if (closeEnough && isAngryAtPlayer(hostile)) {
                            _hostiles.add(hostile);
                        }
                    }
                }

                /*
                if (mob instanceof HostileEntity) {
                    HostileEntity hostile = (HostileEntity) mob;
                }
                 */
            } else if (entity instanceof ProjectileEntity) {
                CachedProjectile proj = new CachedProjectile();
                ProjectileEntity projEntity = (ProjectileEntity)entity;

                boolean inGround = false;
                // Get projectile "inGround" variable
                if (entity instanceof PersistentProjectileEntity) {
                    try {
                        Field inGroundField = PersistentProjectileEntity.class.getDeclaredField("inGround");
                        inGroundField.setAccessible(true);
                        inGround = inGroundField.getBoolean(projEntity);
                    } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }

                if (!inGround) {
                    proj.position = projEntity.getPos();
                    proj.velocity = projEntity.getVelocity();
                    proj.gravity = ProjectileUtil.hasGravity(projEntity) ? ProjectileUtil.GRAVITY_ACCEL : 0;
                    proj.projectileType = projEntity.getClass();
                    _projectiles.add(proj);
                }
            } else if (entity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) entity;
                String name = player.getName().getString();
                _playerMap.put(name, player);
                _playerLastCoordinates.put(name, player.getPos());
            }
        }
    }

    @Override
    protected void reset() {
        // Dirty clears everything.
    }

    public static boolean isAngryAtPlayer(Entity hostile) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        // NOTE: These do not work.
        if (hostile instanceof EndermanEntity) {
            EndermanEntity enderman = (EndermanEntity) hostile;
            return enderman.isAngryAt(player) && enderman.isAngry();
        }
        if (hostile instanceof ZombifiedPiglinEntity) {
            ZombifiedPiglinEntity zombie = (ZombifiedPiglinEntity) hostile;
            // Will ALWAYS be false.
            return zombie.hasAngerTime() && zombie.isAngryAt(player);
        }
        if (isTradingPiglin(hostile)) {
            return false;
        }
        return true;
    }

    public static boolean isTradingPiglin(Entity entity) {
        if (entity instanceof PiglinEntity) {
            PiglinEntity pig = (PiglinEntity) entity;
            for (ItemStack stack : pig.getItemsHand()) {
                if (stack.getItem().equals(Items.GOLD_INGOT)) {
                    // We're trading with this one, ignore it.
                    return true;
                }
            }
        }
        return false;
    }
}
