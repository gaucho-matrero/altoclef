package adris.altoclef.tasks.misc;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DefaultGoToDimensionTask;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.InteractItemWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.resources.CollectBedTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.*;
import adris.altoclef.util.csharpisbetter.ActionListener;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.utils.RayTraceUtils;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.ArrayUtils;


public class PlaceBedAndSetSpawnTask extends Task {

    private static final Block[] BEDS = CollectBedTask.BEDS;
    private static final Vec3i BED_CLEAR_SIZE = new Vec3i(3, 2, 1);
    private static final Vec3i[] BED_BOTTOM_PLATFORM = {
            new Vec3i(0, -1, 0), new Vec3i(1, -1, 0), new Vec3i(2, -1, 0),
            };
    // Kinda silly but who knows if we ever want to change it.
    private static final Vec3i BED_PLACE_STAND_POS = new Vec3i(0, 0, 0);
    private static final Vec3i BED_PLACE_POS = new Vec3i(1, 0, 0);
    private static final Direction BED_PLACE_DIRECTION = Direction.UP;
    private final Timer regionScanTimer = new Timer(9);
    private final Timer bedInteractTimeout = new Timer(5);
    private final Timer inBedTimer = new Timer(1);
    private BlockPos currentBedRegion;
    private BlockPos currentStructure, _currentBreak;
    private boolean spawnSet;
    private boolean _sleepAttemptMade;
    private final ActionListener<String> onCheckGameMessage = new ActionListener<String>() {
        @Override
        public void invoke(String value) {
            if (value.contains("Respawn point set")) {
                spawnSet = true;
                inBedTimer.reset();
            }
        }
    };
    private final ActionListener<String> onOverlayMessage = new ActionListener<String>() {
        @Override
        public void invoke(String value) {
            final String[] NEUTRAL_MESSAGES = { "You can sleep only at night", "You can only sleep at night", "You may not rest now; there are monsters nearby"};
            for (String checkMessage : NEUTRAL_MESSAGES) {
                if (value.contains(checkMessage)) {
                    if (!_sleepAttemptMade) {
                        bedInteractTimeout.reset();
                    }
                    _sleepAttemptMade = true;
                }
            }
        }
    };
    private boolean _wasSleeping;
    private BlockPos _bedForSpawnPoint;

    public void resetSleep() {
        spawnSet = false;
        _sleepAttemptMade = false;
        _wasSleeping = false;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if (mod.getCurrentDimension() != Dimension.OVERWORLD) {
            Debug.logWarning("Can't place spawnpoint/sleep in a bed unless we're in the overworld!");
            return true;
        }
        return spawnSet && !mod.getPlayer().isSleeping() && inBedTimer.elapsed();
    }

    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(4, true);
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker(2);

