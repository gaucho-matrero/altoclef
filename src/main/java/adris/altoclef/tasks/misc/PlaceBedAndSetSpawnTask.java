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

    private final TimerGame _regionScanTimer = new TimerGame(9);
    private final Vec3i BED_CLEAR_SIZE = new Vec3i(3, 2, 3);
    private final Vec3i[] BED_BOTTOM_PLATFORM = new Vec3i[]{
            new Vec3i(0, -1, 0),
            new Vec3i(1, -1, 0),
            new Vec3i(2, -1, 0),
            new Vec3i(0, -1, -1),
            new Vec3i(1, -1, -1),
            new Vec3i(2, -1, -1),
            new Vec3i(0, -1, 1),
            new Vec3i(1, -1, 1),
            new Vec3i(2, -1, 1)
    };
    // Kinda silly but who knows if we ever want to change it.
    private final Vec3i BED_PLACE_STAND_POS = new Vec3i(0, 0, 1);
    private final Vec3i BED_PLACE_POS = new Vec3i(1, 0, 1);
    private final Vec3i[] BED_PLACE_POS_OFFSET = new Vec3i[]{
            BED_PLACE_POS,
            BED_PLACE_POS.north(),
            BED_PLACE_POS.south(),
            BED_PLACE_POS.east(),
            BED_PLACE_POS.west(),
            BED_PLACE_POS.add(-1, 0, 1),
            BED_PLACE_POS.add(1, 0, 1),
            BED_PLACE_POS.add(-1, 0, -1),
            BED_PLACE_POS.add(1, 0, -1),
            BED_PLACE_POS.north(2),
            BED_PLACE_POS.south(2),
            BED_PLACE_POS.east(2),
            BED_PLACE_POS.west(2),
            BED_PLACE_POS.add(-2, 0, 1),
            BED_PLACE_POS.add(-2, 0, 2),
            BED_PLACE_POS.add(2, 0, 1),
            BED_PLACE_POS.add(2, 0, 2),
            BED_PLACE_POS.add(-2, 0, -1),
            BED_PLACE_POS.add(-2, 0, -2),
            BED_PLACE_POS.add(2, 0, -1),
            BED_PLACE_POS.add(2, 0, -2)
    };
    private final Direction BED_PLACE_DIRECTION = Direction.UP;
    private final TimerGame _bedInteractTimeout = new TimerGame(5);
    private final TimerGame _inBedTimer = new TimerGame(1);
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private boolean _stayInBed;
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

    /**
     * Sets the flag to stay in bed.
     *
     * @return The current instance of PlaceBedAndSetSpawnTask.
     */
    public PlaceBedAndSetSpawnTask stayInBed() {
        // Log method call
        Debug.logInternal("Stay in bed method called");

        // Set _stayInBed flag to true
        this._stayInBed = true;
        Debug.logInternal("Setting _stayInBed to true");

        // Return current instance
        return this;
    }

    /**
     * This method is called when the mod starts.
     * It initializes various variables and sets up behaviours for the mod.
     */
    @Override
    protected void onStart(AltoClef mod) {
        // Track bed blocks
        mod.getBlockTracker().trackBlock(ItemHelper.itemsToBlocks(ItemHelper.BED));

        // Push the current behaviour
        mod.getBehaviour().push();

        // Reset progress checker
        _progressChecker.reset();

        // Reset current bed region
        _currentBedRegion = null;

        // Avoid placing blocks near bed
        mod.getBehaviour().avoidBlockPlacing(pos -> {
            if (_currentBedRegion != null) {
                BlockPos start = _currentBedRegion;
                BlockPos end = _currentBedRegion.add(BED_CLEAR_SIZE);
                return start.getX() <= pos.getX() && pos.getX() < end.getX()
                        && start.getZ() <= pos.getZ() && pos.getZ() < end.getZ()
                        && start.getY() <= pos.getY() && pos.getY() < end.getY();
            }
            return false;
        });

        // Avoid breaking blocks near bed
        mod.getBehaviour().avoidBlockBreaking(pos -> {
            if (_currentBedRegion != null) {
                for (Vec3i baseOffs : BED_BOTTOM_PLATFORM) {
                    BlockPos base = _currentBedRegion.add(baseOffs);
                    if (base.equals(pos)) return true;
                }
            }
            // Don't ever break beds. If one exists, we will sleep in it.
            if (mod.getWorld() != null) {
                return mod.getWorld().getBlockState(pos).getBlock() instanceof BedBlock;
            }
            return false;
        });

        // Reset variables for sleep handling
        _spawnSet = false;
        _sleepAttemptMade = false;
        _wasSleeping = false;

        // Subscribe to respawn point set message event
        _respawnPointSetMessageCheck = EventBus.subscribe(ChatMessageEvent.class, evt -> {
            String msg = evt.toString();
            if (msg.contains("Respawn point set")) {
                _spawnSet = true;
                _inBedTimer.reset();
            }
        });

        // Subscribe to respawn failure message event
        _respawnFailureMessageCheck = EventBus.subscribe(GameOverlayEvent.class, evt -> {
            final String[] NEUTRAL_MESSAGES = new String[]{
                    "You can sleep only at night",
                    "You can only sleep at night",
                    "You may not rest now; there are monsters nearby"
            };
            for (String checkMessage : NEUTRAL_MESSAGES) {
                if (evt.message.contains(checkMessage)) {
                    if (!_sleepAttemptMade) {
                        _bedInteractTimeout.reset();
                    }
                    _sleepAttemptMade = true;
                }
            }
        });

        // Logging statements for debugging
        Debug.logInternal("Started onStart() method");
        Debug.logInternal("Current bed region: " + _currentBedRegion);
        Debug.logInternal("Spawn set: " + _spawnSet);
    }

    public void resetSleep() {
        _spawnSet = false;
        _sleepAttemptMade = false;
        _wasSleeping = false;
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
                blockPos.isWithinDistance(mod.getPlayer().getPos(), 40) &&
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
            for (Vec3i BedPlacePos : BED_PLACE_POS_OFFSET) {
                Block getBlock = mod.getWorld().getBlockState(_currentBedRegion.add(BedPlacePos)).getBlock();
                if (getBlock instanceof BedBlock) {
                    mod.getBlockTracker().addBlock(getBlock, _currentBedRegion.add(BedPlacePos));
                    break;
                }
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

    /**
     * Override method called when the task is interrupted.
     *
     * @param mod           The AltoClef mod instance.
     * @param interruptTask The task that interrupted this task.
     */
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Stop tracking beds
        mod.getBlockTracker().stopTracking(ItemHelper.itemsToBlocks(ItemHelper.BED));

        // Pop the behaviour stack
        mod.getBehaviour().pop();

        // Unsubscribe from respawn point set message
        EventBus.unsubscribe(_respawnPointSetMessageCheck);

        // Unsubscribe from respawn failure message
        EventBus.unsubscribe(_respawnFailureMessageCheck);

        // Logging statements for debugging
        Debug.logInternal("Tracking stopped for beds");
        Debug.logInternal("Behaviour popped");
        Debug.logInternal("Unsubscribed from respawn point set message");
        Debug.logInternal("Unsubscribed from respawn failure message");
    }

    /**
     * Checks if the given task is equal to this task.
     *
     * @param other The task to compare with.
     * @return True if the tasks are equal, false otherwise.
     */
    @Override
    protected boolean isEqual(Task other) {
        // Check if the other task is an instance of PlaceBedAndSetSpawnTask
        boolean isSameTask = (other instanceof PlaceBedAndSetSpawnTask);

        if (!isSameTask) {
            // Log a debug message if the tasks are not of the same type
            Debug.logInternal("Tasks are not of the same type");
        }

        return isSameTask;
    }

    /**
     * Returns a string representation of the action performed by this method.
     * The action is described as "Placing a bed nearby + resetting spawn point".
     *
     * @return a string representation of the action
     */
    @Override
    protected String toDebugString() {
        return "Placing a bed nearby + resetting spawn point";
    }

    /**
     * Checks if the spawnpoint/sleep condition is finished.
     *
     * @param mod The AltoClef mod instance.
     * @return Whether the condition is finished.
     */
    @Override
    public boolean isFinished(AltoClef mod) {
        // Check if we are in the overworld
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            Debug.logInternal("Can't place spawnpoint/sleep in a bed unless we're in the overworld!");
            return true;
        }

        // Check if player is sleeping
        boolean isSleeping = mod.getPlayer().isSleeping();

        // Check if timer has elapsed
        boolean timerElapsed = _inBedTimer.elapsed();

        // Check if spawnpoint is set, player is not sleeping, and timer has elapsed
        boolean isFinished = _spawnSet && !isSleeping && timerElapsed;

        // Log the values for debugging
        Debug.logInternal("isSleeping: " + isSleeping);
        Debug.logInternal("timerElapsed: " + timerElapsed);
        Debug.logInternal("isFinished: " + isFinished);

        return isFinished;
    }

    /**
     * Returns the position of the bed where the player last slept.
     *
     * @return The BlockPos of the bed.
     */
    public BlockPos getBedSleptPos() {
        // Log a debug message indicating that the bed slept position is being fetched
        Debug.logInternal("Fetching bed slept position");

        // Return the stored bed position
        return _bedForSpawnPoint;
    }

    /**
     * Checks if the spawn is set.
     *
     * @return true if the spawn is set, false otherwise.
     */
    public boolean isSpawnSet() {
        // Log internal message for debugging
        Debug.logInternal("Checking if spawn is set");

        // Return the value of the _spawnSet variable
        return _spawnSet;
    }

    /**
     * Locates the closest good position within a specified range from the given origin.
     *
     * @param mod    The mod instance.
     * @param origin The origin position.
     * @return The closest good position.
     */
    private BlockPos locateBedRegion(AltoClef mod, BlockPos origin) {
        final int SCAN_RANGE = 10;

        BlockPos closestGood = null;
        double closestDist = Double.POSITIVE_INFINITY;

        for (int x = origin.getX() - SCAN_RANGE; x < origin.getX() + SCAN_RANGE; ++x) {
            for (int z = origin.getZ() - SCAN_RANGE; z < origin.getZ() + SCAN_RANGE; ++z) {
                outer:
                for (int y = origin.getY() - SCAN_RANGE; y < origin.getY() + SCAN_RANGE; ++y) {
                    BlockPos attemptPos = new BlockPos(x, y, z);
                    double distance = attemptPos.getSquaredDistance(mod.getPlayer().getPos());

                    Debug.logInternal("Checking position: " + attemptPos);

                    if (distance > closestDist) {
                        Debug.logInternal("Skipping position: " + attemptPos);
                        continue;
                    }

                    if (isGoodPosition(mod, attemptPos)) {
                        Debug.logInternal("Found good position: " + attemptPos);
                        closestGood = attemptPos;
                        closestDist = distance;
                    }
                }
            }
        }

        return closestGood;
    }

    /**
     * Check if the given position is a good position.
     * A position is considered good if all blocks within a specific area around it can be placed inside or cleared.
     *
     * @param mod The AltoClef mod instance.
     * @param pos The position to check.
     * @return True if the position is good, false otherwise.
     */
    private boolean isGoodPosition(AltoClef mod, BlockPos pos) {
        final BlockPos BED_CLEAR_SIZE = new BlockPos(2, 1, 2);

        // Iterate over the area around the position
        for (int x = 0; x < BED_CLEAR_SIZE.getX(); ++x) {
            for (int y = 0; y < BED_CLEAR_SIZE.getY(); ++y) {
                for (int z = 0; z < BED_CLEAR_SIZE.getZ(); ++z) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (!isGoodToPlaceInsideOrClear(mod, checkPos)) {
                        Debug.logInternal("Not a good position: " + checkPos);
                        return false;
                    }
                }
            }
        }

        Debug.logInternal("Good position");
        return true;
    }

    /**
     * Checks if a given position is good to place inside or clear.
     *
     * @param mod The AltoClef instance.
     * @param pos The position to check.
     * @return True if the position is good to place inside or clear, false otherwise.
     */
    private boolean isGoodToPlaceInsideOrClear(AltoClef mod, BlockPos pos) {
        // Define the offsets to check around the position
        final Vec3i[] CHECK = {
                new Vec3i(0, 0, 0),
                new Vec3i(-1, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(0, 1, 0),
                new Vec3i(0, -1, 0),
                new Vec3i(0, 0, 1),
                new Vec3i(0, 0, -1)
        };

        // Check each offset
        for (Vec3i offset : CHECK) {
            BlockPos newPos = pos.add(offset);
            if (!isGoodAsBorder(mod, newPos)) {
                Debug.logInternal("Not good as border: " + newPos);
                return false;
            }
        }

        Debug.logInternal("Good to place inside or clear");
        return true;
    }

    /**
     * Checks if a block is suitable as a border block.
     *
     * @param mod The mod instance.
     * @param pos The position of the block.
     * @return true if the block can be used as a border, false otherwise.
     */
    private boolean isGoodAsBorder(AltoClef mod, BlockPos pos) {
        // Check if the block is solid
        boolean isSolid = WorldHelper.isSolid(mod, pos);
        Debug.logInternal("isSolid: " + isSolid);

        if (isSolid) {
            // Check if the block can be broken
            boolean canBreak = WorldHelper.canBreak(mod, pos);
            Debug.logInternal("canBreak: " + canBreak);
            return canBreak;
        } else {
            // Check if the block is air
            boolean isAir = WorldHelper.isAir(mod, pos);
            Debug.logInternal("isAir: " + isAir);
            return isAir;
        }
    }
}
