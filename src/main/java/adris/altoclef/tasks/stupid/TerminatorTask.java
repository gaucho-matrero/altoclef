package adris.altoclef.tasks.stupid;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestEntityTask;
import adris.altoclef.tasks.RunAwayFromEntitiesTask;
import adris.altoclef.tasks.RunAwayFromPositionTask;
import adris.altoclef.tasks.SearchChunksExploreTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.misc.KillPlayerTask;
import adris.altoclef.tasks.misc.speedrun.BeatMinecraftTask;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.LookUtil;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Roams around the world to terminate Sarah Khaannah
 */
public class TerminatorTask extends Task {

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

    private final Predicate<PlayerEntity> _ignoreTerminate;

    private final ScanChunksInRadius _scanTask;

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
            // See if there's anyone nearby.
            if (mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), entityIgnoreMaybe -> {
                if (!shouldPunk(mod, (PlayerEntity) entityIgnoreMaybe)) {
                    return true;
                }
                if (entityIgnoreMaybe.isInRange(mod.getPlayer(), 15)) {
                    // We're close, count us.
                    return false;
                } else {
                    // We may be far and obstructed, check.
                    boolean seesPlayer = LookUtil.seesPlayer(entityIgnoreMaybe, mod.getPlayer(), 200);
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
                _runAwayTask = new RunAwayFromPlayersTask(() -> mod.getEntityTracker().getTrackedEntities(PlayerEntity.class).stream().filter(toAccept -> shouldPunk(mod, toAccept)).collect(Collectors.toList()), RUN_AWAY_DISTANCE);
                setDebugState("Running away from players.");
                return _runAwayTask;
            }
        } else {
            // We can totally punk
            _runAwayTask = null;
            if (mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), entityIgnoreMaybe -> !shouldPunk(mod, (PlayerEntity) entityIgnoreMaybe), PlayerEntity.class) != null) {
                return new DoToClosestEntityTask(() -> mod.getPlayer().getPos(),
                        entity -> {
                            if (entity instanceof PlayerEntity) {
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

        // Run away if we're too close to someone.
        if (_runAwayTask != null && _runAwayTask.isActive() && !_runAwayTask.isFinished(mod)) {
            return _runAwayTask;
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

        // Get building materials if we don't have them.
        if (PlaceStructureBlockTask.getMaterialCount(mod) < MIN_BUILDING_BLOCKS) {
            setDebugState("Collecting building materials");
            return PlaceStructureBlockTask.getMaterialTask(PREFERRED_BUILDING_BLOCKS);
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
        return BeatMinecraftTask.diamondArmorEquipped(mod) && mod.getInventoryTracker().hasItem(Items.DIAMOND_SWORD);
    }

    private boolean shouldPunk(AltoClef mod, PlayerEntity player) {
        if (player == null) return false;
        return !mod.getButler().isUserAuthorized(player.getName().getString()) && !_ignoreTerminate.test(player);
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
            super(toRunAwayFrom, distanceToRun);
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