    @Override
    protected void onStart(AltoClef mod) {
        currentBedRegion = null;

        // Don't break our bed thing.
        mod.getConfigState().push();
        mod.getConfigState().avoidBlockPlacing(pos -> {
            if (currentBedRegion != null) {
                BlockPos start = currentBedRegion, end = currentBedRegion.add(BED_CLEAR_SIZE);
                return start.getX() <= pos.getX() && pos.getX() < end.getX() && start.getZ() <= pos.getZ() && pos.getZ() < end.getZ() &&
                       start.getY() <= pos.getY() && pos.getY() < end.getY();
            }
            return false;
        });
        mod.getConfigState().avoidBlockBreaking(pos -> {
            if (currentBedRegion != null) {
                for (Vec3i baseOffs : BED_BOTTOM_PLATFORM) {
                    BlockPos base = currentBedRegion.add(baseOffs);
                    if (base.equals(pos)) return true;
                }
            }
            // Don't ever break beds. If one exists, we will sleep in it.
            return mod.getWorld().getBlockState(pos).getBlock() instanceof BedBlock;
        });

        mod.getBlockTracker().trackBlock(BEDS);

        spawnSet = false;
        _sleepAttemptMade = false;
        _wasSleeping = false;

        mod.onGameMessage.addListener(onCheckGameMessage);
        mod.onGameOverlayMessage.addListener(onOverlayMessage);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Summary:
        // If we find a bed nearby, sleep in it.
        // Otherwise, place bed:
        //      Collect bed if we don't have one.
        //      Find a 3x2x1 region and clear it
        //      Stand on the edge of the long (3) side
        //      Place on the middle block, reliably placing the bed.

        // We cannot do this anywhere but the overworld.
        if (mod.getCurrentDimension() != Dimension.OVERWORLD) {
            setDebugState("Going to the overworld first.");
            return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }

        if (mod.getPlayer().isSleeping()) {
            _progressChecker.reset();
            setDebugState("Sleeping...");
            // Click "leave bed" immediately.

            Screen screen = MinecraftClient.getInstance().currentScreen;
            if (inBedTimer.elapsed() && screen instanceof SleepingChatScreen) {
                _wasSleeping = true;
                //Debug.logMessage("Closing sleeping thing");
                spawnSet = true;
                screen.onClose();
            }
            return null;
        }

        if (_sleepAttemptMade) {
            if (bedInteractTimeout.elapsed()) {
                Debug.logMessage(
                        "Failed to get \"Respawn point set\" message or sleeping, assuming that this bed already contains our spawn.");
                spawnSet = true;
                return null;
            }
        }

        if (mod.getBlockTracker().anyFound(BEDS)) {
            // Sleep in the nearest bed
            setDebugState("Going to bed to sleep...");
            return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(), toSleepIn -> {
                boolean closeEnough = toSleepIn.isWithinDistance(mod.getPlayer().getPos(), 3);
                if (closeEnough) {
                    // why 0.2? I'm tired.
                    Vec3d centerBed = new Vec3d(toSleepIn.getX() + 0.5, toSleepIn.getY() + 0.2, toSleepIn.getZ() + 0.5);
                    BlockHitResult hit = LookUtil.raycast(mod.getPlayer(), centerBed, 6);
                    // TODO: Kinda ugly, but I'm tired and fixing for the 2nd attempt speedrun so I will fix this block later
                    closeEnough = false;
                    if (hit.getType() != HitResult.Type.MISS) {
                        // At this point, if we miss, we probably are close enough.
                        BlockPos p = hit.getBlockPos();
                        if (Util.arrayContains(Util.itemsToBlocks(ItemUtil.BED), mod.getWorld().getBlockState(p).getBlock())) {
                            // We have a bed!
                            closeEnough = true;
                        }
                    }
                }
                BlockPos targetMove = toSleepIn;
                if (!closeEnough) {
                    try {
                        Direction face = mod.getWorld().getBlockState(toSleepIn).get(BedBlock.FACING);
                        Direction side = face.rotateYClockwise();
                        targetMove = toSleepIn.offset(side);
                    } catch (IllegalArgumentException e) {
                        // If bed is not loaded, this will happen. In that case just get to the bed first.
                    }
                } else {
                    inBedTimer.reset();
                }
                // Keep track of where our spawn point is
                _bedForSpawnPoint = WorldUtil.getBedHead(mod, toSleepIn);
                //Debug.logMessage("Bed spawn point: " + _bedForSpawnPoint);
                _progressChecker.reset();
                return new GetToBlockTask(targetMove, closeEnough);
            }, pos -> mod.getBlockTracker().getNearestTracking(pos, BEDS), BEDS);
        }

        // Get a bed if we don't have one.
        if (!mod.getInventoryTracker().hasItem(ItemUtil.BED)) {
            setDebugState("Getting a bed first");
            return TaskCatalogue.getItemTask("bed", 1);
        }

        if (currentBedRegion == null) {
            if (regionScanTimer.elapsed()) {
                Debug.logMessage("Rescanning for nearby bed place position...");
                regionScanTimer.reset();
                currentBedRegion = this.locateBedRegion(mod, mod.getPlayer().getBlockPos());
            }
        }
        if (currentBedRegion == null) {
            setDebugState("Searching for spot to place bed, wandering...");
            return new TimeoutWanderTask();
        }

        // Clear and make bed foundation

        for (Vec3i baseOffs : BED_BOTTOM_PLATFORM) {
            BlockPos toPlace = currentBedRegion.add(baseOffs);
            if (!WorldUtil.isSolid(mod, toPlace)) {
                currentStructure = toPlace;
                break;
            }
        }

        outer:
        for (int dx = 0; dx < BED_CLEAR_SIZE.getX(); ++dx) {
            for (int dz = 0; dz < BED_CLEAR_SIZE.getZ(); ++dz) {
                for (int dy = 0; dy < BED_CLEAR_SIZE.getY(); ++dy) {
                    BlockPos toClear = currentBedRegion.add(dx, dy, dz);
                    if (WorldUtil.isSolid(mod, toClear)) {
                        _currentBreak = toClear;
                        break outer;
                    }
                }
            }
        }

