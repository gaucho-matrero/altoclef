package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.ChatMessageEvent;
import adris.altoclef.eventbus.events.GameOverlayEvent;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
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

    private boolean _stayInBed;

    private final TimerGame _regionScanTimer = new TimerGame(9);

    private final Vec3i BED_CLEAR_SIZE = new Vec3i(3, 2, 2);
    private final Vec3i[] BED_BOTTOM_PLATFORM = new Vec3i[]{
            new Vec3i(0, -1, 0),
            new Vec3i(1, -1, 0),
            new Vec3i(2, -1, 0),
    };
    // Kinda silly but who knows if we ever want to change it.
    private final Vec3i BED_PLACE_STAND_POS = new Vec3i(0, 0, 0);
    private final Vec3i BED_PLACE_POS = new Vec3i(1, 0, 0);
    private final Direction BED_PLACE_DIRECTION = Direction.UP;
    private final TimerGame _bedInteractTimeout = new TimerGame(5);
    private final TimerGame _inBedTimer = new TimerGame(1);
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private BlockPos _currentBedRegion;
    private BlockPos _currentStructure, _currentBreak;
    private boolean _spawnSet;
    private Subscription<ChatMessageEvent> _respawnPointSetMessageCheck;
    private Subscription<GameOverlayEvent> _respawnFailureMessageCheck;
    private boolean _sleepAttemptMade;
    private boolean _wasSleeping;
    private BlockPos _bedForSpawnPoint;

    public PlaceBedAndSetSpawnTask() {

    }

    public PlaceBedAndSetSpawnTask stayInBed() {
        _stayInBed = true;
        return this;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(ItemHelper.itemsToBlocks(ItemHelper.BED));
        mod.getBehaviour().push();
        _progressChecker.reset();
        _currentBedRegion = null;

        // Don't break our bed thing.
        mod.getBehaviour().avoidBlockPlacing(pos -> {
            if (_currentBedRegion != null) {
                BlockPos start = _currentBedRegion,
                        end = _currentBedRegion.add(BED_CLEAR_SIZE);
                return start.getX() <= pos.getX() && pos.getX() < end.getX()
                        && start.getZ() <= pos.getZ() && pos.getZ() < end.getZ()
                        && start.getY() <= pos.getY() && pos.getY() < end.getY();
            }
            return false;
        });
        mod.getBehaviour().avoidBlockBreaking(pos -> {
            if (_currentBedRegion != null) {
                for (Vec3i baseOffs : BED_BOTTOM_PLATFORM) {
                    BlockPos base = _currentBedRegion.add(baseOffs);
                    if (base.equals(pos)) return true;
                }
            }
            // Don't ever break beds. If one exists, we will sleep in it.
            return mod.getWorld().getBlockState(pos).getBlock() instanceof BedBlock;
        });
        _spawnSet = false;
        _sleepAttemptMade = false;
        _wasSleeping = false;
        _respawnPointSetMessageCheck = EventBus.subscribe(ChatMessageEvent.class, evt -> {
            String msg = evt.message.toString();
            if (msg.contains("Respawn point set")) {
                _spawnSet = true;
                _inBedTimer.reset();
            }
        });
        _respawnFailureMessageCheck = EventBus.subscribe(GameOverlayEvent.class, evt -> {
            final String[] NEUTRAL_MESSAGES = new String[]{"You can sleep only at night", "You can only sleep at night", "You may not rest now; there are monsters nearby"};
            for (String checkMessage : NEUTRAL_MESSAGES) {
                if (evt.message.contains(checkMessage)) {
                    if (!_sleepAttemptMade) {
                        _bedInteractTimeout.reset();
                    }
                    _sleepAttemptMade = true;
                }
            }
        });
    }

    public void resetSleep() {
        _spawnSet = false;
        _sleepAttemptMade = false;
        _wasSleeping = false;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!_progressChecker.check(mod) && _currentBedRegion != null) {
            _progressChecker.reset();
            Debug.logMessage("Searching new bed region.");
            _currentBedRegion = null;
        }
        if (mod.getPlayer().isTouchingWater() && mod.getItemStorage().hasItem(ItemHelper.BED)) {
            setDebugState("We are in water. Wandering");
            _currentBedRegion = null;
            return new TimeoutWanderTask();
        }
        if (WorldHelper.isInNetherPortal(mod)) {
            setDebugState("We are in nether portal. Wandering");
            _currentBedRegion = null;
            return new TimeoutWanderTask();
        }
        // Summary:
        // If we find a bed nearby, sleep in it.
        // Otherwise, place bed:
        //      Collect bed if we don't have one.
        //      Find a 3x2x1 region and clear it
        //      Stand on the edge of the long (3) side
        //      Place on the middle block, reliably placing the bed.

        // We cannot do this anywhere but the overworld.
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            setDebugState("Going to the overworld first.");
            return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen instanceof SleepingChatScreen) {
            _progressChecker.reset();
            setDebugState("Sleeping...");
            _wasSleeping = true;
            //Debug.logMessage("Closing sleeping thing");
            _spawnSet = true;
            return null;
        }

        if (_sleepAttemptMade) {
            if (_bedInteractTimeout.elapsed()) {
                Debug.logMessage("Failed to get \"Respawn point set\" message or sleeping, assuming that this bed already contains our spawn.");
                _spawnSet = true;
                return null;
            }
        }
        if (mod.getBlockTracker().anyFound(blockPos -> (WorldHelper.canReach(mod, blockPos) &&
                blockPos.isWithinDistance(mod.getPlayer().getPos(), 30) &&
                mod.getItemStorage().hasItem(ItemHelper.BED)) || (WorldHelper.canReach(mod, blockPos) &&
                !mod.getItemStorage().hasItem(ItemHelper.BED)), ItemHelper.itemsToBlocks(ItemHelper.BED))) {
            // Sleep in the nearest bed
            setDebugState("Going to bed to sleep...");
            return new DoToClosestBlockTask(toSleepIn -> {
                boolean closeEnough = toSleepIn.isWithinDistance(mod.getPlayer().getPos(), 3);
                if (closeEnough) {
                    // why 0.2? I'm tired.
                    Vec3d centerBed = new Vec3d(toSleepIn.getX() + 0.5, toSleepIn.getY() + 0.2, toSleepIn.getZ() + 0.5);
                    BlockHitResult hit = LookHelper.raycast(mod.getPlayer(), centerBed, 6);
                    // TODO: Kinda ugly, but I'm tired and fixing for the 2nd attempt speedrun so I will fix this block later
                    closeEnough = false;
                    if (hit.getType() != HitResult.Type.MISS) {
                        // At this poinAt, if we miss, we probably are close enough.
                        BlockPos p = hit.getBlockPos();
                        if (ArrayUtils.contains(ItemHelper.itemsToBlocks(ItemHelper.BED), mod.getWorld().getBlockState(p).getBlock())) {
                            // We have a bed!
                            closeEnough = true;
                        }
                    }
                }
                _bedForSpawnPoint = WorldHelper.getBedHead(mod, toSleepIn);
                if (_bedForSpawnPoint == null) {
                    _bedForSpawnPoint = toSleepIn;
                }
                if (!closeEnough) {
                    try {
                        Direction face = mod.getWorld().getBlockState(toSleepIn).get(BedBlock.FACING);
                        Direction side = face.rotateYClockwise();
                        /*
                        BlockPos targetMove = toSleepIn.offset(side).offset(side); // Twice, juust to make sure...
                         */
                        return new GetToBlockTask(_bedForSpawnPoint.add(side.getVector()));
                    } catch (IllegalArgumentException e) {
                        // If bed is not loaded, this will happen. In that case just get to the bed first.
                    }
                } else {
                    _inBedTimer.reset();
                }
                if (closeEnough) {
                    _inBedTimer.reset();
                }
                // Keep track of where our spawn point is
                _progressChecker.reset();
                return new InteractWithBlockTask(_bedForSpawnPoint);
            }, ItemHelper.itemsToBlocks(ItemHelper.BED));
        }
        if (_currentBedRegion != null) {
            Block getBlock = mod.getWorld().getBlockState(_currentBedRegion.add(BED_PLACE_POS)).getBlock();
            if (getBlock instanceof BedBlock) {
                mod.getBlockTracker().addBlock(getBlock, _currentBedRegion.add(BED_PLACE_POS));
            }
        }
        // Get a bed if we don't have one.
        if (!mod.getItemStorage().hasItem(ItemHelper.BED)) {
            setDebugState("Getting a bed first");
            return TaskCatalogue.getItemTask("bed", 1);
        }

        if (_currentBedRegion == null) {
            if (_regionScanTimer.elapsed()) {
                Debug.logMessage("Rescanning for nearby bed place position...");
                _regionScanTimer.reset();
                _currentBedRegion = this.locateBedRegion(mod, mod.getPlayer().getBlockPos());
            }
        }
        if (_currentBedRegion == null) {
            setDebugState("Searching for spot to place bed, wandering...");
            return new TimeoutWanderTask();
        }

        // Clear and make bed foundation

        for (Vec3i baseOffs : BED_BOTTOM_PLATFORM) {
            BlockPos toPlace = _currentBedRegion.add(baseOffs);
            if (!WorldHelper.isSolid(mod, toPlace)) {
                _currentStructure = toPlace;
                break;
            }
        }

        outer:
        for (int dx = 0; dx < BED_CLEAR_SIZE.getX(); ++dx) {
            for (int dz = 0; dz < BED_CLEAR_SIZE.getZ(); ++dz) {
                for (int dy = 0; dy < BED_CLEAR_SIZE.getY(); ++dy) {
                    BlockPos toClear = _currentBedRegion.add(dx, dy, dz);
                    if (WorldHelper.isSolid(mod, toClear)) {
                        _currentBreak = toClear;
                        break outer;
                    }
                }
            }
        }

        if (_currentStructure != null) {
            if (WorldHelper.isSolid(mod, _currentStructure)) {
                _currentStructure = null;
            } else {
                setDebugState("Placing structure for bed");
                return new PlaceStructureBlockTask(_currentStructure);
            }
        }
        if (_currentBreak != null) {
            if (!WorldHelper.isSolid(mod, _currentBreak)) {
                _currentBreak = null;
            } else {
                setDebugState("Clearing region for bed");
                return new DestroyBlockTask(_currentBreak);
            }
        }

        BlockPos toStand = _currentBedRegion.add(BED_PLACE_STAND_POS);
        // Our bed region is READY TO BE PLACED
        if (!mod.getPlayer().getBlockPos().equals(toStand)) {
            return new GetToBlockTask(toStand);
        }

        BlockPos toPlace = _currentBedRegion.add(BED_PLACE_POS);
        if (mod.getWorld().getBlockState(toPlace.offset(BED_PLACE_DIRECTION)).getBlock() instanceof BedBlock) {
            setDebugState("Waiting to rescan + find bed that we just placed. Should be almost instant.");
            _progressChecker.reset();
            return null;
        }
        setDebugState("Placing bed...");

        setDebugState("Filling in Portal");
        if (!_progressChecker.check(mod)) {
            mod.getClientBaritone().getPathingBehavior().cancelEverything();
            mod.getClientBaritone().getPathingBehavior().forceCancel();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            _progressChecker.reset();
        }

        // Scoot backwards if we're trying to place and fail
        if (thisOrChildSatisfies(task -> {
            if (task instanceof InteractWithBlockTask intr)
                return intr.getClickStatus() == InteractWithBlockTask.ClickResponse.CLICK_ATTEMPTED;
            return false;
        })) {
            mod.getInputControls().tryPress(Input.MOVE_BACK);
        }
        return new InteractWithBlockTask(new ItemTarget("bed", 1), BED_PLACE_DIRECTION, toPlace.offset(BED_PLACE_DIRECTION.getOpposite()), false);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(ItemHelper.itemsToBlocks(ItemHelper.BED));
        mod.getBehaviour().pop();
        EventBus.unsubscribe(_respawnPointSetMessageCheck);
        EventBus.unsubscribe(_respawnFailureMessageCheck);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof PlaceBedAndSetSpawnTask;
    }

    @Override
    protected String toDebugString() {
        return "Placing a bed nearby + resetting spawn point";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            Debug.logWarning("Can't place spawnpoint/sleep in a bed unless we're in the overworld!");
            return true;
        }
        return _spawnSet && !mod.getPlayer().isSleeping() && _inBedTimer.elapsed();
    }

    public BlockPos getBedSleptPos() {
        return _bedForSpawnPoint;
    }

    public boolean isSpawnSet() {
        return _spawnSet;
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
                    double distance = attemptStandPos.getSquaredDistance(mod.getPlayer().getPos());
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
        final Vec3i[] CHECK = new Vec3i[]{
                new Vec3i(0, 0, 0),
                new Vec3i(-1, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(0, 1, 0),
                new Vec3i(0, -1, 0),
                new Vec3i(0, 0, 1),
                new Vec3i(0, 0, -1)
        };
        for (Vec3i offs : CHECK) {
            if (!isGoodAsBorder(mod, pos.add(offs))) return false;
        }
        return true;
    }

    private boolean isGoodAsBorder(AltoClef mod, BlockPos pos) {
        if (WorldHelper.isSolid(mod, pos)) {
            return WorldHelper.canBreak(mod, pos);
        } else return (WorldHelper.isAir(mod, pos));
    }
}
