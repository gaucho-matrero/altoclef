package adris.altoclef.tasks.stupid;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestEntityTask;
import adris.altoclef.tasks.RunAwayFromEntitiesTask;
import adris.altoclef.tasks.SearchChunksExploreTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.misc.KillPlayerTask;
import adris.altoclef.tasks.misc.speedrun.BeatMinecraftTask;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.ui.MessagePriority;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.LookUtil;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Roams around the world to terminate Sarah Khaannah
 */
public class TerminatorTask extends Task {

    private static final int FEAR_SEE_DISTANCE = 30;
    private static final int FEAR_DISTANCE = 20;
    private static final int RUN_AWAY_DISTANCE = 80;

    private static final int MIN_BUILDING_BLOCKS = 10;
    private static final int PREFERRED_BUILDING_BLOCKS = 60;

    private static final String[] DIAMOND_ARMORS = new String[]{"diamond_chestplate", "diamond_leggings", "diamond_helmet", "diamond_boots"};
    private final Task _prepareEquipmentTask = TaskCatalogue.getSquashedItemTask(
            new ItemTarget("diamond_chestplate", 1),
            new ItemTarget("diamond_leggings", 1),
            new ItemTarget("diamond_helmet", 1),
            new ItemTarget("diamond_boots", 1),
            new ItemTarget("diamond_pickaxe", 1),
            new ItemTarget("diamond_shovel", 1),
            new ItemTarget("diamond_sword", 1)
    );
    private final Task _prepareDiamondMiningEquipmentTask = TaskCatalogue.getSquashedItemTask(
            new ItemTarget("iron_pickaxe", 3)
    );
    private final Task _foodTask = new CollectFoodTask(100);
    private final TimerGame _runAwayExtraTime = new TimerGame(10);
    private final Predicate<PlayerEntity> _ignoreTerminate;
    private final ScanChunksInRadius _scanTask;
    private final TimerGame _funnyMessageTimer = new TimerGame(10);
    private Vec3d _closestPlayerLastPos;
    private Vec3d _closestPlayerLastObservePos;
    private Task _runAwayTask;
    private String _currentVisibleTarget;

    public TerminatorTask(BlockPos center, double scanRadius, Predicate<PlayerEntity> ignorePredicate) {
        _ignoreTerminate = ignorePredicate;
        _scanTask = new ScanChunksInRadius(center, scanRadius);
    }

