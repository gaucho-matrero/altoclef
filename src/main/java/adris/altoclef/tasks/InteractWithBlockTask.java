package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.baritone.GoalAnd;
import adris.altoclef.util.baritone.GoalBlockSide;
import adris.altoclef.util.csharpisbetter.Action;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.Optional;

public class InteractWithBlockTask extends Task {

    private static final int MAX_REACH = 7;
    public final Action TimedOut = new Action();
    private final ItemTarget _toUse;
    private final Direction _direction;
    private final BlockPos _target;
    private final boolean _walkInto;
    private final Vec3i _interactOffset;
    private final Input _interactInput;
    private final boolean _shiftClick;
    private final TimerGame _clickTimer = new TimerGame(5);
    private final MovementProgressChecker _moveChecker = new MovementProgressChecker(4, 0.1, 4, 0.01);
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(5);
    private final int reachDistance = 0;

    public InteractWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto, Vec3i interactOffset, boolean shiftClick) {
        _toUse = toUse;
        _direction = direction;
        _target = target;
        _interactInput = interactInput;
        _walkInto = walkInto;
        _interactOffset = interactOffset;
        _shiftClick = shiftClick;
    }

    public InteractWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto, boolean shiftClick) {
        this(toUse, direction, target, interactInput, walkInto, Vec3i.ZERO, shiftClick);
    }

    public InteractWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, boolean walkInto) {
        this(toUse, direction, target, Input.CLICK_RIGHT, walkInto, true);
    }

    public InteractWithBlockTask(ItemTarget toUse, BlockPos target, boolean walkInto, Vec3i interactOffset) {
        // null means any side is OK
        this(toUse, null, target, Input.CLICK_RIGHT, walkInto, interactOffset, true);
    }

    public InteractWithBlockTask(ItemTarget toUse, BlockPos target, boolean walkInto) {
        this(toUse, target, walkInto, Vec3i.ZERO);
    }

    public InteractWithBlockTask(ItemTarget toUse, BlockPos target) {
        this(toUse, target, false);
    }

    public InteractWithBlockTask(BlockPos target) {
        this(null, null, target, Input.CLICK_RIGHT, false, false);
    }

    public static Optional<Rotation> getReach(BlockPos target, Direction side) {
        Optional<Rotation> reachable;
        IPlayerContext ctx = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext();
        if (side == null) {
            assert MinecraftClient.getInstance().player != null;
            reachable = RotationUtils.reachable(ctx.player(), target, ctx.playerController().getBlockReachDistance());
        } else {
            Vec3i sideVector = side.getVector();
            Vec3d centerOffset = new Vec3d(0.5 + sideVector.getX() * 0.5, 0.5 + sideVector.getY() * 0.5, 0.5 + sideVector.getZ() * 0.5);

            Vec3d sidePoint = centerOffset.add(target.getX(), target.getY(), target.getZ());

            //reachable(this.ctx.player(), _target, this.ctx.playerController().getBlockReachDistance());
            reachable = RotationUtils.reachableOffset(ctx.player(), target, sidePoint, ctx.playerController().getBlockReachDistance(), false);

            // Check for right angle
            if (reachable.isPresent()) {
                // Note: If sneak, use RotationUtils.inferSneakingEyePosition
                Vec3d camPos = ctx.player().getCameraPosVec(1.0F);
                Vec3d vecToPlayerPos = camPos.subtract(sidePoint);

                double dot = vecToPlayerPos.normalize().dotProduct(new Vec3d(sideVector.getX(), sideVector.getY(), sideVector.getZ()));
                if (dot < 0) {
                    // We're perpendicular and cannot face.
                    return Optional.empty();
                }
            }
        }
        return reachable;
    }

    private static Goal createGoalForInteract(BlockPos target, int reachDistance, Direction interactSide, Vec3i interactOffset, boolean walkInto) {

        boolean sideMatters = interactSide != null;
        if (sideMatters) {
            Vec3i offs = interactSide.getVector();
            if (offs.getY() == -1) {
                // If we're below, place ourselves two blocks below.
                offs = offs.down();
            }
            target = target.add(offs);
        }

        if (walkInto) {
            return new GoalTwoBlocks(target);
        } else {
            if (sideMatters) {
                // Make sure we're on the right side of the block.
                Goal sideGoal = new GoalBlockSide(target, interactSide, 1);
                return new GoalAnd(sideGoal, new GoalNear(target.add(interactOffset), reachDistance));
            } else {
                // TODO: Cleaner method of picking which side to approach from. This is only here for the lava stuff.
                return new GoalNear(target.add(interactOffset), reachDistance);
            }
        }
    }

    @Override
    protected void onStart(AltoClef mod) {
        _moveChecker.reset();
        _wanderTask.resetWander();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // Get our use item first
        if (_toUse != null && !mod.getInventoryTracker().targetMet(_toUse)) {
            _moveChecker.reset();
            _clickTimer.reset();
            return TaskCatalogue.getItemTask(_toUse);
        }

        // Wander and check
        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            _moveChecker.reset();
            _clickTimer.reset();
            return _wanderTask;
        }
        if (!_moveChecker.check(mod)) {
            Debug.logMessage("Failed, blacklisting and wandering.");
            mod.getBlockTracker().requestBlockUnreachable(_target);
            TimedOut.invoke();
            return _wanderTask;
        }

        Goal moveGoal = createGoalForInteract(_target, reachDistance, _direction, _interactOffset, _walkInto);
        ICustomGoalProcess proc = mod.getClientBaritone().getCustomGoalProcess();

        switch (rightClick(mod)) {
            case CANT_REACH:
                // Get to our goal then
                if (!proc.isActive()) {
                    proc.setGoalAndPath(moveGoal);
                }
                _clickTimer.reset();
                break;
            case WAIT_FOR_CLICK:
                if (proc.isActive()) {
                    proc.onLostControl();
                }
                _clickTimer.reset();
                break;
            case CLICK_ATTEMPTED:
                if (proc.isActive()) {
                    proc.onLostControl();
                }
                if (_clickTimer.elapsed()) {
                    // We tried clicking but failed.
                    _clickTimer.reset();
                    TimedOut.invoke();
                    mod.getBlockTracker().requestBlockUnreachable(_target);
                    return _wanderTask;
                }
                break;
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getInputControls().release(Input.SNEAK);
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return false;
        //return _trying && !proc(mod).isActive();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof InteractWithBlockTask) {
            InteractWithBlockTask task = (InteractWithBlockTask) obj;
            if ((task._direction == null) != (_direction == null)) return false;
            if (task._direction != null && !task._direction.equals(_direction)) return false;
            if ((task._toUse == null) != (_toUse == null)) return false;
            if (task._toUse != null && !task._toUse.equals(_toUse)) return false;
            if (!task._target.equals(_target)) return false;
            if (!task._interactInput.equals(_interactInput)) return false;
            return task._walkInto == _walkInto;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Interact using " + _toUse + " at " + _target + " dir " + _direction;
    }

    private ClickResponse rightClick(AltoClef mod) {

        // Don't interact if baritone can't interact.
        if (Baritone.getAltoClefSettings().isInteractionPaused()) return ClickResponse.WAIT_FOR_CLICK;

        Optional<Rotation> reachable = getCurrentReach();
        if (reachable.isPresent()) {
            //Debug.logMessage("Reachable: UPDATE");
            mod.getClientBaritone().getLookBehavior().updateTarget(reachable.get(), true);
            if (mod.getClientBaritone().getPlayerContext().isLookingAt(_target)) {
                if (_toUse != null) {
                    if (!mod.getInventoryTracker().equipItem(_toUse)) {
                        Debug.logWarning("Failed to equip item: " + Util.arrayToString(_toUse.getMatches()));
                    }
                } else {
                    mod.getInventoryTracker().deequipRightClickableItem();
                }
                mod.getInputControls().tryPress(_interactInput);
                //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(_interactInput, true);
                if (_shiftClick) {
                    mod.getInputControls().hold(Input.SNEAK);
                }
                //System.out.println(this.ctx.player().playerScreenHandler);

                /*
                if (this.arrivalTickCount++ > 20 || _cancelRightClick) {
                    _failed = true;
                    this.logDirect("Right click timed out/cancelled");
                    return ClickResponse.CLICK_ATTEMPTED;
                }
                 */
            }
            return ClickResponse.WAIT_FOR_CLICK;
        }
        if (_shiftClick) {
            mod.getInputControls().release(Input.SNEAK);
        }
        return ClickResponse.CANT_REACH;
    }

    public Optional<Rotation> getCurrentReach() {
        return getReach(_target, _direction);
    }

    private enum ClickResponse {
        CANT_REACH,
        WAIT_FOR_CLICK,
        CLICK_ATTEMPTED
    }


}
