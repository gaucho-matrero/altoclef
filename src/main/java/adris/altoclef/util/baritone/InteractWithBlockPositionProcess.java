package adris.altoclef.util.baritone;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.ItemTarget;
import baritone.Baritone;
import baritone.api.pathing.goals.*;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.utils.BaritoneProcessHelper;

import java.util.Optional;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

// Literally a copy of baritone's GetToBlockProcess but we pass a position instead of a BOM

public class InteractWithBlockPositionProcess extends BaritoneProcessHelper {
    private BlockPos _target = null;
    //private boolean _rightClickOnArrival;

    private boolean _walkInto;
    private boolean _blockOnTopMustBeRemoved;

    private boolean _cancelRightClick;

    private Direction _interactSide;

    private int arrivalTickCount = 0;

    private ItemTarget _equipTarget = null;

    private AltoClef _mod;

    private boolean _failed;

    // How close we are expected to travel to get to the block. Increases as we fail.
    public int reachCounter = 0;

    private Input _interactInput = Input.CLICK_RIGHT;

    public InteractWithBlockPositionProcess(Baritone baritone, AltoClef mod) {
        super(baritone); _mod = mod;
    }

    public void getToBlock(BlockPos target, Direction interactSide, Input interactInput, boolean blockOnTopMustBeRemoved, boolean walkInto) {
        this.onLostControl();
        _target = target;
        _interactSide = interactSide;
        _interactInput = interactInput;
        _blockOnTopMustBeRemoved = blockOnTopMustBeRemoved;
        _walkInto = walkInto;

        _cancelRightClick = false;

        _failed = false;

        this.arrivalTickCount = 0;

        reachCounter = 0;
    }
    public void getToBlock(BlockPos target, Input interactInput) {
        this.getToBlock(target, interactInput, false);
    }
    public void getToBlock(BlockPos target, Input interactInput, boolean blockOnTopMustBeRemoved) {
        this.getToBlock(target, null, interactInput, blockOnTopMustBeRemoved, false);
    }

    public boolean isActive() {
        return _target != null;
    }

    public boolean failed() {
        return _failed;
    }

    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {

        if (calcFailed) {
            logDebug("Failed to calculate path, increasing reach to " + reachCounter);
            reachCounter++;
        }

        Goal goal = createGoal(_target, reachCounter);

        //if (_rightClickOnArrival || (goal.isInGoal(this.ctx.playerFeet()) && goal.isInGoal(this.baritone.getPathingBehavior().pathStart()) && isSafeToCancel)) {
        if (_interactInput == null) {
            if (goal.isInGoal(this.ctx.player().getBlockPos())) {
                this.onLostControl();
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            } else {
                return new PathingCommand(goal, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
        }

        switch (this.rightClick()) {
            case CANT_REACH:
                return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
            case WAIT_FOR_CLICK:
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            case CLICK_ATTEMPTED:
                this.onLostControl();
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            default:
                return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }

    }

    public synchronized void onLostControl() {
        _target = null;
        this.baritone.getInputOverrideHandler().clearAllKeys();
    }

    public synchronized void setInteractEquipItem(ItemTarget item) {
        _equipTarget = item;
    }

    public String displayName0() {
        return "Get To " + _target;
    }
    private Goal createGoal(BlockPos pos, int reachDistance) {

        boolean sideMatters = !sideDoesntMatter();
        if (sideMatters) {
            Vec3i offs = _interactSide.getVector();
            if (offs.getY() == -1) {
                // If we're below, place ourselves two blocks below.
                offs = offs.down();
            }
            pos = pos.add(offs);


        }

        if (_walkInto) {
            return new GoalTwoBlocks(pos);
        } else {
            if (sideMatters) {
                // Make sure we're on the right side of the block.
                /*
                Vec3i offs = _interactSide.getVector();
                Goal sideGoal;
                if (offs.getY() == 1) {
                    sideGoal = new GoalYLevel(_target.getY() + 1);
                } else if (offs.getY() == -1) {
                    sideGoal = new GoalYLevel(_target.getY() - 1);
                } else {
                    sideGoal = new GoalXZ(_target.getX() + offs.getX(), _target.getZ() + offs.getZ());
                }*/
                Goal sideGoal = new GoalBlockSide(_target, _interactSide, 1);
                return new GoalAnd(sideGoal, new GoalNear(pos, reachDistance));
            } else {
                // TODO: Cleaner method of picking which side to approach from. This is only here for the lava stuff.
                return new GoalNear(pos, reachDistance);
            }
        }
    }

    private ClickResponse rightClick() {

        Optional<Rotation> reachable = getReach();
        if (reachable.isPresent()) {
            //Debug.logMessage("Reachable: UPDATE");
            this.baritone.getLookBehavior().updateTarget(reachable.get(), true);
            if (this.baritone.getPlayerContext().isLookingAt(_target)) {
                if (_equipTarget != null) _mod.getInventoryTracker().equipItem(_equipTarget);
                this.baritone.getInputOverrideHandler().setInputForceState(_interactInput, true);
                //System.out.println(this.ctx.player().playerScreenHandler);

                if (this.arrivalTickCount++ > 20 || _cancelRightClick) {
                    _failed = true;
                    this.logDirect("Right click timed out/cancelled");
                    return ClickResponse.CLICK_ATTEMPTED;
                }
            }
            return ClickResponse.WAIT_FOR_CLICK;
        }
        return ClickResponse.CANT_REACH;
    }

    private enum ClickResponse {
        CANT_REACH,
        WAIT_FOR_CLICK,
        CLICK_ATTEMPTED
    }

    private boolean sideDoesntMatter() {
        return _interactSide == null;
    }

    public Optional<Rotation> getReach() {
        Optional<Rotation> reachable;
        if (sideDoesntMatter()) {
            reachable = RotationUtils.reachable(this.ctx.player(), _target, this.ctx.playerController().getBlockReachDistance());
        } else {
            Vec3i sideVector = _interactSide.getVector();
            Vec3d centerOffset = new Vec3d(0.5 + sideVector.getX() * 0.5, 0.5 + sideVector.getY() * 0.5, 0.5 + sideVector.getZ() * 0.5);

            Vec3d sidePoint = centerOffset.add(_target.getX(), _target.getY(), _target.getZ());

            //reachable(this.ctx.player(), _target, this.ctx.playerController().getBlockReachDistance());
            reachable = RotationUtils.reachableOffset(ctx.player(), _target, sidePoint, ctx.playerController().getBlockReachDistance(), false);

            // Check for right angle
            if (reachable.isPresent()) {
                // Note: If sneak, use RotationUtils.inferSneakingEyePosition
                Vec3d camPos = ctx.player().getCameraPosVec(1.0F);
                Vec3d vecToPlayerPos = camPos.subtract(sidePoint);

                double dot = vecToPlayerPos.normalize().dotProduct(new Vec3d(sideVector.getX(), sideVector.getY(), sideVector.getZ()));
                if (dot < 0) {
                    // We're perpendicular and cannot face.
                    Debug.logMessage("DOT PRODUCT FAIL: " + dot);
                    return Optional.empty();
                }
            }
        }

        return reachable;
    }
}
