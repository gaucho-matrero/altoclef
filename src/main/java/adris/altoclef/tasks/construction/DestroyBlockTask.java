package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.movement.RunAwayFromPositionTask;
import adris.altoclef.tasks.movement.SafeRandomShimmyTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.Slot;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Destroy a block at a position.
 */
public class DestroyBlockTask extends Task implements ITaskRequiresGrounded {
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final MovementProgressChecker _moveChecker = new MovementProgressChecker();
    private final BlockPos _pos;
    Block[] annoyingBlocks = new Block[]{
            Blocks.VINE,
            Blocks.NETHER_SPROUTS,
            Blocks.CAVE_VINES,
            Blocks.CAVE_VINES_PLANT,
            Blocks.TWISTING_VINES,
            Blocks.TWISTING_VINES_PLANT,
            Blocks.WEEPING_VINES_PLANT,
            Blocks.LADDER,
            Blocks.BIG_DRIPLEAF,
            Blocks.BIG_DRIPLEAF_STEM,
            Blocks.SMALL_DRIPLEAF,
            Blocks.TALL_GRASS,
            Blocks.GRASS,
            Blocks.SWEET_BERRY_BUSH
    };
    private Task _unstuckTask = null;
    private boolean isMining;

    public DestroyBlockTask(BlockPos pos) {
        _pos = pos;
    }

    /**
     * Generates an array of BlockPos objects representing the sides of a given BlockPos.
     *
     * @param pos The BlockPos object to generate the sides for.
     * @return An array of BlockPos objects representing the sides of the given BlockPos.
     */
    private static BlockPos[] generateSides(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // Log the values of x, y, and z for debugging
        Debug.logInternal("x = " + x);
        Debug.logInternal("y = " + y);
        Debug.logInternal("z = " + z);

        return new BlockPos[]{
                new BlockPos(x + 1, y, z),
                new BlockPos(x - 1, y, z),
                new BlockPos(x, y, z + 1),
                new BlockPos(x, y, z - 1),
                new BlockPos(x + 1, y, z - 1),
                new BlockPos(x + 1, y, z + 1),
                new BlockPos(x - 1, y, z - 1),
                new BlockPos(x - 1, y, z + 1)
        };
    }

    /**
     * Checks if a block is annoying.
     *
     * @param mod The AltoClef mod instance.
     * @param pos The position of the block.
     * @return true if the block is annoying, false otherwise.
     */
    private boolean isAnnoying(AltoClef mod, BlockPos pos) {
        for (Block annoyingBlock : annoyingBlocks) {
            boolean isAnnoying = mod.getWorld().getBlockState(pos).getBlock() == annoyingBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof DoorBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof FenceBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof FenceGateBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof FlowerBlock;
            if (isAnnoying) {
                Debug.logInternal("Block at position " + pos + " is annoying.");
                return true;
            }
        }
        Debug.logInternal("Block at position " + pos + " is not annoying.");
        return false;
    }

    /**
     * Returns the position of the block where the player is stuck.
     * If there are no annoying block positions, returns null.
     *
     * @param mod The instance of the AltoClef mod.
     * @return The BlockPos of the stuck block, or null if none found.
     */
    private BlockPos stuckInBlock(AltoClef mod) {
        BlockPos playerPos = mod.getPlayer().getBlockPos();
        BlockPos[] toCheck = generateSides(playerPos);
        BlockPos[] toCheckHigh = generateSides(playerPos.up());

        // Check if player position is annoying
        if (isAnnoying(mod, playerPos)) {
            Debug.logInternal("Player position is annoying: " + playerPos);
            return playerPos;
        }

        // Check if player position (up) is annoying
        if (isAnnoying(mod, playerPos.up())) {
            Debug.logInternal("Player position (up) is annoying: " + playerPos.up());
            return playerPos.up();
        }

        // Check each side block position
        for (BlockPos check : toCheck) {
            if (isAnnoying(mod, check)) {
                Debug.logInternal("Block position is annoying: " + check);
                return check;
            }
        }

        // Check each high block position
        for (BlockPos check : toCheckHigh) {
            if (isAnnoying(mod, check)) {
                Debug.logInternal("Block position (up) is annoying: " + check);
                return check;
            }
        }

        Debug.logInternal("No annoying block positions found.");
        return null;
    }

    /**
     * Retrieves a task to get the fence unstuck.
     *
     * @return The task to get the fence unstuck.
     */
    private Task getFenceUnstuckTask() {
        // Log the start of the function
        Debug.logInternal("Entering getFenceUnstuckTask");

        // Create a safe random shimmy task
        Task task = createSafeRandomShimmyTask();

        // Log the end of the function
        Debug.logInternal("Exiting getFenceUnstuckTask");

        // Return the task
        return task;
    }

