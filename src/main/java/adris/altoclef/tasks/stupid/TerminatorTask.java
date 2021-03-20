package adris.altoclef.tasks.stupid;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestEntityTask;
import adris.altoclef.tasks.RunAwayFromEntitiesTask;
import adris.altoclef.tasks.RunAwayFromPositionTask;
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
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Roams around the world to terminate Sarah Khaannah
 */
public class TerminatorTask extends Task {

    private static final int FEAR_SEE_DISTANCE = 50;
    private static final int RUN_AWAY_DISTANCE = 100;

    private static final int MIN_BUILDING_BLOCKS = 10;
    private static final int PREFERRED_BUILDING_BLOCKS = 60;

    private static final String[] DIAMOND_ARMORS = new String[] {"diamond_chestplate", "diamond_leggings", "diamond_helmet", "diamond_boots"};

    private final Task _prepareEquipmentTask = TaskCatalogue.getSquashedItemTask(
            new ItemTarget("diamond_chestplate", 1),
            new ItemTarget("diamond_leggings", 1),
            new ItemTarget("diamond_helmet", 1),
            new ItemTarget("diamond_boots", 1),
            new ItemTarget("diamond_pickaxe", 1),
            new ItemTarget("diamond_shovel", 1),
            new ItemTarget("diamond_sword", 1)
    );

    private final Task _foodTask = new CollectFoodTask(40);

    private Task _runAwayTask;
    private final Timer _runAwayExtraTime = new Timer(10);

    private final Predicate<PlayerEntity> _ignoreTerminate;

    private final ScanChunksInRadius _scanTask;

    private String _currentVisibleTarget;
    private final Timer _funnyMessageTimer = new Timer(10);

    public TerminatorTask(BlockPos center, double scanRadius, Predicate<PlayerEntity> ignorePredicate) {
        _ignoreTerminate = ignorePredicate;
        _scanTask = new ScanChunksInRadius(center, scanRadius);
    }
    public TerminatorTask(BlockPos center, double scanRadius) {
        this(center, scanRadius, ignore -> false);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getConfigState().push();
        mod.getConfigState().setForceFieldPlayers(true);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (!isReadyToPunk(mod)) {

            if (_runAwayTask != null && _runAwayTask.isActive() && !_runAwayTask.isFinished(mod)) {
                // If our last "scare" was too long ago or there are no more nearby players...
                PlayerEntity closest = (PlayerEntity)mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), toIgnore -> !shouldPunk(mod, (PlayerEntity)toIgnore), PlayerEntity.class);
                boolean noneRemote = (closest == null || !closest.isInRange(mod.getPlayer(), RUN_AWAY_DISTANCE));
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
                    if (!entityIgnoreMaybe.isInRange(mod.getPlayer(), RUN_AWAY_DISTANCE)) return true;
                    // We may be far and obstructed, check.
                    boolean seesPlayer = LookUtil.seesPlayer(entityIgnoreMaybe, mod.getPlayer(), FEAR_SEE_DISTANCE);

                    Debug.logInternal("SEES: " + entityIgnoreMaybe.getName().getString() + " : " + entityIgnoreMaybe + " : " + entityIgnoreMaybe.distanceTo(mod.getPlayer()));
                    return !seesPlayer;
                }
            }, PlayerEntity.class) != null) {
                // RUN!
                /*
                ArrayList<BlockPos> positions = new ArrayList<>();
                for(PlayerEntity player : mod.getEntityTracker().getTrackedEntities(PlayerEntity.class)) {
                    if (shouldPunk(mod, player)) {
                        positions.add(player.getBlockPos());
                    }
                }*/
                _runAwayExtraTime.reset();
                try {
                    _runAwayTask = new RunAwayFromPlayersTask(() -> {
                            List<Entity> entities;
                            synchronized (BaritoneHelper.MINECRAFT_LOCK) {
                                entities = mod.getEntityTracker().getTrackedEntities(PlayerEntity.class).stream().filter(toAccept -> shouldPunk(mod, toAccept)).collect(Collectors.toList());
                            }
                            return entities;
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
            if ((mod.getPlayer().getHungerManager().getFoodLevel() < (20 - 3*2) || mod.getPlayer().getHealth() < 10) && mod.getInventoryTracker().totalFoodScore() <= 0) {
                return _foodTask;
            }

            if (mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), entityIgnoreMaybe -> !shouldPunk(mod, (PlayerEntity) entityIgnoreMaybe), PlayerEntity.class) != null) {
                setDebugState("Punking.");
                return new DoToClosestEntityTask(() -> mod.getPlayer().getPos(),
                        entity -> {
                            if (entity instanceof PlayerEntity) {
                                tryDoFunnyMessageTo(mod, (PlayerEntity)entity);
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
        if (mod.getPlayer().getHealth() < 3) return false; // We need to heal.
        return BeatMinecraftTask.diamondArmorEquipped(mod) && mod.getInventoryTracker().hasItem(Items.DIAMOND_SWORD);
    }

    private boolean shouldPunk(AltoClef mod, PlayerEntity player) {
        if (player == null) return false;
        return !mod.getButler().isUserAuthorized(player.getName().getString()) && !_ignoreTerminate.test(player);
    }

    private void tryDoFunnyMessageTo(AltoClef mod, PlayerEntity player) {
        if (_funnyMessageTimer.elapsed()) {
            if (LookUtil.seesPlayer(player, mod.getPlayer(), 200)) {
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

    private static class ScanChunksInRadius extends SearchChunksExploreTask {

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
            return dx*dx + dz*dz < _radius*_radius;
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
            super(toRunAwayFrom, distanceToRun, true);
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