        if (currentStructure != null) {
            if (WorldUtil.isSolid(mod, currentStructure)) {
                currentStructure = null;
            } else {
                setDebugState("Placing structure for bed");
                return new PlaceStructureBlockTask(currentStructure);
            }
        }
        if (_currentBreak != null) {
            if (!WorldUtil.isSolid(mod, _currentBreak)) {
                _currentBreak = null;
            } else {
                setDebugState("Clearing region for bed");
                return new DestroyBlockTask(_currentBreak);
            }
        }

        BlockPos toStand = currentBedRegion.add(BED_PLACE_STAND_POS);
        // Our bed region is READY TO BE PLACED
        if (!mod.getPlayer().getBlockPos().equals(toStand)) {
            return new GetToBlockTask(toStand, false);
        }

        BlockPos toPlace = currentBedRegion.add(BED_PLACE_POS);
        if (mod.getWorld().getBlockState(toPlace.offset(BED_PLACE_DIRECTION)).getBlock() instanceof BedBlock) {
            setDebugState("Waiting to rescan + find bed that we just placed. Should be almost instant.");
            _progressChecker.reset();
            return null;
        }
        setDebugState("Placing bed...");

        setDebugState("Filling in Portal");
        if (!_progressChecker.check(mod)) {
            _progressChecker.reset();
            return _wanderTask;
        }
        return new InteractItemWithBlockTask(new ItemTarget("bed", 1), BED_PLACE_DIRECTION,
                                             toPlace.offset(BED_PLACE_DIRECTION.getOpposite()), false);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getConfigState().pop();
        mod.getBlockTracker().stopTracking(BEDS);
        mod.onGameMessage.removeListener(onCheckGameMessage);
        mod.onGameOverlayMessage.removeListener(onOverlayMessage);
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof PlaceBedAndSetSpawnTask;
    }

    @Override
    protected String toDebugString() {
        return "Placing a bed nearby + resetting spawn point";
    }

    public BlockPos getBedSleptPos() {
        return _bedForSpawnPoint;
    }

    public boolean isSpawnSet() {
        return spawnSet;
    }

    private BlockPos locateBedRegion(AltoClef mod, BlockPos origin) {
        final int SCAN_RANGE = 10;

        BlockPos closestGood = null;
        double closestDist = Double.POSITIVE_INFINITY;
        for (int dx = origin.getX() - SCAN_RANGE; dx < origin.getX() + SCAN_RANGE; ++dx) {
            for (int dz = origin.getZ() - SCAN_RANGE; dz < origin.getZ() + SCAN_RANGE; ++dz) {
                outer:
                for (int dy = origin.getY() - SCAN_RANGE; dy < origin.getY() + SCAN_RANGE; ++dy) {
                    // Test range
                    BlockPos attemptStandPos = new BlockPos(dx, dy, dz);
                    double distance = attemptStandPos.getSquaredDistance(mod.getPlayer().getPos(), false);
                    if (distance > closestDist) continue;
                    // Everything from here on out is checking for a BETTER pos.
                    for (int checkX = 0; checkX < BED_CLEAR_SIZE.getX(); ++checkX) {
                        for (int checkY = 0; checkY < BED_CLEAR_SIZE.getY(); ++checkY) {
                            for (int checkZ = 0; checkZ < BED_CLEAR_SIZE.getZ(); ++checkZ) {
                                BlockPos checkAsGoodArea = attemptStandPos.add(checkX, checkY, checkZ);
                                if (!isGoodToPlaceInsideOrClear(mod, checkAsGoodArea)) {
                                    continue outer;
                                }
                            }
                        }
                    }
                    closestGood = attemptStandPos;
                    closestDist = distance;
                }
            }
        }
        return closestGood;
    }

    private boolean isGoodToPlaceInsideOrClear(AltoClef mod, BlockPos pos) {
        final Vec3i[] CHECK = {
                new Vec3i(0, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0), new Vec3i(0, 1, 0), new Vec3i(0, -1, 0), new Vec3i(0, 0, 1),
                new Vec3i(0, 0, -1)
        };
        for (Vec3i offs : CHECK) {
            if (!isGoodAsBorder(mod, pos.add(offs))) return false;
        }
        return true;
    }

    private boolean isGoodAsBorder(AltoClef mod, BlockPos pos) {
        if (WorldUtil.isSolid(mod, pos)) {
            return WorldUtil.canBreak(mod, pos);
        } else {
            return (WorldUtil.isAir(mod, pos));
        }
    }
}