    /**
     * Creates a new instance of SafeRandomShimmyTask.
     *
     * @return The created SafeRandomShimmyTask.
     */
    private Task createSafeRandomShimmyTask() {
        Task task = new SafeRandomShimmyTask();
        Debug.logInternal("Created SafeRandomShimmyTask: " + task);
        return task;
    }

    /**
     * This method is called when the mod starts.
     * It cancels any ongoing pathing behavior, resets move checker and stuck check.
     * If the cursor stack is not empty, it tries to move it to a suitable slot in the player inventory.
     * If the item can be thrown away, it drops it in an undefined slot or the garbage slot.
     * If the cursor stack is empty, it closes the screen.
     *
     * @param mod The AltoClef mod instance.
     */
    @Override
    protected void onStart(AltoClef mod) {
        // Cancel any ongoing pathing behavior.
        mod.getClientBaritone().getPathingBehavior().forceCancel();

        // Reset move checker and stuck check.
        _moveChecker.reset();
        stuckCheck.reset();

        // Get the item stack in the cursor slot.
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        Debug.logInternal("Cursor stack: " + cursorStack);

        // If the cursor stack is not empty, try to move it to a suitable slot in the player inventory.
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            Debug.logInternal("Move to slot: " + moveTo);

            // If there is a slot where the item can fit, click on that slot to move the item.
            moveTo.ifPresent(slot -> {
                mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP);
                Debug.logInternal("Clicked slot: " + slot);
            });