    public TerminatorTask(BlockPos center, double scanRadius) {
        this(center, scanRadius, ignore -> false);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().setForceFieldPlayers(true);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        PlayerEntity closest = (PlayerEntity) mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), toIgnore -> !shouldPunk(mod, (PlayerEntity) toIgnore), PlayerEntity.class);

        if (closest != null) {
            _closestPlayerLastPos = closest.getPos();
            _closestPlayerLastObservePos = mod.getPlayer().getPos();
        }

        if (!isReadyToPunk(mod)) {

            if (_runAwayTask != null && _runAwayTask.isActive() && !_runAwayTask.isFinished(mod)) {
                // If our last "scare" was too long ago or there are no more nearby players...
                boolean noneRemote = (closest == null || !closest.isInRange(mod.getPlayer(), FEAR_DISTANCE));
                if (_runAwayExtraTime.elapsed() && noneRemote) {
                    Debug.logMessage("Stop running away, we're good.");
                    // Stop running away.
                    _runAwayTask = null;
                } else {
                    return _runAwayTask;
                }
            }

            // See if there's anyone nearby.
            if (mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), entityIgnoreMaybe -> {
                if (!shouldPunk(mod, (PlayerEntity) entityIgnoreMaybe)) {
                    return true;
                }
                if (entityIgnoreMaybe.isInRange(mod.getPlayer(), 15)) {
                    // We're close, count us.
                    return false;
                } else {
                    // Too far away.
                    if (!entityIgnoreMaybe.isInRange(mod.getPlayer(), FEAR_DISTANCE)) return true;
                    // We may be far and obstructed, check.
                    boolean seesPlayer = LookUtil.seesPlayer(entityIgnoreMaybe, mod.getPlayer(), FEAR_SEE_DISTANCE);

                    //Debug.logInternal("SEES: " + entityIgnoreMaybe.getName().getString() + " : " + entityIgnoreMaybe + " : " + entityIgnoreMaybe.distanceTo(mod.getPlayer()));
                    return !seesPlayer;
                }
            }, PlayerEntity.class) != null) {
                // RUN!

                _runAwayExtraTime.reset();
                try {
                    _runAwayTask = new RunAwayFromPlayersTask(() -> {
                        Stream<PlayerEntity> stream = mod.getEntityTracker().getTrackedEntities(PlayerEntity.class).stream();
                        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
                            return stream.filter(toAccept -> shouldPunk(mod, toAccept)).collect(Collectors.toList());
                        }
                    }
                            , RUN_AWAY_DISTANCE);
                } catch (ConcurrentModificationException e) {
                    // oof
                    Debug.logWarning("Duct tape over ConcurrentModificationException (see log)");
                    e.printStackTrace();
                }
                setDebugState("Running away from players.");
                return _runAwayTask;
            }
        } else {
            // We can totally punk
            if (_runAwayTask != null) {
                _runAwayTask = null;
                Debug.logMessage("Stopped running away because we can now punk.");
            }
            // Get building materials if we don't have them.
            if (PlaceStructureBlockTask.getMaterialCount(mod) < MIN_BUILDING_BLOCKS) {
                setDebugState("Collecting building materials");
                return PlaceBlockTask.getMaterialTask(PREFERRED_BUILDING_BLOCKS);
            }

            // Get water to MLG if we are pushed off
            if (!mod.getInventoryTracker().hasItem(Items.WATER_BUCKET)) {
                return TaskCatalogue.getItemTask("water_bucket", 1);
            }
            // Get some food so we can last a little longer.
            if ((mod.getPlayer().getHungerManager().getFoodLevel() < (20 - 3 * 2) || mod.getPlayer().getHealth() < 10) && mod.getInventoryTracker().totalFoodScore() <= 0) {
                return _foodTask;
            }

            if (mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), entityIgnoreMaybe -> !shouldPunk(mod, (PlayerEntity) entityIgnoreMaybe), PlayerEntity.class) != null) {
                setDebugState("Punking.");
                return new DoToClosestEntityTask(() -> mod.getPlayer().getPos(),
                        entity -> {
                            if (entity instanceof PlayerEntity) {
                                tryDoFunnyMessageTo(mod, (PlayerEntity) entity);
                                return new KillPlayerTask(entity.getName().getString());
                            }
                            // Should never happen.
                            Debug.logWarning("This should never happen.");
                            return _scanTask;
                        },
                        ignore -> !shouldPunk(mod, (PlayerEntity) ignore),
                        PlayerEntity.class
                );
            }
        }

        // Get stacked first
        // Equip diamond armor asap
        if (BeatMinecraftTask.hasDiamondArmor(mod) && !BeatMinecraftTask.diamondArmorEquipped(mod)) {
            return new EquipArmorTask(DIAMOND_ARMORS);
        }
        // Get diamond armor + gear first
        if (!BeatMinecraftTask.hasDiamondArmor(mod) || !mod.getInventoryTracker().hasItem(Items.DIAMOND_PICKAXE) || !mod.getInventoryTracker().hasItem(Items.DIAMOND_SWORD)) {
            if (mod.getInventoryTracker().getItemCount(Items.IRON_PICKAXE) <= 1 || (_prepareDiamondMiningEquipmentTask.isActive() && !_prepareDiamondMiningEquipmentTask.isFinished(mod))) {
                setDebugState("Getting iron pickaxes to mine diamonds");
                return _prepareDiamondMiningEquipmentTask;
            }
            setDebugState("Getting gear");
            return _prepareEquipmentTask;
        }

        // Get some food while we're at it.
        if (_foodTask.isActive() && !_foodTask.isFinished(mod)) {
            setDebugState("Collecting food");
            return _foodTask;
        }

        // Collect food
        if (mod.getInventoryTracker().totalFoodScore() <= 0) {
            return _foodTask;
        }

        setDebugState("Scanning for players...");
        _currentVisibleTarget = null;
        if (_scanTask.failedSearch()) {
            Debug.logMessage("Re-searching missed places.");
            _scanTask.resetSearch(mod);
        }

        return _scanTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof TerminatorTask;
    }

    @Override
    protected String toDebugString() {
        return "Terminator Task";
    }

    private boolean isReadyToPunk(AltoClef mod) {
        if (mod.getPlayer().getHealth() <= 5) return false; // We need to heal.
        return BeatMinecraftTask.diamondArmorEquipped(mod) && mod.getInventoryTracker().hasItem(Items.DIAMOND_SWORD);
    }

    private boolean shouldPunk(AltoClef mod, PlayerEntity player) {
        if (player == null || player.isDead()) return false;
        if (player.isCreative() || player.isSpectator()) return false;
        return !mod.getButler().isUserAuthorized(player.getName().getString()) && !_ignoreTerminate.test(player);
    }

    private void tryDoFunnyMessageTo(AltoClef mod, PlayerEntity player) {
        if (_funnyMessageTimer.elapsed()) {
            if (LookUtil.seesPlayer(player, mod.getPlayer(), 80)) {
                String name = player.getName().getString();
                if (_currentVisibleTarget == null || !_currentVisibleTarget.equals(name)) {
                    _currentVisibleTarget = name;
                    _funnyMessageTimer.reset();
                    String funnyMessage = getRandomFunnyMessage();
                    mod.getMessageSender().enqueueWhisper(name, funnyMessage, MessagePriority.ASAP);
                }
            }
        }
    }

    private String getRandomFunnyMessage() {
        return "Prepare to get punked, kid";
    }

    private class ScanChunksInRadius extends SearchChunksExploreTask {

        private final BlockPos _center;
        private final double _radius;

        public ScanChunksInRadius(BlockPos center, double radius) {
            _center = center;
            _radius = radius;
        }

        @Override
        protected boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos) {
            double cx = (pos.getStartX() + pos.getEndX()) / 2.0;
            double cz = (pos.getStartZ() + pos.getEndZ()) / 2.0;
            double dx = _center.getX() - cx,
                    dz = _center.getZ() - cz;
            return dx * dx + dz * dz < _radius * _radius;
        }

        @Override
        protected ChunkPos getBestChunkOverride(AltoClef mod, List<ChunkPos> chunks) {
            // Prioritise the chunk we last saw a player in.
            if (_closestPlayerLastPos != null) {
                double lowestScore = Double.POSITIVE_INFINITY;
                ChunkPos bestChunk = null;
                for (ChunkPos toSearch : chunks) {
                    double cx = (toSearch.getStartX() + toSearch.getEndX() + 1) / 2.0, cz = (toSearch.getStartZ() + toSearch.getEndZ() + 1) / 2.0;
                    double px = mod.getPlayer().getX(), pz = mod.getPlayer().getZ();
                    double distanceSq = (cx - px) * (cx - px) + (cz - pz) * (cz - pz);
                    double pdx = _closestPlayerLastPos.getX() - cx, pdz = _closestPlayerLastPos.getZ() - cz;
                    double distanceToLastPlayerPos = pdx * pdx + pdz * pdz;
                    Vec3d direction = _closestPlayerLastPos.subtract(_closestPlayerLastObservePos).multiply(1, 0, 1).normalize();
                    double dirx = direction.x, dirz = direction.z;
                    double correctDistance = pdx * dirx + pdz * dirz;
                    double tempX = dirx * correctDistance,
                            tempZ = dirz * correctDistance;
                    double perpendicularDistance = ((pdx - tempX) * (pdx - tempX)) + ((pdz - tempZ) * (pdz - tempZ));
                    double score = distanceSq + distanceToLastPlayerPos * 0.6 - correctDistance * 2 + perpendicularDistance * 0.5;
                    if (score < lowestScore) {
                        lowestScore = score;
                        bestChunk = toSearch;
                    }
                }
                return bestChunk;
            }
            return super.getBestChunkOverride(mod, chunks);
        }

        @Override
        protected boolean isEqual(Task obj) {
            if (obj instanceof ScanChunksInRadius) {
                ScanChunksInRadius scan = (ScanChunksInRadius) obj;
                return scan._center.equals(_center) && Math.abs(scan._radius - _radius) <= 1;
            }
            return false;
        }

        @Override
        protected String toDebugString() {
            return "Scanning around a radius";
        }
    }

    private class RunAwayFromPlayersTask extends RunAwayFromEntitiesTask {

        public RunAwayFromPlayersTask(Supplier<List<Entity>> toRunAwayFrom, double distanceToRun) {
            super(toRunAwayFrom, distanceToRun, true, 0.1);
            // More lenient progress checker
            _checker = new MovementProgressChecker(2);
        }

        @Override
        protected boolean isEqual(Task obj) {
            return obj instanceof RunAwayFromPlayersTask;
        }

        @Override
        protected String toDebugString() {
            return "Running away from players";
        }
    }
}