            // If the item can be thrown away, click on an undefined slot to drop the item.
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                Debug.logInternal("Clicked undefined slot");
            }

            // Get the garbage slot and click on it to move the item.
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            Debug.logInternal("Garbage slot: " + garbage);

            garbage.ifPresent(slot -> {
                mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP);
                Debug.logInternal("Clicked slot: " + slot);
            });

            // Click on an undefined slot to drop the item.
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            Debug.logInternal("Clicked undefined slot");
        } else {
            // If the cursor stack is empty, close the screen.
            StorageHelper.closeScreen();
            Debug.logInternal("Closed screen");
        }
    }

    /**
     * This method is called periodically to perform various tasks.
     *
     * @param mod The instance of the mod.
     * @return The next task to be executed.
     */
    @Override
    protected Task onTick(AltoClef mod) {
        // Check if there is white wool at the specified position
        if (mod.getWorld().getBlockState(_pos).getBlock() == Blocks.WHITE_WOOL) {
            // Iterate over all entities in the world
            Iterable<Entity> entities = mod.getWorld().getEntities();
            for (Entity entity : entities) {
                // Check if the entity is a PillagerEntity and is within a distance of 144 blocks from the position
                if (entity instanceof PillagerEntity && _pos.isWithinDistance(entity.getPos(), 144)) {
                    Debug.logMessage("Blacklisting pillager wool.");
                    // Request the block at the position to be marked as unreachable
                    mod.getBlockTracker().requestBlockUnreachable(_pos, 0);
                }
            }
        }

        // Reset the move checker if Baritone is currently pathing
        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            _moveChecker.reset();
        }

        // Check if the player is in a Nether portal
        if (WorldHelper.isInNetherPortal(mod)) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
                // Hold the sneak and move forward inputs to exit the Nether portal
                mod.getInputControls().hold(Input.SNEAK);
                mod.getInputControls().hold(Input.MOVE_FORWARD);
                return null;
            } else {
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            mod.getInputControls().release(Input.SNEAK);
            mod.getInputControls().release(Input.MOVE_BACK);
            mod.getInputControls().release(Input.MOVE_FORWARD);
        }

        // Check if there is an active unstuck task and the player is stuck in a block
        if (_unstuckTask != null && _unstuckTask.isActive() && !_unstuckTask.isFinished(mod) && stuckInBlock(mod) != null) {
            setDebugState("Getting unstuck from block.");
            stuckCheck.reset();
            // Release control of Baritone's custom goal process and explore process
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            return _unstuckTask;
        }

        // Check if the move checker or the stuck check failed
        if (!_moveChecker.check(mod) || !stuckCheck.check(mod)) {
            BlockPos blockStuck = stuckInBlock(mod);
            if (blockStuck != null) {
                _unstuckTask = getFenceUnstuckTask();
                return _unstuckTask;
            }
            stuckCheck.reset();
        }

        // Check if the move checker failed
        if (!_moveChecker.check(mod)) {
            _moveChecker.reset();
            // Request the block at the position to be marked as unreachable
            mod.getBlockTracker().requestBlockUnreachable(_pos);
        }

        // Check if the block above the position is not solid, the player is above the position,
        // and the player is within a distance of 0.89 blocks from the position
        if (!WorldHelper.isSolid(mod, _pos.up()) && mod.getPlayer().getPos().y > _pos.getY() && _pos.isWithinDistance(mod.getPlayer().isOnGround() ? mod.getPlayer().getPos() : mod.getPlayer().getPos().add(0, -1, 0), 0.89)) {
            if (WorldHelper.dangerousToBreakIfRightAbove(mod, _pos)) {
                setDebugState("It's dangerous to break as we're right above it, moving away and trying again.");
                return new RunAwayFromPositionTask(3, _pos.getY(), _pos);
            }
        }

        Optional<Rotation> reach = LookHelper.getReach(_pos);
        if (reach.isPresent() && (mod.getPlayer().isTouchingWater() || mod.getPlayer().isOnGround()) && !mod.getFoodChain().needsToEat() && !WorldHelper.isInNetherPortal(mod) && mod.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
            setDebugState("Block in range, mining...");
            stuckCheck.reset();
            isMining = true;
            mod.getInputControls().release(Input.SNEAK);
            mod.getInputControls().release(Input.MOVE_BACK);
            mod.getInputControls().release(Input.MOVE_FORWARD);
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getBuilderProcess().onLostControl();
            if (!LookHelper.isLookingAt(mod, reach.get())) {
                LookHelper.lookAt(mod, reach.get());
            }
            // Tool equip is handled in `PlayerInteractionFixChain`. Oof.
            mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
        } else {
            setDebugState("Getting to block...");
            if (isMining && mod.getPlayer().isTouchingWater()) {
                isMining = false;
                mod.getBlockTracker().requestBlockUnreachable(_pos);
            } else {
                isMining = false;
            }
            boolean isCloseToMoveBack = _pos.isWithinDistance(mod.getPlayer().getPos(), 2);
            if (isCloseToMoveBack) {
                if (!mod.getClientBaritone().getPathingBehavior().isPathing() && !mod.getPlayer().isTouchingWater() &&
                        !mod.getFoodChain().needsToEat()) {
                    mod.getInputControls().hold(Input.MOVE_BACK);
                    mod.getInputControls().hold(Input.SNEAK);
                } else {
                    mod.getInputControls().release(Input.MOVE_BACK);
                    mod.getInputControls().release(Input.SNEAK);
                }
            }
            if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                mod.getClientBaritone().getBuilderProcess().onLostControl();
                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(mod.getWorld().getBlockState(_pos.up()).getBlock() ==
                        Blocks.SNOW ? new GoalBlock(_pos) : new GoalNear(_pos, 1));
            }
        }
        return null;
    }

    /**
     * This method is called when the task is interrupted or stopped.
     * It cancels Baritone pathing and releases certain input controls.
     *
     * @param mod           The AltoClef mod instance.
     * @param interruptTask The task that interrupted the current task.
     */
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Cancel Baritone pathing
        mod.getClientBaritone().getPathingBehavior().forceCancel();

        // If not in game, return
        if (!AltoClef.inGame()) {
            return;
        }

        // Release input controls
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, false);
        mod.getInputControls().release(Input.SNEAK);
        mod.getInputControls().release(Input.MOVE_BACK);
        mod.getInputControls().release(Input.MOVE_FORWARD);

        // Logging statements for debugging
        Debug.logInternal("onStop method called");
        Debug.logInternal("Baritone pathing cancelled");
        if (!AltoClef.inGame()) {
            Debug.logInternal("Not in game");
        }
        Debug.logInternal("Left click input force state set to false");
        Debug.logInternal("Released sneak input control");
        Debug.logInternal("Released move back input control");
        Debug.logInternal("Released move forward input control");
    }

    /**
     * Checks if the block at the given position is air.
     *
     * @param mod The AltoClef mod instance
     * @return true if the block is air, false otherwise
     */
    @Override
    public boolean isFinished(AltoClef mod) {
        BlockState blockState = mod.getWorld().getBlockState(_pos);
        boolean isAir = blockState.isAir();
        Debug.logInternal("Block at position " + _pos + " is air: " + isAir);
        return isAir;
    }

    /**
     * Checks if this task is equal to another task.
     *
     * @param other The other task to compare against.
     * @return True if the tasks are equal, false otherwise.
     */
    @Override
    protected boolean isEqual(Task other) {
        boolean isSame = false;

        // Check if the other task is an instance of DestroyBlockTask
        if (other instanceof DestroyBlockTask destroyBlockTask) {

            // Check if the positions of the tasks are equal
            if (destroyBlockTask._pos.equals(_pos)) {
                isSame = true;
            }
        }

        // Log the result of the equality check
        Debug.logInternal("isEqual result: " + isSame);

        // Return the result of the equality check
        return isSame;
    }

    /**
     * Generates a debug string representing the block destruction position.
     *
     * @return The debug string.
     */
    @Override
    protected String toDebugString() {
        return "Destroy block at " + _pos.toShortString();
    }
}
